# 小毛蛋背英语

## 项目简介

小毛蛋背英语是一款面向 CET-6 学习的 Android 词汇与练习应用，使用本地数据帮助用户进行词汇学习、拼写练习和学习记录管理。

## 项目定位

这是一个以离线学习为主的个人学习工具。词汇、学习记录和单词本数据保存在设备本地，不依赖在线账号或远程服务。

## 主要功能

- CET-6 高频词汇（当前 781 词）
- 单词搜索
- 顺序与随机背诵
- 拼写练习
- 学习记录与掌握度统计
- 我的单词本
- CET-6 历年真题入口、试卷详情和阅读专项练习
- Jetpack Compose UI、Material 3、自定义底部导航与页面动效/UI 优化

## 技术栈

- Kotlin 2.0.21
- Android Gradle Plugin 8.5.2
- Gradle Wrapper 8.10.2
- Jetpack Compose（Compose BOM 2024.12.01）
- Material 3
- Room 2.6.1
- Android SDK compileSdk 35、targetSdk 35、minSdk 24

## 开发环境

- Android Studio（支持上述 Android Gradle Plugin 的版本）
- JDK 17
- Android SDK Platform 35
- Windows、macOS 或 Linux 均可，项目已包含 Gradle Wrapper

## 项目结构

```text
.
├── app/src/main/java/                 Kotlin 源码、数据层和 Compose 页面
├── app/src/main/assets/cet6/          CET-6 词汇 JSON（公开，781 词）
├── app/src/main/res/                  Android 资源与应用图标
├── app/src/androidTest/               Android instrumentation 测试
├── gradle/                            Gradle Wrapper 文件
├── app/build.gradle.kts                App 模块构建配置
└── settings.gradle.kts                 项目配置
```

## 当前功能完成情况

- 词汇加载、搜索、背诵、拼写练习：已实现
- 学习记录、掌握度统计、我的单词本：已实现
- 真题列表、详情和阅读专项流程：已实现
- 写作、听力、翻译及整套真题的完整答题流程：当前仍为后续功能范围

## 构建方法

Windows PowerShell：

```powershell
.\gradlew.bat assembleDebug --no-daemon --max-workers=1 --console=plain
```

APK 输出路径：`app/build/outputs/apk/debug/app-debug.apk`

## 截图

项目截图将在后续补充。

## 数据说明

公开词汇数据位于 `app/src/main/assets/cet6/`，由 6 个 JSON 文件组成，ID 连续覆盖 1--781。应用会在运行时枚举并合并这些文件。学习记录、背诵进度和单词本数据使用 Room 保存在本地设备中。

## 真题资料说明

真题资料可能涉及第三方或官方版权。当前公开仓库不包含 `真题json文件/2025-12/cet6_2025_12_set1.json`，也不包含真题 PDF、音频或答案等资料。需要真题功能数据的本地开发者应根据合法授权自行准备相应资料。

## License

本项目当前尚未附带正式开源许可证。除明确标注为项目代码或公开词汇数据的内容外，请勿未经授权再分发第三方资料。
