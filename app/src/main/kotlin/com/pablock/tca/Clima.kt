package com.pablock.tca

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONObject
import java.time.LocalDate

data class ReporteClima(
    val ciudad: String,
    val vientoMs: Double,
    val humedadPorciento: Int,
    val precipitacionPorciento: Int,
    val tempMinC: Double,
    val tempMaxC: Double,
    val actualizadoEpochMs: Long,
) {
    fun aJson(): String = JSONObject()
        .put("ciudad", ciudad)
        .put("viento", vientoMs)
        .put("humedad", humedadPorciento)
        .put("precipitacion", precipitacionPorciento)
        .put("tempMin", tempMinC)
        .put("tempMax", tempMaxC)
        .put("actualizado", actualizadoEpochMs)
        .toString()

    companion object {
        fun desdeJson(json: String): ReporteClima {
            val o = JSONObject(json)
            return ReporteClima(
                ciudad = o.getString("ciudad"),
                vientoMs = o.getDouble("viento"),
                humedadPorciento = o.getInt("humedad"),
                precipitacionPorciento = o.getInt("precipitacion"),
                tempMinC = o.getDouble("tempMin"),
                tempMaxC = o.getDouble("tempMax"),
                actualizadoEpochMs = o.getLong("actualizado"),
            )
        }
    }
}

// Tope duro de OpenWeatherMap (plan gratuito) y del prompt: nunca más de 40 peticiones
// al día, y con 1 actualización diaria basta — no hace falta estar refrescando seguido.
const val CLIMA_PETICIONES_MAXIMO_DIA = 40

object ClimaPrefs {
    private val CIUDAD = stringPreferencesKey("clima_ciudad")
    private val REPORTE_JSON = stringPreferencesKey("clima_reporte_json")
    private val ULTIMA_ACTUALIZACION_FECHA = stringPreferencesKey("clima_ultima_actualizacion_fecha")
    private val PETICIONES_FECHA = stringPreferencesKey("clima_peticiones_fecha")
    private val PETICIONES_CONTADOR = intPreferencesKey("clima_peticiones_contador")

    fun ciudadFlow(context: Context): Flow<String?> = context.tcaDataStore.data.map { it[CIUDAD] }

    suspend fun setCiudad(context: Context, ciudad: String) {
        context.tcaDataStore.edit { it[CIUDAD] = ciudad }
    }

    fun reporteFlow(context: Context): Flow<ReporteClima?> = context.tcaDataStore.data.map { prefs ->
        prefs[REPORTE_JSON]?.let { runCatching { ReporteClima.desdeJson(it) }.getOrNull() }
    }

    suspend fun guardarReporte(context: Context, reporte: ReporteClima) {
        context.tcaDataStore.edit { prefs ->
            prefs[REPORTE_JSON] = reporte.aJson()
            prefs[ULTIMA_ACTUALIZACION_FECHA] = formatFecha(LocalDate.now())
        }
    }

    // Ya se actualizó hoy: con eso basta, no hace falta volver a pedir (ver prompt).
    suspend fun yaActualizadoHoy(context: Context): Boolean =
        context.tcaDataStore.data.first()[ULTIMA_ACTUALIZACION_FECHA] == formatFecha(LocalDate.now())

    // Candado de peticiones/día — el contador se reinicia solo al cambiar la fecha.
    suspend fun puedeHacerPeticion(context: Context): Boolean {
        val prefs = context.tcaDataStore.data.first()
        val hoy = formatFecha(LocalDate.now())
        val contadorHoy = if (prefs[PETICIONES_FECHA] == hoy) prefs[PETICIONES_CONTADOR] ?: 0 else 0
        return contadorHoy < CLIMA_PETICIONES_MAXIMO_DIA
    }

    suspend fun registrarPeticion(context: Context) {
        val hoy = formatFecha(LocalDate.now())
        context.tcaDataStore.edit { prefs ->
            val contadorPrevio = if (prefs[PETICIONES_FECHA] == hoy) prefs[PETICIONES_CONTADOR] ?: 0 else 0
            prefs[PETICIONES_FECHA] = hoy
            prefs[PETICIONES_CONTADOR] = contadorPrevio + 1
        }
    }
}
