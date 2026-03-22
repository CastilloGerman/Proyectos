package com.appgestion.api.util;

import com.appgestion.api.dto.request.DeviceClientInfoRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

/**
 * Extrae IP y heurísticas de navegador/SO/tipo de dispositivo a partir de cabeceras HTTP.
 */
public final class ClientMetadataParser {

    private ClientMetadataParser() {}

    public record Snapshot(
            String ipAddress,
            String userAgent,
            String browser,
            String osName,
            String deviceType,
            String displayLabel
    ) {}

    public static Snapshot parse(HttpServletRequest request, DeviceClientInfoRequest clientInfo) {
        String ip = resolveClientIp(request);
        String ua = request.getHeader("User-Agent");
        if (!StringUtils.hasText(ua)) {
            ua = "";
        } else {
            ua = ua.trim();
            if (ua.length() > 512) {
                ua = ua.substring(0, 512);
            }
        }
        String browser = detectBrowser(ua);
        String os = detectOs(ua, clientInfo);
        String deviceType = detectDeviceType(ua, clientInfo);
        String label = buildDisplayLabel(clientInfo, browser, os, deviceType);
        return new Snapshot(ip, ua, browser, os, deviceType, label);
    }

    public static String resolveClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xff)) {
            String first = xff.split(",")[0].trim();
            if (!first.isEmpty()) {
                return truncate(first, 45);
            }
        }
        String real = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(real)) {
            return truncate(real.trim(), 45);
        }
        return truncate(request.getRemoteAddr(), 45);
    }

    private static String buildDisplayLabel(DeviceClientInfoRequest info, String browser, String os, String deviceType) {
        if (info != null && StringUtils.hasText(info.deviceLabel())) {
            return truncate(info.deviceLabel().trim(), 200);
        }
        StringBuilder sb = new StringBuilder();
        if (StringUtils.hasText(browser)) {
            sb.append(browser);
        }
        if (StringUtils.hasText(os)) {
            if (!sb.isEmpty()) {
                sb.append(" · ");
            }
            sb.append(os);
        }
        if (StringUtils.hasText(deviceType) && !"UNKNOWN".equals(deviceType)) {
            if (!sb.isEmpty()) {
                sb.append(" · ");
            }
            sb.append(friendlyDeviceType(deviceType));
        }
        String built = sb.toString().trim();
        return built.isEmpty() ? "Dispositivo desconocido" : truncate(built, 200);
    }

    private static String friendlyDeviceType(String deviceType) {
        return switch (deviceType) {
            case "MOBILE" -> "Móvil";
            case "TABLET" -> "Tablet";
            case "DESKTOP" -> "PC / escritorio";
            default -> deviceType;
        };
    }

    private static String detectBrowser(String ua) {
        if (!StringUtils.hasText(ua)) {
            return "Desconocido";
        }
        String u = ua.toLowerCase();
        if (u.contains("edg/") || u.contains("edgios") || u.contains("edga")) {
            return "Microsoft Edge";
        }
        if (u.contains("opr/") || u.contains("opera")) {
            return "Opera";
        }
        if (u.contains("firefox/") || u.contains("fxios")) {
            return "Firefox";
        }
        if (u.contains("chrome/") || u.contains("crios")) {
            return "Chrome";
        }
        if (u.contains("safari/") && !u.contains("chrome")) {
            return "Safari";
        }
        if (u.contains("trident") || u.contains("msie")) {
            return "Internet Explorer";
        }
        return "Navegador desconocido";
    }

    private static String detectOs(String ua, DeviceClientInfoRequest info) {
        if (info != null && StringUtils.hasText(info.platform())) {
            return truncate(info.platform().trim(), 80);
        }
        if (!StringUtils.hasText(ua)) {
            return "Desconocido";
        }
        String u = ua;
        if (u.contains("Windows NT 10")) {
            return "Windows";
        }
        if (u.contains("Windows")) {
            return "Windows";
        }
        if (u.contains("Android")) {
            return "Android";
        }
        if (u.contains("iPhone") || u.contains("iPad") || u.contains("iOS") || u.contains("CPU OS ")) {
            return "iOS";
        }
        if (u.contains("Mac OS X") || u.contains("Macintosh")) {
            return "macOS";
        }
        if (u.contains("Linux")) {
            return "Linux";
        }
        return "Desconocido";
    }

    private static String detectDeviceType(String ua, DeviceClientInfoRequest info) {
        if (info != null && StringUtils.hasText(info.platform())) {
            String p = info.platform().toLowerCase();
            if (p.contains("android") || p.contains("iphone") || p.contains("ipod")) {
                return "MOBILE";
            }
            if (p.contains("ipad")) {
                return "TABLET";
            }
        }
        if (!StringUtils.hasText(ua)) {
            return "UNKNOWN";
        }
        String u = ua.toLowerCase();
        if (u.contains("ipad") || (u.contains("tablet") && !u.contains("mobile"))) {
            return "TABLET";
        }
        if (u.contains("mobile") || u.contains("iphone") || u.contains("ipod") || u.contains("android")) {
            return "MOBILE";
        }
        return "DESKTOP";
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max);
    }
}
