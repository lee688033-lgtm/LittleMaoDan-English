package com.example.cet6vocabulary.data.model

enum class ExamSection {
    WRITING,
    LISTENING,
    READING,
    TRANSLATION
}

data class Exam(
    val schemaVersion: String,
    val examId: String,
    val title: String,
    val date: String,
    val setNumber: Int,
    val setName: String,
    val sections: List<ExamSectionData>,
    val answerKey: Map<ExamSection, Map<Int, String>>,
    val appConfig: ExamAppConfig?,
    val sourceVerification: ExamSourceVerification?
)

data class ExamSectionData(
    val section: ExamSection,
    val type: String,
    val title: String,
    val durationMinutes: Int,
    val questions: List<ExamQuestion>,
    val subsections: List<ExamSubsection> = emptyList(),
    val audioRef: String? = null
)

data class ExamSubsection(
    val key: String,
    val type: String,
    val title: String,
    val materials: ExamMaterials,
    val questions: List<ExamQuestion>
)

data class ExamMaterials(
    val passage: String? = null,
    val wordBank: Map<String, String> = emptyMap(),
    val passageTitle: String? = null,
    val paragraphs: Map<String, String> = emptyMap(),
    val passages: List<ExamPassage> = emptyList()
)

data class ExamPassage(
    val id: String,
    val title: String,
    val text: String
)

data class ExamQuestion(
    val questionId: String,
    val examId: String,
    val section: ExamSection,
    val subsection: String?,
    val number: Int,
    val type: String?,
    val question: String?,
    val options: Map<String, String>,
    val correctAnswer: String?,
    val explanation: String?,
    val prompt: String?,
    val instruction: String?,
    val minWords: Int?,
    val maxWords: Int?,
    val blank: Int?,
    val passageId: String?,
    val audioRef: String? = null
)

data class ExamAppConfig(
    val singleChoiceScoring: Int,
    val wordBankScoring: Int,
    val paragraphMatchingScoring: Int,
    val showAnswerAfterSubmit: Boolean,
    val recordWrongAnswers: Boolean,
    val allowRetry: Boolean
)

data class ExamSourceVerification(
    val source: String,
    val scope: String,
    val method: String,
    val notableCorrections: List<String>,
    val answerKeyVerified: Boolean
)
