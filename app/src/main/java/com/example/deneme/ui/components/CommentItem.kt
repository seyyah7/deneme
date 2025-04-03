package com.example.deneme.ui.components

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.deneme.model.Comment
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CommentItem(
    comment: Comment,
    currentUserId: String,
    isOwner: Boolean,
    isCommentOwner: Boolean,
    onMarkAsAccepted: () -> Unit,
    onDeleteComment: () -> Unit,
    onUserClick: (String) -> Unit
) {
    var showOptionsMenu by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var isFavorited by remember { mutableStateOf(false) }
    var upvoted by remember { mutableStateOf(false) }
    var downvoted by remember { mutableStateOf(false) }
    
    // Timestamp dönüşümünü güvenli şekilde yap
    val formattedDate = try {
        val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        dateFormat.format(comment.timestamp.toDate())
    } catch (e: Exception) {
        Log.e("CommentItem", "Timestamp dönüştürme hatası: ${e.message}", e)
        "Tarih bilinmiyor" // Hata durumunda varsayılan değer
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (comment.isAcceptedAnswer) 
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else 
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onUserClick(comment.userId) }
                ) {
                    // Profil resmi (harf avatar)
                    Surface(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape),
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = comment.userName.take(1).uppercase(),
                                color = MaterialTheme.colorScheme.onPrimary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Text(
                        text = comment.userName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Eğer sorunun sahibiyse ve yorum kabul edilmemişse, Kabul Et butonu göster
                    if (isOwner && !comment.isAcceptedAnswer) {
                        Button(
                            onClick = onMarkAsAccepted,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.tertiary
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Text("Kabul Et")
                        }
                    }
                    
                    // Yorum kabul edildiyse, Kabul Edildi etiketi göster
                    if (comment.isAcceptedAnswer) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(
                                text = "Kabul Edildi",
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    
                    // Yorum sahibiyse, silme seçeneği için 3 nokta menüsü göster
                    if (isCommentOwner) {
                        Box {
                            IconButton(onClick = { showOptionsMenu = true }) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Daha Fazla"
                                )
                            }
                            
                            DropdownMenu(
                                expanded = showOptionsMenu,
                                onDismissRequest = { showOptionsMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Sil") },
                                    onClick = { 
                                        showDeleteConfirmDialog = true
                                        showOptionsMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = comment.text,
                style = MaterialTheme.typography.bodyMedium
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Alt kısım: Butonlar ve tarih
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = "Yukarı Oy",
                        modifier = Modifier
                            .size(20.dp)
                            .clickable { 
                                upvoted = !upvoted
                                if (upvoted) downvoted = false
                            },
                        tint = if (upvoted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Aşağı Oy",
                        modifier = Modifier
                            .size(20.dp)
                            .clickable { 
                                downvoted = !downvoted
                                if (downvoted) upvoted = false
                            },
                        tint = if (downvoted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Favori",
                        modifier = Modifier
                            .size(20.dp)
                            .clickable { isFavorited = !isFavorited },
                        tint = if (isFavorited) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
                
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
    
    // Silme onay dialoğu
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Yanıtı Sil") },
            text = { Text("Bu yanıtı silmek istediğinizden emin misiniz? Bu işlem geri alınamaz.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteComment()
                        showDeleteConfirmDialog = false
                    }
                ) {
                    Text("Sil", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirmDialog = false }
                ) {
                    Text("İptal")
                }
            }
        )
    }
} 