package org.linphone.activities.main.settings.fragments

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AppCompatActivity
import com.emanuelef.pcap_receiver.CaptureThread
import java.util.Observable
import org.pcap4j.packet.IpV4Packet

class MpxpMainActivity : AppCompatActivity() {
    var mStart: Button? = null
    var mCapThread: CaptureThread? = null
    var mLog: TextView? = null
    var mCaptureRunning = false
    private val captureStartLauncher =
        registerForActivityResult(StartActivityForResult()) { result: ActivityResult ->
            handleCaptureStartResult(result)
        }
    private val captureStopLauncher =
        registerForActivityResult(StartActivityForResult()) { result: ActivityResult ->
            handleCaptureStopResult(result)
        }
    private val captureStatusLauncher =
        registerForActivityResult(StartActivityForResult()) { result: ActivityResult ->
            handleCaptureStatusResult(result)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startCapture()
        if (savedInstanceState != null && savedInstanceState.containsKey("capture_running")) {
            setCaptureRunning(
                savedInstanceState.getBoolean("capture_running")
            )
        } else {
            queryCaptureStatus()
        }

        // will call the "update" method when the capture status changes
    }

    override fun onDestroy() {
        super.onDestroy()
        stopCapture()
        stopCaptureThread()
    }

    fun update(o: Observable, arg: Any) {
        val capture_running = arg as Boolean
        Log.d(TAG, "capture_running: $capture_running")
        setCaptureRunning(capture_running)
    }

    override fun onSaveInstanceState(bundle: Bundle) {
        bundle.putBoolean("capture_running", mCaptureRunning)
        super.onSaveInstanceState(bundle)
    }

    fun onPacketReceived(pkt: IpV4Packet) {
        val hdr = pkt.header
        mLog!!.append(
            String.format(
                "[%s] %s -> %s [%d B]\n",
                hdr.protocol,
                hdr.srcAddr.hostAddress,
                hdr.dstAddr.hostAddress,
                pkt.length()
            )
        )
        val bundle = Bundle()
        bundle.putString("mLog", mLog.toString())
        val mMpxpSettingFragment = MpxpSettingsFragment()
        mMpxpSettingFragment.arguments = bundle
    }

    fun queryCaptureStatus() {
        Log.d(TAG, "Querying PCAPdroid")
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setClassName(PCAPDROID_PACKAGE, CAPTURE_CTRL_ACTIVITY)
        intent.putExtra("action", "get_status")
        try {
            setCaptureRunning(true)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(
                this,
                "PCAPdroid package not found: " + PCAPDROID_PACKAGE,
                Toast.LENGTH_LONG
            ).show()
        }
    }

    fun startCapture() {
        Log.d(TAG, "Starting PCAPdroid")
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setClassName(PCAPDROID_PACKAGE, CAPTURE_CTRL_ACTIVITY)
        intent.putExtra("action", "start")
        intent.putExtra("broadcast_receiver", "com.emanuelef.pcap_receiver.MyBroadcastReceiver")
        intent.putExtra("pcap_dump_mode", "udp_exporter")
        intent.putExtra("collector_ip_address", "127.0.0.1")
        intent.putExtra("collector_port", "5123")
        // intent.putExtra("app_filter", "org.mozilla.firefox");
        setCaptureRunning(true)
    }

    fun stopCapture() {
        Log.d(TAG, "Stopping PCAPdroid")
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setClassName(PCAPDROID_PACKAGE, CAPTURE_CTRL_ACTIVITY)
        intent.putExtra("action", "stop")
        captureStopLauncher.launch(intent)
    }

    fun setCaptureRunning(running: Boolean) {
        mCaptureRunning = running
        mStart!!.text = if (running) "Stop Capture" else "Start Capture"
        if (mCaptureRunning && mCapThread == null) {
            mCapThread = CaptureThread(this)
            mCapThread!!.start()
        } else if (!mCaptureRunning) stopCaptureThread()
    }

    fun stopCaptureThread() {
        if (mCapThread == null) return
        mCapThread!!.stopCapture()
        mCapThread!!.interrupt()
        mCapThread = null
    }

    fun handleCaptureStartResult(result: ActivityResult) {
        Log.d(TAG, "PCAPdroid start result: $result")
        if (result.resultCode == RESULT_OK) {
            Toast.makeText(this, "Capture started!", Toast.LENGTH_SHORT).show()
            setCaptureRunning(true)
            mLog!!.text = ""
        } else {
            Toast.makeText(this, "Capture failed to start", Toast.LENGTH_SHORT).show()
        }
    }

    fun handleCaptureStopResult(result: ActivityResult) {
        Log.d(TAG, "PCAPdroid stop result: $result")
        if (result.resultCode == RESULT_OK) {
            Toast.makeText(this, "Capture stopped!", Toast.LENGTH_SHORT).show()
            setCaptureRunning(false)
        } else {
            Toast.makeText(this, "Could not stop capture", Toast.LENGTH_SHORT).show()
        }
        val intent = result.data
        if (intent != null && intent.hasExtra("bytes_sent")) logStats(intent)
    }

    fun handleCaptureStatusResult(result: ActivityResult) {
        Log.d(TAG, "PCAPdroid status result: $result")
        if (result.resultCode == RESULT_OK && result.data != null) {
            val intent = result.data
            val running = intent!!.getBooleanExtra("running", false)
            val verCode = intent.getIntExtra("version_code", 0)
            var verName = intent.getStringExtra("version_name")
            if (verName == null) verName = "<1.4.6"
            Log.d(TAG, "PCAPdroid $verName($verCode): running=$running")
            setCaptureRunning(running)
        }
    }

    fun logStats(intent: Intent) {
        val stats = """
               *** Stats ***
               Bytes sent: ${intent.getLongExtra("bytes_sent", 0)}
               Bytes received: ${intent.getLongExtra("bytes_rcvd", 0)}
               Packets sent: ${intent.getIntExtra("pkts_sent", 0)}
               Packets received: ${intent.getIntExtra("pkts_rcvd", 0)}
               Packets dropped: ${intent.getIntExtra("pkts_dropped", 0)}
               PCAP dump size: ${intent.getLongExtra("bytes_dumped", 0)}
        """.trimIndent()
        Log.i("stats", stats)
    }

    companion object {
        const val PCAPDROID_PACKAGE =
            "com.emanuelef.remote_capture" // add ".debug" for the debug build of PCAPdroid
        const val CAPTURE_CTRL_ACTIVITY = "com.emanuelef.remote_capture.activities.CaptureCtrl"
        const val CAPTURE_STATUS_ACTION = "com.emanuelef.remote_capture.CaptureStatus"
        const val TAG = "PCAP Receiver"
    }
}
