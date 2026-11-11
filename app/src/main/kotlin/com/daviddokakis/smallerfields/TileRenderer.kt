package com.daviddokakis.smallerfields

import com.dubadai.karoo.common.*
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Draws one SmallerFields tile into a bitmap: two data, each with the Karoo's own field parts
 * (icon top left, short label top right, number bottom right), side by side or stacked.
 *
 * Tiles (design: docs/design/first-look/, round 3):
 * - Separate, data field boundaries on: two small cards (radius 14, 1 px #7D7D7D border), 2 px
 *   apart side by side, 6 px stacked, like the Karoo's own tiles, the page grey between them.
 * - Single, boundaries on: one tile, a 1 px line in the border grey from edge to edge.
 * - Boundaries off: the Karoo draws no cards, so neither do we, and no line.
 * Zone colours (power, heart rate) are the Karoo's own ([ZoneColors]). Sizes are Karoo 3 pixels.
 */
class TileRenderer(context: Context) {

    private val regular: Typeface = context.resources.getFont(R.font.plex_regular)
    /** Numbers: Plex Medium, one step heavier than the Karoo's own, which looks thin at half its size. */
    private val medium: Typeface = context.resources.getFont(R.font.plex_medium)
    private val condensed: Typeface = context.resources.getFont(R.font.plex_condensed_medium)
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.RIGHT }
    private val valuePaint = KarooType.valuePaint()
    /** Cap height of the condensed font per px of text size, to size labels by their capitals like the Karoo. */
    private val capsPerPx: Float = android.graphics.Rect().also {
        text.typeface = condensed; text.textSize = 100f; text.getTextBounds("H", 0, 1, it)
    }.height() / 100f
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }

    fun render(s: SmallerFieldsState, prefs: Prefs, width: Int, height: Int, night: Boolean, boundaries: Boolean): Bitmap {
        val w = max(width, 40).toFloat()
        val h = max(height, 40).toFloat()
        val bmp = Bitmap.createBitmap(w.toInt(), h.toInt(), Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        // Never past the Karoo's rounded tile.
        c.clipPath(Path().apply { addRoundRect(RectF(0f, 0f, w, h), RADIUS * K, RADIUS * K, Path.Direction.CW) })

        val side = prefs.layout == Layout.SIDE
        val full = RectF(0f, 0f, w, h)
        val metrics = listOf(prefs.first, prefs.second)

        if (boundaries && prefs.tiles == Tiles.SEPARATE) {
            c.drawColor(if (night) PAGE_DARK else PAGE_LIGHT)
            val gap = (if (side) GAP_SIDE else GAP_STACKED) * K
            val cards = split(full, side, gap)
            metrics.forEachIndexed { i, m ->
                val r = cards[i]
                val card = Path().apply {
                    addRoundRect(RectF(r.left + 0.5f, r.top + 0.5f, r.right - 0.5f, r.bottom - 0.5f), RADIUS * K, RADIUS * K, Path.Direction.CW)
                }
                fill.color = if (night) Color.BLACK else Color.WHITE
                c.drawPath(card, fill)
                c.save(); c.clipPath(card)
                half(c, r, m, s, prefs.colours(i), night)
                c.restore()
                line.color = BORDER; line.strokeWidth = K
                c.drawPath(card, line)
            }
            return bmp
        }

        val halves = split(full, side, 0f)
        val coloured = metrics.mapIndexed { i, m -> half(c, halves[i], m, s, prefs.colours(i), night) }
        if (boundaries && prefs.tiles == Tiles.SINGLE && coloured.none { it }) {
            fill.color = BORDER
            if (side) c.drawRect(w / 2 - K / 2, 0f, w / 2 + K / 2, h, fill)
            else c.drawRect(0f, h / 2 - K / 2, w, h / 2 + K / 2, fill)
        }
        return bmp
    }

    /**
     * A field the rider hasn't made yet (added to a ride page before it was set up in the app):
     * its name in the Karoo's capitals, then where to set it up. The Karoo draws the tile.
     */
    fun renderNotSetUp(context: Context, slot: Int, width: Int, height: Int, night: Boolean): Bitmap {
        val w = max(width, 40).toFloat()
        val h = max(height, 40).toFloat()
        val bmp = Bitmap.createBitmap(w.toInt(), h.toInt(), Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val ink = if (night) Color.WHITE else Color.BLACK
        val sub = if (night) Color.parseColor("#B4B2A9") else Color.parseColor("#6F6F6F")
        val lines = listOf(context.getString(R.string.not_set_up), context.getString(R.string.open_app))
        text.textAlign = Paint.Align.CENTER; text.textScaleX = 1f
        val title = context.getString(R.string.field_n, slot + 1).uppercase()
        val titlePx = min(26f * K, h * 0.22f)
        val linePx = min(19f * K, h * 0.14f)
        val block = titlePx + linePx * 2.6f
        var y = (h - block) / 2 + titlePx * 0.8f
        text.typeface = condensed; text.textSize = titlePx; text.color = ink
        fitWidth(title, w - 16f * K); c.drawText(title, w / 2, y, text)
        text.typeface = regular; text.color = sub
        y += linePx * 1.6f
        for (l in lines) { text.textSize = linePx; fitWidth(l, w - 16f * K); c.drawText(l, w / 2, y, text); y += linePx * 1.35f }
        text.textAlign = Paint.Align.RIGHT
        return bmp
    }

    private fun fitWidth(s: String, maxW: Float) {
        val tw = text.measureText(s)
        if (tw > maxW) text.textSize *= maxW / tw
    }

    /**
     * Relative has one weight, so smaller numbers have thinner strokes. Thicken them towards the
     * stroke width of the Karoo's own numbers on a quarter page ([REF_SIZE] px), by [weightMatch]
     * (0: Relative as is, 1: the same stroke width at every size).
     */
    private fun boost(stacked: Boolean = false) {
        val size = valuePaint.textSize
        // capped at 1.5 % of the size in every layout (we tried 3.5 / 2.5 / 1.5 %)
        val cap = 0.015f
        val extra = (STROKE_RATIO * (REF_SIZE * K - size) * weightMatch).coerceIn(0f, size * cap)
        if (extra > 0.2f) {
            valuePaint.style = Paint.Style.FILL_AND_STROKE; valuePaint.strokeWidth = extra
            // keep the Karoo's gaps between digits: open the spacing by what the stroke adds
            valuePaint.letterSpacing = KarooType.VALUE_LETTER_SPACING + extra / size
        } else {
            valuePaint.style = Paint.Style.FILL; valuePaint.strokeWidth = 0f
            valuePaint.letterSpacing = KarooType.VALUE_LETTER_SPACING
        }
    }

    /**
     * A stacked half too short for a header above its number (a small or row tile split in two):
     * icon and label on the left, on one line, at the Karoo's header size; the number fills the
     * half's height on the right.
     */
    private fun shortHalf(c: Canvas, r: RectF, m: Metric, s: SmallerFieldsState, col: Colors, night: Boolean): Boolean {
        val capsPx = 20f * K
        // The digits' ink ends 12 px above the half's bottom, like the Karoo's own values
        // (KarooType.valueBaseline: baseline 14 px up), with the same 12 px gap above them.
        val digitBottom = r.bottom - 12f * K
        val digitH = r.height() - 24f * K
        val baseline = digitBottom - digitH / 2f + capsPx / 2f      // label centred on the digits
        val boxW = capsPx * 25f / 23f
        drawIcon(c, m.icon, r.left + 12f * K, baseline, boxW, iconColor(m.icon, col, night))
        var labelEnd = r.left + 12f * K + boxW
        val label = m.label(s.units)
        if (label.isNotEmpty()) {
            text.typeface = condensed; text.textScaleX = 1f; text.color = col.ink
            text.textSize = capsPx / capsPerPx; text.textAlign = Paint.Align.LEFT
            c.drawText(label, labelEnd + 6f * K, baseline, text)
            labelEnd += 6f * K + text.measureText(label)
            text.textAlign = Paint.Align.RIGHT
        }
        val margin = KarooType.VALUE_MARGIN_PX * K
        if (noSensor(m, s)) {
            // Too short for the Karoo's empty-state position: the empty text where the number goes,
            // at the label's 20 px caps (25 looked too big here), right-aligned, centred on the digits
            val p = emptyText(r.right - margin - labelEnd - 12f * K, col, capsPx = 20)
            p.textAlign = Paint.Align.RIGHT
            c.drawText(EMPTY, r.right - margin, digitBottom - digitH / 2f + emptyCaps(p) / 2f, p)
            return col.bg != null
        }
        val value = KarooType.valueText(m.text(s.value(m), s.units))
        valuePaint.color = col.number
        valuePaint.style = Paint.Style.FILL
        valuePaint.textSize = digitSize(sizeBy(value, m), (r.right - margin - labelEnd - 12f * K).toInt(), digitH)
        boost(stacked = true)
        val ink = android.graphics.Rect().also { valuePaint.getTextBounds("8", 0, 1, it) }
        c.drawText(value, r.right - margin - valuePaint.strokeWidth / 2, digitBottom - ink.bottom, valuePaint)
        return col.bg != null
    }

    internal fun noSensor(m: Metric, s: SmallerFieldsState) = m.sensor && s.value(m) == null

    /** The empty-state paint: KarooType's (label font, 25 px caps, centred), shrunk to [room] px. */
    private fun emptyText(room: Float, col: Colors, capsPx: Int = KarooType.EMPTY_CAPS_PX): Paint = KarooType.emptyPaint(EMPTY, 10_000).apply {
        typeface = condensed
        textSize = capsPx * K / capsPerPx
        val w = measureText(EMPTY)
        if (w > room) textSize *= room / w
        color = col.ink
        // plain, without KarooType's label outline: measured on the Karoo, our plain labels already
        // match the native weight (stems 3.19–3.30 px vs native 3.25–3.31 at 20 px caps)
        style = Paint.Style.FILL
    }

    private fun emptyCaps(p: Paint) = android.graphics.Rect().also { p.getTextBounds("N", 0, 1, it) }.height().toFloat()

    /** What to size [value] by: itself, or (steady sizing) the wider of it and the field's [Metric.steady]. */
    private fun sizeBy(value: String, m: Metric): String {
        if (!steadySize) return value
        val live = value.map { if (it.isDigit()) '8' else it }.joinToString("")
        valuePaint.textSize = 100f
        return if (valuePaint.measureText(live) > valuePaint.measureText(m.steady)) live else m.steady
    }

    /** Largest value size whose digits ("8" for every digit) fit [w] wide and [h] tall. */
    private fun digitSize(value: String, w: Int, h: Float): Float {
        val probe = value.map { if (it.isDigit()) '8' else it }.joinToString("")
        val b = android.graphics.Rect()
        valuePaint.textSize = 100f
        valuePaint.getTextBounds("8", 0, 1, b)
        val byHeight = 100f * h / b.height()
        valuePaint.textSize = byHeight
        val tw = valuePaint.measureText(probe)
        return if (tw > w) byHeight * w / tw else byHeight
    }

    private fun split(r: RectF, side: Boolean, gap: Float): List<RectF> =
        if (side) {
            val cw = (r.width() - gap) / 2
            listOf(RectF(r.left, r.top, r.left + cw, r.bottom), RectF(r.right - cw, r.top, r.right, r.bottom))
        } else {
            val ch = (r.height() - gap) / 2
            listOf(RectF(r.left, r.top, r.right, r.top + ch), RectF(r.left, r.bottom - ch, r.right, r.bottom))
        }

    internal class Colors(val bg: Int?, val ink: Int, val number: Int)

    internal fun colors(m: Metric, s: SmallerFieldsState, own: Colours, night: Boolean): Colors {
        val ink = if (night) Color.WHITE else Color.BLACK
        val zone = s.zoneOf(m)
        val count = s.zoneCount(m)
        val strong = zone?.let { ZoneColors.strong(it, count) } ?: return Colors(null, ink, ink)
        val zoneNumber = own.number == NumberColor.ZONE
        return when (own.background) {
            // The Karoo's own zone fill: black text on it in both modes; a zone number is a dark shade of the zone.
            Background.KAROO -> Colors(strong, Color.BLACK, if (zoneNumber) ZoneColors.mix(strong, Color.BLACK, 0.42f) else Color.BLACK)
            Background.SOFT -> Colors(
                ZoneColors.mix(strong, if (night) Color.BLACK else Color.WHITE, if (night) 0.30f else 0.22f),
                ink,
                if (zoneNumber) ZoneColors.number(zone, count, night) ?: ink else ink,
            )
            Background.NONE -> Colors(null, ink, if (zoneNumber) ZoneColors.number(zone, count, night) ?: ink else ink)
        }
    }

    /** One data in [r]. Returns whether it painted a zone background. */
    private fun half(c: Canvas, r: RectF, m: Metric, s: SmallerFieldsState, own: Colours, night: Boolean): Boolean {
        val col = colors(m, s, own, night)
        col.bg?.let { fill.color = it; c.drawRect(r, fill) }
        val stacked = r.width() > r.height() * 1.8f
        if (stacked && r.height() / K < SHORT_HALF) return shortHalf(c, r, m, s, col, night)

        // Header 1:1 like the Karoo's own field header for a tile of this size (measured on our test
        // Karoo; the same rules as BigWatts and ClearWind, playbook "Writing data fields"): capitals
        // 20 px in tiles under 150 px, else 23 px; their top 21 px from the tile's top at 142 px and
        // 26 px from 168 px up; text ends 9 px from the right; icon in a 25 px box 12 px from the
        // left, ending on the baseline (box and icon scale with the capitals).
        val hK = r.height() / K
        val capsPx = (if (hK < 150f) 20f else 23f) * K
        val capsTop = (21f + (hK - 142f) * 5f / 26f).coerceIn(18f, 26f).roundToInt() * K
        val baseline = r.top + capsTop + capsPx
        val boxW = capsPx * 25f / 23f
        val iconX = r.left + 12f * K
        val iconW = boxW
        drawIcon(c, m.icon, iconX, baseline, boxW, iconColor(m.icon, col, night))
        val label = m.label(s.units)
        if (label.isNotEmpty()) {
            text.typeface = condensed; text.textScaleX = 1f; text.color = col.ink
            text.textSize = capsPx / capsPerPx
            val right = r.right - 7f * K                 // drawn at 7 px: the glyphs end 9 px from the edge
            val room = right - (iconX + max(iconW, boxW) + 4f * K)
            val lw = text.measureText(label)
            if (lw > room) text.textSize *= room / lw
            c.drawText(label, right, baseline, text)
        }

        if (noSensor(m, s)) {
            // The Karoo's empty state (KarooType README "Empty states"): label font at 25 px caps,
            // centred, ink bottom 39 px above the bottom, shrunk to fit
            val p = emptyText(r.width() - 2 * KarooType.VALUE_MARGIN_PX * K, col)
            c.drawText(EMPTY, r.centerX(), r.top + KarooType.emptyBaseline((r.height() / K).toInt()) * K, p)
            return col.bg != null
        }

        // Number: like the Karoo's own values (KarooType): its font Relative, letterSpacing -0.04,
        // auto-sized to fill the space under the header, right-aligned with its margin.
        val value = KarooType.valueText(m.text(s.value(m), s.units))
        val margin = KarooType.VALUE_MARGIN_PX * K
        val areaTop = baseline + 4f * K
        val areaBottom = r.bottom - margin
        valuePaint.color = col.number
        val areaW = (r.width() - 2 * margin).toInt()
        valuePaint.style = Paint.Style.FILL
        val tw = (r.width() / K).toInt(); val th = (r.height() / K).toInt()
        val nativeSize = KarooType.valueSize(valuePaint, sizeBy(value, m), KarooType.valueW(tw), KarooType.valueLineH(th)) * K
        val fitSize = if (stacked) digitSize(sizeBy(value, m), areaW, areaBottom - areaTop) else 0f
        if (stacked && fitSize <= nativeSize) {
            // A half-height field has little room under the header, and the Karoo's line-height fit
            // would make its number small: fit the digits themselves (their height) instead, but
            // never bigger than a native field of the half's size (docs/design/field-size).
            valuePaint.textSize = fitSize
            boost(stacked = true)
            c.drawText(value, r.right - margin - valuePaint.strokeWidth / 2, areaBottom - valuePaint.strokeWidth / 2, valuePaint)
        } else {
            // Calibrated against a native tile (KarooType, from BetterIntervals): line area tileH - 35,
            // baseline 14 px above the bottom, width tileW - 2 x margin
            valuePaint.textSize = nativeSize
            boost()
            c.drawText(value, r.right - margin - valuePaint.strokeWidth / 2, r.top + KarooType.valueBaseline(th) * K, valuePaint)
        }
        return col.bg != null
    }

    /**
     * A Karoo icon (38-unit viewport, glyph from about 4 to 34) whose glyph fills a [box] px square
     * at [x], its bottom on [baseline], like the icons in the Karoo's own field headers.
     */
    private fun drawIcon(c: Canvas, icon: Icon, x: Float, baseline: Float, box: Float, color: Int) {
        val k = box / 30f
        val p = Path(icon.path).apply { transform(Matrix().apply { setScale(k, k); postTranslate(x - 4f * k, baseline - 34f * k) }) }
        fill.color = color
        c.drawPath(p, fill)
    }

    /** The Karoo's green for speed and distance icons, the text colour for the rest (and on a zone fill). */
    internal fun iconColor(icon: Icon, col: Colors, night: Boolean): Int =
        if (icon.green && col.bg == null) (if (night) KAROO_GREEN_NIGHT else KAROO_GREEN) else col.ink

    companion object {
        /** The Karoo's own words for a sensor field without a value. */
        internal const val EMPTY = "No Sensor"
        /** Size numbers by the widest value the field normally shows, so they don't jump (our pick). */
        @Volatile var steadySize = true
        /** The Karoo's green on speed and distance icons (sampled from its own fields). */
        private val KAROO_GREEN = Color.parseColor("#00A772")
        private val KAROO_GREEN_NIGHT = Color.parseColor("#33E2AA")
        /** How much smaller numbers are thickened (see boost); the gallery tries several. */
        @Volatile var weightMatch = 0.5f     // picked: A, half way (design/relative-values)
        /** Stacked halves shorter than this (px) put the header beside the number. */
        internal const val SHORT_HALF = 100f
        /** Relative's stroke width per px of text size, and the Karoo's quarter-page value size (px). */
        private const val STROKE_RATIO = 0.07f
        private const val REF_SIZE = 110f
        /** Karoo 3 pixels → this screen (1 on a Karoo 3). */
        internal val K = android.content.res.Resources.getSystem().displayMetrics.density / 1.875f
        private const val RADIUS = 14f
        internal const val GAP_SIDE = 2f
        internal const val GAP_STACKED = 6f
        private const val NUMBER_SCALE_X = 0.82f
        private const val CAP_HEIGHT = 0.698f      // IBM Plex: digits are cap height
        private val BORDER = Color.parseColor("#7D7D7D")
        /** The Karoo's page grey with data field boundaries on (measured on the Karoo 3). */
        internal val PAGE_LIGHT = Color.parseColor("#DDDDDD")
        internal val PAGE_DARK = Color.parseColor("#484848")
    }
}
