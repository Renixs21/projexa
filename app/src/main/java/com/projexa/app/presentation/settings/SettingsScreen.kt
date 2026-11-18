package com.projexa.app.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.projexa.app.R
import com.projexa.app.domain.models.AppSettings
import com.projexa.app.presentation.components.SegmentedChoice
import com.projexa.app.presentation.components.SectionCard
import com.projexa.app.presentation.theme.ProjexaColors

@Composable
fun SettingsScreen(settings: AppSettings, onChange: ((AppSettings) -> AppSettings) -> Unit, onHaptic: () -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            stringResource(R.string.settings_title),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.semantics { heading() },
        )

        SectionCard(title = stringResource(R.string.settings_display)) {
            Text(stringResource(R.string.settings_font_size), color = ProjexaColors.TextPrimary, style = MaterialTheme.typography.bodyLarge)
            Choices(
                options = listOf(
                    stringResource(R.string.opt_normal) to 1f,
                    stringResource(R.string.opt_large) to 1.25f,
                    stringResource(R.string.opt_xlarge) to 1.5f,
                ),
                current = settings.fontScale,
                onSelect = { v -> onHaptic(); onChange { it.copy(fontScale = v) } },
            )
            SwitchRow(stringResource(R.string.settings_high_contrast), settings.highContrast) { v ->
                onHaptic(); onChange { it.copy(highContrast = v) }
            }
            SwitchRow(stringResource(R.string.settings_simple_mode), settings.simpleMode) { v ->
                onHaptic(); onChange { it.copy(simpleMode = v) }
            }
        }

        SectionCard(title = stringResource(R.string.settings_haptics)) {
            Text(stringResource(R.string.settings_pulse), color = ProjexaColors.TextPrimary, style = MaterialTheme.typography.bodyLarge)
            Choices(
                options = listOf(
                    stringResource(R.string.opt_shorter) to 0.6f,
                    stringResource(R.string.opt_normal) to 1f,
                    stringResource(R.string.opt_longer) to 1.5f,
                ),
                current = settings.pulseScale,
                onSelect = { v -> onHaptic(); onChange { it.copy(pulseScale = v) } },
            )
            Text(stringResource(R.string.settings_cue_distance), color = ProjexaColors.TextPrimary, style = MaterialTheme.typography.bodyLarge)
            Choices(
                options = listOf("20 m" to 20, "30 m" to 30, "50 m" to 50),
                current = settings.cueDistanceM,
                onSelect = { v -> onHaptic(); onChange { it.copy(cueDistanceM = v) } },
            )
        }

        SectionCard(title = stringResource(R.string.settings_speech)) {
            Text(stringResource(R.string.settings_speech_rate), color = ProjexaColors.TextPrimary, style = MaterialTheme.typography.bodyLarge)
            Choices(
                options = listOf(
                    stringResource(R.string.opt_slow) to 0.7f,
                    stringResource(R.string.opt_normal) to 1f,
                    stringResource(R.string.opt_fast) to 1.4f,
                ),
                current = settings.speechRate,
                onSelect = { v -> onHaptic(); onChange { it.copy(speechRate = v) } },
            )
        }

        SectionCard(title = stringResource(R.string.settings_belt)) {
            Text(stringResource(R.string.settings_belt_info), color = ProjexaColors.TextSecondary, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun <T> Choices(options: List<Pair<String, T>>, current: T, onSelect: (T) -> Unit) {
    SegmentedChoice(options, current, onSelect)
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onChecked),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = ProjexaColors.TextPrimary, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = null)
    }
}
