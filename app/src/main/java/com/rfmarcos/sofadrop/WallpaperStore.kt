package com.rfmarcos.sofadrop

import android.content.Context
import android.net.Uri
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class StoredWallpaper(
    val file: String,
    val title: String,
    val type: String,
    val tmdbId: Int,
    /** Key of the list it came from (empty in sets made before it was stored). */
    val source: String = "",
)

/**
 * Generated wallpapers live in files/wallpapers/<generation>/ with an index.json.
 * A new generation is written to its own folder and swapped in atomically.
 */
class WallpaperStore(private val context: Context) {

    private val root = File(context.filesDir, DIR)
    private val currentFile = File(root, "current.txt")
    private val servedFile = File(root, "served.txt")
    private val rotationFile = File(root, "rotation.txt")
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    fun newGeneration(): File =
        File(root, "gen-${System.currentTimeMillis()}").apply { mkdirs() }

    /**
     * Make [generation] the current set. The set Projectivy was last given is kept: it caches those
     * URIs (for up to [R.integer.items_cache_duration_millis], or until it restarts) and falls back to
     * its own wallpaper when they fail to load. Before anything was served, the previous set is kept.
     */
    fun publish(generation: File, wallpapers: List<StoredWallpaper>) {
        File(generation, INDEX).writeText(json.encodeToString(wallpapers))
        val keep = servedFile.takeIf { it.exists() }?.readText()?.trim() ?: currentDir()?.name
        val tmp = File(root, "current.tmp")
        tmp.writeText(generation.name)
        tmp.renameTo(currentFile)
        root.listFiles()?.filter { it.isDirectory && it.name != generation.name && it.name != keep }?.forEach { it.deleteRecursively() }
    }

    /** True once Projectivy has been given a set (since 1.0.1). */
    val everServed: Boolean get() = servedFile.exists()

    /** Remember that Projectivy now has the current set's URIs (see [publish]). */
    fun markServed() {
        currentDir()?.let { servedFile.writeText(it.name) }
    }

    /**
     * The part of [wallpapers] to give Projectivy, which keeps no more than [PROJECTIVY_LIMIT]: a larger
     * set is served in turns, each call carrying on where the previous one for the same set stopped.
     */
    fun nextTurn(wallpapers: List<StoredWallpaper>): List<StoredWallpaper> {
        if (wallpapers.size <= PROJECTIVY_LIMIT) return wallpapers
        val generation = currentDir()?.name ?: return wallpapers.take(PROJECTIVY_LIMIT)
        val saved = rotationFile.takeIf { it.exists() }?.readText()?.trim()?.split(" ")
        val start = saved?.takeIf { it.size == 2 && it[0] == generation }?.get(1)?.toIntOrNull() ?: 0
        rotationFile.writeText("$generation ${(start + PROJECTIVY_LIMIT) % wallpapers.size}")
        return turn(wallpapers, start, PROJECTIVY_LIMIT)
    }

    fun discard(generation: File) {
        generation.deleteRecursively()
    }

    private fun currentDir(): File? =
        currentFile.takeIf { it.exists() }?.readText()?.trim()?.let { File(root, it) }?.takeIf { it.isDirectory }

    fun current(): List<StoredWallpaper> {
        val dir = currentDir() ?: return emptyList()
        val index = File(dir, INDEX).takeIf { it.exists() } ?: return emptyList()
        return try {
            json.decodeFromString<List<StoredWallpaper>>(index.readText()).filter { File(dir, it.file).exists() }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun fileOf(wallpaper: StoredWallpaper): File? = currentDir()?.let { File(it, wallpaper.file) }

    /** content:// URI of a current wallpaper, served by [WallpaperContentProvider]. */
    fun uriOf(wallpaper: StoredWallpaper): Uri? {
        val dir = currentDir() ?: return null
        return Uri.Builder().scheme("content").authority("${context.packageName}.wallpapers")
            .appendPath(dir.name).appendPath(wallpaper.file).build()
    }

    companion object {
        private const val INDEX = "index.json"
        const val DIR = "wallpapers"

        /** The most wallpapers Projectivy keeps from one call; the rest of a list is dropped. */
        const val PROJECTIVY_LIMIT = 100

        /** [size] wallpapers of [wallpapers] from [start] on, wrapping round to the first. */
        fun <T> turn(wallpapers: List<T>, start: Int, size: Int): List<T> =
            if (wallpapers.isEmpty()) emptyList()
            else List(minOf(size, wallpapers.size)) { wallpapers[(start + it) % wallpapers.size] }
    }
}
