package com.example.cet6vocabulary.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.cet6vocabulary.data.model.LearningRecord

@Entity(tableName = "learning_records")
data class LearningRecordEntity(
    @PrimaryKey val wordId: Int,
    val spellCount: Int,
    val correctCount: Int,
    val wrongCount: Int,
    val mastery: Int,
    val lastStudyTime: Long?,
    val nextReviewTime: Long?
)

fun LearningRecordEntity.toModel() = LearningRecord(
    wordId = wordId,
    spellCount = spellCount,
    correctCount = correctCount,
    wrongCount = wrongCount,
    mastery = mastery,
    lastStudyTime = lastStudyTime,
    nextReviewTime = nextReviewTime
)

fun LearningRecord.toEntity() = LearningRecordEntity(
    wordId = wordId,
    spellCount = spellCount,
    correctCount = correctCount,
    wrongCount = wrongCount,
    mastery = mastery,
    lastStudyTime = lastStudyTime,
    nextReviewTime = nextReviewTime
)
