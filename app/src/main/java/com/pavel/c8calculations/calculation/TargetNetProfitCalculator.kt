package com.pavel.c8calculations.calculation

import com.pavel.c8calculations.model.DailyCalculationInput
import com.pavel.c8calculations.model.ProfitSimulationDay
import com.pavel.c8calculations.model.TargetNetProfitInput
import com.pavel.c8calculations.model.TargetNetProfitResult
import java.math.BigDecimal

object TargetNetProfitCalculator {
    private val WITHHOLDING_RATE = BigDecimal("0.30")
    private val NET_RATE = BigDecimal("0.70")
    private const val MAX_DAYS = 36500

    fun calculate(input: TargetNetProfitInput): TargetNetProfitResult {
        require(input.startingBalance >= BigDecimal.ZERO) { "Starting balance must be non-negative" }
        require(input.targetNetProfit >= BigDecimal.ZERO) { "Target net profit must be non-negative" }
        require(input.x >= 0) { "X must be non-negative" }
        require(input.l1Count >= 0) { "L1 count must be non-negative" }

        var level = input.startingLevel
        var balance = input.startingBalance
        var date = input.startDate
        val days = mutableListOf<ProfitSimulationDay>()

        fun netProfit(currentBalance: BigDecimal, currentLevel: com.pavel.c8calculations.model.ParticipantLevel): BigDecimal {
            val deposit = LevelConfiguration.forLevel(currentLevel).deposit
            return currentBalance.subtract(deposit).multiply(NET_RATE)
        }

        if (netProfit(balance, level) >= input.targetNetProfit) {
            val deposit = LevelConfiguration.forLevel(level).deposit
            val gross = balance.subtract(deposit)
            return TargetNetProfitResult(date, 0, 0, 0, BigDecimal.ZERO, balance, level, deposit, gross, gross.multiply(WITHHOLDING_RATE), gross.multiply(NET_RATE), emptyList())
        }

        while (true) {
            require(days.size < MAX_DAYS) { "Target net profit was not reached within $MAX_DAYS days" }
            val config = LevelConfiguration.forLevel(level)
            val daily = DailyCalculationEngine.calculate(
                DailyCalculationInput(level, date.dayOfWeek, balance, input.x, input.isVip, input.l1Count)
            )
            val nextLevel = LevelUpgradeEngine.resolveLevel(level, daily.balanceAfter, input.autoUpgradeEnabled)

            var signalBalance = balance
            for (signal in 1..daily.signalCount) {
                signalBalance = signalBalance.add(daily.incomePerSignal)
                val levelForProfit = if (signal == daily.signalCount) nextLevel else level
                val candidateNet = netProfit(signalBalance, levelForProfit)
                if (candidateNet >= input.targetNetProfit) {
                    val reachedDeposit = LevelConfiguration.forLevel(levelForProfit).deposit
                    val gross = signalBalance.subtract(reachedDeposit)
                    val partialDay = ProfitSimulationDay(date, level, config.deposit, signal, daily.incomePerSignal, daily.incomePerSignal.multiply(signal.toBigDecimal()), signalBalance, levelForProfit)
                    val resultDays = days + partialDay
                    return TargetNetProfitResult(
                        reachedDate = date,
                        daysCount = resultDays.size,
                        reachedAfterSignal = signal,
                        totalSignals = days.sumOf { it.signalCount } + signal,
                        totalIncome = days.fold(BigDecimal.ZERO) { acc, day -> acc.add(day.dailyIncome) }.add(daily.incomePerSignal.multiply(signal.toBigDecimal())),
                        reachedBalance = signalBalance,
                        finalLevel = levelForProfit,
                        currentDeposit = reachedDeposit,
                        grossProfit = gross,
                        withholding = gross.multiply(WITHHOLDING_RATE),
                        netProfit = gross.multiply(NET_RATE),
                        days = resultDays,
                    )
                }
            }

            days += ProfitSimulationDay(date, level, config.deposit, daily.signalCount, daily.incomePerSignal, daily.dailyIncome, daily.balanceAfter, nextLevel)
            balance = daily.balanceAfter
            level = nextLevel
            date = date.plusDays(1)
        }
    }
}
