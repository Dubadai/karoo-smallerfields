package com.daviddokakis.smallerfields

/** Everything the fields show. Rendering only reads this. */
data class SmallerFieldsState(
    val values: Map<Metric, Double?> = emptyMap(),
    val powerZones: List<IntRange> = emptyList(),
    val hrZones: List<IntRange> = emptyList(),
    val units: Units = Units(),
) {
    fun value(m: Metric): Double? = values[m]

    /** 0-based zone of [m]'s value in the rider's zones, or null (no zones, no value, not a zone field). */
    fun zoneOf(m: Metric): Int? {
        val zones = zonesFor(m) ?: return null
        val v = value(m)?.toInt() ?: return null
        if (zones.isEmpty()) return null
        val i = zones.indexOfFirst { v in it }
        return when {
            i >= 0 -> i
            v > zones.last().last -> zones.size - 1
            else -> 0
        }
    }

    fun zoneCount(m: Metric): Int = zonesFor(m)?.size ?: 0

    private fun zonesFor(m: Metric) = when (m.zone) {
        ZoneKind.POWER -> powerZones
        ZoneKind.HR -> hrZones
        null -> null
    }
}
