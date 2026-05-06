package com.hubilon.common.security;

import org.springframework.security.oauth2.jwt.Jwt;
import java.util.List;
import java.util.Map;

public final class JwtClaimExtractor {

    private JwtClaimExtractor() {}

    @SuppressWarnings("unchecked")
    public static List<String> extractClientRoles(Jwt jwt, String clientId) {
        Map<String, Object> resourceAccess = jwt.getClaimAsMap("resource_access");
        if (resourceAccess == null) return List.of();
        Map<String, Object> clientAccess = (Map<String, Object>) resourceAccess.get(clientId);
        if (clientAccess == null) return List.of();
        List<String> roles = (List<String>) clientAccess.get("roles");
        return roles != null ? roles : List.of();
    }

    public static String extractDepartmentName(Jwt jwt) {
        List<String> departments = jwt.getClaimAsStringList("department");
        if (departments == null || departments.isEmpty()) return null;
        String[] parts = departments.get(0).split("/");
        return parts.length >= 3 ? parts[2] : null;
    }

    public static String extractTeamName(Jwt jwt) {
        List<String> departments = jwt.getClaimAsStringList("department");
        if (departments == null || departments.isEmpty()) return null;
        String[] parts = departments.get(0).split("/");
        return parts.length >= 4 ? parts[3] : null;
    }
}
