package com.example.cet6vocabulary.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.cet6vocabulary.data.local.entity.WordBookEntity

@Dao
interface WordBookDao {
    @Query("SELECT * FROM word_book ORDER BY addedTime DESC")
    suspend fun getAll(): List<WordBookEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM word_book WHERE wordId = :wordId)")
    suspend fun isInWordBook(wordId: Int): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(record: WordBookEntity)

    @Query("DELETE FROM word_book WHERE wordId = :wordId")
    suspend fun deleteByWordId(wordId: Int)

    @Query("SELECT COUNT(*) FROM word_book")
    suspend fun count(): Int
}
