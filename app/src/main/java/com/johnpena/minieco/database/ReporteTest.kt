package com.johnpena.minieco.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "reportes_test",
    foreignKeys = [
        ForeignKey(
            entity = Estudiante::class,
            parentColumns = ["id"],
            childColumns = ["estudianteId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ReporteTest(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val estudianteId: Int,
    val fecha: Long, // Guardaremos la fecha en milisegundos para precisión exacta
    val notaObtenida: Int,
    val totalDesechos: Int // Cuántos desechos eligió la profesora para este examen
)