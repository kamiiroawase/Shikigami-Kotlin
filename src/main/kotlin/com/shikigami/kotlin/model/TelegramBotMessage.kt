package com.shikigami.kotlin.model

import com.github.kotlintelegrambot.entities.Chat
import com.github.kotlintelegrambot.entities.User

data class TelegramBotMessage(
    val chat: Chat,
    val command: String,
    val files: List<Pair<String, String>>,
    val from: User,
    val messageId: Long,
    val originText: String?,
    val replyToBotSelf: Boolean,
    val replyToFiles: List<Pair<String, String>>,
    val replyToMessageText: String?,
    val text: String?
)