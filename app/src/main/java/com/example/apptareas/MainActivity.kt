package com.example.apptareas

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //Inicializar Firebase
        auth = Firebase.auth

        //Comprobar si el usuario ya inició sesión previamente (Sesión Persistente)
        if (auth.currentUser == null) {
            // No hay sesión activa redirigir al Login
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        // SESION ACTIVA. Cargamos la pantalla principal
        setContentView(R.layout.profile)

        // ... a partir de aquí puedes poner la lógica normal de tu MainActivity
        // (configurar botones, cargar las tareas, etc.)
    }
}