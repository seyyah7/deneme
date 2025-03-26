package com.example.deneme.repository

import com.example.deneme.model.Comment
import com.example.deneme.model.Problem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProblemRepository @Inject constructor() {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val problemsCollection = firestore.collection("problems")
    private val commentsCollection = firestore.collection("comments")

    fun getAllProblems(): Flow<List<Problem>> = flow {
        try {
            val snapshot = problemsCollection
                .whereEqualTo("solved", false)  // Sadece çözülmemiş soruları al
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()
            
            val problems = snapshot.toObjects(Problem::class.java)
            emit(problems)
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    fun getSolvedProblems(): Flow<List<Problem>> = flow {
        try {
            val snapshot = problemsCollection
                .whereEqualTo("solved", true)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()
            
            val problems = snapshot.toObjects(Problem::class.java)
            emit(problems)
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    fun getPopularProblems(): Flow<List<Problem>> = flow {
        try {
            // Önce tüm problemleri alalım
            val snapshot = problemsCollection
                .get()
                .await()
            
            val problems = snapshot.toObjects(Problem::class.java)
            
            // Yorum sayısına göre sıralayacağız
            // Bunun için önce her problem için yorum sayısını bulmalıyız
            val problemsWithCommentCounts = problems.map { problem ->
                val commentCount = commentsCollection
                    .whereEqualTo("problemId", problem.id)
                    .get()
                    .await()
                    .size()
                
                // Problem ve yorum sayısını birlikte saklayacağız
                problem to commentCount
            }
            
            // Yorum sayısına göre sıralayıp sadece problemleri döndürelim
            // En fazla 14 sonuç döndür
            val sortedProblems = problemsWithCommentCounts
                .sortedByDescending { it.second }
                .map { it.first }
                .take(14)
            
            emit(sortedProblems)
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    fun getMyProblems(): Flow<List<Problem>> = flow {
        val currentUser = auth.currentUser ?: return@flow emit(emptyList<Problem>())
        
        try {
            val snapshot = problemsCollection
                .whereEqualTo("userId", currentUser.uid)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()
            
            val problems = snapshot.toObjects(Problem::class.java)
            emit(problems)
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    suspend fun addProblem(title: String, description: String): Result<String> {
        val currentUser = auth.currentUser ?: return Result.failure(IllegalStateException("No authenticated user"))
        
        return try {
            val problemRef = problemsCollection.document()
            
            val problem = Problem(
                id = problemRef.id,
                title = title,
                description = description,
                userId = currentUser.uid,
                userName = currentUser.displayName ?: "Anonim"
            )
            
            problemRef.set(problem).await()
            Result.success(problemRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getProblemById(problemId: String): Flow<Problem?> = flow {
        try {
            val document = problemsCollection.document(problemId).get().await()
            val problem = document.toObject(Problem::class.java)
            emit(problem)
        } catch (e: Exception) {
            emit(null)
        }
    }

    fun getCommentsForProblem(problemId: String): Flow<List<Comment>> = flow {
        try {
            val snapshot = commentsCollection
                .whereEqualTo("problemId", problemId)
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .get()
                .await()
            
            val comments = snapshot.toObjects(Comment::class.java)
            emit(comments)
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    suspend fun addComment(problemId: String, text: String): Result<String> {
        val currentUser = auth.currentUser ?: return Result.failure(IllegalStateException("No authenticated user"))
        
        return try {
            val commentRef = commentsCollection.document()
            
            val comment = Comment(
                id = commentRef.id,
                problemId = problemId,
                text = text,
                userId = currentUser.uid,
                userName = currentUser.displayName ?: "Anonim"
            )
            
            commentRef.set(comment).await()
            Result.success(commentRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun toggleProblemSolvedStatus(problemId: String): Boolean {
        return try {
            // Önce mevcut durumu alalım
            val problemDoc = problemsCollection.document(problemId).get().await()
            val problem = problemDoc.toObject(Problem::class.java) ?: return false
            
            // Mevcut kullanıcının problem sahibi olup olmadığını kontrol edelim
            val currentUser = auth.currentUser ?: return false
            if (problem.userId != currentUser.uid) {
                return false // Sadece problem sahibi durumu değiştirebilir
            }
            
            // Durumu tersine çevirelim
            val newSolvedStatus = !problem.solved
            
            // Firestore'da güncelleme yapalım
            problemsCollection.document(problemId)
                .update("solved", newSolvedStatus)
                .await()
            
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun markCommentAsAccepted(commentId: String): Result<Unit> {
        return try {
            commentsCollection.document(commentId)
                .update("isAcceptedAnswer", true)
                .await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
} 