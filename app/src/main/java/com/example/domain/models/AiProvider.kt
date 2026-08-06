package com.example.domain.models

enum class AiProvider(val displayName: String, val description: String) {
    GOOGLE_GEMINI("Google Gemini", "Fast, capable models by Google"),
    OPENAI("OpenAI", "Industry-standard models (GPT-3.5, GPT-4)"),
    ANTHROPIC_CLAUDE("Anthropic Claude", "Advanced reasoning and large context"),
    OPENROUTER("OpenRouter", "Access multiple open-source models via one API"),
    OLLAMA("Ollama (Local)", "Run models locally on your machine"),
    LM_STUDIO("LM Studio", "Run models locally via LM Studio"),
    CUSTOM("Custom (OpenAI Compatible)", "Connect to any OpenAI-compatible endpoint")
}
