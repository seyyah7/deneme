package com.example.deneme.viewmodel

import android.util.Log
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

    // Kullanıcının soruları ve cevapları için state sınıfları
    private val _userProblemsState = MutableStateFlow<UserProblemsState>(UserProblemsState.Loading)
    val userProblemsState: StateFlow<UserProblemsState> = _userProblemsState

    private val _userCommentsState = MutableStateFlow<UserCommentsState>(UserCommentsState.Loading)
    val userCommentsState: StateFlow<UserCommentsState> = _userCommentsState

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
        Log.d("ProblemViewModel", "loadComments çağrıldı: problemId=$problemId")
        _commentsState.value = CommentsState.Loading
        viewModelScope.launch {
            try {
                // Önce mevcut yorumları gözlemleyelim
                problemRepository.getCommentsForProblem(problemId).collect { comments ->
                    Log.d("ProblemViewModel", "Yorumlar başarıyla alındı: ${comments.size} adet")
                    if (comments.isEmpty()) {
                        Log.d("ProblemViewModel", "Bu soruya hiç yorum eklenmemiş")
                    } else {
                        comments.forEachIndexed { index, comment ->
                            Log.d("ProblemViewModel", "Yorum $index - ID: ${comment.id}, Text: ${comment.text}, ProblemId: ${comment.problemId}")
                        }
                    }
                    _commentsState.value = CommentsState.Success(comments)
                }
            } catch (e: Exception) {
                Log.e("ProblemViewModel", "Yorumları yükleme hatası: ${e.message}", e)
                _commentsState.value = CommentsState.Error("Yorumlar yüklenirken bir hata oluştu: ${e.message}")
            }
        }
    }

    fun addProblem(title: String, description: String) {
        viewModelScope.launch {
            val result = problemRepository.addProblem(title, description)
            result.fold(
                onSuccess = { problemId ->
                    Log.d("ProblemViewModel", "Yeni soru başarıyla eklendi, ID: $problemId, listeleri yeniliyorum")
                    
                    loadProblems()
                    
                    loadSolvedProblems()
                    
                    loadPopularProblems()
                    
                    loadMyProblems()
                    
                    _operation.emit(OperationState.Success("Problem başarıyla eklendi"))
                },
                onFailure = { error ->
                    Log.e("ProblemViewModel", "Soru eklenirken hata: ${error.message}")
                    _operation.emit(OperationState.Error(error.message ?: "Problem eklenirken bir hata oluştu"))
                }
            )
        }
    }

    fun addProblemWithCategory(title: String, description: String, category: String) {
        viewModelScope.launch {
            val result = problemRepository.addProblemWithCategory(title, description, category)
            result.fold(
                onSuccess = { problemId ->
                    Log.d("ProblemViewModel", "Yeni soru başarıyla eklendi, ID: $problemId, kategori: $category")
                    
                    // Tüm listeleri yenile
                    loadProblems()
                    loadSolvedProblems()
                    loadPopularProblems()
                    loadMyProblems()
                    
                    _operation.emit(OperationState.Success("Problem başarıyla eklendi"))
                },
                onFailure = { error ->
                    Log.e("ProblemViewModel", "Soru eklenirken hata: ${error.message}")
                    _operation.emit(OperationState.Error(error.message ?: "Problem eklenirken bir hata oluştu"))
                }
            )
        }
    }

    fun addComment(problemId: String, text: String) {
        viewModelScope.launch {
            Log.d("ProblemViewModel", "addComment çağrıldı: problemId=$problemId, text=$text")
            _operation.emit(OperationState.Loading)
            
            val result = problemRepository.addComment(problemId, text)
            result.fold(
                onSuccess = { commentId ->
                    Log.d("ProblemViewModel", "Yorum başarıyla eklendi, commentId: $commentId, listeleri yeniliyorum")
                    _operation.emit(OperationState.Success("Yorum başarıyla eklendi"))
                    
                    // 1 saniye bekle ve sonra yorumları yeniden yükle
                    kotlinx.coroutines.delay(1000)
                    
                    // Yorumları yenile
                    loadComments(problemId)
                    
                    // Yorum sayısı değiştiği için problem detayını yenile
                    loadProblem(problemId)
                    
                    // Popüler soruları yenile (yorum sayısı değiştiğinden sıralama etkilenir)
                    loadPopularProblems()
                },
                onFailure = { error ->
                    Log.e("ProblemViewModel", "Yorum ekleme hatası: ${error.message}")
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
            Log.d("ProblemViewModel", "markCommentAsAccepted çağrıldı: commentId=$commentId, problemId=$problemId")
            _operation.emit(OperationState.Loading)
            val result = problemRepository.markCommentAsAccepted(commentId)
            result.fold(
                onSuccess = {
                    Log.d("ProblemViewModel", "Yorum başarıyla kabul edildi")
                    _operation.emit(OperationState.Success("Cevap kabul edildi"))
                    
                    // Yorumları yenile
                    loadComments(problemId)
                    
                    // Popüler soruları da yenileyelim
                    loadPopularProblems()
                },
                onFailure = { error ->
                    Log.e("ProblemViewModel", "Yorum kabul etme hatası: ${error.message}")
                    _operation.emit(OperationState.Error(error.message ?: "Yorum güncellenirken bir hata oluştu"))
                }
            )
        }
    }

    // Kullanıcının kendi sorularını getiren yardımcı fonksiyon
    fun loadUserProblems() {
        _userProblemsState.value = UserProblemsState.Loading
        viewModelScope.launch {
            try {
                problemRepository.getUserProblems().collect { problems ->
                    _userProblemsState.value = UserProblemsState.Success(problems)
                }
            } catch (e: Exception) {
                _userProblemsState.value = UserProblemsState.Error(e.message ?: "Bir hata oluştu")
            }
        }
    }

    // Kullanıcının kendi cevaplarını yükle
    fun loadUserComments() {
        _userCommentsState.value = UserCommentsState.Loading
        viewModelScope.launch {
            try {
                problemRepository.getUserComments().collect { comments ->
                    _userCommentsState.value = UserCommentsState.Success(comments)
                }
            } catch (e: Exception) {
                _userCommentsState.value = UserCommentsState.Error(e.message ?: "Bir hata oluştu")
            }
        }
    }

    // Belirli bir kullanıcının sorularını yükleme
    fun loadUserProblemsById(userId: String) {
        _userProblemsState.value = UserProblemsState.Loading
        viewModelScope.launch {
            try {
                problemRepository.getUserProblemsById(userId).collect { problems ->
                    _userProblemsState.value = UserProblemsState.Success(problems)
                }
            } catch (e: Exception) {
                _userProblemsState.value = UserProblemsState.Error(e.message ?: "Bir hata oluştu")
            }
        }
    }

    // Belirli bir kullanıcının yorumlarını yükleme
    fun loadUserCommentsById(userId: String) {
        _userCommentsState.value = UserCommentsState.Loading
        viewModelScope.launch {
            try {
                problemRepository.getUserCommentsById(userId).collect { comments ->
                    _userCommentsState.value = UserCommentsState.Success(comments)
                }
            } catch (e: Exception) {
                _userCommentsState.value = UserCommentsState.Error(e.message ?: "Bir hata oluştu")
            }
        }
    }

    // Soru silme işlemi
    fun deleteProblem(problemId: String) {
        viewModelScope.launch {
            _operation.emit(OperationState.Loading)
            
            val result = problemRepository.deleteProblem(problemId)
            result.fold(
                onSuccess = {
                    _operation.emit(OperationState.Success("Soru başarıyla silindi"))
                    
                    // Tüm listeleri yeniden yükle
                    loadProblems()
                    loadSolvedProblems()
                    loadPopularProblems()
                    loadMyProblems()
                    loadUserProblems()
                },
                onFailure = { error ->
                    _operation.emit(OperationState.Error(error.message ?: "Soru silinirken bir hata oluştu"))
                }
            )
        }
    }

    // Yorum silme işlemi
    fun deleteComment(commentId: String, problemId: String) {
        viewModelScope.launch {
            _operation.emit(OperationState.Loading)
            
            val result = problemRepository.deleteComment(commentId)
            result.fold(
                onSuccess = {
                    _operation.emit(OperationState.Success("Yorum başarıyla silindi"))
                    
                    // İlgili listeleri yenile
                    loadComments(problemId)
                    loadPopularProblems()
                    loadUserComments()
                },
                onFailure = { error ->
                    _operation.emit(OperationState.Error(error.message ?: "Yorum silinirken bir hata oluştu"))
                }
            )
        }
    }

    // DEBUG: Firestore sorun teşhisi için kullanılacak yardımcı fonksiyon
    fun debugFirestoreDiagnostics() {
        viewModelScope.launch {
            Log.d("FirestoreDiag", "========== FIRESTORE SORUN TEŞHİSİ BAŞLIYOR ==========")
            
            // 1. Doğrudan Firestore'dan tüm soruları al (filtre olmadan)
            try {
                val allProblems = problemRepository.getAllProblemsDirectly()
                Log.d("FirestoreDiag", "1. Tüm sorular (filtresiz): ${allProblems.size} adet")
                allProblems.forEachIndexed { index, problem ->
                    Log.d("FirestoreDiag", "   $index. ID: ${problem.id}, Title: ${problem.title}, Solved: ${problem.solved}, AnswerCount: ${problem.answerCount}")
                }
            } catch (e: Exception) {
                Log.e("FirestoreDiag", "Tüm soruları alma hatası: ${e.message}")
            }
            
            // 2. Çözülmemiş soruları al
            try {
                val unsolvedProblems = problemRepository.getUnsolvedProblemsDirectly()
                Log.d("FirestoreDiag", "2. Çözülmemiş sorular (solved=false): ${unsolvedProblems.size} adet")
                unsolvedProblems.forEachIndexed { index, problem ->
                    Log.d("FirestoreDiag", "   $index. ID: ${problem.id}, Title: ${problem.title}, Solved: ${problem.solved}, AnswerCount: ${problem.answerCount}")
                }
            } catch (e: Exception) {
                Log.e("FirestoreDiag", "Çözülmemiş soruları alma hatası: ${e.message}")
            }
            
            // 3. Çözülmüş soruları al
            try {
                val solvedProblems = problemRepository.getSolvedProblemsDirectly()
                Log.d("FirestoreDiag", "3. Çözülmüş sorular (solved=true): ${solvedProblems.size} adet")
                solvedProblems.forEachIndexed { index, problem ->
                    Log.d("FirestoreDiag", "   $index. ID: ${problem.id}, Title: ${problem.title}, Solved: ${problem.solved}, AnswerCount: ${problem.answerCount}")
                }
            } catch (e: Exception) {
                Log.e("FirestoreDiag", "Çözülmüş soruları alma hatası: ${e.message}")
            }
            
            // 4. Popüler soruları al
            try {
                val popularProblems = problemRepository.getPopularProblemsDirectly()
                Log.d("FirestoreDiag", "4. Popüler sorular (answerCount DESC): ${popularProblems.size} adet")
                popularProblems.forEachIndexed { index, problem ->
                    Log.d("FirestoreDiag", "   $index. ID: ${problem.id}, Title: ${problem.title}, Solved: ${problem.solved}, AnswerCount: ${problem.answerCount}")
                }
            } catch (e: Exception) {
                Log.e("FirestoreDiag", "Popüler soruları alma hatası: ${e.message}")
            }
            
            Log.d("FirestoreDiag", "========== FIRESTORE SORUN TEŞHİSİ TAMAMLANDI ==========")
            
            // Normal veri yükleme işlemlerini başlatalım
            loadProblems()
            loadSolvedProblems()
            loadPopularProblems()
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

    // Kullanıcı soruları için state
    sealed class UserProblemsState {
        object Loading : UserProblemsState()
        data class Success(val problems: List<Problem>) : UserProblemsState()
        data class Error(val message: String) : UserProblemsState()
    }

    // Kullanıcı yorumları için state
    sealed class UserCommentsState {
        object Loading : UserCommentsState()
        data class Success(val comments: List<ProblemRepository.CommentWithProblemTitle>) : UserCommentsState()
        data class Error(val message: String) : UserCommentsState()
    }
} 