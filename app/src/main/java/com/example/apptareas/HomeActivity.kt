package com.example.apptareas

import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

class HomeActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    // --- 1. Subimos las variables de las vistas aquí arriba ---
    // Así tanto onCreate como onResume pueden modificarlas
    private lateinit var userName: TextView
    private lateinit var tvProgressPercent: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvDoneCount: TextView
    private lateinit var tvPendingCount: TextView
    private lateinit var rvTasks: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.home)

        // Inicializar Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // 2. Conectar las vistas a nuestras variables globales
        userName = findViewById(R.id.userName)
        tvProgressPercent = findViewById(R.id.tvProgressPercent)
        progressBar = findViewById(R.id.progressBar)
        tvDoneCount = findViewById(R.id.tvDoneCount)
        tvPendingCount = findViewById(R.id.tvPendingCount)
        rvTasks = findViewById(R.id.rvTasks)

        val tvSeeAll = findViewById<TextView>(R.id.tvSeeAll)
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        val fab = findViewById<View>(R.id.fab)

        // Configuración inicial
        rvTasks.layoutManager = LinearLayoutManager(this)
        NotificationUtil.createChannel(this)
        pedirPermisos()

        // --- CONFIGURACIÓN DE NAVEGACIÓN ---
        bottomNav.selectedItemId = R.id.nav_home
        NavigationUtils.configurarNavegacion(this, bottomNav)

        fab.setOnClickListener {
            startActivity(Intent(this, NuevaTarea::class.java))
        }

        tvSeeAll.setOnClickListener {
            startActivity(Intent(this, TaskListActivity::class.java))
            overridePendingTransition(0, 0)
            finish()
        }

        // ¡Fíjate que ya no descargamos datos en onCreate!
    }

    // ========================================================
    // 3. LA MAGIA DE LA ACTUALIZACIÓN AUTOMÁTICA
    // ========================================================
    override fun onResume() {
        super.onResume()
        // Cada vez que esta pantalla vuelva a estar al frente, cargamos datos frescos
        cargarDatosDeFirebase()
    }

    private fun cargarDatosDeFirebase() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val uid = currentUser.uid

            // --- Buscar datos del usuario (Nombre) ---
            db.collection("Usuarios").document(uid).get()
                .addOnSuccessListener { documento ->
                    if (documento.exists()) {
                        val nombre = documento.getString("nombre") ?: "Usuario"
                        val apellido = documento.getString("apellido") ?: ""
                        val inicial = if (apellido.isNotEmpty()) "${apellido[0]}." else ""
                        userName.text = "$nombre $inicial"
                    }
                }

            // --- Buscar las tareas para la lista y el progreso ---
            val cal = Calendar.getInstance()
            val dia = cal.get(Calendar.DAY_OF_MONTH)
            val mes = cal.get(Calendar.MONTH) + 1
            val anio = cal.get(Calendar.YEAR)
            val fechaHoy = String.format("%02d/%02d/%04d", dia, mes, anio)

            db.collection("Usuarios").document(uid).collection("Mis_Tareas")
                .whereEqualTo("fecha", fechaHoy)
                .get()
                .addOnSuccessListener { resultado ->

                    // Esta lista nueva automáticamente "limpia" la visualización anterior
                    val listaTareas = mutableListOf<Task>()
                    var completadas = 0
                    var pendientes = 0

                    for (documento in resultado) {
                        val id = documento.id
                        val titulo = documento.getString("titulo") ?: "Tarea sin título"
                        val fecha = documento.getString("fecha") ?: "Sin fecha"
                        val estado = documento.getString("estado") ?: "pendiente"
                        val prioridad = documento.getString("prioridad") ?: "Baja"

                        listaTareas.add(Task(id, titulo, fecha, estado, prioridad))

                        if (estado.equals("completado", ignoreCase = true) || estado.equals("completada", ignoreCase = true)) {
                            completadas++
                        } else {
                            pendientes++
                        }
                    }

                    // Calcular porcentajes y actualizar la tarjeta de progreso
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

                    // Cargar el adaptador renovado al RecyclerView
                    val adapter = TaskAdapter(listaTareas) { idClickeado ->
                        val intent = Intent(this@HomeActivity, DetalleTarea::class.java)
                        intent.putExtra("ID_DE_LA_TAREA", idClickeado)
                        startActivity(intent)
                    }
                    rvTasks.adapter = adapter
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error al cargar los datos", Toast.LENGTH_SHORT).show()
                }
        }
    }

    // --- LÓGICA DE PERMISOS ---
    private fun pedirPermisos() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100)
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(AlarmManager::class.java)
            if (!alarmManager.canScheduleExactAlarms()) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                startActivity(intent)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permiso concedido
            } else {
                Toast.makeText(this, "Activá las notificaciones para recibir recordatorios", Toast.LENGTH_LONG).show()
            }
        }
    }
}