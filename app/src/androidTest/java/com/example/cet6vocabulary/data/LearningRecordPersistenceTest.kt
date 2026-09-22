package com.example.cet6vocabulary.data

import android.app.Activity
import android.app.Instrumentation
import android.os.Bundle
import android.util.Log
import androidx.room.Room
import com.example.cet6vocabulary.data.local.database.AppDatabase
import com.example.cet6vocabulary.data.local.entity.LearningRecordEntity
import com.example.cet6vocabulary.data.local.entity.StudyProgressEntity
import com.example.cet6vocabulary.data.model.ExamSection
import com.example.cet6vocabulary.data.model.LearningRecord
import com.example.cet6vocabulary.data.repository.ExamDataException
import com.example.cet6vocabulary.data.repository.ExamJsonParser
import com.example.cet6vocabulary.data.repository.ExamRepository
import com.example.cet6vocabulary.data.repository.LearningRecordRepository
import com.example.cet6vocabulary.data.repository.WordRepository
import com.example.cet6vocabulary.presentation.model.calculateLearningStatistics
import kotlinx.coroutines.runBlocking

class PersistenceInstrumentation : Instrumentation() {
    override fun onCreate(arguments: Bundle?) {
        super.onCreate(arguments)
        start()
    }

    override fun onStart() {
        val results = Bundle()
        try {
            runBlocking { verifyPersistence() }
            results.putString("result", "Exam data and Room persistence verified")
            finish(Activity.RESULT_OK, results)
            Runtime.getRuntime().exit(0)
        } catch (error: Throwable) {
            results.putString("error", error.stackTraceToString())
            Log.e("CET6_TEST", "persistence test failed", error)
            finish(Activity.RESULT_CANCELED, results)
            Runtime.getRuntime().exit(1)
        }
    }

    private suspend fun verifyPersistence() {
        val context = targetContext
        verifyExamData(context)
        verifyVocabulary(context)
        verifyStatistics(context)
        val databaseName = "learning_record_restart_test"
        verifyWordBookMigration(context)
        verifyStudyProgress(context)
        context.deleteDatabase(databaseName)

        var database = Room.databaseBuilder(context, AppDatabase::class.java, databaseName).build()
        var repository = LearningRecordRepository(database.learningRecordDao())
        check(repository.getRecord(52) == null)

        val first = repository.recordCorrect(52)
        check(first.spellCount == 1 && first.correctCount == 1 && first.wrongCount == 0)
        check(first.mastery == 2 && first.lastStudyTime != null)

        val wrong = repository.recordWrong(52)
        check(wrong.spellCount == 2 && wrong.correctCount == 1 && wrong.wrongCount == 1)
        check(wrong.mastery == 1)

        repository.recordCorrect(52)
        val thirdCorrect = repository.recordCorrect(52)
        check(thirdCorrect.spellCount == 4 && thirdCorrect.correctCount == 3 && thirdCorrect.wrongCount == 1)
        check(thirdCorrect.mastery == 3)
        database.close()

        database = Room.databaseBuilder(context, AppDatabase::class.java, databaseName).build()
        repository = LearningRecordRepository(database.learningRecordDao())
        check(repository.getRecord(52) == thirdCorrect)
        check(repository.getAllRecords().size == 1)
        val restartStatistics = calculateLearningStatistics(
            WordRepository(context).getAllWords().mapTo(mutableSetOf()) { it.id },
            repository.getAllRecords()
        )
        check(restartStatistics.learnedWords == 1 && restartStatistics.spellCount == 4)
        check(restartStatistics.correctCount == 3 && restartStatistics.wrongCount == 1)
        check(restartStatistics.accuracyPercent == 75 && restartStatistics.mastery3Count == 1)

        val dao = database.learningRecordDao()
        dao.insert(LearningRecordEntity(7, 1, 1, 0, 2, 1L, null))
        dao.insert(LearningRecordEntity(7, 2, 1, 1, 1, 2L, null))
        check(dao.getAll().count { it.wordId == 7 } == 1)
        check(dao.getByWordId(7)?.spellCount == 2)
        database.close()
        
        context.deleteDatabase(databaseName)
    }

