package com.pablock.tca.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pablock.tca.EstadisticasListViewModel
import com.pablock.tca.db.Entrenamiento

@Composable
fun EstadisticasListScreen(
    onSeleccionar: (Long) -> Unit,
    onIrAInicio: () -> Unit,
    onNotas: () -> Unit,
    onHerramientas: () -> Unit,
    onAjustes: () -> Unit,
    viewModel: EstadisticasListViewModel = viewModel(),
) {
    val entrenamientos by viewModel.entrenamientos.collectAsState()
    val hayEntrenamientoEnCurso by hayEntrenamientoEnCursoState()
    var menuAbierto by remember { mutableStateOf(false) }
    var modoSeleccion by remember { mutableStateOf(false) }
    var seleccionados by remember { mutableStateOf(setOf<Long>()) }
    var mostrarConfirmacion by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
    Column(modifier = Modifier.fillMaxSize().background(Color.Black).padding(horizontal = 16.dp)) {
        Box(modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(top = 8.dp)) {
            Icon(
                Icons.Default.Menu,
                contentDescription = "Menú",
                tint = Color.White,
                modifier = Modifier.clickable { menuAbierto = true }.padding(8.dp).size(36.dp),
            )
            Text(
                "Estadísticas",
                color = Color.White,
                fontSize = 24.sp,
                modifier = Modifier.align(Alignment.Center),
            )
            Icon(
                Icons.Default.Delete,
                contentDescription = "Borrar entrenamientos",
                tint = if (modoSeleccion) Color(0xFFE53935) else Color.White,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .clickable {
                        when {
                            !modoSeleccion -> modoSeleccion = true
                            seleccionados.isEmpty() -> modoSeleccion = false
                            else -> mostrarConfirmacion = true
                        }
                    }
                    .padding(8.dp),
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(entrenamientos) { entrenamiento ->
                FilaEntrenamiento(
                    entrenamiento = entrenamiento,
                    modoSeleccion = modoSeleccion,
                    seleccionado = entrenamiento.id in seleccionados,
                    onClick = {
                        if (modoSeleccion) {
                            seleccionados = if (entrenamiento.id in seleccionados) {
                                seleccionados - entrenamiento.id
                            } else {
                                seleccionados + entrenamiento.id
                            }
                        } else {
                            onSeleccionar(entrenamiento.id)
                        }
                    },
                )
            }
        }
    }

    MenuLateral(
        abierto = menuAbierto,
        onCerrar = { menuAbierto = false },
        esPantallaPrincipal = false,
        hayEntrenamientoEnCurso = hayEntrenamientoEnCurso,
        onTerminar = {},
        onInicio = { menuAbierto = false; onIrAInicio() },
        onNotas = { menuAbierto = false; onNotas() },
        onHerramientas = { menuAbierto = false; onHerramientas() },
        onEstadisticas = { menuAbierto = false },
        onAjustes = { menuAbierto = false; onAjustes() },
    )
    }

    if (mostrarConfirmacion) {
        AlertDialog(
            onDismissRequest = { mostrarConfirmacion = false },
            title = { Text("¿Seguro que quieres borrar ${seleccionados.size} entrenamientos?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.eliminar(seleccionados)
                    seleccionados = emptySet()
                    modoSeleccion = false
                    mostrarConfirmacion = false
                }) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { mostrarConfirmacion = false }) { Text("Cancelar") }
            },
        )
    }
}

@Composable
private fun FilaEntrenamiento(
    entrenamiento: Entrenamiento,
    modoSeleccion: Boolean,
    seleccionado: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .border(
                BorderStroke(1.5.dp, if (modoSeleccion && seleccionado) Color(0xFFE53935) else ColorBordeTca),
                RoundedCornerShape(6.dp),
            )
            .padding(16.dp),
    ) {
        Column {
            Text(entrenamiento.nombre ?: entrenamiento.fecha, color = Color.White, fontSize = 18.sp)
            Text(entrenamiento.fecha, color = Color.White.copy(alpha = 0.6f), fontSize = 14.sp)
        }
    }
}
