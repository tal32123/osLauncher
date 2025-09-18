# Math Challenge Overlay Testing Plan

## **Test Environment Setup**

### **Build and Deploy**
1. **Clean Build**: `./gradlew clean assembleDebug`
2. **Install on Device**: `adb install -r app/build/outputs/apk/debug/app-debug.apk`
3. **Grant Permissions**: Manually grant SYSTEM_ALERT_WINDOW and POST_NOTIFICATIONS

### **Test Devices/Emulators**
- [ ] Android 6.0 (API 23) - Basic overlay functionality
- [ ] Android 8.0 (API 26) - TYPE_APPLICATION_OVERLAY transition
- [ ] Android 11 (API 30) - Permission changes
- [ ] Android 13 (API 33) - Notification permission requirements
- [ ] Android 14 (API 34) - Foreground service type requirements
- [ ] Android 15 (API 35) - Visible overlay requirements

## **Test Cases**

### **TC001: Basic Overlay Permission Flow**
**Objective**: Verify overlay permission is properly requested and granted

**Steps**:
1. Fresh install the app
2. Complete onboarding
3. Add a distracting app with time limit
4. Launch the distracting app
5. Wait for session expiry

**Expected Results**:
- [ ] Overlay permission is requested when needed
- [ ] Settings page opens for permission grant
- [ ] App detects permission grant/denial correctly
- [ ] Fallback to in-app dialog when permission denied

### **TC002: Math Challenge Overlay Display**
**Objective**: Verify math challenge overlay appears over distracting apps

**Prerequisites**: Overlay permission granted

**Steps**:
1. Set math challenge to enabled in settings
2. Add Instagram/TikTok as distracting app with 1-minute timer
3. Launch distracting app
4. Wait for countdown to complete
5. Select "Solve a math challenge" option

**Expected Results**:
- [ ] Countdown overlay appears over distracting app
- [ ] Decision dialog appears after countdown
- [ ] Math challenge overlay appears over distracting app
- [ ] Overlay blocks interaction with underlying app
- [ ] Math input field is functional

### **TC003: Math Challenge Completion Flow**
**Objective**: Verify correct math answer extends session

**Prerequisites**: Math challenge overlay displayed

**Steps**:
1. Calculate correct answer to math problem
2. Enter correct answer
3. Submit answer

**Expected Results**:
- [ ] Overlay dismisses after correct answer
- [ ] Time limit dialog appears
- [ ] Can set new session duration
- [ ] Session continues with new timer

### **TC004: Math Challenge Failure Flow**
**Objective**: Verify incorrect/no answer closes app

**Prerequisites**: Math challenge overlay displayed

**Steps**:
1. Enter incorrect answer or wait for timeout
2. Observe behavior

**Expected Results**:
- [ ] App is closed after incorrect answer
- [ ] App is closed after timeout
- [ ] User returns to launcher

### **TC005: Service Start Failure Fallback**
**Objective**: Verify fallback to in-app dialog when overlay service fails

**Steps**:
1. Force app into background restricted mode (varies by device)
2. Trigger session expiry
3. Attempt to show math challenge

**Expected Results**:
- [ ] Service start failure is caught
- [ ] In-app math challenge dialog appears in launcher
- [ ] Math challenge functions correctly in launcher
- [ ] App closure still works when math challenge fails

### **TC006: Android 15 Specific Tests**
**Objective**: Verify Android 15 visible overlay requirements

**Prerequisites**: Android 15 device/emulator

**Steps**:
1. Trigger overlay display
2. Monitor foreground service start behavior

**Expected Results**:
- [ ] Minimal overlay is created before foreground service start
- [ ] Foreground service starts successfully
- [ ] Full overlay displays correctly
- [ ] No ForegroundServiceStartNotAllowedException

### **TC007: Permission Denial Graceful Degradation**
**Objective**: Verify app continues to function without overlay permission

**Steps**:
1. Deny overlay permission when requested
2. Trigger session expiry
3. Verify fallback behavior

**Expected Results**:
- [ ] In-app math challenge appears in launcher
- [ ] App closure works correctly
- [ ] No crashes or errors

### **TC008: Multiple Session Expiry Handling**
**Objective**: Verify handling of multiple simultaneous session expiries

**Steps**:
1. Start multiple distracting apps with short timers
2. Let multiple sessions expire simultaneously

**Expected Results**:
- [ ] Sessions are queued and handled sequentially
- [ ] No overlay conflicts
- [ ] Each session is properly closed

### **TC009: Overlay Interruption Handling**
**Objective**: Verify overlay handles system interruptions

**Steps**:
1. Display math challenge overlay
2. Receive phone call / notification
3. Return to overlay

**Expected Results**:
- [ ] Overlay persists through interruptions
- [ ] Math challenge remains functional
- [ ] No memory leaks or crashes

### **TC010: Cross-Version Compatibility**
**Objective**: Verify functionality across Android versions

**Steps**:
1. Test same flow on each target Android version
2. Note any version-specific behaviors

**Expected Results**:
- [ ] Basic functionality works on all versions
- [ ] Appropriate fallbacks activate for version limitations
- [ ] No crashes on any target version

## **Performance Tests**

### **PT001: Overlay Display Performance**
**Metrics**:
- [ ] Overlay appears within 2 seconds of trigger
- [ ] Service start success rate > 95%
- [ ] Memory usage remains stable
- [ ] No ANRs during overlay operations

### **PT002: Battery Impact Assessment**
**Metrics**:
- [ ] Foreground service properly stops when not needed
- [ ] No significant battery drain from overlay service
- [ ] Wake locks are properly released

## **Debugging Tools**

### **Logging Commands**
```bash
# Monitor overlay service
adb logcat -s OverlayService

# Monitor home view model
adb logcat -s HomeViewModel

# Monitor all app logs
adb logcat | grep com.talauncher

# Check foreground services
adb shell dumpsys activity services | grep com.talauncher
```

### **Permission Checking**
```bash
# Check overlay permission
adb shell appops get com.talauncher SYSTEM_ALERT_WINDOW

# Check notification permission
adb shell pm list permissions -d -g | grep NOTIFICATION
```

### **Service Status**
```bash
# Check if service is running
adb shell ps | grep talauncher

# Check service details
adb shell dumpsys activity services com.talauncher.service.OverlayService
```

## **Known Issues to Watch For**

1. **Android 15**: Service may fail to start without visible overlay
2. **MIUI/ColorOS**: Additional overlay restrictions
3. **Battery Optimization**: Service may be killed by aggressive power management
4. **Screen Rotation**: Overlay may need to handle orientation changes
5. **Multi-Window**: Overlay behavior in split-screen mode

## **Success Criteria**

### **Must Have**:
- [ ] Math challenge overlay appears over distracting apps on all tested Android versions
- [ ] Math challenge input functions correctly
- [ ] Correct answers extend session, incorrect answers close app
- [ ] Graceful fallback when overlay fails
- [ ] No crashes or ANRs

### **Should Have**:
- [ ] Overlay appears within 2 seconds
- [ ] Overlay persists through system interruptions
- [ ] Good performance across all Android versions
- [ ] Proper battery optimization

### **Nice to Have**:
- [ ] Smooth animations
- [ ] Accessibility support
- [ ] Landscape mode support
- [ ] Multi-window compatibility

## **Test Execution Checklist**

- [ ] Environment setup complete
- [ ] Test devices configured
- [ ] All test cases executed
- [ ] Performance tests completed
- [ ] Issues documented and prioritized
- [ ] Fixes implemented for critical issues
- [ ] Regression testing completed
- [ ] Final validation on all target devices