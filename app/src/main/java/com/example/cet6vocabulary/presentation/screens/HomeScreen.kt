package com.example.cet6vocabulary.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
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
import com.example.cet6vocabulary.ui.components.CET6ErrorState
import com.example.cet6vocabulary.ui.components.CET6LoadingState
import com.example.cet6vocabulary.ui.components.CET6SectionTitle
import com.example.cet6vocabulary.ui.components.AnimatedEnterBox
import com.example.cet6vocabulary.ui.components.LucideHomeIcons
import com.example.cet6vocabulary.ui.components.LucideNavigationIcons
import com.example.cet6vocabulary.ui.components.MaoDanCard
import com.example.cet6vocabulary.ui.components.rememberTiltState
import com.example.cet6vocabulary.ui.components.rememberMagneticState
import com.example.cet6vocabulary.ui.components.magnetic
import com.example.cet6vocabulary.ui.components.tilt
import com.example.cet6vocabulary.ui.components.MaoDanOutlinedCard
import com.example.cet6vocabulary.ui.components.MaoDanProgressBar
import com.example.cet6vocabulary.ui.components.ShinyText
import com.example.cet6vocabulary.ui.components.pressScale
import com.example.cet6vocabulary.ui.components.rememberPressScale
import com.example.cet6vocabulary.ui.theme.CET6VocabularyTheme
import com.example.cet6vocabulary.ui.theme.MaoDanDimens
import com.example.cet6vocabulary.ui.theme.MaoDanMotion
import com.example.cet6vocabulary.ui.theme.MaoDanShapes

private sealed interface HomeUiState {
    data object Loading : HomeUiState
    data object Error : HomeUiState
    data class Success(val statistics: LearningStatistics, val wordBookCount: Int) : HomeUiState
}

@Composable
fun HomeScreen(
    words: WordRepository,
    records: LearningRecordRepository,
    wordBook: WordBookRepository,
    refreshKey: Int = 0,
    onOpenStudy: () -> Unit,
    onOpenSpelling: () -> Unit,
    onOpenWordBook: () -> Unit,
    onOpenExams: () -> Unit
) {
    var state by remember { mutableStateOf<HomeUiState>(HomeUiState.Loading) }
    var reloadKey by remember { mutableStateOf(0) }

    LaunchedEffect(refreshKey, reloadKey) {
        state = HomeUiState.Loading
        state = try {
            val vocabulary = words.getAllWords()
            val statistics = calculateLearningStatistics(
                vocabulary.mapTo(mutableSetOf()) { it.id },
                records.getAllRecords()
            )
            HomeUiState.Success(statistics, wordBook.getWordBookCount())
        } catch (_: Exception) {
            HomeUiState.Error
        }
    }

    when (val current = state) {
        HomeUiState.Loading -> CET6LoadingState(message = "正在加载学习数据")
        HomeUiState.Error -> CET6ErrorState(
            message = "学习数据加载失败",
            actionText = "重新加载",
            onActionClick = { reloadKey++ }
        )
        is HomeUiState.Success -> HomeContent(
            statistics = current.statistics,
            wordBookCount = current.wordBookCount,
            onOpenStudy = onOpenStudy,
            onOpenSpelling = onOpenSpelling,
            onOpenWordBook = onOpenWordBook,
            onOpenExams = onOpenExams
        )
    }
}

@Composable
private fun HomeContent(
    statistics: LearningStatistics,
    wordBookCount: Int,
    onOpenStudy: () -> Unit,
    onOpenSpelling: () -> Unit,
    onOpenWordBook: () -> Unit,
    onOpenExams: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = Color.Transparent, // the root scaffold owns the background and the ambient light
        contentColor = MaterialTheme.colorScheme.onBackground
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MaoDanDimens.space16, vertical = MaoDanDimens.space20),
            verticalArrangement = Arrangement.spacedBy(MaoDanDimens.space20)
        ) {
            HomeHeader()
            // The hero carries the page's one deliberate entrance; everything else arrives with the page.
            AnimatedEnterBox(modifier = Modifier.fillMaxWidth()) {
                HomeProgressHero(statistics)
            }
            HomeQuickActions(onOpenStudy = onOpenStudy, onOpenSpelling = onOpenSpelling)
            Column(verticalArrangement = Arrangement.spacedBy(MaoDanDimens.space12)) {
                HomeEntryCard(
                    title = "词汇本",
                    description = if (wordBookCount == 0) "还没有收藏单词" else "已收藏 $wordBookCount 个单词",
                    icon = LucideHomeIcons.Bookmark,
                    onClick = onOpenWordBook
                )
                HomeEntryCard(
                    title = "真题训练",
                    description = "CET-6 历年真题",
                    icon = LucideHomeIcons.FileText,
                    onClick = onOpenExams
                )
            }
            HomeStatsSection(statistics)
        }
    }
}

