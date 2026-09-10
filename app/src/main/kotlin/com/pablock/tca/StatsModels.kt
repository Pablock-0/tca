package com.pablock.tca

import com.pablock.tca.db.Entrenamiento

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
