package com.example.cet6vocabulary.presentation.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.cet6vocabulary.data.model.Word
import com.example.cet6vocabulary.data.repository.LearningRecordRepository
import com.example.cet6vocabulary.data.repository.WordRepository
import com.example.cet6vocabulary.presentation.model.ReviewWord
import com.example.cet6vocabulary.presentation.model.masteryLabel
import com.example.cet6vocabulary.presentation.model.selectReviewWords
import com.example.cet6vocabulary.ui.components.CET6EmptyState
import com.example.cet6vocabulary.ui.components.CET6ErrorState
import com.example.cet6vocabulary.ui.components.CET6LoadingState
import com.example.cet6vocabulary.ui.components.LucideSpellingIcons
import com.example.cet6vocabulary.ui.components.MaoDanCard
import com.example.cet6vocabulary.ui.components.MaoDanFeedbackBox
import com.example.cet6vocabulary.ui.components.MaoDanFeedbackKind
import com.example.cet6vocabulary.ui.components.MaoDanOutlinedCard
import com.example.cet6vocabulary.ui.components.MaoDanPrimaryButton
import com.example.cet6vocabulary.ui.components.MaoDanProgressBar
import com.example.cet6vocabulary.ui.components.MaoDanSecondaryButton
import com.example.cet6vocabulary.ui.theme.MaoDanDimens
import kotlinx.coroutines.launch

private const val ANSWER_NONE = ""
private const val ANSWER_CORRECT = "correct"
private const val ANSWER_WRONG = "wrong"

// The quiz queue is a plain id list: the words themselves are reloaded on every entry, so only
// the order and the cursor need to survive recreation.
private val ReviewQueueSaver = listSaver<SnapshotStateList<Int>, Int>(
    save = { queue -> queue.toList() },
    restore = { saved -> mutableStateListOf<Int>().apply { addAll(saved) } }
)

private sealed interface ReviewUiState {
    data object Loading : ReviewUiState
    data object Error : ReviewUiState
    data class Success(val items: List<ReviewWord>) : ReviewUiState
}

