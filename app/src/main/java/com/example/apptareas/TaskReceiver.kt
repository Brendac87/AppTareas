package com.example.apptareas

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build

import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

class TaskReceiver : BroadcastReceiver() {


    override fun onReceive(context: Context, intent: Intent) {
        val taskName = intent.getStringExtra("task_name") ?: "Tarea pendiente"
        val taskId = intent.getStringExtra("task_id") ?: ""

        //al tocar la notificacion abre la app
        val openAppIntent = Intent(context, HomeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("task_id", taskId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            taskId.hashCode(),
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        //builder para la notificacion
        val notification = NotificationCompat.Builder(context, "task_channel")
            .setSmallIcon(R.drawable.ic_not)
            .setContentTitle("Tarea por vencer")
            .setContentText(taskName)
            .setSubText("Vence en 30 minutos")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVibrate(longArrayOf(0, 300, 100, 300))
            .build()

        //validacion interna:
        // Android 13+ y si se tiene el permiso en este momento
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                //se tiene permiso, mostrar la notificacion
                NotificationManagerCompat.from(context).notify(taskId.hashCode(), notification)
            } else {
                //no tiene permiso.
                //el usuario lo denego no se hace nada.
            }
        } else {
            //Android 12 o inferior, el permiso  concedido al instalar la app
            NotificationManagerCompat.from(context).notify(taskId.hashCode(), notification)
        }
    }
}
