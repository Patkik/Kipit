package com.kipit.app.domain.usecase
import com.kipit.app.core.model.*
import com.kipit.app.data.local.entity.*
import java.time.*
data class LiveKeypadConsequence(val newSafeToSpendToday:Centavos,val mealEquivalent:Float,val categoryUtilizationAfter:Float,val paceDeltaRatio:Float,val triggerImpulseVaultNudge:Boolean)
class EvaluateKeypadImpactUseCase {
 operator fun invoke(amount:Centavos,safe:Centavos,meal:Centavos,spent:Centavos,limit:Centavos,discretionary:Boolean,progress:Float):LiveKeypadConsequence {
 val after=safe-amount; val utilization=if(limit.value>0)(spent+amount).value.toFloat()/limit.value else 0f
 return LiveKeypadConsequence(after,if(meal.value>0)amount.value.toFloat()/meal.value else 0f,utilization,utilization-progress,amount.value>0 && discretionary && (after.value<0 || utilization-progress>0.15f))
 }
}
class CalculateSafeToSpendUseCase {
 operator fun invoke(wallets:List<WalletEntity>,categories:List<CategoryEntity>,transactions:List<TransactionEntity>,config:UserConfigEntity,date:LocalDate=LocalDate.now()):Centavos {
  val liquid=wallets.filter{!it.isArchived && it.type!=WalletType.CREDIT}.fold(0L){a,w->Math.addExact(a,w.balanceCentavos)}
  val month=transactions.filter{Instant.ofEpochMilli(it.timestampEpochMs).atZone(ZoneId.systemDefault()).toLocalDate().let{d->d.year==date.year && d.month==date.month} && it.type==TxType.EXPENSE}
  val bills=categories.filter{it.isFixedBill}.sumMoney{c->Math.subtractExact(c.monthlyLimitCentavos,month.filter{it.categoryId==c.id}.sumMoney{Math.addExact(it.amountCentavos,it.feeCentavos)}).coerceAtLeast(0)}
  // Wallet balances already include the full split debit: do not subtract receivables again.
  return Centavos(Math.subtractExact(Math.subtractExact(liquid,bills),config.lockedSavingsCentavos)/(date.lengthOfMonth()-date.dayOfMonth+1))
 }
 fun recovery(overspent:Centavos,daysRemaining:Int)=overspent/daysRemaining.coerceIn(1,7).toLong()
}
