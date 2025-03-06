package com.example.deneme

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class ProblemListActivity : AppCompatActivity() {
    private lateinit var db: FirebaseFirestore
    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_problem_list)

        db = FirebaseFirestore.getInstance()
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        db.collection("problems")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                val problemTitles = mutableListOf<String>()
                val problemIds = mutableListOf<String>()
                for (doc in documents) {
                    val title = doc.getString("title") ?: "Başlıksız"
                    problemTitles.add(title)
                    problemIds.add(doc.id)
                }
                recyclerView.adapter = ProblemAdapter(problemTitles) { position ->
                    val intent = Intent(this@ProblemListActivity, ProblemDetailActivity::class.java)
                    intent.putExtra("problemId", problemIds[position])
                    startActivity(intent)
                }
            }
    }
}

// ProblemAdapter sınıfını tanımla
class ProblemAdapter(private val problems: List<String>, private val onItemClick: (Int) -> Unit) :
    RecyclerView.Adapter<ProblemAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTextView: TextView = itemView.findViewById(R.id.titleTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_problem, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.titleTextView.text = problems[position]
        holder.itemView.setOnClickListener {
            onItemClick(position)
        }
    }

    override fun getItemCount(): Int = problems.size
}