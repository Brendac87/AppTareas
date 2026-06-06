package com.example.apptareas

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class DetalleTarea : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.detalle_tarea)

        // 1. Inicializar Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // 2. Conectar las vistas de tu XML
        val btnBack = findViewById<ImageButton>(R.id.btn_back)
        val tvTitulo = findViewById<TextView>(R.id.tv_titulo_tarea)
        val tvDescripcion = findViewById<TextView>(R.id.tv_descripcion_tarea)
        val tvPrioridad = findViewById<TextView>(R.id.tv_prioridad)
        val tvCategoria = findViewById<TextView>(R.id.tv_categoria)
        val tvEstado = findViewById<TextView>(R.id.tv_estado)
        val tvFecha = findViewById<TextView>(R.id.tv_fecha)
        val tvHora = findViewById<TextView>(R.id.tv_hora)

        // --- Conectamos el botón de Completar ---
        val btnCompletar = findViewById<Button>(R.id.btn_completar)

        // Configurar el botón de volver
        btnBack.setOnClickListener { finish() }

        // 3. CAMBIO: Obtener el ID seguro que mandamos desde la pantalla anterior
        val idRecibido = intent.getStringExtra("ID_DE_LA_TAREA")

        if (idRecibido != null) {
            val currentUser = auth.currentUser
            if (currentUser != null) {
                val uid = currentUser.uid

                // 4. CAMBIO: Buscar en Firestore directamente por el ID del documento
                db.collection("Usuarios").document(uid)
                    .collection("Mis_Tareas").document(idRecibido) // Búsqueda directa
                    .get()
                    .addOnSuccessListener { tarea ->

                        // Verificamos si el documento existe directamente
                        if (tarea.exists()) {

                            // Guardamos el ID del documento en Firebase para poder actualizarlo luego
                            val idDocumento = tarea.id

                            // 5. Reemplazamos los textos
                            tvTitulo.text = tarea.getString("titulo") ?: "Sin título"
                            tvDescripcion.text = tarea.getString("descripcion") ?: "Sin descripción"
                            tvPrioridad.text = tarea.getString("prioridad") ?: "Baja"
                            tvCategoria.text = tarea.getString("categoria") ?: "Otros"
                            tvFecha.text = tarea.getString("fecha") ?: "--/--/----"
                            tvHora.text = tarea.getString("hora") ?: "--:--"

                            // --- LÓGICA DEL ESTADO Y BOTÓN ---
                            val estadoActual = tarea.getString("estado") ?: "pendiente"
                            tvEstado.text = estadoActual.uppercase() // Lo ponemos en MAYÚSCULAS

                            // Ignoramos mayúsculas/minúsculas por si acaso
                            if (estadoActual.equals("completado", ignoreCase = true) || estadoActual.equals("completada", ignoreCase = true)) {
                                // Si está completada: Color verde vibrante y ocultamos el botón
                                tvEstado.setTextColor(Color.parseColor("#4CAF50"))
                                btnCompletar.visibility = View.GONE
                            } else {
                                // Si está pendiente: Color amarillo y mostramos el botón
                                tvEstado.setTextColor(Color.parseColor("#FFC107"))
                                btnCompletar.visibility = View.VISIBLE
                            }

                            // --- FUNCIONALIDAD DEL BOTÓN COMPLETAR ---
                            btnCompletar.setOnClickListener {
                                // Le decimos a Firebase: "Actualiza el campo 'estado' a 'completado'"
                                db.collection("Usuarios").document(uid)
                                    .collection("Mis_Tareas").document(idDocumento)
                                    .update("estado", "completado")
                                    .addOnSuccessListener {
                                        Toast.makeText(this, "¡Tarea completada!", Toast.LENGTH_SHORT).show()

                                        // Volvemos automáticamente a la lista de tareas
                                        finish()
                                    }
                                    .addOnFailureListener { error ->
                                        Toast.makeText(this, "Error al actualizar: ${error.message}", Toast.LENGTH_SHORT).show()
                                    }
                            }

                        } else {
                            Toast.makeText(this, "No se encontró la tarea en la base de datos", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .addOnFailureListener { error ->
                        Toast.makeText(this, "Error de conexión: ${error.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        } else {
            Toast.makeText(this, "Error: No llegó el ID de la tarea", Toast.LENGTH_SHORT).show()
        }
    }
}