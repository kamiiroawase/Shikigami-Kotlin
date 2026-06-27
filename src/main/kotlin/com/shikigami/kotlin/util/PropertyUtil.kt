package com.shikigami.kotlin.util

import com.aallam.openai.api.http.Timeout
import com.aallam.openai.api.logging.LogLevel
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.LoggingConfig
import com.aallam.openai.client.OpenAI
import com.aallam.openai.client.OpenAIConfig
import com.aallam.openai.client.OpenAIHost
import com.shikigami.kotlin.model.CommandMatch
import com.shikigami.kotlin.model.TelegramBotConfig
import com.shikigami.kotlin.model.TelegramBotProxy
import java.io.FileInputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.Properties
import kotlin.time.Duration.Companion.seconds

object PropertyUtil {
    val props by lazy {
        Properties().apply {
            FileInputStream("config.properties").use { fis ->
                InputStreamReader(fis, StandardCharsets.UTF_8).use { reader ->
                    load(reader)
                }
            }
        }
    }

    val telegramBotConfig by lazy {
        TelegramBotConfig(
            adminChatId = props.getProperty("telegram.bot.admin.chat.id").toLong(),
            adminMessageRelayText = props.getProperty("telegram.bot.admin.message.relay.text"),
            allowedChatIds = props.getProperty("telegram.bot.allowed.chat.ids")
                .split(",")
                .map { it.toLong() },
            commandStartRelayText = props.getProperty("telegram.bot.command.start.relay.text"),
            errorUnknownRelayText = props.getProperty("telegram.bot.error.unknown.relay.text"),
            errorMessageRelayText = props.getProperty("telegram.bot.error.message.relay.text"),
            mmjPrompt = props.getProperty("telegram.bot.mmj.prompt"),
            placeHolderRelayText = props.getProperty("telegram.bot.place.holder.relay.text"),
            proxy = TelegramBotProxy(
                hostname = props.getProperty("telegram.bot.proxy.hostname"),
                port = props.getProperty("telegram.bot.proxy.port").toInt(),
            ),
            rateLimitRelayText = props.getProperty("telegram.bot.rate.limit.relay.text"),
            token = props.getProperty("telegram.bot.token"),
            username = props.getProperty("telegram.bot.username")
        )
    }

    val commandMatches: List<CommandMatch> by lazy {
        val dmxToken = props.getProperty("openai.provider.dmxapi.token")
        val dmxHost = OpenAIHost(props.getProperty("openai.provider.dmxapi.host"))

        val glmToken = props.getProperty("openai.provider.bigmodel.token")
        val glmHost = OpenAIHost(props.getProperty("openai.provider.bigmodel.host"))

        val dsToken = props.getProperty("openai.provider.deepseek.token")
        val dsHost = OpenAIHost(props.getProperty("openai.provider.deepseek.host"))

        val glmModel = ModelId("glm-5.2")
        val gptModel = ModelId("gpt-5.5")
        val dsModel = ModelId("deepseek-v4-pro")
        val geminiModel = ModelId("gemini-3.5-flash")
        val grokModel = ModelId("grok-4.3")
        val claudeModel = ModelId("claude-opus-4-8")

        listOf(
            CommandMatch("start", "start", null, null),

            CommandMatch("openai", "native", gptModel, getOpenAiClient(dmxToken, dmxHost)),
            CommandMatch("openai", "mmj", gptModel, getOpenAiClient(dmxToken, dmxHost)),

            CommandMatch("openai", "native1", gptModel, getOpenAiClient(dmxToken, dmxHost)),
            CommandMatch("openai", "mmj1", gptModel, getOpenAiClient(dmxToken, dmxHost)),

            CommandMatch("openai", "native2", glmModel, getOpenAiClient(glmToken, glmHost)),
            CommandMatch("openai", "mmj2", glmModel, getOpenAiClient(glmToken, glmHost)),

            CommandMatch("openai", "native3", dsModel, getOpenAiClient(dsToken, dsHost)),
            CommandMatch("openai", "mmj3", dsModel, getOpenAiClient(dsToken, dsHost)),

            CommandMatch("openai", "native4", geminiModel, getOpenAiClient(dmxToken, dmxHost)),
            CommandMatch("openai", "mmj4", geminiModel, getOpenAiClient(dmxToken, dmxHost)),

            CommandMatch("openai", "native5", grokModel, getOpenAiClient(dmxToken, dmxHost)),
            CommandMatch("openai", "mmj5", grokModel, getOpenAiClient(dmxToken, dmxHost)),

            CommandMatch("openai", "native6", claudeModel, getOpenAiClient(dmxToken, dmxHost)),
            CommandMatch("openai", "mmj6", claudeModel, getOpenAiClient(dmxToken, dmxHost)),
        )
    }

    private fun getOpenAiClient(token: String, host: OpenAIHost): OpenAI {
        return OpenAI(
            OpenAIConfig(
                timeout = Timeout(
                    request = 360.seconds,
                    socket = 300.seconds,
                    connect = 10.seconds
                ),
                logging = LoggingConfig(logLevel = LogLevel.None),
                token = token,
                host = host
            )
        )
    }
}