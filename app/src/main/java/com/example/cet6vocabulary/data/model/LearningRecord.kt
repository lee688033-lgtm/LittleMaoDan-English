package com.example.cet6vocabulary.data.model

data class LearningRecord(
    val wordId: Int,
    val spellCount: Int = 0,
    val correctCount: Int = 0,
    val wrongCount: Int = 0,
    val mastery: Int = 0,
    val lastStudyTime: Long? = null,
    val nextReviewTime: Long? = null
)
