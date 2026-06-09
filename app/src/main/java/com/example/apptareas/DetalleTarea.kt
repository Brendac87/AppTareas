package com.example.apptareas

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.widget.ImageView
import android.widget.LinearLayout
import com.bumptech.glide.Glide
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class DetalleTarea : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    // --- 1. Variables Globales de las Vistas ---
    private lateinit var btnOptions: ImageButton
    private lateinit var btnCancelar: Button
    private lateinit var btnCompletar: Button
    private lateinit var tvTitulo: TextView
    private lateinit var tvDescripcion: TextView
    private lateinit var tvPrioridad: TextView
    private lateinit var tvCategoria: TextView
    private lateinit var tvEstado: TextView
    private lateinit var tvFecha: TextView
    private lateinit var tvHora: TextView
    private lateinit var layoutFotoArea: LinearLayout
    private lateinit var ivFotoTarea: ImageView
    private lateinit var layoutUbicacionArea: LinearLayout
    private lateinit var mapViewTarea: MapView

    // ID de la tarea a nivel global
    private var idRecibido: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializar OSMDroid
        Configuration.getInstance().load(applicationContext, getSharedPreferences("osmdroid", MODE_PRIVATE))
        setContentView(R.layout.detalle_tarea)

        // Inicializar Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // 2. Conectar las vistas a nuestras variables globales
        val btnBack = findViewById<ImageButton>(R.id.btn_back)
        btnOptions = findViewById(R.id.btn_options)
        btnCancelar = findViewById(R.id.btn_cancelar)
        btnCompletar = findViewById(R.id.btn_completar)
        tvTitulo = findViewById(R.id.tv_titulo_tarea)
        tvDescripcion = findViewById(R.id.tv_descripcion_tarea)
        tvPrioridad = findViewById(R.id.tv_prioridad)
        tvCategoria = findViewById(R.id.tv_categoria)
        tvEstado = findViewById(R.id.tv_estado)
        tvFecha = findViewById(R.id.tv_fecha)
        tvHora = findViewById(R.id.tv_hora)
        layoutFotoArea = findViewById(R.id.layoutFotoArea)
        ivFotoTarea = findViewById(R.id.ivFotoTarea)
        layoutUbicacionArea = findViewById(R.id.layoutUbicacionArea)
        mapViewTarea = findViewById(R.id.mapViewTarea)

        // Configurar el botón de volver
        btnBack.setOnClickListener { finish() }

        // Recibir el ID
        idRecibido = intent.getStringExtra("ID_DE_LA_TAREA")

        if (idRecibido != null) {
            // Configurar el botón Editar
            btnOptions.setOnClickListener {
                val intentEditar = Intent(this, EditarTarea::class.java)
                intentEditar.putExtra("ID_DE_LA_TAREA", idRecibido)
                startActivity(intentEditar)
            }
        } else {
            Toast.makeText(this, "Error: No llegó el ID de la tarea", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    // ========================================================
    // 3. LA MAGIA DE LA ACTUALIZACIÓN AUTOMÁTICA
    // ========================================================
    override fun onResume() {
        super.onResume()
        if (idRecibido != null) {
            cargarDatosDeFirebase()
        }
    }

    private fun cargarDatosDeFirebase() {
        val currentUser = auth.currentUser
        if (currentUser != null && idRecibido != null) {
            val uid = currentUser.uid

            db.collection("Usuarios").document(uid)
                .collection("Mis_Tareas").document(idRecibido!!)
                .get()
                .addOnSuccessListener { tarea ->

                    if (tarea.exists()) {
                        // Reemplazamos los textos
                        tvTitulo.text = tarea.getString("titulo") ?: "Sin título"
                        tvDescripcion.text = tarea.getString("descripcion") ?: "Sin descripción"
                        tvPrioridad.text = tarea.getString("prioridad") ?: "Baja"
                        tvCategoria.text = tarea.getString("categoria") ?: "Otros"
                        tvFecha.text = tarea.getString("fecha") ?: "--/--/----"
                        tvHora.text = tarea.getString("hora") ?: "--:--"

                        // --- LÓGICA DEL ESTADO Y BOTONES ---
                        val estadoActual = tarea.getString("estado") ?: "pendiente"
                        tvEstado.text = estadoActual.uppercase()

                        if (estadoActual.equals("completado", ignoreCase = true) || estadoActual.equals("completada", ignoreCase = true)) {
                            tvEstado.setTextColor(Color.parseColor("#4CAF50"))
                            btnCompletar.visibility = View.GONE
                            btnCancelar.visibility = View.GONE
                            btnOptions.visibility = View.GONE
                        } else if (estadoActual.equals("cancelado", ignoreCase = true) || estadoActual.equals("cancelada", ignoreCase = true)) {
                            tvEstado.setTextColor(Color.parseColor("#F44336"))
                            btnCompletar.visibility = View.GONE
                            btnCancelar.visibility = View.GONE
                            btnOptions.visibility = View.GONE
                        } else {
                            tvEstado.setTextColor(Color.parseColor("#FFC107"))
                            btnCompletar.visibility = View.VISIBLE
                            btnCancelar.visibility = View.VISIBLE
                            btnOptions.visibility = View.VISIBLE
                        }

                        // --- FUNCIONALIDAD DEL BOTÓN CANCELAR ---
                        btnCancelar.setOnClickListener {
                            val builder = AlertDialog.Builder(this)
                            builder.setTitle("¿Cancelar tarea?")
                            builder.setMessage("¿Estás seguro de que deseas cancelar esta tarea? Ya no podrás editarla ni marcarla como completada.")
                            builder.setPositiveButton("Sí, cancelar") { dialog, _ ->
                                db.collection("Usuarios").document(uid)
                                    .collection("Mis_Tareas").document(idRecibido!!)
                                    .update("estado", "cancelado")
                                    .addOnSuccessListener {
                                        Toast.makeText(this, "Tarea cancelada", Toast.LENGTH_SHORT).show()
                                        // Recargamos la pantalla para aplicar cambios visuales
                                        cargarDatosDeFirebase()
                                    }
                                    .addOnFailureListener { error ->
                                        Toast.makeText(this, "Error al cancelar: ${error.message}", Toast.LENGTH_SHORT).show()
                                    }
                                dialog.dismiss()
                            }
                            builder.setNegativeButton("No, mantener") { dialog, _ -> dialog.dismiss() }
                            builder.create().show()
                        }

                        // --- FUNCIONALIDAD DEL BOTÓN COMPLETAR ---
                        btnCompletar.setOnClickListener {
                            db.collection("Usuarios").document(uid)
                                .collection("Mis_Tareas").document(idRecibido!!)
                                .update("estado", "completado")
                                .addOnSuccessListener {
                                    Toast.makeText(this, "¡Tarea completada!", Toast.LENGTH_SHORT).show()
                                    // Recargamos la pantalla para aplicar cambios visuales
                                    cargarDatosDeFirebase()
                                }
                                .addOnFailureListener { error ->
                                    Toast.makeText(this, "Error al actualizar: ${error.message}", Toast.LENGTH_SHORT).show()
                                }
                        }

                        // --- LÓGICA DE LA FOTO ---
                        val fotoUrl = tarea.getString("foto")
                        if (!fotoUrl.isNullOrEmpty()) {
                            layoutFotoArea.visibility = View.VISIBLE
                            Glide.with(this@DetalleTarea)
                                .load(fotoUrl)
                                .into(ivFotoTarea)
                        } else {
                            layoutFotoArea.visibility = View.GONE
                        }

                        // --- LÓGICA DEL MAPA (OSMDROID) ---
                        val ubicacion = tarea.get("ubicacion") as? Map<String, Any>

                        if (ubicacion != null) {
                            layoutUbicacionArea.visibility = View.VISIBLE

                            val latitud = ubicacion["latitud"] as? Double ?: 0.0
                            val longitud = ubicacion["longitud"] as? Double ?: 0.0
                            val direccion = ubicacion["direccion"] as? String ?: "Ubicación"

                            mapViewTarea.setMultiTouchControls(true)
                            val mapController = mapViewTarea.controller
                            mapController.setZoom(18.0)

                            val puntoTarea = GeoPoint(latitud, longitud)
                            mapController.setCenter(puntoTarea)

                            // IMPORTANTE: Limpiar marcadores viejos antes de poner el nuevo
                            mapViewTarea.overlays.clear()

                            val marcador = Marker(mapViewTarea)
                            marcador.position = puntoTarea
                            marcador.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            marcador.title = direccion

                            mapViewTarea.overlays.add(marcador)
                            mapViewTarea.invalidate()
                        } else {
                            layoutUbicacionArea.visibility = View.GONE
                        }

                    } else {
                        Toast.makeText(this, "No se encontró la tarea", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { error ->
                    Toast.makeText(this, "Error de conexión: ${error.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
}