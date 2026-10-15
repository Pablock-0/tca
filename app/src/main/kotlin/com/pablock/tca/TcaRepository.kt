package com.pablock.tca

import android.content.Context
import com.pablock.tca.db.DianaConfig
import com.pablock.tca.db.Entrenamiento
import com.pablock.tca.db.Flecha
import com.pablock.tca.db.Nota
import com.pablock.tca.db.Serie
import com.pablock.tca.db.TcaDatabase
import com.pablock.tca.ui.DISTANCIAS_DEFAULT
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

private val FORMATO_FECHA = DateTimeFormatter.ofPattern("d/M/yy")

fun formatFecha(fecha: LocalDate): String = fecha.format(FORMATO_FECHA)

class TcaRepository(private val context: Context) {
    private val db = TcaDatabase.obtener(context)

    suspend fun resolverDianaCm(distancia: Double): Int =
        db.dianaConfigDao().obtenerPorDistancia(distancia)?.dianaCm
            ?: AjustesPrefs.dianaDefaultCm(context).first()

    suspend fun guardarDianaParaDistancia(distancia: Double, dianaCm: Int) {
        db.dianaConfigDao().guardar(DianaConfig(distancia = distancia, dianaCm = dianaCm))
    }

    suspend fun distanciasGuardadas(): List<DianaConfig> = db.dianaConfigDao().obtenerTodas()

    fun dianasGuardadasFlow(): Flow<List<DianaConfig>> = db.dianaConfigDao().observarTodas()

    suspend fun eliminarDiana(distancia: Double) {
        db.dianaConfigDao().eliminar(distancia)
        // Si la distancia borrada era la "Distancia default" y no es una de las 7 de
        // fábrica (esas siempre siguen seleccionables), cae a "Ninguna" en vez de quedar
        // apuntando a un valor que ya no aparece en el desplegable.
        if (distancia !in DISTANCIAS_DEFAULT && AjustesPrefs.distanciaDefault(context) == distancia) {
            AjustesPrefs.setDistanciaDefault(context, null)
        }
    }

    suspend fun obtenerEntrenamiento(id: Long): Entrenamiento? = db.entrenamientoDao().obtenerPorId(id)

    suspend fun marcarEnEspera() {
        EntrenamientoEnCursoPrefs.marcarEsperaSiNecesario(context, System.currentTimeMillis())
    }

    // Único camino para avanzar el contador (contador simple o Guardar). A partir de la
    // segunda vez en un entrenamiento siempre crea una serie (para no perder tiempo/
    // distancia), pero si guardar=false no hay datos por flecha — solo una cantidad
    // estimada, ver estimarFlechasSinDatos. El primer click de un entrenamiento nuevo
    // (Iniciar -> 00) NO crea serie: todavía no hay diana visible con la que tirar, así
    // que sería una serie fantasma cuyo "tiempo" en realidad es la espera desde que
    // terminó el entrenamiento anterior, no el de una serie real.
    suspend fun avanzarContador(
        guardar: Boolean,
        flechasPendientes: List<FlechaPendiente>,
        distancia: Double,
    ) {
        val estadoPrevio = EntrenamientoEnCursoPrefs.leer(context)
        val ahora = System.currentTimeMillis()
        val deltaSegundos = ((ahora - estadoPrevio.ultimoClickMs) / 1000).coerceAtLeast(0)

        val esPrimerClickDeEntrenamientoNuevo = estadoPrevio.entrenamientoId == null
        val entrenamientoId = estadoPrevio.entrenamientoId
            ?: db.entrenamientoDao().insertar(Entrenamiento(fecha = formatFecha(LocalDate.now())))

        if (!esPrimerClickDeEntrenamientoNuevo || flechasPendientes.isNotEmpty()) {
            // Se resuelve justo aquí (no se cachea) para que, si a media serie el usuario
            // cambió Ajustes > Diana default o Editar dianas, Guardar tome siempre el
            // valor más reciente para esta distancia — ver nota del prompt sobre esto.
            val dianaCm = resolverDianaCm(distancia)
            val orden = db.serieDao().contarPorEntrenamiento(entrenamientoId) + 1
            if (guardar) {
                val serieId = db.serieDao().insertar(
                    Serie(
                        entrenamientoId = entrenamientoId,
                        orden = orden,
                        distancia = distancia,
                        dianaCm = dianaCm,
                        tiempoSegundos = deltaSegundos,
                    ),
                )
                if (flechasPendientes.isNotEmpty()) {
                    db.flechaDao().insertarTodas(
                        flechasPendientes.mapIndexed { i, f ->
                            Flecha(serieId = serieId, orden = i + 1, puntaje = f.puntaje, ubicacionRadialAzimut = f.azimut)
                        },
                    )
                }
            } else {
                db.serieDao().insertar(
                    Serie(
                        entrenamientoId = entrenamientoId,
                        orden = orden,
                        distancia = distancia,
                        dianaCm = dianaCm,
                        tiempoSegundos = deltaSegundos,
                        flechasEstimadas = estimarFlechasSinDatos(entrenamientoId),
                    ),
                )
            }
        }

        EntrenamientoEnCursoPrefs.avanzar(context, entrenamientoId, ahora, AjustesPrefs.zoomDefault(context))
    }

