package com.example.apptareas

import android.content.Intent
import android.os.Bundle
import android.view.View
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
        val fab = findViewById<View>(R.id.fab)

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
                    // Mapeamos los documentos al modelo 'Task' usando su ID único
                    val listaTareas = mutableListOf<Task>()

                    for (document in result) {
                        val id = document.id // ID único del documento
                        val titulo = document.getString("titulo") ?: "Tarea sin título"
                        val fecha = document.getString("fecha") ?: "Sin fecha"
                        val estado = document.getString("estado") ?: "pendiente"

                        listaTareas.add(Task(id, titulo, fecha, estado))
                    }

                    // 6. Actualizar el contador con el tamaño de la lista de objetos
                    labelPending.text = "PENDIENTES · ${listaTareas.size}"

                    // 7. Mostrar/Ocultar el estado vacío dependiendo de si hay tareas
                    if (listaTareas.isEmpty()) {
                        rvTasks.visibility = View.GONE
                        emptyState.visibility = View.VISIBLE
                    } else {
                        rvTasks.visibility = View.VISIBLE
                        emptyState.visibility = View.GONE

                        // Configuramos el TaskAdapter universal pasándole el ID al hacer clic
                        val adapter = TaskAdapter(listaTareas) { idClickeado ->
                            val intent = Intent(this@TaskListActivity, DetalleTarea::class.java)
                            intent.putExtra("ID_DE_LA_TAREA", idClickeado) // Enviamos el ID seguro
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
}