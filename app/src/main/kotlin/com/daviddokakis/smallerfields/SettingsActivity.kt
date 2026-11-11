package com.daviddokakis.smallerfields

import com.dubadai.karoo.common.*
import android.app.Activity
import android.app.AlertDialog
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/**
 * The SmallerFields app, opened from the Karoo's app list (design: docs/design/add-field/):
 * - Your fields: every field made (Field 1–10, what the Karoo lists), or an empty state.
 * - Add field: 4 steps (what it shows, layout, tiles, colours), then "Field N · Remember the name".
 * - Edit: everything on one page, plus Delete (frees the number).
 * One activity; each screen is built in code, and Back goes to the screen before.
 */
class SettingsActivity : Activity() {

    private sealed class Screen {
        object List : Screen()
        data class Add(val slot: Int, val step: Int, val draft: Prefs) : Screen()
        data class Done(val slot: Int) : Screen()
        data class Edit(val slot: Int) : Screen()
    }

    private var screen: Screen = Screen.List
    /** Which data the colour controls set (0: first, 1: second); its half has a ring in the Example. */
    private var colourData = 0
    private val renderer by lazy { TileRenderer(this) }
    private val night get() = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

    override fun onResume() {
        super.onResume()
        show(screen)
    }

    @Deprecated("Activity back handling")
    override fun onBackPressed() {
        when (val s = screen) {
            is Screen.Add -> if (s.step > 1) show(s.copy(step = s.step - 1)) else show(Screen.List)
            is Screen.Done, is Screen.Edit -> show(Screen.List)
            Screen.List -> @Suppress("DEPRECATION") super.onBackPressed()
        }
    }

    /** The current page's scroll view, so a redraw of the same page keeps where you were. */
    private var scroller: ScrollView? = null

    private fun show(s: Screen) {
        // Same page redrawn (a setting changed): keep the scroll position instead of jumping to the top
        val same = when {
            s is Screen.Edit && screen is Screen.Edit -> s.slot == (screen as Screen.Edit).slot
            s is Screen.Add && screen is Screen.Add -> s.step == (screen as Screen.Add).step
            else -> false
        }
        val keepY = if (same) scroller?.scrollY ?: 0 else 0
        screen = s
        when (s) {
            Screen.List -> { colourData = 0; listScreen() }
            is Screen.Add -> addScreen(s)
            is Screen.Done -> doneScreen(s.slot)
            is Screen.Edit -> editScreen(s.slot)
        }
        if (keepY > 0) scroller?.let { v -> v.post { v.scrollTo(0, keepY) } }
    }

    // ---- Your fields ----

    private fun listScreen() {
        val made = Settings.made(this)
        val form = form()
        if (made.isEmpty()) {
            form.addView(ImageView(this).apply {
                setImageBitmap(emptyIllustration())
                adjustViewBounds = true
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }, LinearLayout.LayoutParams(dp(190), WRAP_CONTENT).apply { gravity = Gravity.CENTER_HORIZONTAL; topMargin = dp(4) })
            form.addView(text(getString(R.string.empty_title), 20f, R.color.settings_title, bold = true, center = true).apply { setPadding(0, dp(12), 0, dp(6)) })
            form.addView(text(getString(R.string.empty_text), 14f, R.color.settings_help, center = true).apply { setPadding(0, 0, 0, dp(8)) })
        } else {
            form.addView(settingsHeading(R.drawable.ic_settings_fields, getString(R.string.your_fields)))
            made.forEach { slot ->
                val prefs = Settings.read(this, slot)
                val picture = ImageView(this).apply {
                    setImageBitmap(scaled(tile(prefs), dp(84), dp(59)))
                    importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                }
                form.addView(settingsListCard(
                    picture,
                    getString(R.string.field_n, slot + 1),
                    listOf(getString(prefs.first.nameRes), getString(prefs.second.nameRes)),
                ) { show(Screen.Edit(slot)) })
            }
        }
        val free = Settings.nextFree(this)
        if (free != null) form.addView(settingsButton(SettingsButton.ADD, getString(R.string.add_field)) { show(Screen.Add(free, 1, Settings.NEW)) })
        else form.addView(settingsHelp(getString(R.string.all_used)))
        form.addView(settingsDivider())
        settingsAppRows(form, repo = "karoo-smallerfields")
        settingsPage(getString(R.string.app_short_name), null, form) { finish() }
    }

