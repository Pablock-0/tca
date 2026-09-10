package com.pablock.tca.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pablock.tca.AjustesViewModel
import com.pablock.tca.DIANAS_MAXIMO
import com.pablock.tca.RegistroModo
import com.pablock.tca.db.DianaConfig
import kotlinx.coroutines.launch

private val DIANAS_DEFAULT = listOf(20, 40, 50, 80, 122)
private val COLOR_TOGGLE_ENCENDIDO = Color(0xFF008080)

@Composable
fun AjustesScreen(
    onVolver: () -> Unit,
    onNotas: () -> Unit,
    onHerramientas: () -> Unit,
    onEstadisticas: () -> Unit,
    viewModel: AjustesViewModel = viewModel(),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val contadorClickDoble by viewModel.contadorClickDoble.collectAsState()
    val contadorDigitos by viewModel.contadorExtensionDigitos.collectAsState()
    val zoomDefault by viewModel.zoomDefault.collectAsState()
    val flechasDefault by viewModel.flechasDefault.collectAsState()
    val dianaDefaultCm by viewModel.dianaDefaultCm.collectAsState()
    val dianasGuardadas by viewModel.dianasGuardadas.collectAsState()
    val notificacionActiva by viewModel.notificacionActiva.collectAsState()
    val registroModo by viewModel.registroModo.collectAsState()
    val ubicacionReloj by viewModel.ubicacionRadialReloj.collectAsState()
    val hayEntrenamientoEnCurso by hayEntrenamientoEnCursoState()

    var menuAbierto by remember { mutableStateOf(false) }
    var mostrarDialogoFlechasDefault by remember { mutableStateOf(false) }
    var mostrarDialogoEditarDianas by remember { mutableStateOf(false) }
    var mostrarDialogoExportar by remember { mutableStateOf(false) }

    fun guardarReporte(formato: String, uri: Uri?) {
        if (uri == null) return
        scope.launch {
            val reporte = viewModel.exportarReporte(formato)
            context.contentResolver.openOutputStream(uri)?.use { it.write(reporte.toByteArray()) }
        }
    }
    // Dos launchers (uno por mime) porque CreateDocument necesita el mime fijo desde
    // que se crea, y el formato solo se sabe hasta que el usuario elige en el diálogo.
    val crearDocumentoCsv = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) {
        guardarReporte("csv", it)
    }
    val crearDocumentoTxt = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) {
        guardarReporte("txt", it)
    }
    fun lanzarExportacion(formato: String) {
        if (formato == "txt") crearDocumentoTxt.launch("tca.txt") else crearDocumentoCsv.launch("tca.csv")
    }

    Box(modifier = Modifier.fillMaxSize()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(horizontal = 16.dp),
    ) {
    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
    ) {
        Box(modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(top = 8.dp, bottom = 16.dp)) {
            Icon(
                Icons.Default.Menu,
                contentDescription = "Menú",
                tint = Color.White,
                modifier = Modifier.clickable { menuAbierto = true }.padding(8.dp).size(36.dp),
            )
            Text("Ajustes", color = Color.White, fontSize = 24.sp, modifier = Modifier.align(Alignment.Center))
        }

        SeccionTitulo("Registro")
        FilaToggleTexto(
            etiqueta = "Click de contador",
            opcionA = "Simple",
            opcionB = "Doble",
            valorB = contadorClickDoble,
            onCambiar = viewModel::setContadorClickDoble,
        )
        FilaDropdownEntero(
            etiqueta = "Dígitos en contador",
            valor = contadorDigitos,
            opciones = (1..4).toList(),
            onSeleccionar = viewModel::setContadorExtensionDigitos,
        )
        FilaSwitch(etiqueta = "Zoom default", valor = zoomDefault, onCambiar = viewModel::setZoomDefault)
        FilaClickeable(
            etiqueta = "Flechas default",
            valor = flechasDefault.toString(),
            onClick = { mostrarDialogoFlechasDefault = true },
        )
        FilaClickeable(
            etiqueta = "Diana default",
            valor = "${dianaDefaultCm}cm",
            onClick = {
                val indice = DIANAS_DEFAULT.indexOf(dianaDefaultCm)
                val siguiente = DIANAS_DEFAULT[(indice + 1).mod(DIANAS_DEFAULT.size)]
                viewModel.setDianaDefaultCm(siguiente)
            },
        )
        FilaClickeable(
            etiqueta = "Editar dianas",
            valor = "${dianasGuardadas.size}/$DIANAS_MAXIMO",
            onClick = { mostrarDialogoEditarDianas = true },
        )

        SeccionTitulo("App y datos")
        FilaSwitch(etiqueta = "Notificación \"En curso\"", valor = notificacionActiva, onCambiar = viewModel::setNotificacionActiva)
        FilaToggleTexto(
            etiqueta = "Modalidad registro",
            opcionA = "Diana",
            opcionB = "Datos",
            valorB = registroModo == RegistroModo.DATOS,
            onCambiar = { viewModel.setRegistroModo(if (it) RegistroModo.DATOS else RegistroModo.DIANA) },
        )
        FilaToggleTexto(
            etiqueta = "Ubicación radial",
            opcionA = "Azimut",
            opcionB = "Reloj",
            valorB = ubicacionReloj,
            onCambiar = viewModel::setUbicacionRadialReloj,
        )
        FilaClickeable(
            etiqueta = "Exportar",
            valor = "Exportar ahora",
            onClick = { mostrarDialogoExportar = true },
        )
    }

        Text(
            "Diseñado por Pablock-0, construido por Claude.",
            color = Color.White.copy(alpha = 0.35f),
            fontSize = 12.sp,
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(vertical = 8.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }

    MenuLateral(
        abierto = menuAbierto,
        onCerrar = { menuAbierto = false },
        esPantallaPrincipal = false,
        hayEntrenamientoEnCurso = hayEntrenamientoEnCurso,
        onTerminar = {},
        onInicio = { menuAbierto = false; onVolver() },
        onNotas = { menuAbierto = false; onNotas() },
        onHerramientas = { menuAbierto = false; onHerramientas() },
        onEstadisticas = { menuAbierto = false; onEstadisticas() },
        onAjustes = { menuAbierto = false },
    )
    }

    if (mostrarDialogoFlechasDefault) {
        var texto by remember { mutableStateOf(flechasDefault.toString()) }
        AlertDialog(
            onDismissRequest = { mostrarDialogoFlechasDefault = false },
            title = { Text("Flechas default") },
            text = {
                OutlinedTextField(
                    value = texto,
                    onValueChange = { texto = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    texto.toIntOrNull()?.let(viewModel::setFlechasDefault)
                    mostrarDialogoFlechasDefault = false
                }) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { mostrarDialogoFlechasDefault = false }) { Text("Cancelar") } },
        )
    }

    if (mostrarDialogoEditarDianas) {
        EditarDianasDialog(
            dianas = dianasGuardadas,
            onEditar = viewModel::editarDiana,
            onEliminar = viewModel::eliminarDiana,
            onAgregar = viewModel::agregarDiana,
            onCerrar = { mostrarDialogoEditarDianas = false },
        )
    }

    if (mostrarDialogoExportar) {
        var formato by remember { mutableStateOf("csv") }
        var expandido by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { mostrarDialogoExportar = false },
            title = { Text("Exportar") },
            text = {
                Box {
                    Text(formato, color = Color.White, modifier = Modifier.clickable { expandido = true })
                    DropdownMenu(expanded = expandido, onDismissRequest = { expandido = false }) {
                        DropdownMenuItem(text = { Text("csv") }, onClick = { formato = "csv"; expandido = false })
                        DropdownMenuItem(text = { Text("txt") }, onClick = { formato = "txt"; expandido = false })
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.setExportarFormato(formato)
                    lanzarExportacion(formato)
                    mostrarDialogoExportar = false
                }) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { mostrarDialogoExportar = false }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun SeccionTitulo(texto: String) {
    Text(texto, color = Color.White, fontSize = 18.sp, modifier = Modifier.padding(top = 20.dp, bottom = 8.dp))
}

@Composable
private fun FilaSwitch(etiqueta: String, valor: Boolean, onCambiar: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(etiqueta, color = Color.White)
        Switch(
            checked = valor,
            onCheckedChange = onCambiar,
            colors = SwitchDefaults.colors(
                checkedThumbColor = COLOR_TOGGLE_ENCENDIDO,
                checkedTrackColor = COLOR_TOGGLE_ENCENDIDO.copy(alpha = 0.5f),
            ),
        )
    }
}

@Composable
private fun FilaToggleTexto(etiqueta: String, opcionA: String, opcionB: String, valorB: Boolean, onCambiar: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(etiqueta, color = Color.White)
        Text(
            if (valorB) opcionB else opcionA,
            color = ColorBordeTca,
            modifier = Modifier.clickable { onCambiar(!valorB) },
        )
    }
}

