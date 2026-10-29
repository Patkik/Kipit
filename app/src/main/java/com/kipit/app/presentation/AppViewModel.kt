package com.kipit.app.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import androidx.room.withTransaction
import com.kipit.app.core.model.*
import com.kipit.app.core.util.*
import com.kipit.app.data.local.entity.*
import com.kipit.app.data.local.dao.*
import com.kipit.app.data.repository.KipitRepositoryImpl
import com.kipit.app.domain.usecase.*
import com.kipit.app.service.AppUpdateChecker
import com.kipit.app.service.UpdateCheckResult
import java.time.*

data class UiState(
    val wallets: List<WalletEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val transactions: List<TransactionEntity> = emptyList(),
    val config: UserConfigEntity = UserConfigEntity(),
    val loans: List<SplitReceivableEntity> = emptyList(),
    val impulses: List<ImpulseVaultEntity> = emptyList(),
    val captures: List<PendingCaptureEntity> = emptyList(),
    val macros: List<HabitMacro> = emptyList(),
    val predicted: List<Long> = emptyList(),
    val diagnostics: Diagnostics = Diagnostics(emptyList(), emptyList()),
    val busy: Boolean = false,
    val error: String? = null,
    val ready: Boolean = false
) {
    val safe get() = CalculateSafeToSpendUseCase()(wallets, categories, transactions, config)
}

sealed interface UiIntent {
    data class Settle(val id: Long) : UiIntent
    data class Impulse(val id: Long, val buy: Boolean, val wallet: Long) : UiIntent
    data object Sweep : UiIntent
}

