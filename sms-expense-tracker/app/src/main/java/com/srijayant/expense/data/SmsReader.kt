package com.srijayant.expense.data

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.Telephony
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class SmsMessage(
    val id: String,
    val address: String,
    val body: String,
    val date: Long
)

class SmsReader(private val context: Context) {

    suspend fun readInbox(limit: Int = 2000): List<SmsMessage> = withContext(Dispatchers.IO) {
        val resolver: ContentResolver = context.contentResolver
        val uri: Uri = Telephony.Sms.Inbox.CONTENT_URI
        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE
        )

        val messages = mutableListOf<SmsMessage>()
        var cursor: Cursor? = null
        try {
            cursor = resolver.query(
                uri,
                projection,
                null,
                null,
                "${Telephony.Sms.DATE} DESC"
            )
            cursor?.let { c ->
                val idIdx = c.getColumnIndexOrThrow(Telephony.Sms._ID)
                val addrIdx = c.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
                val bodyIdx = c.getColumnIndexOrThrow(Telephony.Sms.BODY)
                val dateIdx = c.getColumnIndexOrThrow(Telephony.Sms.DATE)

                var count = 0
                while (c.moveToNext() && count < limit) {
                    messages += SmsMessage(
                        id = c.getString(idIdx),
                        address = c.getString(addrIdx).orEmpty(),
                        body = c.getString(bodyIdx).orEmpty(),
                        date = c.getLong(dateIdx)
                    )
                    count++
                }
            }
        } finally {
            cursor?.close()
        }
        messages
    }
}
