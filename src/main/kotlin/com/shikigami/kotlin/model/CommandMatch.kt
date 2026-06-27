package com.shikigami.kotlin.model

import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI

data class CommandMatch(
    val type: String,
    val command: String,
    val model: ModelId?,
    val openAiClient: OpenAI?
)