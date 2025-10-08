package com.example.voicesample

import android.os.Bundle
import android.Manifest
import android.speech.RecognitionListener
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import java.util.Locale

class MainActivity : ComponentActivity(), RecognitionListener {
    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var startButton: Button
    private lateinit var resultTextView: TextView
    private val RECORD_AUDIO_PERMISSION_REQUEST_CODE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContentView(R.layout.activity_main)
        startButton = findViewById(R.id.start_speech_button)
        resultTextView = findViewById(R.id.result_text_view)

        // request permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), RECORD_AUDIO_PERMISSION_REQUEST_CODE)
        } else {
            initializeSpeechRecognizer()
        }

        startButton.setOnClickListener {
            startListening()
        }
    }
    private fun initializeSpeechRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(this, "Couldn't use speech recognizer", Toast.LENGTH_LONG).show()
            return
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
            setRecognitionListener(this@MainActivity)
        }
    }

    private fun startListening() {
        if (!::speechRecognizer.isInitialized) {
            Toast.makeText(this, "Initializing...", Toast.LENGTH_SHORT).show()
            return
        }

        resultTextView.text = "話してください..."

        // 3. 認識Intentの作成と設定
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            // 日本語を設定
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.JAPANESE.toString())
            // 認識結果を複数取得
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)

            // オフライン認識を優先する設定 (API 23以降)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            }
        }

        // 4. 音声認識の開始
        speechRecognizer.startListening(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::speechRecognizer.isInitialized) {
            speechRecognizer.destroy()
        }
    }

    override fun onReadyForSpeech(params: Bundle?) {
        // 認識準備完了
        startButton.isEnabled = false
        resultTextView.text = "どうぞ、お話しください..."
    }

    override fun onResults(results: Bundle?) {
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (!matches.isNullOrEmpty()) {
            val bestResult = matches[0]
            resultTextView.text = "Result: $bestResult\n(候補数: ${matches.size})"
        } else {
            resultTextView.text = "Failed recognizing"
        }
        startButton.isEnabled = true
    }

    override fun onError(error: Int) {
        val errorText = when(error) {
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "NETWORK_TIMEOUT"
            SpeechRecognizer.ERROR_NETWORK -> "NETWORK_ERROR"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "PERMISSION_ERROR"
            SpeechRecognizer.ERROR_CLIENT -> "CLIENT ERROR"
            SpeechRecognizer.ERROR_NO_MATCH -> "NO_MATCH"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "RECOGNIZER_BUSY"
            else -> "OTHER: $error"
        }
        resultTextView.text = "Error occurred: $errorText"
        startButton.isEnabled = true
    }
    // callback
    override fun onBeginningOfSpeech() {}
    override fun onBufferReceived(buffer: ByteArray?) {
        // Do nothing
    }

    override fun onEndOfSpeech() {}
    override fun onPartialResults(partialResults: Bundle?) {}
    override fun onEvent(eventType: Int, params: Bundle?) {}
    override fun onRmsChanged(rmsdB: Float) {}


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == RECORD_AUDIO_PERMISSION_REQUEST_CODE) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                initializeSpeechRecognizer()
            } else {
                Toast.makeText(this, "マイクの許可がないと音声認識は利用できません。", Toast.LENGTH_LONG).show()
                startButton.isEnabled = false
            }
        }
    }
}
