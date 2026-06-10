package com.example.apptareas

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.json.JSONArray
import androidx.core.content.edit

class NotificationActivity : AppCompatActivity() {

    private lateinit var adapter: NotificationAdapter
    private val notifications = mutableListOf<Notification>()

    companion object { const val MAX_NOTIFICACIONES = 6 }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.notification)

        setupRecyclerView()
        setupClickListeners()
        setupBottomNav()
        cargarNotificaciones()
    }

    override fun onResume() {
        super.onResume()
        cargarNotificaciones()
    }

    //RecyclerView
    private fun setupRecyclerView() {
        adapter = NotificationAdapter(mutableListOf()) { notif ->
            marcarComoLeida(notif.id)
            if (notif.taskId.isNotEmpty()) {
                val intent = Intent(this, DetalleTarea::class.java)
                intent.putExtra("ID_DE_LA_TAREA", notif.taskId)
                startActivity(intent)
            }
        }
        findViewById<RecyclerView>(R.id.rvNotifications).adapter = adapter
    }

    //click listeners
    private fun setupClickListeners() {


        findViewById<TextView>(R.id.btnMarkAllRead).setOnClickListener {
            marcarTodasLeidas()
        }
    }

    //Bottom nav
    private fun setupBottomNav() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.selectedItemId = R.id.nav_not
        NavigationUtils.configurarNavegacion(this, bottomNav)


    }

    //carga las notif max 6
    private fun cargarNotificaciones() {
        val prefs = getSharedPreferences("notificaciones", MODE_PRIVATE)
        val array = JSONArray(prefs.getString("lista", "[]"))

        notifications.clear()

        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)


            if (!obj.optBoolean("leida", false)) {
                notifications.add(
                    Notification(
                        id       = obj.optString("id", ""),
                        iconRes  = R.drawable.ic_alarm,
                        iconBg   = 0x1AEC4899,
                        title    = obj.optString("titulo", "Sin título"),
                        subtitle = obj.optString("subtitle", ""),
                        time     = calcularTiempo(obj.optLong("timestamp", System.currentTimeMillis())),
                        isUnread = true,
                        taskId   = obj.optString("taskId", "")
                    )
                )
            }
            //lmite de 6 notificaciones visibles
            if (notifications.size >= MAX_NOTIFICACIONES) break
        }

        adapter.updateList(notifications)
        actualizarContador()
        actualizarEstadoVacio()
    }

    //contador "1 de 6 notificaciones"
    private fun actualizarContador() {
        val tvCount = findViewById<TextView>(R.id.tvCount)
        tvCount.text = when (notifications.size) {
            0    -> ""
            1    -> "1 notificación"
            else -> "${notifications.size} de $MAX_NOTIFICACIONES notificaciones"
        }
    }

    //cuando no hay notif, estado vacío
    private fun actualizarEstadoVacio() {
        val empty = notifications.isEmpty()
        findViewById<View>(R.id.emptyState).visibility =
            if (empty) View.VISIBLE else View.GONE
        findViewById<RecyclerView>(R.id.rvNotifications).visibility =
            if (empty) View.GONE else View.VISIBLE
        findViewById<TextView>(R.id.btnMarkAllRead).visibility =
            if (empty) View.GONE else View.VISIBLE
    }

    //marca una como leída, despues desaparece
    private fun marcarComoLeida(notifId: String) {
        val prefs = getSharedPreferences("notificaciones", MODE_PRIVATE)
        val array = JSONArray(prefs.getString("lista", "[]"))

        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            if (obj.getString("id") == notifId) {
                obj.put("leida", true)
                break
            }
        }

        prefs.edit { putString("lista", array.toString()) }
        cargarNotificaciones()
    }

    //marca todas como leídas, vacia la lista vacía
    private fun marcarTodasLeidas() {
        val prefs = getSharedPreferences("notificaciones", MODE_PRIVATE)
        val array = JSONArray(prefs.getString("lista", "[]"))

        for (i in 0 until array.length()) {
            array.getJSONObject(i).put("leida", true)
        }

        prefs.edit { putString("lista", array.toString()) }
        cargarNotificaciones()
    }

    //calcular tiempo relativo
    private fun calcularTiempo(timestamp: Long?): String {
        if (timestamp == null) return "hace un momento"
        val diff = System.currentTimeMillis() - timestamp
        return when {
            diff < 60_000     -> "hace un momento"
            diff < 3_600_000  -> "hace ${diff / 60_000} min"
            diff < 86_400_000 -> "hace ${diff / 3_600_000} h"
            else              -> "hace ${diff / 86_400_000} días"
        }
    }
}