@Composable
private fun FilaDropdownEntero(etiqueta: String, valor: Int, opciones: List<Int>, onSeleccionar: (Int) -> Unit) {
    var expandido by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(etiqueta, color = Color.White)
        Box {
            Text(valor.toString(), color = ColorBordeTca, modifier = Modifier.clickable { expandido = true })
            DropdownMenu(expanded = expandido, onDismissRequest = { expandido = false }) {
                for (op in opciones) {
                    DropdownMenuItem(text = { Text(op.toString()) }, onClick = { expandido = false; onSeleccionar(op) })
                }
            }
        }
    }
}

@Composable
private fun FilaClickeable(etiqueta: String, valor: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable(onClick = onClick),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(etiqueta, color = Color.White)
        Text(valor, color = ColorBordeTca)
    }
}

@Composable
private fun EditarDianasDialog(
    dianas: List<DianaConfig>,
    onEditar: (Double, Int) -> Unit,
    onEliminar: (Double) -> Unit,
    onAgregar: (Double, Int) -> Unit,
    onCerrar: () -> Unit,
) {
    var mostrarAgregar by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text("Editar dianas") },
        text = {
            LazyColumn {
                items(dianas, key = { it.distancia }) { diana ->
                    var texto by remember(diana.distancia, diana.dianaCm) { mutableStateOf(diana.dianaCm.toString()) }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("${formatDistancia(diana.distancia)}m", color = Color.White, modifier = Modifier.weight(1f))
                        OutlinedTextField(
                            value = texto,
                            onValueChange = { nuevo ->
                                texto = nuevo
                                nuevo.toIntOrNull()?.let { onEditar(diana.distancia, it) }
                            },
                            label = { Text("cm") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                        )
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Eliminar diana",
                            modifier = Modifier.padding(start = 8.dp).clickable { onEliminar(diana.distancia) },
                        )
                    }
                }
                item {
                    TextButton(onClick = { mostrarAgregar = true }, enabled = dianas.size < DIANAS_MAXIMO) {
                        Text(if (dianas.size < DIANAS_MAXIMO) "+Agregar" else "+Agregar (máximo $DIANAS_MAXIMO)")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onCerrar) { Text("Cerrar") }
        },
    )

    if (mostrarAgregar) {
        var distanciaTexto by remember { mutableStateOf("") }
        var dianaTexto by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { mostrarAgregar = false },
            title = { Text("Agregar diana") },
            text = {
                Column {
                    OutlinedTextField(
                        value = distanciaTexto,
                        onValueChange = { distanciaTexto = it },
                        label = { Text("Distancia (m)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    )
                    OutlinedTextField(
                        value = dianaTexto,
                        onValueChange = { dianaTexto = it },
                        label = { Text("Diana (cm)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val distancia = distanciaTexto.toDoubleOrNull()
                    val diana = dianaTexto.toIntOrNull()
                    if (distancia != null && diana != null) onAgregar(distancia, diana)
                    mostrarAgregar = false
                }) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { mostrarAgregar = false }) { Text("Cancelar") } },
        )
    }
}
