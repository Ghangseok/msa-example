---

description: "주문 생성(티켓 001)의 작업 목록. 단계(Phase)마다 PR 하나다"
---

# Tasks: 주문 생성

**Input**: `specs/001-place-order/`의 [spec.md](spec.md), [plan.md](plan.md), [research.md](research.md), [data-model.md](data-model.md), [contracts/](contracts/), [quickstart.md](quickstart.md)

**함께 따른 규칙**: 저장소 루트 `CLAUDE.md`, `.specify/memory/constitution.md`, `docs/standards/`의 다섯 문서(`architecture-rules.md`, `tech-stack.md`, `testing.md`, `coding-conventions.md`, `git-workflow.md`)

**작성일**: 2026-10-06

## 이 문서를 읽는 법

### 한 번에 한 단계만 한다

- **`/speckit-implement`는 한 번에 한 단계만 한다.** 단계 하나가 PR 하나다. 단계의 마지막 작업은 "사용자에게 보고하고 멈춘다"이고, 사용자가 그 단계의 PR을 병합한 뒤 다음 단계를 시작한다.
- 이 문서는 spec-kit 기본 구조(Setup → Foundational → 스토리마다 한 단계 → Polish) 대신 PR 경계로 단계를 나눈다. `docs/standards/git-workflow.md` 2절 "브랜치"가 단계별 PR 브랜치 이름에 tasks.md의 단계 번호를 붙이게 정했기 때문이다(예: `feat/001-place-order-p2`). 경계의 근거는 plan.md "PR 단계" 표다.
- 커밋, push, PR 만들기는 작업으로 넣지 않았다. 저장소 루트 `CLAUDE.md` 1절에 따라 사용자가 요청할 때 한다. 브랜치를 만들거나 바꾸는 일도 사용자에게 묻고 한다.

| 단계 | 브랜치 | 담는 것 | 사용자 스토리 |
|---|---|---|---|
| Phase 0 | main에서 만든 docs 브랜치 두 개 `[제안]` | 테스트 케이스 아홉 개 추가, 기준 문서를 plan 결정에 맞추기 | 없음 |
| Phase 1 | `feat/001-place-order-p1` | 빌드 골격, 기계 검사, CI, 재고 서비스 전체, 재고 쪽 인수 시나리오 테스트 전부, 루트 `contracts/inventory-api.yaml` | User Story 1, 그리고 User Story 2·3·6·8·9의 재고 쪽 |
| Phase 2 | `feat/001-place-order-p2` | 주문 서비스 전체(시도당 제한 시간과 서킷 브레이커 포함) | User Story 2, 3, 4, 5, 6, 7 |
| Phase 3 | `feat/001-place-order-p3` | 재시도와 재시도 포함 전체 30초 한도, 해제의 같은 재시도 정책 | User Story 8, 9 |

### 단계 안의 순서

단계마다 아래 순서로 나눈다. 따로 "Polish" 단계를 두지 않는다.

1. **준비**: 빌드 파일, 설정, 단계 표시(`msa.stage`)
2. **기반**: 모든 스토리가 기대는 코드와 스키마, 테스트 도우미
3. **스토리마다 한 절**: 인수 시나리오 테스트 → 테스트가 실패하는 것을 확인 → 구현 → 통과 확인
4. **마무리와 확인**: 아키텍처 테스트, 기계 검사가 실제로 잡는지 확인, 단계 기준 확인, 보고

### 작업 줄의 형식

```text
- [ ] T### [P] [US#] 설명 (파일 경로)
```

- **`T###`**: 작업 번호(`T001`부터). 단계를 넘어 이어 붙인다.
- **`[P]`**: 같은 절의 다른 `[P]` 작업과 동시에 해도 되는 작업이다. 서로 다른 파일을 고치고, 아직 끝나지 않은 작업에 기대지 않는다.
- **`[US#]`**: spec.md의 User Story 번호다(예: `[US1]`). Phase 0과 각 단계의 준비·기반·마무리 작업에는 붙이지 않는다.
- **파일 경로**: 줄 끝 괄호 안에 저장소 루트 기준 경로를 적는다. 경로는 plan.md "Source Code" 트리를 따른다. 트리에 없는 경로를 고른 곳에는 `[제안]`을 붙였다.
- 작업 줄 아래의 들여 쓴 줄은 그 작업의 세부 내용이다. 작업이 아니다.

### 테스트는 필수다

- 헌법 "실제 조건에서 테스트 먼저" 원칙과 덮어쓴 tasks 템플릿(`.specify/templates/overrides/tasks-template.md`)에 따라 테스트는 필수다. spec-kit 안내의 "Tests are OPTIONAL"보다 이것이 우선한다.
- 스토리마다 인수 시나리오 테스트 작업을 구현 작업보다 앞에 둔다. 테스트 작업 바로 다음에 "테스트가 실패하는 것을 확인한다" 작업을 둔다.
- **인수 시나리오 테스트의 `@DisplayName`은 테스트 케이스 ID로 시작한다** (`docs/standards/testing.md` 3절 "TC 연결"). 단위·Mapper·계약 테스트는 인수 시나리오 테스트를 받치는 테스트라서 ID를 붙이지 않는다(plan.md "인수 시나리오와 테스트 층" 절).
- **기대값은 `docs/test-cases/order-placement.md`에서 그대로 가져온다.** 테스트가 실패하면 구현을 고친다. 기대값을 바꾸거나 테스트를 지우거나 건너뛰게 만들지 않는다. 기대값이 틀렸다고 판단되면 멈추고 사용자에게 묻는다(저장소 루트 `CLAUDE.md` 7절).
- 받치는 테스트(단위·Mapper)의 값도 그 테스트 케이스의 값(상품 A 10개, 주문 번호 1001 같은 값)을 쓴다 `[제안]`.
- **받치는 테스트의 기대값 출처**: 판정 기준 문서(`docs/test-cases/`, `contracts/`, `docs/requirements/`)에 더해, 사용자가 확정한 `docs/design/`과 `docs/analysis/` 문서에서도 가져온다. `specs/` 아래 문서(research.md, data-model.md 등)에만 있는 값은 단언하지 않는다(2026-10-06 사용자 결정). 이 예외는 Phase 0-B에서 저장소 루트 `CLAUDE.md` 7절에 적는다(T010). 인수 시나리오 테스트의 기대값은 지금처럼 `docs/test-cases/`에서만 가져온다.
- **재고 쪽 인수 시나리오 테스트는 모두 Phase 1에서, 재고 구현보다 먼저 쓴다.** P2·P3 스토리의 재고 쪽(시나리오 2.1, 3.1, 3.3, 6.1, 8.1, 9.1의 재고 쪽)도 같다. 재고 서비스는 Phase 1에서 모두 만들기 때문이다. 이 테스트를 P2·P3에 두면 구현 뒤에 쓰게 되어, 헌법 "실제 조건에서 테스트 먼저" 원칙의 "테스트 작업을 구현 작업보다 앞에 둔다"를 어긴다(2026-10-06 사용자 승인).
- **실패 확인 작업에서 실패를 보지 못하면 다음 작업으로 넘어가지 않는다.** 멈추고 사용자에게 묻는다. 예외는 미리 정한 한 가지다. 시나리오 3.2(재고 부족 응답에는 재시도하지 않는다)처럼 "재시도하지 않는다"를 보는 테스트는 재시도가 생기기 전에는 실패할 수 없다. 이 테스트는 재시도를 만든 뒤 구현을 잠시 바꿔 실패를 보고 되돌린다(Phase 3의 T167, 2026-10-06 사용자 승인). 저장소 루트 `CLAUDE.md` 6절의 "고친 부분을 잠시 되돌려 테스트가 실패하는 것을 보고" 방식이다.

### 시간 값은 1/5로 줄인다

서비스 통합 테스트는 research.md 4절 "테스트 시간 줄이기" 줄을 따라 시간 값을 모두 1/5로 줄인 설정으로 돌린다 `[제안]`. 테스트 케이스 문서의 공통 전제("테스트에서는 같은 비율로 줄여 돌릴 수 있다", "테스트에서 시간을 줄여 돌리면 이 값도 같은 비율로 줄인다")는 줄이는 것을 허용하지만, 비율은 정하지 않는다. 1/5라는 비율과 아래 표에서 "줄이지 않는다"로 적은 세 값은 research.md에서 `[제안]`이고, 사용자가 아직 정하지 않았다. 사용자가 정하면 이 절의 태그를 지운다.

| 테스트 케이스 문서의 값 | 테스트에서 쓰는 값 | 출처 |
|---|---|---|
| 시도당 제한 시간 2.5초 | 500ms | research.md 4절 |
| 재시도 전 대기 즉시·1·2·4·8초 | 0·200·400·800·1600ms | research.md 4절 |
| 재시도 포함 전체 30초 | 6초 | research.md 4절 |
| 서킷 열려 있는 시간 10초 | 2초 | research.md 4절 |
| "응답은 3초 뒤에 준다" | 600ms | research.md 4절 |
| "31초 안에" | 6.2초 안에 | research.md 4절 |
| "즉시"(앞 응답 뒤 0.5초 안) | 앞 응답 뒤 0.1초 안 | research.md 4절 |
| "0.8초 이상 1.5초 이하" | 0.16초 이상 0.3초 이하 | research.md 4절에 없는 값이라 1/5로 계산했다 `[제안]` |
| health 확인 "1초 안에 200" | 1초 안에 200 (줄이지 않는다 `[제안]`) | research.md 4절 |
| 서킷이 열린 뒤 "1초 안에" 실패 | 1초 안에 (줄이지 않는다 `[제안]`) | research.md 4절 |
| "고객 응답 뒤 5초 안에" 해제 요청 | 5초 안에 (줄이지 않는다 `[제안]`) | research.md 4절 |

줄이지 않은 기본값이 설정에 맞게 들어갔는지는 단위 테스트로 따로 확인한다(`docs/standards/testing.md` 3절 "시간").

### 항목 ID 표기

- 작업 줄에는 항목 ID를 쓰지 않는다. 테스트 케이스는 spec 시나리오 번호(시나리오 1.1)로, 비기능 요구사항과 업무 규칙은 이름과 원문으로 가리킨다. 스토리는 spec 번호(User Story 1)로 가리킨다(2026-10-06 사용자 승인).
- `@DisplayName`에 넣을 테스트 케이스 ID는 그 절 끝의 "이 절의 시나리오 원문"에서 가져온다. 그 블록의 인용 첫 줄에 ID와 spec 시나리오 번호가 함께 있다.
- 항목 ID를 꼭 적어야 하는 곳은 테스트 케이스 단계 표의 내용(CSV)뿐이다. 그 표는 바로 아래 "이 표에 나온 항목"에서 펼친다. 형식은 저장소 루트 `CLAUDE.md` 2절을 따른다.
- 아키텍처 규칙은 ID 대신 규칙 이름과 `docs/standards/architecture-rules.md`의 절로 가리킨다.
- research.md 결정 2의 추가 테스트 케이스 아홉 개(㉠, ㉡, ㉢, ㉣, ㉥, ㉦, ㉧, ㉨, ㉩)는 PR #17로 테스트 케이스 문서에 들어갔고, spec에 시나리오로 들어갔다. 기호와 spec 시나리오 번호의 짝은 ㉠ 1.6, ㉡ 1.7, ㉥ 1.8, ㉩ 2.2, ㉦ 4.2, ㉧ 5.2, ㉨ 6.2, ㉢ 8.2, ㉣ 9.2다. 이 문서는 두 가지를 함께 쓴다. ㉤는 research.md에서 테스트 케이스가 아닌 "P2 단계의 임시 동작"을 가리키므로 건너뛴다.
- `[제안]`은 사용자가 정하지 않았는데 Claude가 고른 것이다. 모두 맨 끝 "자동으로 고른 것" 절에 모았다. 사용자가 정해야 해서 하지 않은 일은 "사용자 확인 필요" 절에 모았다.

---

## Phase 0: 선행 문서 작업

**상태**: 2026-10-07에 끝났다. 0-A는 PR #17, 0-B는 PR #18로 main에 병합했다. spec·plan·tasks 문서는 그보다 먼저 PR #16(브랜치 `feat/001-place-order`)으로 병합했다.

**목적**: 001의 PR보다 먼저 판정 기준 문서와 기준 문서를 plan의 결정에 맞춘다. 001의 PR 밖에서, main에서 만든 별도 docs 브랜치로 한다.

**문서를 고치는 작업(T001, T002, T005, T006, T007, T008, T009, T010)에는 사용자 승인이 필요하다.** `docs/` 아래 기준 문서를 고치는 일이라서, 저장소 루트 `CLAUDE.md` 1절 "기준 문서를 고치기 전에 묻는다"에 따라 무엇을 왜 고칠지 먼저 보여 주고 답을 받은 뒤 고친다. 확인 작업(T003, T011)과 보고 작업(T004, T012)은 읽기만 한다.

**PR 나누기** `[제안]`: Phase 0을 PR 두 개로 나눈다. 0-A는 테스트 케이스 추가, 0-B는 기준 문서 맞추기다. 저장소 루트 `CLAUDE.md` 1절 "PR 하나에는 한 가지 일만 담는다"를 따른다.

**병합 순서**: 두 PR 모두 P1 단계 PR보다 먼저 병합한다.

- 0-A를 먼저 병합하는 것은 research.md 4절 "테스트 케이스 추가 docs PR의 순서"의 `[제안]`이다. P1의 테스트 케이스 단계 표와 ㉠·㉡ 테스트가 0-A에서 붙는 번호를 쓰기 때문이다.
- 0-B를 먼저 병합하는 것은 2026-10-06 사용자 승인이다. 0-B에는 받치는 테스트의 기대값 출처를 정한 `CLAUDE.md` 7절 예외도 들어간다(T010). 헌법 "기술 제약" 절은 plan의 Technical Context에 기술 스택 문서의 값을 쓰라고 한다. 그런데 plan은 MyBatis 스타터 4.1.0과 JUnit Jupiter 6을 쓰고, 기술 스택 문서는 4.0.x와 JUnit 5를 적는다. 0-B 없이 P1을 병합하면 둘이 어긋난 채 main에 들어간다.

**끝났다고 보는 기준** (plan.md "PR 단계" 표의 0-A 줄과 0-B 줄): 사용자가 두 PR을 병합

| 확인할 것 | 명령 | 기대 결과 |
|---|---|---|
| 0-A가 main에 병합됐다 | `gh pr list --state merged --head docs/order-test-cases` | PR 한 줄 |
| 0-B가 main에 병합됐다 | `gh pr list --state merged --head docs/align-001-decisions` | PR 한 줄 |
| main의 테스트 케이스 문서에 아홉 케이스가 있다 | `git fetch origin; git show origin/main:docs/test-cases/order-placement.md \| Select-String '^## TC-'` | 지금 있는 15줄에 아홉 줄이 더해져 24줄 |

**시작 조건**: 없다. Phase 0의 PR에는 `specs/`를 넣지 않는다.

### 0-A. 테스트 케이스 아홉 개 추가 (브랜치 `docs/order-test-cases` `[제안]`)

- [x] T001 research.md 1절 결정 2의 "테스트 케이스 문서에 더할 케이스 초안" 아홉 개(㉠, ㉡, ㉢, ㉣, ㉥, ㉦, ㉧, ㉨, ㉩)를 테스트 케이스 문서 형식으로 옮긴 글을 사용자에게 보여 주고 승인을 받는다. 사용자 승인 필요 (docs/test-cases/order-placement.md)
  - 형식은 문서의 기존 케이스와 같다: `## <ID> <제목>`, `- 관련:` 줄, `**Given**`/`**When**`/`**Then**`/`**And**` 줄.
  - 번호는 문서의 마지막 번호 다음부터 차례로 붙인다(research.md 결정 2 "번호는 문서에 넣을 때 다음 빈 번호부터 붙인다").
  - 초안의 기대값은 research.md에 있는 그대로 보여 준다. 고칠 곳이 있으면 사용자가 정한다.
  - ㉢ 초안을 보여 줄 때 한 가지를 함께 묻는다. ㉢의 "둘째 주문의 예약 요청은 정확히 4번 간다"는 첫 주문의 해제 요청도 같은 서킷을 지나간다는 점(FR-029, FR-031)을 반영하지 않는다. 해제 응답을 서킷이 성공으로 세는지 실패로 세는지, 비동기 해제가 언제 끼어드는지에 따라 둘째 주문의 요청 수가 3번이 되거나 실행마다 달라진다(2026-10-06 분석에서 찾음).
  - **공통 전제에 더할 줄 두 개도 함께 보여 준다**(2026-10-06 사용자 승인). 인수 테스트 세 개(시나리오 2.1, 4.1, 6.1)가 단언하는 응답 코드와 오류 형식이 지금은 판정 기준 문서에 없기 때문이다. 헌법 "실제 조건에서 테스트 먼저" 원칙은 인수 테스트의 기대값을 확정된 `docs/test-cases/`에서 가져오라고 한다. 값은 research.md 결정 13(사용자 결정)과 `specs/001-place-order/contracts/order-api.yaml`에서 가져왔다. 문장은 아래와 같다 `[제안]`.
    - "주문 API의 성공 응답 코드: 새 주문을 기록하면(확정·거절·실패) 201, 같은 고객이 같은 요청 키로 다시 보내면(처리중 포함) 200."
    - "주문 API의 오류 응답 본문은 Problem Details 형식이다. 토큰이 없으면 `code`는 `UNAUTHORIZED`다."
  - 머리말의 "상태" 줄을 어떻게 바꿀지(예: 추가한 날짜)도 함께 보여 준다.
- [x] T002 T001에서 승인받은 글을 더하고 "상태" 줄을 고친다. 새 케이스는 문서 끝(용어 절 앞)에, 공통 전제의 새 줄은 공통 전제 목록 끝에 더한다. 기존 글은 한 글자도 바꾸지 않는다. 새 용어가 있으면 용어 절에 더한다. 사용자 승인 필요 (docs/test-cases/order-placement.md)
- [x] T003 기존 글이 바뀌지 않았는지 확인한다 (docs/test-cases/order-placement.md)
  - 명령: `git fetch origin` 뒤 `git diff origin/main -- docs/test-cases/order-placement.md`
  - 기대 결과: `-`로 시작하는 줄은 "상태" 줄 하나뿐이고, 나머지는 모두 `+` 줄이다.
  - 명령: `Select-String -Path docs/test-cases/order-placement.md -Pattern '^## TC-'` → 24줄
- [x] T004 사용자에게 보고하고 멈춘다. 고친 파일, 새로 붙은 번호 아홉 개와 ㉠·㉡·㉢·㉣·㉥·㉦·㉧·㉨·㉩의 짝, 공통 전제에 더한 줄, T003의 명령과 결과를 적는다. 커밋·push·PR은 사용자가 요청할 때 한다 (파일 없음, 대화창 보고)

### 0-B. 기준 문서를 plan 결정에 맞추기 (브랜치 `docs/align-001-decisions` `[제안]`)

- [x] T005 research.md 5절 "기준 문서와 어긋난 것" 표에서 아래 세 문서의 줄을 고칠 글을 사용자에게 보여 주고 승인을 받는다. 줄마다 지금 글, 고칠 글, 이유(research.md의 결정 번호)를 나란히 적는다. 사용자 승인 필요 (docs/standards/tech-stack.md, docs/standards/testing.md, docs/design/architecture.md)
  - `docs/standards/tech-stack.md` 표의 여섯 줄: "재시도", "서킷 브레이커", "서비스 간 HTTP", "영속성", "테스트", "정적 분석"
  - `docs/standards/testing.md` 2절 "계약" 행과 "단위" 행(도구 "JUnit 5" → JUnit Jupiter 6. "단위" 행은 2026-10-06 사용자 결정으로 더했다)
  - `docs/standards/coding-conventions.md` 3-8절 표의 두 행(2026-10-06 사용자 결정으로 더했다): "`${}`와 `SELECT *`를 쓰지 않는다" 행의 검사 주체("테스트가" → Gradle 작업), "인수 시나리오 테스트에 테스트 케이스 ID를 남긴다" 행의 대상(`docs/test-cases/*.md` → `docs/test-cases/order-placement.md`, 운영 테스트는 대상에서 뺀다). 근거는 research.md 결정 11(사용자 결정)이다.
  - `docs/design/architecture.md` 머리말과 5절·6절: plan에서 정한 이름으로 바꾸고 `[제안]` 표시를 지운다. 6절의 `REQUEST_HASH`를 빼고 항목 직접 비교로 바꾼다. 주문·예약 항목의 결과 열을 더한다.
  - 저장소 루트 `CLAUDE.md` 7절에 받치는 테스트의 기대값 출처 예외를 더한다(2026-10-06 사용자 결정). 7절 둘째 줄("테스트 코드는 Claude가 쓰더라도, 기대값은 위 문서에서 그대로 가져온다…") 바로 아래에 더할 문장은 아래와 같다(2026-10-06 사용자가 문장과 0-B에 넣는 것을 승인).
    - "받치는 테스트(인수 시나리오를 검증하지 않는 단위·Mapper·계약 테스트)는 위 문서에 더해, 사용자가 확정한 `docs/design/`과 `docs/analysis/` 문서에서도 기대값을 가져올 수 있다. 그 문서에서 "제안"이나 "초안"으로 표시한 부분은 쓰지 않는다. Claude가 만든 `specs/` 아래 문서(research.md, data-model.md 등)에서는 가져오지 않는다. 인수 시나리오 테스트의 기대값은 `docs/test-cases/`에서만 가져온다."
  - 5절 표의 나머지 두 줄은 다른 곳에서 한다: 테스트 케이스 문서는 0-A(T001), spec.md의 `[제안]` 태그는 Phase 1의 첫 작업(T013).
- [x] T006 [P] T005에서 승인받은 대로 기술 스택 표의 여섯 줄을 고친다. 사용자 승인 필요 (docs/standards/tech-stack.md)
- [x] T007 [P] T005에서 승인받은 대로 2절 "계약" 행과 "단위" 행의 도구를 고친다. 사용자 승인 필요 (docs/standards/testing.md)
- [x] T008 [P] T005에서 승인받은 대로 머리말과 5절·6절을 고친다. 사용자 승인 필요 (docs/design/architecture.md)
- [x] T009 [P] T005에서 승인받은 대로 3-8절 표의 두 행을 고친다. 다른 행은 바꾸지 않는다. 사용자 승인 필요 (docs/standards/coding-conventions.md)
- [x] T010 [P] T005에서 승인받은 대로 7절에 받치는 테스트의 기대값 출처 예외를 더한다. 다른 줄은 바꾸지 않는다. 사용자 승인 필요 (CLAUDE.md)
- [x] T011 고친 곳이 승인받은 내용과 같은지 확인한다 (docs/standards/tech-stack.md, docs/standards/testing.md, docs/design/architecture.md, docs/standards/coding-conventions.md, CLAUDE.md)
  - 명령: `git fetch origin` 뒤 `git diff origin/main --stat` → 바뀐 파일이 T006, T007, T008, T009, T010의 다섯 파일뿐이다.
  - 명령: `git diff origin/main -- CLAUDE.md` → `+` 줄은 승인받은 문장뿐이고 `-` 줄이 없다.
  - 명령: `Select-String -Path docs/design/architecture.md -Pattern 'REQUEST_HASH'` → 0줄
- [x] T012 사용자에게 보고하고 멈춘다. 고친 줄마다 지금 글과 바꾼 글, T011의 명령과 결과를 적는다 (파일 없음, 대화창 보고)

Phase 0에서는 항목 ID를 쓰지 않았다.

---

## Phase 1: P1 단계 PR — 빌드 골격, 기계 검사, CI, 재고 서비스 (브랜치 `feat/001-place-order-p1`)

**목적**: 빌드 골격과 기계 검사, CI를 만들고 재고 서비스 전체(예약·해제 API)를 만든다. 서비스 사이 호출이 아직 없으므로 제한 시간과 서킷 브레이커 없이도 헌법 "실패를 전제한 호출" 원칙을 어기지 않는다(spec "우선순위와 PR 단계" 절).

**담는 것** (plan.md "PR 단계" 표의 P1 줄): 빌드 골격(Gradle wrapper, `settings.gradle`, `gradle.properties`, 버전 카탈로그, `build-logic`). 기계 검사와 `config/`, `libs/archunit-rules`. CI. 재고 서비스 전체(예약·해제 API, Flyway 스키마와 초기 데이터, 로그·추적·프로브·그레이스풀 셧다운 설정, `CLAUDE.md`). 재고 쪽 인수 시나리오 테스트 전부(P2·P3 스토리의 재고 쪽 포함, 2026-10-06 사용자 승인). 루트 `contracts/inventory-api.yaml`.

**끝났다고 보는 기준** (plan.md "PR 단계" 표): `.\gradlew.bat build` 종료 코드 0(`msa.stage=P1`), 재고 쪽 인수 시나리오 테스트 통과, GitHub Actions 성공

| 확인할 것 | 명령 | 기대 결과 |
|---|---|---|
| 단계 표시 | `Select-String -Path gradle.properties -Pattern '^msa.stage=P1$'` | 한 줄 |
| 빌드와 모든 검사 | `.\gradlew.bat build` | 종료 코드 0 |
| 재고 쪽 인수 시나리오 테스트 | `.\gradlew.bat :services:inventory-service:test` | 종료 코드 0. 테스트 보고서(`services/inventory-service/build/reports/tests/test/index.html`)에 시나리오 1.1, 1.2, 1.3, 1.4, 1.5, 시나리오 2.1·3.1·3.3·6.1·8.1·9.1의 재고 쪽, ㉠, ㉡, ㉥의 테스트가 모두 통과로 나온다 |
| 계약 현재본과 기능 폴더의 계약이 같다 | `git diff --no-index contracts/inventory-api.yaml specs/001-place-order/contracts/inventory-api.yaml` | 출력 없음, 종료 코드 0 (quickstart.md 3절) |
| GitHub Actions | 사용자가 push한 뒤 `gh run list --branch feat/001-place-order-p1 --limit 1` | 결론(conclusion)이 `success` |

**시작 조건**: Phase 0의 0-A PR과 0-B PR이 모두 main에 병합되어 있다(Phase 0 머리의 "병합 순서". 2026-10-07에 충족했다). 이 단계의 브랜치 `feat/001-place-order-p1`을 최신 main에서 만든다. spec·plan·tasks를 담은 브랜치 `feat/001-place-order`는 PR #16으로 이미 병합했으므로 다시 쓰지 않는다(`docs/standards/git-workflow.md` 2절 "병합된 PR의 브랜치에는 다시 push하지 않는다"). 브랜치를 만드는 일은 사용자에게 묻고 한다.

- 확인 명령: `git branch --show-current` → `feat/001-place-order-p1`

- 확인 명령: `Select-String -Path docs/test-cases/order-placement.md -Pattern '^## TC-'` → 24줄
- 확인 명령: `Select-String -Path docs/standards/tech-stack.md -Pattern 'mybatis-spring-boot-starter 4.0.x'` → 0줄

### 준비 (빌드 골격)

- [ ] T013 spec.md의 `[제안]` 태그 11개를 지우는 일을 사용자에게 보여 주고 승인을 받은 뒤 지운다. 구현을 시작하기 전에 한다. spec이 사용자가 받아들인 내용을 아직 제안처럼 보여 주면, 구현하는 동안 무엇이 정해진 것인지 헷갈리기 때문이다(2026-10-06 사용자 승인). 사용자 승인 필요 (specs/001-place-order/spec.md)
  - 근거: research.md 결정 1("spec.md에서 11개의 `[제안]` 태그를 지우는 일은 spec 수정이다. 무엇을 지울지 보여 주고 사용자 승인을 받은 뒤 고친다").
  - 지울 곳은 spec.md Assumptions의 "`[제안]` 모음" 11개와, 그 11개가 본문에 붙은 자리다. 줄마다 지우기 전과 뒤를 보여 준다. 글의 내용은 바꾸지 않고 태그만 지운다.
  - 확인: `Select-String -Path specs/001-place-order/spec.md -Pattern '\[제안\]'` → 승인받은 곳이 모두 빠졌는지 본다.

- [ ] T014 Gradle wrapper 9.8.0을 만든다 (gradlew, gradlew.bat, gradle/wrapper/gradle-wrapper.properties, gradle/wrapper/gradle-wrapper.jar)
  - 근거: plan.md Technical Context "Gradle wrapper 9.8.0(Groovy DSL)", research.md 0절.
  - 이 PC에는 `gradle` 명령이 없다(2026-10-06 `command -v gradle`로 확인). Gradle 9.8.0 배포본(`https://services.gradle.org/distributions/gradle-9.8.0-bin.zip`)을 scratchpad 폴더에 받아 풀고, 그 안의 `gradle wrapper --gradle-version 9.8.0`을 저장소 루트에서 한 번 실행한다 `[제안]`. 받은 배포본은 저장소에 넣지 않는다.
  - 확인: `.\gradlew.bat --version` → 첫 줄에 `Gradle 9.8.0`, JVM 줄에 `17.0.2`.
- [ ] T015 루트 `settings.gradle`을 만든다 (settings.gradle)
  - `rootProject.name = 'msa-example'` `[제안]`, `includeBuild('build-logic')`, `include 'libs:archunit-rules'`, `include 'services:inventory-service'`.
  - `services:order-service`는 이 단계에서 넣지 않고 Phase 2(T083)에서 더한다 `[제안]`. research.md 결정 4 "기능이 없는 빈 모듈을 미리 만들지 않는다"를 따른다.
  - 루트 `build.gradle`은 두지 않는다(plan.md "Structure Decision").
- [ ] T016 단계 표시를 만든다. 내용은 `msa.stage=P1` 한 줄이다 (gradle.properties)
  - 근거: research.md 결정 11 "지금 단계는 `gradle.properties`의 `msa.stage`(P1, P2, P3)에 적는다".
- [ ] T017 버전 카탈로그를 만든다. P1이 쓰는 것만 넣고, P2·P3에서 쓰는 Resilience4j와 WireMock은 그 단계에서 더한다 `[제안]` (gradle/libs.versions.toml)
  - 버전은 research.md 0절 표 그대로다: Spring Boot 4.1.1(플러그인과 BOM), mybatis-spring-boot-starter 4.1.0, mybatis-spring-boot-starter-test 4.1.0, OpenTelemetry Logback appender(`io.opentelemetry.instrumentation:opentelemetry-logback-appender-1.0`) 2.28.1-alpha, ArchUnit(`com.tngtech.archunit:archunit-junit5`) 1.5.1, swagger-request-validator-core 2.46.1, Spotless Gradle 플러그인 8.10.3, palantir-java-format 2.101.0, SpotBugs Gradle 플러그인 6.5.12, SpotBugs 4.10.4.
  - Spring Boot BOM이 관리하는 것(Flyway와 `flyway-database-oracle`, Oracle JDBC, Testcontainers와 `testcontainers-oracle-xe`, JUnit Jupiter, AssertJ, OpenTelemetry SDK)은 카탈로그에 버전을 적지 않는다. BOM은 Gradle의 `platform(...)`으로 가져온다(research.md 결정 11).
- [ ] T018 포함 빌드 `build-logic`의 빌드 파일을 만든다 (build-logic/settings.gradle, build-logic/build.gradle)
  - `build-logic/settings.gradle`: 루트의 `gradle/libs.versions.toml`을 버전 카탈로그 `libs`로 읽는다.
  - `build-logic/build.gradle`: `groovy-gradle-plugin`을 적용하고, convention plugin이 적용할 Spring Boot Gradle 플러그인, Spotless 플러그인, SpotBugs 플러그인을 `implementation` 의존성으로 둔다.
- [ ] T019 [P] SpotBugs 제외 파일을 만든다. 끄는 규칙은 `EI_EXPOSE_REP`와 `EI_EXPOSE_REP2` 두 개뿐이다 (config/spotbugs/exclude.xml)
  - 근거: research.md 결정 10. 이 파일을 바꾸려면 PR에서 사용자가 승인한다.
- [ ] T020 [P] 허용 목록 파일을 만든다. 첫 줄은 열 이름 `path,marker,reason,approved_pr`이고, 처음에는 열 이름 줄만 둔다 (config/quality/suppression-allowlist.csv)
  - 근거: research.md 결정 11. 줄을 더하려면 PR에서 사용자가 승인한다(`docs/standards/testing.md` 3절 "검사를 끄는 표시").
- [ ] T021 [P] 재고 API의 계약 현재본을 만든다. `specs/001-place-order/contracts/inventory-api.yaml`을 한 글자도 바꾸지 않고 복사한다 (contracts/inventory-api.yaml)
  - 근거: plan.md Constitution Check "계약 우선" 줄, `docs/standards/architecture-rules.md` 2절 "서비스 사이 호출"의 계약 현재본 규칙, quickstart.md 3절(두 파일의 차이 없음).
- [ ] T022 [P] CI를 만든다 (.github/workflows/build.yml)
  - 근거: research.md 결정 5. 검사 범위는 `docs/standards/git-workflow.md` 4절의 필수 검사이고, 기계 검사도 빌드 안에서 함께 돈다.
  - PR과 main push에서 `ubuntu-latest`로 돈다 `[제안]`. JDK 17(Temurin)을 설치한다 `[제안]`. Gradle 캐시는 `gradle/actions/setup-gradle`로 둔다 `[제안]`. Testcontainers는 러너의 Docker를 쓴다 `[제안]`.
  - Actions 버전은 구현할 때 각 저장소의 최신 릴리스를 조회해서 고정한다 `[제안]`. 조회한 명령과 결과를 보고에 적는다.
  - `./gradlew build` 전에 `chmod +x ./gradlew` 단계를 둔다 `[제안]`. Windows에서 만든 파일은 Git에 실행 권한 없이 올라갈 수 있기 때문이다.
  - main 규칙에 필수 검사를 켜는 일은 사용자가 한다(research.md 결정 5).

