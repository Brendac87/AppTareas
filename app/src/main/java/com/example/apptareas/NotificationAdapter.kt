package com.example.apptareas

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView

class NotificationAdapter(
    private val items: MutableList<Notification>,
    private val onClick: (Notification) -> Unit
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
        holder.card.setCardBackgroundColor(0x1AEC4899.toInt())
        holder.dotUnread.visibility = View.VISIBLE

        holder.card.setOnClickListener { onClick(notif) }
    }

    override fun getItemCount() = items.size

    fun updateList(newList: MutableList<Notification>) {
        items.clear()
        items.addAll(newList)
        notifyDataSetChanged()
    }
}