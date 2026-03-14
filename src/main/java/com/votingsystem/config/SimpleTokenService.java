package com.votingsystem.config;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
public class SimpleTokenService {

    public String createToken(String role, Long id, String name, String username) {
        String payload = role + "|" + id + "|" + safe(name) + "|" + safe(username);
        return Base64.getUrlEncoder().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
    }

    public TokenPayload parseAuthorizationHeader(String header) {
        if (header == null || !header.startsWith("Bearer ")) {
            return null;
        }

        try {
            String token = header.substring(7);
            String decoded = new String(Base64.getUrlDecoder().decode(token), StandardCharsets.UTF_8);
            String[] parts = decoded.split("\\|", -1);
            if (parts.length < 4) {
                return null;
            }
            return new TokenPayload(Long.parseLong(parts[1]), parts[0], parts[2], parts[3]);
        } catch (Exception ex) {
            return null;
        }
    }

    private String safe(String value) {
        return value == null ? "" : value.replace("|", " ");
    }

    public record TokenPayload(Long id, String role, String name, String username) {
    }
}
