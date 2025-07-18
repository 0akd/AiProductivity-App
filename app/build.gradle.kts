plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")

}

android {
    namespace = "com.example.myapplication"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.myapplication"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    // Retrofit
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

// Coil for image loading
    implementation("io.coil-kt:coil-compose:2.4.0")

// Compose UI
    implementation("androidx.activity:activity-compose:1.8.0")
    implementation("androidx.compose.ui:ui:1.6.0")
    implementation("androidx.compose.material:material:1.6.0")
    implementation("com.google.android.material:material:1.11.0")
    // Core Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(platform("com.google.firebase:firebase-bom:33.16.0"))
    implementation("com.google.firebase:firebase-analytics")
    // Compose BOM - manages all compose library versions
    implementation(platform(libs.androidx.compose.bom))

    // Compose UI
    implementation(libs.bundles.compose)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.compose.animation)
    implementation("com.google.code.gson:gson:2.10.1")

// In app-level build.gradle
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.1")
    implementation("androidx.activity:activity-compose:1.7.2")
    implementation("androidx.compose.ui:ui:1.5.0")
    implementation("androidx.compose.material3:material3:1.1.1")
    implementation("androidx.compose.ui:ui-tooling-preview:1.5.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Lifecycle & ViewModel
    implementation(libs.bundles.lifecycle)
    implementation(libs.androidx.activity.compose)

    // Navigation
    implementation(libs.androidx.navigation.runtime.android)
    implementation("androidx.compose.ui:ui:1.3.1")
    // Serialization
    implementation(libs.kotlinx.serialization.json)
    implementation("androidx.compose.foundation:foundation:1.6.0")


    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // For web scraping (uncomment when needed)
    // implementation(libs.bundles.networking)
    implementation("com.google.firebase:firebase-auth-ktx:22.3.1")
    implementation("com.google.firebase:firebase-firestore-ktx:24.11.1")
    implementation("com.google.android.gms:play-services-auth:21.0.0")
    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    implementation(libs.androidx.core.ktx)
    implementation(libs.bundles.lifecycle)
    implementation(libs.androidx.activity.compose)

    // Compose BOM
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.bundles.compose)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)


    // Debug
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
