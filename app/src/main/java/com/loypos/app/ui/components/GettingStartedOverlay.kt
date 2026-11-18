package com.loypos.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.loypos.app.ui.theme.LoyBorder
import com.loypos.app.ui.theme.LoyGreen
import com.loypos.app.ui.theme.LoyGreenLight
import com.loypos.app.ui.theme.LoySurface
import com.loypos.app.ui.theme.LoyTextSecondary
import com.loypos.app.ui.theme.accessibleTouchTarget

data class WalkthroughStep(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val tag: String,
    val actionLabel: String? = null,
    val onAction: (() -> Unit)? = null,
)

@Composable
fun GettingStartedOverlay(
    onDismiss: () -> Unit,
    onNavigateToInventory: () -> Unit,
    onNavigateToCheckout: () -> Unit,
) {
    var currentStep by remember { mutableIntStateOf(0) }

    val steps = listOf(
        WalkthroughStep(
            title = "Welcome to LoyPOS Rental",
            description = "Offline-first Event Tool & Equipment Rental POS built for mobile phones and desktop stations. Rent tents, chairs, lighting, sound, and staging with full return tracking.",
            icon = Icons.Outlined.Storefront,
            tag = "step_welcome",
        ),
        WalkthroughStep(
            title = "Event Equipment Catalog",
            description = "Manage tents, seating, tables, lighting, sound systems, and generators with daily rental rates and live fleet availability.",
            icon = Icons.Outlined.Inventory2,
            tag = "step_inventory",
            actionLabel = "Open Equipment",
            onAction = {
                onDismiss()
                onNavigateToInventory()
            }
        ),
        WalkthroughStep(
            title = "Booking Rentals & Returns",
            description = "Pick equipment, set rental duration in days, add event venue and security deposit, then mark items returned when they come back.",
            icon = Icons.Outlined.PointOfSale,
            tag = "step_checkout",
            actionLabel = "Open Rent Desk",
            onAction = {
                onDismiss()
                onNavigateToCheckout()
            }
        ),
        WalkthroughStep(
            title = "Cloud Sync & Desktop Ready",
            description = "Track overdue rentals, shift cash drawers, PDF rental receipts, and sync across mobile and desktop stations via Plesk or JSON backup.",
            icon = Icons.Outlined.CloudSync,
            tag = "step_sync",
        ),
    )

    val step = steps[currentStep]

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = LoySurface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier.fillMaxWidth().padding(16.dp).testTag("getting_started_dialog"),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Header Icon
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(LoyGreenLight),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = step.icon,
                        contentDescription = null,
                        tint = LoyGreen,
                        modifier = Modifier.size(32.dp),
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Title
                Text(
                    text = step.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Description
                Text(
                    text = step.description,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = LoyTextSecondary,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Optional Action Button (e.g. Go to Inventory / Checkout)
                if (step.actionLabel != null && step.onAction != null) {
                    Button(
                        onClick = step.onAction,
                        colors = ButtonDefaults.buttonColors(containerColor = LoyGreen),
                        modifier = Modifier.fillMaxWidth().accessibleTouchTarget(48.dp).testTag("walkthrough_action_btn"),
                    ) {
                        Text(step.actionLabel, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Step indicators
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    steps.indices.forEach { idx ->
                        val active = idx == currentStep
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .size(if (active) 24.dp else 8.dp, 8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (active) LoyGreen else LoyBorder),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Bottom Navigation Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (currentStep > 0) {
                        OutlinedButton(
                            onClick = { currentStep-- },
                            modifier = Modifier.accessibleTouchTarget(48.dp).testTag("walkthrough_prev_btn"),
                        ) {
                            Text("Back")
                        }
                    } else {
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.accessibleTouchTarget(48.dp).testTag("walkthrough_skip_btn"),
                        ) {
                            Text("Skip", color = LoyTextSecondary)
                        }
                    }

                    if (currentStep < steps.size - 1) {
                        Button(
                            onClick = { currentStep++ },
                            colors = ButtonDefaults.buttonColors(containerColor = LoyGreen),
                            modifier = Modifier.accessibleTouchTarget(48.dp).testTag("walkthrough_next_btn"),
                        ) {
                            Text("Next")
                        }
                    } else {
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(containerColor = LoyGreen),
                            modifier = Modifier.accessibleTouchTarget(48.dp).testTag("walkthrough_finish_btn"),
                        ) {
                            Text("Get Started!")
                        }
                    }
                }
            }
        }
    }
}