- [ ] T023 [P] 테스트 케이스 단계 표를 만든다. plan.md "인수 시나리오와 테스트 층" 표의 "테스트 케이스", "서비스", "단계" 칸을 줄마다 옮긴다. 추가 케이스 아홉 개의 번호는 PR #17로 테스트 케이스 문서에 들어간 번호다 (config/quality/test-case-stages.csv)
  - 서비스 칸의 값은 `inventory`(재고), `order`(주문)로 쓴다 `[제안]`. 서비스 프로젝트 이름(`inventory-service`)에서 `-service`를 뺀 값이다.
  - 티켓 002로 넘긴 부분은 research.md 결정 11(사용자 결정)대로 단계를 `T002`로 적은 줄로 넣는다. 해당하는 곳은 시나리오 3.1의 "C1이 조회할 수 있다"와 시나리오 4.1의 조회 부분이다. 테스트 케이스 ID 검사(T029)는 `T002` 줄을 검사하지 않는다.
  - 재고 쪽 줄은 모두 P1이다. 재고 쪽 인수 시나리오 테스트를 모두 Phase 1에서 구현보다 먼저 쓰기 때문이다(이 문서 머리의 "테스트는 필수다").
  - 첫 검사(문서의 모든 ID가 표에 있는가) 때문에 P2·P3 줄도 지금 모두 넣는다. 단계가 지금 단계보다 높은 줄은 검사하지 않으므로 P1 빌드를 막지 않는다(research.md 결정 11).
  - 내용:

    ```text
    tc_id,service,stage
    TC-003,inventory,P1
    TC-005,inventory,P1
    TC-007,inventory,P1
    TC-008,inventory,P1
    TC-012,inventory,P1
    TC-004,inventory,P1
    TC-001,inventory,P1
    TC-002,inventory,P1
    TC-015,inventory,P1
    TC-011,inventory,P1
    TC-006,inventory,P1
    TC-001,order,P2
    TC-002,order,P2
    TC-009,order,P2
    TC-015,order,P2
    TC-010,order,P2
    TC-014,order,P2
    TC-011,order,P2
    TC-013,order,P2
    TC-008,order,P2
    TC-006,order,P3
    TC-004,order,P3
    TC-002,order,T002
    TC-010,order,T002
    TC-016,inventory,P1
    TC-017,inventory,P1
    TC-020,inventory,P1
    TC-021,order,P2
    TC-022,order,P2
    TC-023,order,P2
    TC-024,order,P2
    TC-018,order,P3
    TC-019,order,P3
    ```

**이 표에 나온 항목** (위 T023의 단계 표)

> **TC-003** · `docs/test-cases/order-placement.md` 절 "TC-003 같은 주문으로 예약을 다시 요청해도 재고는 한 번만 줄어든다"
> - **Given** 상품 A의 재고가 10개다
> - **And** 주문 번호 1001로 상품 A 3개 예약이 이미 처리됐다
> - **When** 같은 주문 번호 1001, 상품 A, 3개로 예약을 다시 요청한다
> - **Then** 재고 서비스는 처음과 같은 RESERVED를 돌려준다
> - **And** 상품 A의 재고는 4개가 아니라 7개다
> - 변형:
>   - 같은 요청 두 개를 동시에 보내도 재고는 7개이고 두 응답 모두 RESERVED다.
>   - 첫 결과가 REJECTED였다면, 그 뒤 재고를 채운 다음 재요청해도 REJECTED를 돌려준다.

> **TC-005** · `docs/test-cases/order-placement.md` 절 "TC-005 같은 상품에 주문이 동시에 몰려도 초과 판매되지 않는다"
> - **Given** 상품 A의 재고가 10개다
> - **When** 서로 다른 주문 20건이 동시에 상품 A를 1개씩 주문한다
> - **Then** "확정"은 정확히 10건, "거절"은 10건이다
> - **And** 상품 A의 재고는 0개이고 음수가 아니다
> - 비고: 재고 Pod가 2개이므로 동시 요청이 서로 다른 Pod에서 같은 행을 바꾼다. `[추론]`

> **TC-007** · `docs/test-cases/order-placement.md` 절 "TC-007 해제가 예약보다 먼저 도착해도 재고는 줄지 않는다"
> - **Given** 상품 A의 재고가 10개이고, 주문 번호 1002의 예약 기록이 없다
> - **When** 주문 번호 1002의 해제 요청이 먼저 오고, 그 뒤에 1002, 상품 A, 3개의 예약 요청이 도착한다
> - **Then** 해제 요청은 RELEASED를 돌려준다
> - **And** 예약 요청은 반영되지 않고 RELEASED를 돌려준다
> - **And** 상품 A의 재고는 10개다
> - **When** 같은 해제 요청이 한 번 더 온다
> - **Then** 재고는 여전히 10개다

> **TC-008** · `docs/test-cases/order-placement.md` 절 "TC-008 같은 주문 번호에 다른 내용이 오면 거부한다"
> - **Given** 주문 번호 1003으로 상품 A 3개 예약이 처리됐다
> - **When** 주문 번호 1003, 상품 A, 5개로 예약을 요청한다
> - **Then** 재고 서비스는 요청 키 충돌(409)을 돌려준다
> - **And** 재고는 변하지 않는다
> - **And** 주문 서비스는 이 응답에 재시도하지 않는다
> - **And** 그 주문은 "실패"로 기록되고, 재고 서비스에 해제 요청이 간다

> **TC-012** · `docs/test-cases/order-placement.md` 절 "TC-012 여러 상품 주문이 서로 다른 순서로 동시에 들어와도 교착 없이 처리된다"
> - **Given** 상품 A와 B의 재고가 각각 100개다
> - **When** 주문 50건은 "B 1개, A 1개" 순서로, 다른 50건은 "A 1개, B 1개" 순서로 항목을 적어 동시에 요청한다
> - **Then** 100건 모두 "확정"이다. 교착 오류(ORA-00060)로 실패한 주문이 없다
> - **And** 상품 A와 B의 재고는 각각 0개다
> - 비고: 교착이 나면 재고 서비스는 500을 돌려주고, 주문 서비스는 500에 재시도하지 않는다. 그래서 교착이 한 번이라도 나면 "100건 모두 확정"이 깨진다.

> **TC-004** · `docs/test-cases/order-placement.md` 절 "TC-004 30초 안에 예약 결과를 못 받으면 실패로 기록하고 재고를 해제한다"
> - **Given** 상품 A의 재고가 10개다
> - **And** 재고 서비스가 예약 요청을 처리해 재고를 줄이지만, 응답은 3초 뒤에 준다 (시도당 제한 시간 2.5초보다 길다)
> - **And** 재고 서비스는 해제 요청에는 바로 정상으로 응답한다
> - **When** 고객이 상품 A를 3개 주문한다
> - **Then** 고객은 31초 안에 주문 번호와 "잠시 후 다시 시도"를 받는다
> - **And** 예약 요청은 정확히 6번 갔다
> - **And** 그 주문은 "실패" 상태로 기록되어 있다
> - **And** 고객 응답 뒤 5초 안에 재고 서비스에 해제 요청이 가고, 주문에 "해제 완료"가 기록된다
> - **And** 상품 A의 재고는 10개다
> - **And** 그동안 주문 서비스의 health 확인은 1초 안에 200으로 응답한다
> - 비고: 한 테스트로 두 서비스를 다 확인하기 어렵다. 주문 쪽(시도 횟수, 실패 기록, 해제 요청)은 WireMock으로 지연을 넣어 검증하고, 재고 쪽(해제하면 수량이 복구된다)은 재고 서비스 테스트로 검증한다.

> **TC-001** · `docs/test-cases/order-placement.md` 절 "TC-001 재고가 충분하면 여러 상품 주문이 확정된다"
> - **Given** 상품 A의 재고가 10개, 상품 B의 재고가 5개다
> - **When** 고객이 상품 A 3개와 상품 B 2개를 한 주문으로 요청한다
> - **Then** 고객은 주문 번호와 "확정" 결과를 받는다
> - **And** 그 주문이 C1의 주문으로 두 항목과 함께 "확정" 상태로 기록되어 있다
> - **And** 상품 A의 재고는 7개, 상품 B의 재고는 3개다
> - 변형: 상품 하나만 담은 주문도 같은 방식으로 확정된다.

> **TC-002** · `docs/test-cases/order-placement.md` 절 "TC-002 항목 하나라도 재고가 부족하면 주문 전체가 거절되고 기록된다"
> - **Given** 상품 A의 재고가 10개, 상품 B의 재고가 1개다
> - **When** 고객이 상품 A 3개와 상품 B 2개를 한 주문으로 요청한다
> - **Then** 고객은 주문 번호와 "거절", 부족한 상품 B를 받는다
> - **And** 그 주문이 "거절" 상태로 기록되어 있고 C1이 조회할 수 있다
> - **And** 상품 A의 재고는 10개, 상품 B의 재고는 1개 그대로다 (A도 줄지 않는다)

> **TC-015** · `docs/test-cases/order-placement.md` 절 "TC-015 없는 상품이 섞이면 주문 전체가 거절되고 기록된다"
> - **Given** 상품 A의 재고가 10개다. 상품 Z는 재고 데이터에 없다
> - **When** 고객이 상품 A 3개와 상품 Z 1개를 한 주문으로 요청한다
> - **Then** 고객은 주문 번호와 "거절", 사유 "상품 없음", 없는 상품 Z를 받는다
> - **And** 그 주문이 "거절" 상태로 기록되어 있다
> - **And** 상품 A의 재고는 10개 그대로다
> - **And** 예약 요청은 정확히 1번 갔다 (재시도하지 않는다)
> - 변형: 상품 B의 재고가 1개일 때 상품 A 3개, 상품 Z 1개, 상품 B 2개를 한 주문으로 요청하면, 사유는 "상품 없음"이고 없는 상품 Z와 부족한 상품 B를 모두 받는다. 상품 A와 B의 재고는 그대로다.

> **TC-011** · `docs/test-cases/order-placement.md` 절 "TC-011 같은 주문 요청 키로 다시 보내면 주문은 하나다"
> - **Given** 상품 A의 재고가 10개다
> - **When** 고객이 요청 키 K1로 상품 A 3개를 주문하고, 같은 키 K1로 같은 요청을 한 번 더 보낸다
> - **Then** 두 응답의 주문 번호가 같다
> - **And** 상품 A의 재고는 7개다
> - **When** 고객이 새 요청 키 K2로 같은 내용을 주문한다
> - **Then** 새 주문 번호를 받고 상품 A의 재고는 4개다
> - **When** 고객이 K1으로 상품 A 5개를 주문한다
> - **Then** 422(요청 키 충돌)를 받는다
> - **When** C2가 K1으로 상품 A 1개를 주문한다
> - **Then** 새 주문이 만들어진다 (키는 고객마다 따로 본다)
> - **When** 요청 키 없이 주문한다
> - **Then** 400을 받고 주문은 기록되지 않는다
> - **When** UUID 형식이 아닌 요청 키로 주문한다
> - **Then** 400을 받고 주문은 기록되지 않는다
> - **When** 고객이 요청 키 K3로 상품 A 1개를 주문하고, 재고 서비스의 응답이 늦어 그 요청이 아직 처리 중일 때 같은 키 K3로 같은 요청을 한 번 더 보낸다
> - **Then** 둘째 응답은 첫 요청과 같은 주문 번호와 "처리중"을 받는다
> - **And** 주문은 하나만 기록된다

> **TC-006** · `docs/test-cases/order-placement.md` 절 "TC-006 재고 서비스의 일시 오류는 재시도로 넘어간다"
> - **Given** 상품 A의 재고가 10개다
> - **And** 재고 서비스가 처음 두 번의 예약 요청에 503을 돌려주고, 세 번째부터 정상으로 처리한다
> - **When** 고객이 상품 A를 3개 주문한다
> - **Then** 고객은 "확정"을 받는다
> - **And** 예약 요청은 정확히 3번 갔다. 두 번째는 첫 번째 응답을 받은 뒤 0.5초 안에, 세 번째는 두 번째 응답을 받은 뒤 0.8초 이상 1.5초 이하에 갔다
> - **And** 상품 A의 재고는 7개다

> **TC-009** · `docs/test-cases/order-placement.md` 절 "TC-009 재고 부족 응답에는 재시도하지 않는다"
> - **Given** 상품 A의 재고가 2개다
> - **When** 고객이 상품 A를 3개 주문한다
> - **Then** 예약 요청은 정확히 1번 갔다

> **TC-010** · `docs/test-cases/order-placement.md` 절 "TC-010 로그인한 사람만 주문하고, 자기 주문만 본다"
> - **When** 토큰 없이 주문한다
> - **Then** 401을 받고 주문은 기록되지 않는다
> - **Given** C1의 주문 1004가 있다
> - **When** C2가 주문 1004를 조회한다
> - **Then** "없음"(404)을 받는다
> - **When** C1이 자기 주문 목록을 조회한다
> - **Then** C1의 주문만 나온다
> - **When** C1이 관리자 조회를 요청한다
> - **Then** 403을 받는다
> - **Given** C1과 C2에게 각각 "실패" 주문이 있다
> - **When** M1이 상태 "실패"로 관리자 조회를 한다
> - **Then** C1과 C2의 실패 주문이 모두 재고 해제 결과와 함께 나온다

> **TC-014** · `docs/test-cases/order-placement.md` 절 "TC-014 잘못된 주문 항목은 기록되지 않는다"
> - **When** 항목이 하나도 없는 주문, 수량이 0인 항목이 있는 주문, 같은 상품이 두 줄에 나오는 주문, 항목이 21개인 주문, 수량이 100인 항목이 있는 주문을 각각 요청한다
> - **Then** 다섯 경우 모두 400을 받는다
> - **And** 주문은 기록되지 않고 재고 서비스에 요청이 가지 않는다

> **TC-013** · `docs/test-cases/order-placement.md` 절 "TC-013 재고 서비스 실패가 이어지면 서킷이 열려 바로 실패한다"
> - **Given** 재고 서비스가 모든 예약 요청에 503을 돌려준다
> - **When** 고객들이 주문을 연달아 요청한다
> - **Then** 실패한 시도가 서킷의 열림 조건(design 4절)에 이르면 서킷이 열린다
> - **And** 그 뒤의 주문은 재고 서비스에 요청을 보내지 않고 1초 안에 "실패"와 "잠시 후 다시 시도"를 받는다
> - **And** 그 주문들은 "해제 불필요"로 기록되고 해제 요청도 가지 않는다
> - **And** 서킷이 열리기 전에 예약 요청을 보낸 주문은 "실패"로 기록되고, 재고 서비스가 계속 503을 돌려주는 동안 해제도 실패해 "해제 실패"로 기록된다
> - **When** 재고 서비스가 정상으로 돌아오고 서킷의 열림 시간이 지난다
> - **Then** 시험 호출이 성공해 서킷이 닫히고, 새 주문이 "확정"된다
> - 비고: 서비스 통합 테스트에서 WireMock으로 검증한다. 열림 시간 같은 값은 테스트에서 짧게 바꾼다.

> **TC-016** · `docs/test-cases/order-placement.md` 절 "TC-016 거절된 예약에 해제 요청이 와도 재고는 그대로다"
> - **Given** 상품 A의 재고가 2개다
> - **And** 주문 번호 2001로 상품 A 3개 예약을 요청해 REJECTED를 받았다
> - **When** 주문 번호 2001의 해제 요청이 온다
> - **Then** 해제 요청은 REJECTED를 돌려준다
> - **And** 상품 A의 재고는 2개다

> **TC-017** · `docs/test-cases/order-placement.md` 절 "TC-017 해제한 예약의 기록은 남아 있어서, 같은 예약 요청이 다시 와도 재고가 줄지 않는다"
> - **Given** 상품 A의 재고가 10개다
> - **And** 주문 번호 2002로 상품 A 3개 예약이 처리됐고, 그 뒤 해제됐다
> - **When** 주문 번호 2002, 상품 A, 3개로 예약을 다시 요청한다
> - **Then** 재고 서비스는 RELEASED를 돌려준다
> - **And** 상품 A의 재고는 10개다

> **TC-020** · `docs/test-cases/order-placement.md` 절 "TC-020 예약된 재고에 해제가 두 번 와도 수량은 한 번만 돌아온다"
> - **Given** 상품 A의 재고가 10개다
> - **And** 주문 번호 2003으로 상품 A 3개 예약이 처리됐다
> - **When** 주문 번호 2003의 해제 요청이 온다
> - **Then** 해제 요청은 RELEASED를 돌려준다
> - **And** 상품 A의 재고는 10개다
> - **When** 같은 해제 요청이 한 번 더 온다
> - **Then** 해제 요청은 RELEASED를 돌려준다
> - **And** 상품 A의 재고는 여전히 10개다 (13개가 아니다)

> **TC-021** · `docs/test-cases/order-placement.md` 절 "TC-021 서명이 틀리거나 만료된 토큰으로는 주문할 수 없다"
> - **When** 서명이 틀린 토큰으로 주문한다
> - **Then** 401을 받고 주문은 기록되지 않는다
> - **When** 만료된 토큰으로 주문한다
> - **Then** 401을 받고 주문은 기록되지 않는다

> **TC-022** · `docs/test-cases/order-placement.md` 절 "TC-022 형식이 틀린 주문 항목은 기록되지 않는다"
> - **When** 상품 ID가 빈 항목이 있는 주문, 수량이 정수가 아닌(1.5) 항목이 있는 주문을 각각 요청한다
> - **Then** 두 경우 모두 400을 받는다
> - **And** 주문은 기록되지 않고 재고 서비스에 요청이 가지 않는다

> **TC-023** · `docs/test-cases/order-placement.md` 절 "TC-023 거부된 요청의 키는 남지 않고, 같은 키의 내용 비교는 항목 순서를 보지 않는다"
> - **When** 고객이 요청 키 K4로 수량이 0인 항목이 있는 주문을 보내 400을 받고, 같은 키 K4로 상품 A 1개를 주문한다
> - **Then** 주문 번호를 받고 그 주문이 기록된다
> - **When** 고객이 요청 키 K5로 상품 A 1개와 상품 B 1개를 "A, B" 순서로 주문하고, 같은 키 K5로 "B, A" 순서로 같은 요청을 한 번 더 보낸다
> - **Then** 두 응답의 주문 번호가 같다 (422가 아니다)
> - **And** 주문은 하나만 기록된다

> **TC-024** · `docs/test-cases/order-placement.md` 절 "TC-024 주문 서비스는 재고 서비스 호출에 추적 정보를 넘긴다"
> - **Given** 재고 서비스가 예약 요청에 RESERVED를 돌려준다
> - **When** 고객이 상품 A 1개를 주문한다
> - **Then** 재고 서비스가 받은 예약 요청에 W3C `traceparent` 헤더가 있다

> **TC-018** · `docs/test-cases/order-placement.md` 절 "TC-018 재시도하던 중에 서킷이 열리면 남은 재시도를 하지 않는다"
> - **Given** 재고 서비스가 모든 예약 요청과 해제 요청에 503을 돌려준다
> - **And** 고객 C1의 첫 주문이 예약 요청 6번을 모두 실패하고 "실패"로 끝났다
> - **When** 고객 C1이 둘째 주문을 요청한다
> - **Then** 둘째 주문의 예약 요청은 6번보다 적게 간다. 서킷이 열린 뒤에는 남은 재시도를 하지 않는다
> - **And** 둘째 주문은 "실패"로 기록되고 고객은 "잠시 후 다시 시도"를 받는다
> - 비고: 첫 주문의 해제 요청도 같은 서킷을 지나가므로, 둘째 주문의 요청이 정확히 몇 번인지는 해제가 언제 끼어드는지에 따라 달라진다. 그래서 "6번보다 적다"로 판정한다.

> **TC-019** · `docs/test-cases/order-placement.md` 절 "TC-019 해제 요청도 일시 오류면 같은 정책으로 재시도한다"
> - **Given** 재고 서비스가 예약 요청에 409를 돌려준다
> - **And** 재고 서비스가 해제 요청의 처음 두 번에 503을 돌려주고, 세 번째부터 정상으로 처리한다
> - **When** 고객이 상품 A를 3개 주문한다
> - **Then** 고객은 주문 번호와 "잠시 후 다시 시도"를 받는다
> - **And** 해제 요청은 정확히 3번 갔다. 두 번째는 첫 번째 응답을 받은 뒤 0.5초 안에, 세 번째는 두 번째 응답을 받은 뒤 0.8초 이상 1.5초 이하에 갔다
> - **And** 주문에 "해제 완료"가 기록된다

### 기반 (기계 검사, 재고 서비스 뼈대, 스키마, 테스트 도우미)

**⚠️ 이 절이 끝나기 전에는 User Story 1을 시작하지 않는다.**

- [ ] T024 convention plugin `msa.java-service`를 만든다 (build-logic/src/main/groovy/msa.java-service.gradle)
  - Java 17로 컴파일한다(`options.release = 17`) `[제안]`. Spring Boot Gradle 플러그인을 적용한다(`-parameters` 컴파일 옵션이 켜진다. research.md 결정 9).
  - Spring Boot BOM을 `platform(...)`으로 가져온다. `io.spring.dependency-management` 플러그인은 쓰지 않는다(research.md 결정 11).
  - Spotless를 적용하고 palantir-java-format 2.101.0, 4칸 들여쓰기로 검사한다(`docs/standards/coding-conventions.md` 3-5절).
  - SpotBugs 4.10.4를 적용한다. `effort=max`, `reportLevel=low`, 보고서는 HTML이다(research.md 결정 10의 `[제안]` 세부). 제외 파일은 `config/spotbugs/exclude.xml`이다. 하나라도 찾으면 빌드가 실패한다.
  - `test` 작업은 JUnit Platform을 쓴다. 저장소 루트 `contracts/` 폴더 경로를 시스템 속성 `msa.contractsDir`로 넘긴다 `[제안]`. 두 서비스의 계약 테스트가 이 값으로 계약 파일을 찾는다.
  - T025~T029의 검사 작업을 등록하고 `check` 작업에 연결한다. 그래서 `build`가 검사를 돌린다(quickstart.md 2절).
- [ ] T025 [P] Lombok 검사 작업을 만든다 (build-logic/src/main/groovy/msa/quality/NoLombokCheck.groovy)
  - 검사 방법은 `docs/standards/coding-conventions.md` 3-8절 표의 원문 그대로다: "Gradle 빌드: 어떤 configuration에든 `org.projectlombok` 그룹이 들어오면 빌드를 실패시킨다. 보조로 소스에서 `import lombok.`을 찾는다."
- [ ] T026 [P] 검사를 끄는 표시를 찾는 작업을 만든다 (build-logic/src/main/groovy/msa/quality/SuppressionMarkerCheck.groovy)
  - 검사 방법은 3-8절 표의 원문 그대로다: "`src/**/*.java`와 `build.gradle`에서 `@Disabled`, `@DisabledIf`, `@EnabledIf`, `Assumptions.assume`, `@SuppressWarnings`, `@SuppressFBWarnings`, `NOPMD`, `spotless:off`, `@formatter:off`, `FreezingArchRule`, 테스트 작업의 `exclude`를 찾는다. 허용 목록(파일 경로, 표시, 이유, 승인한 PR 번호)에 없으면 실패한다."
  - 허용 목록은 `config/quality/suppression-allowlist.csv`다. 검사 대상은 이 작업을 적용한 서비스 프로젝트 폴더다.
- [ ] T027 [P] Mapper XML 검사 작업을 만든다 (build-logic/src/main/groovy/msa/quality/MapperXmlCheck.groovy)
  - 검사 방법은 3-8절 표의 원문 그대로다: "Mapper XML을 XML 파서로 읽고, 문장 요소(`select`, `insert`, `update`, `delete`, `sql`)의 글자를 검사한다. XML 주석은 빼고 본다. `${`는 허용 목록에 있는 것만 통과시킨다. `COUNT(*)`는 잡지 않는다". 찾는 것은 `${}`와 `SELECT *`다.
  - 3-8절은 이 검사를 "테스트가" 한다고 적지만, research.md 결정 11(사용자 결정)이 Gradle 작업으로 정했다. 3-8절은 Phase 0-B에서 결정 11에 맞게 고친다(T009, 2026-10-06 사용자 결정).
  - `${}` 예외도 `config/quality/suppression-allowlist.csv`에 적는다(research.md 결정 11). 이때 `marker` 칸은 `${`다 `[제안]`.
- [ ] T028 [P] 19c 이후 SQL 키워드 검사 작업을 만든다 (build-logic/src/main/groovy/msa/quality/Sql19cKeywordCheck.groovy)
  - 검사 방법은 3-8절 표의 원문 그대로다: "Mapper XML과 Flyway SQL에서 금지 키워드를 찾는다: `IF NOT EXISTS`, `IF EXISTS`, `BOOLEAN`(열 타입), `JSON`(열 타입. `IS JSON` 조건은 제외), `SQL_MACRO`, `VECTOR`, `DOMAIN`, `ANNOTATIONS`"
- [ ] T029 [P] 테스트 케이스 ID 검사 작업을 만든다 (build-logic/src/main/groovy/msa/quality/TestCaseIdCheck.groovy)
  - 검사 방법은 research.md 결정 11 원문 그대로다: "검사는 두 가지를 본다. 첫째, `docs/test-cases/order-placement.md`의 모든 테스트 케이스 ID가 이 표에 있는가. 둘째, 단계가 지금 단계 이하인 줄마다 그 서비스의 테스트 소스에 `@DisplayName("<ID>`로 시작하는 테스트가 하나 이상 있는가." "티켓 002로 넘긴 부분은 단계를 `T002`로 적어 검사하지 않는다. 운영 테스트(`docs/test-cases/operations.md`)는 대상에서 뺀다."
  - 지금 단계는 `gradle.properties`의 `msa.stage`에서 읽는다. 단계 순서는 P1 < P2 < P3이다. 단계가 `T002`인 줄은 이 순서에 들지 않으므로 둘째 검사에서 언제나 건너뛴다.
  - 이 작업을 적용한 서비스는 표에서 자기 서비스 칸(T023의 `inventory`, `order`)인 줄만 둘째 검사로 본다 `[제안]`.
- [ ] T030 [P] ArchUnit 규칙 라이브러리를 만든다 (libs/archunit-rules/build.gradle, libs/archunit-rules/src/main/java/com/example/msa/archrules/LayerRules.java, libs/archunit-rules/src/main/java/com/example/msa/archrules/MyBatisAnnotationRules.java, libs/archunit-rules/src/main/java/com/example/msa/archrules/TransactionalRemoteCallRules.java)
  - `build.gradle`: `java-library`와 Spotless만 적용하고 ArchUnit을 `api` 의존성으로 둔다 `[제안]`. `msa.java-service`는 Spring Boot 플러그인이 붙어 라이브러리에 맞지 않는다.
  - 규칙은 서비스의 기본 패키지(`com.example.msa.inventory` 등)를 받아 만든다. 업무 규칙, DTO, Mapper, SQL은 두지 않는다(`docs/standards/architecture-rules.md` 1절 "서비스 경계"의 `libs/` 규칙).
  - `LayerRules`: `docs/standards/coding-conventions.md` 2절 "의존 방향" 원문 그대로다. "controller는 mapper와 client를 직접 부르지 않는다." "mapper, client, domain은 service와 controller를 모른다." "domain은 Spring과 MyBatis를 모른다(순수 Java)." `dto`는 어디서나 쓸 수 있다.
  - `MyBatisAnnotationRules`: 3-8절 원문 그대로다. "`@Select`, `@Insert`, `@Update`, `@Delete`, `@SelectProvider`, `@InsertProvider`, `@UpdateProvider`, `@DeleteProvider`를 쓰지 못하게 한다"
  - `TransactionalRemoteCallRules`: 3-8절 원문 그대로다. "`@Transactional`이 붙은 메서드와 클래스는 `client` 패키지의 클래스를 직접 부르지 않는다"
- [ ] T031 재고 서비스 빌드 파일을 만든다. `plugins { id 'msa.java-service' }`를 적용한다 (services/inventory-service/build.gradle)
  - 의존성: `spring-boot-starter-webmvc`, `-validation`, `-actuator`, `-flyway`, `-opentelemetry`, `flyway-database-oracle`, Oracle JDBC(`com.oracle.database.jdbc:ojdbc17`) `[제안]`, `mybatis-spring-boot-starter`, OpenTelemetry Logback appender.
  - 테스트 의존성: Spring Boot 테스트 스타터, `spring-boot-testcontainers`, `testcontainers-oracle-xe`, `mybatis-spring-boot-starter-test`, `archunit-junit5`, `swagger-request-validator-core`, `project(':libs:archunit-rules')`. Spring Boot 4.1.1의 테스트 스타터 이름은 구현할 때 Spring Boot 4.1.1 문서에서 확인한다. 확인하지 못했다. research.md 0절은 이 스타터를 조회하지 않았다. 확인 결과는 T041에서 보고한다.
  - 재고 서비스는 다른 서비스를 부르지 않으므로 `restclient`, Resilience4j, WireMock을 넣지 않는다(plan.md "Structure Decision").
- [ ] T032 앱 진입점을 만든다 (services/inventory-service/src/main/java/com/example/msa/inventory/InventoryServiceApplication.java)
- [ ] T033 앱 설정을 만든다 (services/inventory-service/src/main/resources/application.yml)
  - DB 접속 값은 환경변수로 받는다 `[제안]`: `INVENTORY_DB_URL`(기본 `jdbc:oracle:thin:@//localhost:1522/XEPDB1`. `infra/compose/compose.yaml`의 inventory-db 호스트 포트가 1522다), `INVENTORY_DB_USERNAME`(기본 `INVENTORY_SVC`), `INVENTORY_SVC_PASSWORD`(기본값 없음. `.env.example`에 이미 있는 이름이다). 저장소에 비밀번호를 넣지 않는다(`docs/standards/architecture-rules.md` 3절 "실행과 배포"의 비밀값 규칙).
  - MyBatis: `mybatis.mapper-locations=classpath:mapper/*.xml`, `mybatis.configuration.map-underscore-to-camel-case=true`, `mybatis.configuration.arg-name-based-constructor-auto-mapping=true` (research.md 결정 9).
  - 로그: `logging.structured.format.console=ecs` `[제안]` (research.md 4절 "로그 형식"의 `[제안]`).
  - 프로브: `management.endpoint.health.probes.enabled=true`. readiness 그룹에 DB 연결 확인(`db`)을 넣고 `[제안]`, 노출 엔드포인트는 `health`만 둔다 `[제안]` (research.md 4절 "프로브"의 `[제안]`). liveness 그룹에는 외부 상태를 넣지 않는다(`docs/standards/architecture-rules.md` 3절 "실행과 배포"의 프로브 규칙).
  - 그레이스풀 셧다운: `server.shutdown=graceful`, `spring.lifecycle.timeout-per-shutdown-phase=35s` (research.md 4절 "그레이스풀 셧다운", 같은 절의 그레이스풀 셧다운 규칙).
  - OTLP 주소는 환경변수 `OTLP_ENDPOINT`(기본 `http://lgtm:4318`)로 받는다 `[제안]`. Spring Boot 4.1.1의 OTLP 속성 이름은 구현할 때 문서에서 확인한다. 확인하지 못했다. 확인 결과는 T078에서 보고한다.
- [ ] T034 [P] 로그 설정을 만든다. 표준 출력은 ECS JSON, 같은 로그를 OpenTelemetry Logback appender로 OTLP에 보낸다. 파일 appender를 두지 않는다 (services/inventory-service/src/main/resources/logback-spring.xml, services/inventory-service/src/main/java/com/example/msa/inventory/config/ObservabilityConfig.java)
  - 근거: research.md 4절 "OTLP 전송", `docs/standards/architecture-rules.md` 4절 "관측성"의 두 규칙.
  - appender에 OpenTelemetry 객체를 연결하는 코드(`OpenTelemetryAppender.install(...)`)를 `ObservabilityConfig`에 둔다 `[제안]`. Spring Boot 4.1.1이 이것을 자동으로 하는지는 구현할 때 문서에서 확인한다. 자동이면 이 클래스를 만들지 않는다. 확인 결과는 T078에서 보고한다.
- [ ] T035 [P] 테스트 설정을 만든다. OTLP 내보내기를 끈다. 테스트는 이 설정을 `@ActiveProfiles("test")`로 켠다 `[제안]` (services/inventory-service/src/test/resources/application-test.yml)
  - 근거: research.md 4절 "OTLP 전송"의 "테스트에서는 내보내기를 끈다".
- [ ] T036 [P] 재고 서비스 규칙 문서를 만든다. 패키지 구조(plan.md "Source Code" 트리의 재고 서비스 부분), DB 계정 `INVENTORY_SVC`와 테이블 세 개, 재고 행은 상품 ID 순서로 잠근다는 규칙, `RESERVATION_ITEMS`가 `STOCK`으로 외래 키를 두지 않는 이유, 테스트 데이터는 상품 ID `A`, `B`, `Z`를 테스트가 직접 넣고 지운다는 규칙을 적는다 (services/inventory-service/CLAUDE.md)
  - 근거: 헌법 "기술 제약" 절("한 서비스에만 해당하는 규칙은 `services/<서비스>/CLAUDE.md`에 적는다"), research.md 결정 4, data-model.md 3절·6-2절, research.md 4절 "초기 재고 데이터".
