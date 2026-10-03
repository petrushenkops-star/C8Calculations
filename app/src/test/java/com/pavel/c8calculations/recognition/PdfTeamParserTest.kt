package com.pavel.c8calculations.recognition

import org.junit.Assert.*
import org.junit.Test

class PdfTeamParserTest {
    @Test fun `accepts Latin and Cyrillic levels with spaces including C0`() {
        val levels = PdfTeamParser.levelPattern.findAll("C0 C1 С2 c 3 с 4 C5 С6").map { it.groupValues[1].toInt() }.toList()
        assertEquals((0..6).toList(), levels)
        assertFalse(PdfTeamParser.levelPattern.containsMatchIn("C10 ABC4 C4name 123456"))
    }

    @Test fun `leftmost leader is excluded regardless of reading order`() {
        val result = PdfTeamParser.parse(listOf(
            PdfLevelLabel(3, 200f, 20f, 10f),
            PdfLevelLabel(6, 40f, 300f, 10f),
            PdfLevelLabel(1, 400f, 50f, 10f),
            PdfLevelLabel(4, 200f, 80f, 10f),
        ))!!
        assertTrue(result.leaderExcluded)
        assertEquals(0, result.counts[6])
        assertEquals(1, result.counts[1])
        assertEquals(4, result.detectedCards)
    }

    @Test fun `does not auto count when leader cannot be identified`() {
        val result = PdfTeamParser.parse(listOf(
            PdfLevelLabel(4, 20f, 20f, 10f), PdfLevelLabel(6, 20f, 50f, 10f),
        ))!!
        assertFalse(result.leaderExcluded)
        assertEquals(0, result.counts.sum())
    }

    @Test fun `only L1 L2 L3 are included and L4 is excluded`() {
        val result = PdfTeamParser.parse(listOf(
            PdfLevelLabel(6, 10f, 20f, 8f),
            PdfLevelLabel(2, 100f, 20f, 8f),
            PdfLevelLabel(3, 200f, 20f, 8f),
            PdfLevelLabel(4, 300f, 20f, 8f),
            PdfLevelLabel(5, 400f, 20f, 8f),
        ))!!
        assertTrue(result.leaderExcluded)
        assertEquals(1, result.counts[2])
        assertEquals(1, result.counts[3])
        assertEquals(1, result.counts[4])
        assertEquals(0, result.counts[5])
    }

    @Test fun `C0 and C1 preserve column depth but do not enter team total`() {
        val result = PdfTeamParser.parse(listOf(
            PdfLevelLabel(6, 10f, 20f, 8f),
            PdfLevelLabel(0, 100f, 20f, 8f),
            PdfLevelLabel(1, 200f, 20f, 8f),
            PdfLevelLabel(4, 300f, 20f, 8f),
            PdfLevelLabel(5, 400f, 20f, 8f),
        ))!!
        assertTrue(result.leaderExcluded)
        assertEquals(1, result.counts[1])
        assertEquals(1, result.counts[4])
        assertEquals(0, result.counts[5])
        assertEquals(1, (2..6).sumOf { result.counts[it] })
    }

    @Test fun `participant card extracts level name uid and date`() {
        val labels = listOf(
            PdfLevelLabel(6, 10f, 20f, 8f),
            PdfLevelLabel(6, 100f, 20f, 8f),
            PdfLevelLabel(4, 100f, 70f, 8f),
        )
        val fragments = listOf(
            PdfTextFragment("C6", 100f, 20f, 8f),
            PdfTextFragment("Павел", 100f, 28f, 8f),
            PdfTextFragment("7226071297", 100f, 36f, 8f),
            PdfTextFragment("31.10.25", 100f, 44f, 8f),
        )

        val result = PdfTeamParser.parse(labels, fragments)!!
        val participant = result.cards.first { it.depth == 1 && it.y == 20 }

        assertEquals(6, participant.level)
        assertEquals("Павел", participant.name)
        assertEquals("7226071297", participant.uid)
        assertEquals("31.10.25", participant.date)
    }

    @Test fun `overprinted labels count once and nearby distinct cards survive`() {
        val result = PdfTeamParser.parse(listOf(
            PdfLevelLabel(6, 5f, 30f, 6f),
            PdfLevelLabel(4, 70f, 10f, 6f), PdfLevelLabel(4, 70.1f, 10.1f, 6f),
            PdfLevelLabel(4, 70f, 20f, 6f),
        ))!!
        assertEquals(2, result.counts[4])
        assertEquals(3, result.detectedCards)
    }

    @Test fun `empty text requests OCR fallback`() {
        assertNull(PdfTeamParser.parse(emptyList()))
    }

    @Test fun `sample PDF geometry yields 28 eligible participants`() {
        val labels = javaClass.getResourceAsStream("/team-pdf-labels.tsv")!!.bufferedReader().useLines { lines ->
            lines.map { line ->
                val values = line.split('\t')
                PdfLevelLabel(values[0].toInt(), values[1].toFloat(), values[2].toFloat(), values[3].toFloat())
            }.toList()
        }
        val result = PdfTeamParser.parse(labels)!!
        assertArrayEquals(intArrayOf(0, 1, 0, 6, 10, 6, 6), result.counts)
        assertEquals(30, result.detectedCards)
        assertTrue(result.leaderExcluded)
        assertEquals(28, (2..6).sumOf { result.counts[it] })
    }
}
