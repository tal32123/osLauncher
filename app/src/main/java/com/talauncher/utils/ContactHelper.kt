package com.talauncher.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ContactInfo(
    val id: String,
    val name: String,
    val phoneNumber: String? = null
)

class ContactHelper(
    private val context: Context,
    private val permissionsHelper: PermissionsHelper
) {

    suspend fun searchContacts(query: String): List<ContactInfo> = withContext(Dispatchers.IO) {
        if (!permissionsHelper.hasContactsPermission()) {
            return@withContext emptyList()
        }

        if (query.isBlank()) {
            return@withContext emptyList()
        }

        try {
            val contacts = mutableListOf<ContactInfo>()
            val normalizedQuery = query.trim().lowercase()

            val cursor = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                ),
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?",
                arrayOf("%$normalizedQuery%"),
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
            )

            cursor?.use {
                val idIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val phoneIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

                val seenContacts = mutableSetOf<String>()

                while (it.moveToNext() && contacts.size < 5) { // Limit to 5 contacts
                    val contactId = it.getString(idIndex)
                    val name = it.getString(nameIndex)
                    val phoneNumber = it.getString(phoneIndex)

                    // Avoid duplicates by contact ID
                    if (seenContacts.add(contactId) && name != null) {
                        contacts.add(
                            ContactInfo(
                                id = contactId,
                                name = name,
                                phoneNumber = phoneNumber
                            )
                        )
                    }
                }
            }

            // Sort by relevance - exact matches first, then starts-with, then contains
            contacts.sortedWith { a, b ->
                val aName = a.name.lowercase()
                val bName = b.name.lowercase()

                when {
                    aName == normalizedQuery && bName != normalizedQuery -> -1
                    bName == normalizedQuery && aName != normalizedQuery -> 1
                    aName.startsWith(normalizedQuery) && !bName.startsWith(normalizedQuery) -> -1
                    bName.startsWith(normalizedQuery) && !aName.startsWith(normalizedQuery) -> 1
                    else -> aName.compareTo(bName)
                }
            }
        } catch (e: Exception) {
            Log.e("ContactHelper", "Error searching contacts", e)
            emptyList()
        }
    }

    fun callContact(contact: ContactInfo) {
        try {
            val phoneNumber = contact.phoneNumber
            if (phoneNumber != null) {
                if (permissionsHelper.hasCallPhonePermission()) {
                    val intent = Intent(Intent.ACTION_CALL).apply {
                        data = Uri.parse("tel:$phoneNumber")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                } else {
                    // Fallback to dialer if no permission
                    val intent = Intent(Intent.ACTION_DIAL).apply {
                        data = Uri.parse("tel:$phoneNumber")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                }
            }
        } catch (e: Exception) {
            Log.e("ContactHelper", "Error calling contact", e)
        }
    }

    fun messageContact(contact: ContactInfo) {
        try {
            val phoneNumber = contact.phoneNumber
            if (phoneNumber != null) {
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("smsto:$phoneNumber")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
        } catch (e: Exception) {
            Log.e("ContactHelper", "Error messaging contact", e)
        }
    }

    fun whatsAppContact(contact: ContactInfo) {
        try {
            val phoneNumber = contact.phoneNumber
            if (phoneNumber != null) {
                // Remove any non-digit characters and ensure proper format
                val cleanNumber = phoneNumber.replace(Regex("[^\\d]"), "")
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("https://wa.me/$cleanNumber")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
        } catch (e: Exception) {
            Log.e("ContactHelper", "Error opening WhatsApp for contact", e)
        }
    }
}