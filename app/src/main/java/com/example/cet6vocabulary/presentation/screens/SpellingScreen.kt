@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.cet6vocabulary.presentation.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.cet6vocabulary.data.local.entity.StudyProgressEntity
import com.example.cet6vocabulary.data.model.Word
import com.example.cet6vocabulary.data.repository.LearningRecordRepository
import com.example.cet6vocabulary.data.repository.StudyProgressRepository
import com.example.cet6vocabulary.data.repository.WordRepository
import com.example.cet6vocabulary.ui.components.CET6EmptyState
import com.example.cet6vocabulary.ui.components.CET6ErrorState
import com.example.cet6vocabulary.ui.components.CET6LoadingState
import com.example.cet6vocabulary.ui.components.PrimaryButton
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private enum class SpellingMode { SEQUENTIAL, RANDOM }
private enum class AnswerState { UNANSWERED, SUBMITTING, CORRECT, WRONG }

@Composable
fun SpellingScreen(
    wordRepository: WordRepository,
    learningRecordRepository: LearningRecordRepository,
    studyProgressRepository: StudyProgressRepository
) {
    var state by remember { mutableStateOf<SpellingUiState>(SpellingUiState.Loading) }
    var mode by remember { mutableStateOf(SpellingMode.SEQUENTIAL) }
    var currentIndex by remember { mutableIntStateOf(0) }
    var randomWords by remember { mutableStateOf(emptyList<Word>()) }
    var userInput by remember { mutableStateOf("") }
    var answerState by remember { mutableStateOf(AnswerState.UNANSWERED) }
    var emptyInput by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    val saveMutex = remember { Mutex() }
    val latestSnapshots = remember { mutableStateMapOf<String, StudyProgressEntity>() }
    var restored by remember { mutableStateOf(false) }

    fun resetAnswer() {
        userInput = ""
        answerState = AnswerState.UNANSWERED
        emptyInput = false
    }

    fun progressKey(saveMode: SpellingMode): String = if (saveMode == SpellingMode.RANDOM) StudyProgressKeys.SPELLING_RANDOM else StudyProgressKeys.SPELLING_SEQUENTIAL

    fun enqueueSave(index: Int = currentIndex, saveMode: SpellingMode = mode, words: List<Word>? = null) {
        val currentState = state as? SpellingUiState.Success ?: return
        val activeWords = words ?: if (saveMode == SpellingMode.RANDOM) randomWords else currentState.words
        if (activeWords.isEmpty()) return
        val progress = StudyProgressEntity(
            progressKey = progressKey(saveMode),
            currentIndex = index.coerceIn(0, activeWords.size),
            randomOrder = if (saveMode == SpellingMode.RANDOM) encodeRandomOrder(activeWords.map { it.id }) else null,
            updatedTime = System.currentTimeMillis()
        )
        latestSnapshots[progress.progressKey] = progress
        scope.launch {
            saveMutex.withLock {
                if (latestSnapshots[progress.progressKey] == progress) {
                    studyProgressRepository.saveProgress(progress)
                }
            }
        }
    }

    fun loadWords() {
        state = SpellingUiState.Loading
        scope.launch {
            runCatching {
                val words = wordRepository.getAllWords().sortedBy(Word::id)
                if (words.isEmpty()) {
                    state = SpellingUiState.Empty
                    return@runCatching
                }
                val progress = studyProgressRepository.getProgress(StudyProgressKeys.SPELLING_SEQUENTIAL)
                currentIndex = (progress?.currentIndex ?: 0).coerceIn(0, words.size)
                state = SpellingUiState.Success(words)
                mode = SpellingMode.SEQUENTIAL
                randomWords = emptyList()
                resetAnswer()
                restored = true
            }.onFailure { state = SpellingUiState.Error }
        }
    }

    LaunchedEffect(Unit) { loadWords() }
    val latestSnapshotState = rememberUpdatedState(latestSnapshots[progressKey(mode)])
    DisposableEffect(lifecycleOwner, restored) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP && restored) {
                latestSnapshotState.value?.let { snapshot ->
                    latestSnapshots[snapshot.progressKey] = snapshot
                    scope.launch {
                        saveMutex.withLock {
                            if (latestSnapshots[snapshot.progressKey] == snapshot) {
                                studyProgressRepository.saveProgress(snapshot)
                            }
                        }
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            if (restored) latestSnapshotState.value?.let { snapshot ->
                scope.launch {
                    saveMutex.withLock {
                        if (latestSnapshots[snapshot.progressKey] == snapshot) {
                            studyProgressRepository.saveProgress(snapshot)
                        }
                    }
                }
            }
        }
    }

    Scaffold(contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0), topBar = { TopAppBar(title = { Text("CET6 拼写") }) }) { padding ->
        when (val currentState = state) {
            SpellingUiState.Loading -> CET6LoadingState(Modifier.padding(padding), "正在加载词汇")
            SpellingUiState.Empty -> CET6EmptyState("暂无词汇", "当前没有可练习的词汇。", Modifier.fillMaxSize().padding(padding))
            SpellingUiState.Error -> CET6ErrorState("词汇加载失败", Modifier.fillMaxSize().padding(padding), "重新加载", ::loadWords)
            is SpellingUiState.Success -> {
                val words = if (mode == SpellingMode.RANDOM) randomWords else currentState.words
                if (words.isEmpty()) {
                    CET6EmptyState("暂无词汇", "当前模式没有可练习的词汇。", Modifier.fillMaxSize().padding(padding))
                } else if (currentIndex >= words.size) {
                    Column(Modifier.fillMaxSize().padding(padding).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Text("本轮拼写已完成", style = MaterialTheme.typography.titleLarge)
                        Text("你已经完成当前词表。", style = MaterialTheme.typography.bodyMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(onClick = { currentIndex = words.lastIndex; resetAnswer(); enqueueSave(currentIndex) }) { Text("上一个") }
                            PrimaryButton("从头开始", { currentIndex = 0; resetAnswer(); enqueueSave(0) })
                        }
                    }
                } else {
                    SpellingContent(
                        words = words,
                        index = currentIndex,
                        mode = mode,
                        input = userInput,
                        answer = answerState,
                        emptyInput = emptyInput,
                        onInput = { userInput = it; emptyInput = false },
                        onCheck = {
                            val currentWord = words[currentIndex]
                            val answer = userInput.trim()
                            if (answer.isEmpty()) {
                                emptyInput = true
                            } else if (answerState == AnswerState.UNANSWERED) {
                                answerState = AnswerState.SUBMITTING
                                scope.launch {
                                    val isCorrect = answer.equals(currentWord.word.trim(), ignoreCase = true)
                                    if (isCorrect) learningRecordRepository.recordCorrect(currentWord.id)
                                    else learningRecordRepository.recordWrong(currentWord.id)
                                    answerState = if (isCorrect) AnswerState.CORRECT else AnswerState.WRONG
                                }
                            }
                        },
                        onRetry = ::resetAnswer,
                        onMode = { nextMode ->
                            if (nextMode == mode) return@SpellingContent
                            enqueueSave(currentIndex, mode)
                            scope.launch {
                                val isRandom = nextMode == SpellingMode.RANDOM
                                val progress = studyProgressRepository.getProgress(progressKey(nextMode))
                                val restoredProgress = restoreStudyProgress(
                                    currentState.words.map(Word::id), progress, isRandom
                                )
                                val nextWords = if (isRandom) {
                                    val byId = currentState.words.associateBy(Word::id)
                                    restoredProgress.randomOrder.mapNotNull(byId::get)
                                } else {
                                    emptyList()
                                }
                                randomWords = nextWords
                                mode = nextMode
                                currentIndex = restoredProgress.currentIndex
                                resetAnswer()
                                enqueueSave(
                                    restoredProgress.currentIndex,
                                    nextMode,
                                    if (isRandom) nextWords else currentState.words
                                )
                            }
                        },
                        onPrevious = {
                            val nextIndex = (if (currentIndex == words.size) words.lastIndex else currentIndex - 1).coerceAtLeast(0)
                            currentIndex = nextIndex
                            resetAnswer()
                            enqueueSave(nextIndex)
                        },
                        onNext = {
                            val nextIndex = (currentIndex + 1).coerceAtMost(words.size)
                            currentIndex = nextIndex
                            resetAnswer()
                            enqueueSave(nextIndex)
                        },
                        padding = padding
                    )
                }
            }
        }
    }
}
private sealed interface SpellingUiState {
    data object Loading : SpellingUiState
    data object Empty : SpellingUiState
    data object Error : SpellingUiState
    data class Success(val words: List<Word>) : SpellingUiState
}

