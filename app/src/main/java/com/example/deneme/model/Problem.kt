package com.example.deneme.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class Problem(
    @DocumentId val id: String = "",
    val title: String = "",
    val description: String = "",
    val userId: String = "",
    val userName: String = "",
    val timestamp: Timestamp = Timestamp.now(),
    val solved: Boolean = false
) 