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

        if (!telegramBotMessage.replyToMessageText.isNullOrBlank()) {
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
        }

        messages.add(
            ChatMessage(
                role = ChatRole.User,
                content = telegramBotMessage.text
            )
        )

        return messages
    }

    fun replaceImageMessages(
        base64Pair: Pair<String?, String?>,
        openAiMessages: MutableList<ChatMessage>,
    ) {
        if (base64Pair.second != null) {
            replaceImageMessage(
                index = openAiMessages.size - 2,
                openAiMessages = openAiMessages,
                base64 = base64Pair.second
            )
        }

        if (base64Pair.first != null) {
            replaceImageMessage(
                index = openAiMessages.size - 1,
                openAiMessages = openAiMessages,
                base64 = base64Pair.first
            )
        }
    }

    private fun replaceImageMessage(
        base64: String?,
        index: Int,
        openAiMessages: MutableList<ChatMessage>,
    ) {
        if (base64 == null) return

        val message = openAiMessages[index]

        message.content?.let {
            val content = mutableListOf<ContentPart>(ImagePart(base64))

            if (it.isNotBlank()) {
                content.add(TextPart(it))
            }

            openAiMessages[index] = ChatMessage(
                role = message.role,
                content = content
            )
        }
    }
}