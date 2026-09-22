@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.cet6vocabulary.presentation.screens

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.example.cet6vocabulary.data.repository.LearningRecordRepository
import com.example.cet6vocabulary.data.repository.WordBookRepository
import com.example.cet6vocabulary.data.repository.WordRepository
import com.example.cet6vocabulary.presentation.model.LearningStatistics
import com.example.cet6vocabulary.presentation.model.calculateLearningStatistics
import com.example.cet6vocabulary.ui.components.CET6EmptyState
import com.example.cet6vocabulary.ui.components.CET6ErrorState
import com.example.cet6vocabulary.ui.components.CET6LoadingState
import com.example.cet6vocabulary.ui.components.CET6SectionTitle
import com.example.cet6vocabulary.ui.components.CET6StatCard
import com.example.cet6vocabulary.ui.components.PrimaryButton

sealed interface MyPageUiState {
    data object Loading : MyPageUiState
    data object Empty : MyPageUiState
    data object Error : MyPageUiState
    data class Success(val statistics: LearningStatistics) : MyPageUiState
}

@Composable
fun MyScreen(
    words: WordRepository,
    records: LearningRecordRepository,
    wordBook: WordBookRepository,
    onOpenWordBook: () -> Unit,
    refreshKey: Int = 0
) {
    var state by remember { mutableStateOf<MyPageUiState>(MyPageUiState.Loading) }
    var reloadKey by remember { mutableIntStateOf(0) }
    var wordBookCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(reloadKey, refreshKey) {
        state = MyPageUiState.Loading
        state = try {
            wordBookCount = wordBook.getWordBookCount()
            val wordIds = words.getAllWords().mapTo(mutableSetOf()) { it.id }
            if (wordIds.isEmpty()) {
                MyPageUiState.Empty
            } else {
                MyPageUiState.Success(calculateLearningStatistics(wordIds, records.getAllRecords()))
            }
        } catch (_: Exception) {
            MyPageUiState.Error
        }
    }

    Scaffold(contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0), topBar = { TopAppBar(title = { Text("我的") }) }) { padding ->
        when (val current = state) {
            MyPageUiState.Loading -> CET6LoadingState(Modifier.fillMaxSize().padding(padding), "正在加载学习数据")
            MyPageUiState.Empty -> Column(Modifier.fillMaxSize().padding(padding), verticalArrangement = Arrangement.Center) {
                CET6EmptyState("暂无词汇", "词库为空，暂时没有可展示的学习统计。")
            }
            MyPageUiState.Error -> Column(Modifier.fillMaxSize().padding(padding), verticalArrangement = Arrangement.Center) {
                CET6ErrorState("学习数据加载失败", actionText = "重新加载", onActionClick = { reloadKey++ })
            }
            is MyPageUiState.Success -> MyContent(current.statistics, wordBookCount, onOpenWordBook, padding)
        }
    }
}

@Composable
private fun MyContent(
    statistics: LearningStatistics,
    wordBookCount: Int,
    onOpenWordBook: () -> Unit,
    padding: PaddingValues
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        OverviewCard(statistics)
        CET6SectionTitle("学习统计")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CET6StatCard(statistics.learnedWords.toString(), "已学习", Modifier.weight(1f))
            CET6StatCard(statistics.unlearnedWords.toString(), "待学习", Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CET6StatCard("${statistics.accuracyPercent}%", "正确率", Modifier.weight(1f))
            CET6StatCard(statistics.wrongCount.toString(), "错误次数", Modifier.weight(1f))
        }
        CET6SectionTitle("掌握情况")
        MasteryCard(statistics)
        CET6SectionTitle("我的单词本")
        WordBookCard(wordBookCount, onOpenWordBook)
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun OverviewCard(statistics: LearningStatistics) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("学习概览", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text(
                "${statistics.learnedWords} / ${statistics.totalWords}",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            LinearProgressIndicator(
                progress = { statistics.learningProgress },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
            )
            Text(
                "学习进度 ${statistics.learnedPercent}%",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun MasteryCard(statistics: LearningStatistics) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            MasteryRow("0级 · 未开始", statistics.mastery0Count)
            MasteryRow("1级 · 学习中", statistics.mastery1Count)
            MasteryRow("2级 · 已练习", statistics.mastery2Count)
            MasteryRow("3级 · 高熟练度", statistics.mastery3Count)
        }
    }
}

@Composable
private fun MasteryRow(label: String, count: Int) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Text(count.toString(), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun WordBookCard(count: Int, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Star, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f).padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("我的单词本", style = MaterialTheme.typography.titleMedium)
                Text("$count 个单词", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "打开我的单词本", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 760)
@Composable
private fun MyPreview() {
    MyContent(
        statistics = calculateLearningStatistics(setOf(1, 2, 3, 4, 5), emptyList()),
        wordBookCount = 2,
        onOpenWordBook = {},
        padding = PaddingValues()
    )
}
