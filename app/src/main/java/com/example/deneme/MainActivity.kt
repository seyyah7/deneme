package com.example.deneme

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.deneme.ui.AppNavigation
import com.example.deneme.ui.theme.DenemeTheme
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Firebase bağlantısını kontrol et
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this)
            }
            
            // Mevcut kullanıcı durumunu log'la
            val currentUser = FirebaseAuth.getInstance().currentUser
            Log.d("MainActivity", "Current user: ${currentUser?.uid ?: "No user logged in"}")
        } catch (e: Exception) {
            Log.e("MainActivity", "Firebase init error: ${e.message}", e)
        }
        
        enableEdgeToEdge()
        setContent {
            DenemeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}