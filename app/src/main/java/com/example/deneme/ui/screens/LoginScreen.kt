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
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.deneme.viewmodel.AuthViewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.ui.graphics.Color

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onNavigateToSignUp: () -> Unit = {},
    onNavigateToHome: () -> Unit = {},
    viewModel: AuthViewModel = hiltViewModel()
) {
    var emailOrUsername by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    
    val authState by viewModel.authState.collectAsState()
    
    LaunchedEffect(authState) {
        when (authState) {
            is AuthViewModel.AuthState.Authenticated -> {
                try {
                    Log.d("LoginScreen", "Kullanıcı kimliği doğrulandı, ana ekrana yönlendiriliyor")
                    onNavigateToHome()
                } catch (e: Exception) {
                    Log.e("LoginScreen", "Navigasyon hatası: ${e.message}", e)
                }
            }
            is AuthViewModel.AuthState.Loading -> {
                Log.d("LoginScreen", "Yükleniyor durumu")
            }
            is AuthViewModel.AuthState.Error -> {
                Log.e("LoginScreen", "Hata durumu: ${(authState as AuthViewModel.AuthState.Error).message}")
            }
            else -> {}
        }
    }

    LaunchedEffect(authState) {
        Log.d("LoginScreen", "Auth state değişti: $authState")
        when (authState) {
            is AuthViewModel.AuthState.Loading -> {
                Log.d("LoginScreen", "Yükleniyor durumu")
            }
            is AuthViewModel.AuthState.Error -> {
                Log.e("LoginScreen", "Hata durumu: ${(authState as AuthViewModel.AuthState.Error).message}")
            }
            else -> {
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
            text = "Giriş Yap",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 32.dp)
        )
        
        OutlinedTextField(
            value = emailOrUsername,
            onValueChange = { emailOrUsername = it },
            label = { Text("E-posta veya Kullanıcı Adı") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )
        
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Şifre") },
            singleLine = true,
            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            trailingIcon = {
                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = if (isPasswordVisible) "Şifreyi Gizle" else "Şifreyi Göster",
                        tint = if (isPasswordVisible) MaterialTheme.colorScheme.primary else Color.Gray
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        )
        
        Button(
            onClick = {
                // E-posta veya kullanıcı adı kontrolü
                if (emailOrUsername.contains("@")) {
                    viewModel.signIn(emailOrUsername, password)
                } else {
                    viewModel.signInWithUsername(emailOrUsername, password)
                }
            },
            enabled = emailOrUsername.isNotBlank() && password.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            if (authState is AuthViewModel.AuthState.Loading) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Text("Giriş Yap")
            }
        }
        
        TextButton(
            onClick = onNavigateToSignUp,
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text("Hesabın yok mu? Kayıt ol")
        }
        
        // Hata mesajı gösterimi
        if (authState is AuthViewModel.AuthState.Error) {
            Text(
                text = (authState as AuthViewModel.AuthState.Error).message,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
} 