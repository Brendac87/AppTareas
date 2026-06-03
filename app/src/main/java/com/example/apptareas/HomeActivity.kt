package com.example.apptareas

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class HomeActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.home) // Asegúrate de que el XML se llame 'home.xml'

        // 1. Inicializar Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // 2. Conectar las vistas del perfil
        val userName = findViewById<TextView>(R.id.userName)

        // 3. Conectar las vistas de progreso
        val tvProgressPercent = findViewById<TextView>(R.id.tvProgressPercent)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        val tvDoneCount = findViewById<TextView>(R.id.tvDoneCount)
        val tvPendingCount = findViewById<TextView>(R.id.tvPendingCount)

        // 4. Conectar navegación y lista
        val tvSeeAll = findViewById<TextView>(R.id.tvSeeAll)
        val rvTasks = findViewById<RecyclerView>(R.id.rvTasks)
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        val fab = findViewById<View>(R.id.fab)

        // --- CONFIGURACIÓN DE NAVEGACIÓN ---

        bottomNav.selectedItemId = R.id.nav_home

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true // Ya estamos aquí
                R.id.nav_tasks -> {
                    startActivity(Intent(this, TaskListActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                else -> false
            }
        }

        fab.setOnClickListener {
            startActivity(Intent(this, NuevaTarea::class.java))
        }

        tvSeeAll.setOnClickListener {
            startActivity(Intent(this, TaskListActivity::class.java))
            overridePendingTransition(0, 0)
            finish()
        }

        // --- LÓGICA DE FIREBASE ---

        rvTasks.layoutManager = LinearLayoutManager(this)

        val currentUser = auth.currentUser
        if (currentUser != null) {
            val uid = currentUser.uid

            // A. Buscar datos del usuario (Nombre)
            db.collection("Usuarios").document(uid).get()
                .addOnSuccessListener { documento ->
                    if (documento.exists()) {
                        val nombre = documento.getString("nombre") ?: "Usuario"
                        val apellido = documento.getString("apellido") ?: ""

                        // Recortamos la inicial del apellido si existe
                        val inicial = if (apellido.isNotEmpty()) "${apellido[0]}." else ""
                        userName.text = "$nombre $inicial"
                    }
                }

            // B. Buscar las tareas para la lista y el progreso
            db.collection("Usuarios").document(uid).collection("Mis_Tareas")
                .get()
                .addOnSuccessListener { resultado ->
                    val titulosDeTareas = mutableListOf<String>()
                    var completadas = 0
                    var pendientes = 0

                    for (documento in resultado) {
                        // Guardamos el título para la lista
                        val titulo = documento.getString("titulo") ?: "Tarea sin título"
                        titulosDeTareas.add(titulo)

                        // Contamos para la estadística
                        val estado = documento.getString("estado") ?: "pendiente"
                        if (estado.equals("completado", ignoreCase = true) || estado.equals("completada", ignoreCase = true)) {
                            completadas++
                        } else {
                            pendientes++
                        }
                    }

                    // C. Calcular porcentajes y actualizar la tarjeta de progreso
                    val totalTareas = completadas + pendientes

                    if (totalTareas > 0) {
                        val porcentaje = (completadas * 100) / totalTareas
                        tvProgressPercent.text = "$porcentaje%"
                        progressBar.progress = porcentaje
                    } else {
                        tvProgressPercent.text = "0%"
                        progressBar.progress = 0
                    }

                    tvDoneCount.text = "$completadas completadas"
                    tvPendingCount.text = "$pendientes pendientes"

                    // D. Cargar la lista de tareas en el RecyclerView
                    val adapter = SimpleTaskAdapter(titulosDeTareas) { tituloClickeado ->
                        val intent = Intent(this@HomeActivity, DetalleTarea::class.java)
                        intent.putExtra("TITULO_DE_LA_TAREA", tituloClickeado)
                        startActivity(intent)
                    }
                    rvTasks.adapter = adapter
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error al cargar los datos", Toast.LENGTH_SHORT).show()
                }
        }
    }

    // --- CLASE ADAPTADOR PARA EL RECYCLERVIEW ---
    class SimpleTaskAdapter(
        private val tasks: List<String>,
        private val onTaskClick: (String) -> Unit
    ) : RecyclerView.Adapter<SimpleTaskAdapter.TaskViewHolder>() {

        class TaskViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val textView: TextView = view.findViewById(android.R.id.text1)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_1, parent, false)
            return TaskViewHolder(view)
        }

        override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
            val titulo = tasks[position]
            holder.textView.text = titulo
            holder.textView.setTextColor(android.graphics.Color.WHITE)

            holder.itemView.setOnClickListener {
                onTaskClick(titulo)
            }
        }

        override fun getItemCount() = tasks.size
    }
}