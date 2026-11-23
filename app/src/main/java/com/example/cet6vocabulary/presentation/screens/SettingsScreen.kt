package com.example.cet6vocabulary.presentation.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.cet6vocabulary.data.ai.AiAssistant
import com.example.cet6vocabulary.data.ai.AiProvider
import com.example.cet6vocabulary.data.ai.AiSettings
import com.example.cet6vocabulary.data.repository.LearningDataJson
import com.example.cet6vocabulary.data.repository.LearningDataRepository
import com.example.cet6vocabulary.data.repository.LearningDataSnapshot
import com.example.cet6vocabulary.data.settings.ThemeMode
import com.example.cet6vocabulary.ui.components.CET6SectionTitle
import com.example.cet6vocabulary.ui.components.MaoDanCard
import com.example.cet6vocabulary.ui.components.MaoDanPrimaryButton
import com.example.cet6vocabulary.ui.components.MaoDanSecondaryButton
import com.example.cet6vocabulary.ui.components.MaoDanTextButton
import com.example.cet6vocabulary.ui.theme.MaoDanDimens
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    learningData: LearningDataRepository,
    aiSettings: AiSettings,
    assistant: AiAssistant,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }
    var statusIsError by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }
    var pendingImport by remember { mutableStateOf<LearningDataSnapshot?>(null) }

    fun report(message: String, error: Boolean = false) {
        status = message
        statusIsError = error
    }

    // SAF 让用户自己选择位置：应用不申请存储权限，也不把文件写到应用私有目录之外。
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                busy = true
                val result = runCatching {
                    val json = learningData.export(System.currentTimeMillis())
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(json.toByteArray(Charsets.UTF_8))
                    } ?: error("无法写入所选文件")
                }
                busy = false
                if (result.isSuccess) {
                    report("学习数据已导出")
                } else {
                    report("导出失败：${result.exceptionOrNull()?.message ?: "未知错误"}", error = true)
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                busy = true
                val result = runCatching {
                    val text = context.contentResolver.openInputStream(uri)
                        ?.bufferedReader(Charsets.UTF_8)
                        ?.use { it.readText() }
                        ?: error("无法读取所选文件")
                    LearningDataJson.decode(text)
                }
                busy = false
                result.onSuccess { pendingImport = it }
                    .onFailure { report("导入失败：${it.message ?: "未知错误"}", error = true) }
            }
        }
    }

    BackHandler(onBack = onBack)

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MaoDanDimens.pageHorizontal),
            verticalArrangement = Arrangement.spacedBy(MaoDanDimens.space16)
        ) {
            Spacer(Modifier.height(MaoDanDimens.space8))
            SettingsHeader(onBack = onBack)

            CET6SectionTitle("外观")
            MaoDanCard(Modifier.fillMaxWidth()) {
                Column(
                    Modifier.fillMaxWidth().padding(MaoDanCardContent),
                    verticalArrangement = Arrangement.spacedBy(MaoDanDimens.space12)
                ) {
                    Text("主题", style = MaterialTheme.typography.titleMedium)
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        ThemeMode.entries.forEachIndexed { index, mode ->
                            SegmentedButton(
                                selected = themeMode == mode,
                                onClick = { onThemeModeChange(mode) },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = ThemeMode.entries.size
                                ),
                                icon = { SegmentedButtonDefaults.Icon(active = themeMode == mode) }
                            ) {
                                Text(mode.label)
                            }
                        }
                    }
                    Text(
                        "「跟随系统」是默认值；浅色 / 深色只影响本应用，不会修改系统设置。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            CET6SectionTitle("AI 助手")
            AiSection(aiSettings = aiSettings, assistant = assistant)

            CET6SectionTitle("数据")
            MaoDanCard(Modifier.fillMaxWidth()) {
                Column(
                    Modifier.fillMaxWidth().padding(MaoDanCardContent),
                    verticalArrangement = Arrangement.spacedBy(MaoDanDimens.space12)
                ) {
                    Text(
                        "学习数据只保存在本机。导出会生成一份 JSON，包含学习记录、单词本与背诵 / 拼写进度，不包含词库本身。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    MaoDanPrimaryButton(
                        text = "导出学习数据",
                        onClick = {
                            exportLauncher.launch(exportFileName(System.currentTimeMillis()))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !busy
                    )
                    MaoDanSecondaryButton(
                        text = "导入学习数据",
                        onClick = {
                            importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream"))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !busy
                    )
                    MaoDanTextButton(
                        text = "清除学习数据",
                        onClick = { showClearDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !busy
                    ) {
                        Text(
                            "清除学习数据",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    status?.let { message ->
                        Text(
                            message,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (statusIsError) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            CET6SectionTitle("关于")
            MaoDanCard(Modifier.fillMaxWidth()) {
                Column(
                    Modifier.fillMaxWidth().padding(MaoDanCardContent),
                    verticalArrangement = Arrangement.spacedBy(MaoDanDimens.space8)
                ) {
                    AboutRow("版本", appVersionName(context))
                    AboutRow("数据", "学习数据全部保存在本机")
                    AboutRow("网络", "仅 AI 助手调用模型时联网")
                    AboutRow("词库", "CET-6 高频词汇，随应用离线提供")
                    Text(
                        "导出的 JSON 由你自行保存与分享；导入会覆盖本机现有的学习记录、单词本与进度。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(MaoDanDimens.space24))
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("清除学习数据") },
            text = {
                Text("将删除全部学习记录、单词本与背诵 / 拼写进度。词库与外观设置不受影响，此操作无法撤销。")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearDialog = false
                        scope.launch {
                            busy = true
                            val result = runCatching { learningData.clearAll() }
                            busy = false
                            if (result.isSuccess) {
                                report("学习数据已清除")
                            } else {
                                report("清除失败：${result.exceptionOrNull()?.message ?: "未知错误"}", error = true)
                            }
                        }
                    }
                ) {
                    Text("清除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("取消") }
            }
        )
    }

    pendingImport?.let { snapshot ->
        AlertDialog(
            onDismissRequest = { pendingImport = null },
            title = { Text("导入学习数据") },
            text = {
                Text(
                    "文件包含 ${snapshot.records.size} 条学习记录、" +
                        "${snapshot.wordBook.size} 个收藏、" +
                        "${snapshot.studyProgress.size} 条进度。\n\n" +
                        "导入会覆盖本机现有的同类数据。"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingImport = null
                        scope.launch {
                            busy = true
                            val result = runCatching { learningData.replaceAll(snapshot) }
                            busy = false
                            if (result.isSuccess) {
                                report("已导入 ${snapshot.records.size} 条学习记录、${snapshot.wordBook.size} 个收藏")
                            } else {
                                report("导入失败：${result.exceptionOrNull()?.message ?: "未知错误"}", error = true)
                            }
                        }
                    }
                ) {
                    Text("覆盖导入")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingImport = null }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun AiSection(
    aiSettings: AiSettings,
    assistant: AiAssistant,
    modifier: Modifier = Modifier
) {
    var provider by remember { mutableStateOf(aiSettings.provider) }
    var apiKey by remember { mutableStateOf(aiSettings.apiKey) }
    var baseUrl by remember { mutableStateOf(aiSettings.baseUrl) }
    var model by remember { mutableStateOf(aiSettings.model) }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }
    var statusIsError by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun persist() = aiSettings.save(provider, apiKey, baseUrl, model)

    MaoDanCard(modifier.fillMaxWidth()) {
        Column(
            Modifier.fillMaxWidth().padding(MaoDanCardContent),
            verticalArrangement = Arrangement.spacedBy(MaoDanDimens.space12)
        ) {
            Text("服务商", style = MaterialTheme.typography.titleMedium)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                AiProvider.entries.forEachIndexed { index, entry ->
                    SegmentedButton(
                        selected = provider == entry,
                        onClick = {
                            val previous = provider
                            provider = entry
                            // Untouched fields follow the preset; edited ones are left alone, and
                            // 自定义 starts empty so the learner can paste their own endpoint.
                            val blankForCustom = entry == AiProvider.CUSTOM
                            baseUrl = when {
                                blankForCustom && baseUrl == previous.defaultBaseUrl -> ""
                                !blankForCustom && (baseUrl.isBlank() || baseUrl == previous.defaultBaseUrl) -> entry.defaultBaseUrl
                                else -> baseUrl
                            }
                            model = when {
                                blankForCustom && model == previous.defaultModel -> ""
                                !blankForCustom && (model.isBlank() || model == previous.defaultModel) -> entry.defaultModel
                                else -> model
                            }
                            status = ""
                        },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = AiProvider.entries.size
                        ),
                        icon = { SegmentedButtonDefaults.Icon(active = provider == entry) }
                    ) {
                        Text(entry.shortLabel)
                    }
                }
            }
            Text(
                "当前：${provider.label}。查词与短文都用这一个模型，接口需兼容 OpenAI Chat Completions。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("API Key") },
                placeholder = { Text("粘贴服务商控制台里的密钥") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                visualTransformation = PasswordVisualTransformation()
            )
            OutlinedTextField(
                value = baseUrl,
                onValueChange = { baseUrl = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("接口地址") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
            )
            OutlinedTextField(
                value = model,
                onValueChange = { model = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("模型名") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(MaoDanDimens.space12)) {
                MaoDanPrimaryButton(
                    text = "保存",
                    onClick = {
                        persist()
                        status = "AI 配置已保存"
                        statusIsError = false
                    },
                    modifier = Modifier.weight(1f)
                )
                MaoDanSecondaryButton(
                    text = if (busy) "连接中…" else "测试连接",
                    onClick = {
                        persist()
                        busy = true
                        status = ""
                        scope.launch {
                            val result = runCatching { assistant.testConnection() }
                            busy = false
                            if (result.isSuccess) {
                                status = "连接成功，模型已响应"
                                statusIsError = false
                            } else {
                                status = result.exceptionOrNull()?.message ?: "连接失败"
                                statusIsError = true
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !busy && apiKey.isNotBlank()
                )
            }
            if (status.isNotBlank()) {
                Text(
                    status,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (statusIsError) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.primary
                )
            }
            Text(
                "智谱 GLM-4-Flash 与硅基流动的部分模型可以免费使用，额度与可用性以服务商为准。" +
                    "调用时只会发送你查询的单词或选中的单词，学习记录本身不会上传；" +
                    "API Key 保存在本机应用私有存储中，不会进入导出文件。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SettingsHeader(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("设置", style = MaterialTheme.typography.headlineMedium)
            Text(
                "外观、AI 助手、学习数据与关于",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AboutRow(label: String, value: String, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(MaoDanDimens.space12))
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private val MaoDanCardContent = MaoDanDimens.space20

private fun exportFileName(now: Long): String =
    "cet6-learning-" + SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date(now)) + ".json"

private fun appVersionName(context: android.content.Context): String =
    runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName
    }.getOrNull() ?: "未知"
