package com.shikigami.kotlin.model

data class TelegramBotConfig(
    val adminChatId: Long,
    val adminMessageRelayText: String,
    val allowedChatIds: List<Long>,
    val commandStartRelayText: String,
    val errorUnknownRelayText: String,
    val errorMessageRelayText: String,
    val mmjPrompt: String,
    val placeHolderRelayText: String,
    val proxy: TelegramBotProxy,
    val rateLimitRelayText: String,
    val token: String,
    val username: String
)