package com.example.cet6vocabulary.presentation.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.cet6vocabulary.data.ai.AiArticle
import com.example.cet6vocabulary.data.ai.AiAssistant
import com.example.cet6vocabulary.data.ai.AiWordExample
import com.example.cet6vocabulary.data.ai.AiWordInfo
import com.example.cet6vocabulary.data.ai.findWordRanges
import com.example.cet6vocabulary.data.repository.LearningRecordRepository
import com.example.cet6vocabulary.data.repository.WordRepository
import com.example.cet6vocabulary.presentation.model.ArticleCandidate
import com.example.cet6vocabulary.presentation.model.MAX_ARTICLE_WORDS
import com.example.cet6vocabulary.presentation.model.MIN_ARTICLE_WORDS
import com.example.cet6vocabulary.presentation.model.articleCandidates
import com.example.cet6vocabulary.presentation.model.masteryLabel
import com.example.cet6vocabulary.presentation.model.selectedArticleWords
import com.example.cet6vocabulary.presentation.model.toggleArticleWord
import com.example.cet6vocabulary.ui.components.CET6SectionTitle
import com.example.cet6vocabulary.ui.components.MaoDanCard
import com.example.cet6vocabulary.ui.components.MaoDanOutlinedCard
import com.example.cet6vocabulary.ui.components.MaoDanPrimaryButton
import com.example.cet6vocabulary.ui.components.MaoDanTextButton
import com.example.cet6vocabulary.ui.theme.MaoDanDimens
import com.example.cet6vocabulary.ui.theme.MaoDanShapes
import kotlinx.coroutines.launch

private enum class AiMode(val label: String) {
    LOOKUP("AI 查词"),
    ARTICLE("复习短文")
}

// Word and article results survive rotation and process death; both are plain text, so a flat
// string list is enough and no JSON round trip is needed.
private val AiWordInfoSaver = listSaver<AiWordInfo?, String>(
    save = { info ->
        if (info == null) emptyList() else listOf(
            info.word,
            info.phonetic,
            info.partOfSpeech,
            info.meanings.joinToString("\n"),
            info.examples.joinToString("\n") { "${it.en}\u0001${it.zh}" },
            info.collocations.joinToString("\n")
        )
    },
    restore = { saved ->
        if (saved.size < 6) null else AiWordInfo(
            word = saved[0],
            phonetic = saved[1],
            partOfSpeech = saved[2],
            meanings = saved[3].split("\n").filter { it.isNotBlank() },
            examples = saved[4].split("\n").filter { it.isNotBlank() }.map { line ->
                val parts = line.split("\u0001")
                AiWordExample(parts.getOrElse(0) { "" }, parts.getOrElse(1) { "" })
            },
            collocations = saved[5].split("\n").filter { it.isNotBlank() }
        )
    }
)

private val AiArticleSaver = listSaver<AiArticle?, String>(
    save = { article ->
        if (article == null) emptyList() else
            listOf(article.title, article.paragraphs.size.toString()) + article.paragraphs + article.translations
    },
    restore = { saved ->
        if (saved.size < 2) {
            null
        } else {
            val paragraphCount = saved[1].toIntOrNull() ?: 0
            AiArticle(
                title = saved[0],
                paragraphs = saved.drop(2).take(paragraphCount),
                translations = saved.drop(2 + paragraphCount)
            )
        }
    }
)

private val StringListSaver = listSaver<List<String>, String>(
    save = { it },
    restore = { it }
)

// Checkbox picks survive rotation and process death; ids are enough because the candidate list
// itself is re-read from Room.
private val WordIdSetSaver = listSaver<Set<Int>, Int>(
    save = { it.toList() },
    restore = { it.toSet() }
)

