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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
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
import com.example.deneme.model.Comment
import com.example.deneme.model.Problem
import com.example.deneme.viewmodel.AuthViewModel
import com.example.deneme.viewmodel.ProblemViewModel
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProblemDetailScreen(
    problemId: String,
    onNavigateBack: () -> Unit,
    viewModel: ProblemViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val problemDetailState by viewModel.problemDetailState.collectAsState()
    val commentsState by viewModel.commentsState.collectAsState()
    val currentUser by authViewModel.currentUser.collectAsState()
    
    var commentText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Problem ve yorumları yükle - problemId değiştiğinde tekrar çağır
    LaunchedEffect(problemId) {
        Log.d("ProblemDetailScreen", "LaunchedEffect: problemId=$problemId, yorumları ve problem detayını yüklüyorum")
        viewModel.loadProblem(problemId)
        viewModel.loadComments(problemId)
    }
    
    // Yorum ekledikten sonra yorumları tekrar yükle
    LaunchedEffect(Unit) {
        viewModel.operation.collectLatest { state ->
            when (state) {
                is ProblemViewModel.OperationState.Loading -> {
                    Log.d("ProblemDetailScreen", "OperationState.Loading")
                    isLoading = true
                }
                is ProblemViewModel.OperationState.Success -> {
                    Log.d("ProblemDetailScreen", "OperationState.Success: ${state.message}")
                    snackbarHostState.showSnackbar(state.message)
                    isLoading = false
                    commentText = ""
                    
                    // Yorumları tekrar yükle
                    viewModel.loadComments(problemId)
                }
                is ProblemViewModel.OperationState.Error -> {
                    Log.e("ProblemDetailScreen", "OperationState.Error: ${state.message}")
                    snackbarHostState.showSnackbar(state.message)
                    isLoading = false
                }
            }
        }
    }
    
    // Problem başlığını alın
    val problemTitle = when (val state = problemDetailState) {
        is ProblemViewModel.ProblemDetailState.Success -> state.problem.title
        else -> "soru başlığı buraya gelecek"
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(problemTitle) },
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
                        value = commentText,
                        onValueChange = { commentText = it },
                        placeholder = { Text("cevaplar buraya yazılıyor") },
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
                        singleLine = true
                    )
                    
                    IconButton(
                        onClick = {
                            if (commentText.isNotBlank()) {
                                isLoading = true
                                viewModel.addComment(problemId, commentText)
                            }
                        },
                        enabled = !isLoading && commentText.isNotBlank(),
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
        when (val problemState = problemDetailState) {
            is ProblemViewModel.ProblemDetailState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is ProblemViewModel.ProblemDetailState.Success -> {
                val problem = problemState.problem
                
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    item {
                        ProblemHeader(
                            problem = problem,
                            isOwner = currentUser?.id == problem.userId,
                            onMarkAsSolved = {
                                viewModel.toggleProblemSolvedStatus(problem.id)
                            },
                            onDeleteProblem = {
                                viewModel.deleteProblem(problem.id)
                                onNavigateBack()
                            }
                        )
                    }
                    
                    when (val commentState = commentsState) {
                        is ProblemViewModel.CommentsState.Loading -> {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            }
                        }
                        is ProblemViewModel.CommentsState.Success -> {
                            if (commentState.comments.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("Henüz yanıt yok, ilk cevaplayan siz olun!")
                                    }
                                }
                            } else {
                                items(commentState.comments) { comment ->
                                    CommentItem(
                                        comment = comment,
                                        isOwner = currentUser?.id == problem.userId,
                                        isCommentOwner = currentUser?.id == comment.userId,
                                        onMarkAsAccepted = {
                                            viewModel.markCommentAsAccepted(comment.id, problemId)
                                        },
                                        onDeleteComment = {
                                            viewModel.deleteComment(comment.id, problemId)
                                        }
                                    )
                                }
                            }
                        }
                        is ProblemViewModel.CommentsState.Error -> {
                            item {
                                Text(
                                    text = commentState.message,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(vertical = 16.dp)
                                )
                            }
                        }
                    }
                }
            }
            is ProblemViewModel.ProblemDetailState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = problemState.message,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun ProblemHeader(
    problem: Problem,
    isOwner: Boolean,
    onMarkAsSolved: () -> Unit,
    onDeleteProblem: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
    val formattedDate = dateFormat.format(problem.timestamp.toDate())
    var showOptionsMenu by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    
    // İkonlar için state'ler
    var isFavorited by remember { mutableStateOf(false) }
    var upvoted by remember { mutableStateOf(false) }
    var downvoted by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Başlık ve Menü
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = problem.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                
                if (isOwner) {
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
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Kullanıcı bilgisi ve Çözüldü butonu
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Profil resmi
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
                                text = problem.userName.take(1).uppercase(),
                                color = MaterialTheme.colorScheme.onPrimary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Text(
                        text = problem.userName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Çözüldü butonu
                if (isOwner && !problem.solved) {
                    Button(
                        onClick = onMarkAsSolved,
                        modifier = Modifier.padding(start = 8.dp),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text("Çözüldü olarak işaretle")
                    }
                } else if (problem.solved) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text(
                            text = "Çözüldü",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Problem açıklaması
            Text(
                text = problem.description,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
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
                            .size(24.dp)
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
                            .size(24.dp)
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
                            .size(24.dp)
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
            title = { Text("Soruyu Sil") },
            text = { Text("Bu soruyu silmek istediğinizden emin misiniz? Bu işlem geri alınamaz.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteProblem()
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

@Composable
fun CommentItem(
    comment: Comment,
    isOwner: Boolean,
    isCommentOwner: Boolean,
    onMarkAsAccepted: () -> Unit,
    onDeleteComment: () -> Unit
) {
    // Debug için yorum bilgilerini logla
    LaunchedEffect(comment.id) {
        Log.d("CommentItem", "Comment rendered - ID: ${comment.id}, Text: ${comment.text}, ProblemId: ${comment.problemId}")
    }
    
    // Timestamp dönüşümünü güvenli şekilde yap
    val formattedDate = try {
        val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        dateFormat.format(comment.timestamp.toDate())
    } catch (e: Exception) {
        Log.e("CommentItem", "Timestamp dönüştürme hatası: ${e.message}", e)
        "Tarih bilinmiyor" // Hata durumunda varsayılan değer
    }
    
    var showOptionsMenu by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var isFavorited by remember { mutableStateOf(false) }
    var upvoted by remember { mutableStateOf(false) }
    var downvoted by remember { mutableStateOf(false) }
    
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
                Text(
                    text = comment.userName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                
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