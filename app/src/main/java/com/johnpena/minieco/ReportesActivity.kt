package com.johnpena.minieco

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.johnpena.minieco.database.MiniEcoDatabase
import com.johnpena.minieco.database.ReporteTest
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ReportesActivity : AppCompatActivity() {

    private lateinit var database: MiniEcoDatabase
    private lateinit var rvReportes: RecyclerView
    private lateinit var adapter: ReporteAdapter

    private var cursoId: Int = -1
    private var listaOriginal: List<ReporteTest> = emptyList()

    private var fechaFiltro: String? = null
    private var estudianteFiltro: String = "Todos"
    private val formatoFecha = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private var estudiantePreFiltro: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reportes)

        cursoId = intent.getIntExtra("CURSO_ID", -1)
        estudiantePreFiltro = intent.getStringExtra("ESTUDIANTE_FILTRO")
        database = MiniEcoDatabase.getDatabase(this)

        rvReportes = findViewById(R.id.rvReportes)
        rvReportes.layoutManager = LinearLayoutManager(this)
        adapter = ReporteAdapter(emptyList()) { reporte -> mostrarDetalleReporte(reporte) }
        rvReportes.adapter = adapter

        val btnFecha = findViewById<Button>(R.id.btnFiltroFecha)
        val btnLimpiar = findViewById<ImageButton>(R.id.btnLimpiarFiltros)

        // AutoCompleteTextView
        val buscadorEstudiantes = findViewById<AutoCompleteTextView>(R.id.spinnerFiltroEstudiante)

        cargarReportes(buscadorEstudiantes)

        btnFecha.setOnClickListener {
            val c = Calendar.getInstance()
            DatePickerDialog(this, { _, year, month, day ->
                c.set(year, month, day)
                fechaFiltro = formatoFecha.format(c.time)
                btnFecha.text = "📅 $fechaFiltro"
                aplicarFiltros()
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
        }

        btnLimpiar.setOnClickListener {
            fechaFiltro = null
            estudianteFiltro = "Todos"
            btnFecha.text = "📅 Seleccionar Fecha"
            buscadorEstudiantes.setText("")
            aplicarFiltros()
        }

        buscadorEstudiantes.setOnItemClickListener { parent, _, position, _ ->
            val seleccion = parent.getItemAtPosition(position).toString()
            estudianteFiltro = if (seleccion == "Mostrar Todos") "Todos" else seleccion.split("\n")[0].trim()
            aplicarFiltros()
        }
    }

    private fun cargarReportes(buscador: AutoCompleteTextView) {
        lifecycleScope.launch {
            listaOriginal = database.miniEcoDao().obtenerReportesPorCurso(cursoId)

            if (estudiantePreFiltro != null) {
                buscador.visibility = View.GONE
                estudianteFiltro = estudiantePreFiltro!!.split("(")[0].trim()
                findViewById<TextView>(R.id.tvTituloReportes).text = "Historial de $estudianteFiltro"
            } else {
                buscador.visibility = View.VISIBLE
                val sugerencias = mutableListOf("Mostrar Todos")
                sugerencias.addAll(listaOriginal.map { "${it.nombreEstudiante}" }.distinct())

                val arrayAdapter = ArrayAdapter(this@ReportesActivity, android.R.layout.simple_dropdown_item_1line, sugerencias)
                buscador.setAdapter(arrayAdapter)
            }
            aplicarFiltros()
        }
    }

    private fun aplicarFiltros() {
        var listaFiltrada = listaOriginal

        if (fechaFiltro != null) {
            listaFiltrada = listaFiltrada.filter { formatoFecha.format(Date(it.fecha)) == fechaFiltro }
        }

        if (estudianteFiltro != "Todos") {
            listaFiltrada = listaFiltrada.filter { it.nombreEstudiante.contains(estudianteFiltro, ignoreCase = true) }
        }

        adapter.actualizarLista(listaFiltrada)
    }

    private fun mostrarDetalleReporte(reporte: ReporteTest) {
        var mensaje = "Nota Final: ${reporte.aciertos}/${reporte.totalPreguntas}\n\n"
        try {
            val jsonArray = JSONArray(reporte.detallesJson)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                mensaje += "${i + 1}. ${obj.getString("desecho")} -> ${obj.getString("colorElegido")} ${if (obj.getBoolean("esCorrecto")) "✅" else "❌"}\n"
            }
        } catch (e: Exception) { mensaje += "Detalles no disponibles." }

        AlertDialog.Builder(this)
            .setTitle(reporte.nombreEstudiante)
            .setMessage(mensaje)
            .setPositiveButton("Cerrar", null)
            .show()
    }


    class ReporteAdapter(
        private var reportes: List<com.johnpena.minieco.database.ReporteTest>,
        private val onClick: (com.johnpena.minieco.database.ReporteTest) -> Unit
    ) : androidx.recyclerview.widget.RecyclerView.Adapter<ReporteAdapter.ReporteViewHolder>() {

        private val format = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())

        inner class ReporteViewHolder(itemView: android.view.View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(itemView) {
            val tvNombre = itemView.findViewById<android.widget.TextView>(R.id.tvReporteEstudiante)
            val tvFecha = itemView.findViewById<android.widget.TextView>(R.id.tvReporteFecha)
            val tvNota = itemView.findViewById<android.widget.TextView>(R.id.tvReporteNota)
        }

        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ReporteViewHolder {
            val view = android.view.LayoutInflater.from(parent.context).inflate(R.layout.item_reporte, parent, false)
            return ReporteViewHolder(view)
        }

        override fun onBindViewHolder(holder: ReporteViewHolder, position: Int) {
            val rep = reportes[position]
            holder.tvNombre.text = rep.nombreEstudiante
            holder.tvFecha.text = format.format(java.util.Date(rep.fecha))

            // Pinta la nota de rojo si reprobó (menos de la mitad)
            holder.tvNota.text = "${rep.aciertos}/${rep.totalPreguntas}"
            if (rep.aciertos < (rep.totalPreguntas / 2.0)) {
                holder.tvNota.setTextColor(android.graphics.Color.parseColor("#F44336"))
            } else {
                holder.tvNota.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
            }

            holder.itemView.setOnClickListener { onClick(rep) }
        }

        override fun getItemCount() = reportes.size

        fun actualizarLista(nuevaLista: List<com.johnpena.minieco.database.ReporteTest>) {
            reportes = nuevaLista
            notifyDataSetChanged()
        }
    }
}