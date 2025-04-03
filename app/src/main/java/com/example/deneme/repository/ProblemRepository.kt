package com.example.deneme.repository

import android.util.Log
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
            Log.d("ProblemRepository", "getAllProblems çağrıldı, solved=false olanları getiriyorum")
            
            // Sorguda dönüşüm sorunlarını önlemek için 
            val snapshot = problemsCollection
                .get()
                .await()
            
            val allProblems = snapshot.toObjects(Problem::class.java)
            Log.d("ProblemRepository", "getAllProblems: Toplam ${allProblems.size} adet soru var")
            
            // Sorgu sonuçlarından çözülmemiş olanları filtreleyelim
            val unsolvedProblems = allProblems.filter { !it.solved }
            Log.d("ProblemRepository", "getAllProblems: ${unsolvedProblems.size} adet çözülmemiş soru filtrelendi, zaman göre sıralanıyor")
            
            // Filtrelenmiş sonuçları timestamp'e göre sıralayalım
            val sortedProblems = unsolvedProblems
                .sortedByDescending { it.timestamp.seconds }
            
            Log.d("ProblemRepository", "getAllProblems: Filtereleme ve sıralama sonrası ${sortedProblems.size} adet soru")
            for (problem in sortedProblems.take(5)) {
                Log.d("ProblemRepository", "getAllProblems Sonuç: ID=${problem.id}, Başlık=${problem.title}, solved=${problem.solved}, timestamp=${problem.timestamp.seconds}")
            }
            
            emit(sortedProblems)
        } catch (e: Exception) {
            Log.e("ProblemRepository", "getAllProblems HATA: ${e.message}")
            emit(emptyList())
        }
    }

    fun getSolvedProblems(): Flow<List<Problem>> = flow {
        try {
            Log.d("ProblemRepository", "getSolvedProblems çağrıldı")
            
            // Sorguda dönüşüm sorunlarını önlemek için 
            val snapshot = problemsCollection
                .get()
                .await()
            
            val allProblems = snapshot.toObjects(Problem::class.java)
            
            // Sorgu sonuçlarından çözülmüş olanları filtreleyelim
            val solvedProblems = allProblems.filter { it.solved }
            
            // Filtrelenmiş sonuçları timestamp'e göre sıralayalım
            val sortedProblems = solvedProblems
                .sortedByDescending { it.timestamp.seconds }
            
            Log.d("ProblemRepository", "getSolvedProblems: ${sortedProblems.size} adet çözülmüş soru bulundu")
            
            emit(sortedProblems)
        } catch (e: Exception) {
            Log.e("ProblemRepository", "getSolvedProblems HATA: ${e.message}")
            emit(emptyList())
        }
    }

    fun getPopularProblems(): Flow<List<Problem>> = flow {
        try {
            Log.d("ProblemRepository", "getPopularProblems çağrıldı")
            
            // Tüm soruları al
            val snapshot = problemsCollection
                .get()
                .await()
            
            val allProblems = snapshot.toObjects(Problem::class.java)
            Log.d("ProblemRepository", "getPopularProblems: Toplam ${allProblems.size} soru alındı")
            
            // Artık çözülmüş soruları FİLTRELEMİYORUZ, hepsini göster!
            // Yorum sayısına göre sırala
            val popularProblems = allProblems
                .sortedByDescending { it.answerCount }
                .take(14)
            
            Log.d("ProblemRepository", "getPopularProblems: Yorum sayısına göre sıralanmış ${popularProblems.size} soru")
            for (problem in popularProblems.take(5)) {
                Log.d("ProblemRepository", "getPopularProblems Sonuç: ID=${problem.id}, Başlık=${problem.title}, answerCount=${problem.answerCount}")
            }
            
            emit(popularProblems)
        } catch (e: Exception) {
            Log.e("ProblemRepository", "getPopularProblems HATA: ${e.message}")
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
        Log.d("ProblemRepository", "addProblem çağrıldı: başlık=$title")
        val currentUser = auth.currentUser ?: return Result.failure(IllegalStateException("No authenticated user"))
        
        return try {
            val problemRef = problemsCollection.document()
            val problemId = problemRef.id
            
            Log.d("ProblemRepository", "Yeni problemin ID'si: $problemId")
            
            // Şu anki zamanı al
            val now = com.google.firebase.Timestamp.now()
            
            // Problem nesnesini oluştur
            val problem = Problem(
                id = problemId,
                title = title,
                description = description,
                userId = currentUser.uid,
                userName = currentUser.displayName ?: "Anonim",
                timestamp = now,
                solved = false,
                answerCount = 0
            )
            
            // Veriyi gönderiyoruz
            Log.d("ProblemRepository", "Problem eklenecek: ${problem.id}, solved=${problem.solved}, timestamp=${problem.timestamp}")
            problemRef.set(problem).await()
            
            // Ekleme işleminin başarılı olduğunu doğrulayalım
            val addedProblem = problemsCollection.document(problemId).get().await().toObject(Problem::class.java)
            
            if (addedProblem != null) {
                Log.d("ProblemRepository", "Problem başarıyla eklendi: ${addedProblem.id}, solved=${addedProblem.solved}, timestamp=${addedProblem.timestamp}")
                Result.success(problemId)
            } else {
                val errorMsg = "Sorun eklendikten sonra doğrulama başarısız oldu"
                Log.e("ProblemRepository", errorMsg)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e("ProblemRepository", "addProblem hata: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun addProblemWithCategory(title: String, description: String, category: String): Result<String> {
        Log.d("ProblemRepository", "addProblemWithCategory çağrıldı: başlık=$title, kategori=$category")
        val currentUser = auth.currentUser ?: return Result.failure(IllegalStateException("Oturum açmış kullanıcı bulunamadı"))
        
        return try {
            val problemRef = problemsCollection.document()
            val problemId = problemRef.id
            
            Log.d("ProblemRepository", "Yeni problemin ID'si: $problemId")
            
            // Şu anki zamanı al
            val now = com.google.firebase.Timestamp.now()
            
            // Problem nesnesini oluştur
            val problem = Problem(
                id = problemId,
                title = title,
                description = description,
                userId = currentUser.uid,
                userName = currentUser.displayName ?: "Anonim",
                timestamp = now,
                solved = false,
                answerCount = 0,
                category = category
            )
            
            // Veriyi gönderiyoruz
            Log.d("ProblemRepository", "Problem eklenecek: ${problem.id}, kategori=${problem.category}, solved=${problem.solved}")
            problemRef.set(problem).await()
            
            // Ekleme işleminin başarılı olduğunu doğrulayalım
            val addedProblem = problemsCollection.document(problemId).get().await().toObject(Problem::class.java)
            
            if (addedProblem != null) {
                Log.d("ProblemRepository", "Problem başarıyla eklendi: ${addedProblem.id}, kategori=${addedProblem.category}")
                Result.success(problemId)
            } else {
                val errorMsg = "Sorun eklendikten sonra doğrulama başarısız oldu"
                Log.e("ProblemRepository", errorMsg)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e("ProblemRepository", "addProblemWithCategory hata: ${e.message}")
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
            Log.d("ProblemRepository", "getCommentsForProblem başladı, problemId: $problemId")
            
            // Yorumları almaya çalış - problemId'yi direkt string olarak kontrol et
            val snapshot = commentsCollection
                .get()
                .await()
            
            Log.d("ProblemRepository", "Tüm yorumlar alındı, döküman sayısı: ${snapshot.size()}")
            
            // Önce tüm yorumları alıp filtreleme yaparak problemId sorunlarını önleyelim
            val allComments = snapshot.toObjects(Comment::class.java)
            
            // ProblemId'ye göre manuel filtreleme yap
            val filteredComments = allComments.filter { 
                it.problemId == problemId 
            }
            
            Log.d("ProblemRepository", "Manuel filtreleme sonrası bu problemin yorumları: ${filteredComments.size} adet")
            
            // Ham veriyi kontrol et
            filteredComments.forEachIndexed { index, comment ->
                Log.d("ProblemRepository", "Filtrelenmiş Yorum $index: ID=${comment.id}, Text=${comment.text}, " +
                    "ProblemId=${comment.problemId}, UserName=${comment.userName}, timestamp=${comment.timestamp}")
            }
            
            emit(filteredComments.sortedBy { it.timestamp.seconds })
        } catch (e: Exception) {
            Log.e("ProblemRepository", "getCommentsForProblem HATA: ${e.message}", e)
            emit(emptyList())
        }
    }

    suspend fun addComment(problemId: String, text: String): Result<String> {
        val currentUser = auth.currentUser ?: return Result.failure(IllegalStateException("Oturum açmış kullanıcı bulunamadı"))
        
        try {
            Log.d("ProblemRepository", "addComment başladı, problemId: $problemId, text: $text, userId: ${currentUser.uid}")
            
            // Transaction yerine normal işlem yapalım - transaction'da sorun olabilir
            // Önce yeni bir yorum referansı oluştur
            val commentRef = commentsCollection.document()
            val commentId = commentRef.id
            
            // Şu anki zamanı al
            val now = com.google.firebase.Timestamp.now()
            
            // Kullanıcı adını kontrol et
            val userName = currentUser.displayName ?: "Anonim"
            
            // Yorum nesnesini oluştur
            val comment = Comment(
                id = commentId,
                problemId = problemId,  // Burada problemId'nin doğru olduğundan emin ol
                text = text,
                userId = currentUser.uid,
                userName = userName,
                timestamp = now,
                isAcceptedAnswer = false
            )
            
            Log.d("ProblemRepository", "Yeni yorum hazırlandı: ID=$commentId, problemId=$problemId, timestamp=${comment.timestamp}")
            
            // Yorumu kaydet
            commentRef.set(comment).await()
            Log.d("ProblemRepository", "Yorum Firestore'a kaydedildi")
            
            // Problem'in answerCount'unu artır
            val problemDoc = problemsCollection.document(problemId)
            val problemSnapshot = problemDoc.get().await()
            
            if (problemSnapshot.exists()) {
                val problem = problemSnapshot.toObject(Problem::class.java)
                if (problem != null) {
                    val newAnswerCount = problem.answerCount + 1
                    problemDoc.update("answerCount", newAnswerCount).await()
                    Log.d("ProblemRepository", "Problem answerCount güncellendi: $newAnswerCount")
                }
            }
            
            // Eklenen yorumun varlığını doğrulayalım
            val addedComment = commentsCollection.document(commentId).get().await()
            
            return if (addedComment.exists()) {
                val comment = addedComment.toObject(Comment::class.java)
                if (comment != null) {
                    Log.d("ProblemRepository", "Yorum başarıyla eklendi ve doğrulandı: ID=${comment.id}, problemId=${comment.problemId}")
                    Result.success(commentId)
                } else {
                    Log.e("ProblemRepository", "Yorum dokümanı alındı fakat Comment nesnesine dönüştürülemedi")
                    Result.failure(Exception("Yorum verisi dönüştürülemedi"))
                }
            } else {
                Log.e("ProblemRepository", "Yorum dokümanı bulunamadı: $commentId")
                Result.failure(Exception("Yorum ekleme işlemi doğrulanamadı"))
            }
        } catch (e: Exception) {
            Log.e("ProblemRepository", "addComment HATA: ${e.message}", e)
            return Result.failure(e)
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

    // Mevcut tüm soruların answerCount alanını bir kereliğine güncelleyen yardımcı fonksiyon
    suspend fun updateAllProblemAnswerCounts(): Result<Int> {
        return try {
            Log.d("ProblemRepository", "updateAllProblemAnswerCounts başlatılıyor")
            
            // Tüm soruları al
            val problems = problemsCollection.get().await().toObjects(Problem::class.java)
            Log.d("ProblemRepository", "Toplam ${problems.size} soru bulundu")
            
            var updatedCount = 0
            
            // Her soru için
            for (problem in problems) {
                // Eğer answerCount alanı yoksa (0 varsayılan değer olarak ayarlanmışsa)
                // veya answerCount değeri yoksa (null ise)
                val problemDoc = problemsCollection.document(problem.id)
                
                // Yorum sayısını al
                val commentCount = commentsCollection
                    .whereEqualTo("problemId", problem.id)
                    .get()
                    .await()
                    .size()
                
                Log.d("ProblemRepository", "Soru ID: ${problem.id}, Mevcut yorum sayısı: $commentCount")
                
                // answerCount'u güncelle
                problemDoc.update("answerCount", commentCount).await()
                updatedCount++
            }
            
            Log.d("ProblemRepository", "Toplam $updatedCount soru güncellendi")
            Result.success(updatedCount)
        } catch (e: Exception) {
            Log.e("ProblemRepository", "updateAllProblemAnswerCounts hata: ${e.message}")
            Result.failure(e)
        }
    }

    // DEBUG: Tüm soruları doğrudan Firestore'dan alan test fonksiyonu
    suspend fun getAllProblemsDirectly(): List<Problem> {
        try {
            Log.d("ProblemRepository", "getAllProblemsDirectly: Tüm soruları alıyorum")
            val snapshot = problemsCollection
                .get()
                .await()
            
            val problems = snapshot.toObjects(Problem::class.java)
            Log.d("ProblemRepository", "getAllProblemsDirectly: ${problems.size} adet soru bulundu")
            return problems
        } catch (e: Exception) {
            Log.e("ProblemRepository", "getAllProblemsDirectly hata: ${e.message}")
            return emptyList()
        }
    }
    
    // DEBUG: Çözülmemiş soruları doğrudan Firestore'dan alan test fonksiyonu
    suspend fun getUnsolvedProblemsDirectly(): List<Problem> {
        try {
            Log.d("ProblemRepository", "getUnsolvedProblemsDirectly: Çözülmemiş soruları alıyorum")
            val snapshot = problemsCollection
                .whereEqualTo("solved", false)
                .get()
                .await()
            
            val problems = snapshot.toObjects(Problem::class.java)
            Log.d("ProblemRepository", "getUnsolvedProblemsDirectly: ${problems.size} adet soru bulundu")
            return problems
        } catch (e: Exception) {
            Log.e("ProblemRepository", "getUnsolvedProblemsDirectly hata: ${e.message}")
            return emptyList()
        }
    }
    
    // DEBUG: Çözülmüş soruları doğrudan Firestore'dan alan test fonksiyonu
    suspend fun getSolvedProblemsDirectly(): List<Problem> {
        try {
            Log.d("ProblemRepository", "getSolvedProblemsDirectly: Çözülmüş soruları alıyorum")
            val snapshot = problemsCollection
                .whereEqualTo("solved", true)
                .get()
                .await()
            
            val problems = snapshot.toObjects(Problem::class.java)
            Log.d("ProblemRepository", "getSolvedProblemsDirectly: ${problems.size} adet soru bulundu")
            return problems
        } catch (e: Exception) {
            Log.e("ProblemRepository", "getSolvedProblemsDirectly hata: ${e.message}")
            return emptyList()
        }
    }
    
    // DEBUG: Popüler soruları doğrudan Firestore'dan alan test fonksiyonu
    suspend fun getPopularProblemsDirectly(): List<Problem> {
        try {
            Log.d("ProblemRepository", "getPopularProblemsDirectly: Popüler soruları alıyorum")
            val snapshot = problemsCollection
                // Çözülmüş soruları FİLTRELEMİYORUZ artık
                .orderBy("answerCount", Query.Direction.DESCENDING)
                .limit(14)
                .get()
                .await()
            
            val problems = snapshot.toObjects(Problem::class.java)
            Log.d("ProblemRepository", "getPopularProblemsDirectly: ${problems.size} adet soru bulundu")
            return problems
        } catch (e: Exception) {
            Log.e("ProblemRepository", "getPopularProblemsDirectly hata: ${e.message}")
            return emptyList()
        }
    }

    // Kullanıcının bir soruyu silmesini sağlayan fonksiyon
    suspend fun deleteProblem(problemId: String): Result<Unit> {
        val currentUser = auth.currentUser ?: return Result.failure(IllegalStateException("Oturum açmış kullanıcı bulunamadı"))
        
        return try {
            Log.d("ProblemRepository", "deleteProblem başladı, problemId: $problemId")
            
            // İlk olarak sorunun var olduğunu ve kullanıcının sorunun sahibi olduğunu kontrol et
            val problemDoc = problemsCollection.document(problemId).get().await()
            
            if (!problemDoc.exists()) {
                Log.e("ProblemRepository", "Silinecek soru bulunamadı: $problemId")
                return Result.failure(Exception("Silinecek soru bulunamadı"))
            }
            
            val problem = problemDoc.toObject(Problem::class.java)
            if (problem == null) {
                Log.e("ProblemRepository", "Soru verisi dönüştürülemedi: $problemId")
                return Result.failure(Exception("Soru verisi dönüştürülemedi"))
            }
            
            // Kullanıcı sorunun sahibi mi kontrol et
            if (problem.userId != currentUser.uid) {
                Log.e("ProblemRepository", "Yetki hatası: Kullanıcı bu soruyu silme yetkisine sahip değil")
                return Result.failure(Exception("Bu soruyu silme yetkiniz yok"))
            }
            
            // Önce bu soruya ait tüm yorumları sil
            val commentsSnapshot = commentsCollection
                .get()
                .await()
            
            val allComments = commentsSnapshot.toObjects(Comment::class.java)
            val problemComments = allComments.filter { it.problemId == problemId }
            
            for (comment in problemComments) {
                commentsCollection.document(comment.id).delete().await()
                Log.d("ProblemRepository", "Soruya ait yorum silindi: ${comment.id}")
            }
            
            // Sonra soruyu sil
            problemsCollection.document(problemId).delete().await()
            Log.d("ProblemRepository", "Soru başarıyla silindi: $problemId")
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("ProblemRepository", "deleteProblem HATA: ${e.message}", e)
            Result.failure(e)
        }
    }

    // Kullanıcının kendi yorumunu silmesini sağlayan fonksiyon
    suspend fun deleteComment(commentId: String): Result<Unit> {
        val currentUser = auth.currentUser ?: return Result.failure(IllegalStateException("Oturum açmış kullanıcı bulunamadı"))
        
        return try {
            Log.d("ProblemRepository", "deleteComment başladı, commentId: $commentId")
            
            // İlk olarak yorumun var olduğunu ve kullanıcının yorumun sahibi olduğunu kontrol et
            val commentDoc = commentsCollection.document(commentId).get().await()
            
            if (!commentDoc.exists()) {
                Log.e("ProblemRepository", "Silinecek yorum bulunamadı: $commentId")
                return Result.failure(Exception("Silinecek yorum bulunamadı"))
            }
            
            val comment = commentDoc.toObject(Comment::class.java)
            if (comment == null) {
                Log.e("ProblemRepository", "Yorum verisi dönüştürülemedi: $commentId")
                return Result.failure(Exception("Yorum verisi dönüştürülemedi"))
            }
            
            // Kullanıcı yorumun sahibi mi kontrol et
            if (comment.userId != currentUser.uid) {
                Log.e("ProblemRepository", "Yetki hatası: Kullanıcı bu yorumu silme yetkisine sahip değil")
                return Result.failure(Exception("Bu yorumu silme yetkiniz yok"))
            }
            
            // Sorunun bulunması ve answerCount'ın azaltılması
            val problemId = comment.problemId
            val problemDoc = problemsCollection.document(problemId).get().await()
            
            if (problemDoc.exists()) {
                val problem = problemDoc.toObject(Problem::class.java)
                if (problem != null && problem.answerCount > 0) {
                    // AnswerCount'u azalt
                    problemsCollection.document(problemId)
                        .update("answerCount", problem.answerCount - 1)
                        .await()
                    Log.d("ProblemRepository", "Sorunun answerCount'ı azaltıldı: ${problem.answerCount - 1}")
                }
            }
            
            // Yorumu sil
            commentsCollection.document(commentId).delete().await()
            Log.d("ProblemRepository", "Yorum başarıyla silindi: $commentId")
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("ProblemRepository", "deleteComment HATA: ${e.message}", e)
            Result.failure(e)
        }
    }

    // Kullanıcının soru ve cevaplarını getiren fonksiyonlar
    fun getUserProblems(): Flow<List<Problem>> = flow {
        val currentUser = auth.currentUser ?: return@flow emit(emptyList<Problem>())
        
        try {
            Log.d("ProblemRepository", "getUserProblems çağrıldı, userId: ${currentUser.uid}")
            
            // Tüm soruları al
            val snapshot = problemsCollection
                .get()
                .await()
            
            val allProblems = snapshot.toObjects(Problem::class.java)
            
            // Kullanıcının sorularını filtrele
            val userProblems = allProblems.filter { it.userId == currentUser.uid }
            
            // Soruları timestamp'e göre sırala (en yeni en üstte)
            val sortedProblems = userProblems.sortedByDescending { it.timestamp.seconds }
            
            Log.d("ProblemRepository", "getUserProblems: ${sortedProblems.size} adet kullanıcı sorusu bulundu")
            
            emit(sortedProblems)
        } catch (e: Exception) {
            Log.e("ProblemRepository", "getUserProblems HATA: ${e.message}")
            emit(emptyList())
        }
    }

    // Belirli bir kullanıcının sorularını getiren fonksiyon
    fun getUserProblemsById(userId: String): Flow<List<Problem>> = flow {
        try {
            Log.d("ProblemRepository", "getUserProblemsById çağrıldı, userId: $userId")
            
            // Tüm soruları al
            val snapshot = problemsCollection
                .get()
                .await()
            
            val allProblems = snapshot.toObjects(Problem::class.java)
            
            // Belirtilen kullanıcının sorularını filtrele
            val userProblems = allProblems.filter { it.userId == userId }
            
            // Soruları timestamp'e göre sırala (en yeni en üstte)
            val sortedProblems = userProblems.sortedByDescending { it.timestamp.seconds }
            
            Log.d("ProblemRepository", "getUserProblemsById: ${sortedProblems.size} adet kullanıcı sorusu bulundu")
            
            emit(sortedProblems)
        } catch (e: Exception) {
            Log.e("ProblemRepository", "getUserProblemsById HATA: ${e.message}")
            emit(emptyList())
        }
    }

    fun getUserComments(): Flow<List<CommentWithProblemTitle>> = flow {
        val currentUser = auth.currentUser ?: return@flow emit(emptyList<CommentWithProblemTitle>())
        
        try {
            Log.d("ProblemRepository", "getUserComments çağrıldı, userId: ${currentUser.uid}")
            
            // Tüm yorumları al
            val commentsSnapshot = commentsCollection
                .get()
                .await()
            
            val allComments = commentsSnapshot.toObjects(Comment::class.java)
            
            // Kullanıcının yorumlarını filtrele
            val userComments = allComments.filter { it.userId == currentUser.uid }
            
            // Tüm soruları al
            val problemsSnapshot = problemsCollection
                .get()
                .await()
            
            val allProblems = problemsSnapshot.toObjects(Problem::class.java)
            val problemsMap = allProblems.associateBy { it.id }
            
            // Yorum ve soru başlıklarını birleştir
            val commentsWithProblemTitles = userComments.mapNotNull { comment ->
                val problem = problemsMap[comment.problemId]
                if (problem != null) {
                    CommentWithProblemTitle(
                        comment = comment,
                        problemTitle = problem.title,
                        problemId = problem.id
                    )
                } else null
            }
            
            // Yorumları timestamp'e göre sırala (en yeni en üstte)
            val sortedComments = commentsWithProblemTitles.sortedByDescending { it.comment.timestamp.seconds }
            
            Log.d("ProblemRepository", "getUserComments: ${sortedComments.size} adet kullanıcı yorumu bulundu")
            
            emit(sortedComments)
        } catch (e: Exception) {
            Log.e("ProblemRepository", "getUserComments HATA: ${e.message}")
            emit(emptyList())
        }
    }

    // Belirli bir kullanıcının yorumlarını getiren fonksiyon
    fun getUserCommentsById(userId: String): Flow<List<CommentWithProblemTitle>> = flow {
        try {
            Log.d("ProblemRepository", "getUserCommentsById çağrıldı, userId: $userId")
            
            // Tüm yorumları al
            val commentsSnapshot = commentsCollection
                .get()
                .await()
            
            val allComments = commentsSnapshot.toObjects(Comment::class.java)
            
            // Belirtilen kullanıcının yorumlarını filtrele
            val userComments = allComments.filter { it.userId == userId }
            
            // Tüm soruları al
            val problemsSnapshot = problemsCollection
                .get()
                .await()
            
            val allProblems = problemsSnapshot.toObjects(Problem::class.java)
            val problemsMap = allProblems.associateBy { it.id }
            
            // Yorum ve soru başlıklarını birleştir
            val commentsWithProblemTitles = userComments.mapNotNull { comment ->
                val problem = problemsMap[comment.problemId]
                if (problem != null) {
                    CommentWithProblemTitle(
                        comment = comment,
                        problemTitle = problem.title,
                        problemId = problem.id
                    )
                } else null
            }
            
            // Yorumları timestamp'e göre sırala (en yeni en üstte)
            val sortedComments = commentsWithProblemTitles.sortedByDescending { it.comment.timestamp.seconds }
            
            Log.d("ProblemRepository", "getUserCommentsById: ${sortedComments.size} adet kullanıcı yorumu bulundu")
            
            emit(sortedComments)
        } catch (e: Exception) {
            Log.e("ProblemRepository", "getUserCommentsById HATA: ${e.message}")
            emit(emptyList())
        }
    }

    // Yorum ve soru başlıklarını birleştirmek için yardımcı veri sınıfı
    data class CommentWithProblemTitle(
        val comment: Comment,
        val problemTitle: String,
        val problemId: String
    )
} 