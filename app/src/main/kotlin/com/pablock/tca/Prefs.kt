package com.pablock.tca

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

val Context.tcaDataStore by preferencesDataStore(name = "tca_prefs")

enum class RegistroModo { DIANA, DATOS }

data class FlechaPendiente(val puntaje: Int, val azimut: Int)

private fun List<FlechaPendiente>.aJson(): String {
    val arr = JSONArray()
    for (f in this) arr.put(JSONObject().put("p", f.puntaje).put("az", f.azimut))
    return arr.toString()
}

private fun String.aFlechasPendientes(): List<FlechaPendiente> {
    if (isBlank()) return emptyList()
    val arr = JSONArray(this)
    return (0 until arr.length()).map {
        val obj = arr.getJSONObject(it)
        FlechaPendiente(obj.getInt("p"), obj.getInt("az"))
    }
}

// Estado del entrenamiento en curso, persistido para sobrevivir a que el proceso
// muera (Android puede matar la app en segundo plano) — por eso no vive solo en
// memoria de un ViewModel, sino en DataStore. Se limpia al terminar el entrenamiento.
object EntrenamientoEnCursoPrefs {
    private val ENTRENAMIENTO_ID = longPreferencesKey("entrenamiento_en_curso_id")
    private val CONTADOR = intPreferencesKey("contador_valor") // -1 = "Iniciar" (no empezado)
    private val DISTANCIA = doublePreferencesKey("distancia_actual")
    private val ULTIMO_CLICK_MS = longPreferencesKey("ultimo_click_epoch_ms")
    private val FLECHAS_PENDIENTES = stringPreferencesKey("flechas_pendientes_json")
    private val ZOOM_ACTIVO = booleanPreferencesKey("zoom_activo")

    data class Estado(
        val entrenamientoId: Long?,
        val contador: Int,
        val distancia: Double,
        val ultimoClickMs: Long,
        val flechasPendientes: List<FlechaPendiente>,
        val zoomActivo: Boolean,
    )

    fun observar(context: Context): Flow<Estado> =
        context.tcaDataStore.data.map { prefs -> prefs.aEstado() }

    suspend fun leer(context: Context): Estado = context.tcaDataStore.data.first().aEstado()

    private fun Preferences.aEstado(): Estado {
        val id = this[ENTRENAMIENTO_ID]
        return Estado(
            entrenamientoId = if (id == null || id < 0) null else id,
            contador = this[CONTADOR] ?: -1,
            distancia = this[DISTANCIA] ?: 10.0,
            ultimoClickMs = this[ULTIMO_CLICK_MS] ?: 0L,
            flechasPendientes = this[FLECHAS_PENDIENTES]?.aFlechasPendientes() ?: emptyList(),
            zoomActivo = this[ZOOM_ACTIVO] ?: false,
        )
    }

    // Marca el instante desde el que se cuenta el tiempo "Inicio -> 00" — solo la
    // primera vez que la pantalla se ve en estado "Iniciar" (ultimoClickMs sin
    // usar todavía), para que ese primer intervalo tenga una duración real.
    suspend fun marcarEsperaSiNecesario(context: Context, ahoraMs: Long) {
        context.tcaDataStore.edit { prefs ->
            if (prefs[ENTRENAMIENTO_ID] == null && (prefs[ULTIMO_CLICK_MS] ?: 0L) == 0L) {
                prefs[ULTIMO_CLICK_MS] = ahoraMs
            }
        }
    }

    // Un solo camino para "avanzar el contador", lo dispare un click simple sobre
    // el contador/botón grande "Iniciar" o un doble click en Guardar: crea el
    // entrenamiento en curso si hace falta (primer click, Iniciar -> 00), incrementa
    // el contador y limpia las flechas pendientes (ya se guardaron como serie antes
    // de llamar esto, si correspondía). Si es el primer click de un entrenamiento
    // nuevo, el Zoom arranca en el valor de "Zoom default" (Ajustes); si el
    // entrenamiento ya estaba en curso, el Zoom no se toca (persiste entre series).
    suspend fun avanzar(context: Context, entrenamientoId: Long, ahoraMs: Long, zoomDefaultSiNuevo: Boolean) {
        context.tcaDataStore.edit { prefs ->
            val contadorPrevio = prefs[CONTADOR] ?: -1
            val esNuevo = contadorPrevio < 0
            prefs[ENTRENAMIENTO_ID] = entrenamientoId
            prefs[CONTADOR] = contadorPrevio + 1
            prefs[ULTIMO_CLICK_MS] = ahoraMs
            prefs[FLECHAS_PENDIENTES] = emptyList<FlechaPendiente>().aJson()
            if (esNuevo) prefs[ZOOM_ACTIVO] = zoomDefaultSiNuevo
        }
    }

    suspend fun setDistancia(context: Context, distancia: Double) {
        context.tcaDataStore.edit { prefs -> prefs[DISTANCIA] = distancia }
    }

    suspend fun setFlechasPendientes(context: Context, flechas: List<FlechaPendiente>) {
        context.tcaDataStore.edit { prefs -> prefs[FLECHAS_PENDIENTES] = flechas.aJson() }
    }

    suspend fun setZoom(context: Context, activo: Boolean) {
        context.tcaDataStore.edit { prefs -> prefs[ZOOM_ACTIVO] = activo }
    }

    suspend fun limpiar(context: Context, ahoraMs: Long) {
        context.tcaDataStore.edit { prefs ->
            prefs.remove(ENTRENAMIENTO_ID)
            prefs[CONTADOR] = -1
            prefs[ULTIMO_CLICK_MS] = ahoraMs
            prefs[FLECHAS_PENDIENTES] = emptyList<FlechaPendiente>().aJson()
        }
    }
}

