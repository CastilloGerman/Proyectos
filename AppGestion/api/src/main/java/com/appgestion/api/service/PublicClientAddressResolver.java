package com.appgestion.api.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;

@Component
public class PublicClientAddressResolver {
    private final List<Cidr> trustedProxies;

    public PublicClientAddressResolver(
            @Value("${app.public-links.trusted-proxies:}") String trustedProxyList) {
        trustedProxies = Arrays.stream(trustedProxyList.split(","))
                .map(String::trim).filter(value -> !value.isEmpty()).map(Cidr::parse).toList();
    }

    public String resolve(HttpServletRequest request) {
        String peer = request.getRemoteAddr();
        if (trustedProxies.stream().noneMatch(cidr -> cidr.contains(peer))) return peer;
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded == null || forwarded.isBlank()) return peer;
        String first = forwarded.split(",", 2)[0].trim();
        if (!first.matches("[0-9a-fA-F:.]+")) return peer;
        try {
            InetAddress.getByName(first);
            return first;
        } catch (UnknownHostException ignored) {
            return peer;
        }
    }

    private record Cidr(byte[] network, int prefix) {
        static Cidr parse(String value) {
            try {
                String[] parts = value.split("/", 2);
                byte[] address = InetAddress.getByName(parts[0]).getAddress();
                int bits = parts.length == 2 ? Integer.parseInt(parts[1]) : address.length * 8;
                if (bits < 0 || bits > address.length * 8) throw new IllegalArgumentException("CIDR prefix out of range");
                return new Cidr(address, bits);
            } catch (UnknownHostException | NumberFormatException e) {
                throw new IllegalArgumentException("Invalid trusted proxy CIDR", e);
            }
        }

        boolean contains(String address) {
            try {
                byte[] candidate = InetAddress.getByName(address).getAddress();
                if (network.length != candidate.length) return false;
                int fullBytes = prefix / 8;
                int remainingBits = prefix % 8;
                for (int i = 0; i < fullBytes; i++) if (candidate[i] != network[i]) return false;
                if (remainingBits == 0) return true;
                int mask = 0xff << (8 - remainingBits);
                return (candidate[fullBytes] & mask) == (network[fullBytes] & mask);
            } catch (UnknownHostException ignored) {
                return false;
            }
        }
    }
}
