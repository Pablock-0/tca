package com.pablock.tca.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "entrenamientos")
data class Entrenamiento(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    // Fecha en formato yyyy-MM-dd — es la clave para agrupar/ordenar en Estadísticas
    // y para el sufijo "(2)", "(3)" cuando ya existe un entrenamiento ese día.
    val fecha: String,
    // Null mientras el entrenamiento sigue en curso (aún no se presiona
    // "Terminar entrenamiento"); se llena con el nombre elegido al finalizar.
    val nombre: String? = null,
    val finalizado: Boolean = false,
)

@Entity(
    tableName = "series",
    foreignKeys = [
        ForeignKey(
            entity = Entrenamiento::class,
            parentColumns = ["id"],
            childColumns = ["entrenamientoId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("entrenamientoId")],
)
data class Serie(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val entrenamientoId: Long,
    // Valor del contador alcanzado al guardar esta serie (1, 2, 3...).
    val orden: Int,
    val distancia: Double,
    val dianaCm: Int,
    val tiempoSegundos: Long,
    // Null si la serie se guardó con Guardar (hay filas reales en Flecha). Si se avanzó
    // el contador sin Guardar, no se conocen los datos de cada flecha — aquí se guarda
    // una estimación (promedio de "flechas por serie" de este entrenamiento, o si no hay
    // ninguna serie con datos reales, "Flechas default" de Ajustes) para no perder el
    // tiempo/distancia de esa serie en las estadísticas.
    val flechasEstimadas: Int? = null,
)

@Entity(
    tableName = "flechas",
    foreignKeys = [
        ForeignKey(
            entity = Serie::class,
            parentColumns = ["id"],
            childColumns = ["serieId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("serieId")],
)
data class Flecha(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val serieId: Long,
    // Número de flecha dentro de la serie (1, 2, 3...), como se muestra en pantalla ("1-8").
    val orden: Int,
    // Puntaje: 0-10 normal, PUNTAJE_X (11) para el anillo X — ver DianaLogic.
    val puntaje: Int,
    // Ubicación radial en formato azimut (0-359°, 0 = norte, sentido horario). Siempre se
    // guarda así aunque Ajustes > Estadísticas muestre "Reloj" — esa es solo transformación de UI.
    val ubicacionRadialAzimut: Int,
)

@Entity(tableName = "diana_config")
data class DianaConfig(
    @PrimaryKey val distancia: Double,
    val dianaCm: Int,
)

@Entity(tableName = "notas")
data class Nota(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val titulo: String,
    val contenido: String = "",
    // "Nota 0": se crea sola en cuanto el usuario escribe sin haber agregado ninguna
    // nota todavía; se puede renombrar pero nunca borrar (ver Editar en Notas).
    val esNota0: Boolean = false,
)
