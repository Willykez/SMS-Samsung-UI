package com.oneui.sms.ui.thread

/** Lightweight, SMS-native helpers. No RCS/MMS assumptions. */
object SmartSms {
    private val otpPatterns = listOf(
        Regex("\\b(?:code|otp|verification|passcode|pin)[^0-9]{0,20}(\\d{4,8})\\b", RegexOption.IGNORE_CASE),
        Regex("\\b(\\d{4,8})\\b.*(?:code|otp|verification|passcode)", RegexOption.IGNORE_CASE),
    )

    fun extractOtp(text: String): String? = otpPatterns.asSequence().mapNotNull { it.find(text)?.groupValues?.getOrNull(1) }.firstOrNull()

    fun smsSegments(text: String): Int {
        if (text.isEmpty()) return 0
        val gsmLimit = if (text.all { it.code < 128 }) 160 else 70
        val multipart = if (text.all { it.code < 128 }) 153 else 67
        return if (text.length <= gsmLimit) 1 else (text.length + multipart - 1) / multipart
    }
}
