package com.example.cet6vocabulary.data.ai

import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import org.json.JSONObject

data class ChatMessage(val role: String, val content: String)

/** Every failure the UI has to explain to the learner travels through this one type. */
class AiException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Minimal OpenAI-compatible chat client. It deliberately uses [HttpURLConnection] instead of a
 * networking library: the app sends one small request at a time, and keeping this class free of
 * Android types means the whole transport can be exercised from a JVM unit test.
 */
class AiClient(
    private val baseUrl: String,
    private val apiKey: String,
    private val model: String,
    private val connectTimeoutMillis: Int = 15_000,
    private val readTimeoutMillis: Int = 90_000
) {
    fun chat(messages: List<ChatMessage>, temperature: Double = 0.6): String {
        val payload = JSONObject().apply {
            put("model", model)
            put("temperature", temperature)
            put("stream", false)
            put(
                "messages",
                org.json.JSONArray().apply {
                    messages.forEach { message ->
                        put(
                            JSONObject().apply {
                                put("role", message.role)
                                put("content", message.content)
                            }
                        )
                    }
                }
            )
        }.toString()

        val connection = (URL(chatCompletionsUrl(baseUrl)).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = connectTimeoutMillis
            readTimeout = readTimeoutMillis
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Authorization", "Bearer $apiKey")
        }
        try {
            connection.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
            val code = connection.responseCode
            val body = (if (code in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader(Charsets.UTF_8)
                ?.use { it.readText() }
                .orEmpty()
            if (code !in 200..299) throw AiException(httpErrorMessage(code, body))
            return contentOf(body)
        } catch (error: AiException) {
            throw error
        } catch (error: SocketTimeoutException) {
            throw AiException("请求超时，请检查网络后重试", error)
        } catch (error: IOException) {
            throw AiException("网络请求失败：${error.message ?: error.javaClass.simpleName}", error)
        } finally {
            connection.disconnect()
        }
    }
}

/**
 * Accepts `https://host/v1`, `https://host/v1/` and a full `.../chat/completions` endpoint,
 * because learners copy whatever the provider's console shows them.
 */
fun chatCompletionsUrl(baseUrl: String): String {
    val trimmed = baseUrl.trim().trimEnd('/')
    require(trimmed.isNotEmpty()) { "empty base url" }
    return if (trimmed.endsWith("/chat/completions")) trimmed else "$trimmed/chat/completions"
}

fun contentOf(body: String): String {
    val json = runCatching { JSONObject(body) }
        .getOrElse { throw AiException("服务返回的内容无法解析：${body.take(120)}") }
    json.optJSONArray("choices")
        ?.optJSONObject(0)
        ?.optJSONObject("message")
        ?.optString("content")
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
        ?.let { return it }
    val detail = json.optJSONObject("error")?.optString("message").orEmpty()
    throw AiException(
        if (detail.isNotBlank()) "服务返回错误：${detail.take(160)}"
        else "服务没有返回内容：${body.take(120)}"
    )
}

fun httpErrorMessage(code: Int, body: String): String {
    val detail = runCatching { JSONObject(body).optJSONObject("error")?.optString("message") }
        .getOrNull()
        ?.trim()
        .orEmpty()
        .take(120)
    val base = when (code) {
        401, 403 -> "API Key 无效或没有访问权限（$code）"
        404 -> "接口地址不存在（404），请检查设置里的接口地址"
        429 -> "请求过于频繁或免费额度已用尽（429）"
        in 500..599 -> "服务暂时不可用（$code），请稍后重试"
        else -> "请求失败（$code）"
    }
    return if (detail.isBlank()) base else "$base：$detail"
}
