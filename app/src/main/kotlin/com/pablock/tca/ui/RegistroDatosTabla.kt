package com.pablock.tca.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.pablock.tca.FlechaPendiente
import com.pablock.tca.PUNTAJE_X
import com.pablock.tca.textoRelojAAzimut

const val FILAS_DATOS_DEFAULT = 6

private val COLOR_BORDE_CAMPO = Color(0xFFCCCCCC)

data class FilaDatos(val puntajeTexto: String = "", val ubicacionTexto: String = "")

// Convierte las filas escritas a mano en flechas reales, ignorando las que no tengan
// un puntaje válido (vacías o a medio escribir). El UR se interpreta como reloj u
// azimut según Ajustes > Estadísticas (ver textoRelojAAzimut / DianaLogic).
fun parsearFilasDatos(filas: List<FilaDatos>, ubicacionReloj: Boolean): List<FlechaPendiente> =
    filas.mapNotNull { fila ->
        val puntajeTexto = fila.puntajeTexto.trim()
        val puntaje = when {
            puntajeTexto.equals("x", ignoreCase = true) -> PUNTAJE_X
            else -> puntajeTexto.toIntOrNull()?.coerceIn(0, 10)
        } ?: return@mapNotNull null
        val azimut = if (ubicacionReloj) {
            textoRelojAAzimut(fila.ubicacionTexto) ?: 0
        } else {
            fila.ubicacionTexto.trim().toIntOrNull()?.let { ((it % 360) + 360) % 360 } ?: 0
        }
        FlechaPendiente(puntaje, azimut)
    }

// Ajustes > Registro > Datos: en vez de tocar la diana, el usuario escribe P y UR a
// mano fila por fila, con un bote de basura por fila y "+Agregar" al final. Centrada
// en la pantalla, con los campos de texto marcados con un borde para que se note
// dónde tocar (antes eran invisibles).
@Composable
fun RegistroDatosTabla(
    filas: List<FilaDatos>,
    onFilasCambiadas: (List<FilaDatos>) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Row(modifier = Modifier.padding(bottom = 8.dp)) {
            Text("P", color = Color.White.copy(alpha = 0.6f), modifier = Modifier.width(70.dp))
            Text("UR", color = Color.White.copy(alpha = 0.6f), modifier = Modifier.width(90.dp))
        }
        filas.forEachIndexed { i, fila ->
            Row(
                modifier = Modifier.padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CampoTexto(
                    valor = fila.puntajeTexto,
                    onCambiar = { nuevo -> onFilasCambiadas(filas.toMutableList().also { it[i] = fila.copy(puntajeTexto = nuevo) }) },
                    modifier = Modifier.width(60.dp),
                )
                CampoTexto(
                    valor = fila.ubicacionTexto,
                    onCambiar = { nuevo -> onFilasCambiadas(filas.toMutableList().also { it[i] = fila.copy(ubicacionTexto = nuevo) }) },
                    modifier = Modifier.width(80.dp).padding(start = 10.dp),
                )
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Quitar fila",
                    tint = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier
                        .padding(start = 12.dp)
                        .clickable { onFilasCambiadas(filas.toMutableList().also { it.removeAt(i) }) },
                )
            }
        }
        TextButton(onClick = { onFilasCambiadas(filas + FilaDatos()) }) {
            Text("+Agregar", color = Color.White)
        }
    }
}

@Composable
private fun CampoTexto(valor: String, onCambiar: (String) -> Unit, modifier: Modifier = Modifier) {
    BasicTextField(
        value = valor,
        onValueChange = onCambiar,
        textStyle = TextStyle(color = Color.White),
        modifier = modifier
            .border(BorderStroke(1.dp, COLOR_BORDE_CAMPO), RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
    )
}
