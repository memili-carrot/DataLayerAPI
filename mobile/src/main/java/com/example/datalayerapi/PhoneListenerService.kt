package com.example.datalayerapi

import android.content.Intent
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import org.json.JSONObject

class PhoneListenerService : WearableListenerService() {

    override fun onMessageReceived(messageEvent: MessageEvent) {
        val path = messageEvent.path
        val jsonMessage = String(messageEvent.data)
        Log.d(TAG, "Received message on path: $path")

        // batteryUsed 값도 추출
        val json = JSONObject(jsonMessage)
        val batteryUsed = json.optInt("batteryUsed", -1)

        when (path) {
            "/accelerometer" -> broadcast("com.example.datalayerapi.ACCELEROMETER_RECEIVED", jsonMessage, batteryUsed)
            "/gyroscope"     -> broadcast("com.example.datalayerapi.GYROSCOPE_RECEIVED", jsonMessage, batteryUsed)
            "/light"         -> broadcast("com.example.datalayerapi.LIGHT_RECEIVED", jsonMessage, batteryUsed)
            "/magnetic"      -> broadcast("com.example.datalayerapi.MAGNETIC_RECEIVED", jsonMessage, batteryUsed)
            "/heartrate"     -> broadcast("com.example.datalayerapi.HEARTRATE_RECEIVED", jsonMessage, batteryUsed) // ✅ 추가
            else             -> Log.w(TAG, "Unknown path received: $path")
        }
    }

    private fun broadcast(action: String, message: String, batteryUsed: Int) {
        val intent = Intent(action).apply {
            putExtra("JsonData", message)
            putExtra("BatteryUsed", batteryUsed)
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    companion object {
        private const val TAG = "PhoneListenerService"
    }
}