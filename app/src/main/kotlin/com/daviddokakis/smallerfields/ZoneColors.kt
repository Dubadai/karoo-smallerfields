package com.daviddokakis.smallerfields

import android.graphics.Color

/**
 * The Karoo's own zone colours. Its ride app colours power AND heart rate zones from one table
 * (ride app colours climb1..7, the same in light and dark mode, black text on them) and picks
 * from it by how many zones the rider profile has: 7 (power) → all seven, 5 (HR) → c1 c2 c3 c4 c6.
 * Found in the ride app's code (docs/design/first-look/README.md).
 */
object ZoneColors {
    private val KAROO = intArrayOf(
        0xFF60EEB2.toInt(), 0xFF00B988.toInt(), 0xFFFFF500.toInt(), 0xFFFB8C65.toInt(),
        0xFFFE581F.toInt(), 0xFFD60404.toInt(), 0xFFB700A2.toInt(),
    )
    /** The same hues, deeper, for numbers on white (BetterIntervals' chart colours: 3:1 or more on white). */
    private val DEEP = intArrayOf(
        0xFF20AA78.toInt(), 0xFF007A5A.toInt(), 0xFFB9A300.toInt(), 0xFFE2683A.toInt(),
        0xFFD9480F.toInt(), 0xFFC40404.toInt(), 0xFFA3008F.toInt(),
    )

    /** Which of the seven colours zone [zone] (0-based) of [count] zones gets, as the Karoo picks. */
    private fun pick(zone: Int, count: Int): Int? {
        val picks = when (count) {
            1 -> listOf(3)
            2 -> listOf(2, 6)
            3 -> listOf(2, 3, 6)
            4 -> listOf(2, 3, 4, 6)
            5 -> listOf(1, 2, 3, 4, 6)
            6 -> listOf(1, 2, 3, 4, 6, 7)
            else -> listOf(1, 2, 3, 4, 5, 6, 7)
        }
        return picks.getOrNull(zone)?.minus(1)
    }

    /** The Karoo's strong zone colour, as its fields fill with Zone Color on. */
    fun strong(zone: Int, count: Int): Int? = pick(zone, count)?.let { KAROO[it] }

    /** The zone colour for a number on the plain tile: deeper on white, the Karoo's own on black. */
    fun number(zone: Int, count: Int, night: Boolean): Int? = pick(zone, count)?.let { if (night) KAROO[it] else DEEP[it] }

    /** [fg] over [bg] at [t] (0..1), opaque. */
    fun mix(fg: Int, bg: Int, t: Float): Int = Color.rgb(
        (Color.red(bg) + (Color.red(fg) - Color.red(bg)) * t).toInt(),
        (Color.green(bg) + (Color.green(fg) - Color.green(bg)) * t).toInt(),
        (Color.blue(bg) + (Color.blue(fg) - Color.blue(bg)) * t).toInt(),
    )
}
