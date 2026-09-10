package com.pablock.tca.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface EntrenamientoDao {
    @Insert
    suspend fun insertar(entrenamiento: Entrenamiento): Long

    @Update
    suspend fun actualizar(entrenamiento: Entrenamiento)

    @Query("SELECT * FROM entrenamientos WHERE id = :id")
    suspend fun obtenerPorId(id: Long): Entrenamiento?

    @Query("SELECT * FROM entrenamientos WHERE finalizado = 1 ORDER BY id DESC")
    fun obtenerFinalizados(): Flow<List<Entrenamiento>>

    @Query("SELECT COUNT(*) FROM entrenamientos WHERE fecha = :fecha AND finalizado = 1")
    suspend fun contarFinalizadosEnFecha(fecha: String): Int

    // Borra en cascada sus series y flechas (ForeignKey.CASCADE en Serie/Flecha).
    @Query("DELETE FROM entrenamientos WHERE id IN (:ids)")
    suspend fun eliminarPorIds(ids: List<Long>)
}

@Dao
interface SerieDao {
    @Insert
    suspend fun insertar(serie: Serie): Long

    @Query("SELECT * FROM series WHERE entrenamientoId = :entrenamientoId ORDER BY orden")
    suspend fun obtenerPorEntrenamiento(entrenamientoId: Long): List<Serie>

    @Query("SELECT * FROM series WHERE entrenamientoId = :entrenamientoId ORDER BY orden")
    fun observarPorEntrenamiento(entrenamientoId: Long): Flow<List<Serie>>

    @Query("SELECT COUNT(*) FROM series WHERE entrenamientoId = :entrenamientoId")
    suspend fun contarPorEntrenamiento(entrenamientoId: Long): Int
}

@Dao
interface FlechaDao {
    @Insert
    suspend fun insertarTodas(flechas: List<Flecha>)

    @Query("SELECT * FROM flechas WHERE serieId = :serieId ORDER BY orden")
    suspend fun obtenerPorSerie(serieId: Long): List<Flecha>

    @Query("SELECT * FROM flechas WHERE serieId IN (:serieIds) ORDER BY serieId, orden")
    suspend fun obtenerPorSeries(serieIds: List<Long>): List<Flecha>
}

@Dao
interface NotaDao {
    @Insert
    suspend fun insertar(nota: Nota): Long

    @Update
    suspend fun actualizar(nota: Nota)

    @Query("SELECT * FROM notas ORDER BY id")
    fun observarTodas(): Flow<List<Nota>>

    @Query("SELECT COUNT(*) FROM notas WHERE esNota0 = 0")
    suspend fun contarNoEspeciales(): Int

    // Guarda de esNota0 = 0 también aquí, no solo en la UI, por si acaso.
    @Query("DELETE FROM notas WHERE id = :id AND esNota0 = 0")
    suspend fun eliminar(id: Long)
}

@Dao
interface DianaConfigDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardar(config: DianaConfig)

    @Query("SELECT * FROM diana_config WHERE distancia = :distancia")
    suspend fun obtenerPorDistancia(distancia: Double): DianaConfig?

    @Query("SELECT * FROM diana_config ORDER BY distancia")
    fun observarTodas(): Flow<List<DianaConfig>>

    @Query("SELECT * FROM diana_config ORDER BY distancia")
    suspend fun obtenerTodas(): List<DianaConfig>

    @Query("SELECT COUNT(*) FROM diana_config")
    suspend fun contar(): Int

    @Query("DELETE FROM diana_config WHERE distancia = :distancia")
    suspend fun eliminar(distancia: Double)
}
