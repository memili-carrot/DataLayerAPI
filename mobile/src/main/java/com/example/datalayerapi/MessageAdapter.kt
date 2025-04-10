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
        val xTextView: TextView = itemView.findViewById(R.id.xValueTextView)
        val yTextView: TextView = itemView.findViewById(R.id.yValueTextView)
        val zTextView: TextView = itemView.findViewById(R.id.zValueTextView)
        val deleteButton: ImageView = itemView.findViewById(R.id.deleteButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.message_item, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val messageData = messages[position]
        holder.timestampTextView.text = messageData.timestamp.toString()
        holder.xTextView.text = "X: ${messageData.x}"
        holder.yTextView.text = "Y: ${messageData.y}"
        holder.zTextView.text = "Z: ${messageData.z}"

        holder.deleteButton.setOnClickListener {
            messages.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, messages.size)
        }
    }

    override fun getItemCount(): Int = messages.size
}