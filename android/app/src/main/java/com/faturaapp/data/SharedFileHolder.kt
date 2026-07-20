package com.faturaapp.data

import android.net.Uri

/**
 * Ponte simples entre o Intent recebido (compartilhamento de PDF por outro app)
 * e a tela de Upload. MainActivity grava aqui; UploadScreen consome uma unica vez.
 */
object SharedFileHolder {
    var pendingUri: Uri? = null

    fun consume(): Uri? {
        val uri = pendingUri
        pendingUri = null
        return uri
    }
}
