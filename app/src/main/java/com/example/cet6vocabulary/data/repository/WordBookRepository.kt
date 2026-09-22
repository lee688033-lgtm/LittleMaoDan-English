package com.example.cet6vocabulary.data.repository

import com.example.cet6vocabulary.data.local.dao.WordBookDao
import com.example.cet6vocabulary.data.local.entity.WordBookEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class WordBookRepository(private val dao: WordBookDao) {
    private val writeMutex = Mutex()

    suspend fun addWord(wordId: Int) = withContext(Dispatchers.IO) {
        writeMutex.withLock { dao.insert(WordBookEntity(wordId, System.currentTimeMillis())) }
    }

    suspend fun removeWord(wordId: Int) = withContext(Dispatchers.IO) {
        writeMutex.withLock { dao.deleteByWordId(wordId) }
    }

    suspend fun isInWordBook(wordId: Int): Boolean = withContext(Dispatchers.IO) { dao.isInWordBook(wordId) }

    suspend fun getAllWordBookRecords(): List<WordBookEntity> = withContext(Dispatchers.IO) { dao.getAll() }

    suspend fun getWordBookWordIds(): List<Int> = getAllWordBookRecords().map { it.wordId }

    suspend fun getWordBookCount(): Int = withContext(Dispatchers.IO) { dao.count() }
}
