package com.pavel.c8calculations.calculation

import com.pavel.c8calculations.model.ParticipantLevel
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class LevelConfigurationTest {

    @Test
    fun allLevelsHaveApprovedConfiguration() {
        val expected = listOf(
            Triple(ParticipantLevel.C1, "300", "2.4"),
            Triple(ParticipantLevel.C2, "700", "5.6"),
            Triple(ParticipantLevel.C3, "1500", "12"),
            Triple(ParticipantLevel.C4, "3000", "24"),
            Triple(ParticipantLevel.C5, "6000", "48"),
            Triple(ParticipantLevel.C6, "10000", "80"),
        )

        assertEquals(6, LevelConfiguration.all().size)
        expected.forEach { (level, deposit, income) ->
            val config = LevelConfiguration.forLevel(level)
            assertEquals(0, BigDecimal(deposit).compareTo(config.deposit))
            assertEquals(0, BigDecimal(income).compareTo(config.incomePerSignal))
        }
    }

    @Test
    fun baseSignalCountsAreCentralizedByLevel() {
        val expected = mapOf(
            ParticipantLevel.C1 to 2,
            ParticipantLevel.C2 to 2,
            ParticipantLevel.C3 to 3,
            ParticipantLevel.C4 to 3,
            ParticipantLevel.C5 to 4,
            ParticipantLevel.C6 to 4,
        )

        expected.forEach { (level, baseSignals) ->
            assertEquals(baseSignals, LevelConfiguration.forLevel(level).baseSignalCount)
        }
    }

    @Test
    fun allReturnsLevelsInNaturalOrder() {
        assertEquals(ParticipantLevel.entries, LevelConfiguration.all().map { it.level })
    }
}
