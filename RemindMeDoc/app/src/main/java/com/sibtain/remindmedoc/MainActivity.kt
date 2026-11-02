package com.sibtain.remindmedoc

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sibtain.remindmedoc.data.DocumentRepository
import com.sibtain.remindmedoc.model.DocumentItem
import com.sibtain.remindmedoc.model.UrgencyLevel
import com.sibtain.remindmedoc.ui.components.AppIcons
import com.sibtain.remindmedoc.ui.screens.AddEditDocumentDialog
import com.sibtain.remindmedoc.ui.screens.DocumentDetailDialog
import com.sibtain.remindmedoc.ui.theme.RemindMeDocTheme
import com.sibtain.remindmedoc.ui.theme.StatusCriticalOrange
import com.sibtain.remindmedoc.ui.theme.StatusExpiredRed
import com.sibtain.remindmedoc.ui.theme.StatusSafeGreen
import com.sibtain.remindmedoc.ui.theme.StatusWarningAmber
import com.sibtain.remindmedoc.util.ExpiryCalculator
import kotlinx.coroutines.delay

enum class DashboardFilter(val label: String) {
    ALL("All Docs"),
    URGENT("Needs Renewal"),
    EXPIRED("Expired"),
    INSURANCE("Insurance Only"),
    ATTACHED("With Attachment")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repository = DocumentRepository(applicationContext)
        setContent {
            RemindMeDocTheme {
                RemindMeDocDashboard(repository = repository)
            }
        }
    }
}

@Composable
fun RemindMeDocDashboard(repository: DocumentRepository) {
    val context = LocalContext.current
    var documents by remember { mutableStateOf(repository.loadDocuments()) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(DashboardFilter.ALL) }

    var showAddEditDialog by remember { mutableStateOf(false) }
    var editingDocument by remember { mutableStateOf<DocumentItem?>(null) }
    var viewingDocumentId by remember { mutableStateOf<String?>(null) }

    // Live 1-second clock ticker so countdowns update in real time
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            nowMillis = System.currentTimeMillis()
        }
    }

    fun persist(updated: List<DocumentItem>) {
        documents = updated
        repository.saveDocuments(updated)
    }

    // Calculate counts for dashboard summary cards
    val breakdowns = remember(documents, nowMillis) {
        documents.associateWith { ExpiryCalculator.calculate(it, nowMillis) }
    }
    val expiredCount = breakdowns.values.count { it.urgency == UrgencyLevel.EXPIRED }
    val dueSoonCount = breakdowns.values.count {
        it.urgency == UrgencyLevel.CRITICAL || it.urgency == UrgencyLevel.DUE_SOON
    }
    val safeCount = breakdowns.values.count { it.urgency == UrgencyLevel.SAFE }

    val filteredSortedDocs = remember(documents, searchQuery, selectedFilter, nowMillis) {
        documents
            .filter { doc ->
                val matchesQuery = searchQuery.isBlank() ||
                    doc.title.contains(searchQuery, ignoreCase = true) ||
                    doc.issuer.contains(searchQuery, ignoreCase = true) ||
                    doc.documentNumber.contains(searchQuery, ignoreCase = true) ||
                    doc.category.displayName.contains(searchQuery, ignoreCase = true)

                val bd = breakdowns[doc] ?: ExpiryCalculator.calculate(doc, nowMillis)
                val matchesFilter = when (selectedFilter) {
                    DashboardFilter.ALL -> true
                    DashboardFilter.URGENT -> bd.urgency == UrgencyLevel.CRITICAL || bd.urgency == UrgencyLevel.DUE_SOON || bd.urgency == UrgencyLevel.EXPIRED
                    DashboardFilter.EXPIRED -> bd.urgency == UrgencyLevel.EXPIRED
                    DashboardFilter.INSURANCE -> doc.category.name.contains("INSURANCE")
                    DashboardFilter.ATTACHED -> !doc.attachmentInternalPath.isNullOrBlank()
                }
                matchesQuery && matchesFilter
            }
            .sortedBy { it.expiryDateMillis }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    editingDocument = null
                    showAddEditDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                icon = {
                    Icon(
                        imageVector = AppIcons.Plus,
                        contentDescription = "Add Document"
                    )
                },
                text = { Text("Add Document", fontWeight = FontWeight.Bold) }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            // Top Hero Banner
            item {
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 20.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = AppIcons.Shield,
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "RemindMeDoc",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = "Insurance & Document Renewal Tracker",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Summary Metric Cards
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            SummaryStatCard(
                                count = expiredCount,
                                label = "Expired",
                                accentColor = StatusExpiredRed,
                                modifier = Modifier.weight(1f),
                                onClick = { selectedFilter = DashboardFilter.EXPIRED }
                            )
                            SummaryStatCard(
                                count = dueSoonCount,
                                label = "Due Soon",
                                accentColor = StatusWarningAmber,
                                modifier = Modifier.weight(1f),
                                onClick = { selectedFilter = DashboardFilter.URGENT }
                            )
                            SummaryStatCard(
                                count = safeCount,
                                label = "Active",
                                accentColor = StatusSafeGreen,
                                modifier = Modifier.weight(1f),
                                onClick = { selectedFilter = DashboardFilter.ALL }
                            )
                        }
                    }
                }
            }

            // Search & Filter Controls
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search insurance, policy no., or provider...") },
                        leadingIcon = {
                            Icon(imageVector = AppIcons.Search, contentDescription = null)
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DashboardFilter.entries.forEach { filter ->
                            FilterChip(
                                selected = selectedFilter == filter,
                                onClick = { selectedFilter = filter },
                                label = { Text(filter.label) }
                            )
                        }
                    }
                }
            }

            // Empty State or Document List
            if (filteredSortedDocs.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = AppIcons.Document,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (documents.isEmpty()) {
                                    "No Documents Added Yet"
                                } else {
                                    "No Matching Documents Found"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (documents.isEmpty()) {
                                    "Add your vehicle insurance, health policy, passport, or licenses and attach their PDF or photo to track exact days remaining before renewal."
                                } else {
                                    "Try clearing your search or switching the filter above."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            if (documents.isEmpty()) {
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Button(
                                        onClick = {
                                            editingDocument = null
                                            showAddEditDialog = true
                                        }
                                    ) {
                                        Text("Add First Document")
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            persist(repository.createSampleDocuments())
                                        }
                                    ) {
                                        Text("Load Demo Policies")
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                items(filteredSortedDocs, key = { it.id }) { doc ->
                    DocumentCountdownCard(
                        item = doc,
                        nowMillis = nowMillis,
                        onClick = { viewingDocumentId = doc.id },
                        onOpenAttachment = { repository.openAttachmentExternally(doc) }
                    )
                }
            }
        }
    }

    // Add / Edit Modal Dialog
    if (showAddEditDialog) {
        AddEditDocumentDialog(
            initialItem = editingDocument,
            onCopyAttachment = { uri -> repository.copyUriToInternalStorage(uri) },
            onDismiss = {
                showAddEditDialog = false
                editingDocument = null
            },
            onSave = { savedItem ->
                val existingIndex = documents.indexOfFirst { it.id == savedItem.id }
                val updated = if (existingIndex >= 0) {
                    val old = documents[existingIndex]
                    if (old.attachmentInternalPath != null && old.attachmentInternalPath != savedItem.attachmentInternalPath) {
                        repository.deleteAttachmentFile(old.attachmentInternalPath)
                    }
                    documents.toMutableList().apply { set(existingIndex, savedItem) }
                } else {
                    documents + savedItem
                }
                persist(updated)
                showAddEditDialog = false
                editingDocument = null
            }
        )
    }

    // Document Details & Live Countdown Modal
    val viewingDoc = documents.firstOrNull { it.id == viewingDocumentId }
    if (viewingDoc != null) {
        DocumentDetailDialog(
            item = viewingDoc,
            nowMillis = nowMillis,
            onDismiss = { viewingDocumentId = null },
            onEdit = {
                viewingDocumentId = null
                editingDocument = viewingDoc
                showAddEditDialog = true
            },
            onRenewOneYear = {
                val renewed = viewingDoc.copy(
                    issueDateMillis = System.currentTimeMillis(),
                    expiryDateMillis = ExpiryCalculator.addOneYear(viewingDoc.expiryDateMillis)
                )
                persist(documents.map { if (it.id == renewed.id) renewed else it })
                Toast.makeText(context, "Extended renewal date by 1 year!", Toast.LENGTH_SHORT).show()
            },
            onDelete = {
                repository.deleteAttachmentFile(viewingDoc.attachmentInternalPath)
                persist(documents.filterNot { it.id == viewingDoc.id })
                viewingDocumentId = null
                Toast.makeText(context, "Document deleted.", Toast.LENGTH_SHORT).show()
            },
            onOpenAttachment = {
                repository.openAttachmentExternally(viewingDoc)
            }
        )
    }
}

