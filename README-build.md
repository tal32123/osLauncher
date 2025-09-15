# TALauncher Build Guide 🚀

This project includes multiple ways to build your Android APK files.

## 📱 APK Files Generated

After building, you'll find APK files in:
- **Debug APK**: `app/build/outputs/apk/debug/app-debug.apk` (~10MB)
- **Release APK**: `app/build/outputs/apk/release/app-release-unsigned.apk` (~7MB)

## 🔧 Build Methods

### Method 1: TypeScript Script with Bun (Recommended)
```bash
# Build debug APK (for testing)
bun run makeAPK.ts debug

# Build release APK (optimized)
bun run makeAPK.ts release

# Build both debug and release
bun run makeAPK.ts both

# Default (debug only)
bun run makeAPK.ts

# Or use npm script
npm run makeAPK debug
npm run makeAPK release
npm run makeAPK both
```

### Method 2: Batch Script
```cmd
# Interactive mode - prompts for build type
build-apk.bat

# Direct commands
build-apk.bat debug
build-apk.bat release
build-apk.bat both
```

### Method 3: Direct Gradle Commands
```cmd
# Debug build
gradlew.bat assembleDebug

# Release build
gradlew.bat assembleRelease

# Both builds
gradlew.bat assembleDebug assembleRelease
```

## 🔍 Build Types Explained

### Debug APK
- **Size**: ~10MB
- **Purpose**: Testing and development
- **Features**:
  - Includes debug symbols
  - Larger file size
  - Easier debugging
  - Can be installed alongside other versions

### Release APK
- **Size**: ~7MB
- **Purpose**: Production/distribution
- **Features**:
  - Optimized and minified
  - Smaller file size
  - Better performance
  - Unsigned (you'll need to sign for Play Store)

## 📱 Installing the APK

### Using ADB (Android Debug Bridge)
```bash
# Install debug version
adb install app/build/outputs/apk/debug/app-debug.apk

# Install release version
adb install app/build/outputs/apk/release/app-release-unsigned.apk
```

### Manual Installation
1. Copy the APK file to your Android device
2. Enable "Install from Unknown Sources" in your device settings
3. Open the APK file and follow installation prompts

## 🐛 Troubleshooting

### Build Fails
- Ensure you have Java 11+ installed
- Make sure Android SDK is properly configured
- Try cleaning: `gradlew.bat clean`

### Permission Issues
- Run command prompt as Administrator
- Check that gradlew.bat has execute permissions

### APK Not Found
- Check the full build log for errors
- Ensure the build completed successfully
- Look for APK files in the correct output directories

## 🔧 Recent Bug Fixes

This build includes fixes for:
1. ✅ Focus mode switch now works properly on home screen
2. ✅ Essential apps display correctly when selected
3. ✅ Removed any artificial delays in app launching
4. ✅ Fixed ViewModel dependency injection issues

## 📊 Build Process

The build process includes:
- Kotlin compilation
- Resource processing
- ProGuard optimization (release only)
- APK packaging
- Lint checking
- Art profile generation (release only)