package com.example.dam_front.utils

import java.text.SimpleDateFormat
import java.util.*

object DateUtils {
    /**
     * Format a date string to a readable French format
     * @param dateString The date string in ISO 8601 format or similar
     * @param longFormat If true, uses "d MMMM yyyy" (e.g., "9 novembre 2025"),
     *                  if false, uses "d MMM yyyy" (e.g., "9 nov 2025")
     * @return Formatted date string in French
     */
    fun formatDate(dateString: String?, longFormat: Boolean = false): String {
        if (dateString.isNullOrBlank()) {
            return "Non planifiée"
        }
        return try {
            val date = parseDate(dateString)

            if (date != null) {
                val outputFormat = if (longFormat) {
                    SimpleDateFormat("d MMMM yyyy", Locale.FRENCH)
                } else {
                    SimpleDateFormat("d MMM yyyy", Locale.FRENCH)
                }
                outputFormat.format(date)
            } else {
                dateString
            }
        } catch (e: Exception) {
            dateString
        }
    }

    fun parseDate(dateString: String?): Date? {
        if (dateString.isNullOrBlank()) return null

        val formats = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX'",
            "yyyy-MM-dd'T'HH:mm:ssXXX'",
            "yyyy-MM-dd"
        )

        for (format in formats) {
            try {
                val inputFormat = SimpleDateFormat(format, Locale.getDefault())
                inputFormat.timeZone = TimeZone.getTimeZone("UTC")
                val date = inputFormat.parse(dateString)
                if (date != null) {
                    return date
                }
            } catch (ignored: Exception) {
                // Try next format
            }
        }
        return null
    }
}



