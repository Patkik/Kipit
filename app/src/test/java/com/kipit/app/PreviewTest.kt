package com.kipit.app

import com.kipit.app.core.model.*
import com.kipit.app.data.local.entity.*
import com.kipit.app.domain.usecase.*
import org.junit.Assert.*
import org.junit.Test
import java.time.*

class PreviewTest {
    private val date = LocalDate.of(2026,10,29)
    private val wallets = listOf(WalletEntity(1,"Cash",WalletType.CASH,300000),WalletEntity(2,"Bank",WalletType.BANK,0))
    private val categories = listOf(CategoryEntity(1,"Food",monthlyLimitCentavos=100000))
    private fun tx(type:TxType=TxType.EXPENSE,amount:Long=30000,fee:Long=0) = TransactionEntity(
        type=type,amountCentavos=amount,feeCentavos=fee,sourceWalletId=1,
        destinationWalletId=if(type==TxType.TRANSFER)2 else null,categoryId=if(type==TxType.EXPENSE)1 else null,
        timestampEpochMs=date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),hourOfDay=0,dayOfWeek=4,isWeekend=false)
    private fun preview(t:TransactionEntity,owed:Long=0)=previewTransaction(wallets,categories,emptyList(),UserConfigEntity(),t,owed,date).value
    @Test fun expenseUsesRollingBudget(){assertEquals(90000L,preview(tx()))}
    @Test fun openingBalanceIncreasesRoom(){assertEquals(110000L,preview(tx(TxType.OPENING)))}
    @Test fun incomeIncreasesRoom(){assertEquals(110000L,preview(tx(TxType.INCOME)))}
    @Test fun transferPreservesPrincipal(){assertEquals(100000L,preview(tx(TxType.TRANSFER)))}
    @Test fun transferFeeReducesPool(){assertEquals(99500L,preview(tx(TxType.TRANSFER,fee=1500)))}
    @Test fun splitIncludesFullCashAdvanced(){assertEquals(83333L,preview(tx(amount=15000),35000))}
    @Test fun billPaymentDoesNotDoubleConsumeReserve(){
        val bill=categories.single().copy(isFixedBill=true,monthlyLimitCentavos=30000)
        assertEquals(90000L,previewTransaction(wallets,listOf(bill),emptyList(),UserConfigEntity(),tx(),date=date).value)
    }
    @Test fun sandboxRoundsFractionalCentavos(){assertEquals(151L,percentageCentavos(15050,1))}
    @Test fun sandboxHandlesLargestAmount(){assertEquals(Long.MAX_VALUE,percentageCentavos(Long.MAX_VALUE,100))}
    @Test(expected=ArithmeticException::class) fun aggregationRejectsOverflow(){listOf(Long.MAX_VALUE,1L).sumMoney{it}}
    @Test fun blankAmountDoesNotNudge(){assertFalse(EvaluateKeypadImpactUseCase()(Centavos(0),Centavos(0),Centavos(7500),Centavos(30000),Centavos(100),true,0f).triggerImpulseVaultNudge)}
}
