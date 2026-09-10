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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HerramientasScreen(
    onIrAClima: () -> Unit,
    onIrANivel: () -> Unit,
    onIrABrujula: () -> Unit,
    onIrAInicio: () -> Unit,
    onNotas: () -> Unit,
    onEstadisticas: () -> Unit,
    onAjustes: () -> Unit,
) {
    val hayEntrenamientoEnCurso by hayEntrenamientoEnCursoState()
    var menuAbierto by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
    Column(modifier = Modifier.fillMaxSize().background(Color.Black).padding(horizontal = 16.dp)) {
        Box(modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(top = 8.dp, bottom = 16.dp)) {
            Icon(
                Icons.Default.Menu,
                contentDescription = "Menú",
                tint = Color.White,
                modifier = Modifier.clickable { menuAbierto = true }.padding(8.dp),
            )
            Text("Herramientas", color = Color.White, fontSize = 24.sp, modifier = Modifier.align(Alignment.Center))
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            FilaHerramienta("Clima", onIrAClima)
            FilaHerramienta("Nivel", onIrANivel)
            FilaHerramienta("Brújula", onIrABrujula)
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
        onHerramientas = { menuAbierto = false },
        onEstadisticas = { menuAbierto = false; onEstadisticas() },
        onAjustes = { menuAbierto = false; onAjustes() },
    )
    }
}

// Altura ~300% de la fila original (padding 16dp + una línea de texto ≈ 56dp) — el
// ancho no cambia, sigue ocupando todo el ancho disponible.
private val ALTURA_FILA_HERRAMIENTA = 168.dp

@Composable
private fun FilaHerramienta(titulo: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(ALTURA_FILA_HERRAMIENTA)
            .clickable(onClick = onClick)
            .border(BorderStroke(1.5.dp, ColorBordeTca), RoundedCornerShape(6.dp))
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(titulo, color = Color.White, fontSize = 18.sp)
    }
}