@Composable
private fun SummaryStatCard(
    count: Int,
    label: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        color = Color.White.copy(alpha = 0.14f),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(accentColor)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.85f)
            )
        }
    }
}

@Composable
private fun DocumentCountdownCard(
    item: DocumentItem,
    nowMillis: Long,
    onClick: () -> Unit,
    onOpenAttachment: () -> Unit
) {
    val breakdown = ExpiryCalculator.calculate(item, nowMillis)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 7.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Top Row: Category Chip + Days Remaining Pill Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(item.category.accentColor.copy(alpha = 0.12f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = item.category.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = item.category.accentColor
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(breakdown.urgency.badgeBgColor)
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = breakdown.badgeText,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = breakdown.urgency.textColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Document Title & Provider
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            val subtitleParts = buildList {
                if (item.issuer.isNotBlank()) add(item.issuer)
                if (item.documentNumber.isNotBlank()) add("No: ${item.documentNumber}")
            }
            if (subtitleParts.isNotEmpty()) {
                Text(
                    text = subtitleParts.joinToString(" • "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Countdown Box with Progress Bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(breakdown.urgency.badgeBgColor.copy(alpha = 0.55f))
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = breakdown.headlineText,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = breakdown.urgency.textColor
                    )
                    Text(
                        text = "Exp: ${ExpiryCalculator.formatDate(item.expiryDateMillis)}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = breakdown.urgency.textColor
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = breakdown.detailedTimeLeftText,
                    style = MaterialTheme.typography.labelSmall,
                    color = breakdown.urgency.textColor.copy(alpha = 0.85f)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color.Black.copy(alpha = 0.08f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(breakdown.remainingFraction)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(breakdown.urgency.textColor)
                    )
                }
            }

            // Bottom Row: Attachment Badge & Renewal Cost
            if (!item.attachmentFileName.isNullOrBlank() || item.renewalCost.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!item.attachmentFileName.isNullOrBlank()) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f))
                                .clickable(onClick = onOpenAttachment)
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = AppIcons.Attach,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = item.attachmentFileName,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    if (item.renewalCost.isNotBlank()) {
                        Text(
                            text = item.renewalCost,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
