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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pablock.tca.BuildConfig
import com.pablock.tca.ClimaViewModel

@Composable
fun ClimaScreen(onVolver: () -> Unit, viewModel: ClimaViewModel = viewModel()) {
    val ciudad by viewModel.ciudad.collectAsState()
    val apiKeyGuardada by viewModel.apiKeyGuardada.collectAsState()
    val necesitaApiKey by viewModel.necesitaApiKey.collectAsState()
    val reporte by viewModel.reporte.collectAsState()
    val cargando by viewModel.cargando.collectAsState()
    val mensaje by viewModel.mensaje.collectAsState()

    var mostrarDialogoCiudad by remember { mutableStateOf(false) }
    var mostrarDialogoApiKey by remember { mutableStateOf(false) }

    // Abre el diálogo de la llave solo (sin acción del usuario) apenas el ViewModel
    // detecta que hace falta — cubre tanto "nunca se configuró" como "quedó inválida".
    LaunchedEffect(necesitaApiKey) {
        if (necesitaApiKey) mostrarDialogoApiKey = true
    }

    Column(modifier = Modifier.fillMaxSize().background(Color.Black).padding(horizontal = 16.dp)) {
        Box(modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(top = 8.dp, bottom = 16.dp)) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Volver",
                tint = Color.White,
                modifier = Modifier.clickable(onClick = onVolver).padding(8.dp),
            )
            Text("Clima", color = Color.White, fontSize = 24.sp, modifier = Modifier.align(Alignment.Center))
            Icon(
                Icons.Default.Refresh,
                contentDescription = "Actualizar",
                tint = if (cargando) Color.White.copy(alpha = 0.4f) else Color.White,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .clickable(enabled = !cargando) { viewModel.actualizar(forzar = true) }
                    .padding(8.dp)
                    .size(28.dp),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().clickable { mostrarDialogoCiudad = true }.padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Ciudad", color = Color.White, fontSize = 32.sp)
            Text(ciudad ?: "sin configurar", color = ColorBordeTca, fontSize = 32.sp)
        }

        Row(
            modifier = Modifier.fillMaxWidth().clickable { mostrarDialogoApiKey = true }.padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Llave API", color = Color.White, fontSize = 32.sp)
            val hayLlave = BuildConfig.OWM_API_KEY.isNotBlank() || !apiKeyGuardada.isNullOrBlank()
            Text(
                if (hayLlave) "configurada" else "sin configurar",
                color = ColorBordeTca,
                fontSize = 32.sp,
            )
        }

        val r = reporte
        if (r != null) {
            Column(modifier = Modifier.padding(top = 24.dp)) {
                FilaClima("Viento", "${r.vientoMs} m/s")
                FilaClima("Humedad", "${r.humedadPorciento}%")
                FilaClima("Precipitación", "${r.precipitacionPorciento}%")
                FilaClima("Temp. mín", "${r.tempMinC}°C")
                FilaClima("Temp. máx", "${r.tempMaxC}°C")
            }
        } else {
            Text(
                "Sin datos todavía. Configura una ciudad y actualiza.",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 32.sp,
                modifier = Modifier.padding(top = 24.dp),
            )
        }

        mensaje?.let {
            Text(it, color = Color.White.copy(alpha = 0.7f), fontSize = 32.sp, modifier = Modifier.padding(top = 16.dp))
        }
    }

    if (mostrarDialogoCiudad) {
        var texto by remember { mutableStateOf(ciudad ?: "") }
        AlertDialog(
            onDismissRequest = { mostrarDialogoCiudad = false },
            title = { Text("Ciudad") },
            text = { OutlinedTextField(value = texto, onValueChange = { texto = it }) },
            confirmButton = {
                TextButton(onClick = { viewModel.setCiudad(texto.trim()); mostrarDialogoCiudad = false }) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { mostrarDialogoCiudad = false }) { Text("Cancelar") } },
        )
    }

    if (mostrarDialogoApiKey) {
        var texto by remember { mutableStateOf(apiKeyGuardada ?: "") }
        val uriHandler = LocalUriHandler.current
        AlertDialog(
            onDismissRequest = { mostrarDialogoApiKey = false },
            title = { Text("Llave de OpenWeatherMap") },
            text = {
                Column {
                    Text(
                        "Clima necesita una llave gratuita de OpenWeatherMap para funcionar.",
                        fontSize = 14.sp,
                    )
                    Text(
                        "Consigue la tuya en openweathermap.org/api",
                        color = ColorBordeTca,
                        fontSize = 14.sp,
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier
                            .padding(top = 4.dp, bottom = 12.dp)
                            .clickable { uriHandler.openUri("https://openweathermap.org/api") },
                    )
                    OutlinedTextField(value = texto, onValueChange = { texto = it }, label = { Text("Llave") })
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.setApiKey(texto); mostrarDialogoApiKey = false }) { Text("Guardar") }
            },
            dismissButton = { TextButton(onClick = { mostrarDialogoApiKey = false }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun FilaClima(etiqueta: String, valor: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(etiqueta, color = Color.White.copy(alpha = 0.8f), fontSize = 32.sp)
        Text(valor, color = Color.White, fontSize = 32.sp)
    }
}
