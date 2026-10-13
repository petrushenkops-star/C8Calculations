package com.pavel.c8calculations.calculation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.math.BigDecimal

class DividendCalculatorTest {
    @Test
    fun calculatesKnownExample() {
        val result = DividendCalculator.calculate(
            DividendInput(days = 10, c2 = 1, c3 = 2, c4 = 8, c5 = 1, c6 = 3)
        )
        assertEquals(BigDecimal("203.840"), result.total)
        assertEquals(BigDecimal("76.80"), result.levels.first { it.level == "C4" }.amount)
    }

    @Test
    fun currentTeamStructureHas28ParticipantsAndC1DoesNotAffectDividend() {
        val currentStructure = DividendInput(days = 10, c1 = 1, c2 = 0, c3 = 6, c4 = 10, c5 = 6, c6 = 6)
        val result = DividendCalculator.calculate(currentStructure)
        assertEquals(28, result.levels.sumOf { it.participants })
        assertEquals(0, BigDecimal("432").compareTo(result.total))

        val sameStructureWithoutC1 = DividendCalculator.calculate(currentStructure.copy(c1 = 0))
        assertEquals(result.total, sameStructureWithoutC1.total)
    }

    @Test
    fun c1DoesNotAffectDividend() {
        val withoutC1 = DividendCalculator.calculate(DividendInput(days = 10, c2 = 2))
        val withC1 = DividendCalculator.calculate(DividendInput(days = 10, c1 = 100, c2 = 2))
        assertEquals(withoutC1.total, withC1.total)
    }

    @Test
    fun rejectsInvalidInput() {
        assertThrows(IllegalArgumentException::class.java) {
            DividendCalculator.calculate(DividendInput(days = 0, c2 = 1))
        }
        assertThrows(IllegalArgumentException::class.java) {
            DividendCalculator.calculate(DividendInput(days = 10, c4 = -1))
        }
    }
    @Test
    fun emptyTeamHasZeroDividendAcrossAllLevels() {
        val result = DividendCalculator.calculate(DividendInput(days = 365, c1 = 40))
        assertEquals(0, BigDecimal.ZERO.compareTo(result.total))
        assertEquals(0, result.levels.sumOf { it.participants })
        assertEquals(5, result.levels.size)
        assertEquals(true, result.levels.all { it.amount.compareTo(BigDecimal.ZERO) == 0 })
    }

    @Test
    fun singleParticipantRateByLevelIsExact() {
        val expectedDaily = mapOf(
            "C2" to "0.224", "C3" to "0.48", "C4" to "0.96",
            "C5" to "1.92", "C6" to "3.20"
        )
        val result = DividendCalculator.calculate(
            DividendInput(days = 1, c2 = 1, c3 = 1, c4 = 1, c5 = 1, c6 = 1)
        )
        result.levels.forEach { level ->
            assertEquals(0, BigDecimal(expectedDaily.getValue(level.level)).compareTo(level.amount))
        }
        assertEquals(0, BigDecimal("6.784").compareTo(result.total))
    }

    @Test
    fun largeParticipantCountsUseExactDecimalArithmetic() {
        val result = DividendCalculator.calculate(
            DividendInput(days = 100, c2 = 100_000, c6 = 100_000)
        )
        assertEquals(0, BigDecimal("342400").compareTo(result.total))
    }

    @Test
    fun negativeC1AndC6AreRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            DividendCalculator.calculate(DividendInput(days = 5, c1 = -1))
        }
        assertThrows(IllegalArgumentException::class.java) {
            DividendCalculator.calculate(DividendInput(days = 5, c6 = -1))
        }
    }

}
