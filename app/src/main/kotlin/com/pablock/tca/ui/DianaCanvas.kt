package com.pablock.tca.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import com.pablock.tca.FlechaPendiente
import com.pablock.tca.ToqueDiana
import com.pablock.tca.calcularToque
import com.pablock.tca.posicionEnDiana

// El círculo de la diana no llena todo el cuadrado: deja un margen alrededor
// (la "zona de interacción" del prompt) del mismo grosor que una banda de
// puntaje, donde también se registran los toques fuera de la diana (P=0).
// Ese margen no se dibuja visible en producción.
const val FACTOR_RADIO_DIANA = 10f / 11f

private val COLORES_BANDA = listOf(
    Color(0xFFF5F5F5), // 1-2 blanco
    Color(0xFFF5F5F5),
    Color(0xFF1A1A1A), // 3-4 negro
    Color(0xFF1A1A1A),
    Color(0xFF2255CC), // 5-6 azul
    Color(0xFF2255CC),
    Color(0xFFCC2A2A), // 7-8 rojo
    Color(0xFFCC2A2A),
    Color(0xFFFFC400), // 9-10 oro
    Color(0xFFFFC400),
)

private val COLOR_LINEA = Color(0xFFCCCCCC)
private val COLOR_LINEA_ALTO_CONTRASTE = Color.Black

// Bandas de color + líneas divisorias — compartido entre la diana interactiva
// (pantalla principal) y la de solo lectura (Estadísticas, siempre sin Zoom).
fun DrawScope.dibujarBandasDiana(radioPx: Float, centro: Offset, zoom: Boolean) {
    val bandasVisibles = if (zoom) 5 else 10

    for (banda in (bandasVisibles - 1) downTo 0) {
        val radioBanda = radioPx * (banda + 1) / bandasVisibles
        drawCircle(color = COLORES_BANDA[9 - banda], radius = radioBanda, center = centro)
    }

    for (limite in 1..bandasVisibles) {
        // La línea entre 9 y 10 (limite=1, misma banda dorada de ambos) necesita el
        // mismo contraste alto que la de 10/X — con el gris normal casi no se veía.
        val color = if (limite == 1) COLOR_LINEA_ALTO_CONTRASTE else COLOR_LINEA
        drawCircle(
            color = color,
            radius = radioPx * limite / bandasVisibles,
            center = centro,
            style = Stroke(width = 1.5f),
        )
    }
    drawCircle(
        color = COLOR_LINEA_ALTO_CONTRASTE,
        radius = radioPx * (if (zoom) 0.1f else 0.05f),
        center = centro,
        style = Stroke(width = 1.5f),
    )
}

@Composable
fun DianaInteractiva(
    flechasPendientes: List<FlechaPendiente>,
    zoom: Boolean,
    onToque: (ToqueDiana) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .pointerInput(zoom) {
                detectTapGestures { offset ->
                    val radioPx = size.width / 2f * FACTOR_RADIO_DIANA
                    val dx = offset.x - size.width / 2f
                    val dy = offset.y - size.height / 2f
                    onToque(calcularToque(dx, dy, radioPx, zoom))
                }
            },
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().aspectRatio(1f)) {
            val radioPx = size.width / 2f * FACTOR_RADIO_DIANA
            val centro = Offset(size.width / 2f, size.height / 2f)

            dibujarBandasDiana(radioPx, centro, zoom)

            // Puntos verdes de las flechas ya registradas en esta serie (aún sin guardar).
            // Con Zoom activo, un toque fuera de la diana anota 5 (ver calcularToque) y sí
            // se dibuja, fuera del círculo visible; el 0-4 no tiene dónde mostrarse ahí
            // (esa vista solo llega al 6), así que esos sí se ocultan.
            for (flecha in flechasPendientes) {
                if (zoom && flecha.puntaje in 0..4) continue
                val (dx, dy) = posicionEnDiana(flecha.puntaje, flecha.azimut, radioPx, zoom)
                drawCircle(color = Color(0xFF4CAF50), radius = 7f, center = centro + Offset(dx, dy))
            }
        }
    }
}

// Colores del mapa de calor según cuántas flechas cayeron en la misma coordenada
// exacta (P y UR iguales) — ver Estadísticas del prompt.
private fun colorMapaCalor(cantidad: Int): Color = when {
    cantidad <= 1 -> Color(0xFF8FD98A)
    cantidad == 2 -> Color(0xFF4CAF50)
    cantidad == 3 -> Color(0xFF2E7D32)
    else -> Color(0xFF0F3D0F)
}

// Diana de solo lectura para el resumen de Estadísticas: siempre a escala completa
// (sin Zoom, eso es solo una ayuda para tirar) y sin capturar toques.
@Composable
fun DianaMapaDeCalor(conteoPorCoordenada: Map<Pair<Int, Int>, Int>, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxWidth().aspectRatio(1f)) {
        val radioPx = size.width / 2f * FACTOR_RADIO_DIANA
        val centro = Offset(size.width / 2f, size.height / 2f)

        dibujarBandasDiana(radioPx, centro, zoom = false)

        for ((coordenada, cantidad) in conteoPorCoordenada) {
            val (puntaje, azimut) = coordenada
            val (dx, dy) = posicionEnDiana(puntaje, azimut, radioPx, zoom = false)
            drawCircle(color = colorMapaCalor(cantidad), radius = 7f, center = centro + Offset(dx, dy))
        }
    }
}
