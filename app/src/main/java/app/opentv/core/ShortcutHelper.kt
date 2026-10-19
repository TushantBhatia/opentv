/*
 * This file is part of OpenTV.
 * Copyright (C) 2026 The OpenTV Contributors
 * Licensed under the GNU General Public License v3.0 or later.
 */
package app.opentv.core

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.widget.Toast
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import app.opentv.MainActivity
import app.opentv.R
import app.opentv.data.model.Channel
import app.opentv.data.model.shownName
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object ShortcutHelper {

    fun pinChannel(context: Context, channel: Channel, scope: CoroutineScope) {
        if (!ShortcutManagerCompat.isRequestPinShortcutSupported(context)) {
            Toast.makeText(context, "Shortcut pinning not supported on this launcher", Toast.LENGTH_SHORT).show()
            return
        }

        scope.launch(Dispatchers.IO) {
            val iconCompat = fetchChannelIcon(context, channel.logoUrl)
            
            val intent = Intent(context, MainActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                putExtra(MainActivity.EXTRA_PLAY_CHANNEL, channel.id)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }

            val shortcut = ShortcutInfoCompat.Builder(context, "channel_${channel.id}")
                .setShortLabel(channel.shownName)
                .setLongLabel(channel.shownName)
                .setIcon(iconCompat)
                .setIntent(intent)
                .build()

            withContext(Dispatchers.Main) {
                val success = ShortcutManagerCompat.requestPinShortcut(context, shortcut, null)
                if (success) {
                    Toast.makeText(context, "Pinned ${channel.shownName} to Home Screen", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "Failed to pin shortcut", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private suspend fun fetchChannelIcon(context: Context, logoUrl: String?): IconCompat {
        if (logoUrl.isNullOrBlank()) {
            return IconCompat.createWithResource(context, R.mipmap.ic_launcher)
        }

        return try {
            val loader = ImageLoader(context)
            val req = ImageRequest.Builder(context)
                .data(logoUrl)
                .allowHardware(false)
                .build()
            val result = (loader.execute(req) as? SuccessResult)?.drawable
            if (result != null) {
                val bitmap = if (result is BitmapDrawable) {
                    result.bitmap
                } else {
                    val bmp = Bitmap.createBitmap(
                        result.intrinsicWidth.coerceAtLeast(128),
                        result.intrinsicHeight.coerceAtLeast(128),
                        Bitmap.Config.ARGB_8888
                    )
                    val canvas = Canvas(bmp)
                    result.setBounds(0, 0, canvas.width, canvas.height)
                    result.draw(canvas)
                    bmp
                }
                IconCompat.createWithBitmap(bitmap)
            } else {
                IconCompat.createWithResource(context, R.mipmap.ic_launcher)
            }
        } catch (_: Throwable) {
            IconCompat.createWithResource(context, R.mipmap.ic_launcher)
        }
    }
}
