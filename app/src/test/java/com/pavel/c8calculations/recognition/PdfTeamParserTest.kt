package com.pavel.c8calculations.recognition

import org.junit.Assert.*
import org.junit.Test

class PdfTeamParserTest {
    @Test fun `accepts Latin and Cyrillic levels with spaces`() {
        val levels = PdfTeamParser.levelPattern.findAll("C1 С2 c 3 с 4 C5 С6").map { it.groupValues[1].toInt() }.toList()
        assertEquals((1..6).toList(), levels)
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

    @Test fun `does not exclude someone from a full left column`() {
        val result = PdfTeamParser.parse(listOf(
            PdfLevelLabel(4, 20f, 20f, 10f), PdfLevelLabel(6, 20f, 50f, 10f),
        ))!!
        assertFalse(result.leaderExcluded)
        assertEquals(2, result.counts.sum())
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
        // Anonymized level/coordinate fixture extracted from the supplied PDF.
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
