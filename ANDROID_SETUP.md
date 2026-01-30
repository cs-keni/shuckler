# Android Development Setup Guide

This guide will help you set up Android development, debug your app, and test it on your device or emulator.

## IDE Choice: Cursor vs Android Studio

**You can use Cursor for all your code editing!** Here's the workflow:

- **Cursor (VSCode):** Write all your Kotlin code, edit XML layouts, manage files
- **Android Studio:** Use only for building, running, and debugging (you can keep it minimized)

**Why you still need Android Studio:**
- Build system (Gradle) - compiles your Kotlin code
- Emulator management
- APK installation and debugging
- Some Android-specific tools

**Workflow:**
1. Write code in Cursor
2. Switch to Android Studio to build/run
3. View logs in Android Studio's Logcat
4. Switch back to Cursor to continue coding

**Alternative:** You can also use Android Studio's IDE if you prefer, but Cursor works great for Android development too!

## Two Ways to Test Your App

### Option 1: Android Emulator (Recommended for Development)
- **Visual:** You see a virtual Android phone on your computer screen
- **Pros:** Fast iteration, easy to test, no need to connect your phone
- **Cons:** May be slower than real device, some features (like headphones) need real device

### Option 2: Physical Device (Your Samsung S22 Ultra)
- **Visual:** App runs directly on your phone
- **Pros:** Real performance, test actual hardware (headphones, lock screen, etc.)
- **Cons:** Need to connect via USB, enable developer mode

**Recommendation:** Use emulator for most development, use your S22 Ultra for testing media controls, headphones, and screen-off playback.

---

## Step 1: Install Android Studio

1. **Download Android Studio**
   - Go to: https://developer.android.com/studio
   - Download the installer for Windows
   - Run the installer

2. **Installation Options**
   - Choose "Standard" installation
   - It will install:
     - Android Studio IDE
     - Android SDK (Software Development Kit)
     - Android Emulator
     - All necessary tools

3. **First Launch Setup**
   - Android Studio will download additional components (this takes a while)
   - Let it complete the setup wizard

---

## Step 2: Set Up Android Emulator (Option 1)

### Create a Virtual Device:

1. **Open Android Studio**
2. **Tools → Device Manager** (or click the device icon in toolbar)
3. **Create Device**
4. **Choose a device:**
   - Select "Phone" category
   - Choose "Pixel 6" or "Pixel 7" (good for testing)
   - Click "Next"
5. **Select System Image:**
   - Choose a recent API level (API 33 or 34 recommended)
   - If not downloaded, click "Download" next to it
   - Wait for download to complete
   - Click "Next"
6. **Finish Setup:**
   - Name your device (e.g., "Pixel 6 API 33")
   - Click "Finish"

### Running the Emulator:

- Click the green ▶️ play button next to your device in Device Manager
- The emulator will boot up (first time takes a minute or two)
- You'll see a virtual Android phone on your screen!

---

## Step 3: Set Up Your Physical Device (Option 2 - Samsung S22 Ultra)

### Enable Developer Mode:

