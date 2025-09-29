package com.talauncher.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.util.Log
import java.util.Locale
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
            val candidateContacts = linkedMapOf<String, ContactInfo>()
            val normalizedQuery = query.trim().lowercase()
            val projection = arrayOf(
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            )

            fun loadContacts(selection: String?, selectionArgs: Array<String>?) {
                if (candidateContacts.size >= MAX_CONTACTS_TO_EVALUATE) return
                val cursor = context.contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    projection,
                    selection,
                    selectionArgs,
                    "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} COLLATE NOCASE ASC"
                )

                cursor?.use {
                    val idIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                    val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                    val phoneIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

                    while (it.moveToNext() && candidateContacts.size < MAX_CONTACTS_TO_EVALUATE) {
                        val contactId = it.getString(idIndex) ?: continue
                        val name = it.getString(nameIndex) ?: continue
                        val phoneNumber = it.getString(phoneIndex)

                        candidateContacts.putIfAbsent(
                            contactId,
                            ContactInfo(
                                id = contactId,
                                name = name,
                                phoneNumber = phoneNumber
                            )
                        )
                    }
                }
            }

            // First load direct matches so they keep priority
            loadContacts(
                selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?",
                selectionArgs = arrayOf("%$normalizedQuery%")
            )

            // Load additional contacts for fuzzy matching until limit reached
            loadContacts(selection = null, selectionArgs = null)

            val scoredContacts = candidateContacts.values.mapNotNull { contact ->
                val score = SearchScoring.calculateRelevanceScore(query, contact.name)
                if (score > 0) {
                    contact to score
                } else {
                    null
                }
            }

            scoredContacts
                .sortedWith(
                    compareByDescending<Pair<ContactInfo, Int>> { it.second }
                        .thenBy { it.first.name.lowercase(Locale.getDefault()) }
                )
                .take(MAX_RESULTS)
                .map { it.first }
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

    fun openContact(contact: ContactInfo) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, contact.id)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("ContactHelper", "Error opening contact", e)
        }
    }

    suspend fun isWhatsAppInstalled(): Boolean = withContext(Dispatchers.IO) {
        try {
            context.packageManager.getPackageInfo("com.whatsapp", 0)
            true
        } catch (e: Exception) {
            false
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

private const val MAX_RESULTS = 5
private const val MAX_CONTACTS_TO_EVALUATE = 200
