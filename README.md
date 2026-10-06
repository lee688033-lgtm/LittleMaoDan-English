# 小毛蛋背英语

一款使用 Kotlin 与 Jetpack Compose 编写的 CET-6 英语学习 Android 应用。词库、练习与学习记录全部在本地完成，不依赖任何后端服务。

## 项目简介

小毛蛋背英语是一个个人自用项目，围绕 CET-6 备考场景把「记单词」和「练真题」放进同一个应用：词库由本地 JSON 动态加载，背诵、拼写和单词本围绕同一套词库运转，学习记录与掌握度写入 Room 本地数据库；真题模块复用同一批题目数据结构，当前已实现阅读专项的作答与答题卡流程。

应用没有账号体系、没有网络请求、没有统计上报，所有数据留在设备上。

## 核心功能

### 已实现

| 模块 | 内容 |
| --- | --- |
| 首页 | 学习进度（已学 / 总数）、背诵与拼写入口、词汇本与真题训练入口、学习统计摘要 |
| 单词 | 词库列表、按英文单词或中文释义搜索、单词详情（音标、词性、释义、该词在词库中的位置） |
| 背诵 | 顺序背诵与随机背诵、显示或隐藏释义、上一个 / 下一个、背诵进度持久化、随机顺序持久化、把单词加入我的单词本 |
| 拼写 | 根据中文释义与音标拼写单词、答案判定、上一题 / 下一题、顺序与随机模式、结果写入学习记录 |
| 我的 | 学习概览、学习统计（已学习 / 待学习 / 正确率 / 错误次数）、掌握情况分级、单词本入口 |
| 我的单词本 | 收藏的单词列表、移出单词本、从单词本直接进入背诵 |
| CET-6 真题 | 历年真题列表、真题详情（写作 / 听力 / 阅读 / 翻译四个部分）、阅读专项练习、阅读答题卡、阅读结果 |
| 底部导航 | 自定义五项底部导航，带选中状态动画 |

### 当前未实现（界面已占位）

真题的写作、听力、翻译以及整套练习仍是占位页，进入后显示「该功能将在下一阶段实现」。阅读专项只提供作答与答题卡流程，不判分，没有正确率与错题统计。

## 产品亮点

- 1495 个 CET-6 词汇通过 `app/src/main/assets/cet6/` 下的 10 个 JSON 动态加载，运行时各页面的分母都来自实际加载结果；固定词库数字只出现在 IDE Preview 示例数据与 instrumentation 测试断言里。
- 英文单词与中文释义双向搜索。
- 顺序与随机两种背诵模式；随机顺序本身也会被持久化，重新进入后序列不变。
- 拼写练习把答对与答错写回学习记录，与掌握度统计共用同一份数据。
- Room 保存学习记录、背诵进度和单词本，数据库带版本号与迁移（当前 `version = 3`）。
- 背诵与拼写中不会的单词可以加入我的单词本，单词本自身是独立复习入口。
- 真题的题目分组与题号范围来自 JSON 结构，答题卡不硬编码题数。
- 底部导航采用 Liquid Glass 视觉：选中胶囊由连续索引动画驱动（spring，约 320ms，带轻微横向拉伸）。这段动画只作用于底部导航的选中状态。
- Aurora 渐变背景与低密度粒子层作为全局背景，深色模式自动降低强度。
- 全局 Reduce Motion：读取系统 `ANIMATOR_DURATION_SCALE` 与 `TRANSITION_ANIMATION_SCALE`，系统关闭动画时页面直接进入终态并停止持续重绘。
- 统一 Design Token：间距与尺寸（`Dimens.kt`）、动效时长与曲线（`Motion.kt`）、圆角（`Shape.kt`）、颜色与语义色（`Color.kt`），组件统一从 `MaterialTheme` 读取。
- 底部导航与五个主页面使用同一套 Lucide 图标，由官方 SVG 逐命令转换为 `ImageVector`，没有引入第三方图标依赖。
- 深浅色主题跟随系统，`values-night/styles.xml` 只覆写窗口背景，避免深色冷启动时先闪一帧白屏。

卡片倾斜、磁吸跟随、聚光灯和流光文字等动效组件已实现但尚未接入任何页面，因此不计入当前功能。

## 技术栈

- Kotlin 2.0.21
- Jetpack Compose（Compose BOM 2024.12.01，`androidx.activity:activity-compose` 1.10.0）
- Material 3（`androidx.compose.material3:material3`）
- Room 2.6.1（`room-runtime` / `room-ktx`，KAPT 生成 DAO 实现）
- Coroutines（DAO 与 Repository 使用挂起函数）
- Gradle 8.10.2（仓库自带 Wrapper）、Android Gradle Plugin 8.5.2
- `compileSdk` 35、`targetSdk` 35、`minSdk` 24
- JDK 17
- `androidx.core:core-ktx` 1.15.0、`androidx.compose.material:material-icons-extended`

页面路由由 `MainActivity` 内的状态与枚举自行管理，没有使用 Jetpack Navigation 组件。

