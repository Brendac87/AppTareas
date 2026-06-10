package com.example.apptareas

//clase molde, guarda toda la info de una notification junta

data class Notification(
    val id       : String  = "",
    val iconRes  : Int     = R.drawable.ic_alarm,
    val iconBg   : Int     = 0x1AEC4899,
    val title    : String  = "",
    val subtitle : String  = "",
    val time     : String  = "",
    val isUnread : Boolean = true,
    val taskId   : String  = ""
)