package com.example.apptareas

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class TaskListActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.task_list)

        // 1. Inicializar Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // 2. Conectar las vistas usando los IDs exactos de tu XML
        val rvTasks = findViewById<RecyclerView>(R.id.rvTasks)
        val labelPending = findViewById<TextView>(R.id.labelPending)
        val emptyState = findViewById<LinearLayout>(R.id.emptyState)
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        val fab = findViewById<View>(R.id.fab) // <- Conectamos el botón flotante

        // Funcionalidad al botón + para ir a NuevaTarea
        fab.setOnClickListener {
            val intent = Intent(this, NuevaTarea::class.java)
            startActivity(intent)
        }

        // 3. Configurar la barra de navegación inferior
        bottomNav.selectedItemId = R.id.nav_tasks
        NavigationUtils.configurarNavegacion(this, bottomNav)

        // 4. Configurar la lista
        rvTasks.layoutManager = LinearLayoutManager(this)

        // 5. Buscar las tareas en Firestore
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val uid = currentUser.uid

            db.collection("Usuarios").document(uid).collection("Mis_Tareas")
                .get()
                .addOnSuccessListener { result ->
                    val titulosDeTareas = mutableListOf<String>()

                    for (document in result) {
                        val titulo = document.getString("titulo") ?: "Tarea sin título"
                        titulosDeTareas.add(titulo)
                    }

                    // 6. Actualizar el contador de PENDIENTES
                    labelPending.text = "PENDIENTES · ${titulosDeTareas.size}"

                    // 7. Mostrar/Ocultar el estado vacío dependiendo de si hay tareas
                    if (titulosDeTareas.isEmpty()) {
                        rvTasks.visibility = View.GONE
                        emptyState.visibility = View.VISIBLE
                    } else {
                        rvTasks.visibility = View.VISIBLE
                        emptyState.visibility = View.GONE

                        val adapter = SimpleTaskAdapter(titulosDeTareas) { tituloClickeado ->
                            val intent = Intent(this@TaskListActivity, DetalleTarea::class.java)
                            intent.putExtra("TITULO_DE_LA_TAREA", tituloClickeado)
                            startActivity(intent)
                        }
                        rvTasks.adapter = adapter
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error al cargar las tareas", Toast.LENGTH_SHORT).show()
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

            // --- ¡CAMBIO AQUÍ! Asignamos el evento de clic a toda la celda ---
            holder.itemView.setOnClickListener {
                onTaskClick(titulo) // Ejecuta la función mandando el título correspondiente
            }
        }

        override fun getItemCount() = tasks.size
    }
}