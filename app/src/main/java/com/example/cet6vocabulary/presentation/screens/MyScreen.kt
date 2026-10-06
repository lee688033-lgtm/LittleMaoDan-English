package com.example.cet6vocabulary.presentation.screens

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.cet6vocabulary.data.repository.LearningRecordRepository
import com.example.cet6vocabulary.data.repository.WordBookRepository
import com.example.cet6vocabulary.data.repository.WordRepository
import com.example.cet6vocabulary.presentation.model.LearningStatistics
import com.example.cet6vocabulary.presentation.model.calculateLearningStatistics
import com.example.cet6vocabulary.ui.components.AnimatedEnterBox
import com.example.cet6vocabulary.ui.components.CET6EmptyState
import com.example.cet6vocabulary.ui.components.CET6ErrorState
import com.example.cet6vocabulary.ui.components.CET6LoadingState
import com.example.cet6vocabulary.ui.components.CET6SectionTitle
import com.example.cet6vocabulary.ui.components.LucideHomeIcons
import com.example.cet6vocabulary.ui.components.LucideMineIcons
import com.example.cet6vocabulary.ui.components.MaoDanCard
import com.example.cet6vocabulary.ui.components.MaoDanOutlinedCard
import com.example.cet6vocabulary.ui.components.MaoDanProgressBar
import com.example.cet6vocabulary.ui.components.pressScale
import com.example.cet6vocabulary.ui.components.rememberPressScale
import com.example.cet6vocabulary.ui.theme.CET6VocabularyTheme
import com.example.cet6vocabulary.ui.theme.MaoDanDimens
import com.example.cet6vocabulary.ui.theme.MaoDanMotion
import com.example.cet6vocabulary.ui.theme.MaoDanShapes

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

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = Color.Transparent, // the root scaffold owns the background and the ambient light
        contentColor = MaterialTheme.colorScheme.onBackground
    ) { padding ->
        when (val current = state) {
            MyPageUiState.Loading -> CET6LoadingState(
                Modifier.fillMaxSize().padding(padding),
                "正在加载学习数据"
            )
            MyPageUiState.Empty -> Column(
                Modifier.fillMaxSize().padding(padding),
                verticalArrangement = Arrangement.Center
            ) {
                CET6EmptyState(
                    "暂无词汇",
                    "词库为空，暂时没有可展示的学习统计。",
                    icon = LucideMineIcons.BookOpen
                )
            }
            MyPageUiState.Error -> Column(
                Modifier.fillMaxSize().padding(padding),
                verticalArrangement = Arrangement.Center
            ) {
                CET6ErrorState(
                    "学习数据加载失败",
                    actionText = "重新加载",
                    onActionClick = { reloadKey++ }
                )
            }
            is MyPageUiState.Success -> MyContent(
                statistics = current.statistics,
                wordBookCount = wordBookCount,
                onOpenWordBook = onOpenWordBook,
                padding = padding
            )
        }
    }
}

@Composable
private fun MyContent(
    statistics: LearningStatistics,
    wordBookCount: Int,
    onOpenWordBook: () -> Unit,
    padding: PaddingValues,
    modifier: Modifier = Modifier
) {
    // One entrance per band: the title lands first, the overview card follows a beat later, and the
    // remaining sections arrive together. Short enough that the page never looks like it is still
    // assembling, and every block degrades to a cut under reduced motion.
    val laterDelay = MaoDanMotion.CardEnterDelayMillis + MaoDanMotion.EnterStaggerMillis
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = MaoDanDimens.pageHorizontal),
        verticalArrangement = Arrangement.spacedBy(MaoDanDimens.space20)
    ) {
        Spacer(Modifier.height(MaoDanDimens.space8))
        AnimatedEnterBox(modifier = Modifier.fillMaxWidth(), delayMillis = 0) { MyHeader() }
        AnimatedEnterBox(modifier = Modifier.fillMaxWidth()) { MyOverviewCard(statistics) }
        AnimatedEnterBox(modifier = Modifier.fillMaxWidth(), delayMillis = laterDelay) {
            MyStatSection(statistics)
        }
        AnimatedEnterBox(modifier = Modifier.fillMaxWidth(), delayMillis = laterDelay) {
            MyMasterySection(statistics)
        }
        AnimatedEnterBox(modifier = Modifier.fillMaxWidth(), delayMillis = laterDelay) {
            MyToolsSection(wordBookCount, onOpenWordBook)
        }
        Spacer(Modifier.height(MaoDanDimens.space24))
    }
}

