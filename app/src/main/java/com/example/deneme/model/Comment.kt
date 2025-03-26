package com.example.deneme.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class Comment(
    @DocumentId val id: String = "",
    val problemId: String = "",
    val text: String = "",
    val userId: String = "",
    val userName: String = "",
    val timestamp: Timestamp = Timestamp.now(),
    val isAcceptedAnswer: Boolean = false
) 