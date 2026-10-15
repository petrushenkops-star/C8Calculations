package com.pavel.c8calculations.calculation

import com.pavel.c8calculations.model.DailyCalculationInput
import com.pavel.c8calculations.model.ParticipantLevel
import com.pavel.c8calculations.model.ProfitSimulationInput
import com.pavel.c8calculations.model.TargetBalanceInput
import com.pavel.c8calculations.model.TargetNetProfitInput
import com.pavel.c8calculations.model.TrustFundLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.DayOfWeek
import java.time.LocalDate

class TrustFundProfitTest {
    private val monday = LocalDate.of(2026, 9, 28)

    private fun assertMoney(expected: String, actual: BigDecimal) {
        assertEquals(0, BigDecimal(expected).compareTo(actual))
    }

    @Test
    fun dailyPaymentsMatchEachApprovedTrustFundLevel() {
        val expected = mapOf(
            TrustFundLevel.NONE to "0",
            TrustFundLevel.VIP1 to "35.7",
            TrustFundLevel.VIP2 to "120",
            TrustFundLevel.VIP3 to "217",
            TrustFundLevel.VIP4 to "500",
            TrustFundLevel.VIP5 to "1060",
            TrustFundLevel.VIP6 to "1900",
        )
        expected.forEach { (fund, payment) ->
            val result = DailyCalculationEngine.calculate(
                DailyCalculationInput(
                    level = ParticipantLevel.C1,
                    dayOfWeek = DayOfWeek.MONDAY,
                    previousBalance = BigDecimal("300"),
                    trustFundLevel = fund,
                )
            )
            assertMoney(payment, result.trustFundIncome)
            assertMoney("4.8", result.dailyIncome)
            assertMoney("304.8", result.balanceAfter.subtract(result.trustFundIncome))
        }
    }

    @Test
    fun fundAndVipSignalBonusAreIndependent() {
        val result = DailyCalculationEngine.calculate(
            DailyCalculationInput(
                ParticipantLevel.C1, DayOfWeek.MONDAY, BigDecimal("300"),
                isVip = true, trustFundLevel = TrustFundLevel.VIP1
            )
        )
        assertEquals(3, result.signalCount)
        assertMoney("7.2", result.dailyIncome)
        assertMoney("35.7", result.trustFundIncome)
        assertMoney("342.9", result.balanceAfter)
    }

    @Test
    fun fixedDateForecastAddsFundToSameBalanceAndCommissionBase() {
        val result = ProfitSimulationEngine.simulate(
            ProfitSimulationInput(
                startDate = monday,
                numberOfDays = 1,
                startingLevel = ParticipantLevel.C5,
                startingBalance = BigDecimal("6000"),
                autoUpgradeEnabled = false,
                trustFundLevel = TrustFundLevel.VIP1,
            )
        )
        assertMoney("192", result.days.single().dailyIncome)
        assertMoney("35.7", result.days.single().trustFundIncome)
        assertMoney("227.7", result.days.single().totalDailyIncome)
        assertMoney("6227.7", result.expectedBalance)
        assertMoney("227.7", result.grossProfit)
        assertMoney("68.310", result.withholding)
        assertMoney("159.390", result.netProfit)
    }

    @Test
    fun trustPaymentCanTriggerAutoUpgradeOnNextDay() {
        val result = ProfitSimulationEngine.simulate(
            ProfitSimulationInput(
                startDate = monday,
                numberOfDays = 2,
                startingLevel = ParticipantLevel.C1,
                startingBalance = BigDecimal("300"),
                trustFundLevel = TrustFundLevel.VIP4,
            )
        )
        assertMoney("804.8", result.days[0].balanceAfter)
        assertEquals(ParticipantLevel.C2, result.days[0].levelForNextDay)
        assertEquals(ParticipantLevel.C2, result.days[1].levelUsed)
        assertMoney("1316.0", result.expectedBalance)
    }

    @Test
    fun targetBalanceReachedByFundAfterAllSignals() {
        val result = TargetBalanceCalculator.calculate(
            TargetBalanceInput(
                startDate = monday,
                startingLevel = ParticipantLevel.C1,
                startingBalance = BigDecimal("300"),
                targetBalance = BigDecimal("320"),
                autoUpgradeEnabled = false,
                trustFundLevel = TrustFundLevel.VIP1,
            )
        )
        assertEquals(monday, result.reachedDate)
        assertEquals(1, result.daysCount)
        assertEquals(2, result.totalSignals)
        assertTrue(result.reachedAfterTrustFund)
        assertMoney("40.5", result.totalIncome)
        assertMoney("340.5", result.reachedBalance)
        assertMoney("35.7", result.days.single().trustFundIncome)
    }

    @Test
    fun targetBalanceReachedBySignalDoesNotPreCreditFund() {
        val result = TargetBalanceCalculator.calculate(
            TargetBalanceInput(
                startDate = monday,
                startingLevel = ParticipantLevel.C1,
                startingBalance = BigDecimal("300"),
                targetBalance = BigDecimal("302"),
                autoUpgradeEnabled = false,
                trustFundLevel = TrustFundLevel.VIP4,
            )
        )
        assertFalse(result.reachedAfterTrustFund)
        assertEquals(1, result.reachedAfterSignal)
        assertEquals(1, result.totalSignals)
        assertMoney("302.4", result.reachedBalance)
        assertMoney("2.4", result.totalIncome)
        assertMoney("0", result.days.single().trustFundIncome)
    }

