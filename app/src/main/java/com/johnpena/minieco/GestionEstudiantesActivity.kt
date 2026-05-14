package com.johnpena.minieco

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class GestionEstudiantesActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private var listaCursos = listOf<Curso>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_gestion_estudiantes)
        db = AppDatabase.getDatabase(this)

        val etNombre = findViewById<EditText>(R.id.etNombreEstudiante)
        val etApellido = findViewById<EditText>(R.id.etApellidoEstudiante)
        val spinner = findViewById<Spinner>(R.id.spinnerCursos)
        val btnGuardar = findViewById<Button>(R.id.btnGuardarEstudiante)

        // Lectura segura sin librerías externas
        lifecycleScope.launch {
            db.miniEcoDao().obtenerTodosLosCursos().collect { cursos ->
                listaCursos = cursos
                val adapter = ArrayAdapter(this@GestionEstudiantesActivity, android.R.layout.simple_spinner_dropdown_item, cursos.map { it.nombreCurso })
                spinner.adapter = adapter
            }
        }

        btnGuardar.setOnClickListener {
            val nom = etNombre.text.toString().trim()
            val ape = etApellido.text.toString().trim()

            if (nom.isEmpty() || ape.isEmpty() || !nom.all { it.isLetter() || it.isWhitespace() } || !ape.all { it.isLetter() || it.isWhitespace() }) {
                Toast.makeText(this, "⚠️ Solo se permiten letras", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (listaCursos.isEmpty() || spinner.selectedItem == null) {
                Toast.makeText(this, "⚠️ Crea un curso primero", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                val cursoId = listaCursos[spinner.selectedItemPosition].idCurso
                db.miniEcoDao().insertarEstudiante(Estudiante(nombre = nom, apellido = ape, idCursoRelacion = cursoId))
                Toast.makeText(this@GestionEstudiantesActivity, "✅ Estudiante matriculado", Toast.LENGTH_SHORT).show()
                etNombre.text.clear()
                etApellido.text.clear()
            }
        }
    }
}