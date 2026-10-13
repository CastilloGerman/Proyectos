package com.appgestion.api.util;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Valida el destino SMTP definido por el usuario (host + puerto) para evitar SSRF / escaneo de red interna:
 * <ul>
 *   <li>solo nombres de dominio (no IPs literales, no {@code localhost}),</li>
 *   <li>solo puertos SMTP estándar,</li>
 *   <li>todas las IPs a las que resuelve el host deben ser públicas (sin loopback, red privada, link-local,
 *       CGNAT, ULA, multicast, metadata cloud 169.254.x.x, etc.).</li>
 * </ul>
 * Debe aplicarse al guardar la configuración Y justo antes de conectar (el DNS puede cambiar).
 */
public final class SmtpTargetValidator {

    /** Puertos SMTP habituales (25, SMTPS 465, submission 587, alternativo 2525). */
    public static final Set<Integer> ALLOWED_PORTS = Set.of(25, 465, 587, 2525);

    private static final int MAX_HOST_LENGTH = 253;
    private static final Pattern HOSTNAME = Pattern.compile(
            "^(?=.{1,253}$)[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?(?:\\.[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?)+$");
    private static final Pattern TLD_WITH_LETTER = Pattern.compile(".*\\.[A-Za-z0-9-]*[A-Za-z][A-Za-z0-9-]*$");

    private SmtpTargetValidator() {}

    @FunctionalInterface
    public interface HostResolver {
        InetAddress[] resolve(String host) throws UnknownHostException;
    }

    /** Valida host y puerto usando el DNS del sistema. */
    public static void validate(String host, Integer port) {
        validate(host, port, InetAddress::getAllByName);
    }

    public static void validate(String host, Integer port, HostResolver resolver) {
        validatePort(port);
        validateHost(host, resolver);
    }

    public static void validatePort(Integer port) {
        if (port != null && !ALLOWED_PORTS.contains(port)) {
            throw new IllegalArgumentException("Puerto SMTP no permitido (usa 25, 465, 587 o 2525)");
        }
    }

    public static void validateHost(String host, HostResolver resolver) {
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("Servidor SMTP no válido");
        }
        String h = host.trim();
        if (h.length() > MAX_HOST_LENGTH
                || !HOSTNAME.matcher(h).matches()
                || !TLD_WITH_LETTER.matcher(h).matches()) {
            throw new IllegalArgumentException("Servidor SMTP no válido");
        }
        InetAddress[] addresses;
        try {
            addresses = resolver.resolve(h);
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("No se pudo resolver el servidor SMTP");
        }
        if (addresses == null || addresses.length == 0) {
            throw new IllegalArgumentException("No se pudo resolver el servidor SMTP");
        }
        for (InetAddress a : addresses) {
            if (!isPublicAddress(a)) {
                throw new IllegalArgumentException("Servidor SMTP no permitido");
            }
        }
    }

    static boolean isPublicAddress(InetAddress a) {
        if (a.isAnyLocalAddress()
                || a.isLoopbackAddress()
                || a.isLinkLocalAddress()
                || a.isSiteLocalAddress()
                || a.isMulticastAddress()) {
            return false;
        }
        byte[] b = a.getAddress();
        if (a instanceof Inet4Address) {
            int o1 = b[0] & 0xFF;
            int o2 = b[1] & 0xFF;
            if (o1 == 0) {
                return false; // 0.0.0.0/8
            }
            if (o1 == 100 && o2 >= 64 && o2 <= 127) {
                return false; // 100.64.0.0/10 CGNAT (también usado por redes internas de proveedores)
            }
            if (o1 == 192 && o2 == 0 && (b[2] & 0xFF) == 0) {
                return false; // 192.0.0.0/24
            }
            if (o1 == 198 && (o2 == 18 || o2 == 19)) {
                return false; // 198.18.0.0/15 benchmarking
            }
            if (o1 >= 240) {
                return false; // 240.0.0.0/4 reservado + broadcast
            }
            return true;
        }
        if (a instanceof Inet6Address) {
            int first = b[0] & 0xFF;
            if ((first & 0xFE) == 0xFC) {
                return false; // fc00::/7 ULA
            }
            return true;
        }
        return false;
    }
}
