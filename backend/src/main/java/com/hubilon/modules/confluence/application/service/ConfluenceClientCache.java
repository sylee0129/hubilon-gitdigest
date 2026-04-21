package com.hubilon.modules.confluence.application.service;

import com.hubilon.common.exception.custom.NotFoundException;
import com.hubilon.modules.confluence.adapter.out.external.ConfluenceApiClient;
import com.hubilon.modules.confluence.adapter.out.persistence.ConfluenceSpaceConfigJpaEntity;
import com.hubilon.modules.confluence.adapter.out.persistence.ConfluenceSpaceConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class ConfluenceClientCache {

    private final ConfluenceSpaceConfigRepository spaceConfigRepository;
    private final AesEncryptionService aesEncryptionService;

    private final ConcurrentHashMap<Long, ConfluenceApiClient> cache = new ConcurrentHashMap<>();

    /**
     * deptId에 해당하는 ConfluenceApiClient를 반환한다.
     * 캐시 미스 시 DB 조회 → 복호화 → 신규 인스턴스 생성 후 캐시에 저장한다.
     * 복호화된 api_token은 절대 로그에 출력하지 않는다.
     */
    public ConfluenceApiClient get(Long deptId) {
        return cache.computeIfAbsent(deptId, id -> {
            log.info("ConfluenceClientCache miss: deptId={}, DB에서 설정 로드", id);
            ConfluenceSpaceConfigJpaEntity config = spaceConfigRepository.findByDeptId(id)
                    .orElseThrow(() -> new NotFoundException(
                            "Confluence Space 설정이 없습니다. deptId=" + id));
            String plainToken = aesEncryptionService.decrypt(config.getApiToken());
            return new ConfluenceApiClient(config.getBaseUrl(), config.getUserEmail(), plainToken);
        });
    }

    /**
     * 설정 변경 또는 삭제 시 호출하여 캐시를 무효화한다.
     */
    public void invalidate(Long deptId) {
        cache.remove(deptId);
        log.info("ConfluenceClientCache invalidated: deptId={}", deptId);
    }
}
