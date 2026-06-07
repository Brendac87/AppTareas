package com.example.apptareas

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

data class AppNotification(
    val id       : String = "",
    val iconRes  : Int    = R.drawable.ic_alarm,
    val iconBg   : Int    = 0x1AEC4899.toInt(),
    val title    : String = "",
    val subtitle : String = "",
    val time     : String = "",
    val isUnread : Boolean = true,
    val taskId   : String = ""
)

class NotificationsActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var adapter: NotificationAdapter

    private val allNotifications = mutableListOf<AppNotification>()
    private var showingAll = true


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.notification)

        db   = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        setupRecyclerView()
        setupFilters()
        setupClickListeners()
        setupBottomNav()
        cargarNotificaciones()
    }

    //RecyclerView
    private fun setupRecyclerView() {
        adapter = NotificationAdapter(mutableListOf()) { notif ->
            marcarComoLeida(notif.id)

            // Si tiene tarea asociada, va al detalle
            if (notif.taskId.isNotEmpty()) {
                val intent = Intent(this, DetalleTarea::class.java)
                intent.putExtra("TITULO_DE_LA_TAREA", notif.title)
                startActivity(intent)
            }
        }
        findViewById<RecyclerView>(R.id.rvNotifications).adapter = adapter
    }

    // filtros Todas / No leídas
    private fun setupFilters() {
        val filterAll    = findViewById<TextView>(R.id.filterAll)
        val filterUnread = findViewById<TextView>(R.id.filterUnread)

        filterAll.setOnClickListener {
            showingAll = true
            filterAll.setBackgroundResource(R.drawable.bg_f_active)
            filterAll.setTextColor(0xFFFFFFFF.toInt())
            filterUnread.setBackgroundResource(R.drawable.bg_f_inactive)
            filterUnread.setTextColor(0x80FFFFFF.toInt())
            actualizarLista()
        }

        filterUnread.setOnClickListener {
            showingAll = false
            filterUnread.setBackgroundResource(R.drawable.bg_f_active)
            filterUnread.setTextColor(0xFFFFFFFF.toInt())
            filterAll.setBackgroundResource(R.drawable.bg_f_inactive)
            filterAll.setTextColor(0x80FFFFFF.toInt())
            actualizarLista()
        }
    }

    // click listeners
    private fun setupClickListeners() {


        findViewById<TextView>(R.id.btnMarkAllRead).setOnClickListener {
            marcarTodasLeidas()
        }
    }

    //nav
    private fun setupBottomNav() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        NavigationUtils.configurarNavegacion(this, bottomNav)
    }

    //carga desde Firestore las  notificaciones NO leídas
    private fun cargarNotificaciones() {
        val uid = auth.currentUser?.uid ?: return

        db.collection("Usuarios").document(uid)
            .collection("Notificaciones")
            .whereEqualTo("leida", false)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot == null) return@addSnapshotListener

                allNotifications.clear()

                for (doc in snapshot.documents) {
                    val notif = AppNotification(
                        id       = doc.id,
                        iconRes  = R.drawable.ic_alarm,
                        iconBg   = 0x1AEC4899.toInt(),
                        title    = doc.getString("titulo")   ?: "",
                        subtitle = doc.getString("subtitle") ?: "",
                        time     = calcularTiempo(doc.getLong("timestamp")),
                        isUnread = true,
                        taskId   = doc.getString("taskId")   ?: ""
                    )
                    allNotifications.add(notif)
                }

                actualizarLista()
                actualizarEstadoVacio()
            }
    }

    //Filtra
    private fun actualizarLista() {
        val filtrada = if (showingAll) {
            allNotifications.toMutableList()
        } else {
            allNotifications.filter { it.isUnread }.toMutableList()
        }
        adapter.updateList(filtrada)
        actualizarEstadoVacio()
    }

    //estado vacia
    private fun actualizarEstadoVacio() {
        val empty = adapter.itemCount == 0
        findViewById<View>(R.id.emptyState).visibility =
            if (empty) View.VISIBLE else View.GONE
        findViewById<RecyclerView>(R.id.rvNotifications).visibility =
            if (empty) View.GONE else View.VISIBLE
    }

    //narca una como leída, desaparece de la lista
    private fun marcarComoLeida(notifId: String) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("Usuarios").document(uid)
            .collection("Notificaciones").document(notifId)
            .update("leida", true)
        //listener detecta el cambio y actualiza la lista solo
    }

    //marca todas como leídas
    private fun marcarTodasLeidas() {
        val uid = auth.currentUser?.uid ?: return
        val batch = db.batch()

        allNotifications.forEach { notif ->
            val ref = db.collection("Usuarios").document(uid)
                .collection("Notificaciones").document(notif.id)
            batch.update(ref, "leida", true)
        }

        batch.commit()
        //el listener detecta los cambios y vacía la lista solo
    }

    //calcula tiempo relativo
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



//adaptador

class NotificationAdapter(
    private val items: MutableList<AppNotification>,
    private val onClick: (AppNotification) -> Unit
) : RecyclerView.Adapter<NotificationAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val card     : CardView  = view.findViewById(R.id.cardNotification)
        val cvIcon   : CardView  = view.findViewById(R.id.cvIcon)
        val ivIcon   : ImageView = view.findViewById(R.id.ivIcon)
        val tvTitle  : TextView  = view.findViewById(R.id.tvTitle)
        val tvSub    : TextView  = view.findViewById(R.id.tvSubtitle)
        val tvTime   : TextView  = view.findViewById(R.id.tvTime)
        val dotUnread: View      = view.findViewById(R.id.dotUnread)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val notif = items[position]

        holder.tvTitle.text = notif.title
        holder.tvSub.text   = notif.subtitle
        holder.tvTime.text  = notif.time

        holder.ivIcon.setImageResource(notif.iconRes)
        holder.cvIcon.setCardBackgroundColor(notif.iconBg)

        holder.card.setCardBackgroundColor(
            if (notif.isUnread) 0x1AEC4899.toInt()
            else 0x12FFFFFF.toInt()
        )

        holder.dotUnread.visibility =
            if (notif.isUnread) View.VISIBLE else View.INVISIBLE

        holder.card.setOnClickListener { onClick(notif) }
    }

    override fun getItemCount() = items.size

    fun updateList(newList: MutableList<AppNotification>) {
        items.clear()
        items.addAll(newList)
        notifyDataSetChanged()
    }
}