    // Promedio de "flechas por serie" entre las series de este entrenamiento que sí
    // tienen datos reales (guardadas con Guardar); si no hay ninguna todavía, usa
    // "Flechas default" de Ajustes (default 6).
    private suspend fun estimarFlechasSinDatos(entrenamientoId: Long): Int {
        val series = db.serieDao().obtenerPorEntrenamiento(entrenamientoId)
        val seriesConDatos = series.filter { it.flechasEstimadas == null }
        if (seriesConDatos.isEmpty()) return AjustesPrefs.flechasDefault(context)
        val totalFlechasReales = db.flechaDao().obtenerPorSeries(seriesConDatos.map { it.id }).size
        return (totalFlechasReales.toDouble() / seriesConDatos.size).roundToInt()
    }

    suspend fun setZoom(activo: Boolean) {
        EntrenamientoEnCursoPrefs.setZoom(context, activo)
    }

    suspend fun quitarUltimaFlecha(flechasPendientes: List<FlechaPendiente>): List<FlechaPendiente> {
        val nuevas = flechasPendientes.dropLast(1)
        EntrenamientoEnCursoPrefs.setFlechasPendientes(context, nuevas)
        return nuevas
    }

    suspend fun registrarFlecha(flechasPendientes: List<FlechaPendiente>, toque: ToqueDiana): List<FlechaPendiente> {
        val nuevas = flechasPendientes + FlechaPendiente(toque.puntaje, toque.azimutGrados)
        EntrenamientoEnCursoPrefs.setFlechasPendientes(context, nuevas)
        return nuevas
    }

    suspend fun cambiarDistancia(distancia: Double) {
        EntrenamientoEnCursoPrefs.setDistancia(context, distancia)
    }

    // Nombre por default = la fecha del entrenamiento (la del registro, no la de
    // "hoy" — evita líos si el entrenamiento cruzó la medianoche); si ya hay otro
    // finalizado ese mismo día Y el usuario deja el nombre tal cual, se le agrega "(n)".
    suspend fun terminarEntrenamiento(entrenamientoId: Long, nombreElegido: String) {
        val entrenamiento = db.entrenamientoDao().obtenerPorId(entrenamientoId) ?: return
        val fecha = entrenamiento.fecha
        var nombreFinal = nombreElegido.trim().ifBlank { fecha }
        if (nombreFinal == fecha) {
            val yaGuardados = db.entrenamientoDao().contarFinalizadosEnFecha(fecha)
            if (yaGuardados > 0) nombreFinal = "$fecha (${yaGuardados + 1})"
        }
        db.entrenamientoDao().actualizar(entrenamiento.copy(nombre = nombreFinal, finalizado = true))
        EntrenamientoEnCursoPrefs.limpiar(context, System.currentTimeMillis())
        // "Distancia default" (Ajustes) solo se aplica AQUÍ, al terminar un entrenamiento
        // completo — nunca entre series (avanzarContador no la toca). "Ninguna" (null,
        // default) dispensa: la distancia se queda como la última usada, igual que siempre.
        AjustesPrefs.distanciaDefault(context)?.let { EntrenamientoEnCursoPrefs.setDistancia(context, it) }
    }

