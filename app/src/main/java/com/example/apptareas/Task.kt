package com.example.apptareas

// Esta clase es como un "molde" que guarda toda la info de una tarea junta
data class Task(
    val id: String,
    val titulo: String,
    val fecha: String,
    val estado: String
)