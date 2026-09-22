# 小毛蛋背英语

一款基于 Kotlin + Jetpack Compose 开发的 CET-6 英语学习 Android 应用。

## 项目简介

小毛蛋背英语是一个个人学习/自用项目，面向 CET-6 词汇学习与练习场景。应用使用本地词汇和本地数据库，提供词汇浏览、背诵、拼写练习、学习记录、单词本以及部分真题练习流程。

## 核心功能

- CET-6 词汇浏览
- 英文单词 / 中文释义搜索
- 顺序背诵
- 随机背诵
- 拼写练习
- 学习记录
- 掌握度统计
- 我的单词本
- CET-6 真题列表
- 真题详情
- 阅读专项练习
- 阅读结果
- 五项底部导航
- 自定义 BottomBar 选中状态动画

## 数据规模

- 当前词汇库包含 781 个 CET-6 词汇
- 词汇通过 `app/src/main/assets/cet6/` 下的本地 JSON 文件加载
- 学习记录、背诵进度和单词本使用 Room 在本地持久化

## 技术栈

- Kotlin 2.0.21
- Jetpack Compose
- Material 3
- Room 2.6.1
- Gradle 8.10.2
- Android Gradle Plugin 8.5.2
- `compileSdk 35`
- `targetSdk 35`
- `minSdk 24`
- JDK 17

## 项目结构

```text
.
├── app/src/main/java/com/example/cet6vocabulary/
│   ├── data/model/                 数据模型
│   ├── data/local/                 Room 数据库、DAO 与 Entity
│   ├── data/repository/            词汇、学习记录、背诵进度、单词本和真题 Repository
│   ├── presentation/screens/        Compose 页面，包括词汇、背诵、拼写、单词本和真题页面
│   └── ui/                          Compose 公共组件与主题
├── app/src/main/assets/cet6/        公开的 CET-6 词汇 JSON 资源
├── app/src/androidTest/             Android instrumentation 测试
├── app/src/main/res/                Android 资源与应用图标
├── gradle/                          Gradle Wrapper 文件
├── app/build.gradle.kts              App 模块构建配置
└── settings.gradle.kts               项目配置
```

项目按 UI 层、数据模型、本地数据层和 Repository 组织；README 不将其描述为未在代码中明确实现的 MVVM 或 Clean Architecture。

## 数据与版权说明

781 个词汇 JSON 位于 `app/src/main/assets/cet6/`，这些文件属于应用运行时资源，会随公开仓库提供。

根目录 `词汇json文件/` 是原始词汇文件目录，不属于运行时资源，并且已经被 Git 忽略。

真题 JSON 不随公开 GitHub 仓库提供。公开仓库包含真题功能相关源码，但原始真题资料因版权及再分发限制未上传，因此直接克隆公开仓库后，真题模块不会自带对应题目数据。

## 开发环境

- Android Studio
- JDK 17
- Android SDK Platform 35
- 支持 Android Gradle Plugin 8.5.2 的构建环境

项目已包含 Gradle Wrapper。

## 构建

在项目根目录执行：

```powershell
.\gradlew.bat assembleDebug --no-daemon --max-workers=1 --console=plain
```

生成的 APK 位于 `app/build/outputs/apk/debug/app-debug.apk`。

## 当前版本

`v1.0.0`，当前完整版本。

## License

License 待确定。
