package com.pablock.tca

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pablock.tca.db.DianaConfig
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
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

    // Estado en vivo del entrenamiento en curso para la pantalla principal: total de
    // flechas (blanco, junto al "n-P" verde) y puntaje/posible (esquina de la diana, si
    // Ajustes > "Score en vivo" está encendido) — suma lo ya guardado en Room
    // (repo.estadoVivoEnCurso) + las flechas pendientes de la serie activa aún sin
    // avanzar/guardar (esas solo las conoce este ViewModel). Se re-consulta solo cuando
    // cambia algo relevante, no en cada emisión de `estado` (evita golpear la base en
    // cada campo no relacionado).
    //
    // Bug 2026-10-02: la clave de distinctUntilChanged solo llevaba (entrenamientoId,
    // pendientes.size) — avanzar de serie SOLO con el contador (sin tocar la diana) crea
    // una Serie nueva en Room (con flechasEstimadas), pero no cambia ninguno de esos dos
    // campos: el entrenamiento sigue siendo el mismo y flechasPendientes se resetea de
    // vacío a vacío. Como la clave no cambiaba, distinctUntilChanged la descartaba y
    // estadoVivoEnCurso() nunca se volvía a pedir — el número en Room ya estaba bien,
    // solo no se re-consultaba hasta el siguiente toque en la diana (que sí cambia
    // flechasPendientes). Se agrega `contador` a la clave: se incrementa en cada avance
    // (con o sin diana), así que ahora SIEMPRE dispara el recálculo correspondiente.
    val estadoVivoEntrenamiento: StateFlow<EstadoVivoEntrenamiento> = estado
        .map { Triple(it.entrenamientoId, it.contador, it.flechasPendientes) }
        .distinctUntilChanged()
        .map { (entrenamientoId, _, pendientes) ->
            if (entrenamientoId == null) {
                EstadoVivoEntrenamiento(totalFlechas = 0, puntaje = 0, puntajePosible = 0)
            } else {
                val base = repo.estadoVivoEnCurso(entrenamientoId)
                base.copy(
                    totalFlechas = base.totalFlechas + pendientes.size,
                    puntaje = base.puntaje + pendientes.sumOf { valorParaPromedio(it.puntaje) },
                    puntajePosible = base.puntajePosible + pendientes.size * 10,
                )
            }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            EstadoVivoEntrenamiento(totalFlechas = 0, puntaje = 0, puntajePosible = 0),
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

    val scoreVisible: StateFlow<Boolean> = AjustesPrefs.scoreVisibleFlow(app)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    init {
        viewModelScope.launch { repo.marcarEnEspera() }
    }

    // digitos se recibe como parámetro (no se lee de contadorExtensionDigitos.value)
    // porque ese StateFlow usa WhileSubscribed: si nada lo colecta con
    // collectAsState() en la pantalla, la fuente de Ajustes nunca se suscribe y
    // .value se queda pegado en el default — era justo lo que pasaba antes (el
    // ajuste de dígitos no tenía ningún efecto).
    fun textoContador(contador: Int, digitos: Int): String {
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