- [ ] T037 재고 스키마 마이그레이션을 만든다. 아래 data-model.md 3절의 열과 제약을 그대로 만든다. 이미 적용된 마이그레이션 파일은 나중에 고치지 않는다 (services/inventory-service/src/main/resources/db/migration/V1__inventory.sql)
  - 원문 (data-model.md 3-1절 `STOCK`):

    | 열 | 타입 | NULL | 뜻 |
    |---|---|---|---|
    | `PRODUCT_ID` | `VARCHAR2(20)` | 아니오 | 상품 ID |
    | `QUANTITY` | `NUMBER(10)` | 아니오 | 남은 수량 |

    - `PK_STOCK` 기본 키 (`PRODUCT_ID`)
    - `CK_STOCK_QTY` CHECK (`QUANTITY >= 0`). 수량을 줄이는 코드에 실수가 있어도 음수가 저장되지 않게 하는 마지막 방어선이다. 아래 업무 규칙을 지킨다.
    - 위 원문의 "아래 업무 규칙"은 data-model.md 3-1절이 인용한 업무 규칙이다. 원문은 "재고 수량은 0보다 작아지지 않는다. 동시에 들어온 주문이 같은 재고를 두고 다퉈도 마찬가지다"(`docs/requirements/uc-001-place-order.md` 6절 "업무 규칙")다.

  - 원문 (data-model.md 3-2절 `RESERVATIONS`):

    | 열 | 타입 | NULL | 뜻 |
    |---|---|---|---|
    | `ORDER_NO` | `NUMBER(19)` | 아니오 | 주문 번호(멱등 키) |
    | `STATUS` | `VARCHAR2(20)` | 아니오 | `RESERVED`, `REJECTED`, `RELEASED` |
    | `REASON` | `VARCHAR2(30)` | 예 | 거절이면 `OUT_OF_STOCK` 또는 `PRODUCT_NOT_FOUND` |
    | `CREATED_AT` | `TIMESTAMP` | 아니오 | 처음 기록한 시각 |
    | `UPDATED_AT` | `TIMESTAMP` | 아니오 | 마지막으로 바꾼 시각 |

    - `PK_RESERVATIONS` 기본 키 (`ORDER_NO`). 같은 주문 번호의 예약은 한 줄뿐이다. 같은 번호가 동시에 들어와도 한쪽만 들어간다.
    - `CK_RESERVATIONS_STATUS`, `CK_RESERVATIONS_REASON` CHECK 제약 `[제안]`.

  - 원문 (data-model.md 3-3절 `RESERVATION_ITEMS`):

    | 열 | 타입 | NULL | 뜻 |
    |---|---|---|---|
    | `ORDER_NO` | `NUMBER(19)` | 아니오 | 주문 번호 |
    | `PRODUCT_ID` | `VARCHAR2(20)` | 아니오 | 상품 ID. `STOCK`에 없는 상품도 들어간다(없는 상품 목록을 남기려고) |
    | `QUANTITY` | `NUMBER(2)` | 아니오 | 요청 수량 |
    | `RESULT` | `VARCHAR2(20)` | 예 | 거절된 예약에서 부족했으면 `OUT_OF_STOCK`, 없었으면 `PRODUCT_NOT_FOUND` `[제안]` |

    - `PK_RESERVATION_ITEMS` 기본 키 (`ORDER_NO`, `PRODUCT_ID`)
    - `FK_RESERVATION_ITEMS_RES` 외래 키 (`ORDER_NO`) → `RESERVATIONS`

  - CHECK 제약의 값 목록은 위 "뜻" 칸의 값이다. `RESERVATION_ITEMS.RESULT`의 값 목록 CHECK는 data-model.md에 없으므로 두지 않는다 `[제안]`.
  - SQL은 19c에서 도는 것만 쓴다(`docs/standards/coding-conventions.md` 3-3절). T028의 검사가 금지 키워드를 잡는다.
- [ ] T038 [P] 초기 재고 데이터를 만든다. `P-001`부터 `P-005`까지 각 100개다 (services/inventory-service/src/main/resources/db/migration/V2__seed_stock.sql)
  - 근거: data-model.md 3-1절, research.md 4절 "초기 재고 데이터". 테스트는 이 행을 쓰지 않는다.
- [ ] T039 테스트용 Oracle 컨테이너를 만든다. `gvenzl/oracle-xe:21.3.0-slim-faststart` 컨테이너 하나를 `@ServiceConnection`이 붙은 정적 필드로 두고, 재고 서비스의 Mapper 테스트와 서비스 통합 테스트가 함께 쓴다 (services/inventory-service/src/test/java/com/example/msa/inventory/OracleTestContainer.java `[제안]`)
  - 근거: research.md 4절 "Testcontainers Oracle 재사용", `docs/standards/testing.md` 3절 "DB는 진짜 Oracle". H2를 쓰지 않는다.
  - 위치는 테스트 최상위 패키지다 `[제안]`. plan.md 트리에 더했다(2026-10-07 사용자 결정).
- [ ] T040 [P] 제공자 계약 검증 도우미를 만든다. 서비스 통합 테스트가 보낸 요청과 받은 응답을 swagger-request-validator-core의 `Request`·`Response`로 옮겨 `contracts/inventory-api.yaml`로 검증한다. 계약 파일 경로는 시스템 속성 `msa.contractsDir`(T024)에서 얻는다 (services/inventory-service/src/test/java/com/example/msa/inventory/contract/ProviderContractValidator.java)
  - 근거: research.md 결정 12. 이 도우미는 주문 서비스와 공유하지 않는다(같은 결정의 `[제안]` 세부).
- [ ] T041 빌드 골격이 도는지 확인한다. research.md 6절 "palantir-java-format이 JDK 17의 Gradle 9.8.0 안에서 Spotless로 도는지"를 여기서 확인한다 (build-logic/src/main/groovy/msa.java-service.gradle)
  - 명령: `.\gradlew.bat :services:inventory-service:spotlessCheck :services:inventory-service:compileJava :services:inventory-service:spotbugsMain :services:inventory-service:compileTestJava`
  - 기대 결과: 종료 코드 0.
  - `compileTestJava`가 테스트 의존성을 모두 내려받는지 본다. T031에서 고른 Spring Boot 4.1.1 테스트 스타터 이름과, 그 이름을 확인한 문서 주소를 보고에 적는다.
  - 이 시점의 `build`는 테스트 케이스 ID 검사 때문에 실패하는 것이 맞다. P1 줄의 테스트가 아직 없기 때문이다. `.\gradlew.bat build`를 돌려 실패 메시지가 P1 줄의 ID를 가리키는지 함께 본다.
  - palantir-java-format이 돌지 않으면 멈추고 오류를 보고한다. 다른 포매터로 바꾸는 일은 research.md 결정을 바꾸는 일이라 사용자가 정한다.

이 목록에서는 항목 ID를 쓰지 않았다.

### User Story 1 — 재시도나 동시 주문이 있어도 재고가 맞게 줄어든다 (Priority: P1)

**목표**: 재고 서비스가 주문 번호를 멱등 키로 예약·해제한다. 같은 예약이 여러 번 와도, 동시에 몰려도, 해제가 먼저 와도 재고는 한 번만, 맞는 만큼만 줄고 0보다 작아지지 않는다.

**이 절에 함께 두는 것**: User Story 2, 3, 6, 8, 9의 재고 쪽 인수 시나리오 테스트(시나리오 2.1, 3.1, 3.3, 6.1, 8.1, 9.1의 재고 쪽)와 그 받치는 테스트도 이 절에 둔다. 재고 서비스의 구현이 모두 이 절에 있기 때문이다. 테스트를 구현보다 먼저 쓰려면 같은 절에 있어야 한다(이 문서 머리의 "테스트는 필수다", 2026-10-06 사용자 승인). 시나리오 9.1의 재고 쪽을 P1에서 검증하는 것은 research.md 결정 3(사용자 결정)이다. 작업마다 붙인 스토리 라벨(`[US2]` 등)은 그 시나리오가 속한 spec 스토리다.

**독립 테스트** (spec User Story 1): 재고 서비스만 띄운다. 예약·해제 요청을 재고 서비스에 직접 보내고, 응답과 재고 수량을 확인한다. 주문 서비스가 없어도 검증할 수 있다.

**읽는 법** (spec User Story 1 머리말, research.md 결정 1로 받아들인 제안 1번): 시나리오 1.2와 1.5의 "주문 N건"은 서로 다른 주문 번호로 보낸 예약 요청 N건으로, "확정"은 RESERVED로, "거절"은 REJECTED로 읽는다.

**테스트 공통** `[제안]`: 서비스 통합 테스트는 `@SpringBootTest(webEnvironment = RANDOM_PORT)`로 앱을 띄우고 테스트용 `RestClient`로 실제 HTTP 요청을 보낸다. 동시 요청은 스레드 풀과 `CountDownLatch`로 한꺼번에 출발시킨다. 테스트는 상품 행(`A`, `B`, `Z`)과 예약 기록을 `JdbcTemplate`으로 직접 넣고 끝나면 지운다(`docs/standards/testing.md` 4절). 테스트 코드는 아직 없는 구현 클래스를 참조하지 않는다. 그래야 다음 확인 작업에서 컴파일 오류가 아니라 단언 실패를 볼 수 있다.

#### 인수 시나리오 테스트 (구현보다 먼저)

- [ ] T042 [P] [US1] 시나리오 1.1의 서비스 통합 테스트를 쓴다. 본 흐름(같은 요청을 다시 보냄), 변형 1(같은 요청 두 개를 동시에 보냄), 변형 2(REJECTED 뒤 재고를 채우고 다시 보냄)를 테스트 메서드 세 개로 나눈다 `[제안]`. 메서드마다 `@DisplayName`은 시나리오 1.1의 테스트 케이스 ID로 시작한다. 기대값은 `docs/test-cases/order-placement.md`에서 그대로 가져온다 (services/inventory-service/src/test/java/com/example/msa/inventory/integration/ReservationIdempotencyIntegrationTest.java)
- [ ] T043 [P] [US1] 시나리오 1.2의 서비스 통합 테스트를 쓴다. 서로 다른 주문 번호 20개로 상품 A 1개씩 예약 요청을 동시에 보낸다. `@DisplayName`은 시나리오 1.2의 테스트 케이스 ID로 시작한다. 기대값은 `docs/test-cases/order-placement.md`에서 그대로 가져온다 (services/inventory-service/src/test/java/com/example/msa/inventory/integration/ReservationConcurrencyIntegrationTest.java)
- [ ] T044 [P] [US1] 시나리오 1.3의 서비스 통합 테스트를 쓴다. 해제 먼저, 예약 나중, 같은 해제 두 번째를 차례로 보낸다. `@DisplayName`은 시나리오 1.3의 테스트 케이스 ID로 시작한다. 기대값은 `docs/test-cases/order-placement.md`에서 그대로 가져온다 (services/inventory-service/src/test/java/com/example/msa/inventory/integration/ReleaseBeforeReserveIntegrationTest.java)
- [ ] T045 [P] [US1] 시나리오 1.4의 서비스 통합 테스트를 쓴다. spec 시나리오 1.4가 "이 스토리에서 검증하는 줄"로 고른 네 줄(Given, When, Then 409, 재고는 변하지 않는다)만 검증한다. 주문 서비스 쪽 두 줄은 Phase 2의 T139가 검증한다. `@DisplayName`은 시나리오 1.4의 테스트 케이스 ID로 시작한다. 기대값은 `docs/test-cases/order-placement.md`에서 그대로 가져온다 (services/inventory-service/src/test/java/com/example/msa/inventory/integration/ReservationConflictIntegrationTest.java)
- [ ] T046 [P] [US1] 시나리오 1.5의 서비스 통합 테스트를 쓴다. 50건은 항목을 "B 1개, A 1개" 순서로, 50건은 "A 1개, B 1개" 순서로 적어 서로 다른 주문 번호 100개로 동시에 보낸다. `@DisplayName`은 시나리오 1.5의 테스트 케이스 ID로 시작한다. 기대값은 `docs/test-cases/order-placement.md`에서 그대로 가져온다 (services/inventory-service/src/test/java/com/example/msa/inventory/integration/ReservationDeadlockIntegrationTest.java)
- [ ] T047 [P] [US1] 시나리오 1.6(㉠ 거절된 예약에 해제 요청이 와도 재고는 그대로다)의 서비스 통합 테스트를 쓴다. 기대값은 `docs/test-cases/order-placement.md`에서 그대로 가져온다. `@DisplayName`은 시나리오 1.6의 테스트 케이스 ID로 시작한다. 스토리 라벨은 해제 동작이 User Story 1의 범위라서 `[US1]`로 붙였다 `[제안]` (services/inventory-service/src/test/java/com/example/msa/inventory/integration/ReleaseRejectedReservationIntegrationTest.java)
- [ ] T048 [P] [US1] 시나리오 1.7(㉡ 해제한 예약의 기록은 남아, 같은 예약 요청이 다시 와도 재고가 줄지 않는다)의 서비스 통합 테스트를 쓴다. 기대값은 `docs/test-cases/order-placement.md`에서 그대로 가져온다. `@DisplayName`은 시나리오 1.7의 테스트 케이스 ID로 시작한다. 스토리 라벨은 `[US1]`이다 `[제안]` (services/inventory-service/src/test/java/com/example/msa/inventory/integration/ReserveAfterReleaseIntegrationTest.java)
- [ ] T049 [P] [US1] 시나리오 1.8(㉥ 예약된 재고에 해제가 두 번 와도 수량은 한 번만 돌아온다)의 서비스 통합 테스트를 쓴다. 기대값은 `docs/test-cases/order-placement.md`에서 그대로 가져온다. `@DisplayName`은 시나리오 1.8의 테스트 케이스 ID로 시작한다. 스토리 라벨은 해제 동작이 User Story 1의 범위라서 `[US1]`이다 (services/inventory-service/src/test/java/com/example/msa/inventory/integration/ReleaseTwiceIntegrationTest.java `[제안]`)
  - 근거: FR-009의 "해제 요청이 여러 번 와도 수량은 한 번만 되돌린다". 이 동작을 예약된 상태에서 보는 테스트 케이스가 없어 0-A에서 더한다(2026-10-06 사용자 결정).
- [ ] T050 [P] [US9] 시나리오 9.1의 재고 쪽 서비스 통합 테스트를 쓴다. 상품 A 10개에서 주문 번호 하나로 A 3개를 예약하고, 같은 주문 번호로 해제한 뒤 재고를 읽는다. 기대값은 테스트 케이스의 "상품 A의 재고는 10개다"이고 `docs/test-cases/order-placement.md`에서 그대로 가져온다. `@DisplayName`은 시나리오 9.1의 테스트 케이스 ID로 시작한다 (services/inventory-service/src/test/java/com/example/msa/inventory/integration/ReleaseRestoresStockIntegrationTest.java)
  - 근거: research.md 결정 3, 테스트 케이스의 비고("재고 쪽(해제하면 수량이 복구된다)은 재고 서비스 테스트로 검증한다").
- [ ] T051 [P] [US2] 시나리오 2.1의 재고 쪽 서비스 통합 테스트를 쓴다. 상품 A 10개, 상품 B 5개에서 주문 쪽 테스트(Phase 2의 T099)가 보내는 것과 같은 예약 요청(A 3개, B 2개)을 재고 서비스에 보내고 재고를 읽는다. `@DisplayName`은 시나리오 2.1의 테스트 케이스 ID로 시작한다. 기대값은 `docs/test-cases/order-placement.md`에서 그대로 가져온다 (services/inventory-service/src/test/java/com/example/msa/inventory/integration/ConfirmedOrderStockIntegrationTest.java)
  - 근거: plan.md 표의 "2.1의 재고 쪽" 줄("같은 예약 요청 → 7개, 3개").
- [ ] T052 [P] [US3] 시나리오 3.1의 재고 쪽 서비스 통합 테스트를 쓴다. 상품 A 10개, 상품 B 1개에서 같은 예약 요청(A 3개, B 2개)을 재고 서비스에 보내고 재고를 읽는다. `@DisplayName`은 시나리오 3.1의 테스트 케이스 ID로 시작한다. 기대값은 `docs/test-cases/order-placement.md`에서 그대로 가져온다 (services/inventory-service/src/test/java/com/example/msa/inventory/integration/RejectedOrderStockIntegrationTest.java)
- [ ] T053 [P] [US3] 시나리오 3.3의 재고 쪽 서비스 통합 테스트를 쓴다. 본 흐름(A 10개, Z 없음)과 변형(B 1개 더함)을 메서드 두 개로 나누고, 같은 예약 요청을 보낸 뒤 응답의 사유와 두 목록, 재고를 검사한다. `@DisplayName`은 시나리오 3.3의 테스트 케이스 ID로 시작한다. 기대값은 `docs/test-cases/order-placement.md`에서 그대로 가져온다 (services/inventory-service/src/test/java/com/example/msa/inventory/integration/ProductNotFoundStockIntegrationTest.java)
- [ ] T054 [P] [US6] 시나리오 6.1의 재고 쪽 서비스 통합 테스트를 쓴다. 상품 A 10개에서 서로 다른 주문 번호 두 개로 A 3개 예약을 차례로 보내고, 첫 예약 뒤 7개, 둘째 예약 뒤 4개인지 검사한다. `@DisplayName`은 시나리오 6.1의 테스트 케이스 ID로 시작한다. 기대값은 `docs/test-cases/order-placement.md`에서 그대로 가져온다 (services/inventory-service/src/test/java/com/example/msa/inventory/integration/DistinctOrdersStockIntegrationTest.java)
  - 근거: plan.md 표의 "6.1의 재고 쪽" 줄("서로 다른 주문 번호의 예약 두 번 → 7개, 4개").
- [ ] T055 [P] [US8] 시나리오 8.1의 재고 쪽 서비스 통합 테스트를 쓴다. 상품 A 10개에서 같은 주문 번호와 같은 내용(A 3개)의 예약 요청을 세 번 보내고 재고가 7개인지 검사한다. 세 번 보내는 이유는 주문 쪽(Phase 3)이 같은 요청을 세 번 보내기 때문이다 `[제안]`. `@DisplayName`은 시나리오 8.1의 테스트 케이스 ID로 시작한다. 기대값은 `docs/test-cases/order-placement.md`에서 그대로 가져온다 (services/inventory-service/src/test/java/com/example/msa/inventory/integration/RetriedReservationStockIntegrationTest.java)
  - 근거: plan.md 표의 "8.1의 재고 쪽" 줄("같은 예약 요청 → 7개").
- [ ] T056 [P] [US1] 제공자 계약 테스트를 쓴다. 예약 `PUT` 200(시나리오 1.1의 요청), 해제 `DELETE` 200(시나리오 1.3의 요청), 409 Problem Details(시나리오 1.4의 요청), `PRODUCT_NOT_FOUND` 거절 응답(시나리오 3.3의 재고 쪽 요청), 시나리오 1.6(㉠)의 해제 `DELETE` 200(REJECTED)을 보내고, 요청과 응답을 T040의 도우미로 `contracts/inventory-api.yaml`에 맞춰 검증한다. 계약 테스트에는 테스트 케이스 ID를 붙이지 않는다 (services/inventory-service/src/test/java/com/example/msa/inventory/contract/ReservationProviderContractTest.java)
  - 근거: plan.md "인수 시나리오와 테스트 층" 표의 "계약" 칸(제공자: 200 응답, `DELETE` 200, 409 Problem Details, 3.3의 재고 쪽 "제공자")과 시나리오 1.6 줄(서비스 통합, 제공자 계약), research.md 결정 12.
- [ ] T057 [US1] 위 테스트가 실패하는 것을 확인한다 (services/inventory-service/src/test/java/com/example/msa/inventory/integration/ReservationIdempotencyIntegrationTest.java)
  - 명령: `.\gradlew.bat :services:inventory-service:test --tests "com.example.msa.inventory.integration.*" --tests "com.example.msa.inventory.contract.*"`
  - 기대 결과: 종료 코드가 0이 아니다. 테스트 보고서(`services/inventory-service/build/reports/tests/test/index.html`)에서 위 "인수 시나리오 테스트" 묶음의 테스트가 모두 실패로 나오고, 실패 이유가 엔드포인트가 없어서 생긴 단언 실패(예: 기대 200, 실제 404)다. 하나라도 통과하면 멈추고 사용자에게 묻는다.
  - 컴파일 오류로 실패하면 이 확인은 성공이 아니다. 테스트가 아직 없는 클래스를 참조하는지 보고 고친다.

**이 절의 시나리오 원문**

> **TC-003** (spec 시나리오 1.1) · `docs/test-cases/order-placement.md` 절 "TC-003 같은 주문으로 예약을 다시 요청해도 재고는 한 번만 줄어든다"
> - **Given** 상품 A의 재고가 10개다
> - **And** 주문 번호 1001로 상품 A 3개 예약이 이미 처리됐다
> - **When** 같은 주문 번호 1001, 상품 A, 3개로 예약을 다시 요청한다
> - **Then** 재고 서비스는 처음과 같은 RESERVED를 돌려준다
> - **And** 상품 A의 재고는 4개가 아니라 7개다
> - 변형:
>   - 같은 요청 두 개를 동시에 보내도 재고는 7개이고 두 응답 모두 RESERVED다.
>   - 첫 결과가 REJECTED였다면, 그 뒤 재고를 채운 다음 재요청해도 REJECTED를 돌려준다.

> **TC-005** (spec 시나리오 1.2) · `docs/test-cases/order-placement.md` 절 "TC-005 같은 상품에 주문이 동시에 몰려도 초과 판매되지 않는다"
> - **Given** 상품 A의 재고가 10개다
> - **When** 서로 다른 주문 20건이 동시에 상품 A를 1개씩 주문한다
> - **Then** "확정"은 정확히 10건, "거절"은 10건이다
> - **And** 상품 A의 재고는 0개이고 음수가 아니다
> - 비고: 재고 Pod가 2개이므로 동시 요청이 서로 다른 Pod에서 같은 행을 바꾼다. `[추론]`

> **TC-007** (spec 시나리오 1.3) · `docs/test-cases/order-placement.md` 절 "TC-007 해제가 예약보다 먼저 도착해도 재고는 줄지 않는다"
> - **Given** 상품 A의 재고가 10개이고, 주문 번호 1002의 예약 기록이 없다
> - **When** 주문 번호 1002의 해제 요청이 먼저 오고, 그 뒤에 1002, 상품 A, 3개의 예약 요청이 도착한다
> - **Then** 해제 요청은 RELEASED를 돌려준다
> - **And** 예약 요청은 반영되지 않고 RELEASED를 돌려준다
> - **And** 상품 A의 재고는 10개다
> - **When** 같은 해제 요청이 한 번 더 온다
> - **Then** 재고는 여전히 10개다

> **TC-008** (spec 시나리오 1.4) · `docs/test-cases/order-placement.md` 절 "TC-008 같은 주문 번호에 다른 내용이 오면 거부한다"
> - **Given** 주문 번호 1003으로 상품 A 3개 예약이 처리됐다
> - **When** 주문 번호 1003, 상품 A, 5개로 예약을 요청한다
> - **Then** 재고 서비스는 요청 키 충돌(409)을 돌려준다
> - **And** 재고는 변하지 않는다
> - **And** 주문 서비스는 이 응답에 재시도하지 않는다
> - **And** 그 주문은 "실패"로 기록되고, 재고 서비스에 해제 요청이 간다

> **TC-012** (spec 시나리오 1.5) · `docs/test-cases/order-placement.md` 절 "TC-012 여러 상품 주문이 서로 다른 순서로 동시에 들어와도 교착 없이 처리된다"
> - **Given** 상품 A와 B의 재고가 각각 100개다
> - **When** 주문 50건은 "B 1개, A 1개" 순서로, 다른 50건은 "A 1개, B 1개" 순서로 항목을 적어 동시에 요청한다
> - **Then** 100건 모두 "확정"이다. 교착 오류(ORA-00060)로 실패한 주문이 없다
> - **And** 상품 A와 B의 재고는 각각 0개다
> - 비고: 교착이 나면 재고 서비스는 500을 돌려주고, 주문 서비스는 500에 재시도하지 않는다. 그래서 교착이 한 번이라도 나면 "100건 모두 확정"이 깨진다.

> **TC-004** (spec 시나리오 9.1의 재고 쪽) · `docs/test-cases/order-placement.md` 절 "TC-004 30초 안에 예약 결과를 못 받으면 실패로 기록하고 재고를 해제한다"
> - **Given** 상품 A의 재고가 10개다
> - **And** 재고 서비스가 예약 요청을 처리해 재고를 줄이지만, 응답은 3초 뒤에 준다 (시도당 제한 시간 2.5초보다 길다)
> - **And** 재고 서비스는 해제 요청에는 바로 정상으로 응답한다
> - **When** 고객이 상품 A를 3개 주문한다
> - **Then** 고객은 31초 안에 주문 번호와 "잠시 후 다시 시도"를 받는다
> - **And** 예약 요청은 정확히 6번 갔다
> - **And** 그 주문은 "실패" 상태로 기록되어 있다
> - **And** 고객 응답 뒤 5초 안에 재고 서비스에 해제 요청이 가고, 주문에 "해제 완료"가 기록된다
> - **And** 상품 A의 재고는 10개다
> - **And** 그동안 주문 서비스의 health 확인은 1초 안에 200으로 응답한다
> - 비고: 한 테스트로 두 서비스를 다 확인하기 어렵다. 주문 쪽(시도 횟수, 실패 기록, 해제 요청)은 WireMock으로 지연을 넣어 검증하고, 재고 쪽(해제하면 수량이 복구된다)은 재고 서비스 테스트로 검증한다.

> **TC-001** (spec 시나리오 2.1의 재고 쪽) · `docs/test-cases/order-placement.md` 절 "TC-001 재고가 충분하면 여러 상품 주문이 확정된다"
> - **Given** 상품 A의 재고가 10개, 상품 B의 재고가 5개다
> - **When** 고객이 상품 A 3개와 상품 B 2개를 한 주문으로 요청한다
> - **Then** 고객은 주문 번호와 "확정" 결과를 받는다
> - **And** 그 주문이 C1의 주문으로 두 항목과 함께 "확정" 상태로 기록되어 있다
> - **And** 상품 A의 재고는 7개, 상품 B의 재고는 3개다
> - 변형: 상품 하나만 담은 주문도 같은 방식으로 확정된다.

> **TC-002** (spec 시나리오 3.1의 재고 쪽) · `docs/test-cases/order-placement.md` 절 "TC-002 항목 하나라도 재고가 부족하면 주문 전체가 거절되고 기록된다"
> - **Given** 상품 A의 재고가 10개, 상품 B의 재고가 1개다
> - **When** 고객이 상품 A 3개와 상품 B 2개를 한 주문으로 요청한다
> - **Then** 고객은 주문 번호와 "거절", 부족한 상품 B를 받는다
> - **And** 그 주문이 "거절" 상태로 기록되어 있고 C1이 조회할 수 있다
> - **And** 상품 A의 재고는 10개, 상품 B의 재고는 1개 그대로다 (A도 줄지 않는다)

> **TC-015** (spec 시나리오 3.3의 재고 쪽) · `docs/test-cases/order-placement.md` 절 "TC-015 없는 상품이 섞이면 주문 전체가 거절되고 기록된다"
> - **Given** 상품 A의 재고가 10개다. 상품 Z는 재고 데이터에 없다
> - **When** 고객이 상품 A 3개와 상품 Z 1개를 한 주문으로 요청한다
> - **Then** 고객은 주문 번호와 "거절", 사유 "상품 없음", 없는 상품 Z를 받는다
> - **And** 그 주문이 "거절" 상태로 기록되어 있다
> - **And** 상품 A의 재고는 10개 그대로다
> - **And** 예약 요청은 정확히 1번 갔다 (재시도하지 않는다)
> - 변형: 상품 B의 재고가 1개일 때 상품 A 3개, 상품 Z 1개, 상품 B 2개를 한 주문으로 요청하면, 사유는 "상품 없음"이고 없는 상품 Z와 부족한 상품 B를 모두 받는다. 상품 A와 B의 재고는 그대로다.

> **TC-011** (spec 시나리오 6.1의 재고 쪽) · `docs/test-cases/order-placement.md` 절 "TC-011 같은 주문 요청 키로 다시 보내면 주문은 하나다"
> - **Given** 상품 A의 재고가 10개다
> - **When** 고객이 요청 키 K1로 상품 A 3개를 주문하고, 같은 키 K1로 같은 요청을 한 번 더 보낸다
> - **Then** 두 응답의 주문 번호가 같다
> - **And** 상품 A의 재고는 7개다
> - **When** 고객이 새 요청 키 K2로 같은 내용을 주문한다
> - **Then** 새 주문 번호를 받고 상품 A의 재고는 4개다
> - **When** 고객이 K1으로 상품 A 5개를 주문한다
> - **Then** 422(요청 키 충돌)를 받는다
> - **When** C2가 K1으로 상품 A 1개를 주문한다
> - **Then** 새 주문이 만들어진다 (키는 고객마다 따로 본다)
> - **When** 요청 키 없이 주문한다
> - **Then** 400을 받고 주문은 기록되지 않는다
> - **When** UUID 형식이 아닌 요청 키로 주문한다
> - **Then** 400을 받고 주문은 기록되지 않는다
> - **When** 고객이 요청 키 K3로 상품 A 1개를 주문하고, 재고 서비스의 응답이 늦어 그 요청이 아직 처리 중일 때 같은 키 K3로 같은 요청을 한 번 더 보낸다
> - **Then** 둘째 응답은 첫 요청과 같은 주문 번호와 "처리중"을 받는다
> - **And** 주문은 하나만 기록된다

> **TC-006** (spec 시나리오 8.1의 재고 쪽) · `docs/test-cases/order-placement.md` 절 "TC-006 재고 서비스의 일시 오류는 재시도로 넘어간다"
> - **Given** 상품 A의 재고가 10개다
> - **And** 재고 서비스가 처음 두 번의 예약 요청에 503을 돌려주고, 세 번째부터 정상으로 처리한다
> - **When** 고객이 상품 A를 3개 주문한다
> - **Then** 고객은 "확정"을 받는다
> - **And** 예약 요청은 정확히 3번 갔다. 두 번째는 첫 번째 응답을 받은 뒤 0.5초 안에, 세 번째는 두 번째 응답을 받은 뒤 0.8초 이상 1.5초 이하에 갔다
> - **And** 상품 A의 재고는 7개다

> **TC-016** (spec 시나리오 1.6) · `docs/test-cases/order-placement.md` 절 "TC-016 거절된 예약에 해제 요청이 와도 재고는 그대로다"
> - **Given** 상품 A의 재고가 2개다
> - **And** 주문 번호 2001로 상품 A 3개 예약을 요청해 REJECTED를 받았다
> - **When** 주문 번호 2001의 해제 요청이 온다
> - **Then** 해제 요청은 REJECTED를 돌려준다
> - **And** 상품 A의 재고는 2개다

> **TC-017** (spec 시나리오 1.7) · `docs/test-cases/order-placement.md` 절 "TC-017 해제한 예약의 기록은 남아 있어서, 같은 예약 요청이 다시 와도 재고가 줄지 않는다"
> - **Given** 상품 A의 재고가 10개다
> - **And** 주문 번호 2002로 상품 A 3개 예약이 처리됐고, 그 뒤 해제됐다
> - **When** 주문 번호 2002, 상품 A, 3개로 예약을 다시 요청한다
> - **Then** 재고 서비스는 RELEASED를 돌려준다
> - **And** 상품 A의 재고는 10개다

> **TC-020** (spec 시나리오 1.8) · `docs/test-cases/order-placement.md` 절 "TC-020 예약된 재고에 해제가 두 번 와도 수량은 한 번만 돌아온다"
> - **Given** 상품 A의 재고가 10개다
> - **And** 주문 번호 2003으로 상품 A 3개 예약이 처리됐다
> - **When** 주문 번호 2003의 해제 요청이 온다
> - **Then** 해제 요청은 RELEASED를 돌려준다
> - **And** 상품 A의 재고는 10개다
> - **When** 같은 해제 요청이 한 번 더 온다
> - **Then** 해제 요청은 RELEASED를 돌려준다
> - **And** 상품 A의 재고는 여전히 10개다 (13개가 아니다)

#### 구현 (받치는 테스트 → 구현 순서)

받치는 테스트(단위, Mapper)는 구현 클래스를 참조하므로, 쓰고 나면 컴파일 오류로 실패한다. 그 실패를 본 뒤 구현한다.

- [ ] T058 [P] [US1] 예약 상태와 사유 enum을 만든다. `ReservationStatus`는 `RESERVED`, `REJECTED`, `RELEASED`, `ReservationReason`은 `OUT_OF_STOCK`, `PRODUCT_NOT_FOUND`다. 이 값 목록은 계약의 enum과 같아야 한다 (services/inventory-service/src/main/java/com/example/msa/inventory/domain/ReservationStatus.java, services/inventory-service/src/main/java/com/example/msa/inventory/domain/ReservationReason.java)
  - 근거: research.md 결정 13, `contracts/inventory-api.yaml`의 `ReservationResult`. 값을 나중에 더하면 계약을 깨는 변경이다(`docs/standards/architecture-rules.md` 2절의 API 변경 규칙).
