package com.example.apptareas

import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.UUID
import androidx.appcompat.app.AlertDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import android.widget.LinearLayout
import android.view.View

class NuevaTarea : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    // Variables dinámicas
    private var prioridadSeleccionada = "Alta"
    private var categoriaSeleccionada = "Estudios"
    //Variabels fecha y hora
    private var fechaSeleccionada = "Sin fecha"
    private var horaSeleccionada = "Sin hora"
    //Variables para la ubicación
    private var latitudGuardada: Double? = null
    private var longitudGuardada: Double? = null
    private var direccionGuardada: String = ""
    //Variables para la Cámara y Storage
    private var uriFotoTemporal: Uri? = null
    private var urlFotoSubida: String? = null

    // Este es el "receptor" que espera a que cierres la cámara
    private val tomarFotoLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { exito ->
        if (exito && uriFotoTemporal != null) {
            // Si la foto se tomó bien, la subimos a Firebase Storage
            subirFotoAFirebase(uriFotoTemporal!!)
        } else {
            Toast.makeText(this, "Se canceló la foto", Toast.LENGTH_SHORT).show()
        }
    }

    private fun subirFotoAFirebase(archivoUri: Uri) {
        Toast.makeText(this, "Subiendo foto, por favor espera...", Toast.LENGTH_LONG).show()

        // 1. Creamos la referencia en Firebase Storage
        val storageRef = FirebaseStorage.getInstance().reference
        val nombreArchivo = "fotos_tareas/IMG_${System.currentTimeMillis()}.jpg"
        val fotoRef = storageRef.child(nombreArchivo)

        // 2. Subimos el archivo
        fotoRef.putFile(archivoUri)
            .addOnSuccessListener {
                // 3. Si se sube bien, le pedimos a Firebase el link (URL) público
                fotoRef.downloadUrl.addOnSuccessListener { uriDescarga ->
                    urlFotoSubida = uriDescarga.toString() // Guardamos el link

                    // Cambiamos el texto del botón para que el usuario sepa que funcionó
                    val btnTomarFoto = findViewById<TextView>(R.id.btnTomarFoto)
                    btnTomarFoto.text = "📸 Foto adjuntada con éxito"
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
        Configuration.getInstance().load(applicationContext, getSharedPreferences("osmdroid", MODE_PRIVATE))
        setContentView(R.layout.nueva_tarea)

        // 1. Inicializar Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // 2. Conectar vistas principales
        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        val etTaskName = findViewById<EditText>(R.id.etTaskName)
        val etTaskDescription = findViewById<EditText>(R.id.etTaskDescription)
        val btnCrearTareaFinal = findViewById<Button>(R.id.btnCrearTareaFinal)

        // 3. Conectar botones de Prioridad
        val btnAlta = findViewById<TextView>(R.id.btnPrioridadAlta)
        val btnMedia = findViewById<TextView>(R.id.btnPrioridadMedia)
        val btnBaja = findViewById<TextView>(R.id.btnPrioridadBaja)

        // 4. Conectar botones de Categoría
        val btnCatEstudios = findViewById<TextView>(R.id.btnCatEstudios)
        val btnCatTrabajo = findViewById<TextView>(R.id.btnCatTrabajo)
        val btnCatPersonal = findViewById<TextView>(R.id.btnCatPersonal)
        val btnCatCompras = findViewById<TextView>(R.id.btnCatCompras)
        val btnCatHobbies = findViewById<TextView>(R.id.btnCatHobbies)
        val btnCatOtros = findViewById<TextView>(R.id.btnCatOtros)

        // 5. Conectar selectores de Fecha, Hora, Ubicacion y foto
        val tvDate = findViewById<TextView>(R.id.tvDate)
        val tvTime = findViewById<TextView>(R.id.tvTime)
        val btnAddLocation = findViewById<TextView>(R.id.btnAddLocation)
        val btnTomarFoto = findViewById<TextView>(R.id.btnTomarFoto)
        val layoutPreviewMapa = findViewById<LinearLayout>(R.id.layoutPreviewMapa)
        val mapViewPreview = findViewById<MapView>(R.id.mapViewPreview)

        // Lista de categorías para manejarlas más fácilmente en un bucle
        val listaCategorias = listOf(btnCatEstudios, btnCatTrabajo, btnCatPersonal, btnCatCompras, btnCatHobbies, btnCatOtros)


        // --- LÓGICA DEL SELECTOR DE FECHA (DatePicker) ---
        // Al tocar el recuadro de fecha, abrimos el calendario del sistema
        tvDate.setOnClickListener {
            val calendario = java.util.Calendar.getInstance()
            val anio = calendario.get(java.util.Calendar.YEAR)
            val mes = calendario.get(java.util.Calendar.MONTH)
            val dia = calendario.get(java.util.Calendar.DAY_OF_MONTH)

            val datePickerDialog = android.app.DatePickerDialog(this, { _, yearSelected, monthSelected, daySelected ->
                val mesReal = monthSelected + 1 // Los meses empiezan en 0 en Java
                fechaSeleccionada = String.format("%02d/%02d/%04d", daySelected, mesReal, yearSelected)
                tvDate.text = fechaSeleccionada // Actualizamos la vista
            }, anio, mes, dia)

            datePickerDialog.show()
        }

        // --- LÓGICA DEL SELECTOR DE HORA (TimePicker) ---
        // Al tocar el recuadro de hora, abrimos el reloj del sistema
        tvTime.setOnClickListener {
            val calendario = java.util.Calendar.getInstance()
            val hora = calendario.get(java.util.Calendar.HOUR_OF_DAY)
            val minuto = calendario.get(java.util.Calendar.MINUTE)

            val timePickerDialog = android.app.TimePickerDialog(this, { _, hourSelected, minuteSelected ->
                // Formateamos para que siempre tenga 2 dígitos (ej: 09:05)
                val horaFormateada = String.format("%02d:%02d", hourSelected, minuteSelected)
                horaSeleccionada = horaFormateada
                tvTime.text = horaSeleccionada // Actualizamos la vista
            }, hora, minuto, false) // false indica que usamos formato de 12 horas (AM/PM)

            timePickerDialog.show()
        }


        // --- LÓGICA DE SELECCIÓN DE PRIORIDAD ---
        fun seleccionarPrioridad(prioridad: String) {
            prioridadSeleccionada = prioridad

            // Primero restablecemos todos los botones al fondo transparente/vidrio y texto blanco
            btnAlta.setBackgroundResource(R.drawable.bg_glass_input)
            btnAlta.setTextColor(Color.WHITE)
            btnMedia.setBackgroundResource(R.drawable.bg_glass_input)
            btnMedia.setTextColor(Color.WHITE)
            btnBaja.setBackgroundResource(R.drawable.bg_glass_input)
            btnBaja.setTextColor(Color.WHITE)

            // Luego aplicamos el diseño seleccionado al botón correspondiente
            when (prioridad) {
                "Alta" -> {
                    btnAlta.setBackgroundResource(R.drawable.bg_option_selected)
                    btnAlta.setTextColor(Color.parseColor("#E9407A")) // El rosa de tu diseño
                }
                "Media" -> {
                    btnMedia.setBackgroundResource(R.drawable.bg_option_selected)
                    btnMedia.setTextColor(Color.WHITE)
                }
                "Baja" -> {
                    btnBaja.setBackgroundResource(R.drawable.bg_option_selected)
                    btnBaja.setTextColor(Color.WHITE)
                }
            }
        }

        btnAlta.setOnClickListener { seleccionarPrioridad("Alta") }
        btnMedia.setOnClickListener { seleccionarPrioridad("Media") }
        btnBaja.setOnClickListener { seleccionarPrioridad("Baja") }


        // --- LÓGICA DE SELECCIÓN DE CATEGORÍA ---
        fun seleccionarCategoria(textViewSeleccionado: TextView, categoria: String) {
            categoriaSeleccionada = categoria

            // Ponemos todas las categorías con el fondo transparente/vidrio
            for (btn in listaCategorias) {
                btn.setBackgroundResource(R.drawable.bg_glass_input)
            }
            // Le ponemos el fondo destacado a la categoría que se presionó
            textViewSeleccionado.setBackgroundResource(R.drawable.bg_option_selected)
        }

        //--- LOGICA DE UBICACION INTELIGENTE ---
        btnAddLocation.setOnClickListener {
            if (latitudGuardada != null) {
                // SI YA HAY UBICACIÓN: Preguntamos si quiere eliminarla
                AlertDialog.Builder(this)
                    .setTitle("Ubicación guardada")
                    .setMessage("¿Deseas eliminar la ubicación actual?")
                    .setPositiveButton("Eliminar") { _, _ ->
                        latitudGuardada = null
                        longitudGuardada = null
                        direccionGuardada = ""

                        btnAddLocation.text = "Añadir Ubicación"
                        btnAddLocation.setBackgroundResource(R.drawable.bg_glass_input)
                        layoutPreviewMapa.visibility = View.GONE // Ocultamos el mapa
                        Toast.makeText(this, "Ubicación eliminada", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("Mantener", null)
                    .show()
            } else {
                // SI NO HAY UBICACIÓN: Buscamos y mostramos la lista
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
                                // 1. Llamamos a nuestra nueva función
                                val opciones = NominatimHelper.buscarOpciones(direccionEscrita)

                                if (opciones.isNotEmpty()) {
                                    // 2. Extraemos solo los nombres para mostrarlos en la lista
                                    val nombresOpciones = opciones.map { it.nombre }.toTypedArray()

                                    // --- 3. CREAMOS EL ADAPTADOR CON EL DISEÑO PERSONALIZADO ---
                                    val adaptador = android.widget.ArrayAdapter(
                                        this@NuevaTarea,
                                        R.layout.item_ubicacion, // Tu nuevo XML
                                        android.R.id.text1,      // El ID del texto dentro de ese XML
                                        nombresOpciones
                                    )

                                    // 4. Creamos el cuadro de diálogo usando el adaptador
                                    AlertDialog.Builder(this@NuevaTarea)
                                        .setTitle("Selecciona la ubicación correcta:")
                                        .setAdapter(adaptador) { _, indiceSeleccionado -> // <--- Usamos setAdapter

                                            // El usuario eligió una opción
                                            val lugarElegido = opciones[indiceSeleccionado]
                                            latitudGuardada = lugarElegido.latitud
                                            longitudGuardada = lugarElegido.longitud
                                            direccionGuardada = lugarElegido.nombre

                                            // Actualizamos el botón
                                            btnAddLocation.text = "📍 Ubicación lista"
                                            btnAddLocation.setBackgroundResource(R.drawable.bg_option_selected)

                                            // MOSTRAR PREVISUALIZACIÓN DEL MAPA
                                            layoutPreviewMapa.visibility = View.VISIBLE
                                            mapViewPreview.setMultiTouchControls(true)
                                            val mapController = mapViewPreview.controller
                                            mapController.setZoom(18.0)

                                            val puntoPivote = GeoPoint(latitudGuardada!!, longitudGuardada!!)
                                            mapController.setCenter(puntoPivote)

                                            // Limpiamos pines anteriores y agregamos el nuevo
                                            mapViewPreview.overlays.clear()
                                            val marcador = Marker(mapViewPreview)
                                            marcador.position = puntoPivote
                                            marcador.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                            marcador.title = direccionGuardada

                                            mapViewPreview.overlays.add(marcador)
                                            mapViewPreview.invalidate() // Refrescar mapa

                                            Toast.makeText(this@NuevaTarea, "¡Ubicación confirmada!", Toast.LENGTH_SHORT).show()
                                        }
                                        .setNegativeButton("Cancelar", null)
                                        .show()
                                } else {
                                    Toast.makeText(this@NuevaTarea, "No se encontraron resultados.", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }
        }

        //---LOGICA CAMARA---
        btnTomarFoto.setOnClickListener {
            if (urlFotoSubida != null) {
                // SI YA HAY FOTO: Le preguntamos si quiere eliminarla
                AlertDialog.Builder(this)
                    .setTitle("Foto ya adjuntada")
                    .setMessage("¿Deseas eliminar la foto actual de esta tarea?")
                    .setPositiveButton("Eliminar") { _, _ ->
                        // Reseteamos la variable
                        urlFotoSubida = null
                        uriFotoTemporal = null

                        // Devolvemos el botón a su aspecto original
                        btnTomarFoto.text = "Añadir foto"
                        btnTomarFoto.setBackgroundResource(R.drawable.bg_glass_input)
                        Toast.makeText(this, "Foto eliminada", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("Mantener", null)
                    .show()
            } else {
                // SI NO HAY FOTO: Ejecutamos tu código original de la cámara
                try {
                    val archivoTemporal = File.createTempFile(
                        "JPEG_${System.currentTimeMillis()}_",
                        ".jpg",
                        cacheDir
                    )

                    uriFotoTemporal = FileProvider.getUriForFile(
                        this,
                        "com.example.apptareas.fileprovider",
                        archivoTemporal
                    )

                    tomarFotoLauncher.launch(uriFotoTemporal!!)

                } catch (e: Exception) {
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    e.printStackTrace()
                }
            }
        }

        btnCatEstudios.setOnClickListener { seleccionarCategoria(btnCatEstudios, "Estudios") }
        btnCatTrabajo.setOnClickListener { seleccionarCategoria(btnCatTrabajo, "Trabajo") }
        btnCatPersonal.setOnClickListener { seleccionarCategoria(btnCatPersonal, "Personal") }
        btnCatCompras.setOnClickListener { seleccionarCategoria(btnCatCompras, "Compras") }
        btnCatHobbies.setOnClickListener { seleccionarCategoria(btnCatHobbies, "Hobbies") }
        btnCatOtros.setOnClickListener { seleccionarCategoria(btnCatOtros, "Otros") }


        // --- BOTÓN VOLVER ---
        btnBack.setOnClickListener { finish() }


        // --- BOTÓN CREAR TAREA FINAL ---
        btnCrearTareaFinal.setOnClickListener {
            val titulo = etTaskName.text.toString().trim()
            val descripcion = etTaskDescription.text.toString().trim()

            if (titulo.isEmpty()) {
                Toast.makeText(this, "Por favor, ingresa un título", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val currentUser = auth.currentUser
            if (currentUser != null) {
                val uid = currentUser.uid
                val idTarea = UUID.randomUUID().toString()
                // Objeto ubicacion
                val objetoUbicacion = if (latitudGuardada != null && longitudGuardada != null) {
                    mapOf(
                        "latitud" to latitudGuardada,
                        "longitud" to longitudGuardada,
                        "direccion" to direccionGuardada
                    )
                } else {
                    null
                }

                val tareaData = hashMapOf(
                    "id" to idTarea,
                    "titulo" to titulo,
                    "descripcion" to descripcion,
                    "fecha" to fechaSeleccionada,
                    "hora" to horaSeleccionada,
                    "prioridad" to prioridadSeleccionada,
                    "categoria" to categoriaSeleccionada,
                    "estado" to "pendiente",
                    "ubicacion" to objetoUbicacion,
                    "foto" to urlFotoSubida
                )

                db.collection("Usuarios").document(uid)
                    .collection("Mis_Tareas").document(idTarea)
                    .set(tareaData)
                    .addOnSuccessListener {
                        Toast.makeText(this, "¡Tarea creada con éxito!", Toast.LENGTH_SHORT).show()
                        val tiempoAlarmaMilis = NotificationUtil.parseDateToMillis(
                            fechaSeleccionada,
                            horaSeleccionada
                        )

                        if (tiempoAlarmaMilis > System.currentTimeMillis()) {
                            NotificationUtil.scheduleReminder(
                                context       = this,
                                taskId        = idTarea,
                                taskName      = titulo,
                                dueTimeMillis = tiempoAlarmaMilis
                            )
                        }
                        finish()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Error al guardar: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            } else {
                // AGREGAMOS ESTO PARA DETECTAR EL PROBLEMA
                Toast.makeText(this, "Error: No hay una sesión de usuario activa", Toast.LENGTH_LONG).show()
            }
        }
    }
}