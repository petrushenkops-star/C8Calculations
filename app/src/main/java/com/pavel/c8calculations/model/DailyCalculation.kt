package com.pavel.c8calculations.model

import java.math.BigDecimal
import java.time.DayOfWeek

data class DailyCalculationInput(
    val level: ParticipantLevel,
    val dayOfWeek: DayOfWeek,
    val previousBalance: BigDecimal,
    val x: Int = 0,
    val isVip: Boolean = false,
    val l1Count: Int = 0,
)

data class DailyCalculationResult(
    val level: ParticipantLevel,
    val signalCount: Int,
    val incomePerSignal: BigDecimal,
    val dailyIncome: BigDecimal,
    val balanceAfter: BigDecimal,
)
