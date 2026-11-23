package com.example.cet6vocabulary.presentation.model

import com.example.cet6vocabulary.data.model.LearningRecord
import com.example.cet6vocabulary.data.model.Word
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewWordsTest {

    private fun word(id: Int) = Word(
        id = id,
        word = "word$id",
        phonetic = "/w/$id/",
        partOfSpeech = "n.",
        meaning = "meaning $id"
    )

    private fun record(
        wordId: Int,
        spellCount: Int = 1,
        wrongCount: Int = 0,
        mastery: Int = 1,
        lastStudyTime: Long? = 0L
    ) = LearningRecord(
        wordId = wordId,
        spellCount = spellCount,
        correctCount = spellCount - wrongCount,
        wrongCount = wrongCount,
        mastery = mastery,
        lastStudyTime = lastStudyTime
    )

    @Test
    fun `never practised word is not due`() {
        assertFalse(isDueForReview(record(1, spellCount = 0, mastery = 0)))
    }

    @Test
    fun `practised word at mastery 1 is due`() {
        assertTrue(isDueForReview(record(1, mastery = 1)))
    }

    @Test
    fun `word mastered twice is not due even with many mistakes`() {
        assertFalse(isDueForReview(record(1, spellCount = 5, wrongCount = 4, mastery = 2)))
    }

    @Test
    fun `answer wrong then correct removes the word from the list`() {
        val words = listOf(word(1))
        val wrong = listOf(record(1, spellCount = 3, wrongCount = 2, mastery = 1))
        assertEquals(1, selectReviewWords(words, wrong).size)

        // recordCorrect raises mastery to 2, which is the only condition the UI promises.
        val corrected = listOf(record(1, spellCount = 4, wrongCount = 2, mastery = 2))
        assertTrue(selectReviewWords(words, corrected).isEmpty())
    }

    @Test
    fun `records outside the wordbook are ignored`() {
        val words = listOf(word(1))
        val records = listOf(record(1), record(99, mastery = 1))
        val selected = selectReviewWords(words, records)
        assertEquals(1, selected.size)
        assertEquals(1, selected.single().word.id)
        assertEquals(1, countReviewWords(setOf(1), records))
    }

    @Test
    fun `lower mastery comes first then more mistakes`() {
        val words = listOf(word(1), word(2), word(3), word(4))
        val records = listOf(
            record(1, mastery = 1, wrongCount = 1),
            record(2, mastery = 0, wrongCount = 0),
            record(3, mastery = 1, wrongCount = 3),
            record(4, mastery = 1, wrongCount = 1, lastStudyTime = 5L)
        )
        val order = selectReviewWords(words, records).map { it.word.id }
        assertEquals(listOf(2, 3, 1, 4), order)
    }

    @Test
    fun `empty records produce empty result`() {
        assertTrue(selectReviewWords(listOf(word(1)), emptyList()).isEmpty())
        assertEquals(0, countReviewWords(setOf(1, 2), emptyList()))
    }

    @Test
    fun `mastery labels agree across screens`() {
        assertEquals("未掌握", masteryLabel(0))
        assertEquals("学习中", masteryLabel(1))
        assertEquals("已练习", masteryLabel(2))
        assertEquals("熟练", masteryLabel(3))
        assertEquals("熟练", masteryLabel(9))
    }
}