@Composable
fun AiScreen(
    assistant: AiAssistant,
    words: WordRepository,
    records: LearningRecordRepository,
    initialQuery: String = "",
    onOpenAiSettings: () -> Unit,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var modeName by rememberSaveable { mutableStateOf(AiMode.LOOKUP.name) }
    val mode = AiMode.entries.firstOrNull { it.name == modeName } ?: AiMode.LOOKUP

    var query by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(initialQuery))
    }
    var lookup by rememberSaveable(stateSaver = AiWordInfoSaver) { mutableStateOf<AiWordInfo?>(null) }
    var lookupError by rememberSaveable { mutableStateOf("") }
    var lookupLoading by remember { mutableStateOf(false) }
    var localHit by rememberSaveable { mutableStateOf("") }

    var article by rememberSaveable(stateSaver = AiArticleSaver) { mutableStateOf<AiArticle?>(null) }
    var articleError by rememberSaveable { mutableStateOf("") }
    var articleLoading by remember { mutableStateOf(false) }
    var articleWords by rememberSaveable(stateSaver = StringListSaver) { mutableStateOf(emptyList()) }
    var candidates by remember { mutableStateOf<List<ArticleCandidate>?>(null) }
    var candidatesError by remember { mutableStateOf("") }
    var selectedIds by rememberSaveable(stateSaver = WordIdSetSaver) { mutableStateOf<Set<Int>>(emptySet()) }
    var selectionInitialized by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val loaded = runCatching { articleCandidates(words.getAllWords(), records.getAllRecords()) }
        candidates = loaded.getOrNull()
        candidatesError = loaded.exceptionOrNull()?.let { "读取练习记录失败：${it.message ?: "未知错误"}" }.orEmpty()
        // Preselect the weakest words once, so "写一篇" is one tap away; after that the learner's
        // own choice is respected, including an emptied selection.
        val ready = loaded.getOrNull()
        if (ready != null && !selectionInitialized) {
            selectedIds = ready.take(MAX_ARTICLE_WORDS).mapTo(mutableSetOf()) { it.word.id }
            selectionInitialized = true
        }
    }

    fun runLookup() {
        val keyword = query.text.trim()
        if (keyword.isEmpty() || lookupLoading) return
        lookupLoading = true
        lookupError = ""
        lookup = null
        localHit = ""
        scope.launch {
            // The bundled wordbook is checked first: it tells the learner whether the word is
            // already available offline, and it lets the model extend the app's own definition.
            val local = runCatching { words.searchWords(keyword) }.getOrNull()
                ?.firstOrNull { it.word.equals(keyword, ignoreCase = true) }
            localHit = local?.let { "${it.partOfSpeech.trim()} ${it.meaning}".trim() }.orEmpty()
            val result = runCatching { assistant.lookupWord(keyword, local?.meaning) }
            lookupLoading = false
            lookup = result.getOrNull()
            lookupError = result.exceptionOrNull()?.message.orEmpty()
        }
    }

    fun runArticle() {
        if (articleLoading) return
        val available = candidates
        if (available == null) {
            articleError = "还在读取练过的单词，请稍候"
            return
        }
        val chosen = selectedArticleWords(available, selectedIds)
        if (chosen.size < MIN_ARTICLE_WORDS) {
            articleError = "至少选 $MIN_ARTICLE_WORDS 个单词"
            return
        }
        articleLoading = true
        articleError = ""
        scope.launch {
            val result = runCatching { assistant.writeArticle(chosen) }
            articleLoading = false
            result.getOrNull()?.let { written ->
                article = written
                articleWords = chosen.map { word -> word.word }
            }
            articleError = result.exceptionOrNull()?.message.orEmpty()
        }
    }

    fun toggleWord(id: Int) {
        selectedIds = toggleArticleWord(selectedIds, id)
    }

    BackHandler(onBack = onBack)

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MaoDanDimens.pageHorizontal),
            verticalArrangement = Arrangement.spacedBy(MaoDanDimens.space16)
        ) {
            Spacer(Modifier.height(MaoDanDimens.space8))
            AiHeader(onBack = onBack)

            if (!assistant.isConfigured) {
                AiSetupCard(onOpenAiSettings = onOpenAiSettings)
                Spacer(Modifier.height(MaoDanDimens.space24))
                return@Column
            }

            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                AiMode.entries.forEachIndexed { index, entry ->
                    SegmentedButton(
                        selected = mode == entry,
                        onClick = { modeName = entry.name },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = AiMode.entries.size
                        ),
                        icon = { SegmentedButtonDefaults.Icon(active = mode == entry) }
                    ) {
                        Text(entry.label)
                    }
                }
            }

            when (mode) {
                AiMode.LOOKUP -> LookupSection(
                    query = query,
                    onQueryChange = { query = it },
                    onSearch = { runLookup() },
                    loading = lookupLoading,
                    localHit = localHit,
                    result = lookup,
                    error = lookupError
                )
                AiMode.ARTICLE -> ArticleSection(
                    candidates = candidates,
                    candidatesError = candidatesError,
                    selectedIds = selectedIds,
                    onToggleWord = { toggleWord(it) },
                    onSelectWeakest = {
                        candidates?.let { list ->
                            selectedIds = list.take(MAX_ARTICLE_WORDS).mapTo(mutableSetOf()) { it.word.id }
                        }
                    },
                    onClearSelection = { selectedIds = emptySet() },
                    loading = articleLoading,
                    article = article,
                    usedWords = articleWords,
                    error = articleError,
                    onGenerate = { runArticle() }
                )
            }

            Spacer(Modifier.height(MaoDanDimens.space24))
        }
    }
}

