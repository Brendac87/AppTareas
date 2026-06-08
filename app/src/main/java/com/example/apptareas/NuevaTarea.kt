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

class NuevaTarea : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    // Variables dinámicas para guardar lo que el usuario elija
    private var prioridadSeleccionada = "Alta"
    private var categoriaSeleccionada = "Estudios"
    // --- VARIABLES NUEVAS PARA FECHA Y HORA ---
    private var fechaSeleccionada = "Sin fecha"
    private var horaSeleccionada = "Sin hora"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

        // 5. Conectar selectores de Fecha y Hora (NUEVO)
        val tvDate = findViewById<TextView>(R.id.tvDate)
        val tvTime = findViewById<TextView>(R.id.tvTime)

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

                val tareaData = hashMapOf(
                    "id" to idTarea,
                    "titulo" to titulo,
                    "descripcion" to descripcion,
                    "fecha" to fechaSeleccionada,
                    "hora" to horaSeleccionada,
                    "prioridad" to prioridadSeleccionada,
                    "categoria" to categoriaSeleccionada,
                    "estado" to "pendiente",
                    "ubicacion" to null,
                    "foto" to null
                )

                db.collection("Usuarios").document(uid)
                    .collection("Mis_Tareas").document(idTarea)
                    .set(tareaData)
                    .addOnSuccessListener {
                        Toast.makeText(this, "¡Tarea creada con éxito!", Toast.LENGTH_SHORT).show()
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