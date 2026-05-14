package com.johnpena.minieco

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.johnpena.minieco.database.MiniEcoDatabase
import com.johnpena.minieco.database.Regla
import kotlinx.coroutines.launch

class GestionReglasActivity : AppCompatActivity() {

    private lateinit var database: MiniEcoDatabase
    private var cursoId: Int = -1
    private lateinit var rvReglas: RecyclerView
    private lateinit var adapter: ReglaAdapter

    // LISTAS CORREGIDAS
    private val todosLosDesechos = listOf(
        "Batería", "Biológico", "Papel", "Cartón", "Metal",
        "Vidrio", "Basura General", "Plástico", "Ropa", "Zapatos"
    )
    private val todosLosColores = listOf(
        "Rojo", "Café", "Azul", "Amarillo", "Verde", "Negro", "Blanco", "Donación"
    )

    private var reglasActuales: List<Regla> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gestion_reglas)

        database = MiniEcoDatabase.getDatabase(this)
        cursoId = intent.getIntExtra("CURSO_ID", -1)

        rvReglas = findViewById(R.id.rvReglas)
        rvReglas.layoutManager = LinearLayoutManager(this)

        adapter = ReglaAdapter(
            reglas = emptyList(),
            onEditClick = { regla -> mostrarDialogoEditarRegla(regla) },
            onDeleteClick = { regla ->
                lifecycleScope.launch {
                    database.miniEcoDao().eliminarRegla(regla)
                    cargarReglas()
                }
            }
        )
        rvReglas.adapter = adapter

        findViewById<FloatingActionButton>(R.id.fabAgregarRegla).setOnClickListener {
            mostrarDialogoCrearRegla()
        }

        cargarReglas()
    }

    private fun cargarReglas() {
        lifecycleScope.launch {
            reglasActuales = database.miniEcoDao().obtenerReglasPorCurso(cursoId)
            adapter.actualizarLista(reglasActuales)
        }
    }

    private fun mostrarDialogoCrearRegla() {
        val desechosUsados = reglasActuales.map { it.tipoDesecho }
        val desechosDisponibles = todosLosDesechos.filterNot { it in desechosUsados }

        if (desechosDisponibles.isEmpty()) {
            Toast.makeText(this, "Ya configuraste los 10 desechos para este curso.", Toast.LENGTH_LONG).show()
            return
        }

        mostrarDialogo(desechosDisponibles, todosLosColores, null)
    }

    private fun mostrarDialogoEditarRegla(reglaAEditar: Regla) {
        // Al editar, el desecho actual SÍ debe estar disponible en la lista
        val desechosUsados = reglasActuales.map { it.tipoDesecho }.toMutableList()
        desechosUsados.remove(reglaAEditar.tipoDesecho)
        val desechosDisponibles = todosLosDesechos.filterNot { it in desechosUsados }

        mostrarDialogo(desechosDisponibles, todosLosColores, reglaAEditar)
    }

    // Función universal para Crear o Editar
    private fun mostrarDialogo(desechos: List<String>, colores: List<String>, reglaAEditar: Regla?) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(if (reglaAEditar == null) "Nueva Regla" else "Editar Regla")

        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(50, 40, 50, 10)
        }

        val spinnerDesecho = Spinner(this)
        spinnerDesecho.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, desechos)

        val spinnerColor = Spinner(this)
        spinnerColor.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, colores)

        // Si es editar, seleccionamos los valores que ya tenía
        if (reglaAEditar != null) {
            spinnerDesecho.setSelection(desechos.indexOf(reglaAEditar.tipoDesecho))
            spinnerColor.setSelection(colores.indexOf(reglaAEditar.colorTacho))
        }

        layout.addView(android.widget.TextView(this).apply { text = "Selecciona el Desecho:" })
        layout.addView(spinnerDesecho)
        layout.addView(android.widget.TextView(this).apply { text = "Selecciona el Destino/Color:"; setPadding(0, 30, 0, 0) })
        layout.addView(spinnerColor)
        builder.setView(layout)

        builder.setPositiveButton("Guardar", null)
        builder.setNegativeButton("Cancelar") { dialog, _ -> dialog.dismiss() }

        val dialog = builder.create()
        dialog.show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val desecho = spinnerDesecho.selectedItem.toString()
            val color = spinnerColor.selectedItem.toString()

            lifecycleScope.launch {
                if (reglaAEditar == null) {
                    database.miniEcoDao().insertarRegla(Regla(cursoId = cursoId, tipoDesecho = desecho, colorTacho = color))
                } else {
                    val reglaActualizada = Regla(id = reglaAEditar.id, cursoId = cursoId, tipoDesecho = desecho, colorTacho = color)
                    database.miniEcoDao().actualizarRegla(reglaActualizada)
                }
                dialog.dismiss()
                cargarReglas()
            }
        }
    }
}