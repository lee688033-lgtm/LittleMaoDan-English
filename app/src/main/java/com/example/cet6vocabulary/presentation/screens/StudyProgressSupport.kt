package com.example.cet6vocabulary.presentation.screens

import com.example.cet6vocabulary.data.local.entity.StudyProgressEntity
import org.json.JSONArray

internal object StudyProgressKeys {
    const val NORMAL_SEQUENTIAL = "normal_sequential"
    const val NORMAL_RANDOM = "normal_random"
    const val WORDBOOK_SEQUENTIAL = "wordbook_sequential"
    const val WORDBOOK_RANDOM = "wordbook_random"
    const val SPELLING_SEQUENTIAL = "spelling_sequential"
    const val SPELLING_RANDOM = "spelling_random"

    fun forMode(wordBookMode: Boolean, random: Boolean): String = when {
        wordBookMode && random -> WORDBOOK_RANDOM
        wordBookMode -> WORDBOOK_SEQUENTIAL
        random -> NORMAL_RANDOM
        else -> NORMAL_SEQUENTIAL
    }
}

internal data class RestoredStudyProgress(
    val currentIndex: Int,
    val randomOrder: List<Int>,
    val needsSave: Boolean
)

internal fun restoreStudyProgress(
    wordIds: List<Int>,
    progress: StudyProgressEntity?,
    random: Boolean,
    shuffle: (List<Int>) -> List<Int> = { it.shuffled() }
): RestoredStudyProgress {
    if (wordIds.isEmpty()) return RestoredStudyProgress(0, emptyList(), false)
    if (!random) {
        val index = (progress?.currentIndex ?: 0).coerceIn(0, wordIds.size)
        return RestoredStudyProgress(index, emptyList(), progress != null && index != progress.currentIndex)
    }

    val available = wordIds.toSet()
    val parsed = parseRandomOrder(progress?.randomOrder)
    val retained = parsed.orEmpty().filter { it in available }.distinct()
    val missing = wordIds.filterNot { it in retained }
    val order = if (retained.isEmpty()) shuffle(wordIds) else retained + shuffle(missing)
    val index = (progress?.currentIndex ?: 0).coerceIn(0, order.size)
    return RestoredStudyProgress(
        currentIndex = index,
        randomOrder = order,
        needsSave = progress == null || parsed == null || order != parsed || index != progress.currentIndex
    )
}

internal fun encodeRandomOrder(ids: List<Int>): String = JSONArray(ids).toString()

internal fun parseRandomOrder(value: String?): List<Int>? = runCatching {
    if (value.isNullOrBlank()) return@runCatching null
    val array = JSONArray(value)
    List(array.length()) { index -> array.getInt(index) }
}.getOrNull()
