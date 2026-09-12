/*
 * BTL Music (2026)
 * © ||BTL||™ (balajitechlabs)
 * GNU GPL-3.0 License
 */

package moe.rukamori.archivetune.playback

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import timber.log.Timber

class BluetoothAutomationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        when (action) {
            BluetoothDevice.ACTION_ACL_DISCONNECTED,
            AudioManager.ACTION_AUDIO_BECOMING_NOISY -> {
                Timber.tag(TAG).i("Audio becoming noisy / Bluetooth disconnected; signaling pause")
                val keyEvent = android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_MEDIA_PAUSE)
                val mediaIntent = Intent(Intent.ACTION_MEDIA_BUTTON).apply {
                    putExtra(Intent.EXTRA_KEY_EVENT, keyEvent)
                    setPackage(context.packageName)
                }
                context.sendBroadcast(mediaIntent)
            }
            BluetoothDevice.ACTION_ACL_CONNECTED -> {
                Timber.tag(TAG).i("Bluetooth audio device connected")
            }
        }
    }

    companion object {
        private const val TAG = "BluetoothAutomation"
    }
}
