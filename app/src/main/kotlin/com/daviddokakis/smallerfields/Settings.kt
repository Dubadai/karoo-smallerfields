package com.daviddokakis.smallerfields

import android.content.Context
import android.content.SharedPreferences

enum class Layout { SIDE, STACKED }
enum class Tiles { SEPARATE, SINGLE }
enum class NumberColor { PLAIN, ZONE }
enum class Background { NONE, KAROO, SOFT }

/** How one of the two data is coloured. */
data class Colours(val number: NumberColor = NumberColor.PLAIN, val background: Background = Background.NONE)

/** One SmallerFields field's choices. Layout and tiles are the whole tile's; colours each data's own. */
data class Prefs(
    val first: Metric,
    val second: Metric,
    val layout: Layout = Layout.SIDE,
    val tiles: Tiles = Tiles.SEPARATE,
    val colours1: Colours = Colours(),
    val colours2: Colours = Colours(),
) {
    /** The colours of data [i] (0: first, 1: second). */
    fun colours(i: Int) = if (i == 0) colours1 else colours2
    fun withColours(i: Int, c: Colours) = if (i == 0) copy(colours1 = c) else copy(colours2 = c)
}

/**
 * The rider's fields (Field 1–10), stored on the Karoo (app-private). The Karoo lists all ten
 * fields from the start; a field the rider hasn't made yet "exists" = false and shows
 * "Not set up yet" on a ride page.
 */
object Settings {
    const val SLOTS = 10
    private const val FILE = "smallerfields"

    /** A new field starts as 3s power | heart rate, side by side, separate tiles. */
    val NEW = Prefs(Metric.POWER_3S, Metric.HR)

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    /** Field [slot] + 1 (0-based slot) has been made in the app. */
    fun exists(context: Context, slot: Int): Boolean = prefs(context).getBoolean(key(slot, "exists"), false)

    /** The fields the rider has made, in order (0-based slots). */
    fun made(context: Context): List<Int> = (0 until SLOTS).filter { exists(context, it) }

    /** The slot a new field takes: the lowest free one, or null when all ten are in use. */
    fun nextFree(context: Context): Int? = (0 until SLOTS).firstOrNull { !exists(context, it) }

    fun read(context: Context, slot: Int): Prefs {
        val p = prefs(context)
        val d = NEW
        return Prefs(
            first = enumOr(p.getString(key(slot, "first"), null), d.first),
            second = enumOr(p.getString(key(slot, "second"), null), d.second),
            layout = enumOr(p.getString(key(slot, "layout"), null), d.layout),
            tiles = enumOr(p.getString(key(slot, "tiles"), null), d.tiles),
            colours1 = colours(p, slot, 1),
            colours2 = colours(p, slot, 2),
        )
    }

    /** Data [n]'s colours; fields made before colours were per data keep their shared ones for both. */
    private fun colours(p: SharedPreferences, slot: Int, n: Int): Colours {
        val number = p.getString(key(slot, "number$n"), null) ?: p.getString(key(slot, "number"), null)
        val background = p.getString(key(slot, "background$n"), null) ?: p.getString(key(slot, "background"), null)
        return Colours(enumOr(number, NumberColor.PLAIN), enumOr(background, Background.NONE))
    }

    /** Saves [v] as field [slot] (making it if it didn't exist). */
    fun write(context: Context, slot: Int, v: Prefs) {
        prefs(context).edit()
            .putBoolean(key(slot, "exists"), true)
            .putString(key(slot, "first"), v.first.name)
            .putString(key(slot, "second"), v.second.name)
            .putString(key(slot, "layout"), v.layout.name)
            .putString(key(slot, "tiles"), v.tiles.name)
            .putString(key(slot, "number1"), v.colours1.number.name)
            .putString(key(slot, "background1"), v.colours1.background.name)
            .putString(key(slot, "number2"), v.colours2.number.name)
            .putString(key(slot, "background2"), v.colours2.background.name)
            .remove(key(slot, "number")).remove(key(slot, "background"))
            .apply()
    }

    /** Frees field [slot]: its number can be used again. */
    fun delete(context: Context, slot: Int) {
        val e = prefs(context).edit()
        listOf("exists", "first", "second", "layout", "tiles", "number", "background", "number1", "background1", "number2", "background2").forEach { e.remove(key(slot, it)) }
        e.apply()
    }

    private fun key(slot: Int, name: String) = "${slot + 1}_$name"

    private inline fun <reified E : Enum<E>> enumOr(name: String?, default: E): E =
        name?.let { n -> enumValues<E>().firstOrNull { it.name == n } } ?: default
}
