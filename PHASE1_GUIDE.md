# Phase 1: Project Setup & Basic UI Foundation

This guide will walk you through creating the Android Studio project and setting up the basic structure.

## Step 1: Create Android Studio Project

1. **In Android Studio:**
   - If you see "Welcome to Android Studio", click **"New Project"**
   - If you already have a project open: **File → New → New Project**

2. **Choose Template:**
   - Select **"Empty Activity"** (simplest template)
   - Click **"Next"**

3. **Configure Your Project:**
   - **Name:** `Shuckler`
   - **Package name:** `com.shuckler.app` (or your preferred package)
   - **Save location:** Choose where you want the project (or use default)
   - **Language:** `Kotlin`
   - **Minimum SDK:** `API 29: Android 10.0 (Q)`
   - **Build configuration language:** `Kotlin DSL` (recommended) or `Groovy`
   - Click **"Finish"**

4. **Wait for Gradle Sync:**
   - Android Studio will download dependencies and set up the project
   - This takes a few minutes the first time
   - Wait for "Gradle build finished" message

## Step 2: Verify Project Structure

After project creation, you should see this structure:

```
app/
├── src/
│   ├── main/
│   │   ├── java/com/shuckler/app/
│   │   │   └── MainActivity.kt
│   │   ├── res/
│   │   │   ├── layout/
│   │   │   │   └── activity_main.xml
│   │   │   ├── values/
│   │   │   │   ├── colors.xml
│   │   │   │   ├── strings.xml
│   │   │   │   └── themes.xml
│   │   │   └── mipmap/ (for app icons)
│   │   └── AndroidManifest.xml
│   └── test/
├── build.gradle.kts (or build.gradle)
└── ...
```

## Step 3: Update build.gradle Dependencies

We'll add the necessary libraries for the project. Open `app/build.gradle.kts` (or `app/build.gradle` if using Groovy).

### Add these dependencies in the `dependencies` block:

```kotlin
dependencies {
    // Existing dependencies...
    
    // Media3 (ExoPlayer) for audio playback
    implementation("androidx.media3:media3-exoplayer:1.2.1")
    implementation("androidx.media3:media3-ui:1.2.1")
    implementation("androidx.media3:media3-session:1.2.1")
    
    // Coroutines for async operations
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    
    // Lifecycle components
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    
    // Navigation Component (for fragments)
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.6")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.6")
    
    // Material Design Components
    implementation("com.google.android.material:material:1.11.0")
    
    // RecyclerView (for lists)
    implementation("androidx.recyclerview:recyclerview:1.3.2")
}
```

### Also check/update the `android` block:

```kotlin
android {
    namespace = "com.shuckler.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.shuckler.app"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        
        // ... rest of config
    }
    
    // ... rest of android block
}
```

**After adding dependencies:**
- Click **"Sync Now"** when prompted
- Wait for Gradle to download the libraries

## Step 4: Add App Icon

1. **Copy icon files:**
   - Copy `shuckle.svg` from your project root
   - In Android Studio: Right-click `app/src/main/res/drawable/` → **Paste**
   - If `drawable` folder doesn't exist, create it: Right-click `res` → **New → Android Resource Directory** → Name: `drawable`

2. **Convert SVG to Android drawable (if needed):**
   - Android Studio can handle SVG files
   - Right-click `shuckle.svg` → **Vector Asset** (if conversion needed)
   - Or use the SVG directly if supported

3. **Alternative: Use PNG for launcher icon:**
   - For the app launcher icon, you might w ant to use the PNG
   - Copy `shuckle.png` to `app/src/main/res/mipmap-xxx/` folders
   - Or use Android Studio's Image Asset Studio: **Right-click res → New → Image Asset**

## Step 5: Update AndroidManifest.xml

Open `app/src/main/AndroidManifest.xml` and add necessary permissions:

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <!-- Permissions -->
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
    
    <!-- For Android 10+, we use app-specific storage, so no external storage permission needed -->

    <application
        android:allowBackup="true"
        android:icon="@drawable/shuckle"
        android:label="@string/app_name"
        android:roundIcon="@drawable/shuckle"
        android:supportsRtl="true"
        android:theme="@style/Theme.Shuckler"
        tools:targetApi="31">
        
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:theme="@style/Theme.Shuckler">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
        
    </application>

</manifest>
```

## Step 6: Create Basic UI Layouts

We'll create layouts for the main activity and three fragments. Let's start with the main activity layout.

### Create `activity_main.xml`:

Replace the existing `res/layout/activity_main.xml` with:

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout 
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    tools:context=".MainActivity">

    <com.google.android.material.bottomnavigation.BottomNavigationView
        android:id="@+id/bottomNavigationView"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:background="?attr/colorPrimary"
        app:itemTextColor="@android:color/white"
        app:itemIconTint="@android:color/white"
        app:menu="@menu/bottom_nav_menu"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent" />

    <androidx.fragment.app.FragmentContainerView
        android:id="@+id/fragmentContainer"
        android:name="androidx.navigation.fragment.NavHostFragment"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        app:defaultNavHost="true"
        app:navGraph="@navigation/nav_graph"
        app:layout_constraintBottom_toTopOf="@id/bottomNavigationView"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toTopOf="parent" />

</androidx.constraintlayout.widget.ConstraintLayout>
```

### Create Fragment Layouts:

