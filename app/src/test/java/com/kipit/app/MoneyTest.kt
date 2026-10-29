package com.kipit.app
import org.junit.Test
import org.junit.Assert.*
import com.kipit.app.core.model.*
import com.kipit.app.core.util.*
import com.kipit.app.data.local.entity.*
import com.kipit.app.domain.usecase.*
import java.time.LocalDate
class MoneyTest {
 @Test fun precedence(){assertEquals(21000L,InlineMathEvaluator.evaluate("120 + 45 * 2").getOrThrow().value)}
 @Test fun trailingOperator(){assertEquals(12000L,InlineMathEvaluator.evaluate("120 + ").getOrThrow().value)}
 @Test fun decimalPrecision(){assertEquals(30L,InlineMathEvaluator.evaluate("0.1+0.2").getOrThrow().value)}
 @Test fun divisionByZero(){assertTrue(InlineMathEvaluator.evaluate("12/0").isFailure)}
 @Test fun overflow(){assertTrue(InlineMathEvaluator.evaluate("9999999999999999999999").isFailure)}
 @Test fun monthEnd(){val wallets=listOf(WalletEntity(name="Cash",type=WalletType.CASH,balanceCentavos=10000));assertEquals(10000L,CalculateSafeToSpendUseCase()(wallets,emptyList(),emptyList(),UserConfigEntity(),LocalDate.of(2026,10,31)).value)}
 @Test fun lockedSavings(){val wallets=listOf(WalletEntity(name="Cash",type=WalletType.CASH,balanceCentavos=10000));assertEquals(6000L,CalculateSafeToSpendUseCase()(wallets,emptyList(),emptyList(),UserConfigEntity(lockedSavingsCentavos=4000),LocalDate.of(2026,10,31)).value)}
 @Test fun splitRemainder(){val e=MicroSyntaxLexer.parse("10 Food split:3 #trip",listOf(WalletEntity(id=1,name="Cash",type=WalletType.CASH,balanceCentavos=10000)),listOf(CategoryEntity(id=1,name="Food",monthlyLimitCentavos=10000))).getOrThrow().single();assertEquals(333L,e.personal.value);assertEquals(667L,e.owed.value);assertEquals("trip",e.tags)}
}
