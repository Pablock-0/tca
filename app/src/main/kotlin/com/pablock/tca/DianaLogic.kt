package com.pablock.tca

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

// Puntaje "X" se guarda distinto de 10 (ver prompt: "X != 10"), aunque para
// promedios/sumas de estadísticas vale igual que un 10 (valorParaPromedio).
const val PUNTAJE_X = 11

fun textoPuntaje(puntaje: Int): String = if (puntaje == PUNTAJE_X) "X" else puntaje.toString()

fun valorParaPromedio(puntaje: Int): Int = if (puntaje == PUNTAJE_X) 10 else puntaje

data class ToqueDiana(val puntaje: Int, val azimutGrados: Int)

// Ajustes > Estadísticas > Ubicación radial: Reloj. En el backend siempre se guarda en
// azimut (0-359°, 0=norte); esto solo transforma cómo se muestra/escribe como texto —
// igual que la hora del calendario del hub: 105 son 1:05 (últimos 2 dígitos = minutos).
fun azimutATextoReloj(azimutGrados: Int): String {
    val totalMinutosReloj = azimutGrados * 2
    val horas = totalMinutosReloj / 60
    val minutos = totalMinutosReloj % 60
    val horasMostradas = if (horas == 0) 12 else horas
    return "$horasMostradas.${minutos.toString().padStart(2, '0')}"
}

// Inversa: interpreta dígitos escritos a mano (sin separador) como hora de reloj y
// los convierte a azimut. Devuelve null si no se puede interpretar como una hora válida.
fun textoRelojAAzimut(texto: String): Int? {
    val digitos = texto.filter { it.isDigit() }
    if (digitos.length < 3) return null
    val minutos = digitos.takeLast(2).toIntOrNull() ?: return null
    val horas = digitos.dropLast(2).toIntOrNull() ?: return null
    if (minutos > 59 || horas !in 1..12) return null
    val totalMinutosReloj = (horas % 12) * 60 + minutos
    return (totalMinutosReloj / 2.0).roundToInt().mod(360)
}

// Fracción del radio real (0..1) que cubre el círculo visible: en modo normal
// llega hasta el borde del 1 (1.0); en Zoom solo se dibuja/registra hasta el
// borde del 6 (0.5), estirado para ocupar el mismo espacio en pantalla — por
// eso cada banda se ve (y se toca) al doble de ancho. Ver Modo Zoom del prompt.
private const val T_REAL_MAXIMO_NORMAL = 1.0
private const val T_REAL_MAXIMO_ZOOM = 0.5

// dx/dy: offset del toque respecto al centro de la diana, en píxeles (coordenadas
// de Compose: y crece hacia abajo). radioPx: radio del círculo exterior visible
// (el "10" ocupa 1/10 de ese radio en modo normal, el "1" el 1/10 más externo —
// 10 bandas iguales, estándar de tiro con arco; X es la mitad interior del 10).
// En Zoom, un toque más allá del círculo visible (que ya solo llega al 6) se
// registra como 5 — es la banda real más cercana a lo que se ve, y evita que
// un toque fuera de la diana se pierda como un 0 franco mientras está el Zoom.
fun calcularToque(dx: Float, dy: Float, radioPx: Float, zoom: Boolean = false): ToqueDiana {
    val distancia = hypot(dx, dy)

    var grados = Math.toDegrees(atan2(dx.toDouble(), -dy.toDouble()))
    if (grados < 0) grados += 360.0
    val azimut = grados.toInt().coerceIn(0, 359)

    if (distancia > radioPx) {
        val puntajeFuera = if (zoom) 5 else 0
        return ToqueDiana(puntaje = puntajeFuera, azimutGrados = azimut)
    }

    val maximo = if (zoom) T_REAL_MAXIMO_ZOOM else T_REAL_MAXIMO_NORMAL
    val t = (distancia / radioPx).toDouble() * maximo
    val banda = (t / 0.1).toInt().coerceIn(0, 9)
    val puntaje = if (banda == 0 && t < 0.05) PUNTAJE_X else 10 - banda
    return ToqueDiana(puntaje = puntaje, azimutGrados = azimut)
}

// Radio (como fracción de radioPx) donde se dibuja cualquier flecha que quede
// fuera del área visible: un poco más allá del borde del círculo.
private const val T_FUERA_DE_VISTA = 1.15

// Inversa de calcularToque: a partir de los datos guardados (P y UR) reconstruye
// dónde dibujar el punto verde — nunca es una copia exacta del toque original,
// solo el punto medio de la banda correspondiente (ver nota del prompt sobre
// que las estadísticas se reconstruyen a partir de los datos, no del marcaje
// exacto). Para P=0 (fuera de la diana), o para cualquier P del 0 al 5 mientras
// el Zoom está activo (nada de eso es visible en ese modo, que solo llega al 6),
// se dibuja siempre en el mismo anillo un poco más allá del borde — antes cada
// puntaje bajo caía a una distancia distinta y parecía un glitch al comparar.
fun posicionEnDiana(puntaje: Int, azimutGrados: Int, radioPx: Float, zoom: Boolean = false): Pair<Float, Float> {
    val t = if (zoom && puntaje in 0..5) {
        T_FUERA_DE_VISTA
    } else {
        val tReal = when {
            puntaje == 0 -> T_FUERA_DE_VISTA
            puntaje == PUNTAJE_X -> 0.025
            // El 10 real ocupa [0.05, 0.1] (la otra mitad de esa banda es la X), así que
            // su punto medio es 0.075, no 0.05 — con 0.05 el punto quedaba pegado al
            // borde de la X y confundía cuál de los dos era.
            puntaje == 10 -> 0.075
            else -> {
                val banda = 10 - puntaje
                banda * 0.1 + 0.05
            }
        }
        val maximo = if (zoom) T_REAL_MAXIMO_ZOOM else T_REAL_MAXIMO_NORMAL
        (tReal / maximo).coerceAtMost(T_FUERA_DE_VISTA)
    }
    val rad = Math.toRadians(azimutGrados.toDouble())
    val dx = (t * radioPx * sin(rad)).toFloat()
    val dy = (-t * radioPx * cos(rad)).toFloat()
    return dx to dy
}
