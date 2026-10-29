package ru.meowmeow.notes.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ИИ-распознавание речи: системный движок телефона переводит голос в текст.
 * Работает на том же стеке, что и ассистент устройства, поэтому сеть и
 * ключи не нужны — всё решает система.
 *
 * Частичные результаты транслируются в [state], финальный — в [onResult].
 */
class VoiceRecognizer(context: Context, private val onResult: (String) -> Unit) {

    data class VoiceState(
        val listening: Boolean = false,
        val partial: String = "",
        val error: String? = null,
    )

    private val appContext = context.applicationContext

    private val _state = MutableStateFlow(VoiceState())
    val state: StateFlow<VoiceState> = _state.asStateFlow()

    private var recognizer: SpeechRecognizer? = null

    /** Есть ли на устройстве сервис распознавания. Нет — кнопка микрофона не показывается. */
    val available: Boolean = SpeechRecognizer.isRecognitionAvailable(appContext)

    fun start() {
        if (!available || _state.value.listening) return
        stopRecognizer()

        val recognizer = SpeechRecognizer.createSpeechRecognizer(appContext)
        this.recognizer = recognizer
        _state.value = VoiceState(listening = true)

        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                _state.value = VoiceState(listening = true)
            }

            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                _state.value = _state.value.copy(listening = false)
            }

            override fun onError(error: Int) {
                val message = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH -> "Речи не услышал, попробуйте ещё раз"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Тишина — попробуйте ещё раз"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Нужно разрешение на микрофон"
                    SpeechRecognizer.ERROR_NETWORK -> "Сервис распознавания недоступен"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Сервис распознавания не ответил"
                    else -> "Распознавание прервано"
                }
                _state.value = VoiceState(error = message)
            }

            override fun onResults(results: Bundle?) {
                val text = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    .orEmpty()
                    .trim()
                _state.value = VoiceState()
                if (text.isNotEmpty()) onResult(text)
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val partial = partialResults
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    .orEmpty()
                _state.value = _state.value.copy(partial = partial)
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        recognizer.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(
                RecognizerIntent.EXTRA_PROMPT,
                "Говорите — слова превратятся в текст заметки",
            )
        })
    }

    fun stop() {
        stopRecognizer()
        _state.value = VoiceState()
    }

    private fun stopRecognizer() {
        recognizer?.stopListening()
        recognizer?.destroy()
        recognizer = null
    }
}
