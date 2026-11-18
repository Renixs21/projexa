package com.projexa.app.presentation.communicate

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.res.stringResource
import com.projexa.app.R
import com.projexa.app.core.speech.SpeechToTextController
import com.projexa.app.core.speech.SttState
import com.projexa.app.core.speech.TextToSpeechController
import com.projexa.app.presentation.components.BannerText
import com.projexa.app.presentation.components.PrimaryButton
import com.projexa.app.presentation.components.ProjexaTextField
import com.projexa.app.presentation.components.SecondaryButton
import com.projexa.app.presentation.components.SectionCard
import com.projexa.app.presentation.theme.ProjexaColors

private val QUICK_MESSAGES = listOf(
    "I am deafblind.",
    "Please help me.",
    "I need directions.",
    "Please guide me.",
    "I need assistance.",
    "Please contact my family.",
)

@Composable
fun CommunicateScreen(vm: ConversationViewModel, speechRate: Float, onHaptic: () -> Unit) {
    val context: Context = LocalContext.current
    val you = stringResource(R.string.comm_you)
    val other = stringResource(R.string.comm_other)
    val messages by vm.messages.collectAsStateWithLifecycle()
    val language by vm.language.collectAsStateWithLifecycle()

    var ttsReady by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }
    var stt by remember { mutableStateOf<SttState>(SttState.Idle) }
    var text by remember { mutableStateOf("") }

    val tts = remember { TextToSpeechController(context) { ttsReady = it } }
    val currentLanguage by androidx.compose.runtime.rememberUpdatedState(language)
    val recognizer = remember {
        SpeechToTextController(
            context = context,
            onFinalResult = { vm.add(Speaker.OTHER, it) },
            onState = { stt = it },
        )
    }
    DisposableEffect(Unit) {
        onDispose {
            tts.shutdown()
            recognizer.stop()
        }
    }

    fun speak(msg: String) {
        if (msg.isBlank()) return
        notice = tts.speak(msg, language.tag, speechRate)
        if (notice == null) vm.add(Speaker.YOU, msg)
        onHaptic()
    }

    val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) recognizer.start(currentLanguage.tag)
        else stt = SttState.Error("Microphone permission is needed to listen. Allow it in Android settings.")
    }
    fun listen() {
        onHaptic()
        if (stt is SttState.Listening) {
            recognizer.stop()
            stt = SttState.Idle
            return
        }
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (granted) recognizer.start(language.tag) else micLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(stringResource(R.string.comm_title), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })

        notice?.let { BannerText(it, ProjexaColors.Danger) }

        SectionCard(title = stringResource(R.string.comm_language_title)) {
            SpeechLanguage.entries.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { l ->
                        val selected = l == language
                        if (selected) {
                            PrimaryButton(
                                l.label,
                                { vm.setLanguage(l) },
                                Modifier.weight(1f).semantics { this.selected = true },
                                icon = Icons.Default.Check,
                            )
                        } else {
                            SecondaryButton(l.label, { vm.setLanguage(l) }, Modifier.weight(1f))
                        }
                    }
                    if (row.size == 1) androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
                }
            }
        }

        SectionCard(title = stringResource(R.string.comm_listen_title)) {
            when (val s = stt) {
                is SttState.Listening ->
                    BannerText(if (s.partial.isBlank()) stringResource(R.string.comm_listening) else stringResource(R.string.comm_hearing, s.partial), ProjexaColors.Success)
                is SttState.Error -> BannerText(s.message, ProjexaColors.Warning)
                SttState.Idle -> Text(
                    stringResource(R.string.comm_listen_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = ProjexaColors.TextSecondary,
                )
            }
            PrimaryButton(
                if (stt is SttState.Listening) stringResource(R.string.comm_stop_listening) else stringResource(R.string.comm_listen),
                onClick = ::listen,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        SectionCard(title = stringResource(R.string.comm_conversation)) {
            if (messages.isEmpty()) {
                Text(stringResource(R.string.comm_no_messages), color = ProjexaColors.TextSecondary)
            } else {
                // The newest message is a live region so TalkBack (and a paired Braille display) outputs it right away.
                val last = messages.last()
                messages.dropLast(1).forEach { m ->
                    Text(
                        "${if (m.speaker == Speaker.YOU) you else other}: ${m.text}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = ProjexaColors.TextSecondary,
                    )
                }
                Text(
                    "${if (last.speaker == Speaker.YOU) you else other}: ${last.text}",
                    style = MaterialTheme.typography.titleLarge,
                    color = ProjexaColors.TextPrimary,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive },
                )
                SecondaryButton(stringResource(R.string.comm_clear), { vm.clear() }, Modifier.fillMaxWidth())
            }
        }

        SectionCard(title = stringResource(R.string.comm_quick)) {
            QUICK_MESSAGES.forEach { m ->
                SecondaryButton(m, onClick = { speak(m) }, enabled = ttsReady, modifier = Modifier.fillMaxWidth())
            }
        }

        SectionCard(title = stringResource(R.string.comm_type)) {
            ProjexaTextField(text, { text = it }, stringResource(R.string.comm_message))
            PrimaryButton(
                stringResource(R.string.comm_speak),
                onClick = { speak(text); text = "" },
                enabled = ttsReady && text.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        SectionCard(title = stringResource(R.string.comm_braille_title)) {
            Text(
                stringResource(R.string.comm_braille_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = ProjexaColors.TextSecondary,
            )
            SecondaryButton(
                stringResource(R.string.comm_braille_pair),
                { context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) },
                Modifier.fillMaxWidth(),
            )
            SecondaryButton(
                stringResource(R.string.comm_braille_access),
                { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) },
                Modifier.fillMaxWidth(),
            )
        }
    }
}
