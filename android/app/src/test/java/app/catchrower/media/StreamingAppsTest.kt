package app.catchrower.media

import org.junit.Assert.assertEquals
import org.junit.Test

class StreamingAppsTest {
    @Test
    fun dayOneIncludesBravePrimeVideoAndAppleTv() {
        val byId = StreamingApps.DAY_ONE.associateBy { it.id }

        assertEquals("Brave", byId.getValue("brave").label)
        assertEquals(listOf("com.brave.browser"), byId.getValue("brave").packageIds)

        assertEquals("Prime Video", byId.getValue("prime").label)
        assertEquals(
            listOf(
                "com.amazon.avod.thirdpartyclient",
                "com.amazon.amazonvideo.livingroom",
            ),
            byId.getValue("prime").packageIds,
        )

        assertEquals("Apple TV", byId.getValue("appletv").label)
        assertEquals(
            listOf("com.apple.atve.androidtv.appletv"),
            byId.getValue("appletv").packageIds,
        )
    }
}
