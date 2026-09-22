package com.example.cet6vocabulary.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "study_progress")
data class StudyProgressEntity(
    @PrimaryKey val progressKey: String,
    val currentIndex: Int,
    val randomOrder: String?,
    val updatedTime: Long
)
