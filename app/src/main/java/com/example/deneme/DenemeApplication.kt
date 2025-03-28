package com.example.deneme

import android.app.Application
import android.util.Log
import com.example.deneme.model.Problem
import com.example.deneme.repository.ProblemRepository
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltAndroidApp
class DenemeApplication : Application() {
    
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    @Inject
    lateinit var problemRepository: ProblemRepository
    
    override fun onCreate() {
        super.onCreate()
        
        // Firebase'i başlat
        FirebaseApp.initializeApp(this)
        
        // Firestore ayarlarını yapılandır
        val settings = FirebaseFirestoreSettings.Builder()
            .build()
        FirebaseFirestore.getInstance().firestoreSettings = settings
        
        // Firebase veritabanını optimize etme ve doğrulama
        verifyAndFixFirestoreData()
    }
    
    private fun verifyAndFixFirestoreData() {
        applicationScope.launch {
            Log.d("DenemeApplication", "====== FIREBASE VERİTABANI DOĞRULAMALARI BAŞLIYOR ======")
            
            try {
                // 1. Tüm soruları doğrulamak için al
                val db = FirebaseFirestore.getInstance()
                val problemsCollection = db.collection("problems")
                
                val snapshot = problemsCollection.get().await()
                val allProblems = snapshot.documents
                
                Log.d("DenemeApplication", "Toplam ${allProblems.size} adet soru bulundu")
                
                var updatedCount = 0
                var noChangeCount = 0
                var errorCount = 0
                
                // Her soru için alanları kontrol et ve düzelt
                for (doc in allProblems) {
                    try {
                        val problemData = doc.data ?: continue
                        val needsUpdate = mutableMapOf<String, Any>()
                        
                        // solved alanını kontrol et
                        if (!problemData.containsKey("solved")) {
                            Log.d("DenemeApplication", "Problem ${doc.id}: 'solved' alanı eksik, ekleniyor...")
                            needsUpdate["solved"] = false
                        }
                        
                        // answerCount alanını kontrol et
                        if (!problemData.containsKey("answerCount")) {
                            Log.d("DenemeApplication", "Problem ${doc.id}: 'answerCount' alanı eksik, ekleniyor...")
                            // Yorum sayısını hesapla
                            val commentCount = db.collection("comments")
                                .whereEqualTo("problemId", doc.id)
                                .get()
                                .await()
                                .size()
                            
                            needsUpdate["answerCount"] = commentCount
                        }
                        
                        // Timestamp alanını kontrol et
                        if (!problemData.containsKey("timestamp")) {
                            Log.d("DenemeApplication", "Problem ${doc.id}: 'timestamp' alanı eksik, ekleniyor...")
                            needsUpdate["timestamp"] = com.google.firebase.Timestamp.now()
                        }
                        
                        // Güncelleme gerekiyorsa uygula
                        if (needsUpdate.isNotEmpty()) {
                            problemsCollection.document(doc.id).update(needsUpdate).await()
                            Log.d("DenemeApplication", "Problem ${doc.id} güncellendi: $needsUpdate")
                            updatedCount++
                        } else {
                            noChangeCount++
                        }
                    } catch (e: Exception) {
                        Log.e("DenemeApplication", "Problem ${doc.id} güncellenirken hata: ${e.message}")
                        errorCount++
                    }
                }
                
                Log.d("DenemeApplication", "Veri doğrulama tamamlandı: $updatedCount soru güncellendi, $noChangeCount soru zaten doğruydu, $errorCount hatalar")
                
                // Problem repository'deki fonksiyonu da çalıştır
                val result = problemRepository.updateAllProblemAnswerCounts()
                result.fold(
                    onSuccess = { count ->
                        Log.d("DenemeApplication", "AnswerCount güncelleme: Toplam $count soru güncellendi")
                    },
                    onFailure = { error ->
                        Log.e("DenemeApplication", "AnswerCount güncelleme hatası: ${error.message}", error)
                    }
                )
            } catch (e: Exception) {
                Log.e("DenemeApplication", "Veritabanı doğrulama işlemi sırasında hata: ${e.message}", e)
            }
            
            Log.d("DenemeApplication", "====== FIREBASE VERİTABANI DOĞRULAMALARI TAMAMLANDI ======")
        }
    }
} 