package com.sibtain.remindmedoc.ui.screens

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sibtain.remindmedoc.model.DocumentItem
import com.sibtain.remindmedoc.ui.components.AppIcons
import com.sibtain.remindmedoc.ui.components.AttachmentPreviewCard
import com.sibtain.remindmedoc.util.ExpiryCalculator
import kotlin.math.abs

@Composable
fun DocumentDetailDialog(
    item: DocumentItem,
    nowMillis: Long,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onRenewOneYear: () -> Unit,
    onDelete: () -> Unit,
    onOpenAttachment: () -> Unit
) {
    val breakdown = ExpiryCalculator.calculate(item, nowMillis)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(item.category.accentColor.copy(alpha = 0.14f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = item.category.displayName,
                            style = MaterialTheme.typography.labelMedium,
                            color = item.category.accentColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = onEdit) {
                        Icon(imageVector = AppIcons.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = AppIcons.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = AppIcons.Close, contentDescription = "Close")
                    }
                }

                Text(
                    text = item.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold
                )

                // Big Countdown Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = breakdown.urgency.badgeBgColor),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = breakdown.urgency.label.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = breakdown.urgency.textColor
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = breakdown.headlineText,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = breakdown.urgency.textColor
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        // Live Days / Hours / Minutes / Seconds Boxes
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            TimeUnitBox(
                                value = abs(breakdown.totalDaysRemaining).toString(),
                                unit = if (breakdown.totalDaysRemaining < 0) "DAYS AGO" else "DAYS",
                                color = breakdown.urgency.textColor
                            )
                            TimeUnitBox(
                                value = breakdown.hoursPart.toString().padStart(2, '0'),
                                unit = "HOURS",
                                color = breakdown.urgency.textColor
                            )
                            TimeUnitBox(
                                value = breakdown.minutesPart.toString().padStart(2, '0'),
                                unit = "MINS",
                                color = breakdown.urgency.textColor
                            )
                            TimeUnitBox(
                                value = breakdown.secondsPart.toString().padStart(2, '0'),
                                unit = "SECS",
                                color = breakdown.urgency.textColor
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        // Progress Bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.Black.copy(alpha = 0.1f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(breakdown.remainingFraction)
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(breakdown.urgency.textColor)
                            )
                        }
                    }
                }

                // Details Table
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (item.issuer.isNotBlank()) {
                            DetailRow("Provider / Issuer", item.issuer)
                        }
                        if (item.documentNumber.isNotBlank()) {
                            DetailRow("Policy / Doc No.", item.documentNumber)
                        }
                        DetailRow("Start / Issue Date", ExpiryCalculator.formatDate(item.issueDateMillis))
                        DetailRow("Expiry / Renewal Date", ExpiryCalculator.formatDate(item.expiryDateMillis))
                        DetailRow("Renewal Alert Window", "${item.reminderDaysBefore} days before expiry")
                        if (item.renewalCost.isNotBlank()) {
                            DetailRow("Renewal Cost / Premium", item.renewalCost)
                        }
                        if (item.notes.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Notes",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = item.notes,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                // Attached File Section
                if (item.attachmentInternalPath != null && item.attachmentFileName != null) {
                    Text(
                        text = "Attached Insurance / Document File",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    AttachmentPreviewCard(
                        fileName = item.attachmentFileName,
                        internalPath = item.attachmentInternalPath,
                        mimeType = item.attachmentMimeType,
                        sizeBytes = item.attachmentSizeBytes,
                        onOpenExternally = onOpenAttachment
                    )
                }

                // Quick Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onEdit,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = AppIcons.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Edit")
                    }
                    Button(
                        onClick = onRenewOneYear,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = AppIcons.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Renew +1 Yr")
                    }
                }
            }
        }
    }
}

@Composable
private fun TimeUnitBox(value: String, unit: String, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.75f))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = color
        )
        Text(
            text = unit,
            style = MaterialTheme.typography.labelSmall,
            color = color.copy(alpha = 0.8f)
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}
