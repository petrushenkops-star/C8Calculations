package com.pavel.c8calculations.calculation

import org.junit.Assert.assertEquals
import org.junit.Test

class TeamReportFormatterTest {
    @Test
    fun `official participants include only C2 to C6`() {
        assertEquals(
            28,
            TeamReportFormatter.officialParticipants(
                c2 = 0,
                c3 = 6,
                c4 = 10,
                c5 = 6,
                c6 = 6,
            ),
        )
    }

    @Test
    fun `formats report in approved order`() {
        val text = TeamReportFormatter.format(
            TeamReportData(
                reportDate = "30.09.2026",
                leaderName = "Павел",
                leaderUid = "7226071297",
                meetings = "2",
                c1 = 1,
                c2 = 0,
                c3 = 6,
                c4 = 10,
                c5 = 6,
                c6 = 6,
                sergeant = "2",
                corporal = "0",
                directParticipants = 14,
                officialParticipants = TeamReportFormatter.officialParticipants(0, 6, 10, 6, 6),
                corporate = "0",
                teamProblems = "",
                tenDayPlan = "Поиск новых участников. Обучение новичков.",
            )
        )

        assertEquals(
            """30.09.2026
Павел
UID:7226071297
1. Совещ.- 2
2. C1 - 1
3. C2 - 0
4. C3 - 6
5. C4 - 10
6. C5 - 6
7. C6 - 6
8. Сержант- 2
9. Капрал- 0
10. Прямые - 14
11. Всего оф. уч.- 28
12. Корпоратив - 0
13. Проблемы команды:
14. План на ближайшие 10 дней:
Поиск новых участников. Обучение новичков.""",
            text,
        )
    }
}
