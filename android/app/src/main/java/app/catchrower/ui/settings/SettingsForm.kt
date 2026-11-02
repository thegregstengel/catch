package app.catchrower.ui.settings

import app.catchrower.data.HouseholdGate
import app.catchrower.update.UpdateChannel

/**
 * Catch Settings edits stay on this form until Save. Miles are not accepted.
 * The goal is a whole number of meters.
 */
object SettingsForm {
    const val MIN_GOAL_METERS = 1_000

    data class Parsed(
        val monthlyGoalMeters: Int,
        val disabledAppPackages: Set<String>,
        val updateChannel: UpdateChannel,
        val settingsPin: String,
        val screenOffWhenIdle: Boolean,
    )

    sealed class ParseResult {
        data class Ok(val parsed: Parsed) : ParseResult()
        data class Invalid(val message: String) : ParseResult()
    }

    fun parse(
        monthlyGoalText: String,
        disabledAppPackages: Set<String>,
        updateChannel: UpdateChannel,
        pinText: String,
        existingPin: String,
        screenOffWhenIdle: Boolean,
    ): ParseResult {
        val goal = monthlyGoalText.toIntOrNull()
        if (goal == null || goal < MIN_GOAL_METERS) {
            return ParseResult.Invalid(
                "Enter the monthly family goal in meters, at least 1,000.",
            )
        }
        val pin = when {
            pinText.isEmpty() -> existingPin
            HouseholdGate.isWellFormed(pinText) -> pinText
            else -> return ParseResult.Invalid("The PIN must be 4 digits.")
        }
        return ParseResult.Ok(
            Parsed(
                monthlyGoalMeters = goal,
                disabledAppPackages = disabledAppPackages,
                updateChannel = updateChannel,
                settingsPin = pin,
                screenOffWhenIdle = screenOffWhenIdle,
            ),
        )
    }
}
