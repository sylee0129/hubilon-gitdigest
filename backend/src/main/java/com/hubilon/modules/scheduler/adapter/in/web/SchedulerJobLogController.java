package com.hubilon.modules.scheduler.adapter.in.web;

import com.hubilon.common.exception.custom.NotFoundException;
import com.hubilon.common.page.PageResult;
import com.hubilon.common.response.Response;
import com.hubilon.modules.scheduler.domain.model.SchedulerFolderResult;
import com.hubilon.modules.scheduler.domain.model.SchedulerJobLog;
import com.hubilon.modules.scheduler.domain.port.in.SchedulerJobLogQueryUseCase;
import com.hubilon.modules.scheduler.domain.port.in.SchedulerTriggerUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Tag(name = "Scheduler", description = "주간보고 스케줄러 잡 로그")
@RestController
@RequestMapping("/api/scheduler")
@RequiredArgsConstructor
public class SchedulerJobLogController {

    private final SchedulerJobLogQueryUseCase schedulerJobLogQueryUseCase;
    private final SchedulerTriggerUseCase schedulerTriggerUseCase;

    @Operation(summary = "스케줄러 잡 로그 목록 조회")
    @GetMapping("/logs")
    public Response<PageResult<SchedulerJobLogResponse>> getLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        Page<SchedulerJobLog> result = schedulerJobLogQueryUseCase.findAll(pageable);

        List<SchedulerJobLogResponse> content = result.getContent().stream()
                .map(SchedulerJobLogController::toResponse)
                .toList();

        return Response.ok(PageResult.of(content, page, size, result.getTotalElements()));
    }

    @Operation(summary = "스케줄러 잡 로그 상세 조회")
    @GetMapping("/logs/{id}")
    public Response<SchedulerJobLogDetailResponse> getLog(@PathVariable Long id) {
        SchedulerJobLog log = schedulerJobLogQueryUseCase.findById(id)
                .orElseThrow(() -> new NotFoundException("스케줄러 잡 로그를 찾을 수 없습니다. id=" + id));
        return Response.ok(toDetailResponse(log));
    }

    @Operation(summary = "스케줄러 수동 실행")
    @PostMapping("/trigger")
    public ResponseEntity<Response<SchedulerJobLogDetailResponse>> trigger() {
        SchedulerJobLog result = schedulerTriggerUseCase.trigger();
        return ResponseEntity.ok(Response.ok(toDetailResponse(result)));
    }

    private static SchedulerJobLogResponse toResponse(SchedulerJobLog log) {
        return new SchedulerJobLogResponse(
                log.getId(),
                log.getExecutedAt(),
                log.getStatus().name(),
                log.getTotalFolderCount(),
                log.getSuccessCount(),
                log.getFailCount(),
                log.getCreatedAt()
        );
    }

    private static SchedulerJobLogDetailResponse toDetailResponse(SchedulerJobLog log) {
        List<SchedulerFolderResultResponse> results = log.getFolderResults().stream()
                .map(r -> new SchedulerFolderResultResponse(
                        r.getId(),
                        r.getFolderId(),
                        r.getFolderName(),
                        r.isSuccess(),
                        r.getErrorMessage(),
                        r.getConfluencePageUrl()
                ))
                .toList();

        return new SchedulerJobLogDetailResponse(
                log.getId(),
                log.getExecutedAt(),
                log.getStatus().name(),
                log.getTotalFolderCount(),
                log.getSuccessCount(),
                log.getFailCount(),
                log.getCreatedAt(),
                results
        );
    }

    public record SchedulerJobLogResponse(
            Long id,
            LocalDateTime executedAt,
            String status,
            int totalFolderCount,
            int successCount,
            int failCount,
            LocalDateTime createdAt
    ) {}

    public record SchedulerJobLogDetailResponse(
            Long id,
            LocalDateTime executedAt,
            String status,
            int totalFolderCount,
            int successCount,
            int failCount,
            LocalDateTime createdAt,
            List<SchedulerFolderResultResponse> folderResults
    ) {}

    public record SchedulerFolderResultResponse(
            Long id,
            Long folderId,
            String folderName,
            boolean success,
            String errorMessage,
            String confluencePageUrl
    ) {}
}
