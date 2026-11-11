package com.daviddokakis.smallerfields

import io.hammerhead.karooext.models.DataType
import kotlin.math.roundToInt

/** Which zones colour a value (from the Karoo rider profile), if any. */
enum class ZoneKind { POWER, HR }

/** How a raw Karoo value (SI units: W, bpm, m/s, m, ms, %) becomes the number on the tile. */
enum class Format { INT, SPEED, DISTANCE, TIME, ELEVATION, GRADE }

/**
 * Everything a SmallerFields half can show. [short] is the label next to the icon: as short as
 * possible, the icon says the rest (bolt = power, so 3s power is just "3S"). Speed, distance and
 * ascent show their unit instead (KM/H or MPH...). Never rename the enum constants: settings
 * store them by name.
 */
enum class Metric(
    val type: String,
    val nameRes: Int,
    val short: String,
    val icon: Icon,
    val format: Format,
    val zone: ZoneKind? = null,
) {
    POWER(DataType.Type.POWER, R.string.m_power, "W", Icon.BOLT, Format.INT, ZoneKind.POWER),
    POWER_3S(DataType.Type.SMOOTHED_3S_AVERAGE_POWER, R.string.m_power_3s, "3S", Icon.BOLT, Format.INT, ZoneKind.POWER),
    POWER_10S(DataType.Type.SMOOTHED_10S_AVERAGE_POWER, R.string.m_power_10s, "10S", Icon.BOLT, Format.INT, ZoneKind.POWER),
    POWER_AVG(DataType.Type.AVERAGE_POWER, R.string.m_power_avg, "AVG", Icon.BOLT, Format.INT),
    POWER_NP(DataType.Type.NORMALIZED_POWER, R.string.m_power_np, "NP", Icon.BOLT, Format.INT),
    POWER_LAP(DataType.Type.POWER_LAP, R.string.m_power_lap, "LAP", Icon.BOLT, Format.INT),
    HR(DataType.Type.HEART_RATE, R.string.m_hr, "HR", Icon.HEART, Format.INT, ZoneKind.HR),
    HR_AVG(DataType.Type.AVERAGE_HR, R.string.m_hr_avg, "AVG", Icon.HEART, Format.INT),
    HR_LAP(DataType.Type.AVERAGE_LAP_HR, R.string.m_hr_lap, "LAP", Icon.HEART, Format.INT),
    CADENCE(DataType.Type.CADENCE, R.string.m_cadence, "RPM", Icon.CADENCE, Format.INT),
    SPEED(DataType.Type.SPEED, R.string.m_speed, "", Icon.SPEED, Format.SPEED),
    SPEED_AVG(DataType.Type.AVERAGE_SPEED, R.string.m_speed_avg, "AVG", Icon.SPEED, Format.SPEED),
    DISTANCE(DataType.Type.DISTANCE, R.string.m_distance, "", Icon.DISTANCE, Format.DISTANCE),
    RIDE_TIME(DataType.Type.RIDE_TIME, R.string.m_ride_time, "RIDE", Icon.CLOCK, Format.TIME),
    LAP_TIME(DataType.Type.ELAPSED_TIME_LAP, R.string.m_lap_time, "LAP", Icon.LAP, Format.TIME),
    ASCENT(DataType.Type.ELEVATION_GAIN, R.string.m_ascent, "", Icon.ASCENT, Format.ELEVATION),
    GRADE(DataType.Type.ELEVATION_GRADE, R.string.m_grade, "%", Icon.GRADE, Format.GRADE),
    ;

    /** The label: [short], or the unit for speed, distance and ascent. */
    fun label(units: Units): String = when (format) {
        Format.SPEED -> if (short.isEmpty()) units.speed else short
        Format.DISTANCE -> units.distance
        Format.ELEVATION -> units.elevation
        else -> short
    }

    /**
     * The widest value this field normally shows (digits as 8), so the number keeps one size as it
     * changes length (99 -> 100 W, 9.9 -> 10.0 km/h); only rarer, longer values shrink it.
     */
    val steady: String get() = when (format) {
        Format.INT -> "888"
        Format.SPEED -> "88.8"
        Format.DISTANCE -> "88.8"
        Format.ELEVATION -> "8888"
        Format.GRADE -> "-88.8"
        Format.TIME -> if (this == RIDE_TIME) "8:88:88" else "88:88"
    }

    /** From a sensor (power meter, heart rate strap, cadence): with no value the field says "No Sensor", like the Karoo's. */
    val sensor: Boolean get() = zone != null || this in setOf(POWER_AVG, POWER_NP, POWER_LAP, HR_AVG, HR_LAP, CADENCE)

    /** The number as the Karoo shows it, or "--" without data. */
    fun text(value: Double?, units: Units): String {
        if (value == null || value.isNaN()) return "--"
        return when (format) {
            Format.INT -> value.roundToInt().toString()
            Format.SPEED -> oneDecimal(value * if (units.imperial) 2.236936 else 3.6)
            Format.DISTANCE -> oneDecimal(value / if (units.imperial) 1609.344 else 1000.0)
            Format.ELEVATION -> (value * if (units.imperialElevation) 3.28084 else 1.0).roundToInt().toString()
            Format.GRADE -> oneDecimal(value)
            Format.TIME -> time((value / 1000).toLong())
        }
    }

    private fun oneDecimal(v: Double) = String.format(java.util.Locale.US, "%.1f", v)

    private fun time(totalSec: Long): String {
        val s = totalSec.coerceAtLeast(0)
        val h = s / 3600; val m = (s % 3600) / 60; val sec = s % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, sec) else "%d:%02d".format(m, sec)
    }
}

/** The rider's units (Karoo rider profile). */
data class Units(val imperial: Boolean = false, val imperialElevation: Boolean = false) {
    val speed get() = if (imperial) "MPH" else "KM/H"
    val distance get() = if (imperial) "MI" else "KM"
    val elevation get() = if (imperialElevation) "FT" else "M"
}
