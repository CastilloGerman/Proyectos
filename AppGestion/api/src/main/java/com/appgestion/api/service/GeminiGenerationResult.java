package com.appgestion.api.service;

/** Respuesta estructurada y contadores de uso devueltos por Gemini, cuando están disponibles. */
public record GeminiGenerationResult<T>(T content, Integer inputTokens, Integer outputTokens) {}
