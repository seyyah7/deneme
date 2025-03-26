package com.example.deneme.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.deneme.viewmodel.AuthViewModel
import com.example.deneme.viewmodel.ProblemViewModel
import com.example.deneme.ui.screens.ExpandableProblemItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateToProblemDetail: (String) -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: ProblemViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val myProblemsState by viewModel.myProblemsState.collectAsState()
    val currentUser by authViewModel.currentUser.collectAsState()
    
    // Tab için state ve callback'ler
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("sorular", "cevaplar", "favori")
    
    LaunchedEffect(Unit) {
        viewModel.loadMyProblems()
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
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
                    myProblemsState = myProblemsState,
                    onProblemClick = onNavigateToProblemDetail,
                    onToggleSolved = { problemId ->
                        viewModel.toggleProblemSolvedStatus(problemId)
                    },
                    currentUserId = currentUser?.id ?: ""
                )
                1 -> UserAnswersTab()
                2 -> UserFavoritesTab()
            }
        }
    }
}

@Composable
fun UserQuestionsTab(
    myProblemsState: ProblemViewModel.ProblemsState,
    onProblemClick: (String) -> Unit,
    onToggleSolved: (String) -> Unit,
    currentUserId: String
) {
    when (myProblemsState) {
        is ProblemViewModel.ProblemsState.Loading -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        is ProblemViewModel.ProblemsState.Success -> {
            if (myProblemsState.problems.isEmpty()) {
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
                    items(myProblemsState.problems) { problem ->
                        ExpandableProblemItem(
                            problem = problem,
                            onProblemClick = { onProblemClick(problem.id) },
                            onToggleSolved = { onToggleSolved(problem.id) },
                            isOwner = currentUserId == problem.userId
                        )
                    }
                }
            }
        }
        is ProblemViewModel.ProblemsState.Error -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = myProblemsState.message,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun UserAnswersTab() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("Henüz cevap vermediğiniz bir soru bulunmamaktadır.")
    }
}

@Composable
fun UserFavoritesTab() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("Henüz favori olarak işaretlediğiniz bir yanıt bulunmamaktadır.")
    }
} 