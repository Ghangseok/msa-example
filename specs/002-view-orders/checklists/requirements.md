# Specification Quality Checklist: 주문 조회

**Purpose**: plan으로 넘어가기 전에 spec이 완전하고 품질 기준을 지키는지 확인한다
**Created**: 2026-10-11
**Feature**: [spec.md](../spec.md)
**기준일**: 2026-10-11. 테스트 케이스 문서에 조회 케이스 9개(main 커밋 `36c23d7`)와 확정 주문 조회 결과를 확인하는 케이스 1개(main 커밋 `0f6bb8f`)가 더해져 병합된 뒤 spec을 고치고 다시 점검했다.

## Content Quality

- [x] No implementation details (languages, frameworks, APIs)
  - 예외 1: 기대값에 들어 있는 HTTP 상태 코드(400, 401, 403, 404)와 역할 이름(`CUSTOMER`, `ADMIN`)은 원문 그대로 두었다.
  - 예외 2 (저장소 루트 `CLAUDE.md` 2절): 인용 블록과 `ID | 출처 | 원문` 표의 원문은 바꾸지 않는다. 그래서 원문에 있던 기술 이름이 남아 있다. 예: Keycloak, Pod, OAuth2 Resource Server, Gateway, JWT.
  - 위 예외 말고는 프레임워크, DB 제품, 라이브러리 이름을 쓰지 않았다. 경로, 필드 이름, 쿼리 매개변수를 쓰지 않았다(`grep`으로 확인했다. 해당 문자열 0건).
- [x] Focused on user value and business needs
- [x] Written for non-technical stakeholders
  - 사용자 지시와 헌법 "검수할 수 있는 산출물" 원칙에 따라, 읽는 사람을 "이 프로젝트를 처음 보는 주니어 개발자"로 바꿔 판정했다.
- [x] All mandatory sections completed
  - User Scenarios & Testing, Requirements, Success Criteria, Assumptions를 모두 채웠다. 템플릿에 없는 절도 더했다. 최상위 절은 "이 spec을 읽는 법", "범위 밖", "용어"이고, User Scenarios 안의 절은 "기능 한눈에 보기", "공통 전제", "우선순위와 PR 단계"다.

## Requirement Completeness

- [x] No [NEEDS CLARIFICATION] markers remain
  - 0개다. 기준 문서에 답이 없던 동작은 사용자가 추천대로 받아들여 테스트 케이스 문서에 더했다. 남은 것은 "plan이 정할 것" 4개(Edge Cases 절 끝)다.
- [x] Requirements are testable and unambiguous
- [x] Success criteria are measurable
  - 문서에 조회의 수치 목표가 없어서 수치를 만들지 않았다. SC-001부터 SC-006까지 시나리오 통과 여부와 "예외 없이"(통과·실패로 가려지는 조건)로 판정한다.
- [x] Success criteria are technology-agnostic (no implementation details)
  - SC의 401, 403, 404는 테스트 케이스의 기대값이라 예외 1에 해당한다.
- [x] All acceptance scenarios are defined
  - 티켓 001 spec이 이 티켓으로 넘긴 조회 줄과 이번에 더해진 케이스 10개를 모두 시나리오 15개로 옮겼다. 2026-10-12에 더해진 정렬·쪽 지정 케이스는 시나리오 2.6으로 옮겨 모두 16개다. 조회와 관련된 테스트 케이스 13개가 모두 들어 있다.
- [x] Edge cases are identified
- [x] Scope is clearly bounded
  - "범위 밖" 절에 유스케이스 문서 4절, 경로·필드·쿼리 매개변수, 티켓 003, 자동 복구를 적었다.
- [x] Dependencies and assumptions identified

## Feature Readiness

- [x] All functional requirements have clear acceptance criteria
  - FR-003의 주문 번호, 주문 항목(상품, 수량), "확정" 상태, 주문 시각은 시나리오 1.5가 확인한다. 상태와 사유는 시나리오 1.4와 2.4가 확인한다.
  - 나머지 FR 12개도 시나리오에 연결되어 있다. 모든 FR 13개에 대응하는 시나리오가 있다.
- [x] User scenarios cover primary flows
- [x] Feature meets measurable outcomes defined in Success Criteria
- [x] No implementation details leak into specification
  - Content Quality 첫 항목의 예외 1, 2와 같다.

## 이 저장소의 추가 확인 (헌법 "검수할 수 있는 산출물", CLAUDE.md 2절·7절)

- [x] 항목 ID를 쓴 모든 자리 바로 아래에 출처와 원문이 있다
  - 스크립트로 확인했다. 인용 블록과 표 밖에서 항목 ID를 쓴 9곳이 모두 2줄 안에 같은 ID의 인용 블록이나 표 행으로 이어진다.
- [x] 옮긴 원문이 기준 문서와 글자 그대로 같다
  - 스크립트로 확인했다. 인용 블록과 표 행에서 160줄을 기준 문서와 비교했다. 불일치 0건. 문장 끝 괄호의 출처 표시를 뺀 원문과도 비교했다.
- [x] 범위 표기(`NNN~NNN`)를 쓰지 않았다
- [x] 기대값을 지어내지 않았다
  - 인수 시나리오의 Given/When/Then은 모두 `docs/test-cases/order-placement.md`의 줄이다. 인용 밖에 다시 적은 "검증하는 줄" 11줄도 원문과 같은지 스크립트로 확인했다. 불일치 0건.
- [x] Claude가 채운 내용을 Assumptions의 "받아들인 제안 모음"에 모았다
  - 사용자가 2026-10-11에 제안 6개를 추천대로 받아들여 본문의 `[제안]` 태그를 지웠다. `grep`으로 확인했다. 본문에 남은 태그 0개다.
- [x] 새 용어를 맨 아래 "용어" 절에 정의했다

## Notes

- **FR-003의 빈칸은 닫혔다**: 확정 주문 조회 결과를 확인하는 케이스가 테스트 케이스 문서에 더해져(PR #28) 시나리오 1.5로 옮겼다. 케이스의 "주문 시각이 나온다"는 값을 못 박지 않고 나오는지만 확인한다.
- **`uc-002`에 "개요" 절이 없는 문제**: `CLAUDE.md` 2-2절은 유스케이스 ID를 쓸 때 "개요" 절을 옮기라고 한다. `docs/requirements/uc-002-view-orders.md`에는 "개요" 절이 없다. 그래서 이 spec은 유스케이스 ID를 쓰지 않고 파일 경로로 가리켰다.
- 검증은 3회 돌렸다. 1회는 케이스가 없던 때, 2회는 케이스 9개가 더해진 뒤, 3회는 확정 주문 조회 케이스가 더해진 뒤다. 3회째에 템플릿 16개와 추가 확인 6개가 모두 통과했다.
- Items marked incomplete require spec updates before `/speckit-clarify` or `/speckit-plan`. 미완료 항목이 없다.
