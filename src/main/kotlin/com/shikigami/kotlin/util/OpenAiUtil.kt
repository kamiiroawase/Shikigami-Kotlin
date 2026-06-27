package com.shikigami.kotlin.util

import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.chat.ImagePart
import com.aallam.openai.api.chat.TextPart
import com.shikigami.kotlin.model.TelegramBotMessage

object OpenAiUtil {
    fun getMessages(
        systemPrompt: String?,
        telegramBotMessage: TelegramBotMessage
    ): MutableList<ChatMessage> {
        val messages = mutableListOf<ChatMessage>()

        if (systemPrompt != null) {
            messages.add(
                ChatMessage(
                    role = ChatRole.System,
                    content = systemPrompt
                )
            )
        }

        if (telegramBotMessage.replyToMessageText != null) {
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
        openaiMessages: MutableList<ChatMessage>,
        base64Pair: Pair<String?, String?>
    ) {
        if (base64Pair.second != null) {
            replaceImageMessage(
                openaiMessages.size - 2,
                openaiMessages,
                base64Pair.second
            )
        }

        if (base64Pair.first != null) {
            replaceImageMessage(
                openaiMessages.size - 1,
                openaiMessages,
                base64Pair.first
            )
        }
    }

    private fun replaceImageMessage(
        index: Int,
        openaiMessages: MutableList<ChatMessage>,
        base64: String?
    ) {
        if (base64 == null) return

        val message = openaiMessages[index]

        openaiMessages[index] = ChatMessage(
            role = message.role,
            content = listOf(
                ImagePart("data:image/jpeg;base64,$base64"),
                TextPart(message.content!!)
            )
        )
    }
}