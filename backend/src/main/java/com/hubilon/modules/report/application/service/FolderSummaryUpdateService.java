package com.hubilon.modules.report.application.service;

import com.hubilon.common.exception.custom.ForbiddenException;
import com.hubilon.common.exception.custom.NotFoundException;
import com.hubilon.modules.folder.adapter.out.persistence.FolderMemberJpaRepository;
import com.hubilon.modules.report.application.dto.FolderSummaryResult;
import com.hubilon.modules.report.application.dto.FolderSummaryUpdateCommand;
import com.hubilon.modules.report.application.mapper.FolderSummaryAppMapper;
import com.hubilon.modules.report.domain.model.FolderSummary;
import com.hubilon.modules.report.domain.port.in.FolderSummaryUpdateUseCase;
import com.hubilon.modules.report.domain.port.out.FolderSummaryCommandPort;
import com.hubilon.modules.report.domain.port.out.FolderSummaryQueryPort;
import com.hubilon.modules.user.adapter.out.persistence.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class FolderSummaryUpdateService implements FolderSummaryUpdateUseCase {

    private final FolderSummaryQueryPort folderSummaryQueryPort;
    private final FolderSummaryCommandPort folderSummaryCommandPort;
    private final FolderSummaryAppMapper folderSummaryAppMapper;
    private final FolderMemberJpaRepository folderMemberJpaRepository;
    private final UserRepository userRepository;

    @Transactional
    @Override
    public FolderSummaryResult update(Long id, FolderSummaryUpdateCommand command, String currentUserEmail) {
        FolderSummary folderSummary = folderSummaryQueryPort.findById(id)
                .orElseThrow(() -> new NotFoundException("폴더 요약을 찾을 수 없습니다. id=" + id));

        // 인가 검증: 현재 사용자가 해당 폴더의 멤버인지 확인
        Long userId = userRepository.findByEmail(currentUserEmail)
                .map(u -> u.getId())
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다. email=" + currentUserEmail));

        boolean isMember = folderMemberJpaRepository
                .findByUserId(userId)
                .stream()
                .anyMatch(m -> m.getFolder().getId().equals(folderSummary.getFolderId()));

        if (!isMember) {
            throw new ForbiddenException("해당 폴더의 멤버가 아닙니다.");
        }

        // PATCH 의미론: 빈 문자열은 null로 변환, null은 기존 값 보존
        String newProgressSummary = resolveField(command.progressSummary(), folderSummary.getProgressSummary());
        String newPlanSummary = resolveField(command.planSummary(), folderSummary.getPlanSummary());

        // summary 필드는 progressSummary 값으로 동기화 (하위호환)
        FolderSummary updated = folderSummary.withManualSummary(newProgressSummary, newPlanSummary);
        FolderSummary saved = folderSummaryCommandPort.save(updated);
        return folderSummaryAppMapper.toResult(saved);
    }

    /**
     * PATCH 필드 해결 규칙:
     * - null → 기존 값 보존
     * - "" (빈 문자열) → null 처리 (삭제)
     * - 값 있음 → 신규 값 사용
     */
    private String resolveField(String newValue, String existing) {
        if (newValue == null) {
            return existing;
        }
        return newValue.isBlank() ? null : newValue;
    }
}
