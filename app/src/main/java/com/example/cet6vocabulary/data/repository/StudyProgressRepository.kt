package com.example.cet6vocabulary.data.repository

import com.example.cet6vocabulary.data.local.dao.StudyProgressDao
import com.example.cet6vocabulary.data.local.entity.StudyProgressEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class StudyProgressRepository(private val dao: StudyProgressDao) {
    private val writeMutex = Mutex()

    suspend fun getProgress(progressKey: String): StudyProgressEntity? = withContext(Dispatchers.IO) {
        dao.getProgress(progressKey)
    }

    suspend fun saveProgress(progress: StudyProgressEntity) = withContext(Dispatchers.IO) {
        writeMutex.withLock { dao.insertOrReplace(progress) }
    }

    suspend fun deleteProgress(progressKey: String) = withContext(Dispatchers.IO) {
        writeMutex.withLock { dao.deleteByKey(progressKey) }
    }

    suspend fun clearAllProgress() = withContext(Dispatchers.IO) {
        writeMutex.withLock { dao.deleteAll() }
    }
}
