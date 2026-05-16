package com.johnpena.minieco

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.johnpena.minieco.database.Curso
import com.johnpena.minieco.database.Estudiante
import com.johnpena.minieco.database.MiniEcoDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DetalleCursoActivity : AppCompatActivity() {

    private lateinit var database: MiniEcoDatabase
    private var cursoId: Int = -1
    private var nombreActual: String = ""
    private var descActual: String = ""

    private lateinit var tvNombre: TextView
    private lateinit var tvDesc: TextView
    private var listaEstudiantesCache: List<Estudiante> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detalle_curso)

        database = MiniEcoDatabase.getDatabase(this)

        cursoId = intent.getIntExtra("CURSO_ID", -1)
        nombreActual = intent.getStringExtra("CURSO_NOMBRE") ?: "Error"
        descActual = intent.getStringExtra("CURSO_DESC") ?: ""

        tvNombre = findViewById(R.id.tvNombreCursoDetalle)
        tvDesc = findViewById(R.id.tvDescCursoDetalle)
        tvNombre.text = nombreActual
        tvDesc.text = descActual

        cargarEstudiantesOcultos()

        findViewById<Button>(R.id.btnEliminarCurso).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Eliminar Curso")
                .setMessage("¿Segura que deseas eliminar $nombreActual? Se borrará todo (estudiantes, reglas y reportes).")
                .setPositiveButton("Sí, eliminar") { _, _ -> eliminarCursoActual() }
                .setNegativeButton("Cancelar", null)
                .show()
        }

        findViewById<Button>(R.id.btnEditarCurso).setOnClickListener { mostrarDialogoEditar() }

        findViewById<Button>(R.id.btnGestionarReglas).setOnClickListener {
            val intent = Intent(this, GestionReglasActivity::class.java)
            intent.putExtra("CURSO_ID", cursoId)
            startActivity(intent)
        }

        findViewById<Button>(R.id.btnGestionarEstudiantes).setOnClickListener {
            val intent = Intent(this, GestionEstudiantesActivity::class.java)
            intent.putExtra("CURSO_ID", cursoId)
            startActivity(intent)
        }


        findViewById<Button>(R.id.btnVerReportes).setOnClickListener {
            val intent = Intent(this, ReportesActivity::class.java)
            intent.putExtra("CURSO_ID", cursoId)
            startActivity(intent)
        }

        findViewById<Button>(R.id.btnModoPrueba).setOnClickListener {
            if (listaEstudiantesCache.isEmpty()) {
                Toast.makeText(this, "⚠️ Primero debes agregar estudiantes a este curso.", Toast.LENGTH_LONG).show()
            } else {
                mostrarDialogoConfigurarTest()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        cargarEstudiantesOcultos() // Refresca si agregaste estudiantes nuevos
    }

    private fun cargarEstudiantesOcultos() {
        lifecycleScope.launch {
            listaEstudiantesCache = database.miniEcoDao().obtenerEstudiantesPorCurso(cursoId)
        }
    }

    private fun mostrarDialogoConfigurarTest() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Configurar Prueba")

        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(50, 40, 50, 10)
        }

        // REEMPLAZO POR AUTOCOMPLETE BUSCADOR
        val inputBuscadorEstudiante = android.widget.AutoCompleteTextView(this).apply {
            hint = "Escribe Apellido o Cédula para buscar..."
            threshold = 1 // Empieza a buscar desde la primera letra
        }

        // Mapeamos los datos para el buscador
        val listaFormateada = listaEstudiantesCache.map {
            "${it.apellido.uppercase()} ${it.nombre.uppercase()} \n(CI: ${it.cedula})"
        }
        val adaptadorBuscador = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, listaFormateada)
        inputBuscadorEstudiante.setAdapter(adaptadorBuscador)

        var estudianteSeleccionadoIndex = -1
        inputBuscadorEstudiante.setOnItemClickListener { _, _, position, _ ->
            val textoSeleccionado = adaptadorBuscador.getItem(position)
            estudianteSeleccionadoIndex = listaFormateada.indexOf(textoSeleccionado)
        }

        val spinnerCantidad = Spinner(this)
        val opcionesCantidad = (1..10).map { "$it Desechos" }
        spinnerCantidad.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, opcionesCantidad)

        val inputPassword = EditText(this).apply {
            hint = "Contraseña de la profesora"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }

        layout.addView(android.widget.TextView(this).apply { text = "Buscar Estudiante (Filtro Dinámico):" })
        layout.addView(inputBuscadorEstudiante)
        layout.addView(android.widget.TextView(this).apply { text = "Cantidad de objetos a evaluar:"; setPadding(0, 30, 0, 0) })
        layout.addView(spinnerCantidad)
        layout.addView(android.widget.TextView(this).apply { text = "Seguridad:"; setPadding(0, 30, 0, 0) })
        layout.addView(inputPassword)

        builder.setView(layout)
        builder.setPositiveButton("INICIAR PRUEBA", null)
        builder.setNegativeButton("Cancelar") { dialog, _ -> dialog.dismiss() }

        val dialog = builder.create()
        dialog.show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val passIngresada = inputPassword.text.toString()
            val passReal = getSharedPreferences("MiniEcoPrefs", Context.MODE_PRIVATE).getString("admin_pass", "basura")

            if (passIngresada != passReal) {
                Toast.makeText(this, "❌ Contraseña incorrecta", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (estudianteSeleccionadoIndex == -1) {
                Toast.makeText(this, "❌ Selecciona un estudiante válido de la lista desplegable", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val estudianteSeleccionado = listaEstudiantesCache[estudianteSeleccionadoIndex]
            val cantidadSeleccionada = spinnerCantidad.selectedItemPosition + 1

            dialog.dismiss()

            val intent = Intent(this, TestActivity::class.java)
            intent.putExtra("CURSO_ID", cursoId)
            intent.putExtra("ESTUDIANTE_ID", estudianteSeleccionado.id)
            // Mandamos Apellidos Nombres + CI
            intent.putExtra("NOMBRE_ESTUDIANTE", "${estudianteSeleccionado.apellido.uppercase()} ${estudianteSeleccionado.nombre.uppercase()} (CI: ${estudianteSeleccionado.cedula})")
            intent.putExtra("TOTAL_PREGUNTAS", cantidadSeleccionada)
            startActivity(intent)
        }
    }

    private fun mostrarDialogoEditar() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Editar Curso")
        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(50, 40, 50, 10)
        }
        val inputNombre = EditText(this).apply { setText(nombreActual) }
        val inputDescripcion = EditText(this).apply { setText(descActual) }
        layout.addView(inputNombre)
        layout.addView(inputDescripcion)
        builder.setView(layout)

        builder.setPositiveButton("Guardar", null)
        builder.setNegativeButton("Cancelar") { dialog, _ -> dialog.dismiss() }
        val dialog = builder.create()
        dialog.show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val nuevoNombre = inputNombre.text.toString().trim()
            val nuevaDesc = inputDescripcion.text.toString().trim()

            if (nuevoNombre.isEmpty() || nuevaDesc.isEmpty()) {
                Toast.makeText(this, "Llena todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val cursoActualizado = Curso(id = cursoId, nombre = nuevoNombre, descripcion = nuevaDesc)

            lifecycleScope.launch(Dispatchers.IO) {
                database.miniEcoDao().actualizarCurso(cursoActualizado)
                withContext(Dispatchers.Main) {
                    nombreActual = nuevoNombre
                    descActual = nuevaDesc
                    tvNombre.text = nombreActual
                    tvDesc.text = descActual
                    Toast.makeText(this@DetalleCursoActivity, "Curso actualizado", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
            }
        }
    }

    private fun eliminarCursoActual() {
        lifecycleScope.launch(Dispatchers.IO) {
            database.miniEcoDao().eliminarCursoPorId(cursoId)
            val prefs = getSharedPreferences("MiniEcoPrefs", MODE_PRIVATE)
            if (prefs.getInt("cursoActivoId", -1) == cursoId) {
                prefs.edit().remove("cursoActivoId").apply()
            }
            withContext(Dispatchers.Main) {
                Toast.makeText(this@DetalleCursoActivity, "Curso eliminado", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
}