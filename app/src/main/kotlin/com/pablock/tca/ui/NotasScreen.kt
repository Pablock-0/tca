package com.pablock.tca.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pablock.tca.NOTAS_MAXIMO
import com.pablock.tca.NotasViewModel
import com.pablock.tca.db.Nota

@Composable
fun NotasScreen(
    onIrAInicio: () -> Unit,
    onIrAEstadisticas: () -> Unit,
    onIrAAjustes: () -> Unit,
    onHerramientas: () -> Unit,
    viewModel: NotasViewModel = viewModel(),
) {
    val notas by viewModel.notas.collectAsState()
    val notaActual by viewModel.notaActual.collectAsState()
    val hayEntrenamientoEnCurso by hayEntrenamientoEnCursoState()
    var menuAbierto by remember { mutableStateOf(false) }
    var menuNotasAbierto by remember { mutableStateOf(false) }
    var mostrarDialogoEditar by remember { mutableStateOf(false) }
    var texto by remember(notaActual?.id) { mutableStateOf(notaActual?.contenido ?: "") }

    Box(modifier = Modifier.fillMaxSize()) {
    Column(modifier = Modifier.fillMaxSize().background(Color.Black).padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Icon(
                Icons.Default.Menu,
                contentDescription = "Menú",
                tint = Color.White,
                modifier = Modifier.clickable { menuAbierto = true }.padding(8.dp),
            )

            Box {
                Text(
                    notaActual?.titulo ?: "Notas",
                    color = Color.White,
                    fontSize = 20.sp,
                    modifier = Modifier.clickable { menuNotasAbierto = true }.padding(8.dp),
                )
                DropdownMenu(expanded = menuNotasAbierto, onDismissRequest = { menuNotasAbierto = false }) {
                    for (nota in notas) {
                        DropdownMenuItem(
                            text = { Text(nota.titulo) },
                            onClick = { menuNotasAbierto = false; viewModel.seleccionar(nota.id) },
                        )
                    }
                    DropdownMenuItem(text = { Text("Editar") }, onClick = { menuNotasAbierto = false; mostrarDialogoEditar = true })
                }
            }
        }

        val nota = notaActual
        BasicTextField(
            value = texto,
            onValueChange = { nuevo ->
                texto = nuevo
                if (nota != null) viewModel.actualizarContenido(nota, nuevo) else viewModel.escribirSinNota(nuevo)
            },
            textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
            modifier = Modifier.fillMaxSize().padding(top = 16.dp),
        )
    }

    MenuLateral(
        abierto = menuAbierto,
        onCerrar = { menuAbierto = false },
        esPantallaPrincipal = false,
        hayEntrenamientoEnCurso = hayEntrenamientoEnCurso,
        onTerminar = {},
        onInicio = { menuAbierto = false; onIrAInicio() },
        onNotas = { menuAbierto = false },
        onHerramientas = { menuAbierto = false; onHerramientas() },
        onEstadisticas = { menuAbierto = false; onIrAEstadisticas() },
        onAjustes = { menuAbierto = false; onIrAAjustes() },
    )
    }

    if (mostrarDialogoEditar) {
        EditarNotasDialog(
            notas = notas,
            onRenombrar = viewModel::renombrar,
            onEliminar = viewModel::eliminar,
            onAgregar = viewModel::crear,
            onCerrar = { mostrarDialogoEditar = false },
        )
    }
}

@Composable
private fun EditarNotasDialog(
    notas: List<Nota>,
    onRenombrar: (Nota, String) -> Unit,
    onEliminar: (Nota) -> Unit,
    onAgregar: () -> Unit,
    onCerrar: () -> Unit,
) {
    var notaAEliminar by remember { mutableStateOf<Nota?>(null) }

    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text("Editar notas") },
        text = {
            LazyColumn {
                items(notas, key = { it.id }) { nota ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedTextField(
                            value = nota.titulo,
                            onValueChange = { onRenombrar(nota, it) },
                            modifier = Modifier.weight(1f),
                        )
                        if (!nota.esNota0) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Eliminar nota",
                                modifier = Modifier.padding(start = 8.dp).clickable { notaAEliminar = nota },
                            )
                        }
                    }
                }
                item {
                    TextButton(onClick = onAgregar, enabled = notas.size < NOTAS_MAXIMO) {
                        Text(if (notas.size < NOTAS_MAXIMO) "+Agregar" else "+Agregar (máximo $NOTAS_MAXIMO)")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onCerrar) { Text("Cerrar") }
        },
    )

    val paraEliminar = notaAEliminar
    if (paraEliminar != null) {
        AlertDialog(
            onDismissRequest = { notaAEliminar = null },
            title = { Text("¿Estás seguro de borrar ${paraEliminar.titulo}?") },
            confirmButton = {
                TextButton(onClick = { onEliminar(paraEliminar); notaAEliminar = null }) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { notaAEliminar = null }) { Text("Cancelar") }
            },
        )
    }
}
