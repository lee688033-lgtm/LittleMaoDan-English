package com.example.cet6vocabulary.presentation.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
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
import com.example.cet6vocabulary.ui.components.LucideHomeIcons
import com.example.cet6vocabulary.ui.components.LucideWordIcons
import com.example.cet6vocabulary.ui.components.MaoDanCard
import com.example.cet6vocabulary.ui.components.MaoDanOutlinedCard
import com.example.cet6vocabulary.ui.components.MaoDanPrimaryButton
import com.example.cet6vocabulary.ui.components.MaoDanSecondaryButton
import com.example.cet6vocabulary.ui.components.MaoDanTextButton
import com.example.cet6vocabulary.ui.components.rememberAnimatedProgress
import com.example.cet6vocabulary.ui.theme.LocalReduceMotion
import com.example.cet6vocabulary.ui.theme.MaoDanDimens
import com.example.cet6vocabulary.ui.theme.MaoDanMotion
import com.example.cet6vocabulary.ui.theme.MaoDanShapes
import com.example.cet6vocabulary.ui.theme.MaoDanTypography
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

 private enum class StudyMode { SEQUENTIAL, RANDOM }
 private enum class StudyTransitionDirection { NEXT, PREVIOUS }
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
    var transitionDirection by remember { mutableStateOf(StudyTransitionDirection.NEXT) }
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
        progressSaveMutex.withLock { studyProgressRepository.saveProgress(progress) }
    }

    fun enqueueProgressSave(index: Int = currentIndex, progressMode: StudyMode = mode) {
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

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = Color.Transparent, // the root scaffold owns the background and the ambient light
        contentColor = MaterialTheme.colorScheme.onBackground
    ) { padding ->
        when (val currentState = state) {
            StudyUiState.Loading -> CET6LoadingState(Modifier.padding(padding), "正在加载词汇")
            StudyUiState.Empty -> CET6EmptyState(
                title = if (wordBookMode) "单词本还是空的" else "暂无可背诵词汇",
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
                        StudyCompletionContent(
                            totalWords = activeWords.size,
                            onRestart = {
                                currentIndex = 0
                                showMeaning = false
                                enqueueProgressSave(0)
                            },
                            modifier = Modifier.fillMaxSize().padding(padding)
                        )
                    } else {
                        StudyContent(
                            words = activeWords,
                            currentIndex = displayIndex,
                            mode = mode,
                            showMeaning = showMeaning,
                            isInWordBook = word.id in wordBookIds,
                            wordBookMode = wordBookMode,
                            transitionDirection = transitionDirection,
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
                                transitionDirection = StudyTransitionDirection.PREVIOUS
                                val nextIndex = (if (currentIndex == activeWords.size) activeWords.lastIndex else currentIndex - 1).coerceAtLeast(0)
                                currentIndex = nextIndex
                                showMeaning = false
                                enqueueProgressSave(nextIndex)
                            },
                            onNext = {
                                transitionDirection = StudyTransitionDirection.NEXT
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
                            modifier = Modifier.padding(padding)
                        )
                    }
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
    transitionDirection: StudyTransitionDirection,
    wordBookMode: Boolean,
    onModeChange: (StudyMode) -> Unit,
    onShowMeaningChange: (Boolean) -> Unit,
    onToggleWordBook: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onRestart: () -> Unit,
    modifier: Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = MaoDanDimens.pageHorizontal),
        verticalArrangement = Arrangement.spacedBy(MaoDanDimens.space16)
    ) {
        Spacer(Modifier.height(MaoDanDimens.space8))
        StudyHeader(
            title = if (wordBookMode) "我的单词本·背诵" else "CET6 背诵",
            currentIndex = currentIndex,
            totalWords = words.size,
            onRestart = onRestart
        )
        StudyProgress(currentIndex = currentIndex, totalWords = words.size)
        StudyModeToggle(mode = mode, onModeChange = onModeChange)
        StudyWordCard(
            word = words[currentIndex],
            showMeaning = showMeaning,
            transitionDirection = transitionDirection
        )
        StudyActionColumn(
            showMeaning = showMeaning,
            isInWordBook = isInWordBook,
            onShowMeaningChange = onShowMeaningChange,
            onToggleWordBook = onToggleWordBook
        )
        StudyNavigation(
            canGoPrevious = currentIndex > 0,
            canGoNext = currentIndex < words.size,
            onPrevious = onPrevious,
            onNext = onNext
        )
        Spacer(Modifier.height(MaoDanDimens.space24))
    }
}

@Composable
private fun StudyHeader(
    title: String,
    currentIndex: Int,
    totalWords: Int,
    onRestart: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                "当前进度 ${currentIndex + 1} / $totalWords",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        MaoDanTextButton(
            text = "从头开始",
            onClick = onRestart
        )
    }
}

@Composable
private fun StudyProgress(
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
                "学习进度",
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
                .height(4.dp)
                .clip(MaoDanShapes.small),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    }
}

@Composable
private fun StudyModeToggle(
    mode: StudyMode,
    onModeChange: (StudyMode) -> Unit,
    modifier: Modifier = Modifier
) {
    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
        SegmentedButton(
            selected = mode == StudyMode.SEQUENTIAL,
            onClick = { onModeChange(StudyMode.SEQUENTIAL) },
            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
            icon = { SegmentedButtonDefaults.Icon(active = mode == StudyMode.SEQUENTIAL) }
        ) {
            Text("顺序")
        }
        SegmentedButton(
            selected = mode == StudyMode.RANDOM,
            onClick = { onModeChange(StudyMode.RANDOM) },
            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
            icon = { SegmentedButtonDefaults.Icon(active = mode == StudyMode.RANDOM) }
        ) {
            Text("随机")
        }
    }
}


