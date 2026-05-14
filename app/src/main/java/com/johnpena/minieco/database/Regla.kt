package com.johnpena.minieco.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "reglas",
    foreignKeys = [
        ForeignKey(
            entity = Curso::class,
            parentColumns = ["id"],
            childColumns = ["cursoId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    // Esto asegura que "Vidrio" solo pueda tener una regla por curso
    indices = [Index(value = ["cursoId", "tipoDesecho"], unique = true)]
)
data class Regla(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val cursoId: Int,
    val tipoDesecho: String,
    val colorTacho: String
)