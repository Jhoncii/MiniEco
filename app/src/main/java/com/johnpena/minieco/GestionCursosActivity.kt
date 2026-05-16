package com.johnpena.minieco

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.johnpena.minieco.database.Curso
import com.johnpena.minieco.database.MiniEcoDatabase
import kotlinx.coroutines.launch

class GestionCursosActivity : AppCompatActivity() {

    private lateinit var database: MiniEcoDatabase
    private lateinit var adapter: CursoAdapter
    private lateinit var rvCursos: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gestion_cursos)

        database = MiniEcoDatabase.getDatabase(this)
        rvCursos = findViewById(R.id.rvCursos)
        rvCursos.layoutManager = LinearLayoutManager(this)

        val fabAgregar = findViewById<FloatingActionButton>(R.id.fabAgregarCurso)

        // Inicializamos el Adapter
        adapter = CursoAdapter(
            cursos = emptyList(),
            context = this,
            onCursoClick = { curso ->
                val intent = Intent(this, DetalleCursoActivity::class.java)
                intent.putExtra("CURSO_ID", curso.id)
                intent.putExtra("CURSO_NOMBRE", curso.nombre)
                intent.putExtra("CURSO_DESC", curso.descripcion)
                startActivity(intent)
            },
            onAplicarReglas = { curso ->
                Toast.makeText(this, "¡Reglas de ${curso.nombre} aplicadas a la cámara!", Toast.LENGTH_SHORT).show()
            },
            onQuitarReglas = { curso ->
                Toast.makeText(this, "Reglas quitadas. MiniEco volvió a la normalidad.", Toast.LENGTH_SHORT).show()
            }
        )
        rvCursos.adapter = adapter

        fabAgregar.setOnClickListener {
            mostrarDialogoCrearCurso()
        }
    }
    override fun onResume() {
        super.onResume()
        cargarCursos()
    }

    private fun cargarCursos() {
        lifecycleScope.launch {
            val listaCursos = database.miniEcoDao().obtenerTodosLosCursos()
            adapter.actualizarLista(listaCursos)
        }
    }

    private fun mostrarDialogoCrearCurso() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Nuevo Curso")

        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(50, 40, 50, 10)
        }

        val inputNombre = EditText(this).apply { hint = "Nombre del curso (Max 10 caracteres)" }
        val inputDescripcion = EditText(this).apply { hint = "Descripción (Max 30 palabras)" }

        layout.addView(inputNombre)
        layout.addView(inputDescripcion)
        builder.setView(layout)

        builder.setPositiveButton("Guardar", null)
        builder.setNegativeButton("Cancelar") { dialog, _ -> dialog.dismiss() }

        val dialog = builder.create()
        dialog.show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val nombre = inputNombre.text.toString().trim()
            val descripcion = inputDescripcion.text.toString().trim()

            if (nombre.isEmpty() || descripcion.isEmpty()) {
                Toast.makeText(this, "Error: Por favor llena todos los campos", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val regexAlfanumerico = "^[a-zA-Z0-9 áéíóúÁÉÍÓÚñÑ]+$".toRegex()
            if (nombre.length > 10 || !nombre.matches(regexAlfanumerico)) {
                Toast.makeText(this, "Error: El nombre debe tener máx 10 caracteres y solo letras/números", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            if (!descripcion.matches(regexAlfanumerico)) {
                Toast.makeText(this, "Error: La descripción solo debe contener letras y números", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val numPalabras = descripcion.split("\\s+".toRegex()).size
            if (numPalabras > 30) {
                Toast.makeText(this, "Error: La descripción no puede tener más de 30 palabras", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val nuevoCurso = Curso(nombre = nombre, descripcion = descripcion)

            lifecycleScope.launch {
                database.miniEcoDao().insertarCurso(nuevoCurso)
                Toast.makeText(this@GestionCursosActivity, "¡Curso creado con éxito!", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
                cargarCursos()
            }
        }
    }
}