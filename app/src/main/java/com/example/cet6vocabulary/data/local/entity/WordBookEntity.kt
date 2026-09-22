package com.example.cet6vocabulary.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "word_book")
data class WordBookEntity(
    @PrimaryKey val wordId: Int,
    val addedTime: Long
)
