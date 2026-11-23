package com.example.cet6vocabulary.data.repository

import com.example.cet6vocabulary.data.local.dao.LearningRecordDao
import com.example.cet6vocabulary.data.local.dao.StudyProgressDao
import com.example.cet6vocabulary.data.local.dao.WordBookDao
import com.example.cet6vocabulary.data.local.entity.LearningRecordEntity
import com.example.cet6vocabulary.data.local.entity.StudyProgressEntity
import com.example.cet6vocabulary.data.local.entity.WordBookEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/** Everything the learner has produced locally: practice records, bookmarks and reading positions. */
data class LearningDataSnapshot(
    val records: List<LearningRecordEntity>,
    val wordBook: List<WordBookEntity>,
    val studyProgress: List<StudyProgressEntity>
) {
    val isEmpty: Boolean
        get() = records.isEmpty() && wordBook.isEmpty() && studyProgress.isEmpty()
}

class LearningDataFormatException(message: String) : Exception(message)

/**
 * 导出格式是显式的、带版本号的 JSON，字段名与 Room 列一一对应。
 * 只包含用户产生的数据，不包含词库本身（词库随安装包提供，导入时不应依赖它的快照）。
 */
object LearningDataJson {
    const val FORMAT = "cet6-learning"
    const val VERSION = 1

    private const val KEY_FORMAT = "format"
    private const val KEY_VERSION = "version"
    private const val KEY_EXPORTED_AT = "exportedAt"
    private const val KEY_RECORDS = "records"
    private const val KEY_WORD_BOOK = "wordBook"
    private const val KEY_PROGRESS = "studyProgress"

    fun encode(snapshot: LearningDataSnapshot, exportedAt: Long): String {
        val root = JSONObject()
        root.put(KEY_FORMAT, FORMAT)
        root.put(KEY_VERSION, VERSION)
        root.put(KEY_EXPORTED_AT, exportedAt)
        root.put(KEY_RECORDS, JSONArray().apply {
            snapshot.records.forEach { record ->
                put(JSONObject().apply {
                    put("wordId", record.wordId)
                    put("spellCount", record.spellCount)
                    put("correctCount", record.correctCount)
                    put("wrongCount", record.wrongCount)
                    put("mastery", record.mastery)
                    putNullable("lastStudyTime", record.lastStudyTime)
                    putNullable("nextReviewTime", record.nextReviewTime)
                })
            }
        })
        root.put(KEY_WORD_BOOK, JSONArray().apply {
            snapshot.wordBook.forEach { entry ->
                put(JSONObject().apply {
                    put("wordId", entry.wordId)
                    put("addedTime", entry.addedTime)
                })
            }
        })
        root.put(KEY_PROGRESS, JSONArray().apply {
            snapshot.studyProgress.forEach { entry ->
                put(JSONObject().apply {
                    put("progressKey", entry.progressKey)
                    put("currentIndex", entry.currentIndex)
                    putNullable("randomOrder", entry.randomOrder)
                    put("updatedTime", entry.updatedTime)
                })
            }
        })
        return root.toString(2)
    }

    fun decode(text: String): LearningDataSnapshot {
        val root = try {
            JSONObject(text)
        } catch (_: Exception) {
            throw LearningDataFormatException("文件不是有效的 JSON")
        }
        if (root.optString(KEY_FORMAT) != FORMAT) {
            throw LearningDataFormatException("不是本应用导出的学习数据")
        }
        val version = root.optInt(KEY_VERSION, 0)
        if (version <= 0 || version > VERSION) {
            throw LearningDataFormatException("不支持的数据版本：$version")
        }

        val records = root.optJSONArray(KEY_RECORDS).mapObjects { item ->
            val wordId = item.requireInt("wordId")
            if (wordId <= 0) throw LearningDataFormatException("学习记录里的 wordId 无效")
            LearningRecordEntity(
                wordId = wordId,
                spellCount = item.requireInt("spellCount"),
                correctCount = item.requireInt("correctCount"),
                wrongCount = item.requireInt("wrongCount"),
                mastery = item.requireInt("mastery"),
                lastStudyTime = item.optNullableLong("lastStudyTime"),
                nextReviewTime = item.optNullableLong("nextReviewTime")
            )
        }

        val wordBook = root.optJSONArray(KEY_WORD_BOOK).mapObjects { item ->
            val wordId = item.requireInt("wordId")
            if (wordId <= 0) throw LearningDataFormatException("单词本里的 wordId 无效")
            WordBookEntity(wordId = wordId, addedTime = item.requireLong("addedTime"))
        }

        val progress = root.optJSONArray(KEY_PROGRESS).mapObjects { item ->
            val key = item.optString("progressKey")
            if (key.isBlank()) throw LearningDataFormatException("学习进度缺少 progressKey")
            StudyProgressEntity(
                progressKey = key,
                currentIndex = item.requireInt("currentIndex"),
                randomOrder = item.optNullableString("randomOrder"),
                updatedTime = item.requireLong("updatedTime")
            )
        }

        return LearningDataSnapshot(records, wordBook, progress)
    }
}

/**
 * 导入是覆盖式：先清空三张表再写入，避免同一 wordId 的旧记录与新数据混在一起。
 * 写入集中在一把互斥锁里，防止与正在进行的练习写入交错。
 */
class LearningDataRepository(
    private val records: LearningRecordDao,
    private val wordBook: WordBookDao,
    private val progress: StudyProgressDao
) {
    private val writeMutex = Mutex()

    suspend fun export(exportedAt: Long): String = withContext(Dispatchers.IO) {
        LearningDataJson.encode(
            LearningDataSnapshot(
                records = records.getAll(),
                wordBook = wordBook.getAll(),
                studyProgress = progress.getAll()
            ),
            exportedAt
        )
    }

    suspend fun replaceAll(snapshot: LearningDataSnapshot) = withContext(Dispatchers.IO) {
        writeMutex.withLock {
            clearInternal()
            snapshot.records.forEach { records.insert(it) }
            snapshot.wordBook.forEach { wordBook.insert(it) }
            snapshot.studyProgress.forEach { progress.insertOrReplace(it) }
        }
    }

    suspend fun clearAll() = withContext(Dispatchers.IO) {
        writeMutex.withLock { clearInternal() }
    }

    private suspend fun clearInternal() {
        records.deleteAll()
        wordBook.deleteAll()
        progress.deleteAll()
    }
}

private fun <T> JSONArray?.mapObjects(transform: (JSONObject) -> T): List<T> {
    if (this == null) return emptyList()
    val result = ArrayList<T>(length())
    for (index in 0 until length()) {
        val item = optJSONObject(index)
            ?: throw LearningDataFormatException("第 ${index + 1} 条数据不是对象")
        result.add(transform(item))
    }
    return result
}

private fun JSONObject.requireInt(key: String): Int {
    if (!has(key)) throw LearningDataFormatException("缺少字段 $key")
    return optInt(key)
}

private fun JSONObject.requireLong(key: String): Long {
    if (!has(key)) throw LearningDataFormatException("缺少字段 $key")
    return optLong(key)
}

private fun JSONObject.optNullableLong(key: String): Long? = if (isNull(key)) null else optLong(key)

private fun JSONObject.optNullableString(key: String): String? = if (isNull(key)) null else optString(key)

private fun JSONObject.putNullable(key: String, value: Any?) {
    put(key, value ?: JSONObject.NULL)
}
