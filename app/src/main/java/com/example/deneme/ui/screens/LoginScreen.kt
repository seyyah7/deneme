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
fun LoginScreen(
    onNavigateToSignUp: () -> Unit,
    onNavigateToHome: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val authState by viewModel.authState.collectAsState()
    
    LaunchedEffect(authState) {
        if (authState is AuthViewModel.AuthState.Authenticated) {
            try {
                Log.d("LoginScreen", "Kullanıcı kimliği doğrulandı, ana ekrana yönlendiriliyor")
                onNavigateToHome()
            } catch (e: Exception) {
                // Navigasyon hatası olursa log at
                Log.e("LoginScreen", "Navigasyon hatası: ${e.message}", e)
            }
        } else if (authState is AuthViewModel.AuthState.Error) {
            Log.e("LoginScreen", "Kimlik doğrulama hatası: ${(authState as AuthViewModel.AuthState.Error).message}")
        }
    }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(authState) {
        Log.d("LoginScreen", "Auth state değişti: $authState")
        when (authState) {
            is AuthViewModel.AuthState.Loading -> {
                Log.d("LoginScreen", "Yükleniyor durumu")
                isLoading = true
                errorMessage = null
            }
            is AuthViewModel.AuthState.Error -> {
                Log.e("LoginScreen", "Hata durumu: ${(authState as AuthViewModel.AuthState.Error).message}")
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
            text = "Deneme",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 32.dp)
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
                Log.d("LoginScreen", "Giriş butonu tıklandı: $email")
                viewModel.signIn(email, password)
            },
            enabled = !isLoading && email.isNotBlank() && password.isNotBlank(),
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
                Text("Giriş Yap")
            }
        }

        TextButton(
            onClick = onNavigateToSignUp,
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            Text("Hesabınız yok mu? Kaydolun")
        }
    }
} 