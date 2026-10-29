package com.kipit.app.domain.usecase
import androidx.room.withTransaction
import com.kipit.app.data.local.KipitDatabase
import com.kipit.app.core.model.*
import kotlinx.coroutines.flow.first
import java.time.*
import javax.inject.Inject
class ExecuteSurplusSweepUseCase @Inject constructor(private val db:KipitDatabase){
 suspend operator fun invoke()=db.withTransaction {
 val dao=db.dao();val date=LocalDate.now();val config=dao.readConfig()?:error("Configuration unavailable")
 require(config.lastSweepDate!=date.toString()){ "Today's sweep is already complete" }
 val wallets=dao.wallets().first();val categories=dao.categories().first();val txs=dao.transactions().first()
 val today=txs.filter{Instant.ofEpochMilli(it.timestampEpochMs).atZone(ZoneId.systemDefault()).toLocalDate()==date && it.type==TxType.EXPENSE}
 val spent=today.sumMoney{Math.addExact(it.amountCentavos,it.feeCentavos)}
 val safe=CalculateSafeToSpendUseCase()(wallets,categories,txs,config).value
 val openingLimit=(Math.addExact(safe,spent/(date.lengthOfMonth()-date.dayOfMonth+1))).coerceAtLeast(0)
 val sweep=Math.subtractExact(openingLimit,spent).coerceAtLeast(0)/2
 val available=Math.multiplyExact(safe,(date.lengthOfMonth()-date.dayOfMonth+1).toLong()).coerceAtLeast(0)
 val amount=sweep.coerceAtMost(available)
 val discretionary=categories.filter{it.isDiscretionary}.map{it.id}.toSet()
 val noSpend=today.none{it.categoryId in discretionary}
 dao.setConfig(config.copy(lockedSavingsCentavos=Math.addExact(config.lockedSavingsCentavos,amount),lastSweepDate=if(amount>0)date.toString() else config.lastSweepDate,currentNoSpendStreak=if(noSpend && config.lastStreakDate!=date.toString()) config.currentNoSpendStreak+1 else if(!noSpend)0 else config.currentNoSpendStreak,lastStreakDate=date.toString()))
 amount
 }
}