@Composable
private fun SpellingContent(
    words: List<Word>,
    index: Int,
    mode: SpellingMode,
    input: String,
    answer: AnswerState,
    emptyInput: Boolean,
    onInput: (String) -> Unit,
    onCheck: () -> Unit,
    onRetry: () -> Unit,
    onMode: (SpellingMode) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    padding: PaddingValues
) {
    val word = words[index]
    val submissionLocked = answer != AnswerState.UNANSWERED
    Column(
        Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("拼写练习", style = MaterialTheme.typography.titleLarge)
                Text("第 ${index + 1} / ${words.size} 词", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = { onMode(SpellingMode.SEQUENTIAL) }) { Text("从头开始") }
        }
        LinearProgressIndicator(
            progress = { (index + 1).toFloat() / words.size },
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = mode == SpellingMode.SEQUENTIAL, onClick = { onMode(SpellingMode.SEQUENTIAL) }, label = { Text("顺序") })
            FilterChip(selected = mode == SpellingMode.RANDOM, onClick = { onMode(SpellingMode.RANDOM) }, label = { Text("随机") })
        }
        SpellingPromptCard(word)
        OutlinedTextField(
            value = input,
            onValueChange = onInput,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("请输入英文单词") },
            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
            singleLine = true,
            enabled = !submissionLocked,
            isError = emptyInput,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, capitalization = KeyboardCapitalization.None, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onCheck() }),
            shape = MaterialTheme.shapes.medium
        )
        if (emptyInput) Text("请输入英文单词", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        when (answer) {
            AnswerState.UNANSWERED -> PrimaryButton("检查答案", onCheck, modifier = Modifier.fillMaxWidth(), icon = Icons.Default.CheckCircle)
            AnswerState.SUBMITTING -> PrimaryButton("保存中", {}, modifier = Modifier.fillMaxWidth(), enabled = false, loading = true)
            AnswerState.CORRECT -> ResultBlock("拼写正确", input, word.word, true)
            AnswerState.WRONG -> ResultBlock("拼写错误", input, word.word, false)
        }
        if (answer == AnswerState.WRONG) {
            OutlinedButton(onClick = onRetry, modifier = Modifier.fillMaxWidth().height(48.dp)) { Text("再试一次") }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onPrevious, enabled = index > 0, modifier = Modifier.weight(1f).height(48.dp)) { Text("上一个") }
            PrimaryButton("下一词", onNext, modifier = Modifier.weight(1f), enabled = index < words.size)
        }
    }
}

