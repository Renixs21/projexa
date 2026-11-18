package com.projexa.app.presentation.communicate

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

enum class SpeechLanguage(val label: String, val tag: String) {
    ENGLISH("English", "en-IN"),
    HINDI("Hindi", "hi-IN"),
    KANNADA("Kannada", "kn-IN"),
    TAMIL("Tamil", "ta-IN"),
    TELUGU("Telugu", "te-IN"),
}

enum class Speaker { YOU, OTHER }

data class ChatMessage(val speaker: Speaker, val text: String)

class ConversationViewModel : ViewModel() {
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages

    private val _language = MutableStateFlow(SpeechLanguage.ENGLISH)
    val language: StateFlow<SpeechLanguage> = _language

    fun add(speaker: Speaker, text: String) = _messages.update { it + ChatMessage(speaker, text) }
    fun setLanguage(l: SpeechLanguage) { _language.value = l }
    fun clear() { _messages.value = emptyList() }
}
