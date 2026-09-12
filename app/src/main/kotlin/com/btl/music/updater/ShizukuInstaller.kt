/*
 * BTL Music (2026)
 * © ||BTL||™ (balajitechlabs)
 * GNU GPL-3.0 License
 */

package com.btl.music.updater

import android.content.Context
import android.os.IBinder
import timber.log.Timber
import java.io.File

object ShizukuInstaller {

    fun isShizukuAvailable(): Boolean {
        return try {
            val binderClass = Class.forName("rikka.shizuku.Shizuku")
            val pingBinderMethod = binderClass.getMethod("pingBinder")
            pingBinderMethod.invoke(null) as? Boolean ?: false
        } catch (_: Exception) {
            false
        }
    }

    fun installApkSilently(context: Context, apkFile: File, onResult: (Boolean, String?) -> Unit) {
        if (!isShizukuAvailable()) {
            onResult(false, "Shizuku service is not active")
            return
        }

        try {
            // Using Shizuku process execution for silent pm install
            val shizukuClass = Class.forName("rikka.shizuku.Shizuku")
            val newProcessMethod = shizukuClass.getMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java,
            )
            val command = arrayOf("pm", "install", "-r", apkFile.absolutePath)
            val process = newProcessMethod.invoke(null, command, null, null) as Process
            val exitCode = process.waitFor()

            if (exitCode == 0) {
                Timber.tag("ShizukuInstaller").i("Silent installation succeeded via Shizuku")
                onResult(true, null)
            } else {
                val error = process.errorStream.bufferedReader().readText()
                Timber.tag("ShizukuInstaller").e("Silent installation failed: %s", error)
                onResult(false, error)
            }
        } catch (e: Exception) {
            Timber.tag("ShizukuInstaller").e(e, "Error executing silent install")
            onResult(false, e.message)
        }
    }
}
