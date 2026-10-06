package com.example.cet6vocabulary.presentation.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
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
import com.example.cet6vocabulary.ui.components.LucideNavigationIcons
import com.example.cet6vocabulary.ui.components.LucideSpellingIcons
import com.example.cet6vocabulary.ui.components.LucideWordIcons
import com.example.cet6vocabulary.ui.components.MaoDanCard
import com.example.cet6vocabulary.ui.components.MaoDanFeedbackBox
import com.example.cet6vocabulary.ui.components.MaoDanFeedbackKind
import com.example.cet6vocabulary.ui.components.MaoDanPrimaryButton
import com.example.cet6vocabulary.ui.components.MaoDanSecondaryButton
import com.example.cet6vocabulary.ui.components.rememberAnimatedProgress
import com.example.cet6vocabulary.ui.theme.MaoDanDimens
import com.example.cet6vocabulary.ui.theme.LocalReduceMotion
import com.example.cet6vocabulary.ui.theme.MaoDanMotion
import com.example.cet6vocabulary.ui.theme.MaoDanShapes
import com.example.cet6vocabulary.ui.theme.MaoDanSuccess
import com.example.cet6vocabulary.ui.theme.MaoDanTypography
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private enum class SpellingMode { SEQUENTIAL, RANDOM }
private enum class AnswerState { UNANSWERED, SUBMITTING, CORRECT, WRONG }

