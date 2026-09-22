package com.example.cet6vocabulary.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.cet6vocabulary.data.local.dao.LearningRecordDao
import com.example.cet6vocabulary.data.local.dao.StudyProgressDao
import com.example.cet6vocabulary.data.local.dao.WordBookDao
import com.example.cet6vocabulary.data.local.entity.LearningRecordEntity
import com.example.cet6vocabulary.data.local.entity.StudyProgressEntity
import com.example.cet6vocabulary.data.local.entity.WordBookEntity

@Database(entities = [LearningRecordEntity::class, WordBookEntity::class, StudyProgressEntity::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun learningRecordDao(): LearningRecordDao
    abstract fun wordBookDao(): WordBookDao
    abstract fun studyProgressDao(): StudyProgressDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS word_book (wordId INTEGER NOT NULL, addedTime INTEGER NOT NULL, PRIMARY KEY(wordId))")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS study_progress (progressKey TEXT NOT NULL, currentIndex INTEGER NOT NULL, randomOrder TEXT, updatedTime INTEGER NOT NULL, PRIMARY KEY(progressKey))")
            }
        }

        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "cet6_database"
            ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build().also { instance = it }
        }
    }
}
