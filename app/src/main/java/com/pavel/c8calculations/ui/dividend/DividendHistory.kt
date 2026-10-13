package com.pavel.c8calculations.ui.dividend

import com.pavel.c8calculations.calculation.DividendInput

/** An immutable snapshot of the counts actually used for a dividend calculation. */
data class DividendHistoryEntry(
    val createdAtMillis: Long,
    val input: DividendInput,
)

/**
 * Simple versioned, delimiter-safe numeric format for local-only calculation history.
 * Corrupted entries are ignored so a damaged preference cannot break the dividends screen.
 */
object DividendHistoryCodec {
    const val MAX_ENTRIES = 50
    private const val PREFIX = "v1"

    fun encode(entries: List<DividendHistoryEntry>): String =
        entries.take(MAX_ENTRIES).joinToString("\n") { entry ->
            val input = entry.input
            listOf(
                PREFIX, entry.createdAtMillis, input.days,
                input.c1, input.c2, input.c3, input.c4, input.c5, input.c6,
            ).joinToString("|")
        }

    fun decode(value: String): List<DividendHistoryEntry> =
        value.lineSequence().mapNotNull(::parseEntry).take(MAX_ENTRIES).toList()

    private fun parseEntry(line: String): DividendHistoryEntry? {
        val fields = line.split('|')
        if (fields.size != 9 || fields[0] != PREFIX) return null
        val timestamp = fields[1].toLongOrNull() ?: return null
        // Preserve readable date formatting on Android (up to year 9999).
        if (timestamp !in 0L..253402300799999L) return null
        val numbers = fields.drop(2).map { it.toIntOrNull() ?: return null }
        if (numbers[0] <= 0 || numbers.drop(1).any { it < 0 }) return null
        return DividendHistoryEntry(
            createdAtMillis = timestamp,
            input = DividendInput(
                days = numbers[0], c1 = numbers[1], c2 = numbers[2], c3 = numbers[3],
                c4 = numbers[4], c5 = numbers[5], c6 = numbers[6],
            ),
        )
    }
}
