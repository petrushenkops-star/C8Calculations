package com.pavel.c8calculations.recognition

import kotlin.math.abs
import kotlin.math.roundToInt

/** Coordinates are PDF points, not image pixels. */
data class PdfLevelLabel(val level: Int, val x: Float, val y: Float, val height: Float)

object PdfTeamParser {
    val levelPattern = Regex("(?iu)(?<![\\p{L}\\p{N}])[CС]\\s*([1-6])(?![\\p{L}\\p{N}])")

    fun parse(raw: List<PdfLevelLabel>): TeamRecognitionResult? {
        val labels = mutableListOf<PdfLevelLabel>()
        raw.forEach { label ->
            // Some exporters paint the same text twice to simulate bold text.
            if (label.level in 1..6 && labels.none {
                abs(it.x - label.x) < 1f && abs(it.y - label.y) < 1f
            }) labels += label
        }
        if (labels.isEmpty()) return null
        val first = labels.minBy { it.x }
        val tolerance = (first.height * 0.5f).coerceAtLeast(1f)
        val leftColumn = labels.filter { abs(it.x - first.x) <= tolerance }
        val leader = leftColumn.singleOrNull()
        val counts = IntArray(7)
        labels.forEach { if (it !== leader) counts[it.level]++ }
        return TeamRecognitionResult(counts, labels.size, leader != null, labels.map {
            TeamDetectedCard(it.level, it.x.roundToInt(), it.y.roundToInt(), it === leader)
        })
    }
}
