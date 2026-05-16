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


        if (curso.id == cursoActivoId) {
            holder.btnAplicar.visibility = View.GONE
            holder.btnQuitar.visibility = View.VISIBLE
        } else {
            holder.btnAplicar.visibility = View.VISIBLE
            holder.btnQuitar.visibility = View.GONE
        }


        holder.itemView.setOnClickListener { onCursoClick(curso) }


        holder.btnAplicar.setOnClickListener {
            cursoActivoId = curso.id
            prefs.edit().putInt("cursoActivoId", curso.id).apply()
            notifyDataSetChanged()
            onAplicarReglas(curso)
        }


        holder.btnQuitar.setOnClickListener {
            cursoActivoId = -1
            prefs.edit().remove("cursoActivoId").apply()
            notifyDataSetChanged()
            onQuitarReglas(curso)
        }
    }

    override fun getItemCount() = cursos.size


    fun actualizarLista(nuevaLista: List<Curso>) {
        cursos = nuevaLista
        notifyDataSetChanged()
    }
}