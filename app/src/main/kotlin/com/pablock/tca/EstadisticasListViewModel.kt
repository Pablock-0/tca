package com.pablock.tca

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pablock.tca.db.Entrenamiento
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class EstadisticasListViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = TcaRepository(app)

    val entrenamientos: StateFlow<List<Entrenamiento>> = repo.entrenamientosFinalizados()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun eliminar(ids: Set<Long>) = viewModelScope.launch {
        repo.eliminarEntrenamientos(ids)
    }
}
