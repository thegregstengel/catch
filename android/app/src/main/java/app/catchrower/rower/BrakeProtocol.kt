package app.catchrower.rower

/**
 * The Hydrow brake controller's serial protocol: space-separated text lines.
 *
 * Live traffic uses lowercase headers (`di2`, `ds2`, `rl`) and float
 * fields. Commands are terminated with `\n\r`.
 */
object BrakeProtocol {
    const val BAUD = 921_600

    val UART_CANDIDATES = listOf(
        "/dev/ttyMT1",
        "/dev/ttyS1",
        "/dev/ttyACM0",
        "/dev/ttyUSB0",
    )

    // Command prefixes (BrakePacketType) — firmware accepts these as written.
    const val CMD_CONTINUOUS = "Cm"
    const val CMD_SET_RESISTANCE = "Cl"
    const val CMD_QUERY_RESISTANCE = "Ql"

    // Canonical response headers (matched case-insensitively)
    const val HDR_DS2 = "ds2"
    const val HDR_DS = "ds"
    const val HDR_DI2 = "di2"
    const val HDR_DI = "di"
    const val HDR_RESISTANCE = "rl"

    fun terminate(command: String): String = "$command\n\r"

    fun continuousModeOn(): String = terminate("$CMD_CONTINUOUS 1")

    fun setResistance(level: Int): String =
        terminate("$CMD_SET_RESISTANCE ${level.coerceIn(1, 20)}")

    fun queryResistance(): String = terminate(CMD_QUERY_RESISTANCE)

    data class ParsedStroke(
        val distanceMeters: Float,
        val averagePowerWatts: Float,
        val sequence: Int,
        /** True when [distanceMeters] is session-cumulative from the brake. */
        val distanceIsAbsolute: Boolean,
    )

    data class ParsedInstant(
        val watts: Float,
        val sequence: Int,
        val handleMeters: Float? = null,
    )

    /**
     * Parse a single UART text line into stroke / instantaneous / resistance updates.
     */
    fun parseLine(raw: String): ParsedPacket? {
        val line = raw.trim()
        if (line.isEmpty()) return null
        // Packets may arrive glued after a stripped CR; keep the first token only.
        val parts = line.split(Regex("\\s+"))
        if (parts.isEmpty()) return null
        val header = parts[0].lowercase()
        return when (header) {
            HDR_DS2 -> parseDs2(parts)
            HDR_DS -> parseDsLegacy(parts)
            HDR_DI2, HDR_DI -> parseDi(parts)
            HDR_RESISTANCE -> {
                val level = parts.getOrNull(1)?.toFloatOrNull()?.toInt() ?: return null
                ParsedPacket.Resistance(level)
            }
            else -> null
        }
    }

    private fun parseDs2(parts: List<String>): ParsedPacket? {
        // ds2 startHandle endHandle distance averagePower endOfRecovery endOfDrive sequence
        if (parts.size < 8) return null
        val distance = parts[3].toFloatOrNull() ?: return null
        val power = parts[4].toFloatOrNull() ?: return null
        val seq = parts[7].toFloatOrNull()?.toInt() ?: return null
        // Brake reports cumulative meters for the stroke session when distance grows large;
        // small values (~stroke length) are treated as deltas by the accumulator.
        val absolute = distance >= 20f
        return ParsedPacket.Stroke(ParsedStroke(distance, power, seq, absolute))
    }

    private fun parseDsLegacy(parts: List<String>): ParsedPacket? {
        // ds start end driveRev10 recoveryRev10 watts10 ... sequence
        if (parts.size < 9) return null
        val watts = parts[5].toFloatOrNull() ?: return null
        val seq = parts[8].toFloatOrNull()?.toInt() ?: return null
        val driveRev = (parts[3].toFloatOrNull() ?: 0f) / 10f
        val distance = (driveRev * 0.08f).coerceAtLeast(0.5f)
        val power = if (watts > 50f) watts else watts / 10f
        return ParsedPacket.Stroke(ParsedStroke(distance, power, seq, distanceIsAbsolute = false))
    }

    private fun parseDi(parts: List<String>): ParsedPacket? {
        // Live: di2 rpm revolutions watts handleMeters sequence
        // Legacy ints: di rpm10 revolutions100 watts10 handleMilliMeter sequence
        if (parts.size < 6) return null
        val wattsField = parts[3].toFloatOrNull() ?: return null
        val handle = parts[4].toFloatOrNull()
        val seq = parts[5].toFloatOrNull()?.toInt() ?: return null
        // Legacy integer packets used tenths of a watt; live di2 sends watts as floats.
        val looksLegacyInt = parts[3].toIntOrNull() != null && !parts[3].contains('.')
        val watts = if (looksLegacyInt) wattsField / 10f else wattsField
        return ParsedPacket.Instant(ParsedInstant(watts, seq, handle))
    }

    sealed class ParsedPacket {
        data class Stroke(val data: ParsedStroke) : ParsedPacket()
        data class Instant(val data: ParsedInstant) : ParsedPacket()
        data class Resistance(val level: Int) : ParsedPacket()
    }
}
