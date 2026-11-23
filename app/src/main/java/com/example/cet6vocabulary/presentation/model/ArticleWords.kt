package com.example.cet6vocabulary.presentation.model

import com.example.cet6vocabulary.data.model.LearningRecord
import com.example.cet6vocabulary.data.model.Word

/** Below this there is not enough vocabulary to build a readable article. */
const val MIN_ARTICLE_WORDS = 3

/** Upper bound for one request: more words make the model pad the text instead of using them. */
const val MAX_ARTICLE_WORDS = 12

/** A practised word together with the record that makes it available for the article. */
data class ArticleCandidate(val word: Word, val record: LearningRecord)

/**
 * Every practised word, weakest mastery first, then the ones with the most mistakes, then the
 * ones left alone the longest. The list is what the learner picks from; the order only decides
 * which words are pre-selected and how the picker reads.
 */
fun articleCandidates(words: List<Word>, records: List<LearningRecord>): List<ArticleCandidate> {
    val practised = records.filter { it.spellCount > 0 }.associateBy { it.wordId }
    if (practised.isEmpty()) return emptyList()
    return words
        .mapNotNull { word -> practised[word.id]?.let { ArticleCandidate(word, it) } }
        .sortedWith(
            compareBy<ArticleCandidate> { it.record.mastery }
                .thenByDescending { it.record.wrongCount }
                .thenBy { it.record.lastStudyTime ?: 0L }
                .thenBy { it.word.word }
        )
}

/**
 * The learner's picks in ranking order, so the article prompt does not depend on the order the
 * checkboxes happened to be tapped. Ids that are no longer candidates are ignored.
 */
fun selectedArticleWords(candidates: List<ArticleCandidate>, selectedIds: Set<Int>): List<Word> =
    candidates.filter { it.word.id in selectedIds }.map { it.word }

/**
 * Adds or removes one pick, never letting the selection exceed [limit]. Removing stays possible
 * at the limit, otherwise a full selection could not be corrected.
 */
fun toggleArticleWord(
    selected: Set<Int>,
    wordId: Int,
    limit: Int = MAX_ARTICLE_WORDS
): Set<Int> = when {
    wordId in selected -> selected - wordId
    selected.size >= limit -> selected
    else -> selected + wordId
}
