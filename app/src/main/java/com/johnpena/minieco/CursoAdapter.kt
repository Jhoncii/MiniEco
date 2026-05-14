package com.johnpena.minieco

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.johnpena.minieco.database.Curso

class CursoAdapter(
    private var cursos: List<Curso>,
    private val context: Context,
    private val onCursoClick: (Curso) -> Unit,
    private val onAplicarReglas: (Curso) -> Unit,
    private val onQuitarReglas: (Curso) -> Unit
) : RecyclerView.Adapter<CursoAdapter.CursoViewHolder>() {

    // Leemos qué curso está activo actualmente en la memoria del teléfono
    private val prefs = context.getSharedPreferences("MiniEcoPrefs", Context.MODE_PRIVATE)
    private var cursoActivoId = prefs.getInt("cursoActivoId", -1)

    inner class CursoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNombre: TextView = itemView.findViewById(R.id.tvItemNombreCurso)
        val tvDesc: TextView = itemView.findViewById(R.id.tvItemDescCurso)
        val btnAplicar: ImageButton = itemView.findViewById(R.id.btnAplicarReglas)
        val btnQuitar: ImageButton = itemView.findViewById(R.id.btnQuitarReglas)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CursoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_curso, parent, false)
        return CursoViewHolder(view)
    }

    override fun onBindViewHolder(holder: CursoViewHolder, position: Int) {
        val curso = cursos[position]
        holder.tvNombre.text = curso.nombre
        holder.tvDesc.text = curso.descripcion

        // Lógica de los botones Verde y Rojo
        if (curso.id == cursoActivoId) {
            holder.btnAplicar.visibility = View.GONE
            holder.btnQuitar.visibility = View.VISIBLE
        } else {
            holder.btnAplicar.visibility = View.VISIBLE
            holder.btnQuitar.visibility = View.GONE
        }

        // Si tocan la tarjeta (entrar a ver estudiantes y reglas)
        holder.itemView.setOnClickListener { onCursoClick(curso) }

        // Botón Verde (Aplicar)
        holder.btnAplicar.setOnClickListener {
            cursoActivoId = curso.id
            prefs.edit().putInt("cursoActivoId", curso.id).apply()
            notifyDataSetChanged() // Actualiza la lista para cambiar los botones
            onAplicarReglas(curso)
        }

        // Botón Rojo (Quitar)
        holder.btnQuitar.setOnClickListener {
            cursoActivoId = -1
            prefs.edit().remove("cursoActivoId").apply()
            notifyDataSetChanged() // Actualiza la lista
            onQuitarReglas(curso)
        }
    }

    override fun getItemCount() = cursos.size

    // Función para recargar la lista cuando creas o borras un curso
    fun actualizarLista(nuevaLista: List<Curso>) {
        cursos = nuevaLista
        notifyDataSetChanged()
    }
}