@HiltViewModel
class AppViewModel @Inject constructor(
    val repository: KipitRepositoryImpl
) : ViewModel() {
    private val mutable = MutableStateFlow(UiState())
    val state = mutable.asStateFlow()
    val effects = Channel<String>(Channel.BUFFERED)

    private val _updateState = MutableStateFlow<UpdateCheckResult>(UpdateCheckResult.Idle)
    val updateState = _updateState.asStateFlow()

    private val dao = repository.dao

    init {
        viewModelScope.launch {
            if (dao.readConfig() == null) {
                repository.db.withTransaction {
                    dao.setConfig(UserConfigEntity())
                    listOf(
                        "Physical Cash" to WalletType.CASH,
                        "GCash" to WalletType.E_WALLET,
                        "Maya" to WalletType.E_WALLET,
                        "BPI Savings" to WalletType.BANK
                    ).forEach {
                        dao.insertWallet(WalletEntity(name = it.first, type = it.second, balanceCentavos = 0))
                    }
                    listOf("Food", "Transport", "Utilities", "Tech & Gaming", "Groceries", "Subscriptions").forEachIndexed { i, n ->
                        dao.insertCategory(CategoryEntity(name = n, monthlyLimitCentavos = if (i == 2) 300000 else 150000, isDiscretionary = i != 2, isFixedBill = i == 2))
                    }
                }
            }
            launch {
                combine(dao.wallets(), dao.categories(), dao.transactions(), dao.config()) { w, c, t, u ->
                    mutable.value.copy(wallets = w, categories = c, transactions = t, config = u ?: UserConfigEntity(), ready = true)
                }.collect {
                    mutable.value = it
                    refreshAnalytics()
                }
            }
            launch { dao.receivables().collect { v -> mutable.update { it.copy(loans = v) } } }
            launch { dao.impulses().collect { v -> mutable.update { it.copy(impulses = v) } } }
            launch { dao.captures().collect { v -> mutable.update { it.copy(captures = v) } } }
        }
    }

    private suspend fun refreshAnalytics() {
        val now = ZonedDateTime.now()
        val since = now.toInstant().toEpochMilli() - 2592000000L
        val macros = dao.macros(since)
        val predicted = dao.predictions(since, now.hour, now.dayOfWeek.value >= 6)
        val diagnostics = RunBehavioralDiagnosticsUseCase(dao)(System.currentTimeMillis())
        mutable.update { it.copy(macros = macros, predicted = predicted, diagnostics = diagnostics) }
    }

    fun clearError() {
        mutable.update { it.copy(error = null) }
    }

    fun checkForUpdates(currentVersion: String = com.kipit.app.BuildConfig.VERSION_NAME) {
        if (_updateState.value is UpdateCheckResult.Checking) return
        _updateState.value = UpdateCheckResult.Checking
        viewModelScope.launch {
            _updateState.value = AppUpdateChecker.checkForUpdates(currentVersion)
        }
    }

    fun action(block: suspend () -> Unit) {
        if (mutable.value.busy) return
        mutable.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            try {
                block()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                mutable.update { it.copy(error = e.message ?: "Could not save. Try again.") }
                effects.send(e.message ?: "Could not save. Try again.")
            } finally {
                mutable.update { it.copy(busy = false) }
            }
        }
    }

    fun dispatch(intent: UiIntent) = action {
        when (intent) {
            is UiIntent.Settle -> repository.settle(intent.id)
            is UiIntent.Impulse -> repository.decideImpulse(intent.id, intent.buy, intent.wallet)
            UiIntent.Sweep -> {
                val n = ExecuteSurplusSweepUseCase(repository.db)()
                effects.send("Reserved ${Centavos(n).toPhpString()} in savings")
            }
        }
    }

    fun log(
        amount: Long,
        type: TxType,
        wallet: Long,
        destination: Long?,
        category: Long?,
        note: String,
        fee: Long = 0,
        owed: Long = 0,
        debtor: String = "",
        capture: Long? = null,
        operationKey: String = java.util.UUID.randomUUID().toString(),
        onSuccess: () -> Unit = {}
    ) = action {
        val now = ZonedDateTime.now()
        val tx = TransactionEntity(
            type = type,
            amountCentavos = amount,
            feeCentavos = fee,
            sourceWalletId = wallet,
            destinationWalletId = destination,
            categoryId = category,
            timestampEpochMs = now.toInstant().toEpochMilli(),
            hourOfDay = now.hour,
            dayOfWeek = now.dayOfWeek.value,
            isWeekend = now.dayOfWeek.value >= 6,
            note = note,
            operationKey = if (capture != null) "capture:$capture" else operationKey
        )
        repository.db.withTransaction {
            repository.commit(tx, owed, debtor)
            if (capture != null) dao.deleteCapture(capture)
        }
        effects.send("Recorded")
        onSuccess()
    }

    fun batch(input: String, onSuccess: () -> Unit) = action {
        val s = state.value
        val entries = MicroSyntaxLexer.parse(input, s.wallets, s.categories).getOrThrow()
        repository.db.withTransaction {
            entries.forEach { e ->
                val now = ZonedDateTime.now()
                repository.commit(
                    TransactionEntity(
                        type = TxType.EXPENSE,
                        amountCentavos = e.personal.value,
                        sourceWalletId = e.walletId,
                        categoryId = e.categoryId ?: s.categories.first().id,
                        timestampEpochMs = now.toInstant().toEpochMilli(),
                        hourOfDay = now.hour,
                        dayOfWeek = now.dayOfWeek.value,
                        isWeekend = now.dayOfWeek.value >= 6,
                        note = e.note,
                        tagsCsv = e.tags
                    ),
                    e.owed.value,
                    if (e.owed.value > 0) "Split group" else ""
                )
            }
        }
        effects.send("Batch recorded")
        onSuccess()
    }

    fun park(name: String, amount: Long, category: Long, onSuccess: () -> Unit) = action {
        require(name.isNotBlank() && amount > 0)
        dao.insertImpulse(ImpulseVaultEntity(itemName = name, amountCentavos = amount, categoryId = category, createdAtMs = System.currentTimeMillis()))
        effects.send("Parked for seven days")
        onSuccess()
    }

    fun dismissCapture(id: Long) = action {
        dao.deleteCapture(id)
    }

    fun saveSettings(meal: Long, goal: Long, onSuccess: () -> Unit = {}) = action {
        require(meal > 0 && goal > 0)
        repository.db.withTransaction {
            val current = dao.readConfig() ?: error("Configuration unavailable")
            dao.setConfig(current.copy(mealAnchorCentavos = meal, savingsGoalCentavos = goal))
        }
        onSuccess()
    }

    fun setBudget(id: Long, limit: Long, onSuccess: () -> Unit = {}) = action {
        require(limit >= 0)
        repository.db.withTransaction {
            val c = dao.categories().first().first { it.id == id }
            dao.updateCategory(c.copy(monthlyLimitCentavos = limit))
        }
        onSuccess()
    }

    suspend fun search(q: String): List<TransactionEntity> {
        val quote = 34.toChar().toString()
        return dao.search(quote + q.replace(quote, quote + quote) + quote)
    }
}
