package com.johnpena.minieco

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "cursos")
data class Curso(
    @PrimaryKey(autoGenerate = true) val idCurso: Int = 0,
    val nombreCurso: String
)

@Entity(
    tableName = "reglas_desecho",
    foreignKeys = [ForeignKey(entity = Curso::class, parentColumns = ["idCurso"], childColumns = ["idCursoRelacion"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = ["idCursoRelacion", "desecho"], unique = true)]
)
data class ReglaDesecho(
    @PrimaryKey(autoGenerate = true) val idRegla: Int = 0,
    val idCursoRelacion: Int,
    val desecho: String,
    val colorTacho: String,
    val descripcion: String
)

@Entity(
    tableName = "estudiantes",
    foreignKeys = [ForeignKey(entity = Curso::class, parentColumns = ["idCurso"], childColumns = ["idCursoRelacion"], onDelete = ForeignKey.CASCADE)]
)
data class Estudiante(
    @PrimaryKey(autoGenerate = true) val idEstudiante: Int = 0,
    val nombre: String,
    val apellido: String,
    val idCursoRelacion: Int
)

@Entity(
    tableName = "registros_test",
    foreignKeys = [
        ForeignKey(entity = Curso::class, parentColumns = ["idCurso"], childColumns = ["idCursoRelacion"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Estudiante::class, parentColumns = ["idEstudiante"], childColumns = ["idEstudianteRelacion"], onDelete = ForeignKey.CASCADE)
    ]
)
data class RegistroTest(
    @PrimaryKey(autoGenerate = true) val idTest: Int = 0,
    val fecha: Long = System.currentTimeMillis(),
    val idCursoRelacion: Int,
    val idEstudianteRelacion: Int,
    val nombreCursoSnapshot: String, // Guardamos el nombre por si lo editan
    val nombreEstudianteSnapshot: String,
    val objetoDetectado: String,
    val resultado: String // Correcto/Incorrecto
)

@Dao
interface MiniEcoDao {
    // --- CURSOS ---
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertarCurso(curso: Curso): Long
    @Update suspend fun actualizarCurso(curso: Curso) // ¡PARA EDITAR!
    @Delete suspend fun eliminarCurso(curso: Curso)
    @Query("SELECT * FROM cursos") fun obtenerTodosLosCursos(): Flow<List<Curso>>

    // --- REGLAS ---
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertarRegla(regla: ReglaDesecho)
    @Delete suspend fun eliminarRegla(regla: ReglaDesecho)
    @Query("SELECT * FROM reglas_desecho WHERE idCursoRelacion = :cursoId") suspend fun obtenerReglasPorCurso(cursoId: Int): List<ReglaDesecho>

    // --- ESTUDIANTES ---
    @Insert suspend fun insertarEstudiante(estudiante: Estudiante)
    @Update suspend fun actualizarEstudiante(estudiante: Estudiante) // ¡PARA EDITAR!
    @Delete suspend fun eliminarEstudiante(estudiante: Estudiante)
    @Query("SELECT * FROM estudiantes WHERE idCursoRelacion = :cursoId") fun obtenerEstudiantesPorCurso(cursoId: Int): Flow<List<Estudiante>>

    // --- REGISTROS DE TEST (Sin Delete ni Update manual) ---
    @Insert suspend fun guardarResultadoTest(registro: RegistroTest)
    @Query("SELECT * FROM registros_test ORDER BY fecha DESC") fun obtenerTodosLosRegistros(): Flow<List<RegistroTest>>
}

@Database(entities = [Curso::class, ReglaDesecho::class, Estudiante::class, RegistroTest::class], version = 4, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun miniEcoDao(): MiniEcoDao
    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "minieco_database")
                    .fallbackToDestructiveMigration() // Limpia versiones viejas si las hay
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}