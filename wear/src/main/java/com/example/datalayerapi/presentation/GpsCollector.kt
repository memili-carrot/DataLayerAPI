package com.example.datalayerapi.presentation

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.android.gms.location.LocationServices
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject

class GpsCollector(
    private val context: Context,
    private val durationSec: Int
) {

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    private val handler = Handler(Looper.getMainLooper())

    fun start() {
        handler.postDelayed({ stop() }, durationSec * 1000L)
        getAndSendLocation()
    }

    @SuppressLint("MissingPermission")
    private fun getAndSendLocation() {
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                Log.d(TAG, "🛰 lastLocation result: $location")

                if (location != null) {
                    val json = JSONObject().apply {
                        val arr = JSONArray().apply {
                            put(JSONObject().apply {
                                put("timestamp", System.currentTimeMillis())
                                put("sensor", "GPS")
                                put("x", location.latitude)
                                put("y", location.longitude)
                                put("z", 0f)
                            })
                        }
                        put("gps", arr)
                    }

                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val node = Wearable.getNodeClient(context).connectedNodes.await().firstOrNull()
                            node?.let {
                                Wearable.getMessageClient(context)
                                    .sendMessage(it.id, "/gps", json.toString().toByteArray())
                                    .addOnSuccessListener {
                                        Log.d(TAG, "✅ GPS sent")
                                    }
                                    .addOnFailureListener { e ->
                                        Log.e(TAG, "❌ Failed to send GPS", e)
                                    }
                            } ?: Log.w(TAG, "⚠️ No connected node found for GPS")
                        } catch (e: Exception) {
                            Log.e(TAG, "❌ Error during GPS send: ${e.message}", e)
                        }
                    }
                } else {
                    Log.w(TAG, "⚠️ location is null – cannot send GPS data")
                }
            }
            .addOnFailureListener {
                Log.e(TAG, "❌ Failed to get lastLocation", it)
            }
    }

    fun stop() {
        handler.removeCallbacksAndMessages(null)
    }

    companion object {
        private const val TAG = "GpsCollector"
    }
}