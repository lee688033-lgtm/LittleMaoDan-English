# 小毛蛋背英语

> 一款用 Kotlin 与 Jetpack Compose 编写的 CET-6 英语学习 Android 应用：本地词库、背诵、拼写、学习记录、单词本，加上 CET-6 真题的阅读答题卡。全部数据留在设备上。

A CET-6 vocabulary and exam-practice Android app built with Kotlin and Jetpack Compose. No account, no network requests, no analytics.

![Release](https://img.shields.io/badge/release-v1.1.0-2c6fdb?style=flat-square)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-7f52ff?style=flat-square)
![Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20%C2%B7%20Material%203-4285f4?style=flat-square)
![Room](https://img.shields.io/badge/storage-Room%202.6.1-3ddc84?style=flat-square)
![SDK](https://img.shields.io/badge/minSdk%2024%20%C2%B7%20targetSdk%2035-Android-green?style=flat-square)
![Permissions](https://img.shields.io/badge/Android%20permissions-none-lightgrey?style=flat-square)

**下载**：[Releases](https://github.com/lee688033-lgtm/LittleMaoDan-English/releases) ｜ [v1.1.0 Debug APK](https://github.com/lee688033-lgtm/LittleMaoDan-English/releases/download/v1.1.0/LittleMaoDan-English-v1.1.0-debug.apk)

## 目录

- [项目简介](#项目简介)
- [30 秒了解](#30-秒了解)
- [核心功能](#核心功能)
- [UI 与交互](#ui-与交互)
- [技术栈](#技术栈)
- [技术亮点](#技术亮点)
- [项目架构](#项目架构)
- [数据组织](#数据组织)
- [真题数据说明](#真题数据说明)
- [本地运行](#本地运行)
- [构建 APK](#构建-apk)
- [测试](#测试)
- [版本与更新](#版本与更新)
- [界面截图](#界面截图)
- [已知限制](#已知限制)
- [后续规划](#后续规划)
- [开发记录](#开发记录)
- [第三方素材与许可](#第三方素材与许可)
- [License](#license)

## 项目简介

小毛蛋背英语是一个个人 CET-6 备考项目，把「记单词」和「练真题」放进同一个离线应用里：

- 词汇数据是仓库内的 JSON，运行时由 `WordRepository` 一次性加载，背诵、拼写、单词本、学习统计都围绕这同一份词库运转。
- 学习行为（拼写对错、掌握度、背诵进度、随机序列、单词本收藏）写入本地 Room 数据库，进程重启后仍然一致。
- 真题模块保留了完整的题目数据结构、解析逻辑与阅读答题卡流程；真题原始数据受版权限制不分发，详见[真题数据说明](#真题数据说明)。

应用没有账号体系，没有网络请求，`AndroidManifest.xml` 未声明任何 `uses-permission`，所有状态都在本机。

## 30 秒了解

| 项目 | 说明 |
| --- | --- |
| 当前版本 | `v1.1.0`（`versionCode = 2`，`versionName = "1.1.0"`） |
| 安装包 | [Releases](https://github.com/lee688033-lgtm/LittleMaoDan-English/releases) 提供 Debug APK，未做正式签名 |
| 词库 | 1495 个 CET-6 高频词汇，位于 `app/src/main/assets/cet6/` 的 10 个 JSON |
| 运行环境 | Android 7.0（API 24）及以上，target SDK 35 |
| 技术 | Kotlin 2.0.21 · Jetpack Compose · Material 3 · Room 2.6.1（KAPT） |
| 数据 | 全部本地，无网络、无权限、无上报 |

## 核心功能

### 词汇学习

- 首页展示学习进度（已学 / 总数）、背诵与拼写入口、单词本与真题入口、学习统计摘要。
- 单词页支持词库列表浏览，按英文单词或中文释义搜索。
- 单词详情展示音标、词性、释义，以及该词在已加载词库中的位置。

### 背诵

- 顺序背诵与随机背诵两种模式。
- 可以显示或隐藏释义，上一个 / 下一个切换。
- 背诵进度与随机序列本身都会被持久化：退出重进后，同一模式继续原来的序列，而不是重新洗牌。
- 遇到不会的词可以直接加入我的单词本。

### 拼写训练

- 给出中文释义与音标，输入对应英文单词。
- 判定正确与错误，错误时展示标准答案并可重试；支持上一题 / 下一题，顺序与随机两种推进方式。
- 拼写结果写入学习记录，与掌握度、正确率统计共用同一份数据。

### 学习记录与掌握度

- 每个单词一条记录：拼写次数、正确次数、错误次数、掌握度等级、最近学习时间。
- 「我的」页汇总学习概览、学习统计（已学习 / 待学习 / 正确率 / 错误次数）、掌握情况分级，并给出单词本入口。
- 统计的分母来自实际加载的词库，不是写死的常量。

### 我的单词本

- 收藏的单词列表、移出单词本、从单词本直接进入背诵，作为独立的复习入口。

### CET-6 真题

- 历年真题列表与真题详情，详情按写作 / 听力 / 阅读 / 翻译四个部分组织。
- 阅读专项提供作答与答题卡流程。
- **写作、听力、翻译以及整套练习目前仍是占位页**，进入后显示「该功能将在下一阶段实现」。
- 阅读专项只负责作答与答题卡，**不判分**，没有正确率与错题统计。

### 阅读答题卡

- 按 Section 分组显示题号，区分已答 / 未答 / 当前三态，实时给出已答与未答数量。
- 题目分组与题号范围由 JSON 结构推导，不在代码里硬编码题数；点击题号可直接跳到对应题目。
- 答题卡状态是纯内存计算（`buildReadingSheet`），不写入数据库，避免中途退出留下半成品记录。

## UI 与交互

- **Liquid Glass 底部导航**：五项导航的选中态是一颗滑动的胶囊，由连续索引动画驱动（`spring(dampingRatio = 0.82f, stiffness = 380f)`，320ms token，带轻微横向拉伸）。这段动画只作用于底部导航的选中状态，不是全局页面转场。
- **环境光背景**：Aurora 渐变背景叠加低密度粒子层作为全局背景，深色主题自动降低强度。
- **首页动效卡片**：聚光灯高光（跟随触点）、3D 倾斜、磁吸跟随、流光文字已在首页卡片上接入；其余页面保持克制，避免视觉噪声。
- **全局 Reduce Motion**：读取 `Settings.Global.ANIMATOR_DURATION_SCALE` 与 `TRANSITION_ANIMATION_SCALE`，并在 TalkBack 触摸浏览开启时一并降级；系统关闭动画时页面直接进入终态并停止持续重绘，而不是「播一个更快的动画」。

## 技术栈

| 分类 | 技术 | 版本 |
| --- | --- | --- |
| 语言 | Kotlin | 2.0.21 |
| UI | Jetpack Compose（BOM） | 2024.12.01 |
| 设计系统 | Material 3 | 由 BOM 提供 |
| Activity 集成 | androidx.activity:activity-compose | 1.10.0 |
| 核心扩展 | androidx.core:core-ktx | 1.15.0 |
| 数据库 | Room（runtime / ktx / compiler，KAPT） | 2.6.1 |
| 异步 | Kotlin Coroutines（DAO 与 Repository 挂起函数） | 随 Kotlin |
| 构建 | Android Gradle Plugin | 8.5.2 |
| 构建 | Gradle（仓库自带 Wrapper） | 8.10.2 |
| SDK | compileSdk / targetSdk / minSdk | 35 / 35 / 24 |
| JDK | 17 | |

页面路由由 `MainActivity` 内的状态与枚举管理，**没有使用 Jetpack Navigation**；图标依赖只有 `material-icons-extended`，底部导航与五个主页面使用的图标是自建 Lucide 转换结果。

## 技术亮点

- **零权限、零网络的离线架构**：词汇与学习数据全部落在 APK 资源与应用私有数据库里，没有 INTERNET 权限，也就没有隐私弹窗与服务端依赖。
- **单一数据源驱动 UI**：词库由 `WordRepository` 一次加载并按 ID 排序，首页进度、统计分母、背诵与拼写的边界都取实际加载结果；固定的 1495 只出现在 IDE Preview 示例数据与 instrumentation 断言里，因此扩充词库不需要改动任何页面。
- **严格的数据校验**：词汇 JSON 逐条校验 `id` / `word` / `phonetic` / `partOfSpeech` / `meaning`，字段缺失即抛错而不是静默跳过；真题 JSON 额外校验 `examId` 与 `questionId` 唯一性，重复直接报错。
- **可持久化的学习状态**：`learning_records` 记录拼写与对错并推导出 0-3 级掌握度，`study_progress` 用 `progressKey` 分别保存顺序与随机模式下的 `currentIndex` 和整条随机序列，`word_book` 独立保存收藏，三者互不耦合。
- **Room 手写迁移**：数据库 `version = 3`，`MIGRATION_1_2`、`MIGRATION_2_3` 显式迁移，不使用 `fallbackToDestructiveMigration`，老用户的学习数据在升级后仍然保留。
- **答题卡与数据解耦**：`buildReadingSheet` 从 JSON 的 section / subsection 与题号范围推导出分组、总数与当前态，不硬编码题数；答题过程只存在于内存，不写数据库。
- **自绘 Design Token 与动效体系**：间距尺寸（`Dimens.kt`）、时长与曲线（`Motion.kt`）、圆角（`Shape.kt`）、颜色语义（`Color.kt`）统一收口，组件一律从 `MaterialTheme` 取值，深浅色跟随系统。
- **零依赖图标体系**：底部导航与五个页面的图标由 Lucide 官方 24×24 SVG 逐命令转换为 `ImageVector`（`M/m`、`L/l`、`H/h`、`V/v`、`A/a`、`Z` 一对一映射，含弧线的 large-arc / sweep flag），统一 viewport 24、stroke 2、圆头圆角、只描边不填充，不引入任何图标库依赖。
- **Android 15（API 35）适配**：`enableEdgeToEdge` + `WindowInsets.safeDrawing` 统一处理状态栏与手势区；底部导航自己消费 `navigationBars` inset，页面内容底边与卡片顶边对齐，实测无遮挡、切换无 Layout Jump；`values-night/styles.xml` 覆写窗口背景，避免深色冷启动时长时间白屏。

## 项目架构

```text
.
├── app/src/main/java/com/example/cet6vocabulary/
│   ├── MainActivity.kt                    应用入口、底部导航、页面路由状态、全局背景
│   ├── data/
│   │   ├── model/                         Word / LearningRecord / Exam 等数据模型
│   │   ├── local/database/AppDatabase.kt  Room 数据库（version = 3）与手写迁移
│   │   ├── local/dao/                     LearningRecordDao / StudyProgressDao / WordBookDao
│   │   ├── local/entity/                  三张表对应的 Entity
│   │   └── repository/                    WordRepository、LearningRecordRepository、
│   │                                      StudyProgressRepository、WordBookRepository、
│   │                                      ExamRepository、ExamJsonParser
│   ├── presentation/
│   │   ├── model/                         LearningStatistics、ReadingAnswerSheet（纯函数，不依赖 Compose）
│   │   └── screens/                       首页 / 单词 / 背诵 / 拼写 / 我的 / 单词本 / 真题 / 答题卡
│   └── ui/
│       ├── components/                    底部导航、动效组件、Lucide 图标、基础组件
│       └── theme/                         Color / Dimens / Motion / Shape / Type / Theme
├── app/src/main/assets/cet6/              10 个 CET-6 词汇 JSON（运行时资源）
├── app/src/main/res/                      应用图标、主题样式、values-night
├── app/src/androidTest/                   自定义 Instrumentation 校验程序
├── docs/                                  开发记录
├── build.gradle.kts / settings.gradle.kts / gradle.properties
└── gradle/wrapper/                        Gradle 8.10.2 Wrapper
```

数据流向：词汇 JSON → `WordRepository` → 页面；用户行为 → Repository → Room；页面读取 Room 与词库计算 `LearningStatistics`。项目按 UI、展示模型、Repository、本地数据层分层组织，**没有实现 ViewModel、依赖注入或 Clean Architecture**，本文不做这类包装。

## 数据组织

- 词库共 **1495** 个 CET-6 高频词汇，分布在 `app/src/main/assets/cet6/` 的 **10** 个 JSON 文件中。
- 词汇 ID 范围是 **1-1500**，其中 **1251-1255 是词库自身的断档**（所以是 1495 而不是 1500），不是加载丢失。因此单词详情页显示的「词库进度」取该词在已加载列表中的位置，而不是它的 ID。
- 每条记录字段为 `id` / `word` / `phonetic` / `partOfSpeech` / `meaning`。
- 学习记录、背诵进度、单词本存在本地 Room（`learning_records`、`study_progress`、`word_book`），数据库文件位于应用私有目录，不会上传。
- 根目录 `词汇json文件/` 是词库的原始来源副本，不属于运行时资源，已被 `.gitignore` 排除，不进入仓库。

## 真题数据说明

真题原始资料受版权与再分发限制，不在本项目的公开范围内：

- 公开仓库不包含 `真题json文件/` 目录，也没有任何真题 JSON、试卷 PDF 或听力音频。
- 公开 Release 提供的 APK 同样不含真题数据：`app/build.gradle.kts` 默认不把该目录挂进 assets，只有本地显式开启开关时才会打进安装包。
- 真题模块的源码（`ExamRepository`、`ExamJsonParser`、真题页面、阅读答题卡）完整保留，题目数据结构与解析逻辑可以直接参考。
- **从本仓库克隆后直接构建，真题列表会进入「真题数据加载失败」错误态并提供「重新加载」按钮**（安装包内没有题库数据，`ExamRepository` 抛出 `ExamDataException`），返回即可正常使用其余功能；词汇、背诵、拼写、单词本不受影响。
- 本项目不提供绕过该限制获取真题数据的方式。

本地持有真题资料、需要继续调试真题模块时，把资料放在根目录 `真题json文件/`（该目录已被 Git 忽略），再执行：

```powershell
.\gradlew.bat assembleDebug "-Pcet6.localExamAssets=true"
```

不带这个开关的每一次构建，产物里都不含真题数据。该开关只能通过 Gradle 命令行属性传入，写进 `local.properties` 不生效。

## 本地运行

环境要求：

- JDK 17
- Android SDK Platform 35（`ANDROID_HOME` 指向 SDK，或用 Android Studio 打开项目由它提供）
- Gradle 8.10.2（仓库自带 Wrapper，无需本机安装 Gradle）

用 Android Studio 打开仓库根目录，等待 Gradle Sync 完成，选择 `app` 直接 Run 即可。仓库不包含 `local.properties`，也不要提交它（已被 Git 忽略）。

## 构建 APK

Windows PowerShell，在项目根目录执行：

```powershell
.\gradlew.bat clean
.\gradlew.bat assembleDebug --no-daemon --max-workers=1 --console=plain
```

Debug 产物：

```text
app/build/outputs/apk/debug/app-debug.apk
```

Release 变体可以构建，但项目**尚未配置正式签名**，产物是未签名包，不能直接安装：

```powershell
.\gradlew.bat assembleRelease --no-daemon --max-workers=1 --console=plain
# app/build/outputs/apk/release/app-release-unsigned.apk
```

安装到已连接的设备：

```powershell
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

说明：中文路径在部分环境下会触发 Android Gradle Plugin 的路径检查，`gradle.properties` 已包含 `android.overridePathCheck=true`；项目放在英文路径时该开关不影响构建。

## 测试

这里如实说明当前测试的真实情况。

- **没有 JVM 单元测试**：项目不存在 `app/src/test/`，`.\gradlew.bat test` 会以 NO-SOURCE 通过，这不代表任何断言被执行。
- **有一个自定义 Instrumentation 校验程序**：`app/src/androidTest/.../LearningRecordPersistenceTest.kt` 中的 `PersistenceInstrumentation` 继承 `Instrumentation` 而不是 JUnit `@Test` 类，`app/build.gradle.kts` 已把 `testInstrumentationRunner` 指向它。它用一串 `check(...)` 断言跑完整条链路，任一断言失败即返回 `RESULT_CANCELED` 并打印堆栈。
- **覆盖内容**：词汇加载（1495 条、ID 序列与 1251-1255 断档、字段非空、英文与中文释义搜索）、学习统计计算、Room 学习记录重启后一致、掌握度推导、单词本迁移、背诵进度持久化、真题 JSON 解析与阅读答题卡推导。
- **运行方式**：

```powershell
.\gradlew.bat assembleDebugAndroidTest --no-daemon --max-workers=1 --console=plain
.\gradlew.bat connectedDebugAndroidTest --no-daemon --max-workers=1 --console=plain
```

- **两个必须知道的口径**：
  1. 因为 runner 被替换成自定义 `Instrumentation`，`connectedDebugAndroidTest` 生成的 XML 永远是 `tests="0"`，结论来自 instrumentation 自身输出的 `result` 字段，不能表述成「N 个测试通过」。
  2. 校验程序包含真题断言，因此**只在带 `-Pcet6.localExamAssets=true` 构建的包上才会通过**；公开仓库默认构建没有真题数据，直接运行会在真题解析这一步失败。另外 `connectedDebugAndroidTest` 结束后会卸载被测应用并清掉学习数据，回归期间可用 `adb install -r` 配合 `adb shell am instrument -w`（runner 为 `com.example.cet6vocabulary.data.PersistenceInstrumentation`）手动运行以避免清数据。

- **没有配置 CI/CD**，仓库内没有任何流水线定义。

## 版本与更新

当前版本 `v1.1.0`（`versionCode = 2`），Tag `v1.1.0` 指向 `e6446f6`。

### v1.1.0

- CET-6 词库由 781 扩充至 1495 词（10 个 JSON）。
- 新增 CET-6 真题模块：真题列表、写作 / 听力 / 阅读 / 翻译四个部分的详情组织、阅读专项作答流程。
- 新增阅读答题卡，分组与题号范围由 JSON 结构推导。
- 底部导航重构为 Liquid Glass 选中态动画，图标替换为自建 Lucide 体系。
- 新增 Aurora 与粒子背景、首页动效卡片（聚光灯 / 倾斜 / 磁吸 / 流光文字）。
- 新增全局 Reduce Motion（含 TalkBack 触摸浏览降级）。
- 统一 Design Token：`Dimens` / `Motion` / `Shape` / `Color`。
- 深色模式适配，修复深色冷启动长时间白屏。
- 修复词库 ID 断档导致的进度显示问题：进度改按已加载列表中的位置计算。
- 把真题原始数据从公开仓库与公开安装包中隔离出去。

### v1.0.0

首个完整版本：词汇浏览与搜索、顺序 / 随机背诵、拼写练习、学习记录与掌握度、我的单词本、自定义五项底部导航、Room 本地持久化，共 781 词。

**APK 下载**：前往 [GitHub Releases](https://github.com/lee688033-lgtm/LittleMaoDan-English/releases)。Release 提供的是用于项目展示与测试的 **Debug APK**，不是正式签名构建；APK 不提交进 Git 仓库。

## 界面截图

> 仓库内暂无截图，将在后续版本补充。可以安装 Release 中的 Debug APK 自行查看。

## 已知限制

- 真题的写作、听力、翻译与整套练习是占位页；阅读专项不判分，没有正确率与错题统计。
- 克隆仓库后默认构建没有真题数据，真题列表进入错误态（设计如此，见[真题数据说明](#真题数据说明)）。
- 未配置正式签名，没有可安装的 Release 构建；Release 资产是 Debug APK。
- 没有 JVM 单元测试，也没有 CI。
- 词库 ID 存在 1251-1255 断档，任何依赖连续 ID 的逻辑都需要特别处理。
- 动效的像素级观感未经主观验收。
- 项目尚未选择开源许可证，默认法律状态是「保留所有权利」。

## 后续规划

1. 阅读专项的判分、错题与正确率统计。
2. 听力、写作、翻译专项，或者收掉这几个占位页。
3. 补齐词库 1251-1255 空档并继续扩充。
4. 把自定义 Instrumentation 校验拆成可枚举的 JUnit 用例，并补 JVM 单元测试。
5. 本地学习数据导出与恢复。
6. 配置正式签名与可安装的 Release 构建。
7. 补充应用截图与版本发布说明。

## 开发记录

- [`docs/踩坑日志.md`](docs/踩坑日志.md)：按时间顺序记录的开发问题、根因与验证方式，涵盖 Compose 动效、Edge-to-edge 与 Insets、Room 迁移、Gradle 参数、APK 资源隔离、发布流程等。
- [`docs/开发阶段项目检查报告.md`](docs/开发阶段项目检查报告.md)：v1.1.0 发布前的仓库与构建检查快照，属历史文档，结论以 README 与 Release Notes 为准。

## 第三方素材与许可

- 底部导航与五个页面的图标几何取自 [Lucide](https://lucide.dev)（ISC 许可），按官方 24×24 SVG 逐命令转换为 `ImageVector`，未引入运行时依赖。
- AndroidX、Compose、Room、Material 3 依各自的 Apache License 2.0 使用。
- Gradle Wrapper 脚本依 Apache License 2.0 分发。
- 词汇 JSON 由项目自行整理维护，字段与范围见[数据组织](#数据组织)。
- CET-6 真题原始资料不随本仓库与安装包分发。

## License

License 待确定。本项目当前未附带许可证文件，默认「保留所有权利」。
