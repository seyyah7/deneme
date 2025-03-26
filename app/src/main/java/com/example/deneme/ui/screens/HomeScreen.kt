package com.example.deneme.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.deneme.model.Problem
import com.example.deneme.viewmodel.ProblemViewModel
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.clickable
import androidx.compose.material3.HorizontalDivider
import com.example.deneme.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToProblemDetail: (String) -> Unit,
    viewModel: ProblemViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val problemsState by viewModel.problemsState.collectAsState()
    val solvedProblemsState by viewModel.solvedProblemsState.collectAsState()
    val popularProblemsState by viewModel.popularProblemsState.collectAsState()
    
    // Tab için state ve callback'ler
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Sorular", "Çözülenler", "Popüler")
    
    // Tab değiştiğinde ilgili verileri yükle
    LaunchedEffect(selectedTabIndex) {
        when (selectedTabIndex) {
            0 -> viewModel.loadProblems()
            1 -> viewModel.loadSolvedProblems()
            2 -> viewModel.loadPopularProblems()
        }
    }
    
    Column(modifier = Modifier.fillMaxSize()) {
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
        
        // Tab İçerikleri
        when (selectedTabIndex) {
            0 -> ProblemsList(
                problemsState = problemsState,
                onProblemClick = onNavigateToProblemDetail,
                onToggleSolved = { problemId -> 
                    viewModel.toggleProblemSolvedStatus(problemId)
                },
                authViewModel = authViewModel
            )
            1 -> ProblemsList(
                problemsState = solvedProblemsState,
                onProblemClick = onNavigateToProblemDetail,
                onToggleSolved = { problemId -> 
                    viewModel.toggleProblemSolvedStatus(problemId)
                },
                authViewModel = authViewModel
            )
            2 -> ProblemsList(
                problemsState = popularProblemsState,
                onProblemClick = onNavigateToProblemDetail,
                onToggleSolved = { problemId -> 
                    viewModel.toggleProblemSolvedStatus(problemId)
                },
                authViewModel = authViewModel
            )
        }
    }
}

@Composable
fun ProblemsList(
    problemsState: ProblemViewModel.ProblemsState,
    onProblemClick: (String) -> Unit,
    onToggleSolved: (String) -> Unit,
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    
    when (problemsState) {
        is ProblemViewModel.ProblemsState.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        is ProblemViewModel.ProblemsState.Success -> {
            if (problemsState.problems.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Bu kategoride soru bulunamadı")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(problemsState.problems) { problem ->
                        ExpandableProblemItem(
                            problem = problem,
                            onProblemClick = { onProblemClick(problem.id) },
                            onToggleSolved = { onToggleSolved(problem.id) },
                            isOwner = currentUser?.id == problem.userId
                        )
                    }
                }
            }
        }
        is ProblemViewModel.ProblemsState.Error -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Hata: ${problemsState.message}")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpandableProblemItem(
    problem: Problem,
    onProblemClick: () -> Unit,
    onToggleSolved: () -> Unit,
    isOwner: Boolean = false
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onProblemClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Sadece başlık göster
            Text(
                text = problem.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
} 