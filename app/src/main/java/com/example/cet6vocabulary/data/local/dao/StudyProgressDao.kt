package com.example.cet6vocabulary.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.cet6vocabulary.data.local.entity.StudyProgressEntity

@Dao
interface StudyProgressDao {
    @Query("SELECT * FROM study_progress WHERE progressKey = :progressKey")
    suspend fun getProgress(progressKey: String): StudyProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(progress: StudyProgressEntity)

    @Query("DELETE FROM study_progress WHERE progressKey = :progressKey")
    suspend fun deleteByKey(progressKey: String)

    @Query("DELETE FROM study_progress")
    suspend fun deleteAll()
}
