package com.rfmarcos.sofadrop

import org.junit.Assert.assertEquals
import org.junit.Test

/** How a set larger than Projectivy keeps is served in turns, see [WallpaperStore.turn]. */
class RotationTest {

    private val set = (0 until 180).toList()

    @Test
    fun turnWrapsRoundToTheFirst() {
        val second = WallpaperStore.turn(set, 100, 100)
        assertEquals(100, second.size)
        assertEquals((100 until 180) + (0 until 20), second)
    }

    @Test
    fun everyWallpaperComesUpWithinTwoTurns() {
        val first = WallpaperStore.turn(set, 0, 100)
        val second = WallpaperStore.turn(set, 100 % set.size, 100)
        assertEquals(set.toSet(), (first + second).toSet())
    }

    @Test
    fun smallSetIsServedWhole() {
        assertEquals(listOf(1, 2, 3), WallpaperStore.turn(listOf(1, 2, 3), 2, 100).sorted())
        assertEquals(emptyList<Int>(), WallpaperStore.turn(emptyList<Int>(), 0, 100))
    }
}
