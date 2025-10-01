import java.io.File
import java.net.URI
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.Locale
import java.util.zip.ZipInputStream

fun determineHostOs(): String {
    val name = System.getProperty("os.name").lowercase(Locale.US)
    return when {
        name.contains("win") -> "windows"
        name.contains("mac") || name.contains("darwin") -> "mac"
        else -> "linux"
    }
}

fun downloadFile(url: String, destination: File) {
    destination.parentFile.mkdirs()
    URI(url).toURL().openStream().use { input ->
        Files.copy(input, destination.toPath(), StandardCopyOption.REPLACE_EXISTING)
    }
}

fun unzip(zipFile: File, targetDir: File) {
    ZipInputStream(zipFile.inputStream()).use { zip ->
        var entry = zip.nextEntry
        while (entry != null) {
            val outFile = targetDir.resolve(entry.name)
            if (entry.isDirectory) {
                outFile.mkdirs()
            } else {
                outFile.parentFile.mkdirs()
                outFile.outputStream().use { output ->
                    zip.copyTo(output)
                }
            }
            zip.closeEntry()
            entry = zip.nextEntry
        }
    }
}

fun runSdkManager(sdkManager: File, sdkRoot: File, packages: List<String>) {
    if (packages.isEmpty()) {
        return
    }

    val command = mutableListOf(sdkManager.absolutePath, "--sdk_root=${sdkRoot.absolutePath}")
    command.addAll(packages)

    val process = ProcessBuilder(command)
        .directory(sdkRoot)
        .apply {
            val javaHome = System.getenv("JAVA_HOME") ?: System.getProperty("java.home")
            if (javaHome != null) {
                environment()["JAVA_HOME"] = javaHome
            }
        }
        .redirectErrorStream(true)
        .start()

    val output = StringBuilder()
    val reader = process.inputStream.bufferedReader()
    val writer = process.outputStream.bufferedWriter()
    while (true) {
        val line = reader.readLine() ?: break
        output.appendLine(line)
        if (line.trim().startsWith("Accept?")) {
            writer.write("y\n")
            writer.flush()
        }
    }
    writer.close()
    reader.close()

    val exitCode = process.waitFor()
    if (exitCode != 0) {
        error("Failed to install Android SDK components.\n$output")
    }
}

fun ensureManagedAndroidSdk(rootDir: File): File {
    val os = determineHostOs()
    val sdkDir = File(rootDir, ".android-sdk")

    val requiredArtifacts = mapOf(
        "platforms;android-36" to sdkDir.resolve("platforms/android-36/android.jar"),
        "build-tools;35.0.0" to sdkDir.resolve("build-tools/35.0.0"),
        "platform-tools" to if (os == "windows") sdkDir.resolve("platform-tools/adb.exe") else sdkDir.resolve("platform-tools/adb")
    )

    val missingPackages = requiredArtifacts
        .filterValues { !it.exists() }
        .keys
        .toList()

    if (missingPackages.isEmpty()) {
        ensureRobolectricDependencies(sdkDir)
        return sdkDir
    }

    val cmdlineToolsDir = sdkDir.resolve("cmdline-tools/latest")
    val sdkManagerPath = if (os == "windows") cmdlineToolsDir.resolve("bin/sdkmanager.bat") else cmdlineToolsDir.resolve("bin/sdkmanager")

    if (!sdkManagerPath.exists()) {
        val cmdlineToolsUrl = when (os) {
            "windows" -> "https://dl.google.com/android/repository/commandlinetools-win-11076708_latest.zip"
            "mac" -> "https://dl.google.com/android/repository/commandlinetools-mac-11076708_latest.zip"
            else -> "https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip"
        }

        println("Downloading Android command line tools for $os...")
        val downloadsDir = sdkDir.resolve("downloads").apply { mkdirs() }
        val zipFile = downloadsDir.resolve("commandlinetools-$os.zip")
        downloadFile(cmdlineToolsUrl, zipFile)

        val tempDir = sdkDir.resolve("cmdline-tools-temp").apply {
            deleteRecursively()
            mkdirs()
        }
        unzip(zipFile, tempDir)

        val extractedRoot = tempDir.resolve("cmdline-tools")
        cmdlineToolsDir.parentFile.mkdirs()
        if (cmdlineToolsDir.exists()) {
            cmdlineToolsDir.deleteRecursively()
        }
        extractedRoot.copyRecursively(cmdlineToolsDir, overwrite = true)
        tempDir.deleteRecursively()
    }

    if (!sdkManagerPath.canExecute()) {
        sdkManagerPath.setExecutable(true)
    }

    println("Installing Android SDK components: ${missingPackages.joinToString(", ")} ...")
    runSdkManager(sdkManagerPath, sdkDir, missingPackages)

    ensureRobolectricDependencies(sdkDir)

    return sdkDir
}

fun ensureRobolectricDependencies(sdkDir: File) {
    val dependenciesDir = sdkDir.resolve("robolectric-deps").apply { mkdirs() }
    val artifacts = listOf(
        "android-all-instrumented-5.0.2_r3-robolectric-r0-i7.jar" to
            "https://repo1.maven.org/maven2/org/robolectric/android-all-instrumented/5.0.2_r3-robolectric-r0-i7/android-all-instrumented-5.0.2_r3-robolectric-r0-i7.jar",
        "android-all-instrumented-15-robolectric-12650502-i7.jar" to
            "https://repo1.maven.org/maven2/org/robolectric/android-all-instrumented/15-robolectric-12650502-i7/android-all-instrumented-15-robolectric-12650502-i7.jar"
    )

    artifacts.forEach { (fileName, url) ->
        val target = dependenciesDir.resolve(fileName)
        if (!target.exists()) {
            println("Downloading Robolectric dependency $fileName...")
            downloadFile(url, target)
        }
    }
}

val androidHome = System.getenv("ANDROID_HOME")?.takeIf { it.isNotBlank() && File(it).exists() }
    ?: System.getenv("ANDROID_SDK_ROOT")?.takeIf { it.isNotBlank() && File(it).exists() }
    ?: ensureManagedAndroidSdk(rootDir).absolutePath

val localPropertiesFile = File(rootDir, "local.properties")
val escapedPath = androidHome.replace("\\", "\\\\")
if (!localPropertiesFile.exists() || localPropertiesFile.readText() != "sdk.dir=$escapedPath") {
    localPropertiesFile.writeText("sdk.dir=$escapedPath")
}

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