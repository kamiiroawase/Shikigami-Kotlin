package com.shikigami.kotlin.model

import com.aallam.openai.client.OpenAIConfig

data class OpenAiConfig(
    val model: String,
    val requestConfig: OpenAIConfig
)