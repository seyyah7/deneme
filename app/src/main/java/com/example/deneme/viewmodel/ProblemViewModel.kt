package com.example.deneme.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.deneme.model.Comment
import com.example.deneme.model.Problem
import com.example.deneme.repository.ProblemRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProblemViewModel @Inject constructor(
    private val problemRepository: ProblemRepository
) : ViewModel() {

    private val _problemsState = MutableStateFlow<ProblemsState>(ProblemsState.Loading)
    val problemsState: StateFlow<ProblemsState> = _problemsState

    private val _solvedProblemsState = MutableStateFlow<ProblemsState>(ProblemsState.Loading)
    val solvedProblemsState: StateFlow<ProblemsState> = _solvedProblemsState

    private val _popularProblemsState = MutableStateFlow<ProblemsState>(ProblemsState.Loading)
    val popularProblemsState: StateFlow<ProblemsState> = _popularProblemsState

    private val _myProblemsState = MutableStateFlow<ProblemsState>(ProblemsState.Loading)
    val myProblemsState: StateFlow<ProblemsState> = _myProblemsState

    private val _problemDetailState = MutableStateFlow<ProblemDetailState>(ProblemDetailState.Loading)
    val problemDetailState: StateFlow<ProblemDetailState> = _problemDetailState

    private val _commentsState = MutableStateFlow<CommentsState>(CommentsState.Loading)
    val commentsState: StateFlow<CommentsState> = _commentsState
    
    private val _operation = MutableSharedFlow<OperationState>()
    val operation: SharedFlow<OperationState> = _operation

    fun loadProblems() {
        _problemsState.value = ProblemsState.Loading
        viewModelScope.launch {
            problemRepository.getAllProblems().collect { problems ->
                _problemsState.value = ProblemsState.Success(problems)
            }
        }
    }

    fun loadSolvedProblems() {
        _solvedProblemsState.value = ProblemsState.Loading
        viewModelScope.launch {
            problemRepository.getSolvedProblems().collect { problems ->
                _solvedProblemsState.value = ProblemsState.Success(problems)
            }
        }
    }

    fun loadPopularProblems() {
        _popularProblemsState.value = ProblemsState.Loading
        viewModelScope.launch {
            problemRepository.getPopularProblems().collect { problems ->
                _popularProblemsState.value = ProblemsState.Success(problems)
            }
        }
    }

    fun loadMyProblems() {
        _myProblemsState.value = ProblemsState.Loading
        viewModelScope.launch {
            problemRepository.getMyProblems().collect { problems ->
                _myProblemsState.value = ProblemsState.Success(problems)
            }
        }
    }

    fun loadProblem(problemId: String) {
        _problemDetailState.value = ProblemDetailState.Loading
        viewModelScope.launch {
            problemRepository.getProblemById(problemId).collect { problem ->
                if (problem != null) {
                    _problemDetailState.value = ProblemDetailState.Success(problem)
                } else {
                    _problemDetailState.value = ProblemDetailState.Error("Problem bulunamadı")
                }
            }
        }
    }

    fun loadComments(problemId: String) {
        _commentsState.value = CommentsState.Loading
        viewModelScope.launch {
            problemRepository.getCommentsForProblem(problemId).collect { comments ->
                _commentsState.value = CommentsState.Success(comments)
            }
        }
    }

    fun addProblem(title: String, description: String) {
        viewModelScope.launch {
            val result = problemRepository.addProblem(title, description)
            result.fold(
                onSuccess = { problemId ->
                    _operation.emit(OperationState.Success("Problem başarıyla eklendi"))
                    // Listeleri güncelle
                    loadProblems()
                    loadSolvedProblems()
                    loadPopularProblems()
                    loadMyProblems()
                },
                onFailure = { error ->
                    _operation.emit(OperationState.Error(error.message ?: "Problem eklenirken bir hata oluştu"))
                }
            )
        }
    }

    fun addComment(problemId: String, text: String) {
        viewModelScope.launch {
            val result = problemRepository.addComment(problemId, text)
            result.fold(
                onSuccess = { commentId ->
                    _operation.emit(OperationState.Success("Yorum başarıyla eklendi"))
                    loadComments(problemId)
                },
                onFailure = { error ->
                    _operation.emit(OperationState.Error(error.message ?: "Yorum eklenirken bir hata oluştu"))
                }
            )
        }
    }

    fun toggleProblemSolvedStatus(problemId: String) {
        viewModelScope.launch {
            try {
                _operation.emit(OperationState.Loading)
                val result = problemRepository.toggleProblemSolvedStatus(problemId)
                if (result) {
                    _operation.emit(OperationState.Success("Sorun durumu güncellendi"))
                    // Listeleri güncelle
                    loadProblems()
                    loadSolvedProblems()
                    loadPopularProblems()
                    loadMyProblems()
                } else {
                    _operation.emit(OperationState.Error("Durum güncellenirken bir hata oluştu"))
                }
            } catch (e: Exception) {
                _operation.emit(OperationState.Error(e.message ?: "Beklenmeyen bir hata oluştu"))
            }
        }
    }

    fun markCommentAsAccepted(commentId: String, problemId: String) {
        viewModelScope.launch {
            val result = problemRepository.markCommentAsAccepted(commentId)
            result.fold(
                onSuccess = {
                    _operation.emit(OperationState.Success("Cevap kabul edildi"))
                    loadComments(problemId)
                },
                onFailure = { error ->
                    _operation.emit(OperationState.Error(error.message ?: "Yorum güncellenirken bir hata oluştu"))
                }
            )
        }
    }

    sealed class ProblemsState {
        object Loading : ProblemsState()
        data class Success(val problems: List<Problem>) : ProblemsState()
        data class Error(val message: String) : ProblemsState()
    }

    sealed class ProblemDetailState {
        object Loading : ProblemDetailState()
        data class Success(val problem: Problem) : ProblemDetailState()
        data class Error(val message: String) : ProblemDetailState()
    }

    sealed class CommentsState {
        object Loading : CommentsState()
        data class Success(val comments: List<Comment>) : CommentsState()
        data class Error(val message: String) : CommentsState()
    }

    sealed class OperationState {
        object Loading : OperationState()
        data class Success(val message: String) : OperationState()
        data class Error(val message: String) : OperationState()
    }
} 