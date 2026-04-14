package com.hubilon.modules.confluence.application.service;

import com.hubilon.common.exception.custom.ExternalServiceException;
import com.hubilon.common.exception.custom.NotFoundException;
import com.hubilon.modules.confluence.adapter.in.web.WeeklyConfluenceRequest;
import com.hubilon.modules.confluence.adapter.in.web.WeeklyConfluenceRequest.WeeklyReportRowDto;
import com.hubilon.modules.confluence.adapter.out.external.ConfluenceApiClient;
import com.hubilon.modules.confluence.adapter.out.external.ConfluenceApiClient.ConfluencePage;
import com.hubilon.modules.confluence.application.port.in.UploadWeeklyReportUseCase;
import com.hubilon.modules.confluence.config.ConfluenceProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConfluenceWeeklyReportService implements UploadWeeklyReportUseCase {

    private static final Map<String, Integer> CATEGORY_ORDER = Map.of(
            "DEVELOPMENT", 1,
            "NEW_BUSINESS", 2,
            "OTHER", 3
    );

    private static final Map<String, String> CATEGORY_LABEL = Map.of(
            "DEVELOPMENT", "개발사업",
            "NEW_BUSINESS", "신규추진사업",
            "OTHER", "기타"
    );

    private final ConfluenceProperties properties;
    private final ConfluenceApiClient confluenceApiClient;

    @Override
    public String upload(WeeklyConfluenceRequest request) {
        LocalDate startDate = LocalDate.parse(request.startDate());
        LocalDate endDate = LocalDate.parse(request.endDate());

        String parentId;
        if (properties.parentPageId() != null && !properties.parentPageId().isBlank()) {
            parentId = properties.parentPageId();
        } else {
            parentId = confluenceApiClient
                    .findPageByTitle(properties.spaceKey(), properties.parentPageTitle())
                    .map(ConfluencePage::id)
                    .orElseThrow(() -> new NotFoundException(
                            "Confluence 상위 페이지를 찾을 수 없습니다: " + properties.parentPageTitle()
                    ));
        }

        String pageTitle = buildPageTitle(startDate);
        String xhtml = buildXhtml(request.rows(), startDate, endDate);

        return confluenceApiClient.findPageByTitle(properties.spaceKey(), pageTitle)
                .map(page -> {
                    log.info("Confluence 페이지 수정: title={}, id={}", pageTitle, page.id());
                    return confluenceApiClient.updatePage(page.id(), page.version() + 1, pageTitle, xhtml);
                })
                .orElseGet(() -> {
                    log.info("Confluence 페이지 생성: title={}, parentId={}", pageTitle, parentId);
                    return confluenceApiClient.createPage(properties.spaceKey(), parentId, pageTitle, xhtml);
                });
    }

    private String buildPageTitle(LocalDate startDate) {
        int dayOfMonth = startDate.getDayOfMonth();
        int firstDayOfWeekValue = startDate.withDayOfMonth(1).getDayOfWeek().getValue() % 7; // 0=Sunday
        int weekNumber = (int) Math.ceil((dayOfMonth + firstDayOfWeekValue) / 7.0);
        return startDate.getMonthValue() + "월_" + weekNumber + "주차_주간보고";
    }

    private String buildXhtml(List<WeeklyReportRowDto> rows, LocalDate startDate, LocalDate endDate) {
        int month = startDate.getMonthValue();
        int dayOfMonth = startDate.getDayOfMonth();
        int firstDayOfWeekValue = startDate.withDayOfMonth(1).getDayOfWeek().getValue() % 7;
        int weekNumber = (int) Math.ceil((dayOfMonth + firstDayOfWeekValue) / 7.0);

        LocalDate nextStart = startDate.plusDays(7);
        LocalDate nextEnd = endDate.plusDays(7);

        String startMDD = formatMDD(startDate);
        String endMDD = formatMDD(endDate);
        String nextStartMDD = formatMDD(nextStart);
        String nextEndMDD = formatMDD(nextEnd);

        // 카테고리 정렬 후 그룹핑
        List<WeeklyReportRowDto> sorted = rows.stream()
                .sorted(Comparator.comparingInt(r -> CATEGORY_ORDER.getOrDefault(r.category(), 99)))
                .toList();

        Map<String, List<WeeklyReportRowDto>> grouped = new LinkedHashMap<>();
        for (WeeklyReportRowDto row : sorted) {
            grouped.computeIfAbsent(row.category(), k -> new java.util.ArrayList<>()).add(row);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("<h2>플랫폼개발실 | ").append(month).append("월 ").append(weekNumber).append("주</h2>\n");
        sb.append("<table style=\"width: 100%;\">\n");
        sb.append("  <colgroup>\n");
        sb.append("    <col style=\"width: 8%;\" />\n");
        sb.append("    <col style=\"width: 14%;\" />\n");
        sb.append("    <col style=\"width: 35%;\" />\n");
        sb.append("    <col style=\"width: 35%;\" />\n");
        sb.append("    <col style=\"width: 8%;\" />\n");
        sb.append("  </colgroup>\n");
        sb.append("  <tbody>\n");

        // 헤더 행
        sb.append("    <tr>\n");
        sb.append("      <th style=\"background-color: #dae4f0; text-align: center;\">사업구분</th>\n");
        sb.append("      <th style=\"background-color: #dae4f0; text-align: center;\">프로젝트명</th>\n");
        sb.append("      <th style=\"background-color: #dae4f0; text-align: center;\">금주 진행사항 (")
                .append(escape(startMDD)).append("~").append(escape(endMDD)).append(")</th>\n");
        sb.append("      <th style=\"background-color: #dae4f0; text-align: center;\">차주 진행계획 (")
                .append(escape(nextStartMDD)).append("~").append(escape(nextEndMDD)).append(")</th>\n");
        sb.append("      <th style=\"background-color: #dae4f0; text-align: center;\">담당자</th>\n");
        sb.append("    </tr>\n");

        // 데이터 행
        for (Map.Entry<String, List<WeeklyReportRowDto>> entry : grouped.entrySet()) {
            String category = entry.getKey();
            List<WeeklyReportRowDto> categoryRows = entry.getValue();
            String label = CATEGORY_LABEL.getOrDefault(category, escape(category));
            int rowspan = categoryRows.size();

            for (int i = 0; i < categoryRows.size(); i++) {
                WeeklyReportRowDto row = categoryRows.get(i);
                sb.append("    <tr>\n");

                if (i == 0) {
                    sb.append("      <td rowspan=\"").append(rowspan)
                            .append("\" style=\"background-color: #dae4f0; text-align: center; font-weight: bold;\">")
                            .append(escape(label)).append("</td>\n");
                }

                sb.append("      <td>").append(escape(row.folderName())).append("</td>\n");
                sb.append("      <td>").append(escapeWithBreaks(row.progressSummary())).append("</td>\n");
                sb.append("      <td>").append(escapeWithBreaks(row.planSummary())).append("</td>\n");
                sb.append("      <td style=\"text-align: center;\">")
                        .append(membersToHtml(row.members())).append("</td>\n");
                sb.append("    </tr>\n");
            }
        }

        sb.append("  </tbody>\n");
        sb.append("</table>");

        return sb.toString();
    }

    private String formatMDD(LocalDate date) {
        return date.getMonthValue() + "." + String.format("%02d", date.getDayOfMonth());
    }

    private String escape(String text) {
        if (text == null) return "";
        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private String escapeWithBreaks(String text) {
        if (text == null) return "";
        return escape(text).replace("\n", "<br/>");
    }

    private String membersToHtml(List<String> members) {
        if (members == null || members.isEmpty()) return "";
        return members.stream()
                .map(this::escape)
                .reduce((a, b) -> a + "<br/>" + b)
                .orElse("");
    }
}
