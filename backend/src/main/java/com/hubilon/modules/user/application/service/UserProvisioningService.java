package com.hubilon.modules.user.application.service;

import com.hubilon.modules.department.domain.model.Department;
import com.hubilon.modules.department.domain.port.out.DepartmentCommandPort;
import com.hubilon.modules.department.domain.port.out.DepartmentQueryPort;
import com.hubilon.modules.team.application.port.out.TeamCommandPort;
import com.hubilon.modules.team.application.port.out.TeamQueryPort;
import com.hubilon.modules.team.domain.model.Team;
import com.hubilon.modules.user.domain.model.User;
import com.hubilon.modules.user.domain.port.out.UserCommandPort;
import com.hubilon.modules.user.domain.port.out.UserQueryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserProvisioningService {

    private final UserQueryPort userQueryPort;
    private final UserCommandPort userCommandPort;
    private final TeamQueryPort teamQueryPort;
    private final TeamCommandPort teamCommandPort;
    private final DepartmentQueryPort departmentQueryPort;
    private final DepartmentCommandPort departmentCommandPort;

    @Transactional
    public User provisionOrSync(String email) {
        try {
            return doProvisionOrSync(email);
        } catch (DataIntegrityViolationException e) {
            log.warn("[JIT Provisioning] DataIntegrityViolation for email={}, retrying", email);
            return doProvisionOrSync(email);
        }
    }

    private User doProvisionOrSync(String email) {
        Map<String, Object> claims = extractClaims();

        String preferredUsername = getClaimAsString(claims, "preferred_username");
        String givenName = getClaimAsString(claims, "given_name");
        String familyName = getClaimAsString(claims, "family_name");
        String fullName = buildFullName(givenName, familyName);
        User.Role role = extractRole(claims);
        log.info("[JIT] email={} roles={} realm_access={} resource_access_keys={} resolved_role={}",
                email, claims.get("roles"),
                claims.get("realm_access") instanceof Map<?,?> m ? m.get("roles") : null,
                claims.get("resource_access") instanceof Map<?,?> ra ? ra.keySet() : null,
                role);
        Long teamId = resolveTeamId(claims);

        Optional<User> existing = userQueryPort.findByEmail(email);
        if (existing.isPresent()) {
            User user = existing.get();
            boolean changed = !role.equals(user.getRole())
                    || !Objects.equals(teamId, user.getTeamId())
                    || !Objects.equals(fullName, user.getName());
            if (changed) {
                User updated = User.builder()
                        .id(user.getId())
                        .name(fullName)
                        .email(user.getEmail())
                        .password(user.getPassword())
                        .keycloakUsername(preferredUsername)
                        .teamId(teamId)
                        .role(role)
                        .build();
                return userCommandPort.save(updated);
            }
            return user;
        }

        User newUser = User.builder()
                .name(fullName)
                .email(email)
                .password(null)
                .keycloakUsername(preferredUsername)
                .teamId(teamId)
                .role(role)
                .build();
        return userCommandPort.save(newUser);
    }

    private Map<String, Object> extractClaims() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            return jwt.getClaims();
        }
        if (authentication != null && authentication.getCredentials() instanceof Jwt jwt) {
            return jwt.getClaims();
        }
        log.warn("[JIT Provisioning] Cannot extract JWT claims from SecurityContext");
        return Map.of();
    }

    private String getClaimAsString(Map<String, Object> claims, String key) {
        Object val = claims.get(key);
        return val != null ? val.toString() : null;
    }

    private String buildFullName(String givenName, String familyName) {
        if (givenName == null && familyName == null) return "Unknown";
        if (givenName == null) return familyName;
        if (familyName == null) return givenName;
        if (isKorean(givenName) || isKorean(familyName)) {
            return familyName + givenName;
        }
        return givenName + " " + familyName;
    }

    private boolean isKorean(String text) {
        return text != null && text.chars().anyMatch(c -> c >= 0xAC00 && c <= 0xD7A3);
    }

    private User.Role extractRole(Map<String, Object> claims) {
        // 1. 커스텀 "roles" 클레임
        if (containsAdmin(claims.get("roles"))) return User.Role.ADMIN;

        // 2. Keycloak 기본 realm_access.roles
        Object realmAccess = claims.get("realm_access");
        if (realmAccess instanceof Map<?, ?> realmMap) {
            if (containsAdmin(realmMap.get("roles"))) return User.Role.ADMIN;
        }

        // 3. resource_access.<client>.roles (클라이언트 롤)
        Object resourceAccess = claims.get("resource_access");
        if (resourceAccess instanceof Map<?, ?> resourceMap) {
            for (Object clientObj : resourceMap.values()) {
                if (clientObj instanceof Map<?, ?> clientMap) {
                    if (containsAdmin(clientMap.get("roles"))) return User.Role.ADMIN;
                }
            }
        }

        return User.Role.USER;
    }

    private boolean containsAdmin(Object rolesObj) {
        if (rolesObj instanceof List<?> rolesList) {
            return rolesList.stream()
                    .map(Object::toString)
                    .anyMatch(r -> "ADMIN".equals(r));
        }
        return false;
    }

    private Long resolveTeamId(Map<String, Object> claims) {
        Object deptObj = claims.get("department");
        if (deptObj == null) return null;

        List<?> deptList = deptObj instanceof List ? (List<?>) deptObj : List.of(deptObj);
        if (deptList.isEmpty()) return null;

        // /user_group 같은 flat 그룹 제외 — non-empty 세그먼트 3개 이상인 조직 경로만 사용
        String deptPath = deptList.stream()
                .map(Object::toString)
                .filter(path -> Arrays.stream(path.split("/")).filter(s -> !s.isBlank()).count() >= 3)
                .findFirst()
                .orElse(null);
        if (deptPath == null) return null;

        String[] segments = deptPath.split("/");

        List<String> parts = new ArrayList<>();
        for (String seg : segments) {
            if (seg != null && !seg.isBlank()) parts.add(seg);
        }

        if (parts.isEmpty()) return null;

        if (parts.size() < 2) {
            return resolveOrCreateTeam(parts.get(0), null);
        }

        String deptName = parts.get(parts.size() - 2);
        String teamName = parts.get(parts.size() - 1);

        Department dept = departmentQueryPort.findByName(deptName)
                .orElseGet(() -> departmentCommandPort.save(
                        Department.builder().name(deptName).build()
                ));

        return resolveOrCreateTeam(teamName, dept.getId());
    }

    private Long resolveOrCreateTeam(String teamName, Long deptId) {
        if (deptId != null) {
            Optional<Team> found = teamQueryPort.findByNameAndDeptId(teamName, deptId);
            if (found.isPresent()) return found.get().getId();
        } else {
            Optional<Team> found = teamQueryPort.findByName(teamName);
            if (found.isPresent()) return found.get().getId();
        }

        try {
            Long effectiveDeptId = deptId != null ? deptId : getOrCreateDefaultDeptId();
            Team saved = teamCommandPort.save(Team.builder().name(teamName).deptId(effectiveDeptId).build());
            return saved.getId();
        } catch (DataIntegrityViolationException e) {
            if (deptId != null) {
                return teamQueryPort.findByNameAndDeptId(teamName, deptId)
                        .map(Team::getId).orElse(null);
            }
            return teamQueryPort.findByName(teamName)
                    .map(Team::getId).orElse(null);
        }
    }

    private Long getOrCreateDefaultDeptId() {
        return departmentQueryPort.findByName("Default")
                .map(Department::getId)
                .orElseGet(() -> departmentCommandPort.save(
                        Department.builder().name("Default").build()
                ).getId());
    }
}

