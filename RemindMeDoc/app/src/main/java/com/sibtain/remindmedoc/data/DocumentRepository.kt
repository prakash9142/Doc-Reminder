package com.sibtain.remindmedoc.data

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.core.content.FileProvider
import com.sibtain.remindmedoc.model.DocumentCategory
import com.sibtain.remindmedoc.model.DocumentItem
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.concurrent.TimeUnit

data class CopiedAttachment(
    val displayName: String,
    val internalPath: String,
    val mimeType: String,
    val sizeBytes: Long
)

class DocumentRepository(private val context: Context) {

    private val prefs = context.getSharedPreferences("remind_me_doc_prefs", Context.MODE_PRIVATE)
    private val attachmentsDir = File(context.filesDir, "attachments").apply {
        if (!exists()) mkdirs()
    }

    fun loadDocuments(): List<DocumentItem> {
        val rawJson = prefs.getString(KEY_DOCUMENTS_JSON, null) ?: return emptyList()
        return try {
            val array = JSONArray(rawJson)
            val list = mutableListOf<DocumentItem>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    DocumentItem(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        title = obj.optString("title", "Untitled Document"),
                        category = DocumentCategory.fromName(obj.optString("category")),
                        issuer = obj.optString("issuer", ""),
                        documentNumber = obj.optString("documentNumber", ""),
                        issueDateMillis = obj.optLong("issueDateMillis", System.currentTimeMillis()),
                        expiryDateMillis = obj.optLong("expiryDateMillis", System.currentTimeMillis()),
                        reminderDaysBefore = obj.optInt("reminderDaysBefore", 30),
                        renewalCost = obj.optString("renewalCost", ""),
                        notes = obj.optString("notes", ""),
                        attachmentFileName = obj.optString("attachmentFileName", "").ifBlank { null },
                        attachmentInternalPath = obj.optString("attachmentInternalPath", "").ifBlank { null },
                        attachmentMimeType = obj.optString("attachmentMimeType", "").ifBlank { null },
                        attachmentSizeBytes = obj.optLong("attachmentSizeBytes", 0L),
                        createdAtMillis = obj.optLong("createdAtMillis", System.currentTimeMillis())
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveDocuments(items: List<DocumentItem>) {
        val array = JSONArray()
        items.forEach { item ->
            val obj = JSONObject().apply {
                put("id", item.id)
                put("title", item.title)
                put("category", item.category.name)
                put("issuer", item.issuer)
                put("documentNumber", item.documentNumber)
                put("issueDateMillis", item.issueDateMillis)
                put("expiryDateMillis", item.expiryDateMillis)
                put("reminderDaysBefore", item.reminderDaysBefore)
                put("renewalCost", item.renewalCost)
                put("notes", item.notes)
                put("attachmentFileName", item.attachmentFileName ?: "")
                put("attachmentInternalPath", item.attachmentInternalPath ?: "")
                put("attachmentMimeType", item.attachmentMimeType ?: "")
                put("attachmentSizeBytes", item.attachmentSizeBytes)
                put("createdAtMillis", item.createdAtMillis)
            }
            array.put(obj)
        }
        prefs.edit().putString(KEY_DOCUMENTS_JSON, array.toString()).apply()
    }

    fun copyUriToInternalStorage(uri: Uri): CopiedAttachment? {
        return try {
            val resolver = context.contentResolver
            var displayName = "attached_document"
            var sizeBytes = 0L

            resolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIdx >= 0) {
                        val name = cursor.getString(nameIdx)
                        if (!name.isNullOrBlank()) displayName = name
                    }
                    val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (sizeIdx >= 0 && !cursor.isNull(sizeIdx)) {
                        sizeBytes = cursor.getLong(sizeIdx)
                    }
                }
            }

            val mimeType = resolver.getType(uri) ?: guessMimeType(displayName)
            val safeFileName = "${System.currentTimeMillis()}_${displayName.replace(Regex("[^a-zA-Z0-9._-]"), "_")}"
            val destFile = File(attachmentsDir, safeFileName)

            resolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return null

            if (sizeBytes <= 0L) {
                sizeBytes = destFile.length()
            }

            CopiedAttachment(
                displayName = displayName,
                internalPath = destFile.absolutePath,
                mimeType = mimeType,
                sizeBytes = sizeBytes
            )
        } catch (e: Exception) {
            null
        }
    }

