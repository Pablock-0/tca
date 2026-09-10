package com.pablock.tca

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pablock.tca.db.Nota
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

const val NOTAS_MAXIMO = 10

class NotasViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = TcaRepository(app)

    val notas: StateFlow<List<Nota>> = repo.notas()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val notaSeleccionadaId = MutableStateFlow<Long?>(null)

    // Si no se ha elegido ninguna explícitamente, muestra la más reciente.
    val notaActual: StateFlow<Nota?> = combine(notas, notaSeleccionadaId) { lista, id ->
        lista.find { it.id == id } ?: lista.lastOrNull()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun seleccionar(id: Long) {
        notaSeleccionadaId.value = id
    }

    fun crear() = viewModelScope.launch {
        if (notas.value.size >= NOTAS_MAXIMO) return@launch
        notaSeleccionadaId.value = repo.crearNota()
    }

    fun actualizarContenido(nota: Nota, contenido: String) = viewModelScope.launch {
        repo.guardarNota(nota.copy(contenido = contenido))
    }

    // Sin ninguna nota todavía: escribir directo en el bloc crea "Nota 0" sola.
    fun escribirSinNota(contenido: String) = viewModelScope.launch {
        notaSeleccionadaId.value = repo.crearNota0(contenido)
    }

    fun renombrar(nota: Nota, nuevoTitulo: String) = viewModelScope.launch {
        repo.guardarNota(nota.copy(titulo = nuevoTitulo))
    }

    fun eliminar(nota: Nota) = viewModelScope.launch {
        if (!nota.esNota0) repo.eliminarNota(nota.id)
    }
}