@Composable
private fun MyHeader(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaoDanDimens.space4)
    ) {
        Text(
            "我的",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            "学习记录与词汇管理",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MyOverviewCard(statistics: LearningStatistics, modifier: Modifier = Modifier) {
    MaoDanCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaoDanDimens.space20),
            verticalArrangement = Arrangement.spacedBy(MaoDanDimens.space16)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MyIconBadge(LucideMineIcons.ChartNoAxesColumn)
                Spacer(Modifier.width(MaoDanDimens.space12))
                Text(
                    "学习概览",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    statistics.learnedWords.toString(),
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    " / ${statistics.totalWords} 词",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = MaoDanDimens.space4)
                )
            }
            MaoDanProgressBar(progress = statistics.learningProgress)
            Text(
                "学习进度 ${statistics.learnedPercent}%",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MyStatSection(statistics: LearningStatistics, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaoDanDimens.space12)
    ) {
        CET6SectionTitle("学习统计")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MaoDanDimens.space12)
        ) {
            MyStatTile(
                label = "已学习",
                value = statistics.learnedWords.toString(),
                modifier = Modifier.weight(1f)
            )
            MyStatTile(
                label = "待学习",
                value = statistics.unlearnedWords.toString(),
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MaoDanDimens.space12)
        ) {
            MyStatTile(
                label = "正确率",
                value = "${statistics.accuracyPercent}%",
                modifier = Modifier.weight(1f)
            )
            MyStatTile(
                label = "错误次数",
                value = statistics.wrongCount.toString(),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun MyStatTile(label: String, value: String, modifier: Modifier = Modifier) {
    MaoDanOutlinedCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = MaoDanDimens.iconButtonSize)
                .padding(MaoDanDimens.cardContent),
            verticalArrangement = Arrangement.spacedBy(MaoDanDimens.space4)
        ) {
            Text(
                value,
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MyMasterySection(statistics: LearningStatistics, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaoDanDimens.space12)
    ) {
        CET6SectionTitle("掌握情况")
        MaoDanCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(MaoDanDimens.cardContent),
                verticalArrangement = Arrangement.spacedBy(MaoDanDimens.space12)
            ) {
                MyMasteryRow("0级 · 未开始", statistics.mastery0Count, statistics.totalWords)
                MyMasteryRow("1级 · 学习中", statistics.mastery1Count, statistics.totalWords)
                MyMasteryRow("2级 · 已练习", statistics.mastery2Count, statistics.totalWords)
                MyMasteryRow("3级 · 高熟练度", statistics.mastery3Count, statistics.totalWords)
            }
        }
    }
}

@Composable
private fun MyMasteryRow(label: String, count: Int, totalWords: Int, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaoDanDimens.space8)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                count.toString(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        MyMasteryBar(count = count, totalWords = totalWords, modifier = Modifier.fillMaxWidth())
    }
}

/** Proportion of the whole wordbook sitting at this mastery level; no new data is derived. */
@Composable
private fun MyMasteryBar(count: Int, totalWords: Int, modifier: Modifier = Modifier) {
    val fraction = if (totalWords <= 0) 0f else (count.toFloat() / totalWords).coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .height(MaoDanDimens.space4)
            .clip(MaoDanShapes.pill)
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(
            Modifier
                .fillMaxWidth(fraction)
                .fillMaxHeight()
                .clip(MaoDanShapes.pill)
                .background(MaterialTheme.colorScheme.primary)
        )
    }
}

@Composable
private fun MyToolsSection(
    wordBookCount: Int,
    onOpenWordBook: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaoDanDimens.space12)
    ) {
        CET6SectionTitle("学习工具")
        MyWordBookCard(count = wordBookCount, onClick = onOpenWordBook)
    }
}

@Composable
private fun MyWordBookCard(count: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressScale = rememberPressScale(
        interactionSource = interactionSource,
        pressedScale = MaoDanMotion.CardPressedScale
    )
    MaoDanOutlinedCard(
        modifier = modifier
            .fillMaxWidth()
            .pressScale(pressScale)
            .clip(MaoDanShapes.large)
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = MaoDanDimens.iconButtonSize)
                .padding(MaoDanDimens.cardContent),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MyIconBadge(LucideHomeIcons.Bookmark)
            Spacer(Modifier.width(MaoDanDimens.space12))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(MaoDanDimens.space4)
            ) {
                Text(
                    "我的单词本",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    if (count == 0) "还没有收藏单词" else "已收藏 $count 个单词",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(MaoDanDimens.space8))
            Icon(
                imageVector = LucideHomeIcons.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MyIconBadge(icon: ImageVector, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(MaoDanShapes.medium)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 760)
@Composable
private fun MyPreview() {
    CET6VocabularyTheme {
        MyContent(
            statistics = calculateLearningStatistics((1..936).toSet(), emptyList())
                .copy(learnedWords = 4, unlearnedWords = 932, mastery0Count = 932),
            wordBookCount = 1,
            onOpenWordBook = {},
            padding = PaddingValues()
        )
    }
}
