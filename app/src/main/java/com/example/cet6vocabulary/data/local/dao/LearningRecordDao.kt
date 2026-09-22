package com.example.cet6vocabulary.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.cet6vocabulary.data.local.entity.LearningRecordEntity

@Dao
interface LearningRecordDao {
    @Query("SELECT * FROM learning_records WHERE wordId = :wordId LIMIT 1")
    suspend fun getByWordId(wordId: Int): LearningRecordEntity?

    @Query("SELECT * FROM learning_records ORDER BY wordId")
    suspend fun getAll(): List<LearningRecordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: LearningRecordEntity)

    @Delete
    suspend fun delete(record: LearningRecordEntity)

    @Query("DELETE FROM learning_records WHERE wordId = :wordId")
    suspend fun deleteByWordId(wordId: Int)
}
