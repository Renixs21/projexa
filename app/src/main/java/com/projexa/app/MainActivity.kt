package com.projexa.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.ViewModelProvider
import com.projexa.app.core.haptics.PhoneHaptics
import com.projexa.app.presentation.BeltViewModel
import com.projexa.app.presentation.communicate.CommunicateScreen
import com.projexa.app.presentation.communicate.ConversationViewModel
import com.projexa.app.presentation.device.DeviceScreen
import com.projexa.app.presentation.home.HomeScreen
import com.projexa.app.domain.models.Place
import com.projexa.app.presentation.navigation.NavViewModel
import com.projexa.app.presentation.navigation.NavigationScreen
import com.projexa.app.presentation.settings.SettingsScreen
import com.projexa.app.presentation.components.PrimaryButton
import com.projexa.app.domain.models.AppSettings
import androidx.annotation.StringRes
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.projexa.app.presentation.theme.ProjexaColors
import com.projexa.app.presentation.theme.ProjexaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val app = applicationContext as ProjexaApp
            val settings by app.settings.collectAsStateWithLifecycle()
            val s = settings
            ProjexaTheme(highContrast = s?.highContrast == true, fontScale = s?.fontScale ?: 1f) {
                when {
                    s == null -> Box(Modifier.fillMaxSize().background(ProjexaColors.Background))
                    !s.onboardingDone -> OnboardingScreen {
                        app.updateSettings { it.copy(onboardingDone = true) }
                    }
                    else -> ProjexaRoot(s)
                }
            }
        }
    }
}

private enum class Tab(@StringRes val labelRes: Int, val icon: ImageVector) {
    HOME(R.string.tab_home, Icons.Default.Home),
    NAVIGATION(R.string.tab_navigation, Icons.Default.Search),
    COMMUNICATE(R.string.tab_communicate, Icons.AutoMirrored.Filled.Send),
    DEVICE(R.string.tab_device, Icons.Default.Info),
    SETTINGS(R.string.tab_settings, Icons.Default.Settings),
}

@Composable
private fun OnboardingScreen(onDone: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(ProjexaColors.Background).verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
    ) {
        Text(
            stringResource(R.string.onboarding_title),
            style = MaterialTheme.typography.headlineMedium,
            color = ProjexaColors.Blue,
            modifier = Modifier.semantics { heading() },
        )
        Text(stringResource(R.string.onboarding_body), style = MaterialTheme.typography.bodyLarge, color = ProjexaColors.TextPrimary)
        PrimaryButton(stringResource(R.string.onboarding_start), onDone, Modifier.fillMaxWidth())
    }
}

