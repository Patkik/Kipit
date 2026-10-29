package com.kipit.app.core.model

import java.math.BigDecimal
import java.math.RoundingMode

inline fun <T> Iterable<T>.sumMoney(value: (T) -> Long): Long =
    fold(0L) { total, item -> Math.addExact(total, value(item)) }

fun percentageCentavos(amount: Long, percent: Int): Long {
    require(amount >= 0 && percent in 0..100)
    return BigDecimal.valueOf(amount).multiply(BigDecimal.valueOf(percent.toLong()))
        .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP).longValueExact()
}