**`fragment_search.xml`** (in `res/layout/`):

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:padding="16dp">

    <EditText
        android:id="@+id/searchEditText"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:hint="Search for music..."
        android:inputType="text"
        android:padding="12dp" />

    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/searchResultsRecyclerView"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:layout_marginTop="16dp" />

</LinearLayout>
```

**`fragment_library.xml`** (in `res/layout/`):

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:padding="16dp">

    <TextView
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="Your Library"
        android:textSize="24sp"
        android:textStyle="bold"
        android:layout_marginBottom="16dp" />

    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/libraryRecyclerView"
        android:layout_width="match_parent"
        android:layout_height="match_parent" />

</LinearLayout>
```

**`fragment_player.xml`** (in `res/layout/`):

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:gravity="center"
    android:padding="32dp">

    <TextView
        android:id="@+id/trackTitleTextView"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="No track selected"
        android:textSize="20sp"
        android:textStyle="bold"
        android:layout_marginBottom="8dp" />

    <TextView
        android:id="@+id/trackArtistTextView"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text=""
        android:textSize="16sp"
        android:layout_marginBottom="32dp" />

    <LinearLayout
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:gravity="center">

        <Button
            android:id="@+id/previousButton"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="Previous" />

        <Button
            android:id="@+id/playPauseButton"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="Play"
            android:layout_marginStart="16dp"
            android:layout_marginEnd="16dp" />

        <Button
            android:id="@+id/nextButton"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="Next" />

    </LinearLayout>

    <Button
        android:id="@+id/loopButton"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Loop: Off"
        android:layout_marginTop="32dp" />

</LinearLayout>
```

## Step 7: Create Navigation Resources

### Create `res/menu/bottom_nav_menu.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<menu xmlns:android="http://schemas.android.com/apk/res/android">
    <item
        android:id="@+id/navigation_search"
        android:icon="@android:drawable/ic_menu_search"
        android:title="Search" />
    <item
        android:id="@+id/navigation_library"
        android:icon="@android:drawable/ic_menu_view"
        android:title="Library" />
    <item
        android:id="@+id/navigation_player"
        android:icon="@android:drawable/ic_media_play"
        android:title="Player" />
</menu>
```

### Create `res/navigation/nav_graph.xml`:

1. Right-click `res` → **New → Android Resource Directory**
2. Resource type: `navigation`
3. Click **OK**
4. Right-click `res/navigation` → **New → Navigation Resource File**
5. Name: `nav_graph`
6. Add this content:

```xml
<?xml version="1.0" encoding="utf-8"?>
<navigation xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:id="@+id/nav_graph"
    app:startDestination="@id/navigation_search">

    <fragment
        android:id="@+id/navigation_search"
        android:name="com.shuckler.app.SearchFragment"
        android:label="Search" />

    <fragment
        android:id="@+id/navigation_library"
        android:name="com.shuckler.app.LibraryFragment"
        android:label="Library" />

    <fragment
        android:id="@+id/navigation_player"
        android:name="com.shuckler.app.PlayerFragment"
        android:label="Player" />

</navigation>
```

## Step 8: Create Fragment Classes

Create three basic fragment classes. In `app/src/main/java/com/shuckler/app/`:

**`SearchFragment.kt`:**

```kotlin
package com.shuckler.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment

class SearchFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_search, container, false)
    }
}
```

**`LibraryFragment.kt`:**

```kotlin
package com.shuckler.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment

class LibraryFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_library, container, false)
    }
}
```

**`PlayerFragment.kt`:**

```kotlin
package com.shuckler.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment

class PlayerFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_player, container, false)
    }
}
```

## Step 9: Update MainActivity.kt

Replace the default `MainActivity.kt` with:

```kotlin
package com.shuckler.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val navView: BottomNavigationView = findViewById(R.id.bottomNavigationView)
        val navController = findNavController(R.id.fragmentContainer)

        // Setup bottom navigation
        navView.setupWithNavController(navController)
    }
}
```

## Step 10: Test the App

1. **Connect device or start emulator**
2. **Click Run button** (green play icon) or press `Shift + F10`
3. **App should launch** showing the Search screen
4. **Test navigation:** Tap bottom navigation items to switch between Search, Library, and Player screens

## Verification Checklist

- [ ] Project created with correct package name (`com.shuckler.app`)
- [ ] Min SDK: 29, Target SDK: 36
- [ ] Dependencies added and synced
- [ ] App icon added (shuckle.svg or shuckle.png)
- [ ] AndroidManifest.xml has correct permissions
- [ ] Three fragment layouts created
- [ ] Three fragment classes created
- [ ] Navigation set up with bottom navigation
- [ ] MainActivity.kt updated
- [ ] App runs without crashes
- [ ] Can navigate between Search, Library, and Player screens
- [ ] UI elements are visible

## Next Steps

Once Phase 1 is complete and tested, move to **Phase 2: Basic Audio Playback**.

## Troubleshooting

**Build errors:**
- Make sure all dependencies are synced (File → Sync Project with Gradle Files)
- Check that package names match in all files

**Navigation not working:**
- Verify `nav_graph.xml` exists in `res/navigation/`
- Check that fragment class names match in nav_graph.xml

**App crashes on launch:**
- Check Logcat for error messages
- Verify all layout files exist and are properly formatted
- Make sure fragment classes extend Fragment correctly
