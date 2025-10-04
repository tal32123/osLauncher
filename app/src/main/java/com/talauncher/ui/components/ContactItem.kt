package com.talauncher.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.talauncher.ui.theme.*
import com.talauncher.utils.ContactInfo

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ContactItem(
    contact: ContactInfo,
    layout: com.talauncher.data.model.AppSectionLayoutOption = com.talauncher.data.model.AppSectionLayoutOption.LIST,
    onCall: () -> Unit,
    onMessage: () -> Unit,
    onWhatsApp: () -> Unit,
    onOpenContact: () -> Unit,
    showPhoneAction: Boolean,
    showMessageAction: Boolean,
    showWhatsAppAction: Boolean
) {
    val isGridMode = layout == com.talauncher.data.model.AppSectionLayoutOption.GRID_3 ||
                     layout == com.talauncher.data.model.AppSectionLayoutOption.GRID_4
    PrimerCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenContact() },
        colors = CardDefaults.cardColors(
            containerColor = PrimerGreen.copy(alpha = 0.05f)
        ),
        border = BorderStroke(1.dp, PrimerGreen.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PrimerSpacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Contact initial - hide in grid mode if contact has a name
            if (!isGridMode || contact.name.isBlank()) {
                Surface(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape),
                    color = PrimerGreen.copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, PrimerGreen.copy(alpha = 0.3f))
                ) {
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = contact.name.firstOrNull()?.uppercase() ?: "?",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = PrimerGreen
                        )
                    }
                }

                Spacer(modifier = Modifier.width(PrimerSpacing.md))
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = contact.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Hide phone number in grid mode if contact has a name
                if (!isGridMode || contact.name.isBlank()) {
                    contact.phoneNumber?.let { phone ->
                        Text(
                            text = phone,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Row {
                if (showPhoneAction) {
                    IconButton(onClick = onCall) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "Call",
                            tint = PrimerGreen
                        )
                    }
                }
                if (showMessageAction) {
                    IconButton(onClick = onMessage) {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = "Message",
                            tint = PrimerGreen
                        )
                    }
                }
                if (showWhatsAppAction) {
                    IconButton(onClick = onWhatsApp) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "WhatsApp",
                            tint = PrimerGreen
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ContactGridItem(
    contact: ContactInfo,
    onCall: () -> Unit,
    onMessage: () -> Unit,
    onWhatsApp: () -> Unit,
    onOpenContact: () -> Unit,
    showPhoneAction: Boolean,
    showMessageAction: Boolean,
    showWhatsAppAction: Boolean
) {
    var showActionsMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {
                    // If only one action is available, use it directly
                    when {
                        showPhoneAction && !showMessageAction && !showWhatsAppAction -> onCall()
                        !showPhoneAction && showMessageAction && !showWhatsAppAction -> onMessage()
                        !showPhoneAction && !showMessageAction && showWhatsAppAction -> onWhatsApp()
                        else -> showActionsMenu = true
                    }
                },
                onLongClick = { onOpenContact() }
            )
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Grid mode: show only the contact name (no icon or phone number)
        Text(
            text = contact.name,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }

    // Actions menu dropdown
    if (showActionsMenu) {
        DropdownMenu(
            expanded = showActionsMenu,
            onDismissRequest = { showActionsMenu = false }
        ) {
            if (showPhoneAction) {
                DropdownMenuItem(
                    text = { Text("Call") },
                    onClick = {
                        showActionsMenu = false
                        onCall()
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = null,
                            tint = PrimerGreen
                        )
                    }
                )
            }
            if (showMessageAction) {
                DropdownMenuItem(
                    text = { Text("Message") },
                    onClick = {
                        showActionsMenu = false
                        onMessage()
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = null,
                            tint = PrimerGreen
                        )
                    }
                )
            }
            if (showWhatsAppAction) {
                DropdownMenuItem(
                    text = { Text("WhatsApp") },
                    onClick = {
                        showActionsMenu = false
                        onWhatsApp()
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = null,
                            tint = PrimerGreen
                        )
                    }
                )
            }
            DropdownMenuItem(
                text = { Text("Open Contact") },
                onClick = {
                    showActionsMenu = false
                    onOpenContact()
                }
            )
        }
    }
}