@Composable
fun ReviewScreen(
    words: WordRepository,
    records: LearningRecordRepository,
    onBack: () -> Unit
) {
    var state by remember { mutableStateOf<ReviewUiState>(ReviewUiState.Loading) }
    var reloadKey by remember { mutableIntStateOf(0) }
    var wordById by remember { mutableStateOf<Map<Int, Word>>(emptyMap()) }
    var inQuiz by rememberSaveable { mutableStateOf(false) }
    var index by rememberSaveable { mutableIntStateOf(0) }
    var input by rememberSaveable { mutableStateOf("") }
    var answerKind by rememberSaveable { mutableStateOf(ANSWER_NONE) }
    var correctCount by rememberSaveable { mutableIntStateOf(0) }
    val queue = rememberSaveable(saver = ReviewQueueSaver) { mutableStateListOf<Int>() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(reloadKey) {
        state = try {
            val allWords = words.getAllWords()
            wordById = allWords.associateBy(Word::id)
            ReviewUiState.Success(selectReviewWords(allWords, records.getAllRecords()))
        } catch (_: Exception) {
            ReviewUiState.Error
        }
    }

    fun backToList() {
        inQuiz = false
        reloadKey++
    }

    fun startQuiz() {
        scope.launch {
            val fresh = runCatching {
                selectReviewWords(words.getAllWords(), records.getAllRecords())
            }.getOrElse { emptyList() }
            queue.clear()
            queue.addAll(fresh.map { it.word.id })
            index = 0
            input = ""
            answerKind = ANSWER_NONE
            correctCount = 0
            inQuiz = queue.isNotEmpty()
            if (queue.isEmpty()) reloadKey++
        }
    }

    BackHandler {
        if (inQuiz) backToList() else onBack()
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onBackground
    ) { padding ->
        val current = state
        when {
            current is ReviewUiState.Loading -> CET6LoadingState(
                Modifier.fillMaxSize().padding(padding),
                "正在整理待复习单词"
            )
            current is ReviewUiState.Error -> Column(
                Modifier.fillMaxSize().padding(padding),
                verticalArrangement = Arrangement.Center
            ) {
                CET6ErrorState(
                    "复习数据加载失败",
                    actionText = "重新加载",
                    onActionClick = { reloadKey++ }
                )
            }
            current is ReviewUiState.Success && inQuiz -> {
                val quizWords = queue.mapNotNull(wordById::get)
                when {
                    quizWords.isEmpty() -> ReviewCompletionContent(
                        total = 0,
                        correctCount = 0,
                        onRestart = ::startQuiz,
                        onBackToList = ::backToList,
                        modifier = Modifier.fillMaxSize().padding(padding)
                    )
                    index >= quizWords.size -> ReviewCompletionContent(
                        total = quizWords.size,
                        correctCount = correctCount,
                        onRestart = ::startQuiz,
                        onBackToList = ::backToList,
                        modifier = Modifier.fillMaxSize().padding(padding)
                    )
                    else -> ReviewQuizContent(
                        word = quizWords[index],
                        index = index,
                        total = quizWords.size,
                        input = input,
                        answerKind = answerKind,
                        onInput = { input = it },
                        onSubmit = {
                            if (answerKind == ANSWER_NONE && input.isNotBlank()) {
                                val target = quizWords[index]
                                val correct = input.trim().equals(target.word.trim(), ignoreCase = true)
                                answerKind = if (correct) ANSWER_CORRECT else ANSWER_WRONG
                                if (correct) correctCount++
                                scope.launch {
                                    if (correct) records.recordCorrect(target.id)
                                    else records.recordWrong(target.id)
                                }
                            }
                        },
                        onNext = {
                            index++
                            input = ""
                            answerKind = ANSWER_NONE
                        },
                        onBackToList = ::backToList,
                        modifier = Modifier.fillMaxSize().padding(padding)
                    )
                }
            }
            current is ReviewUiState.Success -> ReviewListContent(
                reviewItems = current.items,
                onBack = onBack,
                onStart = ::startQuiz,
                modifier = Modifier.fillMaxSize().padding(padding)
            )
        }
    }
}

@Composable
private fun ReviewListContent(
    reviewItems: List<ReviewWord>,
    onBack: () -> Unit,
    onStart: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.padding(horizontal = MaoDanDimens.pageHorizontal),
        verticalArrangement = Arrangement.spacedBy(MaoDanDimens.space12),
        contentPadding = PaddingValues(top = MaoDanDimens.space8, bottom = MaoDanDimens.space24)
    ) {
        item(key = "header") {
            ReviewHeader(
                title = "待复习",
                subtitle = "练习过但还没掌握的词",
                onBack = onBack
            )
        }
        item(key = "summary") {
            MaoDanCard(Modifier.fillMaxWidth()) {
                Column(
                    Modifier.fillMaxWidth().padding(MaoDanDimens.cardContent),
                    verticalArrangement = Arrangement.spacedBy(MaoDanDimens.space8)
                ) {
                    Text(
                        "${reviewItems.size}",
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "个单词等待复习",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "答错的词会自动进来。在这里答对一次，掌握度升到 2 级，就会自动离开列表。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        item(key = "start") {
            MaoDanPrimaryButton(
                text = "开始复习",
                onClick = onStart,
                modifier = Modifier.fillMaxWidth(),
                enabled = reviewItems.isNotEmpty()
            )
        }
        if (reviewItems.isEmpty()) {
            item(key = "empty") {
                CET6EmptyState(
                    "暂无待复习单词",
                    "在背诵或拼写中答错的词会自动出现在这里。",
                    icon = LucideSpellingIcons.RotateCcw
                )
            }
        } else {
            items(reviewItems, key = { it.word.id }) { item ->
                ReviewRow(item)
            }
        }
    }
}

@Composable
private fun ReviewHeader(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineMedium)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ReviewRow(item: ReviewWord, modifier: Modifier = Modifier) {
    MaoDanOutlinedCard(modifier = modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(MaoDanDimens.cardContent),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(MaoDanDimens.space4)
            ) {
                Text(item.word.word, style = MaterialTheme.typography.titleLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(MaoDanDimens.space8)) {
                    if (item.word.phonetic.isNotBlank()) {
                        Text(
                            item.word.phonetic,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (item.word.partOfSpeech.isNotBlank()) {
                        Text(
                            item.word.partOfSpeech,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Text(
                    item.word.meaning,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(MaoDanDimens.space8))
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(MaoDanDimens.space4)
            ) {
                Text(
                    "错误 ${item.record.wrongCount} 次",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error
                )
                Text(
                    masteryLabel(item.record.mastery),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ReviewQuizContent(
    word: Word,
    index: Int,
    total: Int,
    input: String,
    answerKind: String,
    onInput: (String) -> Unit,
    onSubmit: () -> Unit,
    onNext: () -> Unit,
    onBackToList: () -> Unit,
    modifier: Modifier = Modifier
) {
    val answered = answerKind != ANSWER_NONE
    Column(
        modifier
            .padding(horizontal = MaoDanDimens.pageHorizontal)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(MaoDanDimens.space16)
    ) {
        Spacer(Modifier.height(MaoDanDimens.space8))
        ReviewHeader(
            title = "复习 ${index + 1} / $total",
            subtitle = "看释义拼出单词",
            onBack = onBackToList
        )
        MaoDanProgressBar(progress = if (total == 0) 0f else index.toFloat() / total)
        MaoDanCard(Modifier.fillMaxWidth()) {
            Column(
                Modifier.fillMaxWidth().padding(MaoDanDimens.space20),
                verticalArrangement = Arrangement.spacedBy(MaoDanDimens.space8)
            ) {
                Text(
                    word.meaning,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(horizontalArrangement = Arrangement.spacedBy(MaoDanDimens.space8)) {
                    if (word.partOfSpeech.isNotBlank()) {
                        Text(
                            word.partOfSpeech,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (word.phonetic.isNotBlank()) {
                        Text(
                            word.phonetic,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        OutlinedTextField(
            value = input,
            onValueChange = onInput,
            modifier = Modifier.fillMaxWidth(),
            enabled = !answered,
            singleLine = true,
            label = { Text("输入单词") },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { onSubmit() })
        )
        when (answerKind) {
            ANSWER_CORRECT -> MaoDanFeedbackBox(MaoDanFeedbackKind.SUCCESS) {
                Column(verticalArrangement = Arrangement.spacedBy(MaoDanDimens.space4)) {
                    Text("回答正确", style = MaterialTheme.typography.titleMedium)
                    Text(word.word, style = MaterialTheme.typography.bodyMedium)
                }
            }
            ANSWER_WRONG -> MaoDanFeedbackBox(MaoDanFeedbackKind.ERROR) {
                Column(verticalArrangement = Arrangement.spacedBy(MaoDanDimens.space4)) {
                    Text("回答错误", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "正确答案：${word.word}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
        if (answered) {
            MaoDanPrimaryButton(
                text = if (index + 1 >= total) "完成复习" else "下一题",
                onClick = onNext,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            MaoDanPrimaryButton(
                text = "提交答案",
                onClick = onSubmit,
                modifier = Modifier.fillMaxWidth(),
                enabled = input.isNotBlank()
            )
        }
        Spacer(Modifier.height(MaoDanDimens.space24))
    }
}

@Composable
private fun ReviewCompletionContent(
    total: Int,
    correctCount: Int,
    onRestart: () -> Unit,
    onBackToList: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier.padding(horizontal = MaoDanDimens.pageHorizontal),
        verticalArrangement = Arrangement.spacedBy(MaoDanDimens.space16)
    ) {
        Spacer(Modifier.height(MaoDanDimens.space8))
        ReviewHeader(title = "复习完成", subtitle = "本轮结果", onBack = onBackToList)
        MaoDanCard(Modifier.fillMaxWidth()) {
            Column(
                Modifier.fillMaxWidth().padding(MaoDanDimens.space20),
                verticalArrangement = Arrangement.spacedBy(MaoDanDimens.space8)
            ) {
                Text(
                    "$correctCount / $total",
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "答对的词掌握度升到 2 级，已离开待复习列表；答错的词会继续留在列表里。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        MaoDanPrimaryButton(
            text = "再练一轮",
            onClick = onRestart,
            modifier = Modifier.fillMaxWidth()
        )
        MaoDanSecondaryButton(
            text = "返回列表",
            onClick = onBackToList,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
