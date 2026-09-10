package com.pablock.tca

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ClimaViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = ClimaRepository(app)
    private val ctx = app

    val ciudad: StateFlow<String?> = ClimaPrefs.ciudadFlow(ctx)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val reporte: StateFlow<ReporteClima?> = ClimaPrefs.reporteFlow(ctx)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _mensaje = MutableStateFlow<String?>(null)
    val mensaje: StateFlow<String?> = _mensaje

    private val _cargando = MutableStateFlow(false)
    val cargando: StateFlow<Boolean> = _cargando

    init {
        // Al abrir la pantalla: si hay ciudad configurada, internet, y todavía no se
        // actualizó hoy, pide el reporte una sola vez — nunca más de una vez al día.
        viewModelScope.launch {
            val ciudadGuardada = ClimaPrefs.ciudadFlow(ctx).first()
            if (!ciudadGuardada.isNullOrBlank() && !ClimaPrefs.yaActualizadoHoy(ctx)) {
                actualizar(forzar = false)
            }
        }
    }

    fun setCiudad(nombre: String) = viewModelScope.launch { ClimaPrefs.setCiudad(ctx, nombre) }

    // forzar=true es el botón de actualizar manual — igual respeta el candado diario.
    // Lee la ciudad directo de DataStore (no del StateFlow) para no depender de que ya
    // haya un suscriptor activo — relevante porque init también llama a esto.
    fun actualizar(forzar: Boolean) = viewModelScope.launch {
        val ciudadActual = ClimaPrefs.ciudadFlow(ctx).first()
        if (ciudadActual.isNullOrBlank()) {
            _mensaje.value = "Configura una ciudad primero."
            return@launch
        }
        if (BuildConfig.OWM_API_KEY.isBlank()) {
            _mensaje.value = "Falta la API key de OpenWeatherMap (se configura en local.properties)."
            return@launch
        }
        if (!hayInternet(ctx)) {
            _mensaje.value = "Sin internet — mostrando el último dato guardado."
            return@launch
        }
        if (!forzar && ClimaPrefs.yaActualizadoHoy(ctx)) return@launch
        if (!ClimaPrefs.puedeHacerPeticion(ctx)) {
            _mensaje.value = "Se llegó al máximo de $CLIMA_PETICIONES_MAXIMO_DIA peticiones de hoy."
            return@launch
        }

        _cargando.value = true
        ClimaPrefs.registrarPeticion(ctx)
        val resultado = repo.descargarReporte(ciudadActual, BuildConfig.OWM_API_KEY)
        resultado.onSuccess { ClimaPrefs.guardarReporte(ctx, it) }
            .onFailure { _mensaje.value = "No se pudo actualizar: ${it.message}" }
        _cargando.value = false
    }
}
