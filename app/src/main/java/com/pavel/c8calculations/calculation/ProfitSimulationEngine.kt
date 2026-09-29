package com.pavel.c8calculations.calculation

import com.pavel.c8calculations.model.DailyCalculationInput
import com.pavel.c8calculations.model.ProfitSimulationDay
import com.pavel.c8calculations.model.ProfitSimulationInput
import com.pavel.c8calculations.model.ProfitSimulationResult
import java.math.BigDecimal

object ProfitSimulationEngine {
    private val WITHHOLDING_RATE = BigDecimal("0.30")
    private val NET_RATE = BigDecimal("0.70")

    fun simulate(input: ProfitSimulationInput): ProfitSimulationResult {
        require(input.numberOfDays >= 0) { "Number of days must be non-negative" }
        require(input.startingBalance >= BigDecimal.ZERO) { "Starting balance must be non-negative" }

        var level = input.startingLevel
        var balance = input.startingBalance
        val days = buildList {
            repeat(input.numberOfDays) { index ->
                val date = input.startDate.plusDays(index.toLong())
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
                add(
                    ProfitSimulationDay(
                        date = date,
                        levelUsed = level,
                        depositUsed = config.deposit,
                        signalCount = daily.signalCount,
                        incomePerSignal = daily.incomePerSignal,
                        dailyIncome = daily.dailyIncome,
                        balanceAfter = daily.balanceAfter,
                        levelForNextDay = nextLevel,
                    )
                )
                balance = daily.balanceAfter
                level = nextLevel
            }
        }

        val currentDeposit = LevelConfiguration.forLevel(level).deposit
        val grossProfit = balance.subtract(currentDeposit)
        val withholding = grossProfit.multiply(WITHHOLDING_RATE)
        val netProfit = grossProfit.multiply(NET_RATE)

        return ProfitSimulationResult(
            days = days,
            expectedBalance = balance,
            finalLevel = level,
            currentDeposit = currentDeposit,
            grossProfit = grossProfit,
            withholding = withholding,
            netProfit = netProfit,
        )
    }
}
