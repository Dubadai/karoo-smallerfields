package com.daviddokakis.smallerfields

/**
 * The showcase numbers (design-assets/tools/field-gallery/showcase.json) for the Settings
 * previews and the field gallery: 3s power 280 W is zone 4 at FTP 280, heart rate 132 bpm zone 2.
 */
object ExampleData {
    val STATE = SmallerFieldsState(
        values = mapOf(
            Metric.POWER to 280.0, Metric.POWER_3S to 280.0, Metric.POWER_10S to 275.0,
            Metric.POWER_AVG to 228.0, Metric.POWER_NP to 251.0, Metric.POWER_LAP to 282.0,
            Metric.HR to 132.0, Metric.HR_AVG to 128.0, Metric.HR_LAP to 136.0,
            Metric.CADENCE to 88.0, Metric.SPEED to 28.4 / 3.6, Metric.SPEED_AVG to 29.1 / 3.6,
            Metric.DISTANCE to 24_600.0, Metric.RIDE_TIME to 4_360_000.0, Metric.LAP_TIME to 261_000.0,
            Metric.ASCENT to 312.0, Metric.GRADE to 3.5,
        ),
        powerZones = listOf(0..154, 155..210, 211..252, 253..294, 295..336, 337..420, 421..2000),
        hrZones = listOf(0..113, 114..142, 143..161, 162..175, 176..220),
    )

    /** The made-up fields in the empty state's illustration. */
    val EMPTY_CARDS = listOf(
        Prefs(Metric.POWER_3S, Metric.HR, colours1 = Colours(background = Background.KAROO), colours2 = Colours(NumberColor.ZONE)),
        Prefs(Metric.SPEED, Metric.LAP_TIME),
        Prefs(Metric.POWER_AVG, Metric.HR_AVG, layout = Layout.STACKED),
    )

    /** A quarter-page field on the Karoo 3 (pixels): the size every preview is drawn at. */
    const val QUARTER_W = 238
    const val QUARTER_H = 168
}
