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
}
