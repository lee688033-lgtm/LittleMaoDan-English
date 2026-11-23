package com.example.cet6vocabulary

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.example.cet6vocabulary.data.ai.AiAssistant
import com.example.cet6vocabulary.data.ai.AiSettings
import com.example.cet6vocabulary.data.local.database.AppDatabase
import com.example.cet6vocabulary.data.repository.LearningDataRepository
import com.example.cet6vocabulary.data.repository.LearningRecordRepository
import com.example.cet6vocabulary.data.repository.ExamRepository
import com.example.cet6vocabulary.data.repository.StudyProgressRepository
import com.example.cet6vocabulary.data.repository.WordBookRepository
import com.example.cet6vocabulary.data.repository.WordRepository
import com.example.cet6vocabulary.data.settings.AppSettings
import com.example.cet6vocabulary.data.settings.ThemeMode
import com.example.cet6vocabulary.presentation.screens.*
import com.example.cet6vocabulary.ui.theme.CET6VocabularyTheme
import com.example.cet6vocabulary.ui.components.AnimatedPageContent
import com.example.cet6vocabulary.ui.components.BrandAvatarIcon
import com.example.cet6vocabulary.ui.components.BrandBottomBar
import com.example.cet6vocabulary.ui.components.BrandDictionaryIcon
import com.example.cet6vocabulary.ui.components.BrandEggIcon
import com.example.cet6vocabulary.ui.components.BrandMemoryIcon
import com.example.cet6vocabulary.ui.components.BrandSpellingIcon
import com.example.cet6vocabulary.ui.components.CET6NavigationItem
import com.example.cet6vocabulary.ui.components.MaoDanAuroraBackground
import com.example.cet6vocabulary.ui.components.MaoDanParticleBackground

