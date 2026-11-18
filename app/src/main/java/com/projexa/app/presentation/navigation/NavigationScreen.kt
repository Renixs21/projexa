package com.projexa.app.presentation.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.projexa.app.R
import com.projexa.app.data.navigation.NavPhase
import com.projexa.app.data.navigation.NavUiState
import com.projexa.app.domain.models.NavCueType
import com.projexa.app.domain.models.Place
import com.projexa.app.domain.models.SavedPlaces
import com.projexa.app.presentation.BeltUiState
import com.projexa.app.presentation.components.BannerText
import com.projexa.app.presentation.components.PrimaryButton
import com.projexa.app.presentation.components.ProjexaTextField
import com.projexa.app.presentation.components.SecondaryButton
import com.projexa.app.presentation.components.SectionCard
import com.projexa.app.presentation.components.localized
import com.projexa.app.presentation.theme.ProjexaColors

@Composable
fun NavigationScreen(
    nav: NavUiState,
    saved: SavedPlaces,
    belt: BeltUiState,
    onSearch: (String) -> Unit,
    onStart: (Place) -> Unit,
    onStop: () -> Unit,
    onSetHome: (Place) -> Unit,
    onSetWork: (Place) -> Unit,
    onDismissError: () -> Unit,
    onPlayCue: (NavCueType) -> Unit,
    onStopCue: () -> Unit,
) {
    var showTutorial by rememberSaveable { mutableStateOf(false) }
    if (showTutorial) {
        Column(Modifier.fillMaxSize()) {
            SecondaryButton(stringResource(R.string.tut_back), { showTutorial = false }, Modifier.fillMaxWidth().padding(16.dp))
            HapticTutorialScreen(belt, onPlayCue, onStopCue)
        }
        return
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            stringResource(R.string.nav_title),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.semantics { heading() },
        )

        nav.error?.let {
            BannerText(it, ProjexaColors.Danger)
            SecondaryButton(stringResource(R.string.nav_dismiss), onDismissError, Modifier.fillMaxWidth())
        }

        when (nav.phase) {
            NavPhase.IDLE -> IdleContent(nav, saved, onSearch, onStart, onSetHome, onSetWork)
            NavPhase.ROUTING -> {
                BannerText(
                    stringResource(R.string.nav_finding_route, nav.destination?.name.orEmpty()),
                    ProjexaColors.Warning,
                )
                SecondaryButton(stringResource(R.string.nav_cancel), onStop, Modifier.fillMaxWidth())
            }
            NavPhase.ACTIVE, NavPhase.ARRIVED -> ActiveContent(nav, belt, onStop)
        }

        SectionCard(title = stringResource(R.string.nav_safety_title)) {
            Text(
                stringResource(R.string.nav_safety_text),
                style = MaterialTheme.typography.bodyMedium,
                color = ProjexaColors.TextSecondary,
            )
        }

        SecondaryButton(stringResource(R.string.nav_open_tutorial), { showTutorial = true }, Modifier.fillMaxWidth())
    }
}

@Composable
private fun IdleContent(
    nav: NavUiState,
    saved: SavedPlaces,
    onSearch: (String) -> Unit,
    onStart: (Place) -> Unit,
    onSetHome: (Place) -> Unit,
    onSetWork: (Place) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }

    SectionCard(title = stringResource(R.string.nav_where_to)) {
        ProjexaTextField(query, { query = it }, stringResource(R.string.nav_search_hint))
        PrimaryButton(
            if (nav.searching) stringResource(R.string.nav_searching) else stringResource(R.string.nav_search),
            onClick = { onSearch(query) },
            enabled = query.isNotBlank() && !nav.searching,
            modifier = Modifier.fillMaxWidth(),
        )
    }

    if (saved.home != null || saved.work != null) {
        SectionCard(title = stringResource(R.string.nav_shortcuts)) {
            saved.home?.let { SecondaryButton(stringResource(R.string.nav_go_home), { onStart(it) }, Modifier.fillMaxWidth()) }
            saved.work?.let { SecondaryButton(stringResource(R.string.nav_go_work), { onStart(it) }, Modifier.fillMaxWidth()) }
        }
    }

    if (nav.searchResults.isNotEmpty()) {
        SectionCard(title = stringResource(R.string.nav_results)) {
            nav.searchResults.forEach { place ->
                Text(place.name, style = MaterialTheme.typography.bodyLarge, color = ProjexaColors.TextPrimary)
                PrimaryButton(stringResource(R.string.nav_walk_here), { onStart(place) }, Modifier.fillMaxWidth())
                SecondaryButton(stringResource(R.string.nav_save_home), { onSetHome(place) }, Modifier.fillMaxWidth())
                SecondaryButton(stringResource(R.string.nav_save_work), { onSetWork(place) }, Modifier.fillMaxWidth())
            }
        }
    }

    if (saved.history.isNotEmpty()) {
        SectionCard(title = stringResource(R.string.nav_recent)) {
            saved.history.forEach { place ->
                SecondaryButton(place.name.take(60), { onStart(place) }, Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun ActiveContent(nav: NavUiState, belt: BeltUiState, onStop: () -> Unit) {
    SectionCard(title = stringResource(R.string.nav_walking_to, nav.destination?.name?.take(50).orEmpty())) {
        Text(
            nav.instruction.orEmpty(),
            style = MaterialTheme.typography.headlineMedium,
            color = ProjexaColors.TextPrimary,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
        nav.distToNextM?.let {
            if (nav.phase == NavPhase.ACTIVE) {
                Text(stringResource(R.string.nav_in_meters, it), style = MaterialTheme.typography.titleLarge, color = ProjexaColors.Cyan)
            }
        }
        if (nav.remainingM != null && nav.etaMinutes != null && nav.phase == NavPhase.ACTIVE) {
            Text(
                stringResource(R.string.nav_remaining, nav.remainingM, nav.etaMinutes),
                style = MaterialTheme.typography.bodyLarge,
                color = ProjexaColors.TextSecondary,
            )
        }
        nav.cue?.let { BannerText(stringResource(R.string.nav_belt_vibration, it.localized()), ProjexaColors.Blue) }
        if (nav.recalculating) BannerText(stringResource(R.string.nav_off_route), ProjexaColors.Warning)
        if (!belt.isConnected) BannerText(stringResource(R.string.nav_no_belt), ProjexaColors.Warning)
        nav.gpsAccuracyM?.let {
            Text(
                stringResource(R.string.nav_gps_accuracy, it),
                style = MaterialTheme.typography.bodyMedium,
                color = ProjexaColors.TextSecondary,
            )
        }
    }
    PrimaryButton(
        if (nav.phase == NavPhase.ARRIVED) stringResource(R.string.nav_done) else stringResource(R.string.nav_stop),
        onStop,
        Modifier.fillMaxWidth(),
    )
}
