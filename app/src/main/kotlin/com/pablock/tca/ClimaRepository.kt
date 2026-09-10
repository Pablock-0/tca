package com.pablock.tca

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

fun hayInternet(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
    val red = cm.activeNetwork ?: return false
    val capacidades = cm.getNetworkCapabilities(red) ?: return false
    return capacidades.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}

class ClimaRepository(private val context: Context) {

    // Descarga el pronóstico de 5 días/3h de OpenWeatherMap y se queda solo con el
    // resumen de "hoy" (viento/humedad/precipitación actuales, mín/máx del día) — ver
    // prompt: Herramientas > Clima. Respeta el candado de peticiones/día del llamador.
    suspend fun descargarReporte(ciudad: String, apiKey: String): Result<ReporteClima> = withContext(Dispatchers.IO) {
        runCatching {
            val url = "https://api.openweathermap.org/data/2.5/forecast?q=${URLEncoder.encode(ciudad, "UTF-8")}" +
                "&appid=$apiKey&units=metric&lang=es"
            val conexion = URL(url).openConnection() as HttpURLConnection
            conexion.connectTimeout = 10_000
            conexion.readTimeout = 10_000
            try {
                if (conexion.responseCode != 200) {
                    error("OpenWeatherMap respondió ${conexion.responseCode}")
                }
                val cuerpo = conexion.inputStream.bufferedReader().use { it.readText() }
                parsearReporteDeHoy(ciudad, cuerpo)
            } finally {
                conexion.disconnect()
            }
        }
    }

    private fun parsearReporteDeHoy(ciudad: String, cuerpoJson: String): ReporteClima {
        val raiz = JSONObject(cuerpoJson)
        val lista = raiz.getJSONArray("list")
        val hoy = Instant.now().atZone(ZoneId.systemDefault()).toLocalDate()
        val formato = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

        var tempMin = Double.MAX_VALUE
        var tempMax = -Double.MAX_VALUE
        var precipitacionMax = 0.0
        var vientoActual = 0.0
        var humedadActual = 0
        var menorDiferencia = Long.MAX_VALUE
        val ahoraEpoch = Instant.now().epochSecond

        for (i in 0 until lista.length()) {
            val entrada = lista.getJSONObject(i)
            val fechaHora = java.time.LocalDateTime.parse(entrada.getString("dt_txt"), formato)
                .atZone(ZoneId.systemDefault())
            if (fechaHora.toLocalDate() != hoy) continue

            val main = entrada.getJSONObject("main")
            tempMin = minOf(tempMin, main.getDouble("temp_min"))
            tempMax = maxOf(tempMax, main.getDouble("temp_max"))
            precipitacionMax = maxOf(precipitacionMax, entrada.optDouble("pop", 0.0))

            val diferencia = kotlin.math.abs(entrada.getLong("dt") - ahoraEpoch)
            if (diferencia < menorDiferencia) {
                menorDiferencia = diferencia
                vientoActual = entrada.getJSONObject("wind").getDouble("speed")
                humedadActual = main.getInt("humidity")
            }
        }

        // Si ya pasaron todas las franjas de "hoy" (por el desfase UTC del API), usa
        // sencillamente la primera franja disponible como mínimo/máximo/actual.
        if (tempMin == Double.MAX_VALUE && lista.length() > 0) {
            val primera = lista.getJSONObject(0)
            val main = primera.getJSONObject("main")
            tempMin = main.getDouble("temp_min")
            tempMax = main.getDouble("temp_max")
            precipitacionMax = primera.optDouble("pop", 0.0)
            vientoActual = primera.getJSONObject("wind").getDouble("speed")
            humedadActual = main.getInt("humidity")
        }

        return ReporteClima(
            ciudad = ciudad,
            vientoMs = vientoActual,
            humedadPorciento = humedadActual,
            precipitacionPorciento = (precipitacionMax * 100).toInt(),
            tempMinC = tempMin,
            tempMaxC = tempMax,
            actualizadoEpochMs = System.currentTimeMillis(),
        )
    }
}
