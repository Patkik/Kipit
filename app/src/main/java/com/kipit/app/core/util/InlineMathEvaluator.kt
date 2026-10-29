package com.kipit.app.core.util
import com.kipit.app.core.model.Centavos
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
object InlineMathEvaluator {
 fun evaluate(input:String):Result<Centavos> = runCatching {
  val s=input.filterNot(Char::isWhitespace).trimEnd('+','-','*','/')
  if(s.isEmpty()) return@runCatching Centavos(0)
  var i=0
  fun number():BigDecimal { val start=i; if(i<s.length && s[i] in "+-")i++;while(i<s.length && (s[i].isDigit() || s[i]=='.'))i++;return s.substring(start,i).toBigDecimal() }
  fun term():BigDecimal { var v=number();while(i<s.length && s[i] in "*/"){val op=s[i++];val n=number();v=if(op=='*')v.multiply(n,MathContext.DECIMAL128) else v.divide(n,MathContext.DECIMAL128)};return v }
  var v=term();while(i<s.length){val op=s[i++];require(op in "+-");val n=term();v=if(op=='+')v.add(n) else v.subtract(n)}
  Centavos(v.setScale(2,RoundingMode.HALF_UP).movePointRight(2).longValueExact())
 }
}
