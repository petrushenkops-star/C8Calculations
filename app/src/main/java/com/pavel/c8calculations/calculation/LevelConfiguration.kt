package com.pavel.c8calculations.calculation

import com.pavel.c8calculations.model.LevelConfig
import com.pavel.c8calculations.model.ParticipantLevel
import java.math.BigDecimal

object LevelConfiguration {

    private val configs: Map<ParticipantLevel, LevelConfig> = listOf(
        LevelConfig(ParticipantLevel.C1, "300".toBigDecimal(), "2.4".toBigDecimal(), 2),
        LevelConfig(ParticipantLevel.C2, "700".toBigDecimal(), "5.6".toBigDecimal(), 2),
        LevelConfig(ParticipantLevel.C3, "1500".toBigDecimal(), "12".toBigDecimal(), 3),
        LevelConfig(ParticipantLevel.C4, "3000".toBigDecimal(), "24".toBigDecimal(), 3),
        LevelConfig(ParticipantLevel.C5, "6000".toBigDecimal(), "48".toBigDecimal(), 4),
        LevelConfig(ParticipantLevel.C6, "10000".toBigDecimal(), "80".toBigDecimal(), 4),
    ).associateBy(LevelConfig::level)

    fun forLevel(level: ParticipantLevel): LevelConfig =
        requireNotNull(configs[level]) { "Configuration is missing for $level" }

    fun all(): List<LevelConfig> = ParticipantLevel.entries.map(::forLevel)
}
