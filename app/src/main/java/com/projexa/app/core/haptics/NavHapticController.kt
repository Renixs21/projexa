package com.projexa.app.core.haptics

import com.projexa.app.domain.models.BeltCommand
import com.projexa.app.domain.models.ConnectionState
import com.projexa.app.domain.models.NavCueType
import com.projexa.app.domain.repositories.BeltRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Sends a navigation cue to the belt and refreshes it every second (the belt drops a cue after 3 s
 * without a refresh, so a crashed app or lost link cannot leave a cue stuck).
 * Obstacle priority is enforced on the belt itself, not here.
 *
 * @param pulseScale user preference multiplying pulse on/off times; clamped to the belt's accepted range.
 */
class NavHapticController(
    private val repo: BeltRepository,
    private val scope: CoroutineScope,
    private val pulseScale: () -> Float = { 1f },
) {
    private val _active = MutableStateFlow<NavCueType?>(null)
    val active: StateFlow<NavCueType?> = _active
    private var job: Job? = null

    fun play(cue: NavCueType) {
        job?.cancel()
        _active.value = cue
        job = scope.launch {
            while (repo.connection.value is ConnectionState.Connected) {
                val s = pulseScale()
                repo.send(
                    BeltCommand.NavCue(
                        cue.direction,
                        (cue.onMs * s).toInt().coerceIn(40, 1000),
                        (cue.offMs * s).toInt().coerceIn(40, 2000),
                    ),
                )
                delay(REFRESH_MS)
            }
            _active.value = null
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        _active.value = null
        repo.send(BeltCommand.NavClear)
    }

    private companion object { const val REFRESH_MS = 1000L }
}
