package com.shikigami.kotlin.model

data class TelegramBotConfig(
    val adminChatId: Long,
    val adminMessageRelayText: String,
    val allowedChatIds: List<Long>,
    val commandStartRelayText: String,
    val errorEmptyRelayText: String,
    val errorFileRelayText: String,
    val errorMessageRelayText: String,
    val errorUnknownRelayText: String,
    val mmjPrompt: String,
    val placeHolderRelayText: String,
    val proxy: Pair<String, Int>?,
    val rateLimitRelayText: String,
    val token: String,
    val username: String
)