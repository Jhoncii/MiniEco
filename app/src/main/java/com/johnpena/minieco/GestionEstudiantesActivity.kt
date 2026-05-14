package com.johnpena.minieco

import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.johnpena.minieco.database.Estudiante
import com.johnpena.minieco.database.MiniEcoDatabase
import kotlinx.coroutines.launch

class GestionEstudiantesActivity : AppCompatActivity() {

    private lateinit var database: MiniEcoDatabase
    private var cursoId: Int = -1
    private var cantidadActualEstudiantes = 0
    private lateinit var adapter: EstudianteAdapter
    private lateinit var rvEstudiantes: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gestion_estudiantes)

        database = MiniEcoDatabase.getDatabase(this)
        cursoId = intent.getIntExtra("CURSO_ID", -1)

        val fabAgregar = findViewById<FloatingActionButton>(R.id.fabAgregarEstudiante)
        rvEstudiantes = findViewById(R.id.rvEstudiantes)
        rvEstudiantes.layoutManager = LinearLayoutManager(this)

        adapter = EstudianteAdapter(
            estudiantes = emptyList(),
            onEditClick = { estudiante -> mostrarDialogoEstudiante(estudiante) },
            onDeleteClick = { estudiante ->
                AlertDialog.Builder(this)
                    .setTitle("Eliminar Estudiante")
                    .setMessage("¿Seguro que deseas eliminar a ${estudiante.nombre}? Perderá su historial de tests.")
                    .setPositiveButton("Sí, eliminar") { _, _ ->
                        lifecycleScope.launch {
                            database.miniEcoDao().eliminarEstudiante(estudiante)
                            cargarEstudiantes()
                        }
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            },
            onTestClick = { estudiante ->
                Toast.makeText(this, "Siguiente Fase: Abrir Test para ${estudiante.nombre}", Toast.LENGTH_SHORT).show()
            }
        )
        rvEstudiantes.adapter = adapter

        cargarEstudiantes()

        fabAgregar.setOnClickListener {
            if (cantidadActualEstudiantes >= 30) {
                Toast.makeText(this, "Límite alcanzado: Máximo 30 estudiantes.", Toast.LENGTH_LONG).show()
            } else {
                mostrarDialogoEstudiante(null)
            }
        }
    }

    private fun cargarEstudiantes() {
        lifecycleScope.launch {
            val lista = database.miniEcoDao().obtenerEstudiantesPorCurso(cursoId)
            cantidadActualEstudiantes = lista.size
            adapter.actualizarLista(lista)
        }
    }

    private fun mostrarDialogoEstudiante(estudianteAEditar: Estudiante?) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(if (estudianteAEditar == null) "Nuevo Estudiante" else "Editar Estudiante")

        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(50, 40, 50, 10)
        }

        val inputNombre = EditText(this).apply {
            hint = "Nombre (Max 10 letras)"
            setText(estudianteAEditar?.nombre ?: "")
        }
        val inputApellido = EditText(this).apply {
            hint = "Apellido (Max 10 letras)"
            setText(estudianteAEditar?.apellido ?: "")
        }

        layout.addView(inputNombre)
        layout.addView(inputApellido)
        builder.setView(layout)

        builder.setPositiveButton("Guardar", null)
        builder.setNegativeButton("Cancelar") { dialog, _ -> dialog.dismiss() }

        val dialog = builder.create()
        dialog.show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val nombre = inputNombre.text.toString().trim()
            val apellido = inputApellido.text.toString().trim()

            if (nombre.isEmpty() || apellido.isEmpty()) {
                Toast.makeText(this, "Llena todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val regexSoloLetras = "^[a-zA-Z áéíóúÁÉÍÓÚñÑ]+$".toRegex()

            if (nombre.length > 10 || !nombre.matches(regexSoloLetras)) {
                Toast.makeText(this, "Nombre inválido: Max 10 letras, sin números", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (apellido.length > 10 || !apellido.matches(regexSoloLetras)) {
                Toast.makeText(this, "Apellido inválido: Max 10 letras, sin números", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                if (estudianteAEditar == null) {
                    database.miniEcoDao().insertarEstudiante(Estudiante(cursoId = cursoId, nombre = nombre, apellido = apellido))
                    Toast.makeText(this@GestionEstudiantesActivity, "Estudiante agregado", Toast.LENGTH_SHORT).show()
                } else {
                    val actualizado = Estudiante(id = estudianteAEditar.id, cursoId = cursoId, nombre = nombre, apellido = apellido)
                    database.miniEcoDao().actualizarEstudiante(actualizado)
                    Toast.makeText(this@GestionEstudiantesActivity, "Estudiante actualizado", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
                cargarEstudiantes()
            }
        }
    }
}