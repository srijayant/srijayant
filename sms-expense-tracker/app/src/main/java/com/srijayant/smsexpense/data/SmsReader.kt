package com.srijayant.smsexpense.data

import android.content.Context
import android.provider.Telephony

/**
 * Reads the device SMS inbox via the [Telephony.Sms.Inbox] content provider.
 * Requires the READ_SMS runtime permission to be granted before calling.
 */
class SmsReader(private val context: Context) {

    /**
     * Returns inbox messages received at or after [fromTimestampMillis],
     * newest first.
     */
    fun readInbox(fromTimestampMillis: Long): List<SmsMessage> {
        val messages = mutableListOf<SmsMessage>()
        val projection = arrayOf(
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE
        )
        val cursor = context.contentResolver.query(
            Telephony.Sms.Inbox.CONTENT_URI,
            projection,
            "${Telephony.Sms.DATE} >= ?",
            arrayOf(fromTimestampMillis.toString()),
            "${Telephony.Sms.DATE} DESC"
        ) ?: return emptyList()

        cursor.use {
            val addressIdx = it.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            val bodyIdx = it.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val dateIdx = it.getColumnIndexOrThrow(Telephony.Sms.DATE)
            while (it.moveToNext()) {
                messages += SmsMessage(
                    sender = it.getString(addressIdx) ?: "",
                    body = it.getString(bodyIdx) ?: "",
                    timestampMillis = it.getLong(dateIdx)
                )
            }
        }
        return messages
    }
}
