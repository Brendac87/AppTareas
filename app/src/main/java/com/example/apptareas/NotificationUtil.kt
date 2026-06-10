package com.example.apptareas

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

object NotificationUtil {

    private const val CHANNEL_ID   = "task_channel"
    private const val CHANNEL_NAME = "Recordatorios de tareas"

    //crear el canal,llamarlo una sola vez en homeActivity
    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notificaciones de tareas por vencer"
            enableVibration(true)
        }

        context.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    //para programar notificaciones 30 min antes dl vencimiento
    fun scheduleReminder(
        context:       Context,
        taskId:        String,
        taskName:      String,
        dueTimeMillis: Long
    ) {

        //30 antes
        val triggerAt = dueTimeMillis - (30 * 60 * 1000)

        //si ya paso entonces no programar
        if (triggerAt < System.currentTimeMillis()) return

        val intent = Intent(context, TaskReceiver::class.java).apply {
            putExtra("task_name", taskName)
            putExtra("task_id",   taskId)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            taskId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(AlarmManager::class.java)

        //verifica la version
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent
                )
            } else {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent
                )

            }
        } else {
            //Android 11 o menor, siempre puede usar alarma exacta
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent
            )

        }
    }

    //cancelar, cuando se completa o cuando se elimine la tarea
    fun cancelReminder(context: Context, taskId: String) {
        val intent = Intent(context, TaskReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            taskId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        context.getSystemService(AlarmManager::class.java).cancel(pendingIntent)
    }


    //parsear fecha a milisegundos, convierte "05/06/2026 14:30"/entiende el AlarmManager
    fun parseDateToMillis(date: String, time: String): Long {
        return try {
            val format = java.text.SimpleDateFormat(
                "dd/MM/yyyy HH:mm",
                java.util.Locale.getDefault()
            )
            format.parse("$date $time")?.time ?: 0L
        } catch (e: Exception) {

            0L
        }
    }
}