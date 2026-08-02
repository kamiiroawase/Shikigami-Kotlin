package com.shikigami.kotlin.util

import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.chat.ImagePart
import com.aallam.openai.api.chat.TextPart
import com.shikigami.kotlin.model.TelegramBotMessage

object OpenAiUtil {
    fun getMessages(
        base64Pair: Pair<String?, String?>,
        systemPrompt: String?,
        telegramBotMessage: TelegramBotMessage
    ): MutableList<ChatMessage> {
        val messages = mutableListOf<ChatMessage>()

        if (!systemPrompt.isNullOrBlank()) {
            messages.add(
                ChatMessage(
                    role = ChatRole.System,
                    content = systemPrompt
                )
            )
        }

        if (base64Pair.second != null || !telegramBotMessage.replyToMessageText.isNullOrBlank()) {
            val role = if (telegramBotMessage.replyToBotSelf) {
                ChatRole.Assistant
            } else {
                ChatRole.User
            }

            base64Pair.second?.let {
                messages.add(
                    ChatMessage(
                        role = role,
                        content = if (!telegramBotMessage.replyToMessageText.isNullOrBlank()) {
                            listOf(ImagePart(it), TextPart(telegramBotMessage.replyToMessageText))
                        } else {
                            listOf(ImagePart(it))
                        }
                    )
                )
            } ?: messages.add(
                ChatMessage(
                    role = role,
                    content = telegramBotMessage.replyToMessageText
                )
            )
        }

        base64Pair.first?.let {
            messages.add(
                ChatMessage(
                    role = ChatRole.User,
                    content = if (!telegramBotMessage.text.isNullOrBlank()) {
                        listOf(ImagePart(it), TextPart(telegramBotMessage.text))
                    } else {
                        listOf(ImagePart(it))
                    }
                )
            )
        } ?: messages.add(
            ChatMessage(
                role = ChatRole.User,
                content = telegramBotMessage.text
            )
        )

        return messages
    }
}