    fun entrenamientosFinalizados(): Flow<List<Entrenamiento>> = db.entrenamientoDao().obtenerFinalizados()

    // Total de flechas de un entrenamiento (guardado o en curso): suma "Serie.flechasEstimadas"
    // cuando la serie se avanzó solo con el contador (sin Guardar, sin filas en Flecha), o el
    // conteo real de Flecha cuando sí las hay — mismo criterio en cargarResumen, cargarDetalle
    // y el contador en vivo de la pantalla principal (totalFlechasEnCurso), para no divergir.
    private fun sumarFlechasDeSeries(series: List<Serie>, flechasPorSerieId: Map<Long, Int>): Int =
        series.sumOf { it.flechasEstimadas ?: (flechasPorSerieId[it.id] ?: 0) }

    // Estado en vivo para la pantalla principal mientras el entrenamiento sigue abierto —
    // no incluye las flechas de la serie actual aún sin avanzar (esas las suma el
    // ViewModel aparte, ver PrincipalViewModel.estadoVivoEntrenamiento, porque solo él
    // conoce flechasPendientes). Una sola consulta a Room resuelve total + puntaje/posible.
    suspend fun estadoVivoEnCurso(entrenamientoId: Long): EstadoVivoEntrenamiento {
        val series = db.serieDao().obtenerPorEntrenamiento(entrenamientoId)
        val flechas = db.flechaDao().obtenerPorSeries(series.map { it.id })
        val flechasPorSerieId = flechas.groupingBy { it.serieId }.eachCount()
        return EstadoVivoEntrenamiento(
            totalFlechas = sumarFlechasDeSeries(series, flechasPorSerieId),
            puntaje = flechas.sumOf { valorParaPromedio(it.puntaje) },
            puntajePosible = flechas.size * 10,
        )
    }

    suspend fun cargarResumen(entrenamientoId: Long): ResumenEntrenamiento? {
        val entrenamiento = db.entrenamientoDao().obtenerPorId(entrenamientoId) ?: return null
        val series = db.serieDao().obtenerPorEntrenamiento(entrenamientoId)
        val flechas = db.flechaDao().obtenerPorSeries(series.map { it.id })
        val conteo = flechas.groupingBy { it.puntaje to it.ubicacionRadialAzimut }.eachCount()
        // El mapa de calor (conteoPorCoordenada) se queda solo con flechas reales a propósito
        // — las estimadas no tienen coordenada que graficar.
        val flechasPorSerieId = flechas.groupingBy { it.serieId }.eachCount()
        val totalFlechas = sumarFlechasDeSeries(series, flechasPorSerieId)
        return ResumenEntrenamiento(entrenamiento, totalFlechas, conteo)
    }

