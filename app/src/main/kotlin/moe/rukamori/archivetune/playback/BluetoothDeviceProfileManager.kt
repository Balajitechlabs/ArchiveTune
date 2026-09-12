/*
 * BTL Music (2026)
 * © ||BTL||™ (balajitechlabs)
 * GNU GPL-3.0 License
 */

package moe.rukamori.archivetune.playback

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import moe.rukamori.archivetune.utils.dataStore
import org.json.JSONArray
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BluetoothDeviceProfileManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    suspend fun saveProfileForDevice(deviceMacAddress: String, bandGains: FloatArray) {
        val key = stringPreferencesKey("bt_profile_$deviceMacAddress")
        val jsonArray = JSONArray()
        bandGains.forEach { jsonArray.put(it.toDouble()) }

        context.dataStore.edit { preferences ->
            preferences[key] = jsonArray.toString()
        }
        Timber.tag(TAG).d("Saved EQ profile for Bluetooth device: %s", deviceMacAddress)
    }

    suspend fun getProfileForDevice(deviceMacAddress: String): FloatArray? {
        val key = stringPreferencesKey("bt_profile_$deviceMacAddress")
        val preferences = context.dataStore.data.first()
        val raw = preferences[key] ?: return null

        return try {
            val jsonArray = JSONArray(raw)
            val result = FloatArray(jsonArray.length())
            for (i in 0 until jsonArray.length()) {
                result[i] = jsonArray.getDouble(i).toFloat()
            }
            result
        } catch (e: Exception) {
            Timber.tag(TAG).w(e, "Failed to parse Bluetooth EQ profile")
            null
        }
    }

    companion object {
        private const val TAG = "BluetoothProfiles"
    }
}