@Composable
private fun AiHeader(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("AI 助手", style = MaterialTheme.typography.headlineMedium)
            Text(
                "查词与复习短文 · 需要联网",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AiSetupCard(onOpenAiSettings: () -> Unit, modifier: Modifier = Modifier) {
    MaoDanCard(Modifier.fillMaxWidth()) {
        Column(
            Modifier.fillMaxWidth().padding(MaoDanDimens.space20),
            verticalArrangement = Arrangement.spacedBy(MaoDanDimens.space12)
        ) {
            Text("还没有配置 AI 服务", style = MaterialTheme.typography.titleMedium)
            Text(
                "AI 助手使用免费的第三方大模型接口，需要你自己的 API Key。" +
                    "填写后可以查词库里没有的单词，也可以把练习过的单词串成一篇复习短文。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            MaoDanPrimaryButton(
                text = "去设置填写",
                onClick = onOpenAiSettings,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun LookupSection(
    query: TextFieldValue,
    onQueryChange: (TextFieldValue) -> Unit,
    onSearch: () -> Unit,
    loading: Boolean,
    localHit: String,
    result: AiWordInfo?,
    error: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(MaoDanDimens.space12)) {
        CET6SectionTitle("查词")
        OutlinedTextField(
            value = query.text,
            onValueChange = { onQueryChange(TextFieldValue(it)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = MaoDanShapes.medium,
            placeholder = { Text("输入单词，例如 serendipity") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch() })
        )
        MaoDanPrimaryButton(
            text = if (loading) "正在询问 AI…" else "查询",
            onClick = onSearch,
            modifier = Modifier.fillMaxWidth(),
            enabled = query.text.isNotBlank() && !loading
        )
        if (localHit.isNotBlank()) {
            Text(
                "词库中已有：$localHit",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
        if (loading) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(MaoDanDimens.space8))
                Text(
                    "正在生成释义，通常需要几秒",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (error.isNotBlank()) {
            Text(error, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
        }
        result?.let { WordInfoCard(it) }
        Text(
            "AI 结果只用于参考，不会进入词库，也不计入学习统计。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun WordInfoCard(info: AiWordInfo, modifier: Modifier = Modifier) {
    MaoDanCard(Modifier.fillMaxWidth()) {
        Column(
            Modifier.fillMaxWidth().padding(MaoDanDimens.space20),
            verticalArrangement = Arrangement.spacedBy(MaoDanDimens.space12)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    info.word.ifBlank { "—" },
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.width(MaoDanDimens.space8))
                if (info.phonetic.isNotBlank()) {
                    Text(
                        info.phonetic,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (info.partOfSpeech.isNotBlank()) {
                Box(
                    Modifier
                        .clip(MaoDanShapes.pill)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(horizontal = MaoDanDimens.space12, vertical = MaoDanDimens.space4)
                ) {
                    Text(
                        info.partOfSpeech,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            info.meanings.forEach { meaning ->
                Text(meaning, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            }
            if (info.examples.isNotEmpty()) {
                HorizontalDivider()
                info.examples.forEach { example ->
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        if (example.en.isNotBlank()) {
                            Text(example.en, style = MaterialTheme.typography.bodyMedium)
                        }
                        if (example.zh.isNotBlank()) {
                            Text(
                                example.zh,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            if (info.collocations.isNotEmpty()) {
                HorizontalDivider()
                Text(
                    "常见搭配",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    info.collocations.joinToString("；"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ArticleSection(
    candidates: List<ArticleCandidate>?,
    candidatesError: String,
    selectedIds: Set<Int>,
    onToggleWord: (Int) -> Unit,
    onSelectWeakest: () -> Unit,
    onClearSelection: () -> Unit,
    loading: Boolean,
    article: AiArticle?,
    usedWords: List<String>,
    error: String,
    onGenerate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val total = candidates?.size ?: 0
    val enough = selectedIds.size >= MIN_ARTICLE_WORDS
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(MaoDanDimens.space12)) {
        CET6SectionTitle("复习短文")
        Text(
            when {
                candidatesError.isNotBlank() -> candidatesError
                candidates == null -> "正在读取练过的单词…"
                total == 0 -> "还没有练习过的单词，先去拼写或待复习练几个词。"
                else -> "练过 $total 个单词，自己勾选 $MIN_ARTICLE_WORDS-$MAX_ARTICLE_WORDS 个，正文里会高亮你选的词。"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = if (candidatesError.isNotBlank()) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
        if (candidates != null && total > 0) {
            WordPickerCard(
                candidates = candidates,
                selectedIds = selectedIds,
                onToggle = onToggleWord,
                onSelectWeakest = onSelectWeakest,
                onClear = onClearSelection
            )
        }
        MaoDanPrimaryButton(
            text = when {
                loading -> "正在写短文…"
                article != null -> "用这些词再写一篇"
                else -> "用所选的 ${selectedIds.size} 个词写一篇"
            },
            onClick = onGenerate,
            modifier = Modifier.fillMaxWidth(),
            enabled = !loading && enough
        )
        if (total > 0 && !enough) {
            Text(
                "至少选 $MIN_ARTICLE_WORDS 个单词。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
        if (loading) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(MaoDanDimens.space8))
                Text(
                    "模型正在串词成文，通常需要 10 秒左右",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (error.isNotBlank()) {
            Text(error, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
        }
        article?.let { ArticleCard(it, usedWords) }
    }
}

/** Checkbox list of practised words; the ranking only decides the order and the quick preselect. */
@Composable
private fun WordPickerCard(
    candidates: List<ArticleCandidate>,
    selectedIds: Set<Int>,
    onToggle: (Int) -> Unit,
    onSelectWeakest: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    MaoDanOutlinedCard(modifier.fillMaxWidth()) {
        Column(
            Modifier.fillMaxWidth().padding(MaoDanDimens.space16),
            verticalArrangement = Arrangement.spacedBy(MaoDanDimens.space4)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "已选 ${selectedIds.size} / $MAX_ARTICLE_WORDS",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                MaoDanTextButton(
                    text = if (candidates.size <= MAX_ARTICLE_WORDS) "全选" else "选前 $MAX_ARTICLE_WORDS 个",
                    onClick = onSelectWeakest,
                    enabled = candidates.isNotEmpty()
                )
                MaoDanTextButton(
                    text = "清空",
                    onClick = onClear,
                    enabled = selectedIds.isNotEmpty()
                )
            }
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 340.dp)) {
                items(candidates, key = { it.word.id }) { candidate ->
                    val selected = candidate.word.id in selectedIds
                    val canToggle = selected || selectedIds.size < MAX_ARTICLE_WORDS
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .toggleable(
                                value = selected,
                                enabled = canToggle,
                                onValueChange = { onToggle(candidate.word.id) }
                            )
                            .padding(vertical = MaoDanDimens.space4),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = selected, onCheckedChange = null, enabled = canToggle)
                        Spacer(Modifier.width(MaoDanDimens.space8))
                        Column(
                            Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                candidate.word.word,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                            Text(
                                listOf(candidate.word.partOfSpeech.trim(), candidate.word.meaning)
                                    .filter { it.isNotEmpty() }
                                    .joinToString(" "),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(Modifier.width(MaoDanDimens.space8))
                        Text(
                            "${masteryLabel(candidate.record.mastery)} · 错 ${candidate.record.wrongCount}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ArticleCard(article: AiArticle, usedWords: List<String>, modifier: Modifier = Modifier) {
    val highlight = MaterialTheme.colorScheme.primary
    MaoDanOutlinedCard(Modifier.fillMaxWidth()) {
        Column(
            Modifier.fillMaxWidth().padding(MaoDanDimens.space20),
            verticalArrangement = Arrangement.spacedBy(MaoDanDimens.space12)
        ) {
            Text(article.title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
            if (usedWords.isNotEmpty()) {
                Text(
                    "本篇用到：${usedWords.joinToString("、")} · ${article.wordCount} 词",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            article.paragraphs.forEachIndexed { index, paragraph ->
                Column(verticalArrangement = Arrangement.spacedBy(MaoDanDimens.space4)) {
                    Text(
                        highlightedText(paragraph, usedWords, highlight),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    // Translation sits directly under its own paragraph, so a model that drops one
                    // cannot make the rest look mismatched.
                    article.translationFor(index)?.let { translation ->
                        Text(
                            translation,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            if (!article.hasAlignedTranslations && article.translations.isNotEmpty()) {
                HorizontalDivider()
                Text(
                    "中文翻译",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    article.translations.joinToString("\n"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                "短文由 AI 生成，仅用于复习，请以词典释义为准。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Highlights the words the article was asked to reuse, so review can be scanned word by word. */
@Composable
private fun highlightedText(text: String, words: List<String>, color: Color): AnnotatedString =
    remember(text, words, color) {
        val ranges = findWordRanges(text, words)
        buildAnnotatedString {
            append(text)
            ranges.forEach { range ->
                addStyle(SpanStyle(color = color, fontWeight = FontWeight.SemiBold), range.first, range.last + 1)
            }
        }
    }
