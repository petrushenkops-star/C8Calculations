package com.pavel.c8calculations.recognition

import kotlin.math.abs
import kotlin.math.roundToInt

/** Coordinates are PDF points, not image pixels. */
data class PdfLevelLabel(val level: Int, val x: Float, val y: Float, val height: Float)

object PdfTeamParser {
    val levelPattern = Regex("(?iu)(?<![\\p{L}\\p{N}])[CС]\\s*([0-6])(?![\\p{L}\\p{N}])")

    fun parse(raw: List<PdfLevelLabel>): TeamRecognitionResult? {
        val labels = mutableListOf<PdfLevelLabel>()
        raw.forEach { label ->
            // Some exporters paint the same text twice to simulate bold text.
            if (label.level in 0..6 && labels.none {
                abs(it.x - label.x) < 1f && abs(it.y - label.y) < 1f
            }) labels += label
        }
        if (labels.isEmpty()) return null

        val sorted = labels.sortedWith(compareBy<PdfLevelLabel> { it.x }.thenBy { it.y })
        val columns = groupIntoColumns(sorted)
        val leaderColumn = columns.firstOrNull().orEmpty()
        val leader = leaderColumn.singleOrNull()

        // L1 is the first column to the right of the leader, then L2 and L3.
        // C0 and C1 stay in column geometry, but neither belongs to the team total.
        // Keeping them in geometry prevents a real L4 from shifting into L3.
        val firstThreeLevels = if (leader != null) columns.drop(1).take(3).flatten() else emptyList()
        val counts = IntArray(7)
        firstThreeLevels.forEach { label ->
            if (label.level in 1..6) counts[label.level]++
        }

        return TeamRecognitionResult(counts, labels.size, leader != null, labels.map {
            TeamDetectedCard(it.level, it.x.roundToInt(), it.y.roundToInt(), it === leader)
        })
    }

    private fun groupIntoColumns(labels: List<PdfLevelLabel>): List<List<PdfLevelLabel>> {
        if (labels.isEmpty()) return emptyList()
        val typicalHeight = labels.map { it.height }.filter { it > 0f }.average().toFloat().takeIf { it.isFinite() } ?: 8f
        val tolerance = (typicalHeight * 1.5f).coerceAtLeast(2f)
        val columns = mutableListOf<MutableList<PdfLevelLabel>>()
        labels.forEach { label ->
            val current = columns.lastOrNull()
            val currentX = current?.map { it.x }?.average()?.toFloat()
            if (current == null || currentX == null || abs(label.x - currentX) > tolerance) {
                columns += mutableListOf(label)
            } else {
                current += label
            }
        }
        return columns
    }
}
