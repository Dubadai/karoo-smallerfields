package com.daviddokakis.smallerfields

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View

/**
 * DEBUG ONLY: what the field gallery (FieldGallery.kt) draws, with the field's real renderer and
 * the showcase numbers (ExampleData.STATE). Boundaries on, as in our mockups.
 */
object GalleryFields {
    private val LOOKS = mapOf(
        "separate-side" to Prefs(Metric.POWER_3S, Metric.HR),
        "separate-stacked" to Prefs(Metric.POWER_3S, Metric.HR, layout = Layout.STACKED),
        "single-side" to Prefs(Metric.SPEED, Metric.LAP_TIME, tiles = Tiles.SINGLE),
        "icons-a" to Prefs(Metric.CADENCE, Metric.DISTANCE),
        "icons-b" to Prefs(Metric.RIDE_TIME, Metric.ASCENT),
        "icons-c" to Prefs(Metric.GRADE, Metric.SPEED),
        "karoo-bg" to Prefs(Metric.POWER_3S, Metric.HR, colours1 = Colours(background = Background.KAROO), colours2 = Colours(background = Background.KAROO)),
        "mixed" to Prefs(Metric.POWER_3S, Metric.HR, colours1 = Colours(background = Background.KAROO), colours2 = Colours(NumberColor.ZONE)),
        "soft-zone" to Prefs(Metric.POWER_3S, Metric.HR, layout = Layout.STACKED, colours1 = Colours(NumberColor.ZONE, Background.SOFT), colours2 = Colours(NumberColor.ZONE, Background.SOFT)),
    )
    /** The standard looks (docs/design/gallery/README.md) plus the "Not set up yet" tile. */
    /** A ride for the README GIF (seq-<look>-00…): 3s power builds into zone 5 and eases off, heart rate follows. */
    private val SEQ = listOf(196 to 128, 214 to 129, 238 to 131, 262 to 134, 284 to 138, 306 to 143, 324 to 148, 331 to 152, 312 to 155, 288 to 153, 258 to 149, 226 to 144)

    val variants = LOOKS.keys.filterNot { it.startsWith("icons-") } + "not-set-up" +
        SEQ.indices.flatMap { i -> listOf("mixed", "soft-zone", "karoo-bg").map { "seq-$it-%02d".format(i) } }
    val sizes: List<String>? = listOf("quarter", "small", "row", "half")

    fun render(context: Context, variant: String, w: Int, h: Int, night: Boolean): Bitmap {
        val fv = FieldViews(context)
        if (variant == "not-set-up") return draw(context, fv.notSetUp(0, w, h, night), w, h)
        Regex("seq-(.+)-(\\d+)").matchEntire(variant)?.destructured?.let { (look, n) ->
            val (p, hr) = SEQ[n.toInt()]
            val state = ExampleData.STATE.copy(values = ExampleData.STATE.values + mapOf(Metric.POWER_3S to p.toDouble(), Metric.HR to hr.toDouble()))
            return draw(context, fv.build(state, LOOKS.getValue(look), w, h, night, boundaries = true), w, h)
        }
        return draw(context, fv.build(ExampleData.STATE, LOOKS.getValue(variant), w, h, night, boundaries = true), w, h)
    }

    /** The field's layout as the Karoo shows it: applied, laid out at [w] × [h], drawn. */
    private fun draw(context: Context, rv: android.widget.RemoteViews, w: Int, h: Int): Bitmap {
        val view = rv.apply(context, android.widget.FrameLayout(context))
        view.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY))
        view.layout(0, 0, w, h)
        return Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).also { view.draw(Canvas(it)) }
    }
}
