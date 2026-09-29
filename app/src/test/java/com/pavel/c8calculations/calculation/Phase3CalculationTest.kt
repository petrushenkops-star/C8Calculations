package com.pavel.c8calculations.calculation

import com.pavel.c8calculations.model.DailyCalculationInput
import com.pavel.c8calculations.model.ParticipantLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.math.BigDecimal
import java.time.DayOfWeek

class Phase3CalculationTest {

    @Test
    fun regularDaysUseBasePlusXPlusVip() {
        assertEquals(2, SignalRuleEngine.signalCount(ParticipantLevel.C1, DayOfWeek.MONDAY))
        assertEquals(4, SignalRuleEngine.signalCount(ParticipantLevel.C2, DayOfWeek.SUNDAY, x = 1, isVip = true))
        assertEquals(5, SignalRuleEngine.signalCount(ParticipantLevel.C3, DayOfWeek.THURSDAY, x = 1, isVip = true))
        assertEquals(6, SignalRuleEngine.signalCount(ParticipantLevel.C6, DayOfWeek.WEDNESDAY, x = 1, isVip = true))
    }

    @Test
    fun fridayAndSaturdayUseOnlyOnePlusZ() {
        assertEquals(1, SignalRuleEngine.signalCount(ParticipantLevel.C6, DayOfWeek.FRIDAY, x = 99, isVip = true, l1Count = 9))
        assertEquals(2, SignalRuleEngine.signalCount(ParticipantLevel.C1, DayOfWeek.SATURDAY, x = 99, isVip = true, l1Count = 10))
    }

    @Test
    fun dailyCalculationAddsFullSignalIncomeWithoutWithholding() {
        val result = DailyCalculationEngine.calculate(
            DailyCalculationInput(
                level = ParticipantLevel.C4,
                dayOfWeek = DayOfWeek.MONDAY,
                previousBalance = BigDecimal("3000"),
                x = 1,
                isVip = true,
            )
        )

        assertEquals(5, result.signalCount)
        assertEquals(0, BigDecimal("24").compareTo(result.incomePerSignal))
        assertEquals(0, BigDecimal("120").compareTo(result.dailyIncome))
        assertEquals(0, BigDecimal("3120").compareTo(result.balanceAfter))
        assertEquals(ParticipantLevel.C4, result.level)
    }

    @Test
    fun invalidManualValuesAreRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            SignalRuleEngine.signalCount(ParticipantLevel.C1, DayOfWeek.MONDAY, x = -1)
        }
        assertThrows(IllegalArgumentException::class.java) {
            SignalRuleEngine.signalCount(ParticipantLevel.C1, DayOfWeek.MONDAY, l1Count = -1)
        }
    }
}
