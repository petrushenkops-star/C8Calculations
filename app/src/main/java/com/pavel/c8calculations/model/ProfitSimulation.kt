package com.pavel.c8calculations.model

import java.math.BigDecimal
import java.time.LocalDate

data class ProfitSimulationInput(
    val startDate: LocalDate,
    val numberOfDays: Int,
    val startingLevel: ParticipantLevel,
    val startingBalance: BigDecimal,
    val autoUpgradeEnabled: Boolean = true,
    val x: Int = 0,
    val isVip: Boolean = false,
    val l1Count: Int = 0,
)

data class ProfitSimulationDay(
    val date: LocalDate,
    val levelUsed: ParticipantLevel,
    val depositUsed: BigDecimal,
    val signalCount: Int,
    val incomePerSignal: BigDecimal,
    val dailyIncome: BigDecimal,
    val balanceAfter: BigDecimal,
    val levelForNextDay: ParticipantLevel,
)

data class ProfitSimulationResult(
    val days: List<ProfitSimulationDay>,
    val expectedBalance: BigDecimal,
    val finalLevel: ParticipantLevel,
    val currentDeposit: BigDecimal,
    val grossProfit: BigDecimal,
    val withholding: BigDecimal,
    val netProfit: BigDecimal,
)
