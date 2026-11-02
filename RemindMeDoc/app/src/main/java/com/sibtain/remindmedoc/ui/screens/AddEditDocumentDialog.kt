package com.sibtain.remindmedoc.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sibtain.remindmedoc.data.CopiedAttachment
import com.sibtain.remindmedoc.model.DocumentCategory
import com.sibtain.remindmedoc.model.DocumentItem
import com.sibtain.remindmedoc.ui.components.AppIcons
import com.sibtain.remindmedoc.ui.components.AttachmentPreviewCard
import com.sibtain.remindmedoc.util.ExpiryCalculator
import java.util.UUID
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditDocumentDialog(
    initialItem: DocumentItem?,
    onCopyAttachment: (Uri) -> CopiedAttachment?,
    onDismiss: () -> Unit,
    onSave: (DocumentItem) -> Unit
) {
    val now = System.currentTimeMillis()
    val defaultExpiry = now + TimeUnit.DAYS.toMillis(30)

    var title by remember { mutableStateOf(initialItem?.title ?: "") }
    var category by remember { mutableStateOf(initialItem?.category ?: DocumentCategory.VEHICLE_INSURANCE) }
    var issuer by remember { mutableStateOf(initialItem?.issuer ?: "") }
    var documentNumber by remember { mutableStateOf(initialItem?.documentNumber ?: "") }
    var issueDateMillis by remember { mutableLongStateOf(initialItem?.issueDateMillis ?: now) }
    var expiryDateMillis by remember { mutableLongStateOf(initialItem?.expiryDateMillis ?: defaultExpiry) }
    var reminderDaysBefore by remember { mutableIntStateOf(initialItem?.reminderDaysBefore ?: 30) }
    var renewalCost by remember { mutableStateOf(initialItem?.renewalCost ?: "") }
    var notes by remember { mutableStateOf(initialItem?.notes ?: "") }

    var attachmentFileName by remember { mutableStateOf(initialItem?.attachmentFileName) }
    var attachmentInternalPath by remember { mutableStateOf(initialItem?.attachmentInternalPath) }
    var attachmentMimeType by remember { mutableStateOf(initialItem?.attachmentMimeType) }
    var attachmentSizeBytes by remember { mutableLongStateOf(initialItem?.attachmentSizeBytes ?: 0L) }

    var showIssueDatePicker by remember { mutableStateOf(false) }
    var showExpiryDatePicker by remember { mutableStateOf(false) }
    var titleError by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val copied = onCopyAttachment(uri)
            if (copied != null) {
                attachmentFileName = copied.displayName
                attachmentInternalPath = copied.internalPath
                attachmentMimeType = copied.mimeType
                attachmentSizeBytes = copied.sizeBytes
                if (title.isBlank()) {
                    title = copied.displayName.substringBeforeLast(".")
                        .replace("_", " ")
                        .replace("-", " ")
                }
            }
        }
    }

    val previewItem = DocumentItem(
        id = initialItem?.id ?: "preview",
        title = title.ifBlank { "Document Preview" },
        category = category,
        issuer = issuer,
        documentNumber = documentNumber,
        issueDateMillis = issueDateMillis,
        expiryDateMillis = expiryDateMillis,
        reminderDaysBefore = reminderDaysBefore
    )
    val liveBreakdown = ExpiryCalculator.calculate(previewItem)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header Bar
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = AppIcons.Close,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (initialItem == null) "Add Insurance / Document" else "Edit Document",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                if (title.isBlank()) {
                                    titleError = true
                                    return@Button
                                }
                                onSave(
                                    DocumentItem(
                                        id = initialItem?.id ?: UUID.randomUUID().toString(),
                                        title = title.trim(),
                                        category = category,
                                        issuer = issuer.trim(),
                                        documentNumber = documentNumber.trim(),
                                        issueDateMillis = issueDateMillis,
                                        expiryDateMillis = expiryDateMillis,
                                        reminderDaysBefore = reminderDaysBefore,
                                        renewalCost = renewalCost.trim(),
                                        notes = notes.trim(),
                                        attachmentFileName = attachmentFileName,
                                        attachmentInternalPath = attachmentInternalPath,
                                        attachmentMimeType = attachmentMimeType,
                                        attachmentSizeBytes = attachmentSizeBytes,
                                        createdAtMillis = initialItem?.createdAtMillis ?: System.currentTimeMillis()
                                    )
                                )
                            }
                        ) {
                            Text("Save")
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Live Countdown Preview Banner
                    Card(
                        colors = CardDefaults.cardColors(containerColor = liveBreakdown.urgency.badgeBgColor),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "LIVE EXPIRY COUNTDOWN",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = liveBreakdown.urgency.textColor,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = liveBreakdown.headlineText,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = liveBreakdown.urgency.textColor
                                )
                                Text(
                                    text = "Target Renewal Date: ${ExpiryCalculator.formatDate(expiryDateMillis)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = liveBreakdown.urgency.textColor.copy(alpha = 0.85f)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(liveBreakdown.urgency.textColor)
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = liveBreakdown.badgeText,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.surface
                                )
                            }
                        }
                    }

                    // 1. File Attachment Section (PDF / Image)
                    Text(
                        text = "1. Attach Policy / Document File (PDF or Image)",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )

                    if (attachmentInternalPath != null && attachmentFileName != null) {
                        AttachmentPreviewCard(
                            fileName = attachmentFileName!!,
                            internalPath = attachmentInternalPath!!,
                            mimeType = attachmentMimeType,
                            sizeBytes = attachmentSizeBytes
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(
                                onClick = {
                                    filePickerLauncher.launch(arrayOf("application/pdf", "image/*", "*/*"))
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Replace File")
                            }
                            OutlinedButton(
                                onClick = {
                                    attachmentFileName = null
                                    attachmentInternalPath = null
                                    attachmentMimeType = null
                                    attachmentSizeBytes = 0L
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Remove File")
                            }
                        }
                    } else {
                        OutlinedButton(
                            onClick = {
                                filePickerLauncher.launch(arrayOf("application/pdf", "image/*", "*/*"))
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = AppIcons.Attach,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Select Insurance PDF or Photo from Device")
                        }
                    }

                    // 2. Category Selection
                    Text(
                        text = "2. Document Category",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DocumentCategory.entries.forEach { cat ->
                            FilterChip(
                                selected = category == cat,
                                onClick = { category = cat },
                                label = { Text(cat.displayName) }
                            )
                        }
                    }

                    // 3. Document Details
                    Text(
                        text = "3. Policy / Document Details",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = title,
                        onValueChange = {
                            title = it
                            if (it.isNotBlank()) titleError = false
                        },
                        label = { Text("Document or Insurance Title *") },
                        placeholder = { Text("e.g. Car Insurance, Health Policy, Passport") },
                        isError = titleError,
                        supportingText = if (titleError) {
                            { Text("Please enter a document name") }
                        } else null,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = issuer,
                            onValueChange = { issuer = it },
                            label = { Text("Provider / Authority") },
                            placeholder = { Text("e.g. HDFC ERGO, LIC") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = documentNumber,
                            onValueChange = { documentNumber = it },
                            label = { Text("Policy / Doc No.") },
                            placeholder = { Text("e.g. POL-99412") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // 4. Issue Date & Expiry Date Pickers
                    Text(
                        text = "4. Validity & Expiration Dates",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { showIssueDatePicker = true },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Start / Issue Date",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = AppIcons.Calendar,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = ExpiryCalculator.formatDate(issueDateMillis),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { showExpiryDatePicker = true },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Expiry / Renewal Date *",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = AppIcons.Calendar,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = ExpiryCalculator.formatDate(expiryDateMillis),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // Quick Expiry Presets
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "+15 Days" to 15L,
                            "+30 Days" to 30L,
                            "+6 Months" to 182L,
                            "+1 Year" to 365L
                        ).forEach { (label, days) ->
                            OutlinedButton(
                                onClick = {
                                    expiryDateMillis = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(days)
                                }
                            ) {
                                Text(label, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    // 5. Renewal Alert Lead Time
                    Text(
                        text = "5. Flag as 'Renewal Due Soon' Before Expiry",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(7, 15, 30, 60, 90).forEach { days ->
                            FilterChip(
                                selected = reminderDaysBefore == days,
                                onClick = { reminderDaysBefore = days },
                                label = { Text("$days days before") }
                            )
                        }
                    }

                    // 6. Optional Cost & Notes
                    OutlinedTextField(
                        value = renewalCost,
                        onValueChange = { renewalCost = it },
                        label = { Text("Renewal Premium / Fee (Optional)") },
                        placeholder = { Text("e.g. ₹12,500 / year") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes / Coverage Details (Optional)") },
                        placeholder = { Text("Add agent contact, insured value, or renewal link") },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }

    if (showIssueDatePicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = issueDateMillis)
        DatePickerDialog(
            onDismissRequest = { showIssueDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        state.selectedDateMillis?.let { issueDateMillis = it }
                        showIssueDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showIssueDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = state)
        }
    }

    if (showExpiryDatePicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = expiryDateMillis)
        DatePickerDialog(
            onDismissRequest = { showExpiryDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        state.selectedDateMillis?.let { expiryDateMillis = it }
                        showExpiryDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExpiryDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = state)
        }
    }
}