/** Click-driven slide direction, deliberately decoupled from currentIndex. */
private enum class SpellingTransitionDirection { NEXT, PREVIOUS }

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
    var transitionDirection by remember { mutableStateOf(SpellingTransitionDirection.NEXT) }
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

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = Color.Transparent, // the root scaffold owns the background and the ambient light
        contentColor = MaterialTheme.colorScheme.onBackground
    ) { padding ->
        when (val currentState = state) {
            SpellingUiState.Loading -> CET6LoadingState(Modifier.padding(padding), "正在加载词汇")
            SpellingUiState.Empty -> CET6EmptyState("暂无词汇", "当前没有可练习的词汇。", Modifier.fillMaxSize().padding(padding))
            SpellingUiState.Error -> CET6ErrorState("词汇加载失败", Modifier.fillMaxSize().padding(padding), "重新加载", ::loadWords)
            is SpellingUiState.Success -> {
                val words = if (mode == SpellingMode.RANDOM) randomWords else currentState.words
                if (words.isEmpty()) {
                    CET6EmptyState("暂无词汇", "当前模式没有可练习的词汇。", Modifier.fillMaxSize().padding(padding))
                } else if (currentIndex >= words.size) {
                    SpellingCompletionContent(
                        totalWords = words.size,
                        onPrevious = { currentIndex = words.lastIndex; resetAnswer(); enqueueSave(currentIndex) },
                        onRestart = { currentIndex = 0; resetAnswer(); enqueueSave(0) },
                        modifier = Modifier.fillMaxSize().padding(padding)
                    )
                } else {
                    SpellingContent(
                        words = words,
                        index = currentIndex,
                        mode = mode,
                        input = userInput,
                        answer = answerState,
                        emptyInput = emptyInput,
                        transitionDirection = transitionDirection,
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
                            transitionDirection = SpellingTransitionDirection.PREVIOUS
                            val nextIndex = (if (currentIndex == words.size) words.lastIndex else currentIndex - 1).coerceAtLeast(0)
                            currentIndex = nextIndex
                            resetAnswer()
                            enqueueSave(nextIndex)
                        },
                        onNext = {
                            transitionDirection = SpellingTransitionDirection.NEXT
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
    transitionDirection: SpellingTransitionDirection,
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
        Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = MaoDanDimens.pageHorizontal),
        verticalArrangement = Arrangement.spacedBy(MaoDanDimens.space16)
    ) {
        Spacer(Modifier.height(MaoDanDimens.space8))
        SpellingHeader()
        SpellingProgress(currentIndex = index, totalWords = words.size)
        SpellingModeToggle(mode = mode, onModeChange = onMode)
        SpellingPromptCard(word = word, transitionDirection = transitionDirection)
        SpellingAnswerInput(
            input = input,
            enabled = !submissionLocked,
            emptyInput = emptyInput,
            onInput = onInput,
            onCheck = onCheck
        )
        SpellingAnswerAction(
            answer = answer,
            input = input,
            word = word,
            onCheck = onCheck,
            onRetry = onRetry
        )
        SpellingNavigation(
            canGoPrevious = index > 0,
            canGoNext = index < words.size,
            onPrevious = onPrevious,
            onNext = onNext
        )
        Spacer(Modifier.height(MaoDanDimens.space24))
    }
}

@Composable
private fun SpellingHeader(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaoDanDimens.space4)
    ) {
        Text(
            "拼写训练",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            "根据提示拼写单词",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SpellingProgress(
    currentIndex: Int,
    totalWords: Int,
    modifier: Modifier = Modifier
) {
    val progress = rememberAnimatedProgress(
        target = (currentIndex + 1).toFloat() / totalWords
    )
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaoDanDimens.space4)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "第 ${currentIndex + 1} 题",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "${currentIndex + 1} / $totalWords",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(MaoDanDimens.space4)
                .clip(MaoDanShapes.small),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

@Composable
private fun SpellingModeToggle(
    mode: SpellingMode,
    onModeChange: (SpellingMode) -> Unit,
    modifier: Modifier = Modifier
) {
    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
        SegmentedButton(
            selected = mode == SpellingMode.SEQUENTIAL,
            onClick = { onModeChange(SpellingMode.SEQUENTIAL) },
            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
            icon = { SegmentedButtonDefaults.Icon(active = mode == SpellingMode.SEQUENTIAL) }
        ) {
            Text("顺序")
        }
        SegmentedButton(
            selected = mode == SpellingMode.RANDOM,
            onClick = { onModeChange(SpellingMode.RANDOM) },
            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
            icon = { SegmentedButtonDefaults.Icon(active = mode == SpellingMode.RANDOM) }
        ) {
            Text("随机")
        }
    }
}

@Composable
private fun SpellingPromptCard(
    word: Word,
    transitionDirection: SpellingTransitionDirection,
    modifier: Modifier = Modifier
) {
    val reduceMotion = LocalReduceMotion.current
    val directionSign = if (transitionDirection == SpellingTransitionDirection.NEXT) 1 else -1
    val slideDistancePx = with(LocalDensity.current) { MaoDanMotion.WordSwitchOffset.roundToPx() }
    MaoDanCard(modifier = modifier.fillMaxWidth()) {
        AnimatedContent(
            targetState = word,
            transitionSpec = {
                // Only the word travels, by a few dp: the card frame stays put so the eye keeps its
                // anchor while reading. Reduced motion degrades to a plain crossfade.
                val distance = if (reduceMotion) 0 else slideDistancePx * directionSign
                (slideInHorizontally(MaoDanMotion.normal()) { distance } + fadeIn(MaoDanMotion.normal()))
                    .togetherWith(slideOutHorizontally(MaoDanMotion.normal()) { -distance } + fadeOut(MaoDanMotion.normal()))
            },
            label = "spellingPrompt"
        ) { targetWord ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MaoDanDimens.space20, vertical = MaoDanDimens.space24),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(MaoDanDimens.space12)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "请拼写这个单词",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "NO.${targetWord.id}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (targetWord.phonetic.isNotBlank()) {
                    Text(
                        targetWord.phonetic,
                        style = MaoDanTypography.promptDisplay,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                }
                if (targetWord.partOfSpeech.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .clip(MaoDanShapes.pill)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(horizontal = MaoDanDimens.space12, vertical = MaoDanDimens.space4)
                    ) {
                        Text(
                            targetWord.partOfSpeech,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
                )
                Text(
                    targetWord.meaning,
                    style = MaoDanTypography.meaning,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun SpellingAnswerInput(
    input: String,
    enabled: Boolean,
    emptyInput: Boolean,
    onInput: (String) -> Unit,
    onCheck: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaoDanDimens.space4)
    ) {
        OutlinedTextField(
            value = input,
            onValueChange = onInput,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("请输入英文单词") },
            leadingIcon = {
                Icon(
                    imageVector = LucideNavigationIcons.Spell,
                    contentDescription = null,
                    tint = if (emptyInput) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            singleLine = true,
            enabled = enabled,
            isError = emptyInput,
            textStyle = MaoDanTypography.inputDisplay,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Ascii,
                capitalization = KeyboardCapitalization.None,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { onCheck() }),
            shape = MaoDanShapes.large,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                errorBorderColor = MaterialTheme.colorScheme.error,
                cursorColor = MaterialTheme.colorScheme.primary,
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                focusedLeadingIconColor = MaterialTheme.colorScheme.primary,
                unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
        if (emptyInput) {
            Text(
                "请输入英文单词",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun SpellingAnswerAction(
    answer: AnswerState,
    input: String,
    word: Word,
    onCheck: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedContent(
        targetState = answer,
        transitionSpec = {
            fadeIn(MaoDanMotion.fast()) togetherWith fadeOut(MaoDanMotion.fast())
        },
        label = "spellingAnswerAction",
        modifier = modifier.fillMaxWidth()
    ) { currentAnswer ->
        when (currentAnswer) {
            AnswerState.UNANSWERED -> MaoDanPrimaryButton(
                text = "检查答案",
                onClick = onCheck,
                modifier = Modifier.fillMaxWidth(),
                content = {
                    Icon(
                        imageVector = LucideSpellingIcons.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(MaoDanDimens.space4))
                    Text("检查答案", style = MaterialTheme.typography.labelLarge)
                }
            )
            AnswerState.SUBMITTING -> MaoDanPrimaryButton(
                text = "保存中",
                onClick = {},
                enabled = false,
                modifier = Modifier.fillMaxWidth(),
                content = {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(MaoDanDimens.space4))
                    Text("保存中", style = MaterialTheme.typography.labelLarge)
                }
            )
            AnswerState.CORRECT -> MaoDanFeedbackBox(
                kind = MaoDanFeedbackKind.SUCCESS,
                modifier = Modifier.fillMaxWidth()
            ) {
                SpellingResultCard(input = input, answer = word.word, correct = true)
            }
            AnswerState.WRONG -> Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(MaoDanDimens.space8)
            ) {
                MaoDanFeedbackBox(
                    kind = MaoDanFeedbackKind.ERROR,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SpellingResultCard(input = input, answer = word.word, correct = false)
                }
                MaoDanSecondaryButton(
                    text = "再试一次",
                    onClick = onRetry,
                    modifier = Modifier.fillMaxWidth(),
                    content = {
                        Icon(
                            imageVector = LucideSpellingIcons.RotateCcw,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(MaoDanDimens.space4))
                        Text("再试一次", style = MaterialTheme.typography.labelLarge)
                    }
                )
            }
        }
    }
}

@Composable
private fun SpellingResultCard(
    input: String,
    answer: String,
    correct: Boolean,
    modifier: Modifier = Modifier
) {
    val accent = if (correct) MaoDanSuccess else MaterialTheme.colorScheme.error
    val container = if (correct) MaoDanSuccess.copy(alpha = 0.12f) else MaterialTheme.colorScheme.errorContainer
    val titleColor = if (correct) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onErrorContainer
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaoDanShapes.large,
        color = container,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaoDanDimens.cardContent),
            verticalArrangement = Arrangement.spacedBy(MaoDanDimens.space8)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MaoDanDimens.space8)
            ) {
                Icon(
                    imageVector = if (correct) LucideSpellingIcons.CircleCheck else LucideSpellingIcons.CircleX,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = accent
                )
                Text(
                    if (correct) "拼写正确" else "拼写错误",
                    style = MaterialTheme.typography.titleMedium,
                    color = titleColor
                )
            }
            SpellingResultRow(
                label = "你的答案",
                value = input,
                valueColor = titleColor,
                strikethrough = !correct
            )
            SpellingResultRow(
                label = "标准答案",
                value = answer,
                valueColor = MaterialTheme.colorScheme.onSurface,
                strikethrough = false
            )
        }
    }
}

@Composable
private fun SpellingResultRow(
    label: String,
    value: String,
    valueColor: Color,
    strikethrough: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaoDanDimens.space8)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
            color = valueColor,
            textAlign = TextAlign.End,
            textDecoration = if (strikethrough) TextDecoration.LineThrough else null
        )
    }
}

