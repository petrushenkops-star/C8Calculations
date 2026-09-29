package com.pavel.c8calculations.calculation

import com.pavel.c8calculations.model.DailyCalculationInput
import com.pavel.c8calculations.model.ProfitSimulationDay
import com.pavel.c8calculations.model.TargetBalanceInput
import com.pavel.c8calculations.model.TargetBalanceResult
import java.math.BigDecimal

object TargetBalanceCalculator {
    private val WITHHOLDING_RATE = BigDecimal("0.30")
    private val NET_RATE = BigDecimal("0.70")
    private const val MAX_DAYS = 36500

    fun calculate(input: TargetBalanceInput): TargetBalanceResult {
        require(input.startingBalance >= BigDecimal.ZERO) { "Starting balance must be non-negative" }
        require(input.targetBalance >= BigDecimal.ZERO) { "Target balance must be non-negative" }
        require(input.x >= 0) { "X must be non-negative" }
        require(input.l1Count >= 0) { "L1 count must be non-negative" }

        var level = input.startingLevel
        var balance = input.startingBalance
        var date = input.startDate
        val days = mutableListOf<ProfitSimulationDay>()

        while (balance < input.targetBalance) {
            require(days.size < MAX_DAYS) { "Target balance was not reached within $MAX_DAYS days" }

            val config = LevelConfiguration.forLevel(level)
            val daily = DailyCalculationEngine.calculate(
                DailyCalculationInput(
                    level = level,
                    dayOfWeek = date.dayOfWeek,
                    previousBalance = balance,
                    x = input.x,
                    isVip = input.isVip,
                    l1Count = input.l1Count,
                )
            )
            val nextLevel = LevelUpgradeEngine.resolveLevel(
                currentLevel = level,
                balanceAfterDay = daily.balanceAfter,
                autoUpgradeEnabled = input.autoUpgradeEnabled,
            )
            days += ProfitSimulationDay(
                date = date,
                levelUsed = level,
                depositUsed = config.deposit,
                signalCount = daily.signalCount,
                incomePerSignal = daily.incomePerSignal,
                dailyIncome = daily.dailyIncome,
                balanceAfter = daily.balanceAfter,
                levelForNextDay = nextLevel,
            )
            balance = daily.balanceAfter
            level = nextLevel
            if (balance < input.targetBalance) date = date.plusDays(1)
        }

        val deposit = LevelConfiguration.forLevel(level).deposit
        val gross = balance.subtract(deposit)
        return TargetBalanceResult(
            reachedDate = if (days.isEmpty()) input.startDate else days.last().date,
            daysCount = days.size,
            totalSignals = days.sumOf { it.signalCount },
            totalIncome = days.fold(BigDecimal.ZERO) { acc, day -> acc.add(day.dailyIncome) },
            reachedBalance = balance,
            finalLevel = level,
            currentDeposit = deposit,
            grossProfit = gross,
            withholding = gross.multiply(WITHHOLDING_RATE),
            netProfit = gross.multiply(NET_RATE),
            days = days,
        )
    }
}
