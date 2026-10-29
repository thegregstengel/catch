package app.catchrower.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class PlainNotesTest {
    @Test
    fun rejoinsWrappedLinesAndKeepsHeadingsAndBullets() {
        val notes = """
            The first stable release of Catch. It contains everything in the beta,
            and was tested on a tablet.

            ### Rowing

            - Live metrics from the brake, including
              power and split.
            - A session summary.
        """.trimIndent()
        assertEquals(
            "The first stable release of Catch. It contains everything in the beta, and was tested on a tablet.\n" +
                "Rowing\n" +
                "• Live metrics from the brake, including power and split.\n" +
                "• A session summary.",
            plainNotes(notes),
        )
    }
}
