package com.example.a2024capstonesample

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.a2024capstonesample.Room.MyEntity
import com.example.a2024capstonesample.databinding.DataRoomBinding

class MyEntityAdapter(
    private val entities: List<MyEntity>,
    private val onDeleteClick: () -> Unit
) : RecyclerView.Adapter<MyEntityAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val itemDate: TextView = itemView.findViewById(R.id.itemDate)
        val itemImagePath: TextView = itemView.findViewById(R.id.itemImagePath)
        val itemScore: TextView = itemView.findViewById(R.id.itemScore)

        fun bind(entity: MyEntity) {
            itemDate.text = entity.date
            itemImagePath.text = entity.imagePath
            itemScore.text = entity.formattedScore
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_entity, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(entities[position])
    }

    override fun getItemCount(): Int = entities.size
}

