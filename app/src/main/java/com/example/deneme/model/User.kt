package com.example.deneme.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class User(
    @DocumentId val id: String = "",
    val email: String = "",
    val username: String = "",
    val displayName: String = "",
    val profileImageUrl: String = "",
    val createdAt: Timestamp = Timestamp.now(),
    val problemCount: Int = 0,
    val answerCount: Int = 0,
    val acceptedAnswerCount: Int = 0,
    val bio: String = "",
    val role: String = "user" // user, admin, moderator gibi roller
) 