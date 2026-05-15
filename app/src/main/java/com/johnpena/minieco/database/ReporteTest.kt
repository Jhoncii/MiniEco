package com.johnpena.minieco.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reportes_test")
data class ReporteTest(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val cursoId: Int,
    val estudianteId: Int,
    val nombreEstudiante: String,
    val fecha: Long,
    val aciertos: Int,
    val totalPreguntas: Int,
    val detallesJson: String
)