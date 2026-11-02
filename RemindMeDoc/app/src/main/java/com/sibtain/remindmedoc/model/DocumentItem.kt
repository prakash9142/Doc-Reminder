package com.sibtain.remindmedoc.model

import androidx.compose.ui.graphics.Color
import com.sibtain.remindmedoc.ui.theme.StatusCriticalBg
import com.sibtain.remindmedoc.ui.theme.StatusCriticalOrange
import com.sibtain.remindmedoc.ui.theme.StatusExpiredBg
import com.sibtain.remindmedoc.ui.theme.StatusExpiredRed
import com.sibtain.remindmedoc.ui.theme.StatusSafeBg
import com.sibtain.remindmedoc.ui.theme.StatusSafeGreen
import com.sibtain.remindmedoc.ui.theme.StatusWarningAmber
import com.sibtain.remindmedoc.ui.theme.StatusWarningBg

enum class DocumentCategory(
    val displayName: String,
    val shortName: String,
    val accentColor: Color
) {
    VEHICLE_INSURANCE("Vehicle Insurance", "Vehicle Ins.", Color(0xFF1565C0)),
    HEALTH_INSURANCE("Health Insurance", "Health Ins.", Color(0xFF00838F)),
    LIFE_INSURANCE("Life / Term Insurance", "Life Ins.", Color(0xFF6A1B9A)),
    HOME_INSURANCE("Home / Property Insurance", "Home Ins.", Color(0xFF2E7D32)),
    IDENTITY_DOC("Passport / ID Card", "Passport / ID", Color(0xFF4527A0)),
    VEHICLE_LICENSE("Driving License / RC / PUC", "License / PUC", Color(0xFFEF6C00)),
    WARRANTY("Product Warranty / AMC", "Warranty", Color(0xFF37474F)),
    OTHER("Other Document", "Other", Color(0xFF546E7A));

    companion object {
        fun fromName(name: String?): DocumentCategory {
            return entries.firstOrNull { it.name == name } ?: OTHER
        }
    }
}

enum class UrgencyLevel(
    val label: String,
    val textColor: Color,
    val badgeBgColor: Color
) {
    EXPIRED("Expired", StatusExpiredRed, StatusExpiredBg),
    CRITICAL("Critical (< 7 Days)", StatusCriticalOrange, StatusCriticalBg),
    DUE_SOON("Renewal Due Soon", StatusWarningAmber, StatusWarningBg),
    SAFE("Active & Valid", StatusSafeGreen, StatusSafeBg)
}

data class ExpiryBreakdown(
    val totalDaysRemaining: Long,
    val monthsPart: Int,
    val daysPart: Int,
    val hoursPart: Long,
    val minutesPart: Long,
    val secondsPart: Long,
    val remainingFraction: Float,
    val urgency: UrgencyLevel,
    val badgeText: String,
    val headlineText: String,
    val detailedTimeLeftText: String
)

data class DocumentItem(
    val id: String,
    val title: String,
    val category: DocumentCategory,
    val issuer: String,
    val documentNumber: String,
    val issueDateMillis: Long,
    val expiryDateMillis: Long,
    val reminderDaysBefore: Int = 30,
    val renewalCost: String = "",
    val notes: String = "",
    val attachmentFileName: String? = null,
    val attachmentInternalPath: String? = null,
    val attachmentMimeType: String? = null,
    val attachmentSizeBytes: Long = 0L,
    val createdAtMillis: Long = System.currentTimeMillis()
)
