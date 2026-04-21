package com.hubilon.modules.confluence.application.service;

import com.hubilon.common.exception.custom.NotFoundException;
import com.hubilon.modules.category.domain.model.Category;
import com.hubilon.modules.category.domain.port.out.CategoryQueryPort;
import com.hubilon.modules.confluence.adapter.in.web.WeeklyConfluenceRequest;
import com.hubilon.modules.confluence.adapter.in.web.WeeklyConfluenceRequest.WeeklyReportRowDto;
import com.hubilon.modules.confluence.adapter.out.external.ConfluenceApiClient;
import com.hubilon.modules.confluence.adapter.out.persistence.ConfluenceSpaceConfigJpaEntity;
import com.hubilon.modules.confluence.adapter.out.persistence.ConfluenceSpaceConfigRepository;
import com.hubilon.modules.confluence.adapter.out.persistence.ConfluenceTeamConfigJpaEntity;
import com.hubilon.modules.confluence.adapter.out.persistence.ConfluenceTeamConfigRepository;
import com.hubilon.modules.confluence.application.port.in.UploadWeeklyReportUseCase;
import com.hubilon.modules.team.adapter.out.persistence.TeamJpaEntity;
import com.hubilon.modules.team.adapter.out.persistence.TeamRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConfluenceWeeklyReportService implements UploadWeeklyReportUseCase {

    private record CategoryMeta(Map<Long, Integer> order, Map<Long, String> label) {}

    private final TeamRepository teamRepository;
    private final ConfluenceSpaceConfigRepository spaceConfigRepository;
    private final ConfluenceTeamConfigRepository teamConfigRepository;
    private final ConfluenceClientCache confluenceClientCache;
    private final CategoryQueryPort categoryQueryPort;

    @Override
    public String upload(WeeklyConfluenceRequest request) {
        Long teamId = request.teamId();
        if (teamId == null) {
            throw new NotFoundException("teamId가 필요합니다.");
        }

        TeamJpaEntity team = teamRepository.findById(teamId)
                .orElseThrow(() -> new NotFoundException("팀을 찾을 수 없습니다. teamId=" + teamId));
        Long deptId = team.getDeptId();

        ConfluenceSpaceConfigJpaEntity spaceConfig = spaceConfigRepository.findByDeptId(deptId)
                .orElseThrow(() -> new NotFoundException(
                        "Confluence Space 설정이 없습니다. deptId=" + deptId));

        ConfluenceApiClient client = confluenceClientCache.get(deptId);

        ConfluenceTeamConfigJpaEntity teamConfig = teamConfigRepository.findByTeamId(teamId)
                .orElseThrow(() -> new NotFoundException(
                        "Confluence Team 설정이 없습니다. teamId=" + teamId));

        String spaceKey = spaceConfig.getSpaceKey();
        String parentPageId = teamConfig.getParentPageId();

        LocalDate startDate = LocalDate.parse(request.startDate());
        LocalDate endDate = LocalDate.parse(request.endDate());

        CategoryMeta categoryMeta = loadCategoryMeta();
        String pageTitle = buildPageTitle(startDate);
        String xhtml = buildXhtml(request.rows(), startDate, endDate, categoryMeta);

        return client.findPageByTitle(spaceKey, pageTitle)
                .map(page -> {
                    log.info("Confluence 페이지 수정: title={}, id={}", pageTitle, page.id());
                    return client.updatePage(page.id(), page.version() + 1, pageTitle, xhtml);
                })
                .orElseGet(() -> {
                    log.info("Confluence 페이지 생성: title={}, parentId={}", pageTitle, parentPageId);
                    return client.createPage(spaceKey, parentPageId, pageTitle, xhtml);
                });
    }

    private CategoryMeta loadCategoryMeta() {
        List<Category> categories = categoryQueryPort.findAllOrderBySortOrder();
        Map<Long, Integer> order = categories.stream()
                .collect(Collectors.toMap(Category::getId, Category::getSortOrder));
        Map<Long, String> label = categories.stream()
                .collect(Collectors.toMap(Category::getId, Category::getName));
        return new CategoryMeta(order, label);
    }

    private String buildPageTitle(LocalDate startDate) {
        int dayOfMonth = startDate.getDayOfMonth();
        int firstDayOfWeekValue = startDate.withDayOfMonth(1).getDayOfWeek().getValue() % 7;
        int weekNumber = (int) Math.ceil((dayOfMonth + firstDayOfWeekValue) / 7.0);
        return startDate.getMonthValue() + "월_" + weekNumber + "주차_주간보고";
    }

    private String buildXhtml(List<WeeklyReportRowDto> rows, LocalDate startDate, LocalDate endDate,
                               CategoryMeta categoryMeta) {
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

        List<WeeklyReportRowDto> sorted = rows.stream()
                .sorted(Comparator.comparingInt(r ->
                        r.categoryId() != null ? categoryMeta.order().getOrDefault(r.categoryId(), 99) : 99))
                .toList();

        Map<Long, List<WeeklyReportRowDto>> grouped = new LinkedHashMap<>();
        for (WeeklyReportRowDto row : sorted) {
            Long key = row.categoryId() != null ? row.categoryId() : -1L;
            grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(row);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("<h2>플랫폼개발팀 | ").append(month).append("월 ").append(weekNumber).append("주</h2>\n");
        sb.append("<table style=\"width: 100%;\">\n");
        sb.append("  <colgroup>\n");
        sb.append("    <col style=\"width: 8%;\" />\n");
        sb.append("    <col style=\"width: 14%;\" />\n");
        sb.append("    <col style=\"width: 35%;\" />\n");
        sb.append("    <col style=\"width: 35%;\" />\n");
        sb.append("    <col style=\"width: 8%;\" />\n");
        sb.append("  </colgroup>\n");
        sb.append("  <tbody>\n");

        sb.append("    <tr>\n");
        sb.append("      <th style=\"background-color: #dae4f0; text-align: center;\">사업구분</th>\n");
        sb.append("      <th style=\"background-color: #dae4f0; text-align: center;\">프로젝트명</th>\n");
        sb.append("      <th style=\"background-color: #dae4f0; text-align: center;\">금주 진행사항 (")
                .append(escape(startMDD)).append("~").append(escape(endMDD)).append(")</th>\n");
        sb.append("      <th style=\"background-color: #dae4f0; text-align: center;\">차주 진행계획 (")
                .append(escape(nextStartMDD)).append("~").append(escape(nextEndMDD)).append(")</th>\n");
        sb.append("      <th style=\"background-color: #dae4f0; text-align: center;\">담당자</th>\n");
        sb.append("    </tr>\n");

        for (Map.Entry<Long, List<WeeklyReportRowDto>> entry : grouped.entrySet()) {
            Long categoryId = entry.getKey();
            List<WeeklyReportRowDto> categoryRows = entry.getValue();

            String label;
            if (categoryId == -1L) {
                WeeklyReportRowDto first = categoryRows.get(0);
                label = first.categoryName() != null ? first.categoryName() : "기타";
            } else {
                WeeklyReportRowDto first = categoryRows.get(0);
                label = categoryMeta.label().getOrDefault(categoryId,
                        first.categoryName() != null ? first.categoryName() : "기타");
            }

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
