package com.example.deneme.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.deneme.model.User
import com.example.deneme.repository.AuthRepository
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Initial)
    val authState: StateFlow<AuthState> = _authState

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser

    init {
        Log.d("AuthViewModel", "ViewModel başlatılıyor")
        checkAuthState()
        
        Log.d("AuthViewModel", "Auth state listener ekleniyor")
        authRepository.addAuthStateListener { user ->
            if (user != null) {
                Log.d("AuthViewModel", "Auth state değişti: Kullanıcı giriş yapmış: ${user.uid}")
                _authState.value = AuthState.Authenticated(user)
                getUserProfile()
            } else {
                Log.d("AuthViewModel", "Auth state değişti: Kullanıcı giriş yapmamış")
                _authState.value = AuthState.Unauthenticated
                _currentUser.value = null
            }
        }
    }

    private fun checkAuthState() {
        try {
            val user = authRepository.currentUser
            if (user != null) {
                Log.d("AuthViewModel", "Mevcut bir kullanıcı oturumu var: ${user.uid}")
                _authState.value = AuthState.Authenticated(user)
                getUserProfile()
            } else {
                Log.d("AuthViewModel", "Mevcut bir kullanıcı oturumu yok")
                _authState.value = AuthState.Unauthenticated
            }
        } catch (e: Exception) {
            Log.e("AuthViewModel", "Oturum durumu kontrol edilirken hata: ${e.message}", e)
            _authState.value = AuthState.Error("Oturum durumu kontrol edilirken hata: ${e.message}")
        }
    }

    fun signUp(email: String, password: String, name: String) {
        Log.d("AuthViewModel", "Kayıt işlemi başlatılıyor: $email")
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                val result = authRepository.signUp(email, password, name)
                result.fold(
                    onSuccess = { user ->
                        Log.d("AuthViewModel", "Kayıt başarılı: ${user.uid}")
                        _authState.value = AuthState.Authenticated(user)
                        getUserProfile()
                    },
                    onFailure = { error ->
                        Log.e("AuthViewModel", "Kayıt hatası: ${error.message}", error)
                        _authState.value = AuthState.Error(error.message ?: "Kayıt sırasında bir hata oluştu")
                    }
                )
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Beklenmeyen kayıt hatası: ${e.message}", e)
                _authState.value = AuthState.Error("Beklenmeyen bir hata oluştu: ${e.message}")
            }
        }
    }

    fun signIn(email: String, password: String) {
        Log.d("AuthViewModel", "Giriş işlemi başlatılıyor: $email")
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                val result = authRepository.signIn(email, password)
                result.fold(
                    onSuccess = { user ->
                        Log.d("AuthViewModel", "Giriş başarılı: ${user.uid}")
                        _authState.value = AuthState.Authenticated(user)
                        getUserProfile()
                    },
                    onFailure = { error ->
                        Log.e("AuthViewModel", "Giriş hatası: ${error.message}", error)
                        _authState.value = AuthState.Error(error.message ?: "Giriş sırasında bir hata oluştu")
                    }
                )
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Beklenmeyen giriş hatası: ${e.message}", e)
                _authState.value = AuthState.Error("Beklenmeyen bir hata oluştu: ${e.message}")
            }
        }
    }

    fun signOut() {
        Log.d("AuthViewModel", "Çıkış yapılıyor")
        authRepository.signOut()
        _authState.value = AuthState.Unauthenticated
        _currentUser.value = null
    }

    private fun getUserProfile() {
        Log.d("AuthViewModel", "Kullanıcı profili alınıyor")
        viewModelScope.launch {
            try {
                val result = authRepository.getCurrentUserProfile()
                result.fold(
                    onSuccess = { user ->
                        Log.d("AuthViewModel", "Kullanıcı profili başarıyla alındı: ${user.id}")
                        _currentUser.value = user
                    },
                    onFailure = { error ->
                        Log.e("AuthViewModel", "Kullanıcı profili alınamadı: ${error.message}", error)
                        // Profil bilgisi alınamadı, ama yine de giriş yapılmış durumda
                    }
                )
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Profil alma hatası: ${e.message}", e)
            }
        }
    }

    sealed class AuthState {
        object Initial : AuthState()
        object Loading : AuthState()
        object Unauthenticated : AuthState()
        data class Authenticated(val user: FirebaseUser) : AuthState()
        data class Error(val message: String) : AuthState()
    }
} 