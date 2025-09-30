pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }

    plugins {
        id("com.android.application") version "8.13.0"
        id("org.jetbrains.kotlin.android") version "2.2.20"
        id("org.jetbrains.kotlin.plugin.compose") version "2.2.20"
        id("com.google.devtools.ksp") version "2.2.20-2.0.3"
    }

    resolutionStrategy {
        eachPlugin {
            val version = requested.version
            if (!version.isNullOrEmpty()) {
                when (requested.id.id) {
                    "org.jetbrains.kotlin.android",
                    "org.jetbrains.kotlin.plugin.compose" ->
                        useModule("org.jetbrains.kotlin:kotlin-gradle-plugin:$version")

                    "com.google.devtools.ksp" ->
                        useModule("com.google.devtools.ksp:symbol-processing-gradle-plugin:$version")
                }
            }
        }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "TALauncher"
include(":app")
include(":uitests")