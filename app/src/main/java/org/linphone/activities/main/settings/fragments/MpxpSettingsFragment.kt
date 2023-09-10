/*
 * Copyright (c) 2010-2020 Belledonne Communications SARL.
 *
 * This file is part of linphone-android
 * (see https://www.linphone.org).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.linphone.activities.main.settings.fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.lifecycle.ViewModelProvider
import org.linphone.R
import org.linphone.activities.GenericActivity
import org.linphone.activities.main.settings.viewmodels.MpxpSettingsViewModel
import org.linphone.databinding.SettingsMpxpFragmentBinding
import android.content.ActivityNotFoundException
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AppCompatActivity
import org.pcap4j.packet.IpV4Packet
import java.nio.ByteBuffer

class MpxpSettingsFragment : GenericSettingFragment<SettingsMpxpFragmentBinding>(){
    private lateinit var viewModel: MpxpSettingsViewModel
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


    override fun getLayoutId(): Int = R.layout.settings_mpxp_fragment
    var mLog: String = ""

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.lifecycleOwner = viewLifecycleOwner
        binding.sharedMainViewModel = sharedViewModel

        viewModel = ViewModelProvider(this)[MpxpSettingsViewModel::class.java]
        binding.viewModel = viewModel

        startCapture()
        binding.pktsLog.text = mLog
    }

    fun startCapture() {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setClassName(PCAPDROID_PACKAGE, CAPTURE_CTRL_ACTIVITY)
        intent.putExtra("action", "start")
        intent.putExtra("broadcast_receiver", "com.emanuelef.pcap_receiver.MyBroadcastReceiver")
        intent.putExtra("pcap_dump_mode", "udp_exporter")
        intent.putExtra("collector_ip_address", "127.0.0.1")
        intent.putExtra("collector_port", "5123")
        //intent.putExtra("app_filter", "org.mozilla.firefox");
        captureStartLauncher.launch(intent)
    }

    fun onPacketReceived(pkt: IpV4Packet) {
        val hdr = pkt.header
        mLog!!.append(
            String.format(
                "[%s] %s -> %s [%d B]\n",
                hdr.protocol,
                hdr.srcAddr.hostAddress, hdr.dstAddr.hostAddress,
                pkt.length()
            )
        )
    }

    fun queryCaptureStatus() {
        Log.d(TAG, "Querying PCAPdroid")
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setClassName(PCAPDROID_PACKAGE, CAPTURE_CTRL_ACTIVITY)
        intent.putExtra("action", "get_status")
        try {
            captureStatusLauncher.launch(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(
                this,
                "PCAPdroid package not found: " + PCAPDROID_PACKAGE,
                Toast.LENGTH_LONG
            ).show()
        }
    }
    companion object {
        const val TAG = "CaptureThread"
        const val PCAP_HDR_SIZE = 24
        val PCAP_HDR_START_BYTES = ByteBuffer.wrap(hex2bytes("d4c3b2a1020004000000000000000000"))
        fun hex2bytes(s: String): ByteArray {
            val len = s.length
            val data = ByteArray(len / 2)
            var i = 0
            while (i < len) {
                data[i / 2] = ((s[i].digitToIntOrNull(16) ?: -1 shl 4)
                + s[i + 1].digitToIntOrNull(16)!! ?: -1).toByte()
                i += 2
            }
            return data
        }
    }
}
