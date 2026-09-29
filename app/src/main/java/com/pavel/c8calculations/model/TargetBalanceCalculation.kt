package com.pavel.c8calculations.model

import java.math.BigDecimal
import java.time.LocalDate

data class TargetBalanceInput(
    val startDate: LocalDate,
    val startingLevel: ParticipantLevel,
    val startingBalance: BigDecimal,
    val targetBalance: BigDecimal,
    val autoUpgradeEnabled: Boolean = true,
    val x: Int = 0,
    val isVip: Boolean = false,
    val l1Count: Int = 0,
)

data class TargetBalanceResult(
    val reachedDate: LocalDate,
    val daysCount: Int,
    val totalSignals: Int,
    val totalIncome: BigDecimal,
    val reachedBalance: BigDecimal,
    val finalLevel: ParticipantLevel,
    val currentDeposit: BigDecimal,
    val grossProfit: BigDecimal,
    val withholding: BigDecimal,
    val netProfit: BigDecimal,
    val days: List<ProfitSimulationDay>,
)
