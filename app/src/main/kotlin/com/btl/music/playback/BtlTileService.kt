/*
 * BTL Music (2026)
 * © ||BTL||™ (balajitechlabs)
 * GNU GPL-3.0 License
 */

package com.btl.music.playback

import android.content.Intent
import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import moe.rukamori.archivetune.MainActivity
import moe.rukamori.archivetune.R

class BtlTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTileState(isPlaying = false)
    }

    override fun onClick() {
        super.onClick()
        val tile = qsTile ?: return

        if (tile.state == Tile.STATE_ACTIVE) {
            updateTileState(isPlaying = false)
        } else {
            updateTileState(isPlaying = true)
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivityAndCollapse(intent)
        }
    }

    private fun updateTileState(isPlaying: Boolean) {
        val tile = qsTile ?: return
        tile.state = if (isPlaying) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = if (isPlaying) "BTL Playing" else "BTL Music"
        tile.icon = Icon.createWithResource(
            this,
            if (isPlaying) R.drawable.pause else R.drawable.play,
        )
        tile.updateTile()
    }
}
