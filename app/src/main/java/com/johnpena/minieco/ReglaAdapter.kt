package com.johnpena.minieco

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.johnpena.minieco.database.Regla

class ReglaAdapter(
    private var reglas: List<Regla>,
    private val onEditClick: (Regla) -> Unit,
    private val onDeleteClick: (Regla) -> Unit
) : RecyclerView.Adapter<ReglaAdapter.ReglaViewHolder>() {

    inner class ReglaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvDesecho: TextView = itemView.findViewById(R.id.tvDesechoItem)
        val tvColor: TextView = itemView.findViewById(R.id.tvColorItem)
        val btnEditar: ImageButton = itemView.findViewById(R.id.btnEditarReglaItem)
        val btnEliminar: ImageButton = itemView.findViewById(R.id.btnEliminarReglaItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReglaViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_regla, parent, false)
        return ReglaViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReglaViewHolder, position: Int) {
        val regla = reglas[position]
        holder.tvDesecho.text = regla.tipoDesecho
        holder.tvColor.text = regla.colorTacho

        holder.btnEditar.setOnClickListener { onEditClick(regla) }
        holder.btnEliminar.setOnClickListener { onDeleteClick(regla) }
    }

    override fun getItemCount() = reglas.size

    fun actualizarLista(nuevaLista: List<Regla>) {
        reglas = nuevaLista
        notifyDataSetChanged()
    }
}