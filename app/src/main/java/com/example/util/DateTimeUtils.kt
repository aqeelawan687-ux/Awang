package com.example.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateTimeUtils {
    const val DATE_TIME_PATTERN = "dd MMM yyyy • hh:mm a"

    fun formatDateTime(timestamp: Long): String {
        if (timestamp <= 0L) return "N/A"
        val sdf = SimpleDateFormat(DATE_TIME_PATTERN, Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}
