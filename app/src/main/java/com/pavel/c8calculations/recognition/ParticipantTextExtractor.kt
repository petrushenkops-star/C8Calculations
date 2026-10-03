package com.pavel.c8calculations.recognition

internal data class ParticipantFields(
    val name: String = "",
    val uid: String = "",
    val date: String = "",
)

internal object ParticipantTextExtractor {
    private val uidPattern = Regex("(?<!\\d)\\d{6,15}(?!\\d)")
    private val datePattern = Regex("(?<!\\d)\\d{1,2}[./-]\\d{1,2}[./-]\\d{2,4}(?!\\d)")
    private val levelPattern = Regex("(?iu)(?<![\\p{L}\\p{N}])[CС]\\s*[0-6](?![\\p{L}\\p{N}])")
    private val nonNamePattern = Regex("(?iu)^(uid|id|дата|date)$")

    fun extract(fragments: List<String>): ParticipantFields {
        if (fragments.isEmpty()) return ParticipantFields()

        val uid = mostFrequent(
            fragments.flatMap { uidPattern.findAll(it).map { match -> match.value }.toList() }
        )
        val date = mostFrequent(
            fragments.flatMap { datePattern.findAll(it).map { match -> match.value }.toList() }
        )

        val nameCandidates = fragments.mapNotNull { fragment ->
            var candidate = fragment
            candidate = candidate.replace(levelPattern, " ")
            candidate = candidate.replace(uidPattern, " ")
            candidate = candidate.replace(datePattern, " ")
            candidate = candidate
                .replace('|', ' ')
                .replace('•', ' ')
                .replace(Regex("\\s+"), " ")
                .trim(' ', '-', ':', ';', ',', '.')

            if (candidate.isBlank() || !candidate.any(Char::isLetter)) return@mapNotNull null
            if (nonNamePattern.matches(candidate)) return@mapNotNull null
            if (candidate.length > 80) return@mapNotNull null
            candidate
        }

        return ParticipantFields(
            name = mostFrequent(nameCandidates),
            uid = uid,
            date = date,
        )
    }

    private fun mostFrequent(values: List<String>): String {
        if (values.isEmpty()) return ""
        return values
            .groupBy { it.trim().lowercase() }
            .values
            .maxWithOrNull(compareBy<List<String>> { it.size }.thenBy { it.firstOrNull()?.length ?: 0 })
            ?.firstOrNull()
            .orEmpty()
            .trim()
    }
}
