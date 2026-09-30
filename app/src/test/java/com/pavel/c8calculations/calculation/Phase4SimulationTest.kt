package com.pavel.c8calculations.calculation

import com.pavel.c8calculations.model.ParticipantLevel
import com.pavel.c8calculations.model.ProfitSimulationInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

class Phase4SimulationTest {

    private fun assertMoney(expected: String, actual: BigDecimal) =
        assertEquals(0, BigDecimal(expected).compareTo(actual))

    @Test
    fun upgradeChoosesMaximumAvailableLevelWithoutReducingBalance() {
        assertEquals(
            ParticipantLevel.C4,
            LevelUpgradeEngine.resolveLevel(ParticipantLevel.C1, BigDecimal("3500"), true)
        )
        assertEquals(
            ParticipantLevel.C1,
            LevelUpgradeEngine.resolveLevel(ParticipantLevel.C1, BigDecimal("10000"), false)
        )
    }

    @Test
    fun upgradeAppliesOnlyFromNextCalendarDay() {
        val result = ProfitSimulationEngine.simulate(
            ProfitSimulationInput(
                startDate = LocalDate.of(2026, 9, 28),
                numberOfDays = 2,
                startingLevel = ParticipantLevel.C1,
                startingBalance = BigDecimal("697.6"),
            )
        )

        assertEquals(ParticipantLevel.C1, result.days[0].levelUsed)
        assertMoney("4.8", result.days[0].dailyIncome)
        assertMoney("702.4", result.days[0].balanceAfter)
        assertEquals(ParticipantLevel.C2, result.days[0].levelForNextDay)
        assertEquals(ParticipantLevel.C2, result.days[1].levelUsed)
        assertMoney("11.2", result.days[1].dailyIncome)
        assertMoney("713.6", result.expectedBalance)
    }

    @Test
    fun autoUpgradeOffKeepsOriginalLevelAndDeposit() {
        val result = ProfitSimulationEngine.simulate(
            ProfitSimulationInput(
                startDate = LocalDate.of(2026, 9, 28),
                numberOfDays = 2,
                startingLevel = ParticipantLevel.C1,
                startingBalance = BigDecimal("10000"),
                autoUpgradeEnabled = false,
            )
        )

        assertEquals(ParticipantLevel.C1, result.finalLevel)
        assertMoney("300", result.currentDeposit)
        assertEquals(listOf(ParticipantLevel.C1, ParticipantLevel.C1), result.days.map { it.levelUsed })
    }

    @Test
    fun thirtyPercentAffectsOnlyNetProfitAndNeverBalance() {
        val result = ProfitSimulationEngine.simulate(
            ProfitSimulationInput(
                startDate = LocalDate.of(2026, 9, 28),
                numberOfDays = 1,
                startingLevel = ParticipantLevel.C1,
                startingBalance = BigDecimal("300"),
                autoUpgradeEnabled = false,
            )
        )

        assertMoney("304.8", result.expectedBalance)
        assertMoney("4.8", result.grossProfit)
        assertMoney("1.440", result.withholding)
        assertMoney("3.360", result.netProfit)
    }

    @Test
    fun fridayAndSaturdayRulesRemainActiveInsideForecast() {
        val result = ProfitSimulationEngine.simulate(
            ProfitSimulationInput(
                startDate = LocalDate.of(2026, 10, 2),
                numberOfDays = 2,
                startingLevel = ParticipantLevel.C6,
                startingBalance = BigDecimal("10000"),
                l1Count = 10,
            )
        )

        assertEquals(listOf(2, 2), result.days.map { it.signalCount })
        assertMoney("10320", result.expectedBalance)
    }

    @Test
    fun dateModeProcessesTheWholeEndDateWhileTargetModeMayStopMidDay() {
        val dateResult = ProfitSimulationEngine.simulate(
            ProfitSimulationInput(
                startDate = LocalDate.of(2026, 9, 30),
                numberOfDays = 30,
                startingLevel = ParticipantLevel.C5,
                startingBalance = BigDecimal("6000"),
                autoUpgradeEnabled = true,
                x = 1,
                isVip = true,
                l1Count = 0,
            )
        )

        assertEquals(LocalDate.of(2026, 10, 29), dateResult.days.last().date)
        assertEquals(6, dateResult.days.last().signalCount)
        assertMoney("14512", dateResult.expectedBalance)

        val targetResult = TargetBalanceCalculator.calculate(
            com.pavel.c8calculations.model.TargetBalanceInput(
                startDate = LocalDate.of(2026, 9, 30),
                startingLevel = ParticipantLevel.C5,
                startingBalance = BigDecimal("6000"),
                targetBalance = BigDecimal("14352"),
                autoUpgradeEnabled = true,
                x = 1,
                isVip = true,
                l1Count = 0,
            )
        )

        assertEquals(LocalDate.of(2026, 10, 29), targetResult.reachedDate)
        assertEquals(4, targetResult.reachedAfterSignal)
        assertMoney("14352", targetResult.reachedBalance)
    }

    @Test
    fun invalidSimulationValuesAreRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            ProfitSimulationEngine.simulate(
                ProfitSimulationInput(
                    startDate = LocalDate.of(2026, 9, 28),
                    numberOfDays = -1,
                    startingLevel = ParticipantLevel.C1,
                    startingBalance = BigDecimal("300"),
                )
            )
        }
    }
}
