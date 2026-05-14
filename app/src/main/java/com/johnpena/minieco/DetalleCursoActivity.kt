package com.johnpena.minieco

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.johnpena.minieco.database.Curso
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

        findViewById<Button>(R.id.btnEliminarCurso).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Eliminar Curso")
                .setMessage("¿Segura que deseas eliminar $nombreActual? Se borrará todo (estudiantes y reglas).")
                .setPositiveButton("Sí, eliminar") { _, _ -> eliminarCursoActual() }
                .setNegativeButton("Cancelar", null)
                .show()
        }

        findViewById<Button>(R.id.btnEditarCurso).setOnClickListener {
            mostrarDialogoEditar()
        }

        findViewById<Button>(R.id.btnGestionarReglas).setOnClickListener {
            val intent = android.content.Intent(this, GestionReglasActivity::class.java)
            intent.putExtra("CURSO_ID", cursoId)
            startActivity(intent)
        }

        findViewById<Button>(R.id.btnGestionarEstudiantes).setOnClickListener {
            val intent = android.content.Intent(this, GestionEstudiantesActivity::class.java)
            intent.putExtra("CURSO_ID", cursoId)
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
                finish() // Cierra la pantalla y vuelve a la lista
            }
        }
    }
}