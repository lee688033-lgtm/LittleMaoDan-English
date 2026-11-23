package com.example.cet6vocabulary.presentation.model

import com.example.cet6vocabulary.data.model.LearningRecord
import org.junit.Assert.assertEquals
import org.junit.Test

class LearningStatisticsTest {

    private fun record(
        wordId: Int,
        spellCount: Int = 0,
        correctCount: Int = 0,
        wrongCount: Int = 0,
        mastery: Int = 0
    ) = LearningRecord(
        wordId = wordId,
        spellCount = spellCount,
        correctCount = correctCount,
        wrongCount = wrongCount,
        mastery = mastery
    )

    @Test
    fun `empty wordbook has no percentage and no crash`() {
        val stats = calculateLearningStatistics(emptySet(), emptyList())
        assertEquals(0, stats.totalWords)
        assertEquals(0, stats.learnedPercent)
        assertEquals(0f, stats.learningProgress, 0.0001f)
        assertEquals("0 / 0", stats.progressText)
    }

    @Test
    fun `records of words outside the wordbook are ignored`() {
        val stats = calculateLearningStatistics(
            wordIds = setOf(1, 2),
            records = listOf(
                record(1, spellCount = 2, correctCount = 1, wrongCount = 1, mastery = 1),
                record(99, spellCount = 10, correctCount = 10, mastery = 3)
            )
        )
        assertEquals(2, stats.totalWords)
        assertEquals(1, stats.learnedWords)
        assertEquals(2, stats.spellCount)
        assertEquals(1, stats.correctCount)
        assertEquals(50, stats.accuracyPercent)
        assertEquals(50, stats.learnedPercent)
    }

    @Test
    fun `words without a record count as unlearned mastery zero`() {
        val stats = calculateLearningStatistics(
            wordIds = setOf(1, 2, 3),
            records = listOf(record(1, spellCount = 1, correctCount = 1, mastery = 1))
        )
        assertEquals(2, stats.unlearnedWords)
        assertEquals(2, stats.mastery0Count)
        assertEquals(1, stats.mastery1Count)
        assertEquals(0, stats.mastery2Count)
        assertEquals(0, stats.mastery3Count)
    }

    @Test
    fun `accuracy uses truncating integer division`() {
        val stats = calculateLearningStatistics(
            wordIds = setOf(1),
            records = listOf(record(1, spellCount = 3, correctCount = 2, mastery = 2))
        )
        assertEquals(66, stats.accuracyPercent)
    }

    @Test
    fun `progress text keeps the slash spacing`() {
        val stats = calculateLearningStatistics(
            wordIds = setOf(1, 2, 3, 4),
            records = listOf(record(1, spellCount = 1), record(2, spellCount = 2))
        )
        assertEquals("2 / 4", stats.progressText)
        assertEquals(0.5f, stats.learningProgress, 0.0001f)
    }
}
