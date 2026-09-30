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
        assertEquals(BigDecimal("1346.40"), result.total)
        assertEquals(BigDecimal("76.80"), result.levels.first { it.level == "C4" }.amount)
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
