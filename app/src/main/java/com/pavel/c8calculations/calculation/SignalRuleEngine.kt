package com.pavel.c8calculations.calculation

import com.pavel.c8calculations.model.ParticipantLevel
import java.time.DayOfWeek

object SignalRuleEngine {
    fun signalCount(
        level: ParticipantLevel,
        dayOfWeek: DayOfWeek,
        x: Int = 0,
        isVip: Boolean = false,
        l1Count: Int = 0,
    ): Int {
        require(x >= 0) { "X must be non-negative" }
        require(l1Count >= 0) { "L1 count must be non-negative" }

        val config = LevelConfiguration.forLevel(level)
        return when (dayOfWeek) {
            DayOfWeek.FRIDAY -> config.fridaySignalCount + if (l1Count >= 10) 1 else 0
            DayOfWeek.SATURDAY -> config.saturdaySignalCount + if (l1Count >= 10) 1 else 0
            else -> config.baseSignalCount + x + if (isVip) 1 else 0
        }
    }
}
