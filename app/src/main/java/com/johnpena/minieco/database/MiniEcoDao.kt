package com.johnpena.minieco.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface MiniEcoDao {
    // --- CURSOS ---
    @Insert suspend fun insertarCurso(curso: Curso)
    @Query("SELECT * FROM cursos") suspend fun obtenerTodosLosCursos(): List<Curso>
    @Query("DELETE FROM cursos WHERE id = :id") suspend fun eliminarCursoPorId(id: Int)
    @Update suspend fun actualizarCurso(curso: Curso)

    // --- REGLAS ---
    @Insert suspend fun insertarRegla(regla: Regla)
    @Query("SELECT * FROM reglas WHERE cursoId = :cursoId") suspend fun obtenerReglasPorCurso(cursoId: Int): List<Regla>
    @Delete suspend fun eliminarRegla(regla: Regla)
    @Update suspend fun actualizarRegla(regla: Regla)

    // --- ESTUDIANTES ---
    @Insert suspend fun insertarEstudiante(estudiante: Estudiante)
    @Query("SELECT * FROM estudiantes WHERE cursoId = :cursoId") suspend fun obtenerEstudiantesPorCurso(cursoId: Int): List<Estudiante>
    @Delete suspend fun eliminarEstudiante(estudiante: Estudiante) // NUEVA ORDEN PARA BORRAR
    @Update suspend fun actualizarEstudiante(estudiante: Estudiante) // NUEVA ORDEN PARA EDITAR

    // --- REPORTES ---
    @Insert suspend fun insertarReporte(reporte: ReporteTest)
    @Query("SELECT * FROM reportes_test WHERE estudianteId = :estudianteId ORDER BY fecha DESC") suspend fun obtenerReportesPorEstudiante(estudianteId: Int): List<ReporteTest>
}