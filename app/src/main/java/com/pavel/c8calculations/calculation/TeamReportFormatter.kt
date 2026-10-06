package com.pavel.c8calculations.calculation

data class TeamReportData(
    val reportDate: String,
    val leaderName: String,
    val leaderUid: String,
    val meetings: String,
    val c1: Int,
    val c2: Int,
    val c3: Int,
    val c4: Int,
    val c5: Int,
    val c6: Int,
    val sergeant: String,
    val corporal: String,
    val directParticipants: Int,
    val officialParticipants: Int,
    val corporate: String,
    val teamProblems: String,
    val tenDayPlan: String,
)

object TeamReportFormatter {
    fun officialParticipants(c2: Int, c3: Int, c4: Int, c5: Int, c6: Int): Int =
        c2 + c3 + c4 + c5 + c6

    fun format(data: TeamReportData): String = buildString {
        appendLine(data.reportDate)
        appendLine(data.leaderName.ifBlank { "—" })
        appendLine("UID:${data.leaderUid.ifBlank { "—" }}")
        appendLine("1. Совещ.- ${data.meetings.ifBlank { "0" }}")
        appendLine("2. C1 - ${data.c1}")
        appendLine("3. C2 - ${data.c2}")
        appendLine("4. C3 - ${data.c3}")
        appendLine("5. C4 - ${data.c4}")
        appendLine("6. C5 - ${data.c5}")
        appendLine("7. C6 - ${data.c6}")
        appendLine("8. Сержант- ${data.sergeant.ifBlank { "0" }}")
        appendLine("9. Капрал- ${data.corporal.ifBlank { "0" }}")
        appendLine("10. Прямые - ${data.directParticipants}")
        appendLine("11. Всего оф. уч.- ${data.officialParticipants}")
        appendLine("12. Корпоратив - ${data.corporate.ifBlank { "0" }}")
        appendLine("13. Проблемы команды:")
        if (data.teamProblems.isNotBlank()) appendLine(data.teamProblems.trim())
        appendLine("14. План на ближайшие 10 дней:")
        if (data.tenDayPlan.isNotBlank()) append(data.tenDayPlan.trim())
    }.trimEnd()
}
