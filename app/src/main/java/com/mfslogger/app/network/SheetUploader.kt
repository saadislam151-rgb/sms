package com.mfslogger.app.network

import android.content.Context
import android.util.Log
import com.mfslogger.app.Prefs
import com.mfslogger.app.parser.ParsedPayment
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

/**
 * Sends a parsed payment to the Google Apps Script Web App, which then
 * appends it as a row in the target Google Sheet. Uses a plain
 * HttpURLConnection on a background thread so the app has zero extra
 * networking dependencies.
 */
object SheetUploader {

    private const val TAG = "SheetUploader"
    private val executor = Executors.newSingleThreadExecutor()

    fun upload(context: Context, payment: ParsedPayment, onResult: (Boolean, String) -> Unit = { _, _ -> }) {
        val scriptUrl = Prefs.getScriptUrl(context)
        if (scriptUrl.isBlank()) {
            Log.w(TAG, "No script URL configured; dropping payment ${payment.transactionId}")
            onResult(false, "No Apps Script URL configured")
            return
        }

        executor.execute {
            try {
                val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())

                val json = JSONObject().apply {
                    put("secret", Prefs.getSecret(context))
                    put("senderId", payment.senderId)
                    put("payerNumber", payment.payerNumber)
                    put("transactionId", payment.transactionId)
                    put("amount", payment.amount)
                    put("timestamp", timestamp)
                }

                val url = URL(scriptUrl)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                conn.connectTimeout = 15000
                conn.readTimeout = 15000
                // Apps Script web apps issue a redirect (302) from the
                // exec URL to a googleusercontent URL; follow it.
                conn.instanceFollowRedirects = true

                conn.outputStream.use { it.write(json.toString().toByteArray(Charsets.UTF_8)) }

                val code = conn.responseCode
                val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                val responseText = stream?.bufferedReader()?.use { it.readText() } ?: ""
                conn.disconnect()

                val success = code in 200..299
                Log.i(TAG, "Upload ${payment.transactionId}: HTTP $code -> $responseText")
                onResult(success, responseText)
            } catch (e: Exception) {
                Log.e(TAG, "Upload failed for ${payment.transactionId}", e)
                onResult(false, e.message ?: "Unknown error")
            }
        }
    }
}
