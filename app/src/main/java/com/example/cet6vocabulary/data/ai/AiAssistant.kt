package com.example.cet6vocabulary.data.ai

import com.example.cet6vocabulary.data.model.Word
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The two AI features the app offers, on top of [AiClient]. Preferences are re-read on every
 * call, so saving a new key in settings applies without restarting the screen.
 */
class AiAssistant(private val settings: AiSettings) {

    val isConfigured: Boolean get() = settings.isConfigured

    suspend fun lookupWord(input: String, localMeaning: String? = null): AiWordInfo =
        withContext(Dispatchers.IO) {
            val word = input.trim()
            if (word.isEmpty()) throw AiException("请输入要查询的单词")
            val messages = buildWordLookupMessages(word, localMeaning)
            parseWordLookup(client().chat(messages, temperature = 0.2), word)
        }

    suspend fun writeArticle(words: List<Word>): AiArticle = withContext(Dispatchers.IO) {
        if (words.isEmpty()) throw AiException("还没有可以使用的单词")
        parseArticle(client().chat(buildArticleMessages(words), temperature = 0.8))
    }

    suspend fun testConnection(): String = withContext(Dispatchers.IO) {
        client().chat(
            listOf(
                ChatMessage("system", "你是连接测试助手，只回复 OK。"),
                ChatMessage("user", "ping")
            ),
            temperature = 0.0
        )
    }

    private fun client(): AiClient {
        if (!settings.isConfigured) throw AiException("还没有配置 AI 服务，请先到设置填写 API Key")
        return AiClient(settings.baseUrl, settings.apiKey, settings.model)
    }
}
