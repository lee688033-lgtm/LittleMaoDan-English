@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.cet6vocabulary.presentation.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.cet6vocabulary.data.local.entity.StudyProgressEntity
import com.example.cet6vocabulary.data.model.Word
import com.example.cet6vocabulary.data.repository.StudyProgressRepository
import com.example.cet6vocabulary.data.repository.WordBookRepository
import com.example.cet6vocabulary.data.repository.WordRepository
import com.example.cet6vocabulary.ui.components.CET6EmptyState
import com.example.cet6vocabulary.ui.components.CET6ErrorState
import com.example.cet6vocabulary.ui.components.CET6LoadingState
import com.example.cet6vocabulary.ui.components.PrimaryButton
import com.example.cet6vocabulary.ui.components.SecondaryButton
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private enum class StudyMode { SEQUENTIAL, RANDOM }
private sealed interface StudyUiState {
    data object Loading : StudyUiState
    data object Empty : StudyUiState
    data class Success(val words: List<Word>) : StudyUiState
    data object Error : StudyUiState
}

@Composable
fun StudyScreen(
    repository: WordRepository,
    wordBookRepository: WordBookRepository,
    studyProgressRepository: StudyProgressRepository,
    wordBookMode: Boolean = false
) {
    var state by remember { mutableStateOf<StudyUiState>(StudyUiState.Loading) }
    var mode by remember { mutableStateOf(StudyMode.SEQUENTIAL) }
    var currentIndex by remember { mutableIntStateOf(0) }
    var showMeaning by remember { mutableStateOf(false) }
    var randomWords by remember { mutableStateOf(emptyList<Word>()) }
    var wordBookIds by remember { mutableStateOf(emptySet<Int>()) }
    var initialized by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    val progressSaveMutex = remember { Mutex() }

    fun createProgressSnapshot(
        index: Int = currentIndex,
        progressMode: StudyMode = mode
    ): StudyProgressEntity? {
        val success = state as? StudyUiState.Success ?: return null
        val activeWords = if (progressMode == StudyMode.SEQUENTIAL) success.words else randomWords
        if (activeWords.isEmpty()) return null
        return StudyProgressEntity(
            progressKey = StudyProgressKeys.forMode(wordBookMode, progressMode == StudyMode.RANDOM),
            currentIndex = index.coerceIn(0, activeWords.size),
            randomOrder = if (progressMode == StudyMode.RANDOM) encodeRandomOrder(activeWords.map { it.id }) else null,
            updatedTime = System.currentTimeMillis()
        )
    }

    suspend fun saveProgress(progress: StudyProgressEntity?) {
        if (progress == null) return
        progressSaveMutex.withLock {
            studyProgressRepository.saveProgress(progress)
        }
    }

    fun enqueueProgressSave(
        index: Int = currentIndex,
        progressMode: StudyMode = mode
    ) {
        val progress = createProgressSnapshot(index, progressMode) ?: return
        scope.launch { saveProgress(progress) }
    }

    fun loadWords() {
        state = StudyUiState.Loading
        try {
            val allWords = repository.getAllWords().sortedBy { it.id }
            scope.launch {
                runCatching {
                    val records = wordBookRepository.getAllWordBookRecords()
                    wordBookIds = records.mapTo(mutableSetOf()) { it.wordId }
                    val words = if (wordBookMode) records.mapNotNull { record -> allWords.firstOrNull { it.id == record.wordId } } else allWords
                    if (words.isEmpty()) {
                        state = StudyUiState.Empty
                        return@runCatching
                    }
                    val progressKey = StudyProgressKeys.forMode(wordBookMode, false)
                    val progress = studyProgressRepository.getProgress(progressKey)
                    val restored = restoreStudyProgress(words.map { it.id }, progress, random = false)
                    state = StudyUiState.Success(words)
                    currentIndex = restored.currentIndex
                    randomWords = emptyList()
                    mode = StudyMode.SEQUENTIAL
                    showMeaning = false
                    initialized = true
                    if (restored.needsSave) saveProgress(createProgressSnapshot(restored.currentIndex, StudyMode.SEQUENTIAL))
                }.onFailure { state = StudyUiState.Error }
            }
        } catch (_: Exception) {
            state = StudyUiState.Error
        }
    }

    LaunchedEffect(repository, wordBookRepository, studyProgressRepository, wordBookMode) { loadWords() }
    DisposableEffect(lifecycleOwner, initialized) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP && initialized) {
                val progress = createProgressSnapshot()
                scope.launch { saveProgress(progress) }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            if (initialized) {
                val progress = createProgressSnapshot()
                scope.launch { saveProgress(progress) }
            }
        }
    }

    Scaffold(contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0), topBar = { TopAppBar(title = { Text(if (wordBookMode) "我的单词本·背诵" else "CET6 背诵") }) }) { padding ->
        when (val currentState = state) {
            StudyUiState.Loading -> CET6LoadingState(Modifier.padding(padding), "正在加载词汇")
            StudyUiState.Empty -> CET6EmptyState(
                title = if (wordBookMode) "单词本还是空的" else "暂无词汇",
                description = if (wordBookMode) "在背诵时收藏需要重点记忆的单词。" else "当前没有可学习的词汇。",
                modifier = Modifier.fillMaxSize().padding(padding)
            )
            StudyUiState.Error -> CET6ErrorState(
                message = "词汇加载失败",
                modifier = Modifier.fillMaxSize().padding(padding),
                actionText = "重新加载",
                onActionClick = ::loadWords
            )
            is StudyUiState.Success -> {
                val activeWords = if (mode == StudyMode.SEQUENTIAL) currentState.words else randomWords
                if (activeWords.isEmpty()) {
                    CET6EmptyState("暂无词汇", "当前模式没有可学习的词汇。", Modifier.fillMaxSize().padding(padding))
                } else {
                    val isComplete = currentIndex >= activeWords.size
                    val displayIndex = currentIndex.coerceIn(0, activeWords.lastIndex)
                    val word = activeWords[displayIndex]
                    if (isComplete) {
                        Column(
                            Modifier.fillMaxSize().padding(padding).padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("本轮背诵已完成", style = MaterialTheme.typography.titleLarge)
                            Text("你已经完成当前词表。", style = MaterialTheme.typography.bodyMedium)
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedButton(onClick = {
                                    val nextIndex = activeWords.lastIndex
                                    currentIndex = nextIndex
                                    showMeaning = false
                                enqueueProgressSave(nextIndex)
                                }) { Text("上一个") }
                                PrimaryButton("从头开始", {
                                    currentIndex = 0
                                    showMeaning = false
                                    enqueueProgressSave(0)
                                })
                            }
                        }
                    } else
                    StudyContent(
                        words = activeWords,
                        currentIndex = displayIndex,
                        mode = mode,
                        showMeaning = showMeaning,
                        isInWordBook = word.id in wordBookIds,
                        onModeChange = { nextMode ->
                            if (nextMode == mode) return@StudyContent
                            val previousProgress = createProgressSnapshot()
                            scope.launch {
                                saveProgress(previousProgress)
                                if (nextMode == StudyMode.RANDOM) {
                                    val progress = studyProgressRepository.getProgress(
                                        StudyProgressKeys.forMode(wordBookMode, random = true)
                                    )
                                    val restored = restoreStudyProgress(currentState.words.map { it.id }, progress, random = true)
                                    randomWords = restored.randomOrder.mapNotNull { id -> currentState.words.firstOrNull { it.id == id } }
                                    currentIndex = restored.currentIndex
                                } else {
                                    val progress = studyProgressRepository.getProgress(
                                        StudyProgressKeys.forMode(wordBookMode, random = false)
                                    )
                                    currentIndex = restoreStudyProgress(
                                        currentState.words.map { it.id },
                                        progress,
                                        random = false
                                    ).currentIndex
                                    randomWords = emptyList()
                                }
                                mode = nextMode
                                showMeaning = false
                            }
                        },
                        onShowMeaningChange = { showMeaning = it },
                        onToggleWordBook = {
                            scope.launch {
                                if (word.id in wordBookIds) {
                                    wordBookRepository.removeWord(word.id)
                                    wordBookIds = wordBookIds - word.id
                                } else {
                                    wordBookRepository.addWord(word.id)
                                    wordBookIds = wordBookIds + word.id
                                }
                            }
                        },
                        onPrevious = {
                            val nextIndex = (if (currentIndex == activeWords.size) activeWords.lastIndex else currentIndex - 1).coerceAtLeast(0)
                            currentIndex = nextIndex
                            showMeaning = false
                            enqueueProgressSave(nextIndex)
                        },
                        onNext = {
                            val nextIndex = (currentIndex + 1).coerceAtMost(activeWords.size)
                            currentIndex = nextIndex
                            showMeaning = false
                            enqueueProgressSave(nextIndex)
                        },
                        onRestart = {
                            currentIndex = 0
                            showMeaning = false
                            enqueueProgressSave(0)
                        },
                        modifier = Modifier.fillMaxSize().padding(padding)
                    )
                }
            }
        }
    }
}

