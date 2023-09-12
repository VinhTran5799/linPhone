package com.emanuelef.pcap_receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import java.util.Observable
import org.linphone.activities.main.settings.fragments.MpxpMainActivity

class MyBroadcastReceiver : BroadcastReceiver() {
    class CaptureObservable private constructor() : Observable() {
        fun update(running: Boolean) {
            setChanged()
            notifyObservers(running)
        }

        companion object {
            val instance = CaptureObservable()
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d("MyBroadcastReceiver", "onReceive $action")
        if (action == MpxpMainActivity.CAPTURE_STATUS_ACTION) {
            // Notify via the CaptureObservable
            val running = intent.getBooleanExtra("running", true)
            CaptureObservable.instance.update(running)
        }
    }

    companion object {
        private const val TAG = "MyBroadcastReceiver"
    }
}
