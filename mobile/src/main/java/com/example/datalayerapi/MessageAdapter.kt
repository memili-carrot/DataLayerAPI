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
        val valueLabelView: TextView = itemView.findViewById(R.id.valueLabelTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_sensor_data, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val item = messages[position]

        // 시간 포맷 표시
        val formattedTime = formatTimestamp(item.timestamp)
        holder.timestampView.text = "Time: $formattedTime"

        when (item.sensor) {
            "Light" -> {
                holder.valueLabelView.visibility = View.VISIBLE
                holder.valueLabelView.text = "lux: %.2f".format(item.x)

                holder.xView.visibility = View.GONE
                holder.yView.visibility = View.GONE
                holder.zView.visibility = View.GONE
            }

            "HeartRate" -> {
                holder.valueLabelView.visibility = View.VISIBLE
                holder.valueLabelView.text = "bpm: %.1f".format(item.x)

                holder.xView.visibility = View.GONE
                holder.yView.visibility = View.GONE
                holder.zView.visibility = View.GONE
            }

            "GPS" -> {
                holder.valueLabelView.visibility = View.VISIBLE
                holder.valueLabelView.text = "lat: %.5f / lon: %.5f".format(item.x, item.y)

                holder.xView.visibility = View.GONE
                holder.yView.visibility = View.GONE
                holder.zView.visibility = View.GONE
            }


            else -> {
                // 기본: 3축 센서
                holder.valueLabelView.visibility = View.GONE

                holder.xView.visibility = View.VISIBLE
                holder.yView.visibility = View.VISIBLE
                holder.zView.visibility = View.VISIBLE

                holder.xView.text = "X: %.3f".format(item.x)
                holder.yView.text = "Y: %.3f".format(item.y)
                holder.zView.text = "Z: %.3f".format(item.z)
            }
        }
    }

    override fun getItemCount(): Int = messages.size
}

private fun formatTimestamp(timestampMillis: Long): String {
    val date = Date(timestampMillis)
    val sdf = SimpleDateFormat("yyyy.MM.dd HH:mm:ss", Locale.getDefault())
    return sdf.format(date)
}