# Manual Test Steps for Math Challenge Overlay

## Current Test Environment
- **Device**: Android Emulator API 36 (Android 15)
- **Status**: ✅ App installed and running
- **Overlay Permission**: ✅ Granted
- **Notification Permission**: ✅ Granted

## Test Steps to Execute

### **Step 1: Complete Onboarding (if needed)**
1. Look at the emulator screen
2. If onboarding screen appears, complete the setup
3. Grant any additional permissions requested

### **Step 2: Enable Math Challenge Feature**
1. Navigate to Settings in the app (swipe left from home screen)
2. Find "Math Challenge" toggle
3. Enable the math challenge feature
4. Set difficulty to "Easy" for testing
5. Also enable "Time Limit Prompt" if available

### **Step 3: Set Up Test App with Short Timer**
1. From the app drawer (swipe right), find YouTube
2. Long press on YouTube to add it as a distracting app
3. Set a very short time limit (1-2 minutes) for quick testing
4. Add YouTube to the home screen if needed

### **Step 4: Test the Overlay Flow**
1. Launch YouTube from the launcher
2. Wait for the countdown overlay to appear (should show after 1-2 minutes)
3. **VERIFY**: Countdown overlay appears over YouTube app
4. Wait for countdown to complete
5. **VERIFY**: Decision dialog appears with math challenge option
6. Select "Solve a math challenge"
7. **VERIFY**: Math challenge overlay appears over YouTube
8. **VERIFY**: Cannot interact with YouTube while overlay is showing

### **Step 5: Test Math Challenge Completion**
1. **Test Correct Answer**:
   - Solve the math problem correctly
   - Submit the answer
   - **VERIFY**: Overlay dismisses and time limit dialog appears
   - Set a new timer if prompted

2. **Test Incorrect Answer**:
   - Launch YouTube again with short timer
   - When math challenge appears, enter wrong answer
   - **VERIFY**: App is closed and returns to launcher

## Expected Results

### ✅ Success Indicators:
- [ ] Countdown overlay appears over YouTube (not in launcher)
- [ ] Overlay dims the background app
- [ ] Math challenge overlay blocks YouTube interaction
- [ ] Correct answer extends session
- [ ] Incorrect answer closes app
- [ ] No crashes or ANRs

### ❌ Failure Indicators:
- Overlay appears in launcher instead of over YouTube
- Cannot see overlay at all
- Can still interact with YouTube while overlay is showing
- App crashes when overlay should appear
- Math challenge input doesn't work

## Debugging Commands

If issues occur, run these commands to debug:

```bash
# Check if overlay service is running
adb shell ps | grep talauncher

# Monitor app logs in real-time
adb logcat -s OverlayService HomeViewModel MainActivity

# Check for any errors
adb logcat | grep -E "(ERROR|FATAL|Exception)"

# Verify permissions
adb shell appops get com.talauncher SYSTEM_ALERT_WINDOW

# Force stop and restart app
adb shell am force-stop com.talauncher
adb shell am start -n com.talauncher/.MainActivity
```

## Test Results Log

**Date**:
**Tester**:
**Android Version**: API 36 (Android 15)

### Test Results:
- [ ] Step 1 (Onboarding): ✅ ❌
- [ ] Step 2 (Enable Math Challenge): ✅ ❌
- [ ] Step 3 (Setup Test App): ✅ ❌
- [ ] Step 4 (Overlay Flow): ✅ ❌
- [ ] Step 5 (Math Challenge): ✅ ❌

### Issues Found:


### Overall Result: ✅ PASS / ❌ FAIL

### Notes: