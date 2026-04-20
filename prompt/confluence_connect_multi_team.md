# Confluence 연동 정보 동적 관리 및 멀티 테넌트 적용 요청

현재 `application.yml`에 고정된 단일 Confluence 설정을 DB로 이전하고, '실(Space)' 및 '팀' 단위로 설정을 동적으로 로드하도록 리팩토링하려고 합니다.

## 1. DB 스키마 설계 (테이블 신설 및 수정)

### A. confluence_space_configs (실 단위 설정)
1번 요구사항인 실별 인증 정보와 Space Key를 관리합니다.
- `id`: PK
- `dept_id`: FK (departments.id 참조 - 어느 '실'의 설정인지 구분)
- `user_email`: Confluence 사용자 이메일
- `api_token`: Confluence API Token (암호화 저장)
- `space_key`: 해당 실의 Confluence Space Key
- `base_url`: Confluence Base URL

### B. confluence_team_configs (팀 단위 설정)
2번 요구사항인 팀별 주간보고 경로(부모 폴더 ID)를 관리합니다.
- `id`: PK
- `team_id`: FK (teams.id 참조)
- `parent_page_id`: 해당 팀의 주간보고가 업로드될 부모 폴더 ID

## 2. 동적 설정 적용 로직 (Service Layer)
- **yml 제거:** 기존 `@Value`로 가져오던 `confluence.*` 설정들을 제거합니다.
- **Runtime 로드:** 스케줄러가 동작하거나 사용자가 업로드 버튼을 누를 때, 해당 사업의 `team_id`와 연관된 `dept_id`를 추적하여 DB에서 적절한 `user_email`과 `api_token`을 동적으로 가져와 API 호출 객체를 생성합니다.

## 3. 관리 페이지 구성 (Frontend)
- **실 설정 관리:** 각 실(Department)을 선택하고 해당 실에 적용될 Confluence 인증 정보(이메일, 토큰, 스페이스 키)를 등록/수정하는 UI.
- **팀 경로 관리:** 각 팀별로 주간보고가 쌓일 부모 페이지 ID를 입력하는 UI.

## 4. 요청 사항
- React를 사용하여 실별/팀별 정보를 입력하고 수정할 수 있는 관리 페이지 컴포넌트를 구성해줘.
- **멀티 테넌시 고려:** 특정 팀의 보고서를 업로드할 때, 해당 팀이 속한 실의 인증 정보를 정확히 찾아가는 매핑 로직을 구현해줘.