@Composable
private fun StudyContent(
    words: List<Word>,
    currentIndex: Int,
    mode: StudyMode,
    showMeaning: Boolean,
    isInWordBook: Boolean,
    onModeChange: (StudyMode) -> Unit,
    onShowMeaningChange: (Boolean) -> Unit,
    onToggleWordBook: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onRestart: () -> Unit,
    modifier: Modifier
) {
    val word = words[currentIndex]
    Column(
        modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("学习进度", style = MaterialTheme.typography.titleLarge)
                Text("${currentIndex + 1} / ${words.size}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = onRestart) { Text("从头开始") }
        }
        LinearProgressIndicator(
            progress = { (currentIndex + 1).toFloat() / words.size },
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = mode == StudyMode.SEQUENTIAL, onClick = { onModeChange(StudyMode.SEQUENTIAL) }, label = { Text("顺序") })
            FilterChip(selected = mode == StudyMode.RANDOM, onClick = { onModeChange(StudyMode.RANDOM) }, label = { Text("随机") })
        }
        StudyWordCard(word, showMeaning, isInWordBook, onShowMeaningChange, onToggleWordBook)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onPrevious, enabled = currentIndex > 0, modifier = Modifier.weight(1f).height(48.dp)) { Text("上一个") }
            PrimaryButton("下一个", onNext, modifier = Modifier.weight(1f), enabled = currentIndex < words.size)
        }
    }
}