- [ ] T059 [US1] 예약 판단의 단위 테스트를 쓴다. plan.md 표의 시나리오 1.1 "단위" 칸 "예약 판단(같은 내용, 다른 내용)"이다. 같은 내용(저장된 A 3개, 들어온 A 3개)이면 저장된 결과를 돌려준다는 판단, 다른 내용(A 5개)이면 충돌이라는 판단, 항목 순서만 다르면 같은 내용이라는 판단, 해제 표식이면 비교하지 않고 RELEASED라는 판단을 검사한다. 값은 시나리오 1.1, 1.3, 1.4의 값을 쓴다 (services/inventory-service/src/test/java/com/example/msa/inventory/domain/ReservationDecisionTest.java)
  - 기대값의 출처: 도메인 분석 6절 첫 문단("내용(상품 순으로 정렬한 항목 목록)이 다르면 호출한 쪽의 버그로 본다")과 표 1번, 3번, 7번. 구현 방법의 근거는 data-model.md 6-2절 1번과 research.md 결정 14다. 해제 표식을 "항목 없는 기록"으로 나타내는 것은 data-model.md에만 있으므로 단언하지 않는다. Spring 없이 돈다.
- [ ] T060 [US3] 사유 우선순위의 단위 테스트를 쓴다. plan.md 표의 "3.3의 재고 쪽" 줄 "단위" 칸 "사유 우선순위(상품 없음이 먼저)"다. 시나리오 3.3 변형의 값(Z 없음, B 부족)으로 사유가 `PRODUCT_NOT_FOUND`이고 두 목록이 모두 차는지 검사한다. 예약 판단 로직(T061)보다 먼저 쓴다. 테스트 전략 문서 3절이 도메인 단위 테스트에 TDD를 권장하기 때문이다(2026-10-06 사용자 승인) (services/inventory-service/src/test/java/com/example/msa/inventory/domain/ReservationDecisionReasonTest.java)
- [ ] T061 [US1] 예약 판단 로직을 만든다. 저장된 항목과 들어온 항목을 상품 ID 순으로 정렬해 비교한다. 항목별 재고 확인 결과로 RESERVED와 REJECTED를 정하고, 사유는 없는 상품이 하나라도 있으면 `PRODUCT_NOT_FOUND`, 아니면 `OUT_OF_STOCK`이다. 부족한 상품 목록과 없는 상품 목록은 상품 ID 순이다. Spring과 MyBatis를 모르는 순수 Java로 쓴다 (services/inventory-service/src/main/java/com/example/msa/inventory/domain/ReservationDecision.java)
  - 근거: data-model.md 6-2절 1번·3번, `contracts/inventory-api.yaml`의 `shortageProductIds`, `missingProductIds` 설명("상품 ID 순").
  - 사유 우선순위의 단위 테스트는 위 T060이다.
- [ ] T062 [P] [US1] DTO를 만든다. `ReserveRequest`(항목 목록. 항목은 안쪽 record `Item(productId, quantity)` `[제안]`), `ReservationResponse`(`orderNo`, `status`, `reason`, `shortageProductIds`, `missingProductIds`), `ReleaseResponse`(`orderNo`, `status`) `[제안]`, `StockRow`는 record다. `ReservationWithItems`는 `<collection>`으로 읽는 1:N 부모라서 Lombok 없는 일반 클래스다 (services/inventory-service/src/main/java/com/example/msa/inventory/dto/ReserveRequest.java, services/inventory-service/src/main/java/com/example/msa/inventory/dto/ReservationResponse.java, services/inventory-service/src/main/java/com/example/msa/inventory/dto/ReleaseResponse.java, services/inventory-service/src/main/java/com/example/msa/inventory/dto/StockRow.java, services/inventory-service/src/main/java/com/example/msa/inventory/dto/ReservationWithItems.java)
  - 근거: data-model.md 7절, research.md 결정 9. `ReleaseResponse`는 data-model.md 7절에 없다. 계약의 `ReleaseResult`가 `ReservationResult`와 필드가 달라서 따로 둔다 `[제안]`.
  - 받는 쪽이 모르는 필드를 무시하도록 JSON 역직렬화 설정을 둔다(plan.md Constitution Check 7번 줄의 `[제안]`).
- [ ] T063 [US1] 재고 Mapper 테스트를 쓴다. plan.md 표의 시나리오 1.2 "Mapper" 칸 "재고 행 잠금과 감소"다. 상품 A 10개 행을 `SELECT ... FOR UPDATE`로 읽고 1개 줄이면 9개가 되는지, 잠금을 잡은 동안 다른 트랜잭션의 같은 행 잠금이 기다리는지 검사한다. `@MybatisTest`와 T039의 Oracle 컨테이너를 쓴다 (services/inventory-service/src/test/java/com/example/msa/inventory/mapper/StockMapperTest.java)
  - 기대값의 출처: 설계 6절 "예약 처리" 2번("재고 행을 상품 ID 순서로 하나씩 `SELECT ... FOR UPDATE`로 잠그고 읽는다")과 그 아래 설명("행 잠금으로 동시 예약의 초과 판매를 막고").
- [ ] T064 [US1] 예약 Mapper 테스트를 쓴다. plan.md 표의 시나리오 1.1 "Mapper" 칸 "예약 기록 삽입, 고유 제약 충돌"과 시나리오 1.3 "Mapper" 칸 "해제 표식 삽입"이다. 주문 번호 1001의 예약 기록과 항목을 넣고 `ReservationWithItems`로 한 번에 읽는지(`<collection>`), 같은 주문 번호를 한 번 더 넣으면 고유 제약 위반 예외가 나는지, 주문 번호 1002의 해제 표식(RELEASED)을 넣고 읽는지 검사한다 (services/inventory-service/src/test/java/com/example/msa/inventory/mapper/ReservationMapperTest.java)
  - 기대값의 출처: 도메인 분석 6절 표 2번("예약 기록의 주문 번호 고유 제약 때문에 한쪽만 반영된다")과 6번("해제 표식(해제됨)만 남긴다"), 설계 6절 "예약 처리" 4번. `<collection>`으로 한 번에 읽는 것은 `docs/standards/coding-conventions.md` 3-2절이다.
- [ ] T065 [US9] 수량 복구 Mapper 테스트를 쓴다. plan.md 표의 시나리오 9.1 재고 쪽 "Mapper" 칸 "수량 복구"다. 상품 A 7개 행에 3개를 되돌리면 10개가 되는지 검사한다. T063과 파일을 나눠 User Story 9의 작업이 User Story 1의 파일을 고치지 않게 한다 `[제안]` (services/inventory-service/src/test/java/com/example/msa/inventory/mapper/StockRestoreMapperTest.java)
- [ ] T066 [US1] 재고 Mapper와 SQL을 만든다. 아래 data-model.md 6-2절의 SQL을 쓰고, 수량 복구는 같은 모양의 `UPDATE STOCK SET QUANTITY = QUANTITY + #{quantity} WHERE PRODUCT_ID = #{productId}`로 쓴다 `[제안]`. 메서드 이름은 `select…`, `update…`로 시작하고 SQL id와 같다 (services/inventory-service/src/main/java/com/example/msa/inventory/mapper/StockMapper.java, services/inventory-service/src/main/resources/mapper/StockMapper.xml)
  - 원문 (data-model.md 6-2절 2번): `SELECT QUANTITY FROM STOCK WHERE PRODUCT_ID = #{productId} FOR UPDATE`
  - 원문 (data-model.md 6-2절 3번): `UPDATE STOCK SET QUANTITY = QUANTITY - #{quantity} WHERE PRODUCT_ID = #{productId}`
  - 파라미터는 `#{}`만 쓴다. `SELECT *`를 쓰지 않는다. 애너테이션 SQL을 쓰지 않는다(`docs/standards/coding-conventions.md` 3-2절).
- [ ] T067 [US1] 예약 Mapper와 SQL을 만든다. 예약 기록과 항목을 상품 ID 순으로 한 번에 읽는 `resultMap`(`<collection>`), 예약 기록 잠금 조회(`SELECT ... FOR UPDATE`, data-model.md 6-3절 1번), 예약 기록 삽입, 항목 삽입(항목별 `RESULT` 포함), 상태 변경을 둔다 (services/inventory-service/src/main/java/com/example/msa/inventory/mapper/ReservationMapper.java, services/inventory-service/src/main/resources/mapper/ReservationMapper.xml)
- [ ] T068 [US1] research.md 6절 "Testcontainers 2.0.5가 이 PC의 Docker Desktop에서 `gvenzl/oracle-xe:21.3.0-slim-faststart`를 띄우는지"를 확인한다. 첫 Mapper 테스트를 돌린다 (services/inventory-service/src/test/java/com/example/msa/inventory/mapper/StockMapperTest.java)
  - 명령: `.\gradlew.bat :services:inventory-service:test --tests "com.example.msa.inventory.mapper.*"`
  - 기대 결과: 종료 코드 0. 컨테이너가 뜨는 데 걸린 시간을 테스트 로그에서 읽어 보고에 적는다.
  - 컨테이너가 뜨지 않으면 `docs/references/docker-desktop.md`부터 본다. 로컬 인프라가 떠 있어 메모리가 모자라면 `.\tools\infra-down.ps1`로 내릴지 사용자에게 먼저 묻는다(quickstart.md 1절).
- [ ] T069 [US1] research.md 6절 "MyBatis 3.5.19에서 record 생성자 매핑이 되는지, 생성자 안 `<collection>`이 안 되는지"를 확인한다 (services/inventory-service/src/test/java/com/example/msa/inventory/mapper/ReservationMapperTest.java)
  - record 생성자 매핑: `StockRow`(record)를 읽는 Mapper 테스트가 T068에서 통과했는지 본다.
  - 생성자 안 `<collection>`: `ReservationWithItems`를 잠시 record로 바꾸고 `<constructor>` 안에 `<collection>`을 두어 T064를 돌려 본다. 결과를 보고에 적고 일반 클래스로 되돌린다.
  - record로 된다면 research.md 결정 9의 판단과 다르다. plan을 바꿀지는 사용자가 정하므로, 일반 클래스를 그대로 두고 보고한다.
- [ ] T070 [US1] 예약·해제 트랜잭션 서비스를 만든다. `reserve`와 `release`는 각각 한 트랜잭션이다. `reserve`는 data-model.md 6-2절 1~4번, `release`는 6-3절 1~2번 순서를 따른다. 재고 행은 언제나 상품 ID 순서로 잠근다 (services/inventory-service/src/main/java/com/example/msa/inventory/service/ReservationTxService.java)
  - 근거: data-model.md 6-2절, 6-3절, `docs/standards/coding-conventions.md` 3-2절 "여러 행을 잠글 때는 언제나 같은 순서".
- [ ] T071 [US1] 예약 흐름 서비스를 만든다. `ReservationTxService`를 부르고, 같은 주문 번호가 동시에 들어와 `PK_RESERVATIONS`에 걸리면 전체를 롤백한 뒤 1번부터 한 번 다시 한다. 해제 표식을 넣다 `PK_RESERVATIONS`에 걸려도 같다 (services/inventory-service/src/main/java/com/example/msa/inventory/service/ReservationService.java)
  - 근거: data-model.md 6-2절 4번("다시 하는 횟수는 한 번으로 둔다 `[제안]`"), 6-3절 2번.
- [ ] T072 [US1] 오류 응답 변환을 만든다. 모두 Problem Details(`application/problem+json`)이고 `type`은 `urn:msa-example:problem:<코드>`, 확장 필드는 `code`와 `errors`다. 형식 오류 400은 `INVALID_REQUEST`(필드별 `errors`), 같은 주문 번호에 다른 내용 409는 `RESERVATION_CONFLICT`, 교착(`ORA-00060`) 등 예상하지 못한 오류 500은 `INTERNAL_ERROR`다 (services/inventory-service/src/main/java/com/example/msa/inventory/exception/ProblemDetailsHandler.java, services/inventory-service/src/main/java/com/example/msa/inventory/exception/ReservationConflictException.java)
  - 근거: research.md 결정 13, `contracts/inventory-api.yaml`의 응답 정의.
- [ ] T073 [US1] 예약·해제 컨트롤러를 만든다. `PUT /reservations/{orderNo}`와 `DELETE /reservations/{orderNo}`이고 업무 결과는 처음이든 재요청이든 언제나 200이다. 컨트롤러는 형식 검사, 서비스 호출, 응답 변환만 한다 (services/inventory-service/src/main/java/com/example/msa/inventory/controller/ReservationController.java)
  - 형식 검사는 data-model.md 5-2절 원문 그대로다:

    | 검사 | 실패하면 |
    |---|---|
    | 경로의 주문 번호가 양의 정수다 | 400 |
    | 항목이 1개 이상 20개 이하, 수량 1~99, 같은 상품은 한 줄, 상품 ID 형식 | 400 |

  - 상품 ID 형식은 `^[A-Z0-9-]{1,20}$`다(`contracts/inventory-api.yaml`의 `ReservationItem.productId`).
- [ ] T074 [US1] 이 절의 테스트가 모두 통과하는지 확인한다 (services/inventory-service/src/test/java/com/example/msa/inventory/integration/ReservationIdempotencyIntegrationTest.java)
  - 명령: `.\gradlew.bat :services:inventory-service:test`
  - 기대 결과: 종료 코드 0. 위 "인수 시나리오 테스트" 묶음의 테스트와 받치는 테스트(T059, T060, T063, T064, T065)가 모두 통과한다. 통과·실패 수를 보고에 적는다.
  - 실패하면 구현을 고친다. 기대값을 바꾸지 않는다.
- [ ] T075 [US1] research.md 6절 "swagger-request-validator-core 2.46.1이 OpenAPI 3.0.3 계약과 JDK 17에서 도는지"를 확인한다. T074에서 T056의 계약 테스트가 통과했는지 보고, 계약에 어긋난 응답을 하나 일부러 만들어(예: 응답의 `status`를 계약에 없는 값으로 바꾼 가짜 응답) 도우미가 잡는지 본 뒤 되돌린다 (services/inventory-service/src/test/java/com/example/msa/inventory/contract/ReservationProviderContractTest.java)

이 목록에서는 항목 ID를 쓰지 않았다.

**체크포인트**: 재고 서비스만으로 User Story 1의 시나리오 다섯 개, 시나리오 2.1·3.1·3.3·6.1·8.1·9.1의 재고 쪽, ㉠, ㉡, ㉥이 통과한다.

### 마무리와 확인

- [ ] T076 재고 서비스의 아키텍처 테스트를 쓴다. `libs/archunit-rules`의 세 규칙(계층 의존, 애너테이션 SQL 금지, `@Transactional`에서 `client` 직접 호출 금지)을 `com.example.msa.inventory`에 적용한다. 재고 서비스에는 `client` 패키지가 없으므로, 트랜잭션 안 원격 호출 규칙에만 `allowEmptyShould(true)`를 둔다. 나중에 재고 서비스에 `client` 패키지가 생기면 이 규칙이 바로 검사한다(2026-10-07 사용자 승인. `CLAUDE.md` 7절의 "ArchUnit 규칙 예외"로 승인받은 것이다). 구현이 끝난 뒤에 쓰는 이유는, ArchUnit이 검사할 클래스가 없는 규칙을 실패로 보기 때문이다 (services/inventory-service/src/test/java/com/example/msa/inventory/architecture/ArchitectureTest.java)
  - 근거: plan.md "인수 시나리오와 테스트 층" 절 끝 문단("모든 단계에서 함께 도는 아키텍처 테스트"), `docs/standards/testing.md` 2절 "아키텍처" 층.
- [ ] T077 기계 검사와 아키텍처 테스트가 실제로 잡는지 하나씩 확인한다. 아래 위반을 하나 넣고 `.\gradlew.bat :services:inventory-service:check`가 실패하는 것과 실패 메시지가 그 규칙을 가리키는 것을 본 뒤 되돌린다. 위반은 한 번에 하나씩 넣는다 (services/inventory-service/build.gradle)
  1. 컨트롤러에서 `StockMapper`를 직접 부른다 → T076의 계층 규칙
  2. Mapper 메서드에 `@Select`를 붙인다 → T076의 애너테이션 SQL 규칙
  3. Mapper XML에 `${orderNo}`를 쓴다 → T027
  4. Mapper XML에 `SELECT *`를 쓴다 → T027
  5. 새 임시 파일 `services/inventory-service/src/main/resources/db/migration/V99__check.sql`에 `CREATE TABLE IF NOT EXISTS ...`를 쓴다 → T028. 이미 있는 V1·V2 파일은 고치지 않는다
  6. 테스트 메서드 하나에 `@Disabled`를 붙인다 → T026
  7. `build.gradle`에 `compileOnly 'org.projectlombok:lombok'`을 더한다 → T025
  8. 인수 시나리오 테스트 하나의 `@DisplayName` 앞 ID를 잠시 지운다 → T029
  - 끝나면 `git status`와 `git diff`로 위반이 모두 되돌려졌는지 본다. 결과(위반, 실패한 작업, 메시지 한 줄)를 표로 보고에 적는다.
  - 트랜잭션 안 원격 호출 규칙은 `client` 패키지가 생기는 Phase 2(T151)에서 확인한다.
- [ ] T078 재고 서비스 설정이 plan대로 들어갔는지 확인한다 (services/inventory-service/src/main/resources/application.yml)
  - 명령: `Select-String -Path services/inventory-service/src/main/resources/application.yml -Pattern 'shutdown: graceful','timeout-per-shutdown-phase: 35s','probes','structured','arg-name-based-constructor-auto-mapping'`
  - 기대 결과: 다섯 가지가 모두 나온다. 비밀번호 값이 파일에 없다(`Select-String -Pattern 'password'`로 나온 줄이 환경변수 참조뿐이다).
  - T033과 T034에서 "확인하지 못했다"로 남긴 두 가지의 결과를 보고에 적는다. 하나는 Spring Boot 4.1.1의 OTLP 속성 이름이고, 다른 하나는 OpenTelemetry Logback appender를 자동으로 연결하는지다. 각각 확인한 문서 주소와 그 결과로 고른 설정(속성 이름, `ObservabilityConfig`를 만들었는지)을 적는다.
- [ ] T079 이 단계의 기준을 확인한다. 이 단계 머리의 "끝났다고 보는 기준" 표에서 GitHub Actions를 뺀 네 줄을 차례로 실행한다 (gradle.properties)
  - 기대 결과: `msa.stage=P1` 한 줄, `.\gradlew.bat build` 종료 코드 0, 재고 서비스 테스트 종료 코드 0, 계약 두 파일의 차이 없음. 실행한 명령과 종료 코드, 테스트 통과·실패 수를 보고에 적는다(저장소 루트 `CLAUDE.md` 6절).
  - quickstart.md 3절 P1 줄과 대조해, 재고 서비스의 인수 시나리오 테스트(시나리오 1.1, 1.2, 1.3, 1.4, 1.5, 시나리오 2.1·3.1·3.3·6.1·8.1·9.1의 재고 쪽, ㉠, ㉡, ㉥)가 모두 보고서에 있는지 하나씩 적는다.
- [ ] T080 사용자가 push한 뒤 GitHub Actions 결과를 확인한다. research.md 6절 "GitHub Actions 러너에서 Oracle 컨테이너를 띄우는 시간과 메모리"도 여기서 확인한다 (.github/workflows/build.yml)
  - 명령: `gh run list --branch feat/001-place-order-p1 --limit 1`, `gh run view <run-id> --log`
  - 기대 결과: 결론이 `success`. 로그에서 Oracle 컨테이너가 뜨는 데 걸린 시간과 전체 빌드 시간을 읽어 보고에 적는다. 메모리 부족으로 실패하면 멈추고 보고한다.
  - push는 사용자가 한다. push 전이면 이 작업은 "실행하지 않았다"로 보고한다.
- [ ] T081 사용자에게 보고하고 멈춘다. 만들거나 고친 파일, T013·T041·T057·T068·T069·T074·T075·T077·T078·T079·T080의 명령과 결과, spec 시나리오 1.1, 1.2, 1.3, 1.4, 1.5와 시나리오 2.1·3.1·3.3·6.1·8.1·9.1의 재고 쪽, ㉠, ㉡, ㉥을 하나씩 대조한 결과, research.md 6절에서 이 단계에 확인한 다섯 가지(T041, T068, T069, T075, T080)와 tasks.md가 새로 적은 미확인 항목 세 가지(T041, T078)의 결과를 적는다. 커밋·push·PR은 사용자가 요청할 때 한다 (파일 없음, 대화창 보고)

이 목록에서는 항목 ID를 쓰지 않았다.

---

## Phase 2: P2 단계 PR — 주문 서비스 (브랜치 `feat/001-place-order-p2`)

**목적**: 주문 서비스 전체를 만든다. 주문 서비스가 재고 서비스를 처음 부르므로, 시도당 제한 시간과 서킷 브레이커를 이 단계에 처음부터 넣는다. 재시도는 없다(spec "우선순위와 PR 단계" 절, FR-028).

**담는 것** (plan.md "PR 단계" 표의 P2 줄): 주문 서비스 전체(주문 API, JWT 검증, 항목·요청 키 검사, 주문 저장, `InventoryClient`의 시도당 제한 시간과 서킷 브레이커, 해제 실행기, 추적 전달, 로그·프로브·그레이스풀 셧다운 설정, `CLAUDE.md`). `msa.stage=P2`. P2 스토리의 재고 쪽 인수 테스트는 Phase 1에 있다.

**끝났다고 보는 기준** (plan.md "PR 단계" 표): `.\gradlew.bat build` 종료 코드 0, P1의 테스트도 그대로 통과, GitHub Actions 성공

| 확인할 것 | 명령 | 기대 결과 |
|---|---|---|
| 단계 표시 | `Select-String -Path gradle.properties -Pattern '^msa.stage=P2$'` | 한 줄 |
| 빌드와 모든 검사 | `.\gradlew.bat build` | 종료 코드 0 |
| P1의 테스트가 그대로 통과 | `.\gradlew.bat :services:inventory-service:test` | 종료 코드 0. 이 단계는 재고 서비스를 고치지 않는다(`git fetch origin` 뒤 `git diff origin/main --stat -- services/inventory-service`의 출력이 없다) |
| 주문 쪽 인수 시나리오 테스트 | `.\gradlew.bat :services:order-service:test` | 종료 코드 0. 시나리오 2.1, 3.1, 3.2, 3.3, 4.1, 5.1, 6.1, 7.1, 7.2와 ㉦, ㉧, ㉨, ㉩의 테스트가 통과로 나온다(quickstart.md 3절 P2 줄) |
| GitHub Actions | 사용자가 push한 뒤 `gh run list --branch feat/001-place-order-p2 --limit 1` | 결론이 `success` |

**시작 조건**: Phase 1의 PR이 main에 병합되어 있고, 이 단계의 브랜치를 main에서 만들었다. 브랜치를 만드는 일은 사용자에게 묻고 한다. 확인 명령: `git branch --show-current` → `feat/001-place-order-p2`.

**테스트 공통** `[제안]`: 주문 서비스의 서비스 통합 테스트는 `@SpringBootTest`와 MockMvc로 앱을 부르고, 고객은 Spring Security Test의 `jwt()`로 흉내 낸다(`docs/standards/testing.md` 3절 "인증"). 고객 C1, C2의 `sub` 값은 테스트 안에서 정한 문자열이다. 재고 서비스 자리에는 WireMock을 두고(`docs/standards/testing.md` 3절 "상대 서비스는 WireMock"), 재고 주소 설정을 WireMock 주소로 바꾼다. "기록되어 있다", "기록되지 않는다"는 저장된 주문을 `JdbcTemplate`으로 직접 읽어 확인한다(spec "공통 전제"에 더한 전제). 재고 수량 기대값은 spec Assumptions "재고 수량 기대값을 나눠 검증한다"(research.md 결정 1로 받아들인 제안 2번)대로 나눈다. 주문 쪽은 WireMock이 받은 예약 요청의 횟수와 내용(주문 번호, 상품, 수량)을 본다. 재고 쪽(같은 내용의 예약 요청을 실제 재고 서비스에 보내 재고 수량을 보는 테스트)은 Phase 1에서 이미 썼다.

### 준비

- [ ] T082 단계 표시를 `msa.stage=P2`로 올리고, 테스트 케이스 단계 표에서 단계가 P2인 줄이 plan.md "인수 시나리오와 테스트 층" 표와 같은지 확인한다. 다르면 표를 고치지 말고 멈춰 사용자에게 묻는다 (gradle.properties, config/quality/test-case-stages.csv)
  - 근거: research.md 결정 11. 표는 Phase 1(T023)에서 모든 줄을 넣었으므로, 이 단계에서는 보통 표를 바꿀 일이 없다.
  - 확인: `Select-String -Path config/quality/test-case-stages.csv -Pattern ',P2$'` → 13줄 (모두 `order` 줄이다)
- [ ] T083 루트 설정에 `include 'services:order-service'`를 더한다 (settings.gradle)
- [ ] T084 버전 카탈로그에 Resilience4j `resilience4j-circuitbreaker` 2.4.0과 WireMock `org.wiremock:wiremock-standalone` 3.13.2를 더한다. 재시도 모듈은 Phase 3(T157)에서 더한다 `[제안]` (gradle/libs.versions.toml)
  - 근거: research.md 0절, 결정 8. Resilience4j의 Spring Boot 자동 설정 모듈과 Spring Cloud Circuit Breaker는 쓰지 않는다.
- [ ] T085 주문 서비스 빌드 파일을 만든다. `plugins { id 'msa.java-service' }`를 적용한다 (services/order-service/build.gradle)
  - 의존성: `spring-boot-starter-webmvc`, `-validation`, `-actuator`, `-flyway`, `-opentelemetry`, `-oauth2-resource-server`, `-restclient`, `flyway-database-oracle`, Oracle JDBC(T031과 같은 것), `mybatis-spring-boot-starter`, OpenTelemetry Logback appender, `resilience4j-circuitbreaker`.
  - 테스트 의존성: Spring Boot 테스트 스타터(T031과 같은 것), `spring-boot-starter-security-test`, `spring-boot-testcontainers`, `testcontainers-oracle-xe`, `mybatis-spring-boot-starter-test`, `archunit-junit5`, `wiremock-standalone`, `swagger-request-validator-core`, `project(':libs:archunit-rules')`.
- [ ] T086 앱 진입점을 만든다 (services/order-service/src/main/java/com/example/msa/order/OrderServiceApplication.java)
- [ ] T087 앱 설정을 만든다 (services/order-service/src/main/resources/application.yml)
  - DB 접속 값은 환경변수로 받는다 `[제안]`: `ORDER_DB_URL`(기본 `jdbc:oracle:thin:@//localhost:1521/XEPDB1`. `infra/compose/compose.yaml`의 order-db 호스트 포트가 1521이다), `ORDER_DB_USERNAME`(기본 `ORDER_SVC`), `ORDER_SVC_PASSWORD`(기본값 없음. `.env.example`에 이미 있는 이름이다).
  - JWT 발급자: `spring.security.oauth2.resourceserver.jwt.issuer-uri`를 환경변수 `JWT_ISSUER_URI`(기본 `http://localhost:8180/realms/msa`)로 받는다 `[제안]`. 기본값은 data-model.md 5-1절의 발급자다.
  - 재고 호출: 주소는 환경변수 `INVENTORY_BASE_URL`(기본 `http://inventory:8080`)로 받는다 `[제안]`(research.md 4절 "재고 서비스 주소 기본값"). 연결·요청 제한 시간은 각각 2.5초다(research.md 결정 6).
  - 서킷 브레이커: research.md 결정 8의 표 원문 그대로다. 이 표(설계 4절 값을 Resilience4j 설정 이름으로 옮긴 대응표)는 research.md에서 `[제안]`이다. `slidingWindowType=COUNT_BASED`, `slidingWindowSize=10`, `minimumNumberOfCalls=10`, `failureRateThreshold=50`, `waitDurationInOpenState=10s`, `permittedNumberOfCallsInHalfOpenState=3`.
  - 해제 실행기: 스레드 2~4개, 대기열 100, 종료할 때 실행 중인 해제를 최대 30초 기다린다(research.md 4절 "해제를 고객 응답 뒤에 비동기로 돌리는 방식"의 `[제안]`).
  - MyBatis, 로그(`ecs`), 프로브, 그레이스풀 셧다운, OTLP 주소는 재고 서비스(T033)와 같은 방식이다.
  - 값은 모두 `@ConfigurationProperties`로 받는다(`docs/standards/coding-conventions.md` 3-5절). 코드에 적지 않는다.
- [ ] T088 [P] 로그 설정을 만든다. 재고 서비스(T034)와 같은 방식이다 (services/order-service/src/main/resources/logback-spring.xml, services/order-service/src/main/java/com/example/msa/order/config/ObservabilityConfig.java)
- [ ] T089 [P] 테스트 설정을 만든다. 이 문서 머리의 "시간 값은 1/5로 줄인다" 표대로 시도당 제한 시간 500ms, 서킷 열려 있는 시간 2초로 둔다 `[제안]`. OTLP 내보내기를 끈다 (services/order-service/src/test/resources/application-test.yml)
- [ ] T090 [P] 주문 서비스 규칙 문서를 만든다. 패키지 구조(plan.md "Source Code" 트리의 주문 서비스 부분), DB 계정 `ORDER_SVC`와 테이블, `OrderService.place()`에는 트랜잭션이 없고 저장은 `OrderTxService`에서 따로 커밋한다는 규칙, 고객 ID는 JWT `sub`에서만 얻는다는 규칙, 재고는 `InventoryClient`로만 부른다는 규칙을 적는다 (services/order-service/CLAUDE.md)
  - 근거: 헌법 "기술 제약" 절, research.md 결정 4, `docs/standards/coding-conventions.md` 3-1절, `docs/standards/architecture-rules.md` 5절 "보안"의 JWT 규칙.

이 목록에서는 항목 ID를 쓰지 않았다.

### 기반

**⚠️ 이 절이 끝나기 전에는 User Story 2를 시작하지 않는다.**

- [ ] T091 주문 스키마 마이그레이션을 만든다. 아래 data-model.md 2절의 시퀀스, 열, 제약을 그대로 만든다 (services/order-service/src/main/resources/db/migration/V1__orders.sql)
  - 원문 (data-model.md 2-1절): 주문 번호를 만드는 시퀀스다. `START WITH 1 INCREMENT BY 1 NOCACHE`로 만든다 `[제안]`.
  - 원문 (data-model.md 2-2절 `ORDERS`):

    | 열 | 타입 | NULL | 뜻 |
    |---|---|---|---|
    | `ORDER_NO` | `NUMBER(19)` | 아니오 | 주문 번호. `ORDERS_SEQ`에서 받는다. 재고 예약의 멱등 키다 |
    | `CUSTOMER_ID` | `VARCHAR2(255)` | 아니오 | 고객 ID. 검증한 JWT의 `sub` 값이다 |
    | `REQUEST_KEY` | `VARCHAR2(36)` | 아니오 | 주문 요청 키(UUID 문자열) |
    | `STATUS` | `VARCHAR2(20)` | 아니오 | `PENDING`, `CONFIRMED`, `REJECTED`, `FAILED` |
    | `REASON` | `VARCHAR2(30)` | 예 | `OUT_OF_STOCK`, `PRODUCT_NOT_FOUND`, `RETRY_LATER`. 처리중과 확정이면 비어 있다 |
    | `RELEASE_STATUS` | `VARCHAR2(20)` | 예 | `NOT_REQUIRED`, `RELEASED`, `RELEASE_FAILED`. "실패"가 아니거나 해제 요청 전이면 비어 있다 |
    | `CREATED_AT` | `TIMESTAMP` | 아니오 | 처음 기록한 시각 |
    | `UPDATED_AT` | `TIMESTAMP` | 아니오 | 마지막으로 바꾼 시각 |

    - `PK_ORDERS` 기본 키 (`ORDER_NO`)
    - `UK_ORDERS_CUSTOMER_KEY` 고유 제약 (`CUSTOMER_ID`, `REQUEST_KEY`). 같은 고객의 같은 키는 주문 하나다. 다른 고객의 같은 키는 별개다.
    - `CK_ORDERS_STATUS`, `CK_ORDERS_REASON`, `CK_ORDERS_RELEASE_STATUS` CHECK 제약으로 값 목록을 막는다 `[제안]`.

  - 원문 (data-model.md 2-3절 `ORDER_ITEMS`):

    | 열 | 타입 | NULL | 뜻 |
    |---|---|---|---|
    | `ORDER_NO` | `NUMBER(19)` | 아니오 | 주문 번호 |
    | `PRODUCT_ID` | `VARCHAR2(20)` | 아니오 | 상품 ID(`^[A-Z0-9-]{1,20}$`) |
    | `QUANTITY` | `NUMBER(2)` | 아니오 | 수량 1~99 |
    | `REJECT_REASON` | `VARCHAR2(20)` | 예 | 거절된 주문에서 이 상품이 부족했으면 `OUT_OF_STOCK`, 없었으면 `PRODUCT_NOT_FOUND`. 그 밖에는 비어 있다 `[제안]` |

    - `PK_ORDER_ITEMS` 기본 키 (`ORDER_NO`, `PRODUCT_ID`). 같은 상품은 한 주문에 한 줄만 들어간다.
    - `FK_ORDER_ITEMS_ORDERS` 외래 키 (`ORDER_NO`) → `ORDERS`
    - `CK_ORDER_ITEMS_QTY` CHECK (`QUANTITY BETWEEN 1 AND 99`)

  - 시퀀스 이름은 `ORDERS_SEQ`다(data-model.md 1절 표). CHECK 제약의 값 목록은 위 "뜻" 칸의 값이다. `ORDER_ITEMS.REJECT_REASON`의 값 목록 CHECK는 data-model.md에 없으므로 두지 않는다 `[제안]`.
