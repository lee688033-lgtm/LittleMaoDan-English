package com.example.cet6vocabulary.data.repository

import android.content.Context
import com.example.cet6vocabulary.data.model.Word
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray

class WordRepository(context: Context) {
    private val assets = context.applicationContext.assets
    private val loadMutex = Mutex()

    @Volatile
    private var loadedWords: List<Word>? = null

    @Volatile
    private var wordsById: Map<Int, Word> = emptyMap()

    suspend fun getAllWords(): List<Word> {
        loadedWords?.let { return it }
        return loadMutex.withLock {
            loadedWords ?: withContext(Dispatchers.IO) { loadWords() }.also { words ->
                wordsById = words.associateBy { it.id }
                loadedWords = words
            }
        }
    }

    suspend fun getWordById(id: Int): Word? {
        getAllWords()
        return wordsById[id]
    }

    suspend fun searchWords(keyword: String): List<Word> {
        val query = keyword.trim()
        if (query.isEmpty()) return emptyList()
        return getAllWords().filter {
            it.word.contains(query, ignoreCase = true) || it.meaning.contains(query, ignoreCase = true)
        }
    }

    private fun loadWords(): List<Word> {
        val assetFiles = assets.list(ASSET_DIRECTORY).orEmpty().filter { it.endsWith(".json") }.sorted()
        require(assetFiles.isNotEmpty()) { "No JSON vocabulary files found in assets/$ASSET_DIRECTORY" }
        val loaded = assetFiles.flatMap { fileName ->
            val json = assets.open("$ASSET_DIRECTORY/$fileName").bufferedReader(Charsets.UTF_8).use { it.readText() }
            parseFile(fileName, JSONArray(json))
        }
        val invalidIds = loaded.filter { it.id < 1 }.map { it.id }
        require(invalidIds.isEmpty()) { "Invalid word id(s): ${invalidIds.joinToString()}" }
        val duplicates = loaded.groupBy { it.id }.filterValues { it.size > 1 }.keys
        require(duplicates.isEmpty()) { "Duplicate word id(s): ${duplicates.joinToString()}" }
        return loaded.sortedBy { it.id }
    }

    private fun parseFile(fileName: String, array: JSONArray): List<Word> = (0 until array.length()).map { index ->
        val item = array.optJSONObject(index) ?: error("$fileName: item $index is not an object")
        fun required(name: String): String = item.optString(name).trim().also { require(it.isNotEmpty()) { "$fileName: item $index has missing or empty $name" } }
        Word(item.optInt("id", 0), required("word"), item.optString("phonetic"), item.optString("partOfSpeech"), required("meaning"))
    }

    private companion object { const val ASSET_DIRECTORY = "cet6" }
}
