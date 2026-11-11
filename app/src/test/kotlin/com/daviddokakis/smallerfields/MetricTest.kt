package com.daviddokakis.smallerfields

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MetricTest {
    private val metric = Units()
    private val imperial = Units(imperial = true, imperialElevation = true)

    @Test fun `no value shows two dashes`() {
        assertEquals("--", Metric.POWER.text(null, metric))
        assertEquals("--", Metric.POWER.text(Double.NaN, metric))
    }

    @Test fun `whole numbers round`() {
        assertEquals("250", Metric.POWER.text(249.6, metric))
        assertEquals("142", Metric.HR.text(142.2, metric))
    }

    @Test fun `speed comes in metres per second`() {
        assertEquals("36.0", Metric.SPEED.text(10.0, metric))
        assertEquals("22.4", Metric.SPEED.text(10.0, imperial))
    }

    @Test fun `distance comes in metres`() {
        assertEquals("12.3", Metric.DISTANCE.text(12_345.0, metric))
        assertEquals("7.7", Metric.DISTANCE.text(12_345.0, imperial))
    }

    @Test fun `ascent follows the elevation unit`() {
        assertEquals("100", Metric.ASCENT.text(100.0, metric))
        assertEquals("328", Metric.ASCENT.text(100.0, imperial))
    }

    @Test fun `times come in milliseconds`() {
        assertEquals("1:05", Metric.LAP_TIME.text(65_000.0, metric))
        assertEquals("1:02:05", Metric.RIDE_TIME.text(3_725_000.0, metric))
        assertEquals("0:00", Metric.RIDE_TIME.text(-5_000.0, metric))
    }

    @Test fun `grade keeps one decimal`() {
        assertEquals("5.2", Metric.GRADE.text(5.24, metric))
        assertEquals("-3.0", Metric.GRADE.text(-3.0, metric))
    }

    @Test fun `speed distance and ascent show their unit as the label`() {
        assertEquals("KM/H", Metric.SPEED.label(metric))
        assertEquals("MPH", Metric.SPEED.label(imperial))
        assertEquals("AVG", Metric.SPEED_AVG.label(imperial))
        assertEquals("MI", Metric.DISTANCE.label(imperial))
        assertEquals("FT", Metric.ASCENT.label(imperial))
        assertEquals("W", Metric.POWER.label(imperial))
    }

    @Test fun `zones come from the rider profile`() {
        val s = SmallerFieldsState(
            values = mapOf(Metric.POWER to 175.0, Metric.HR to 190.0, Metric.CADENCE to 90.0),
            powerZones = listOf(0..149, 150..199, 200..249),
            hrZones = listOf(0..120, 121..150, 151..170),
        )
        assertEquals(1, s.zoneOf(Metric.POWER))
        assertEquals(2, s.zoneOf(Metric.HR))         // above the top zone: the top zone
        assertNull(s.zoneOf(Metric.CADENCE))         // not a zone field
        assertNull(s.zoneOf(Metric.POWER_3S))        // no value
        assertEquals(3, s.zoneCount(Metric.POWER))
    }

    @Test fun `zone colours follow the Karoo for each zone count`() {
        // 7 zones use all seven colours in order; 5 zones skip the 5th and 7th
        assertEquals(ZoneColors.strong(0, 7), ZoneColors.strong(0, 5))
        assertEquals(ZoneColors.strong(5, 7), ZoneColors.strong(4, 5))
        assertNull(ZoneColors.strong(5, 5))
    }
}