@Composable
private fun SpellingPromptCard(word: Word) {
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
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("NO.${word.id}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            if (word.phonetic.isNotBlank()) Text(word.phonetic, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (word.partOfSpeech.isNotBlank()) Text(word.partOfSpeech, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(word.meaning, style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
private fun ResultBlock(status: String, input: String, answer: String, correct: Boolean) {
    val statusColor = if (correct) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (correct) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
        ),
        shape = MaterialTheme.shapes.large
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(if (correct) Icons.Default.CheckCircle else Icons.Default.Close, contentDescription = null, tint = statusColor)
                Text(status, color = statusColor, style = MaterialTheme.typography.titleLarge)
            }
            Text("你的答案：$input", style = MaterialTheme.typography.bodyMedium)
            Text("标准答案：$answer", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 760)
@Composable
private fun SpellingPreview() {
    SpellingContent(
        words = listOf(Word(128, "abandon", "/əˈbændən/", "v.", "放弃；遗弃")),
        index = 0,
        mode = SpellingMode.SEQUENTIAL,
        input = "aband",
        answer = AnswerState.UNANSWERED,
        emptyInput = false,
        onInput = {},
        onCheck = {},
        onRetry = {},
        onMode = {},
        onPrevious = {},
        onNext = {},
        padding = PaddingValues()
    )
}
