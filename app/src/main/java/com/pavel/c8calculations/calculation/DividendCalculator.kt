package com.pavel.c8calculations.calculation

import java.math.BigDecimal

data class DividendInput(
    val days: Int,
    val c1: Int = 0,
    val c2: Int = 0,
    val c3: Int = 0,
    val c4: Int = 0,
    val c5: Int = 0,
    val c6: Int = 0,
)

data class DividendLevelResult(
    val level: String,
    val participants: Int,
    val baseAmount: BigDecimal,
    val amount: BigDecimal,
)

data class DividendResult(
    val days: Int,
    val levels: List<DividendLevelResult>,
    val total: BigDecimal,
)

object DividendCalculator {
    private val rate = BigDecimal("0.02")
    private val baseAmounts = linkedMapOf(
        "C2" to BigDecimal("11.2"),
        "C3" to BigDecimal("24"),
        "C4" to BigDecimal("48"),
        "C5" to BigDecimal("96"),
        "C6" to BigDecimal("160"),
    )

    fun calculate(input: DividendInput): DividendResult {
        require(input.days > 0) { "Количество дней должно быть больше 0" }
        val counts = listOf(input.c1, input.c2, input.c3, input.c4, input.c5, input.c6)
        require(counts.all { it >= 0 }) { "Количество участников не может быть отрицательным" }

        val participantCounts = mapOf(
            "C2" to input.c2,
            "C3" to input.c3,
            "C4" to input.c4,
            "C5" to input.c5,
            "C6" to input.c6,
        )
        val days = BigDecimal(input.days)
        val levels = baseAmounts.map { (level, baseAmount) ->
            val participants = participantCounts.getValue(level)
            val amount = baseAmount.multiply(rate).multiply(days).multiply(BigDecimal(participants))
            DividendLevelResult(level, participants, baseAmount, amount)
        }
        return DividendResult(input.days, levels, levels.fold(BigDecimal.ZERO) { sum, item -> sum + item.amount })
    }
}
