package com.example.cet6vocabulary

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.example.cet6vocabulary.data.local.database.AppDatabase
import com.example.cet6vocabulary.data.repository.LearningRecordRepository
import com.example.cet6vocabulary.data.repository.ExamRepository
import com.example.cet6vocabulary.data.repository.StudyProgressRepository
import com.example.cet6vocabulary.data.repository.WordBookRepository
import com.example.cet6vocabulary.data.repository.WordRepository
import com.example.cet6vocabulary.presentation.screens.*
import com.example.cet6vocabulary.ui.theme.CET6VocabularyTheme
import com.example.cet6vocabulary.ui.components.BrandAvatarIcon
import com.example.cet6vocabulary.ui.components.BrandBottomBar
import com.example.cet6vocabulary.ui.components.BrandDictionaryIcon
import com.example.cet6vocabulary.ui.components.BrandEggIcon
import com.example.cet6vocabulary.ui.components.BrandMemoryIcon
import com.example.cet6vocabulary.ui.components.BrandSpellingIcon
import com.example.cet6vocabulary.ui.components.CET6NavigationItem

data class NavItem(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private enum class ExamRoute {
    LIST,
    DETAIL,
    PLACEHOLDER,
    READING,
    READING_RESULT
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.Transparent.toArgb(), Color.Transparent.toArgb()),
            navigationBarStyle = SystemBarStyle.auto(Color.Transparent.toArgb(), Color.Transparent.toArgb())
        )
        setContent { Cet6VocabularyApp() }
    }
}