- [ ] T092 테스트용 Oracle 컨테이너를 만든다. 재고 서비스의 T039와 같은 방식이고, 공유하지 않고 주문 서비스에 따로 둔다 (services/order-service/src/test/java/com/example/msa/order/OracleTestContainer.java `[제안]`)
- [ ] T093 [P] WireMock 도우미를 만든다. JUnit 확장 `WireMockExtension`으로 WireMock을 띄우고, 재고 주소 설정을 그 주소로 바꾸고, 예약·해제 스텁과 받은 요청 조회를 돕는다 (services/order-service/src/test/java/com/example/msa/order/integration/InventoryWireMock.java `[제안]`)
  - 근거: research.md 4절 "WireMock"(`wiremock-standalone` 3.13.2와 `WireMockExtension`).
- [ ] T094 [P] 소비자 계약 검증 도우미를 만든다. WireMock 스텁이 돌려주는 응답과 WireMock이 받은 요청(`getAllServeEvents`)을 swagger-request-validator-core의 `Request`·`Response`로 옮겨 `contracts/inventory-api.yaml`로 검증한다. 계약 파일 경로는 시스템 속성 `msa.contractsDir`에서 얻는다 (services/order-service/src/test/java/com/example/msa/order/contract/ConsumerContractValidator.java)
  - 근거: research.md 결정 12. 재고 서비스의 T040과 공유하지 않는다.
  - 계약에 없는 응답 코드(502·503·504와 지연·연결 끊김)는 재고 앱이 아니라 네트워크와 인프라의 오류를 흉내 낸 것이다. 그래서 그 스텁의 응답은 계약 검증에서 뺀다 `[제안]`. 받은 요청은 언제나 검증한다.
- [ ] T095 [P] 주문 상태 전이의 단위 테스트를 쓴다. `OrderStatus.canMoveTo(...)`가 아래를 지키는지 검사한다. 이 테스트는 아직 없는 `OrderStatus`를 참조하므로 컴파일 오류로 실패한다. 그 실패를 본 뒤 T096을 한다 (services/order-service/src/test/java/com/example/msa/order/domain/OrderStatusTest.java `[제안]`)
  - 처리중(`PENDING`)에서는 확정(`CONFIRMED`), 거절(`REJECTED`), 실패(`FAILED`)로 갈 수 있다.
  - 확정과 거절에서는 다른 상태로 가지 않는다.
  - 실패에서는 실패로만 간다(해제 결과를 기록할 때).
  - 기대값의 출처: 도메인 분석 4절 "주문 상태"의 그림. 그림에 있는 전이만 허락하고 나머지는 막는다(2026-10-06 사용자 결정). `docs/standards/testing.md` 2절이 단위 테스트 대상에 "주문 상태 전이"를 넣는다.
- [ ] T096 [P] 주문 상태와 사유 enum을 만든다. `OrderStatus`(`PENDING`, `CONFIRMED`, `REJECTED`, `FAILED`)에 `canMoveTo(...)`를 두고, data-model.md 4-1절 표대로 `PENDING`에서만 다른 상태로 갈 수 있게 한다. `OrderReason`(`OUT_OF_STOCK`, `PRODUCT_NOT_FOUND`, `RETRY_LATER`), `ReleaseStatus`(`NOT_REQUIRED`, `RELEASED`, `RELEASE_FAILED`)도 만든다 (services/order-service/src/main/java/com/example/msa/order/domain/OrderStatus.java, services/order-service/src/main/java/com/example/msa/order/domain/OrderReason.java, services/order-service/src/main/java/com/example/msa/order/domain/ReleaseStatus.java)
  - 근거: data-model.md 4-1절·4-2절, research.md 결정 13, `docs/standards/coding-conventions.md` 3-1절.
  - 상태 전이의 단위 테스트는 위 T095다.
- [ ] T097 오류 응답 변환의 바탕을 만든다. 모두 Problem Details이고 `type`은 `urn:msa-example:problem:<코드>`, 확장 필드는 `code`와 `errors`다. 코드별 처리는 각 스토리에서 더한다 (services/order-service/src/main/java/com/example/msa/order/exception/ProblemDetailsHandler.java)
  - 근거: research.md 결정 13, `contracts/order-api.yaml`의 `Problem`.
- [ ] T098 재고 호출 설정 클래스를 만든다. 주소, 연결 제한 시간, 요청 제한 시간을 받는 `@ConfigurationProperties` record다. 서킷 브레이커 값은 User Story 7(T144)에서, 재시도와 전체 한도 값은 Phase 3(T159)에서 더한다 (services/order-service/src/main/java/com/example/msa/order/config/InventoryClientProperties.java)
  - 근거: research.md 4절 "설정 클래스", data-model.md 7절.

이 목록에서는 항목 ID를 쓰지 않았다.

### User Story 2 — 고객은 재고가 있는 상품 여러 개를 한 번에 주문하고 확정 결과를 받는다 (Priority: P2)

**목표**: 로그인한 고객의 주문을 "처리중"으로 기록하고, 재고 예약 결과가 RESERVED면 "확정"으로 바꿔 주문 번호와 함께 돌려준다.

**독립 테스트** (spec User Story 2): 주문 서비스를 띄우고 재고 서비스 자리에는 WireMock을 둔다. WireMock이 RESERVED를 돌려줄 때 고객이 주문 번호와 "확정"을 받는지, 주문이 "확정"으로 저장되는지 확인한다.

#### 인수 시나리오 테스트 (구현보다 먼저)