    suspend fun cargarDetalle(entrenamientoId: Long): DetalleEntrenamiento? {
        val entrenamiento = db.entrenamientoDao().obtenerPorId(entrenamientoId) ?: return null
        val series = db.serieDao().obtenerPorEntrenamiento(entrenamientoId)
        val flechasPorSerieId = db.flechaDao().obtenerPorSeries(series.map { it.id }).groupBy { it.serieId }

        // Número de flechas de una serie: real si se guardó con Guardar, estimado si solo
        // se avanzó el contador (ver Serie.flechasEstimadas).
        fun numeroFlechas(serie: Serie): Int = serie.flechasEstimadas ?: (flechasPorSerieId[serie.id]?.size ?: 0)

        val totalFlechas = series.sumOf(::numeroFlechas)
        val totalSeries = series.size
        val flechasPorSerie = if (totalSeries > 0) totalFlechas.toDouble() / totalSeries else 0.0
        val tiempoPromedio = if (series.isNotEmpty()) series.map { it.tiempoSegundos }.average() else 0.0

        // Solo con datos reales — una serie estimada no tiene puntajes que promediar.
        val promedioPorDistancia = series.groupBy { it.distancia }.map { (distancia, seriesDeEsaDistancia) ->
            val puntajes = seriesDeEsaDistancia.flatMap { flechasPorSerieId[it.id] ?: emptyList() }
                .map { valorParaPromedio(it.puntaje) }
            distancia to puntajes.takeIf { it.isNotEmpty() }?.average()
        }.sortedBy { it.first }

        val conteoDianas = series.groupingBy { it.dianaCm }.eachCount()
        val maxUsos = conteoDianas.values.maxOrNull() ?: 0
        val dianaPrincipal = conteoDianas.filter { it.value == maxUsos }.keys.sorted()

        val detalleSeries = series.map { serie ->
            val flechasOrdenadas = (flechasPorSerieId[serie.id] ?: emptyList()).sortedBy { it.orden }
            DetalleSerie(
                orden = serie.orden,
                distancia = serie.distancia,
                dianaCm = serie.dianaCm,
                tiempoSegundos = serie.tiempoSegundos,
                numeroFlechas = numeroFlechas(serie),
                flechas = flechasOrdenadas.map { Triple(it.orden, it.puntaje, it.ubicacionRadialAzimut) },
            )
        }

        return DetalleEntrenamiento(
            entrenamiento = entrenamiento,
            totalFlechas = totalFlechas,
            totalSeries = totalSeries,
            flechasPorSerie = flechasPorSerie,
            tiempoPromedioSegundos = tiempoPromedio,
            puntajePromedioPorDistancia = promedioPorDistancia,
            dianaPrincipal = dianaPrincipal,
            series = detalleSeries,
        )
    }

    suspend fun eliminarEntrenamientos(ids: Set<Long>) {
        db.entrenamientoDao().eliminarPorIds(ids.toList())
    }

    fun notas(): Flow<List<Nota>> = db.notaDao().observarTodas()

    suspend fun crearNota(): Long {
        val numero = db.notaDao().contarNoEspeciales() + 1
        return db.notaDao().insertar(Nota(titulo = "Nota $numero"))
    }

    // Se crea sola la primera vez que el usuario escribe sin haber agregado ninguna
    // nota todavía (ver prompt: "se guardará como Nota 0").
    suspend fun crearNota0(contenido: String): Long =
        db.notaDao().insertar(Nota(titulo = "Nota 0", contenido = contenido, esNota0 = true))

    suspend fun guardarNota(nota: Nota) {
        db.notaDao().actualizar(nota)
    }

    suspend fun eliminarNota(id: Long) {
        db.notaDao().eliminar(id)
    }

    // Exporta todos los entrenamientos finalizados, una fila por flecha con datos
    // reales — las series avanzadas solo con el contador (sin Guardar) no tienen P/UR
    // real por flecha, así que no aportan filas aquí. "csv" separa con comas, "txt"
    // con tabulador — mismo contenido en ambos casos (ver Ajustes > Exportar).
    suspend fun exportarReporte(formato: String): String {
        val sep = if (formato == "txt") "\t" else ","
        val sb = StringBuilder("fecha_entrenamiento${sep}serie${sep}diana${sep}tiempo${sep}flecha${sep}P${sep}UR\n")
        val entrenamientos = db.entrenamientoDao().obtenerFinalizados().first()
        for (entrenamiento in entrenamientos) {
            val series = db.serieDao().obtenerPorEntrenamiento(entrenamiento.id)
            val flechasPorSerieId = db.flechaDao().obtenerPorSeries(series.map { it.id }).groupBy { it.serieId }
            for (serie in series) {
                val flechas = (flechasPorSerieId[serie.id] ?: emptyList()).sortedBy { it.orden }
                for (flecha in flechas) {
                    sb.append(entrenamiento.fecha).append(sep)
                        .append(serie.orden).append(sep)
                        .append(serie.dianaCm).append(sep)
                        .append(serie.tiempoSegundos).append(sep)
                        .append(flecha.orden).append(sep)
                        .append(textoPuntaje(flecha.puntaje)).append(sep)
                        .append(flecha.ubicacionRadialAzimut).append('\n')
                }
            }
        }
        return sb.toString()
    }
}
