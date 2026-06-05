package com.example.apptareas
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.google.firebase.Firebase

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
        //Usuario
        val currentUser = auth.currentUser

        if (currentUser != null) {
            // El usuario está logueado, obtenemos su UID
            val uid = currentUser.uid

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
        } else {
            Toast.makeText(this, "Sesión no iniciada", Toast.LENGTH_SHORT).show()
        }
        
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.selectedItemId = R.id.nav_profile
        NavigationUtils.configurarNavegacion(this, bottomNav)
        
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnEditProfile)
            .setOnClickListener {
                startActivity(Intent(this, EditProfileActivity::class.java))
            }

    }
}

