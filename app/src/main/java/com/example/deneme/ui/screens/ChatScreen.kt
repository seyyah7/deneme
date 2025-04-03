package com.example.deneme.ui.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.deneme.model.Message
import com.example.deneme.model.User
import com.example.deneme.viewmodel.ChatViewModel
import com.example.deneme.viewmodel.AuthViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    userId: String,
    onNavigateBack: () -> Unit,
    chatViewModel: ChatViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    // Kullanıcı bilgileri
    var otherUser by remember { mutableStateOf<User?>(null) }
    val currentUser by authViewModel.currentUser.collectAsState()
    val messages by chatViewModel.messages.collectAsState()
    val isLoading by chatViewModel.isLoading.collectAsState()
    var messageText by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Kullanıcı bilgilerini yükle
    LaunchedEffect(userId) {
        authViewModel.getUserById(userId) { result ->
            result.fold(
                onSuccess = { otherUser = it },
                onFailure = { /* Hata durumu işleme */ }
            )
        }
    }
    
    // Mesajları yükle
    LaunchedEffect(userId, currentUser) {
        if (currentUser != null) {
            // Debug mesajı ekleyelim
            Log.d("ChatScreen", "Loading messages between ${currentUser!!.id} and $userId")
            chatViewModel.loadMessages(currentUser!!.id, userId)
            
            // Görülen mesajları okundu olarak işaretle
            // Bu ek fonksiyon çağrısı, sohbeti açtığımızda mesajların hemen okundu olarak işaretlenmesini sağlar
            val chatId = chatViewModel.createChatId(currentUser!!.id, userId)
            chatViewModel.markMessagesAsRead(chatId, currentUser!!.id, userId)
        } else {
            Log.e("ChatScreen", "Cannot load messages, currentUser is null")
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(otherUser?.username ?: "Sohbet") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        placeholder = { Text("Mesajınızı yazın...") },
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
                        singleLine = true
                    )
                    
                    IconButton(
                        onClick = {
                            if (messageText.isNotBlank() && currentUser != null) {
                                chatViewModel.sendMessage(
                                    senderId = currentUser!!.id,
                                    receiverId = userId,
                                    text = messageText
                                )
                                messageText = ""
                            }
                        },
                        enabled = messageText.isNotBlank(),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send, 
                            contentDescription = "Gönder",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        // Mesajlar alanı
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            if (isLoading && messages.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (messages.isEmpty()) {
                Text(
                    text = "Henüz mesaj yok. Bir şeyler yazarak sohbete başlayın!",
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge
                )
            } else {
                // Mesajları güncel zaman damgasına göre sıralama
                val sortedMessages = remember(messages) {
                    Log.d("ChatScreen", "Sorting ${messages.size} messages")
                    // Eskiden yeniye sıralama (en yeni mesaj en altta)
                    messages.sortedBy { it.timestamp?.seconds ?: 0 }
                }
                
                // Lazycolumn'un scroll pozisyonunu hatırlamak için
                val listState = rememberLazyListState()
                
                // Yeni mesaj geldiğinde en alttaki mesaja scroll yapma
                LaunchedEffect(sortedMessages.size) {
                    if (sortedMessages.isNotEmpty()) {
                        listState.animateScrollToItem(sortedMessages.size - 1)
                    }
                }
                
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    reverseLayout = false, // Normal yukarıdan aşağı sıralama
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(sortedMessages) { message ->
                        // Log mesajı ekleme
                        Log.d("ChatScreen", "Displaying message: ID=${message.id}, Text=${message.text}")
                        
                        MessageItem(
                            message = message,
                            isOwnMessage = message.senderId == currentUser?.id
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MessageItem(
    message: Message,
    isOwnMessage: Boolean
) {
    val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val formattedTime = dateFormat.format(message.timestamp.toDate())
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = if (isOwnMessage) Alignment.End else Alignment.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isOwnMessage) 16.dp else 4.dp,
                bottomEnd = if (isOwnMessage) 4.dp else 16.dp
            ),
            color = if (isOwnMessage)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Text(
                    text = formattedTime,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.align(Alignment.End),
                    color = if (isOwnMessage)
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
} 