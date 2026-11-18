package com.projexa.app.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.projexa.app.presentation.theme.ProjexaColors

/** Honest "not built yet" screen for later phases. It has no controls, so nothing here is fake. */
@Composable
fun ComingLaterScreen(title: String, phase: String, summary: String) {
    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(title, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
        Text("Not available yet. Planned for $phase.", color = ProjexaColors.Warning, style = MaterialTheme.typography.bodyLarge)
        Text(summary, color = ProjexaColors.TextSecondary, style = MaterialTheme.typography.bodyMedium)
    }
}
