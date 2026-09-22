package com.example.cet6vocabulary.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.cet6vocabulary.data.local.entity.WordBookEntity
import com.example.cet6vocabulary.data.model.Word
import com.example.cet6vocabulary.data.repository.WordBookRepository
import com.example.cet6vocabulary.data.repository.WordRepository

@Composable
fun WordBookScreen(words: WordRepository, wordBook: WordBookRepository, onStartStudy: () -> Unit, onBack: () -> Unit) {
    var records by remember { mutableStateOf(emptyList<WordBookEntity>()) }
    var reloadKey by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    BackHandler(onBack = onBack)
    LaunchedEffect(reloadKey) { records = wordBook.getAllWordBookRecords() }
    val items = records.mapNotNull { record -> words.getWordById(record.wordId)?.let { record to it } }
    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回") }
            Text("我的单词本", style = MaterialTheme.typography.headlineMedium)
        }
        Text("${items.size} 个单词", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(12.dp))
        Button(onClick = onStartStudy, enabled = items.isNotEmpty(), modifier = Modifier.fillMaxWidth()) { Text("开始背诵") }
        Spacer(Modifier.height(12.dp))
        if (items.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("单词本还是空的"); Spacer(Modifier.height(8.dp)); Text("在背诵时遇到不会的单词，可以加入这里。") } }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(items, key = { it.first.wordId }) { (_, word) ->
                    WordBookRow(word) { scope.launch { wordBook.removeWord(word.id); reloadKey++ } }
                }
            }
        }
    }
}

@Composable
private fun WordBookRow(word: Word, onRemove: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("NO.${word.id}", style = MaterialTheme.typography.labelLarge)
                Text(word.word, style = MaterialTheme.typography.titleLarge)
                if (word.phonetic.isNotBlank()) Text(word.phonetic)
                if (word.partOfSpeech.isNotBlank()) Text(word.partOfSpeech)
                Text(word.meaning)
            }
            TextButton(onClick = onRemove) { Text("移除") }
        }
    }
}







