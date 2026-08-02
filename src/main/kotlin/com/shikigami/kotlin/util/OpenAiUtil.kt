package com.shikigami.kotlin.util

import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.chat.ContentPart
import com.aallam.openai.api.chat.ImagePart
import com.aallam.openai.api.chat.TextPart
import com.shikigami.kotlin.model.TelegramBotMessage

object OpenAiUtil {
    fun getMessages(
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

        if (telegramBotMessage.replyToBotSelf) {
            messages.add(
                ChatMessage(
                    role = ChatRole.Assistant,
                    content = telegramBotMessage.replyToMessageText
                )
            )
        } else {
            messages.add(
                ChatMessage(
                    role = ChatRole.User,
                    content = telegramBotMessage.replyToMessageText
                )
            )
        }

        messages.add(
            ChatMessage(
                role = ChatRole.User,
                content = telegramBotMessage.text
            )
        )

        return messages
    }

    fun removeBlankPlaceholder(
        base64: String?,
        index: Int,
        openAiMessages: MutableList<ChatMessage>,
    ) {
        val message = openAiMessages[index]

        if (base64 == null) {
            if (message.content.isNullOrBlank()) {
                openAiMessages.removeAt(index = index)
            }

            return
        }

        message.content.let {
            val content = mutableListOf<ContentPart>(ImagePart(base64))

            if (!it.isNullOrBlank()) {
                content.add(TextPart(it))
            }

            openAiMessages[index] = ChatMessage(
                role = message.role,
                content = content
            )
        }
    }
}