package com.example.deneme.ui.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Email
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
import com.example.deneme.model.Problem
import com.example.deneme.model.User
import com.example.deneme.repository.ProblemRepository
import com.example.deneme.viewmodel.AuthViewModel
import com.example.deneme.viewmodel.ProblemViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    userId: String,
    onNavigateBack: () -> Unit,
    onNavigateToProblemDetail: (String) -> Unit,
    onNavigateToChat: (String) -> Unit = {},
    authViewModel: AuthViewModel = hiltViewModel(),
    viewModel: ProblemViewModel = hiltViewModel()
) {
    // Kullanıcı bilgileri
    var user by remember { mutableStateOf<User?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    
    // Tab state 
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Sorular", "Cevaplar")
    
    // Kullanıcı bilgilerini al
    LaunchedEffect(userId) {
        authViewModel.getUserById(userId) { result ->
            result.fold(
                onSuccess = { user = it },
                onFailure = { 
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Kullanıcı bilgileri alınamadı")
                    }
                }
            )
        }
    }
    
    // Kullanıcının sorduğu soruları ve verdiği cevapları yükle
    LaunchedEffect(userId) {
        viewModel.loadUserProblemsById(userId)
        viewModel.loadUserCommentsById(userId)
    }
    
    val userProblemsState by viewModel.userProblemsState.collectAsState()
    val userCommentsState by viewModel.userCommentsState.collectAsState()
    
    // Mevcut kullanıcının ID'sini al
    val currentUserId = authViewModel.currentUser.collectAsState().value?.id ?: ""
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(user?.username ?: "Kullanıcı Profili") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Profil bilgileri
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Profil resmi
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (user?.profileImageUrl?.isNotEmpty() == true) {
                        // Profil resmi varsa göster
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(user?.profileImageUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Profil Resmi",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        // Varsayılan profil ikonu
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Profil",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(20.dp)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Kullanıcı adı
                Text(
                    text = user?.username ?: "",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                // Kullanıcı biyografisi (eğer varsa)
                if (!user?.bio.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = user?.bio ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }

                // Profil sahibi, giriş yapan kullanıcı değilse "Mesaj Gönder" butonunu göster
                if (userId != currentUserId) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            // Mesaj gönderme ekranına yönlendir
                            onNavigateToChat(userId)
                        },
                        modifier = Modifier.fillMaxWidth(0.8f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = "Mesaj Gönder",
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text("Mesaj Gönder")
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Tab Row
            TabRow(
                selectedTabIndex = selectedTabIndex,
                modifier = Modifier.fillMaxWidth()
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        text = { Text(title) },
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index }
                    )
                }
            }
            
            // Tab içerikleri
            when (selectedTabIndex) {
                0 -> {
                    // Kullanıcının soruları
                    when (userProblemsState) {
                        is ProblemViewModel.UserProblemsState.Loading -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                        is ProblemViewModel.UserProblemsState.Success -> {
                            val problems = (userProblemsState as ProblemViewModel.UserProblemsState.Success).problems
                            if (problems.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Bu kullanıcı henüz soru sormamış.")
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(problems) { problem ->
                                        UserProfileProblemItem(
                                            problem = problem,
                                            onProblemClick = { onNavigateToProblemDetail(problem.id) }
                                        )
                                    }
                                }
                            }
                        }
                        is ProblemViewModel.UserProblemsState.Error -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = (userProblemsState as ProblemViewModel.UserProblemsState.Error).message,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
                1 -> {
                    // Kullanıcının cevapları
                    when (userCommentsState) {
                        is ProblemViewModel.UserCommentsState.Loading -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                        is ProblemViewModel.UserCommentsState.Success -> {
                            val comments = (userCommentsState as ProblemViewModel.UserCommentsState.Success).comments
                            if (comments.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Bu kullanıcı henüz cevap vermemiş.")
                                }
                            } else {
                                // Cevapları soruya göre gruplayalım
                                val groupedComments = comments.groupBy { it.problemId }

                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    groupedComments.forEach { (problemId, commentsWithTitle) ->
                                        // Gruplanmış sorular için başlık göster
                                        item(key = "problem_$problemId") {
                                            // Her problem başlığı için ilk elemanın problem başlığını kullan
                                            val problemTitle = commentsWithTitle.first().problemTitle
                                            ProblemTitleCard(
                                                problemTitle = problemTitle,
                                                problemId = problemId,
                                                onNavigateToProblemDetail = { onNavigateToProblemDetail(problemId) }
                                            )
                                        }
                                        
                                        // Bu soruya verilen cevapları göster
                                        items(commentsWithTitle, key = { it.comment.id }) { commentWithTitle ->
                                            UserProfileCommentItem(
                                                comment = commentWithTitle,
                                                problemTitle = commentsWithTitle.first().problemTitle,
                                                onProblemClick = { onNavigateToProblemDetail(problemId) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        is ProblemViewModel.UserCommentsState.Error -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = (userCommentsState as ProblemViewModel.UserCommentsState.Error).message,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UserProfileProblemItem(
    problem: Problem,
    onProblemClick: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    val formattedDate = dateFormat.format(problem.timestamp.toDate())
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onProblemClick),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Başlık
            Text(
                text = problem.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Alt bilgiler
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Çözüldü durumu
                if (problem.solved) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = "Çözüldü",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun UserProfileCommentItem(
    comment: ProblemRepository.CommentWithProblemTitle,
    problemTitle: String,
    onProblemClick: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    val formattedDate = dateFormat.format(comment.comment.timestamp.toDate())
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onProblemClick),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Soru başlığı - 'Soru:' ibaresini kaldırıyoruz
            Text(
                text = problemTitle,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Yorum metni
            Text(
                text = comment.comment.text,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
    }
} 