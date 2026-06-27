package com.shikigami.kotlin.base

import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.message
import com.github.kotlintelegrambot.entities.Message
import com.github.kotlintelegrambot.entities.MessageEntity
import com.github.kotlintelegrambot.entities.User
import com.shikigami.kotlin.model.TelegramBotMessage
import com.shikigami.kotlin.util.LimiterUtil
import com.shikigami.kotlin.util.OpenAiUtil
import com.shikigami.kotlin.util.PropertyUtil
import com.shikigami.kotlin.util.TelegramBotUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import java.net.InetSocketAddress
import java.net.Proxy

object App : CoroutineScope {
    override val coroutineContext = Dispatchers.IO + SupervisorJob()

    val commandMatches by lazy {
        PropertyUtil.commandMatches
    }

    val telegramBotConfig by lazy {
        PropertyUtil.telegramBotConfig
    }

    @JvmStatic
    fun main(args: Array<String>) {
        bot {
            token = telegramBotConfig.token
            proxy = telegramBotConfig.proxy.run {
                Proxy(Proxy.Type.HTTP, InetSocketAddress(hostname, port))
            }
            dispatch {
                message {
                    val messageTriple = TelegramBotUtil.dispatchMessage(
                        telegramBotConfig = telegramBotConfig,
                        message = message,
                        bot = bot
                    )

                    if (messageTriple == null) return@message

                    val (from, text, commandPair) = messageTriple

                    val (command, commandEntity) = commandPair

                    commandMatches.forEach { match ->
                        if (match.command != command) {
                            return@forEach
                        }

                        when (match.type) {
                            "start" -> {
                                if (!LimiterUtil.commandStartLimiter.allow(from.id.toString())) {
                                    return@message
                                }

                                launch {
                                    TelegramBotUtil.sendMessageWithRetry(
                                        relayText = telegramBotConfig.commandStartRelayText,
                                        message = message,
                                        bot = bot
                                    )
                                }
                            }

                            "openai" -> {
                                if (!LimiterUtil.commandOpenaiLimiter.allow(from.id.toString())) {
                                    TelegramBotUtil.sendMessageWithRetry(
                                        relayText = telegramBotConfig.rateLimitRelayText,
                                        message = message,
                                        bot = bot
                                    )

                                    return@message
                                }

                                botCommandCallback(
                                    from = from,
                                    text = text,
                                    message = message,
                                    commandEntity = commandEntity,
                                    callback = { telegramBotMessage ->
                                        if (match.openAiClient == null || match.model == null) {
                                            return@botCommandCallback
                                        }

                                        runOpenaiCompletion(
                                            bot = bot,
                                            message = message,
                                            openAiModel = match.model,
                                            openAiClient = match.openAiClient,
                                            telegramBotMessage = telegramBotMessage,
                                            openAiMessages = OpenAiUtil.getMessages(
                                                if (match.command.startsWith("mmj")) {
                                                    telegramBotConfig.mmjPrompt
                                                } else {
                                                    null
                                                },
                                                telegramBotMessage
                                            )
                                        )
                                    }
                                )
                            }
                        }

                        return@message
                    }
                }
            }
        }.startPolling()
    }

    private fun botCommandCallback(
        from: User,
        text: String,
        message: Message,
        commandEntity: MessageEntity,
        callback: (TelegramBotMessage) -> Unit
    ) {
        val (newText, replyToMessageText) = TelegramBotUtil.getMessageTexts(
            commandEntity = commandEntity,
            message = message,
            text = text,
        )

        if (newText == null) return

        callback.invoke(
            TelegramBotMessage(
                text = newText,
                replyToBotSelf = from.username == telegramBotConfig.username,
                replyToMessageText = replyToMessageText,
                fileIds = message.photo?.takeIf { it.isNotEmpty() }?.map { it.fileId }
                    ?: message.sticker?.let { sticker ->
                        if (!sticker.isAnimated) listOf(sticker.fileId) else null
                    }
                    ?: emptyList(),
                replyToFileIds = message.replyToMessage?.photo?.takeIf { it.isNotEmpty() }
                    ?.map { it.fileId }
                    ?: message.replyToMessage?.sticker?.let { sticker ->
                        if (!sticker.isAnimated) listOf(sticker.fileId) else null
                    }
                    ?: emptyList(),
            )
        )
    }

    private fun runOpenaiCompletion(
        bot: Bot,
        message: Message,
        openAiModel: ModelId,
        openAiClient: OpenAI,
        telegramBotMessage: TelegramBotMessage,
        openAiMessages: MutableList<ChatMessage>
    ) {
        launch {
            val fullText = StringBuilder()
            val editChannel = Channel<StringBuilder>(Channel.Factory.CONFLATED)

            val editJob = TelegramBotUtil.getEditJob(
                telegramBotConfig = telegramBotConfig,
                editChannel = editChannel,
                message = message,
                app = this@App,
                bot = bot
            )

            OpenAiUtil.replaceImageMessages(
                openAiMessages,
                TelegramBotUtil.getFilesBase64(this@App, bot, telegramBotMessage)
            )

            try {
                openAiClient.chatCompletions(
                    ChatCompletionRequest(
                        messages = openAiMessages,
                        model = openAiModel,
                    )
                ).collect { chunk ->
                    val deltaText = chunk.choices
                        .mapNotNull { it.delta?.content }
                        .joinToString("")

                    if (deltaText.isNotEmpty()) {
                        fullText.append(deltaText)
                        editChannel.trySend(fullText)
                    }
                }
            } catch (e: Exception) {
                fullText.append(
                    e.message?.let {
                        "${telegramBotConfig.errorMessageRelayText}$it"
                    } ?: telegramBotConfig.errorUnknownRelayText
                )

                editChannel.send(fullText)
                editChannel.close()
            }

            editJob.join()
        }
    }
}