package com.example.apptareas

import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.io.File
import java.util.Calendar

class EditarTarea : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    // Variables dinámicas para guardar lo que el usuario elija (o lo que venga de la BD)
    private var prioridadSeleccionada = "Alta"
    private var categoriaSeleccionada = "Otros"
    private var fechaSeleccionada = "Sin fecha"
    private var horaSeleccionada = "Sin hora"

    // Variables para la ubicación
    private var latitudGuardada: Double? = null
    private var longitudGuardada: Double? = null
    private var direccionGuardada: String = ""

    // Variables para la Cámara y Storage
    private var uriFotoTemporal: Uri? = null
    private var urlFotoSubida: String? = null

    // Receptor de la cámara
    private val tomarFotoLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { exito ->
        if (exito && uriFotoTemporal != null) {
            subirFotoAFirebase(uriFotoTemporal!!)
        } else {
            Toast.makeText(this, "Se canceló la foto", Toast.LENGTH_SHORT).show()
        }
    }

    private fun subirFotoAFirebase(archivoUri: Uri) {
        Toast.makeText(this, "Subiendo foto, por favor espera...", Toast.LENGTH_LONG).show()

        val storageRef = FirebaseStorage.getInstance().reference
        val nombreArchivo = "fotos_tareas/IMG_${System.currentTimeMillis()}.jpg"
        val fotoRef = storageRef.child(nombreArchivo)

        fotoRef.putFile(archivoUri)
            .addOnSuccessListener {
                fotoRef.downloadUrl.addOnSuccessListener { uriDescarga ->
                    urlFotoSubida = uriDescarga.toString() // Guardamos el nuevo link

                    val btnTomarFoto = findViewById<TextView>(R.id.btnTomarFoto)
                    btnTomarFoto.text = "📸 Foto adjuntada"
                    btnTomarFoto.setBackgroundResource(R.drawable.bg_option_selected)
                }
            }
            .addOnFailureListener { error ->
                Toast.makeText(this, "Error al subir: ${error.message}", Toast.LENGTH_LONG).show()
                error.printStackTrace()
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Inicializar OSMDroid ANTES de la vista
        Configuration.getInstance().load(applicationContext, getSharedPreferences("osmdroid", MODE_PRIVATE))
        setContentView(R.layout.editar_tarea)

        // 1. Inicializar Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // 2. Conectar vistas
        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        val etTaskName = findViewById<EditText>(R.id.etTaskName)
        val etTaskDescription = findViewById<EditText>(R.id.etTaskDescription)
        val tvDate = findViewById<TextView>(R.id.tvDate)
        val tvTime = findViewById<TextView>(R.id.tvTime)
        val btnGuardarCambios = findViewById<Button>(R.id.btnGuardarCambios)

        val btnAlta = findViewById<TextView>(R.id.btnPrioridadAlta)
        val btnMedia = findViewById<TextView>(R.id.btnPrioridadMedia)
        val btnBaja = findViewById<TextView>(R.id.btnPrioridadBaja)

        val btnCatEstudios = findViewById<TextView>(R.id.btnCatEstudios)
        val btnCatTrabajo = findViewById<TextView>(R.id.btnCatTrabajo)
        val btnCatPersonal = findViewById<TextView>(R.id.btnCatPersonal)
        val btnCatCompras = findViewById<TextView>(R.id.btnCatCompras)
        val btnCatHobbies = findViewById<TextView>(R.id.btnCatHobbies)
        val btnCatOtros = findViewById<TextView>(R.id.btnCatOtros)
        val listaCategorias = listOf(btnCatEstudios, btnCatTrabajo, btnCatPersonal, btnCatCompras, btnCatHobbies, btnCatOtros)

        val btnTomarFoto = findViewById<TextView>(R.id.btnTomarFoto)
        val btnAddLocation = findViewById<TextView>(R.id.btnAddLocation)
        val layoutPreviewMapa = findViewById<LinearLayout>(R.id.layoutPreviewMapa)
        val mapViewPreview = findViewById<MapView>(R.id.mapViewPreview)


        // --- FUNCIONES VISUALES ---
        fun seleccionarPrioridad(prioridad: String) {
            prioridadSeleccionada = prioridad
            btnAlta.setBackgroundResource(R.drawable.bg_glass_input)
            btnAlta.setTextColor(Color.WHITE)
            btnMedia.setBackgroundResource(R.drawable.bg_glass_input)
            btnMedia.setTextColor(Color.WHITE)
            btnBaja.setBackgroundResource(R.drawable.bg_glass_input)
            btnBaja.setTextColor(Color.WHITE)

            when (prioridad.lowercase()) {
                "alta" -> {
                    btnAlta.setBackgroundResource(R.drawable.bg_option_selected)
                    btnAlta.setTextColor(Color.parseColor("#E9407A"))
                }
                "media" -> {
                    btnMedia.setBackgroundResource(R.drawable.bg_option_selected)
                    btnMedia.setTextColor(Color.WHITE)
                }
                "baja" -> {
                    btnBaja.setBackgroundResource(R.drawable.bg_option_selected)
                    btnBaja.setTextColor(Color.WHITE)
                }
            }
        }

        fun seleccionarCategoria(textViewSeleccionado: TextView, categoria: String) {
            categoriaSeleccionada = categoria
            for (btn in listaCategorias) {
                btn.setBackgroundResource(R.drawable.bg_glass_input)
            }
            textViewSeleccionado.setBackgroundResource(R.drawable.bg_option_selected)
        }

        // Asignar clics a los botones de categoría y prioridad
        btnAlta.setOnClickListener { seleccionarPrioridad("Alta") }
        btnMedia.setOnClickListener { seleccionarPrioridad("Media") }
        btnBaja.setOnClickListener { seleccionarPrioridad("Baja") }

        btnCatEstudios.setOnClickListener { seleccionarCategoria(btnCatEstudios, "Estudios") }
        btnCatTrabajo.setOnClickListener { seleccionarCategoria(btnCatTrabajo, "Trabajo") }
        btnCatPersonal.setOnClickListener { seleccionarCategoria(btnCatPersonal, "Personal") }
        btnCatCompras.setOnClickListener { seleccionarCategoria(btnCatCompras, "Compras") }
        btnCatHobbies.setOnClickListener { seleccionarCategoria(btnCatHobbies, "Hobbies") }
        btnCatOtros.setOnClickListener { seleccionarCategoria(btnCatOtros, "Otros") }

        // --- LÓGICA DE FECHA Y HORA ---
        tvDate.setOnClickListener {
            val cal = Calendar.getInstance()
            android.app.DatePickerDialog(this, { _, year, month, day ->
                val mesReal = month + 1
                fechaSeleccionada = String.format("%02d/%02d/%04d", day, mesReal, year)
                tvDate.text = fechaSeleccionada
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        tvTime.setOnClickListener {
            val cal = Calendar.getInstance()
            android.app.TimePickerDialog(this, { _, hour, minute ->
                horaSeleccionada = String.format("%02d:%02d", hour, minute)
                tvTime.text = horaSeleccionada
            }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), false).show()
        }

        btnBack.setOnClickListener { finish() }

        // --- LÓGICA CÁMARA INTELIGENTE ---
        btnTomarFoto.setOnClickListener {
            if (urlFotoSubida != null) {
                AlertDialog.Builder(this)
                    .setTitle("Foto adjuntada")
                    .setMessage("¿Deseas eliminar la foto actual de esta tarea?")
                    .setPositiveButton("Eliminar") { _, _ ->
                        urlFotoSubida = null
                        uriFotoTemporal = null
                        btnTomarFoto.text = "Añadir foto"
                        btnTomarFoto.setBackgroundResource(R.drawable.bg_glass_input)
                        Toast.makeText(this, "Foto eliminada", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("Mantener", null)
                    .show()
            } else {
                try {
                    val archivoTemporal = File.createTempFile("JPEG_${System.currentTimeMillis()}_", ".jpg", cacheDir)
                    uriFotoTemporal = FileProvider.getUriForFile(this, "com.example.apptareas.fileprovider", archivoTemporal)
                    tomarFotoLauncher.launch(uriFotoTemporal!!)
                } catch (e: Exception) {
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }

// --- LÓGICA UBICACIÓN INTELIGENTE ---
        btnAddLocation.setOnClickListener {
            if (latitudGuardada != null) {
                AlertDialog.Builder(this)
                    .setTitle("Ubicación guardada")
                    .setMessage("¿Deseas eliminar la ubicación actual?")
                    .setPositiveButton("Eliminar") { _, _ ->
                        latitudGuardada = null
                        longitudGuardada = null
                        direccionGuardada = ""
                        btnAddLocation.text = "Añadir Ubicación"
                        btnAddLocation.setBackgroundResource(R.drawable.bg_glass_input)
                        layoutPreviewMapa.visibility = View.GONE
                        Toast.makeText(this, "Ubicación eliminada", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("Mantener", null)
                    .show()
            } else {
                val input = EditText(this)
                input.hint = "Ej: Mitre 123, Avellaneda"
                input.setPadding(50, 40, 50, 40)

                AlertDialog.Builder(this)
                    .setTitle("Buscar Dirección")
                    .setMessage("Ingresa la calle y ciudad:")
                    .setView(input)
                    .setPositiveButton("Buscar") { _, _ ->
                        val direccionEscrita = input.text.toString()
                        if (direccionEscrita.isNotEmpty()) {
                            Toast.makeText(this, "Buscando opciones...", Toast.LENGTH_SHORT).show()

                            CoroutineScope(Dispatchers.Main).launch {
                                val opciones = NominatimHelper.buscarOpciones(direccionEscrita)
                                if (opciones.isNotEmpty()) {
                                    val nombresOpciones = opciones.map { it.nombre }.toTypedArray()

                                    // --- CREAMOS EL ADAPTADOR CON EL DISEÑO PERSONALIZADO ---
                                    val adaptador = android.widget.ArrayAdapter(
                                        this@EditarTarea,
                                        R.layout.item_ubicacion, // Tu nuevo XML
                                        android.R.id.text1,      // El ID del texto dentro de ese XML
                                        nombresOpciones
                                    )

                                    // --- USAMOS SETADAPTER EN LUGAR DE SETITEMS ---
                                    AlertDialog.Builder(this@EditarTarea)
                                        .setTitle("Selecciona la ubicación correcta:")
                                        .setAdapter(adaptador) { _, indiceSeleccionado ->
                                            val lugarElegido = opciones[indiceSeleccionado]
                                            latitudGuardada = lugarElegido.latitud
                                            longitudGuardada = lugarElegido.longitud
                                            direccionGuardada = lugarElegido.nombre

                                            btnAddLocation.text = "📍 Ubicación adjunta"
                                            btnAddLocation.setBackgroundResource(R.drawable.bg_option_selected)

                                            layoutPreviewMapa.visibility = View.VISIBLE
                                            mapViewPreview.setMultiTouchControls(true)
                                            val mapController = mapViewPreview.controller
                                            mapController.setZoom(18.0)
                                            val puntoPivote = GeoPoint(latitudGuardada!!, longitudGuardada!!)
                                            mapController.setCenter(puntoPivote)

                                            mapViewPreview.overlays.clear()
                                            val marcador = Marker(mapViewPreview)
                                            marcador.position = puntoPivote
                                            marcador.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                            marcador.title = direccionGuardada
                                            mapViewPreview.overlays.add(marcador)
                                            mapViewPreview.invalidate()
                                        }
                                        .setNegativeButton("Cancelar", null)
                                        .show()
                                } else {
                                    Toast.makeText(this@EditarTarea, "No se encontraron resultados.", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }
        }

        // ========================================================
        // 5. CARGAR LOS DATOS ACTUALES DE FIREBASE
        // ========================================================
        val idRecibido = intent.getStringExtra("ID_DE_LA_TAREA")
        val currentUser = auth.currentUser

        if (idRecibido != null && currentUser != null) {
            val uid = currentUser.uid

            db.collection("Usuarios").document(uid).collection("Mis_Tareas").document(idRecibido)
                .get()
                .addOnSuccessListener { tarea ->
                    if (tarea.exists()) {
                        // Textos
                        etTaskName.setText(tarea.getString("titulo") ?: "")
                        etTaskDescription.setText(tarea.getString("descripcion") ?: "")

                        // Fecha y Hora
                        fechaSeleccionada = tarea.getString("fecha") ?: "Sin fecha"
                        tvDate.text = fechaSeleccionada
                        horaSeleccionada = tarea.getString("hora") ?: "Sin hora"
                        tvTime.text = horaSeleccionada

                        // Prioridad
                        val prioridadBD = tarea.getString("prioridad") ?: "Baja"
                        seleccionarPrioridad(prioridadBD)

                        // Categoría
                        val categoriaBD = tarea.getString("categoria") ?: "Otros"
                        when (categoriaBD.lowercase()) {
                            "estudios" -> seleccionarCategoria(btnCatEstudios, "Estudios")
                            "trabajo" -> seleccionarCategoria(btnCatTrabajo, "Trabajo")
                            "personal" -> seleccionarCategoria(btnCatPersonal, "Personal")
                            "compras" -> seleccionarCategoria(btnCatCompras, "Compras")
                            "hobbies" -> seleccionarCategoria(btnCatHobbies, "Hobbies")
                            else -> seleccionarCategoria(btnCatOtros, "Otros")
                        }

                        // --- CARGAR FOTO PREVIA ---
                        urlFotoSubida = tarea.getString("foto")
                        if (!urlFotoSubida.isNullOrEmpty()) {
                            btnTomarFoto.text = "📸 Foto adjuntada"
                            btnTomarFoto.setBackgroundResource(R.drawable.bg_option_selected)
                        }

                        // --- CARGAR UBICACIÓN PREVIA ---
                        val ubicacion = tarea.get("ubicacion") as? Map<String, Any>
                        if (ubicacion != null) {
                            latitudGuardada = ubicacion["latitud"] as? Double
                            longitudGuardada = ubicacion["longitud"] as? Double
                            direccionGuardada = ubicacion["direccion"] as? String ?: ""

                            if (latitudGuardada != null && longitudGuardada != null) {
                                btnAddLocation.text = "📍 Ubicación adjunta"
                                btnAddLocation.setBackgroundResource(R.drawable.bg_option_selected)

                                layoutPreviewMapa.visibility = View.VISIBLE
                                mapViewPreview.setMultiTouchControls(true)
                                val mapController = mapViewPreview.controller
                                mapController.setZoom(18.0)
                                val puntoPivote = GeoPoint(latitudGuardada!!, longitudGuardada!!)
                                mapController.setCenter(puntoPivote)

                                val marcador = Marker(mapViewPreview)
                                marcador.position = puntoPivote
                                marcador.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                marcador.title = direccionGuardada
                                mapViewPreview.overlays.add(marcador)
                                mapViewPreview.invalidate()
                            }
                        }
                    }
                }

            // ========================================================
            // 6. GUARDAR LOS CAMBIOS EN FIREBASE
            // ========================================================
            btnGuardarCambios.setOnClickListener {
                val nuevoTitulo = etTaskName.text.toString().trim()
                val nuevaDesc = etTaskDescription.text.toString().trim()

                if (nuevoTitulo.isEmpty()) {
                    Toast.makeText(this, "El título no puede estar vacío", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val objetoUbicacion = if (latitudGuardada != null && longitudGuardada != null) {
                    mapOf(
                        "latitud" to latitudGuardada,
                        "longitud" to longitudGuardada,
                        "direccion" to direccionGuardada
                    )
                } else null

                // Usamos "Any?" para permitir guardar "null" si el usuario eliminó la foto o ubicación
                val actualizaciones = mapOf<String, Any?>(
                    "titulo" to nuevoTitulo,
                    "descripcion" to nuevaDesc,
                    "fecha" to fechaSeleccionada,
                    "hora" to horaSeleccionada,
                    "prioridad" to prioridadSeleccionada,
                    "categoria" to categoriaSeleccionada,
                    "foto" to urlFotoSubida,
                    "ubicacion" to objetoUbicacion
                )

                db.collection("Usuarios").document(uid).collection("Mis_Tareas").document(idRecibido)
                    .update(actualizaciones)
                    .addOnSuccessListener {
                        Toast.makeText(this, "¡Tarea actualizada!", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Error al actualizar: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            }
        }
    }
}