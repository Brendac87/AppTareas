package com.example.apptareas

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()

        // 1. Si NO hay sesión activa, mandamos al Login
        if (auth.currentUser == null) {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        // 2. ¡CAMBIO AQUÍ! Si SÍ hay sesión activa, mandamos directo a HomeActivity
        val intent = Intent(this, HomeActivity::class.java)
        startActivity(intent)
        finish()
    }
}