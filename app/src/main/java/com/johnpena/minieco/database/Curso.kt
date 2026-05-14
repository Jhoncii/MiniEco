package com.johnpena.minieco.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cursos")
data class Curso(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nombre: String,
    val descripcion: String
)