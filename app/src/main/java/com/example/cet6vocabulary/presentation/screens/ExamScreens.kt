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
import androidx.compose.material.icons.filled.Headphones
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.cet6vocabulary.data.model.Exam
import com.example.cet6vocabulary.data.model.ExamSection
import com.example.cet6vocabulary.data.model.ExamSectionData
import com.example.cet6vocabulary.data.model.ExamQuestion
import com.example.cet6vocabulary.data.repository.ExamRepository
import com.example.cet6vocabulary.ui.components.CET6EmptyState
import com.example.cet6vocabulary.ui.components.CET6ErrorState
import com.example.cet6vocabulary.ui.components.CET6LoadingState
import com.example.cet6vocabulary.ui.components.PrimaryButton
import com.example.cet6vocabulary.ui.components.SecondaryButton

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
        topBar = {
            TopAppBar(
                title = { Text("CET-6 真题") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
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
        topBar = {
            TopAppBar(
                title = { Text("真题详情") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
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
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
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
    onComplete: (Int) -> Unit,
    onBack: () -> Unit
) {
    val reading = exam.sections.first { it.section == ExamSection.READING }
    val questions = reading.questions
    var questionIndex by remember { mutableIntStateOf(0) }
    var showUnansweredDialog by remember { mutableStateOf(false) }
    val question = questions.getOrNull(questionIndex)

    if (question == null) {
        ReadingResultScreen(exam, answers.size, onRestart = { answers.clear(); questionIndex = 0 }, onBack = onBack)
        return
    }

    val canGoBack = questionIndex > 0
    val isLast = questionIndex == questions.lastIndex
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("阅读 · ${sectionLabel(question.subsection)}") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        bottomBar = {
            Row(
                Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(onClick = { questionIndex-- }, enabled = canGoBack, modifier = Modifier.weight(1f)) {
                    Text("上一题")
                }
                Button(
                    onClick = {
                        if (isLast) {
                            if (answers.containsKey(question.questionId)) onComplete(answers.size)
                            else showUnansweredDialog = true
                        } else if (!answers.containsKey(question.questionId)) {
                            showUnansweredDialog = true
                        } else {
                            questionIndex++
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) { Text(if (isLast) "完成阅读" else "下一题") }
            }
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("${question.number} / ${questions.last().number}", style = MaterialTheme.typography.titleLarge)
            Text("第 ${questionIndex + 1} / ${questions.size} 题", color = MaterialTheme.colorScheme.onSurfaceVariant)
            ReadingMaterial(reading, question)
            Text(question.question.orEmpty(), style = MaterialTheme.typography.titleMedium)
            readingOptions(reading, question).toSortedMap().forEach { (key, value) ->
                val selected = answers[question.questionId] == key
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { answers[question.questionId] = key },
                    colors = CardDefaults.cardColors(
                        containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
                    )
                ) {
                    Text("$key. $value", Modifier.fillMaxWidth().padding(14.dp))
                }
            }
        }
    }

    if (showUnansweredDialog) {
        AlertDialog(
            onDismissRequest = { showUnansweredDialog = false },
            title = { Text("本题尚未作答") },
            text = { Text("是否继续下一题？") },
            confirmButton = {
                TextButton(onClick = {
                    showUnansweredDialog = false
                    if (isLast) onComplete(answers.size) else questionIndex++
                }) { Text("继续") }
            },
            dismissButton = { TextButton(onClick = { showUnansweredDialog = false }) { Text("返回答题") } }
        )
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
    answeredCount: Int,
    onRestart: () -> Unit,
    onBack: () -> Unit
) {
    val total = exam.sections.first { it.section == ExamSection.READING }.questions.size
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("阅读结果") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("阅读完成", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(20.dp))
            Text("$total 题", style = MaterialTheme.typography.titleLarge)
            Text("已作答 $answeredCount 题")
            Text("未作答 ${total - answeredCount} 题")
            Spacer(Modifier.height(24.dp))
            PrimaryButton("重新开始", onRestart, Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            SecondaryButton("返回真题详情", onBack, Modifier.fillMaxWidth())
        }
    }
}
