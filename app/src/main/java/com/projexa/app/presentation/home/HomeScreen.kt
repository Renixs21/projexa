package com.projexa.app.presentation.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.projexa.app.R
import com.projexa.app.domain.models.ConnectionState
import com.projexa.app.presentation.BeltUiState
import com.projexa.app.presentation.components.ActionTile
import com.projexa.app.presentation.components.BannerText
import com.projexa.app.presentation.components.BeltMonitor
import com.projexa.app.presentation.components.BeltRadar
import com.projexa.app.presentation.components.Overline
import com.projexa.app.presentation.components.PrimaryButton
import com.projexa.app.presentation.components.SecondaryButton
import com.projexa.app.presentation.components.SectionCard
import com.projexa.app.presentation.components.StatusPill
import com.projexa.app.presentation.theme.ProjexaColors
import java.text.DateFormat
import java.util.Calendar
import java.util.Date

@Composable
fun HomeScreen(
    state: BeltUiState,
    onConnectToggle: () -> Unit,
    onOpenNavigation: () -> Unit,
    onOpenCommunicate: () -> Unit,
) {
    val hour = Calendar.getInstance().apply { timeInMillis = state.wallClockMs }.get(Calendar.HOUR_OF_DAY)
    val greeting = stringResource(
        when (hour) {
            in 5..11 -> R.string.greet_morning
            in 12..16 -> R.string.greet_afternoon
            else -> R.string.greet_evening
        },
    )

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Header
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Overline(stringResource(R.string.app_name), color = ProjexaColors.Blue)
                Text(
                    greeting,
                    style = MaterialTheme.typography.headlineMedium,
                    color = ProjexaColors.TextPrimary,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(state.wallClockMs)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = ProjexaColors.TextSecondary,
                )
            }
            ConnectionPill(state)
        }

        // Hero: live radar
        SectionCard(title = stringResource(R.string.home_live_monitoring)) {
            BeltRadar(state.snapshot, state.isLive, state.thresholds, Modifier.align(Alignment.CenterHorizontally))
            MonitoringBanner(state)
            PrimaryButton(
                text = when (state.connection) {
                    is ConnectionState.Disconnected -> stringResource(R.string.home_connect)
                    is ConnectionState.Connecting -> stringResource(R.string.home_cancel_connecting)
                    is ConnectionState.Connected -> stringResource(R.string.home_disconnect)
                },
                onClick = onConnectToggle,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        // Sensor grid
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Overline(stringResource(R.string.home_sensors))
            BeltMonitor(state.snapshot, state.isLive, state.thresholds, showRadar = false)
        }

        // Quick access
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Overline(stringResource(R.string.home_quick))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ActionTile(
                    stringResource(R.string.tab_navigation),
                    stringResource(R.string.action_nav_sub),
                    Icons.Default.LocationOn,
                    onOpenNavigation,
                    Modifier.weight(1f),
                )
                ActionTile(
                    stringResource(R.string.tab_communicate),
                    stringResource(R.string.action_comm_sub),
                    Icons.AutoMirrored.Filled.Send,
                    onOpenCommunicate,
                    Modifier.weight(1f),
                )
            }
        }
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun ConnectionPill(state: BeltUiState) {
    when (val c = state.connection) {
        is ConnectionState.Connected -> StatusPill(c.device.name, ProjexaColors.Success)
        is ConnectionState.Connecting -> StatusPill("${c.device.name}…", ProjexaColors.Warning)
        ConnectionState.Disconnected -> StatusPill(stringResource(R.string.home_not_connected), ProjexaColors.Danger)
    }
}

@Composable
fun ConnectionSummary(state: BeltUiState) {
    when (val c = state.connection) {
        is ConnectionState.Connected ->
            BannerText(stringResource(R.string.home_connected_to, c.device.name), ProjexaColors.Success)
        is ConnectionState.Connecting ->
            BannerText(
                if (c.attempt <= 1) stringResource(R.string.home_connecting_to, c.device.name)
                else stringResource(R.string.home_reconnecting, c.device.name, c.attempt),
                ProjexaColors.Warning,
            )
        ConnectionState.Disconnected -> BannerText(stringResource(R.string.home_not_connected), ProjexaColors.Danger)
    }
}

@Composable
fun MonitoringBanner(state: BeltUiState) {
    when {
        !state.isConnected -> BannerText(stringResource(R.string.mon_unavailable), ProjexaColors.Warning)
        state.snapshot == null -> BannerText(stringResource(R.string.mon_waiting), ProjexaColors.Warning)
        state.isStale -> BannerText(stringResource(R.string.mon_stale), ProjexaColors.Danger)
        else -> BannerText(stringResource(R.string.mon_active), ProjexaColors.Success)
    }
}
