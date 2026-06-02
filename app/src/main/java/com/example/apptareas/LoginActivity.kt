package com.example.apptareas

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

class LoginActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login)

        auth = Firebase.auth

        val emailEditText = findViewById<TextInputEditText>(R.id.tilEmail)
        val passwordEditText = findViewById<TextInputEditText>(R.id.tilEmail)
        val btnLogin = findViewById<Button>(R.id.btnLogin)

        btnLogin.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // ¡Login exitoso! Navegamos a la pantalla principal
                        // Cambia "MainActivity" por el nombre de la Activity a la que quieras ir
                        val intent = Intent(this, MainActivity::class.java)
                        startActivity(intent)

                        // Cerramos esta pantalla para que no pueda volver atrás al login
                        finish()
                    } else {
                        // Error (contraseña incorrecta, usuario no existe, etc.)
                        val mensajeError = task.exception?.message ?: "Error al iniciar sesión"
                        Toast.makeText(this, mensajeError, Toast.LENGTH_LONG).show()
                    }
                }
        }
    }
}