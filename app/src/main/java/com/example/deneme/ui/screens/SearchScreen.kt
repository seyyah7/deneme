package com.example.deneme.ui.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.deneme.model.Problem
import com.example.deneme.model.ProblemCategory
import com.example.deneme.viewmodel.ProblemViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onNavigateToProblemDetail: (String) -> Unit,
    viewModel: ProblemViewModel = hiltViewModel()
) {
    val allProblemsState by viewModel.problemsState.collectAsState()
    val solvedProblemsState by viewModel.solvedProblemsState.collectAsState()
    val searchQuery = remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    var selectedCategory by remember { mutableStateOf<ProblemCategory?>(null) }
    var showCategoryGrid by remember { mutableStateOf(true) }
    var searchResults by remember { mutableStateOf<List<Problem>>(emptyList()) }
    
    // Tüm soruları kategoriye ve arama sorgusuna göre filtrele
    fun filterProblems() {
        val allProblems = when (allProblemsState) {
            is ProblemViewModel.ProblemsState.Success -> (allProblemsState as ProblemViewModel.ProblemsState.Success).problems
            else -> emptyList()
        }
        
        val solvedProblems = when (solvedProblemsState) {
            is ProblemViewModel.ProblemsState.Success -> (solvedProblemsState as ProblemViewModel.ProblemsState.Success).problems
            else -> emptyList()
        }
        
        // Tüm soruları birleştir
        val combinedProblems = allProblems + solvedProblems
        
        searchResults = combinedProblems.filter { problem ->
            val matchesCategory = selectedCategory?.let { problem.category == it.name } ?: true
            val matchesQuery = if (searchQuery.value.isNotBlank()) {
                problem.title.contains(searchQuery.value, ignoreCase = true) || 
                problem.description.contains(searchQuery.value, ignoreCase = true)
            } else true
            
            matchesCategory && matchesQuery
        }
        
        Log.d("SearchScreen", "Filtreleme sonucu: ${searchResults.size} sonuç bulundu")
        
        // Kategori seçildiğinde ve/veya arama yapıldığında grid'i gizle
        showCategoryGrid = searchQuery.value.isBlank() && selectedCategory == null
    }
    
    LaunchedEffect(Unit) {
        // Tüm soruları yükle
        viewModel.loadProblems()
        viewModel.loadSolvedProblems()
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Arama Çubuğu
        OutlinedTextField(
            value = searchQuery.value,
            onValueChange = { 
                searchQuery.value = it
                filterProblems()
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            placeholder = { Text("Soru veya cevap ara...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Ara") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = {
                    filterProblems()
                    focusManager.clearFocus()
                }
            )
        )
        
        // Seçilen kategori gösterimi ve temizleme
        if (selectedCategory != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Kategori: ${selectedCategory!!.displayName}",
                    modifier = Modifier.weight(1f)
                )
                
                TextButton(onClick = { 
                    selectedCategory = null
                    filterProblems()
                }) {
                    Text("Temizle")
                }
            }
        }
        
        // Kategori Izgara Görünümü veya Arama Sonuçları
        if (showCategoryGrid) {
            Text(
                text = "Kategoriler",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(ProblemCategory.values()) { category ->
                    CategoryCard(
                        category = category,
                        onClick = {
                            selectedCategory = category
                            filterProblems()
                        }
                    )
                }
            }
        } else {
            // Arama Sonuçları
            Text(
                text = "Sonuçlar (${searchResults.size})",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            
            if (searchResults.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Sonuç bulunamadı")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(searchResults) { problem ->
                        SearchResultItem(
                            problem = problem,
                            onClick = { onNavigateToProblemDetail(problem.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryCard(
    category: ProblemCategory,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.5f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = category.displayName,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun SearchResultItem(
    problem: Problem,
    onClick: () -> Unit
) {
    // Kategori adını display name'e çevir
    val categoryDisplayName = try {
        ProblemCategory.valueOf(problem.category).displayName
    } catch (e: Exception) {
        "Genel"
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
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
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Kategori ve Durum
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Kategori etiketi
                Surface(
                    modifier = Modifier.padding(4.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = categoryDisplayName,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                
                // Çözüldü etiketi
                if (problem.solved) {
                    Surface(
                        modifier = Modifier.padding(4.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer
                    ) {
                        Text(
                            text = "Çözüldü",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
} 