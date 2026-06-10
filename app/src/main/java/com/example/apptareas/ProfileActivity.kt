package com.example.apptareas

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.google.firebase.Firebase
import androidx.appcompat.app.AppCompatDelegate
import androidx.cardview.widget.CardView
import androidx.core.os.LocaleListCompat

class ProfileActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.profile)

        auth = Firebase.auth
        db = Firebase.firestore

        val userNameTextView = findViewById<TextView>(R.id.userName)
        val userEmailTextView = findViewById<TextView>(R.id.userEmail)

        // --- NUEVO: Conectamos las vistas de las estadísticas ---
        val tvStatDone = findViewById<TextView>(R.id.tvStatDone)
        val tvStatPending = findViewById<TextView>(R.id.tvStatPending)

        // Usuario
        val currentUser = auth.currentUser

        if (currentUser != null) {
            // El usuario está logueado, obtenemos su UID
            val uid = currentUser.uid

            // 1. Obtener datos del perfil (Tu código original)
            db.collection("Usuarios").document(uid).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {

                        val nombre = document.getString("nombre") ?: "Usuario"
                        val email = document.getString("email") ?: currentUser.email

                        userNameTextView.text = nombre
                        userEmailTextView.text = email

                        val iniciales = nombre.take(2).uppercase()
                        findViewById<TextView>(R.id.tvInitials).text = iniciales

                    } else {
                        Toast.makeText(this, "No se encontró el perfil del usuario", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e("FirestoreError", "Error al obtener datos", exception)
                    Toast.makeText(this, "Error al cargar el perfil", Toast.LENGTH_SHORT).show()
                }

            // --- NUEVO: 2. Obtener y calcular las estadísticas de las tareas ---
            db.collection("Usuarios").document(uid).collection("Mis_Tareas")
                .get()
                .addOnSuccessListener { resultado ->
                    var completadas = 0
                    var pendientes = 0

                    for (documento in resultado) {
                        val estado = documento.getString("estado") ?: "pendiente"

                        if (estado.equals("completado", ignoreCase = true) || estado.equals("completada", ignoreCase = true)) {
                            completadas++
                        } else if (estado.equals("pendiente", ignoreCase = true)) {
                            pendientes++
                        }
                    }

                    // Actualizamos la interfaz
                    tvStatDone.text = completadas.toString()
                    tvStatPending.text = pendientes.toString()
                }
                .addOnFailureListener {
                    Log.e("FirestoreError", "Error al obtener estadísticas")
                }

        } else {
            Toast.makeText(this, "Sesión no iniciada", Toast.LENGTH_SHORT).show()
        }

        // --- NAVEGACIÓN Y BOTONES ---
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.selectedItemId = R.id.nav_profile
        NavigationUtils.configurarNavegacion(this, bottomNav)

        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnEditProfile)
            .setOnClickListener {
                startActivity(Intent(this, EditProfileActivity::class.java))
            }

        // notificaciones es un toast de momento
        findViewById<androidx.cardview.widget.CardView>(R.id.optNotifications)
            .setOnClickListener {
                Toast.makeText(this, "Próximamente", Toast.LENGTH_SHORT).show()
            }

        // para ayuda un dialogo simple
        findViewById<androidx.cardview.widget.CardView>(R.id.optHelp)
            .setOnClickListener {
                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Ayuda")
                    .setMessage("Para dudas o soporte, escribinos a:\n\nsoporte@apptareas.com")
                    .setPositiveButton("Cerrar", null)
                    .show()
            }
        val optLanguage = findViewById<CardView>(R.id.optLanguage) // O usa ViewBinding

        optLanguage.setOnClickListener {
            // Aquí puedes mostrar un diálogo simple para elegir
            val languages = arrayOf("Español", "English")
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Seleccionar Idioma")
            builder.setItems(languages) { _, which ->
                val localeTag = if (which == 0) "es" else "en"
                val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags(localeTag)
                // Esto cambia el idioma y recrea la actividad automáticamente
                AppCompatDelegate.setApplicationLocales(appLocale)
            }
            builder.show()
        }

        // cerrar sesion
        findViewById<androidx.cardview.widget.CardView>(R.id.optLogout)
            .setOnClickListener {
                FirebaseAuth.getInstance().signOut()
                val intent = Intent(this, LoginActivity::class.java)
                // limpia el historial de actividades asi no puede volver atras usando el boton del celular
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            }
    }
}
