package com.example.deneme.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class Message(
    @DocumentId val id: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val text: String = "",
    val timestamp: Timestamp = Timestamp.now(),
    val isRead: Boolean = false,
    val chatId: String = ""  // senderId_receiverId şeklinde oluşturulacak
)

data class Chat(
    @DocumentId val id: String = "",
    val participants: List<String> = listOf(), // [kullanıcı1Id, kullanıcı2Id]
    val lastMessage: String = "",
    val lastMessageTimestamp: Timestamp = Timestamp.now(),
    val lastMessageSenderId: String = "",
    val unreadCount: Int = 0
) 