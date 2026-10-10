package com.appgestion.api.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;

/**
 * Resuelve la IP real del cliente según el modo configurado en {@code app.security.client-ip-mode}.
 *
 * Modos:
 * <ul>
 *   <li><b>none</b> (default): ignora todas las cabeceras proxy; devuelve siempre la IP peer.</li>
 *   <li><b>trusted-proxies</b>: valida la IP peer contra una lista CIDR de proxies de confianza
 *       ({@code app.security.trusted-proxies}). Solo si el peer es confiable, lee {@code X-Forwarded-For}
 *       y extrae el primer valor. Si no, devuelve la IP peer.</li>
 *   <li><b>vercel</b>: usa {@code x-vercel-forwarded-for} (sobrevive si hay otro proxy delante de Vercel).
 *       Si falta, usa la última/única entrada de {@code X-Forwarded-For}.
 *       Vercel sobrescribe {@code X-Forwarded-For} con la IP real del cliente (anti-spoofing),
 *       por lo que no se necesita validar proxies de confianza en este modo.</li>
 * </ul>
 *
 * El perfil {@code serverless} activa automáticamente el modo {@code vercel}.
 */
@Component
public class TrustedClientAddressResolver {

    public enum ClientIpMode {
        NONE,
        TRUSTED_PROXIES,
        VERCEL
    }

    private final ClientIpMode mode;
    private final List<Cidr> trustedProxies;

    public TrustedClientAddressResolver(
            @Value("${app.security.client-ip-mode:none}") String clientIpMode,
            @Value("${app.security.trusted-proxies:}") String trustedProxyList) {
        this.mode = parseMode(clientIpMode);
        this.trustedProxies = Cidr.parseList(trustedProxyList);
    }

    private static ClientIpMode parseMode(String value) {
        if (value == null) return ClientIpMode.NONE;
        try {
            // Acepta "trusted-proxies" y "trusted_proxies" como TRUSTED_PROXIES
            String normalized = value.trim().toUpperCase().replace('-', '_');
            return ClientIpMode.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            return ClientIpMode.NONE;
        }
    }

    /**
     * Resuelve la IP del cliente a partir de la petición HTTP.
     *
     * @param request petición HTTP
     * @return IP del cliente (sin truncar)
     */
    public String resolve(HttpServletRequest request) {
        return resolveByMode(request, mode, trustedProxies);
    }

    /**
     * Versión estática para uso en código que no inyecta Spring (filtros servlet).
     */
    public static String resolveStatic(HttpServletRequest request, ClientIpMode mode, List<Cidr> trustedProxies) {
        return resolveByMode(request, mode, trustedProxies);
    }

    private static String resolveByMode(HttpServletRequest request, ClientIpMode mode, List<Cidr> trustedProxies) {
        return switch (mode) {
            case NONE -> request.getRemoteAddr();
            case TRUSTED_PROXIES -> resolveTrustedProxies(request, trustedProxies);
            case VERCEL -> resolveVercel(request);
        };
    }

    private static String resolveTrustedProxies(HttpServletRequest request, List<Cidr> trustedProxies) {
        String peer = request.getRemoteAddr();
        if (trustedProxies.isEmpty()) {
            return peer;
        }
        if (trustedProxies.stream().noneMatch(cidr -> cidr.contains(peer))) {
            return peer;
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded == null || forwarded.isBlank()) {
            return peer;
        }
        String first = forwarded.split(",", 2)[0].trim();
        if (!first.matches("[0-9a-fA-F:.]+")) {
            return peer;
        }
        try {
            InetAddress.getByName(first);
            return first;
        } catch (UnknownHostException ignored) {
            return peer;
        }
    }

    private static String resolveVercel(HttpServletRequest request) {
        // x-vercel-forwarded-for: idéntica a x-forwarded-for pero sobrevive si hay otro proxy delante.
        String vercelFf = request.getHeader("x-vercel-forwarded-for");
        if (vercelFf != null && !vercelFf.isBlank()) {
            String ip = vercelFf.split(",")[0].trim();
            if (ip.matches("[0-9a-fA-F:.]+")) {
                try {
                    InetAddress.getByName(ip);
                    return ip;
                } catch (UnknownHostException ignored) {
                    // fall through
                }
            }
        }
        // Fallback: última/única entrada de X-Forwarded-For (Vercel la sobrescribe con anti-spoofing).
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            String[] parts = xff.split(",");
            String last = parts[parts.length - 1].trim();
            if (last.matches("[0-9a-fA-F:.]+")) {
                try {
                    InetAddress.getByName(last);
                    return last;
                } catch (UnknownHostException ignored) {
                    // fall through
                }
            }
        }
        return request.getRemoteAddr();
    }

    /**
     * Representación de un rango CIDR para validar si una IP pertenece a él.
     */
    public record Cidr(byte[] network, int prefix) {

        /**
         * Parsea una lista CIDR separada por comas.
         */
        public static List<Cidr> parseList(String commaSeparated) {
            if (commaSeparated == null || commaSeparated.isBlank()) {
                return List.of();
            }
            return Arrays.stream(commaSeparated.split(","))
                    .map(String::trim)
                    .filter(value -> !value.isEmpty())
                    .map(Cidr::parse)
                    .toList();
        }

        public static Cidr parse(String value) {
            try {
                String[] parts = value.split("/", 2);
                byte[] address = InetAddress.getByName(parts[0]).getAddress();
                int bits = parts.length == 2 ? Integer.parseInt(parts[1]) : address.length * 8;
                if (bits < 0 || bits > address.length * 8) {
                    throw new IllegalArgumentException("CIDR prefix out of range");
                }
                return new Cidr(address, bits);
            } catch (UnknownHostException | NumberFormatException e) {
                throw new IllegalArgumentException("Invalid trusted proxy CIDR: " + value, e);
            }
        }

        public boolean contains(String address) {
            try {
                byte[] candidate = InetAddress.getByName(address).getAddress();
                if (network.length != candidate.length) {
                    return false;
                }
                int fullBytes = prefix / 8;
                int remainingBits = prefix % 8;
                for (int i = 0; i < fullBytes; i++) {
                    if (candidate[i] != network[i]) return false;
                }
                if (remainingBits == 0) return true;
                int mask = 0xff << (8 - remainingBits);
                return (candidate[fullBytes] & mask) == (network[fullBytes] & mask);
            } catch (UnknownHostException ignored) {
                return false;
            }
        }
    }
}
