package com.projexa.app.data.navigation

import com.projexa.app.core.haptics.NavHapticController
import com.projexa.app.domain.models.GeoPoint
import com.projexa.app.domain.models.ManeuverKind
import com.projexa.app.domain.models.NavCueType
import com.projexa.app.domain.models.Place
import com.projexa.app.domain.models.Route
import com.projexa.app.domain.usecases.RouteProgress
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

enum class NavPhase { IDLE, ROUTING, ACTIVE, ARRIVED }

data class NavUiState(
    val phase: NavPhase = NavPhase.IDLE,
    val searching: Boolean = false,
    val searchResults: List<Place> = emptyList(),
    val destination: Place? = null,
    val instruction: String? = null,
    val distToNextM: Int? = null,
    val remainingM: Int? = null,
    val etaMinutes: Int? = null,
    val cue: NavCueType? = null,
    val recalculating: Boolean = false,
    val gpsAccuracyM: Int? = null,
    val error: String? = null,
)

/**
 * Turns real GPS fixes + a walking route into on-screen directions and belt haptic cues.
 * Obstacle priority is enforced by the belt firmware; this only sends low-priority navigation cues.
 */
class NavigationCoordinator(
    private val osm: OsmClient,
    private val location: LocationSource,
    private val haptics: NavHapticController,
    private val instructionDistanceM: () -> Double,
    private val scope: CoroutineScope,
    private val onDestinationStarted: suspend (Place) -> Unit,
) {
    private val _state = MutableStateFlow(NavUiState())
    val state: StateFlow<NavUiState> = _state

    private var job: Job? = null

    fun search(query: String) {
        if (query.isBlank()) return
        _state.update { it.copy(searching = true, error = null) }
        scope.launch {
            try {
                val results = osm.search(query)
                _state.update {
                    it.copy(
                        searching = false,
                        searchResults = results,
                        error = if (results.isEmpty()) "No places found for \"$query\"." else null,
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(searching = false, error = "Search failed. Check your internet connection.") }
            }
        }
    }

    fun start(destination: Place) {
        job?.cancel()
        _state.value = NavUiState(phase = NavPhase.ROUTING, destination = destination, instruction = "Waiting for GPS…")
        job = scope.launch {
            try {
                onDestinationStarted(destination)
                val first = withTimeoutOrNull(GPS_TIMEOUT_MS) {
                    location.fixes().first { it.accuracyM <= MAX_USABLE_ACCURACY_M }
                }
                if (first == null) {
                    fail("Could not get a GPS position. Go outdoors, make sure Location is on, and try again.")
                    return@launch
                }
                val route = try {
                    osm.walkingRoute(first.point, destination.point)
                } catch (e: Exception) {
                    fail("Could not get a walking route. Check your internet connection.")
                    return@launch
                }
                run(destination, route)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                fail("Navigation stopped: location is unavailable.")
            }
        }
    }

    private suspend fun run(destination: Place, initial: Route) {
        var route = initial
        var stepIdx = 0
        var offRouteCount = 0
        var straightUntil = System.currentTimeMillis() + STRAIGHT_BURST_MS
        var recalculating = false

        _state.update { it.copy(phase = NavPhase.ACTIVE, instruction = route.steps.first().instruction) }

        location.fixes().first { fix ->
            if (fix.accuracyM > MAX_USABLE_ACCURACY_M) {
                _state.update { it.copy(gpsAccuracyM = fix.accuracyM.toInt()) }
                return@first false
            }
            val p = fix.point

            // Route deviation -> recalculate after several consecutive off-route fixes.
            val offBy = RouteProgress.distanceToRouteM(p, route)
            val threshold = maxOf(OFF_ROUTE_M, fix.accuracyM * 1.5)
            offRouteCount = if (offBy > threshold) offRouteCount + 1 else 0
            if (offRouteCount >= OFF_ROUTE_FIXES && !recalculating) {
                recalculating = true
                _state.update { it.copy(recalculating = true) }
                try {
                    route = osm.walkingRoute(p, destination.point)
                    stepIdx = 0
                    offRouteCount = 0
                    straightUntil = System.currentTimeMillis() + STRAIGHT_BURST_MS
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: Exception) {
                    // keep the old route; try again after more off-route fixes
                    offRouteCount = 0
                }
                recalculating = false
            }

            val progress = RouteProgress.advance(route, stepIdx, p)
            if (progress.stepIndex != stepIdx) {
                stepIdx = progress.stepIndex
                straightUntil = System.currentTimeMillis() + STRAIGHT_BURST_MS
            }
            val next = route.steps[minOf(stepIdx + 1, route.steps.lastIndex)]

            if (progress.arrived) {
                _state.update {
                    it.copy(
                        phase = NavPhase.ARRIVED, instruction = "You have arrived", cue = NavCueType.ARRIVED,
                        distToNextM = 0, remainingM = 0, etaMinutes = 0, recalculating = false,
                    )
                }
                haptics.play(NavCueType.ARRIVED)
                delay(ARRIVED_BUZZ_MS)
                haptics.stop()
                _state.update { it.copy(cue = null) }
                return@first true
            }

            val desired: NavCueType? = when {
                progress.distToNextM <= instructionDistanceM() -> when (next.kind) {
                    ManeuverKind.LEFT -> NavCueType.TURN_LEFT
                    ManeuverKind.RIGHT -> NavCueType.TURN_RIGHT
                    ManeuverKind.UTURN -> NavCueType.STOP
                    else -> null
                }
                else -> null
            } ?: if (System.currentTimeMillis() < straightUntil) NavCueType.STRAIGHT else null

            driveHaptics(desired)

            _state.update {
                it.copy(
                    instruction = next.instruction,
                    distToNextM = progress.distToNextM.toInt(),
                    remainingM = progress.remainingM.toInt(),
                    etaMinutes = ((progress.remainingM / WALK_SPEED_MPS) / 60).toInt().coerceAtLeast(1),
                    cue = desired,
                    recalculating = recalculating,
                    gpsAccuracyM = fix.accuracyM.toInt(),
                )
            }
            false
        }
    }

    private fun driveHaptics(desired: NavCueType?) {
        if (desired == null) {
            if (haptics.active.value != null) haptics.stop()
        } else if (haptics.active.value != desired) {
            haptics.play(desired)
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        haptics.stop()
        _state.update { NavUiState(searchResults = it.searchResults) }
    }

    private fun fail(message: String) {
        haptics.stop()
        _state.update { NavUiState(searchResults = it.searchResults, error = message) }
    }

    fun clearError() = _state.update { it.copy(error = null) }

    companion object {
        const val GPS_TIMEOUT_MS = 30_000L
        const val MAX_USABLE_ACCURACY_M = 40f
        const val OFF_ROUTE_M = 40.0
        const val OFF_ROUTE_FIXES = 3
        const val STRAIGHT_BURST_MS = 4_000L
        const val ARRIVED_BUZZ_MS = 5_000L
        const val WALK_SPEED_MPS = 1.3
    }
}
