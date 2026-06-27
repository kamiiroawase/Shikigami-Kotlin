package com.shikigami.kotlin.model

data class TelegramBotMessage(
    val text: String,
    val fileIds: List<String>,
    val replyToFileIds: List<String>,
    val replyToBotSelf: Boolean,
    val replyToMessageText: String?
)