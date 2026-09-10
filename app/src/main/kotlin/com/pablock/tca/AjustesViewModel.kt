package com.pablock.tca

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pablock.tca.db.DianaConfig
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

const val DIANAS_MAXIMO = 15

class AjustesViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = TcaRepository(app)
    private val ctx = app

    val contadorClickDoble: StateFlow<Boolean> = AjustesPrefs.contadorClickDobleFlow(ctx)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val contadorExtensionDigitos: StateFlow<Int> = AjustesPrefs.contadorExtensionDigitosFlow(ctx)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 2)

    val zoomDefault: StateFlow<Boolean> = AjustesPrefs.zoomDefaultFlow(ctx)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val flechasDefault: StateFlow<Int> = AjustesPrefs.flechasDefaultFlow(ctx)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 6)

    val dianaDefaultCm: StateFlow<Int> = AjustesPrefs.dianaDefaultCm(ctx)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 40)

    val dianasGuardadas: StateFlow<List<DianaConfig>> = repo.dianasGuardadasFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notificacionActiva: StateFlow<Boolean> = AjustesPrefs.notificacionActivaFlow(ctx)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val registroModo: StateFlow<RegistroModo> = AjustesPrefs.registroModoFlow(ctx)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RegistroModo.DIANA)

    val ubicacionRadialReloj: StateFlow<Boolean> = AjustesPrefs.ubicacionRadialRelojFlow(ctx)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val exportarFormato: StateFlow<String> = AjustesPrefs.exportarFormatoFlow(ctx)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "csv")

    fun setContadorClickDoble(doble: Boolean) = viewModelScope.launch { AjustesPrefs.setContadorClickDoble(ctx, doble) }

    fun setContadorExtensionDigitos(digitos: Int) = viewModelScope.launch { AjustesPrefs.setContadorExtensionDigitos(ctx, digitos) }

    fun setZoomDefault(activo: Boolean) = viewModelScope.launch { AjustesPrefs.setZoomDefault(ctx, activo) }

    fun setFlechasDefault(cantidad: Int) = viewModelScope.launch { AjustesPrefs.setFlechasDefault(ctx, cantidad) }

    fun setDianaDefaultCm(cm: Int) = viewModelScope.launch { AjustesPrefs.setDianaDefaultCm(ctx, cm) }

    fun agregarDiana(distancia: Double, dianaCm: Int) = viewModelScope.launch {
        if (dianasGuardadas.value.size >= DIANAS_MAXIMO) return@launch
        repo.guardarDianaParaDistancia(distancia, dianaCm)
    }

    fun editarDiana(distancia: Double, dianaCm: Int) = viewModelScope.launch {
        repo.guardarDianaParaDistancia(distancia, dianaCm)
    }

    fun eliminarDiana(distancia: Double) = viewModelScope.launch {
        repo.eliminarDiana(distancia)
    }

    fun setNotificacionActiva(activa: Boolean) = viewModelScope.launch { AjustesPrefs.setNotificacionActiva(ctx, activa) }

    fun setRegistroModo(modo: RegistroModo) = viewModelScope.launch { AjustesPrefs.setRegistroModo(ctx, modo) }

    fun setUbicacionRadialReloj(reloj: Boolean) = viewModelScope.launch { AjustesPrefs.setUbicacionRadialReloj(ctx, reloj) }

    fun setExportarFormato(formato: String) = viewModelScope.launch { AjustesPrefs.setExportarFormato(ctx, formato) }

    suspend fun exportarReporte(formato: String): String = repo.exportarReporte(formato)
}
