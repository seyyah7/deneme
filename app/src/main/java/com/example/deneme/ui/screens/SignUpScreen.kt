package com.example.deneme.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.deneme.viewmodel.AuthViewModel

@Composable
fun SignUpScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToHome: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val authState by viewModel.authState.collectAsState()
    
    LaunchedEffect(authState) {
        if (authState is AuthViewModel.AuthState.Authenticated) {
            try {
                Log.d("SignUpScreen", "Kullanıcı kimliği doğrulandı, ana ekrana yönlendiriliyor")
                onNavigateToHome()
            } catch (e: Exception) {
                // Navigasyon hatası olursa log at
                Log.e("SignUpScreen", "Navigasyon hatası: ${e.message}", e)
            }
        } else if (authState is AuthViewModel.AuthState.Error) {
            Log.e("SignUpScreen", "Kimlik doğrulama hatası: ${(authState as AuthViewModel.AuthState.Error).message}")
        }
    }

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(authState) {
        Log.d("SignUpScreen", "Auth state değişti: $authState")
        when (authState) {
            is AuthViewModel.AuthState.Loading -> {
                Log.d("SignUpScreen", "Yükleniyor durumu")
                isLoading = true
                errorMessage = null
            }
            is AuthViewModel.AuthState.Error -> {
                Log.e("SignUpScreen", "Hata durumu: ${(authState as AuthViewModel.AuthState.Error).message}")
                isLoading = false
                errorMessage = (authState as AuthViewModel.AuthState.Error).message
            }
            else -> {
                isLoading = false
                errorMessage = null
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Deneme - Kayıt Ol",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Ad Soyad") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        )

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("E-posta") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Şifre") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        )

        errorMessage?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        Button(
            onClick = {
                Log.d("SignUpScreen", "Kayıt butonu tıklandı: $email")
                viewModel.signUp(email, password, name)
            },
            enabled = !isLoading && name.isNotBlank() && email.isNotBlank() && password.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Kayıt Ol")
            }
        }

        TextButton(
            onClick = onNavigateToLogin,
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            Text("Zaten hesabınız var mı? Giriş yapın")
        }
    }
} 