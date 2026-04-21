package com.hubilon.modules.confluence.application.service;

import com.hubilon.common.exception.custom.InvalidRequestException;
import com.hubilon.common.exception.custom.NotFoundException;
import com.hubilon.modules.confluence.adapter.out.external.ConfluenceApiClient;
import com.hubilon.modules.confluence.adapter.out.persistence.ConfluenceSpaceConfigJpaEntity;
import com.hubilon.modules.confluence.adapter.out.persistence.ConfluenceSpaceConfigRepository;
import com.hubilon.modules.confluence.adapter.out.persistence.ConfluenceTeamConfigJpaEntity;
import com.hubilon.modules.confluence.adapter.out.persistence.ConfluenceTeamConfigRepository;
import com.hubilon.modules.confluence.application.dto.SpaceConfigResponse;
import com.hubilon.modules.confluence.application.dto.TeamConfigResponse;
import com.hubilon.modules.department.adapter.out.persistence.DepartmentJpaEntity;
import com.hubilon.modules.department.adapter.out.persistence.DepartmentRepository;
import com.hubilon.modules.team.adapter.out.persistence.TeamJpaEntity;
import com.hubilon.modules.team.adapter.out.persistence.TeamRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConfluenceConfigService {

    private static final String MASKED_TOKEN = "***";

    private final ConfluenceSpaceConfigRepository spaceConfigRepository;
    private final ConfluenceTeamConfigRepository teamConfigRepository;
    private final DepartmentRepository departmentRepository;
    private final TeamRepository teamRepository;
    private final AesEncryptionService aesEncryptionService;
    private final ConfluenceClientCache confluenceClientCache;

    @Value("${CONFLUENCE_ALLOWED_HOSTS:}")
    private String allowedHostsRaw;

    // ===== Space 기능 =====

    @Transactional
    public SpaceConfigResponse upsertSpaceConfig(Long deptId, String userEmail,
                                                 String apiToken, String spaceKey,
                                                 String baseUrl, String currentUserEmail) {
        validateSsrf(baseUrl);

        DepartmentJpaEntity dept = departmentRepository.findById(deptId)
                .orElseThrow(() -> new NotFoundException("부서를 찾을 수 없습니다. deptId=" + deptId));

        String encryptedToken = aesEncryptionService.encrypt(apiToken);

        ConfluenceSpaceConfigJpaEntity entity = spaceConfigRepository.findByDeptId(deptId)
                .map(existing -> {
                    existing.update(userEmail, encryptedToken, spaceKey, baseUrl, currentUserEmail);
                    return existing;
                })
                .orElseGet(() -> ConfluenceSpaceConfigJpaEntity.builder()
                        .deptId(deptId)
                        .userEmail(userEmail)
                        .apiToken(encryptedToken)
                        .spaceKey(spaceKey)
                        .baseUrl(baseUrl)
                        .createdBy(currentUserEmail)
                        .updatedBy(currentUserEmail)
                        .build());

        ConfluenceSpaceConfigJpaEntity saved = spaceConfigRepository.saveAndFlush(entity);
        confluenceClientCache.invalidate(deptId);

        log.info("Confluence Space 설정 저장: deptId={}, spaceKey={}, updatedBy={}", deptId, spaceKey, currentUserEmail);
        return toSpaceResponse(saved, dept.getName());
    }

    @Transactional(readOnly = true)
    public List<SpaceConfigResponse> getSpaceConfigs() {
        return spaceConfigRepository.findAll().stream()
                .map(entity -> {
                    String deptName = departmentRepository.findById(entity.getDeptId())
                            .map(DepartmentJpaEntity::getName)
                            .orElse("(삭제된 부서)");
                    return toSpaceResponse(entity, deptName);
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteSpaceConfig(Long deptId, String currentUserEmail) {
        ConfluenceSpaceConfigJpaEntity entity = spaceConfigRepository.findByDeptId(deptId)
                .orElseThrow(() -> new NotFoundException("Confluence Space 설정이 없습니다. deptId=" + deptId));
        spaceConfigRepository.delete(entity);
        confluenceClientCache.invalidate(deptId);
        log.info("Confluence Space 설정 삭제: deptId={}, deletedBy={}", deptId, currentUserEmail);
    }

    public void testConnection(Long deptId) {
        ConfluenceSpaceConfigJpaEntity config = spaceConfigRepository.findByDeptId(deptId)
                .orElseThrow(() -> new NotFoundException("Confluence Space 설정이 없습니다. deptId=" + deptId));
        ConfluenceApiClient client = confluenceClientCache.get(deptId);
        client.testConnection(config.getSpaceKey());
        log.info("Confluence 연결 테스트 성공: deptId={}, spaceKey={}", deptId, config.getSpaceKey());
    }

    public void testConnectionDirect(String userEmail, String apiToken, String spaceKey, String baseUrl) {
        validateSsrf(baseUrl);
        ConfluenceApiClient client = new ConfluenceApiClient(baseUrl, userEmail, apiToken);
        client.testConnection(spaceKey);
        log.info("Confluence 직접 연결 테스트 성공: spaceKey={}", spaceKey);
    }

    // ===== Team 기능 =====

    @Transactional
    public TeamConfigResponse upsertTeamConfig(Long teamId, String parentPageId, String currentUserEmail) {
        TeamJpaEntity team = teamRepository.findById(teamId)
                .orElseThrow(() -> new NotFoundException("팀을 찾을 수 없습니다. teamId=" + teamId));

        ConfluenceTeamConfigJpaEntity entity = teamConfigRepository.findByTeamId(teamId)
                .map(existing -> {
                    existing.update(parentPageId, currentUserEmail);
                    return existing;
                })
                .orElseGet(() -> ConfluenceTeamConfigJpaEntity.builder()
                        .teamId(teamId)
                        .parentPageId(parentPageId)
                        .createdBy(currentUserEmail)
                        .updatedBy(currentUserEmail)
                        .build());

        ConfluenceTeamConfigJpaEntity saved = teamConfigRepository.saveAndFlush(entity);
        log.info("Confluence Team 설정 저장: teamId={}, parentPageId={}, updatedBy={}", teamId, parentPageId, currentUserEmail);
        return toTeamResponse(saved, team.getName());
    }

    @Transactional(readOnly = true)
    public List<TeamConfigResponse> getTeamConfigs() {
        return teamConfigRepository.findAll().stream()
                .map(entity -> {
                    String teamName = teamRepository.findById(entity.getTeamId())
                            .map(TeamJpaEntity::getName)
                            .orElse("(삭제된 팀)");
                    return toTeamResponse(entity, teamName);
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteTeamConfig(Long teamId) {
        ConfluenceTeamConfigJpaEntity entity = teamConfigRepository.findByTeamId(teamId)
                .orElseThrow(() -> new NotFoundException("Confluence Team 설정이 없습니다. teamId=" + teamId));
        teamConfigRepository.delete(entity);
        log.info("Confluence Team 설정 삭제: teamId={}", teamId);
    }

    // ===== 내부 유틸 =====

    private void validateSsrf(String baseUrl) {
        if (allowedHostsRaw == null || allowedHostsRaw.isBlank()) {
            return;
        }
        Set<String> allowedHosts = Set.of(allowedHostsRaw.split(","))
                .stream()
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toSet());

        if (allowedHosts.isEmpty()) {
            return;
        }

        try {
            String host = new URI(baseUrl).getHost();
            if (host == null || !allowedHosts.contains(host)) {
                throw new InvalidRequestException(
                        "허용되지 않은 Confluence URL입니다. 허용 호스트: " + allowedHosts);
            }
        } catch (InvalidRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidRequestException("유효하지 않은 Base URL입니다: " + baseUrl);
        }
    }

    private SpaceConfigResponse toSpaceResponse(ConfluenceSpaceConfigJpaEntity entity, String deptName) {
        return new SpaceConfigResponse(
                entity.getId(),
                entity.getDeptId(),
                deptName,
                entity.getUserEmail(),
                MASKED_TOKEN,
                entity.getSpaceKey(),
                entity.getBaseUrl(),
                entity.getUpdatedBy(),
                entity.getUpdatedAt()
        );
    }

    private TeamConfigResponse toTeamResponse(ConfluenceTeamConfigJpaEntity entity, String teamName) {
        return new TeamConfigResponse(
                entity.getId(),
                entity.getTeamId(),
                teamName,
                entity.getParentPageId(),
                entity.getUpdatedBy(),
                entity.getUpdatedAt()
        );
    }
}
