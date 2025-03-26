package com.example.deneme.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.deneme.viewmodel.ProblemViewModel
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProblemScreen(
    onNavigateBack: () -> Unit,
    viewModel: ProblemViewModel = hiltViewModel()
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var snackbarHostState = remember { SnackbarHostState() }
    
    LaunchedEffect(Unit) {
        viewModel.operation.collectLatest { state ->
            when (state) {
                is ProblemViewModel.OperationState.Loading -> {
                    isLoading = true
                }
                is ProblemViewModel.OperationState.Success -> {
                    snackbarHostState.showSnackbar(state.message)
                    onNavigateBack()
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
                title = { Text("Yeni Soru Ekle") },
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
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Başlık") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth()
            )
            
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Açıklama") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                singleLine = false
            )
            
            Button(
                onClick = {
                    isLoading = true
                    viewModel.addProblem(title, description)
                },
                modifier = Modifier.align(Alignment.End),
                enabled = !isLoading && title.isNotBlank() && description.isNotBlank()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Soru Ekle")
                }
            }
        }
    }
} 