package com.urbanblade.mobile.core.media

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import java.io.File

/** Descarga autenticada; solo se comparte el PDF, nunca la URL o el token de la API. */
object MembershipCardFiles {
    suspend fun save(context: Context, body: ResponseBody): File = withContext(Dispatchers.IO) {
        val folder = File(context.cacheDir, "membership-cards").apply { mkdirs() }
        val file = File.createTempFile("tarjeta-", ".pdf", folder)
        try {
            body.use { response ->
                response.byteStream().use { input ->
                    file.outputStream().use { output ->
                        val buffer = ByteArray(8192)
                        var total = 0
                        while (true) {
                            val count = input.read(buffer)
                            if (count < 0) break
                            total += count
                            require(total <= 10 * 1024 * 1024) { "El documento es demasiado grande." }
                            output.write(buffer, 0, count)
                        }
                    }
                }
            }
            require(file.inputStream().use {
                val header = ByteArray(5)
                it.read(header) == 5 && String(header, Charsets.US_ASCII) == "%PDF-"
            }) {
                "No recibimos una tarjeta válida. Intenta de nuevo."
            }
            file
        } catch (e: Exception) {
            file.delete()
            throw e
        }
    }

    fun open(context: Context, file: File): Boolean = runCatching {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.documents", file)
        context.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/pdf")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    }.getOrDefault(false)
}
