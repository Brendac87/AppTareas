package com.example.apptareas

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
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
    private lateinit var adapter: TaskAdapter // Guardamos el adaptador a nivel de clase

    // Variables para mantener el estado de la búsqueda
    private var listaOriginal = mutableListOf<Task>()
    private var textoBusqueda = ""
    private var filtroActual = "Todas"

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

        // --- NUEVO: Conectar Vistas de Búsqueda y Filtros ---
        val etSearch = findViewById<EditText>(R.id.etSearch)
        val filterAll = findViewById<TextView>(R.id.filterAll)
        val chipPending = findViewById<TextView>(R.id.chipPending)
        val chipDone = findViewById<TextView>(R.id.chipDone)
        val chipCanceled = findViewById<TextView>(R.id.chipCanceled)
        val chipHigh = findViewById<TextView>(R.id.chipHigh)
        val chipMedium = findViewById<TextView>(R.id.chipMedium)
        val chipLow = findViewById<TextView>(R.id.chipLow)

        val listaChips = listOf(filterAll, chipPending, chipDone,chipCanceled, chipHigh, chipMedium, chipLow)

        // Funcionalidad al botón + para ir a NuevaTarea
        fab.setOnClickListener {
            val intent = Intent(this, NuevaTarea::class.java)
            startActivity(intent)
        }

        // 3. Configurar la barra de navegación inferior
        bottomNav.selectedItemId = R.id.nav_tasks
        NavigationUtils.configurarNavegacion(this, bottomNav)

        // 4. Configurar la lista y el adaptador (inicia con lista vacía)
        rvTasks.layoutManager = LinearLayoutManager(this)
        adapter = TaskAdapter(emptyList()) { idClickeado ->
            val intent = Intent(this@TaskListActivity, DetalleTarea::class.java)
            intent.putExtra("ID_DE_LA_TAREA", idClickeado)
            startActivity(intent)
        }
        rvTasks.adapter = adapter

        // --- LÓGICA DE BÚSQUEDA (TEXTO) ---
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                textoBusqueda = s.toString()
                aplicarFiltros(labelPending, emptyState, rvTasks)
            }
        })

        // --- LÓGICA DE FILTROS (CHIPS) ---
        fun seleccionarFiltro(chipSeleccionado: TextView, nombreFiltro: String) {
            filtroActual = nombreFiltro

            // Restablecer el diseño de todos los chips al estado inactivo
            for (chip in listaChips) {
                chip.setBackgroundResource(R.drawable.bg_f_inactive)
                chip.setTextColor(Color.parseColor("#80FFFFFF"))
            }

            // Aplicar el diseño activo al chip presionado
            chipSeleccionado.setBackgroundResource(R.drawable.bg_f_active)
            chipSeleccionado.setTextColor(Color.WHITE)

            // Aplicar los filtros a la lista visual
            aplicarFiltros(labelPending, emptyState, rvTasks)
        }

        // Asignar clics a los botones
        filterAll.setOnClickListener { seleccionarFiltro(filterAll, "Todas") }
        chipPending.setOnClickListener { seleccionarFiltro(chipPending, "Pendientes") }
        chipDone.setOnClickListener { seleccionarFiltro(chipDone, "Completadas") }
        chipCanceled.setOnClickListener { seleccionarFiltro(chipCanceled, "Canceladas") }
        chipHigh.setOnClickListener { seleccionarFiltro(chipHigh, "Alta") }
        chipMedium.setOnClickListener { seleccionarFiltro(chipMedium, "Media") }
        chipLow.setOnClickListener { seleccionarFiltro(chipLow, "Baja") }

        // 5. Buscar las tareas en Firestore
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val uid = currentUser.uid

            db.collection("Usuarios").document(uid).collection("Mis_Tareas")
                .get()
                .addOnSuccessListener { result ->
                    listaOriginal.clear() // Limpiamos la lista maestra por si acaso

                    for (document in result) {
                        val id = document.id
                        val titulo = document.getString("titulo") ?: "Tarea sin título"
                        val fecha = document.getString("fecha") ?: "Sin fecha"
                        val estado = document.getString("estado") ?: "pendiente"
                        // IMPORTANTE: Recuperamos la prioridad para el Helper
                        val prioridad = document.getString("prioridad") ?: "Baja"

                        listaOriginal.add(Task(id, titulo, fecha, estado, prioridad))
                    }

                    // Al terminar de cargar la base de datos, aplicamos todo por primera vez
                    aplicarFiltros(labelPending, emptyState, rvTasks)
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error al cargar las tareas", Toast.LENGTH_SHORT).show()
                }
        }
    }

    // --- LA FUNCIÓN QUE COORDINA LA MAGIA ---
    private fun aplicarFiltros(labelPending: TextView, emptyState: LinearLayout, rvTasks: RecyclerView) {

        // Usamos nuestro archivo 'ayudante' externo
        val listaFiltrada = TaskFilterHelper.filtrarTareas(listaOriginal, textoBusqueda, filtroActual)

        // Actualizamos el adaptador
        adapter.actualizarLista(listaFiltrada)

        // Actualizamos los textos de estado
        labelPending.text = "RESULTADOS · ${listaFiltrada.size}"

        if (listaFiltrada.isEmpty()) {
            rvTasks.visibility = View.GONE
            emptyState.visibility = View.VISIBLE
        } else {
            rvTasks.visibility = View.VISIBLE
            emptyState.visibility = View.GONE
        }
    }
}