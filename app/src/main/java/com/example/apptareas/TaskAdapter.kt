package com.example.apptareas

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TaskAdapter(
    private var tasks: List<Task>,
    private val onTaskClick: (String) -> Unit
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    class TaskViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView = view.findViewById(R.id.tvItemTitle)
        val tvDate: TextView = view.findViewById(R.id.tvItemDate)
        val tvStatus: TextView = view.findViewById(R.id.tvItemStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_task, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = tasks[position]

        holder.tvTitle.text = task.titulo
        holder.tvDate.text = task.fecha
        holder.tvStatus.text = task.estado.uppercase()

        when (task.estado.lowercase()) {
            "pendiente" -> holder.tvStatus.setTextColor(Color.parseColor("#FFC107"))
            "completado", "completada" -> holder.tvStatus.setTextColor(Color.parseColor("#4CAF50"))
            "cancelado", "cancelada" -> holder.tvStatus.setTextColor(Color.parseColor("#F44336"))
            else -> holder.tvStatus.setTextColor(Color.WHITE)
        }

        // --- EL ÚNICO CAMBIO ES AQUÍ ABAJO ---
        holder.itemView.setOnClickListener {
            onTaskClick(task.id) // Le pasamos el ID único a la mochila, no el título
        }
    }

    override fun getItemCount() = tasks.size

    fun actualizarLista(nuevaLista: List<Task>) {
        this.tasks = nuevaLista
        notifyDataSetChanged()
    }
}