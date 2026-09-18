package com.mfslogger.app.parser

/**
 * Result of successfully parsing a payment-notification SMS.
 */
data class ParsedPayment(
    val payerNumber: String,
    val transactionId: String,
    val amount: String,
    val rawMessage: String,
    val senderId: String
)

/**
 * Parses "money received" SMS bodies from Bangladeshi MFS providers
 * (bKash, Nagad, Rocket, Upay, etc). These messages differ slightly by
 * provider but share the same three data points: the payer's number,
 * a transaction ID, and an amount. Rather than hard-coding one exact
 * template per provider, this uses a set of tolerant regexes tried in
 * order, plus a generic fallback so new/unlisted providers still work
 * as long as the message contains a phone number, an amount, and an
 * alphanumeric transaction id near a recognizable label.
 *
 * NOTE ON SCOPE: this only reads and reformats data already visible in
 * a payment-confirmation SMS the user's own phone received (i.e. money
 * that already arrived). It intentionally never attempts to verify
 * PINs, initiate transfers, or interact with the MFS account in any way.
 */
object SmsParser {

    // Amount: "Tk 1,250.00", "Tk. 500", "BDT 200.50"
    private val amountRegex = Regex(
        """(?:Tk\.?|BDT)\s*([\d,]+(?:\.\d{1,2})?)""",
        RegexOption.IGNORE_CASE
    )

    // Payer's phone number: "from 01712345678", "from 8801712345678"
    private val payerRegex = Regex(
        """from\s+(\+?88)?(01[3-9]\d{8})""",
        RegexOption.IGNORE_CASE
    )

    // Transaction ID under several common labels
    private val txnIdRegex = Regex(
        """(?:TrxID|Transaction\s?ID|TxnID|Txn\s?ID|Ref(?:erence)?\s?(?:No|ID)?)[:\s]+([A-Za-z0-9]{4,})""",
        RegexOption.IGNORE_CASE
    )

    // Only treat the message as a "money received" notification, not a
    // payment/withdrawal/cash-out, so we don't log outgoing transactions
    // as incoming ones.
    private val receivedKeywordRegex = Regex(
        """received|credited|you\s+have\s+got""",
        RegexOption.IGNORE_CASE
    )

    fun parse(senderId: String, body: String): ParsedPayment? {
        if (!receivedKeywordRegex.containsMatchIn(body)) return null

        val amount = amountRegex.find(body)?.groupValues?.get(1)?.replace(",", "") ?: return null
        val payer = payerRegex.find(body)?.groupValues?.get(2) ?: extractAnyBdNumber(body) ?: return null
        val txnId = txnIdRegex.find(body)?.groupValues?.get(1) ?: return null

        return ParsedPayment(
            payerNumber = payer,
            transactionId = txnId,
            amount = amount,
            rawMessage = body,
            senderId = senderId
        )
    }

    /** Fallback: grab the first standalone 11-digit BD mobile number in the text. */
    private fun extractAnyBdNumber(body: String): String? {
        val generic = Regex("""(?<!\d)(01[3-9]\d{8})(?!\d)""")
        return generic.find(body)?.value
    }
}
