package com.hubilon.modules.report;

import com.hubilon.modules.report.adapter.out.ai.AiSummaryAdapter;
import com.hubilon.modules.report.domain.model.FolderAiSummaryResult;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class AiSummaryAdapterParseFolderSummaryTest {

    private final AiSummaryAdapter adapter = new AiSummaryAdapter();

    private FolderAiSummaryResult parse(String raw) {
        return (FolderAiSummaryResult) ReflectionTestUtils.invokeMethod(adapter, "parseFolderSummary", raw);
    }

    @Test
    void 정상케이스_두_구분자_모두_있는_경우() {
        String raw = """
                [금주 진행사항 (04/01~04/07)]
                - 홍길동: 로그인 기능 구현
                - 김철수: 대시보드 UI 개선

                [차주 진행계획 (04/08~04/14)]
                - 홍길동: 로그아웃 기능 완료 예정
                - 김철수: 차트 컴포넌트 개발 예정
                """;

        FolderAiSummaryResult result = parse(raw);

        assertThat(result.progressSummary()).contains("[금주 진행사항");
        assertThat(result.progressSummary()).contains("홍길동: 로그인 기능 구현");
        assertThat(result.progressSummary()).doesNotContain("[차주 진행계획");
        assertThat(result.planSummary()).contains("[차주 진행계획");
        assertThat(result.planSummary()).contains("홍길동: 로그아웃 기능 완료 예정");
        assertThat(result.aiUsed()).isTrue();
    }

    @Test
    void fallback_케이스_구분자_없는_경우_전체가_progressSummary에_들어가고_planSummary는_자동추론불가() {
        String raw = "구분자 없는 텍스트입니다. AI가 형식을 맞추지 못한 경우입니다.";

        FolderAiSummaryResult result = parse(raw);

        assertThat(result.progressSummary()).isEqualTo("구분자 없는 텍스트입니다. AI가 형식을 맞추지 못한 경우입니다.");
        assertThat(result.planSummary()).isEqualTo("(자동 추론 불가)");
        assertThat(result.aiUsed()).isTrue();
    }

    @Test
    void 금주_구분자만_있고_차주_구분자_없는_경우() {
        String raw = """
                [금주 진행사항 (04/01~04/07)]
                - 홍길동: 기능 구현 완료
                """;

        FolderAiSummaryResult result = parse(raw);

        assertThat(result.progressSummary()).contains("[금주 진행사항");
        assertThat(result.planSummary()).isEqualTo("(자동 추론 불가)");
        assertThat(result.aiUsed()).isTrue();
    }

    @Test
    void HTML_태그가_포함된_경우_sanitize_처리() {
        String raw = """
                [금주 진행사항 (04/01~04/07)]
                - <b>홍길동</b>: <em>로그인</em> 기능 구현

                [차주 진행계획 (04/08~04/14)]
                - <b>홍길동</b>: 로그아웃 예정
                """;

        FolderAiSummaryResult result = parse(raw);

        assertThat(result.progressSummary()).doesNotContain("<b>");
        assertThat(result.progressSummary()).doesNotContain("</b>");
        assertThat(result.progressSummary()).contains("홍길동");
        assertThat(result.planSummary()).doesNotContain("<b>");
        assertThat(result.planSummary()).contains("홍길동");
    }
}
