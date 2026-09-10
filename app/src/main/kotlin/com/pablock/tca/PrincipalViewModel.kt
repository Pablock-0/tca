package com.pablock.tca

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pablock.tca.db.DianaConfig
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PrincipalViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = TcaRepository(app)

    val estado: StateFlow<EntrenamientoEnCursoPrefs.Estado> = EntrenamientoEnCursoPrefs.observar(app)
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            EntrenamientoEnCursoPrefs.Estado(null, -1, 10.0, 0L, emptyList(), false),
        )

    val contadorClickDoble: StateFlow<Boolean> = AjustesPrefs.contadorClickDobleFlow(app)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val contadorExtensionDigitos: StateFlow<Int> = AjustesPrefs.contadorExtensionDigitosFlow(app)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 2)

    val registroModo: StateFlow<RegistroModo> = AjustesPrefs.registroModoFlow(app)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RegistroModo.DIANA)

    val ubicacionRadialReloj: StateFlow<Boolean> = AjustesPrefs.ubicacionRadialRelojFlow(app)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val distanciasGuardadas: StateFlow<List<DianaConfig>> = repo.dianasGuardadasFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch { repo.marcarEnEspera() }
    }

    fun textoContador(contador: Int): String {
        val digitos = contadorExtensionDigitos.value
        val modulo = Math.pow(10.0, digitos.toDouble()).toInt()
        return if (contador < 0) "Iniciar" else (contador % modulo).toString().padStart(digitos, '0')
    }

    fun onToqueDiana(toque: ToqueDiana) = viewModelScope.launch {
        repo.registrarFlecha(estado.value.flechasPendientes, toque)
    }

    fun onRegresar() = viewModelScope.launch {
        repo.quitarUltimaFlecha(estado.value.flechasPendientes)
    }

    // Un solo click en el botón grande "Iniciar" (reemplaza a la diana antes de la
    // primera serie) o un click/doble click en el contador de arriba hacen exactamente
    // lo mismo: avanzar sin guardar flechas.
    fun onAvanzarContador() = viewModelScope.launch {
        val e = estado.value
        repo.avanzarContador(guardar = false, flechasPendientes = e.flechasPendientes, distancia = e.distancia)
    }

    fun onGuardarDobleClick() = viewModelScope.launch {
        val e = estado.value
        repo.avanzarContador(guardar = true, flechasPendientes = e.flechasPendientes, distancia = e.distancia)
    }

    // Ajustes > Registro > Datos: las flechas no vienen de tocar la diana sino de una
    // tabla escrita a mano (ver RegistroDatosTabla) — se guardan con el mismo camino.
    fun onGuardarDatosManual(flechas: List<FlechaPendiente>) = viewModelScope.launch {
        val e = estado.value
        repo.avanzarContador(guardar = true, flechasPendientes = flechas, distancia = e.distancia)
    }

    fun onDistanciaCambiada(distancia: Double) = viewModelScope.launch {
        repo.cambiarDistancia(distancia)
    }

    fun onToggleZoom() = viewModelScope.launch {
        repo.setZoom(!estado.value.zoomActivo)
    }

    suspend fun nombreDefaultEntrenamiento(): String {
        val id = estado.value.entrenamientoId ?: return ""
        return repo.obtenerEntrenamiento(id)?.fecha ?: ""
    }

    fun onTerminarEntrenamiento(nombre: String, onListo: () -> Unit) = viewModelScope.launch {
        val id = estado.value.entrenamientoId ?: return@launch
        repo.terminarEntrenamiento(id, nombre)
        onListo()
    }
}
