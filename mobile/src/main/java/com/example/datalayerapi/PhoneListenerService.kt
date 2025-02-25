package com.example.datalayerapi

import android.content.Intent
import android.util.Log
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService

class PhoneListenerService : WearableListenerService() {

    override fun onMessageReceived(messageEvent: MessageEvent) {
        val jsonMessage = String(messageEvent.data)
        Log.d(TAG, "Received JSON: $jsonMessage")

        val intent = Intent("com.example.datalayerapi.MESSAGE_RECEIVED").apply {
            putExtra("JsonData", jsonMessage)
        }
        sendBroadcast(intent)
    }

    companion object {
        private const val TAG = "PhoneListenerService"
        private const val MESSAGE_PATH = "/deploy"
    }
}