@Composable
private fun ProjexaRoot(settings: AppSettings) {
    val context = LocalContext.current
    val app = context.applicationContext as ProjexaApp
    val vm: BeltViewModel = viewModel(
        factory = viewModelFactory { initializer { BeltViewModel(app.beltRepository, app.settingsStore, app.navHaptics) } },
    )
    val navVm: NavViewModel = viewModel(
        factory = viewModelFactory { initializer { NavViewModel(app.navigationCoordinator, app.settingsStore) } },
    )
    val conversationVm: ConversationViewModel = viewModel()
    val state by vm.uiState.collectAsStateWithLifecycle()
    val navState by navVm.state.collectAsStateWithLifecycle()
    val saved by navVm.saved.collectAsStateWithLifecycle()
    var pendingPlace by remember { mutableStateOf<Place?>(null) }
    val locationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val place = pendingPlace
        pendingPlace = null
        if (granted && place != null) navVm.start(place)
        else if (!granted) vm.postMessage("PROJEXA needs the Location permission to give walking directions. Allow it in Android settings.")
    }
    fun hasLocationPermission() =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    val haptics = remember { PhoneHaptics(context) }
    var selectedTab by rememberSaveable { mutableStateOf(Tab.HOME) }
    val visibleTabs = Tab.entries.filter { !(settings.simpleMode && it == Tab.DEVICE) }
    val tab = if (selectedTab in visibleTabs) selectedTab else Tab.HOME
    fun selectTab(t: Tab) { selectedTab = t }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) vm.onConnectToggle()
        else vm.postMessage("PROJEXA needs the Nearby devices (Bluetooth) permission to talk to the belt. Allow it in Android settings.")
    }
    val connectToggle: () -> Unit = {
        haptics.confirm()
        val needsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
        if (needsPermission) permissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT) else vm.onConnectToggle()
    }

    Scaffold(
        containerColor = ProjexaColors.Background,
        bottomBar = {
            NavigationBar(containerColor = ProjexaColors.Surface) {
                visibleTabs.forEach { t ->
                    NavigationBarItem(
                        selected = tab == t,
                        onClick = { selectTab(t) },
                        icon = { Icon(t.icon, contentDescription = null) },
                        label = { Text(stringResource(t.labelRes), fontSize = 11.sp, maxLines = 1, softWrap = false) },
                        alwaysShowLabel = true,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = ProjexaColors.Cyan,
                            selectedTextColor = ProjexaColors.TextPrimary,
                            indicatorColor = ProjexaColors.Blue.copy(alpha = 0.18f),
                            unselectedIconColor = ProjexaColors.TextSecondary,
                            unselectedTextColor = ProjexaColors.TextSecondary,
                        ),
                    )
                }
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            when (tab) {
                Tab.HOME -> HomeScreen(
                    state = state,
                    onConnectToggle = connectToggle,
                    onOpenNavigation = { selectTab(Tab.NAVIGATION) },
                    onOpenCommunicate = { selectTab(Tab.COMMUNICATE) },
                )
                Tab.NAVIGATION -> NavigationScreen(
                    nav = navState,
                    saved = saved,
                    belt = state,
                    onSearch = { haptics.confirm(); navVm.search(it) },
                    onStart = { place ->
                        haptics.confirm()
                        if (hasLocationPermission()) navVm.start(place)
                        else { pendingPlace = place; locationLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) }
                    },
                    onStop = { haptics.confirm(); navVm.stop() },
                    onSetHome = { haptics.confirm(); navVm.setHome(it) },
                    onSetWork = { haptics.confirm(); navVm.setWork(it) },
                    onDismissError = navVm::clearError,
                    onPlayCue = { haptics.confirm(); vm.playCue(it) },
                    onStopCue = { haptics.confirm(); vm.stopCue() },
                )
                Tab.COMMUNICATE -> CommunicateScreen(vm = conversationVm, speechRate = settings.speechRate, onHaptic = haptics::confirm)
                Tab.DEVICE -> DeviceScreen(
                    state = state,
                    onConnectToggle = connectToggle,
                    onTestMotor = { haptics.confirm(); vm.testMotor(it) },
                    onRunTest = { haptics.confirm(); vm.runSensorAndMotorTest() },
                )
                Tab.SETTINGS -> SettingsScreen(
                    settings = settings,
                    onChange = app::updateSettings,
                    onHaptic = haptics::confirm,
                )
            }
        }
    }

    state.pickerDevices?.let { devices ->
        AlertDialog(
            onDismissRequest = vm::dismissPicker,
            title = { Text("Choose your belt") },
            text = {
                Column {
                    devices.forEach { d ->
                        TextButton(onClick = { vm.connectTo(d) }) { Text("${d.name}  (${d.address})") }
                    }
                }
            },
            confirmButton = { TextButton(onClick = vm::dismissPicker) { Text("Cancel") } },
        )
    }

    state.message?.let { msg ->
        AlertDialog(
            onDismissRequest = vm::dismissMessage,
            text = { Text(msg) },
            confirmButton = { TextButton(onClick = vm::dismissMessage) { Text("OK") } },
        )
    }
}
