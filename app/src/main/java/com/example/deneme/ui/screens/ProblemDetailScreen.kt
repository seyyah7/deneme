package com.example.deneme.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
    
    LaunchedEffect(Unit) {
        viewModel.loadProblem(problemId)
        viewModel.loadComments(problemId)
    }
    
    LaunchedEffect(Unit) {
        viewModel.operation.collectLatest { state ->
            when (state) {
                is ProblemViewModel.OperationState.Loading -> {
                    isLoading = true
                }
                is ProblemViewModel.OperationState.Success -> {
                    snackbarHostState.showSnackbar(state.message)
                    isLoading = false
                    commentText = ""
                }
                is ProblemViewModel.OperationState.Error -> {
                    snackbarHostState.showSnackbar(state.message)
                    isLoading = false
                }
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Soru Detayı") },
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
                        placeholder = { Text("Yorum yazın...") },
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
                        enabled = !isLoading && commentText.isNotBlank()
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Gönder")
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
                                item {
                                    Text(
                                        text = "Yanıtlar (${commentState.comments.size})",
                                        style = MaterialTheme.typography.titleMedium,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                }
                                
                                items(commentState.comments) { comment ->
                                    CommentItem(
                                        comment = comment,
                                        isOwner = currentUser?.id == problem.userId,
                                        onMarkAsAccepted = {
                                            viewModel.markCommentAsAccepted(comment.id, problemId)
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
    onMarkAsSolved: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
    val formattedDate = dateFormat.format(problem.timestamp.toDate())
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(16.dp)
    ) {
        Text(
            text = problem.title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (problem.solved) {
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = "Çözüldü",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            
            Text(
                text = "Soran: ${problem.userName}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Text(
                text = formattedDate,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = problem.description,
            style = MaterialTheme.typography.bodyMedium
        )
        
        if (isOwner && !problem.solved) {
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = onMarkAsSolved,
                modifier = Modifier.align(Alignment.End)
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Çözüldü",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Çözüldü olarak işaretle")
            }
        }
    }
}

@Composable
fun CommentItem(
    comment: Comment,
    isOwner: Boolean,
    onMarkAsAccepted: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
    val formattedDate = dateFormat.format(comment.timestamp.toDate())
    
    val backgroundColor = if (comment.isAcceptedAnswer) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        Color.Transparent
    }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = comment.userName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            
            if (comment.isAcceptedAnswer) {
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Kabul edildi",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            "Kabul edildi",
                            color = MaterialTheme.colorScheme.onPrimary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            } else if (isOwner && !comment.isAcceptedAnswer) {
                TextButton(
                    onClick = onMarkAsAccepted,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("Kabul et", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = comment.text,
            style = MaterialTheme.typography.bodyMedium
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = formattedDate,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.align(Alignment.End)
        )
    }
} 