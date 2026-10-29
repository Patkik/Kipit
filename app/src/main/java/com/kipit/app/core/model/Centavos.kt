package com.kipit.app.core.model
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale
@JvmInline value class Centavos(val value:Long):Comparable<Centavos> {
 operator fun plus(v:Centavos)=Centavos(Math.addExact(value,v.value))
 operator fun minus(v:Centavos)=Centavos(Math.subtractExact(value,v.value))
 operator fun times(v:Long)=Centavos(Math.multiplyExact(value,v))
 operator fun div(v:Long)=Centavos(value/v)
 override fun compareTo(other:Centavos)=value.compareTo(other.value)
 fun toPhpString():String { val n=java.math.BigInteger.valueOf(value).abs(); val parts=n.divideAndRemainder(java.math.BigInteger.valueOf(100)); return (if(value<0) "−" else "")+"₱"+String.format(Locale.US,"%,d",parts[0])+"."+parts[1].toString().padStart(2,'0') }
 companion object { fun parse(s:String)=Centavos(BigDecimal(s.replace(",","")).setScale(2,RoundingMode.UNNECESSARY).movePointRight(2).longValueExact()) }
}
enum class WalletType { CASH,E_WALLET,BANK,CREDIT }
enum class TxType { EXPENSE,INCOME,TRANSFER,OPENING }
enum class ImpulseStatus { COOLING_OFF,SKIPPED_AND_SAVED,PURCHASED }
