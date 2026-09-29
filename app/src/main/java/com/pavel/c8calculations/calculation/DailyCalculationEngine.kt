package com.pavel.c8calculations.calculation

import com.pavel.c8calculations.model.DailyCalculationInput
import com.pavel.c8calculations.model.DailyCalculationResult

object DailyCalculationEngine {
    fun calculate(input: DailyCalculationInput): DailyCalculationResult {
        val config = LevelConfiguration.forLevel(input.level)
        val signals = SignalRuleEngine.signalCount(
            level = input.level,
            dayOfWeek = input.dayOfWeek,
            x = input.x,
            isVip = input.isVip,
            l1Count = input.l1Count,
        )
        val dailyIncome = config.incomePerSignal.multiply(signals.toBigDecimal())

        return DailyCalculationResult(
            level = input.level,
            signalCount = signals,
            incomePerSignal = config.incomePerSignal,
            dailyIncome = dailyIncome,
            balanceAfter = input.previousBalance.add(dailyIncome),
        )
    }
}
