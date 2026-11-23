package com.example.cet6vocabulary.presentation.model

import com.example.cet6vocabulary.data.model.LearningRecord
import com.example.cet6vocabulary.data.model.Word
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ArticleWordsTest {

    private fun word(id: Int) = Word(id, "word$id", "", "n.", "释义$id")

    private fun record(
        id: Int,
        spellCount: Int = 1,
        mastery: Int = 1,
        wrongCount: Int = 0,
        lastStudyTime: Long? = 0L
    ) = LearningRecord(id, spellCount, spellCount, wrongCount, mastery, lastStudyTime)

    private val words = (1..5).map(::word)

    @Test
    fun listsOnlyPractisedWords() {
        val records = listOf(record(1), record(2, spellCount = 0), record(3, spellCount = 4))
        assertEquals(listOf("word1", "word3"), articleCandidates(words, records).map { it.word.word })
    }

    @Test
    fun exposesTheRecordUsedForThePicker() {
        val records = listOf(record(1, mastery = 1, wrongCount = 3))
        val candidate = articleCandidates(words, records).single()
        assertEquals(1, candidate.record.mastery)
        assertEquals(3, candidate.record.wrongCount)
    }

    @Test
    fun ordersByMasteryThenMistakesThenOldestReview() {
        val records = listOf(
            record(1, mastery = 3, wrongCount = 9, lastStudyTime = 1L),
            record(2, mastery = 1, wrongCount = 1, lastStudyTime = 50L),
            record(3, mastery = 1, wrongCount = 4, lastStudyTime = 90L),
            record(4, mastery = 1, wrongCount = 1, lastStudyTime = 10L),
            record(5, mastery = 2, wrongCount = 0, lastStudyTime = 5L)
        )
        val picked = articleCandidates(words, records).map { it.word.word }
        assertEquals(listOf("word3", "word4", "word2", "word5", "word1"), picked)
    }

    @Test
    fun selectionFollowsRankingNotTapOrder() {
        val records = listOf(record(1, mastery = 3), record(2, mastery = 1), record(3, mastery = 2))
        val candidates = articleCandidates(words, records)
        val chosen = selectedArticleWords(candidates, setOf(1, 3))
        assertEquals(listOf("word3", "word1"), chosen.map { it.word })
    }

    @Test
    fun ignoresSelectedIdsThatAreNotCandidates() {
        val records = listOf(record(1))
        val candidates = articleCandidates(words, records)
        assertEquals(listOf("word1"), selectedArticleWords(candidates, setOf(1, 99)).map { it.word })
    }

    @Test
    fun ignoresRecordsForUnknownWords() {
        val records = listOf(record(1), record(99))
        assertEquals(listOf("word1"), articleCandidates(words, records).map { it.word.word })
    }

    @Test
    fun toggleAddsAndRemoves() {
        val added = toggleArticleWord(emptySet(), 3)
        assertEquals(setOf(3), added)
        assertEquals(emptySet<Int>(), toggleArticleWord(added, 3))
    }

    @Test
    fun toggleRefusesToExceedTheLimit() {
        val full = (1..3).toSet()
        assertEquals(full, toggleArticleWord(full, 4, limit = 3))
    }

    @Test
    fun toggleCanRemoveWhenTheLimitIsReached() {
        val full = (1..3).toSet()
        assertEquals(setOf(1, 3), toggleArticleWord(full, 2, limit = 3))
    }

    @Test
    fun defaultLimitAllowsTwelveWords() {
        var selected = emptySet<Int>()
        (1..20).forEach { selected = toggleArticleWord(selected, it) }
        assertEquals(MAX_ARTICLE_WORDS, selected.size)
        assertEquals(12, MAX_ARTICLE_WORDS)
    }

    @Test
    fun handlesEmptyInputs() {
        assertTrue(articleCandidates(words, emptyList()).isEmpty())
        assertTrue(articleCandidates(emptyList(), listOf(record(1))).isEmpty())
        assertTrue(selectedArticleWords(emptyList(), setOf(1)).isEmpty())
    }
}
