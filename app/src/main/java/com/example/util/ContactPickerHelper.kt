package com.example.util

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract

object ContactPickerHelper {

    data class ContactData(
        val name: String,
        val phoneNumber: String
    )

    fun extractContactDetails(context: Context, contactUri: Uri): ContactData? {
        var contactName = ""
        var contactNumber = ""
        val contentResolver = context.contentResolver

        try {
            // First try querying phone number directly if uri points to data / phone
            val phoneCursor: Cursor? = contentResolver.query(
                contactUri,
                null,
                null,
                null,
                null
            )

            phoneCursor?.use { cursor ->
                if (cursor.moveToFirst()) {
                    // Check if DISPLAY_NAME exists
                    val nameIdx = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                    if (nameIdx != -1) {
                        contactName = cursor.getString(nameIdx) ?: ""
                    }

                    // Check if NUMBER column is directly available (e.g. from CommonDataKinds.Phone)
                    val numberIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    if (numberIdx != -1) {
                        contactNumber = cursor.getString(numberIdx) ?: ""
                    }

                    // If phone number is not directly in the cursor, check by contact ID
                    val idIdx = cursor.getColumnIndex(ContactsContract.Contacts._ID)
                    val hasPhoneIdx = cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)

                    if (contactNumber.isBlank() && idIdx != -1) {
                        val contactId = cursor.getString(idIdx)
                        val hasPhone = if (hasPhoneIdx != -1) cursor.getInt(hasPhoneIdx) else 1

                        if (hasPhone > 0 && contactId != null) {
                            val pCursor = contentResolver.query(
                                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                                "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                                arrayOf(contactId),
                                null
                            )
                            pCursor?.use { pc ->
                                if (pc.moveToFirst()) {
                                    val numCol = pc.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                                    if (numCol != -1) {
                                        contactNumber = pc.getString(numCol) ?: ""
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Clean up phone number format (keep plus and digits, remove spaces or hyphens if needed or leave clean)
        val cleanNumber = contactNumber.replace("[^0-9+]".toRegex(), " ").trim().replace("\\s+".toRegex(), " ")

        return if (contactName.isNotBlank() || cleanNumber.isNotBlank()) {
            ContactData(
                name = contactName.trim(),
                phoneNumber = cleanNumber.trim()
            )
        } else {
            null
        }
    }
}
