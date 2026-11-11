package com.daviddokakis.smallerfields

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.os.Build
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.util.TypedValue
import android.widget.RemoteViews
import androidx.annotation.RequiresApi
import com.dubadai.karoo.common.KarooType
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * A SmallerFields tile as a layout instead of one drawn picture. The Karoo lays it out at the height
 * the tile really has, so on a map page with a route (a shorter row the app isn't told about) the
 * header keeps the Karoo's size and place and the number shrinks to the room left, like the Karoo's
 * own fields. Sizes and positions come from KarooType's header and value rules at the reported size.
 */
@RequiresApi(Build.VERSION_CODES.S)
class FieldViews(private val context: Context) {
    private val colours = TileRenderer(context)
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = context.resources.getFont(R.font.plex_condensed_medium) }
    private val valuePaint = KarooType.valuePaint()

    /** Label font: cap height per px of text size, and how far its ascent reaches above the caps. */
    private val capsPerPx = capHeight(labelPaint, "H") / 100f

    fun build(s: SmallerFieldsState, prefs: Prefs, width: Int, height: Int, night: Boolean, boundaries: Boolean): RemoteViews {
        val side = prefs.layout == Layout.SIDE
        val root = RemoteViews(context.packageName, if (side) R.layout.field_side else R.layout.field_stacked)
        val cards = boundaries && prefs.tiles == Tiles.SEPARATE
        val k = TileRenderer.K
        if (cards) root.setInt(R.id.pair, "setBackgroundColor", if (night) TileRenderer.PAGE_DARK else TileRenderer.PAGE_LIGHT)
        val gap = if (!cards) 0f else (if (side) TileRenderer.GAP_SIDE else TileRenderer.GAP_STACKED) * k
        // The size each half is drawn for (the Karoo's reported size; the layout reflows if it's shorter)
        val hw = if (side) (width - gap) / 2f else width.toFloat()
        val hh = if (side) height.toFloat() else (height - gap) / 2f
        var anyFill = false
        listOf(prefs.first, prefs.second).forEachIndexed { i, m ->
            val col = colours.colors(m, s, prefs.colours(i), night)
            anyFill = anyFill || col.bg != null
            val short = !side && hh / k < TileRenderer.SHORT_HALF
            val half = RemoteViews(
                context.packageName,
                when { side -> R.layout.field_half_side; short -> R.layout.field_half_short; else -> R.layout.field_half_stacked },
            )
            if (i == 1 && gap > 0f) half.setViewLayoutMargin(R.id.half, if (side) RemoteViews.MARGIN_LEFT else RemoteViews.MARGIN_TOP, gap, PX)
            if (cards) half.setInt(R.id.half, "setBackgroundResource", if (night) R.drawable.field_card_dark else R.drawable.field_card_light)
            col.bg?.let { bg ->
                half.setViewVisibility(R.id.fill, android.view.View.VISIBLE)
                half.setImageViewResource(R.id.fill, fillShape(cards, side, i))
                half.setInt(R.id.fill, "setColorFilter", bg)
                if (cards) listOf(RemoteViews.MARGIN_LEFT, RemoteViews.MARGIN_TOP, RemoteViews.MARGIN_RIGHT, RemoteViews.MARGIN_BOTTOM)
                    .forEach { half.setViewLayoutMargin(R.id.fill, it, k, PX) }
            }
            if (short) shortHalf(half, m, s, col, hw, hh, night) else fullHalf(half, m, s, col, hw, hh, stacked = !side, night)
            root.addView(R.id.pair, half)
        }
        if (boundaries && prefs.tiles == Tiles.SINGLE && !anyFill) root.setViewVisibility(R.id.divider, android.view.View.VISIBLE)
        return root
    }

    /** A field the rider hasn't made yet: "FIELD N", then where to set it up (sizes as the picture had them). */
    fun notSetUp(slot: Int, width: Int, height: Int, night: Boolean): RemoteViews {
        val k = TileRenderer.K
        val v = RemoteViews(context.packageName, R.layout.field_not_set_up)
        val room = width - 16f * k
        val titleSize = min(26f * k, height * 0.22f)
        val lineSize = min(19f * k, height * 0.14f)
        val regular = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = context.resources.getFont(R.font.plex_regular) }
        fun fit(p: Paint, text: String, size: Float): Float { p.textSize = size; val w = p.measureText(text); return if (w > room) size * room / w else size }
        val title = context.getString(R.string.field_n, slot + 1).uppercase()
        v.setTextViewText(R.id.title, title)
        v.setTextColor(R.id.title, if (night) Color.WHITE else Color.BLACK)
        v.setTextViewTextSize(R.id.title, PX, fit(labelPaint, title, titleSize))
        listOf(R.id.line1 to R.string.not_set_up, R.id.line2 to R.string.open_app).forEach { (id, res) ->
            val text = context.getString(res)
            v.setTextViewText(id, text)
            v.setTextColor(id, Color.parseColor(if (night) "#B4B2A9" else "#6F6F6F"))
            v.setTextViewTextSize(id, PX, fit(regular, text, lineSize))
        }
        return v
    }

    /** Header above, number under it: KarooType's header rules for a tile [hh] px tall. */
    private fun fullHalf(v: RemoteViews, m: Metric, s: SmallerFieldsState, col: TileRenderer.Colors, hw: Float, hh: Float, stacked: Boolean, night: Boolean) {
        val k = TileRenderer.K
        val hK = (hh / k).roundToInt()
        val caps = KarooType.headerCapsPx(hK) * k
        val capsTop = KarooType.headerCapsTop(hK, fullWidth = false) * k
        val baseline = capsTop + caps
        icon(v, m, col, x = KarooType.HEADER_ICON_LEFT_PX * k, baseline = baseline, box = caps * 25f / 23f, night = night)

        val label = m.label(s.units)
        val right = KarooType.HEADER_RIGHT_INSET_PX * k
        labelPaint.textSize = caps / capsPerPx
        val room = hw - right - (KarooType.HEADER_ICON_LEFT_PX * k + caps * 25f / 23f + 4f * k)
        if (label.isNotEmpty() && labelPaint.measureText(label) > room) labelPaint.textSize *= room / labelPaint.measureText(label)
        v.setTextViewText(R.id.label, label)
        v.setTextColor(R.id.label, col.ink)
        v.setTextViewTextSize(R.id.label, PX, labelPaint.textSize)
        // The label's box starts at its ascent (no font padding): put the caps' top on capsTop
        val capsNow = capsPerPx * labelPaint.textSize
        v.setViewLayoutMargin(R.id.label, RemoteViews.MARGIN_TOP, capsTop - (-labelPaint.fontMetrics.ascent - capsNow), PX)
        v.setViewLayoutMargin(R.id.label, RemoteViews.MARGIN_RIGHT, right, PX)

        val margin = KarooType.VALUE_MARGIN_PX * k
        if (colours.noSensor(m, s)) {
            empty(v, col, hw - 2 * margin, KarooType.EMPTY_CAPS_PX * k, bottom = KarooType.EMPTY_BOTTOM_PX * k)
            return
        }
        val value = KarooType.valueText(m.text(s.value(m), s.units))
        v.setTextViewText(R.id.value, steady(value, m, col.number))
        v.setTextColor(R.id.value, col.number)
        v.setViewLayoutMargin(R.id.value, RemoteViews.MARGIN_LEFT, margin, PX)
        v.setViewLayoutMargin(R.id.value, RemoteViews.MARGIN_RIGHT, margin, PX)
        // The number's box, so that at the reported size it lands where the Karoo puts its own:
        //  - a field: line area tileH - 35, baseline 14 px above the bottom (KarooType valueLineH / valueBaseline);
        //  - a stacked half: the digits' ink fills from 4 px under the header to 8 px above the bottom,
        //    but never bigger than a native field of the half's size (docs/design/field-size).
        // On a shorter row the box shrinks with the tile and auto-size makes the number smaller.
        val m100 = metrics()
        val nativeSize = KarooType.valueSize(valuePaint, sizeBy(value, m), KarooType.valueW(hw.roundToInt()), KarooType.valueLineH(hK)) * k
        val areaTop = baseline + 4f * k
        val areaBottom = hh - margin
        val fillSize = (areaBottom - areaTop) / m100.digit
        val (top, bottom) = if (!stacked || nativeSize < fillSize) {
            val bottomMargin = 14f * k - m100.descent * nativeSize
            (35f * k - bottomMargin) to bottomMargin
        } else {
            val size = fillSize
            val bottomMargin = hh - (areaBottom + m100.descent * size)
            (hh - bottomMargin - m100.line * size) to bottomMargin
        }
        v.setViewLayoutMargin(R.id.value, RemoteViews.MARGIN_TOP, top, PX)
        v.setViewLayoutMargin(R.id.value, RemoteViews.MARGIN_BOTTOM, bottom, PX)
    }

    /** A stacked half too short for a header above: icon and label left, the number right, one line. */
    private fun shortHalf(v: RemoteViews, m: Metric, s: SmallerFieldsState, col: TileRenderer.Colors, hw: Float, hh: Float, night: Boolean) {
        val k = TileRenderer.K
        val caps = 20f * k
        val box = caps * 25f / 23f
        iconSize(v, m, col, box, night)
        v.setViewLayoutMargin(R.id.icon, RemoteViews.MARGIN_LEFT, 12f * k, PX)
        val label = m.label(s.units)
        v.setTextViewText(R.id.label, label)
        v.setTextColor(R.id.label, col.ink)
        v.setTextViewTextSize(R.id.label, PX, caps / capsPerPx)
        v.setViewLayoutMargin(R.id.label, RemoteViews.MARGIN_LEFT, 6f * k, PX)
        val margin = KarooType.VALUE_MARGIN_PX * k
        if (colours.noSensor(m, s)) {
            v.setViewVisibility(R.id.value, android.view.View.GONE)
            v.setViewVisibility(R.id.empty, android.view.View.VISIBLE)
            v.setTextViewText(R.id.empty, TileRenderer.EMPTY)
            v.setTextColor(R.id.empty, col.ink)
            v.setTextViewTextSize(R.id.empty, PX, caps / capsPerPx)
            v.setViewLayoutMargin(R.id.empty, RemoteViews.MARGIN_RIGHT, margin, PX)
            return
        }
        val value = KarooType.valueText(m.text(s.value(m), s.units))
        v.setTextViewText(R.id.value, steady(value, m, col.number))
        v.setTextColor(R.id.value, col.number)
        v.setViewLayoutMargin(R.id.value, RemoteViews.MARGIN_LEFT, 12f * k, PX)
        v.setViewLayoutMargin(R.id.value, RemoteViews.MARGIN_RIGHT, margin, PX)
        // Digits 12 px from top and bottom (like the Karoo's values): a box one line tall around them
        val size = (hh - 24f * k) / metrics().digit
        val pad = max(0f, (hh - metrics().line * size) / 2f).roundToInt()
        v.setViewPadding(R.id.value, 0, pad, 0, pad)
    }

    /** The Karoo's "No Sensor": label font at [capsPx] caps, centred, its bottom [bottom] px up, shrunk to [room]. */
    private fun empty(v: RemoteViews, col: TileRenderer.Colors, room: Float, capsPx: Float, bottom: Float) {
        v.setViewVisibility(R.id.value, android.view.View.GONE)
        v.setViewVisibility(R.id.empty, android.view.View.VISIBLE)
        labelPaint.textSize = capsPx / capsPerPx
        val w = labelPaint.measureText(TileRenderer.EMPTY)
        if (w > room) labelPaint.textSize *= room / w
        v.setTextViewText(R.id.empty, TileRenderer.EMPTY)
        v.setTextColor(R.id.empty, col.ink)
        v.setTextViewTextSize(R.id.empty, PX, labelPaint.textSize)
        v.setViewLayoutMargin(R.id.empty, RemoteViews.MARGIN_BOTTOM, bottom - labelPaint.fontMetrics.descent, PX)
    }

    /** The icon: the glyph's ink (4…34 of 38 units) fills [box] px, at [x], its bottom on [baseline]. */
    private fun icon(v: RemoteViews, m: Metric, col: TileRenderer.Colors, x: Float, baseline: Float, box: Float, night: Boolean) {
        val size = iconSize(v, m, col, box, night)
        val unit = size / 38f
        v.setViewLayoutMargin(R.id.icon, RemoteViews.MARGIN_LEFT, x - 4f * unit, PX)
        v.setViewLayoutMargin(R.id.icon, RemoteViews.MARGIN_TOP, baseline - 34f * unit, PX)
    }

    private fun iconSize(v: RemoteViews, m: Metric, col: TileRenderer.Colors, box: Float, night: Boolean): Float {
        val size = box * 38f / 30f
        v.setImageViewResource(R.id.icon, glyph(m.icon))
        v.setInt(R.id.icon, "setColorFilter", colours.iconColor(m.icon, col, night))
        v.setViewLayoutWidth(R.id.icon, size, PX)
        v.setViewLayoutHeight(R.id.icon, size, PX)
        return size
    }

    /**
     * The value with invisible "8"s in front when it's shorter than the field's usual widest value
     * ([Metric.steady]), so auto-size keeps one size as it changes length (our steady sizing).
     */
    private fun steady(value: String, m: Metric, color: Int): CharSequence {
        val live = value.map { if (it.isDigit()) '8' else it }.joinToString("")
        valuePaint.textSize = 100f
        if (valuePaint.measureText(live) >= valuePaint.measureText(m.steady)) return value
        val pad = m.steady.take(max(0, m.steady.length - live.length))
        if (pad.isEmpty()) return value
        return SpannableString(pad + value).apply {
            setSpan(ForegroundColorSpan(Color.TRANSPARENT), 0, pad.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    private fun sizeBy(value: String, m: Metric): String {
        val live = value.map { if (it.isDigit()) '8' else it }.joinToString("")
        valuePaint.textSize = 100f
        return if (valuePaint.measureText(live) > valuePaint.measureText(m.steady)) live else m.steady
    }

    /** Relative's metrics per px of text size: descent, line (ascent + descent), digit ink height. */
    private class Metrics(val descent: Float, val line: Float, val digit: Float)

    private fun metrics(): Metrics {
        valuePaint.textSize = 100f
        val fm = valuePaint.fontMetrics
        return Metrics(fm.descent / 100f, (fm.descent - fm.ascent) / 100f, capHeight(valuePaint, "8") / 100f)
    }

    private fun capHeight(p: Paint, s: String): Float {
        val old = p.textSize
        p.textSize = 100f
        val r = Rect().also { p.getTextBounds(s, 0, s.length, it) }
        p.textSize = old
        return r.height().toFloat()
    }

    private fun fillShape(cards: Boolean, side: Boolean, i: Int) = when {
        cards -> R.drawable.field_fill_all
        side -> if (i == 0) R.drawable.field_fill_left else R.drawable.field_fill_right
        else -> if (i == 0) R.drawable.field_fill_top else R.drawable.field_fill_bottom
    }

    private fun glyph(icon: Icon) = when (icon) {
        Icon.BOLT -> R.drawable.glyph_bolt
        Icon.HEART -> R.drawable.glyph_heart
        Icon.SPEED -> R.drawable.glyph_speed
        Icon.CADENCE -> R.drawable.glyph_cadence
        Icon.DISTANCE -> R.drawable.glyph_distance
        Icon.CLOCK -> R.drawable.glyph_clock
        Icon.LAP -> R.drawable.glyph_lap
        Icon.ASCENT -> R.drawable.glyph_ascent
        Icon.GRADE -> R.drawable.glyph_grade
    }

    private companion object {
        const val PX = TypedValue.COMPLEX_UNIT_PX
    }
}
