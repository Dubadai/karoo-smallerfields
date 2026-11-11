package com.daviddokakis.smallerfields

import com.dubadai.karoo.common.*
import io.hammerhead.karooext.KarooSystemService
import io.hammerhead.karooext.models.ActiveRidePage
import io.hammerhead.karooext.models.RideProfile
import io.hammerhead.karooext.models.UserProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Live data from the Karoo. One instance, owned by [SmallerFieldsExtension]. Every metric in
 * [Metric] streams all the time, so a change in Settings shows at once.
 */
class SmallerFieldsSession(private val karoo: KarooSystemService) {

    // One thread for all state changes, so updates never race.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default.limitedParallelism(1))
    private val _state = MutableStateFlow(SmallerFieldsState())
    val state: StateFlow<SmallerFieldsState> = _state

    /**
     * The ride page on screen (null until the Karoo says). With the same field on two pages the
     * Karoo can show one page's picture in the other's tile, so only views on this page draw
     * (KarooType README, "Keeping a tile steady").
     */
    private val _activePage = MutableStateFlow<RideProfile.Page?>(null)
    val activePage: StateFlow<RideProfile.Page?> = _activePage

    fun start() {
        for (m in Metric.entries) {
            scope.launch {
                karoo.streamDataFlow(m.type).collect { s ->
                    val v = s.single()
                    _state.update { it.copy(values = it.values + (m to v)) }
                }
            }
        }
        scope.launch {
            karoo.consumerFlow<ActiveRidePage>().collect { _activePage.value = it.page }
        }
        scope.launch {
            karoo.consumerFlow<UserProfile>().collect { p ->
                _state.update {
                    it.copy(
                        powerZones = p.powerZones.map { z -> z.min..z.max },
                        hrZones = p.heartRateZones.map { z -> z.min..z.max },
                        units = Units(
                            imperial = p.preferredUnit.distance == UserProfile.PreferredUnit.UnitType.IMPERIAL,
                            imperialElevation = p.preferredUnit.elevation == UserProfile.PreferredUnit.UnitType.IMPERIAL,
                        ),
                    )
                }
            }
        }
    }

    fun stop() = scope.cancel()
}
