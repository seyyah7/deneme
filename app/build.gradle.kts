plugins {
    id("org.jetbrains.kotlin.android") // Sürümü kaldır, sadece ID kullan
    id("com.android.application")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.deneme"
    compileSdk = 30 // 34 yerine 30’a düşür

    defaultConfig {
        applicationId = "com.example.deneme"
        minSdk = 24
        targetSdk = 30 // 34 yerine 30’a düşür
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8 // 17 yerine 1.8’e düşür (daha hafif)
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8" // 17 yerine 1.8’e düşür
    }
    buildFeatures {
        viewBinding = true
        compose = true // Compose özelliğini etkinleştir
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.4.7" // 1.5.12 yerine 1.4.7’ye düşür
    }
}

dependencies {
    // Temel Android bağımlılıkları
    implementation("androidx.core:core-ktx:1.7.0") // Daha eski sürüm
    implementation("androidx.appcompat:appcompat:1.4.1") // Daha eski sürüm
    implementation("com.google.android.material:material:1.6.1") // Daha eski sürüm
    implementation("androidx.constraintlayout:constraintlayout:2.1.3") // Daha eski sürüm

    // Firebase bağımlılıkları
    implementation(platform("com.google.firebase:firebase-bom:31.0.0")) // 32.6.0 yerine 31.0.0’a düşür
    implementation("com.google.firebase:firebase-analytics-ktx")
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")
    implementation("com.google.firebase:firebase-messaging-ktx")

    // Coroutines (daha eski sürüm)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.6.4")

    // Görsel işleme (daha eski sürüm)
    implementation("com.github.bumptech.glide:glide:4.12.0")

    // Jetpack Compose bağımlılıkları
    implementation("androidx.activity:activity-compose:1.7.2") // Daha eski sürüm
    implementation(platform("androidx.compose:compose-bom:2022.10.00")) // Daha eski BOM
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.runtime:runtime")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.navigation:navigation-compose:2.5.0") // Daha eski sürüm

    // Test bağımlılıkları
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.3") // Daha eski sürüm
    androidTestImplementation("androidx.test.espresso:espresso-core:3.4.0") // Daha eski sürüm
    androidTestImplementation(platform("androidx.compose:compose-bom:2022.10.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}