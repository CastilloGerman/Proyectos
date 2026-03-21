package com.appgestion.api.dto.support;

/**
 * Adjunto para correos multipart (soporte, etc.).
 */
public record AdjuntoCorreo(String filename, byte[] data, String contentType) {}
