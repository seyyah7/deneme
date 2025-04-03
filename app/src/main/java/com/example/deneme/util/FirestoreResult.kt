package com.example.deneme.util

sealed class FirestoreResult<out T> {
    data class Success<T>(val data: T) : FirestoreResult<T>()
    data class Error(val exception: Exception) : FirestoreResult<Nothing>()
    object Loading : FirestoreResult<Nothing>()
}

fun <T> asComment(document: T): T = document
fun <T> asProblem(document: T): T = document 