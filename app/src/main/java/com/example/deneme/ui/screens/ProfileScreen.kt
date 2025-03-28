package com.example.deneme.ui.screens

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.deneme.model.Problem
import com.example.deneme.repository.ProblemRepository
import com.example.deneme.viewmodel.AuthViewModel
import com.example.deneme.viewmodel.ProblemViewModel
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateToProblemDetail: (String) -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: ProblemViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val userProblemsState by viewModel.userProblemsState.collectAsState()
    val userCommentsState by viewModel.userCommentsState.collectAsState() 
    val currentUser by authViewModel.currentUser.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Tab için state ve callback'ler
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("sorular", "cevaplar", "favori")
    
    // İşlem durumunu takip et
    LaunchedEffect(Unit) {
        viewModel.operation.collectLatest { state ->
            when (state) {
                is ProblemViewModel.OperationState.Success -> {
                    snackbarHostState.showSnackbar(state.message)
                }
                is ProblemViewModel.OperationState.Error -> {
                    snackbarHostState.showSnackbar(state.message)
                }
                else -> {}
            }
        }
    }
    
    // Kullanıcı verileri yükle
    LaunchedEffect(Unit) {
        viewModel.loadUserProblems()
        viewModel.loadUserComments()
    }
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
        ) {
            // Çıkış butonu
            IconButton(
                onClick = {
                    authViewModel.signOut()
                    onNavigateToLogin()
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = "Çıkış yap"
                )
            }
            
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(32.dp))
                
                // Profil resmi
                Surface(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Profil",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Kullanıcı adı
                Text(
                    text = currentUser?.name ?: "kullanıcı adı",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Tab Row
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            text = { 
                                Text(
                                    text = title,
                                    textAlign = TextAlign.Center
                                )
                            },
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index }
                        )
                    }
                }
                
                // Tab İçerikleri
                when (selectedTabIndex) {
                    0 -> UserQuestionsTab(
                        userProblemsState = userProblemsState,
                        onProblemClick = onNavigateToProblemDetail,
                        onDeleteProblem = { problemId ->
                            viewModel.deleteProblem(problemId)
                        }
                    )
                    1 -> UserAnswersTab(
                        userCommentsState = userCommentsState,
                        onNavigateToProblemDetail = onNavigateToProblemDetail,
                        onDeleteComment = { commentId, problemId ->
                            viewModel.deleteComment(commentId, problemId)
                        }
                    )
                    2 -> UserFavoritesTab(
                        onProblemClick = onNavigateToProblemDetail
                    )
                }
            }
        }
    }
}

@Composable
fun UserQuestionsTab(
    userProblemsState: ProblemViewModel.UserProblemsState,
    onProblemClick: (String) -> Unit,
    onDeleteProblem: (String) -> Unit
) {
    when (userProblemsState) {
        is ProblemViewModel.UserProblemsState.Loading -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        is ProblemViewModel.UserProblemsState.Success -> {
            if (userProblemsState.problems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Henüz soru sormadınız.")
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(userProblemsState.problems) { problem ->
                        UserProblemItem(
                            problem = problem,
                            onProblemClick = onProblemClick,
                            onDeleteProblem = onDeleteProblem
                        )
                    }
                }
            }
        }
        is ProblemViewModel.UserProblemsState.Error -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = userProblemsState.message,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun UserProblemItem(
    problem: Problem,
    onProblemClick: (String) -> Unit,
    onDeleteProblem: (String) -> Unit
) {
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showOptionsMenu by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(8.dp)
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
                // Başlık - Tıklanabilir
                Text(
                    text = problem.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { 
                            onProblemClick(problem.id)
                        }
                )
                
                // 3 nokta menüsü
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
            
            // Durum etiketi
            if (problem.solved) {
                Surface(
                    modifier = Modifier
                        .padding(top = 4.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.tertiary
                ) {
                    Text(
                        text = "Çözüldü",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        color = MaterialTheme.colorScheme.onTertiary
                    )
                }
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
                        onDeleteProblem(problem.id)
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
fun UserAnswersTab(
    userCommentsState: ProblemViewModel.UserCommentsState,
    onNavigateToProblemDetail: (String) -> Unit,
    onDeleteComment: (String, String) -> Unit
) {
    when (userCommentsState) {
        is ProblemViewModel.UserCommentsState.Loading -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        is ProblemViewModel.UserCommentsState.Success -> {
            if (userCommentsState.comments.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Henüz bir soruya cevap vermediniz.")
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(userCommentsState.comments) { commentWithTitle ->
                        UserCommentItem(
                            commentWithTitle = commentWithTitle,
                            onNavigateToProblemDetail = onNavigateToProblemDetail,
                            onDeleteComment = onDeleteComment
                        )
                    }
                }
            }
        }
        is ProblemViewModel.UserCommentsState.Error -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = userCommentsState.message,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun UserCommentItem(
    commentWithTitle: ProblemRepository.CommentWithProblemTitle,
    onNavigateToProblemDetail: (String) -> Unit,
    onDeleteComment: (String, String) -> Unit
) {
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showOptionsMenu by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigateToProblemDetail(commentWithTitle.problemId) },
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Problem Başlığı ve Menü
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = commentWithTitle.problemTitle,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                
                // 3 nokta menüsü
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
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Yorum içeriği - sadece kısa bir bölüm
            Text(
                text = commentWithTitle.comment.text,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Kabul edildi durumu
            if (commentWithTitle.comment.isAcceptedAnswer) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.tertiary
                ) {
                    Text(
                        text = "Kabul Edildi",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        color = MaterialTheme.colorScheme.onTertiary
                    )
                }
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
                        onDeleteComment(commentWithTitle.comment.id, commentWithTitle.problemId)
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
fun UserFavoritesTab(
    onProblemClick: (String) -> Unit,
    viewModel: ProblemViewModel = hiltViewModel()
) {
    val favoritesState = remember { mutableStateOf<List<Problem>>(emptyList()) }
    val isLoading = remember { mutableStateOf(true) }
    
    // Favoriler henüz uygulanmadığı için boş bir sayfa gösteriyoruz
    if (isLoading.value) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else if (favoritesState.value.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            Text("Henüz favori bir soru eklemediniz.")
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(favoritesState.value) { problem ->
                UserProblemItem(
                    problem = problem,
                    onProblemClick = onProblemClick,
                    onDeleteProblem = { /* Favoriden kaldırma işlevi eklenecek */ }
                )
            }
        }
    }
} 