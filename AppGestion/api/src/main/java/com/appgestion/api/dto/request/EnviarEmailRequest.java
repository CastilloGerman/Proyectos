package com.appgestion.api.dto.request;

/**
 * Opcional: permite enviar a un email distinto al del cliente.
 * Si no se envía, se usa el email del cliente.
 */
public record EnviarEmailRequest(String email) {}
