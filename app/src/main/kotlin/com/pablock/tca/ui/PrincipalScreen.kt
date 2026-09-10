package com.pablock.tca.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pablock.tca.PrincipalViewModel
import com.pablock.tca.RegistroModo
import com.pablock.tca.textoPuntaje

@Composable
fun PrincipalScreen(
    onEstadisticas: () -> Unit,
    onNotas: () -> Unit,
    onAjustes: () -> Unit,
    onHerramientas: () -> Unit,
    viewModel: PrincipalViewModel = viewModel(),
) {
    val estado by viewModel.estado.collectAsState()
    val contadorClickDoble by viewModel.contadorClickDoble.collectAsState()
    val registroModo by viewModel.registroModo.collectAsState()
    val ubicacionReloj by viewModel.ubicacionRadialReloj.collectAsState()
    val distanciasGuardadas by viewModel.distanciasGuardadas.collectAsState()
    var menuAbierto by remember { mutableStateOf(false) }
    var mostrarDialogoTerminar by remember { mutableStateOf(false) }
    var filasDatos by remember(estado.contador) { mutableStateOf(List(FILAS_DATOS_DEFAULT) { FilaDatos() }) }

    Box(modifier = Modifier.fillMaxSize()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(horizontal = 16.dp),
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Box(
                        modifier = Modifier
                            .clickable { menuAbierto = true }
                            .padding(12.dp),
                    ) {
                        Icon(Icons.Default.Menu, contentDescription = "Menú", tint = Color.White, modifier = Modifier.size(36.dp))
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    DistanciaSelector(
                        distanciaActual = estado.distancia,
                        distanciasGuardadas = distanciasGuardadas.map { it.distancia },
                        onSeleccionar = viewModel::onDistanciaCambiada,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    BotonBordeAccion(doble = contadorClickDoble, padding = 25.dp, onActivar = viewModel::onAvanzarContador) {
                        Text(viewModel.textoContador(estado.contador), color = Color.White, fontSize = 33.sp)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    BotonBorde(onClick = viewModel::onToggleZoom) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "Zoom",
                            tint = if (estado.zoomActivo) Color(0xFF4CAF50) else Color.White,
                            modifier = Modifier.size(36.dp),
                        )
                    }
                }
            }

            // Punto 1: la etiqueta "n-P" vive arriba, centrada, en vez de bajo la diana.
            val ultima = estado.flechasPendientes.lastOrNull()
            Text(
                text = if (ultima != null) "${estado.flechasPendientes.size}-${textoPuntaje(ultima.puntaje)}" else "",
                color = Color(0xFF4CAF50),
                fontSize = 40.sp,
                modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 16.dp),
            )
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            if (estado.contador < 0) {
                // Sin borde: es el único botón de la pantalla principal que no lo lleva.
                BotonBorde(
                    onClick = viewModel::onAvanzarContador,
                    conBorde = false,
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                ) {
                    Text("Iniciar", color = Color.White, fontSize = 48.sp)
                }
            } else if (registroModo == RegistroModo.DATOS) {
                RegistroDatosTabla(
                    filas = filasDatos,
                    onFilasCambiadas = { filasDatos = it },
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                DianaInteractiva(
                    flechasPendientes = estado.flechasPendientes,
                    zoom = estado.zoomActivo,
                    onToque = viewModel::onToqueDiana,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        // Punto 4: una sola barra a todo lo ancho, sin recuadro por botón — solo una
        // línea arriba y un divisor en medio (forma de "T"); cada mitad ocupa medio
        // ancho de pantalla.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(129.dp)
                .navigationBarsPadding()
                .drawBehind {
                    drawLine(ColorBordeTca, Offset(0f, 0f), Offset(size.width, 0f), strokeWidth = 4f)
                },
        ) {
            MitadBarraInferior(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                onClick = {
                    if (registroModo == RegistroModo.DATOS) {
                        filasDatos = List(FILAS_DATOS_DEFAULT) { FilaDatos() }
                    } else {
                        viewModel.onRegresar()
                    }
                },
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Deshacer última flecha",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp),
                )
            }
            Box(modifier = Modifier.width(1.5.dp).fillMaxHeight().background(ColorBordeTca))
            MitadBarraInferior(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                doble = contadorClickDoble,
                onDobleClick = {
                    if (registroModo == RegistroModo.DATOS) {
                        viewModel.onGuardarDatosManual(parsearFilasDatos(filasDatos, ubicacionReloj))
                        filasDatos = List(FILAS_DATOS_DEFAULT) { FilaDatos() }
                    } else {
                        viewModel.onGuardarDobleClick()
                    }
                },
            ) {
                Text("Guardar", color = Color.White, fontSize = 18.sp)
            }
        }
    }

    MenuLateral(
        abierto = menuAbierto,
        onCerrar = { menuAbierto = false },
        esPantallaPrincipal = true,
        hayEntrenamientoEnCurso = estado.entrenamientoId != null,
        onTerminar = { menuAbierto = false; mostrarDialogoTerminar = true },
        onInicio = { menuAbierto = false },
        onNotas = { menuAbierto = false; onNotas() },
        onHerramientas = { menuAbierto = false; onHerramientas() },
        onEstadisticas = { menuAbierto = false; onEstadisticas() },
        onAjustes = { menuAbierto = false; onAjustes() },
    )
    }

    if (mostrarDialogoTerminar) {
        var nombre by remember { mutableStateOf("") }
        LaunchedEffect(Unit) { nombre = viewModel.nombreDefaultEntrenamiento() }
        AlertDialog(
            onDismissRequest = { mostrarDialogoTerminar = false },
            title = { Text("Guardar entrenamiento") },
            text = {
                OutlinedTextField(value = nombre, onValueChange = { nombre = it }, label = { Text("Nombre") })
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onTerminarEntrenamiento(nombre) { mostrarDialogoTerminar = false }
                }) { Text("Guardar") }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoTerminar = false }) { Text("Cancelar") }
            },
        )
    }
}

// Mitad de la barra inferior (Regresar/Guardar): sin borde propio ni padding fijo,
// solo el clickable — el "cuadro" de antes ahora es la línea de arriba + el divisor
// central de la barra completa (ver Row que la contiene).
@Composable
private fun MitadBarraInferior(
    modifier: Modifier = Modifier,
    doble: Boolean = false,
    onClick: (() -> Unit)? = null,
    onDobleClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    var ultimoClickMs by remember { mutableLongStateOf(0L) }
    val interaccion = if (doble) {
        Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
            val ahora = System.currentTimeMillis()
            if (ahora - ultimoClickMs <= UMBRAL_DOBLE_CLICK_MS) {
                ultimoClickMs = 0L
                onDobleClick?.invoke()
            } else {
                ultimoClickMs = ahora
            }
        }
    } else {
        Modifier.clickable { onClick?.invoke() }
    }
    Box(modifier = modifier.then(interaccion), contentAlignment = Alignment.Center, content = content)
}
