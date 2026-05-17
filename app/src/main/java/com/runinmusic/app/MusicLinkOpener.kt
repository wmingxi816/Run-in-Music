package com.runinmusic.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.runinmusic.app.core.model.SongCandidate

object MusicLinkOpener {
    fun open(context: Context, song: SongCandidate): Boolean {
        val url = song.qqUrl ?: song.neteaseUrl ?: return false
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return runCatching {
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }
}
