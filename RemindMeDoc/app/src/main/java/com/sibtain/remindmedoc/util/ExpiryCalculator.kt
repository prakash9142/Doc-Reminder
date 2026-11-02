package com.sibtain.remindmedoc.util

import com.sibtain.remindmedoc.model.DocumentItem
import com.sibtain.remindmedoc.model.ExpiryBreakdown
import com.sibtain.remindmedoc.model.UrgencyLevel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.abs

object ExpiryCalculator {

    private val dateFormatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    fun formatDate(millis: Long): String {
        if (millis <= 0L) return "Not set"
        return synchronized(dateFormatter) {
            dateFormatter.format(Date(millis))
        }
    }

    fun normalizeToEndOfDay(millis: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = millis
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        return cal.timeInMillis
    }

    fun normalizeToStartOfDay(millis: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = millis
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun addOneYear(expiryMillis: Long): Long {
        val base = maxOf(expiryMillis, System.currentTimeMillis())
        val cal = Calendar.getInstance()
        cal.timeInMillis = base
        cal.add(Calendar.YEAR, 1)
        return cal.timeInMillis
    }

    fun calculate(item: DocumentItem, nowMillis: Long = System.currentTimeMillis()): ExpiryBreakdown {
        val todayStart = normalizeToStartOfDay(nowMillis)
        val expiryStart = normalizeToStartOfDay(item.expiryDateMillis)
        val expiryEnd = normalizeToEndOfDay(item.expiryDateMillis)

        // Exact calendar day difference
        val dayDiffMillis = expiryStart - todayStart
        val totalDays = TimeUnit.MILLISECONDS.toDays(dayDiffMillis)

        // Exact live countdown to end of expiry day (23:59:59)
        val diffMillis = expiryEnd - nowMillis
        val absMillis = abs(diffMillis)

        val hoursPart = TimeUnit.MILLISECONDS.toHours(absMillis) % 24
        val minutesPart = TimeUnit.MILLISECONDS.toMinutes(absMillis) % 60
        val secondsPart = TimeUnit.MILLISECONDS.toSeconds(absMillis) % 60

        val absDays = abs(totalDays).toInt()
        val monthsPart = absDays / 30
        val daysPart = absDays % 30

        val urgency = when {
            totalDays < 0 -> UrgencyLevel.EXPIRED
            totalDays <= 7 -> UrgencyLevel.CRITICAL
            totalDays <= item.reminderDaysBefore -> UrgencyLevel.DUE_SOON
            else -> UrgencyLevel.SAFE
        }

        // Remaining validity progress bar (0.0 = expired, 1.0 = full period left)
        val startMillis = if (item.issueDateMillis in 1 until item.expiryDateMillis) {
            normalizeToStartOfDay(item.issueDateMillis)
        } else {
            // Default to 365 days before expiry if issue date was not specified
            expiryStart - TimeUnit.DAYS.toMillis(365)
        }
        val totalSpan = (expiryEnd - startMillis).coerceAtLeast(TimeUnit.DAYS.toMillis(1))
        val remainingSpan = (expiryEnd - nowMillis).coerceIn(0L, totalSpan)
        val remainingFraction = (remainingSpan.toFloat() / totalSpan.toFloat()).coerceIn(0f, 1f)

        val badgeText = when {
            totalDays < -1L -> "EXPIRED ${abs(totalDays)}d AGO"
            totalDays == -1L -> "EXPIRED YESTERDAY"
            totalDays == 0L -> "EXPIRES TODAY"
            totalDays == 1L -> "1 DAY LEFT"
            else -> "$totalDays DAYS LEFT"
        }

        val headlineText = when {
            totalDays < 0L -> {
                val overdueDays = abs(totalDays)
                if (monthsPart > 0) {
                    "Expired $overdueDays days ago (${monthsPart}m ${daysPart}d overdue)"
                } else {
                    "Expired $overdueDays day${if (overdueDays == 1L) "" else "s"} ago"
                }
            }
            totalDays == 0L -> "Expires today at midnight!"
            else -> {
                if (monthsPart > 0) {
                    "$totalDays days remaining (${monthsPart} mo ${daysPart} d)"
                } else {
                    "$totalDays day${if (totalDays == 1L) "" else "s"} remaining"
                }
            }
        }

        val detailedTimeLeftText = if (totalDays < 0L) {
            "Overdue by ${abs(totalDays)}d ${hoursPart}h ${minutesPart}m ${secondsPart}s"
        } else {
            "${totalDays}d ${hoursPart}h ${minutesPart}m ${secondsPart}s left until renewal"
        }

        return ExpiryBreakdown(
            totalDaysRemaining = totalDays,
            monthsPart = monthsPart,
            daysPart = daysPart,
            hoursPart = hoursPart,
            minutesPart = minutesPart,
            secondsPart = secondsPart,
            remainingFraction = remainingFraction,
            urgency = urgency,
            badgeText = badgeText,
            headlineText = headlineText,
            detailedTimeLeftText = detailedTimeLeftText
        )
    }

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0L) return ""
        val kb = bytes / 1024.0
        if (kb < 1024.0) return String.format(Locale.US, "%.1f KB", kb)
        val mb = kb / 1024.0
        return String.format(Locale.US, "%.2f MB", mb)
    }
}