@Composable
private fun StudyWordCard(
    word: Word,
    showMeaning: Boolean,
    isInWordBook: Boolean,
    onShowMeaningChange: (Boolean) -> Unit,
    onToggleWordBook: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("NO.${word.id}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text(word.word, style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.Bold)
            if (word.phonetic.isNotBlank()) Text(word.phonetic, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (word.partOfSpeech.isNotBlank()) Text(word.partOfSpeech, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (showMeaning) {
                Text(word.meaning, style = MaterialTheme.typography.bodyLarge)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                SecondaryButton(if (showMeaning) "隐藏释义" else "显示释义", { onShowMeaningChange(!showMeaning) })
                SecondaryButton(
                    text = if (isInWordBook) "移出单词本" else "加入单词本",
                    onClick = onToggleWordBook,
                    icon = Icons.Default.Star
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 760)
@Composable
private fun StudyPreview() {
    StudyContent(
        words = listOf(Word(1, "abandon", "/əˈbændən/", "v.", "放弃；抛弃")),
        currentIndex = 0,
        mode = StudyMode.SEQUENTIAL,
        showMeaning = true,
        isInWordBook = false,
        onModeChange = {},
        onShowMeaningChange = {},
        onToggleWordBook = {},
        onPrevious = {},
        onNext = {},
        onRestart = {},
        modifier = Modifier.fillMaxSize()
    )
}
