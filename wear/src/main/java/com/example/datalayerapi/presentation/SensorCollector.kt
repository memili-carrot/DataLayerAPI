package com.example.datalayerapi.presentation

import android.content.Context
import android.hardware.*
import android.os.Handler
import android.os.Looper

class SensorCollector(
    private val context: Context,
    private val sensorType: Int,
    private val sensorDelay: Int,
    private val durationSec: Int,
    private val onComplete: (List<SensorData>) -> Unit
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val dataList = mutableListOf<SensorData>()
    private var sensor: Sensor? = null

    fun start() {
        sensor = sensorManager.getDefaultSensor(sensorType)
        sensor?.let {
            sensorManager.registerListener(this, it, sensorDelay)
            Handler(Looper.getMainLooper()).postDelayed({
                stop()
            }, durationSec * 1000L)
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
        onComplete(dataList)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            val data = SensorData(
                x = it.values.getOrElse(0) { 0f },
                y = it.values.getOrElse(1) { 0f },
                z = it.values.getOrElse(2) { 0f },
                timestamp = System.currentTimeMillis()
            )
            dataList.add(data)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}