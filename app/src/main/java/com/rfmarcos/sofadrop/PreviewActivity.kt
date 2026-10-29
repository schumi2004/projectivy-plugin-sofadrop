package com.rfmarcos.sofadrop

import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.util.concurrent.Executors

/**
 * Full-screen view of the generated wallpapers: ◀ ▶ browse, OK shows one in the settings preview,
 * ▼ blocks its title (see [Settings.blocked]), ▲ marks it watched on the watch history's tracker
 * (pressed twice, see [toggleWatched]), and the info cluster fades out when idle so the wallpaper
 * can be seen whole.
 */
class PreviewActivity : AppCompatActivity() {

    private lateinit var image: ImageView
    private lateinit var overlay: View
    private lateinit var pinBadge: View
    private lateinit var store: WallpaperStore
    private var wallpapers: List<StoredWallpaper> = emptyList()
    private var position = 0
    private val handler = Handler(Looper.getMainLooper())
    private val hideOverlay = Runnable { overlay.animate().alpha(0f).setDuration(400).start(); updatePinBadge() }

    // Watch history: what the tracker says, what was marked here (to undo it), and a pending confirmation
    private val watchHistory by lazy { WatchHistory(this) }
    private val worker = Executors.newSingleThreadExecutor()
    private val watchStatuses = mutableMapOf<TitleRef, WatchStatus?>()
    private val marked = mutableMapOf<TitleRef, Pair<String, WatchStatus?>>()
    private var confirming: TitleRef? = null
    private var confirmUntil = 0L
    private var busy = false
    private var historyChanged = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Settings.init(this)
        setContentView(R.layout.preview_activity)
        image = findViewById(R.id.viewer_image)
        overlay = findViewById(R.id.viewer_overlay)
        pinBadge = findViewById(R.id.viewer_pin_badge)
        store = WallpaperStore(this)
        wallpapers = store.current()
        // Open on the pinned wallpaper so the user sees what they chose
        position = wallpapers.indexOfFirst { pinOf(it) == Settings.previewPin }.coerceAtLeast(0)
        show()
    }

    private val overlayHidden get() = overlay.alpha < 0.5f

    private fun showOverlay() {
        overlay.animate().cancel()
        overlay.alpha = 1f
        pinBadge.visibility = View.GONE
        handler.removeCallbacks(hideOverlay)
        handler.postDelayed(hideOverlay, 4000)
    }

    private fun updatePinBadge() {
        val pinned = wallpapers.isNotEmpty() && pinOf(wallpapers[position]) == Settings.previewPin
        pinBadge.visibility = if (pinned && overlayHidden) View.VISIBLE else View.GONE
    }

    private fun show() {
        val title = findViewById<TextView>(R.id.viewer_title)
        val subtitle = findViewById<TextView>(R.id.viewer_subtitle)
        if (wallpapers.isEmpty()) {
            title.setText(R.string.preview_empty)
            return
        }
        val wallpaper = wallpapers[position]
        val file = store.fileOf(wallpaper)
        val metrics = resources.displayMetrics
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file?.path, options)
        var sample = 1
        while (options.outWidth / (sample * 2) >= metrics.widthPixels) sample *= 2
        image.setImageBitmap(BitmapFactory.decodeFile(file?.path, BitmapFactory.Options().apply { inSampleSize = sample }))

        title.text = wallpaper.title
        val source = Sources.available().firstOrNull { it.key == wallpaper.source }?.let { Sources.label(this, it) }
        subtitle.text = listOfNotNull(getString(R.string.viewer_position, position + 1, wallpapers.size), source).joinToString("  ·  ")
        refreshInfo()
        showOverlay()
    }

    /** Badges and key hints of the wallpaper shown, which follow pinning, blocking and the watch status. */
    private fun refreshInfo() {
        if (wallpapers.isEmpty()) return
        val wallpaper = wallpapers[position]
        val pinned = pinOf(wallpaper) == Settings.previewPin
        findViewById<View>(R.id.viewer_pinned).visibility = if (pinned) View.VISIBLE else View.GONE
        val blocked = refOf(wallpaper) in Settings.blockedTitles
        findViewById<View>(R.id.viewer_blocked).visibility = if (blocked) View.VISIBLE else View.GONE
        val chips = findViewById<LinearLayout>(R.id.viewer_chips)
        chips.removeAllViews()
        chips.addView(chip("◀ ▶", getString(R.string.viewer_browse)))
        chips.addView(chip("OK", getString(if (pinned && !picking) R.string.viewer_unpin else R.string.viewer_use)))
        if (!picking) chips.addView(chip("▼", getString(if (blocked) R.string.viewer_unblock else R.string.viewer_block)))

        val ref = refOf(wallpaper)
        val watched = findViewById<TextView>(R.id.viewer_watched)
        if (watchHistory.tracker == null || picking) {
            watched.visibility = View.GONE
            return
        }
        if (ref !in watchStatuses) {
            watched.visibility = View.GONE
            loadStatus(ref)
            return
        }
        val status = watchStatuses[ref]
        watched.visibility = if (status == null) View.GONE else View.VISIBLE
        watched.text = when (status) {
            is WatchStatus.InProgress -> getString(R.string.viewer_watching, status.completed, status.aired)
            else -> getString(R.string.viewer_watched)
        }
        val hint = when {
            ref in marked -> R.string.viewer_watch_undo
            status == WatchStatus.Watched -> null // watched before: nothing to do here
            confirming == ref && System.currentTimeMillis() < confirmUntil -> R.string.viewer_watch_confirm
            else -> R.string.viewer_watch_mark
        }
        if (hint != null) chips.addView(chip("▲", getString(hint)))
    }

    /** Ask the tracker about [ref] in the background, then show it if it is still on screen. */
    private fun loadStatus(ref: TitleRef) {
        worker.execute {
            val status = runCatching { watchHistory.status(ref) }.getOrNull()
            handler.post {
                if (isDestroyed) return@post
                watchStatuses[ref] = status
                if (wallpapers.getOrNull(position)?.let(::refOf) == ref) refreshInfo()
            }
        }
    }

    /**
     * ▲ marks the title watched on the tracker after a second press (for a show: all its episodes), or
     * takes back what was marked here. Titles already watched before are left alone.
     */
    private fun toggleWatched() {
        val wallpaper = wallpapers[position]
        val ref = refOf(wallpaper)
        if (watchHistory.tracker == null || picking || busy || ref !in watchStatuses) return
        val name = watchHistory.trackerName
        marked[ref]?.let { (watchedAt, before) ->
            if (!watchHistory.canUndo(before)) {
                toast(getString(R.string.watch_undo_unavailable, name))
                return
            }
            inBackground({ watchHistory.undo(ref, watchedAt) }) { result ->
                if (result.getOrNull() == true) {
                    marked -= ref
                    watchStatuses[ref] = before
                    historyChanged = true
                    toast(getString(R.string.watch_unmarked, name))
                } else {
                    toast(getString(R.string.watch_failed, name, result.exceptionOrNull()?.message.orEmpty()))
                }
            }
            return
        }
        val before = watchStatuses[ref]
        if (before == WatchStatus.Watched) {
            toast(getString(R.string.watch_already, name))
            return
        }
        val now = System.currentTimeMillis()
        if (confirming != ref || now > confirmUntil) {
            confirming = ref
            confirmUntil = now + CONFIRM_MILLIS
            val message = if (ref.type == MediaType.TV) R.string.watch_confirm_show else R.string.watch_confirm_movie
            toast(getString(message, wallpaper.title, name))
            handler.postDelayed({ if (!isDestroyed) refreshInfo() }, CONFIRM_MILLIS + 100)
            return
        }
        confirming = null
        inBackground({ watchHistory.mark(ref) }) { result ->
            result.onSuccess { watchedAt ->
                marked[ref] = watchedAt to before
                watchStatuses[ref] = WatchStatus.Watched
                historyChanged = true
                toast(getString(R.string.watch_marked, name))
            }.onFailure {
                toast(getString(R.string.watch_failed, name, it.message.orEmpty()))
            }
        }
    }

    private fun <T> inBackground(task: () -> T, done: (Result<T>) -> Unit) {
        busy = true
        worker.execute {
            val result = runCatching(task)
            handler.post {
                busy = false
                if (isDestroyed) return@post
                done(result)
                refreshInfo()
            }
        }
    }

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_LONG).show()

    /** A key glyph in a white rounded square followed by what it does, on a translucent pill. */
    private fun chip(key: String, label: String): View {
        fun dp(v: Int) = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v.toFloat(), resources.displayMetrics).toInt()
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundResource(R.drawable.chip_background)
            setPadding(dp(6), dp(5), dp(14), dp(5))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                .apply { marginStart = dp(10) }
            addView(TextView(context).apply {
                text = key
                setTextColor(Color.rgb(11, 26, 36))
                textSize = 12f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setBackgroundResource(R.drawable.chip_key)
                setPadding(dp(7), dp(2), dp(7), dp(2))
            })
            addView(TextView(context).apply {
                text = label
                setTextColor(Color.argb(230, 255, 255, 255))
                textSize = 14f
                setPadding(dp(8), 0, 0, 0)
            })
        }
    }

    private fun pinOf(wallpaper: StoredWallpaper) = "${wallpaper.type}:${wallpaper.tmdbId}"

    private fun refOf(wallpaper: StoredWallpaper) =
        TitleRef(if (wallpaper.type == MediaType.TV.path) MediaType.TV else MediaType.MOVIE, wallpaper.tmdbId)

    /** Block or unblock the title shown; the current set keeps it until the next one is made. */
    private fun toggleBlock() {
        val wallpaper = wallpapers[position]
        val block = refOf(wallpaper) !in Settings.blockedTitles
        Settings.setBlocked(refOf(wallpaper), block, wallpaper.title)
        // A blocked title can't stay the settings preview
        if (block && Settings.previewPin == pinOf(wallpaper)) Settings.pinPreview("", "")
        Toast.makeText(this, if (block) R.string.viewer_blocked_toast else R.string.viewer_unblocked_toast, Toast.LENGTH_LONG).show()
    }

    /** Opened from "Preview title" to pick one: OK keeps it and goes back there. */
    private val picking get() = intent.getBooleanExtra(EXTRA_PICK, false)

    private fun togglePin() {
        val wallpaper = wallpapers[position]
        if (picking) {
            Settings.pinPreview(pinOf(wallpaper), wallpaper.title)
            finish()
            return
        }
        val pinned = Settings.previewPin != pinOf(wallpaper)
        if (pinned) Settings.pinPreview(pinOf(wallpaper), wallpaper.title) else Settings.pinPreview("", "")
        Toast.makeText(this, if (pinned) R.string.preview_pinned else R.string.preview_unpinned, Toast.LENGTH_SHORT).show()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (wallpapers.isEmpty()) return super.onKeyDown(keyCode, event)
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_MEDIA_NEXT -> position = (position + 1) % wallpapers.size
            KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_MEDIA_PREVIOUS -> position = (position - 1 + wallpapers.size) % wallpapers.size
            // While the overlay is hidden, OK only brings it back: nothing gets pinned by accident
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER -> if (!overlayHidden) togglePin()
            // Same as OK: only with the overlay shown, and not while picking the preview
            KeyEvent.KEYCODE_DPAD_DOWN -> if (!overlayHidden && !picking) toggleBlock()
            KeyEvent.KEYCODE_DPAD_UP -> if (!overlayHidden && !picking) toggleWatched()
            else -> return super.onKeyDown(keyCode, event)
        }
        show()
        return true
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        worker.shutdown()
        // Watched badges and hidden watched titles come from the tracker at each generation
        if (historyChanged) GenerateWallpapersWorker.runNow(applicationContext)
        super.onDestroy()
    }

    companion object {
        const val EXTRA_PICK = "pick"
        private const val CONFIRM_MILLIS = 6000L
    }
}
