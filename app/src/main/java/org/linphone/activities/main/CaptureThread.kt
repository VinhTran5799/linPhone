 package org.linphone.activities.main

 import android.util.Log
 import org.linphone.activities.main.settings.fragments.MpxpSettingsFragment
 import org.pcap4j.packet.IllegalRawDataException
 import org.pcap4j.packet.IpV4Packet
 import java.io.IOException
 import java.net.DatagramPacket
 import java.net.DatagramSocket
 import java.net.SocketException
 import java.nio.ByteBuffer

 class CaptureThread(val mActivity: MpxpSettingsFragment) : Thread() {
    private var mSocket: DatagramSocket? = null
    override fun run() {
        try {
            // Important: requires "android.permission.INTERNET"
            mSocket = DatagramSocket(5123)
            val buf = ByteArray(65535)
            val datagram = DatagramPacket(buf, buf.size)
            Log.d(TAG, "running")
            while (true) {
                mSocket!!.receive(datagram)
                val len = datagram.length
                val data = ByteBuffer.wrap(buf, 0, len)
                if (len == PCAP_HDR_SIZE && ByteBuffer.wrap(
                        buf,
                        0,
                        PCAP_HDR_START_BYTES.capacity()
                    ) == PCAP_HDR_START_BYTES
                ) {
                    Log.d(TAG, "Detected PCAP header, skipping")
                    continue
                }

                // struct pcaprec_hdr_s
                if (len < 16) {
                    Log.w(TAG, "Invalid PCAP record: $len")
                    continue
                }

                // Skip the pcaprec_hdr_s record to get the IPv4 packet
                try {
                    val pkt = IpV4Packet.newPacket(buf, 16, len - 16)
                    mActivity.runOnUiThread { mActivity.onPacketReceived(pkt) }
                } catch (e: IllegalRawDataException) {
                    // Invalid packet
                    e.printStackTrace()
                }
            }
        } catch (e: IOException) {
            if (e !is SocketException) // raised when capture is stopped
                e.printStackTrace()
        }
    }

    fun stopCapture() {
        if (mSocket != null) mSocket!!.close()
        try {
            join()
        } catch (e: InterruptedException) {
            e.printStackTrace()
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
