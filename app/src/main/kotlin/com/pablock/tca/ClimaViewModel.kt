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

    // Llave puesta a mano por quien instala el APK (vacía en un build personal donde
    // BuildConfig.OWM_API_KEY ya trae la de local.properties). Ver apiKeyEfectiva().
    val apiKeyGuardada: StateFlow<String?> = ClimaPrefs.apiKeyFlow(ctx)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val reporte: StateFlow<ReporteClima?> = ClimaPrefs.reporteFlow(ctx)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _mensaje = MutableStateFlow<String?>(null)
    val mensaje: StateFlow<String?> = _mensaje

    private val _cargando = MutableStateFlow(false)
    val cargando: StateFlow<Boolean> = _cargando

    // true cuando no hay ninguna llave disponible (ni de BuildConfig ni guardada) — la
    // pantalla usa esto para abrir el diálogo de pedir la llave automáticamente.
    private val _necesitaApiKey = MutableStateFlow(false)
    val necesitaApiKey: StateFlow<Boolean> = _necesitaApiKey

    fun setApiKey(key: String) = viewModelScope.launch { ClimaPrefs.setApiKey(ctx, key.trim()) }

    // BuildConfig trae la llave real en un build personal (local.properties lleno) y
    // vacía en el APK público (compilado con -PtcaPublicBuild=true, ver build.gradle.kts);
    // en ese caso se usa la que la persona haya puesto a mano en la app.
    private suspend fun apiKeyEfectiva(): String {
        if (BuildConfig.OWM_API_KEY.isNotBlank()) return BuildConfig.OWM_API_KEY
        return ClimaPrefs.apiKeyFlow(ctx).first().orEmpty()
    }

    init {
        // Al abrir la pantalla (entrar a Herramientas > Clima): si hay ciudad
        // configurada, actualiza igual que el botón de refrescar de la esquina
        // (forzar=true) — el candado de $CLIMA_PETICIONES_MAXIMO_DIA/día sigue
        // aplicando dentro de actualizar(), forzar solo salta el "ya se actualizó
        // hoy" para que abrir la pantalla siempre traiga el dato más reciente.
        viewModelScope.launch {
            val ciudadGuardada = ClimaPrefs.ciudadFlow(ctx).first()
            if (!ciudadGuardada.isNullOrBlank()) {
                actualizar(forzar = true)
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
        val key = apiKeyEfectiva()
        if (key.isBlank()) {
            _necesitaApiKey.value = true
            _mensaje.value = "Configura tu llave de OpenWeatherMap para usar Clima."
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
        val resultado = repo.descargarReporte(ciudadActual, key)
        resultado.onSuccess {
            _necesitaApiKey.value = false
            ClimaPrefs.guardarReporte(ctx, it)
        }.onFailure {
            _mensaje.value = if (it.message?.contains("401") == true) {
                _necesitaApiKey.value = true
                "Llave inválida — revisa que la copiaste bien."
            } else {
                "No se pudo actualizar: ${it.message}"
            }
        }
        _cargando.value = false
    }
}
