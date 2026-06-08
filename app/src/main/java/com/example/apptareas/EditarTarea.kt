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
import java.util.Calendar

class EditarTarea : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    // Variables dinámicas para guardar lo que el usuario elija (o lo que venga de la BD)
    private var prioridadSeleccionada = "Alta"
    private var categoriaSeleccionada = "Otros"
    private var fechaSeleccionada = "Sin fecha"
    private var horaSeleccionada = "Sin hora"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.editar_tarea)

        // 1. Inicializar Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // 2. Conectar vistas principales
        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        val etTaskName = findViewById<EditText>(R.id.etTaskName)
        val etTaskDescription = findViewById<EditText>(R.id.etTaskDescription)
        val tvDate = findViewById<TextView>(R.id.tvDate)
        val tvTime = findViewById<TextView>(R.id.tvTime)
        val btnGuardarCambios = findViewById<Button>(R.id.btnGuardarCambios)

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

        val listaCategorias = listOf(btnCatEstudios, btnCatTrabajo, btnCatPersonal, btnCatCompras, btnCatHobbies, btnCatOtros)

        // --- FUNCIONES VISUALES ---

        // Función para cambiar el color de la prioridad
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

        // Función para cambiar el color de la categoría
        fun seleccionarCategoria(textViewSeleccionado: TextView, categoria: String) {
            categoriaSeleccionada = categoria

            for (btn in listaCategorias) {
                btn.setBackgroundResource(R.drawable.bg_glass_input)
            }
            textViewSeleccionado.setBackgroundResource(R.drawable.bg_option_selected)
        }

        // Asignar clics a los botones
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

                // --- CAMBIO AQUÍ: Formato estricto forzado a 2 dígitos ---
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
                        // Rellenar EditTexts
                        etTaskName.setText(tarea.getString("titulo") ?: "")
                        etTaskDescription.setText(tarea.getString("descripcion") ?: "")

                        // Rellenar Fecha y Hora
                        fechaSeleccionada = tarea.getString("fecha") ?: "Sin fecha"
                        tvDate.text = fechaSeleccionada
                        horaSeleccionada = tarea.getString("hora") ?: "Sin hora"
                        tvTime.text = horaSeleccionada

                        // Rellenar y activar colores de Prioridad
                        val prioridadBD = tarea.getString("prioridad") ?: "Baja"
                        seleccionarPrioridad(prioridadBD)

                        // Rellenar y activar colores de Categoría
                        val categoriaBD = tarea.getString("categoria") ?: "Otros"
                        when (categoriaBD.lowercase()) {
                            "estudios" -> seleccionarCategoria(btnCatEstudios, "Estudios")
                            "trabajo" -> seleccionarCategoria(btnCatTrabajo, "Trabajo")
                            "personal" -> seleccionarCategoria(btnCatPersonal, "Personal")
                            "compras" -> seleccionarCategoria(btnCatCompras, "Compras")
                            "hobbies" -> seleccionarCategoria(btnCatHobbies, "Hobbies")
                            else -> seleccionarCategoria(btnCatOtros, "Otros")
                        }
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error al cargar datos", Toast.LENGTH_SHORT).show()
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

                // Usamos un mapa solo con los campos que queremos actualizar
                val actualizaciones = mapOf(
                    "titulo" to nuevoTitulo,
                    "descripcion" to nuevaDesc,
                    "fecha" to fechaSeleccionada,
                    "hora" to horaSeleccionada,
                    "prioridad" to prioridadSeleccionada,
                    "categoria" to categoriaSeleccionada
                )

                db.collection("Usuarios").document(uid).collection("Mis_Tareas").document(idRecibido)
                    .update(actualizaciones) // <--- Actualiza sin borrar el campo 'estado' o 'id'
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