data class NavItem(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private enum class ExamRoute {
    LIST,
    DETAIL,
    PLACEHOLDER,
    READING,
    READING_SHEET,
    READING_RESULT
}

// The answer map is mutated in place by the reading screens, so it is stored as a flat
// key/value list that the saved-instance-state bundle can carry across recreation.
private val ReadingAnswersSaver = listSaver<SnapshotStateMap<String, String>, String>(
    save = { answers -> answers.flatMap { (questionId, answer) -> listOf(questionId, answer) } },
    restore = { saved ->
        mutableStateMapOf<String, String>().apply {
            saved.chunked(2).forEach { pair ->
                if (pair.size == 2) put(pair[0], pair[1])
            }
        }
    }
)

// Question ids the learner has explicitly submitted. The answers map alone cannot tell a
// submitted answer from one that was only selected before leaving through the answer sheet.
private val SubmittedQuestionIdsSaver = listSaver<SnapshotStateList<String>, String>(
    save = { submitted -> submitted.toList() },
    restore = { saved -> mutableStateListOf<String>().apply { addAll(saved) } }
)

private val ExamRouteSaver = Saver<ExamRoute?, String>(
    save = { route -> route?.name.orEmpty() },
    restore = { name -> if (name.isEmpty()) null else ExamRoute.valueOf(name) }
)

private val NullableStringSaver = Saver<String?, String>(
    save = { value -> value.orEmpty() },
    restore = { value -> value.ifEmpty { null } }
)

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
    var selected by rememberSaveable { mutableIntStateOf(0) }
    var showWordBook by rememberSaveable { mutableStateOf(false) }
    var showReview by rememberSaveable { mutableStateOf(false) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var showAi by rememberSaveable { mutableStateOf(false) }
    var aiInitialQuery by rememberSaveable { mutableStateOf("") }
    var studyWordBook by rememberSaveable { mutableStateOf(false) }
    var examRoute by rememberSaveable(stateSaver = ExamRouteSaver) { mutableStateOf<ExamRoute?>(null) }
    var selectedExamId by rememberSaveable(stateSaver = NullableStringSaver) { mutableStateOf<String?>(null) }
    val readingAnswers = rememberSaveable(saver = ReadingAnswersSaver) { mutableStateMapOf<String, String>() }
    val readingSubmitted = rememberSaveable(saver = SubmittedQuestionIdsSaver) { mutableStateListOf<String>() }
    // Reading session state lives here so the practice screen and the answer sheet share one
    // in-memory copy; switching between the two routes cannot recreate it.
    var readingQuestionIndex by rememberSaveable { mutableIntStateOf(0) }
    var placeholderTitle by rememberSaveable { mutableStateOf("真题练习") }
    var myRefreshKey by remember { mutableIntStateOf(0) }
    var wordBookOrigin by rememberSaveable { mutableIntStateOf(4) }
    val wordRepository = remember { WordRepository(context) }
    val database = remember { AppDatabase.getInstance(context) }
    val learningRecords = remember { LearningRecordRepository(database.learningRecordDao()) }
    val studyProgress = remember { StudyProgressRepository(database.studyProgressDao()) }
    val wordBook = remember { WordBookRepository(database.wordBookDao()) }
    val examRepository = remember { ExamRepository(context) }
    val learningData = remember {
        LearningDataRepository(
            records = database.learningRecordDao(),
            wordBook = database.wordBookDao(),
            progress = database.studyProgressDao()
        )
    }
    val appSettings = remember { AppSettings(context) }
    val aiSettings = remember { AiSettings(context) }
    val aiAssistant = remember { AiAssistant(aiSettings) }
    var themeModeName by rememberSaveable { mutableStateOf(appSettings.themeMode.name) }
    val themeMode = ThemeMode.entries.firstOrNull { it.name == themeModeName } ?: ThemeMode.SYSTEM
    BackHandler(enabled = showWordBook) {
        showWordBook = false
        selected = wordBookOrigin
        studyWordBook = false
        myRefreshKey++
    }
    BackHandler(enabled = showSettings) {
        showSettings = false
        myRefreshKey++
    }
    BackHandler(enabled = showReview) {
        showReview = false
        myRefreshKey++
    }
    BackHandler(enabled = showAi) {
        showAi = false
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
            ExamRoute.READING_SHEET -> examRoute = ExamRoute.READING
            ExamRoute.READING_RESULT -> examRoute = ExamRoute.DETAIL
            ExamRoute.DETAIL -> examRoute = ExamRoute.LIST
            ExamRoute.LIST -> {
                examRoute = null
                selected = 0
            }
            null -> Unit
        }
    }

    // Identifies the visible destination so the page-enter animation replays on navigation only,
    // never on in-page reloads. Mirrors the precedence of the destination switch below.
    val pageKey = when {
        showAi -> "ai"
        showSettings -> "settings"
        showReview -> "review"
        showWordBook -> "wordBook"
        examRoute != null -> "exam:${examRoute!!.name}:$selectedExamId"
        else -> "tab:$selected"
    }

    CET6VocabularyTheme(
        darkTheme = when (themeMode) {
            ThemeMode.SYSTEM -> isSystemInDarkTheme()
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
        }
    ) {
        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
            if (!showWordBook && !showReview && !showSettings && !showAi && examRoute == null) {
                BrandBottomBar(
                    items = items.map { CET6NavigationItem(it.label, it.icon) },
                    selectedIndex = selected,
                    onItemSelected = { selected = it; studyWordBook = false; myRefreshKey++ }
                )
            }
            }
        ) { padding ->
            // Ambient light for the whole app, with a layer of dust over it. Scaffold measures
            // every root of this lambda against the full window and places them all at (0, 0) in
            // emission order, so these paint behind the content below; the bottom bar is a later
            // slot, so its own opaque surface still covers both.
            MaoDanAuroraBackground(Modifier.fillMaxSize())
            MaoDanParticleBackground(Modifier.fillMaxSize())
            Box(Modifier.fillMaxSize().padding(if (showWordBook) PaddingValues() else padding)) {
                AnimatedPageContent(pageKey = pageKey) {
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
                                            readingSubmitted.clear()
                                            readingQuestionIndex = 0
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
                            else ReadingPracticeScreen(
                                exam = exam,
                                answers = readingAnswers,
                                submitted = readingSubmitted.toSet(),
                                questionIndex = readingQuestionIndex,
                                onQuestionIndexChange = { readingQuestionIndex = it },
                                onSubmit = { questionId ->
                                    if (questionId !in readingSubmitted) readingSubmitted.add(questionId)
                                },
                                onRetry = { questionId ->
                                    readingSubmitted.remove(questionId)
                                    readingAnswers.remove(questionId)
                                },
                                onOpenAnswerSheet = { examRoute = ExamRoute.READING_SHEET },
                                onComplete = { examRoute = ExamRoute.READING_RESULT },
                                onRestart = {
                                    readingAnswers.clear()
                                    readingSubmitted.clear()
                                    readingQuestionIndex = 0
                                    examRoute = ExamRoute.READING
                                },
                                onBack = { examRoute = ExamRoute.DETAIL }
                            )
                        }
                        !showWordBook && examRoute == ExamRoute.READING_SHEET -> {
                            val exam = selectedExamId?.let(examRepository::getExam)
                            if (exam == null) ExamPlaceholderScreen(title = placeholderTitle, onBack = { examRoute = ExamRoute.DETAIL })
                            else ReadingAnswerSheetScreen(
                                exam = exam,
                                answers = readingAnswers,
                                currentIndex = readingQuestionIndex,
                                onSelectQuestion = { index -> readingQuestionIndex = index; examRoute = ExamRoute.READING },
                                onFinish = { examRoute = ExamRoute.READING_RESULT },
                                onBack = { examRoute = ExamRoute.READING }
                            )
                        }
                        !showWordBook && examRoute == ExamRoute.READING_RESULT -> {
                            val exam = selectedExamId?.let(examRepository::getExam)
                            if (exam == null) ExamPlaceholderScreen(title = placeholderTitle, onBack = { examRoute = ExamRoute.DETAIL })
                            else ReadingResultScreen(
                                exam = exam,
                                answers = readingAnswers,
                                onRestart = {
                                    readingAnswers.clear()
                                    readingSubmitted.clear()
                                    readingQuestionIndex = 0
                                    examRoute = ExamRoute.READING
                                },
                                onBack = { examRoute = ExamRoute.DETAIL }
                            )
                        }
                        !showWordBook && examRoute == ExamRoute.PLACEHOLDER -> ExamPlaceholderScreen(
                            title = placeholderTitle,
                            onBack = { examRoute = ExamRoute.DETAIL }
                        )
                        showAi -> AiScreen(
                            assistant = aiAssistant,
                            words = wordRepository,
                            records = learningRecords,
                            initialQuery = aiInitialQuery,
                            onOpenAiSettings = { showAi = false; showSettings = true },
                            onBack = { showAi = false; myRefreshKey++ }
                        )
                        showSettings -> SettingsScreen(
                            themeMode = themeMode,
                            onThemeModeChange = { mode ->
                                themeModeName = mode.name
                                appSettings.themeMode = mode
                            },
                            learningData = learningData,
                            aiSettings = aiSettings,
                            assistant = aiAssistant,
                            onBack = { showSettings = false; myRefreshKey++ }
                        )
                        showReview -> ReviewScreen(
                            words = wordRepository,
                            records = learningRecords,
                            onBack = { showReview = false; myRefreshKey++ }
                        )
                        showWordBook -> WordBookScreen(wordRepository, wordBook, onStartStudy = { showWordBook = false; selected = 2; studyWordBook = true }, onBack = { showWordBook = false; selected = wordBookOrigin; studyWordBook = false; myRefreshKey++ })
                        selected == 0 -> HomeScreen(wordRepository, learningRecords, wordBook, refreshKey = myRefreshKey, onOpenStudy = { selected = 2; studyWordBook = false }, onOpenSpelling = { selected = 3; studyWordBook = false }, onOpenWordBook = { wordBookOrigin = 0; showWordBook = true }, onOpenAi = { aiInitialQuery = ""; showAi = true })
                        selected == 1 -> WordScreen(
                            wordRepository,
                            wordBook,
                            learningRecords,
                            onOpenAi = { keyword -> aiInitialQuery = keyword; showAi = true }
                        )
                        selected == 2 -> StudyScreen(wordRepository, wordBook, studyProgress, studyWordBook)
                        selected == 3 -> SpellingScreen(wordRepository, learningRecords, studyProgress)
                        selected == 4 -> MyScreen(wordRepository, learningRecords, wordBook, onOpenWordBook = { wordBookOrigin = 4; showWordBook = true }, onOpenReview = { showReview = true }, onOpenAi = { aiInitialQuery = ""; showAi = true }, onOpenSettings = { showSettings = true }, refreshKey = myRefreshKey)
                        else -> Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { Text(items[selected].label, style = MaterialTheme.typography.headlineMedium); Spacer(Modifier.height(12.dp)); Text("功能将在后续阶段逐步加入") }
                    }
                }
            }
        }
    }
}




