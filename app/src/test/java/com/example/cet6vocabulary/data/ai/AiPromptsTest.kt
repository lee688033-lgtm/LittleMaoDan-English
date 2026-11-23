package com.example.cet6vocabulary.data.ai

import com.example.cet6vocabulary.data.model.Word
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AiPromptsTest {

    @Test
    fun parsesFencedLookupJson() {
        val raw = """
            ```json
            {
              "word": "serendipity",
              "phonetic": "/ˌserənˈdɪpəti/",
              "partOfSpeech": "n.",
              "meanings": ["意外发现珍奇事物的能力"],
              "examples": [{"en": "A fortunate serendipity.", "zh": "一次幸运的意外发现。"}],
              "collocations": ["pure serendipity"]
            }
            ```
        """.trimIndent()

        val info = parseWordLookup(raw, "serendipity")

        assertEquals("serendipity", info.word)
        assertEquals("n.", info.partOfSpeech)
        assertEquals(listOf("意外发现珍奇事物的能力"), info.meanings)
        assertEquals("A fortunate serendipity.", info.examples.single().en)
        assertEquals("一次幸运的意外发现。", info.examples.single().zh)
        assertEquals(listOf("pure serendipity"), info.collocations)
    }

    @Test
    fun ignoresProseAroundJson() {
        val raw = """好的，这是结果：{"word":"latent","meanings":["潜在的"]} 希望有帮助。"""
        val info = parseWordLookup(raw, "latent")
        assertEquals("latent", info.word)
        assertEquals(listOf("潜在的"), info.meanings)
    }

    @Test
    fun fallsBackToSingleMeaningAndPlainExampleStrings() {
        val raw = """{"word":"latent","meaning":"adj. 潜在的","examples":["Latent heat is real."]}"""
        val info = parseWordLookup(raw, "latent")
        assertEquals(listOf("adj. 潜在的"), info.meanings)
        assertEquals("Latent heat is real.", info.examples.single().en)
        assertEquals("", info.examples.single().zh)
    }

    @Test
    fun missingWordFallsBackToQuery() {
        val info = parseWordLookup("""{"meanings":["释义"]}""", "quirk")
        assertEquals("quirk", info.word)
    }

    @Test(expected = AiException::class)
    fun rejectsResultWithoutMeaning() {
        parseWordLookup("""{"word":"x","phonetic":"/x/"}""", "x")
    }

    @Test(expected = AiException::class)
    fun rejectsNonJson() {
        parseWordLookup("抱歉，我无法回答", "x")
    }

    @Test
    fun parsesArticleWithAlignedTranslations() {
        val article = parseArticle(
            """{"title":"Campus Life","paragraphs":["One.","Two."],"translations":["第一段。","第二段。"]}"""
        )
        assertEquals("Campus Life", article.title)
        assertEquals(listOf("One.", "Two."), article.paragraphs)
        assertEquals(listOf("第一段。", "第二段。"), article.translations)
        assertTrue(article.hasAlignedTranslations)
        assertEquals("第二段。", article.translationFor(1))
    }

    @Test
    fun fallsBackToSingleTranslationString() {
        val article = parseArticle(
            """{"title":"T","paragraphs":["Only one."],"translation":"只有一段中文"}"""
        )
        assertEquals(listOf("只有一段中文"), article.translations)
        assertEquals("只有一段中文", article.translationFor(0))
    }

    @Test
    fun doesNotForceUnalignedTranslationOntoParagraphs() {
        val article = parseArticle(
            """{"title":"T","paragraphs":["One.","Two."],"translation":"只有一段翻译"}"""
        )
        assertEquals(listOf("只有一段翻译"), article.translations)
        assertFalse(article.hasAlignedTranslations)
        assertNull(article.translationFor(0))
    }

    @Test
    fun parsesArticleFromSingleBodyField() {
        val article = parseArticle(
            """{"title":"T","content":"First paragraph.\n\nSecond paragraph.","translation":"译文"}"""
        )
        assertEquals(listOf("First paragraph.", "Second paragraph."), article.paragraphs)
        assertEquals(listOf("译文"), article.translations)
    }

    @Test
    fun defaultsArticleTitle() {
        val article = parseArticle("""{"paragraphs":["Body."]}""")
        assertEquals("复习短文", article.title)
        assertTrue(article.translations.isEmpty())
    }

    @Test(expected = AiException::class)
    fun rejectsArticleWithoutBody() {
        parseArticle("""{"title":"Empty"}""")
    }

    @Test
    fun lookupPromptCarriesWordAndLocalMeaning() {
        val messages = buildWordLookupMessages("  latent  ", "adj. 潜在的")
        val user = messages.last().content
        assertTrue(user.contains("latent"))
        assertTrue(user.contains("词库里已有的释义是「adj. 潜在的」"))
        assertTrue(user.contains("\"collocations\""))
    }

    @Test
    fun articlePromptListsEveryWord() {
        val words = listOf(
            Word(1, "forehead", "/ˈfɔːhed/", "n.", "前额"),
            Word(2, "latent", "/ˈleɪtnt/", "adj.", "潜在的；隐藏的")
        )
        val user = buildArticleMessages(words).last().content
        assertTrue(user.contains("- forehead (n. 前额)"))
        assertTrue(user.contains("- latent (adj. 潜在的)"))
        assertTrue(user.contains("180-220"))
    }

    @Test
    fun findsWholeWordsAndInflectionsIgnoringCase() {
        val text = "The Forehead is high. This forehead never aches, but foreheads do."
        val ranges = findWordRanges(text, listOf("forehead"))
        assertEquals(3, ranges.size)
        assertEquals("Forehead", text.substring(ranges[0].first, ranges[0].last + 1))
        assertEquals("forehead", text.substring(ranges[1].first, ranges[1].last + 1))
        assertEquals("foreheads", text.substring(ranges[2].first, ranges[2].last + 1))
    }

    @Test
    fun matchesIrregularInflectionsOfWordsEndingInY() {
        val text = "She studies and studied, then she was studying."
        val ranges = findWordRanges(text, listOf("study"))
        assertEquals(3, ranges.size)
    }

    @Test
    fun doesNotMatchInsideLongerWords() {
        assertTrue(findWordRanges("foreheadx", listOf("forehead")).isEmpty())
    }

    @Test
    fun countsWordsForTheArticleHeader() {
        val article = parseArticle("""{"paragraphs":["one two","three four five six"]}""")
        assertEquals(6, article.wordCount)
    }

    @Test
    fun dropsOverlappingRanges() {
        val ranges = findWordRanges("forehead", listOf("head", "forehead"))
        assertEquals(1, ranges.size)
        assertEquals(0, ranges.single().first)
        assertEquals(7, ranges.single().last)
    }

    @Test
    fun escapesRegexCharacters() {
        val ranges = findWordRanges("a (test) here", listOf("(test)"))
        assertEquals(1, ranges.size)
    }

    @Test
    fun emptyInputsYieldNoRanges() {
        assertTrue(findWordRanges("", listOf("x")).isEmpty())
        assertTrue(findWordRanges("text", emptyList()).isEmpty())
        assertTrue(findWordRanges("text", listOf(" , ")).isEmpty())
    }
}
