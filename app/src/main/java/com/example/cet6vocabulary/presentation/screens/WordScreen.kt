package com.example.cet6vocabulary.presentation.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.cet6vocabulary.data.model.Word
import com.example.cet6vocabulary.data.repository.WordRepository
import com.example.cet6vocabulary.ui.components.CET6EmptyState
import com.example.cet6vocabulary.ui.components.CET6ErrorState
import com.example.cet6vocabulary.ui.components.CET6LoadingState
import com.example.cet6vocabulary.ui.components.LucideHomeIcons
import com.example.cet6vocabulary.ui.components.LucideWordIcons
import com.example.cet6vocabulary.ui.components.MaoDanCard
import com.example.cet6vocabulary.ui.components.MaoDanOutlinedCard
import com.example.cet6vocabulary.ui.theme.CET6VocabularyTheme
import com.example.cet6vocabulary.ui.theme.MaoDanDimens
import com.example.cet6vocabulary.ui.theme.MaoDanShapes
import com.example.cet6vocabulary.ui.theme.MaoDanTypography

private sealed interface WordUiState {
    data object Loading : WordUiState
    data class Success(val words: List<Word>) : WordUiState
    data class Error(val message: String) : WordUiState
}

@Composable
fun WordScreen(repository: WordRepository) {
    var state by remember { mutableStateOf<WordUiState>(WordUiState.Loading) }
    var query by remember { mutableStateOf(TextFieldValue()) }
    var selectedWord by remember { mutableStateOf<Word?>(null) }

    fun load() {
        state = WordUiState.Loading
        state = try {
            WordUiState.Success(repository.getAllWords())
        } catch (error: Exception) {
            WordUiState.Error(error.message ?: "unknown")
        }
    }

    LaunchedEffect(repository) { load() }

    // Only consume the system back event while a word detail is open; with
    // enabled = false the default Activity/Navigation back behavior is kept.
    BackHandler(enabled = selectedWord != null) {
        selectedWord = null
    }

    // Positions come from the loaded list, not from the id: ids have gaps
    // (1251-1255 are absent), so an id is never the reader-facing index.
    val loadedWords = (state as? WordUiState.Success)?.words.orEmpty()

    selectedWord?.let { word ->
        WordDetailContent(
            word = word,
            totalWords = loadedWords.size,
            position = loadedWords.indexOfFirst { it.id == word.id } + 1,
            onBack = { selectedWord = null }
        )
        return
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = Color.Transparent, // the root scaffold owns the background and the ambient light
        contentColor = MaterialTheme.colorScheme.onBackground
    ) { padding ->
        when (val current = state) {
            WordUiState.Loading -> CET6LoadingState(
                Modifier.padding(padding),
                "\u6b63\u5728\u52a0\u8f7d\u8bcd\u6c47"
            )
            is WordUiState.Error -> CET6ErrorState(
                message = "\u8bcd\u6c47\u52a0\u8f7d\u5931\u8d25",
                modifier = Modifier.padding(padding),
                actionText = "\u91cd\u65b0\u52a0\u8f7d",
                onActionClick = ::load
            )
            is WordUiState.Success -> WordListContent(
                words = current.words,
                query = query,
                onQueryChange = { query = it },
                onWordClick = { selectedWord = it },
                repository = repository,
                modifier = Modifier.padding(padding)
            )
        }
    }
}

@Composable
private fun WordListContent(
    words: List<Word>,
    query: TextFieldValue,
    onQueryChange: (TextFieldValue) -> Unit,
    onWordClick: (Word) -> Unit,
    repository: WordRepository,
    modifier: Modifier = Modifier
) {
    val filtered = if (query.text.isBlank()) words else repository.searchWords(query.text)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = MaoDanDimens.pageHorizontal)
    ) {
        Spacer(Modifier.height(MaoDanDimens.space16))

        // Page header
        Text(
            "CET6 \u8bcd\u6c47",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(MaoDanDimens.space4))
        Text(
            "\u641c\u7d22\u5e76\u67e5\u770b\u8bcd\u6c47\u8be6\u60c5",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(MaoDanDimens.space16))

        // Search bar
        WordSearchBar(
            value = query.text,
            onValueChange = { onQueryChange(TextFieldValue(it)) },
            placeholder = "\u641c\u7d22\u5355\u8bcd\u6216\u4e2d\u6587\u91ca\u4e49"
        )

        Spacer(Modifier.height(MaoDanDimens.space12))

        // Count label
        Text(
            text = if (query.text.isBlank())
                "\u5168\u90e8 ${words.size} \u4e2a\u8bcd"
            else
                "\u627e\u5230 ${filtered.size} \u4e2a\u8bcd",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(MaoDanDimens.space12))

        if (filtered.isEmpty()) {
            CET6EmptyState(
                title = "\u6ca1\u6709\u627e\u5230\u76f8\u5173\u8bcd\u6c47",
                description = "\u8bd5\u8bd5\u82f1\u6587\u5355\u8bcd\u6216\u4e2d\u6587\u91ca\u4e49\u3002",
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(MaoDanDimens.space8)
            ) {
                items(filtered, key = { it.id }) { word ->
                    WordListCard(
                        word = word,
                        onClick = { onWordClick(word) }
                    )
                }
                item { Spacer(Modifier.height(MaoDanDimens.space24)) }
            }
        }
    }
}

