package com.example.cet6vocabulary.data.repository

import com.example.cet6vocabulary.data.local.dao.LearningRecordDao
import com.example.cet6vocabulary.data.local.entity.toEntity
import com.example.cet6vocabulary.data.local.entity.toModel
import com.example.cet6vocabulary.data.model.LearningRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class LearningRecordRepository(private val dao: LearningRecordDao) {
    private val writeMutex = Mutex()

    suspend fun getRecord(wordId: Int): LearningRecord? = withContext(Dispatchers.IO) {
        dao.getByWordId(wordId)?.toModel()
    }

    suspend fun getAllRecords(): List<LearningRecord> = withContext(Dispatchers.IO) {
        dao.getAll().map { it.toModel() }
    }

    suspend fun recordCorrect(wordId: Int): LearningRecord = withContext(Dispatchers.IO) {
        writeMutex.withLock { record(wordId, correct = true) }
    }

    suspend fun recordWrong(wordId: Int): LearningRecord = withContext(Dispatchers.IO) {
        writeMutex.withLock { record(wordId, correct = false) }
    }

    suspend fun clearRecord(wordId: Int) = withContext(Dispatchers.IO) {
        writeMutex.withLock { dao.deleteByWordId(wordId) }
    }

    private suspend fun record(wordId: Int, correct: Boolean): LearningRecord {
        val existing = dao.getByWordId(wordId)?.toModel() ?: LearningRecord(wordId = wordId)
        val updated = existing.copy(
            spellCount = existing.spellCount + 1,
            correctCount = existing.correctCount + if (correct) 1 else 0,
            wrongCount = existing.wrongCount + if (correct) 0 else 1,
            mastery = when {
                !correct -> 1
                existing.correctCount + 1 >= 3 -> 3
                else -> 2
            },
            lastStudyTime = System.currentTimeMillis(),
            nextReviewTime = null
        )
        dao.insert(updated.toEntity())
        return updated
    }
}
