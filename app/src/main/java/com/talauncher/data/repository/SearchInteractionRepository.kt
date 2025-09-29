package com.talauncher.data.repository

import com.talauncher.data.database.SearchInteractionDao
import com.talauncher.data.model.SearchInteractionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.collections.buildList

class SearchInteractionRepository(
    private val interactionDao: SearchInteractionDao,
    private val timeProvider: () -> Long = System::currentTimeMillis
) {
    enum class ContactAction(val storageKey: String) {
        CALL("call"),
        MESSAGE("message"),
        WHATSAPP("whatsapp"),
        OPEN("open")
    }

    data class SearchInteractionSnapshot(
        val appLastUsed: Map<String, Long>,
        val contactLastUsed: Map<String, Long>
    ) {
        companion object {
            val EMPTY = SearchInteractionSnapshot(emptyMap(), emptyMap())
        }
    }

    suspend fun recordAppLaunch(packageName: String) {
        val interaction = SearchInteractionEntity(
            itemKey = buildAppKey(packageName),
            lastUsedAt = timeProvider()
        )
        withContext(Dispatchers.IO) {
            interactionDao.upsertInteraction(interaction)
        }
    }

    suspend fun recordContactAction(contactId: String, action: ContactAction) {
        val interaction = SearchInteractionEntity(
            itemKey = buildContactKey(contactId, action),
            lastUsedAt = timeProvider()
        )
        withContext(Dispatchers.IO) {
            interactionDao.upsertInteraction(interaction)
        }
    }

    suspend fun getLastUsedSnapshot(
        appPackages: Collection<String>,
        contactIds: Collection<String>
    ): SearchInteractionSnapshot {
        if (appPackages.isEmpty() && contactIds.isEmpty()) {
            return SearchInteractionSnapshot.EMPTY
        }

        val keys = buildList {
            appPackages.forEach { packageName ->
                add(buildAppKey(packageName))
            }
            contactIds.forEach { contactId ->
                ContactAction.values().forEach { action ->
                    add(buildContactKey(contactId, action))
                }
            }
        }

        if (keys.isEmpty()) {
            return SearchInteractionSnapshot.EMPTY
        }

        val interactions = withContext(Dispatchers.IO) {
            if (keys.size <= INTERACTION_QUERY_BATCH_SIZE) {
                interactionDao.getInteractions(keys)
            } else {
                keys
                    .chunked(INTERACTION_QUERY_BATCH_SIZE)
                    .flatMap { batch -> interactionDao.getInteractions(batch) }
            }
        }

        if (interactions.isEmpty()) {
            return SearchInteractionSnapshot.EMPTY
        }

        val appLastUsed = mutableMapOf<String, Long>()
        val contactLastUsed = mutableMapOf<String, Long>()

        interactions.forEach { entity ->
            val segments = entity.itemKey.split(DELIMITER)
            if (segments.isEmpty()) return@forEach
            when (segments.first()) {
                APP_PREFIX -> {
                    val packageName = segments.getOrNull(1) ?: return@forEach
                    val existing = appLastUsed[packageName]
                    if (existing == null || entity.lastUsedAt > existing) {
                        appLastUsed[packageName] = entity.lastUsedAt
                    }
                }
                CONTACT_PREFIX -> {
                    val contactId = segments.getOrNull(2) ?: return@forEach
                    val existing = contactLastUsed[contactId]
                    if (existing == null || entity.lastUsedAt > existing) {
                        contactLastUsed[contactId] = entity.lastUsedAt
                    }
                }
            }
        }

        return SearchInteractionSnapshot(appLastUsed, contactLastUsed)
    }

    private fun buildAppKey(packageName: String): String =
        listOf(APP_PREFIX, packageName).joinToString(DELIMITER)

    private fun buildContactKey(contactId: String, action: ContactAction): String =
        listOf(CONTACT_PREFIX, action.storageKey, contactId).joinToString(DELIMITER)

    companion object {
        private const val APP_PREFIX = "app"
        private const val CONTACT_PREFIX = "contact"
        private const val DELIMITER = "|"
        private const val INTERACTION_QUERY_BATCH_SIZE = 900
    }
}
