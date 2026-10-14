package com.example.intervaltimer2.audio

import android.media.AudioManager
import android.media.ToneGenerator
import android.util.Log

class SoundManager {

    private var toneGenerator: ToneGenerator? = try {
        ToneGenerator(AudioManager.STREAM_MUSIC, 100)
    } catch (e: Exception) {
        Log.e("SoundManager", "ToneGenerator init error", e)
        null
    }

    fun playBeat() {
        toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 40)
    }

    fun playCountdown() {
        toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 80)
    }

    fun playStepChange() {
        toneGenerator?.startTone(ToneGenerator.TONE_PROP_PROMPT, 250)
    }

    fun release() {
        toneGenerator?.release()
        toneGenerator = null
    }
}
