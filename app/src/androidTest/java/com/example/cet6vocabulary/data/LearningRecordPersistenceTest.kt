package com.example.cet6vocabulary.data

import android.app.Activity
import android.app.Instrumentation
import android.os.Bundle
import android.util.Log
import androidx.room.Room
import com.example.cet6vocabulary.data.local.database.AppDatabase
import com.example.cet6vocabulary.data.local.entity.LearningRecordEntity
import com.example.cet6vocabulary.data.local.entity.StudyProgressEntity
import com.example.cet6vocabulary.data.local.entity.WordBookEntity
import com.example.cet6vocabulary.data.model.ExamSection
import com.example.cet6vocabulary.data.model.LearningRecord
import com.example.cet6vocabulary.data.model.Word
import com.example.cet6vocabulary.data.repository.ExamDataException
import com.example.cet6vocabulary.data.repository.ExamJsonParser
import com.example.cet6vocabulary.data.repository.ExamRepository
import com.example.cet6vocabulary.data.repository.LearningDataFormatException
import com.example.cet6vocabulary.data.repository.LearningDataJson
import com.example.cet6vocabulary.data.repository.LearningDataRepository
import com.example.cet6vocabulary.data.repository.LearningDataSnapshot
import com.example.cet6vocabulary.data.repository.LearningRecordRepository
import com.example.cet6vocabulary.data.repository.WordRepository
import com.example.cet6vocabulary.presentation.model.calculateLearningStatistics
import com.example.cet6vocabulary.presentation.model.countReviewWords
import com.example.cet6vocabulary.presentation.model.selectReviewWords
import com.example.cet6vocabulary.presentation.model.ReadingSheet
import com.example.cet6vocabulary.presentation.model.ReadingSheetItem
import com.example.cet6vocabulary.presentation.model.ReadingSheetSection
import com.example.cet6vocabulary.presentation.model.ReadingSheetStatus
import com.example.cet6vocabulary.presentation.model.buildReadingSheet
import com.example.cet6vocabulary.presentation.model.scoreReading
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
        verifyReadingAnswerSheet(context)
        verifyReadingStageHasNoPersistence(context)
        verifyVocabulary(context)
        verifyStatistics(context)
        verifyReviewSelection()
        verifyLearningDataTransfer(context)
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

        // Reading scoring: 30 questions, one point each in this paper's appConfig.
        val correctAnswers = readingQuestions.associate { it.questionId to it.correctAnswer.orEmpty() }
        val allCorrect = scoreReading(exam, correctAnswers)
        check(allCorrect.total == 30 && allCorrect.maxPoints == 30)
        check(allCorrect.correct == 30 && allCorrect.incorrect == 0 && allCorrect.unanswered == 0)
        check(allCorrect.earnedPoints == 30 && allCorrect.correctRate == 100)
        check(allCorrect.outcomes.map { it.subsectionLabel }.distinct() == listOf("Section A", "Section B", "Section C"))
        check(allCorrect.outcomes.map { it.number } == (26..55).toList())

        val oneWrong = scoreReading(exam, correctAnswers + ("reading_26" to "A"))
        check(oneWrong.correct == 29 && oneWrong.incorrect == 1 && oneWrong.unanswered == 0)
        check(oneWrong.earnedPoints == 29 && oneWrong.correctRate == 96)

        val partial = scoreReading(exam, mapOf("reading_26" to "H"))
        check(partial.answered == 1 && partial.unanswered == 29)
        check(partial.correct == 1 && partial.correctRate == 3)

        // The paper answer key alone must grade questions that carry no inline answer.
        val keyOnlyExam = exam.copy(
            sections = exam.sections.map { section ->
                if (section.section == ExamSection.READING) {
                    section.copy(questions = section.questions.map { it.copy(correctAnswer = null) })
                } else {
                    section
                }
            }
        )
        val keyOnly = scoreReading(keyOnlyExam, correctAnswers)
        check(keyOnly.correct == 30 && keyOnly.maxPoints == 30 && keyOnly.correctRate == 100)
    }

    /** Stage 10.4: the answer sheet reports answered / unanswered / current, derived from real data. */
    private fun verifyReadingAnswerSheet(context: android.content.Context) {
        val exam = ExamRepository(context).getExam("cet6_2025_12_set1") ?: error("2025-12 set1 not found")
        val reading = exam.sections.first { it.section == ExamSection.READING }
        val answers = linkedMapOf<String, String>()

        var sheet = buildReadingSheet(reading, answers, 0)
        check(sheet.total == 30)
        check(sheet.answered == 0 && sheet.unanswered == 30)
        check(sheet.items.map { it.number } == (26..55).toList())
        check(sheet.items.map { it.index } == (0..29).toList())
        check(sheet.items.map { it.questionId } == reading.questions.map { it.questionId })
        check(ReadingSheetStatus.entries.size == 3)

        // Subsections and their number ranges come from the JSON, not from hardcoded counts.
        check(sheet.sections.size == 3)
        check(sheet.sections.map { it.key } == listOf("sectionA", "sectionB", "sectionC"))
        check(sheet.sections.map { it.label } == listOf("Section A", "Section B", "Section C"))
        check(sheet.sections.map { it.items.size } == listOf(10, 10, 10))
        check(sheet.sections[0].firstNumber == 26 && sheet.sections[0].lastNumber == 35)
        check(sheet.sections[1].firstNumber == 36 && sheet.sections[1].lastNumber == 45)
        check(sheet.sections[2].firstNumber == 46 && sheet.sections[2].lastNumber == 55)
        check(sheet.sections.sumOf { it.answeredCount } == 0)

        // Fresh sheet: question 26 is current, every other question is unanswered.
        check(sheet.items.first().status == ReadingSheetStatus.CURRENT)
        check(sheet.items.drop(1).all { it.status == ReadingSheetStatus.UNANSWERED })

        // Answering 26 while staying on it: the current marker wins, the answer is still counted.
        answers["reading_26"] = "H"
        sheet = buildReadingSheet(reading, answers, 0)
        check(sheet.answered == 1 && sheet.unanswered == 29)
        check(sheet.items.first().status == ReadingSheetStatus.CURRENT)
        check(sheet.sections[0].answeredCount == 1)

        // Jumping to 40 (index 14) moves the current marker and turns 26 into answered.
        answers["reading_40"] = "A"
        sheet = buildReadingSheet(reading, answers, 14)
        check(sheet.answered == 2 && sheet.unanswered == 28)
        check(sheet.items[14].number == 40 && sheet.items[14].status == ReadingSheetStatus.CURRENT)
        check(sheet.items.first().status == ReadingSheetStatus.ANSWERED)
        check(sheet.sections[0].answeredCount == 1 && sheet.sections[1].answeredCount == 1)
        check(sheet.sections[2].answeredCount == 0)

        // Changing an answer keeps the question answered.
        answers["reading_40"] = "C"
        sheet = buildReadingSheet(reading, answers, 14)
        check(sheet.answered == 2 && sheet.unanswered == 28)
        check(sheet.items[14].status == ReadingSheetStatus.CURRENT)
        check(sheet.items.first().status == ReadingSheetStatus.ANSWERED)

        // Jumping back to the first question keeps both answers.
        sheet = buildReadingSheet(reading, answers, 0)
        check(sheet.items.first().status == ReadingSheetStatus.CURRENT)
        check(sheet.items[14].status == ReadingSheetStatus.ANSWERED)
        check(sheet.answered == 2 && sheet.unanswered == 28)

        // Restart clears everything back to unanswered.
        answers.clear()
        sheet = buildReadingSheet(reading, answers, 0)
        check(sheet.answered == 0 && sheet.unanswered == 30)
        check(sheet.items.drop(1).all { it.status == ReadingSheetStatus.UNANSWERED })

        // A fully answered sheet finishes with 30 answered / 0 unanswered.
        reading.questions.forEach { answers[it.questionId] = "A" }
        sheet = buildReadingSheet(reading, answers, 29)
        check(sheet.answered == 30 && sheet.unanswered == 0)
        check(sheet.items.last().status == ReadingSheetStatus.CURRENT)
        check(sheet.sections.sumOf { it.answeredCount } == 30)

        // An out-of-range index simply marks nothing current instead of crashing the sheet.
        sheet = buildReadingSheet(reading, answers, 99)
        check(sheet.answered == 30 && sheet.items.none { it.status == ReadingSheetStatus.CURRENT })
    }

    /** Reading answers stay in memory: the sheet carries no correctness data and Room gains no exam tables. */
    private suspend fun verifyReadingStageHasNoPersistence(context: android.content.Context) {
        val sheetFields = listOf(
            ReadingSheet::class.java,
            ReadingSheetSection::class.java,
            ReadingSheetItem::class.java
        ).flatMap { type -> type.declaredFields.map { it.name.lowercase() } }
        check(sheetFields.none { it.contains("correct") || it.contains("score") || it.contains("accuracy") || it.contains("wrong") })

        // @Database cannot be read on device (only kotlin.Metadata survives D8), so the Room
        // surface is probed directly: DAO methods, generated impl, entity classes, and the schema
        // a freshly created database actually has.
        check(AppDatabase::class.java.declaredMethods
            .filter { java.lang.reflect.Modifier.isAbstract(it.modifiers) }
            .map { it.name }.toSet() == setOf("learningRecordDao", "wordBookDao", "studyProgressDao"))
        check(runCatching { Class.forName("com.example.cet6vocabulary.data.local.database.AppDatabase_Impl") }.isSuccess)
        check(runCatching { Class.forName("com.example.cet6vocabulary.data.local.entity.ExamAnswerEntity") }.isFailure)
        check(runCatching { Class.forName("com.example.cet6vocabulary.data.local.entity.ExamProgressEntity") }.isFailure)

        val databaseName = "room_schema_probe_test"
        context.deleteDatabase(databaseName)
        val database = Room.databaseBuilder(context, AppDatabase::class.java, databaseName).build()
        val sqlite = database.openHelper.writableDatabase
        check(sqlite.compileStatement("PRAGMA user_version").use { it.simpleQueryForLong() } == 3L)
        val tables = mutableListOf<String>()
        sqlite.query("SELECT name FROM sqlite_master WHERE type = 'table'").use { cursor ->
            while (cursor.moveToNext()) tables.add(cursor.getString(0))
        }
        check(tables.none { it.contains("exam", ignoreCase = true) }) { "unexpected exam table in $tables" }
        check(tables.containsAll(listOf("learning_records", "word_book", "study_progress"))) { "missing tables in $tables" }
        database.close()
        context.deleteDatabase(databaseName)
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
    private suspend fun verifyVocabulary(context: android.content.Context) {
        val repository = WordRepository(context)
        val words = repository.getAllWords()
        val retiredIds = listOf(1251, 1252, 1253, 1254, 1255)
        check(words.size == 1495)
        check(words.map { it.id } == ((1..1500).toList() - retiredIds))
        check(words.map { it.id }.toSet().size == 1495)
        check(words.none { it.id in 1251..1255 })
        check(words.none { it.word.isBlank() })
        check(words.all { it.phonetic.isNotBlank() && it.partOfSpeech.isNotBlank() && it.meaning.isNotBlank() })
        check(words.any { it.id == 626 && it.word == "fiscal" })
        check(words.any { it.id == 781 && it.word == "subordinate" })
        check(words.any { it.id == 782 && it.word == "submit" })
        check(words.any { it.id == 936 && it.word == "basement" })
        check(words.any { it.id == 937 && it.word == "mortal" })
        check(words.any { it.id == 1088 && it.word == "slide" })
        check(words.any { it.id == 1089 && it.word == "spur" })
        check(words.any { it.id == 1250 && it.word == "trace" })
        check(words.any { it.id == 1256 && it.word == "testify" })
        check(words.any { it.id == 1500 && it.word == "intuition" })
        check(repository.searchWords("fiscal").any { it.id == 626 })
        check(repository.searchWords("财政的").any { it.id == 626 })
        check(repository.searchWords("submit").any { it.id == 782 })
        check(repository.searchWords("地下室").any { it.id == 936 })
        check(repository.searchWords("mortal").any { it.id == 937 })
        check(repository.searchWords("slide").any { it.id == 1088 })
        check(repository.searchWords("spur").any { it.id == 1089 })
        check(repository.searchWords("trace").any { it.id == 1250 })
        check(repository.searchWords("testify").any { it.id == 1256 })
        check(repository.searchWords("intuition").any { it.id == 1500 })
        check(repository.searchWords("直觉").any { it.id == 1500 })
    }
    private suspend fun verifyStatistics(context: android.content.Context) {
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

    /**
     * 待复习是纯函数选词：只收练习过、且掌握度 ≤1 的词；
     * 排序依次是掌握度升序、错误次数降序、最久未复习优先。
     */
    private fun verifyReviewSelection() {
        val words = listOf(
            Word(1, "alpha", "", "", "第一个"),
            Word(2, "beta", "", "", "第二个"),
            Word(3, "gamma", "", "", "第三个"),
            Word(4, "delta", "", "", "第四个"),
            Word(5, "epsilon", "", "", "第五个"),
            Word(6, "zeta", "", "", "第六个")
        )
        val records = listOf(
            LearningRecord(1, spellCount = 1, correctCount = 0, wrongCount = 1, mastery = 1, lastStudyTime = 300L),
            LearningRecord(2, spellCount = 3, correctCount = 3, wrongCount = 0, mastery = 3, lastStudyTime = 100L),
            LearningRecord(3, spellCount = 5, correctCount = 3, wrongCount = 2, mastery = 3, lastStudyTime = 200L),
            LearningRecord(4, spellCount = 2, correctCount = 2, wrongCount = 0, mastery = 2, lastStudyTime = 100L),
            LearningRecord(5, spellCount = 0, correctCount = 0, wrongCount = 0, mastery = 0, lastStudyTime = null),
            LearningRecord(6, spellCount = 4, correctCount = 1, wrongCount = 3, mastery = 1, lastStudyTime = 50L)
        )

        // 6 与 1 同为 1 级，6 错得更多排在前面；3 已是 3 级，即使错过 2 次也不算待复习；5 从未练习，同样不算。
        check(selectReviewWords(words, records).map { it.word.id } == listOf(6, 1))
        check(countReviewWords(words.mapTo(mutableSetOf()) { it.id }, records) == 2)
        check(countReviewWords(emptySet(), records) == 0)
        check(countReviewWords(setOf(9), records) == 0)
        check(selectReviewWords(words, emptyList()).isEmpty())
    }

    /**
     * 导出格式与覆盖式导入：编码 → 解码必须原样还原，格式/版本不对要明确拒绝；
     * 导入后再导出应得到同一份数据，清除后三张表都为空。
     */
    private suspend fun verifyLearningDataTransfer(context: android.content.Context) {
        val records = listOf(
            LearningRecordEntity(1, 3, 2, 1, 2, 111L, null),
            LearningRecordEntity(2, 1, 0, 1, 1, null, 222L)
        )
        // DAO 按 addedTime DESC 返回，快照保持同一顺序，便于直接比较。
        val wordBook = listOf(WordBookEntity(9, 2000L), WordBookEntity(1, 1000L))
        val progress = listOf(
            StudyProgressEntity("normal_random", 4, "1,2,3", 4000L),
            StudyProgressEntity("spelling_sequential", 12, null, 3000L)
        )
        val snapshot = LearningDataSnapshot(records, wordBook, progress)

        val encoded = LearningDataJson.encode(snapshot, 12_345L)
        check(encoded.contains("\"format\": \"cet6-learning\""))

        val decoded = LearningDataJson.decode(encoded)
        check(decoded.records == records)
        check(decoded.wordBook == wordBook)
        check(decoded.studyProgress == progress)

        check(
            runCatching { LearningDataJson.decode("not json") }.exceptionOrNull()
                is LearningDataFormatException
        )
        check(
            runCatching { LearningDataJson.decode("""{"format":"other","version":1}""") }
                .exceptionOrNull() is LearningDataFormatException
        )
        check(
            runCatching { LearningDataJson.decode("""{"format":"cet6-learning","version":99}""") }
                .exceptionOrNull() is LearningDataFormatException
        )
        check(
            LearningDataJson.decode(
                LearningDataJson.encode(LearningDataSnapshot(emptyList(), emptyList(), emptyList()), 1L)
            ).isEmpty
        )

        val databaseName = "learning_data_transfer_test"
        context.deleteDatabase(databaseName)
        val database = Room.databaseBuilder(context, AppDatabase::class.java, databaseName).build()
        val repository = LearningDataRepository(
            records = database.learningRecordDao(),
            wordBook = database.wordBookDao(),
            progress = database.studyProgressDao()
        )
        // 先放入与快照冲突的旧数据，验证导入是覆盖而不是合并。
        database.learningRecordDao().insert(LearningRecordEntity(7, 9, 9, 0, 3, 1L, null))
        database.wordBookDao().insert(WordBookEntity(7, 1L))
        database.studyProgressDao().insertOrReplace(StudyProgressEntity("stale", 5, null, 1L))

        repository.replaceAll(decoded)
        check(database.learningRecordDao().getAll() == records)
        check(database.wordBookDao().getAll() == wordBook)
        check(database.studyProgressDao().getAll() == progress)
        check(database.studyProgressDao().getProgress("stale") == null)

        val roundTrip = LearningDataJson.decode(repository.export(99L))
        check(roundTrip.records == records && roundTrip.wordBook == wordBook)
        check(roundTrip.studyProgress == progress)

        repository.clearAll()
        check(database.learningRecordDao().getAll().isEmpty())
        check(database.wordBookDao().count() == 0)
        check(database.studyProgressDao().getAll().isEmpty())

        database.close()
        context.deleteDatabase(databaseName)
    }
}









