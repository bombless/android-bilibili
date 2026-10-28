package com.bilibili.compose.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FormatUtils {

    fun formatCount(count: Long): String {
        return when {
            count >= 100_000_000 -> String.format(Locale.getDefault(), "%.1f亿", count / 100_000_000.0)
            count >= 10_000 -> String.format(Locale.getDefault(), "%.1f万", count / 10_000.0)
            count > 0 -> count.toString()
            else -> "0"
        }
    }

    fun formatDuration(seconds: Long): String {
        val s = seconds % 60
        val m = (seconds / 60) % 60
        val h = seconds / 3600
        return if (h > 0) {
            String.format(Locale.getDefault(), "%02d:%02d:%02d", h, m, s)
        } else {
            String.format(Locale.getDefault(), "%02d:%02d", m, s)
        }
    }

    fun formatDate(timestampSeconds: Long): String {
        if (timestampSeconds <= 0) return ""
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date(timestampSeconds * 1000))
    }
}
