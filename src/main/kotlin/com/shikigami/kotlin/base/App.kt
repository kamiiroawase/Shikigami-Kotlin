package com.shikigami.kotlin.base

import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.message
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.Message
import com.github.kotlintelegrambot.entities.ReplyParameters
import com.shikigami.kotlin.limiter.RateLimiter
import com.shikigami.kotlin.model.TelegramBotMessage
import com.shikigami.kotlin.util.OpenAiUtil
import com.shikigami.kotlin.util.StringUtil
import com.shikigami.kotlin.util.TelegramBotUtil
import java.net.InetSocketAddress
import java.net.Proxy
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

object App : CoroutineScope {
    override val coroutineContext = Dispatchers.IO + SupervisorJob()

    val commandStartLimiter = RateLimiter(1, 60000)

    val commandOpenaiLimiter = RateLimiter(10, 60000)

    @JvmStatic
    fun main(args: Array<String>) {
        val bot = bot {
            token = Properties.telegramBotConfig.token

            proxy = Properties.telegramBotConfig.proxy?.run {
                Proxy(Proxy.Type.HTTP, InetSocketAddress(first, second))
            } ?: Proxy.NO_PROXY

            dispatch {
                message {
                    val telegramBotMessage = runCatching {
                        TelegramBotUtil.dispatchMessage(
                            telegramBotConfig = Properties.telegramBotConfig,
                            message = message
                        )
                    }.getOrElse { return@message }

                    Properties.commandMatches.forEach { (commandPair, openAiPair) ->
                        val (type, command) = commandPair
                        val (model, openAiClient) = openAiPair ?: Pair(null, null)

                        if (command != telegramBotMessage.command) {
                            return@forEach
                        }

                        val from = telegramBotMessage.from.id.toString()

                        when (type) {
                            "start" -> {
                                if (!commandStartLimiter.allow(from)) {
                                    return@message
                                }

                                launch {
                                    TelegramBotUtil.sendMessageWithRetry(
                                        replyParams = ReplyParameters(telegramBotMessage.messageId),
                                        relayText = Properties.telegramBotConfig.commandStartRelayText,
                                        chatId = ChatId.fromId(telegramBotMessage.chat.id),
                                        bot = bot
                                    )
                                }
                            }

                            "openai" -> {
                                if (telegramBotMessage.text == null) {
                                    return@message
                                }

                                if (openAiClient == null || model == null) {
                                    return@message
                                }

                                if (!commandOpenaiLimiter.allow(from)) {
                                    launch {
                                        TelegramBotUtil.sendMessageWithRetry(
                                            replyParams = ReplyParameters(telegramBotMessage.messageId),
                                            relayText = Properties.telegramBotConfig.rateLimitRelayText,
                                            chatId = ChatId.fromId(telegramBotMessage.chat.id),
                                            bot = bot
                                        )
                                    }

                                    return@message
                                }

                                runOpenaiCompletion(
                                    bot = bot,
                                    openAiModel = model,
                                    openAiClient = openAiClient,
                                    telegramBotMessage = telegramBotMessage,
                                    openAiMessages = OpenAiUtil.getMessages(
                                        telegramBotMessage = telegramBotMessage,
                                        systemPrompt = if (command.startsWith("mmj")) {
                                            Properties.telegramBotConfig.mmjPrompt
                                        } else {
                                            null
                                        }
                                    )
                                )
                            }
                        }

                        return@message
                    }
                }
            }
        }

        bot.startPolling()
    }

    private fun runOpenaiCompletion(
        bot: Bot,
        openAiModel: ModelId,
        openAiClient: OpenAI,
        telegramBotMessage: TelegramBotMessage,
        openAiMessages: MutableList<ChatMessage>
    ) {
        if (telegramBotMessage.chat.id !in Properties.telegramBotConfig.allowedChatIds) {
            launch {
                TelegramBotUtil.sendMessageWithRetry(
                    bot = bot,
                    chatId = ChatId.fromId(Properties.telegramBotConfig.adminChatId),
                    relayText = Properties.telegramBotConfig.adminMessageRelayText +
                            "\n${telegramBotMessage.from.firstName}" +
                            (telegramBotMessage.from.lastName?.let {
                                if (!it.isBlank()) {
                                    " it"
                                } else {
                                    null
                                }
                            } ?: "") +
                            (telegramBotMessage.from.username?.let {
                                if (!it.isBlank()) {
                                    "（@${it}）：\n"
                                } else {
                                    null
                                }
                            } ?: "：\n") +
                            telegramBotMessage.originText,
                )
            }
        }

        val resultMessageDeferred = CompletableDeferred<Message?>()

        launch {
            TelegramBotUtil.sendMessageWithRetry(
                bot = bot,
                chatId = ChatId.fromId(telegramBotMessage.chat.id),
                replyParams = ReplyParameters(telegramBotMessage.messageId),
                relayText = Properties.telegramBotConfig.placeHolderRelayText,
                callback = {
                    resultMessageDeferred.complete(it)
                }
            )
        }

        launch {
            val content = try {
                OpenAiUtil.replaceImageMessages(
                    openAiMessages = openAiMessages,
                    base64Pair = Pair(
                        telegramBotMessage.replyToFiles.lastOrNull()?.let {
                            async { TelegramBotUtil.getFileBase64(bot = bot, file = it) }
                        }?.await(),
                        telegramBotMessage.files.lastOrNull()?.let {
                            async { TelegramBotUtil.getFileBase64(bot = bot, file = it) }
                        }?.await()
                    ),
                )

                try {
                    openAiClient.chatCompletion(
                        ChatCompletionRequest(
                            messages = openAiMessages,
                            model = openAiModel,
                        )
                    ).choices.firstOrNull()?.message?.content?.let {
                        it.ifBlank {
                            null
                        }
                    } ?: Properties.telegramBotConfig.errorEmptyRelayText
                } catch (e: Exception) {
                    e.message?.let {
                        "${Properties.telegramBotConfig.errorMessageRelayText}$it"
                    } ?: Properties.telegramBotConfig.errorUnknownRelayText
                }
            } catch (_: Exception) {
                Properties.telegramBotConfig.errorFileRelayText
            }

            resultMessageDeferred.await()?.let {
                TelegramBotUtil.editMessageWithRetry(
                    content = StringUtil.truncateToUtf16(maxLength = 4096, text = content),
                    chatId = ChatId.fromId(it.chat.id),
                    messageId = it.messageId,
                    bot = bot
                )
            }
        }
    }
}