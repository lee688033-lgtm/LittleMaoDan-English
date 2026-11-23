package com.example.cet6vocabulary.presentation.model

import com.example.cet6vocabulary.data.model.Exam
import com.example.cet6vocabulary.data.model.ExamAppConfig
import com.example.cet6vocabulary.data.model.ExamQuestion
import com.example.cet6vocabulary.data.model.ExamSection
import com.example.cet6vocabulary.data.model.ExamSectionData

/**
 * One graded reading question.
 *
 * [correctAnswer] is null when neither the question nor the paper's answer key carries an answer;
 * such a question is shown in review but excluded from the score so a data gap cannot silently
 * lower the percentage.
 */
data class ReadingQuestionOutcome(
    val number: Int,
    val questionId: String,
    val subsectionKey: String?,
    val subsectionLabel: String,
    val question: String?,
    val selectedAnswer: String?,
    val correctAnswer: String?,
    val explanation: String?,
    val points: Int
) {
    val answered: Boolean get() = selectedAnswer != null
    val gradable: Boolean get() = correctAnswer != null
    val correct: Boolean get() = gradable && selectedAnswer == correctAnswer
}

data class ReadingScore(
    val outcomes: List<ReadingQuestionOutcome>
) {
    val total: Int get() = outcomes.size
    val gradableCount: Int get() = outcomes.count { it.gradable }
    val answered: Int get() = outcomes.count { it.answered }
    val unanswered: Int get() = outcomes.count { !it.answered }
    val correct: Int get() = outcomes.count { it.correct }
    val incorrect: Int get() = outcomes.count { it.answered && it.gradable && !it.correct }
    val earnedPoints: Int get() = outcomes.filter { it.correct }.sumOf { it.points }
    val maxPoints: Int get() = outcomes.filter { it.gradable }.sumOf { it.points }

    /** Percentage over every gradable question, unanswered ones included as wrong. */
    val correctRate: Int? get() = if (gradableCount == 0) null else correct * 100 / gradableCount
}

/**
 * Grades a whole reading section in data order. Pure: the same answers always produce the same
 * score, so the result screen can rebuild it after rotation or process death without persisting a
 * second copy of the result.
 */
fun scoreReading(exam: Exam, answers: Map<String, String>): ReadingScore {
    val section = exam.sections.firstOrNull { it.section == ExamSection.READING }
        ?: return ReadingScore(emptyList())
    val answerKey = exam.answerKey[ExamSection.READING].orEmpty()
    val outcomes = section.questions.map { question ->
        ReadingQuestionOutcome(
            number = question.number,
            questionId = question.questionId,
            subsectionKey = question.subsection,
            subsectionLabel = subsectionLabel(section, question),
            question = question.question,
            selectedAnswer = answers[question.questionId],
            correctAnswer = resolveCorrectAnswer(question, answerKey),
            explanation = question.explanation?.takeIf { it.isNotBlank() },
            points = pointsFor(question, section, exam.appConfig)
        )
    }
    return ReadingScore(outcomes)
}

fun resolveCorrectAnswer(exam: Exam, question: ExamQuestion): String? =
    resolveCorrectAnswer(question, exam.answerKey[ExamSection.READING].orEmpty())

private fun resolveCorrectAnswer(question: ExamQuestion, answerKey: Map<Int, String>): String? =
    question.correctAnswer?.takeIf { it.isNotBlank() } ?: answerKey[question.number]

private fun pointsFor(
    question: ExamQuestion,
    section: ExamSectionData,
    config: ExamAppConfig?
): Int {
    val subsectionType = section.subsections.firstOrNull { it.key == question.subsection }?.type
    return when (question.type ?: subsectionType) {
        "word_bank" -> config?.wordBankScoring ?: 1
        "paragraph_matching" -> config?.paragraphMatchingScoring ?: 1
        "single_choice" -> config?.singleChoiceScoring ?: 1
        else -> 1
    }
}

private fun subsectionLabel(section: ExamSectionData, question: ExamQuestion): String {
    val subsection = section.subsections.firstOrNull { it.key == question.subsection }
    return subsection?.title?.takeIf { it.isNotBlank() } ?: "阅读"
}
