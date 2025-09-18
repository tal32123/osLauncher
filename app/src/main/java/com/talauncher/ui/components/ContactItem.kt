package com.talauncher.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
    onMessage: () -> Unit
) {
    var showActions by remember { mutableStateOf(false) }

    PrimerCard(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { onCall() },
                onLongClick = { showActions = true }
            ),
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

            Surface(
                color = PrimerGreen.copy(alpha = 0.1f),
                shape = PrimerShapes.small,
                border = BorderStroke(1.dp, PrimerGreen.copy(alpha = 0.3f))
            ) {
                Text(
                    text = "Contact",
                    modifier = Modifier.padding(
                        horizontal = PrimerSpacing.xs,
                        vertical = 2.dp
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = PrimerGreen
                )
            }
        }
    }

    if (showActions) {
        ContactActionDialog(
            contact = contact,
            onDismiss = { showActions = false },
            onCall = {
                onCall()
                showActions = false
            },
            onMessage = {
                onMessage()
                showActions = false
            }
        )
    }
}

@Composable
fun ContactActionDialog(
    contact: ContactInfo,
    onDismiss: () -> Unit,
    onCall: () -> Unit,
    onMessage: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = contact.name,
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(PrimerSpacing.sm)
            ) {
                Text(
                    text = "Choose an action:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (contact.phoneNumber != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(PrimerSpacing.sm)
                    ) {
                        Button(
                            onClick = onCall,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PrimerGreen
                            )
                        ) {
                            Text("Call")
                        }

                        OutlinedButton(
                            onClick = onMessage,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = PrimerGreen
                            ),
                            border = BorderStroke(1.dp, PrimerGreen)
                        ) {
                            Text("Message")
                        }
                    }
                } else {
                    Text(
                        text = "No phone number available",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = PrimerShapes.medium
    )
}