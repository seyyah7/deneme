package com.example.deneme.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.deneme.model.Chat
import com.example.deneme.model.Message
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor() : ViewModel() {
    
    private val firestore = FirebaseFirestore.getInstance()
    private val messagesCollection = firestore.collection("messages")
    private val chatsCollection = firestore.collection("chats")
    
    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages
    
    private val _chats = MutableStateFlow<List<Chat>>(emptyList())
    val chats: StateFlow<List<Chat>> = _chats
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    
    private val _totalUnreadCount = MutableStateFlow(0)
    val totalUnreadCount: StateFlow<Int> = _totalUnreadCount
    
    // Chat ID oluştur (kullanıcı id'lerini alfabetik sıraya göre birleştir)
    fun createChatId(userId1: String, userId2: String): String {
        return if (userId1 < userId2) "${userId1}_${userId2}" else "${userId2}_${userId1}"
    }
    
    // Kullanıcının tüm sohbetlerini yükle
    fun loadChats(userId: String) {
        _isLoading.value = true
        
        // Debug mesajı ekle
        Log.d("ChatViewModel", "Loading chats for user: $userId")
        
        // Mevcut dinleyicileri temizle ve yeni bir dinleyici ekle
        chatsCollection
            .whereArrayContains("participants", userId)
            .orderBy("lastMessageTimestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("ChatViewModel", "Error loading chats: ${error.message}", error)
                    _isLoading.value = false
                    return@addSnapshotListener
                }
                
                // Değişen belgelerin sayısını loglayalım
                Log.d("ChatViewModel", "Chats snapshot received: ${snapshot?.documents?.size ?: 0} documents")
                
                val chatList = mutableListOf<Chat>()
                var totalUnread = 0
                
                snapshot?.documents?.forEach { document ->
                    val chat = document.toObject(Chat::class.java)
                    chat?.let { 
                        chatList.add(it)
                        Log.d("ChatViewModel", "Chat loaded: ID=${it.id}, LastMessage=${it.lastMessage}, LastSender=${it.lastMessageSenderId}")
                        
                        // Eğer son mesaj benden değilse ve okunmamış mesaj varsa sayacı arttır
                        if (it.lastMessageSenderId != userId && it.unreadCount > 0) {
                            totalUnread += it.unreadCount
                            Log.d("ChatViewModel", "Unread messages in chat ${it.id}: ${it.unreadCount}")
                        }
                    }
                }
                
                _chats.value = chatList
                _totalUnreadCount.value = totalUnread
                Log.d("ChatViewModel", "Total unread messages: $totalUnread")
                _isLoading.value = false
            }
    }
    
    // İki kullanıcı arasındaki mesajları yükle
    fun loadMessages(currentUserId: String, otherUserId: String) {
        _isLoading.value = true
        val chatId = createChatId(currentUserId, otherUserId)
        
        // Debug mesajı ekle
        Log.d("ChatViewModel", "Loading messages for chat: $chatId between users: $currentUserId and $otherUserId")
        
        // Gerçek zamanlı dinleyici ile mesajları takip et
        messagesCollection
            .whereEqualTo("chatId", chatId)
            .orderBy("timestamp", Query.Direction.ASCENDING)  // Zaman damgasına göre artan sıralama
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("ChatViewModel", "Error loading messages: ${error.message}", error)
                    _isLoading.value = false
                    return@addSnapshotListener
                }
                
                // Değişen belgelerin sayısını loglayalım
                Log.d("ChatViewModel", "Messages snapshot received: ${snapshot?.documents?.size ?: 0} documents")
                
                val messageList = mutableListOf<Message>()
                snapshot?.documents?.forEach { document ->
                    val message = document.toObject(Message::class.java)
                    if (message != null) {
                        messageList.add(message)
                        Log.d("ChatViewModel", "Message loaded: ID=${message.id}, Text=${message.text}, From=${message.senderId}, To=${message.receiverId}")
                    } else {
                        Log.e("ChatViewModel", "Failed to convert document to Message: ${document.id}")
                    }
                }
                
                _messages.value = messageList
                _isLoading.value = false
                
                // Mesajları okundu olarak işaretle
                markMessagesAsRead(chatId, currentUserId, otherUserId)
            }
    }
    
    // Mesaj gönder
    fun sendMessage(senderId: String, receiverId: String, text: String) {
        viewModelScope.launch {
            try {
                val chatId = createChatId(senderId, receiverId)
                
                // Debug mesajı ekle
                Log.d("ChatViewModel", "Sending message from $senderId to $receiverId: $text")
                
                // Yeni mesaj oluştur
                val messageRef = messagesCollection.document()
                val messageId = messageRef.id
                
                val newMessage = Message(
                    id = messageId,
                    senderId = senderId,
                    receiverId = receiverId,
                    text = text,
                    timestamp = Timestamp.now(),
                    isRead = false,
                    chatId = chatId
                )
                
                // Mesajı Firestore'a ekle
                messageRef.set(newMessage).await()
                Log.d("ChatViewModel", "Message saved to Firestore: $messageId")
                
                // Chat nesnesini güncelle veya oluştur
                updateOrCreateChat(chatId, senderId, receiverId, text)
                
                // Mesajlar listesini güncelle (gerçek zamanlı dinleyici bunu otomatik yapacak)
                loadChats(senderId) // Mesajlar sekmesini güncelle
                
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error sending message: ${e.message}", e)
            }
        }
    }
    
    // Chat nesnesini güncelle veya yeni oluştur
    private suspend fun updateOrCreateChat(chatId: String, senderId: String, receiverId: String, lastMessage: String) {
        try {
            val chatDoc = chatsCollection.document(chatId).get().await()
            
            if (chatDoc.exists()) {
                // Mevcut sohbeti güncelle
                val currentChat = chatDoc.toObject(Chat::class.java)
                val currentUnreadCount = currentChat?.unreadCount ?: 0
                
                // Eğer son mesajı gönderen aynı kişiyse, unreadCount'u 1 artır
                // Farklı bir gönderen ise (yani alıcı cevap vermişse), unreadCount'u 1 olarak sıfırla
                val newUnreadCount = if (currentChat?.lastMessageSenderId == senderId) {
                    currentUnreadCount + 1
                } else {
                    1
                }
                
                val updateData = mapOf(
                    "lastMessage" to lastMessage,
                    "lastMessageTimestamp" to Timestamp.now(),
                    "lastMessageSenderId" to senderId,
                    "unreadCount" to newUnreadCount,
                    "participants" to listOf(senderId, receiverId) // Katılımcıları güncelle
                )
                
                Log.d("ChatViewModel", "Updating chat: $chatId, sender: $senderId, newUnreadCount: $newUnreadCount")
                chatsCollection.document(chatId).update(updateData).await()
                
            } else {
                // Yeni sohbet oluştur
                val newChat = Chat(
                    id = chatId,
                    participants = listOf(senderId, receiverId),
                    lastMessage = lastMessage,
                    lastMessageTimestamp = Timestamp.now(),
                    lastMessageSenderId = senderId,
                    unreadCount = 1
                )
                
                Log.d("ChatViewModel", "Creating new chat: $chatId with sender: $senderId and receiver: $receiverId")
                chatsCollection.document(chatId).set(newChat).await()
            }
        } catch (e: Exception) {
            Log.e("ChatViewModel", "Error updating chat: ${e.message}", e)
        }
    }
    
    // Mesajları okundu olarak işaretle
    fun markMessagesAsRead(chatId: String, currentUserId: String, otherUserId: String) {
        viewModelScope.launch {
            try {
                // Alıcısı ben olan ve okunmamış mesajları bul
                val unreadMessages = messagesCollection
                    .whereEqualTo("chatId", chatId)
                    .whereEqualTo("receiverId", currentUserId)
                    .whereEqualTo("isRead", false)
                    .get()
                    .await()
                
                // Mesajları okundu olarak işaretle
                for (document in unreadMessages.documents) {
                    messagesCollection.document(document.id)
                        .update("isRead", true)
                }
                
                // Chat nesnesindeki okunmamış mesaj sayısını sıfırla
                val chatDoc = chatsCollection.document(chatId).get().await()
                if (chatDoc.exists() && chatDoc.get("lastMessageSenderId") != currentUserId) {
                    chatsCollection.document(chatId)
                        .update("unreadCount", 0)
                }
                
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error marking messages as read: ${e.message}", e)
            }
        }
    }
} 