package com.johnpena.minieco.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Curso::class, Estudiante::class, Regla::class, ReporteTest::class],
    version = 4,
    exportSchema = false
)
abstract class MiniEcoDatabase : RoomDatabase() {

    abstract fun miniEcoDao(): MiniEcoDao

    companion object {
        @Volatile
        private var INSTANCE: MiniEcoDatabase? = null

        fun getDatabase(context: Context): MiniEcoDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MiniEcoDatabase::class.java,
                    "minieco_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}