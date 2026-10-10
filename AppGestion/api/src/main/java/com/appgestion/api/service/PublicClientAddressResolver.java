package com.appgestion.api.service;

import com.appgestion.api.util.TrustedClientAddressResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Resuelve la IP del cliente para enlaces públicos.
 * Delega en {@link TrustedClientAddressResolver} para usar el mismo modo configurado
 * ({@code app.security.client-ip-mode}).
 */
@Component
public class PublicClientAddressResolver {

    private final TrustedClientAddressResolver.ClientIpMode mode;
    private final List<TrustedClientAddressResolver.Cidr> trustedProxies;

    public PublicClientAddressResolver(
            @Value("${app.security.client-ip-mode:none}") String clientIpMode,
            @Value("${app.security.trusted-proxies:}") String trustedProxyList) {
        this.mode = parseMode(clientIpMode);
        this.trustedProxies = TrustedClientAddressResolver.Cidr.parseList(trustedProxyList);
    }

    private static TrustedClientAddressResolver.ClientIpMode parseMode(String value) {
        if (value == null) return TrustedClientAddressResolver.ClientIpMode.NONE;
        try {
            String normalized = value.trim().toUpperCase().replace('-', '_');
            return TrustedClientAddressResolver.ClientIpMode.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            return TrustedClientAddressResolver.ClientIpMode.NONE;
        }
    }

    public String resolve(HttpServletRequest request) {
        return TrustedClientAddressResolver.resolveStatic(request, mode, trustedProxies);
    }
}
