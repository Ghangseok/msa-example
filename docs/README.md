# docs

spec-kit이 따라야 하는 기준 문서와 참고 자료를 두는 곳이다. 기능별 spec-kit 산출물은 `specs/`에, 서비스 사이 계약의 현재본은 `contracts/`에 두고 여기에는 두지 않는다. 이렇게 나눈 이유는 [ADR-0002](adr/0002-docs-folder-structure.md)에 있다.

## 폴더 규칙

| 폴더 | 담는 것 | 성격 | spec-kit에서 쓰는 곳 |
|---|---|---|---|
| `requirements/` | 요구사항 정의서, 유스케이스 | 기준 | `/speckit-specify` |
| `analysis/` | 도메인 분석, 장애 분석, AS-IS 분석 | 기준 | `/speckit-specify`, `/speckit-clarify` |
| `design/` | 아키텍처·인터페이스·DB·화면 설계 | 기준 | `/speckit-plan` |
| `test-cases/` | 테스트 케이스와 시나리오 | 기준 | spec.md의 인수 시나리오, `/speckit-tasks` |
| `standards/` | 코딩 규약, 아키텍처 규칙 | 기준 | `/speckit-constitution` |
| `adr/` | 여러 서비스에 걸친 아키텍처 결정 | 기준 | `/speckit-plan`의 근거 |
| `references/` | 개념 정리, 규정, 벤더 API 문서, 레거시 매뉴얼 | 참고 | 필요할 때만 |
| `_originals/` | 받은 원본(xlsx, pptx, hwp) | 보관 | AI에 직접 주지 않는다. 위 폴더의 md 변환본을 `@`로 가리킨다 |

- **기준**: spec이 따라야 하는 문서다. spec과 어긋나면 둘 중 하나를 고친다.
- **참고**: 이해를 돕지만 spec이 따를 의무는 없다.

## 문서 목록

| 문서 | 내용 | 상태 |
|---|---|---|
| [references/msa-k8s-primer.md](references/msa-k8s-primer.md) | 마이크로서비스와 쿠버네티스 기초 (아래 문서들의 원문) | 완료 |
| [references/docker-desktop.md](references/docker-desktop.md) | Docker Desktop 설정 위치, 이 프로젝트의 권장 설정, 문제 해결 | 2026-10-04 확인 |
| [requirements/uc-001-place-order.md](requirements/uc-001-place-order.md) | UC-001 주문 생성 | 초안 |
| [requirements/uc-002-view-orders.md](requirements/uc-002-view-orders.md) | UC-002 주문 조회 (고객, 관리자) | 초안 |
| [requirements/non-functional.md](requirements/non-functional.md) | 비기능 요구사항 NFR-001~009 | 초안 |
| [analysis/domain-analysis.md](analysis/domain-analysis.md) | 서비스 경계, 주문·예약 상태, 멱등성, 중복 주문, 장애 모드, 미결 사항 OQ-001~009 (모두 결정) | 초안 |
| [design/architecture.md](design/architecture.md) | 런타임 구성, 요청 흐름, 재시도, 인터페이스, 데이터, 인증 | 초안 |
| [standards/architecture-rules.md](standards/architecture-rules.md) | 아키텍처 규칙 STD-001~019 | 초안 |
| [standards/tech-stack.md](standards/tech-stack.md) | 기술 스택과 버전 | 확정 |
| [standards/testing.md](standards/testing.md) | 테스트 전략 (B안: 서비스 통합 중심) | 확정 |
| [standards/coding-conventions.md](standards/coding-conventions.md) | 코딩 규약 (A안: 계층형 + MyBatis) | 확정 |
| [standards/git-workflow.md](standards/git-workflow.md) | 브랜치, 커밋, PR 규칙 (GitHub Flow + spec 단위 브랜치) | 확정 |
| [test-cases/order-placement.md](test-cases/order-placement.md) | 주문 생성·조회 테스트 TC-001~014 | 초안 |
| [test-cases/operations.md](test-cases/operations.md) | 운영 상황 테스트 TC-101~109 | 초안 |
| [adr/](adr/) | ADR-0001~0007 | 모두 채택 |

## ID 규칙

spec과 plan이 어느 문서에서 나왔는지 거슬러 찾을 수 있도록 기준 항목마다 ID를 붙인다.

| 접두어 | 뜻 | 위치 |
|---|---|---|
| `UC-NNN` | 유스케이스 | requirements/ |
| `BR-NNN` | 업무 규칙. UC-001은 `BR-0NN`, UC-002는 `BR-1NN`처럼 유스케이스마다 백 단위를 나눈다 | requirements/ (유스케이스 안) |
| `NFR-NNN` | 비기능 요구사항 | requirements/non-functional.md |
| `OQ-NNN` | 미결 사항 | analysis/domain-analysis.md |
| `STD-NNN` | 아키텍처·코딩 규칙 | standards/ |
| `TC-0NN` / `TC-1NN` | 기능 테스트 / 운영 테스트 | test-cases/ |
| `ADR-NNNN` | 아키텍처 결정 | adr/ |

- `FR-`, `SC-`, `T`는 spec-kit이 spec.md와 tasks.md 안에서 쓰므로 docs에서는 쓰지 않는다.
- 한 번 붙인 ID는 다른 뜻으로 다시 쓰지 않는다. 필요 없어진 항목은 지우지 말고 `폐기`로 표시한다.
- 기준 문서를 고치면 `grep -rn "NFR-002" specs/`처럼 ID로 영향받는 spec을 찾는다.

## 출처와 근거 표시

- 원문 출처는 `P§절번호`로 적는다. P는 [references/msa-k8s-primer.md](references/msa-k8s-primer.md)다. 예: `P§6-①`은 6절 ①항.
- 근거 등급은 원문 표기를 그대로 쓰고 하나를 더한다.
  - `[문헌]` 공식 문서·1차 자료
  - `[현장]` 실무자 후기
  - `[추론]` 근거로부터의 논리적 도출
  - `[제안]` 원문에 없고 이 문서를 만들면서 덧붙인 것. 확정 전이므로 검토 후 태그를 지우거나 고친다.

## spec-kit에서 가리키는 법

```text
/speckit-constitution @docs/standards/ 의 규칙(architecture-rules, tech-stack, testing, coding-conventions)으로 헌법을 만들어줘

/speckit-specify @docs/requirements/uc-001-place-order.md 의 주문 생성 기능.
  인수 시나리오는 @docs/test-cases/order-placement.md 를 따르고, spec에 출처 ID(UC-001, BR-001 등)를 남겨줘

/speckit-clarify  @docs/analysis/domain-analysis.md 의 미결 사항(OQ)도 함께 확인해줘

/speckit-plan @docs/design/architecture.md @docs/adr/ 기준으로.
  Source Code 절에 services/order-service/, services/inventory-service/ 경로를 적어줘
```
