/*
 * BTL Music (2026)
 * © ||BTL||™ (balajitechlabs)
 * GNU GPL-3.0 License
 */

package moe.rukamori.archivetune.ui.player

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

object LyricCardExporter {

    fun generateLyricCardBitmap(
        context: Context,
        songTitle: String,
        artistName: String,
        selectedLyrics: List<String>,
        artworkBitmap: Bitmap? = null,
        width: Int = 1080,
        height: Int = 1920,
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Draw vibrant dark gradient background
        val bgPaint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, width.toFloat(), height.toFloat(),
                intArrayOf(Color.rgb(20, 20, 30), Color.rgb(8, 8, 12), Color.rgb(30, 15, 25)),
                null,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Draw optional blurred artwork backdrop
        artworkBitmap?.let { art ->
            val artPaint = Paint().apply { alpha = 40 }
            val scaledArt = Bitmap.createScaledBitmap(art, width, width, true)
            canvas.drawBitmap(scaledArt, 0f, (height - width) / 2f, artPaint)
        }

        // Draw pill card container
        val cardPaint = Paint().apply {
            color = Color.argb(180, 30, 30, 40)
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val cardRect = RectF(80f, 250f, width - 80f, height - 350f)
        canvas.drawRoundRect(cardRect, 48f, 48f, cardPaint)

        // Draw App Branding
        val brandPaint = Paint().apply {
            color = Color.rgb(200, 200, 220)
            textSize = 36f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText("BTL MUSIC", 140f, 340f, brandPaint)

        // Draw Song Title & Artist
        val titlePaint = Paint().apply {
            color = Color.WHITE
            textSize = 52f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText(songTitle.take(28), 140f, 420f, titlePaint)

        val artistPaint = Paint().apply {
            color = Color.rgb(170, 170, 190)
            textSize = 38f
            typeface = Typeface.DEFAULT
            isAntiAlias = true
        }
        canvas.drawText(artistName.take(32), 140f, 480f, artistPaint)

        // Draw Lyrics Quotes
        val lyricPaint = Paint().apply {
            color = Color.WHITE
            textSize = 56f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            isAntiAlias = true
        }

        var startY = 640f
        for (line in selectedLyrics.take(7)) {
            canvas.drawText(line, 140f, startY, lyricPaint)
            startY += 90f
        }

        // Draw Footer
        val footerPaint = Paint().apply {
            color = Color.rgb(130, 130, 150)
            textSize = 28f
            isAntiAlias = true
        }
        canvas.drawText("||BTL||™ (balajitechlabs)", 140f, height - 400f, footerPaint)

        return bitmap
    }

    fun saveBitmapToCache(context: Context, bitmap: Bitmap): File {
        val file = File(context.cacheDir, "lyric_card_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        return file
    }
}
