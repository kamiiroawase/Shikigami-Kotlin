package com.shikigami.kotlin.util

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.Message
import com.github.kotlintelegrambot.entities.MessageEntity
import com.github.kotlintelegrambot.entities.ReplyParameters
import com.github.kotlintelegrambot.entities.User
import com.github.kotlintelegrambot.network.fold
import com.shikigami.kotlin.base.App
import com.shikigami.kotlin.model.TelegramBotConfig
import com.shikigami.kotlin.model.TelegramBotMessage
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import java.util.Base64
import kotlin.text.ifEmpty

object TelegramBotUtil {
    fun dispatchMessage(
        bot: Bot,
        message: Message,
        telegramBotConfig: TelegramBotConfig
    ): Triple<User, String, Pair<String, MessageEntity>>? {
        val from = message.from ?: return null
        val text = message.text ?: message.caption ?: return null

        val commandEntity = message.entities?.firstOrNull()
            ?: message.captionEntities?.firstOrNull()
            ?: return null

        if (from.isBot) {
            if (message.chat.type !in listOf("supergroup", "group")) {
                return null
            }

            if (message.senderChat == null) {
                return null
            }

            if (message.senderChat!!.id != message.chat.id) {
                return null
            }
        }

        val targetBotUsername = text.substring(
            commandEntity.offset,
            commandEntity.offset + commandEntity.length
        ).split("@").getOrNull(1)

        if (targetBotUsername != null) {
            if (targetBotUsername != telegramBotConfig.username) return null
        }

        if (message.chat.id !in telegramBotConfig.allowedChatIds) {
            if (message.chat.type == "private") {
                bot.sendMessage(
                    text = "${telegramBotConfig.adminMessageRelayText}\n@${from.username ?: ""}：\n$text",
                    chatId = ChatId.fromId(telegramBotConfig.adminChatId),
                )
            } else {
                return null
            }
        }

        val command = text.substring(
            commandEntity.offset,
            commandEntity.offset + commandEntity.length
        ).removePrefix("/").substringBefore("@")

        return Triple(from, text, Pair(command, commandEntity))
    }

    fun getMessageTexts(
        text: String,
        message: Message,
        commandEntity: MessageEntity
    ): Pair<String?, String?> {
        val newText = text
            .drop(commandEntity.offset + commandEntity.length)
            .ifEmpty { null }

        var replyToMessageText = message.replyToMessage?.text ?: message.replyToMessage?.caption

        val replyToCommandEntity = message.replyToMessage?.entities?.firstOrNull()
            ?: message.replyToMessage?.captionEntities?.firstOrNull()

        if (replyToCommandEntity != null) {
            replyToMessageText = replyToMessageText
                ?.drop(replyToCommandEntity.offset + replyToCommandEntity.length)
                ?.ifEmpty { null }
        }

        return Pair(newText, replyToMessageText)
    }

    fun sendMessageWithRetry(
        bot: Bot,
        message: Message,
        relayText: String,
        times: Int = 0,
        callback: ((Message) -> Unit)? = null
    ) {
        bot.sendMessage(
            text = relayText,
            chatId = ChatId.fromId(message.chat.id),
            replyParameters = ReplyParameters(messageId = message.messageId)
        ).fold(
            ifError = {
                sendMessageWithRetry(bot, message, relayText, times + 1)
            },
            ifSuccess = { message ->
                callback?.invoke(message)
            }
        )
    }

    fun editMessageWithRetry(
        bot: Bot,
        message: Message,
        chatId: ChatId,
        textString: String,
        times: Int = 0
    ) {
        bot.editMessageText(
            chatId = chatId,
            messageId = message.messageId,
            text = textString
        ).fold(
            error = {
                editMessageWithRetry(bot, message, chatId, textString, times + 1)
            },
            response = {

            }
        )
    }

    fun getEditJob(
        app: App,
        bot: Bot,
        message: Message,
        telegramBotConfig: TelegramBotConfig,
        editChannel: Channel<StringBuilder>
    ): Job {
        var chatId: ChatId? = null
        var resultMessage: Message? = null
        val deferred = CompletableDeferred<Boolean>()

        sendMessageWithRetry(bot, message, telegramBotConfig.placeHolderRelayText) { it ->
            resultMessage = it
            chatId = ChatId.fromId(it.chat.id)
            deferred.complete(true)
        }

        return app.launch {
            var firstSent = false
            var lastSentText = ""

            for (text in editChannel) {
                val textString = text.toString()

                if (textString != lastSentText) {
                    if (resultMessage != null && chatId != null) {
                        editMessageWithRetry(
                            bot,
                            resultMessage,
                            chatId,
                            textString
                        )
                        firstSent = true
                    }

                    lastSentText = textString
                }
            }

            if (!firstSent) {
                deferred.await()

                if (resultMessage != null && chatId != null) {
                    editMessageWithRetry(
                        bot,
                        resultMessage,
                        chatId,
                        lastSentText
                    )
                }
            }
        }
    }

    suspend fun getFilesBase64(
        app: App,
        bot: Bot,
        telegramBotMessage: TelegramBotMessage
    ): Pair<String?, String?> {
        val jobs = mutableListOf<Job>()
        var currentFileBase64: String? = null
        var repliedFileBase64: String? = null

        telegramBotMessage.replyToFileIds.firstOrNull()?.let { fileId ->
            jobs.add(app.launch {
                repliedFileBase64 = getFileBase64(bot, fileId)
            })
        }

        telegramBotMessage.fileIds.firstOrNull()?.let { fileId ->
            jobs.add(app.launch {
                currentFileBase64 = getFileBase64(bot, fileId)
            })
        }

        jobs.joinAll()

        return Pair(currentFileBase64, repliedFileBase64)
    }

    fun getFileBase64(
        bot: Bot,
        fileId: String,
        maxTimes: Int = 3,
        times: Int = 0
    ): String? {
        if (times < maxTimes) {
            return try {
                Base64.getEncoder().encodeToString(bot.downloadFileBytes(fileId))
            } catch (_: Exception) {
                getFileBase64(bot, fileId, times + 1)
            }
        }

        return null
    }
}