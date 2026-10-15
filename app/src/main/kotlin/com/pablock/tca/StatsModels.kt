package com.pablock.tca

import com.pablock.tca.db.Entrenamiento

// Estado en vivo de un entrenamiento en curso, para la pantalla principal (total de
// flechas arriba de la diana, y puntaje/posible en su esquina si Ajustes > "Score en
// vivo" está encendido) — una sola consulta a Room resuelve los tres a la vez.
data class EstadoVivoEntrenamiento(
    val totalFlechas: Int,
    // Suma solo sobre flechas reales (con puntaje guardado) de las series ya guardadas
    // con Guardar; las series avanzadas solo con el contador no aportan — no hay
    // puntaje que contar. X cuenta como 10 (valorParaPromedio), igual que en promedios.
    val puntaje: Int,
    // 10 × la cantidad de esas mismas flechas reales (no todas las flechas del
    // entrenamiento: inflar el posible con flechas de valor desconocido no tendría sentido).
    val puntajePosible: Int,
)

data class ResumenEntrenamiento(
    val entrenamiento: Entrenamiento,
    val totalFlechas: Int,
    // (puntaje, ubicaciónRadialAzimut) -> cuántas flechas cayeron exactamente ahí.
    val conteoPorCoordenada: Map<Pair<Int, Int>, Int>,
)

data class DetalleSerie(
    val orden: Int,
    val distancia: Double,
    val dianaCm: Int,
    val tiempoSegundos: Long,
    // Real (flechas.size) si la serie se guardó con Guardar, o estimado (ver
    // TcaRepository.avanzarContador) si se avanzó solo con el contador.
    val numeroFlechas: Int,
    // (orden de la flecha, puntaje, ubicación radial), ordenadas — vacía si numeroFlechas
    // es una estimación (no se conocen los datos de cada flecha, solo la cantidad).
    val flechas: List<Triple<Int, Int, Int>>,
)

data class DetalleEntrenamiento(
    val entrenamiento: Entrenamiento,
    val totalFlechas: Int,
    val totalSeries: Int,
    val flechasPorSerie: Double,
    val tiempoPromedioSegundos: Double,
    // distancia -> puntaje promedio (valorParaPromedio) de las flechas tiradas ahí;
    // null si esa distancia solo tuvo series estimadas (sin datos reales de flecha).
    val puntajePromedioPorDistancia: List<Pair<Double, Double?>>,
    // tamaños de diana (cm) empatados como los más usados — normalmente uno solo.
    val dianaPrincipal: List<Int>,
    val series: List<DetalleSerie>,
)
