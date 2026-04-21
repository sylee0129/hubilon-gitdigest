package com.hubilon.modules.confluence;

import com.hubilon.modules.category.domain.port.out.CategoryQueryPort;
import com.hubilon.modules.confluence.adapter.in.web.WeeklyConfluenceRequest;
import com.hubilon.modules.confluence.adapter.in.web.WeeklyConfluenceRequest.WeeklyReportRowDto;
import com.hubilon.modules.confluence.adapter.out.persistence.ConfluenceSpaceConfigRepository;
import com.hubilon.modules.confluence.adapter.out.persistence.ConfluenceTeamConfigRepository;
import com.hubilon.modules.confluence.application.service.ConfluenceClientCache;
import com.hubilon.modules.confluence.application.service.ConfluenceWeeklyReportService;
import com.hubilon.modules.team.adapter.out.persistence.TeamRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class ConfluenceWeeklyReportServiceTest {

    @Mock TeamRepository teamRepository;
    @Mock ConfluenceSpaceConfigRepository spaceConfigRepository;
    @Mock ConfluenceTeamConfigRepository teamConfigRepository;
    @Mock ConfluenceClientCache confluenceClientCache;
    @Mock CategoryQueryPort categoryQueryPort;
    @InjectMocks ConfluenceWeeklyReportService service;

    private String invokePageTitle(LocalDate startDate) throws Exception {
        Method m = ConfluenceWeeklyReportService.class
                .getDeclaredMethod("buildPageTitle", LocalDate.class, String.class);
        m.setAccessible(true);
        return (String) m.invoke(service, startDate, "테스트팀");
    }

    private String invokeXhtml(List<WeeklyReportRowDto> rows, LocalDate start, LocalDate end) throws Exception {
        Class<?> metaClass = null;
        for (Class<?> c : ConfluenceWeeklyReportService.class.getDeclaredClasses()) {
            if (c.getSimpleName().equals("CategoryMeta")) { metaClass = c; break; }
        }
        Map<Long, Integer> order = Map.of(1L, 1, 2L, 2, 3L, 3);
        Map<Long, String> label = Map.of(1L, "개발사업", 2L, "신규추진사업", 3L, "기타");
        Constructor<?> ctor = metaClass.getDeclaredConstructors()[0];
        ctor.setAccessible(true);
        Object meta = ctor.newInstance(order, label);
        Method m = ConfluenceWeeklyReportService.class
                .getDeclaredMethod("buildXhtml", List.class, LocalDate.class, LocalDate.class, metaClass, String.class);
        m.setAccessible(true);
        return (String) m.invoke(service, rows, start, end, meta, "테스트팀");
    }

    @Nested @DisplayName("buildPageTitle")
    class BuildPageTitle {
        @Test @DisplayName("월 첫째 주")
        void firstWeek() throws Exception {
            assertThat(invokePageTitle(LocalDate.of(2026, 3, 1))).isEqualTo("3월_1주차_테스트팀_주간보고");
        }
        @Test @DisplayName("4월 3주차")
        void april_week3() throws Exception {
            assertThat(invokePageTitle(LocalDate.of(2026, 4, 13))).isEqualTo("4월_3주차_테스트팀_주간보고");
        }
        @Test @DisplayName("월 마지막 주")
        void lastWeek() throws Exception {
            assertThat(invokePageTitle(LocalDate.of(2026, 4, 27))).startsWith("4월_").contains("테스트팀").endsWith("주간보고");
        }
        @Test @DisplayName("제목 형식 검증")
        void titleFormat() throws Exception {
            assertThat(invokePageTitle(LocalDate.of(2026, 1, 5))).matches("\\d+월_\\d+주차_.+_주간보고");
        }
    }

    @Nested @DisplayName("buildXhtml")
    class BuildXhtml {
        private final LocalDate START = LocalDate.of(2026, 4, 13);
        private final LocalDate END   = LocalDate.of(2026, 4, 17);

        @Test @DisplayName("HTML 특수문자 이스케이프")
        void htmlEscape() throws Exception {
            WeeklyReportRowDto row = new WeeklyReportRowDto(
                    1L, 1L, "개발사업", "프로젝트 A&B",
                    List.of("홍길<동>"), "진행 & 완료 <test>", "계획");
            String xhtml = invokeXhtml(List.of(row), START, END);
            assertThat(xhtml).contains("프로젝트 A&amp;B");
            assertThat(xhtml).contains("홍길&lt;동&gt;");
            assertThat(xhtml).contains("진행 &amp; 완료 &lt;test&gt;");
            assertThat(xhtml).doesNotContain("<test>");
        }

        @Test @DisplayName("줄바꿈을 br로 변환")
        void newlineToBr() throws Exception {
            WeeklyReportRowDto row = new WeeklyReportRowDto(
                    1L, 1L, "개발사업", "폴더", List.of(), "라인1\n라인2", "계획1\n계획2");
            String xhtml = invokeXhtml(List.of(row), START, END);
            assertThat(xhtml).contains("라인1<br/>라인2");
            assertThat(xhtml).contains("계획1<br/>계획2");
        }

        @Test @DisplayName("헤더에 기간 날짜가 포함된다")
        void headerContainsDates() throws Exception {
            String xhtml = invokeXhtml(List.of(), START, END);
            // formatMDD → M/DD 형식 (예: 4/13)
            assertThat(xhtml).contains("4/13").contains("4/17").contains("4/20").contains("4/24");
        }

        @Test @DisplayName("카테고리 정렬 순서 — sortOrder 기준")
        void categoryOrder() throws Exception {
            WeeklyReportRowDto dev = new WeeklyReportRowDto(1L, 1L, "개발사업", "개발", List.of(), "", "");
            WeeklyReportRowDto nb  = new WeeklyReportRowDto(2L, 2L, "신규추진사업", "신규", List.of(), "", "");
            WeeklyReportRowDto oth = new WeeklyReportRowDto(3L, 3L, "기타", "기타", List.of(), "", "");
            String xhtml = invokeXhtml(List.of(oth, nb, dev), START, END);
            assertThat(xhtml.indexOf("개발사업")).isLessThan(xhtml.indexOf("신규추진사업"));
            assertThat(xhtml.indexOf("신규추진사업")).isLessThan(xhtml.indexOf("기타"));
        }

        @Test @DisplayName("categoryId=null이면 기타 그룹으로 처리")
        void nullCategoryIdFallsToEtc() throws Exception {
            WeeklyReportRowDto row = new WeeklyReportRowDto(null, null, null, "폴더", List.of(), "진행", "계획");
            assertThat(invokeXhtml(List.of(row), START, END)).contains("기타");
        }

        @Test @DisplayName("categoryId=null이고 categoryName이 있으면 categoryName을 레이블로 사용")
        void nullCategoryIdWithCategoryName() throws Exception {
            WeeklyReportRowDto row = new WeeklyReportRowDto(null, null, "특수사업", "폴더", List.of(), "진행", "계획");
            assertThat(invokeXhtml(List.of(row), START, END)).contains("특수사업");
        }

        @Test @DisplayName("다중 멤버는 br로 구분")
        void multipleMembers() throws Exception {
            WeeklyReportRowDto row = new WeeklyReportRowDto(
                    1L, 1L, "개발사업", "폴더", List.of("홍길동", "김철수"), "", "");
            assertThat(invokeXhtml(List.of(row), START, END)).contains("홍길동<br/>김철수");
        }

        @Test @DisplayName("rows가 빈 리스트이면 테이블 구조만 생성")
        void emptyRows() throws Exception {
            String xhtml = invokeXhtml(List.of(), START, END);
            assertThat(xhtml).contains("<table ").contains("</table>").contains("<tbody>");
        }

        @Test @DisplayName("카테고리 rowspan이 해당 카테고리 행 수와 일치")
        void rowspanMatchesCategoryCount() throws Exception {
            WeeklyReportRowDto dev1 = new WeeklyReportRowDto(1L, 1L, "개발사업", "폴더1", List.of(), "", "");
            WeeklyReportRowDto dev2 = new WeeklyReportRowDto(1L, 1L, "개발사업", "폴더2", List.of(), "", "");
            WeeklyReportRowDto dev3 = new WeeklyReportRowDto(1L, 1L, "개발사업", "폴더3", List.of(), "", "");
            assertThat(invokeXhtml(List.of(dev1, dev2, dev3), START, END)).contains("rowspan=\"3\"");
        }
    }
}
