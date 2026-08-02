package com.shikigami.kotlin.util

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.Message
import com.github.kotlintelegrambot.entities.ReplyParameters
import com.github.kotlintelegrambot.network.fold
import com.shikigami.kotlin.model.TelegramBotConfig
import com.shikigami.kotlin.model.TelegramBotMessage
import java.util.Base64

object TelegramBotUtil {
    fun dispatchMessage(
        message: Message,
        telegramBotConfig: TelegramBotConfig
    ): TelegramBotMessage {
        val from = message.from ?: error("")
        val text = message.text ?: message.caption ?: error("")

        val commandEntity = message.entities?.firstOrNull()
            ?: message.captionEntities?.firstOrNull()
            ?: error("")

        if (from.isBot) {
            if (message.chat.type !in listOf("supergroup", "group")) {
                error("")
            }

            if (message.senderChat == null) {
                error("")
            }

            if (message.senderChat!!.id != message.chat.id) {
                error("")
            }
        }

        val targetBotUsername = text.substring(
            commandEntity.offset,
            commandEntity.offset + commandEntity.length
        ).split("@").getOrNull(1)

        if (targetBotUsername != null) {
            if (targetBotUsername != telegramBotConfig.username) {
                error("")
            }
        }

        if (message.chat.id !in telegramBotConfig.allowedChatIds) {
            if (message.chat.type != "private") {
                error("")
            }
        }

        val command = text.substring(
            commandEntity.offset,
            commandEntity.offset + commandEntity.length
        ).removePrefix("/").substringBefore("@")

        val newText = text
            .drop(commandEntity.offset + commandEntity.length)
            .ifEmpty { null }

        var replyToMessageText = message.replyToMessage?.let {
            it.text ?: it.caption ?: ""
        }

        val replyToCommandEntity = message.replyToMessage?.entities?.firstOrNull()
            ?: message.replyToMessage?.captionEntities?.firstOrNull()

        if (replyToCommandEntity != null) {
            replyToMessageText = replyToMessageText
                ?.drop(replyToCommandEntity.offset + replyToCommandEntity.length)
                ?.ifEmpty { null }
        }

        return TelegramBotMessage(
            from = from,
            text = newText,
            command = command,
            originText = text,
            chat = message.chat,
            messageId = message.messageId,
            replyToMessageText = replyToMessageText,
            replyToBotSelf = message.replyToMessage?.from?.username == telegramBotConfig.username,
            files = message.photo?.takeIf { it.isNotEmpty() }?.map { Pair("jpeg", it.fileId) }
                ?: message.sticker?.let {
                    if (!it.isAnimated) listOf(Pair("webp", it.fileId)) else null
                }
                ?: emptyList(),
            replyToFiles = message.replyToMessage?.photo?.takeIf { it.isNotEmpty() }
                ?.map { Pair("jpeg", it.fileId) }
                ?: message.replyToMessage?.sticker?.let {
                    if (!it.isAnimated) listOf(Pair("webp", it.fileId)) else null
                }
                ?: emptyList(),
        )
    }

    fun sendMessageWithRetry(
        bot: Bot,
        chatId: ChatId,
        relayText: String,
        callback: ((Message?) -> Unit)? = null,
        replyParams: ReplyParameters? = null,
        times: Int = 0
    ) {
        bot.sendMessage(chatId = chatId, text = relayText, replyParameters = replyParams).fold(
            ifError = {
                if (times < 3) {
                    sendMessageWithRetry(bot, chatId, relayText, callback, replyParams, times + 1)
                } else {
                    callback?.invoke(null)
                }
            },
            ifSuccess = { message ->
                callback?.invoke(message)
            }
        )
    }

    fun editMessageWithRetry(
        bot: Bot,
        chatId: ChatId,
        content: String,
        messageId: Long,
        times: Int = 0
    ) {
        bot.editMessageText(
            chatId = chatId,
            messageId = messageId,
            text = content
        ).fold(
            error = {
                if (times < 3) {
                    editMessageWithRetry(bot, chatId, content, messageId, times + 1)
                }
            },
            response = {

            }
        )
    }

    fun getFileBase64(
        bot: Bot,
        file: Pair<String, String>,
        maxTimes: Int = 3,
        times: Int = 0
    ): String? {
        if (times < maxTimes) {
            return try {
                "data:image/${file.first};base64," + Base64.getEncoder()
                    .encodeToString(bot.downloadFileBytes(file.second))
            } catch (_: Exception) {
                getFileBase64(bot, file, maxTimes, times + 1)
            }
        }

        return null
    }
}