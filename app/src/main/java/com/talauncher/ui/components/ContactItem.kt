package com.talauncher.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.talauncher.ui.theme.*
import com.talauncher.utils.ContactInfo

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ContactItem(
    contact: ContactInfo,
    onCall: () -> Unit,
    onMessage: () -> Unit,
    onWhatsApp: () -> Unit,
    onOpenContact: () -> Unit,
    showPhoneAction: Boolean,
    showMessageAction: Boolean,
    showWhatsAppAction: Boolean
) {
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
            // Contact initial
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
                            imageVector = Icons.Default.Send,
                            contentDescription = "WhatsApp",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}