@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.cet6vocabulary.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.example.cet6vocabulary.data.model.Word
import com.example.cet6vocabulary.data.repository.WordRepository
import com.example.cet6vocabulary.ui.components.CET6EmptyState
import com.example.cet6vocabulary.ui.components.CET6ErrorState
import com.example.cet6vocabulary.ui.components.CET6LoadingState
import com.example.cet6vocabulary.ui.components.CET6SearchBar
import com.example.cet6vocabulary.ui.components.CET6WordItem

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

    selectedWord?.let { word ->
        WordDetailScreen(word, onBack = { selectedWord = null })
        return
    }

    Scaffold(contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0), topBar = { TopAppBar(title = { Text("CET6 词汇") }) }) { padding ->
        when (val current = state) {
            WordUiState.Loading -> CET6LoadingState(Modifier.padding(padding), "正在加载词汇")
            is WordUiState.Error -> CET6ErrorState(
                message = "词汇加载失败",
                modifier = Modifier.padding(padding),
                actionText = "重新加载",
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

    Column(modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        CET6SearchBar(
            value = query.text,
            onValueChange = { onQueryChange(TextFieldValue(it)) },
            placeholder = "搜索单词或中文释义",
            modifier = Modifier.padding(top = 12.dp)
        )
        Text(
            text = if (query.text.isBlank()) "全部 ${words.size} 个词" else "找到 ${filtered.size} 个词",
            modifier = Modifier.padding(vertical = 16.dp),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (filtered.isEmpty()) {
            CET6EmptyState(
                title = "没有找到相关词汇",
                description = "试试英文单词或中文释义。",
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filtered, key = { it.id }) { word ->
                    CET6WordItem(
                        word = word.word,
                        phonetic = word.phonetic,
                        partOfSpeech = word.partOfSpeech,
                        meaning = word.meaning,
                        learningState = "",
                        onClick = { onWordClick(word) }
                    )
                }
            }
        }
    }
}

@Composable
private fun WordDetailScreen(word: Word, onBack: () -> Unit) {
    Scaffold(
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("单词详情") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("NO.${word.id}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Text(word.word, style = MaterialTheme.typography.displayLarge)
                    if (word.phonetic.isNotBlank()) Text(word.phonetic, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (word.partOfSpeech.isNotBlank()) Text(word.partOfSpeech, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Text("中文释义", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.padding(top = 4.dp))
                    Text(word.meaning, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}
