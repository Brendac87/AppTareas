package com.example.apptareas

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
class LoginActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login)

        auth = FirebaseAuth.getInstance()

        val emailEditText = findViewById<TextInputEditText>(R.id.etEmail)
        val passwordEditText = findViewById<TextInputEditText>(R.id.etpassword)
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
        //-- BOTON REGISTRARSE--
        val tvSignUp = findViewById<TextView>(R.id.tvSignUp)
        tvSignUp.setOnClickListener {
            val intent = Intent(this, CreateProfileActivity::class.java)
            startActivity(intent)
        }
        val tvForgotPassword = findViewById<TextView>(R.id.forgotPassword)
        tvForgotPassword.setOnClickListener {
            mostrarDialogoRecuperacion()
        }
    }
    private fun mostrarDialogoRecuperacion() {
        val input = android.widget.EditText(this).apply {
            hint = "tu@email.com"
            inputType = android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            setPadding(50, 30, 50, 30)
        }

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Recuperar contraseña")
            .setMessage("Ingresá tu email y te enviamos un link.")
            .setView(input)
            .setPositiveButton("Enviar") { _, _ ->
                val email = input.text.toString().trim()
                if (email.isNotEmpty()) {
                    enviarEmailRecuperacion(email)
                } else {
                    Toast.makeText(this, "Ingresá un email", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun enviarEmailRecuperacion(email: String) {
        auth.sendPasswordResetEmail(email)
            .addOnSuccessListener {
                Toast.makeText(
                    this,
                    "Si el correo está registrado, recibirás un enlace para restablecer tu contraseña.",
                    Toast.LENGTH_LONG
                ).show()
            }
            .addOnFailureListener {
                Toast.makeText(
                    this,
                    "No encontramos una cuenta con ese email",
                    Toast.LENGTH_LONG
                ).show()
            }
    }
}