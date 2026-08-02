package com.shikigami.kotlin.base

import com.aallam.openai.api.http.Timeout
import com.aallam.openai.api.logging.LogLevel
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.LoggingConfig
import com.aallam.openai.client.OpenAI
import com.aallam.openai.client.OpenAIConfig
import com.aallam.openai.client.OpenAIHost
import com.shikigami.kotlin.model.TelegramBotConfig
import java.io.FileInputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.Properties
import kotlin.time.Duration.Companion.seconds

object Properties {
    private val props = Properties().apply {
        FileInputStream("config.properties").use { fis ->
            InputStreamReader(fis, StandardCharsets.UTF_8).use { reader ->
                load(reader)
            }
        }
    }

    val telegramBotConfig = let {
        val proxyHostname = props.getProperty("telegram.bot.proxy.hostname")

        TelegramBotConfig(
            adminChatId = props.getProperty("telegram.bot.admin.chat.id").toLong(),
            adminMessageRelayText = props.getProperty("telegram.bot.admin.message.relay.text"),
            allowedChatIds = props.getProperty("telegram.bot.allowed.chat.ids")
                .split(",")
                .map { it.toLong() },
            commandStartRelayText = props.getProperty("telegram.bot.command.start.relay.text"),
            errorEmptyRelayText = props.getProperty("telegram.bot.error.empty.relay.text"),
            errorFileRelayText = props.getProperty("telegram.bot.error.file.relay.text"),
            errorMessageRelayText = props.getProperty("telegram.bot.error.message.relay.text"),
            errorUnknownRelayText = props.getProperty("telegram.bot.error.unknown.relay.text"),
            mmjPrompt = props.getProperty("telegram.bot.mmj.prompt"),
            placeHolderRelayText = props.getProperty("telegram.bot.place.holder.relay.text"),
            proxy = if (!proxyHostname.isNullOrBlank()) {
                Pair(
                    proxyHostname,
                    props.getProperty("telegram.bot.proxy.port").toInt(),
                )
            } else {
                null
            },
            rateLimitRelayText = props.getProperty("telegram.bot.rate.limit.relay.text"),
            token = props.getProperty("telegram.bot.token"),
            username = props.getProperty("telegram.bot.username")
        )
    }

    val commandMatches = let {
        val dmxToken = props.getProperty("openai.provider.dmxapi.token")
        val dmxHost = OpenAIHost(props.getProperty("openai.provider.dmxapi.host"))

        val dsToken = props.getProperty("openai.provider.deepseek.token")
        val dsHost = OpenAIHost(props.getProperty("openai.provider.deepseek.host"))

        val doubaoToken = props.getProperty("openai.provider.doubao.token")
        val doubaoHost = OpenAIHost(props.getProperty("openai.provider.doubao.host"))

        val glmToken = props.getProperty("openai.provider.bigmodel.token")
        val glmHost = OpenAIHost(props.getProperty("openai.provider.bigmodel.host"))

        val kimiToken = props.getProperty("openai.provider.kimi.token")
        val kimiHost = OpenAIHost(props.getProperty("openai.provider.kimi.host"))

        val claudeModel = ModelId(props.getProperty("openai.model.claude.default"))
        val doubaoModel = ModelId(props.getProperty("openai.model.doubao.default"))
        val dsModel = ModelId(props.getProperty("openai.model.ds.default"))
        val geminiModel = ModelId(props.getProperty("openai.model.gemini.default"))
        val glmModel = ModelId(props.getProperty("openai.model.glm.default"))
        val gptModel = ModelId(props.getProperty("openai.model.gpt.default"))
        val grokModel = ModelId(props.getProperty("openai.model.grok.default"))
        val kimiModel = ModelId(props.getProperty("openai.model.kimi.default"))

        val dsOpenAiClient = getOpenAiClient(dsToken, dsHost)
        val doubaoOpenAiClient = getOpenAiClient(doubaoToken, doubaoHost)
        val dmxOpenAiClient = getOpenAiClient(dmxToken, dmxHost)
        val glmOpenAiClient = getOpenAiClient(glmToken, glmHost)
        val kimiOpenAiClient = getOpenAiClient(kimiToken, kimiHost)

        listOf(
            Pair(Pair("start", "start"), null),

            Pair(Pair("openai", "native"), Pair(gptModel, dmxOpenAiClient)),
            Pair(Pair("openai", "mmj"), Pair(gptModel, dmxOpenAiClient)),

            Pair(Pair("openai", "native1"), Pair(gptModel, dmxOpenAiClient)),
            Pair(Pair("openai", "mmj1"), Pair(gptModel, dmxOpenAiClient)),

            Pair(Pair("openai", "native2"), Pair(glmModel, glmOpenAiClient)),
            Pair(Pair("openai", "mmj2"), Pair(glmModel, glmOpenAiClient)),

            Pair(Pair("openai", "native3"), Pair(dsModel, dsOpenAiClient)),
            Pair(Pair("openai", "mmj3"), Pair(dsModel, dsOpenAiClient)),

            Pair(Pair("openai", "native4"), Pair(geminiModel, dmxOpenAiClient)),
            Pair(Pair("openai", "mmj4"), Pair(geminiModel, dmxOpenAiClient)),

            Pair(Pair("openai", "native5"), Pair(grokModel, dmxOpenAiClient)),
            Pair(Pair("openai", "mmj5"), Pair(grokModel, dmxOpenAiClient)),

            Pair(Pair("openai", "native6"), Pair(claudeModel, dmxOpenAiClient)),
            Pair(Pair("openai", "mmj6"), Pair(claudeModel, dmxOpenAiClient)),

            Pair(Pair("openai", "native7"), Pair(doubaoModel, doubaoOpenAiClient)),
            Pair(Pair("openai", "mmj7"), Pair(doubaoModel, doubaoOpenAiClient)),

            Pair(Pair("openai", "native8"), Pair(kimiModel, kimiOpenAiClient)),
            Pair(Pair("openai", "mmj8"), Pair(kimiModel, kimiOpenAiClient)),
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