    private fun verifyExamData(context: android.content.Context) {
        val parseFailure = runCatching { ExamJsonParser().parse("{", "broken.json") }.exceptionOrNull()
        check(parseFailure is ExamDataException && parseFailure.message?.contains("broken.json") == true)

        val repository = ExamRepository(context)
        val exams = repository.getExams()
        check(exams.size == 1)

        val exam = repository.getExam("cet6_2025_12_set1") ?: error("2025-12 set1 not found")
        check(exam.date == "2025-12")
        check(exam.setName == "set1")
        check(exam.sections.map { it.section } == listOf(ExamSection.WRITING, ExamSection.LISTENING, ExamSection.READING, ExamSection.TRANSLATION))

        check(repository.getQuestions(exam.examId).size == 57)
        check(repository.getQuestionsBySection(exam.examId, ExamSection.WRITING).size == 1)
        check(repository.getQuestionsBySection(exam.examId, ExamSection.LISTENING).size == 25)
        val readingQuestions = repository.getQuestionsBySection(exam.examId, ExamSection.READING)
        check(readingQuestions.size == 30)
        check(readingQuestions.map { it.number } == (26..55).toList())
        check(repository.getQuestionsBySection(exam.examId, ExamSection.TRANSLATION).size == 1)

        val allQuestions = repository.getQuestions(exam.examId)
        check(allQuestions.map { it.questionId }.toSet().size == 57)
        check(allQuestions.all { it.examId == exam.examId })
        check(repository.getQuestion(exam.examId, "reading_46")?.correctAnswer == "D")

        val listeningAnswers = exam.answerKey[ExamSection.LISTENING] ?: error("listening answer key not found")
        val readingAnswers = exam.answerKey[ExamSection.READING] ?: error("reading answer key not found")
        check(listeningAnswers.size == 25)
        check(readingAnswers.size == 30)
        check(allQuestions.filter { it.section == ExamSection.LISTENING }.all { listeningAnswers[it.number] == it.correctAnswer })
        check(allQuestions.filter { it.section == ExamSection.READING }.all { readingAnswers[it.number] == it.correctAnswer })

        val listening = repository.getQuestionsBySection(exam.examId, ExamSection.LISTENING)
        check(listening.all { it.question == "" })
        check(listening.all { it.explanation == null && it.audioRef == null })
        check(listening.all { it.options.keys == setOf("A", "B", "C", "D") })
        check(listening.all { it.correctAnswer != null && it.correctAnswer in setOf("A", "B", "C", "D") })

        val readingSection = exam.sections.first { it.section == ExamSection.READING }
        val sectionA = readingSection.subsections.first { it.key == "sectionA" }
        val sectionB = readingSection.subsections.first { it.key == "sectionB" }
        val sectionC = readingSection.subsections.first { it.key == "sectionC" }
        check(exam.sections.first { it.section == ExamSection.WRITING }.questions.single().prompt?.isNotBlank() == true)
        check(exam.sections.first { it.section == ExamSection.TRANSLATION }.questions.single().prompt?.isNotBlank() == true)
        check(sectionA.materials.passage?.isNotBlank() == true)
        check(sectionA.materials.wordBank.size == 15)
        check(sectionB.materials.paragraphs.size == 16)
        check(sectionB.materials.paragraphs.values.all { it.isNotBlank() })
        check(sectionC.materials.passages.size == 2)
        check(sectionC.questions.all { it.passageId != null && it.passageId in setOf("passage_1", "passage_2") })
        check(sectionC.questions.all { it.options.keys == setOf("A", "B", "C", "D") })
        check(sectionC.questions.all { it.correctAnswer != null && it.correctAnswer in setOf("A", "B", "C", "D") })
    }

