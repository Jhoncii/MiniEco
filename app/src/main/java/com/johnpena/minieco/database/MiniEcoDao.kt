package com.johnpena.minieco.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface MiniEcoDao {
    // Cursos
    @Insert suspend fun insertarCurso(curso: Curso)
    @Query("SELECT * FROM cursos") suspend fun obtenerTodosLosCursos(): List<Curso>
    @Query("DELETE FROM cursos WHERE id = :id") suspend fun eliminarCursoPorId(id: Int)
    @Update suspend fun actualizarCurso(curso: Curso)

    // Reglas
    @Insert suspend fun insertarRegla(regla: Regla)
    @Query("SELECT * FROM reglas WHERE cursoId = :cursoId") suspend fun obtenerReglasPorCurso(cursoId: Int): List<Regla>
    @Delete suspend fun eliminarRegla(regla: Regla)
    @Update suspend fun actualizarRegla(regla: Regla)

    // Estudiantes
    @Insert suspend fun insertarEstudiante(estudiante: Estudiante)
    @Query("SELECT * FROM estudiantes WHERE cursoId = :cursoId ORDER BY apellido ASC")
    suspend fun obtenerEstudiantesPorCurso(cursoId: Int): List<Estudiante>
    @Delete suspend fun eliminarEstudiante(estudiante: Estudiante)
    @Update suspend fun actualizarEstudiante(estudiante: Estudiante)
    @Query("SELECT * FROM estudiantes WHERE cedula = :cedula LIMIT 1")
    suspend fun obtenerEstudiantePorCedula(cedula: String): Estudiante?
    @Query("SELECT * FROM estudiantes WHERE cedula = :cedula AND cursoId = :cursoId LIMIT 1")
    suspend fun verificarEstudianteEnCurso(cedula: String, cursoId: Int): Estudiante?

    // Reportes
    @Insert suspend fun insertarReporte(reporte: ReporteTest)
    @Query("SELECT * FROM reportes_test WHERE cursoId = :cursoId ORDER BY fecha DESC") suspend fun obtenerReportesPorCurso(cursoId: Int): List<ReporteTest>
}