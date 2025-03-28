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
    val isAcceptedAnswer: Boolean = false,
    val upvotes: Int = 0,
    val downvotes: Int = 0,
    val favoritedBy: List<String> = emptyList() // favoriye ekleyen kullanıcı ID'leri
) {
    override fun toString(): String {
        return "Comment(id='$id', problemId='$problemId', text='$text', userId='$userId', userName='$userName', timestamp=$timestamp, isAcceptedAnswer=$isAcceptedAnswer, upvotes=$upvotes, downvotes=$downvotes, favoritedBy=${favoritedBy.size})"
    }
} 