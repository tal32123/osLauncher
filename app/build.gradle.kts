import java.util.Date

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

tasks.register("checkGitStatus") {
    doLast {
        val gitStatus = providers.exec {
            commandLine("git", "status", "--porcelain")
        }.standardOutput.asText.get().trim()

        if (gitStatus.isNotEmpty()) {
            throw GradleException("Build failed: Uncommitted changes detected. Please commit your changes before building.\n$gitStatus")
        }

        println("[OK] Git status clean - proceeding with build")
    }
}

tasks.register("generateCommitInfo") {
    dependsOn("checkGitStatus")
    doLast {
        val commitHash = providers.exec {
            commandLine("git", "rev-parse", "HEAD")
        }.standardOutput.asText.get().trim()

        val commitMessage = providers.exec {
            commandLine("git", "log", "-1", "--pretty=format:%s")
        }.standardOutput.asText.get().trim()

        val commitDate = providers.exec {
            commandLine("git", "log", "-1", "--pretty=format:%ci")
        }.standardOutput.asText.get().trim()

        val branch = providers.exec {
            commandLine("git", "rev-parse", "--abbrev-ref", "HEAD")
        }.standardOutput.asText.get().trim()

        val buildTime = Date().toString()
        val commitInfoJson = """
{
    "commit": "$commitHash",
    "message": "$commitMessage",
    "date": "$commitDate",
    "branch": "$branch",
    "buildTime": "$buildTime"
}
        """.trimIndent()

        val assetsDir = file("src/main/assets")
        assetsDir.mkdirs()
        val commitInfoFile = file("src/main/assets/commit_info.json")
        commitInfoFile.writeText(commitInfoJson)

        println("[OK] Generated commit info: $commitHash")
    }
}

tasks.matching { it.name.startsWith("assemble") || it.name.startsWith("bundle") }.configureEach {
    dependsOn("generateCommitInfo")
}

tasks.register("unitTestSuite") {
    group = "verification"
    description = "Runs all unit test variants without relying on a JUnit Suite class."
    dependsOn("testDebugUnitTest")
}

android {
    namespace = "com.talauncher"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.talauncher"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.7.6"
    }

    lint {
        abortOnError = false
        warningsAsErrors = false
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Compose BOM
    val composeBom = platform("androidx.compose:compose-bom:2025.09.00")
    implementation(composeBom)
    testImplementation(composeBom)
    androidTestImplementation(composeBom)

    // Core Android libraries
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.4")
    implementation("androidx.activity:activity-compose:1.10.0")

    // Compose UI (versions managed by BOM)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.foundation:foundation")
    // Material icons extended (for icons like ExpandMore)
    implementation("androidx.compose.material:material-icons-extended")

    // ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.9.5")

    // Room
    implementation("androidx.room:room-runtime:2.7.2")
    implementation("androidx.room:room-ktx:2.7.2")
    ksp("androidx.room:room-compiler:2.7.2")

    // Kotlin Flows
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // Unit Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:5.14.2")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.4.0")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    testImplementation("org.robolectric:robolectric:4.14.1")
    testImplementation("org.robolectric:shadows-framework:4.14.1")
    testImplementation("androidx.test:core:1.6.1")
    testImplementation("androidx.test.ext:junit:1.2.1")

    // UI debugging tools (still needed for development)
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Android instrumentation testing (Compose UI + Espresso)
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.test.espresso:espresso-intents:3.6.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test:rules:1.6.1")
    androidTestImplementation("androidx.test.uiautomator:uiautomator:2.3.0")
}


