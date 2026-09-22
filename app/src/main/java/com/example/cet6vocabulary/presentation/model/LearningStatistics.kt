package com.example.cet6vocabulary.presentation.model

import com.example.cet6vocabulary.data.model.LearningRecord

data class LearningStatistics(
    val totalWords: Int,
    val learnedWords: Int,
    val unlearnedWords: Int,
    val spellCount: Int,
    val correctCount: Int,
    val wrongCount: Int,
    val accuracyPercent: Int,
    val mastery0Count: Int,
    val mastery1Count: Int,
    val mastery2Count: Int,
    val mastery3Count: Int
) {
    val learnedPercent: Int
        get() = if (totalWords == 0) 0 else learnedWords * 100 / totalWords

    val progressText: String
        get() = learnedWords.toString() + 32.toChar() + 47.toChar() + 32.toChar() + totalWords

    val learningProgress: Float
        get() = if (totalWords == 0) 0f else learnedWords.toFloat() / totalWords
}

fun calculateLearningStatistics(
    wordIds: Set<Int>,
    records: List<LearningRecord>
): LearningStatistics {
    val recordsByWordId = records.associateBy { it.wordId }
    val currentRecords = wordIds.mapNotNull(recordsByWordId::get)
    val learnedWords = currentRecords.count { it.spellCount > 0 }
    val spellCount = currentRecords.sumOf { it.spellCount }
    val correctCount = currentRecords.sumOf { it.correctCount }
    val wrongCount = currentRecords.sumOf { it.wrongCount }

    return LearningStatistics(
        totalWords = wordIds.size,
        learnedWords = learnedWords,
        unlearnedWords = wordIds.size - learnedWords,
        spellCount = spellCount,
        correctCount = correctCount,
        wrongCount = wrongCount,
        accuracyPercent = if (spellCount == 0) 0 else correctCount * 100 / spellCount,
        mastery0Count = wordIds.count { recordsByWordId[it]?.mastery ?: 0 == 0 },
        mastery1Count = currentRecords.count { it.mastery == 1 },
        mastery2Count = currentRecords.count { it.mastery == 2 },
        mastery3Count = currentRecords.count { it.mastery == 3 }
    )
}
