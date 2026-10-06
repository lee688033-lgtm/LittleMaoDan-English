plugins { id("com.android.application"); id("org.jetbrains.kotlin.android"); id("org.jetbrains.kotlin.plugin.compose"); kotlin("kapt") }

// Real exam papers live in the ignored top-level exam directory, so injecting them into the
// APK is opt-in. Without the property the exam module simply shows its empty state, which is
// exactly what a fresh clone from the public repository gets.
val localExamAssetsEnabled = providers.gradleProperty("cet6.localExamAssets").orNull.toBoolean()

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
    defaultConfig { applicationId = "com.example.cet6vocabulary"; minSdk = 24; targetSdk = 35; versionCode = 2; versionName = "1.1.0"; testInstrumentationRunner = "com.example.cet6vocabulary.data.PersistenceInstrumentation" }
}

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
}