    // ---- Add field ----

    private fun addScreen(s: Screen.Add) {
        val body = column()
        body.addView(stepBar(s.step))
        fun update(p: Prefs) = show(s.copy(draft = p))
        body.addView(View(this).apply { contentDescription = getString(R.string.step_n, s.step) }, LinearLayout.LayoutParams(MATCH_PARENT, dp(8)))
        when (s.step) {
            1 -> {
                body.addView(stepTitle(R.string.step_data))
                body.addView(example(s.draft))
                body.addView(picker(R.drawable.ic_settings_first, R.string.setting_first, s.draft.first) { update(s.draft.copy(first = it)) })
                body.addView(picker(R.drawable.ic_settings_second, R.string.setting_second, s.draft.second) { update(s.draft.copy(second = it)) })
            }
            2 -> {
                body.addView(stepTitle(R.string.step_layout))
                body.addView(choices(
                    listOf(getString(R.string.layout_side) to Layout.SIDE, getString(R.string.layout_stacked) to Layout.STACKED),
                    s.draft.layout, { s.draft.copy(layout = it) },
                ) { update(s.draft.copy(layout = it)) })
                body.addView(settingsHelp(getString(R.string.layout_help)))
            }
            3 -> {
                body.addView(stepTitle(R.string.step_tiles))
                body.addView(choices(
                    listOf(getString(R.string.tiles_separate) to Tiles.SEPARATE, getString(R.string.tiles_single) to Tiles.SINGLE),
                    s.draft.tiles, { s.draft.copy(tiles = it) },
                ) { update(s.draft.copy(tiles = it)) })
                body.addView(settingsHelp(getString(R.string.tiles_help)))
            }
            else -> {
                body.addView(stepTitle(R.string.step_colours).apply { setPadding(0, 0, 0, dp(6)) })
                body.addView(pickExample(s.draft, compact = true))
                colourControls(body, s.draft, compact = true) { update(it) }
            }
        }
        val last = s.step == 4
        val next = settingsButton(SettingsButton.MAIN, getString(if (last) R.string.save_field else R.string.next)) {
            if (last) { Settings.write(this, s.slot, s.draft); show(Screen.Done(s.slot)) } else show(s.copy(step = s.step + 1))
        }
        page(bar(getString(R.string.new_field), close = true) { show(Screen.List) }, body, next)
    }