@Composable
private fun WordSearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        shape = MaoDanShapes.medium,
        placeholder = {
            Text(placeholder, style = MaterialTheme.typography.bodyMedium)
        },
        leadingIcon = {
            Icon(
                imageVector = LucideWordIcons.Search,
                contentDescription = "\u641c\u7d22",
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingIcon = {
            if (value.isNotEmpty()) {
                IconButton(onClick = { onValueChange("") }) {
                    Text(
                        "\u00d7",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Search
        )
    )
}

@Composable
private fun WordListCard(
    word: Word,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    MaoDanOutlinedCard(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaoDanShapes.large)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaoDanDimens.space16),
            verticalAlignment = Alignment.CenterVertically
        ) {
            WordIdBadge(word.id)
            Spacer(Modifier.width(MaoDanDimens.space12))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    word.word,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (word.phonetic.isNotBlank()) {
                    Text(
                        word.phonetic,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    word.meaning,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
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
private fun WordIdBadge(id: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(MaoDanShapes.medium)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = id.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun WordDetailContent(
    word: Word,
    totalWords: Int,
    position: Int,
    onBack: () -> Unit,
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
                .padding(horizontal = MaoDanDimens.pageHorizontal)
        ) {
            Spacer(Modifier.height(MaoDanDimens.space8))

            // Custom header
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = LucideWordIcons.ArrowLeft,
                        contentDescription = "\u8fd4\u56de",
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(Modifier.width(MaoDanDimens.space4))
                Text(
                    "\u5355\u8bcd\u8be6\u60c5",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "NO.${word.id}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.height(MaoDanDimens.space20))

            // Word card
            MaoDanCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(MaoDanDimens.space24),
                    verticalArrangement = Arrangement.spacedBy(MaoDanDimens.space8)
                ) {
                    Text(
                        word.word,
                        style = MaoDanTypography.wordDisplay,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (word.phonetic.isNotBlank()) {
                        Text(
                            word.phonetic,
                            style = MaoDanTypography.phonetic,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (word.partOfSpeech.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .clip(MaoDanShapes.pill)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .padding(
                                    horizontal = MaoDanDimens.space12,
                                    vertical = MaoDanDimens.space4
                                )
                        ) {
                            Text(
                                word.partOfSpeech,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(MaoDanDimens.space16))

            // Meaning card
            MaoDanOutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(MaoDanDimens.space20),
                    verticalArrangement = Arrangement.spacedBy(MaoDanDimens.space12)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        WordIconBadge(LucideHomeIcons.Bookmark)
                        Spacer(Modifier.width(MaoDanDimens.space8))
                        Text(
                            "\u4e2d\u6587\u91ca\u4e49",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        word.meaning,
                        style = MaoDanTypography.meaning,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            if (position in 1..totalWords) {
                Spacer(Modifier.height(MaoDanDimens.space16))
                Text(
                    "\u8bcd\u5e93\u8fdb\u5ea6 $position / $totalWords",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun WordIconBadge(icon: ImageVector, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(36.dp)
            .clip(MaoDanShapes.medium)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 760)
@Composable
private fun WordListPreview() {
    CET6VocabularyTheme {
        WordListContent(
            words = listOf(
                Word(1, "forehead", "/\u02c8f\u02d0hed/", "n.", "\u524d\u989d"),
                Word(2, "finger", "/\u02c8f\u026a\u014b\u0261\u0259(r)/", "n.; v.", "\u624b\u6307\uff1b\u544a\u53d1"),
                Word(3, "nerve", "/n\u025c\u02d0v/", "n.; v.", "\u795e\u7ecf\uff1b\u52c7\u6c14")
            ),
            query = TextFieldValue(),
            onQueryChange = {},
            onWordClick = {},
            repository = throw UnsupportedOperationException("Preview only"),
            modifier = Modifier
        )
    }
}