## 项目架构

```text
app/src/main/java/com/example/cet6vocabulary/
├── MainActivity.kt                       应用入口、底部导航与页面路由状态
├── data/model/                           Word、LearningRecord、Exam 等数据模型
├── data/local/                           Room 数据库、DAO、Entity 与迁移
├── data/repository/                      WordRepository、LearningRecordRepository、
│                                        StudyProgressRepository、WordBookRepository、
│                                        ExamRepository、ExamJsonParser
├── presentation/model/                   阅读答题卡纯模型（不依赖 Compose）
├── presentation/screens/                 各页面 Compose UI
└── ui/                                   自定义组件、图标、Design Token 与主题

app/src/main/assets/cet6/                 10 个 CET-6 词汇 JSON（运行时资源）
app/src/androidTest/                       instrumentation 测试
app/src/main/res/                          应用图标、主题与深色样式
```

数据流向：词汇 JSON 由 `WordRepository` 一次性加载并按 ID 排序；页面 UI 状态保存在 `MainActivity` 与各页面的可组合函数中；学习记录、背诵进度和单词本经 Repository 写入 Room。项目按 UI、数据模型、本地数据层和 Repository 分层组织，没有实现 ViewModel、依赖注入或 Clean Architecture，本文不做这类描述。

## 数据说明

- 词库共 1495 个 CET-6 高频词汇，分布在 `app/src/main/assets/cet6/` 的 10 个 JSON 文件中。
- 词汇 ID 范围是 1-1500，其中 1251-1255 是词库自身的断档，不是数据丢失。因此单词详情页显示的「词库进度」取该词在已加载列表中的位置，而不是它的 ID。
- 每条词汇字段为 `id` / `word` / `phonetic` / `partOfSpeech` / `meaning`，加载时逐条校验，字段缺失即报错。
- 学习记录、背诵进度与单词本存储在本地 Room 数据库（`learning_records`、`study_progress`、`word_book`），不会上传。
- 根目录 `词汇json文件/` 是词库的原始来源副本，不属于运行时资源，已被 Git 忽略。

## 真题资料说明

真题原始资料受版权与再分发限制，不在本项目的公开范围内：

- 公开仓库不包含 `真题json文件/` 目录，也没有任何真题 JSON、试卷 PDF 或听力音频。
- 公开 Release 提供的 APK 同样不包含真题原始资料：`app/build.gradle.kts` 默认不把该目录加入资源，只有本地显式开启时才会打进安装包。
- 真题模块的源码（`ExamRepository`、`ExamJsonParser`、真题页面与答题卡）完整保留，数据结构与解析逻辑可以直接参考。
- 因此从本仓库克隆后直接构建，真题列表会显示「真题数据加载失败」的错误态并提供重新加载按钮（因为安装包里没有题库数据），点击返回即可正常使用其它功能；词汇、背诵、拼写、单词本不受影响。

本地需要继续用真题资料调试时，把资料放在根目录 `真题json文件/`（该目录已被 Git 忽略），再执行：

```powershell
.\gradlew.bat assembleDebug "-Pcet6.localExamAssets=true"
```

没有带这个开关的每一次构建，产物里都不会包含真题数据。

## 开发环境

- Android Studio，或等价的 Android SDK 环境
- JDK 17
- Android SDK Platform 35
- Gradle 8.10.2（仓库自带 Wrapper，无需本机安装 Gradle）
- Android Gradle Plugin 8.5.2

仓库不包含 `local.properties`，SDK 位置由 `ANDROID_HOME` 或 Android Studio 提供。

## 构建项目

Windows PowerShell，在项目根目录执行：

```powershell
.\gradlew.bat clean
.\gradlew.bat assembleDebug --no-daemon --max-workers=1 --console=plain
```

APK 输出位置：

```text
app/build/outputs/apk/debug/app-debug.apk
```

安装到已连接的设备：

```powershell
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

instrumentation 测试：

```powershell
.\gradlew.bat assembleDebugAndroidTest --no-daemon --max-workers=1 --console=plain
.\gradlew.bat connectedDebugAndroidTest --no-daemon --max-workers=1 --console=plain
```

中文路径在部分环境下会触发 Android Gradle Plugin 的路径检查，`gradle.properties` 已包含 `android.overridePathCheck=true`；项目放在英文路径时该开关不影响构建。

## 截图

> 项目截图将在后续版本补充。

## 当前版本

当前版本 `v1.1.0`（`versionCode = 2`，`versionName = "1.1.0"`）。GitHub Releases 提供的是用于项目展示与测试的 Debug APK，项目尚未配置正式签名，因此没有签名版 Release 安装包。

## Roadmap

1. 阅读专项的判分、错题与正确率统计。
2. 听力、写作、翻译专项，或收掉这几个占位页。
3. 词库继续扩充，并补齐 1251-1255 的空档。
4. 本地学习数据导出与恢复。
5. 按需接入已完成但尚未使用的动效组件，或把它们从代码中移除。
6. 补充应用截图与版本发布说明。

## License

License 待确定。
