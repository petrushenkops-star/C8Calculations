package com.pavel.c8calculations.ui.dividend

import com.pavel.c8calculations.calculation.DividendCalculator
import com.pavel.c8calculations.calculation.DividendInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class DividendHistoryCodecTest {
    @Test
    fun roundTripPreservesCountsAndDatesAndOrder() {
        val first = DividendHistoryEntry(
            1791889200000L, DividendInput(days = 10, c1 = 1, c2 = 0, c3 = 6, c4 = 10, c5 = 6, c6 = 6)
        )
        val second = DividendHistoryEntry(
            1791802800000L, DividendInput(days = 3, c2 = 2, c6 = 1)
        )
        val restored = DividendHistoryCodec.decode(DividendHistoryCodec.encode(listOf(first, second)))
        assertEquals(listOf(first, second), restored)
        assertEquals(0, BigDecimal("432").compareTo(DividendCalculator.calculate(restored[0].input).total))
    }

    @Test
    fun invalidEntriesAreDiscardedWithoutLosingValidOnes() {
        val valid = DividendHistoryCodec.encode(
            listOf(DividendHistoryEntry(100L, DividendInput(days = 5, c2 = 2)))
        )
        val corrupted = listOf(
            "v1|a|10|0|1|2|3|4|5",
            "v1|100|0|0|1|2|3|4|5",
            "v1|100|10|0|1|-2|3|4|5",
            "v1|100|10|0|1|2|3|4|999999999999",
            "v2|100|10|0|1|2|3|4|5",
            "v1|100|10|0|1",
        ).joinToString("\n")
        val restored = DividendHistoryCodec.decode("$corrupted\n$valid")
        assertEquals(1, restored.size)
        assertEquals(2, restored.single().input.c2)
        assertTrue(DividendHistoryCodec.decode("").isEmpty())
    }

    @Test
    fun capsHistoryAtFiftyMostRecentEntries() {
        val saved = (0 until 70).map { timestamp ->
            DividendHistoryEntry(timestamp.toLong(), DividendInput(days = timestamp + 1, c4 = 1))
        }
        val restored = DividendHistoryCodec.decode(DividendHistoryCodec.encode(saved))
        assertEquals(DividendHistoryCodec.MAX_ENTRIES, restored.size)
        assertEquals(saved.take(50), restored)
    }

    @Test
    fun olderSnapshotIsIndependentOfCurrentTeamCounts() {
        val snapshot = DividendHistoryEntry(200L, DividendInput(days = 10, c4 = 10, c5 = 6))
        val restored = DividendHistoryCodec.decode(DividendHistoryCodec.encode(listOf(snapshot))).single()
        val oldTotal = DividendCalculator.calculate(restored.input).total
        val newTotal = DividendCalculator.calculate(DividendInput(days = 10, c4 = 3, c5 = 1)).total
        assertTrue(oldTotal > newTotal)
        assertEquals(DividendInput(days = 10, c4 = 10, c5 = 6), restored.input)
    }
}
