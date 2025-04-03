package com.example.deneme.ui.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.deneme.model.Chat
import com.example.deneme.model.User
import com.example.deneme.viewmodel.AuthViewModel
import com.example.deneme.viewmodel.ChatViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesScreen(
    onNavigateToChat: (String) -> Unit = {},
    viewModel: ChatViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    val chats by viewModel.chats.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    // Kullanıcı bilgilerini saklamak için map
    var userInfoMap by remember { mutableStateOf<Map<String, User>>(emptyMap()) }
    
    // Mevcut kullanıcının sohbetlerini yükle
    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            Log.d("MessagesScreen", "Loading chats for user: ${currentUser!!.id}")
            viewModel.loadChats(currentUser!!.id)
        } else {
            Log.d("MessagesScreen", "Current user is null, cannot load chats")
        }
    }
    
    // Sohbet katılımcılarının bilgilerini yükle - geliştirilmiş versiyon
    LaunchedEffect(chats) {
        if (chats.isEmpty()) {
            Log.d("MessagesScreen", "No chats available to load user info")
            return@LaunchedEffect
        }
        
        Log.d("MessagesScreen", "Loading user info for ${chats.size} chats")
        val otherUserIds = chats.mapNotNull { chat -> 
            chat.participants.find { it != currentUser?.id }
        }.distinct()
        
        Log.d("MessagesScreen", "Found ${otherUserIds.size} unique user IDs to load")
        
        otherUserIds.forEach { userId ->
            if (!userInfoMap.containsKey(userId)) {
                Log.d("MessagesScreen", "Loading user info for: $userId")
                authViewModel.getUserById(userId) { result ->
                    result.fold(
                        onSuccess = { user ->
                            Log.d("MessagesScreen", "Successfully loaded user: ${user.username}")
                            userInfoMap = userInfoMap + (userId to user)
                        },
                        onFailure = {
                            Log.e("MessagesScreen", "Error loading user info for $userId: ${it.message}")
                        }
                    )
                }
            } else {
                Log.d("MessagesScreen", "User info for $userId already in cache")
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mesajlar") }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (chats.isEmpty()) {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Henüz mesajınız yok",
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Kullanıcıların profiline gidip 'Mesaj Gönder' butonuna tıklayarak sohbete başlayabilirsiniz",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(chats) { chat ->
                        // Diğer kullanıcının ID'sini bul
                        val otherUserId = chat.participants.find { it != currentUser?.id } ?: return@items
                        
                        // Kullanıcı bilgilerini al
                        val otherUser = userInfoMap[otherUserId]
                        
                        ChatListItem(
                            chat = chat,
                            otherUser = otherUser,
                            currentUserId = currentUser?.id ?: "",
                            onClick = { onNavigateToChat(otherUserId) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChatListItem(
    chat: Chat,
    otherUser: User?,
    currentUserId: String,
    onClick: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd.MM.yyyy, HH:mm", Locale.getDefault())
    val formattedDate = dateFormat.format(chat.lastMessageTimestamp.toDate())
    
    // Son mesajı gönderen kullanıcının ben olup olmadığını kontrol et
    val isLastMessageFromMe = chat.lastMessageSenderId == currentUserId
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Profil resmi
            Surface(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape),
                color = MaterialTheme.colorScheme.primary
            ) {
                if (otherUser?.profileImageUrl?.isNotEmpty() == true) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(otherUser.profileImageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Profil Resmi",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (otherUser?.username?.take(1) ?: "?").uppercase(),
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = otherUser?.username ?: "Kullanıcı",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isLastMessageFromMe) "Sen: ${chat.lastMessage}" else chat.lastMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    
                    // Okunmamış mesaj sayısı
                    if (chat.unreadCount > 0 && !isLastMessageFromMe) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = chat.unreadCount.toString(),
                                color = MaterialTheme.colorScheme.onPrimary,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }
        }
    }
} 