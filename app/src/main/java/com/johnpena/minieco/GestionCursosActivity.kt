package com.johnpena.minieco

import android.database.sqlite.SQLiteConstraintException
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class GestionCursosActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private var listaCursos = listOf<Curso>()

    private val misDesechos = arrayOf("battery", "biological", "cardboard", "clothes", "glass", "metal", "paper", "plastic", "shoes", "trash")

    // --- AQUÍ ESTÁN TUS 8 NUEVOS COLORES ---
    private val misTachos = arrayOf(
        "Rojo (Peligrosos)",
        "Café (Orgánicos)",
        "Azul (Papel/Cartón)",
        "Amarillo (Metal)",
        "Verde (Vidrio)",
        "Blanco (Plástico)",
        "Negro (Basura General)",
        "Donación (Ropa/Zapatos)"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_gestion_cursos)
        db = AppDatabase.getDatabase(this)

        val etNombreCurso = findViewById<EditText>(R.id.etNombreCurso)
        val btnCrearCurso = findViewById<Button>(R.id.btnCrearCurso)

        val spinnerCursos = findViewById<Spinner>(R.id.spinnerCursos)
        val spinnerDesechos = findViewById<Spinner>(R.id.spinnerDesechos)
        val spinnerColores = findViewById<Spinner>(R.id.spinnerColores)
        val etDescripcion = findViewById<EditText>(R.id.etDescripcion)
        val btnAgregarRegla = findViewById<Button>(R.id.btnAgregarRegla)

        spinnerDesechos.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, misDesechos)
        spinnerColores.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, misTachos)

        lifecycleScope.launch {
            db.miniEcoDao().obtenerTodosLosCursos().collect { cursos ->
                listaCursos = cursos
                spinnerCursos.adapter = ArrayAdapter(this@GestionCursosActivity, android.R.layout.simple_spinner_dropdown_item, cursos.map { it.nombreCurso })
            }
        }

        btnCrearCurso.setOnClickListener {
            val nombre = etNombreCurso.text.toString().trim()
            if (nombre.isNotEmpty() && nombre.all { it.isLetter() || it.isWhitespace() }) {
                lifecycleScope.launch {
                    db.miniEcoDao().insertarCurso(Curso(nombreCurso = nombre))
                    Toast.makeText(this@GestionCursosActivity, "Curso Creado", Toast.LENGTH_SHORT).show()
                    etNombreCurso.text.clear()
                }
            } else {
                Toast.makeText(this, "Solo letras para el curso", Toast.LENGTH_SHORT).show()
            }
        }

        btnAgregarRegla.setOnClickListener {
            if (listaCursos.isEmpty() || spinnerCursos.selectedItem == null) {
                Toast.makeText(this, "Primero crea un curso", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val desc = etDescripcion.text.toString().trim()
            val palabras = desc.split("\\s+".toRegex()).filter { it.isNotEmpty() }

            if (palabras.size > 30 || !desc.all { it.isLetter() || it.isWhitespace() || it in ".,;" }) {
                Toast.makeText(this, "Descripción inválida (Máx 30 palabras)", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val cursoSeleccionado = listaCursos[spinnerCursos.selectedItemPosition]
            val nuevaRegla = ReglaDesecho(
                idCursoRelacion = cursoSeleccionado.idCurso,
                desecho = spinnerDesechos.selectedItem.toString(),
                colorTacho = spinnerColores.selectedItem.toString(),
                descripcion = desc
            )

            lifecycleScope.launch {
                try {
                    db.miniEcoDao().insertarRegla(nuevaRegla)
                    Toast.makeText(this@GestionCursosActivity, "Regla añadida con éxito", Toast.LENGTH_SHORT).show()
                    etDescripcion.text.clear()
                } catch (e: SQLiteConstraintException) {
                    Toast.makeText(this@GestionCursosActivity, "❌ Error: Ese desecho ya está configurado en este curso.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}