1. **On your S22 Ultra:**
   - Go to **Settings → About phone**
   - Find **Build number**
   - Tap **Build number 7 times** (you'll see "You are now a developer!")

2. **Enable USB Debugging:**
   - Go to **Settings → Developer options** (now visible)
   - Enable **USB debugging**
   - Enable **Install via USB** (optional but helpful)

### Connect Your Phone:

1. **Connect via USB cable** to your computer
2. **On your phone:** When prompted, allow USB debugging (check "Always allow from this computer")
3. **In Android Studio:** You should see your device appear in the device dropdown (top toolbar)

---

## Step 4: Viewing Logs and Debug Output

### Using Android Studio's Logcat:

1. **Open Logcat:**
   - Bottom of Android Studio, click **"Logcat"** tab
   - If not visible: **View → Tool Windows → Logcat**

2. **Filter Logs:**
   - Use the search box to filter by your app name or tag
   - Common filters:
     - `package:com.shuckler.app` (shows only your app)
     - `tag:MyTag` (shows logs with specific tag)

3. **Log Levels:**
   - **Verbose** (gray) - Everything
   - **Debug** (blue) - Debug messages
   - **Info** (green) - Informational
   - **Warn** (orange) - Warnings
   - **Error** (red) - Errors

### Printing Debug Output in Kotlin:

```kotlin
import android.util.Log

// In your code:
Log.d("TAG", "This is a debug message")
Log.i("TAG", "Info message")
Log.e("TAG", "Error message", exception)
```

**Example:**
```kotlin
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("MainActivity", "Activity created!")
    }
}
```

This will appear in Logcat when you run your app.

---

## Step 5: Running Your App

### From Android Studio:

1. **Select your device:**
   - Top toolbar, device dropdown (shows emulator or connected phone)
   - Choose your emulator or "Samsung S22 Ultra"

2. **Run the app:**
   - Click the green ▶️ **Run** button (or press `Shift + F10`)
   - Or: **Run → Run 'app'**

3. **What happens:**
   - Android Studio builds your app (compiles Kotlin to APK)
   - Installs APK on device/emulator
   - Launches the app
   - Logcat shows output

### First Run:

- First build takes a few minutes (downloading dependencies)
- Subsequent builds are much faster
- App will appear on your device/emulator screen

---

## Step 6: Debugging

### Breakpoints:

1. **Set a breakpoint:**
   - Click in the left margin next to a line of code (red dot appears)

2. **Debug mode:**
   - Click the 🐛 **Debug** button (or `Shift + F9`)
   - App runs in debug mode

3. **When breakpoint hits:**
   - Execution pauses
   - You can:
     - Inspect variables (hover over them)
     - Step through code (F8 = step over, F7 = step into)
     - View call stack
     - Evaluate expressions

### Common Debugging Tips:

- **Check Logcat** for errors (red messages)
- **Use breakpoints** to pause execution and inspect state
- **Check Android Monitor** for memory/CPU usage
- **Use "Run" for quick testing, "Debug" for detailed inspection**

---

## Visual Testing

### What You'll See:

- **Emulator:** Full Android phone interface on your computer
- **Physical Device:** App runs on your actual phone screen
- **Both:** You can interact with the app, see UI, test functionality

### UI Layout Preview:

Android Studio has a **Layout Editor**:
- Open any `.xml` layout file (e.g., `activity_main.xml`)
- See a visual preview of your UI
- Drag and drop UI elements
- Preview on different screen sizes

---

## Quick Reference Commands

### Android Studio Shortcuts:
- **Run:** `Shift + F10`
- **Debug:** `Shift + F9`
- **Stop:** `Ctrl + F2`
- **Build:** `Ctrl + F9`
- **Open Logcat:** `Alt + 6`

### ADB Commands (Advanced - Optional):
If you want to use command line:

```bash
# List connected devices
adb devices

# Install APK
adb install app-debug.apk

# View logs
adb logcat

# Clear logs
adb logcat -c
```

---

## Troubleshooting

### Emulator Won't Start:
- Check if virtualization is enabled in BIOS (Intel VT-x or AMD-V)
- Try a different system image (API 30 instead of 34)
- Increase emulator RAM in AVD settings

### Phone Not Detected:
- Make sure USB debugging is enabled
- Try different USB cable
- Install Samsung USB drivers (if needed)
- Check Windows Device Manager for your phone

### Build Errors:
- **Sync Project:** Click "Sync Now" if prompted
- **Invalid Gradle JDK:** File → Project Structure → SDK Location → Set JDK
- **Missing dependencies:** File → Sync Project with Gradle Files

### App Crashes:
- Check Logcat for red error messages
- Look for stack traces (they show which line crashed)
- Common issues: Null pointer exceptions, missing permissions

---

## Next Steps

Once you have Android Studio set up:
1. Create the Android project (Phase 1)
2. Run it on emulator to see it launch
3. Check Logcat to see any output
4. Start building the UI!

---

## Recommended Setup for Shuckler

For this project, I recommend:
- **Primary:** Use Android Emulator for most development
- **Secondary:** Use your S22 Ultra for testing:
  - Media controls (headphones, lock screen)
  - Screen-off playback
  - Real-world performance
  - Notification controls

You can switch between devices easily in Android Studio's device dropdown!
