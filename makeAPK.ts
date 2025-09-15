import { execSync } from 'child_process';
import { existsSync, statSync } from 'fs';
import { join } from 'path';

console.log('🚀 TALauncher APK Builder');
console.log('========================\n');

function runCommand(command: string, description: string): boolean {
    console.log(`📦 ${description}...`);
    try {
        execSync(command, {
            cwd: __dirname,
            stdio: 'inherit',
            shell: 'powershell.exe'
        });
        console.log(`✅ ${description} completed successfully!\n`);
        return true;
    } catch (error) {
        console.error(`❌ ${description} failed:`);
        console.error((error as Error).message);
        return false;
    }
}

function checkAPKFiles(): void {
    const debugAPK = join(__dirname, 'app', 'build', 'outputs', 'apk', 'debug', 'app-debug.apk');
    const releaseAPK = join(__dirname, 'app', 'build', 'outputs', 'apk', 'release', 'app-release-unsigned.apk');

    console.log('📱 APK Files Status:');

    if (existsSync(debugAPK)) {
        const stats = statSync(debugAPK);
        console.log(`✅ Debug APK: ${(stats.size / 1024 / 1024).toFixed(2)} MB`);
        console.log(`   📁 Location: ${debugAPK}`);
    } else {
        console.log('❌ Debug APK: Not found');
    }

    if (existsSync(releaseAPK)) {
        const stats = statSync(releaseAPK);
        console.log(`✅ Release APK: ${(stats.size / 1024 / 1024).toFixed(2)} MB (unsigned)`);
        console.log(`   📁 Location: ${releaseAPK}`);
    } else {
        console.log('❌ Release APK: Not found');
    }

    console.log();
}

// Get build type from command line arguments
const buildType = process.argv[2] || 'debug';
const validTypes = ['debug', 'release', 'both'];

if (!validTypes.includes(buildType)) {
    console.log('Usage: bun run makeAPK.ts [debug|release|both]');
    console.log('Default: debug');
    process.exit(1);
}

console.log(`🎯 Building ${buildType} APK(s)...\n`);

let success = true;

if (buildType === 'debug' || buildType === 'both') {
    success &&= runCommand('./gradlew.bat assembleDebug', 'Building Debug APK');
}

if (buildType === 'release' || buildType === 'both') {
    success &&= runCommand('./gradlew.bat assembleRelease', 'Building Release APK');
}

console.log('🔍 Build Results:');
console.log('================');

if (success) {
    checkAPKFiles();
    console.log('🎉 Build completed successfully!');
    console.log('📱 You can now install the APK on your Android device.');
    console.log('💡 Tip: Use "adb install path/to/apk" to install via ADB');
} else {
    console.log('💥 Build failed! Check the errors above.');
    process.exit(1);
}