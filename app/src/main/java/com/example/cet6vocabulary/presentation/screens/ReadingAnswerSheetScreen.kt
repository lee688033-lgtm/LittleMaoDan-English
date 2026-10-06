package com.example.cet6vocabulary.presentation.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.cet6vocabulary.data.model.Exam
import com.example.cet6vocabulary.data.model.ExamSection
import com.example.cet6vocabulary.presentation.model.ReadingSheetItem
import com.example.cet6vocabulary.presentation.model.ReadingSheetStatus
import com.example.cet6vocabulary.presentation.model.buildReadingSheet
import com.example.cet6vocabulary.ui.components.PrimaryButton
import com.example.cet6vocabulary.ui.components.CET6EmptyState

private val SheetCellHeight = 52.dp
private val SheetCellMinWidth = 52.dp
private val SheetCellGap = 8.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingAnswerSheetScreen(
    exam: Exam,
    answers: Map<String, String>,
    currentIndex: Int,
    onSelectQuestion: (Int) -> Unit,
    onFinish: (Int) -> Unit,
    onBack: () -> Unit
) {
    val reading = exam.sections.firstOrNull { it.section == ExamSection.READING }
    if (reading == null) {
        // A navigation callback must not fire while composing; show the empty state instead.
        ReadingSheetEmptyScreen(exam = exam, onBack = onBack)
        return
    }
    // Cheap to rebuild (30 entries) and reading the answer map inside remember() would cache a
    // stale sheet, so it is derived on every recomposition.
    val sheet = buildReadingSheet(reading, answers, currentIndex)
    var showFinishDialog by remember { mutableStateOf(false) }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = Color.Transparent, // the root scaffold owns the background and the ambient light
        contentColor = MaterialTheme.colorScheme.onBackground,
        topBar = {
            TopAppBar(
                title = { Text("阅读答题卡") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        bottomBar = {
            Row(Modifier.fillMaxWidth().padding(12.dp)) {
                PrimaryButton(
                    text = "完成阅读",
                    onClick = {
                        if (sheet.unanswered > 0) showFinishDialog = true else onFinish(sheet.answered)
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(exam.title, style = MaterialTheme.typography.titleLarge)
                Text(
                    "${exam.date} · 第 ${exam.setNumber} 套 · 阅读 ${sheet.total} 题",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SheetStatCard("已作答", "${sheet.answered} / ${sheet.total}", Modifier.weight(1f))
                SheetStatCard("未作答", "${sheet.unanswered} / ${sheet.total}", Modifier.weight(1f))
            }
            SheetLegend()
            sheet.sections.forEach { section ->
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            section.label,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            "${section.firstNumber} - ${section.lastNumber}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    SheetGrid(section.items, onSelectQuestion)
                }
            }
        }
    }

    if (showFinishDialog) {
        AlertDialog(
            onDismissRequest = { showFinishDialog = false },
            title = { Text("还有 ${sheet.unanswered} 道题未作答") },
            text = { Text("是否完成阅读？") },
            confirmButton = {
                TextButton(onClick = {
                    showFinishDialog = false
                    onFinish(sheet.answered)
                }) { Text("完成阅读") }
            },
            dismissButton = { TextButton(onClick = { showFinishDialog = false }) { Text("返回答题") } }
        )
    }
}

@Composable
private fun SheetStatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(value, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SheetLegend() {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        SheetLegendItem(
            label = "当前题",
            container = MaterialTheme.colorScheme.primary,
            border = MaterialTheme.colorScheme.primary
        )
        SheetLegendItem(
            label = "已作答",
            container = MaterialTheme.colorScheme.primaryContainer,
            border = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
        )
        SheetLegendItem(
            label = "未作答",
            container = MaterialTheme.colorScheme.surface,
            border = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
        )
    }
}

@Composable
private fun SheetLegendItem(label: String, container: Color, border: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            Modifier.size(14.dp)
                .clip(MaterialTheme.shapes.small)
                .background(container)
                .border(1.dp, border, MaterialTheme.shapes.small)
        )
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SheetGrid(items: List<ReadingSheetItem>, onSelectQuestion: (Int) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        // Column count follows the available width so every number keeps a comfortable target.
        val columns = ((maxWidth + SheetCellGap) / (SheetCellMinWidth + SheetCellGap)).toInt().coerceAtLeast(3)
        Column(verticalArrangement = Arrangement.spacedBy(SheetCellGap)) {
            items.chunked(columns).forEach { rowItems ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(SheetCellGap)) {
                    rowItems.forEach { item -> SheetCell(item, Modifier.weight(1f), onSelectQuestion) }
                    repeat(columns - rowItems.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun SheetCell(item: ReadingSheetItem, modifier: Modifier = Modifier, onSelectQuestion: (Int) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val shape = MaterialTheme.shapes.medium
    val container: Color
    val border: Color
    val content: Color
    val stateLabel: String
    when (item.status) {
        ReadingSheetStatus.CURRENT -> {
            container = scheme.primary
            border = scheme.primary
            content = scheme.onPrimary
            stateLabel = "当前题"
        }
        ReadingSheetStatus.ANSWERED -> {
            container = scheme.primaryContainer
            border = scheme.primary.copy(alpha = 0.35f)
            content = scheme.primary
            stateLabel = "已作答"
        }
        ReadingSheetStatus.UNANSWERED -> {
            container = scheme.surface
            border = scheme.outline.copy(alpha = 0.45f)
            content = scheme.onSurfaceVariant
            stateLabel = "未作答"
        }
    }
    Box(
        modifier = modifier
            .height(SheetCellHeight)
            .clip(shape)
            .background(container)
            .border(1.dp, border, shape)
            .clickable { onSelectQuestion(item.index) }
            .semantics { contentDescription = "第 ${item.number} 题 $stateLabel" },
        contentAlignment = Alignment.Center
    ) {
        Text(
            item.number.toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = if (item.status == ReadingSheetStatus.CURRENT) FontWeight.Bold else FontWeight.Normal,
            color = content
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReadingSheetEmptyScreen(exam: Exam, onBack: () -> Unit) {
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = Color.Transparent, // the root scaffold owns the background and the ambient light
        contentColor = MaterialTheme.colorScheme.onBackground,
        topBar = {
            TopAppBar(
                title = { Text("阅读答题卡") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        CET6EmptyState(
            title = "暂无阅读题目",
            description = "${exam.title} 还没有可用的阅读专项数据。",
            modifier = Modifier.fillMaxSize().padding(padding)
        )
    }
}
