package com.example.datalayerapi

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MessageAdapter(private val messages: List<WorkoutData>) :
    RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

    class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val xView: TextView = itemView.findViewById(R.id.xValueTextView)
        val yView: TextView = itemView.findViewById(R.id.yValueTextView)
        val zView: TextView = itemView.findViewById(R.id.zValueTextView)
        val timestampView: TextView = itemView.findViewById(R.id.timestampTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_sensor_data, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val item = messages[position]
        holder.xView.text = "X: %.3f".format(item.x)
        holder.yView.text = "Y: %.3f".format(item.y)
        holder.zView.text = "Z: %.3f".format(item.z)

        val formattedTime = formatTimestamp(item.timestamp)
        holder.timestampView.text = "Time: $formattedTime"
    }

    override fun getItemCount() = messages.size
}
private fun formatTimestamp(timestampMillis: Long): String {
    val date = Date(timestampMillis)
    val sdf = SimpleDateFormat("yyyy.MM.dd HH:mm:ss", Locale.getDefault())
    return sdf.format(date)
}