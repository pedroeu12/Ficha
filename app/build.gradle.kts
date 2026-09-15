plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.pedroeu.ficha"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.pedroeu.ficha"
        minSdk = 26
        targetSdk = 35
        versionCode = 61
        versionName = "3.21.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    /**
     * A signing key that is part of the project, rather than whatever the machine had.
     *
     * The Android plugin signs a debug build with ~/.android/debug.keystore, generating one
     * if the machine has none. A CI runner is a fresh machine every time, so every build was
     * signed with a brand new key — and Android refuses to update an installed app whose
     * signature changed. That is why installing over the top always failed and the app had to
     * be uninstalled first, losing the characters with it.
     *
     * This is a debug-grade key, kept in the repository on purpose and with the conventional
     * debug password. It protects nothing: the APK it signs is published publicly on every
     * commit. What it buys is that build 48 and build 60 are signed by the same hand, so the
     * installer treats the second as an update to the first.
     */
    signingConfigs {
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        // Lets the app show which build it is, so "am I running the new code?" is answerable.
        buildConfig = true
    }

    /**
     * Unit tests get the app's resources, so the interface can be drawn inside them.
     *
     * Without this a Compose test on the JVM cannot inflate a theme and every screen test
     * fails at the first colour lookup. With it, Robolectric can render a real screen, and a
     * test can ask the questions no amount of reading the source answers: is this list
     * scrollable, is the option at the bottom reachable, did tapping it actually change
     * anything. That is the class of bug that kept shipping — twenty-eight invocations drawn
     * into a dialog that clips at the fold reads perfectly well as source.
     */
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            all { it.systemProperty("robolectric.logging", "stdout") }
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3:1.3.1")
    implementation("androidx.compose.material:material-icons-extended")

    implementation("androidx.navigation:navigation-compose:2.8.4")

    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    testImplementation("junit:junit:4.13.2")
    // Drawing the interface on the JVM, so a test can see what the player sees.
    testImplementation("org.robolectric:robolectric:4.14.1")
    testImplementation("androidx.test.ext:junit:1.2.1")
    testImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
