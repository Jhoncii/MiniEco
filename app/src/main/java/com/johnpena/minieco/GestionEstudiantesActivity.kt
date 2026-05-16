package com.johnpena.minieco

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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
            onEditClick = { est -> mostrarDialogoEstudiante(est) },
            onDeleteClick = { est ->
                AlertDialog.Builder(this)
                    .setTitle("Eliminar Estudiante")
                    .setMessage("¿Seguro que deseas eliminar a ${est.nombre}? Perderá su historial de tests.")
                    .setPositiveButton("Sí, eliminar") { _, _ ->
                        lifecycleScope.launch {
                            database.miniEcoDao().eliminarEstudiante(est)
                            cargarEstudiantes()
                        }
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            },
            onTestClick = { _ -> },
            onCardClick = { est ->
                val intent = Intent(this, ReportesActivity::class.java)
                intent.putExtra("CURSO_ID", cursoId)
                intent.putExtra("ESTUDIANTE_FILTRO", "${est.apellido.uppercase()} ${est.nombre.uppercase()}")
                startActivity(intent)
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


        val inputCedula = EditText(this).apply {
            hint = "Cédula (10 dígitos)"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            filters = arrayOf(android.text.InputFilter.LengthFilter(10))
            setText(estudianteAEditar?.cedula ?: "")
            if (estudianteAEditar != null) isEnabled = false
        }

        val inputNombre = EditText(this).apply {
            hint = "Nombres (Max 30 letras)"
            filters = arrayOf(android.text.InputFilter.LengthFilter(30))
            setText(estudianteAEditar?.nombre ?: "")
        }
        val inputApellido = EditText(this).apply {
            hint = "Apellidos (Max 30 letras)"
            filters = arrayOf(android.text.InputFilter.LengthFilter(30))
            setText(estudianteAEditar?.apellido ?: "")
        }

        inputCedula.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (s?.length == 10 && estudianteAEditar == null) {
                    lifecycleScope.launch {
                        val existente = database.miniEcoDao().obtenerEstudiantePorCedula(s.toString())
                        if (existente != null) {
                            inputNombre.setText(existente.nombre)
                            inputApellido.setText(existente.apellido)
                            Toast.makeText(this@GestionEstudiantesActivity, "✨ ¡Estudiante encontrado y auto-completado!", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        layout.addView(inputCedula)
        layout.addView(inputNombre)
        layout.addView(inputApellido)
        builder.setView(layout)

        builder.setPositiveButton("Guardar", null)
        builder.setNegativeButton("Cancelar") { dialog, _ -> dialog.dismiss() }

        val dialog = builder.create()
        dialog.show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val cedula = inputCedula.text.toString().trim()
            val nombre = inputNombre.text.toString().trim()
            val apellido = inputApellido.text.toString().trim()

            if (cedula.isEmpty() || nombre.isEmpty() || apellido.isEmpty()) {
                Toast.makeText(this, "Por favor llena todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Validación Algoritmo Cédula Ecuador
            if (!validarCedulaEcuatoriana(cedula)) {
                Toast.makeText(this, "❌ Ingrese una cedula real", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val regexLetras = "^[a-zA-Z áéíóúÁÉÍÓÚñÑ]+$".toRegex()
            if (!nombre.matches(regexLetras) || !apellido.matches(regexLetras)) {
                Toast.makeText(this, "Los nombres y apellidos solo deben contener letras", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                if (estudianteAEditar == null) {

                    val existeEnCurso = database.miniEcoDao().verificarEstudianteEnCurso(cedula, cursoId)
                    if (existeEnCurso != null) {
                        Toast.makeText(this@GestionEstudiantesActivity, "❌ El estudiante ya está registrado en este curso", Toast.LENGTH_LONG).show()
                        return@launch // Detenemos el guardado inmediatamente
                    }
                    // ------------------------------------

                    database.miniEcoDao().insertarEstudiante(Estudiante(cursoId = cursoId, nombre = nombre, apellido = apellido, cedula = cedula))
                    Toast.makeText(this@GestionEstudiantesActivity, "Estudiante creado", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                    cargarEstudiantes()
                } else {
                    val actualizado = Estudiante(id = estudianteAEditar.id, cursoId = cursoId, nombre = nombre, apellido = apellido, cedula = cedula)
                    database.miniEcoDao().actualizarEstudiante(actualizado)
                    Toast.makeText(this@GestionEstudiantesActivity, "Estudiante actualizado", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                    cargarEstudiantes()
                }
            }
        }
    }

    private fun validarCedulaEcuatoriana(cedula: String): Boolean {
        if (cedula.length != 10 || !cedula.all { it.isDigit() }) return false
        val prov = cedula.substring(0, 2).toInt()
        if (prov !in 1..24 && prov != 30) return false
        val tercerDigito = cedula[2].toString().toInt()
        if (tercerDigito >= 6) return false

        val verificador = cedula[9].toString().toInt()
        var suma = 0
        val coeficientes = intArrayOf(2, 1, 2, 1, 2, 1, 2, 1, 2)

        for (i in 0 until 9) {
            var valor = cedula[i].toString().toInt() * coeficientes[i]
            if (valor > 9) valor -= 9
            suma += valor
        }
        val residuo = suma % 10
        val verificadorCalculado = if (residuo == 0) 0 else 10 - residuo
        return verificadorCalculado == verificador
    }
}