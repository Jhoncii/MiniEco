package com.johnpena.minieco

import android.content.Context
import android.media.MediaPlayer

object AudioPlayerHelper {
    private var mediaPlayerDesecho: MediaPlayer? = null
    private var mediaPlayerColor: MediaPlayer? = null

    fun reproducir(context: Context, audioDesechoId: Int, audioColorId: Int) {
        // Detener si hay algo sonando
        detener()

        mediaPlayerDesecho = MediaPlayer.create(context, audioDesechoId)
        mediaPlayerColor = MediaPlayer.create(context, audioColorId)

        // Cuando termine el audio del desecho, arranca el del color
        mediaPlayerDesecho?.setOnCompletionListener {
            mediaPlayerColor?.start()
            it.release() // Liberar memoria
        }

        mediaPlayerColor?.setOnCompletionListener {
            it.release()
        }

        mediaPlayerDesecho?.start()
    }

    fun detener() {
        mediaPlayerDesecho?.release()
        mediaPlayerColor?.release()
        mediaPlayerDesecho = null
        mediaPlayerColor = null
    }
}