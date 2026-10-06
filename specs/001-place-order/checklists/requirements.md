# Specification Quality Checklist: 주문 생성

**Purpose**: plan으로 넘어가기 전에 spec이 완전하고 품질 기준을 지키는지 확인한다
**Created**: 2026-10-05
**Feature**: [spec.md](../spec.md)
**기준일**: 2026-10-05. spec을 처음 만들 때의 점검이다. 그 뒤의 변화(테스트 케이스 24개, 아래 Notes 두 항목의 해결)는 반영하지 않았다.

## Content Quality

- [x] No implementation details (languages, frameworks, APIs)
  - 예외 1 (사용자 지시): 기대값에 들어 있는 HTTP 상태 코드(200, 400, 401, 409, 422, 500, 502·503·504), 헤더 이름(`Idempotency-Key`), UUID, 서비스 이름(주문 서비스, 재고 서비스), 예약 결과 값(RESERVED, REJECTED, RELEASED)은 기술 중립 원칙의 예외로 원문 그대로 두었다.
  - 예외 2 (저장소 루트 `CLAUDE.md` 2절): 인용 블록과 `ID | 출처 | 원문` 표의 원문은 바꾸지 않는다. 그래서 원문에 있던 기술 이름이 남아 있다. 예: `ORA-00060`, WireMock, Pod, Java 17, Tomcat, Keycloak, `CUSTOMER`, `ADMIN`.
  - 예외 3: Key Entities의 상태 그림은 도메인 분석 4절, 5절의 그림을 그대로 옮겨서 영어 상태 이름(PENDING 등)이 함께 있다.
  - 위 예외 말고는 프레임워크, DB 제품, 라이브러리 이름을 쓰지 않았다. 용어 절에서 원문에 나온 기술 이름을 정의할 때만 이름을 적었다.
- [x] Focused on user value and business needs
- [x] Written for non-technical stakeholders
  - 사용자 지시와 헌법 "검수할 수 있는 산출물" 원칙에 따라, 읽는 사람을 "이 프로젝트를 처음 보는 주니어 개발자"로 바꿔 판정했다.
- [x] All mandatory sections completed
  - User Scenarios & Testing, Requirements, Success Criteria, Assumptions를 모두 채웠다. 템플릿에 없는 절도 더했다. 최상위 절은 "이 spec을 읽는 법", "범위 밖", "용어"이고, User Scenarios 안의 절은 "기능 한눈에 보기", "공통 전제", "우선순위와 PR 단계"다.

## Requirement Completeness

- [x] No [NEEDS CLARIFICATION] markers remain
  - 0개다. 기준 문서에 답이 없는 곳 11개는 `[제안]`으로 채우고 Assumptions의 "`[제안]` 모음"에 모았다. 사용자가 검토해야 한다.
- [x] Requirements are testable and unambiguous
- [x] Success criteria are measurable
  - SC-001부터 SC-005까지 모두 시간(31초, 1초, 0.5초, 0.8~1.5초)이나 횟수(6번)로 판정한다.
- [x] Success criteria are technology-agnostic (no implementation details)
  - SC-005의 "200으로 응답"은 테스트 케이스의 기대값이라 예외 1에 해당한다.
- [x] All acceptance scenarios are defined
  - 테스트 케이스 15개를 모두 사용자 스토리에 배정했다. 두 스토리에 걸친 케이스 하나는 재고 쪽(시나리오 1.4)과 주문 쪽(시나리오 7.2)으로 나눴다.
  - 조회가 섞인 두 케이스의 조회 부분(시나리오 3.1의 "C1이 조회할 수 있다", 시나리오 4.1의 셋째 줄부터 끝까지)은 사용자 결정에 따라 티켓 002에서 검증한다고 적었다.
- [x] Edge cases are identified
- [x] Scope is clearly bounded
  - "범위 밖" 절에 티켓 002, 티켓 003, 유스케이스 8절 "범위 밖", 자동 복구를 적었다.
- [x] Dependencies and assumptions identified

