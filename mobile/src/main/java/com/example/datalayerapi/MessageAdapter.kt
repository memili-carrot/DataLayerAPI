package com.example.datalayerapi

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MessageAdapter(private val messages: MutableList<WorkoutData>) :
    RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

    inner class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val timestampTextView: TextView = itemView.findViewById(R.id.timestampTextView)
        val durationTextView: TextView = itemView.findViewById(R.id.durationTextView)
        val repsTextView: TextView = itemView.findViewById(R.id.repsTextView)
        val deleteButton: ImageView = itemView.findViewById(R.id.deleteButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.message_item, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val messageData = messages[position]

        holder.timestampTextView.text = messageData.timestamp.chunked(10).joinToString("\n")
        holder.durationTextView.text = "시간: ${messageData.duration}초"
        holder.repsTextView.text = "반복: ${messageData.reps}회"

        holder.deleteButton.setOnClickListener {
            messages.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, messages.size)
        }
    }

    override fun getItemCount(): Int = messages.size
}

