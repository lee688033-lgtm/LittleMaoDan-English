package com.example.cet6vocabulary.data.ai

import java.io.InputStream
import java.net.InetAddress
import java.net.ServerSocket
import kotlin.concurrent.thread
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Exercises the real socket path against a local server; no network access is needed. The fake
 * server is hand written on [ServerSocket] because the JVM HTTP server class is not part of the
 * android.jar classpath these tests compile against.
 */
class AiClientTest {

    private lateinit var server: FakeHttpServer

    @Before
    fun startServer() {
        server = FakeHttpServer()
    }

    @After
    fun stopServer() {
        server.close()
    }

    private fun client(apiKey: String = "sk-test") =
        AiClient("http://127.0.0.1:${server.port}/v1", apiKey, "glm-4-flash")

    @Test
    fun sendsModelKeyAndMessagesAndReturnsContent() {
        server.respondWith(
            200,
            """{"choices":[{"message":{"role":"assistant","content":"{\"word\":\"x\"}"}}]}"""
        )

        val content = client().chat(listOf(ChatMessage("user", "hi")))

        assertEquals("""{"word":"x"}""", content)
        assertTrue(server.lastRequestLine.startsWith("POST /v1/chat/completions"))
        assertTrue(server.lastBody.contains("\"model\":\"glm-4-flash\""))
        assertTrue(server.lastBody.contains("\"role\":\"user\""))
        assertTrue(server.lastBody.contains("\"content\":\"hi\""))
        assertEquals("Bearer sk-test", server.lastAuthorization)
    }

    @Test
    fun reportsInvalidKeyWithServerDetail() {
        server.respondWith(401, """{"error":{"message":"invalid api key"}}""")
        val error = runCatching { client("bad").chat(listOf(ChatMessage("user", "hi"))) }
            .exceptionOrNull()
        assertTrue(error is AiException)
        assertTrue(error!!.message!!.contains("API Key 无效"))
        assertTrue(error.message!!.contains("invalid api key"))
    }

    @Test
    fun reportsRateLimit() {
        server.respondWith(429, """{"error":{"message":"quota"}}""")
        val error = runCatching { client().chat(listOf(ChatMessage("user", "hi"))) }
            .exceptionOrNull()
        assertTrue(error!!.message!!.contains("429"))
    }

    @Test
    fun failsWhenResponseHasNoChoices() {
        server.respondWith(200, """{"id":"x"}""")
        val error = runCatching { client().chat(listOf(ChatMessage("user", "hi"))) }
            .exceptionOrNull()
        assertTrue(error is AiException)
        assertTrue(error!!.message!!.contains("没有返回内容"))
    }

    @Test
    fun failsWhenBodyIsNotJson() {
        server.respondWith(200, "<html>oops</html>")
        val error = runCatching { client().chat(listOf(ChatMessage("user", "hi"))) }
            .exceptionOrNull()
        assertTrue(error!!.message!!.contains("无法解析"))
    }

    @Test
    fun unreachablePortBecomesFriendlyError() {
        val error = runCatching {
            AiClient("http://127.0.0.1:1/v1", "k", "m", connectTimeoutMillis = 800)
                .chat(listOf(ChatMessage("user", "hi")))
        }.exceptionOrNull()
        assertTrue(error is AiException)
        assertTrue(error!!.message!!.startsWith("网络请求失败"))
    }
}

/** One-shot HTTP server: answers the first request and keeps what it received for assertions. */
private class FakeHttpServer {

    private val socket = ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))
    val port: Int = socket.localPort

    @Volatile
    var lastRequestLine: String = ""
    @Volatile
    var lastBody: String = ""
    @Volatile
    var lastAuthorization: String? = null

    init {
        thread(isDaemon = true, name = "fake-ai-server") {
            runCatching { socket.accept().use { handle(it.getInputStream(), it.getOutputStream()) } }
        }
    }

    fun respondWith(status: Int, body: String) {
        this.status = status
        this.responseBody = body
    }

    @Volatile
    private var status: Int = 200

    @Volatile
    private var responseBody: String = "{}"

    private fun handle(input: InputStream, output: java.io.OutputStream) {
        val head = StringBuilder()
        while (!head.endsWith("\r\n\r\n")) {
            val next = input.read()
            if (next < 0) return
            head.append(next.toChar())
        }
        val lines = head.toString().split("\r\n")
        lastRequestLine = lines.first()
        lines.drop(1).forEach { line ->
            val separator = line.indexOf(':')
            if (separator > 0) {
                val name = line.substring(0, separator).trim().lowercase()
                if (name == "authorization") lastAuthorization = line.substring(separator + 1).trim()
            }
        }
        val contentLength = lines.firstOrNull { it.startsWith("Content-Length:", ignoreCase = true) }
            ?.substringAfter(':')?.trim()?.toIntOrNull() ?: 0
        val body = ByteArray(contentLength)
        var read = 0
        while (read < contentLength) {
            val count = input.read(body, read, contentLength - read)
            if (count < 0) break
            read += count
        }
        lastBody = String(body, 0, read, Charsets.UTF_8)

        val bytes = responseBody.toByteArray(Charsets.UTF_8)
        val responseHead = "HTTP/1.1 $status OK\r\n" +
            "Content-Type: application/json\r\n" +
            "Content-Length: ${bytes.size}\r\n" +
            "Connection: close\r\n\r\n"
        output.write(responseHead.toByteArray(Charsets.UTF_8))
        output.write(bytes)
        output.flush()
    }

    fun close() {
        runCatching { socket.close() }
    }
}

class ChatCompletionsUrlTest {

    @Test
    fun appendsPathToBaseUrl() {
        assertEquals("https://a/v1/chat/completions", chatCompletionsUrl("https://a/v1"))
        assertEquals("https://a/v1/chat/completions", chatCompletionsUrl("https://a/v1/"))
        assertEquals("https://a/v1/chat/completions", chatCompletionsUrl(" https://a/v1 "))
    }

    @Test
    fun keepsFullEndpoint() {
        assertEquals("https://a/v1/chat/completions", chatCompletionsUrl("https://a/v1/chat/completions"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsBlankBaseUrl() {
        chatCompletionsUrl("   ")
    }
}

class HttpErrorTextTest {

    @Test
    fun mapsStatusCodes() {
        assertTrue(httpErrorMessage(403, "").contains("没有访问权限"))
        assertTrue(httpErrorMessage(404, "").contains("接口地址不存在"))
        assertTrue(httpErrorMessage(500, "").contains("暂时不可用"))
        assertTrue(httpErrorMessage(418, "").contains("418"))
    }
}
