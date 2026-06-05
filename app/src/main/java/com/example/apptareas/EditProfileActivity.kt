package com.example.apptareas

import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore

class EditProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.edit_profile)

        auth = Firebase.auth
        db = Firebase.firestore

        val etName     = findViewById<TextInputEditText>(R.id.etName)
        val etEmail    = findViewById<TextInputEditText>(R.id.etEmail)
        val etPassword = findViewById<TextInputEditText>(R.id.etPassword)
        val tilName    = findViewById<TextInputLayout>(R.id.tilName)
        val btnSave    = findViewById<Button>(R.id.btnSave)
        val btnBack    = findViewById<ImageButton>(R.id.btnBack)

        val currentUser = auth.currentUser

        //prellenar datos
        if (currentUser != null) {
            etEmail.setText(currentUser.email)

            db.collection("Usuarios").document(currentUser.uid).get()
                .addOnSuccessListener { doc ->
                    etName.setText(doc.getString("nombre") ?: "")
                }
        }

        //volver
        btnBack.setOnClickListener { finish() }

        //para guardar
        btnSave.setOnClickListener {
            val nuevoNombre   = etName.text.toString().trim()
            val nuevaPassword = etPassword.text.toString().trim()

            if (nuevoNombre.isEmpty()) {
                tilName.error = "El nombre no puede estar vacío"
                return@setOnClickListener
            }
            tilName.error = null

            val uid = currentUser?.uid ?: return@setOnClickListener

            db.collection("Usuarios").document(uid)
                .update("nombre", nuevoNombre)
                .addOnSuccessListener {
                    if (nuevaPassword.isNotEmpty()) {
                        if (nuevaPassword.length < 6) {
                            etPassword.error = "Mínimo 6 caracteres"
                            return@addOnSuccessListener
                        }
                        currentUser.updatePassword(nuevaPassword)
                            .addOnSuccessListener {
                                Toast.makeText(this, "Perfil actualizado", Toast.LENGTH_SHORT).show()
                                finish()
                            }
                            .addOnFailureListener {
                                Toast.makeText(this, "Error al cambiar contraseña", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        Toast.makeText(this, "Perfil actualizado", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error al guardar", Toast.LENGTH_SHORT).show()
                }
        }
    }
}