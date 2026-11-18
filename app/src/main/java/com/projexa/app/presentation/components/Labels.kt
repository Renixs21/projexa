package com.projexa.app.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.projexa.app.R
import com.projexa.app.domain.models.Direction
import com.projexa.app.domain.models.NavCueType
import com.projexa.app.domain.models.ProximityLevel

/** Localized display names for domain enums (domain code stays free of Android resources). */
@Composable
fun Direction.localized(): String = stringResource(
    when (this) {
        Direction.FRONT -> R.string.dir_front
        Direction.BACK -> R.string.dir_back
        Direction.LEFT -> R.string.dir_left
        Direction.RIGHT -> R.string.dir_right
    },
)

@Composable
fun ProximityLevel.localized(): String = stringResource(
    when (this) {
        ProximityLevel.CLEAR -> R.string.lvl_clear
        ProximityLevel.CAUTION -> R.string.lvl_caution
        ProximityLevel.WARNING -> R.string.lvl_warning
        ProximityLevel.DANGER -> R.string.lvl_danger
        ProximityLevel.CRITICAL -> R.string.lvl_critical
        ProximityLevel.NO_READING -> R.string.lvl_no_reading
    },
)

@Composable
fun NavCueType.localized(): String = stringResource(
    when (this) {
        NavCueType.STRAIGHT -> R.string.cue_straight
        NavCueType.TURN_LEFT -> R.string.cue_left
        NavCueType.TURN_RIGHT -> R.string.cue_right
        NavCueType.STOP -> R.string.cue_stop
        NavCueType.ARRIVED -> R.string.cue_arrived
    },
)

@Composable
fun NavCueType.localizedMeaning(): String = stringResource(
    when (this) {
        NavCueType.STRAIGHT -> R.string.cue_straight_meaning
        NavCueType.TURN_LEFT -> R.string.cue_left_meaning
        NavCueType.TURN_RIGHT -> R.string.cue_right_meaning
        NavCueType.STOP -> R.string.cue_stop_meaning
        NavCueType.ARRIVED -> R.string.cue_arrived_meaning
    },
)
