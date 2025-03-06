package com.example.deneme

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class ProblemDetailActivity : AppCompatActivity() {
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_problem_detail)

        db = FirebaseFirestore.getInstance()
        val problemId = intent.getStringExtra("problemId")

        val titleTextView = findViewById<TextView>(R.id.titleTextView)
        val descriptionTextView = findViewById<TextView>(R.id.descriptionTextView)

        db.collection("problems").document(problemId!!)
            .get()
            .addOnSuccessListener { doc ->
                titleTextView.text = doc.getString("title")
                descriptionTextView.text = doc.getString("description")
            }
    }
}