package com.projexa.app.core.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import java.util.Locale

sealed interface SttState {
    data object Idle : SttState
    data class Listening(val partial: String = "") : SttState
    data class Error(val message: String) : SttState
}

/** Android SpeechRecognizer wrapper. Must be used from the main thread. Needs RECORD_AUDIO. */
class SpeechToTextController(
    private val context: Context,
    private val onFinalResult: (String) -> Unit,
    private val onState: (SttState) -> Unit,
) {
    private var recognizer: SpeechRecognizer? = null

    fun isAvailable() = SpeechRecognizer.isRecognitionAvailable(context)

    fun start(languageTag: String) {
        stop()
        if (!isAvailable()) {
            onState(SttState.Error("Speech recognition is not available on this phone."))
            return
        }
        val r = SpeechRecognizer.createSpeechRecognizer(context)
        recognizer = r
        r.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) = onState(SttState.Listening())
            override fun onPartialResults(partialResults: Bundle?) {
                val t = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                onState(SttState.Listening(t))
            }
            override fun onResults(results: Bundle?) {
                val t = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                onState(SttState.Idle)
                if (t.isNotBlank()) onFinalResult(t) else onState(SttState.Error("Did not catch that. Please try again."))
            }
            override fun onError(error: Int) = onState(SttState.Error(describe(error)))
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        r.startListening(intent)
    }

    fun stop() {
        recognizer?.destroy()
        recognizer = null
    }

    private fun describe(code: Int) = when (code) {
        SpeechRecognizer.ERROR_NO_MATCH -> "Did not catch that. Please try again."
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech heard. Please try again."
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission is needed."
        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT, SpeechRecognizer.ERROR_SERVER ->
            "This language may need an internet connection for speech recognition."
        SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED, SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE ->
            "This language is not available for speech recognition on this phone."
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech recognizer is busy. Try again."
        else -> "Speech recognition error ($code)."
    }
}

/** Android TextToSpeech wrapper. */
class TextToSpeechController(context: Context, private val onReady: (Boolean) -> Unit) {
    private var tts: TextToSpeech? = null
    var ready = false
        private set

    init {
        tts = TextToSpeech(context) { status ->
            ready = status == TextToSpeech.SUCCESS
            onReady(ready)
        }
    }

    /** @return null on success, otherwise a message explaining why it could not speak. */
    fun speak(text: String, languageTag: String, rate: Float = 1f): String? {
        val t = tts ?: return "Text-to-speech is not available."
        if (!ready) return "Text-to-speech is still starting."
        val result = t.setLanguage(Locale.forLanguageTag(languageTag))
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            return "The voice for this language is not installed. Install it in Android text-to-speech settings."
        }
        t.setSpeechRate(rate)
        t.speak(text, TextToSpeech.QUEUE_FLUSH, null, "projexa-msg")
        return null
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
