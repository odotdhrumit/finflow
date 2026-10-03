package com.example.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateUtils {

    private val fullDateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    private val shortDateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
    private val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())

    fun formatFullDate(millis: Long): String {
        return fullDateFormat.format(Date(millis))
    }

    fun formatShortDate(millis: Long): String {
        return shortDateFormat.format(Date(millis))
    }

    fun formatDate(millis: Long): String {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply { timeInMillis = millis }

        val isSameDay = now.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
                now.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR)
        if (isSameDay) return "Today"

        now.add(Calendar.DAY_OF_YEAR, -1)
        val isYesterday = now.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
                now.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR)
        if (isYesterday) return "Yesterday"

        return fullDateFormat.format(Date(millis))
    }

    fun formatTime(millis: Long): String {
        return timeFormat.format(Date(millis))
    }

    fun formatMonthYear(millis: Long): String {
        return monthYearFormat.format(Date(millis))
    }

    fun getStartOfDay(millis: Long = System.currentTimeMillis()): Long {
        return Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    fun getEndOfDay(millis: Long = System.currentTimeMillis()): Long {
        return Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis
    }

    fun getStartOfWeek(millis: Long = System.currentTimeMillis()): Long {
        return Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    fun getStartOfMonth(millis: Long = System.currentTimeMillis()): Long {
        return Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    fun getMonthsDifference(startMillis: Long, targetMillis: Long): Int {
        val startCal = Calendar.getInstance().apply { timeInMillis = startMillis }
        val targetCal = Calendar.getInstance().apply { timeInMillis = targetMillis }
        val yearDiff = targetCal.get(Calendar.YEAR) - startCal.get(Calendar.YEAR)
        val monthDiff = targetCal.get(Calendar.MONTH) - startCal.get(Calendar.MONTH)
        val total = yearDiff * 12 + monthDiff
        return if (total < 1) 1 else total
    }

    fun formatRelative(millis: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - millis
        val minutes = diff / (60 * 1000)
        val hours = diff / (60 * 60 * 1000)
        val days = diff / (24 * 60 * 60 * 1000)
        return when {
            diff < 60 * 1000 -> "Just now"
            minutes < 60 -> "${minutes}m ago"
            hours < 24 -> "${hours}h ago"
            days == 1L -> "Yesterday"
            days < 7 -> "${days}d ago"
            else -> formatDate(millis)
        }
    }

    fun getDueStatus(dueDateMillis: Long): String {
        val now = System.currentTimeMillis()
        val startOfToday = getStartOfDay(now)
        val startOfDue = getStartOfDay(dueDateMillis)
        val diffDays = ((startOfDue - startOfToday) / (24 * 60 * 60 * 1000)).toInt()

        return when {
            diffDays < 0 -> "Overdue by ${-diffDays} day${if (-diffDays > 1) "s" else ""}"
            diffDays == 0 -> "EMI Due Today"
            diffDays == 1 -> "EMI Due Tomorrow"
            else -> "EMI Due in $diffDays Days"
        }
    }

    fun getDaysRemainingText(dueDateMillis: Long): String {
        val now = System.currentTimeMillis()
        val startOfToday = getStartOfDay(now)
        val startOfDue = getStartOfDay(dueDateMillis)
        val diffDays = ((startOfDue - startOfToday) / (24 * 60 * 60 * 1000)).toInt()

        return when {
            diffDays < 0 -> "${-diffDays}d overdue"
            diffDays == 0 -> "Due today"
            diffDays == 1 -> "1 day remaining"
            else -> "$diffDays days remaining"
        }
    }
}
