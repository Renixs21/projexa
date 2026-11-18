package com.projexa.app.domain.models

/** User preferences persisted with DataStore. */
data class AppSettings(
    val fontScale: Float = 1f,
    val highContrast: Boolean = false,
    /** Hides the technical Device tab. */
    val simpleMode: Boolean = false,
    /** Multiplies navigation pulse on/off times (belt limits still apply). */
    val pulseScale: Float = 1f,
    /** Distance from a turn at which the belt starts the turn cue. */
    val cueDistanceM: Int = 30,
    val speechRate: Float = 1f,
    val onboardingDone: Boolean = false,
)
