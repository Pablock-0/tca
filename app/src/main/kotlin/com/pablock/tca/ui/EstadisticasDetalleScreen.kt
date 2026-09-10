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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pablock.tca.AjustesPrefs
import com.pablock.tca.DetalleEntrenamiento
import com.pablock.tca.DetalleSerie
import com.pablock.tca.TcaRepository
import com.pablock.tca.azimutATextoReloj
import com.pablock.tca.textoPuntaje
import com.pablock.tca.ui.formatDistancia

private fun formatDuracion(segundos: Long): String {
    val m = segundos / 60
    val s = segundos % 60
    return "%d:%02d".format(m, s)
}

@Composable
fun EstadisticasDetalleScreen(entrenamientoId: Long, onVolver: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { TcaRepository(context) }
    val detalle by produceState<DetalleEntrenamiento?>(initialValue = null, entrenamientoId) {
        value = repo.cargarDetalle(entrenamientoId)
    }
    val ubicacionReloj by produceState(initialValue = false) {
        value = AjustesPrefs.ubicacionRadialReloj(context)
    }

    Column(modifier = Modifier.fillMaxSize().background(Color.Black).padding(horizontal = 16.dp)) {
        Box(modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(top = 8.dp)) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Volver al resumen",
                tint = Color.White,
                modifier = Modifier.clickable(onClick = onVolver).padding(8.dp),
            )
        }

        val d = detalle
        if (d == null) {
            Text("Cargando…", color = Color.White, modifier = Modifier.padding(top = 24.dp))
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(top = 8.dp)) {
                item { TablaResumen(d) }
                item {
                    Text(
                        "Detalle",
                        color = Color.White,
                        fontSize = 18.sp,
                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                    )
                }
                items(d.series) { serie -> FilaSerieDetalle(serie, ubicacionReloj) }
            }
        }
    }
}

@Composable
private fun TablaResumen(detalle: DetalleEntrenamiento) {
    Column {
        FilaResumen("Total de flechas", detalle.totalFlechas.toString())
        FilaResumen("Total de series", detalle.totalSeries.toString())
        FilaResumen("Flechas por serie", "%.1f".format(detalle.flechasPorSerie))
        FilaResumen("Tiempo promedio entre serie", formatDuracion(detalle.tiempoPromedioSegundos.toLong()))
        for ((distancia, promedio) in detalle.puntajePromedioPorDistancia) {
            FilaResumen("Puntaje promedio a ${formatDistancia(distancia)}", promedio?.let { "%.1f".format(it) } ?: "N/D")
        }
        FilaResumen("Diana principal", detalle.dianaPrincipal.joinToString(" / ") { "${it}cm" })
    }
}

@Composable
private fun FilaResumen(etiqueta: String, valor: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(etiqueta, color = Color.White.copy(alpha = 0.8f))
        Text(valor, color = Color.White)
    }
}

@Composable
private fun FilaSerieDetalle(serie: DetalleSerie, ubicacionReloj: Boolean) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text("Serie ${serie.orden}:", color = Color.White, fontSize = 16.sp)
        Row(modifier = Modifier.fillMaxWidth()) {
            if (serie.flechas.isNotEmpty()) {
                Column(modifier = Modifier.padding(start = 16.dp)) {
                    Text("P  UR", color = Color.White.copy(alpha = 0.6f))
                    for ((_, puntaje, ur) in serie.flechas) {
                        val urTexto = if (ubicacionReloj) azimutATextoReloj(ur) else "$ur°"
                        Text("${textoPuntaje(puntaje)}  $urTexto", color = Color.White)
                    }
                }
            }
            Column(modifier = Modifier.padding(start = 32.dp)) {
                Text("Tiempo: ${formatDuracion(serie.tiempoSegundos)}", color = Color.White)
                Text("Distancia: ${formatDistancia(serie.distancia)}", color = Color.White)
                Text("Diana: ${serie.dianaCm}", color = Color.White)
                Text("Flechas: ${serie.numeroFlechas}", color = Color.White)
            }
        }
    }
}
