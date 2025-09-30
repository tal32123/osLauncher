# TALauncher

A calm, text-first Android launcher designed to help you reclaim focus. **TALauncher** is short for **That Android Launcher**—a personal project by Tal that replaces the noisy default home screen with a beautifully minimalist experience.

## Table of Contents
- [Why TALauncher](#why-talauncher)
- [Core Features](#core-features)
- [Design Principles](#design-principles)
- [Architecture & Tech Stack](#architecture--tech-stack)
- [Getting Started](#getting-started)
- [Building the App](#building-the-app)
- [Testing](#testing)
- [Project Structure](#project-structure)
- [Roadmap Highlights](#roadmap-highlights)
- [Contributing](#contributing)

## Why TALauncher
Modern launchers are designed to keep you engaged—often at the cost of your attention. TALauncher takes the opposite approach. It embraces quiet typography, intentional friction, and a distraction-free flow so you can:

- Launch the apps that actually matter.
- Hide the ones that pull you away from your goals.
- Stay mindful about how you spend time on your device.

## Core Features
- **Essential Apps Home Screen** – Pin a curated list of must-have apps alongside the time and date. Everything is rendered as elegant text over your wallpaper or a solid color.
- **Searchable App Drawer** – Browse every installed application in an alphabetical list, with a fast search bar and built-in shortcuts to Android settings.
- **Long-Press Actions** – Uninstall or open app info directly from the drawer with a long press—no cluttered icons required.
- **Focus Mode** – Toggle a focus session that hides or gently blocks distracting apps until you intentionally opt back in.
- **Usage Insights** – Review lightweight statistics about time spent inside your distracting apps for gentle accountability.
- **Offline by Design** – TALauncher works entirely on-device. No data leaves your phone.

## Design Principles
- **Minimal, Not Bare** – Every line of text, every bit of spacing, and every animation is deliberate.
- **Typography-First** – The interface is icon-free and built around beautiful typography and careful composition.
- **Calm Interactions** – Subtle transitions and predictable gestures create a peaceful flow.
- **Intentionally Opinionated** – No widgets, icon packs, notification dots, or other distractions.

## Architecture & Tech Stack
- **Language:** 100% Kotlin
- **UI:** Jetpack Compose with a text-centric design system
- **Architecture Pattern:** MVVM (Model-View-ViewModel) using StateFlow for reactive state
- **Persistence:** Room database for user choices like essential and distracting apps
- **System Integrations:** UsageStatsManager for insights, PackageManager for installed apps
- **Minimum SDK:** Android 5.0 (API 21) while compiling against the latest stable SDK
- **Tooling:** Gradle, ktlint, and a growing automated test suite (unit + instrumentation)

## Getting Started
1. **Install prerequisites**
   - Android Studio Giraffe (or newer)
   - Android SDK & command-line tools
   - Java 11 or later
   - [Bun](https://bun.sh/) (optional) if you want to use the helper scripts
2. **Clone the repository**
   ```bash
   git clone https://github.com/your-username/TALauncher.git
   cd TALauncher
   ```
3. **Open in Android Studio**
   - Choose **File ▸ Open…**, select the project folder, and let Gradle sync.
   - Run the `app` configuration on an emulator or physical device.

## Building the App
You can build through Android Studio as usual, or use the command line:

### Bun Script (Recommended)
```bash
bun run makeAPK.ts           # Default debug build
bun run makeAPK.ts debug     # Explicit debug build
bun run makeAPK.ts release   # Optimized release build
bun run makeAPK.ts both      # Generate both variants
```

### npm Script
```bash
npm run makeAPK debug
npm run makeAPK release
npm run makeAPK both
```

### Gradle Wrapper
```bash
./gradlew assembleDebug      # Debug APK at app/build/outputs/apk/debug/
./gradlew assembleRelease    # Release APK at app/build/outputs/apk/release/
```

### Windows Batch Helper
```cmd
build-apk.bat          # Interactive mode
build-apk.bat debug    # Specific variant
build-apk.bat release
build-apk.bat both
```

Install a generated APK on a connected device with:
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Testing
Quality matters just as much as minimalism. Run the test suites before shipping a change:

```bash
./gradlew test                # Unit tests
./gradlew connectedAndroidTest # Instrumented tests (device/emulator required)
```
Additional helper scripts live in the project root (see `run_unit_tests.bat` and `run_android_tests.bat`).

## Project Structure
```
app/                # Android application module written in Kotlin + Jetpack Compose
build.gradle.kts    # Root Gradle configuration
makeAPK.ts          # Bun script for orchestrated builds
README-build.md     # Extended build documentation
spec.md             # Product spec & feature breakdown
```

## Roadmap Highlights
- Complete focus-mode friction flows for distracting apps
- Deepen usage insights with richer, yet calm, reporting
- Expand automated coverage across ViewModels and UI components
- Polish accessibility with larger text sizing options

## Contributing
TALauncher is intentionally opinionated. If you'd like to contribute, please:

1. Align with the minimalist, text-only philosophy—icons, widgets, and visual noise won't be accepted.
2. Keep Kotlin code idiomatic, well-tested, and free of lint errors.
3. Open a pull request describing the change, why it matters, and how you verified it.

Together we can craft the Android home screen Tal always wanted—one that respects attention and feels as calm as it looks.
