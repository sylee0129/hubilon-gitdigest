package com.hubilon.modules.confluence;

import com.hubilon.modules.confluence.adapter.in.web.WeeklyConfluenceRequest;
import com.hubilon.modules.confluence.adapter.in.web.WeeklyConfluenceRequest.WeeklyReportRowDto;
import com.hubilon.modules.confluence.adapter.out.external.ConfluenceApiClient;
import com.hubilon.modules.confluence.adapter.out.external.ConfluenceApiClient.ConfluencePage;
import com.hubilon.modules.confluence.application.service.ConfluenceWeeklyReportService;
import com.hubilon.modules.confluence.config.ConfluenceProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConfluenceWeeklyReportServiceTest {

    @Mock
    ConfluenceProperties properties;

    @Mock
    ConfluenceApiClient confluenceApiClient;

    @InjectMocks
    ConfluenceWeeklyReportService service;

    // --- 리플렉션 헬퍼 ---

    private String invokePageTitle(LocalDate startDate) throws Exception {
        Method m = ConfluenceWeeklyReportService.class
                .getDeclaredMethod("buildPageTitle", LocalDate.class);
        m.setAccessible(true);
        return (String) m.invoke(service, startDate);
    }

    @SuppressWarnings("unchecked")
    private String invokeXhtml(List<WeeklyReportRowDto> rows, LocalDate start, LocalDate end) throws Exception {
        Method m = ConfluenceWeeklyReportService.class
                .getDeclaredMethod("buildXhtml", List.class, LocalDate.class, LocalDate.class);
        m.setAccessible(true);
        return (String) m.invoke(service, rows, start, end);
    }

    // ============================================================
    // buildPageTitle 테스트
    // ============================================================

    @Nested
    @DisplayName("buildPageTitle")
    class BuildPageTitle {

        @Test
        @DisplayName("월 첫째 주 — 1일이 일요일인 경우")
        void firstWeek_sundayStart() throws Exception {
            // 2026-03-01 은 일요일
            LocalDate date = LocalDate.of(2026, 3, 1);
            String title = invokePageTitle(date);
            assertThat(title).isEqualTo("3월_1주차_주간보고");
        }

        @Test
        @DisplayName("4월 3주차 — 2026-04-13 (월)")
        void april_week3() throws Exception {
            // 2026-04-13 → 4월 3주차
            LocalDate date = LocalDate.of(2026, 4, 13);
            String title = invokePageTitle(date);
            assertThat(title).isEqualTo("4월_3주차_주간보고");
        }

        @Test
        @DisplayName("월 마지막 주")
        void lastWeek() throws Exception {
            // 2026-04-27 → 4월 5주차
            LocalDate date = LocalDate.of(2026, 4, 27);
            String title = invokePageTitle(date);
            assertThat(title).startsWith("4월_").endsWith("주차_주간보고");
        }

        @Test
        @DisplayName("제목 형식 검증 — {N}월_{W}주차_주간보고")
        void titleFormat() throws Exception {
            LocalDate date = LocalDate.of(2026, 1, 5);
            String title = invokePageTitle(date);
            assertThat(title).matches("\\d+월_\\d+주차_주간보고");
        }
    }

    // ============================================================
    // buildXhtml 테스트
    // ============================================================

    @Nested
    @DisplayName("buildXhtml")
    class BuildXhtml {

        private final LocalDate START = LocalDate.of(2026, 4, 13);
        private final LocalDate END   = LocalDate.of(2026, 4, 17);

        @Test
        @DisplayName("HTML 특수문자 이스케이프 — & < > \" '")
        void htmlEscape() throws Exception {
            WeeklyReportRowDto row = new WeeklyReportRowDto(
                    "DEVELOPMENT",
                    "프로젝트 A&B",
                    List.of("홍길<동>"),
                    "진행 & 완료 <test>",
                    "계획 \"next\" 'step'"
            );

            String xhtml = invokeXhtml(List.of(row), START, END);

            // folderName 이스케이프
            assertThat(xhtml).contains("프로젝트 A&amp;B");
            // members 이스케이프
            assertThat(xhtml).contains("홍길&lt;동&gt;");
            // progressSummary 이스케이프
            assertThat(xhtml).contains("진행 &amp; 완료 &lt;test&gt;");
            // planSummary 이스케이프
            assertThat(xhtml).contains("계획 &quot;next&quot; &#39;step&#39;");

            // 원본 특수문자가 그대로 남아있으면 안 됨
            assertThat(xhtml).doesNotContain("<test>");
        }

        @Test
        @DisplayName("줄바꿈을 <br/>로 변환")
        void newlineToBr() throws Exception {
            WeeklyReportRowDto row = new WeeklyReportRowDto(
                    "DEVELOPMENT",
                    "폴더",
                    List.of(),
                    "라인1\n라인2",
                    "계획1\n계획2"
            );

            String xhtml = invokeXhtml(List.of(row), START, END);

            assertThat(xhtml).contains("라인1<br/>라인2");
            assertThat(xhtml).contains("계획1<br/>계획2");
        }

        @Test
        @DisplayName("헤더에 기간 날짜가 포함된다")
        void headerContainsDates() throws Exception {
            String xhtml = invokeXhtml(List.of(), START, END);

            // 금주 날짜
            assertThat(xhtml).contains("4.13");
            assertThat(xhtml).contains("4.17");
            // 차주 날짜
            assertThat(xhtml).contains("4.20");
            assertThat(xhtml).contains("4.24");
        }

        @Test
        @DisplayName("카테고리 정렬 순서 — DEVELOPMENT, NEW_BUSINESS, OTHER")
        void categoryOrder() throws Exception {
            WeeklyReportRowDto dev = new WeeklyReportRowDto("DEVELOPMENT", "개발", List.of(), "", "");
            WeeklyReportRowDto nb  = new WeeklyReportRowDto("NEW_BUSINESS", "신규", List.of(), "", "");
            WeeklyReportRowDto oth = new WeeklyReportRowDto("OTHER", "기타", List.of(), "", "");

            // 역순으로 전달
            String xhtml = invokeXhtml(List.of(oth, nb, dev), START, END);

            int devIdx  = xhtml.indexOf("개발사업");
            int nbIdx   = xhtml.indexOf("신규추진사업");
            int othIdx  = xhtml.indexOf("기타");

            assertThat(devIdx).isLessThan(nbIdx);
            assertThat(nbIdx).isLessThan(othIdx);
        }

        @Test
        @DisplayName("다중 멤버는 <br/>로 구분")
        void multipleMembers() throws Exception {
            WeeklyReportRowDto row = new WeeklyReportRowDto(
                    "DEVELOPMENT",
                    "폴더",
                    List.of("홍길동", "김철수"),
                    "",
                    ""
            );

            String xhtml = invokeXhtml(List.of(row), START, END);

            assertThat(xhtml).contains("홍길동<br/>김철수");
        }

        @Test
        @DisplayName("rows가 빈 리스트이면 테이블 구조만 생성")
        void emptyRows() throws Exception {
            String xhtml = invokeXhtml(List.of(), START, END);

            assertThat(xhtml).contains("<table ");
            assertThat(xhtml).contains("</table>");
            assertThat(xhtml).contains("<tbody>");
        }

        @Test
        @DisplayName("카테고리 rowspan이 해당 카테고리 행 수와 일치")
        void rowspanMatchesCategoryCount() throws Exception {
            WeeklyReportRowDto dev1 = new WeeklyReportRowDto("DEVELOPMENT", "폴더1", List.of(), "", "");
            WeeklyReportRowDto dev2 = new WeeklyReportRowDto("DEVELOPMENT", "폴더2", List.of(), "", "");
            WeeklyReportRowDto dev3 = new WeeklyReportRowDto("DEVELOPMENT", "폴더3", List.of(), "", "");

            String xhtml = invokeXhtml(List.of(dev1, dev2, dev3), START, END);

            assertThat(xhtml).contains("rowspan=\"3\"");
        }
    }
}
