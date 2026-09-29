package com.pavel.c8calculations.calculation

import com.pavel.c8calculations.model.ParticipantLevel
import java.math.BigDecimal

object LevelUpgradeEngine {
    fun resolveLevel(
        currentLevel: ParticipantLevel,
        balanceAfterDay: BigDecimal,
        autoUpgradeEnabled: Boolean,
    ): ParticipantLevel {
        if (!autoUpgradeEnabled) return currentLevel

        return LevelConfiguration.all()
            .filter { balanceAfterDay >= it.deposit }
            .maxByOrNull { it.deposit }
            ?.level
            ?.takeIf { it.ordinal > currentLevel.ordinal }
            ?: currentLevel
    }
}
