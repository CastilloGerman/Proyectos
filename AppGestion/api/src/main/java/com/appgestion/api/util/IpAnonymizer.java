package com.appgestion.api.util;

import org.springframework.util.StringUtils;

/**
 * Anonimización ligera de IP (p. ej. RGPD): IPv4 → último octeto a 0; IPv6 → conserva prefijo aproximado.
 */
public final class IpAnonymizer {

    private IpAnonymizer() {}

    public static String anonymize(String ip) {
        if (!StringUtils.hasText(ip)) {
            return null;
        }
        String s = ip.trim();
        if (s.contains(".")) {
            int last = s.lastIndexOf('.');
            if (last > 0 && last < s.length() - 1) {
                return s.substring(0, last + 1) + "0";
            }
            return s;
        }
        if (s.contains(":")) {
            String[] parts = s.split(":", -1);
            if (parts.length >= 4) {
                return String.join(":", java.util.Arrays.copyOf(parts, 4)) + ":0:0:0:0";
            }
            return s;
        }
        return s;
    }
}