    private suspend fun verifyWordBookMigration(context: android.content.Context) {
        val databaseName = "word_book_migration_test"
        context.deleteDatabase(databaseName)
        val old = context.openOrCreateDatabase(databaseName, 0, null)
        old.execSQL("CREATE TABLE learning_records (wordId INTEGER NOT NULL PRIMARY KEY, spellCount INTEGER NOT NULL, correctCount INTEGER NOT NULL, wrongCount INTEGER NOT NULL, mastery INTEGER NOT NULL, lastStudyTime INTEGER, nextReviewTime INTEGER)")
        old.execSQL("INSERT INTO learning_records VALUES (9, 2, 1, 1, 1, 10, NULL)")
        old.execSQL("PRAGMA user_version = 1")
        old.close()
        val database = Room.databaseBuilder(context, AppDatabase::class.java, databaseName).addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3).build()
        val records = LearningRecordRepository(database.learningRecordDao())
        check(records.getRecord(9)?.spellCount == 2)
        val book = com.example.cet6vocabulary.data.repository.WordBookRepository(database.wordBookDao())
        book.addWord(9); book.addWord(9); check(book.getWordBookCount() == 1); book.removeWord(9); check(book.getWordBookCount() == 0); book.addWord(10); check(records.getRecord(9)?.spellCount == 2)
        val progress = com.example.cet6vocabulary.data.repository.StudyProgressRepository(database.studyProgressDao())
        progress.saveProgress(StudyProgressEntity("normal_sequential", 56, null, 10L))
        check(progress.getProgress("normal_sequential")?.currentIndex == 56)
        progress.saveProgress(StudyProgressEntity("normal_sequential", 57, null, 11L))
        check(progress.getProgress("normal_sequential")?.currentIndex == 57)
        progress.deleteProgress("normal_sequential")
        check(progress.getProgress("normal_sequential") == null)
        database.close()
        context.deleteDatabase(databaseName)
    }

    private suspend fun verifyStudyProgress(context: android.content.Context) {
        val databaseName = "study_progress_test"
        context.deleteDatabase(databaseName)
        var database = Room.databaseBuilder(context, AppDatabase::class.java, databaseName).build()
        val repository = com.example.cet6vocabulary.data.repository.StudyProgressRepository(database.studyProgressDao())
        check(repository.getProgress("normal_sequential") == null)
        repository.saveProgress(StudyProgressEntity("normal_sequential", 56, null, 1L))
        repository.saveProgress(StudyProgressEntity("normal_random", 3, "[83,12,276,41,159]", 2L))
        repository.saveProgress(StudyProgressEntity("wordbook_sequential", 2, null, 3L))
        repository.saveProgress(StudyProgressEntity("wordbook_random", 1, "[201,80,15]", 4L))
        check(repository.getProgress("normal_sequential")?.currentIndex == 56)
        check(repository.getProgress("normal_random")?.randomOrder == "[83,12,276,41,159]")
        check(repository.getProgress("wordbook_sequential")?.currentIndex == 2)
        database.close()
        database = Room.databaseBuilder(context, AppDatabase::class.java, databaseName).build()
        val reopened = com.example.cet6vocabulary.data.repository.StudyProgressRepository(database.studyProgressDao())
        check(reopened.getProgress("wordbook_random")?.currentIndex == 1)
        reopened.saveProgress(StudyProgressEntity("wordbook_random", 2, "[201,15]", 5L))
        check(reopened.getProgress("wordbook_random")?.randomOrder == "[201,15]")
        reopened.deleteProgress("wordbook_sequential")
        check(reopened.getProgress("wordbook_sequential") == null)
        reopened.saveProgress(StudyProgressEntity(com.example.cet6vocabulary.presentation.screens.StudyProgressKeys.NORMAL_SEQUENTIAL, 387, null, 6L))
        check(reopened.getProgress(com.example.cet6vocabulary.presentation.screens.StudyProgressKeys.NORMAL_SEQUENTIAL)?.currentIndex == 387)
        check(reopened.getProgress("normal_random")?.currentIndex == 3)
        reopened.saveProgress(StudyProgressEntity("spelling_sequential", 387, null, 8L))
        check(reopened.getProgress("spelling_sequential")?.currentIndex == 387)
        check(reopened.getProgress("normal_sequential")?.currentIndex == 387)
        database.close()
        context.deleteDatabase(databaseName)
    }
    private fun verifyVocabulary(context: android.content.Context) {
        val words = WordRepository(context).getAllWords()
        check(words.size == 781)
        check(words.map { it.id } == (1..781).toList())
        check(words.map { it.id }.toSet().size == 781)
        check(words.none { it.word.isBlank() })
        check(words.all { it.phonetic.isNotBlank() && it.partOfSpeech.isNotBlank() && it.meaning.isNotBlank() })
        check(words.any { it.id == 626 && it.word == "fiscal" })
        check(words.any { it.id == 781 && it.word == "subordinate" })
        check(WordRepository(context).searchWords("fiscal").any { it.id == 626 })
        check(WordRepository(context).searchWords("财政的").any { it.id == 626 })
    }
    private fun verifyStatistics(context: android.content.Context) {
        val wordIds = WordRepository(context).getAllWords().mapTo(mutableSetOf()) { it.id }
        val totalWords = wordIds.size

        val empty = calculateLearningStatistics(wordIds, emptyList())
        check(empty.totalWords == totalWords)
        check(empty.learnedWords == 0 && empty.unlearnedWords == totalWords)
        check(empty.spellCount == 0 && empty.correctCount == 0 && empty.wrongCount == 0)
        check(empty.accuracyPercent == 0 && empty.learningProgress == 0f)
        check(empty.mastery0Count == totalWords)

        val emptyVocabulary = calculateLearningStatistics(emptySet(), emptyList())
        check(emptyVocabulary.totalWords == 0 && emptyVocabulary.learnedPercent == 0)
        check(emptyVocabulary.learningProgress == 0f && emptyVocabulary.accuracyPercent == 0)

        val records = listOf(
            LearningRecord(1, spellCount = 3, correctCount = 2, wrongCount = 1, mastery = 2),
            LearningRecord(2, spellCount = 2, correctCount = 1, wrongCount = 1, mastery = 1),
            LearningRecord(3, spellCount = 5, correctCount = 4, wrongCount = 1, mastery = 3)
        )
        val statistics = calculateLearningStatistics(wordIds, records)
        check(statistics.learnedWords == 3 && statistics.unlearnedWords == totalWords - 3)
        check(statistics.spellCount == 10 && statistics.correctCount == 7 && statistics.wrongCount == 3)
        check(statistics.accuracyPercent == 70)
        check(statistics.mastery0Count == totalWords - 3)
        check(statistics.mastery1Count == 1 && statistics.mastery2Count == 1 && statistics.mastery3Count == 1)
    }
}









