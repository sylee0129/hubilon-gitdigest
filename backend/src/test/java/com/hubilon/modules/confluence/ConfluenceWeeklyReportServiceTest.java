package com.hubilon.modules.confluence;

import com.hubilon.modules.category.domain.port.out.CategoryQueryPort;
import com.hubilon.modules.confluence.adapter.in.web.WeeklyConfluenceRequest;
import com.hubilon.modules.confluence.adapter.in.web.WeeklyConfluenceRequest.WeeklyReportRowDto;
import com.hubilon.modules.confluence.adapter.out.external.ConfluenceApiClient;
import com.hubilon.modules.confluence.adapter.out.external.ConfluenceApiClient.ConfluencePage;
import com.hubilon.modules.confluence.adapter.out.external.ConfluenceApiClient.ConfluencePageInfo;
import com.hubilon.modules.confluence.adapter.out.persistence.ConfluenceSpaceConfigRepository;
import com.hubilon.modules.confluence.adapter.out.persistence.ConfluenceTeamConfigJpaEntity;
import com.hubilon.modules.confluence.adapter.out.persistence.ConfluenceTeamConfigRepository;
import com.hubilon.modules.confluence.application.service.ConfluenceClientCache;
import com.hubilon.modules.confluence.application.service.ConfluenceWeeklyReportService;
import com.hubilon.modules.report.domain.port.in.FolderSummaryAiSummarizeUseCase;
import com.hubilon.modules.report.domain.port.out.FolderSummaryQueryPort;
import com.hubilon.modules.team.adapter.out.persistence.TeamJpaEntity;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConfluenceWeeklyReportServiceTest {

    @Mock TeamRepository teamRepository;
    @Mock ConfluenceSpaceConfigRepository spaceConfigRepository;
    @Mock ConfluenceTeamConfigRepository teamConfigRepository;
    @Mock ConfluenceClientCache confluenceClientCache;
    @Mock CategoryQueryPort categoryQueryPort;
    @Mock FolderSummaryQueryPort folderSummaryQueryPort;
    @Mock FolderSummaryAiSummarizeUseCase folderSummaryAiSummarizeUseCase;
    @InjectMocks ConfluenceWeeklyReportService service;

    // ─────────────────────────────────────────────────
    // 헬퍼: private 메서드 reflective 호출
    // ─────────────────────────────────────────────────

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

    /**
     * resolveMonthlyParentPageId 호출 헬퍼.
     * teamConfig, spaceKey, team, displayMonday, email 을 직접 넘긴다.
     */
    private String invokeResolveMonthlyParentPageId(
            ConfluenceTeamConfigJpaEntity teamConfig,
            String spaceKey,
            TeamJpaEntity team,
            LocalDate displayMonday,
            String email
    ) throws Exception {
        Method m = ConfluenceWeeklyReportService.class.getDeclaredMethod(
                "resolveMonthlyParentPageId",
                ConfluenceTeamConfigJpaEntity.class,
                String.class,
                TeamJpaEntity.class,
                LocalDate.class,
                String.class
        );
        m.setAccessible(true);
        return (String) m.invoke(service, teamConfig, spaceKey, team, displayMonday, email);
    }

    // ─────────────────────────────────────────────────
    // buildPageTitle
    // ─────────────────────────────────────────────────

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

    // ─────────────────────────────────────────────────
    // buildXhtml
    // ─────────────────────────────────────────────────

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

    // ─────────────────────────────────────────────────
    // resolveMonthlyParentPageId
    // ─────────────────────────────────────────────────

    @Nested @DisplayName("resolveMonthlyParentPageId")
    class ResolveMonthlyParentPageId {

        private static final String SPACE_KEY = "TEAM";
        private static final Long   DEPT_ID   = 1L;
        private static final Long   TEAM_ID   = 10L;
        private static final LocalDate MONDAY  = LocalDate.of(2026, 4, 13); // 4월

        private TeamJpaEntity team() {
            return TeamJpaEntity.builder()
                    .id(TEAM_ID).name("테스트팀").deptId(DEPT_ID).build();
        }

        private ConfluenceTeamConfigJpaEntity teamConfig(String pageId) {
            return ConfluenceTeamConfigJpaEntity.builder()
                    .id(1L).teamId(TEAM_ID).parentPageId(pageId).build();
        }

        /** 케이스 1: space에서 월 페이지 발견 + ID가 이미 동일 → DB 갱신 없음 */
        @Test @DisplayName("space에서 월 페이지 발견, ID 동일 → DB 갱신 없음")
        void case1_found_same_id() throws Exception {
            String existingId = "page-100";
            ConfluenceApiClient mockClient = mock(ConfluenceApiClient.class);
            when(confluenceClientCache.get(DEPT_ID)).thenReturn(mockClient);
            when(mockClient.findPageByTitleInSpace(SPACE_KEY, "테스트팀_4월"))
                    .thenReturn(Optional.of(new ConfluencePage(existingId, 1, "http://example.com")));

            ConfluenceTeamConfigJpaEntity config = teamConfig(existingId);
            String result = invokeResolveMonthlyParentPageId(config, SPACE_KEY, team(), MONDAY, "user@test.com");

            assertThat(result).isEqualTo(existingId);
            verify(teamConfigRepository, never()).save(any());
        }

        /** 케이스 2: space에서 월 페이지 발견 + ID가 다름 → DB 자동 갱신 */
        @Test @DisplayName("space에서 월 페이지 발견, ID 다름 → DB 자동 갱신")
        void case2_found_different_id() throws Exception {
            String storedId  = "page-old";
            String freshId   = "page-new";
            ConfluenceApiClient mockClient = mock(ConfluenceApiClient.class);
            when(confluenceClientCache.get(DEPT_ID)).thenReturn(mockClient);
            when(mockClient.findPageByTitleInSpace(SPACE_KEY, "테스트팀_4월"))
                    .thenReturn(Optional.of(new ConfluencePage(freshId, 1, "http://example.com")));

            ConfluenceTeamConfigJpaEntity config = teamConfig(storedId);
            String result = invokeResolveMonthlyParentPageId(config, SPACE_KEY, team(), MONDAY, "user@test.com");

            assertThat(result).isEqualTo(freshId);
            verify(teamConfigRepository, times(1)).save(config);
            assertThat(config.getParentPageId()).isEqualTo(freshId);
        }

        /**
         * 케이스 3: 월 페이지 미발견 + storedPageId가 `_n월` 패턴
         * → getPageInfo로 부모 ID 조회 후 그 부모 아래에 신규 월 페이지 생성
         */
        @Test @DisplayName("월 페이지 미발견, storedPageId가 _n월 패턴 → 부모 조회 후 신규 생성")
        void case3_not_found_stored_is_monthly() throws Exception {
            String storedId  = "page-month-stored";
            String parentId  = "page-root";
            String createdId = "page-created";
            ConfluenceApiClient mockClient = mock(ConfluenceApiClient.class);
            when(confluenceClientCache.get(DEPT_ID)).thenReturn(mockClient);
            when(mockClient.findPageByTitleInSpace(SPACE_KEY, "테스트팀_4월"))
                    .thenReturn(Optional.empty());
            // storedPageId 의 title 이 "_3월" 패턴 → isMonthlyPageTitle == true
            when(mockClient.getPageInfo(storedId))
                    .thenReturn(Optional.of(new ConfluencePageInfo(storedId, "테스트팀_3월", parentId)));
            when(mockClient.createPage(SPACE_KEY, parentId, "테스트팀_4월", "", true))
                    .thenReturn(new ConfluencePage(createdId, 1, "http://example.com"));

            ConfluenceTeamConfigJpaEntity config = teamConfig(storedId);
            String result = invokeResolveMonthlyParentPageId(config, SPACE_KEY, team(), MONDAY, "user@test.com");

            assertThat(result).isEqualTo(createdId);
            verify(mockClient).createPage(SPACE_KEY, parentId, "테스트팀_4월", "", true);
            verify(teamConfigRepository, times(1)).save(config);
        }

        /**
         * 케이스 4: 월 페이지 미발견 + storedPageId가 루트 페이지 (제목이 _n월 패턴 아님)
         * → storedPageId 를 루트로 신규 월 페이지 생성
         */
        @Test @DisplayName("월 페이지 미발견, storedPageId가 루트 패턴 → storedPageId를 루트로 신규 생성")
        void case4_not_found_stored_is_root() throws Exception {
            String storedId  = "page-root";
            String createdId = "page-created";
            ConfluenceApiClient mockClient = mock(ConfluenceApiClient.class);
            when(confluenceClientCache.get(DEPT_ID)).thenReturn(mockClient);
            when(mockClient.findPageByTitleInSpace(SPACE_KEY, "테스트팀_4월"))
                    .thenReturn(Optional.empty());
            // storedPageId 의 title 이 일반 제목 → isMonthlyPageTitle == false
            when(mockClient.getPageInfo(storedId))
                    .thenReturn(Optional.of(new ConfluencePageInfo(storedId, "테스트팀 루트", "page-grand-parent")));
            when(mockClient.createPage(SPACE_KEY, storedId, "테스트팀_4월", "", true))
                    .thenReturn(new ConfluencePage(createdId, 1, "http://example.com"));

            ConfluenceTeamConfigJpaEntity config = teamConfig(storedId);
            String result = invokeResolveMonthlyParentPageId(config, SPACE_KEY, team(), MONDAY, "user@test.com");

            assertThat(result).isEqualTo(createdId);
            verify(mockClient).createPage(SPACE_KEY, storedId, "테스트팀_4월", "", true);
            verify(teamConfigRepository, times(1)).save(config);
        }
    }
}
