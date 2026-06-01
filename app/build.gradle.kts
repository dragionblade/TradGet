import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    // Processes google-services.json and injects Firebase config
    id("com.google.gms.google-services")
}

// Read local.properties so MAPS_API_KEY can be injected into BuildConfig
val localProps = Properties()
val localPropsFile = rootProject.file("local.properties")
if (localPropsFile.exists()) {
    localProps.load(localPropsFile.inputStream())
}

android {
    namespace = "com.example.tradget"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.tradget"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Inject API key into BuildConfig and Manifest placeholder
        val mapsKey = localProps.getProperty("MAPS_API_KEY", "")
        buildConfigField("String", "MAPS_API_KEY", "\"$mapsKey\"")
        manifestPlaceholders["MAPS_API_KEY"] = mapsKey
    }

    buildFeatures {
        buildConfig = true
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation("com.google.android.material:material:1.11.0")
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    implementation("com.google.android.gms:play-services-location:21.0.1")
    implementation("com.google.android.libraries.places:places:3.5.0")
    // HTTP client for Directions API calls
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    
    // JSON serialization for caching
    implementation("com.google.code.gson:gson:2.10.1")

    // ── Firebase ───────────────────────────────────────────────────────────────
    // BOM controls all Firebase library versions in sync
    implementation(platform("com.google.firebase:firebase-bom:33.1.0"))
    implementation("com.google.firebase:firebase-auth")           // Authentication
    implementation("com.google.firebase:firebase-firestore")      // Cloud Firestore (rides, users, requests)
    implementation("com.google.firebase:firebase-database")       // Realtime Database (live chat)

    // Google Sign-In (works alongside Firebase Auth)
    implementation("com.google.android.gms:play-services-auth:21.2.0")
}