    fun deleteAttachmentFile(path: String?) {
        if (path.isNullOrBlank()) return
        try {
            val file = File(path)
            if (file.exists()) {
                file.delete()
            }
        } catch (_: Exception) {
        }
    }

    fun openAttachmentExternally(item: DocumentItem) {
        val path = item.attachmentInternalPath
        if (path.isNullOrBlank()) {
            Toast.makeText(context, "No attachment file found.", Toast.LENGTH_SHORT).show()
            return
        }
        val file = File(path)
        if (!file.exists()) {
            Toast.makeText(context, "Attached file no longer exists on device.", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val mime = item.attachmentMimeType ?: guessMimeType(file.name)
            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, mime)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(viewIntent, "Open ${item.attachmentFileName ?: "Document"}"))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "No app installed to open this file type.", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Unable to open attachment: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    fun createSampleDocuments(): List<DocumentItem> {
        val now = System.currentTimeMillis()
        val dayMs = TimeUnit.DAYS.toMillis(1)
        return listOf(
            DocumentItem(
                id = UUID.randomUUID().toString(),
                title = "Comprehensive Car Insurance",
                category = DocumentCategory.VEHICLE_INSURANCE,
                issuer = "HDFC ERGO General Insurance",
                documentNumber = "POL-2319-8841-00",
                issueDateMillis = now - (353 * dayMs),
                expiryDateMillis = now + (12 * dayMs),
                reminderDaysBefore = 30,
                renewalCost = "₹14,850 / year",
                notes = "Zero depreciation + Roadside assistance cover included. Renew before NCB expires."
            ),
            DocumentItem(
                id = UUID.randomUUID().toString(),
                title = "Family Floater Health Policy",
                category = DocumentCategory.HEALTH_INSURANCE,
                issuer = "Star Health & Allied Insurance",
                documentNumber = "SH-FAM-902144",
                issueDateMillis = now - (360 * dayMs),
                expiryDateMillis = now + (5 * dayMs),
                reminderDaysBefore = 30,
                renewalCost = "₹22,400 / year",
                notes = "Sum Insured: ₹10 Lakhs. Cashless network hospital card stored in wallet."
            ),
            DocumentItem(
                id = UUID.randomUUID().toString(),
                title = "Pollution Under Control (PUC)",
                category = DocumentCategory.VEHICLE_LICENSE,
                issuer = "State Transport Department",
                documentNumber = "PUC-KA01-77312",
                issueDateMillis = now - (184 * dayMs),
                expiryDateMillis = now - (4 * dayMs),
                reminderDaysBefore = 15,
                renewalCost = "₹150",
                notes = "6-month emission certificate expired. Visit emission testing center this week."
            ),
            DocumentItem(
                id = UUID.randomUUID().toString(),
                title = "Term Life Protection Plan",
                category = DocumentCategory.LIFE_INSURANCE,
                issuer = "LIC of India",
                documentNumber = "LIC-884920113",
                issueDateMillis = now - (180 * dayMs),
                expiryDateMillis = now + (185 * dayMs),
                reminderDaysBefore = 30,
                renewalCost = "₹18,200 / year",
                notes = "Annual premium due in May. Nominee details verified."
            )
        )
    }

    private fun guessMimeType(name: String): String {
        val lower = name.lowercase()
        return when {
            lower.endsWith(".pdf") -> "application/pdf"
            lower.endsWith(".png") -> "image/png"
            lower.endsWith(".jpg") || lower.endsWith(".jpeg") -> "image/jpeg"
            lower.endsWith(".webp") -> "image/webp"
            else -> "*/*"
        }
    }

    companion object {
        private const val KEY_DOCUMENTS_JSON = "saved_documents_v1"
    }
}