## Feature Readiness

- [x] All functional requirements have clear acceptance criteria
  - 모든 FR이 확인할 수 있는 문장으로 되어 있다. 다만 아래 FR은 테스트 케이스 문서에 대응하는 인수 시나리오가 없다. 테스트의 기대값을 어디서 가져올지는 사용자가 정해야 한다(Notes 참고).
- [x] User scenarios cover primary flows
- [x] Feature meets measurable outcomes defined in Success Criteria
- [x] No implementation details leak into specification
  - Content Quality 첫 항목의 예외 1~3과 같다.

## 이 저장소의 추가 확인 (헌법 "검수할 수 있는 산출물", CLAUDE.md 2절·7절)

- [x] 항목 ID를 쓴 모든 자리 바로 아래에 출처와 원문이 있다
  - 스크립트로 확인했다. 본문 줄의 ID는 다음 줄이 인용 블록이나 표인지, 인용·표 묶음 안의 ID는 같은 묶음 안에 펼친 항목이 있는지 검사했다. 문제 0건.
- [x] 옮긴 원문이 기준 문서와 글자 그대로 같다
  - 스크립트로 확인했다. 테스트 케이스 15개 본문(16곳), 업무 규칙, 대체 흐름, 미결 사항, 비기능 요구사항, 유스케이스 개요를 원본과 비교했다. 불일치 0건.
- [x] 범위 표기(`NNN~NNN`)를 쓰지 않았다
- [x] 기대값을 지어내지 않았다
  - 인수 시나리오의 Given/When/Then은 모두 원문 인용이다. 원문에 없는 줄은 시나리오 7.2의 상황 만들기 한 줄뿐이고 `[제안]`을 붙였다.
- [x] Claude가 채운 내용에 `[제안]`을 붙이고 Assumptions에 모았다
- [x] 새 용어를 맨 아래 "용어" 절에 정의했다

## Notes

- **인수 시나리오가 없는 FR**: 아래 FR(또는 FR의 일부)은 테스트 케이스 문서에 대응하는 시나리오가 없다. 헌법 "실제 조건에서 테스트 먼저" 원칙에 따라 테스트는 만들지만, 기대값의 출처를 사용자가 정해야 한다. 기준 문서(`docs/requirements/`, 설계 문서)의 문장을 기대값으로 쓸지, 테스트 케이스 문서에 케이스를 더할지 고른다.
  - FR-009의 "거절됨이면 그대로 둔다"
  - FR-011 (예약 기록을 지우지 않는다)
  - FR-012 (재고는 초기 데이터로만, 재고 서비스는 외부에 열지 않는다)
  - FR-014의 "요청에 적힌 사용자 정보로 주인을 정하지 않는다"
  - FR-020 (거절·실패 사유와 상품 목록을 주문에 기록한다)
  - FR-027의 "500이면 재시도하지 않는다" (409 쪽은 시나리오 7.2가 확인한다)
  - FR-028 (P2 단계의 실패 처리, `[제안]`)
  - FR-035 (서킷이 열리면 재시도도 하지 않는다)
  - FR-036 (해제 요청의 재시도)
  - FR-037 (값을 설정으로 둔다)
- **P1 단계에서 만드는 해제의 검증 시점**: "예약됨을 해제하면 수량이 돌아온다"(FR-009 앞부분)를 확인하는 인수 시나리오는 시나리오 9.1(P3)의 재고 쪽뿐이다. 유스케이스 7절 표를 그대로 따랐기 때문이다. 해제 기능은 P1 단계 PR에서 만드는데, 그 검증은 P3 단계 PR까지 미뤄진다.
- 검증은 1회 돌렸고, 고친 뒤 ID 검사와 원문 비교를 다시 돌려 둘 다 0건을 확인했다.
- Items marked incomplete require spec updates before `/speckit-clarify` or `/speckit-plan`. 지금 미완료 항목은 없다. 위 Notes의 두 가지는 사용자 결정이 필요하다.
