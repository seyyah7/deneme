// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:7.3.0") // 8.7.0 yerine 7.3.0’a düşür
        classpath("com.google.gms:google-services:4.3.15") // Daha eski sürüm
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.0") // 1.9.23 yerine 1.9.0’ya düşür
    }
}

tasks.register("clean", Delete::class) {
    delete(layout.buildDirectory)
}