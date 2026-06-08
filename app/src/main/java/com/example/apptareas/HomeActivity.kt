package com.example.apptareas

import android.app.AlarmManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
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
import android.Manifest
import android.provider.Settings
import java.util.Calendar

class HomeActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.home)

        //notificacion crear canal, pedir los permisos para mostrar notif
        NotificationUtil.createChannel(this)
        pedirPermisos()

        //prueba borrar despues
        NotificationUtil.scheduleReminder(
            context       = this,
            taskId        = "test_001",
            taskName      = "Esta es una tarea de prueba",
            dueTimeMillis = 0L
        )

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
        NavigationUtils.configurarNavegacion(this, bottomNav)

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

            //Buscar datos del usuario (Nombre)
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

            //-- Buscar las tareas para la lista y el progreso--
            //Obtenemos la fecha exacta de hoy en formato "d/m/yyyy"
            val cal = Calendar.getInstance()
            val dia = cal.get(Calendar.DAY_OF_MONTH)
            val mes = cal.get(Calendar.MONTH) + 1
            val anio = cal.get(Calendar.YEAR)

            val fechaHoy = String.format("%02d/%02d/%04d", dia, mes, anio)
            Toast.makeText(this, "Home buscando tareas de: $fechaHoy", Toast.LENGTH_LONG).show()

            //Buscamos en Firebase con la nueva fecha estandarizada
            db.collection("Usuarios").document(uid).collection("Mis_Tareas")
                .whereEqualTo("fecha", fechaHoy)
                .get()
                .addOnSuccessListener { resultado ->

                    val listaTareas = mutableListOf<Task>()
                    var completadas = 0
                    var pendientes = 0

                    for (documento in resultado) {
                        // Obtenemos los campos y el ID de Firestore
                        val id = documento.id
                        val titulo = documento.getString("titulo") ?: "Tarea sin título"
                        val fecha = documento.getString("fecha") ?: "Sin fecha"
                        val estado = documento.getString("estado") ?: "pendiente"
                        val prioridad = documento.getString("prioridad") ?: "Baja"

                        // Agregamos el objeto Task a la lista visual
                        listaTareas.add(Task(id, titulo, fecha, estado, prioridad))

                        // Contamos los estados (ahora la barra de progreso será exclusiva de HOY)
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

                    // --- CAMBIO: Usamos el TaskAdapter y enviamos el ID al clic ---
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

    // ---parte de los permisos de las notificaciones---

    private fun pedirPermisos() {
        //permiso de notificaciones (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    100
                )
            }
        }

        //permiso de alarma exacta (Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(AlarmManager::class.java)
            if (!alarmManager.canScheduleExactAlarms()) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                startActivity(intent)
            }
        }
    }

    //resultado del permiso de notificaciones
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permiso concedido
            } else {
                Toast.makeText(
                    this,
                    "Activá las notificaciones para recibir recordatorios",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}