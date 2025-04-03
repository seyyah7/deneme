package com.example.deneme.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.deneme.model.User
import com.example.deneme.repository.AuthRepository
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Initial)
    val authState: StateFlow<AuthState> = _authState

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser
    
    // Firestore erişimi
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

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

    // Profil resmi yükleme fonksiyonu
    fun uploadProfileImage(imageUri: Uri, onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) {
        val currentFirebaseUser = authRepository.currentUser
        if (currentFirebaseUser == null) {
            onError("Kullanıcı oturumu açık değil.")
            return
        }
        
        // Upload işlemi sırasında loading state
        _authState.value = AuthState.Loading
        
        viewModelScope.launch {
            try {
                Log.d("AuthViewModel", "Profil resmi yükleniyor: $imageUri")
                
                // Storage referansı oluştur
                val storageRef = storage.reference
                val profileImagesRef = storageRef.child("profile_images/${currentFirebaseUser.uid}.jpg")
                
                // Resmi yükle
                val uploadTask = profileImagesRef.putFile(imageUri)
                uploadTask.await()
                
                // Download URL'ini al
                val downloadUrl = profileImagesRef.downloadUrl.await().toString()
                Log.d("AuthViewModel", "Profil resmi yüklendi: $downloadUrl")
                
                // Kullanıcı verisini güncelle
                val userRef = firestore.collection("users").document(currentFirebaseUser.uid)
                userRef.update("profileImageUrl", downloadUrl).await()
                
                // Mevcut kullanıcı bilgisini güncelle
                _currentUser.value = _currentUser.value?.copy(profileImageUrl = downloadUrl)
                
                // Yükleme başarılı
                _authState.value = when (val state = _authState.value) {
                    is AuthState.Authenticated -> state
                    else -> AuthState.Authenticated(currentFirebaseUser)
                }
                
                // Başarı bildirimi
                onSuccess()
                
                // Kullanıcı profilini tekrar yükle
                getUserProfile()
                
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Profil resmi yükleme hatası: ${e.message}", e)
                _authState.value = when (val state = _authState.value) {
                    is AuthState.Authenticated -> state
                    else -> AuthState.Error("Profil resmi yüklenirken hata oluştu: ${e.message}")
                }
                onError("Profil resmi yüklenirken hata oluştu: ${e.message}")
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

    // Bir kullanıcının bilgilerini Firestore'dan getir
    fun getUserById(userId: String, callback: (Result<User>) -> Unit) {
        viewModelScope.launch {
            try {
                val userDoc = firestore.collection("users").document(userId).get().await()
                
                if (userDoc.exists()) {
                    val user = userDoc.toObject(User::class.java)
                    if (user != null) {
                        callback(Result.success(user))
                    } else {
                        callback(Result.failure(Exception("Kullanıcı verisi dönüştürülemedi")))
                    }
                } else {
                    callback(Result.failure(Exception("Kullanıcı bulunamadı")))
                }
            } catch (e: Exception) {
                callback(Result.failure(e))
            }
        }
    }

    // Kullanıcı adı ile giriş yapma fonksiyonu
    fun signInWithUsername(username: String, password: String) {
        Log.d("AuthViewModel", "Kullanıcı adı ile giriş işlemi başlatılıyor: $username")
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                val result = authRepository.signInWithUsername(username, password)
                result.fold(
                    onSuccess = { user ->
                        Log.d("AuthViewModel", "Kullanıcı adı ile giriş başarılı: ${user.uid}")
                        _authState.value = AuthState.Authenticated(user)
                        getUserProfile()
                    },
                    onFailure = { error ->
                        Log.e("AuthViewModel", "Kullanıcı adı ile giriş hatası: ${error.message}", error)
                        _authState.value = AuthState.Error(error.message ?: "Giriş sırasında bir hata oluştu")
                    }
                )
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Beklenmeyen giriş hatası: ${e.message}", e)
                _authState.value = AuthState.Error("Beklenmeyen bir hata oluştu: ${e.message}")
            }
        }
    }

    // Kullanıcı adının kullanılabilir olup olmadığını kontrol et
    suspend fun isUsernameAvailable(username: String): Boolean {
        return try {
            authRepository.isUsernameAvailable(username)
        } catch (e: Exception) {
            Log.e("AuthViewModel", "Kullanıcı adı kontrolü hatası: ${e.message}", e)
            false
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