package com.example.deneme.model

sealed class FirestoreResult<out T> {
    data class Success<T>(val data: T) : FirestoreResult<T>()
    data class Error(val exception: Exception) : FirestoreResult<Nothing>()
    object Loading : FirestoreResult<Nothing>()
} 