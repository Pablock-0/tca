package com.pablock.tca.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pablock.tca.ResumenEntrenamiento
import com.pablock.tca.TcaRepository

@Composable
fun EstadisticasResumenScreen(entrenamientoId: Long, onVerDetalle: () -> Unit, onVolver: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { TcaRepository(context) }
    val resumen by produceState<ResumenEntrenamiento?>(initialValue = null, entrenamientoId) {
        value = repo.cargarResumen(entrenamientoId)
    }

    Column(modifier = Modifier.fillMaxSize().background(Color.Black).padding(horizontal = 16.dp)) {
        Box(modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(top = 8.dp)) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Volver a Estadísticas",
                tint = Color.White,
                modifier = Modifier.clickable(onClick = onVolver).padding(8.dp),
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Flechas totales", color = Color.White, fontSize = 20.sp)
            Text(resumen?.totalFlechas?.toString() ?: "…", color = Color.White, fontSize = 32.sp)
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            DianaMapaDeCalor(
                conteoPorCoordenada = resumen?.conteoPorCoordenada ?: emptyMap(),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        BotonBorde(
            onClick = onVerDetalle,
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 8.dp),
        ) {
            Text("Ver detalle", color = Color.White)
        }
    }
}
