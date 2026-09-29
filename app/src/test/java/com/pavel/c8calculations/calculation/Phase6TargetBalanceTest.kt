package com.pavel.c8calculations.calculation

import com.pavel.c8calculations.model.ParticipantLevel
import com.pavel.c8calculations.model.TargetBalanceInput
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

class Phase6TargetBalanceTest {
    private fun assertMoney(expected: String, actual: BigDecimal) =
        assertEquals(0, BigDecimal(expected).compareTo(actual))

    @Test
    fun alreadyReachedTargetReturnsZeroDays() {
        val start = LocalDate.of(2026, 9, 29)
        val result = TargetBalanceCalculator.calculate(
            TargetBalanceInput(start, ParticipantLevel.C1, BigDecimal("700"), BigDecimal("600"))
        )
        assertEquals(start, result.reachedDate)
        assertEquals(0, result.daysCount)
        assertEquals(0, result.reachedAfterSignal)
        assertEquals(0, result.totalSignals)
        assertMoney("0", result.totalIncome)
        assertMoney("700", result.reachedBalance)
    }

    @Test
    fun stopsOnFirstCalendarDayTargetIsReached() {
        val result = TargetBalanceCalculator.calculate(
            TargetBalanceInput(
                startDate = LocalDate.of(2026, 9, 28),
                startingLevel = ParticipantLevel.C1,
                startingBalance = BigDecimal("697.6"),
                targetBalance = BigDecimal("710"),
            )
        )
        assertEquals(LocalDate.of(2026, 9, 29), result.reachedDate)
        assertEquals(2, result.daysCount)
        assertEquals(2, result.reachedAfterSignal)
        assertEquals(4, result.totalSignals)
        assertMoney("16.0", result.totalIncome)
        assertMoney("713.6", result.reachedBalance)
        assertEquals(ParticipantLevel.C2, result.finalLevel)
        assertMoney("700", result.currentDeposit)
    }

    @Test
    fun targetModeKeepsFridaySaturdayAndUpgradeRules() {
        val result = TargetBalanceCalculator.calculate(
            TargetBalanceInput(
                startDate = LocalDate.of(2026, 10, 2),
                startingLevel = ParticipantLevel.C5,
                startingBalance = BigDecimal("9950"),
                targetBalance = BigDecimal("10100"),
                l1Count = 10,
            )
        )
        assertEquals(2, result.daysCount)
        assertEquals(1, result.reachedAfterSignal)
        assertEquals(3, result.totalSignals)
        assertMoney("176", result.totalIncome)
        assertMoney("10126", result.reachedBalance)
        assertEquals(ParticipantLevel.C6, result.finalLevel)
    }

    @Test
    fun profitIsCalculatedFromReachedBalanceAndFinalDeposit() {
        val result = TargetBalanceCalculator.calculate(
            TargetBalanceInput(
                startDate = LocalDate.of(2026, 9, 28),
                startingLevel = ParticipantLevel.C1,
                startingBalance = BigDecimal("300"),
                targetBalance = BigDecimal("304"),
                autoUpgradeEnabled = false,
            )
        )
        assertEquals(2, result.reachedAfterSignal)
        assertMoney("304.8", result.reachedBalance)
        assertMoney("4.8", result.grossProfit)
        assertMoney("1.440", result.withholding)
        assertMoney("3.360", result.netProfit)
    }
}
