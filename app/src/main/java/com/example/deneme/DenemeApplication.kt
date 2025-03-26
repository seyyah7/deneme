package com.example.deneme

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class DenemeApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Firebase'i başlat
        FirebaseApp.initializeApp(this)
        
        // Firestore ayarlarını yapılandır
        val settings = FirebaseFirestoreSettings.Builder()
            .build()
        FirebaseFirestore.getInstance().firestoreSettings = settings
    }
} 