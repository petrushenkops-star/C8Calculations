package com.pavel.c8calculations.recognition

import kotlin.math.abs
import kotlin.math.roundToInt

/** Coordinates are PDF points, not image pixels. */
data class PdfLevelLabel(val level: Int, val x: Float, val y: Float, val height: Float)
data class PdfTextFragment(val text: String, val x: Float, val y: Float, val height: Float)

object PdfTeamParser {
    val levelPattern = Regex("(?iu)(?<![\\p{L}\\p{N}])[CС]\\s*([0-6])(?![\\p{L}\\p{N}])")

    fun parse(raw: List<PdfLevelLabel>, textFragments: List<PdfTextFragment> = emptyList()): TeamRecognitionResult? {
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
        val firstThreeLevels = if (leader != null) columns.drop(1).take(3).flatten() else emptyList()
        val counts = IntArray(7)
        firstThreeLevels.forEach { label ->
            if (label.level in 1..6) counts[label.level]++
        }

        val cards = buildList {
            columns.forEachIndexed { columnIndex, column ->
                column.sortedBy { it.y }.forEach { label ->
                    val cardText = textForCard(label, column, columnIndex, columns, textFragments)
                    val fields = ParticipantTextExtractor.extract(cardText)
                    add(
                        TeamDetectedCard(
                            level = label.level,
                            x = label.x.roundToInt(),
                            y = label.y.roundToInt(),
                            excludedAsLeader = label === leader,
                            depth = if (leader != null) columnIndex else null,
                            name = fields.name,
                            uid = fields.uid,
                            date = fields.date,
                        )
                    )
                }
            }
        }

        return TeamRecognitionResult(counts, labels.size, leader != null, cards)
    }

    private fun textForCard(
        label: PdfLevelLabel,
        column: List<PdfLevelLabel>,
        columnIndex: Int,
        columns: List<List<PdfLevelLabel>>,
        fragments: List<PdfTextFragment>,
    ): List<String> {
        if (fragments.isEmpty()) return emptyList()
        val sorted = column.sortedBy { it.y }
        val index = sorted.indexOf(label)
        val gaps = sorted.zipWithNext { a, b -> b.y - a.y }.filter { it > 1f }
        val fallbackGap = gaps.sorted().let { values ->
            if (values.isEmpty()) 36f else values[values.size / 2]
        }
        val nextY = sorted.getOrNull(index + 1)?.y
        val top = label.y - (label.height * 1.5f).coerceAtLeast(3f)
        val bottom = (nextY?.minus(0.5f) ?: (label.y + fallbackGap)).coerceAtLeast(label.y + label.height * 2f)
        val nextColumnX = columns.getOrNull(columnIndex + 1)?.map { it.x }?.average()?.toFloat()
        val left = label.x - (label.height * 2f).coerceAtLeast(4f)
        val right = nextColumnX?.minus((label.height * 1.5f).coerceAtLeast(3f)) ?: (label.x + 170f)

        return fragments
            .filter { it.x in left..right && it.y in top..bottom }
            .sortedBy { it.y }
            .map { it.text }
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
