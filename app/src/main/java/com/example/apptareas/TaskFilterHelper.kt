package com.example.apptareas

// Usamos 'object' en lugar de 'class' para poder llamarlo desde cualquier lado fácilmente
object TaskFilterHelper {

    fun filtrarTareas(
        listaOriginal: List<Task>,
        busqueda: String,
        filtroActivo: String
    ): List<Task> {

        var listaFiltrada = listaOriginal

        // 1. Aplicar la búsqueda por texto (ignora mayúsculas/minúsculas)
        if (busqueda.isNotEmpty()) {
            listaFiltrada = listaFiltrada.filter { tarea ->
                tarea.titulo.contains(busqueda, ignoreCase = true)
            }
        }

        // 2. Aplicar el filtro de los botones (Chips)
        listaFiltrada = when (filtroActivo) {
            "Pendientes" -> listaFiltrada.filter { it.estado.equals("pendiente", ignoreCase = true) }
            "Completadas" -> listaFiltrada.filter { it.estado.equals("completado", ignoreCase = true) || it.estado.equals("completada", ignoreCase = true) }
            "Canceladas" -> listaFiltrada.filter { it.estado.equals("cancelado", ignoreCase = true) || it.estado.equals("cancelada", ignoreCase = true) }
            "Alta" -> listaFiltrada.filter { it.prioridad.equals("Alta", ignoreCase = true) }
            "Media" -> listaFiltrada.filter { it.prioridad.equals("Media", ignoreCase = true) }
            "Baja" -> listaFiltrada.filter { it.prioridad.equals("Baja", ignoreCase = true) }
            else -> listaFiltrada // Si es "Todas", no filtramos nada más
        }

        return listaFiltrada
    }
}