@Composable
private fun HomeHeader() {
    Column(verticalArrangement = Arrangement.spacedBy(MaoDanDimens.space4)) {
        // The page's one deliberate focal point; the subtitle below stays a plain label.
        ShinyText(
            "CET6 高频词汇",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            "高频词汇，循序掌握",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun HomeProgressHero(statistics: LearningStatistics) {
    // The hero is the one surface worth lighting up; the rest of the page stays flat on purpose.
    MaoDanCard(modifier = Modifier.fillMaxWidth(), spotlightEnabled = true) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaoDanDimens.space20),
            verticalArrangement = Arrangement.spacedBy(MaoDanDimens.space16)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                HomeIconBadge(LucideHomeIcons.TrendingUp)
                Spacer(Modifier.width(MaoDanDimens.space12))
                Text(
                    "学习进度",
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "已完成 ${statistics.learnedPercent}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "剩余 ${statistics.unlearnedWords} 词",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun HomeQuickActions(onOpenStudy: () -> Unit, onOpenSpelling: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(MaoDanDimens.space12)) {
        CET6SectionTitle("开始学习")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MaoDanDimens.space12)
        ) {
            HomeQuickActionCard(
                title = "背诵",
                description = "开始复习单词",
                icon = LucideNavigationIcons.Study,
                onClick = onOpenStudy,
                modifier = Modifier.weight(1f),
                shineTitle = true,
                tiltEnabled = true
            )
            HomeQuickActionCard(
                title = "拼写",
                description = "检查拼写能力",
                icon = LucideNavigationIcons.Spell,
                onClick = onOpenSpelling,
                modifier = Modifier.weight(1f),
                magneticEnabled = true
            )
        }
    }
}

@Composable
private fun HomeQuickActionCard(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shineTitle: Boolean = false,
    tiltEnabled: Boolean = false,
    magneticEnabled: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressScale = rememberPressScale(
        interactionSource = interactionSource,
        pressedScale = MaoDanMotion.CardPressedScale
    )
    val tiltState = rememberTiltState(tiltEnabled)
    val magneticState = rememberMagneticState(magneticEnabled)
    MaoDanOutlinedCard(
        modifier = modifier
            .magnetic(magneticState)
            .tilt(tiltState)
            .pressScale(pressScale)
            .clip(MaoDanShapes.large)
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick
            ),
        spotlightEnabled = true
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = MaoDanDimens.iconButtonSize)
                .padding(MaoDanDimens.space16),
            verticalArrangement = Arrangement.spacedBy(MaoDanDimens.space12)
        ) {
            HomeIconBadge(icon)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                // The primary action follows the page title by one beat; the other cards keep
                // enabled = false, which renders exactly like the plain Text it replaces.
                ShinyText(
                    text = title,
                    enabled = shineTitle,
                    delayMillis = MaoDanMotion.ShineDelayMillis + MaoDanMotion.EnterStaggerMillis,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    description,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun HomeEntryCard(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
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
            ),
        spotlightEnabled = true
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = MaoDanDimens.iconButtonSize)
                .padding(MaoDanDimens.space16),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HomeIconBadge(icon)
            Spacer(Modifier.width(MaoDanDimens.space12))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    description,
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
private fun HomeStatsSection(statistics: LearningStatistics) {
    Column(verticalArrangement = Arrangement.spacedBy(MaoDanDimens.space12)) {
        CET6SectionTitle("学习统计")
        MaoDanCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = MaoDanDimens.space16),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HomeStatItem(
                    label = "待学习",
                    value = statistics.unlearnedWords.toString(),
                    modifier = Modifier.weight(1f)
                )
                HomeStatDivider()
                HomeStatItem(
                    label = "正确率",
                    value = "${statistics.accuracyPercent}%",
                    modifier = Modifier.weight(1f)
                )
                HomeStatDivider()
                HomeStatItem(
                    label = "错误次数",
                    value = statistics.wrongCount.toString(),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun HomeStatItem(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MaoDanDimens.space4)
    ) {
        Text(
            value,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun HomeStatDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(28.dp)
            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
    )
}

@Composable
private fun HomeIconBadge(icon: ImageVector, modifier: Modifier = Modifier) {
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
private fun HomePreview() {
    CET6VocabularyTheme {
        HomeContent(
            statistics = calculateLearningStatistics(
                wordIds = (1..781).toSet(),
                records = emptyList()
            ).copy(learnedWords = 3, unlearnedWords = 778),
            wordBookCount = 5,
            onOpenStudy = {},
            onOpenSpelling = {},
            onOpenWordBook = {},
            onOpenExams = {}
        )
    }
}