@Composable
fun Cet6VocabularyApp() {
    val context = LocalContext.current.applicationContext
    val items = listOf(
        NavItem("首页", BrandEggIcon),
        NavItem("单词", BrandDictionaryIcon),
        NavItem("背诵", BrandMemoryIcon),
        NavItem("拼写", BrandSpellingIcon),
        NavItem("我的", BrandAvatarIcon)
    )
    var selected by remember { mutableIntStateOf(0) }
    var showWordBook by remember { mutableStateOf(false) }
    var studyWordBook by remember { mutableStateOf(false) }
    var examRoute by remember { mutableStateOf<ExamRoute?>(null) }
    var selectedExamId by remember { mutableStateOf<String?>(null) }
    val readingAnswers = remember { mutableStateMapOf<String, String>() }
    var readingAnsweredCount by remember { mutableIntStateOf(0) }
    var placeholderTitle by remember { mutableStateOf("真题练习") }
    var myRefreshKey by remember { mutableIntStateOf(0) }
    val wordRepository = remember { WordRepository(context) }
    val database = remember { AppDatabase.getInstance(context) }
    val learningRecords = remember { LearningRecordRepository(database.learningRecordDao()) }
    val studyProgress = remember { StudyProgressRepository(database.studyProgressDao()) }
    val wordBook = remember { WordBookRepository(database.wordBookDao()) }
    val examRepository = remember { ExamRepository(context) }
    BackHandler(enabled = showWordBook) {
        showWordBook = false
        selected = 4
        studyWordBook = false
        myRefreshKey++
    }
    BackHandler(enabled = !showWordBook && studyWordBook) {
        showWordBook = true
        studyWordBook = false
    }
    BackHandler(enabled = !showWordBook && examRoute != null) {
        when (examRoute) {
            ExamRoute.PLACEHOLDER -> examRoute = ExamRoute.DETAIL
            ExamRoute.READING -> examRoute = ExamRoute.DETAIL
            ExamRoute.READING_RESULT -> examRoute = ExamRoute.DETAIL
            ExamRoute.DETAIL -> examRoute = ExamRoute.LIST
            ExamRoute.LIST -> {
                examRoute = null
                selected = 0
            }
            null -> Unit
        }
    }

    CET6VocabularyTheme {
        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
            if (!showWordBook && examRoute == null) {
                BrandBottomBar(
                    items = items.map { CET6NavigationItem(it.label, it.icon) },
                    selectedIndex = selected,
                    onItemSelected = { selected = it; studyWordBook = false; myRefreshKey++ }
                )
            }
            }
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(if (showWordBook) PaddingValues() else padding)) {
                when {
                    !showWordBook && examRoute == ExamRoute.LIST -> ExamListScreen(
                        repository = examRepository,
                        onBack = { examRoute = null; selected = 0 },
                        onOpenExam = { examId -> selectedExamId = examId; examRoute = ExamRoute.DETAIL }
                    )
                    !showWordBook && examRoute == ExamRoute.DETAIL -> {
                        val exam = selectedExamId?.let(examRepository::getExam)
                        if (exam == null) {
                            ExamPlaceholderScreen("真题详情", onBack = { examRoute = ExamRoute.LIST })
                        } else {
                            ExamDetailScreen(
                                exam = exam,
                                onBack = { examRoute = ExamRoute.LIST },
                                onStart = { section ->
                                    placeholderTitle = when (section) {
                                        null -> "整套练习"
                                        com.example.cet6vocabulary.data.model.ExamSection.WRITING -> "写作"
                                        com.example.cet6vocabulary.data.model.ExamSection.LISTENING -> "听力"
                                        com.example.cet6vocabulary.data.model.ExamSection.READING -> "阅读"
                                        com.example.cet6vocabulary.data.model.ExamSection.TRANSLATION -> "翻译"
                                    }
                                    if (section == com.example.cet6vocabulary.data.model.ExamSection.READING) {
                                        readingAnswers.clear()
                                        examRoute = ExamRoute.READING
                                    } else {
                                        examRoute = ExamRoute.PLACEHOLDER
                                    }
                                }
                            )
                        }
                    }
                    !showWordBook && examRoute == ExamRoute.READING -> {
                        val exam = selectedExamId?.let(examRepository::getExam)
                        if (exam == null) ExamPlaceholderScreen(title = placeholderTitle, onBack = { examRoute = ExamRoute.DETAIL })
                        else ReadingPracticeScreen(exam, readingAnswers, { count -> readingAnsweredCount = count; examRoute = ExamRoute.READING_RESULT }, { examRoute = ExamRoute.DETAIL })
                    }
                    !showWordBook && examRoute == ExamRoute.READING_RESULT -> {
                        val exam = selectedExamId?.let(examRepository::getExam)
                        if (exam == null) ExamPlaceholderScreen(title = placeholderTitle, onBack = { examRoute = ExamRoute.DETAIL })
                        else ReadingResultScreen(exam, readingAnsweredCount, { readingAnswers.clear(); examRoute = ExamRoute.READING }, { examRoute = ExamRoute.DETAIL })
                    }
                    !showWordBook && examRoute == ExamRoute.PLACEHOLDER -> ExamPlaceholderScreen(
                        title = placeholderTitle,
                        onBack = { examRoute = ExamRoute.DETAIL }
                    )
                    showWordBook -> WordBookScreen(wordRepository, wordBook, onStartStudy = { showWordBook = false; selected = 2; studyWordBook = true }, onBack = { showWordBook = false; selected = 4; studyWordBook = false; myRefreshKey++ })
                    selected == 0 -> HomeScreen(wordRepository, learningRecords, wordBook, refreshKey = myRefreshKey, onOpenStudy = { selected = 2; studyWordBook = false }, onOpenSpelling = { selected = 3; studyWordBook = false }, onOpenWordBook = { showWordBook = true }, onOpenExams = { selected = 0; examRoute = ExamRoute.LIST })
                    selected == 1 -> WordScreen(wordRepository)
                    selected == 2 -> StudyScreen(wordRepository, wordBook, studyProgress, studyWordBook)
                    selected == 3 -> SpellingScreen(wordRepository, learningRecords, studyProgress)
                    selected == 4 -> MyScreen(wordRepository, learningRecords, wordBook, onOpenWordBook = { showWordBook = true }, refreshKey = myRefreshKey)
                    else -> Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { Text(items[selected].label, style = MaterialTheme.typography.headlineMedium); Spacer(Modifier.height(12.dp)); Text("功能将在后续阶段逐步加入") }
                }
            }
        }
    }
}




