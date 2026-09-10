package com.pablock.tca

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

private const val CANAL_ID = "entrenamiento_en_curso"
private const val NOTIFICACION_ID = 1

// Aviso de que el cronómetro del entrenamiento sigue corriendo en el backend aunque
// el usuario haya salido de la app (ver Ajustes > Notificación, default activada).
object NotificacionEntrenamiento {
    fun crearCanal(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val canal = NotificationChannel(
            CANAL_ID,
            "Entrenamiento en curso",
            NotificationManager.IMPORTANCE_LOW,
        )
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(canal)
    }

    fun mostrar(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val concedido = ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            if (!concedido) return
        }
        val notificacion = NotificationCompat.Builder(context, CANAL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentTitle("Entrenamiento en curso")
            .setContentText("TCA sigue registrando el tiempo entre series.")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        ContextCompat.getSystemService(context, NotificationManager::class.java)
            ?.notify(NOTIFICACION_ID, notificacion)
    }

    fun ocultar(context: Context) {
        ContextCompat.getSystemService(context, NotificationManager::class.java)
            ?.cancel(NOTIFICACION_ID)
    }
}
