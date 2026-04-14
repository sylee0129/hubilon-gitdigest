# Role: Hubilon GitDigest to Confluence Uploader (with Fallback Logic)

## 1. 개요
- Hubilon GitDigest 데이터를 추출하여 **기존 주간보고 엑셀 템플릿과 동일한 본문 구성**으로 Confluence 페이지를 생성한다.
- 데이터가 누락된 경우 지정된 기본 문구로 본문을 채운다.

## 2. 연동 및 대상 정보
- **URL**: https://hubilon-platform-dev-team.atlassian.net/wiki/spaces/Y2yJ6hFKUNFK
- **Space Key**: `Y2yJ6hFKUNFK`
- **Parent Page**: `주간보고` 폴더
- **페이지 제목**: `M월_N주차_주간보고` (현재 날짜 기준 자동 계산)

## 3. 본문 구성 
Confluence 페이지 본문은 아래의 엑셀 구조를 표(Table) 형태로 동일하게 재현한다.
- @sample/04월3주_주간보고_플랫폼개발실_20260413.xlsx 파일 참고
- @frontend/src/utils/weeklyExcelExport.ts 참고

## 5. 실행 절차
1. `confluence-mcp`를 통해 `주간보고` 상위 페이지 ID를 조회한다.
2. 현재 날짜를 분석하여 `4월_3주차_주간보고` 형식을 생성한다.
2. **XHTML 변환**: Confluence 전용 표(Table) 서식을 생성한다. 엑셀의 헤더 색상이나 테두리 느낌이 나도록 깔끔하게 구성한다.
3. **업로드**: 
    - 동일 제목 페이지 유무 확인 후 `create_content`를 호출하여 생성한다.