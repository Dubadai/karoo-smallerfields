package com.daviddokakis.smallerfields

import com.dubadai.karoo.common.*
import android.graphics.Path
import androidx.core.graphics.PathParser

/**
 * The Karoo's own field icons (KarooType.Glyph, shared by every app), from its ride app (drawables ic_power, ic_heart, ic_speed, ...;
 * 38 x 38 viewport, filled). Drawn in the text colour; speed and distance in the Karoo's green,
 * as on its own fields. The glyphs sit inside the viewport roughly from 4 to 34.
 */
enum class Icon(pathData: String, val green: Boolean = false) {
    BOLT(KarooType.Glyph.BOLT),
    HEART(KarooType.Glyph.HEART),
    SPEED(KarooType.Glyph.SPEED, green = true),
    CADENCE(KarooType.Glyph.CADENCE),
    DISTANCE(KarooType.Glyph.DISTANCE, green = true),
    CLOCK(KarooType.Glyph.CLOCK),
    LAP(KarooType.Glyph.LAP),
    ASCENT(KarooType.Glyph.ASCENT),
    GRADE(KarooType.Glyph.GRADE),
    ;

    val path: Path = PathParser.createPathFromPathData(pathData)
}
