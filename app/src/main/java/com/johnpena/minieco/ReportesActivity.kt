package com.johnpena.minieco

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageButton
import android.widget.Spinner
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

    // Variables para filtros
    private var fechaFiltro: String? = null
    private var estudianteFiltro: String = "Todos"
    private val formatoFecha = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reportes)

        cursoId = intent.getIntExtra("CURSO_ID", -1)
        database = MiniEcoDatabase.getDatabase(this)

        rvReportes = findViewById(R.id.rvReportes)
        rvReportes.layoutManager = LinearLayoutManager(this)
        adapter = ReporteAdapter(emptyList()) { reporte -> mostrarDetalleReporte(reporte) }
        rvReportes.adapter = adapter

        val btnFecha = findViewById<Button>(R.id.btnFiltroFecha)
        val btnLimpiar = findViewById<ImageButton>(R.id.btnLimpiarFiltros)
        val spinnerEstudiantes = findViewById<Spinner>(R.id.spinnerFiltroEstudiante)

        cargarReportes(spinnerEstudiantes)

        // CALENDARIO
        btnFecha.setOnClickListener {
            val c = Calendar.getInstance()
            DatePickerDialog(this, { _, year, month, day ->
                c.set(year, month, day)
                fechaFiltro = formatoFecha.format(c.time)
                btnFecha.text = "📅 $fechaFiltro"
                aplicarFiltros()
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
        }

        // LIMPIAR FILTROS
        btnLimpiar.setOnClickListener {
            fechaFiltro = null
            estudianteFiltro = "Todos"
            btnFecha.text = "📅 Seleccionar Fecha"
            spinnerEstudiantes.setSelection(0)
            aplicarFiltros()
        }

        // FILTRO POR ESTUDIANTE
        spinnerEstudiantes.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                estudianteFiltro = parent?.getItemAtPosition(position).toString()
                aplicarFiltros()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun cargarReportes(spinner: Spinner) {
        lifecycleScope.launch {
            // Ya vienen ordenados por fecha desde la Base de Datos (del más nuevo al más viejo)
            listaOriginal = database.miniEcoDao().obtenerReportesPorCurso(cursoId)

            // Llenar el Spinner de estudiantes únicos
            val nombresUnicos = mutableListOf("Todos")
            nombresUnicos.addAll(listaOriginal.map { it.nombreEstudiante }.distinct())
            spinner.adapter = ArrayAdapter(this@ReportesActivity, android.R.layout.simple_spinner_dropdown_item, nombresUnicos)

            aplicarFiltros()
        }
    }

    private fun aplicarFiltros() {
        var listaFiltrada = listaOriginal

        // Filtro de Fecha
        if (fechaFiltro != null) {
            listaFiltrada = listaFiltrada.filter { formatoFecha.format(Date(it.fecha)) == fechaFiltro }
        }

        // Filtro de Estudiante
        if (estudianteFiltro != "Todos") {
            listaFiltrada = listaFiltrada.filter { it.nombreEstudiante == estudianteFiltro }
        }

        adapter.actualizarLista(listaFiltrada)
    }

    // EL CHISME (Leer el JSON)
    private fun mostrarDetalleReporte(reporte: ReporteTest) {
        var mensaje = "Nota Final: ${reporte.aciertos}/${reporte.totalPreguntas}\n\n"

        try {
            val jsonArray = JSONArray(reporte.detallesJson)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val desecho = obj.getString("desecho")
                val elegido = obj.getString("colorElegido")
                val esCorrecto = obj.getBoolean("esCorrecto")

                val icono = if (esCorrecto) "✅" else "❌"
                mensaje += "${i + 1}. $desecho -> $elegido $icono\n"
            }
        } catch (e: Exception) {
            mensaje += "Detalles no disponibles."
        }

        AlertDialog.Builder(this)
            .setTitle("Reporte de ${reporte.nombreEstudiante}")
            .setMessage(mensaje)
            .setPositiveButton("Cerrar", null)
            .show()
    }
}

// --- EL ADAPTER (Lo pongo aquí mismo para ahorrarte crear otro archivo) ---
class ReporteAdapter(
    private var reportes: List<ReporteTest>,
    private val onClick: (ReporteTest) -> Unit
) : RecyclerView.Adapter<ReporteAdapter.ReporteViewHolder>() {

    private val format = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    inner class ReporteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNombre = itemView.findViewById<TextView>(R.id.tvReporteEstudiante)
        val tvFecha = itemView.findViewById<TextView>(R.id.tvReporteFecha)
        val tvNota = itemView.findViewById<TextView>(R.id.tvReporteNota)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReporteViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_reporte, parent, false)
        return ReporteViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReporteViewHolder, position: Int) {
        val rep = reportes[position]
        holder.tvNombre.text = rep.nombreEstudiante
        holder.tvFecha.text = format.format(Date(rep.fecha))

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

    fun actualizarLista(nuevaLista: List<ReporteTest>) {
        reportes = nuevaLista
        notifyDataSetChanged()
    }
}