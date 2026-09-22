package com.example.cet6vocabulary.data.repository

import android.content.Context
import com.example.cet6vocabulary.data.model.Exam
import com.example.cet6vocabulary.data.model.ExamQuestion
import com.example.cet6vocabulary.data.model.ExamSection

class ExamRepository(
    context: Context,
    private val parser: ExamJsonParser = ExamJsonParser()
) {
    private val applicationContext = context.applicationContext
    private val examList: List<Exam> by lazy { loadExams() }

    fun getExams(): List<Exam> = examList

    fun getExam(examId: String): Exam? = examList.firstOrNull { it.examId == examId }

    fun getQuestions(examId: String): List<ExamQuestion> = requireExam(examId).sections.flatMap { it.questions }

    fun getQuestionsBySection(examId: String, section: ExamSection): List<ExamQuestion> =
        requireExam(examId).sections.firstOrNull { it.section == section }?.questions.orEmpty()

    fun getQuestion(examId: String, questionId: String): ExamQuestion? =
        getQuestions(examId).firstOrNull { it.questionId == questionId }

    private fun requireExam(examId: String): Exam =
        getExam(examId) ?: throw ExamDataException("Exam '$examId' was not found")

    private fun loadExams(): List<Exam> {
        val assetFiles = findExamAssets()
        if (assetFiles.isEmpty()) {
            throw ExamDataException("No exam JSON files found in date directories")
        }
        val loaded = assetFiles.map { assetPath ->
            try {
                val json = applicationContext.assets.open(assetPath).bufferedReader(Charsets.UTF_8).use { it.readText() }
                parser.parse(json, assetPath)
            } catch (error: ExamDataException) {
                throw error
            } catch (error: Exception) {
                throw ExamDataException("Failed to load exam asset '$assetPath': ${error.message}", error)
            }
        }
        val duplicateExamIds = loaded.groupBy { it.examId }.filterValues { it.size > 1 }.keys
        if (duplicateExamIds.isNotEmpty()) {
            throw ExamDataException("Duplicate examId(s): ${duplicateExamIds.sorted().joinToString()}")
        }
        loaded.forEach { exam ->
            val questions = exam.sections.flatMap { it.questions }
            val duplicateQuestionIds = questions.groupBy { it.questionId }.filterValues { it.size > 1 }.keys
            if (duplicateQuestionIds.isNotEmpty()) {
                throw ExamDataException(
                    "${exam.examId}: duplicate questionId(s): ${duplicateQuestionIds.sorted().joinToString()}"
                )
            }
        }
        return loaded.sortedWith(compareBy<Exam> { it.date }.thenBy { it.setNumber }.thenBy { it.examId })
    }

    private fun findExamAssets(): List<String> =
        applicationContext.assets.list(ASSET_ROOT).orEmpty()
            .filter { DATE_DIRECTORY.matches(it) }
            .flatMap { collectJsonAssets(it) }
            .sorted()

    private fun collectJsonAssets(path: String): List<String> {
        val children = applicationContext.assets.list(path).orEmpty()
        if (children.isEmpty()) {
            return if (path.endsWith(".json", ignoreCase = true)) listOf(path) else emptyList()
        }
        return children.flatMap { child ->
            val childPath = "$path/$child"
            if (child.endsWith(".json", ignoreCase = true)) listOf(childPath) else collectJsonAssets(childPath)
        }
    }

    private companion object {
        const val ASSET_ROOT = ""
        val DATE_DIRECTORY = Regex("^[0-9]{4}-[0-9]{2}$")
    }
}