@Composable
private fun StudyWordCard(
    word: Word,
    showMeaning: Boolean,
    transitionDirection: StudyTransitionDirection,
    modifier: Modifier = Modifier
) {
    val reduceMotion = LocalReduceMotion.current
    val directionSign = if (transitionDirection == StudyTransitionDirection.NEXT) 1 else -1
    val slideDistancePx = with(LocalDensity.current) { MaoDanMotion.WordSwitchOffset.roundToPx() }
    MaoDanCard(
        modifier = modifier.fillMaxWidth()
    ) {
        AnimatedContent(
            targetState = word,
            transitionSpec = {
                // Only the word travels, and only by a few dp, so the card frame stays put as an
                // anchor while reading. Matches the spelling page; reduced motion is a crossfade.
                val distance = if (reduceMotion) 0 else slideDistancePx * directionSign
                (slideInHorizontally(MaoDanMotion.normal()) { distance } + fadeIn(MaoDanMotion.normal()))
                    .togetherWith(slideOutHorizontally(MaoDanMotion.normal()) { -distance } + fadeOut(MaoDanMotion.normal()))
            },
            label = "wordCard"
        ) { targetWord ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MaoDanDimens.space24, vertical = MaoDanDimens.space24),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(MaoDanDimens.space8)
            ) {
                Text(
                    "NO.${targetWord.id}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(MaoDanDimens.space8))
                Text(
                    targetWord.word,
                    style = MaoDanTypography.wordDisplay,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                if (targetWord.phonetic.isNotBlank()) {
                    Text(
                        targetWord.phonetic,
                        style = MaoDanTypography.phonetic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                StudyMeaningReveal(word = targetWord, showMeaning = showMeaning)
            }
        }
    }
}

@Composable
private fun StudyMeaningReveal(
    word: Word,
    showMeaning: Boolean,
    modifier: Modifier = Modifier
) {
    val reduceMotion = LocalReduceMotion.current
    // Revealing the meaning changes the card height, so the expand keeps a token duration and only
    // runs shorter when the system asks for less motion; the fade stays either way.
    val revealSpec = remember(reduceMotion) {
        if (reduceMotion) MaoDanMotion.fast<IntSize>() else MaoDanMotion.normal<IntSize>()
    }
    AnimatedVisibility(
        visible = showMeaning,
        enter = fadeIn(MaoDanMotion.fast()) + expandVertically(
            animationSpec = revealSpec,
            expandFrom = Alignment.Top
        ),
        exit = fadeOut(MaoDanMotion.fast()) + shrinkVertically(
            animationSpec = revealSpec,
            shrinkTowards = Alignment.Top
        )
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(top = MaoDanDimens.space16),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(MaoDanDimens.space8)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )
            Text(
                word.meaning,
                style = MaoDanTypography.meaning,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = MaoDanDimens.space8)
            )
        }
    }
}

@Composable
private fun StudyActionColumn(
    showMeaning: Boolean,
    isInWordBook: Boolean,
    onShowMeaningChange: (Boolean) -> Unit,
    onToggleWordBook: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaoDanDimens.space8)
    ) {
        MaoDanPrimaryButton(
            text = if (showMeaning) "隐藏释义" else "查看释义",
            onClick = { onShowMeaningChange(!showMeaning) },
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            MaoDanTextButton(
                text = if (isInWordBook) "已加入单词本" else "加入单词本",
                onClick = onToggleWordBook,
                content = {
                    Icon(
                        imageVector = LucideHomeIcons.Bookmark,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = if (isInWordBook) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(MaoDanDimens.space4))
                    Text(
                        if (isInWordBook) "已加入单词本" else "加入单词本",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isInWordBook) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            )
        }
    }
}

@Composable
private fun StudyNavigation(
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
            text = "上一个",
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
                Text("上一个", style = MaterialTheme.typography.labelLarge)
            }
        )
        MaoDanPrimaryButton(
            text = "下一个",
            onClick = onNext,
            enabled = canGoNext,
            modifier = Modifier.weight(1f),
            content = {
                Text("下一个", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.width(MaoDanDimens.space4))
                Icon(
                    imageVector = LucideHomeIcons.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        )
    }
}

@Composable
private fun StudyCompletionContent(
    totalWords: Int,
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
        Text(
            "本轮背诵已完成",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(MaoDanDimens.space8))
        Text(
            "已完成 $totalWords 个单词的学习",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(MaoDanDimens.space24))
        MaoDanPrimaryButton(
            text = "从头开始",
            onClick = onRestart
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, widthDp = 360, heightDp = 760)
@Composable
private fun StudyPreview() {
    StudyContent(
        words = listOf(Word(1, "abandon", "/əˈbændən/", "v.", "放弃；抛弃")),
        currentIndex = 0,
        mode = StudyMode.SEQUENTIAL,
        showMeaning = true,
        isInWordBook = false,
        wordBookMode = false,
        transitionDirection = StudyTransitionDirection.NEXT,
        onModeChange = {},
        onShowMeaningChange = {},
        onToggleWordBook = {},
        onPrevious = {},
        onNext = {},
        onRestart = {},
        modifier = Modifier.fillMaxSize()
    )
}