- [ ] T099 [P] [US2] 시나리오 2.1의 주문 쪽 서비스 통합 테스트를 쓴다. WireMock 예약 스텁이 RESERVED를 돌려줄 때, C1이 상품 A 3개와 상품 B 2개를 주문한다. 응답(주문 번호, `CONFIRMED`), 저장된 주문(C1의 `sub`, 두 항목, `CONFIRMED`), WireMock이 받은 예약 요청(1번, 같은 주문 번호, A 3개와 B 2개)을 검사한다. 변형(상품 하나만 담은 주문)은 메서드를 따로 둔다. `@DisplayName`은 시나리오 2.1의 테스트 케이스 ID로 시작한다. 기대값은 `docs/test-cases/order-placement.md`에서 그대로 가져온다 (services/order-service/src/test/java/com/example/msa/order/integration/PlaceOrderConfirmedIntegrationTest.java)
  - 응답 코드 201은 테스트 케이스 문서의 공통 전제에 있다(PR #17). 헌법 "실제 조건에서 테스트 먼저" 원칙대로 그 문서에서 가져와 단언한다.
- [ ] T100 [P] [US2] 소비자 계약 테스트에 RESERVED 스텁을 검사하는 메서드를 쓴다. 시나리오 2.1의 주문을 한 번 보내고, T094의 도우미로 스텁 응답과 받은 예약 요청을 검증한다. ID를 붙이지 않는다 (services/order-service/src/test/java/com/example/msa/order/contract/InventoryConsumerContractTest.java)
  - 근거: plan.md 표의 시나리오 2.1 "계약" 칸 "소비자: 요청과 스텁 응답".
- [ ] T101 [P] [US4] 주문의 주인이 토큰의 고객인지 보는 서비스 통합 테스트를 쓴다. C1의 토큰으로 올바른 주문을 보내면서, 요청 본문에 계약에 없는 필드 `customerId`와 헤더 `X-Customer-Id`를 넣고 두 값을 C2의 `sub`로 채운다 `[제안]`. 저장된 주문의 고객이 C1의 `sub`인지 검사한다. 인수 시나리오 테스트가 아니므로 테스트 케이스 ID를 붙이지 않는다 (services/order-service/src/test/java/com/example/msa/order/integration/CustomerIdentityIntegrationTest.java `[제안]`)
  - 기대값은 유스케이스 6절 업무 규칙 "주문은 로그인한 고객 본인의 이름으로만 만든다"(`docs/requirements/uc-001-place-order.md`)다. research.md 결정 2(사용자 결정)의 표가 FR-014의 기대값을 이 규칙에서 가져온다고 적었다(2026-10-06 사용자 승인).
  - 이 동작은 User Story 2의 컨트롤러(T110)가 만든다. 그래서 테스트를 User Story 4 절이 아니라 이 절에 두어 구현보다 먼저 쓴다.
- [ ] T102 [P] [US2] 시나리오 2.2(㉩ 주문 서비스는 재고 서비스 호출에 추적 정보를 넘긴다)의 서비스 통합 테스트를 쓴다. 기대값은 `docs/test-cases/order-placement.md`에서 그대로 가져온다. WireMock이 받은 예약 요청에 W3C `traceparent` 헤더가 있는지 검사한다. `@DisplayName`은 시나리오 2.2의 테스트 케이스 ID로 시작한다 (services/order-service/src/test/java/com/example/msa/order/integration/TracePropagationIntegrationTest.java `[제안]`)
  - 테스트 설정은 OTLP 내보내기만 끄고 추적 자체는 끄지 않는다(T089). 추적을 끄면 헤더도 붙지 않는다.
  - 근거: spec FR-038, 헌법 "관측 가능성" 원칙의 "서비스 사이 호출에 추적 정보를 넘긴다". 0-A(PR #17)에서 테스트 케이스로 더했다(2026-10-07 사용자 결정).
- [ ] T103 [US2] 위 테스트가 실패하는 것을 확인한다 (services/order-service/src/test/java/com/example/msa/order/integration/PlaceOrderConfirmedIntegrationTest.java)
  - 명령: `.\gradlew.bat :services:order-service:test --tests "com.example.msa.order.integration.PlaceOrderConfirmedIntegrationTest" --tests "com.example.msa.order.integration.CustomerIdentityIntegrationTest" --tests "com.example.msa.order.integration.TracePropagationIntegrationTest" --tests "com.example.msa.order.contract.*"`
  - 기대 결과: 종료 코드가 0이 아니고, 실패 이유가 주문 API가 없어서 생긴 단언 실패(예: 주문 API가 없어 404)다. 하나라도 통과하면 멈추고 사용자에게 묻는다.

**이 절의 시나리오 원문**

> **TC-001** (spec 시나리오 2.1) · `docs/test-cases/order-placement.md` 절 "TC-001 재고가 충분하면 여러 상품 주문이 확정된다"
> - **Given** 상품 A의 재고가 10개, 상품 B의 재고가 5개다
> - **When** 고객이 상품 A 3개와 상품 B 2개를 한 주문으로 요청한다
> - **Then** 고객은 주문 번호와 "확정" 결과를 받는다
> - **And** 그 주문이 C1의 주문으로 두 항목과 함께 "확정" 상태로 기록되어 있다
> - **And** 상품 A의 재고는 7개, 상품 B의 재고는 3개다
> - 변형: 상품 하나만 담은 주문도 같은 방식으로 확정된다.

> **TC-024** (spec 시나리오 2.2) · `docs/test-cases/order-placement.md` 절 "TC-024 주문 서비스는 재고 서비스 호출에 추적 정보를 넘긴다"
> - **Given** 재고 서비스가 예약 요청에 RESERVED를 돌려준다
> - **When** 고객이 상품 A 1개를 주문한다
> - **Then** 재고 서비스가 받은 예약 요청에 W3C `traceparent` 헤더가 있다

#### 구현

- [ ] T104 [US2] 주문 Mapper 테스트를 쓴다. plan.md 표의 시나리오 2.1 "Mapper" 칸 "주문·항목 저장, `<collection>` 조회"다. 시퀀스로 주문 번호를 받아 주문과 두 항목(A 3개, B 2개)을 넣고, `OrderWithItems`로 한 번에 읽는지, 상태를 `CONFIRMED`로 바꾸는지 검사한다 (services/order-service/src/test/java/com/example/msa/order/mapper/OrderMapperTest.java)
- [ ] T105 [P] [US2] DTO를 만든다. `PlaceOrderRequest`, `OrderItemRequest`, `OrderResponse`(`orderNo`, `status`, `reason`, `shortageProductIds`, `missingProductIds`)는 record이고, `OrderWithItems`는 `<collection>`으로 읽는 1:N 부모라서 Lombok 없는 일반 클래스다 (services/order-service/src/main/java/com/example/msa/order/dto/PlaceOrderRequest.java, services/order-service/src/main/java/com/example/msa/order/dto/OrderItemRequest.java, services/order-service/src/main/java/com/example/msa/order/dto/OrderResponse.java, services/order-service/src/main/java/com/example/msa/order/dto/OrderWithItems.java)
  - 근거: data-model.md 7절, `contracts/order-api.yaml`. 응답에 `releaseStatus`를 넣지 않는다(research.md 결정 13의 `[제안]` 세부).
- [ ] T106 [US2] 주문 Mapper와 SQL을 만든다. 주문 번호는 `<selectKey order="BEFORE">`로 `ORDERS_SEQ`에서 받는다. 주문 삽입, 항목 삽입, 주문과 항목을 한 번에 읽는 `resultMap`(`<collection>`), 결과 저장(상태, 사유, `UPDATED_AT`)을 둔다 (services/order-service/src/main/java/com/example/msa/order/mapper/OrderMapper.java, services/order-service/src/main/resources/mapper/OrderMapper.xml)
  - 근거: `docs/standards/coding-conventions.md` 3-2절.
- [ ] T107 [US2] 재고 예약 호출을 만든다. `InventoryClient.reserve(orderNo, items)`가 `PUT /reservations/{orderNo}`를 부르고 200 응답을 `ReservationResult`로 바꾼다. `RestClient`는 Spring Boot가 주는 `RestClient.Builder`로 만들고 `[제안]`, 요청 팩토리는 `JdkClientHttpRequestFactory`에 연결·요청 제한 시간을 `InventoryClientProperties`에서 넣는다 `[제안]` (services/order-service/src/main/java/com/example/msa/order/client/InventoryClient.java, services/order-service/src/main/java/com/example/msa/order/client/ReservationResult.java, services/order-service/src/main/java/com/example/msa/order/config/InventoryClientConfig.java)
  - 근거: research.md 결정 6과 그 `[제안]` 세부. `RestClient.Builder`로 만들어야 추적 정보(`traceparent`)가 붙는다. `RestClient.builder()`를 직접 부르지 않는다.
- [ ] T108 [US2] 트랜잭션 서비스를 만든다. `savePending()`(트랜잭션 1)이 `PENDING` 주문과 항목을 넣고, `applyResult()`(트랜잭션 2)가 RESERVED면 `CONFIRMED`로 바꾼다. 상태를 바꿀 때 `OrderStatus.canMoveTo(...)`로 확인한다 (services/order-service/src/main/java/com/example/msa/order/service/OrderTxService.java)
  - 근거: data-model.md 6-1절 2번·4번, `docs/standards/coding-conventions.md` 3-1절.
- [ ] T109 [US2] 주문 흐름 서비스를 만든다. `place()`에는 `@Transactional`을 붙이지 않는다. `savePending()` → `InventoryClient.reserve()`(트랜잭션 밖) → `applyResult()` 순서다 (services/order-service/src/main/java/com/example/msa/order/service/OrderService.java)
  - 근거: data-model.md 6-1절, `docs/standards/architecture-rules.md` 2절 "서비스 사이 호출"의 트랜잭션 밖 원격 호출 규칙.
- [ ] T110 [US2] 주문 컨트롤러를 만든다. `POST /orders`가 JWT의 `sub`, `Idempotency-Key` 헤더, 본문을 받아 `OrderService.place()`를 부르고 201과 `OrderResponse`를 돌려준다. 고객 ID는 `sub`에서만 얻고 본문이나 다른 헤더의 사용자 정보를 쓰지 않는다 (services/order-service/src/main/java/com/example/msa/order/controller/OrderController.java)
  - 근거: `contracts/order-api.yaml`, research.md 결정 13, `docs/standards/architecture-rules.md` 5절 "보안"의 JWT 규칙.
- [ ] T111 [US2] 이 절의 테스트가 모두 통과하는지 확인한다 (services/order-service/src/test/java/com/example/msa/order/integration/PlaceOrderConfirmedIntegrationTest.java)
  - 명령: `.\gradlew.bat :services:order-service:test`
  - 기대 결과: 종료 코드 0. 통과·실패 수를 보고에 적는다.
- [ ] T112 [US2] research.md 6절 "WireMock 3.13.2의 `WireMockExtension`이 JUnit Jupiter 6.0.3에서 도는지"를 확인한다. T111에서 T099가 `WireMockExtension`으로 돌았는지 테스트 로그에서 보고 결과를 보고에 적는다. 돌지 않으면 멈추고 오류를 보고한다. 다른 방식(예: `WireMockServer`를 직접 띄우기)으로 바꾸는 일은 research.md 4절의 `[제안]`을 바꾸는 일이라 사용자에게 묻는다 (services/order-service/src/test/java/com/example/msa/order/integration/InventoryWireMock.java)

이 목록에서는 항목 ID를 쓰지 않았다.

**체크포인트**: 재고가 충분한 주문이 "확정"으로 끝난다.

### User Story 3 — 고객은 항목 하나라도 재고가 부족하거나 없는 상품이면 거절 결과를 받고, 어느 상품의 재고도 줄지 않는다 (Priority: P2)

**목표**: 예약 결과가 REJECTED면 주문을 "거절"로 바꾸고 사유와 부족한 상품 목록, 없는 상품 목록을 기록하고 돌려준다. 재고 부족과 상품 없음에는 다시 요청하지 않는다.

**독립 테스트** (spec User Story 3): WireMock이 REJECTED와 부족한 상품 목록(또는 없는 상품 목록)을 돌려줄 때, 고객이 받는 응답과 저장된 주문, 예약 요청 횟수를 확인한다.

#### 인수 시나리오 테스트 (구현보다 먼저)

- [ ] T113 [P] [US3] 시나리오 3.1의 주문 쪽 서비스 통합 테스트를 쓴다. WireMock이 REJECTED(`OUT_OF_STOCK`, 부족한 상품 B)를 돌려줄 때 C1이 A 3개와 B 2개를 주문한다. 응답, 저장된 주문(`REJECTED`), 받은 예약 요청 내용을 검사한다. "C1이 조회할 수 있다"는 티켓 002에서 검증하므로 넣지 않는다(spec 시나리오 3.1). `@DisplayName`은 시나리오 3.1의 테스트 케이스 ID로 시작한다. 기대값은 `docs/test-cases/order-placement.md`에서 그대로 가져온다 (services/order-service/src/test/java/com/example/msa/order/integration/PlaceOrderRejectedIntegrationTest.java)
- [ ] T114 [P] [US3] 시나리오 3.2의 서비스 통합 테스트를 쓴다. WireMock이 REJECTED(`OUT_OF_STOCK`)를 돌려줄 때 C1이 A 3개를 주문하고, 예약 요청이 정확히 1번 갔는지 검사한다. P3에서 재시도가 들어와도 이 테스트를 고치지 않고 통과해야 한다(spec 시나리오 3.2 아래 문단). 이 테스트는 P2에서 실패할 수 없다. 재시도가 없어 요청은 언제나 1번이기 때문이다. 실패 확인은 Phase 3의 T167에서 한다(이 문서 머리의 "테스트는 필수다"). `@DisplayName`은 시나리오 3.2의 테스트 케이스 ID로 시작한다. 기대값은 `docs/test-cases/order-placement.md`에서 그대로 가져온다 (services/order-service/src/test/java/com/example/msa/order/integration/NoRetryOnOutOfStockIntegrationTest.java)
- [ ] T115 [P] [US3] 시나리오 3.3의 주문 쪽 서비스 통합 테스트를 쓴다. 본 흐름(없는 상품 Z)과 변형(없는 상품 Z와 부족한 상품 B)을 메서드 두 개로 나눈다. 응답의 사유와 두 목록, 저장된 주문, 예약 요청이 정확히 1번 갔는지 검사한다. `@DisplayName`은 시나리오 3.3의 테스트 케이스 ID로 시작한다. 기대값은 `docs/test-cases/order-placement.md`에서 그대로 가져온다 (services/order-service/src/test/java/com/example/msa/order/integration/PlaceOrderProductNotFoundIntegrationTest.java)
- [ ] T116 [US3] 소비자 계약 테스트에 REJECTED 스텁 두 가지(`OUT_OF_STOCK`, `PRODUCT_NOT_FOUND`)를 검사하는 메서드를 더한다. ID를 붙이지 않는다 (services/order-service/src/test/java/com/example/msa/order/contract/InventoryConsumerContractTest.java)
  - 근거: plan.md 표의 시나리오 3.1, 3.3 "계약" 칸 "소비자".
- [ ] T117 [US3] 위 테스트가 실패하는 것을 확인한다 (services/order-service/src/test/java/com/example/msa/order/integration/PlaceOrderRejectedIntegrationTest.java)
  - 명령: `.\gradlew.bat :services:order-service:test --tests "com.example.msa.order.integration.*" --tests "com.example.msa.order.contract.*"`
  - 기대 결과: 종료 코드가 0이 아니다. T113, T115, T116이 실패한다(거절 처리가 없어 상태나 목록이 다르다). 이 가운데 하나라도 통과하면 멈추고 사용자에게 묻는다. T114는 이 단계에서 통과하는 것이 맞다(T114의 설명). T114의 실패 확인은 Phase 3의 T167에서 한다.

**이 절의 시나리오 원문**

> **TC-002** (spec 시나리오 3.1) · `docs/test-cases/order-placement.md` 절 "TC-002 항목 하나라도 재고가 부족하면 주문 전체가 거절되고 기록된다"
> - **Given** 상품 A의 재고가 10개, 상품 B의 재고가 1개다
> - **When** 고객이 상품 A 3개와 상품 B 2개를 한 주문으로 요청한다
> - **Then** 고객은 주문 번호와 "거절", 부족한 상품 B를 받는다
> - **And** 그 주문이 "거절" 상태로 기록되어 있고 C1이 조회할 수 있다
> - **And** 상품 A의 재고는 10개, 상품 B의 재고는 1개 그대로다 (A도 줄지 않는다)

> **TC-009** (spec 시나리오 3.2) · `docs/test-cases/order-placement.md` 절 "TC-009 재고 부족 응답에는 재시도하지 않는다"
> - **Given** 상품 A의 재고가 2개다
> - **When** 고객이 상품 A를 3개 주문한다
> - **Then** 예약 요청은 정확히 1번 갔다

> **TC-015** (spec 시나리오 3.3) · `docs/test-cases/order-placement.md` 절 "TC-015 없는 상품이 섞이면 주문 전체가 거절되고 기록된다"
> - **Given** 상품 A의 재고가 10개다. 상품 Z는 재고 데이터에 없다
> - **When** 고객이 상품 A 3개와 상품 Z 1개를 한 주문으로 요청한다
> - **Then** 고객은 주문 번호와 "거절", 사유 "상품 없음", 없는 상품 Z를 받는다
> - **And** 그 주문이 "거절" 상태로 기록되어 있다
> - **And** 상품 A의 재고는 10개 그대로다
> - **And** 예약 요청은 정확히 1번 갔다 (재시도하지 않는다)
> - 변형: 상품 B의 재고가 1개일 때 상품 A 3개, 상품 Z 1개, 상품 B 2개를 한 주문으로 요청하면, 사유는 "상품 없음"이고 없는 상품 Z와 부족한 상품 B를 모두 받는다. 상품 A와 B의 재고는 그대로다.

#### 구현

- [ ] T118 [US3] 주문 Mapper 테스트에 "항목별 거절 사유 저장"을 검사하는 메서드를 더한다. 시나리오 3.3 변형의 값으로 Z는 `PRODUCT_NOT_FOUND`, B는 `OUT_OF_STOCK`, A는 빈 값으로 저장되는지 본다 (services/order-service/src/test/java/com/example/msa/order/mapper/OrderMapperTest.java)
  - 근거: plan.md 표의 시나리오 3.1 "Mapper" 칸.
- [ ] T119 [US3] 거절 처리를 만든다. `InventoryClient`가 REJECTED 응답의 사유와 두 목록을 읽는다. `applyResult()`가 `REJECTED`, 사유, 항목별 `REJECT_REASON`을 저장한다. `OrderResponse`의 `shortageProductIds`와 `missingProductIds`는 `REJECT_REASON`에서 상품 ID 순으로 만든다 (services/order-service/src/main/java/com/example/msa/order/service/OrderTxService.java, services/order-service/src/main/resources/mapper/OrderMapper.xml)
  - 근거: data-model.md 6-1절 4번, 2-3절 마지막 문단("그래서 같은 키로 다시 보내도 처음과 같은 목록이 나온다").
  - REJECTED에는 다시 요청하지 않는다(FR-026).
- [ ] T120 [US3] 이 절의 테스트와 앞 절의 테스트가 모두 통과하는지 확인한다 (services/order-service/src/test/java/com/example/msa/order/integration/PlaceOrderRejectedIntegrationTest.java)
  - 명령: `.\gradlew.bat :services:order-service:test`
  - 기대 결과: 종료 코드 0.

이 목록에서는 항목 ID를 쓰지 않았다.

**체크포인트**: 거절된 주문이 사유, 목록과 함께 기록되고, 예약 요청은 한 번만 간다.

### User Story 4 — 로그인하지 않은 사람은 주문할 수 없다 (Priority: P2)

**목표**: 주문 서비스가 JWT를 직접 검증하고, 고객을 확인할 수 없는 요청은 주문을 기록하지 않고 401로 거부한다.

**독립 테스트** (spec User Story 4): 주문 서비스에 토큰 없이 주문을 보낸다. 401을 받는지, 주문이 저장되지 않았는지 확인한다.

#### 인수 시나리오 테스트 (구현보다 먼저)

- [ ] T121 [US4] 시나리오 4.1의 서비스 통합 테스트를 쓴다. spec 시나리오 4.1이 "이 스토리에서 검증하는 줄"로 고른 첫 When/Then만 검증한다. 토큰 없이 올바른 주문을 보내고 401, 주문 기록이 없음, WireMock이 요청을 받지 않았음을 검사한다. 조회 부분은 티켓 002에서 검증한다. `@DisplayName`은 시나리오 4.1의 테스트 케이스 ID로 시작한다. 기대값은 `docs/test-cases/order-placement.md`에서 그대로 가져온다 (services/order-service/src/test/java/com/example/msa/order/integration/UnauthenticatedOrderIntegrationTest.java)
  - 응답 본문이 Problem Details이고 `code`가 `UNAUTHORIZED`인지도 단언한다. 이 기대값은 테스트 케이스 문서의 공통 전제에 있다(PR #17).
- [ ] T122 [P] [US4] 시나리오 4.2(㉦ 서명이 틀리거나 만료된 토큰으로는 주문할 수 없다)의 서비스 통합 테스트를 쓴다. 기대값은 `docs/test-cases/order-placement.md`에서 그대로 가져온다. `@DisplayName`은 시나리오 4.2의 테스트 케이스 ID로 시작한다 (services/order-service/src/test/java/com/example/msa/order/integration/InvalidTokenIntegrationTest.java `[제안]`)
  - Spring Security Test의 `jwt()`는 토큰 검증을 건너뛰므로 이 테스트에는 쓰지 않는다. 테스트용 RSA 키 쌍으로 서명이 틀린 토큰과 만료된 토큰을 만들고, 이 테스트에서만 주문 서비스가 테스트용 공개 키로 토큰을 검증하게 설정한다 `[제안]`.
  - Spring Boot의 리소스 서버 자동 설정(T085의 의존성, T087의 설정)이 보안 설정(T124) 전에도 401을 돌려줄 수 있다. T123에서 이 테스트가 통과하면 멈추고 사용자에게 묻는다(이 문서 머리의 "테스트는 필수다").
  - 근거: spec Edge Cases의 받아들인 제안 9번. 0-A에서 테스트 케이스로 더한다(2026-10-06 사용자 결정).
- [ ] T123 [US4] 위 테스트가 실패하는 것을 확인한다 (services/order-service/src/test/java/com/example/msa/order/integration/UnauthenticatedOrderIntegrationTest.java)
  - 명령: `.\gradlew.bat :services:order-service:test --tests "com.example.msa.order.integration.UnauthenticatedOrderIntegrationTest" --tests "com.example.msa.order.integration.InvalidTokenIntegrationTest"`
  - 기대 결과: 종료 코드가 0이 아니다. Spring Boot 기본 보안 설정이 401을 돌려줄 수 있으므로, 실패 이유는 응답 본문이 Problem Details가 아니라는 단언 실패여야 한다. 테스트가 통과하면 다음 작업으로 넘어가지 않고 멈춰 사용자에게 묻는다.

**이 절의 시나리오 원문**

> **TC-010** (spec 시나리오 4.1) · `docs/test-cases/order-placement.md` 절 "TC-010 로그인한 사람만 주문하고, 자기 주문만 본다"
> - **When** 토큰 없이 주문한다
> - **Then** 401을 받고 주문은 기록되지 않는다
> - **Given** C1의 주문 1004가 있다
> - **When** C2가 주문 1004를 조회한다
> - **Then** "없음"(404)을 받는다
> - **When** C1이 자기 주문 목록을 조회한다
> - **Then** C1의 주문만 나온다
> - **When** C1이 관리자 조회를 요청한다
> - **Then** 403을 받는다
> - **Given** C1과 C2에게 각각 "실패" 주문이 있다
> - **When** M1이 상태 "실패"로 관리자 조회를 한다
> - **Then** C1과 C2의 실패 주문이 모두 재고 해제 결과와 함께 나온다

> **TC-021** (spec 시나리오 4.2) · `docs/test-cases/order-placement.md` 절 "TC-021 서명이 틀리거나 만료된 토큰으로는 주문할 수 없다"
> - **When** 서명이 틀린 토큰으로 주문한다
> - **Then** 401을 받고 주문은 기록되지 않는다
> - **When** 만료된 토큰으로 주문한다
> - **Then** 401을 받고 주문은 기록되지 않는다

#### 구현

- [ ] T124 [US4] 보안 설정을 만든다. OAuth2 리소스 서버로 JWT의 서명, 만료, 발급자를 검증하고 `POST /orders`는 인증된 사용자만 부른다. health 엔드포인트는 인증 없이 연다. 인증 실패는 `AuthenticationEntryPoint`가 401 Problem Details(`code` = `UNAUTHORIZED`)로 낸다. 역할과 관계없이 로그인한 사용자는 주문할 수 있다(spec Edge Cases, research.md 결정 1로 받아들인 제안 10번). 세션을 만들지 않는다 (services/order-service/src/main/java/com/example/msa/order/config/SecurityConfig.java, services/order-service/src/main/java/com/example/msa/order/exception/ProblemAuthenticationEntryPoint.java `[제안]`)
  - 근거: data-model.md 5-1절 첫 줄, research.md 결정 13의 `[제안]` 세부(401도 Problem Details), `docs/standards/architecture-rules.md` 5절 "보안"의 JWT 규칙, 3절 "실행과 배포"의 남아야 하는 데이터 규칙(세션).
  - 토큰 전체를 로그에 남기지 않는다(헌법 "관측 가능성" 원칙).
- [ ] T125 [US4] 이 스토리의 테스트와 앞 스토리의 테스트가 모두 통과하는지 확인한다 (services/order-service/src/test/java/com/example/msa/order/integration/UnauthenticatedOrderIntegrationTest.java)
  - 명령: `.\gradlew.bat :services:order-service:test`
  - 기대 결과: 종료 코드 0.

이 목록에서는 항목 ID를 쓰지 않았다.

### User Story 5 — 잘못된 주문 항목은 기록되지 않고 오류를 받는다 (Priority: P2)

**목표**: 주문 항목 규칙을 어긴 주문은 기록하지 않고 400을 돌려주며, 재고 서비스에도 요청하지 않는다.

**독립 테스트** (spec User Story 5): 규칙을 어긴 주문 다섯 가지를 보내 모두 400을 받는지, 주문이 저장되지 않았는지, WireMock에 요청이 가지 않았는지 확인한다.

#### 인수 시나리오 테스트 (구현보다 먼저)

- [ ] T126 [P] [US5] 시나리오 5.1의 서비스 통합 테스트를 쓴다. 다섯 경우를 매개변수 테스트 하나로 돌린다 `[제안]`. 경우마다 400, 주문 기록 없음, WireMock이 요청을 받지 않았음을 검사한다. `@DisplayName`은 시나리오 5.1의 테스트 케이스 ID로 시작한다. 기대값은 `docs/test-cases/order-placement.md`에서 그대로 가져온다 (services/order-service/src/test/java/com/example/msa/order/integration/InvalidOrderItemsIntegrationTest.java)
- [ ] T127 [P] [US5] 주문 항목 검사의 단위 테스트를 쓴다. plan.md 표의 시나리오 5.1 "단위" 칸 "주문 항목 검사 다섯 경우"다. 테스트 케이스의 다섯 경우가 모두 검사 실패이고, 올바른 항목(A 3개, B 2개)은 통과하는지 검사한다. 이 테스트는 아직 없는 `OrderItems`를 참조하므로 컴파일 오류로 실패한다 (services/order-service/src/test/java/com/example/msa/order/domain/OrderItemsTest.java)
- [ ] T128 [P] [US5] 시나리오 5.2(㉧ 형식이 틀린 주문 항목은 기록되지 않는다)의 서비스 통합 테스트를 쓴다. 기대값은 `docs/test-cases/order-placement.md`에서 그대로 가져온다. 경우마다 400, 주문 기록 없음, WireMock이 요청을 받지 않았음을 검사한다. `@DisplayName`은 시나리오 5.2의 테스트 케이스 ID로 시작한다 (services/order-service/src/test/java/com/example/msa/order/integration/MalformedOrderItemsIntegrationTest.java `[제안]`)
  - 근거: spec Edge Cases의 받아들인 제안 11번. 0-A에서 테스트 케이스로 더한다(2026-10-06 사용자 결정).
- [ ] T129 [US5] 위 테스트가 실패하는 것을 확인한다 (services/order-service/src/test/java/com/example/msa/order/integration/InvalidOrderItemsIntegrationTest.java)
  - T126과 T128을 먼저 확인한다. 두 테스트 모두 항목 검사가 없어 주문이 기록되므로 실패해야 한다. T127을 아직 쓰지 않은 상태에서 `.\gradlew.bat :services:order-service:test --tests "com.example.msa.order.integration.InvalidOrderItemsIntegrationTest"`를 돌리면 종료 코드가 0이 아니고, 검사가 없어 201이 나오거나 주문이 기록된 단언 실패가 보인다. 그다음 T127을 쓰고 `compileTestJava`가 실패하는 것을 본다.

**이 절의 시나리오 원문**

> **TC-014** (spec 시나리오 5.1) · `docs/test-cases/order-placement.md` 절 "TC-014 잘못된 주문 항목은 기록되지 않는다"
> - **When** 항목이 하나도 없는 주문, 수량이 0인 항목이 있는 주문, 같은 상품이 두 줄에 나오는 주문, 항목이 21개인 주문, 수량이 100인 항목이 있는 주문을 각각 요청한다
> - **Then** 다섯 경우 모두 400을 받는다
> - **And** 주문은 기록되지 않고 재고 서비스에 요청이 가지 않는다

> **TC-022** (spec 시나리오 5.2) · `docs/test-cases/order-placement.md` 절 "TC-022 형식이 틀린 주문 항목은 기록되지 않는다"
> - **When** 상품 ID가 빈 항목이 있는 주문, 수량이 정수가 아닌(1.5) 항목이 있는 주문을 각각 요청한다
> - **Then** 두 경우 모두 400을 받는다
> - **And** 주문은 기록되지 않고 재고 서비스에 요청이 가지 않는다

#### 구현

- [ ] T130 [US5] 주문 항목 검사를 만든다. `OrderItems.validate(...)`가 아래 규칙을 검사한다. 컨트롤러는 Bean Validation으로 형식(상품 ID 형식, 수량이 정수)을 검사한다. 실패하면 주문을 기록하지 않고 400 Problem Details(`code` = `INVALID_ORDER_ITEMS`, 필드별 `errors`)를 돌려준다 (services/order-service/src/main/java/com/example/msa/order/domain/OrderItems.java, services/order-service/src/main/java/com/example/msa/order/controller/OrderController.java, services/order-service/src/main/java/com/example/msa/order/exception/ProblemDetailsHandler.java)
  - 검사 규칙은 data-model.md 5-1절 원문 그대로다:

    | 검사 | 실패하면 | 어디서 |
    |---|---|---|
    | 주문 항목이 1개 이상 20개 이하, 수량 1~99, 같은 상품은 한 줄 | 400 | `domain`의 `OrderItems.validate(...)` |
    | 상품 ID가 `^[A-Z0-9-]{1,20}$`이고 수량이 정수다 | 400 | controller의 Bean Validation과 `domain` |

  - `OrderItems`는 Spring과 MyBatis를 모르는 순수 Java다(`docs/standards/coding-conventions.md` 2절).
  - `code` 값은 `contracts/order-api.yaml`의 400 설명에서 가져온다.
- [ ] T131 [US5] 이 스토리의 테스트와 앞 스토리의 테스트가 모두 통과하는지 확인한다 (services/order-service/src/test/java/com/example/msa/order/domain/OrderItemsTest.java)
  - 명령: `.\gradlew.bat :services:order-service:test`
  - 기대 결과: 종료 코드 0.

이 목록에서는 항목 ID를 쓰지 않았다.

### User Story 6 — 고객이 같은 주문을 실수로 두 번 보내도 주문은 하나다 (Priority: P2)

**목표**: 같은 고객이 같은 주문 요청 키로 다시 보내면 새 주문을 만들지 않고 이미 있는 주문의 지금 상태를 돌려준다. 내용이 다르면 422, 키가 없거나 UUID가 아니면 400이다.

**독립 테스트** (spec User Story 6): 같은 키와 같은 내용으로 두 번 주문해 두 응답의 주문 번호가 같은지 확인한다. 키를 바꾸거나, 내용을 바꾸거나, 고객을 바꾸거나, 키를 빼거나, 형식이 틀린 키를 보내 각각의 결과를 확인한다.

#### 인수 시나리오 테스트 (구현보다 먼저)

- [ ] T132 [P] [US6] 시나리오 6.1의 주문 쪽 서비스 통합 테스트를 쓴다. 테스트 케이스의 When 일곱 개를 모두 검증한다(plan.md 표). K1·K2·K3은 테스트에서 만든 UUID다. 재고 수량 기대값(7개, 4개)은 주문 쪽에서는 WireMock이 받은 예약 요청의 횟수와 주문 번호로 본다. 첫 요청이 처리 중인 경우(K3)는 WireMock 예약 스텁에 지연을 넣고 둘째 요청을 다른 스레드에서 보낸다. `@DisplayName`은 시나리오 6.1의 테스트 케이스 ID로 시작한다. 기대값은 `docs/test-cases/order-placement.md`에서 그대로 가져온다 (services/order-service/src/test/java/com/example/msa/order/integration/IdempotencyKeyIntegrationTest.java)
  - 응답 코드(같은 키의 재요청 200, 새 주문 201)도 단언한다. 이 기대값은 테스트 케이스 문서의 공통 전제에 있다(PR #17).
  - K3 스텁의 지연은 시도당 제한 시간(테스트 설정 500ms)보다 짧게 둔다 `[제안]`. 길면 첫 요청이 시간 초과로 "실패"가 되어, 이 테스트가 보려는 "처리중"과 관계없는 동작이 섞인다.
- [ ] T133 [P] [US6] 시나리오 6.2(㉨ 거부된 요청의 키는 남지 않고, 같은 키의 내용 비교는 항목 순서를 보지 않는다)의 서비스 통합 테스트를 쓴다. 기대값은 `docs/test-cases/order-placement.md`에서 그대로 가져온다. 두 경우를 메서드 두 개로 나눈다 `[제안]`. `@DisplayName`은 시나리오 6.2의 테스트 케이스 ID로 시작한다 (services/order-service/src/test/java/com/example/msa/order/integration/IdempotencyKeyReuseIntegrationTest.java `[제안]`)
  - 첫째 경우(400으로 거부된 요청의 키를 다시 쓴다)는 User Story 5의 항목 검사(T130)가 주문을 저장하기 전에 거부하므로, 이 절에 오기 전에 이미 통과할 수 있다. T134에서 통과하면 멈추고 사용자에게 묻는다.
  - 근거: spec Edge Cases의 받아들인 제안 7번과 8번. 0-A에서 테스트 케이스로 더한다(2026-10-06 사용자 결정).
- [ ] T134 [US6] 위 테스트가 실패하는 것을 확인한다 (services/order-service/src/test/java/com/example/msa/order/integration/IdempotencyKeyIntegrationTest.java)
  - 명령: `.\gradlew.bat :services:order-service:test --tests "com.example.msa.order.integration.IdempotencyKeyIntegrationTest" --tests "com.example.msa.order.integration.IdempotencyKeyReuseIntegrationTest"`
  - 기대 결과: 종료 코드가 0이 아니다. 같은 키의 둘째 요청이 고유 제약 위반으로 500이 되거나, 키 형식 검사가 없어 단언이 실패한다. 통과하면 멈추고 사용자에게 묻는다.

**이 절의 시나리오 원문**

> **TC-011** (spec 시나리오 6.1) · `docs/test-cases/order-placement.md` 절 "TC-011 같은 주문 요청 키로 다시 보내면 주문은 하나다"
> - **Given** 상품 A의 재고가 10개다
> - **When** 고객이 요청 키 K1로 상품 A 3개를 주문하고, 같은 키 K1로 같은 요청을 한 번 더 보낸다
> - **Then** 두 응답의 주문 번호가 같다
> - **And** 상품 A의 재고는 7개다
> - **When** 고객이 새 요청 키 K2로 같은 내용을 주문한다
> - **Then** 새 주문 번호를 받고 상품 A의 재고는 4개다
> - **When** 고객이 K1으로 상품 A 5개를 주문한다
> - **Then** 422(요청 키 충돌)를 받는다
> - **When** C2가 K1으로 상품 A 1개를 주문한다
> - **Then** 새 주문이 만들어진다 (키는 고객마다 따로 본다)
> - **When** 요청 키 없이 주문한다
> - **Then** 400을 받고 주문은 기록되지 않는다
> - **When** UUID 형식이 아닌 요청 키로 주문한다
> - **Then** 400을 받고 주문은 기록되지 않는다
> - **When** 고객이 요청 키 K3로 상품 A 1개를 주문하고, 재고 서비스의 응답이 늦어 그 요청이 아직 처리 중일 때 같은 키 K3로 같은 요청을 한 번 더 보낸다
> - **Then** 둘째 응답은 첫 요청과 같은 주문 번호와 "처리중"을 받는다
> - **And** 주문은 하나만 기록된다

> **TC-023** (spec 시나리오 6.2) · `docs/test-cases/order-placement.md` 절 "TC-023 거부된 요청의 키는 남지 않고, 같은 키의 내용 비교는 항목 순서를 보지 않는다"
> - **When** 고객이 요청 키 K4로 수량이 0인 항목이 있는 주문을 보내 400을 받고, 같은 키 K4로 상품 A 1개를 주문한다
> - **Then** 주문 번호를 받고 그 주문이 기록된다
> - **When** 고객이 요청 키 K5로 상품 A 1개와 상품 B 1개를 "A, B" 순서로 주문하고, 같은 키 K5로 "B, A" 순서로 같은 요청을 한 번 더 보낸다
> - **Then** 두 응답의 주문 번호가 같다 (422가 아니다)
> - **And** 주문은 하나만 기록된다

#### 구현

- [ ] T135 [US6] 주문 Mapper 테스트에 "고객·키 고유 제약"을 검사하는 메서드를 더한다. 같은 고객과 같은 키로 두 번 넣으면 고유 제약 위반 예외가 나고, 다른 고객의 같은 키는 들어가는지 본다. 고객과 키로 주문과 항목을 한 번에 읽는 조회도 검사한다 (services/order-service/src/test/java/com/example/msa/order/mapper/OrderMapperTest.java)
  - 근거: plan.md 표의 시나리오 6.1 "Mapper" 칸. 기대값의 출처는 도메인 분석 7절("서버는 (고객, 키)가 같으면 새 주문을 만들지 않고 이미 있는 주문을 돌려준다", "같은 고객 안에서만 비교한다")과 설계 6절 표의 `UNIQUE(CUSTOMER_ID, REQUEST_KEY)`다.
- [ ] T136 [US6] 같은 키의 재요청 처리를 만든다 (services/order-service/src/main/java/com/example/msa/order/service/OrderService.java, services/order-service/src/main/java/com/example/msa/order/service/OrderTxService.java, services/order-service/src/main/java/com/example/msa/order/domain/OrderItems.java, services/order-service/src/main/resources/mapper/OrderMapper.xml, services/order-service/src/main/java/com/example/msa/order/controller/OrderController.java)
  - 컨트롤러: `Idempotency-Key` 헤더가 없거나 UUID 문자열(36자)이 아니면 400 Problem Details(`code` = `INVALID_IDEMPOTENCY_KEY`)다(data-model.md 5-1절 둘째 줄, `contracts/order-api.yaml`).
  - `savePending()`이 `UK_ORDERS_CUSTOMER_KEY`에 걸리면 이미 있는 주문을 항목과 함께 읽는다. `OrderItems`로 상품 ID 순 정렬 비교를 해서 같으면 그 주문의 지금 상태로 200, 다르면 422 Problem Details(`code` = `IDEMPOTENCY_KEY_CONFLICT`)다. 재고 서비스는 부르지 않는다.
  - 근거: data-model.md 6-1절 2번("다른 요청이 같은 키로 아직 커밋하지 않았으면 Oracle이 그 커밋까지 기다린 뒤 제약 위반을 낸다"), spec Edge Cases의 받아들인 제안 7번(거부된 요청의 키는 남지 않는다)과 8번(항목 순서는 보지 않는다).
- [ ] T137 [US6] 이 스토리의 테스트와 앞 스토리의 테스트가 모두 통과하는지 확인한다 (services/order-service/src/test/java/com/example/msa/order/integration/IdempotencyKeyIntegrationTest.java)
  - 명령: `.\gradlew.bat :services:order-service:test`
  - 기대 결과: 종료 코드 0.

이 목록에서는 항목 ID를 쓰지 않았다.

### User Story 7 — 재고 서비스가 계속 실패하면 고객은 기다리지 않고 바로 실패 응답을 받는다 (Priority: P2)

**목표**: 재고 호출에 서킷 브레이커 하나를 둔다. 서킷이 열려 있으면 재고를 부르지 않고 바로 "실패"로 기록하고 1초 안에 응답한다. 409·500에는 재시도하지 않고 "실패"로 기록한 뒤 해제를 요청한다. P2에는 재시도가 없으므로 일시 오류도 한 번 시도로 "실패"가 되고 해제를 요청한다(FR-028, research.md 결정 1로 받아들인 제안 3번). 해제도 한 번만 시도한다(받아들인 제안 4번).

**독립 테스트** (spec User Story 7): WireMock이 모든 예약 요청에 503을 돌려주게 하고 주문을 연달아 보내 서킷이 열리는지, 그 뒤 주문이 1초 안에 실패 응답을 받는지 확인한다. WireMock이 409를 돌려주게 해서 재시도하지 않는지, 해제 요청이 가는지도 확인한다.

#### 인수 시나리오 테스트 (구현보다 먼저)

- [ ] T138 [P] [US7] 시나리오 7.1의 서비스 통합 테스트를 쓴다. WireMock이 모든 예약 요청과 해제 요청에 503을 돌려준다. 서킷이 열릴 때까지 주문을 연달아 보내고(서킷이 열린 뒤의 첫 주문을 "WireMock이 받은 예약 요청 수가 늘지 않은 주문"으로 알아본다), 그 뒤 주문의 응답 시간이 1초 안인지, `FAILED`·`RETRY_LATER`·`NOT_REQUIRED`로 기록되고 해제 요청이 가지 않는지, 서킷이 열리기 전 주문이 `FAILED`·`RELEASE_FAILED`로 기록되는지 검사한다. 그다음 WireMock을 정상(RESERVED)으로 바꾸고 열림 시간(테스트 설정 2초)이 지난 뒤 새 주문이 `CONFIRMED`인지 본다. `@DisplayName`은 시나리오 7.1의 테스트 케이스 ID로 시작한다. 기대값은 `docs/test-cases/order-placement.md`에서 그대로 가져온다 (services/order-service/src/test/java/com/example/msa/order/integration/CircuitBreakerIntegrationTest.java)
  - **P3에서 재시도가 들어와도 이 테스트를 고치지 않고 통과해야 한다.** 그래서 테스트 케이스 문장에 있는 것만 단언한다. 주문마다 예약 요청이 몇 번 갔는지, 몇 번째 주문에서 서킷이 열리는지는 단언하지 않는다 `[제안]`. research.md 결정 2가 "P2 단계의 임시 동작"에 전용 테스트를 두지 않고 이 시나리오가 P2와 P3 모두에서 지나가게 한 이유다.
  - "해제 실패"는 해제가 비동기라서 바로 기록되지 않는다. 정해진 시간까지 저장된 주문을 다시 읽는 방식으로 기다린다 `[제안]`. 기다리는 한도는 테스트 케이스에 값이 없어 단언하지 않고, 테스트가 끝없이 돌지 않게 하는 안전장치로만 둔다(10초) `[제안]`.
  - 열리는 조건과 열림 시간은 테스트 케이스가 가리키는 설계 4절의 값이다. 시간 값은 1/5로 줄인 테스트 설정(T089)을 쓴다 `[제안]`. "1초 안에"는 줄이지 않는다 `[제안]`.
- [ ] T139 [P] [US7] 시나리오 7.2의 서비스 통합 테스트를 쓴다. spec 시나리오 7.2가 "이 스토리에서 검증하는 줄"로 고른 줄만 검증한다. WireMock이 예약 요청에 409 Problem Details(`code` = `RESERVATION_CONFLICT`)를 돌려줄 때(spec 받아들인 제안 5번) 예약 요청이 정확히 1번 갔는지, 주문이 `FAILED`로 기록되는지, 고객 응답 뒤 해제 요청(`DELETE /reservations/{orderNo}`)이 가는지 검사한다. `@DisplayName`은 시나리오 7.2의 테스트 케이스 ID로 시작한다. 기대값은 `docs/test-cases/order-placement.md`에서 그대로 가져온다 (services/order-service/src/test/java/com/example/msa/order/integration/ReservationConflictOrderIntegrationTest.java)
- [ ] T140 [P] [US7] 재고 서비스가 500을 돌려줄 때의 서비스 통합 테스트를 쓴다. WireMock이 예약 요청에 500 Problem Details(`code` = `INTERNAL_ERROR`)를 돌려줄 때 예약 요청이 정확히 1번 갔는지, 주문이 `FAILED`로 기록되는지, 고객 응답 뒤 해제 요청(`DELETE /reservations/{orderNo}`)이 가는지 검사한다. 인수 시나리오 테스트가 아니므로 테스트 케이스 ID를 붙이지 않는다 (services/order-service/src/test/java/com/example/msa/order/integration/InventoryServerErrorIntegrationTest.java `[제안]`)
  - 기대값은 테스트 케이스 문서 공통 전제의 일곱째 줄이다: "재고 서비스가 409나 500을 돌려주면 주문 서비스는 재시도하지 않는다. 주문을 "실패"로 기록하고 재고 서비스에 해제를 요청한다." research.md 결정 2(사용자 결정)의 표가 FR-027의 500 쪽 기대값을 이 줄에서 가져온다고 적었다(2026-10-06 사용자 승인).
  - 스텁의 `code` 값은 재고 서비스 계약(`contracts/inventory-api.yaml`)의 500 응답이다. 기대값이 아니라 스텁을 만드는 값이다.
  - P3에서 재시도가 들어와도 이 테스트를 고치지 않고 통과해야 한다. 실패 확인은 이 절의 T143에서 하고, 재시도가 생긴 뒤에는 Phase 3의 T167에서 한 번 더 한다.
- [ ] T141 [US7] 소비자 계약 테스트에 409 스텁과, 시나리오 7.1의 회복 뒤 RESERVED 스텁·해제 스텁을 검사하는 메서드를 더한다. 503 스텁의 응답은 계약에 없는 응답이라 검증하지 않고 받은 요청만 검증한다(T094) (services/order-service/src/test/java/com/example/msa/order/contract/InventoryConsumerContractTest.java)
  - 근거: plan.md 표의 시나리오 7.1 "계약" 칸 "소비자", 시나리오 7.2 "계약" 칸 "소비자: 409 스텁".
- [ ] T142 [P] [US7] 재고 호출 설정의 기본값 단위 테스트를 쓴다. 설정 파일을 덮어쓰지 않은 기본값이 시도당 제한 시간(연결·요청) 2.5초인지, 서킷 브레이커의 숫자 값이 설계 4절 표의 값(최근 10번, 최소 10번 기록, 실패율 50%, 열려 있는 시간 10초, 시험 호출 3번)인지 검사한다 (services/order-service/src/test/java/com/example/msa/order/config/InventoryClientPropertiesTest.java `[제안]`)
  - 근거: `docs/standards/testing.md` 3절 "시간"("기본값이 설정에 맞게 들어갔는지는 단위 테스트로 따로 확인한다"). 2.5초는 비기능 요구사항 문서(`docs/requirements/non-functional.md`)의 "장애 전파 차단" 목표값("재시도 포함 전체 30초, 시도당 2.5초, 재시도 최대 5회(즉시, 1, 2, 4, 8초 뒤). 서킷이 열린 뒤에는 1초 안에 실패 응답")의 "시도당 2.5초"다. 서킷 브레이커 값은 시나리오 7.1의 테스트 케이스가 "서킷의 열림 조건(design 4절)"으로 가리키는 값이다.
  - `slidingWindowType=COUNT_BASED`와 반쯤 열린 상태의 실패율 기준은 단언하지 않는다. 이 두 값은 설계 4절에 없고, research.md 결정 8의 `[제안]` 대응표에서 왔기 때문이다(2026-10-06 사용자 승인). 숫자 값은 사용자가 확정한 설계 문서에 있으므로 받치는 테스트의 기대값으로 쓴다(이 문서 머리의 "받치는 테스트의 기대값 출처", 2026-10-06 사용자 결정).
  - 위치는 `config/` 테스트 패키지다 `[제안]`. plan.md 트리에 더했다(2026-10-07 사용자 결정).
- [ ] T143 [US7] 위 테스트가 실패하는 것을 확인한다 (services/order-service/src/test/java/com/example/msa/order/integration/CircuitBreakerIntegrationTest.java)
  - 명령: `.\gradlew.bat :services:order-service:test --tests "com.example.msa.order.integration.CircuitBreakerIntegrationTest" --tests "com.example.msa.order.integration.ReservationConflictOrderIntegrationTest" --tests "com.example.msa.order.integration.InventoryServerErrorIntegrationTest"`
  - 기대 결과: 종료 코드가 0이 아니다. 503·409·500 처리와 서킷이 없어 500이 나오거나 상태가 다른 단언 실패다. T142는 서킷 값 필드가 없어 컴파일 오류로 실패한다. 하나라도 통과하면 멈추고 사용자에게 묻는다.

**이 절의 시나리오 원문**

> **TC-013** (spec 시나리오 7.1) · `docs/test-cases/order-placement.md` 절 "TC-013 재고 서비스 실패가 이어지면 서킷이 열려 바로 실패한다"
> - **Given** 재고 서비스가 모든 예약 요청에 503을 돌려준다
> - **When** 고객들이 주문을 연달아 요청한다
> - **Then** 실패한 시도가 서킷의 열림 조건(design 4절)에 이르면 서킷이 열린다
> - **And** 그 뒤의 주문은 재고 서비스에 요청을 보내지 않고 1초 안에 "실패"와 "잠시 후 다시 시도"를 받는다
> - **And** 그 주문들은 "해제 불필요"로 기록되고 해제 요청도 가지 않는다
> - **And** 서킷이 열리기 전에 예약 요청을 보낸 주문은 "실패"로 기록되고, 재고 서비스가 계속 503을 돌려주는 동안 해제도 실패해 "해제 실패"로 기록된다
> - **When** 재고 서비스가 정상으로 돌아오고 서킷의 열림 시간이 지난다
> - **Then** 시험 호출이 성공해 서킷이 닫히고, 새 주문이 "확정"된다
> - 비고: 서비스 통합 테스트에서 WireMock으로 검증한다. 열림 시간 같은 값은 테스트에서 짧게 바꾼다.

> **TC-008** (spec 시나리오 7.2) · `docs/test-cases/order-placement.md` 절 "TC-008 같은 주문 번호에 다른 내용이 오면 거부한다"
> - **Given** 주문 번호 1003으로 상품 A 3개 예약이 처리됐다
> - **When** 주문 번호 1003, 상품 A, 5개로 예약을 요청한다
> - **Then** 재고 서비스는 요청 키 충돌(409)을 돌려준다
> - **And** 재고는 변하지 않는다
> - **And** 주문 서비스는 이 응답에 재시도하지 않는다
> - **And** 그 주문은 "실패"로 기록되고, 재고 서비스에 해제 요청이 간다

#### 구현

- [ ] T144 [US7] 서킷 브레이커를 만든다. `InventoryClientProperties`에 서킷 값을 더하고, `InventoryClientConfig`에 Resilience4j `CircuitBreaker` 인스턴스 하나(`inventory`)를 만들어 예약과 해제가 함께 쓰게 한다. 실패로 세는 것은 연결 실패, 시도 제한 시간 초과, 5xx이고, 업무 결과(RESERVED·REJECTED·RELEASED)와 4xx는 성공으로 센다 (services/order-service/src/main/java/com/example/msa/order/config/InventoryClientProperties.java, services/order-service/src/main/java/com/example/msa/order/config/InventoryClientConfig.java)
  - 근거: research.md 결정 8의 표("설계 4절 값과 Resilience4j 설정의 대응", research.md에서 `[제안]`), `docs/standards/architecture-rules.md` 2절 "서비스 사이 호출"의 서킷 브레이커 규칙.
- [ ] T145 [US7] 재고 호출의 실패 처리를 만든다. `InventoryClient`가 409·500·연결 실패·시간 초과·502·503·504·서킷 열림을 `InventoryCallFailure`로 알린다. 호출 문맥 `InventoryCallContext`가 서킷이 허가해 실제로 보낸 시도 수를 센다. 서킷 상태를 미리 따로 묻지 않는다 (services/order-service/src/main/java/com/example/msa/order/client/InventoryClient.java, services/order-service/src/main/java/com/example/msa/order/client/InventoryCallFailure.java, services/order-service/src/main/java/com/example/msa/order/client/InventoryCallContext.java)
  - 근거: data-model.md 6-1절 마지막 문단, research.md 결정 6("상태 코드별 처리도 같은 클래스에 둔다").
  - 재고 호출 로그에 몇 번째 시도인지와 서킷 상태를 남긴다(`docs/standards/coding-conventions.md` 3-6절).
- [ ] T146 [US7] 실패 주문의 저장과 해제를 만든다. `applyResult()`가 그 밖의 결과에 `FAILED`와 `RETRY_LATER`를 저장하고, 예약 요청을 한 번도 보내지 않았으면 `RELEASE_STATUS`에 `NOT_REQUIRED`도 함께 저장한다. 예약 요청을 보낸 적이 있으면 `OrderService`가 `ReservationReleaser`에 해제를 맡기고 바로 201로 응답한다. 해제 작업은 `InventoryClient.release(orderNo)`를 한 번 부르고, 결과가 RELEASED나 REJECTED면 `RELEASED`, 끝내 실패하면 `RELEASE_FAILED`를 새 트랜잭션으로 저장하고 오류 로그를 남긴다 (services/order-service/src/main/java/com/example/msa/order/service/OrderService.java, services/order-service/src/main/java/com/example/msa/order/service/OrderTxService.java, services/order-service/src/main/java/com/example/msa/order/service/ReservationReleaser.java, services/order-service/src/main/java/com/example/msa/order/config/ReleaseExecutorConfig.java, services/order-service/src/main/java/com/example/msa/order/config/ReleaseExecutorProperties.java)
  - 근거: data-model.md 6-1절 4~6번, 4-2절 표, research.md 4절 "해제를 고객 응답 뒤에 비동기로 돌리는 방식"(`releaseExecutor` 빈, 종료할 때 실행 중인 해제를 기다린다).
  - 예약 결과 RELEASED를 받으면 실패로 둔다(data-model.md 4-1절의 `[제안]`).
- [ ] T147 [US7] 이 스토리의 테스트와 앞 스토리의 테스트가 모두 통과하는지 확인한다 (services/order-service/src/test/java/com/example/msa/order/integration/CircuitBreakerIntegrationTest.java)
  - 명령: `.\gradlew.bat :services:order-service:test`
  - 기대 결과: 종료 코드 0. 시나리오 7.1의 "1초 안에" 응답 시간을 테스트 로그에서 읽어 보고에 적는다.
- [ ] T148 [US7] research.md 6절 "JDK HttpClient의 요청 제한 시간이 연결 시간까지 포함해 시도당 2.5초를 지키는지"를 확인한다. 받치는 테스트 두 개를 쓴다. 하나는 WireMock 예약 스텁에 3초 지연을 1/5로 줄인 600ms를 넣고 한 시도가 테스트 설정의 500ms 근처에서 시간 초과로 끝나는지 본다. 다른 하나는 응답하지 않는 주소로 연결하는 단위 테스트다. 응답하지 않는 주소를 만드는 방법은 구현할 때 고르고 보고에 적는다 `[제안]` (services/order-service/src/test/java/com/example/msa/order/integration/InventoryClientTimeoutIntegrationTest.java `[제안]`)
  - 기대값은 비기능 요구사항 문서(`docs/requirements/non-functional.md`)의 "장애 전파 차단" 목표값("재시도 포함 전체 30초, 시도당 2.5초, 재시도 최대 5회(즉시, 1, 2, 4, 8초 뒤). 서킷이 열린 뒤에는 1초 안에 실패 응답")의 "시도당 2.5초"(테스트 설정 500ms `[제안]`)다. 인수 시나리오 테스트가 아니므로 ID를 붙이지 않는다.
  - 연결 시간이 포함되지 않아 2.5초를 넘기면 멈추고 보고한다. 해결 방법(예: 전체 시도 시간을 따로 재서 끊기)은 research.md 결정 6을 바꾸는 일이라 사용자에게 묻는다.
- [ ] T149 [US7] research.md 6절 "Resilience4j 2.4.0 핵심 모듈이 Spring Boot 4.1.1 앱 안에서 문제없이 도는지"를 확인한다. T147에서 시나리오 7.1이 통과했고, 테스트 로그에 의존성 충돌이나 클래스 로딩 경고가 없는지 본다. `.\gradlew.bat :services:order-service:dependencies --configuration runtimeClasspath`에서 Resilience4j가 끌어온 의존성 버전이 Spring Boot가 관리하는 버전과 부딪히는지도 본다 (services/order-service/build.gradle)

이 목록에서는 항목 ID를 쓰지 않았다.

**체크포인트**: 재고 서비스가 계속 실패해도 주문 서비스는 1초 안에 실패를 돌려주고, 409와 500에는 재시도하지 않으며, 실패 주문은 해제를 요청한다.

### 마무리와 확인

- [ ] T150 주문 서비스의 아키텍처 테스트를 쓴다. `libs/archunit-rules`의 세 규칙(계층 의존, 애너테이션 SQL 금지, `@Transactional`에서 `client` 직접 호출 금지)을 `com.example.msa.order`에 적용한다 (services/order-service/src/test/java/com/example/msa/order/architecture/ArchitectureTest.java)
- [ ] T151 주문 서비스에서 기계 검사와 아키텍처 테스트가 실제로 잡는지 확인한다. T077과 같은 방식으로 위반을 하나씩 넣고 `.\gradlew.bat :services:order-service:check`가 실패하는 것을 본 뒤 되돌린다. 특히 Phase 1에서 확인하지 못한 "`OrderTxService`의 `@Transactional` 메서드에서 `InventoryClient`를 직접 부른다" 위반을 넣어 T150이 잡는지 본다 (services/order-service/src/main/java/com/example/msa/order/service/OrderTxService.java)
  - 끝나면 `git diff`로 위반이 모두 되돌려졌는지 본다.
- [ ] T152 추적 정보 전달과 설정을 확인한다 (services/order-service/src/main/java/com/example/msa/order/config/InventoryClientConfig.java)
  - 명령: `Get-ChildItem services/order-service/src/main/java -Recurse -Filter *.java | Select-String 'RestClient\.builder\('` → 0줄. `RestClient`를 Spring Boot가 주는 `RestClient.Builder`로만 만든다는 뜻이다(research.md 결정 6).
  - 실제로 `traceparent` 헤더가 넘어가는지는 T102(㉩)가 검증한다. 여러 서비스의 로그를 추적 번호 하나로 찾는 요청 추적은 티켓 003의 운영 테스트에서 확인한다.
  - `Select-String -Path services/order-service/src/main/resources/application.yml -Pattern 'shutdown: graceful','timeout-per-shutdown-phase: 35s','probes','structured'` → 네 가지가 모두 나온다.
- [ ] T153 이 단계의 기준을 확인한다. 이 단계 머리의 "끝났다고 보는 기준" 표에서 GitHub Actions를 뺀 네 줄을 차례로 실행한다 (gradle.properties)
  - 기대 결과: `msa.stage=P2` 한 줄, `.\gradlew.bat build` 종료 코드 0, 두 서비스의 테스트 종료 코드 0, 재고 서비스 파일이 바뀌지 않음. 실행한 명령과 종료 코드, 테스트 통과·실패 수를 보고에 적는다.
  - quickstart.md 3절 P2 줄과 대조해, 시나리오 2.1, 3.1, 3.2, 3.3, 4.1, 5.1, 6.1, 7.1, 7.2의 테스트, ㉦·㉧·㉨·㉩의 테스트, T101과 T140의 테스트가 모두 보고서에 있는지 하나씩 적는다.
- [ ] T154 사용자가 push한 뒤 GitHub Actions 결과를 확인한다. push 전이면 "실행하지 않았다"로 보고한다 (.github/workflows/build.yml)
  - 명령: `gh run list --branch feat/001-place-order-p2 --limit 1` → 결론이 `success`.
- [ ] T155 사용자에게 보고하고 멈춘다. 만들거나 고친 파일, 각 확인 작업의 명령과 결과, spec 시나리오 2.1, 3.1, 3.2, 3.3, 4.1, 5.1, 6.1, 7.1, 7.2와 ㉦, ㉧, ㉨, ㉩을 하나씩 대조한 결과, research.md 6절에서 이 단계에 확인한 세 가지(T112, T148, T149)의 결과를 적는다. 커밋·push·PR은 사용자가 요청할 때 한다 (파일 없음, 대화창 보고)

이 목록에서는 항목 ID를 쓰지 않았다.

---

## Phase 3: P3 단계 PR — 재시도와 전체 30초 한도 (브랜치 `feat/001-place-order-p3`)

**목적**: 재고 호출의 일시 오류에 재시도를 더하고, 재시도를 포함한 전체 30초 한도를 둔다. 해제에도 같은 재시도 정책을 쓴다. 재시도는 멱등한 호출에만 해야 하므로, 예약의 멱등성(P1)과 제한 시간·서킷 브레이커(P2)가 먼저 있어야 한다(spec User Story 8의 "Why this priority").

**담는 것** (plan.md "PR 단계" 표의 P3 줄): Resilience4j Retry(즉시·1·2·4·8초, 일시 오류만, 서킷 안쪽), 요청마다 30초 한도, 해제의 같은 재시도 정책. `msa.stage=P3`.

**끝났다고 보는 기준** (plan.md "PR 단계" 표): `.\gradlew.bat build` 종료 코드 0, P1·P2의 테스트도 그대로 통과, GitHub Actions 성공

| 확인할 것 | 명령 | 기대 결과 |
|---|---|---|
| 단계 표시 | `Select-String -Path gradle.properties -Pattern '^msa.stage=P3$'` | 한 줄 |
| 빌드와 모든 검사 | `.\gradlew.bat build` | 종료 코드 0 |
| P1·P2의 테스트가 그대로 통과 | `.\gradlew.bat :services:inventory-service:test :services:order-service:test` | 종료 코드 0. P1·P2의 인수 시나리오 테스트 파일을 고치지 않았다(`git fetch origin` 뒤 `git diff origin/main --stat -- services/*/src/test`에 나오는 기존 파일은 이 단계에서 메서드를 더한 계약 테스트와 기본값 테스트뿐이다). 재고 서비스 파일은 바뀌지 않는다 |
| P3 인수 시나리오 테스트 | 위 명령의 테스트 보고서 | 시나리오 8.1, 시나리오 9.1의 주문 쪽, ㉢, ㉣의 테스트가 통과로 나온다(quickstart.md 3절 P3 줄) |
| GitHub Actions | 사용자가 push한 뒤 `gh run list --branch feat/001-place-order-p3 --limit 1` | 결론이 `success` |

**시작 조건**: Phase 2의 PR이 main에 병합되어 있고, 이 단계의 브랜치를 main에서 만들었다. 브랜치를 만드는 일은 사용자에게 묻고 한다. 확인 명령: `git branch --show-current` → `feat/001-place-order-p3`.

### 준비

- [ ] T156 단계 표시를 `msa.stage=P3`으로 올리고, 테스트 케이스 단계 표에서 단계가 P3인 줄이 plan.md 표와 추가 케이스 표와 같은지 확인한다. 다르면 표를 고치지 말고 멈춰 사용자에게 묻는다 (gradle.properties, config/quality/test-case-stages.csv)
  - 확인: `Select-String -Path config/quality/test-case-stages.csv -Pattern ',P3$'` → 4줄 (모두 `order` 줄이다)
- [ ] T157 버전 카탈로그에 Resilience4j `resilience4j-retry` 2.4.0을 더하고 주문 서비스 의존성에 넣는다 (gradle/libs.versions.toml, services/order-service/build.gradle)
  - 근거: research.md 결정 7.
- [ ] T158 재시도와 전체 한도 설정을 더한다. 기본 설정은 재시도 최대 5번, 재시도 전 대기 0·1·2·4·8초, 전체 30초다. 테스트 설정은 이 문서 머리의 "시간 값은 1/5로 줄인다" 표대로 대기 0·200·400·800·1600ms, 전체 6초다 `[제안]`. 서킷의 횟수 값(10번, 50%, 3번)은 시간 값이 아니므로 줄이지 않는다 (services/order-service/src/main/resources/application.yml, services/order-service/src/test/resources/application-test.yml)

이 목록에서는 항목 ID를 쓰지 않았다.

### 기반

- [ ] T159 재고 호출 설정 클래스에 재시도 최대 횟수, 대기 목록, 전체 한도를 더하고, 기본값 단위 테스트(T142)에 이 세 값의 검사를 더한다. 기대값은 비기능 요구사항 문서(`docs/requirements/non-functional.md`)의 "장애 전파 차단" 목표값("재시도 포함 전체 30초, 시도당 2.5초, 재시도 최대 5회(즉시, 1, 2, 4, 8초 뒤)") 가운데 재시도 세 값이다. 이미 있는 검사는 바꾸지 않는다 (services/order-service/src/main/java/com/example/msa/order/config/InventoryClientProperties.java, services/order-service/src/test/java/com/example/msa/order/config/InventoryClientPropertiesTest.java)
  - 근거: research.md 4절 "설정 클래스", `docs/standards/testing.md` 3절 "시간".

이 목록에서는 항목 ID를 쓰지 않았다.

### User Story 8 — 재고 서비스에 잠깐 오류가 나도 주문은 확정된다 (Priority: P3)

**목표**: 연결 실패, 시도 제한 시간 초과, 502·503·504에는 같은 주문 번호로 즉시, 1, 2, 4, 8초 뒤에 최대 5번 다시 시도한다. 서킷이 열려 있으면 재시도하지 않는다.

**독립 테스트** (spec User Story 8): WireMock이 처음 두 번은 503을, 세 번째부터는 RESERVED를 돌려주게 한다. 고객이 "확정"을 받는지, 예약 요청 횟수와 간격이 기대값과 맞는지 확인한다.

#### 인수 시나리오 테스트 (구현보다 먼저)

- [ ] T160 [P] [US8] 시나리오 8.1의 주문 쪽 서비스 통합 테스트를 쓴다. WireMock 시나리오 기능으로 예약 요청 첫 두 번에 503, 셋째부터 RESERVED를 돌려준다. 응답이 `CONFIRMED`인지, 예약 요청이 정확히 3번 갔는지, 세 요청의 주문 번호와 본문이 같은지, 간격이 맞는지 검사한다. 간격은 1/5 규칙으로 줄인 값이다 `[제안]`: 둘째는 첫째 응답 뒤 0.1초 안, 셋째는 둘째 응답 뒤 0.16초 이상 0.3초 이하. 응답을 받은 시각은 WireMock이 요청을 받은 시각으로 대신 잰다 `[제안]`. 503 스텁에는 지연이 없어 두 시각의 차이가 작기 때문이다. 재고 수량 기대값(7개)은 T055가 재고 쪽에서 본다. `@DisplayName`은 시나리오 8.1의 테스트 케이스 ID로 시작한다. 기대값은 `docs/test-cases/order-placement.md`에서 그대로 가져온다 (services/order-service/src/test/java/com/example/msa/order/integration/TransientErrorRetryIntegrationTest.java)
- [ ] T161 [P] [US8] 시나리오 8.2(㉢ 재시도하던 중에 서킷이 열리면 남은 재시도를 하지 않는다)의 서비스 통합 테스트를 쓴다. 기대값은 `docs/test-cases/order-placement.md`에서 그대로 가져온다. 시간 값은 1/5 규칙을 따른다 `[제안]`. `@DisplayName`은 시나리오 8.2의 테스트 케이스 ID로 시작한다. 스토리 라벨은 재시도 동작이라 `[US8]`로 붙였다 `[제안]` (services/order-service/src/test/java/com/example/msa/order/integration/CircuitOpensDuringRetryIntegrationTest.java)
  - 근거: plan.md "인수 시나리오와 테스트 층" 표의 시나리오 8.2 줄, FR-035.
- [ ] T162 [US8] 소비자 계약 테스트에 시나리오 8.1의 RESERVED 스텁과 받은 예약 요청 세 개를 검증하는 메서드를 더한다. 503 스텁의 응답은 검증하지 않는다(T094) (services/order-service/src/test/java/com/example/msa/order/contract/InventoryConsumerContractTest.java)
  - 근거: plan.md 표의 시나리오 8.1 "계약" 칸 "소비자".
- [ ] T163 [P] [US8] 대기 일정의 단위 테스트를 쓴다. plan.md 표의 시나리오 8.1 "단위" 칸 "대기 일정(0·1·2·4·8초)"이다. 기본 설정으로 첫째부터 다섯째 재시도 전 대기가 0, 1000, 2000, 4000, 8000밀리초인지 검사한다. 기대값은 비기능 요구사항 문서(`docs/requirements/non-functional.md`)의 "장애 전파 차단" 목표값의 "재시도 최대 5회(즉시, 1, 2, 4, 8초 뒤)"다 (services/order-service/src/test/java/com/example/msa/order/client/RetryScheduleTest.java `[제안]`)
  - 위치는 `client/` 테스트 패키지다 `[제안]`. plan.md 트리에 더했다(2026-10-07 사용자 결정). 대기 일정 클래스(T165)가 `client` 패키지에 있어서다.
- [ ] T164 [US8] 위 테스트가 실패하는 것을 확인한다 (services/order-service/src/test/java/com/example/msa/order/integration/TransientErrorRetryIntegrationTest.java)
  - 명령: `.\gradlew.bat :services:order-service:test --tests "com.example.msa.order.integration.TransientErrorRetryIntegrationTest" --tests "com.example.msa.order.integration.CircuitOpensDuringRetryIntegrationTest"`
  - 기대 결과: 종료 코드가 0이 아니다. 재시도가 없어 T160은 `FAILED`와 요청 1번으로 단언이 실패한다. T163은 대기 일정 클래스가 없어 컴파일 오류로 실패한다. 하나라도 통과하면 멈추고 사용자에게 묻는다.

**이 절의 시나리오 원문**

> **TC-006** (spec 시나리오 8.1) · `docs/test-cases/order-placement.md` 절 "TC-006 재고 서비스의 일시 오류는 재시도로 넘어간다"
> - **Given** 상품 A의 재고가 10개다
> - **And** 재고 서비스가 처음 두 번의 예약 요청에 503을 돌려주고, 세 번째부터 정상으로 처리한다
> - **When** 고객이 상품 A를 3개 주문한다
> - **Then** 고객은 "확정"을 받는다
> - **And** 예약 요청은 정확히 3번 갔다. 두 번째는 첫 번째 응답을 받은 뒤 0.5초 안에, 세 번째는 두 번째 응답을 받은 뒤 0.8초 이상 1.5초 이하에 갔다
> - **And** 상품 A의 재고는 7개다

> **TC-018** (spec 시나리오 8.2) · `docs/test-cases/order-placement.md` 절 "TC-018 재시도하던 중에 서킷이 열리면 남은 재시도를 하지 않는다"
> - **Given** 재고 서비스가 모든 예약 요청과 해제 요청에 503을 돌려준다
> - **And** 고객 C1의 첫 주문이 예약 요청 6번을 모두 실패하고 "실패"로 끝났다
> - **When** 고객 C1이 둘째 주문을 요청한다
> - **Then** 둘째 주문의 예약 요청은 6번보다 적게 간다. 서킷이 열린 뒤에는 남은 재시도를 하지 않는다
> - **And** 둘째 주문은 "실패"로 기록되고 고객은 "잠시 후 다시 시도"를 받는다
> - 비고: 첫 주문의 해제 요청도 같은 서킷을 지나가므로, 둘째 주문의 요청이 정확히 몇 번인지는 해제가 언제 끼어드는지에 따라 달라진다. 그래서 "6번보다 적다"로 판정한다.

#### 구현

- [ ] T165 [US8] 대기 일정 클래스를 만든다. 설정의 대기 목록을 받아, 몇 번째 재시도인지에 따라 대기 밀리초를 돌려준다. Resilience4j `IntervalBiFunction`으로 감싸 쓴다 (services/order-service/src/main/java/com/example/msa/order/client/RetrySchedule.java `[제안]`)
  - 근거: research.md 결정 7("`IntervalBiFunction`으로 시도 번호에 따라 0, 1000, 2000, 4000, 8000밀리초를 돌려준다").
- [ ] T166 [US8] 재시도를 만든다. 요청마다 `Retry`를 새로 만들고 설정 객체는 공유한다. 순서는 `Decorators`로 "재시도(서킷 브레이커(재고 호출))"다. 재시도할 예외는 연결 실패와 시간 초과(`ResourceAccessException`), 502·503·504(`HttpServerErrorException`의 상태 코드)뿐이다. 서킷이 열려 생긴 `CallNotPermittedException`, 409, 500, 4xx, 업무 결과에는 재시도하지 않는다. 재시도할 때 같은 주문 번호를 보낸다 (services/order-service/src/main/java/com/example/msa/order/client/InventoryClient.java, services/order-service/src/main/java/com/example/msa/order/config/InventoryClientConfig.java)
  - 근거: research.md 결정 7, `docs/standards/architecture-rules.md` 2절 "서비스 사이 호출"의 재시도 규칙과 멱등 규칙과 서킷 브레이커 규칙.
  - 재고 호출 로그에 몇 번째 시도인지와 서킷 상태를 남긴다(`docs/standards/coding-conventions.md` 3-6절).
- [ ] T167 [US8] "재시도하지 않는다"를 보는 테스트가 결함을 잡는지 확인한다. 이 테스트들은 재시도가 없던 P2에서 실패할 수 없었다(이 문서 머리의 "테스트는 필수다"). T166에서 만든 재시도 판단을 아래처럼 하나씩 잠시 바꾸고, 해당 테스트가 실패하는 것을 본 뒤 되돌린다(2026-10-06 사용자 승인) (services/order-service/src/main/java/com/example/msa/order/client/InventoryClient.java)
  1. 업무 결과 REJECTED에도 재시도하게 바꾼다(`retryOnResult`) → 시나리오 3.2의 T114와 시나리오 3.3의 T115가 실패한다(예약 요청이 1번보다 많다)
  2. 500에도 재시도하게 바꾼다 → T140이 실패한다
  3. 409에도 재시도하게 바꾼다 → 시나리오 7.2의 T139가 실패한다
  - 명령: `.\gradlew.bat :services:order-service:test --tests "com.example.msa.order.integration.NoRetryOnOutOfStockIntegrationTest" --tests "com.example.msa.order.integration.PlaceOrderProductNotFoundIntegrationTest" --tests "com.example.msa.order.integration.InventoryServerErrorIntegrationTest" --tests "com.example.msa.order.integration.ReservationConflictOrderIntegrationTest"`
  - 기대 결과: 바꿀 때마다 해당 테스트가 실패하고, 되돌린 뒤에는 종료 코드 0이다. 되돌린 뒤 `git diff -- services/order-service/src/main`에 T166의 변경 말고 다른 것이 없는지 본다. 바꿨는데도 테스트가 통과하면 멈추고 사용자에게 묻는다.
- [ ] T168 [US8] 이 스토리의 테스트와 P1·P2 테스트가 모두 통과하는지 확인한다. 특히 시나리오 3.2와 3.3의 "예약 요청은 정확히 1번", 시나리오 7.1, 시나리오 7.2, T140, T101의 테스트가 고치지 않은 채 통과하는지 본다 (services/order-service/src/test/java/com/example/msa/order/integration/TransientErrorRetryIntegrationTest.java)
  - 명령: `.\gradlew.bat :services:order-service:test :services:inventory-service:test`
  - 기대 결과: 종료 코드 0. P2의 테스트가 실패하면 테스트를 고치지 않고 구현을 고친다. 테스트가 P2 동작에만 맞게 쓰여 있다고 판단되면 멈추고 사용자에게 묻는다(저장소 루트 `CLAUDE.md` 7절).

이 목록에서는 항목 ID를 쓰지 않았다.

**체크포인트**: 일시 오류가 두 번 나도 주문은 "확정"되고, 재고는 한 번만 줄어든다.

### User Story 9 — 재고 확인이 30초 안에 끝나지 않으면 고객은 실패 응답을 받고, 재고는 줄어든 채 남지 않는다 (Priority: P3)

**목표**: 재시도를 포함해 30초 안에 예약 결과를 얻지 못하면 주문을 "실패"로 바꾸고 응답한 뒤 해제를 요청한다. 해제에도 예약과 같은 재시도 정책을 쓴다. 재고 쪽(해제하면 수량이 돌아온다)은 Phase 1의 T050이 이미 검증했다.

**독립 테스트** (spec User Story 9): WireMock이 예약 요청에 3초 뒤에 응답하고 해제 요청에는 바로 응답하게 한다. 고객 응답 시간, 예약 요청 횟수, 주문 상태와 해제 결과, health 확인 응답을 확인한다.

#### 인수 시나리오 테스트 (구현보다 먼저)

- [ ] T169 [P] [US9] 시나리오 9.1의 주문 쪽 서비스 통합 테스트를 쓴다. WireMock 예약 스텁은 RESERVED를 600ms(3초를 1/5로 `[제안]`) 뒤에 돌려주고, 해제 스텁은 RELEASED를 바로 돌려준다. 응답이 6.2초 안에 오고 주문 번호와 `FAILED`·`RETRY_LATER`인지, 예약 요청이 정확히 6번 갔는지, 주문이 `FAILED`로 기록되는지, 고객 응답 뒤 5초 안에 해제 요청이 가고 `RELEASE_STATUS`가 `RELEASED`인지, 그동안 다른 스레드에서 부른 health 확인이 1초 안에 200인지 검사한다. "상품 A의 재고는 10개다"는 T050이 재고 쪽에서 검증했다. `@DisplayName`은 시나리오 9.1의 테스트 케이스 ID로 시작한다. 기대값은 `docs/test-cases/order-placement.md`에서 그대로 가져온다 (services/order-service/src/test/java/com/example/msa/order/integration/OverallTimeLimitIntegrationTest.java)
  - 1/5로 줄이면 여섯째 시도가 5.5초에 시작해 6초에 끝나고, 응답 기한(6.2초)까지 여유가 0.2초다. CI에서 이 여유가 모자라 흔들리면 기대값이나 비율을 바꾸지 말고 멈춰 사용자에게 묻는다.
- [ ] T170 [P] [US9] 시나리오 9.2(㉣ 해제 요청도 일시 오류면 같은 정책으로 재시도한다)의 서비스 통합 테스트를 쓴다. 기대값은 `docs/test-cases/order-placement.md`에서 그대로 가져온다. 시간 값은 1/5 규칙을 따른다 `[제안]`. `@DisplayName`은 시나리오 9.2의 테스트 케이스 ID로 시작한다. 스토리 라벨은 실패 주문의 해제라서 `[US9]`로 붙였다 `[제안]` (services/order-service/src/test/java/com/example/msa/order/integration/ReleaseRetryIntegrationTest.java)
  - 근거: plan.md "인수 시나리오와 테스트 층" 표의 시나리오 9.2 줄, FR-036.
- [ ] T171 [US9] 소비자 계약 테스트에 시나리오 9.1의 지연된 RESERVED 스텁, 해제 스텁, 받은 예약·해제 요청을 검증하는 메서드를 더한다 (services/order-service/src/test/java/com/example/msa/order/contract/InventoryConsumerContractTest.java)
  - 근거: plan.md 표의 "9.1의 주문 쪽" 줄 "계약" 칸 "소비자".
- [ ] T172 [P] [US9] 30초 한도 판단의 단위 테스트를 쓴다. plan.md 표의 "9.1의 주문 쪽" 줄 "단위" 칸 "30초 한도 판단"이다. 시작 시각과 지금 시각, 다음 대기를 넣어, 다음 시도가 30초가 되기 전에 시작하면 재시도를 허락하고 그렇지 않으면 막는지 검사한다. 또 남은 시간이 시도당 제한 시간보다 짧을 때 그 시도에 주는 제한 시간이 남은 시간인지 검사한다. 이 기대값의 근거는 같은 공통 전제의 "시도는 30초에 끊는다"다(2026-10-06 사용자 승인). 기대값의 근거는 테스트 케이스 문서 공통 전제 "30초가 되기 전이면, 남은 시간이 시도당 제한 시간(2.5초)보다 짧아도 시도를 시작한다"와 비기능 요구사항 문서(`docs/requirements/non-functional.md`)의 "장애 전파 차단" 목표값의 "재시도 포함 전체 30초"다. 시각은 주입한 시계로 정한다 `[제안]` (services/order-service/src/test/java/com/example/msa/order/client/InventoryCallContextTest.java `[제안]`)
- [ ] T173 [US9] 위 테스트가 실패하는 것을 확인한다 (services/order-service/src/test/java/com/example/msa/order/integration/OverallTimeLimitIntegrationTest.java)
  - 명령: `.\gradlew.bat :services:order-service:test --tests "com.example.msa.order.integration.OverallTimeLimitIntegrationTest" --tests "com.example.msa.order.integration.ReleaseRetryIntegrationTest"`
  - 기대 결과: T170은 해제 재시도가 없어 실패한다. T172는 한도 판단 메서드가 없어 컴파일 오류로 실패한다.
  - T169는 User Story 8의 재시도만으로 통과할 수 있다. 시도당 2.5초와 대기를 모두 더해도 30초를 넘지 않기 때문이다(research.md 결정 7). 통과하면 그 사실을 보고에 적고, 테스트 설정의 시도당 제한 시간을 잠시 1초로 늘려 T169가 실패하는 것(예약 요청이 6번보다 적거나 응답이 늦다)을 본 뒤 되돌린다 `[제안]`. 이것은 시도당 제한 시간이 빠지면 이 테스트가 잡는다는 확인이다(spec SC-002).

**이 절의 시나리오 원문**

> **TC-004** (spec 시나리오 9.1의 주문 쪽) · `docs/test-cases/order-placement.md` 절 "TC-004 30초 안에 예약 결과를 못 받으면 실패로 기록하고 재고를 해제한다"
> - **Given** 상품 A의 재고가 10개다
> - **And** 재고 서비스가 예약 요청을 처리해 재고를 줄이지만, 응답은 3초 뒤에 준다 (시도당 제한 시간 2.5초보다 길다)
> - **And** 재고 서비스는 해제 요청에는 바로 정상으로 응답한다
> - **When** 고객이 상품 A를 3개 주문한다
> - **Then** 고객은 31초 안에 주문 번호와 "잠시 후 다시 시도"를 받는다
> - **And** 예약 요청은 정확히 6번 갔다
> - **And** 그 주문은 "실패" 상태로 기록되어 있다
> - **And** 고객 응답 뒤 5초 안에 재고 서비스에 해제 요청이 가고, 주문에 "해제 완료"가 기록된다
> - **And** 상품 A의 재고는 10개다
> - **And** 그동안 주문 서비스의 health 확인은 1초 안에 200으로 응답한다
> - 비고: 한 테스트로 두 서비스를 다 확인하기 어렵다. 주문 쪽(시도 횟수, 실패 기록, 해제 요청)은 WireMock으로 지연을 넣어 검증하고, 재고 쪽(해제하면 수량이 복구된다)은 재고 서비스 테스트로 검증한다.

> **TC-019** (spec 시나리오 9.2) · `docs/test-cases/order-placement.md` 절 "TC-019 해제 요청도 일시 오류면 같은 정책으로 재시도한다"
> - **Given** 재고 서비스가 예약 요청에 409를 돌려준다
> - **And** 재고 서비스가 해제 요청의 처음 두 번에 503을 돌려주고, 세 번째부터 정상으로 처리한다
> - **When** 고객이 상품 A를 3개 주문한다
> - **Then** 고객은 주문 번호와 "잠시 후 다시 시도"를 받는다
> - **And** 해제 요청은 정확히 3번 갔다. 두 번째는 첫 번째 응답을 받은 뒤 0.5초 안에, 세 번째는 두 번째 응답을 받은 뒤 0.8초 이상 1.5초 이하에 갔다
> - **And** 주문에 "해제 완료"가 기록된다

#### 구현

- [ ] T174 [US9] 전체 한도 판단을 만든다. `InventoryCallContext`가 시작 시각과 시도 수를 들고, 재시도 여부 판단 함수(`retryOnException`)가 "지금 시각 + 다음 대기 < 시작 시각 + 전체 한도"일 때만 재시도를 허락한다. 한도를 넘으면 남은 재시도를 하지 않고 "실패"로 처리한다. 진행 중인 시도도 전체 한도에서 끊는다. 시도마다 제한 시간을 "시도당 제한 시간"과 "전체 한도까지 남은 시간" 가운데 짧은 쪽으로 준다 (services/order-service/src/main/java/com/example/msa/order/client/InventoryCallContext.java, services/order-service/src/main/java/com/example/msa/order/config/InventoryClientConfig.java)
  - 근거: 테스트 케이스 문서 공통 전제 "30초가 되기 전이면, 남은 시간이 시도당 제한 시간(2.5초)보다 짧아도 시도를 시작한다. 시도는 30초에 끊는다."와 spec FR-034(2026-10-06 사용자 승인). research.md 결정 7은 "시도당 2.5초와 대기를 모두 더해도 30초를 넘지 않으므로 끊지 않아도 된다"고 적었다. 그런데 이 계산은 한 시도가 연결 시간을 포함해 2.5초를 넘지 않는다는 전제에 기대고, 그 전제는 T148에서 확인하기 전까지 확인되지 않았다.
  - 시도 하나의 제한 시간을 남은 시간으로 줄이는 방법(예: 그 시도에만 제한 시간을 줄인 요청을 만든다)은 구현할 때 고르고 보고에 적는다 `[제안]`.
- [ ] T175 [US9] 해제에 예약과 같은 재시도 정책을 쓴다. `ReservationReleaser`의 해제 호출을 예약과 같은 방식(요청마다 새 `Retry`, 같은 서킷 안쪽, 같은 전체 한도)으로 감싼다. 서킷에 막히면 재시도하지 않고, 끝내 실패하면 `RELEASE_FAILED`를 저장하고 오류 로그를 남긴다 (services/order-service/src/main/java/com/example/msa/order/service/ReservationReleaser.java, services/order-service/src/main/java/com/example/msa/order/client/InventoryClient.java)
  - 근거: FR-036, spec Edge Cases "서킷이 열린 상태에서 해제를 요청해야 한다", data-model.md 4-2절.
- [ ] T176 [US9] 이 스토리의 테스트와 P1·P2·User Story 8 테스트가 모두 통과하는지 확인한다 (services/order-service/src/test/java/com/example/msa/order/integration/OverallTimeLimitIntegrationTest.java)
  - 명령: `.\gradlew.bat :services:order-service:test :services:inventory-service:test`
  - 기대 결과: 종료 코드 0. 시나리오 9.1의 고객 응답 시간과 health 응답 시간을 테스트 로그에서 읽어 보고에 적는다.

이 목록에서는 항목 ID를 쓰지 않았다.

**체크포인트**: 재고 응답이 늦어도 고객은 한도 안에 실패를 받고, 실패 주문의 예약은 해제된다.

### 마무리와 확인

- [ ] T177 이 단계의 기준을 확인한다. 이 단계 머리의 "끝났다고 보는 기준" 표에서 GitHub Actions를 뺀 네 줄을 차례로 실행한다 (gradle.properties)
  - 기대 결과: `msa.stage=P3` 한 줄, `.\gradlew.bat build` 종료 코드 0, 두 서비스 테스트 종료 코드 0, P1·P2의 인수 시나리오 테스트 파일이 바뀌지 않음. 실행한 명령과 종료 코드, 테스트 통과·실패 수를 보고에 적는다.
  - quickstart.md 3절 P3 줄과 대조해, 시나리오 8.1, 시나리오 9.1의 주문 쪽, ㉢, ㉣의 테스트가 보고서에 있는지 하나씩 적는다. spec의 SC-001부터 SC-005까지 다섯 개를 하나씩 대조하고, 각각을 검증한 테스트를 적는다.
- [ ] T178 사용자가 push한 뒤 GitHub Actions 결과를 확인한다. push 전이면 "실행하지 않았다"로 보고한다. 시나리오 9.1의 테스트가 CI에서 시간 여유 때문에 흔들렸는지도 로그에서 본다 (.github/workflows/build.yml)
  - 명령: `gh run list --branch feat/001-place-order-p3 --limit 1` → 결론이 `success`.
- [ ] T179 사용자에게 보고하고 멈춘다. 만들거나 고친 파일, 각 확인 작업의 명령과 결과, spec 시나리오 8.1, 9.1의 주문 쪽, ㉢, ㉣과 SC-001부터 SC-005까지를 하나씩 대조한 결과, T167의 결과를 적는다. 커밋·push·PR은 사용자가 요청할 때 한다 (파일 없음, 대화창 보고)

이 목록에서는 항목 ID를 쓰지 않았다.

---

## plan 표와 작업 대응

plan.md "인수 시나리오와 테스트 층" 표의 줄마다, 그 줄의 서비스·단계·테스트 층에 놓인 작업 번호다. 표의 "—" 칸은 그 층에 테스트가 없다는 뜻이라 작업도 없다. 줄은 spec 시나리오 번호로 가리킨다. 모든 단계에서 함께 도는 아키텍처 테스트는 T076(재고)과 T150(주문)이다.

| spec 시나리오 | 서비스 | 단계 | 단위 | Mapper | 서비스 통합 (인수) | 계약 |
|---|---|---|---|---|---|---|
| 1.1 | 재고 | Phase 1 | T059 | T064 | T042 | T056 |
| 1.2 | 재고 | Phase 1 | — | T063 | T043 | — |
| 1.3 | 재고 | Phase 1 | — | T064 | T044 | T056 |
| 1.4 | 재고 | Phase 1 | — | — | T045 | T056 |
| 1.5 | 재고 | Phase 1 | — | — | T046 | — |
| 9.1의 재고 쪽 | 재고 | Phase 1 | — | T065 | T050 | — |
| 2.1의 재고 쪽 | 재고 | Phase 1 | — | — | T051 | — |
| 3.1의 재고 쪽 | 재고 | Phase 1 | — | — | T052 | — |
| 3.3의 재고 쪽 | 재고 | Phase 1 | T060 | — | T053 | T056 |
| 6.1의 재고 쪽 | 재고 | Phase 1 | — | — | T054 | — |
| 8.1의 재고 쪽 | 재고 | Phase 1 | — | — | T055 | — |
| 2.1 | 주문 | Phase 2 | — | T104 | T099 | T100 |
| 3.1 | 주문 | Phase 2 | — | T118 | T113 | T116 |
| 3.2 | 주문 | Phase 2 | — | — | T114 | — |
| 3.3 | 주문 | Phase 2 | — | — | T115 | T116 |
| 4.1 | 주문 | Phase 2 | — | — | T121 | — |
| 5.1 | 주문 | Phase 2 | T127 | — | T126 | — |
| 6.1 | 주문 | Phase 2 | — | T135 | T132 | — |
| 7.1 | 주문 | Phase 2 | — | — | T138 | T141 |
| 7.2 | 주문 | Phase 2 | — | — | T139 | T141 |
| 8.1 | 주문 | Phase 3 | T163 | — | T160 | T162 |
| 9.1의 주문 쪽 | 주문 | Phase 3 | T172 | — | T169 | T171 |
| 3.1의 조회 부분 | 주문 | 티켓 002 | — | — | — | — |
| 4.1의 조회 부분 | 주문 | 티켓 002 | — | — | — | — |

plan.md 표에 줄이 없지만 판정 기준 문서에서 기대값을 가져오는 테스트(인수 시나리오 테스트가 아니라서 테스트 케이스 ID를 붙이지 않는다):

| 검증하는 것 | 기대값을 가져오는 곳 | 단계 | 작업 |
|---|---|---|---|
| FR-014 (주문의 주인은 토큰의 고객이다) | 유스케이스 6절 업무 규칙 "주문은 로그인한 고객 본인의 이름으로만 만든다" | Phase 2 | T101 |
| FR-027의 500 쪽 (500이면 재시도하지 않는다) | 테스트 케이스 문서 공통 전제 일곱째 줄 | Phase 2 | T140 |
| 주문 상태 전이 (단위 테스트) | 도메인 분석 4절 "주문 상태"의 그림 | Phase 2 | T095 |

추가 케이스 표(research.md 결정 2의 아홉 케이스. 첫 칸 앞의 번호는 spec 시나리오 번호다):

| 추가할 케이스 | 서비스 | 단계 | 서비스 통합 | 계약 |
|---|---|---|---|---|
| 1.6 ㉠ 거절된 예약에 해제 요청이 와도 재고는 그대로다 | 재고 | Phase 1 | T047 | T056 |
| 1.7 ㉡ 해제한 예약의 기록은 남아, 같은 예약 요청이 다시 와도 재고가 줄지 않는다 | 재고 | Phase 1 | T048 | — |
| 1.8 ㉥ 예약된 재고에 해제가 두 번 와도 수량은 한 번만 돌아온다 | 재고 | Phase 1 | T049 | — |
| 4.2 ㉦ 서명이 틀리거나 만료된 토큰으로는 주문할 수 없다 | 주문 | Phase 2 | T122 | — |
| 5.2 ㉧ 형식이 틀린 주문 항목은 기록되지 않는다 | 주문 | Phase 2 | T128 | — |
| 6.2 ㉨ 거부된 요청의 키는 남지 않고, 같은 키의 내용 비교는 항목 순서를 보지 않는다 | 주문 | Phase 2 | T133 | — |
| 2.2 ㉩ 주문 서비스는 재고 서비스 호출에 추적 정보를 넘긴다 | 주문 | Phase 2 | T102 | — |
| 8.2 ㉢ 재시도하던 중에 서킷이 열리면 남은 재시도를 하지 않는다 | 주문 | Phase 3 | T161 | — |
| 9.2 ㉣ 해제 요청도 일시 오류면 같은 정책으로 재시도한다 | 주문 | Phase 3 | T170 | — |

research.md 6절 "확인하지 못한 것"과 확인 작업:

| 확인하지 못한 것 (research.md 6절) | 가리킨 단계와 방법 | 작업 |
|---|---|---|
| WireMock 3.13.2의 `WireMockExtension`이 JUnit Jupiter 6.0.3에서 도는지 | P2 첫 서비스 통합 테스트 | T112 |
| JDK HttpClient의 요청 제한 시간이 연결 시간까지 포함해 시도당 2.5초를 지키는지 | P2. 3초 지연 테스트와 응답하지 않는 주소 단위 테스트 | T148 |
| MyBatis 3.5.19에서 record 생성자 매핑이 되는지, 생성자 안 `<collection>`이 안 되는지 | P1 첫 Mapper 테스트 | T069 |
| swagger-request-validator-core 2.46.1이 OpenAPI 3.0.3 계약과 JDK 17에서 도는지 | P1 계약 테스트(제공자) | T075 |
| palantir-java-format이 JDK 17의 Gradle 9.8.0 안에서 Spotless로 도는지 | P1 첫 `./gradlew build` | T041 |
| Testcontainers 2.0.5가 이 PC의 Docker Desktop에서 Oracle 이미지를 띄우는지 | P1 첫 Mapper 테스트 | T068 |
| Resilience4j 2.4.0 핵심 모듈이 Spring Boot 4.1.1 앱 안에서 문제없이 도는지 | P2 서비스 통합 테스트 | T149 |
| GitHub Actions 러너에서 Oracle 컨테이너를 띄우는 시간과 메모리 | P1 PR의 첫 CI 실행 | T080 |

research.md 6절 밖에서 tasks.md가 새로 적은 미확인 항목과 확인 작업:

| 확인하지 못한 것 | 처음 적은 작업 | 확인하고 보고하는 작업 |
|---|---|---|
| Spring Boot 4.1.1 테스트 스타터의 이름 | T031 | T041 (`compileTestJava`) |
| Spring Boot 4.1.1의 OTLP 속성 이름 | T033 | T078 |
| OpenTelemetry Logback appender를 Spring Boot 4.1.1이 자동으로 연결하는지 | T034 | T078 |

이 절에서는 항목 ID를 쓰지 않았다.

---

## 의존 관계와 실행 순서

### 단계 사이

- **Phase 0 → Phase 1**: 0-A와 0-B가 모두 main에 병합된 뒤 Phase 1을 시작한다(Phase 1 머리의 "시작 조건"). 0-A가 있어야 T023(단계 표의 ㉠·㉡·㉢·㉣·㉥ 줄), T047, T048, T049, T056의 ㉠ 부분을 할 수 있다. 0-B가 있어야 기술 스택 문서가 plan의 버전과 같아진다.
- **Phase 1 → Phase 2 → Phase 3**: 앞 단계의 PR이 main에 병합된 뒤, main에서 다음 단계 브랜치를 만든다. Phase 2는 Phase 1의 재고 서비스와 빌드 골격을, Phase 3는 Phase 2의 `InventoryClient`와 서킷 브레이커를 쓴다.
- Phase 2의 T102(㉩), T122(㉦), T128(㉧), T133(㉨)과 Phase 3의 T161(㉢), T170(㉣)도 0-A의 병합에 기댄다.

### 단계 안

- 준비 → 기반 → 스토리 절 → 마무리 순서다. 기반이 끝나기 전에는 스토리 절을 시작하지 않는다.
- 스토리 절 안에서는 인수 시나리오 테스트 → 실패 확인 → 받치는 테스트와 구현 → 통과 확인 순서다.
- Phase 1: User Story 1 절 하나다. 이 절에 User Story 2, 3, 6, 8, 9의 재고 쪽 인수 테스트도 함께 있다. 재고 구현이 모두 이 절에 있어서, 테스트를 구현보다 먼저 쓰려면 같은 절에 두어야 한다.
- Phase 2: User Story 2 → 3 → 4 → 5 → 6 → 7. 모두 같은 `OrderController`, `OrderService`, `OrderTxService`를 차례로 넓히므로 순서대로 한다. User Story 2가 주문 흐름의 뼈대를 만든다.
- Phase 3: User Story 8 → 9. User Story 9의 전체 한도와 해제 재시도는 User Story 8의 재시도 위에 얹는다.
- 같은 파일을 여러 스토리가 넓히는 곳: 주문 쪽 소비자 계약 테스트(T100, T116, T141, T162, T171), 재고 쪽 제공자 계약 테스트(T056), 주문 Mapper 테스트(T104, T118, T135). 이 작업에는 `[P]`를 붙이지 않았다.

### 동시에 할 수 있는 작업의 예

```text
# Phase 1 준비: 서로 다른 파일
T019 config/spotbugs/exclude.xml
T020 config/quality/suppression-allowlist.csv
T023 config/quality/test-case-stages.csv
T021 contracts/inventory-api.yaml
T022 .github/workflows/build.yml

# Phase 1 기반: 검사 작업 다섯 개와 ArchUnit 라이브러리
T025 NoLombokCheck.groovy
T026 SuppressionMarkerCheck.groovy
T027 MapperXmlCheck.groovy
T028 Sql19cKeywordCheck.groovy
T029 TestCaseIdCheck.groovy
T030 libs/archunit-rules

# Phase 1 User Story 1 절의 인수 시나리오 테스트 (재고 쪽 전부)
T042 ReservationIdempotencyIntegrationTest.java
T043 ReservationConcurrencyIntegrationTest.java
T044 ReleaseBeforeReserveIntegrationTest.java
T045 ReservationConflictIntegrationTest.java
T046 ReservationDeadlockIntegrationTest.java
T047 ReleaseRejectedReservationIntegrationTest.java
T048 ReserveAfterReleaseIntegrationTest.java
T050 ReleaseRestoresStockIntegrationTest.java
T051 ConfirmedOrderStockIntegrationTest.java
T052 RejectedOrderStockIntegrationTest.java
T053 ProductNotFoundStockIntegrationTest.java
T054 DistinctOrdersStockIntegrationTest.java
T055 RetriedReservationStockIntegrationTest.java
T056 ReservationProviderContractTest.java

# Phase 2 User Story 3의 테스트
T113 PlaceOrderRejectedIntegrationTest.java
T114 NoRetryOnOutOfStockIntegrationTest.java
T115 PlaceOrderProductNotFoundIntegrationTest.java
```

## 구현 전략

- **이 기능의 P1은 MVP가 아니다.** spec User Story 1의 "Why this priority"가 적은 대로 P1은 재고 서비스 안의 예약·해제만 다룬다. 고객이 주문할 수 있게 되는 것은 Phase 2가 병합된 뒤다.
- **단계마다 PR 하나를 병합한다.** 각 PR은 헌법을 지킨 채로 병합된다. P1에는 서비스 사이 호출이 없고, P2는 재고 호출을 처음 만들면서 제한 시간과 서킷 브레이커를 함께 넣고, P3가 재시도와 전체 한도를 더한다(spec "우선순위와 PR 단계" 절).
- **단계를 넘어 테스트를 고치지 않는다.** P2의 시나리오 3.2, 3.3, 7.1, 7.2 테스트와 T140은 P3에서 재시도가 들어와도 고치지 않고 통과해야 한다. 그래서 P2에서 테스트 케이스 문장에 있는 것만 단언한다.
- **재고 쪽 인수 테스트는 모두 P1에 있다.** P2와 P3는 재고 서비스를 고치지 않는다.
- **한 단계 안에서는 스토리 순서대로 한다.** 스토리마다 체크포인트에서 그때까지의 테스트가 모두 통과하는지 본다.

---

## 자동으로 고른 것

이번 실행에서 묻지 않고 고른 것이다. 본문에서 `[제안]`을 붙인 곳이다. 사용자가 받아들이면 본문의 태그를 지운다. data-model.md, research.md의 원문을 옮기면서 따라온 `[제안]`은 이 표에 넣지 않았다. 그 태그는 원래 문서의 것이다.

| # | 고른 것 | 다른 선택지 | 고른 이유 | 작업 |
|---|---|---|---|---|
| 1 | Phase 0을 PR 두 개(0-A 테스트 케이스, 0-B 기준 문서)로 나눈다 | PR 하나로 묶기, 기준 문서 고치기를 P1 PR에 넣기 | 저장소 루트 `CLAUDE.md` 1절 "PR 하나에는 한 가지 일만 담는다". 0-A는 판정 기준 추가, 0-B는 기준 문서 정리라서 일이 다르다 | T001~T012 |
| 2 | 0-B를 P1 PR보다 먼저 병합하기를 권한다 | P1과 같은 때나 뒤에 병합 | 병합된 기준 문서(예: tech-stack.md의 재시도 행)와 P1 코드가 어긋난 채 main에 들어가지 않는다 | Phase 0 머리말 |
| 3 | 브랜치 이름 `docs/order-test-cases`, `docs/align-001-decisions` | 다른 영어 설명 | `docs/standards/git-workflow.md` 2절의 `<종류>/<설명>`, 2~4단어, 40자 이하 규칙을 지킨다 | Phase 0 |
| 4 | 받치는 테스트(단위·Mapper)의 값도 테스트 케이스의 값을 쓴다 | 테스트마다 임의 값 | 저장소 루트 `CLAUDE.md` 7절이 기대값을 판정 기준 문서에서 가져오라고 한다. 같은 값을 쓰면 인수 시나리오와 이어진다 | 머리말, T059, T063 등 |
| 5 | 주문의 주인을 보는 테스트(T101)에서 위조 값을 본문 필드 `customerId`와 헤더 `X-Customer-Id`에 넣는다. 파일 이름은 `CustomerIdentityIntegrationTest` | 다른 필드·헤더 이름, 둘 중 하나만 넣기 | 계약에 없는 본문 필드와 흔히 쓰는 사용자 헤더를 모두 막는지 한 번에 본다 | T101 |
| 6 | "0.8초 이상 1.5초 이하"를 1/5로 계산한 0.16초 이상 0.3초 이하를 표에 적는다 | 구현할 때 계산 | research.md 4절의 규칙을 그대로 곱한 값이다. 미리 적어 두면 구현할 때 계산 실수가 없다 | 머리말, T160 |
| 7 | Gradle 9.8.0 배포본을 scratchpad에 받아 wrapper를 한 번 만든다 | 사용자가 Gradle을 설치한다, wrapper 파일을 손으로 쓴다 | 이 PC에 `gradle` 명령이 없다. 저장소 밖 임시 폴더라 저장소에 남는 것이 없다 | T014 |
| 8 | `rootProject.name = 'msa-example'` | 다른 이름 | 저장소 이름과 같다 | T015 |
| 9 | `services:order-service`를 P2에서 `include`한다 | P1에 미리 넣는다 | research.md 결정 4 "기능이 없는 빈 모듈을 미리 만들지 않는다" | T015, T083 |
| 10 | 버전 카탈로그에 단계마다 그 단계가 쓰는 것만 더한다 | P1에 전부 넣는다 | 쓰지 않는 의존성을 미리 두지 않는다(저장소 루트 `CLAUDE.md` 5절 "요청받은 것만 만든다") | T017, T084, T157 |
| 11 | 단계 표의 서비스 칸 값은 `inventory`, `order` | `재고`/`주문`, `inventory-service` | 프로젝트 이름에서 바로 얻을 수 있고 영어 소문자라 CSV에서 다루기 쉽다 | T023, T029 |
| 12 | 500 응답 테스트(T140)의 파일 이름 `InventoryServerErrorIntegrationTest`, 스텁 본문은 재고 계약의 500 Problem Details(`code` = `INTERNAL_ERROR`) | 409 테스트 파일에 메서드로 더하기 | 시나리오 7.2의 인수 테스트 파일에 인수 시나리오가 아닌 테스트를 섞지 않는다 | T140 |
| 13 | CI 세부: `ubuntu-latest`, Temurin 17, `gradle/actions/setup-gradle`, 러너의 Docker, Actions 버전은 조회해 고정 | 다른 러너, 다른 JDK 배포판 | research.md 결정 5의 `[제안]` 세부를 그대로 옮겼다 | T022 |
| 14 | CI에서 `chmod +x ./gradlew` 단계를 둔다 | 커밋 전에 `git update-index --chmod=+x gradlew` | Windows에서 만든 파일은 실행 권한 없이 올라갈 수 있다. git 작업 없이 CI 설정만으로 해결한다 | T022 |
| 15 | Java 17 컴파일은 `options.release = 17` | Gradle 툴체인(`java.toolchain`) | 이 PC의 JDK 17.0.2를 그대로 쓰고, 툴체인이 JDK를 내려받을 일이 없다 | T024 |
| 16 | 계약 폴더 경로를 시스템 속성 `msa.contractsDir`로 넘긴다 | 테스트 코드에 상대 경로를 적는다, 계약 파일을 서비스로 복사한다 | 두 서비스가 같은 방법으로 루트 `contracts/`를 읽고, 경로가 convention plugin 한곳에 있다 | T024, T040, T094 |
| 17 | `${}` 예외의 `marker` 칸 값은 `${` | `$`, `${}` | 검사가 찾는 글자 그대로다 | T027 |
| 18 | 테스트 케이스 ID 검사는 서비스마다 자기 서비스 줄만 본다 | 루트에서 한 번 모든 줄을 본다 | 루트 `build.gradle`을 두지 않으므로(plan.md) 검사는 서비스 프로젝트에 붙는다 | T029 |
| 19 | `libs/archunit-rules`에는 `java-library`와 Spotless만 적용한다 | `msa.java-service` 적용, 라이브러리용 convention plugin 추가 | `msa.java-service`는 Spring Boot 플러그인이 붙어 라이브러리에 맞지 않고, 새 plugin은 plan 트리에 없다. 대신 이 라이브러리에는 SpotBugs와 검사 표시 검사가 돌지 않는다 | T030 |
| 20 | Oracle JDBC는 `ojdbc17` | `ojdbc11` | Spring Boot가 관리하는 23.26 계열에서 JDK 17 이상용 jar다 | T031 |
| 21 | DB 환경변수 `INVENTORY_DB_URL`, `INVENTORY_DB_USERNAME`, `ORDER_DB_URL`, `ORDER_DB_USERNAME`과 로컬 기본값 | Spring 기본 이름(`SPRING_DATASOURCE_URL`) | 비밀번호 환경변수가 이미 `.env.example`에 서비스별 이름으로 있어 같은 방식으로 맞췄다. 기본 주소는 `infra/compose/compose.yaml`의 호스트 포트다 | T033, T087 |
| 22 | OTLP 주소 환경변수 `OTLP_ENDPOINT` | OpenTelemetry 표준 이름 `OTEL_EXPORTER_OTLP_ENDPOINT` | research.md 4절이 "환경변수(기본 `http://lgtm:4318`)"로만 적어서 짧은 이름을 골랐다. 표준 이름이 낫다고 보면 바꾼다 | T033 |
| 23 | appender 연결 코드는 `config/ObservabilityConfig`에 둔다 | 자동 설정에 맡긴다 | Spring Boot 4.1.1이 자동으로 하는지 확인하지 못했다. 자동이면 만들지 않는다 | T034, T088 |
| 24 | 테스트 설정은 `application-test.yml`과 `@ActiveProfiles("test")` | `src/test/resources/application.yml` | 같은 이름의 파일은 주 설정을 통째로 가린다 | T035, T089 |
| 25 | 테스트용 Oracle 컨테이너 클래스는 테스트 최상위 패키지에 둔다 | `support` 패키지를 새로 만든다 | plan 트리에 없는 패키지를 만들지 않는다 | T039, T092 |
| 26 | `RESERVATION_ITEMS.RESULT`, `ORDER_ITEMS.REJECT_REASON`에는 값 목록 CHECK를 두지 않는다 | 둔다 | data-model.md의 제약 목록에 없다 | T037, T091 |
| 27 | 재고 서비스 통합 테스트는 `RANDOM_PORT`와 `RestClient`로 실제 HTTP를 보낸다 | MockMvc | 헌법 "실제 조건에서 테스트 먼저"의 이유("실제 DB와 실제 HTTP에서만 제대로 확인된다"). 동시 요청이 실제 Tomcat 스레드로 들어간다 | User Story 1 머리 |
| 28 | 주문 서비스 통합 테스트는 MockMvc와 `jwt()` | `RANDOM_PORT`와 테스트용 `JwtDecoder` | `docs/standards/testing.md` 3절 "인증"이 Spring Security Test의 JWT 지원을 쓰라고 한다 | Phase 2 머리 |
| 29 | 시나리오 1.1의 본 흐름과 변형 두 개를 메서드 세 개로 나눈다 | 메서드 하나 | 어느 부분이 실패했는지 바로 보인다 | T042 |
| 30 | ㉠·㉡·㉥은 `[US1]`, ㉩은 `[US2]`, ㉦은 `[US4]`, ㉧은 `[US5]`, ㉨은 `[US6]`, ㉢은 `[US8]`, ㉣은 `[US9]` | 라벨을 붙이지 않는다 | 아홉 케이스는 spec 스토리에 없지만, 다루는 동작(해제, 재시도, 실패 주문의 해제)이 속한 스토리로 붙였다 | T047, T048, T161, T170 |
| 31 | 예약 요청 항목은 `ReserveRequest` 안쪽 record `Item` | 별도 파일 | data-model.md 7절에 항목 DTO 이름이 없고, 요청 안에서만 쓴다 | T062 |
| 32 | 해제 응답 `ReleaseResponse`를 따로 둔다 | `ReservationResponse`를 같이 쓴다 | 계약에서 `ReleaseResult`와 `ReservationResult`의 필드가 다르다 | T062 |
| 33 | 수량 복구 SQL은 감소 SQL과 같은 모양(`QUANTITY + #{quantity}`) | 다른 모양 | data-model.md 6-3절에 SQL 문장이 없다 | T066 |
| 34 | 수량 복구 Mapper 테스트를 `StockRestoreMapperTest`로 따로 둔다 | `StockMapperTest`에 메서드를 더한다 | User Story 9의 작업이 User Story 1의 파일을 고치지 않는다 | T065 |
| 35 | 테스트 케이스 문서 공통 전제에 더할 두 줄의 문장(성공 응답 코드, 401의 Problem Details `code`) | 공통 전제 대신 각 테스트 케이스에 줄을 더하기 | 여러 케이스에 걸친 값이라 공통 전제의 기존 "오류 응답 코드" 줄과 같은 자리에 둔다 | T001 |
| 36 | (2026-10-07 사용자 승인으로 바뀜) 재고 서비스에도 트랜잭션 안 원격 호출 규칙을 적용하고 그 규칙에만 `allowEmptyShould(true)`를 둔다 | 적용하지 않는다 | 재고 서비스에 `client` 패키지가 생기면 규칙이 바로 검사한다 | T076 |
| 37 | 401 처리 클래스 `ProblemAuthenticationEntryPoint`를 `exception/`에 둔다 | `SecurityConfig` 안의 람다 | Problem Details 변환을 `exception/`에 모은 plan 트리와 맞춘다 | T124 |
| 38 | WireMock 도우미를 `integration/InventoryWireMock.java`에 둔다 | 다른 패키지 | 서비스 통합 테스트만 쓴다 | T093 |
| 39 | 계약에 없는 응답(502·503·504, 지연)의 스텁 응답은 계약 검증에서 빼고 받은 요청만 검증한다 | 계약에 503을 더한다, 검증 실패를 허용한다 | 이 응답은 재고 앱이 아니라 네트워크와 인프라 오류를 흉내 낸 것이다. 계약을 바꾸는 일은 사용자가 정한다 | T094, T141, T162 |
| 40 | 시나리오 5.1의 다섯 경우를 매개변수 테스트 하나로 돌린다 | 메서드 다섯 개 | 같은 검사를 다섯 번 되풀이하지 않는다 | T126 |
| 41 | 시나리오 6.1의 "처리 중" 경우는 스텁 지연을 시도당 제한 시간보다 짧게 둔다 | 길게 둔다 | 길면 첫 요청이 시간 초과로 실패해 보려는 동작과 섞인다 | T132 |
| 42 | 시나리오 7.1 테스트는 주문마다 시도 수와 서킷이 열리는 주문 순번을 단언하지 않는다 | P2 동작에 맞춰 자세히 단언한다 | P3에서 재시도가 들어와도 테스트를 고치지 않고 통과해야 한다(research.md 결정 2) | T138 |
| 43 | 비동기 해제 결과는 저장된 주문을 다시 읽으며 기다리고, 안전장치 한도를 10초로 둔다 | 고정 시간만큼 잠든다 | 기다리는 시간이 테스트 케이스에 없어 단언하지 않고, 테스트가 끝없이 돌지 않게만 막는다 | T138 |
| 44 | 기본값 단위 테스트를 `config/` 테스트 패키지에 둔다 | `domain/` 테스트 패키지 | 검사 대상 클래스가 `config` 패키지에 있다 | T142 |
| 45 | 연결 시간 포함 확인 테스트를 `integration/InventoryClientTimeoutIntegrationTest.java`에 두고, 응답하지 않는 주소를 만드는 방법은 구현할 때 고른다 | 별도 패키지, 지금 방법을 정한다 | 환경마다 동작이 달라 실제로 돌려 보고 골라야 한다 | T148 |
| 46 | 시나리오 8.1의 간격은 WireMock이 요청을 받은 시각의 차이로 잰다 | 주문 서비스 안에서 응답 시각을 기록한다 | 503 스텁에 지연이 없어 응답 시각과 다음 요청 시각의 차이가 작고, 운영 코드에 테스트용 기록을 넣지 않는다 | T160 |
| 47 | 시나리오 8.1의 재고 쪽은 같은 예약 요청을 세 번 보낸다 | 한 번 보낸다 | 주문 쪽이 같은 요청을 세 번 보내는 상황과 같게 둔다 | T055 |
| 48 | 대기 일정 클래스와 그 단위 테스트를 `client` 패키지에 둔다 | `domain` 패키지 | 대기 일정은 업무 규칙이 아니라 재고 호출의 기술 설정이다 | T163, T165 |
| 49 | 30초 한도 판단 테스트는 주입한 시계로 시각을 정한다 | 실제 시간을 기다린다 | 빠르고 흔들리지 않는다 | T172 |
| 50 | 시나리오 9.1 주문 쪽의 실패 확인은 테스트 설정의 시도당 제한 시간을 잠시 1초로 늘려서 본다 | 실패 확인을 건너뛴다 | 이 테스트는 User Story 8의 재시도만으로 통과할 수 있다. 시도당 제한 시간이 빠지면 이 테스트가 잡는다는 것을 보여 준다 | T173 |
| 51 | 환경변수 `JWT_ISSUER_URI`, `INVENTORY_BASE_URL`과 그 기본값 | 다른 이름 | 기본값은 data-model.md 5-1절의 발급자와 research.md 4절의 재고 주소다 | T087 |
| 52 | 진행 중인 시도를 전체 한도에서 끊는 구체적인 방법은 구현할 때 고른다 | 지금 정한다 | `JdkClientHttpRequestFactory`의 제한 시간은 팩토리에 걸려 있어, 시도마다 바꾸는 방법은 코드를 써 보고 골라야 한다 | T174 |
| 53 | "재시도하지 않는다" 테스트의 실패 확인은 재시도 판단에 REJECTED, 500, 409를 하나씩 잠시 더하는 방식으로 한다 | 테스트 쪽 스텁을 바꾼다 | 테스트를 고치지 않고 구현만 잠시 바꿔야 "이 테스트가 결함을 잡는다"를 보여 줄 수 있다 | T167 |

---

## 사용자 확인 필요

아래는 이번 실행에서 하지 않았다. 기준 문서나 spec, plan, research를 고치거나, 판정 기준 문서에 없는 기대값을 정하는 일이라서다. 헌법의 "반드시" 규칙을 어기게 되는 선택은 없었다. "(2026-10-06 정함)"이나 "(2026-10-07 정함)"이 붙은 항목은 /speckit-analyze 뒤에 사용자가 정했고, 정한 내용을 본문에 반영했다. 아직 열려 있는 것은 3번(spec 태그 지우기의 승인)과 10번(구현 중에 생길 수 있는 결정)이다.

1. **(2026-10-07 끝남) Phase 0의 모든 작업.** 0-A는 PR #17, 0-B는 PR #18로 병합했다.
2. **(2026-10-06 정함) research.md 5절 표에 없는 기준 문서 어긋남 두 가지.** 둘 다 0-B에서 고친다. 코딩 규약 3-8절 두 행은 T009, 테스트 전략 2절 "단위" 행은 T007이다.
3. **spec.md의 `[제안]` 태그 11개 지우기**(T013, Phase 1의 첫 작업). research.md 결정 1에 따라 무엇을 지울지 보여 주고 승인받은 뒤 지운다.
4. **(2026-10-06 정함) 주문 상태 전이의 단위 테스트.** 둔다. 기대값은 도메인 분석 4절의 그림에서 가져온다(T095).
5. **(2026-10-06 정함) 받치는 테스트의 기대값 출처.** 판정 기준 문서와, 사용자가 확정한 `docs/design/`·`docs/analysis/` 문서에서 가져온다. `specs/` 아래 문서에만 있는 값은 단언하지 않는다. `CLAUDE.md` 7절에 예외로 적는다(T005, T010). 확인해 보니 T063, T064, 주문 Mapper 테스트가 보는 동작(행 잠금, 주문 번호 고유 제약, 고객·키 고유 제약)은 설계 6절과 도메인 분석 6·7절에도 있었다. data-model.md에만 있던 "해제 표식은 항목 없는 기록"이라는 표현만 단언에서 뺐다(T059, T064).
6. **(2026-10-06 정함) 서킷 브레이커 기본값의 단위 테스트**(T142). 설계 4절 표의 숫자를 기대값으로 쓴다. 설계 문서는 사용자가 확정한 문서다.
7. **(2026-10-06 정함) spec Edge Cases의 인수 테스트.** 받아들인 제안 9번(잘못된 토큰), 11번(형식이 틀린 항목), 7·8번(요청 키의 두 경우)은 추가 케이스 ㉦, ㉧, ㉨로 0-A에서 테스트 케이스 문서에 더하고 Phase 2에 테스트를 둔다(T122, T128, T133). 10번(관리자 역할의 주문)과 그 밖의 Edge Cases는 테스트를 두지 않는다.
8. **(2026-10-07 정함) 추적 정보(`traceparent`) 전달의 테스트.** 추가 케이스 ㉩로 0-A에서 테스트 케이스 문서에 더하고, Phase 2에 서비스 통합 테스트(T102)를 둔다.
9. **(2026-10-07 정함) plan.md "Source Code" 트리에 없던 경로.** plan.md 트리에 더했다. 파일·패키지 이름은 tasks.md가 고른 것이라 트리에도 `[제안]`으로 표시했다.
10. **구현 중에 생길 수 있는 결정.** 아래 경우가 생기면 작업이 멈추고 사용자에게 묻도록 적었다.
    - research.md 6절의 확인이 실패해 plan의 결정을 바꿔야 할 때(T041, T069, T112, T148, T149)
    - 시나리오 9.1의 주문 쪽 테스트가 1/5로 줄인 시간의 여유(0.2초) 때문에 CI에서 흔들릴 때(T169, T178). 기대값이나 줄이는 비율을 바꾸는 일이다.
    - P2 테스트가 P3 구현과 맞지 않아 보일 때(T168)

11. **(2026-10-06 정함) 예약된 재고에 해제가 두 번 오는 경우의 테스트.** 추가 케이스 ㉥로 0-A에서 테스트 케이스 문서에 더하고, Phase 1에 서비스 통합 테스트(T049)를 둔다.
12. **(2026-10-07 정함) 재고 서비스 아키텍처 테스트의 규칙 예외**(T076). 재고 서비스에도 "트랜잭션 안 원격 호출" 규칙을 적용하고, 그 규칙에만 `allowEmptyShould(true)`를 둔다.
---

## 용어

- **단계 (phase)**: 이 문서에서 PR 하나에 대응하는 작업 묶음이다. Phase 0은 선행 문서 작업이고, Phase 1·2·3은 plan.md의 P1·P2·P3 단계 PR이다.
- **단계 표시 (`msa.stage`)**: 지금 PR이 어느 단계인지 적는 `gradle.properties`의 속성이다. 테스트 케이스 ID 검사가 이 값보다 높은 단계의 줄은 검사하지 않는다.
- **체크포인트 (checkpoint)**: 스토리 절 끝에서 그때까지 만든 것이 모두 도는지 멈춰 보는 지점이다. 이 문서에서는 스토리마다 하나씩 둔다.
- **받치는 테스트 (supporting test)**: 인수 시나리오 테스트를 아래에서 받치는 단위·Mapper·계약 테스트다. 테스트 케이스 ID를 붙이지 않는다.
- **되돌려 확인하기 (revert-to-fail check)**: 이미 통과하는 테스트가 결함을 잡는지 보려고 구현 한 줄을 잠시 망가뜨려 실패를 보고 되돌리는 확인이다. 재시도가 생기기 전에는 실패할 수 없는 "재시도하지 않는다" 테스트에 쓴다(T167).
- **Gradle 배포본 (Gradle distribution)**: Gradle 프로그램을 압축한 zip 파일이다. 이 PC에 Gradle이 없어서 wrapper를 처음 만들 때 한 번만 임시로 받는다.
- **실행 권한 비트 (executable bit)**: 리눅스에서 파일을 프로그램으로 실행해도 되는지 표시하는 값이다. Windows에서 만든 `gradlew`에는 없을 수 있어 CI에서 켠다.
- **툴체인 (toolchain)**: Gradle이 빌드에 쓸 JDK를 골라 주고, 없으면 내려받는 기능이다. 이 문서에서는 쓰지 않고 `options.release = 17`로 Java 17 컴파일을 정한다.
- **시스템 속성 (system property)**: Java 프로그램에 실행할 때 넘기는 이름과 값의 쌍이다. 테스트에 계약 폴더 경로(`msa.contractsDir`)를 넘기는 데 쓴다.
- **`RANDOM_PORT`**: `@SpringBootTest`가 앱을 빈 포트에서 실제 웹 서버로 띄우게 하는 설정이다. 재고 서비스 통합 테스트가 실제 HTTP 요청을 보내려고 쓴다.
- **MockMvc**: 실제 웹 서버 없이 Spring MVC에 요청을 넣어 응답을 받는 테스트 도구다. 주문 서비스 통합 테스트가 Spring Security Test의 `jwt()`와 함께 쓴다.
- **`jwt()`**: Spring Security Test가 주는 기능으로, 테스트 요청에 검증된 JWT가 있는 것처럼 꾸민다. Keycloak 없이 고객 C1, C2를 흉내 낸다.
- **`CountDownLatch`**: 여러 스레드가 신호를 기다렸다가 한꺼번에 출발하게 하는 Java 도구다. 동시 예약 테스트에서 요청을 동시에 보낼 때 쓴다.
- **`JdbcTemplate`**: SQL을 짧은 코드로 실행하는 Spring 도구다. 테스트가 상품 행을 넣고 지우거나 저장된 주문을 직접 읽을 때 쓴다.
- **WireMock 시나리오 (WireMock scenario)**: 같은 요청에 몇 번째로 왔는지에 따라 다른 응답을 돌려주게 하는 WireMock 기능이다. "처음 두 번은 503, 셋째부터 정상" 스텁을 만든다.
- **매개변수 테스트 (parameterized test)**: 같은 테스트를 입력만 바꿔 여러 번 돌리는 JUnit 기능이다. 잘못된 주문 항목 다섯 가지를 한 테스트로 돌린다.
- **주입한 시계 (injected clock)**: 지금 시각을 직접 읽지 않고 밖에서 넣어 준 `Clock` 객체로 읽게 만든 것이다. 30초 한도 판단을 기다리지 않고 단위 테스트한다.
- **안전장치 한도 (safety timeout)**: 테스트가 끝없이 기다리지 않게 두는 최대 시간이다. 기대값이 아니라서 단언하지 않는다.
- **소비자 계약 테스트 / 제공자 계약 테스트 (consumer / provider contract test)**: 계약을 부르는 쪽(주문 서비스)과 내주는 쪽(재고 서비스)이 각자 `contracts/inventory-api.yaml`에 맞는지 확인하는 테스트다. 서비스마다 자기 검증 도우미를 둔다.
- **스텁 (stub)**: 테스트에서 상대 서비스 대신 정해진 응답을 돌려주도록 정한 것이다. 이 문서에서는 WireMock 스텁을 뜻한다.
- **해제 표식 (release marker)**: 예약 기록이 없는데 해제 요청이 먼저 왔을 때 남기는, 항목 없는 RELEASED 예약 기록이다. 늦게 온 예약 요청이 재고를 줄이지 못하게 막는다.
- **호출 문맥 (call context)**: 재고 호출 한 번(재시도 포함)마다 만드는 객체로, 시작 시각과 실제로 보낸 시도 수를 든다. 30초 한도와 해제가 필요한지를 판단한다.
- **해제 실행기 (release executor)**: 실패한 주문의 예약 해제를 고객 응답과 따로 돌리는 스레드 풀이다. 종료할 때 실행 중인 해제를 기다린다.
- **허용 목록 (allowlist)**: 검사의 예외로 사용자가 승인한 것만 적어 둔 파일이다. `config/quality/suppression-allowlist.csv`에 검사를 끄는 표시와 `${}` 예외를 적는다.
- **테스트 케이스 단계 표 (test-case stage table)**: 테스트 케이스마다 어느 서비스가 어느 단계에서 테스트를 갖춰야 하는지 적은 `config/quality/test-case-stages.csv`다. 테스트 케이스 ID 검사가 읽는다.
- **convention plugin**: 여러 하위 프로젝트에 같은 빌드 설정을 적용하는 Gradle 플러그인이다. `msa.java-service`가 두 서비스에 Java 버전, Spotless, SpotBugs, 기계 검사를 적용한다.
- **포함 빌드 (included build)**: 루트 `settings.gradle`의 `includeBuild`로 묶는, 따로 빌드되는 Gradle 프로젝트다. `build-logic/`이 포함 빌드다.
- **버전 카탈로그 (version catalog)**: 의존성 버전을 `gradle/libs.versions.toml` 한 파일에 모아 두는 Gradle 기능이다.
- **BOM (Bill of Materials)**: 서로 맞는 라이브러리 버전 묶음을 적은 파일이다. Spring Boot BOM을 `platform(...)`으로 가져와 관리 대상 라이브러리의 버전을 따로 적지 않는다.
- **Problem Details**: HTTP API의 오류 응답을 JSON으로 적는 표준 형식(RFC 9457)이다. 두 서비스의 모든 오류 응답이 이 형식이고, `code`와 `errors` 필드를 더한다.
