package com.example.cet6vocabulary.data.ai

import com.example.cet6vocabulary.data.model.Word
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

data class AiWordExample(val en: String, val zh: String)

data class AiWordInfo(
    val word: String,
    val phonetic: String,
    val partOfSpeech: String,
    val meanings: List<String>,
    val examples: List<AiWordExample>,
    val collocations: List<String>
)

data class AiArticle(
    val title: String,
    val paragraphs: List<String>,
    val translations: List<String>
) {
    /**
     * One Chinese translation per paragraph. A single blob of text cannot say which paragraph it
     * belongs to, and models happily translate only the last one, so the prompt asks for an array
     * and the UI pairs entries by index.
     */
    val hasAlignedTranslations: Boolean
        get() = translations.size == paragraphs.size && translations.any { it.isNotBlank() }

    fun translationFor(index: Int): String? =
        if (hasAlignedTranslations) translations.getOrNull(index)?.takeIf { it.isNotBlank() } else null

    /** Shown next to the article so a model that ignores the 180-220 word request is visible. */
    val wordCount: Int
        get() = paragraphs.sumOf { paragraph ->
            paragraph.split(Regex("\\s+")).count { it.isNotBlank() }
        }
}

private const val LOOKUP_SYSTEM = "你是 CET-6 备考的英汉词典助手。释义一律使用简体中文，" +
    "英文只用于例句与搭配。只输出一个 JSON 对象，不要输出解释文字，不要使用 markdown 代码块。"

private const val ARTICLE_SYSTEM = "你是英语写作老师，为备考 CET-6 的学生写复习短文。" +
    "只输出一个 JSON 对象，不要输出解释文字，不要使用 markdown 代码块。"

/**
 * Prompt for a word that is not in the bundled wordbook. [localMeaning] is the wordbook entry
 * when there is one, so the model extends the app's own definition instead of contradicting it.
 */
fun buildWordLookupMessages(input: String, localMeaning: String? = null): List<ChatMessage> {
    val word = input.trim()
    require(word.isNotEmpty()) { "empty word" }
    val context = localMeaning?.takeIf { it.isNotBlank() }
        ?.let { "词库里已有的释义是「$it」，请在此基础上补全，不要与之冲突。" }
        .orEmpty()
    val user = buildString {
        append("查询英文单词「").append(word).append("」。\n")
        if (context.isNotEmpty()) append(context).append('\n')
        append(
            """
            JSON 结构：
            {
              "word": "单词原形",
              "phonetic": "国际音标，例如 /ˈfɔːhed/",
              "partOfSpeech": "词性缩写，例如 n.; v.",
              "meanings": ["n. 中文释义1", "v. 中文释义2"],
              "examples": [{"en": "英文例句", "zh": "中文翻译"}],
              "collocations": ["常见搭配1", "常见搭配2"]
            }
            要求：
            1. meanings 给 1-3 条，每条都是简体中文，并以词性开头（例如「n. 意外发现珍奇事物的能力」）。
               不要输出英文解释，不要照抄英英词典的释义。
            2. examples 给 2 条，en 是英文句子且难度接近 CET-6，zh 必须是对应的中文翻译。
            3. collocations 给 2-4 个英文搭配。
            4. 如果输入不是英文单词，word 原样返回，meanings 用中文写明无法识别。
            """.trimIndent()
        )
    }
    return listOf(ChatMessage("system", LOOKUP_SYSTEM), ChatMessage("user", user))
}

/**
 * Prompt for the review article. Only the English words and their first meaning are sent: the
 * article must reuse what the learner actually practised.
 */
fun buildArticleMessages(words: List<Word>): List<ChatMessage> {
    require(words.isNotEmpty()) { "empty word list" }
    val vocabulary = words.joinToString(separator = "\n") { word ->
        val meaning = word.meaning.substringBefore('；').substringBefore(';').trim()
        "- ${word.word} (${word.partOfSpeech.trim()} $meaning)".trim()
    }
    val user = buildString {
        append("请用下面这些学生学过的 CET-6 单词写一篇英语复习短文。\n")
        append(vocabulary).append('\n')
        append(
            """
            JSON 结构：
            {
              "title": "英文标题",
              "paragraphs": ["第一段英文", "第二段英文"],
              "translations": ["第一段中文翻译", "第二段中文翻译"]
            }
            要求：
            1. 短文 180-220 词，分 2-4 段，每段只写一段文字，不要换行。
            2. 上面每个单词至少出现一次，可以变化时态、单复数或词性。
            3. 内容连贯，像一篇校园、科技或社会话题的小短文，不要把单词逐个罗列。
            4. 句式与用词贴近 CET-6 阅读难度，不要出现超纲的生僻词。
            5. translations 与 paragraphs 一一对应，数量必须相同：第 N 条是第 N 段的中文翻译，
               每段都要翻译，不能只翻最后一段，也不要合并成一段。
            """.trimIndent()
        )
    }
    return listOf(ChatMessage("system", ARTICLE_SYSTEM), ChatMessage("user", user))
}

