package app.catchrower.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StatsResetTest {
    @Test
    fun unconfirmedResetDoesNotErase() {
        assertTrue(
            StatsReset.decide(StatsReset.Target.Profile, profileId = 4L, confirmed = false)
                is StatsReset.Decision.Refused,
        )
        assertTrue(
            StatsReset.decide(StatsReset.Target.Household, profileId = null, confirmed = false)
                is StatsReset.Decision.Refused,
        )
        assertTrue(
            StatsReset.decide(StatsReset.Target.Profile, profileId = null, confirmed = true)
                is StatsReset.Decision.Refused,
        )
    }

    @Test
    fun confirmedProfileResetErasesOnlyThatProfile() {
        val decision = StatsReset.decide(StatsReset.Target.Profile, profileId = 4L, confirmed = true)
        assertEquals(StatsReset.Decision.EraseProfile(4L), decision)
    }

    @Test
    fun confirmedHouseholdResetErasesHouseholdMetrics() {
        assertEquals(
            StatsReset.Decision.EraseHousehold,
            StatsReset.decide(StatsReset.Target.Household, profileId = null, confirmed = true),
        )
    }

    @Test
    fun warningsNameWhatWillBeErased() {
        val profile = StatsReset.warning(StatsReset.Target.Profile, "Ada")
        assertTrue(profile.title.contains("Ada"))
        assertTrue(profile.body.contains("saved sessions"))
        assertTrue(profile.body.contains("distance"))
        assertTrue(profile.body.contains("calendar days"))
        assertTrue(profile.body.contains("awards"))
        assertTrue(profile.body.contains("challenge progress"))
        assertTrue(profile.body.contains("profile itself stays"))
        assertFalse(profile.body.contains("PIN"))

        val household = StatsReset.warning(StatsReset.Target.Household, "Ada")
        assertTrue(household.body.contains("every rower"))
        assertTrue(household.body.contains("saved sessions"))
        assertTrue(household.body.contains("distance"))
        assertTrue(household.body.contains("awards"))
        assertTrue(household.body.contains("challenge progress"))
        assertTrue(household.body.contains("Profiles"))
        assertTrue(household.body.contains("settings PIN"))
        assertTrue(household.body.contains("family goal stay"))
    }
}
