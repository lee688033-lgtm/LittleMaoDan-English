import java.util.Properties

plugins { id("com.android.application"); id("org.jetbrains.kotlin.android"); id("org.jetbrains.kotlin.plugin.compose"); kotlin("kapt") }

// Real exam papers live in the ignored top-level exam directory, so injecting them into the
// APK is opt-in. Without the property the exam module simply shows its empty state, which is
// exactly what a fresh clone from the public repository gets.
val localExamAssetsEnabled = providers.gradleProperty("cet6.localExamAssets").orNull.toBoolean()

// Release signing material is never committed: it is read from keystore.properties (git
// ignored) or from CET6_KEYSTORE_FILE / CET6_KEYSTORE_PASSWORD / CET6_KEY_ALIAS /
// CET6_KEY_PASSWORD. Without either the release build still succeeds, it is just unsigned,
// so a fresh clone can always run assembleRelease.
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
fun signingValue(propertyName: String, envName: String): String? =
    (keystoreProperties.getProperty(propertyName) ?: System.getenv(envName))?.takeIf { it.isNotBlank() }
val releaseStoreFile = signingValue("storeFile", "CET6_KEYSTORE_FILE")
val releaseStorePassword = signingValue("storePassword", "CET6_KEYSTORE_PASSWORD")
val releaseKeyAlias = signingValue("keyAlias", "CET6_KEY_ALIAS")
val releaseKeyPassword = signingValue("keyPassword", "CET6_KEY_PASSWORD")
val releaseSigningReady =
    listOf(releaseStoreFile, releaseStorePassword, releaseKeyAlias, releaseKeyPassword).all { it != null }

android { namespace = "com.example.cet6vocabulary"; compileSdk = 35
    sourceSets {
        getByName("main") {
            // Exam papers are opt-in only: a published artifact must never carry them.
            // Local exam testing: .\gradlew.bat assembleDebug "-Pcet6.localExamAssets=true"
            // Release builds and any artifact intended for publication must be built without it.
            if (localExamAssetsEnabled) {
                assets.srcDir(rootProject.file("真题json文件"))
            }
        }
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
    defaultConfig { applicationId = "com.example.cet6vocabulary"; minSdk = 24; targetSdk = 35; versionCode = 3; versionName = "1.2.0"; testInstrumentationRunner = "com.example.cet6vocabulary.data.PersistenceInstrumentation" }
    signingConfigs {
        create("release") {
            if (releaseSigningReady) {
                storeFile = rootProject.file(releaseStoreFile!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }
    buildTypes {
        release {
            // Debug builds stay unminified so stack traces stay readable; release builds are
            // shrunk, obfuscated and resource-shrunk. The deobfuscation file lands in
            // app/build/outputs/mapping/release/mapping.txt.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (releaseSigningReady) signingConfig = signingConfigs.getByName("release")
        }
    }
}

// gradle.properties deliberately keeps the daemon on the platform charset (see the comment
// there), so any Java source compiled by Gradle is told its encoding explicitly.
tasks.withType<org.gradle.api.tasks.compile.JavaCompile>().configureEach { options.encoding = "UTF-8" }

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")
    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
    // The android.jar handed to JVM tests only stubs org.json, so the real implementation is
    // needed to test the AI prompts and the HTTP client on the host JVM.
    testImplementation("org.json:json:20240303")
}