/** Strips markdown fences and surrounding prose; models add both even when told not to. */
fun extractJsonObject(raw: String): String {
    var text = raw.trim()
    if (text.startsWith("```")) {
        text = text.removePrefix("```json").removePrefix("```JSON").removePrefix("```").trimStart()
        val closing = text.lastIndexOf("```")
        if (closing >= 0) text = text.substring(0, closing)
    }
    val start = text.indexOf('{')
    val end = text.lastIndexOf('}')
    if (start < 0 || end <= start) throw AiException("模型返回的内容不是 JSON：${raw.take(80)}")
    return text.substring(start, end + 1)
}

fun parseWordLookup(raw: String, fallbackWord: String = ""): AiWordInfo {
    val json = jsonObjectOf(raw)
    val word = json.optString("word").trim().ifBlank { fallbackWord }
    val meanings = json.optJSONArray("meanings").stringList().ifEmpty {
        listOfNotNull(json.optString("meaning").trim().takeIf { it.isNotEmpty() })
    }
    if (meanings.isEmpty()) throw AiException("模型没有返回释义，请重试")
    return AiWordInfo(
        word = word,
        phonetic = json.optString("phonetic").trim(),
        partOfSpeech = json.optString("partOfSpeech").trim(),
        meanings = meanings,
        examples = json.examples(),
        collocations = json.optJSONArray("collocations").stringList()
    )
}

fun parseArticle(raw: String): AiArticle {
    val json = jsonObjectOf(raw)
    val paragraphs = json.optJSONArray("paragraphs").stringList().ifEmpty {
        val body = listOf("article", "content", "text")
            .firstNotNullOfOrNull { key -> json.optString(key).trim().takeIf { it.isNotEmpty() } }
            .orEmpty()
        body.split(Regex("\\n\\s*\\n")).map { it.trim() }.filter { it.isNotEmpty() }
    }
    if (paragraphs.isEmpty()) throw AiException("模型没有返回短文内容，请重试")
    // Older/looser models may answer with a single translation string; keep that usable by
    // splitting on blank lines, and let the UI fall back to one block when the counts differ.
    val translations = json.optJSONArray("translations").stringList().ifEmpty {
        listOf("translation", "zh", "chinese")
            .firstNotNullOfOrNull { key -> json.optString(key).trim().takeIf { it.isNotEmpty() } }
            .orEmpty()
            .split(Regex("\\n\\s*\\n"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }
    return AiArticle(
        title = json.optString("title").trim().ifBlank { "复习短文" },
        paragraphs = paragraphs,
        translations = translations
    )
}

/**
 * Character ranges of [words] inside [text], ignoring case and matching whole words. Inflected
 * forms count as the same word (`finger` matches `fingers`), because the article prompt allows the
 * model to change tense or number. Overlaps are dropped so a word containing another one is
 * highlighted once.
 */
fun findWordRanges(text: String, words: List<String>): List<IntRange> {
    if (text.isEmpty() || words.isEmpty()) return emptyList()
    val found = ArrayList<IntRange>()
    words.map { it.trim() }.filter { it.isNotEmpty() }.distinct().forEach { word ->
        // A word boundary only exists next to a word character; wrapping "(test)" in \b would
        // never match, so the boundary is added per side.
        val prefix = if (word.first().isLetterOrDigit()) "\\b" else ""
        val suffix = if (word.last().isLetterOrDigit()) "\\b" else ""
        val stems = buildList {
            add(Regex.escape(word))
            if (word.length > 2 && word.endsWith("y", ignoreCase = true)) {
                add(Regex.escape(word.dropLast(1)) + "(?:ies|ied|ying|ier|iest)")
            }
        }
        val pattern = Regex(
            prefix + "(?:" + stems.joinToString("|") + ")(?:s|es|ed|d|ing|er|est)?" + suffix,
            RegexOption.IGNORE_CASE
        )
        pattern.findAll(text).forEach { found.add(it.range) }
    }
    if (found.isEmpty()) return emptyList()
    val sorted = found.sortedWith(compareBy<IntRange> { it.first }.thenByDescending { it.last })
    val merged = ArrayList<IntRange>()
    sorted.forEach { range ->
        val last = merged.lastOrNull()
        if (last == null || range.first > last.last) merged.add(range)
    }
    return merged
}

private fun jsonObjectOf(raw: String): JSONObject =
    try {
        JSONObject(extractJsonObject(raw))
    } catch (error: JSONException) {
        throw AiException("模型返回的 JSON 无法解析，请重试", error)
    }

private fun JSONArray?.stringList(): List<String> {
    val array = this ?: return emptyList()
    return (0 until array.length()).mapNotNull { index ->
        when (val item = array.opt(index)) {
            is JSONObject -> item.optString("text").trim().takeIf { it.isNotEmpty() }
            else -> array.optString(index).trim().takeIf { it.isNotEmpty() }
        }
    }
}

private fun JSONObject.examples(): List<AiWordExample> {
    val array = optJSONArray("examples") ?: return emptyList()
    return (0 until array.length()).mapNotNull { index ->
        when (val item = array.opt(index)) {
            is JSONObject -> {
                val en = item.optString("en").trim()
                val zh = item.optString("zh").trim()
                if (en.isBlank() && zh.isBlank()) null else AiWordExample(en, zh)
            }
            is String -> item.trim().takeIf { it.isNotEmpty() }?.let { AiWordExample(it, "") }
            else -> null
        }
    }
}
