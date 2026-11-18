package com.projexa.app.presentation.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.projexa.app.R
import com.projexa.app.domain.models.NavCueType
import com.projexa.app.presentation.BeltUiState
import com.projexa.app.presentation.components.BannerText
import com.projexa.app.presentation.components.PrimaryButton
import com.projexa.app.presentation.components.SecondaryButton
import com.projexa.app.presentation.components.SectionCard
import com.projexa.app.presentation.components.localized
import com.projexa.app.presentation.components.localizedMeaning
import com.projexa.app.presentation.theme.ProjexaColors

/** Learn the navigation cues on the real belt. Route guidance sends these same cues. */
@Composable
fun HapticTutorialScreen(
    state: BeltUiState,
    onPlay: (NavCueType) -> Unit,
    onStop: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            stringResource(R.string.tut_title),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.semantics { heading() },
        )

        SectionCard(title = stringResource(R.string.tut_title)) {
            Text(
                stringResource(R.string.tut_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = ProjexaColors.TextSecondary,
            )
            if (!state.isConnected) {
                BannerText(stringResource(R.string.tut_connect), ProjexaColors.Danger)
            }
            NavCueType.entries.forEach { cue ->
                val playing = state.activeCue == cue
                Text(
                    "${cue.localized()}: ${cue.localizedMeaning()}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = ProjexaColors.TextPrimary,
                )
                if (playing) {
                    PrimaryButton(
                        stringResource(R.string.tut_stop_cue, cue.localized()),
                        onClick = onStop,
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    SecondaryButton(
                        stringResource(R.string.tut_feel, cue.localized()),
                        onClick = { onPlay(cue) },
                        enabled = state.isConnected,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            if (state.activeCue != null) {
                SecondaryButton(stringResource(R.string.tut_stop_all), onClick = onStop, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
