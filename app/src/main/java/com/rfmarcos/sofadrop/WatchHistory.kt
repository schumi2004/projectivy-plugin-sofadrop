package com.rfmarcos.sofadrop

import android.content.Context
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Marking titles watched from the wallpaper viewer, on the tracker chosen as watch history
 * ([Settings.watchSource]). Everything but the constructor does network requests: call it off the
 * main thread.
 */
class WatchHistory(context: Context) {

    /** [Settings.WATCH_TRAKT], [Settings.WATCH_SIMKL], or null when no tracker is connected. */
    val tracker: String? = Settings.watchSource
    val trackerName: String get() = if (tracker == Settings.WATCH_SIMKL) "Simkl" else "Trakt"

    private val trakt = if (tracker == Settings.WATCH_TRAKT) TraktClient(Settings.traktClientId, Settings.traktClientSecret) else null
    private val simkl = if (tracker == Settings.WATCH_SIMKL) SimklClient(Settings.simklClientId, TmdbClient(Settings.token, context.cacheDir)) else null
    private val content by lazy { WatchedContent.load(trakt, simkl) }

    /** What the tracker says about [ref] now. */
    fun status(ref: TitleRef): WatchStatus? = content?.status(ref)

    /** Mark [ref] watched now (a show: all its episodes); returns the time used, which [undo] needs. */
    fun mark(ref: TitleRef): String {
        val watchedAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'.000Z'", Locale.US)
            .apply { timeZone = TimeZone.getTimeZone("UTC") }.format(Date())
        val added = trakt?.addToHistory(ref, watchedAt) ?: simkl?.addToHistory(ref, watchedAt) ?: false
        if (!added) throw IOException("$trackerName doesn't know this title")
        return watchedAt
    }

    /** Take back what [mark] did at [watchedAt]. */
    fun undo(ref: TitleRef, watchedAt: String): Boolean =
        trakt?.removeFromHistory(ref, watchedAt) ?: simkl?.removeFromHistory(ref) ?: false

    /**
     * Trakt removes only the plays [mark] added. Simkl keeps one state per title, so undoing a show that
     * was being watched would also wipe its earlier progress: only titles without one can be undone there.
     */
    fun canUndo(before: WatchStatus?) = tracker == Settings.WATCH_TRAKT || before == null
}
