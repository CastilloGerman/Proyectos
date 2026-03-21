package com.appgestion.api.service;

/** Resultado de crear una sesión de Checkout de Stripe para cobrar una factura. */
public record PaymentLinkResult(String id, String url) {}
