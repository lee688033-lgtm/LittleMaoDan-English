package com.example.cet6vocabulary.presentation.model

import com.example.cet6vocabulary.data.model.ExamQuestion
import com.example.cet6vocabulary.data.model.ExamSectionData

/**
 * Answer-sheet state for one reading practice session.
 *
 * The sheet deliberately carries no correctness information: it only reports whether a question
 * has been answered, which one is on screen, and how the questions group into subsections. All
 * ranges and counts are derived from the parsed exam data, never hardcoded per section.
 */
enum class ReadingSheetStatus { UNANSWERED, ANSWERED, CURRENT }

data class ReadingSheetItem(
    val index: Int,
    val questionId: String,
    val number: Int,
    val answered: Boolean,
    val current: Boolean
) {
    // The current question is highlighted on top of its own answering state, so the visual status
    // is derived here while the counts stay based on [answered] alone.
    val status: ReadingSheetStatus
        get() = when {
            current -> ReadingSheetStatus.CURRENT
            answered -> ReadingSheetStatus.ANSWERED
            else -> ReadingSheetStatus.UNANSWERED
        }
}

data class ReadingSheetSection(
    val key: String,
    val label: String,
    val items: List<ReadingSheetItem>
) {
    val firstNumber: Int get() = items.firstOrNull()?.number ?: 0
    val lastNumber: Int get() = items.lastOrNull()?.number ?: 0
    val answeredCount: Int get() = items.count { it.answered }
}

data class ReadingSheet(
    val sections: List<ReadingSheetSection>
) {
    val items: List<ReadingSheetItem> get() = sections.flatMap { it.items }
    val total: Int get() = items.size
    val answered: Int get() = items.count { it.answered }
    val unanswered: Int get() = total - answered
}

/**
 * Builds the sheet for the whole reading part (one grid per subsection, in data order).
 *
 * [answers] maps questionId to the selected option, [currentIndex] is the question currently shown
 * by the practice screen. Both are plain in-memory values owned above the two screens so that
 * travelling to the sheet and back cannot recreate them.
 */
fun buildReadingSheet(
    section: ExamSectionData,
    answers: Map<String, String>,
    currentIndex: Int
): ReadingSheet {
    val indexById = section.questions.withIndex().associate { (index, question) -> question.questionId to index }
    val groups: List<Triple<String, String, List<ExamQuestion>>> =
        if (section.subsections.isNotEmpty()) {
            section.subsections.map { Triple(it.key, subsectionLabel(it.key, it.title), it.questions) }
        } else {
            listOf(Triple(section.type, section.title, section.questions))
        }
    val sheets = groups.mapNotNull { (key, label, questions) ->
        val items = questions.mapNotNull { question ->
            val index = indexById[question.questionId] ?: return@mapNotNull null
            ReadingSheetItem(
                index = index,
                questionId = question.questionId,
                number = question.number,
                answered = answers.containsKey(question.questionId),
                current = index == currentIndex
            )
        }
        if (items.isEmpty()) null else ReadingSheetSection(key = key, label = label, items = items)
    }
    return ReadingSheet(sheets)
}

private fun subsectionLabel(key: String, title: String): String =
    title.ifBlank { "Section " + key.removePrefix("section") }
