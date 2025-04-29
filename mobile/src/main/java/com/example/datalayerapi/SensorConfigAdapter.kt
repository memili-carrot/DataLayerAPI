package com.example.datalayerapi

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView

class SensorConfigAdapter(
    private val context: Context,
    private val sensorOptions: List<String>,
    private val delayOptions: List<String>,
    private val configList: MutableList<SensorConfigItem>
) : RecyclerView.Adapter<SensorConfigAdapter.ConfigViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConfigViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.sensor_config_item, parent, false)
        return ConfigViewHolder(view)
    }

    override fun onBindViewHolder(holder: ConfigViewHolder, position: Int) {
        val config = configList[position]

        // 센서 스피너 설정
        val sensorAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, sensorOptions)
        sensorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        holder.sensorSpinner.adapter = sensorAdapter
        holder.sensorSpinner.setSelection(sensorOptions.indexOf(config.sensorName))
        holder.sensorSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
                config.sensorName = sensorOptions[pos]
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // 딜레이 스피너 설정
        val delayAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, delayOptions)
        delayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        holder.delaySpinner.adapter = delayAdapter
        holder.delaySpinner.setSelection(delayOptions.indexOf(config.delayOption))
        holder.delaySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
                config.delayOption = delayOptions[pos]
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        holder.durationEditText.setText(config.durationSec.toString())
        holder.durationEditText.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                config.durationSec = holder.durationEditText.text.toString().toIntOrNull() ?: 5
            }
        }

        // 삭제 버튼
        holder.removeButton.setOnClickListener {
            configList.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, configList.size)
        }
    }

    override fun getItemCount(): Int = configList.size

    inner class ConfigViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val sensorSpinner: Spinner = itemView.findViewById(R.id.sensorSpinner)
        val delaySpinner: Spinner = itemView.findViewById(R.id.delaySpinner)
        val durationEditText: EditText = itemView.findViewById(R.id.durationEditText)
        val removeButton: ImageButton = itemView.findViewById(R.id.removeButton)
    }
}