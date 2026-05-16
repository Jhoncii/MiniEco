package com.johnpena.minieco

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.johnpena.minieco.database.Estudiante

class EstudianteAdapter(
    private var estudiantes: List<Estudiante>,
    private val onEditClick: (Estudiante) -> Unit,
    private val onDeleteClick: (Estudiante) -> Unit,
    private val onTestClick: (Estudiante) -> Unit,
    private val onCardClick: (Estudiante) -> Unit
) : RecyclerView.Adapter<EstudianteAdapter.EstudianteViewHolder>() {

    inner class EstudianteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNombre: TextView = itemView.findViewById(R.id.tvNombreCompletoEstudiante)
        val tvCedula: TextView = itemView.findViewById(R.id.tvCedulaEstudiante)
        val btnEditar: ImageButton = itemView.findViewById(R.id.btnEditarEstudiante)
        val btnEliminar: ImageButton = itemView.findViewById(R.id.btnEliminarEstudiante)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EstudianteViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_estudiante, parent, false)
        return EstudianteViewHolder(view)
    }

    override fun onBindViewHolder(holder: EstudianteViewHolder, position: Int) {
        val estudiante = estudiantes[position]

        // Formato pedido: APELLIDOS NOMBRES
        holder.tvNombre.text = "${estudiante.apellido.uppercase()} ${validaNombre(estudiante.nombre)}"
        holder.tvCedula.text = "CI: ${estudiante.cedula}"

        holder.btnEditar.setOnClickListener { onEditClick(estudiante) }
        holder.btnEliminar.setOnClickListener { onDeleteClick(estudiante) }
        holder.itemView.setOnClickListener { onCardClick(estudiante) }
    }

    override fun getItemCount() = estudiantes.size

    fun actualizarLista(nuevaLista: List<Estudiante>) {
        estudiantes = nuevaLista
        notifyDataSetChanged()
    }

    private fun validaNombre(s: String): String {
        return s.split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
    }
}