package com.pavel.c8calculations.model

import java.math.BigDecimal

/** Optional daily trust fund income, independent of the VIP signal bonus. */
enum class TrustFundLevel(val dailyIncome: BigDecimal) {
    NONE(BigDecimal.ZERO),
    VIP1(BigDecimal("35.7")),
    VIP2(BigDecimal("120")),
    VIP3(BigDecimal("217")),
    VIP4(BigDecimal("500")),
}
