package com.pavel.c8calculations.model

import java.math.BigDecimal

data class LevelConfig(
    val level: ParticipantLevel,
    val deposit: BigDecimal,
    val incomePerSignal: BigDecimal,
    val baseSignalCount: Int,
    val fridaySignalCount: Int,
    val saturdaySignalCount: Int,
)