    @Test
    fun targetNetProfitReachedByFundAfterSignals() {
        val result = TargetNetProfitCalculator.calculate(
            TargetNetProfitInput(
                startDate = monday,
                startingLevel = ParticipantLevel.C1,
                startingBalance = BigDecimal("300"),
                targetNetProfit = BigDecimal("20"),
                autoUpgradeEnabled = false,
                trustFundLevel = TrustFundLevel.VIP1,
            )
        )
        assertTrue(result.reachedAfterTrustFund)
        assertEquals(2, result.totalSignals)
        assertMoney("340.5", result.reachedBalance)
        assertMoney("40.5", result.totalIncome)
        assertMoney("12.150", result.withholding)
        assertMoney("28.350", result.netProfit)
    }

    @Test
    fun targetNetProfitReachedBySignalDoesNotPreCreditFund() {
        val result = TargetNetProfitCalculator.calculate(
            TargetNetProfitInput(
                startDate = monday,
                startingLevel = ParticipantLevel.C1,
                startingBalance = BigDecimal("300"),
                targetNetProfit = BigDecimal("1.5"),
                autoUpgradeEnabled = false,
                trustFundLevel = TrustFundLevel.VIP1,
            )
        )
        assertFalse(result.reachedAfterTrustFund)
        assertEquals(1, result.reachedAfterSignal)
        assertMoney("302.4", result.reachedBalance)
        assertMoney("2.4", result.totalIncome)
        assertMoney("0", result.days.single().trustFundIncome)
    }

    @Test
    fun defaultNoFundPreservesExistingFridaySignalRules() {
        val result = DailyCalculationEngine.calculate(
            DailyCalculationInput(
                level = ParticipantLevel.C6,
                dayOfWeek = DayOfWeek.FRIDAY,
                previousBalance = BigDecimal("10000"),
                x = 3,
                isVip = true,
                l1Count = 10,
            )
        )
        assertEquals(2, result.signalCount)
        assertMoney("160", result.dailyIncome)
        assertMoney("0", result.trustFundIncome)
        assertMoney("10160", result.balanceAfter)
    }
    @Test
    fun switchDisablesFundWithoutForgettingTheSelectedLevel() {
        TrustFundLevel.entries.filter { it != TrustFundLevel.NONE }.forEach { level ->
            assertEquals(TrustFundLevel.NONE, level.whenEnabled(false))
            assertEquals(level, level.whenEnabled(true))
            val off = DailyCalculationEngine.calculate(
                DailyCalculationInput(
                    level = ParticipantLevel.C1,
                    dayOfWeek = DayOfWeek.MONDAY,
                    previousBalance = BigDecimal("300"),
                    trustFundLevel = level.whenEnabled(false),
                )
            )
            assertMoney("0", off.trustFundIncome)
            assertMoney("304.8", off.balanceAfter)
        }
    }

    @Test
    fun vip5IncomeTriggersNextDayUpgradeAndCommission() {
        val result = ProfitSimulationEngine.simulate(
            ProfitSimulationInput(
                startDate = monday,
                numberOfDays = 1,
                startingLevel = ParticipantLevel.C1,
                startingBalance = BigDecimal("300"),
                trustFundLevel = TrustFundLevel.VIP5,
            )
        )
        assertMoney("1060", result.days.single().trustFundIncome)
        assertMoney("1364.8", result.expectedBalance)
        assertEquals(ParticipantLevel.C2, result.finalLevel)
        assertMoney("700", result.currentDeposit)
        assertMoney("664.8", result.grossProfit)
        assertMoney("199.440", result.withholding)
        assertMoney("465.360", result.netProfit)
    }

    @Test
    fun vip6FundIncomeIsIncludedInTargetBalanceAndNetProfit() {
        val targetBalance = TargetBalanceCalculator.calculate(
            TargetBalanceInput(
                startDate = monday,
                startingLevel = ParticipantLevel.C1,
                startingBalance = BigDecimal("300"),
                targetBalance = BigDecimal("1000"),
                autoUpgradeEnabled = false,
                trustFundLevel = TrustFundLevel.VIP6,
            )
        )
        assertTrue(targetBalance.reachedAfterTrustFund)
        assertEquals(2, targetBalance.totalSignals)
        assertMoney("1900", targetBalance.days.single().trustFundIncome)
        assertMoney("2204.8", targetBalance.reachedBalance)
        assertMoney("1904.8", targetBalance.totalIncome)

        val targetNet = TargetNetProfitCalculator.calculate(
            TargetNetProfitInput(
                startDate = monday,
                startingLevel = ParticipantLevel.C1,
                startingBalance = BigDecimal("300"),
                targetNetProfit = BigDecimal("1000"),
                autoUpgradeEnabled = false,
                trustFundLevel = TrustFundLevel.VIP6,
            )
        )
        assertTrue(targetNet.reachedAfterTrustFund)
        assertMoney("2204.8", targetNet.reachedBalance)
        assertMoney("1904.8", targetNet.totalIncome)
        assertMoney("1333.360", targetNet.netProfit)
    }

    @Test
    fun disabledVip6GivesSameFixedDateForecastAsNoFund() {
        val withoutFund = ProfitSimulationEngine.simulate(
            ProfitSimulationInput(monday, 3, ParticipantLevel.C1, BigDecimal("300"), autoUpgradeEnabled = false)
        )
        val disabled = ProfitSimulationEngine.simulate(
            ProfitSimulationInput(
                monday, 3, ParticipantLevel.C1, BigDecimal("300"),
                autoUpgradeEnabled = false,
                trustFundLevel = TrustFundLevel.VIP6.whenEnabled(false),
            )
        )
        assertMoney(withoutFund.expectedBalance.toPlainString(), disabled.expectedBalance)
        assertMoney("0", disabled.days.fold(BigDecimal.ZERO) { acc, day -> acc.add(day.trustFundIncome) })
    }

}
