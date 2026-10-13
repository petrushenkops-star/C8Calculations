package com.pavel.c8calculations.ui.dividend

import android.content.Context

/** Private app-local preferences: no server, account or additional Android permissions. */
object DividendHistoryStorage {
    private const val FILE_NAME = "dividend_history"
    private const val ENTRIES_KEY = "entries"

    fun load(context: Context): List<DividendHistoryEntry> =
        DividendHistoryCodec.decode(
            context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
                .getString(ENTRIES_KEY, "").orEmpty()
        )

    fun add(context: Context, input: com.pavel.c8calculations.calculation.DividendInput): List<DividendHistoryEntry> {
        val entry = DividendHistoryEntry(System.currentTimeMillis(), input)
        val updated = (listOf(entry) + load(context)).take(DividendHistoryCodec.MAX_ENTRIES)
        context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE).edit()
            .putString(ENTRIES_KEY, DividendHistoryCodec.encode(updated)).apply()
        return updated
    }

    fun clear(context: Context) {
        context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE).edit()
            .remove(ENTRIES_KEY).apply()
    }
}
