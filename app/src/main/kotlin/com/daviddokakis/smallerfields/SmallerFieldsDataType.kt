package com.daviddokakis.smallerfields

import com.dubadai.karoo.common.*
import android.content.Context
import android.content.res.Configuration
import android.os.SystemClock
import android.widget.RemoteViews
import io.hammerhead.karooext.extension.DataTypeImpl
import io.hammerhead.karooext.internal.Emitter
import io.hammerhead.karooext.internal.ViewEmitter
import io.hammerhead.karooext.models.DataPoint
import io.hammerhead.karooext.models.DataType
import io.hammerhead.karooext.models.StreamState
import io.hammerhead.karooext.models.UpdateGraphicConfig
import io.hammerhead.karooext.models.ViewConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Field [slot] + 1 (0-based slot): two data in one tile, any size, built as a layout (FieldViews). A field
 * not made in the app yet shows "Not set up yet".
 */
class SmallerFieldsDataType(
    extension: String,
    private val slot: Int,
    private val session: SmallerFieldsSession,
) : DataTypeImpl(extension, typeId(slot)) {

    /**
     * Numeric fallback for places that show the field as a plain number: its first data.
     * Always Streaming: any other state makes the Karoo cover the whole tile with its own
     * "UNKNOWN / No Sensor", hiding the other half. Empty halves draw their own "No Sensor".
     */
    override fun startStream(emitter: Emitter<StreamState>) {
        val job = CoroutineScope(Dispatchers.Default).launch {
            // The Karoo logs and drops every empty point ("Filtering invalid datapoint"): send one
            // to stay Streaming, then none until a value comes.
            var sentEmpty = false
            session.state.collect { s ->
                val v = if (made) s.value(firstMetric) else null
                if (v == null && sentEmpty) return@collect
                sentEmpty = v == null
                emitter.onNext(
                    StreamState.Streaming(
                        DataPoint(dataTypeId, values = if (v == null) emptyMap() else mapOf(DataType.Field.SINGLE to v)),
                    ),
                )
            }
        }
        emitter.setCancellable { job.cancel() }
    }

    @Volatile private var firstMetric: Metric = Metric.POWER_3S
    @Volatile private var made = false

    override fun startView(context: Context, config: ViewConfig, emitter: ViewEmitter) {
        Timber.d("View started (slot ${slot + 1}): $config")
        emitter.onNext(UpdateGraphicConfig(showHeader = false))
        val (w, h) = config.viewSize
        val renderer = TileRenderer(context)
        if (config.preview) {
            // The page editor's preview can keep running after it closes and its updates land in
            // the live tile: draw one still picture with example values, start no loop.
            val prefs = Settings.read(context, slot)
            emitter.updateView(views(context, renderer, ExampleData.STATE, prefs, made = true, w, h, isNight(context), config.boundariesEnabled))
            return
        }
        val job = CoroutineScope(Dispatchers.Default).launch {
            // The Karoo drops updates closer than ~0.9 s apart: redraw as soon as allowed when
            // something visible changes, and every few seconds anyway.
            var lastRender = 0L
            var shown: Key? = null
            var wasOnScreen = true
            // Sleep until the data or the page on screen changes (karoo-common FieldPacer), no polling
            val pace = FieldPacer(listOf(session.state, session.activePage))
            val pagePace = FieldPacer(listOf(session.activePage))
            while (isActive) {
                pace.mark()
                pagePace.mark()
                val onScreen = onScreen(config)
                if (onScreen != wasOnScreen) { wasOnScreen = onScreen; if (onScreen) shown = null }
                if (!onScreen) { pagePace.sleep(FieldPacer.CAP_MS); continue }
                val s = session.state.value
                val prefs = Settings.read(context, slot)
                made = Settings.exists(context, slot)
                firstMetric = prefs.first
                val night = isNight(context)
                val key = Key(
                    prefs.first.text(s.value(prefs.first), s.units), prefs.second.text(s.value(prefs.second), s.units),
                    s.zoneOf(prefs.first), s.zoneOf(prefs.second), s.units, prefs, night, made,
                )
                val since = SystemClock.elapsedRealtime() - lastRender
                if ((key != shown && since >= MIN_GAP_MS) || since >= IDLE_REDRAW_MS) {
                    try {
                        emitter.updateView(views(context, renderer, s, prefs, made, w, h, night, config.boundariesEnabled))
                        shown = key
                    } catch (e: Exception) {
                        Timber.e(e, "Render failed")
                    }
                    lastRender = SystemClock.elapsedRealtime()
                }
                pace.sleep(FieldPacer.nextMs(key != shown, SystemClock.elapsedRealtime() - lastRender, MIN_GAP_MS, IDLE_REDRAW_MS))
            }
        }
        emitter.setCancellable { job.cancel() }
    }

    /**
     * The tile: a layout the Karoo lays out at the height the tile really has (a map page with a
     * route makes rows shorter without telling us), so the header stays the Karoo's own size and
     * the number fits the room left. Before Android 12 (not a Karoo 3): the drawn picture.
     */
    private fun views(context: Context, renderer: TileRenderer, s: SmallerFieldsState, prefs: Prefs, made: Boolean, w: Int, h: Int, night: Boolean, boundaries: Boolean): RemoteViews {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val fv = FieldViews(context)
            return if (made) fv.build(s, prefs, w, h, night, boundaries) else fv.notSetUp(slot, w, h, night)
        }
        return RemoteViews(context.packageName, R.layout.field_main).apply {
            setImageViewBitmap(R.id.field, if (made) renderer.render(s, prefs, w, h, night, boundaries) else renderer.renderNotSetUp(context, slot, w, h, night))
        }
    }

    /** Draw only while the page on screen shows this field at this view's size (true until the Karoo says). */
    private fun onScreen(config: ViewConfig): Boolean {
        val page = session.activePage.value ?: return true
        return KarooType.onPage(
            page.elements.map { it.gridSize },
            page.elements.filter { it.dataTypeId == dataTypeId }.map { it.gridSize },
            config.gridSize,
        )
    }

    private fun isNight(context: Context) =
        (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

    /** What triggers a redraw when it changes. */
    private data class Key(
        val a: String, val b: String, val za: Int?, val zb: Int?, val units: Units, val prefs: Prefs, val night: Boolean,
        val made: Boolean,
    )

    companion object {
        fun typeId(slot: Int) = "smallerfields-${slot + 1}"
        private const val MIN_GAP_MS = 920L
        private const val IDLE_REDRAW_MS = 5_000L
    }
}
