package com.example.cet6vocabulary.presentation.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Create
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.cet6vocabulary.data.model.Exam
import com.example.cet6vocabulary.data.model.ExamSection
import com.example.cet6vocabulary.data.model.ExamSectionData
import com.example.cet6vocabulary.data.model.ExamQuestion
import com.example.cet6vocabulary.data.repository.ExamRepository
import com.example.cet6vocabulary.presentation.model.ReadingQuestionOutcome
import com.example.cet6vocabulary.presentation.model.ReadingScore
import com.example.cet6vocabulary.presentation.model.resolveCorrectAnswer
import com.example.cet6vocabulary.presentation.model.scoreReading
import com.example.cet6vocabulary.ui.components.CET6EmptyState
import com.example.cet6vocabulary.ui.components.CET6ErrorState
import com.example.cet6vocabulary.ui.components.CET6LoadingState
import com.example.cet6vocabulary.ui.components.PrimaryButton
import com.example.cet6vocabulary.ui.components.SecondaryButton
import com.example.cet6vocabulary.ui.theme.MaoDanSuccess

private sealed interface ExamListState {
    data object Loading : ExamListState
    data object Empty : ExamListState
    data object Error : ExamListState
    data class Success(val exams: List<Exam>) : ExamListState
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamListScreen(
    repository: ExamRepository,
    onBack: () -> Unit,
    onOpenExam: (String) -> Unit
) {
    var state by remember { mutableStateOf<ExamListState>(ExamListState.Loading) }
    var reloadKey by remember { mutableStateOf(0) }
    LaunchedEffect(reloadKey) {
        state = try {
            val exams = repository.getExams()
            if (exams.isEmpty()) ExamListState.Empty else ExamListState.Success(exams)
        } catch (_: Exception) {
            ExamListState.Error
        }
    }
    Scaffold(
        containerColor = Color.Transparent, // the root scaffold owns the background and the ambient light
        contentColor = MaterialTheme.colorScheme.onBackground,
        topBar = {
            TopAppBar(
                title = { Text("CET-6 真题") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        when (val current = state) {
            ExamListState.Loading -> CET6LoadingState(Modifier.padding(padding), "正在加载真题")
            ExamListState.Empty -> CET6EmptyState(
                title = "暂无真题",
                description = "当前还没有可用的 CET-6 真题数据。",
                modifier = Modifier.padding(padding)
            )
            ExamListState.Error -> CET6ErrorState(
                message = "真题数据加载失败",
                modifier = Modifier.padding(padding),
                actionText = "重新加载",
                onActionClick = { reloadKey++ }
            )
            is ExamListState.Success -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { Text("历年真题", style = MaterialTheme.typography.titleLarge) }
                items(current.exams, key = { it.examId }) { exam ->
                    ExamListItem(exam, onClick = { onOpenExam(exam.examId) })
                }
            }
        }
    }
}

@Composable
private fun ExamListItem(exam: Exam, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(exam.title, style = MaterialTheme.typography.titleLarge)
            Text("${exam.date} · 第 ${exam.setNumber} 套", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("${exam.questionsCount()} 题", style = MaterialTheme.typography.titleMedium)
            ExamCountSummary(exam)
            PrimaryButton("查看详情", onClick, Modifier.fillMaxWidth())
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamDetailScreen(
    exam: Exam,
    onBack: () -> Unit,
    onStart: (ExamSection?) -> Unit
) {
    Scaffold(
        containerColor = Color.Transparent, // the root scaffold owns the background and the ambient light
        contentColor = MaterialTheme.colorScheme.onBackground,
        topBar = {
            TopAppBar(
                title = { Text("真题详情") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(exam.title, style = MaterialTheme.typography.headlineMedium)
                    Text("${exam.questionsCount()} 题", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            item { PrimaryButton("整套练习", { onStart(null) }, Modifier.fillMaxWidth()) }
            item { Text("专项练习", style = MaterialTheme.typography.titleLarge) }
            items(exam.sections, key = { it.section.name }) { section ->
                ExamSectionItem(section, onClick = { onStart(section.section) })
            }
        }
    }
}

@Composable
private fun ExamSectionItem(section: ExamSectionData, onClick: () -> Unit) {
    val (label, icon) = sectionPresentation(section.section)
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(label, style = MaterialTheme.typography.titleMedium)
                Text("${section.questions.size} 题", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("进入", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamPlaceholderScreen(
    title: String,
    onBack: () -> Unit
) {
    Scaffold(
        containerColor = Color.Transparent, // the root scaffold owns the background and the ambient light
        contentColor = MaterialTheme.colorScheme.onBackground,
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(16.dp))
            Text("该功能将在下一阶段实现", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))
            SecondaryButton("返回详情", onBack)
        }
    }
}

private fun Exam.questionsCount(): Int = sections.sumOf { it.questions.size }

@Composable
private fun ExamCountSummary(exam: Exam) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        exam.sections.forEach { section ->
            val (label, _) = sectionPresentation(section.section)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(section.questions.size.toString(), style = MaterialTheme.typography.titleMedium)
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private fun sectionPresentation(section: ExamSection): Pair<String, ImageVector> = when (section) {
    ExamSection.WRITING -> "写作" to Icons.Default.Create
    ExamSection.LISTENING -> "听力" to Icons.Default.Headphones
    ExamSection.READING -> "阅读" to Icons.AutoMirrored.Filled.MenuBook
    ExamSection.TRANSLATION -> "翻译" to Icons.Default.Translate
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingPracticeScreen(
    exam: Exam,
    answers: MutableMap<String, String>,
    submitted: Set<String>,
    questionIndex: Int,
    onQuestionIndexChange: (Int) -> Unit,
    onSubmit: (String) -> Unit,
    onRetry: (String) -> Unit,
    onOpenAnswerSheet: () -> Unit,
    onComplete: () -> Unit,
    onRestart: () -> Unit,
    onBack: () -> Unit
) {
    val reading = exam.sections.first { it.section == ExamSection.READING }
    val questions = reading.questions
    var showUnansweredDialog by remember { mutableStateOf(false) }
    // The index lives above this screen, so a round trip through the answer sheet comes back to
    // the same question with every selected answer intact.
    val safeIndex = questionIndex.coerceIn(0, questions.lastIndex)
    val question = questions.getOrNull(safeIndex)

    if (question == null) {
        ReadingResultScreen(exam, answers, onRestart = onRestart, onBack = onBack)
        return
    }

    val selectedAnswer = answers[question.questionId]
    val isSubmitted = question.questionId in submitted
    // A submitted question is graded even when the paper asks to hide the answer: the option
    // locks, and the result screen still reports the score at the end.
    val showFeedback = isSubmitted && (exam.appConfig?.showAnswerAfterSubmit ?: true)
    val allowRetry = exam.appConfig?.allowRetry ?: true
    val correctAnswer = resolveCorrectAnswer(exam, question)
    val scrollState = rememberScrollState()
    var lastSeenSubmission by remember(question.questionId) { mutableStateOf(isSubmitted) }
    // Each question starts at its own heading; otherwise the previous question's scroll position
    // would drop the learner into the middle of a passage they have not seen yet.
    LaunchedEffect(question.questionId) { scrollState.scrollTo(0) }
    // Submitting a question grows the page with the feedback card; move to it so the verdict is
    // visible without hunting below a long passage.
    LaunchedEffect(question.questionId, isSubmitted) {
        if (isSubmitted && !lastSeenSubmission) scrollState.animateScrollTo(scrollState.maxValue)
        lastSeenSubmission = isSubmitted
    }

    val canGoBack = safeIndex > 0
    val isLast = safeIndex == questions.lastIndex
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = Color.Transparent, // the root scaffold owns the background and the ambient light
        contentColor = MaterialTheme.colorScheme.onBackground,
        topBar = {
            TopAppBar(
                title = { Text("阅读 · ${sectionLabel(question.subsection)}") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    TextButton(onClick = onOpenAnswerSheet) { Text("答题卡") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        bottomBar = {
            Column(
                Modifier.fillMaxWidth().padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (showFeedback && allowRetry) {
                    OutlinedButton(
                        onClick = { onRetry(question.questionId) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("重做本题")
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = { onQuestionIndexChange(safeIndex - 1) },
                        enabled = canGoBack,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("上一题")
                    }
                    Button(
                        onClick = {
                            when {
                                isSubmitted -> if (isLast) onComplete() else onQuestionIndexChange(safeIndex + 1)
                                selectedAnswer != null -> onSubmit(question.questionId)
                                isLast -> showUnansweredDialog = true
                                else -> onQuestionIndexChange(safeIndex + 1)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            when {
                                isSubmitted -> if (isLast) "完成阅读" else "下一题"
                                selectedAnswer != null -> "提交答案"
                                isLast -> "完成阅读"
                                else -> "下一题"
                            }
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(scrollState).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("${question.number} / ${questions.last().number}", style = MaterialTheme.typography.titleLarge)
            Text("第 ${safeIndex + 1} / ${questions.size} 题", color = MaterialTheme.colorScheme.onSurfaceVariant)
            ReadingMaterial(reading, question)
            Text(question.question.orEmpty(), style = MaterialTheme.typography.titleMedium)
            readingOptions(reading, question).toSortedMap().forEach { (key, value) ->
                val selected = selectedAnswer == key
                val isCorrectOption = showFeedback && correctAnswer != null && key == correctAnswer
                val isWrongSelection = showFeedback && selected && correctAnswer != null && key != correctAnswer
                Card(
                    modifier = Modifier.fillMaxWidth().clickable(enabled = !isSubmitted) {
                        answers[question.questionId] = key
                    },
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            isCorrectOption -> MaoDanSuccess.copy(alpha = 0.14f)
                            isWrongSelection -> MaterialTheme.colorScheme.errorContainer
                            selected -> MaterialTheme.colorScheme.primaryContainer
                            else -> MaterialTheme.colorScheme.surface
                        }
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        when {
                            isCorrectOption -> MaoDanSuccess
                            isWrongSelection -> MaterialTheme.colorScheme.error
                            selected -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
                        }
                    )
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text("$key. $value", Modifier.weight(1f))
                        if (isCorrectOption) {
                            Text(
                                "正确答案",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else if (isWrongSelection) {
                            Text(
                                "你的答案",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            if (showFeedback) {
                ReadingFeedbackCard(selectedAnswer, correctAnswer, question.explanation)
            }
        }
    }

    if (showUnansweredDialog) {
        AlertDialog(
            onDismissRequest = { showUnansweredDialog = false },
            title = { Text("本题尚未作答") },
            text = { Text(if (isLast) "是否完成阅读？" else "是否继续下一题？") },
            confirmButton = {
                TextButton(onClick = {
                    showUnansweredDialog = false
                    if (isLast) onComplete() else onQuestionIndexChange(safeIndex + 1)
                }) { Text(if (isLast) "完成阅读" else "继续") }
            },
            dismissButton = { TextButton(onClick = { showUnansweredDialog = false }) { Text("返回答题") } }
        )
    }
}

@Composable
private fun ReadingFeedbackCard(
    selectedAnswer: String?,
    correctAnswer: String?,
    explanation: String?
) {
    val isCorrect = correctAnswer != null && selectedAnswer == correctAnswer
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                correctAnswer == null -> MaterialTheme.colorScheme.surfaceVariant
                isCorrect -> MaoDanSuccess.copy(alpha = 0.14f)
                else -> MaterialTheme.colorScheme.errorContainer
            }
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            when {
                correctAnswer == null -> MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
                isCorrect -> MaoDanSuccess
                else -> MaterialTheme.colorScheme.error
            }
        )
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    when {
                        correctAnswer == null -> Icons.Default.Info
                        isCorrect -> Icons.Default.CheckCircle
                        else -> Icons.Default.Cancel
                    },
                    contentDescription = null,
                    tint = when {
                        correctAnswer == null -> MaterialTheme.colorScheme.onSurfaceVariant
                        isCorrect -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.error
                    }
                )
                Text(
                    when {
                        correctAnswer == null -> "暂无参考答案"
                        selectedAnswer == null -> "本题未作答"
                        isCorrect -> "回答正确"
                        else -> "回答错误"
                    },
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Text("正确答案：${correctAnswer ?: "—"}", style = MaterialTheme.typography.bodyLarge)
            explanation?.takeIf { it.isNotBlank() }?.let {
                Text("解析：$it", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

private fun readingOptions(section: ExamSectionData, question: ExamQuestion): Map<String, String> {
    val subsection = section.subsections.firstOrNull { it.key == question.subsection }
    return when {
        question.subsection?.endsWith('A') == true -> subsection?.materials?.wordBank.orEmpty()
        question.subsection?.endsWith('B') == true -> subsection?.materials?.paragraphs.orEmpty()
        else -> question.options
    }
}

@Composable
private fun ReadingMaterial(section: ExamSectionData, question: ExamQuestion) {
    val subsection = section.subsections.firstOrNull { it.key == question.subsection } ?: return
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(subsection.title, style = MaterialTheme.typography.titleMedium)
        when (question.subsection) {
            "sectionA" -> {
                subsection.materials.passage?.let { Text(it, style = MaterialTheme.typography.bodyLarge) }
                if (subsection.materials.wordBank.isNotEmpty()) {
                    Text("词库", style = MaterialTheme.typography.titleSmall)
                    Text(subsection.materials.wordBank.toSortedMap().entries.joinToString("   ") { "${it.key}. ${it.value}" })
                }
            }
            "sectionB" -> subsection.materials.paragraphs.toSortedMap().forEach { (key, text) ->
                Text("$key  $text", style = MaterialTheme.typography.bodyLarge)
            }
            "sectionC" -> subsection.materials.passages.firstOrNull { it.id == question.passageId }?.let {
                Text(it.title, style = MaterialTheme.typography.titleSmall)
                Text(it.text, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

private fun sectionLabel(key: String?): String = when (key) {
    "sectionA" -> "Section A"
    "sectionB" -> "Section B"
    "sectionC" -> "Section C"
    else -> "阅读"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingResultScreen(
    exam: Exam,
    answers: Map<String, String>,
    onRestart: () -> Unit,
    onBack: () -> Unit
) {
    val score = scoreReading(exam, answers)
    val allowRetry = exam.appConfig?.allowRetry ?: true
    // Question order already follows Section A/B/C, so grouping by the parsed subsection keeps the
    // review in exam order and works for any number of subsections in a future paper.
    val groups = score.outcomes.groupBy { it.subsectionKey ?: "reading" }
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = Color.Transparent, // the root scaffold owns the background and the ambient light
        contentColor = MaterialTheme.colorScheme.onBackground,
        topBar = {
            TopAppBar(
                title = { Text("阅读结果") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        bottomBar = {
            Row(
                Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (allowRetry) {
                    PrimaryButton("重新作答", onRestart, Modifier.weight(1f))
                }
                SecondaryButton("返回真题详情", onBack, Modifier.weight(1f))
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { ReadingScoreSummary(exam, score) }
            item { Text("逐题回顾", style = MaterialTheme.typography.titleLarge) }
            groups.forEach { (key, outcomes) ->
                item(key = "section-$key") { ReadingReviewSectionHeader(outcomes) }
                items(outcomes, key = { it.questionId }) { outcome -> ReadingReviewCard(outcome) }
            }
        }
    }
}

@Composable
private fun ReadingScoreSummary(exam: Exam, score: ReadingScore) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(exam.title, style = MaterialTheme.typography.titleLarge)
            Text("阅读 · ${score.total} 题", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        score.earnedPoints.toString(),
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        " / ${score.maxPoints} 分",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    if (score.maxPoints == 0) "题库缺少参考答案，暂无法判分"
                    else "正确率 ${score.correctRate ?: 0}%",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ReadingScoreStatCard("正确", score.correct, Modifier.weight(1f))
            ReadingScoreStatCard("错误", score.incorrect, Modifier.weight(1f))
            ReadingScoreStatCard("未作答", score.unanswered, Modifier.weight(1f))
        }
    }
}

@Composable
private fun ReadingScoreStatCard(label: String, value: Int, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(value.toString(), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ReadingReviewSectionHeader(outcomes: List<ReadingQuestionOutcome>) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            outcomes.firstOrNull()?.subsectionLabel ?: "阅读",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f)
        )
        Text(
            "${outcomes.firstOrNull()?.number ?: 0} - ${outcomes.lastOrNull()?.number ?: 0}",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ReadingReviewCard(outcome: ReadingQuestionOutcome) {
    val status = when {
        !outcome.gradable -> "暂无答案"
        outcome.correct -> "✓ 回答正确"
        !outcome.answered -> "— 未作答"
        else -> "✗ 回答错误"
    }
    val accent = when {
        !outcome.gradable -> MaterialTheme.colorScheme.outline
        outcome.correct -> MaoDanSuccess
        !outcome.answered -> MaterialTheme.colorScheme.outline
        else -> MaterialTheme.colorScheme.error
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.55f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "第 ${outcome.number} 题",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                Text(status, style = MaterialTheme.typography.labelLarge)
            }
            outcome.question?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                "你的答案：${outcome.selectedAnswer ?: "—"}　正确答案：${outcome.correctAnswer ?: "—"}",
                style = MaterialTheme.typography.bodyLarge
            )
            outcome.explanation?.let {
                Text("解析：$it", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
