package com.hubilon.modules.scheduler.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static lombok.AccessLevel.PROTECTED;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = PROTECTED)
public class SchedulerJobLog {

    private Long id;
    private LocalDateTime executedAt;
    private SchedulerJobStatus status;
    private int totalFolderCount;
    private int successCount;
    private int failCount;
    private LocalDateTime createdAt;

    @Builder.Default
    private List<SchedulerFolderResult> folderResults = new ArrayList<>();

    public static SchedulerJobLog createRunning(int totalFolderCount) {
        return SchedulerJobLog.builder()
                .executedAt(LocalDateTime.now())
                .status(SchedulerJobStatus.RUNNING)
                .totalFolderCount(totalFolderCount)
                .successCount(0)
                .failCount(0)
                .folderResults(new ArrayList<>())
                .build();
    }

    public void recordSuccess(Long folderId, String folderName, String confluencePageUrl) {
        folderResults.add(SchedulerFolderResult.builder()
                .folderId(folderId)
                .folderName(folderName)
                .success(true)
                .confluencePageUrl(confluencePageUrl)
                .build());
        this.successCount++;
    }

    public void recordFail(Long folderId, String folderName, String errorMessage) {
        folderResults.add(SchedulerFolderResult.builder()
                .folderId(folderId)
                .folderName(folderName)
                .success(false)
                .errorMessage(errorMessage)
                .build());
        this.failCount++;
    }

    /** 단일 Confluence 업로드 성공 후 성공 항목들의 URL을 일괄 업데이트 */
    public void updateSuccessUrls(String confluencePageUrl) {
        folderResults.stream()
                .filter(SchedulerFolderResult::isSuccess)
                .forEach(r -> r.setConfluencePageUrl(confluencePageUrl));
    }

    /** Confluence 업로드 실패 시 성공 항목들을 실패로 변경 */
    public void markSuccessAsFailed(String errorMessage) {
        List<SchedulerFolderResult> toFail = folderResults.stream()
                .filter(SchedulerFolderResult::isSuccess)
                .toList();
        toFail.forEach(r -> {
            r.setSuccess(false);
            r.setErrorMessage("Confluence 업로드 실패: " + errorMessage);
        });
        int moved = toFail.size();
        this.successCount -= moved;
        this.failCount += moved;
    }

    public void finalizeStatus() {
        if (failCount == 0) {
            this.status = SchedulerJobStatus.SUCCESS;
        } else if (successCount == 0) {
            this.status = SchedulerJobStatus.FAIL;
        } else {
            this.status = SchedulerJobStatus.PARTIAL_FAIL;
        }
    }

    public List<SchedulerFolderResult> getFolderResults() {
        return Collections.unmodifiableList(folderResults);
    }
}
