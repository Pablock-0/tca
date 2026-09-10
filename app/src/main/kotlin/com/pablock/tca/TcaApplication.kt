package com.pablock.tca

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TcaApplication : Application() {
    private val scope = CoroutineScope(Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        NotificacionEntrenamiento.crearCanal(this)

        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            // La app pasó a segundo plano: si hay un entrenamiento en curso y la
            // notificación está activada, avisamos que el cronómetro sigue corriendo.
            override fun onStop(owner: LifecycleOwner) {
                scope.launch {
                    val estado = EntrenamientoEnCursoPrefs.leer(this@TcaApplication)
                    if (estado.entrenamientoId != null && AjustesPrefs.notificacionActiva(this@TcaApplication)) {
                        NotificacionEntrenamiento.mostrar(this@TcaApplication)
                    }
                }
            }

            override fun onStart(owner: LifecycleOwner) {
                NotificacionEntrenamiento.ocultar(this@TcaApplication)
            }
        })
    }
}
