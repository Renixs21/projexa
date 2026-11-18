package com.projexa.app.presentation.device

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.projexa.app.domain.models.ConnectionState
import com.projexa.app.domain.models.Direction
import com.projexa.app.presentation.BeltUiState
import com.projexa.app.presentation.components.BeltMonitor
import com.projexa.app.presentation.components.PrimaryButton
import com.projexa.app.presentation.components.SecondaryButton
import com.projexa.app.presentation.components.SectionCard
import com.projexa.app.presentation.home.ConnectionSummary
import com.projexa.app.presentation.home.MonitoringBanner
import com.projexa.app.presentation.theme.ProjexaColors

private const val INVALID_STREAK_WARN = 10

@Composable
fun DeviceScreen(
    state: BeltUiState,
    onConnectToggle: () -> Unit,
    onTestMotor: (Direction) -> Unit,
    onRunTest: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Device", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })

        SectionCard(title = "ESP32 belt") {
            ConnectionSummary(state)
            PrimaryButton(
                text = if (state.connection is ConnectionState.Disconnected) "Connect to belt" else "Disconnect",
                onClick = onConnectToggle,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        SectionCard(title = "Belt sensors and motors") {
            MonitoringBanner(state)
            BeltMonitor(state.snapshot, state.isLive, state.thresholds)
        }

        SectionCard(title = "Sensor health") {
            val snap = state.snapshot
            if (!state.isConnected || snap == null) {
                Text("Unavailable until the belt is connected and sending data.", color = ProjexaColors.TextSecondary)
            } else {
                Direction.entries.forEach { d ->
                    val streak = snap.invalidStreak[d] ?: 0
                    val text = if (streak >= INVALID_STREAK_WARN)
                        "${d.label}: no echo for $streak scans. Nothing within range, or the sensor may be faulty."
                    else "${d.label}: OK"
                    Text(
                        text,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (streak >= INVALID_STREAK_WARN) ProjexaColors.Warning else ProjexaColors.Success,
                    )
                }
            }
        }

        SectionCard(title = "Test") {
            Text(
                "Pulses each belt motor for half a second, in order: front, back, left, right. " +
                    "A motor that is already on for a nearby obstacle stays on.",
                style = MaterialTheme.typography.bodyMedium,
                color = ProjexaColors.TextSecondary,
            )
            PrimaryButton(
                text = if (state.testRunning) "Testing…" else "Run sensor and motor test",
                onClick = onRunTest,
                enabled = state.isConnected && !state.testRunning,
                modifier = Modifier.fillMaxWidth(),
            )
            Direction.entries.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { d ->
                        SecondaryButton(
                            text = "Test ${d.label}",
                            onClick = { onTestMotor(d) },
                            enabled = state.isConnected,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }

        SectionCard(title = "Bluetooth details") {
            val c = state.connection
            val (name, addr) = when (c) {
                is ConnectionState.Connected -> c.device.name to c.device.address
                is ConnectionState.Connecting -> c.device.name to c.device.address
                else -> null to null
            }
            Detail("Device", name ?: "None")
            Detail("Address", addr ?: "None")
            Detail("Profile", "Bluetooth Classic SPP")
            Detail("Last update", state.lastUpdateAgeMs?.let { "%.1f s ago".format(it / 1000f) } ?: "No data yet")
            Detail("Packets received", state.stats.packetsReceived.toString())
            Detail("Malformed lines", state.stats.malformedLines.toString())
            Detail("Obstacle detection limit", "50 cm (set on the belt)")
        }
    }
}

@Composable
private fun Detail(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = ProjexaColors.TextSecondary, style = MaterialTheme.typography.bodyMedium)
        Text(value, color = ProjexaColors.TextPrimary, style = MaterialTheme.typography.bodyMedium)
    }
}
