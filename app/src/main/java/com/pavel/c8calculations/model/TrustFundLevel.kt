package com.pavel.c8calculations.model

import java.math.BigDecimal

/** Optional daily trust fund income, independent of the VIP signal bonus. */
enum class TrustFundLevel(val dailyIncome: BigDecimal) {
    NONE(BigDecimal.ZERO),
    VIP1(BigDecimal("35.7")),
    VIP2(BigDecimal("120")),
    VIP3(BigDecimal("217")),
    VIP4(BigDecimal("500")),
    VIP5(BigDecimal("1060")),
    VIP6(BigDecimal("1900"));

    /** A disabled trust fund never adds income, but the chosen level is kept for later. */
    fun whenEnabled(enabled: Boolean): TrustFundLevel = if (enabled) this else NONE
}
