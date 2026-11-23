package com.example.cet6vocabulary.presentation.model

import com.example.cet6vocabulary.data.model.Exam
import com.example.cet6vocabulary.data.model.ExamAppConfig
import com.example.cet6vocabulary.data.model.ExamQuestion
import com.example.cet6vocabulary.data.model.ExamSection
import com.example.cet6vocabulary.data.model.ExamSectionData
import com.example.cet6vocabulary.data.model.ExamSubsection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadingScoringTest {

    private val config = ExamAppConfig(
        singleChoiceScoring = 1,
        wordBankScoring = 2,
        paragraphMatchingScoring = 3,
        showAnswerAfterSubmit = true,
        recordWrongAnswers = true,
        allowRetry = true
    )

    private fun question(
        number: Int,
        subsection: String,
        type: String? = null,
        correctAnswer: String? = null,
        explanation: String? = null
    ) = ExamQuestion(
        questionId = "q$number",
        examId = "exam",
        section = ExamSection.READING,
        subsection = subsection,
        number = number,
        type = type,
        question = "question $number",
        options = emptyMap(),
        correctAnswer = correctAnswer,
        explanation = explanation,
        prompt = null,
        instruction = null,
        minWords = null,
        maxWords = null,
        blank = null,
        passageId = null
    )

    private fun exam(
        questions: List<ExamQuestion>,
        answerKey: Map<Int, String> = emptyMap(),
        appConfig: ExamAppConfig? = config
    ) = Exam(
        schemaVersion = "1",
        examId = "exam",
        title = "2025-12 Set 1",
        date = "2025-12",
        setNumber = 1,
        setName = "Set 1",
        sections = listOf(
            ExamSectionData(
                section = ExamSection.READING,
                type = "reading",
                title = "Reading",
                durationMinutes = 40,
                questions = questions,
                subsections = listOf(
                    ExamSubsection(
                        key = "A",
                        type = "word_bank",
                        title = "Section A",
                        materials = com.example.cet6vocabulary.data.model.ExamMaterials(),
                        questions = questions.filter { it.subsection == "A" }
                    ),
                    ExamSubsection(
                        key = "B",
                        type = "paragraph_matching",
                        title = "Section B",
                        materials = com.example.cet6vocabulary.data.model.ExamMaterials(),
                        questions = questions.filter { it.subsection == "B" }
                    ),
                    ExamSubsection(
                        key = "C",
                        type = "single_choice",
                        title = "Section C",
                        materials = com.example.cet6vocabulary.data.model.ExamMaterials(),
                        questions = questions.filter { it.subsection == "C" }
                    )
                )
            )
        ),
        answerKey = mapOf(ExamSection.READING to answerKey),
        appConfig = appConfig,
        sourceVerification = null
    )

    @Test
    fun `all answers correct earns full points`() {
        val questions = listOf(
            question(26, "A", type = "word_bank", correctAnswer = "H"),
            question(27, "B", type = "paragraph_matching", correctAnswer = "J"),
            question(28, "C", type = "single_choice", correctAnswer = "B")
        )
        val score = scoreReading(
            exam(questions),
            mapOf("q26" to "H", "q27" to "J", "q28" to "B")
        )
        assertEquals(3, score.total)
        assertEquals(3, score.correct)
        assertEquals(0, score.incorrect)
        assertEquals(0, score.unanswered)
        assertEquals(2 + 3 + 1, score.earnedPoints)
        assertEquals(2 + 3 + 1, score.maxPoints)
        assertEquals(100, score.correctRate)
    }

    @Test
    fun `unanswered questions lower the rate and stay counted as not correct`() {
        val questions = listOf(
            question(26, "A", type = "word_bank", correctAnswer = "H"),
            question(27, "B", type = "paragraph_matching", correctAnswer = "J")
        )
        val score = scoreReading(exam(questions), mapOf("q26" to "H"))
        assertEquals(1, score.correct)
        assertEquals(0, score.incorrect)
        assertEquals(1, score.unanswered)
        assertEquals(2, score.earnedPoints)
        assertEquals(5, score.maxPoints)
        assertEquals(50, score.correctRate)
    }

    @Test
    fun `wrong answer is counted as incorrect and earns nothing`() {
        val questions = listOf(question(26, "C", type = "single_choice", correctAnswer = "D"))
        val score = scoreReading(exam(questions), mapOf("q26" to "A"))
        assertEquals(0, score.correct)
        assertEquals(1, score.incorrect)
        assertEquals(0, score.earnedPoints)
        assertEquals(0, score.correctRate)
    }

    @Test
    fun `answer key is used when the question carries no answer`() {
        val questions = listOf(question(26, "A", type = "word_bank"))
        val score = scoreReading(exam(questions, answerKey = mapOf(26 to "K")), mapOf("q26" to "K"))
        assertEquals(1, score.gradableCount)
        assertEquals(1, score.correct)
        assertEquals(2, score.earnedPoints)
    }

    @Test
    fun `question level answer wins over the answer key`() {
        val questions = listOf(question(26, "A", type = "word_bank", correctAnswer = "H"))
        val score = scoreReading(exam(questions, answerKey = mapOf(26 to "K")), mapOf("q26" to "H"))
        assertEquals(1, score.correct)
    }

    @Test
    fun `question without any answer is excluded from points and rate`() {
        val questions = listOf(
            question(26, "A", type = "word_bank", correctAnswer = "H"),
            question(27, "A", type = "word_bank")
        )
        val score = scoreReading(exam(questions), mapOf("q26" to "H", "q27" to "A"))
        assertEquals(2, score.answered)
        assertEquals(1, score.gradableCount)
        assertEquals(1, score.correct)
        assertEquals(2, score.maxPoints)
        assertEquals(100, score.correctRate)
        assertTrue(score.outcomes[1].answered)
        assertEquals(false, score.outcomes[1].gradable)
    }

    @Test
    fun `no gradable question yields a null rate instead of zero`() {
        val questions = listOf(question(26, "A", type = "word_bank"))
        val score = scoreReading(exam(questions), emptyMap())
        assertNull(score.correctRate)
        assertEquals(0, score.maxPoints)
    }

    @Test
    fun `missing app config falls back to one point per question`() {
        val questions = listOf(
            question(26, "A", type = "word_bank", correctAnswer = "H"),
            question(27, "B", type = "paragraph_matching", correctAnswer = "J")
        )
        val score = scoreReading(
            exam(questions, appConfig = null),
            mapOf("q26" to "H", "q27" to "J")
        )
        assertEquals(2, score.earnedPoints)
        assertEquals(2, score.maxPoints)
    }

    @Test
    fun `question type falls back to the subsection type`() {
        val questions = listOf(question(26, "A", type = null, correctAnswer = "H"))
        val score = scoreReading(exam(questions), mapOf("q26" to "H"))
        assertEquals(2, score.earnedPoints)
    }

    @Test
    fun `unknown question type scores one point`() {
        val questions = listOf(question(26, "A", type = "true_false", correctAnswer = "T"))
        val score = scoreReading(exam(questions), mapOf("q26" to "T"))
        assertEquals(1, score.earnedPoints)
    }

    @Test
    fun `exam without a reading section scores nothing`() {
        val exam = Exam(
            schemaVersion = "1",
            examId = "exam",
            title = "title",
            date = "2025-12",
            setNumber = 1,
            setName = "Set 1",
            sections = emptyList(),
            answerKey = emptyMap(),
            appConfig = config,
            sourceVerification = null
        )
        val score = scoreReading(exam, mapOf("q26" to "H"))
        assertEquals(0, score.total)
        assertNull(score.correctRate)
    }

    @Test
    fun `blank answers are treated as missing`() {
        val questions = listOf(question(26, "A", type = "word_bank", correctAnswer = "  "))
        val score = scoreReading(exam(questions), mapOf("q26" to ""))
        assertNull(score.outcomes.single().correctAnswer)
        assertEquals(0, score.gradableCount)
    }

    @Test
    fun `explanation is dropped when blank`() {
        val questions = listOf(
            question(26, "A", type = "word_bank", correctAnswer = "H", explanation = "  "),
            question(27, "A", type = "word_bank", correctAnswer = "J", explanation = "because")
        )
        val outcomes = scoreReading(exam(questions), emptyMap()).outcomes
        assertNull(outcomes[0].explanation)
        assertEquals("because", outcomes[1].explanation)
    }
}
