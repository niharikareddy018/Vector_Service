package com.company.vectortool.middleware;

import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class AuthService {

    private final Map<String, String> tokenRegistry = new HashMap<>();

    public AuthService() {
        tokenRegistry.put("secure-admin-token-xyz-123", "admin_user");
        tokenRegistry.put("secure-staff-token-abc-789", "staff_user");
    }

    public boolean validateToken(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        return tokenRegistry.containsKey(token.trim());
    }

    public String getUserRole(String token) {
        return tokenRegistry.getOrDefault(token.trim(), "anonymous");
    }
}
