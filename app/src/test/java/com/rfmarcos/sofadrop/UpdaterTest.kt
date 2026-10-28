package com.rfmarcos.sofadrop

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Which GitHub release each build offers (the list comes newest first, as /releases returns it). */
class UpdaterTest {

    private val releases = (Json.parseToJsonElement(
        """[{"tag_name":"v1.1.0.12-dev","draft":true,"prerelease":true},
            {"tag_name":"v1.1.0.6-dev","draft":false,"prerelease":true},
            {"tag_name":"v1.2.0-beta","draft":false,"prerelease":true},
            {"tag_name":"v1.1.0.5-dev","draft":false,"prerelease":false},
            {"tag_name":"v1.1.0","draft":false,"prerelease":false},
            {"tag_name":"v1.0.2","draft":false,"prerelease":false}]"""
    ) as JsonArray).filterIsInstance<JsonObject>()

    @Test
    fun devBuildTakesNewestDevRelease() {
        assertEquals("v1.1.0.6-dev", Updater.newest(releases, dev = true)?.str("tag_name"))
    }

    @Test
    fun regularBuildSkipsDevAndPrereleases() {
        assertEquals("v1.1.0", Updater.newest(releases, dev = false)?.str("tag_name"))
    }

    @Test
    fun noMatchingRelease() {
        assertNull(Updater.newest(releases.filter { it.str("tag_name")?.endsWith("-dev") == false }, dev = true))
    }

    @Test
    fun devVersionsCompareByRunNumber() {
        assertTrue(Updater.isNewer("1.1.0.10-dev", "1.1.0.9"))
        assertTrue(Updater.isNewer("1.1.0.6", "1.1.0"))
        assertFalse(Updater.isNewer("1.1.0.5-dev", "1.1.0.5"))
    }
}