object AjustesPrefs {
    private val DIANA_DEFAULT_CM = intPreferencesKey("diana_default_cm")
    private val NOTIFICACION_ACTIVA = booleanPreferencesKey("notificacion_entrenamiento_activa")
    private val CONTADOR_CLICK_DOBLE = booleanPreferencesKey("contador_click_doble")
    private val CONTADOR_EXTENSION_DIGITOS = intPreferencesKey("contador_extension_digitos")
    private val REGISTRO_MODO = stringPreferencesKey("registro_modo")
    private val UBICACION_RADIAL_RELOJ = booleanPreferencesKey("ubicacion_radial_reloj")

    fun dianaDefaultCm(context: Context): Flow<Int> =
        context.tcaDataStore.data.map { it[DIANA_DEFAULT_CM] ?: 40 }

    suspend fun setDianaDefaultCm(context: Context, cm: Int) {
        context.tcaDataStore.edit { it[DIANA_DEFAULT_CM] = cm }
    }

    fun notificacionActivaFlow(context: Context): Flow<Boolean> =
        context.tcaDataStore.data.map { it[NOTIFICACION_ACTIVA] ?: false }

    suspend fun notificacionActiva(context: Context): Boolean =
        context.tcaDataStore.data.first()[NOTIFICACION_ACTIVA] ?: true

    suspend fun setNotificacionActiva(context: Context, activa: Boolean) {
        context.tcaDataStore.edit { it[NOTIFICACION_ACTIVA] = activa }
    }

    // Click sencillo/doble en Contador y Guardar (default doble).
    fun contadorClickDobleFlow(context: Context): Flow<Boolean> =
        context.tcaDataStore.data.map { it[CONTADOR_CLICK_DOBLE] ?: true }

    suspend fun setContadorClickDoble(context: Context, doble: Boolean) {
        context.tcaDataStore.edit { it[CONTADOR_CLICK_DOBLE] = doble }
    }

    // Cuántos dígitos muestra el contador antes de reiniciar (1-4, default 2;
    // con 4 dígitos cuenta hasta 9999 antes de reiniciar a 0000).
    fun contadorExtensionDigitosFlow(context: Context): Flow<Int> =
        context.tcaDataStore.data.map { it[CONTADOR_EXTENSION_DIGITOS] ?: 2 }

    suspend fun setContadorExtensionDigitos(context: Context, digitos: Int) {
        context.tcaDataStore.edit { it[CONTADOR_EXTENSION_DIGITOS] = digitos.coerceIn(1, 4) }
    }

    fun registroModoFlow(context: Context): Flow<RegistroModo> =
        context.tcaDataStore.data.map { prefs ->
            if (prefs[REGISTRO_MODO] == RegistroModo.DATOS.name) RegistroModo.DATOS else RegistroModo.DIANA
        }

    suspend fun setRegistroModo(context: Context, modo: RegistroModo) {
        context.tcaDataStore.edit { it[REGISTRO_MODO] = modo.name }
    }

    // false = Azimut (default), true = Reloj — solo cambia cómo se muestra la
    // ubicación radial como texto (Estadísticas > Detalle), nunca cómo se guarda.
    fun ubicacionRadialRelojFlow(context: Context): Flow<Boolean> =
        context.tcaDataStore.data.map { it[UBICACION_RADIAL_RELOJ] ?: false }

    suspend fun ubicacionRadialReloj(context: Context): Boolean =
        context.tcaDataStore.data.first()[UBICACION_RADIAL_RELOJ] ?: false

    suspend fun setUbicacionRadialReloj(context: Context, reloj: Boolean) {
        context.tcaDataStore.edit { it[UBICACION_RADIAL_RELOJ] = reloj }
    }

    // Cuántas flechas asumir en una serie que se avanzó solo con el contador (sin
    // Guardar), cuando en ese entrenamiento no hay todavía ninguna serie con datos
    // reales de las que sacar un promedio. Rango 0-99 (ver TcaRepository.avanzarContador).
    // Ajustable desde Ajustes > "Flechas default" cuando esa pantalla exista (Fase 3);
    // por ahora solo se usa el valor default.
    private val FLECHAS_DEFAULT = intPreferencesKey("flechas_default")

    fun flechasDefaultFlow(context: Context): Flow<Int> =
        context.tcaDataStore.data.map { it[FLECHAS_DEFAULT] ?: 6 }

    suspend fun flechasDefault(context: Context): Int =
        context.tcaDataStore.data.first()[FLECHAS_DEFAULT] ?: 6

    suspend fun setFlechasDefault(context: Context, cantidad: Int) {
        context.tcaDataStore.edit { it[FLECHAS_DEFAULT] = cantidad.coerceIn(0, 99) }
    }

    private val ZOOM_DEFAULT = booleanPreferencesKey("zoom_default")

    fun zoomDefaultFlow(context: Context): Flow<Boolean> =
        context.tcaDataStore.data.map { it[ZOOM_DEFAULT] ?: false }

    // "csv" o "txt" — mismo contenido, solo cambia el delimitador/extensión (default csv).
    private val EXPORTAR_FORMATO = stringPreferencesKey("exportar_formato")

    fun exportarFormatoFlow(context: Context): Flow<String> =
        context.tcaDataStore.data.map { it[EXPORTAR_FORMATO] ?: "csv" }

    suspend fun setExportarFormato(context: Context, formato: String) {
        context.tcaDataStore.edit { it[EXPORTAR_FORMATO] = formato }
    }

    suspend fun zoomDefault(context: Context): Boolean =
        context.tcaDataStore.data.first()[ZOOM_DEFAULT] ?: false

    suspend fun setZoomDefault(context: Context, activo: Boolean) {
        context.tcaDataStore.edit { it[ZOOM_DEFAULT] = activo }
    }
}
