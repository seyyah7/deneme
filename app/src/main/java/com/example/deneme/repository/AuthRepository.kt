package com.example.deneme.repository

import com.example.deneme.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import android.util.Log

@Singleton
class AuthRepository @Inject constructor() {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val usersCollection = firestore.collection("users")

    fun addAuthStateListener(listener: (FirebaseUser?) -> Unit) {
        auth.addAuthStateListener { listener(it.currentUser) }
    }

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    suspend fun signUp(email: String, password: String, name: String): Result<FirebaseUser> {
        return try {
            Log.d("AuthRepository", "Kayıt işlemi başlatılıyor: $email")
            // Önce Firebase Authentication'a kullanıcı oluştur
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user ?: throw IllegalStateException("User was not created")
            Log.d("AuthRepository", "Firebase Auth kayıt başarılı: ${firebaseUser.uid}")
            
            try {
                // Ardından Firestore'a kullanıcı bilgilerini kaydet
                val user = User(
                    id = firebaseUser.uid,
                    name = name,
                    email = email,
                    photoUrl = firebaseUser.photoUrl?.toString() ?: ""
                )
                
                Log.d("AuthRepository", "Firestore'a kullanıcı kaydı başlatılıyor: ${firebaseUser.uid}")
                usersCollection.document(firebaseUser.uid).set(user).await()
                Log.d("AuthRepository", "Firestore'a kullanıcı kaydı başarılı: ${firebaseUser.uid}")
                Result.success(firebaseUser)
            } catch (e: Exception) {
                // Firestore kaydı başarısız olursa kullanıcı yine de giriş yapmış olsun
                Log.e("AuthRepository", "Firestore kayıt hatası: ${e.message}", e)
                Result.success(firebaseUser) // Yine de başarılı sayalım
            }
        } catch (e: Exception) {
            // Authentication hatası
            Log.e("AuthRepository", "Auth kayıt hatası: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun signIn(email: String, password: String): Result<FirebaseUser> {
        return try {
            Log.d("AuthRepository", "Giriş işlemi başlatılıyor: $email")
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val user = authResult.user ?: throw IllegalStateException("User was not authenticated")
            Log.d("AuthRepository", "Giriş başarılı: ${user.uid}")
            Result.success(user)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Giriş hatası: ${e.message}", e)
            Result.failure(e)
        }
    }

    fun signOut() {
        auth.signOut()
    }

    suspend fun getCurrentUserProfile(): Result<User> {
        val currentUser = auth.currentUser ?: return Result.failure(IllegalStateException("No authenticated user"))
        
        return try {
            val userDoc = usersCollection.document(currentUser.uid).get().await()
            val user = userDoc.toObject(User::class.java) ?: User(id = currentUser.uid)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
} 