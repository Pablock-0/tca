package com.pablock.tca.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Entrenamiento::class, Serie::class, Flecha::class, DianaConfig::class, Nota::class],
    version = 4,
)
abstract class TcaDatabase : RoomDatabase() {
    abstract fun entrenamientoDao(): EntrenamientoDao
    abstract fun serieDao(): SerieDao
    abstract fun flechaDao(): FlechaDao
    abstract fun dianaConfigDao(): DianaConfigDao
    abstract fun notaDao(): NotaDao

    companion object {
        @Volatile private var instancia: TcaDatabase? = null

        fun obtener(context: Context): TcaDatabase =
            instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(
                    context.applicationContext,
                    TcaDatabase::class.java,
                    "tca.db",
                )
                    // Todavía en desarrollo activo (sin usuarios reales) — más simple
                    // recrear la base en cada cambio de esquema que mantener migraciones.
                    .fallbackToDestructiveMigration(true)
                    .build().also { instancia = it }
            }
    }
}
