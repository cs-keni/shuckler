plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

fun localProp(name: String): String {
    val f = rootProject.file("local.properties")
    if (!f.exists()) return ""
    return f.readLines().firstOrNull { it.startsWith("$name=") }?.removePrefix("$name=").orEmpty()
}

android {
    namespace = "com.shuckler.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.shuckler.app"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Spotify Client ID for playlist import. Add to gradle.properties: SPOTIFY_CLIENT_ID=your_id
        val spotifyId = project.findProperty("SPOTIFY_CLIENT_ID") as? String ?: ""
        buildConfigField("String", "SPOTIFY_CLIENT_ID", "\"$spotifyId\"")

        // Last.fm credentials — stored in local.properties, which is excluded from git.
        buildConfigField("String", "LAST_FM_API_KEY", "\"${localProp("LAST_FM_API_KEY")}\"")
        buildConfigField("String", "LAST_FM_API_SECRET", "\"${localProp("LAST_FM_API_SECRET")}\"")
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
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.media)
    implementation(libs.okhttp)
    implementation(libs.coil.compose)
    implementation(libs.androidx.compose.ui.text.google.fonts)
    implementation(libs.androidx.palette.ktx)
    implementation(libs.newpipe.extractor)
    implementation(libs.reorderable)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