@Composable
private fun SpellingNavigation(
    canGoPrevious: Boolean,
    canGoNext: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MaoDanDimens.space12)
    ) {
        MaoDanSecondaryButton(
            text = "上一题",
            onClick = onPrevious,
            enabled = canGoPrevious,
            modifier = Modifier.weight(1f),
            content = {
                Icon(
                    imageVector = LucideWordIcons.ArrowLeft,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(MaoDanDimens.space4))
                Text("上一题", style = MaterialTheme.typography.labelLarge)
            }
        )
        MaoDanPrimaryButton(
            text = "下一题",
            onClick = onNext,
            enabled = canGoNext,
            modifier = Modifier.weight(1f),
            content = {
                Text("下一题", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.width(MaoDanDimens.space4))
                Icon(
                    imageVector = LucideSpellingIcons.ArrowRight,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        )
    }
}

@Composable
private fun SpellingCompletionContent(
    totalWords: Int,
    onPrevious: () -> Unit,
    onRestart: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = MaoDanDimens.pageHorizontal),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = LucideSpellingIcons.CircleCheck,
            contentDescription = null,
            modifier = Modifier.size(MaoDanDimens.space48),
            tint = MaoDanSuccess
        )
        Spacer(Modifier.height(MaoDanDimens.space16))
        Text(
            "本轮拼写已完成",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(MaoDanDimens.space8))
        Text(
            "你已经完成当前词表。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(MaoDanDimens.space4))
        Text(
            "共 $totalWords 个单词",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(MaoDanDimens.space24))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MaoDanDimens.space12)
        ) {
            MaoDanSecondaryButton(
                text = "上一个",
                onClick = onPrevious,
                modifier = Modifier.weight(1f),
                content = {
                    Icon(
                        imageVector = LucideWordIcons.ArrowLeft,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(MaoDanDimens.space4))
                    Text("上一个", style = MaterialTheme.typography.labelLarge)
                }
            )
            MaoDanPrimaryButton(
                text = "从头开始",
                onClick = onRestart,
                modifier = Modifier.weight(1f),
                content = {
                    Icon(
                        imageVector = LucideSpellingIcons.RotateCcw,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(MaoDanDimens.space4))
                    Text("从头开始", style = MaterialTheme.typography.labelLarge)
                }
            )
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
        transitionDirection = SpellingTransitionDirection.NEXT,
        onInput = {},
        onCheck = {},
        onRetry = {},
        onMode = {},
        onPrevious = {},
        onNext = {},
        padding = PaddingValues()
    )
}

@Preview(showBackground = true, widthDp = 360, heightDp = 760)
@Composable
private fun SpellingResultPreview() {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(MaoDanDimens.pageHorizontal),
        verticalArrangement = Arrangement.spacedBy(MaoDanDimens.space12)
    ) {
        SpellingResultCard(input = "aband", answer = "abandon", correct = true)
        SpellingResultCard(input = "aband", answer = "abandon", correct = false)
    }
}
