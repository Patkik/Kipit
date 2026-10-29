package com.kipit.app.domain.usecase

import com.kipit.app.core.model.*
import com.kipit.app.data.local.entity.*
import java.time.*

/** Applies a proposed operation to an immutable snapshot, using the committed ledger rules. */
fun previewTransaction(
    wallets: List<WalletEntity>, categories: List<CategoryEntity>,
    transactions: List<TransactionEntity>, config: UserConfigEntity,
    tx: TransactionEntity, owed: Long = 0, date: LocalDate = LocalDate.now()
): Centavos {
    require(tx.amountCentavos >= 0 && tx.feeCentavos >= 0 && owed >= 0)
    val outflow = Math.addExact(Math.addExact(tx.amountCentavos, tx.feeCentavos), owed)
    val projected = wallets.map { wallet ->
        val change = when {
            wallet.id == tx.sourceWalletId -> if (tx.type in listOf(TxType.INCOME,TxType.OPENING)) tx.amountCentavos else -outflow
            tx.type == TxType.TRANSFER && wallet.id == tx.destinationWalletId -> tx.amountCentavos
            else -> 0L
        }
        wallet.copy(balanceCentavos = Math.addExact(wallet.balanceCentavos, change))
    }
    return CalculateSafeToSpendUseCase()(projected, categories, transactions + tx, config, date)
}
