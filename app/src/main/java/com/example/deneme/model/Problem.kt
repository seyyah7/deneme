package com.example.deneme.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

// Kategori enumeration
enum class ProblemCategory(val displayName: String) {
    URGENT("Acil"),
    EDUCATION("Eğitim"),
    TECHNOLOGY("Teknoloji"),
    ECONOMY("Ekonomi"),
    HEALTH("Sağlık"),
    MEDIA("Medya"),
    SPORTS("Spor"),
    SUGGESTION("Öneri")
}

data class Problem(
    @DocumentId val id: String = "",
    val title: String = "",
    val description: String = "",
    val userId: String = "",
    val userName: String = "",
    val timestamp: Timestamp = Timestamp.now(),
    val solved: Boolean = false,
    val answerCount: Int = 0,
    val category: String = ProblemCategory.SUGGESTION.name, // varsayılan olarak öneri kategorisi
    val upvotes: Int = 0,
    val downvotes: Int = 0,
    val favoritedBy: List<String> = emptyList() // favoriye ekleyen kullanıcı ID'leri
) 