    /** The name to remember, and where to pick it on the Karoo. */
    private fun doneScreen(slot: Int) {
        val n = slot + 1
        val body = column()
        body.addView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            background = rounded(color(R.color.example_card), dp(12).toFloat())
            setPadding(dp(14), dp(10), dp(14), dp(12))
            addView(text(getString(R.string.field_n, n), 28f, R.color.settings_title, bold = true, center = true))
            addView(text(getString(R.string.remember_name), 16f, R.color.settings_title, bold = true, center = true).apply { setPadding(0, dp(2), 0, dp(4)) })
            addView(text(getString(R.string.remember_text), 13f, R.color.settings_help, center = true))
        }, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = dp(8) })
        body.addView(text(getString(R.string.put_on_page), 15f, R.color.settings_title, bold = true).apply { setPadding(0, dp(12), 0, dp(2)) })
        listOf(getString(R.string.how_1), getString(R.string.how_2), getString(R.string.how_3, n)).forEachIndexed { i, line ->
            body.addView(LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dp(3), 0, dp(3))
                addView(text("${i + 1}", 12f, R.color.settings_title, bold = true, center = true).apply {
                    gravity = Gravity.CENTER
                    background = rounded(color(R.color.segment_track), dp(11).toFloat())
                }, LinearLayout.LayoutParams(dp(22), dp(22)))
                addView(text(line, 13f, R.color.settings_title, bold = i == 2).apply { setPadding(dp(10), 0, 0, 0) })
            })
        }
        page(bar(getString(R.string.new_field), close = true) { show(Screen.List) }, body,
            settingsButton(SettingsButton.MAIN, getString(R.string.done)) { show(Screen.List) })
    }

    // ---- Edit ----

    /** Kit order: the look (layout, tiles, colours), then what the field shows, then Delete. */
    private fun editScreen(slot: Int) {
        val prefs = Settings.read(this, slot)
        fun save(p: Prefs) { Settings.write(this, slot, p); show(Screen.Edit(slot)) }
        val form = form()
        form.addView(settingsHeading(R.drawable.ic_settings_layout, getString(R.string.setting_layout)))
        form.addView(settingsSegmented(listOf(getString(R.string.layout_side) to Layout.SIDE, getString(R.string.layout_stacked) to Layout.STACKED), prefs.layout) { save(prefs.copy(layout = it)) })
        form.addView(settingsHelp(getString(R.string.layout_help)))
        form.addView(settingsDivider())
        form.addView(settingsHeading(R.drawable.ic_settings_tiles, getString(R.string.setting_tiles)))
        form.addView(settingsSegmented(listOf(getString(R.string.tiles_separate) to Tiles.SEPARATE, getString(R.string.tiles_single) to Tiles.SINGLE), prefs.tiles) { save(prefs.copy(tiles = it)) })
        form.addView(settingsHelp(getString(R.string.tiles_help)))
        form.addView(settingsDivider())
        colourControls(form, prefs) { save(it) }
        form.addView(settingsDivider())
        form.addView(picker(R.drawable.ic_settings_first, R.string.setting_first, prefs.first) { save(prefs.copy(first = it)) })
        form.addView(picker(R.drawable.ic_settings_second, R.string.setting_second, prefs.second) { save(prefs.copy(second = it)) })
        form.addView(settingsDivider())
        form.addView(settingsButton(SettingsButton.REMOVE, getString(R.string.delete_field)) {
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.delete_confirm, slot + 1))
                .setPositiveButton(R.string.delete) { _, _ -> Settings.delete(this, slot); show(Screen.List) }
                .setNegativeButton(R.string.cancel, null)
                .show()
        })
        form.addView(settingsHelp(getString(R.string.delete_help, slot + 1)))
        settingsPage(getString(R.string.field_n, slot + 1), exampleCard(prefs), form) { show(Screen.List) }
    }

    /** The picked data's Number and Background (each data has its own; pick it in the Example). */
    private fun colourControls(body: LinearLayout, p: Prefs, compact: Boolean = false, onChange: (Prefs) -> Unit) {
        val name = getString((if (colourData == 0) p.first else p.second).nameRes)
        body.addView(
            if (compact) text(getString(R.string.colours_of, name), 15f, R.color.settings_title, bold = true).apply { setPadding(0, dp(4), 0, 0) }
            else settingsHeading(R.drawable.ic_settings_colours, getString(R.string.colours_of, name)),
        )
        val own = p.colours(colourData)
        val number = settingsSegmented(listOf(getString(R.string.number_plain) to NumberColor.PLAIN, getString(R.string.number_zone) to NumberColor.ZONE), own.number) {
            onChange(p.withColours(colourData, own.copy(number = it)))
        }
        val background = settingsSegmented(
            listOf(getString(R.string.background_none) to Background.NONE, getString(R.string.background_karoo) to Background.KAROO, getString(R.string.background_soft) to Background.SOFT),
            own.background,
        ) { onChange(p.withColours(colourData, own.copy(background = it))) }
        if (compact) {
            // Label and control on one line, so the colour step fits one screen
            fun row(res: Int, control: View) = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dp(6), 0, 0)
                addView(text(getString(res), 14f, R.color.settings_title), LinearLayout.LayoutParams(dp(84), WRAP_CONTENT))
                addView(control, LinearLayout.LayoutParams(0, dp(36), 1f))
            }
            body.addView(row(R.string.setting_number, number))
            body.addView(row(R.string.setting_background, background))
        } else {
            body.addView(settingsSubLabel(getString(R.string.setting_number)))
            body.addView(number)
            body.addView(settingsSubLabel(getString(R.string.setting_background)))
            body.addView(background)
            body.addView(settingsHelp("${getString(R.string.tap_hint)}. ${getString(R.string.zones_help)}"))
        }
    }

    // ---- Pictures ----

    /** The field as [p] draws it at a quarter page, with the showcase numbers. */
    private fun tile(p: Prefs, ring: Int? = null): Bitmap {
        val w = ExampleData.QUARTER_W; val h = ExampleData.QUARTER_H
        // With a ring the tile gets a margin, so the outline can sit outside the picked field
        val m = if (ring != null) RING_MARGIN else 0f
        val out = Bitmap.createBitmap((w + 2 * m).toInt(), (h + 2 * m).toInt(), Bitmap.Config.ARGB_8888)
        val c = Canvas(out)
        c.translate(m, m)
        // The Karoo's tile under the field (white / black card with its border), as on a ride page
        val card = RectF(0.5f, 0.5f, w - 0.5f, h - 0.5f)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = if (night) Color.BLACK else Color.WHITE
        c.drawRoundRect(card, 14f, 14f, paint)
        c.drawBitmap(renderer.render(ExampleData.STATE, p, w, h, night, boundaries = true), 0f, 0f, null)
        paint.style = Paint.Style.STROKE; paint.strokeWidth = 1f; paint.color = Color.parseColor("#7D7D7D")
        c.drawRoundRect(card, 14f, 14f, paint)
        if (ring != null) {
            // The picked half: an Apple-blue outline a little outside it (so it reads as selected,
            // not as its border); the other half faded, so it is clear which you colour
            val side = p.layout == Layout.SIDE
            fun halfRect(i: Int) = if (side) RectF(if (i == 0) 0f else w / 2f, 0f, if (i == 0) w / 2f else w.toFloat(), h.toFloat())
            else RectF(0f, if (i == 0) 0f else h / 2f, w.toFloat(), if (i == 0) h / 2f else h.toFloat())
            val fade = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = color(R.color.example_card); alpha = 165 }
            c.drawRect(halfRect(1 - ring), fade)
            val stroke = 7f
            val r = halfRect(ring).apply { inset(-(RING_GAP + stroke / 2), -(RING_GAP + stroke / 2)) }
            paint.strokeWidth = stroke; paint.color = color(R.color.select_blue)
            c.drawRoundRect(r, 14f + RING_GAP + stroke / 2, 14f + RING_GAP + stroke / 2, paint)
        }
        return out
    }

    /** The empty state: three made-up fields, tilted and stacked like cards, so it reads as an example. */
    private fun emptyIllustration(): Bitmap {
        val w = 436; val h = 330
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(out)
        val card = Paint(Paint.ANTI_ALIAS_FLAG)
        val edge = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 1f; color = if (night) Color.parseColor("#3A3A38") else Color.parseColor("#E4E4E0") }
        val title = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Typeface.DEFAULT_BOLD; textSize = 20f; color = color(R.color.settings_title) }
        val what = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 15f; color = color(R.color.settings_help); textAlign = Paint.Align.RIGHT }
        // (centre x, centre y, degrees, field number, made-up field): back to front
        val cards = listOf(
            listOf(188f, 236f, 2.5f, 3f, 2f), listOf(178f, 100f, -3.5f, 1f, 0f), listOf(278f, 180f, 1.5f, 2f, 1f),
        )
        val shortWhat = listOf("3s power · HR", "Speed · Lap", "Avg power · HR")
        for (k in cards) {
            val (cx, cy, rot, n, i) = k
            val p = ExampleData.EMPTY_CARDS[i.toInt()]
            c.save(); c.translate(cx, cy); c.rotate(rot)
            val r = RectF(-126f, -78f, 126f, 78f)
            card.color = if (night) Color.parseColor("#1D1D1D") else Color.WHITE
            card.setShadowLayer(22f, 0f, 7f, Color.argb(if (night) 120 else 36, 0, 0, 0))
            c.drawRoundRect(r, 20f, 20f, card)
            card.clearShadowLayer()
            c.drawRoundRect(r, 20f, 20f, edge)
            c.drawText(getString(R.string.field_n, n.toInt()), r.left + 16f, r.top + 36f, title)
            c.drawText(shortWhat[i.toInt()], r.right - 16f, r.top + 36f, what)
            val t = tile(p)
            c.drawBitmap(Bitmap.createScaledBitmap(t, (t.width * 0.48f).toInt(), (t.height * 0.48f).toInt(), true), r.left + 16f, r.top + 54f, null)
            c.restore()
        }
        return out
    }

    // ---- Building blocks ----

    /**
     * A kit page (SettingsLayout): the bar with back and "Settings", [title] big under it, the Example
     * [card] if the page has one, then [form]; all of it scrolls.
     */
    private fun settingsPage(title: String, card: ExampleCard?, form: LinearLayout, onBack: () -> Unit) {
        val layout = SettingsLayout(
            this,
            back = settingsBack(getString(R.string.back)) { onBack() },
            barTitle = getString(R.string.settings),
            title = settingsTitle(title),
            card = card,
            form = form,
            pageColor = color(R.color.settings_bg),
            textColor = color(R.color.settings_title),
        )
        scroller = layout.scroll
        setContentView(layout)
    }

    private fun form() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(12), 0, dp(12), dp(24))
    }

    /**
     * The Edit page's Example: the field with these settings, the picked data's half ringed. Tap a
     * half to pick which data the colour controls set. The picture draws its own Karoo tile (the ring
     * sits outside it), so it isn't framed.
     */
    private fun exampleCard(p: Prefs) = ExampleCard(this).apply {
        val picture = scaled(tile(p, colourData), dp(123), dp(91))
        show(picture, getString(R.string.example_description), framed = false)
        onTap { x, y ->
            val i = if (p.layout == Layout.SIDE) (if (x < picture.width / 2f) 0 else 1) else (if (y < picture.height / 2f) 0 else 1)
            if (i != colourData) { colourData = i; show(screen) }
        }
    }

    /** The Add field steps: a fixed bar, the step scrolling, its main button fixed at the bottom. */
    private fun page(bar: View, body: View, bottom: View) {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(color(R.color.settings_bg))
            addView(bar, LinearLayout.LayoutParams(MATCH_PARENT, dp(56)).apply { topMargin = dp(4) })
            addView(ScrollView(this@SettingsActivity).apply {
                isFillViewport = true
                addView(body)
                scroller = this
            }, LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f))
            addView(FrameLayout(this@SettingsActivity).apply {
                setPadding(dp(12), 0, dp(12), dp(4))
                addView(bottom)
            }, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        }
        setContentView(root)
    }

    /** The same bar as SettingsLayout's, with a close × (leaving Add field) or the back arrow. */
    private fun bar(title: String, close: Boolean, onBack: () -> Unit) = FrameLayout(this).apply {
        val icon = if (close) ImageView(this@SettingsActivity).apply {
            setImageResource(R.drawable.ic_close)
            contentDescription = getString(R.string.close)
            scaleType = ImageView.ScaleType.CENTER
            isClickable = true
            setOnClickListener { onBack() }
        } else settingsBack(getString(R.string.back)) { onBack() }
        addView(icon, FrameLayout.LayoutParams(dp(48), dp(48), Gravity.START or Gravity.CENTER_VERTICAL).apply { marginStart = dp(4) })
        addView(TextView(this@SettingsActivity).apply {
            text = title
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            includeFontPadding = false
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            setTextColor(color(R.color.settings_title))
        }, FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT, Gravity.CENTER))
    }

    private fun column() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(12), 0, dp(12), dp(16))
    }

    private fun text(s: String, sp: Float, colorRes: Int, bold: Boolean = false, center: Boolean = false) = TextView(this).apply {
        text = s
        setTextSize(TypedValue.COMPLEX_UNIT_SP, sp)
        setTextColor(color(colorRes))
        if (bold) setTypeface(typeface, Typeface.BOLD)
        if (center) {
            gravity = Gravity.CENTER_HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        }
    }

    private fun stepTitle(res: Int) = settingsTitle(getString(res)).apply { setPadding(0, dp(6), 0, dp(10)) }
    private fun scaled(b: Bitmap, w: Int, h: Int): Bitmap = Bitmap.createScaledBitmap(b, w, h, true)

    private fun stepBar(step: Int) = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        setPadding(0, dp(4), 0, 0)
        for (i in 1..4) addView(View(this@SettingsActivity).apply {
            background = rounded(color(if (i <= step) R.color.segment_selected else R.color.settings_divider), dp(2).toFloat())
        }, LinearLayout.LayoutParams(0, dp(3), 1f).apply { if (i > 1) marginStart = dp(4) })
    }

    /**
     * "Tap a field to change its colours", then the Example: tap a half to pick the data its colour
     * controls set; that half gets a blue outline and the other fades.
     */
    private fun pickExample(p: Prefs, compact: Boolean) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        addView(text(getString(R.string.tap_hint), if (compact) 12f else 13f, R.color.settings_help, center = true).apply { setPadding(0, 0, 0, dp(if (compact) 2 else 4)) })
        val card = example(p, ring = colourData, compact = compact)
        val image = card.getChildAt(0)
        image.setOnTouchListener { v, e ->
            if (e.action == android.view.MotionEvent.ACTION_UP) {
                val i = if (p.layout == Layout.SIDE) (if (e.x < v.width / 2f) 0 else 1) else (if (e.y < v.height / 2f) 0 else 1)
                if (i != colourData) { colourData = i; show(screen) }
                v.performClick()
            }
            true
        }
        addView(card)
    }

    /** The field as these settings draw it, on the Example card grey. */
    private fun example(p: Prefs, ring: Int? = null, compact: Boolean = false) = FrameLayout(this).apply {
        background = rounded(color(R.color.example_card), dp(12).toFloat())
        setPadding(0, dp(if (compact) 3 else 8), 0, dp(if (compact) 3 else 8))
        addView(ImageView(this@SettingsActivity).apply {
            setImageBitmap(tile(p, ring))
            contentDescription = getString(R.string.example_description)
        }, when {
            // With a ring the picture has a margin for the outline: the same tile size, a bigger box
            ring != null && compact -> FrameLayout.LayoutParams(dp(80), dp(59), Gravity.CENTER)
            ring != null -> FrameLayout.LayoutParams(dp(123), dp(91), Gravity.CENTER)
            compact -> FrameLayout.LayoutParams(dp(72), dp(51), Gravity.CENTER)
            else -> FrameLayout.LayoutParams(dp(110), dp(78), Gravity.CENTER)
        })
        layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { bottomMargin = dp(6) }
    }

    /** Two big choices side by side, each drawn as the tile it gives; the picked one has a ring. */
    private fun <T> choices(options: List<Pair<String, T>>, selected: T, preview: (T) -> Prefs, onPick: (T) -> Unit) = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        options.forEachIndexed { i, (label, value) ->
            val picked = value == selected
            // Same selector as the Example's picked field: an Apple-blue outline a little outside
            // the card, with a gap (the space is kept when not picked, so nothing moves).
            val ring = FrameLayout(this@SettingsActivity).apply {
                setPadding(dp(5), dp(5), dp(5), dp(5))
                if (picked) background = GradientDrawable().apply {
                    cornerRadius = dp(16).toFloat(); setStroke(dp(3), color(R.color.select_blue))
                }
                isClickable = true
                isSelected = picked
                setOnClickListener { if (!picked) onPick(value) }
            }
            ring.addView(LinearLayout(this@SettingsActivity).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
                setPadding(dp(8), dp(10), dp(8), dp(10))
                background = rounded(color(R.color.example_card), dp(12).toFloat())
                addView(ImageView(this@SettingsActivity).apply {
                    setImageBitmap(tile(preview(value)))
                    importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                }, LinearLayout.LayoutParams(dp(92), dp(65)))
                addView(text(label, 14f, R.color.settings_title, bold = picked, center = true).apply { setPadding(0, dp(8), 0, 0) })
            })
            addView(ring, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply { if (i > 0) marginStart = dp(2) })
        }
    }

    /** "First" / "Second" with the chosen data under it; tapping it opens the list of data. */
    private fun picker(icon: Int, title: Int, value: Metric, onPick: (Metric) -> Unit) =
        settingsNavRow(icon, getString(title), getString(value.nameRes)) {
            val all = Metric.entries
            AlertDialog.Builder(this)
                .setTitle(R.string.pick_data)
                .setSingleChoiceItems(all.map { getString(it.nameRes) }.toTypedArray(), all.indexOf(value)) { d, i ->
                    d.dismiss(); onPick(all[i])
                }
                .show()
        }

    private fun rounded(fill: Int, radius: Float) = GradientDrawable().apply { setColor(fill); cornerRadius = radius }
    private fun color(res: Int) = getColor(res)
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private companion object {
        /** Space between the picked field and its outline, and the room around the tile for it (tile px). */
        const val RING_GAP = 4f
        const val RING_MARGIN = 14f
    }
}
