package com.mfslogger.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.mfslogger.app.Prefs
import com.mfslogger.app.network.SheetUploader
import com.mfslogger.app.parser.SmsParser

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) return

        // A single SMS can arrive split across multiple PDU parts; join them.
        val sender = messages[0].originatingAddress ?: "unknown"
        val fullBody = messages.joinToString(separator = "") { it.messageBody ?: "" }

        if (!Prefs.isWatched(context, sender)) {
            Log.d(TAG, "Ignoring SMS from unwatched sender: $sender")
            return
        }

        val parsed = SmsParser.parse(sender, fullBody)
        if (parsed == null) {
            Log.d(TAG, "Sender $sender is watched but message didn't match a payment pattern")
            return
        }

        Log.i(TAG, "Matched payment: ${parsed.amount} from ${parsed.payerNumber}, txn=${parsed.transactionId}")
        SheetUploader.upload(context, parsed)
    }

    companion object {
        private const val TAG = "SmsReceiver"
    }
}
