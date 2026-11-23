package com.example.cet6vocabulary.presentation.model

import com.example.cet6vocabulary.data.model.LearningRecord
import com.example.cet6vocabulary.data.model.Word

/** A practised word that has not settled at a stable mastery level yet. */
data class ReviewWord(
    val word: Word,
    val record: LearningRecord
)

/**
 * 待复习口径：练习过（spellCount > 0）且掌握度不高于 1 级。
 *
 * 答错会把掌握度压回 1 级，答对一次升到 2 级，因此“答对就离开列表”是成立的；
 * 如果再把“错误次数多”单独作为条件，会出现答对后仍留在列表里的矛盾状态。
 * 从未练习的词属于“待学习”，不进入待复习，否则整个词库都会被倒进来。
 */
fun isDueForReview(record: LearningRecord): Boolean =
    record.spellCount > 0 && record.mastery <= 1

/**
 * 掌握度低的排前面，其次错误次数多的，最后按最久没复习的排。
 * 结果只依赖入参，方便在 Instrumentation 里直接断言。
 */
fun selectReviewWords(words: List<Word>, records: List<LearningRecord>): List<ReviewWord> {
    val dueRecords = records.filter(::isDueForReview).associateBy { it.wordId }
    if (dueRecords.isEmpty()) return emptyList()
    return words.mapNotNull { word ->
        dueRecords[word.id]?.let { ReviewWord(word, it) }
    }.sortedWith(
        compareBy<ReviewWord> { it.record.mastery }
            .thenByDescending { it.record.wrongCount }
            .thenBy { it.record.lastStudyTime ?: 0L }
    )
}

/** Count for the entry card; records of words that are not in the wordbook are ignored. */
fun countReviewWords(wordIds: Set<Int>, records: List<LearningRecord>): Int =
    records.count { it.wordId in wordIds && isDueForReview(it) }

/** 掌握度的统一文案，待复习列表、单词详情与统计页共用。 */
fun masteryLabel(mastery: Int): String = when (mastery) {
    0 -> "未掌握"
    1 -> "学习中"
    2 -> "已练习"
    else -> "熟练"
}
