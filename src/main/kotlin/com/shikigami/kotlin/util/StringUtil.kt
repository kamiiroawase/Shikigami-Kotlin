package com.shikigami.kotlin.util

object StringUtil {
    fun truncateToUtf16(maxLength: Int, text: String): String {
        if (text.length <= maxLength) return text

        var end = maxLength - 1

        while (end > 0 && Character.isLowSurrogate(text[end])) end--

        return text.substring(0, end) + "\u2026"
    }
}