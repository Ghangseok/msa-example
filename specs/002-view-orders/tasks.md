---

description: "주문 조회(티켓 002)의 작업 목록. 단계(Phase)마다 PR 하나다"
---

# Tasks: 주문 조회

**Input**: `specs/002-view-orders/`의 [spec.md](spec.md), [plan.md](plan.md), [research.md](research.md), [data-model.md](data-model.md), [contracts/](contracts/), [quickstart.md](quickstart.md)

**함께 따른 규칙**: 저장소 루트 `CLAUDE.md`, `.specify/memory/constitution.md`, `docs/standards/`의 다섯 문서(`architecture-rules.md`, `tech-stack.md`, `testing.md`, `coding-conventions.md`, `git-workflow.md`)

**작성일**: 2026-10-13

## 이 문서를 읽는 법

### 한 번에 한 단계만 한다

- **`/speckit-implement`는 한 번에 한 단계만 한다.** 단계 하나가 PR 하나다. 단계의 마지막 작업은 "사용자에게 보고하고 멈춘다"이고, 사용자가 그 단계의 PR을 병합한 뒤 다음 단계를 시작한다.
- 이 문서는 spec-kit 기본 구조(Setup → Foundational → 스토리마다 한 단계 → Polish) 대신 PR 경계로 단계를 나눈다. `docs/standards/git-workflow.md` 2절 "브랜치"가 단계별 PR 브랜치 이름에 tasks.md의 단계 번호를 붙이게 정했기 때문이다(예: `feat/002-view-orders-p1`). 경계는 사용자가 2026-10-11에 정한 Y안(PR 2개)이고, 근거는 plan.md "PR 단계" 절이다.
- 커밋, push, PR 만들기는 작업으로 넣지 않았다. 저장소 루트 `CLAUDE.md` 1절에 따라 사용자가 요청할 때 한다. 브랜치를 만들거나 바꾸는 일도 사용자가 한다.
- 001의 Phase 0 같은 선행 docs 단계는 없다. 선행 docs 작업(PR #29, #30, #31, #32)이 모두 main에 병합됐다.

| 단계 | 단계 이름 `[제안]` | 브랜치 `[제안]` | 담는 것 | 사용자 스토리 |
|---|---|---|---|---|
| Phase 1 | `T002-P1` | `feat/002-view-orders-p1` | 설정 세 곳 변경, 테스트 도우미, 고객의 주문 하나 조회와 목록 조회, 로그인 확인, 쪽 지정, 조회 실패 응답 | User Story 1, 2, 3 (시나리오 12개) |
| Phase 2 | `T002-P2` | `feat/002-view-orders-p2` | `/admin/**` 규칙, 역할 변환기, 403 처리기, 관리자 조회와 `releaseStatus` | User Story 4 (시나리오 4개) |

단계 이름과 브랜치 이름은 사용자가 정하지 않았다. plan.md가 제안한 이름을 그대로 썼다.

### 단계 안의 순서

단계마다 아래 순서로 나눈다. 따로 "Polish" 단계를 두지 않는다. Phase 2에는 기반 작업이 없어서 "기반" 절이 없다 `[제안]`.

1. **준비**: 설정 변경과 단계 표시(`msa.stage`)
2. **기반**: 모든 스토리가 기대는 테스트 도우미
3. **스토리마다 한 절**: 인수 시나리오 테스트 → 테스트가 실패하는 것을 확인 → 받치는 테스트와 구현 → 통과 확인
4. **마무리와 확인**: 아키텍처 테스트, 서비스 규칙 문서, 기계 검사와 GitHub Actions, 시나리오 대조, 확인하지 못한 것 처리, 보고

### 작업 줄의 형식

```text
- [ ] T### [P] [US#] 설명 (파일 경로)
```

- **`T###`**: 작업 번호(`T001`부터). 단계를 넘어 이어 붙인다.
- **`[P]`**: 같은 절의 다른 `[P]` 작업과 동시에 해도 되는 작업이다. 서로 다른 파일을 고치고, 아직 끝나지 않은 작업에 기대지 않는다.
- **`[US#]`**: spec.md의 User Story 번호다(예: `[US1]`). 준비, 기반, 마무리 작업에는 붙이지 않는다.
- **파일 경로**: 줄 끝 괄호 안에 저장소 루트 기준 경로를 적는다. 경로는 plan.md "Source Code" 트리를 따른다. 트리에 없는 경로나 이름을 고른 곳에는 `[제안]`을 붙였다.
- 작업 줄 아래의 들여 쓴 줄은 그 작업의 세부 내용이다. 작업이 아니다.
- **`사용자 확인 필요`**가 붙은 작업은 무엇을 왜 고칠지 먼저 사용자에게 보여 주고 답을 받은 뒤에 고친다(저장소 루트 `CLAUDE.md` 1절). 답을 받기 전에는 그 작업과 그 작업에 기대는 작업을 하지 않는다.

### 테스트는 필수다

- 헌법 "실제 조건에서 테스트 먼저" 원칙과 덮어쓴 tasks 템플릿(`.specify/templates/overrides/tasks-template.md`)에 따라 테스트는 필수다. spec-kit 안내의 "Tests are OPTIONAL"보다 이것이 우선한다.
- 스토리마다 인수 시나리오 테스트 작업을 구현 작업보다 앞에 둔다. 테스트 작업 바로 다음에 "테스트가 실패하는 것을 확인한다" 작업을 둔다.
- **인수 시나리오 테스트의 `@DisplayName`은 테스트 케이스 ID로 시작한다** (`docs/standards/testing.md` 3절 "TC 연결"). 단위·Mapper 테스트와 plan.md가 "받치는 테스트"로 부른 테스트는 인수 시나리오를 직접 검증하지 않아서 ID를 붙이지 않는다.
- **기대값은 `docs/test-cases/order-placement.md`에서 그대로 가져온다.** 이 문서의 상태는 "확정"이다. 테스트가 실패하면 구현을 고친다. 기대값을 바꾸거나 테스트를 지우거나 건너뛰게 만들지 않는다. 기대값이 틀렸다고 판단되면 멈추고 사용자에게 묻는다(저장소 루트 `CLAUDE.md` 7절).
- **문서에 없는 기대값은 만들지 않는다.** plan이 정한 동작 가운데 테스트 케이스 문서에 아직 없는 것은 단언하지 않는다. 목록은 맨 끝 "사용자 확인 필요" 4번에 있다. research.md 3절의 후보 5(관리자 응답의 고객 ID)와 후보 6(쿼리 값의 대소문자)이 여기에 든다.
- **받치는 테스트의 기대값 출처**: 판정 기준 문서(`docs/test-cases/`, `contracts/`, `docs/requirements/`)에 더해, 사용자가 확정한 `docs/design/`과 `docs/analysis/` 문서에서도 가져온다. `specs/` 아래 문서(research.md, data-model.md 등)에만 있는 값은 단언하지 않는다(저장소 루트 `CLAUDE.md` 7절).
- **실패 확인 작업에서 실패를 보지 못하면 다음 작업으로 넘어가지 않는다.** 멈추고 사용자에게 묻는다. 실패의 종류도 본다. API가 없어서 생긴 실패(404, 405, 응답 본문에 `code` 없음)가 맞다. 컴파일 오류나 앱이 뜨지 못하는 오류는 맞지 않으니, 그때도 멈추고 보고한다.
- **미리 정한 예외 한 가지** (사용자 결정 2026-10-13): 시나리오 3.1(토큰 없는 조회는 401)은 구현 전에도 통과한다. 기존 `SecurityConfig`의 `anyRequest().authenticated()`가 조회 세 경로를 이미 막기 때문이다. 이 테스트는 구현을 잠시 망가뜨려 실패를 보고 되돌린다(Phase 1의 T041). 저장소 루트 `CLAUDE.md` 6절의 "고친 부분을 잠시 되돌려 테스트가 실패하는 것을 보고" 방식이다. 001 tasks.md의 T167이 같은 방식을 썼다.
- **받치는 테스트는 컴파일 오류로 실패해도 된다.** 아직 없는 클래스를 참조하기 때문이다. 단, 인수 시나리오 테스트의 실패 확인을 마친 뒤에 쓴다. 같은 테스트 소스 묶음에 컴파일 오류가 있으면 인수 시나리오 테스트가 돌지 못한다. 이 실패는 `.\gradlew.bat :services:order-service:compileTestJava`의 종료 코드가 0이 아니고, 오류 메시지가 아직 없는 클래스나 메서드를 가리키는 것으로 본다 `[제안]`.

### 이 티켓이 001과 다른 점

- 시간을 줄이는 설정이 없다. 이 티켓에는 제한 시간, 재시도 대기, 서킷 열림 시간이 없다. 001의 "시간 값은 1/5로 줄인다" 절을 가져오지 않았다.
- WireMock이 없다. 조회는 재고 서비스를 부르지 않는다. 서비스 통합 테스트는 `InventoryWireMock`이 아니라 `IntegrationTestSupport`를 상속한 `ViewTestSupport`를 쓴다.
- 재시도, 서킷 브레이커, 소비자·제공자 계약 테스트가 없다.
- "주문이 있다"는 상태는 주문 생성 API가 아니라 `OrderMapper`로 직접 저장해서 만든다(research.md 항목 12). 주문 번호는 시퀀스가 정하므로, 테스트 케이스 문서의 "주문 1004" 같은 번호는 테스트에서 저장 결과의 번호로 대신한다.
- 새 Flyway 파일, 새 인덱스, 새 라이브러리가 없다. 루트 `contracts/`도 바꾸지 않는다. 조회 API의 계약은 `specs/002-view-orders/contracts/order-query-api.yaml`에 있다.

### 검사의 한계

시나리오 1.1이 쓰는 테스트 케이스 번호와 시나리오 1.2, 2.1, 4.1, 4.2가 쓰는 테스트 케이스 번호는 001의 테스트가 이미 그 번호로 `@DisplayName`을 시작한다. 그래서 테스트 케이스 ID 기계 검사가 이 티켓의 테스트가 있는지 확인하지 못한다. plan.md의 표와 단계 끝의 시나리오 대조 작업(Phase 1의 T046, Phase 2의 T073)으로 확인한다(research.md 항목 13의 "검사의 한계").

### 항목 ID 표기

- 작업 줄에는 항목 ID를 쓰지 않았다. 테스트 케이스는 spec 시나리오 번호(시나리오 1.1)로, 업무 규칙은 문서 경로와 절로 가리킨다. 스토리는 spec 번호(User Story 1)로 가리킨다.
- `@DisplayName`에 넣을 테스트 케이스 ID는 각 스토리 절의 "이 절의 시나리오 원문"에서 가져온다. 그 블록의 인용 첫 줄에 ID와 spec 시나리오 번호가 함께 있다.
- 항목 ID를 꼭 적어야 하는 곳은 테스트 케이스 단계 표의 내용(CSV)뿐이다. 그 표는 바로 아래 "이 표에 나온 항목"에서 펼친다. 형식은 저장소 루트 `CLAUDE.md` 2절을 따른다.
- 아키텍처 규칙은 ID 대신 규칙 이름과 `docs/standards/architecture-rules.md`의 절로 가리킨다.
- `[제안]`은 사용자가 정하지 않았는데 Claude가 고른 것이다. 모두 맨 끝 "자동으로 고른 것" 절에 모았다. 사용자가 정해야 해서 하지 않은 일은 "사용자 확인 필요" 절에 모았다.

### 명령을 돌리기 전에

- 이 PC에서 `gradlew`를 돌리기 전에 `JAVA_HOME`을 JDK 17로 지정한다. 2026-10-07에 시스템 `JAVA_HOME`이 JDK 8인 것을 확인했다. 이 문서를 쓰는 지금은 다시 확인하지 않았다. PowerShell은 `$env:JAVA_HOME = 'C:\Program Files\Java\jdk-17.0.2'`다. 먼저 `java -version`과 `$env:JAVA_HOME`을 확인하고, 다르면 지정한다.
- 서비스 통합 테스트와 전체 `build`는 Testcontainers의 Oracle을 띄워서 메모리를 많이 쓴다. 로컬 인프라(`infra/`)가 켜져 있으면(`docker ps`) 내릴지 사용자에게 먼저 묻는다. 묻지 않고 내리지 않는다. 백그라운드 실행이 메모리 부족으로 멈추면 다시 시작하지 않고 보고한다.
- Docker나 Testcontainers에 문제가 생기면 `docs/references/docker-desktop.md`부터 본다.

---

## Phase 1: T002-P1 단계 PR — 고객 조회와 로그인 확인 (브랜치 `feat/002-view-orders-p1`)

**목적**: 로그인한 고객이 자기 주문 하나와 자기 주문 목록을 조회하고, 로그인하지 않은 사람은 조회하지 못한다(spec User Story 1, 2, 3). 목록은 주문 번호가 큰 순서로 나오고 쪽으로 나뉜다. 조회 실패 응답(404, 400)을 Problem Details로 낸다. 기계 검사를 위한 설정 세 곳을 고친다.

**담는 것** (plan.md "PR 단계" 표의 1단계 줄): 설정 세 곳(`config/quality/test-case-stages.csv`, `TestCaseIdCheck.groovy`, `gradle.properties`), 테스트 도우미 `ViewTestSupport`, `OrderQueryController`·`OrderQueryService`, `OrderMapper`의 조회 메서드와 SQL, 쪽 지정(`PageRequest`)과 DTO, 조회 실패 응답, 시나리오 1.1~1.5, 2.1~2.6, 3.1의 테스트, 주문 서비스 `CLAUDE.md`의 고객 조회 부분. `msa.stage=T002-P1`.

**끝났다고 보는 기준** (quickstart.md 3절의 1단계 줄): `.\gradlew.bat build` 종료 코드 0, 001의 테스트도 그대로 통과, 단계가 `T002-P1`인 줄마다 `@DisplayName`이 그 번호로 시작하는 테스트가 있음, GitHub Actions 성공

| 확인할 것 | 명령 | 기대 결과 |
|---|---|---|
| 단계 표시 | `Select-String -Path gradle.properties -Pattern '^msa.stage=T002-P1$'` | 한 줄 |
| 빌드와 모든 검사 | `.\gradlew.bat build` | 종료 코드 0. 001의 테스트가 그대로 통과한다 |
| 이 단계의 인수 시나리오 테스트 | `.\gradlew.bat :services:order-service:test --tests "com.example.msa.order.integration.ViewOrderIntegrationTest" --tests "com.example.msa.order.integration.ListOrdersIntegrationTest" --tests "com.example.msa.order.integration.UnauthenticatedViewIntegrationTest" --tests "com.example.msa.order.integration.OrderOwnerQueryIntegrationTest"` | 종료 코드 0. 시나리오 1.1~1.5, 2.1~2.6, 3.1의 테스트와 받치는 통합 테스트가 통과로 나온다 |
| 재고 서비스 변경 없음 | `git fetch origin` 뒤 `git diff origin/main --stat -- services/inventory-service` | 출력 없음. 이 단계는 재고 서비스를 고치지 않는다 |
| GitHub Actions | 사용자가 push한 뒤 `gh run list --branch feat/002-view-orders-p1 --limit 1` | 결론(conclusion)이 `success` |

**시작 조건**: 이 tasks.md를 담은 PR(브랜치 `docs/002-view-orders-tasks`)이 main에 병합되어 있고, 이 단계의 브랜치를 최신 main에서 만들었다. 브랜치를 만드는 일은 사용자가 한다.

- 확인 명령: `git branch --show-current` → `feat/002-view-orders-p1`
- 확인 명령: `Select-String -Path gradle.properties -Pattern '^msa.stage=P3$'` → 한 줄 (이 단계가 시작할 때 값이다)

### 준비 (설정 세 곳)

**⚠️ T001에서 사용자 승인을 받기 전에는 T002~T004를 하지 않는다.** 세 곳은 모두 기계 검사를 바꾸는 설정이다.

- [x] T001 설정 세 곳의 변경 내용을 사용자에게 보여 주고 승인을 받는다. 사용자 확인 필요 (파일 없음, 대화창 보고)
  - 보여 줄 것: 곳마다 지금 글, 고칠 글, 이유. 이유의 근거는 plan.md "PR 단계"의 "설정 변경" 항과 research.md 항목 13이다.
  - **테스트 케이스 단계 표**: 지금 단계가 `T002`인 줄 13개를 `T002-P1`과 `T002-P2`로 나눈다. 시나리오 1.2, 2.1(1단계)과 시나리오 4.1, 4.2(2단계)가 같은 테스트 케이스 번호를 쓰므로, 그 번호는 줄을 둘로 나눈다 `[제안]`. 표에는 같은 번호가 같은 서비스에 단계만 달리 두 줄 있는 전례가 이미 있다. 이유는 맨 끝 "자동으로 고른 것" 3번에 있다.
  - **단계 순서 목록(`STAGE_ORDER`)**: 지금 `['P1', 'P2', 'P3']`다. 뒤에 `'T002-P1'`, `'T002-P2'`를 더한다 `[제안]`. 순서를 001의 세 단계 뒤에 두는 이유는, 이 티켓의 단계에서도 001의 모든 테스트가 계속 있어야 하기 때문이다. 검사를 끄는 변경이 아니라 검사 범위를 넓히는 변경이다. 검사 논리(단계가 지금 단계 이하인 줄마다 테스트가 있는가)는 그대로다. 지금은 단계가 `T002`인 줄이 순서에 없어서 검사에서 건너뛴다. 이름이 순서에 들어가면 그 줄들이 검사 대상이 된다.
  - **`msa.stage`**: 지금 `P3`다. 1단계 PR에서는 `T002-P1`, 2단계 PR에서는 `T002-P2`로 올린다 `[제안]`. 001의 테스트는 계속 통과해야 한다. 단계가 `T002-P1`이어도 `P1`, `P2`, `P3` 줄은 모두 검사 대상이고, 그 줄의 테스트는 이미 있다.
  - **고치는 단계**: 세 곳 모두 1단계 PR에서 고친다. 2단계 PR에서는 `msa.stage`만 올린다(T050). 001이 단계 표의 모든 줄을 첫 단계에서 넣은 방식과 같고, 단계 표 검사의 첫째 항목(문서의 모든 테스트 케이스 ID가 표에 있는가)이 첫 단계부터 표 전체를 요구한다. plan.md도 단계 순서 목록을 1단계 PR에서 더하는 것이 자연스럽다고 제안했다.
  - 같이 물을 것: 코딩 규약 문서 3-8절 표의 "테스트 케이스 ID 검사" 행의 끝에서 둘째 문장이 단계 값 `T002`를 설명한다. 단계 이름이 바뀌면 그 문장이 맞지 않게 된다. 이 문서를 고치는 일은 이 PR에 넣지 않고 별도 docs PR로 한다. 고칠 문장은 맨 끝 "사용자 확인 필요" 2번에 있다.
  - 승인받은 내용과 다르게 고치지 않는다. 사용자가 이름이나 값을 바꾸면 이 문서의 해당 작업을 먼저 고친다.
- [x] T002 [P] 테스트 케이스 단계 표에서 단계가 `T002`인 줄 13개의 단계 값을 아래 표대로 바꾸고, 시나리오 4.1·4.2용 줄 하나를 더한다. 다른 줄은 한 글자도 바꾸지 않는다. 사용자 확인 필요 (config/quality/test-case-stages.csv)
  - 줄 순서는 바꾸지 않는다. 시나리오 1.2·2.1용 줄(`T002-P1`) 바로 아래에 시나리오 4.1·4.2용 줄(`T002-P2`)을 끼워 넣는다. 검사는 줄 순서를 보지 않는다.
  - 단계는 plan.md "인수 시나리오와 테스트 층" 표의 "단계 (Y안)" 칸을 따른다. 서비스 칸은 모두 `order`다.
  - 시나리오와 줄의 짝(머리 줄 `tc_id,service,stage`는 세지 않는다): 시나리오 1.1은 첫째 줄, 1.2·2.1은 둘째 줄, 4.1·4.2는 셋째 줄, 3.1은 넷째 줄, 1.3은 다섯째 줄, 4.3은 여섯째 줄, 2.3은 일곱째 줄, 2.2는 여덟째 줄, 2.5는 아홉째 줄, 1.4는 열째 줄, 4.4는 열한째 줄, 2.4는 열두째 줄, 1.5는 열셋째 줄, 2.6은 열넷째 줄이다.
  - 아래 블록은 이 티켓의 줄만 모아 보인 것이다. 실제 파일에서는 이 줄들이 떨어져 있다(시나리오 1.1, 1.2, 2.1, 4.1, 4.2용 기존 두 줄은 24~25줄, 나머지는 35~45줄). 파일의 줄 위치는 바꾸지 않는다.
  - 바꾼 뒤 이 티켓의 줄 14개:

    ```text
    tc_id,service,stage
    TC-002,order,T002-P1
    TC-010,order,T002-P1
    TC-010,order,T002-P2
    TC-025,order,T002-P1
    TC-026,order,T002-P1
    TC-027,order,T002-P2
    TC-028,order,T002-P1
    TC-029,order,T002-P1
    TC-030,order,T002-P1
    TC-031,order,T002-P1
    TC-032,order,T002-P2
    TC-033,order,T002-P1
    TC-034,order,T002-P1
    TC-035,order,T002-P1
    ```

**이 표에 나온 항목** (위 T002의 단계 표)

> **TC-002** · `docs/test-cases/order-placement.md` 절 "TC-002 항목 하나라도 재고가 부족하면 주문 전체가 거절되고 기록된다"
> - **Given** 상품 A의 재고가 10개, 상품 B의 재고가 1개다
> - **When** 고객이 상품 A 3개와 상품 B 2개를 한 주문으로 요청한다
> - **Then** 고객은 주문 번호와 "거절", 부족한 상품 B를 받는다
> - **And** 그 주문이 "거절" 상태로 기록되어 있고 C1이 조회할 수 있다
> - **And** 상품 A의 재고는 10개, 상품 B의 재고는 1개 그대로다 (A도 줄지 않는다)

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

> **TC-025** · `docs/test-cases/order-placement.md` 절 "TC-025 토큰이 없거나 서명이 틀리거나 만료된 토큰으로는 주문을 조회할 수 없다"
> - **When** 토큰 없이 주문 목록을 조회한다
> - **Then** 401을 받는다
> - **When** 토큰 없이 주문 하나를 조회한다
> - **Then** 401을 받는다
> - **When** 토큰 없이 관리자 조회를 한다
> - **Then** 401을 받는다
> - **When** 서명이 틀린 토큰으로 주문 목록을 조회한다
> - **Then** 401을 받는다
> - **When** 만료된 토큰으로 주문 목록을 조회한다
> - **Then** 401을 받는다

> **TC-026** · `docs/test-cases/order-placement.md` 절 "TC-026 없는 주문 번호와 숫자가 아닌 주문 번호는 "없음"이다"
> - **Given** 주문 9999는 없다
> - **When** C1이 주문 9999를 조회한다
> - **Then** "없음"(404)을 받는다. 다른 고객의 주문을 조회했을 때와 같은 응답이다
> - **When** C1이 주문 번호 `abc`로 조회한다
> - **Then** "없음"(404)을 받는다

> **TC-027** · `docs/test-cases/order-placement.md` 절 "TC-027 관리자 조회의 조건은 생략할 수 있고, 둘을 함께 주면 모두 맞아야 하며, 잘못된 값은 거부한다"
> - **Given** C1의 "확정" 주문, C1의 "실패" 주문(해제 결과 "해제 완료"), C2의 "실패" 주문(해제 결과 "해제 실패")이 있다
> - **When** M1이 조건 없이 관리자 조회를 한다
> - **Then** 세 주문이 모두 나온다
> - **When** M1이 상태 "실패"와 해제 결과 "해제 실패"를 함께 주고 관리자 조회를 한다
> - **Then** C2의 "실패" 주문만 나온다
> - **When** M1이 해제 결과 "해제 실패"만 주고 관리자 조회를 한다
> - **Then** C2의 "실패" 주문만 나온다
> - **When** M1이 없는 상태 이름으로 관리자 조회를 한다
> - **Then** 400을 받는다
> - **When** M1이 없는 해제 결과 값으로 관리자 조회를 한다
> - **Then** 400을 받는다

> **TC-028** · `docs/test-cases/order-placement.md` 절 "TC-028 주문이 하나도 없는 고객의 목록은 비어 있다"
> - **Given** C2의 주문이 하나도 없다
> - **When** C2가 자기 주문 목록을 조회한다
> - **Then** 오류 없이 빈 목록을 받는다

> **TC-029** · `docs/test-cases/order-placement.md` 절 "TC-029 고객의 목록에는 다른 고객의 주문이 섞이지 않는다"
> - **Given** C1에게 주문 1004가 있고, C2에게 주문 1005가 있다
> - **When** C1이 자기 주문 목록을 조회한다
> - **Then** 주문 1004만 나오고 주문 1005는 나오지 않는다
> - **When** C2가 자기 주문 목록을 조회한다
> - **Then** 주문 1005만 나오고 주문 1004는 나오지 않는다

> **TC-030** · `docs/test-cases/order-placement.md` 절 "TC-030 관리자 역할만 있는 사용자는 고객용 조회로 자기 주문만 본다"
> - **Given** M1의 주문 1006과 C1의 주문 1004가 있다
> - **When** M1이 자기 주문 목록을 조회한다
> - **Then** 주문 1006만 나온다
> - **When** M1이 주문 1006을 조회한다
> - **Then** 주문 1006을 받는다
> - **When** M1이 C1의 주문 1004를 조회한다
> - **Then** "없음"(404)을 받는다

> **TC-031** · `docs/test-cases/order-placement.md` 절 "TC-031 거절된 주문을 조회하면 사유와 상품 목록이 함께 나온다"
> - **Given** 상품 A의 재고가 10개, 상품 B의 재고가 1개다. 상품 Z는 재고 데이터에 없다
> - **And** C1이 상품 A 3개와 상품 B 2개를 주문해 "거절"로 기록됐다
> - **When** C1이 그 주문을 조회한다
> - **Then** 상태 "거절", 사유 "재고 부족", 부족한 상품 B가 나온다
> - **Given** C1이 상품 A 3개, 상품 Z 1개, 상품 B 2개를 주문해 "거절"로 기록됐다
> - **When** C1이 그 주문을 조회한다
> - **Then** 상태 "거절", 사유 "상품 없음", 없는 상품 Z와 부족한 상품 B가 모두 나온다

> **TC-032** · `docs/test-cases/order-placement.md` 절 "TC-032 관리자는 해제 실패 주문, 해제 결과가 비어 있는 주문, 처리중 주문을 찾는다"
> - **Given** C1의 "실패" 주문(해제 결과 "해제 실패"), C2의 "실패" 주문(해제 결과 "해제 완료"), C2의 "실패" 주문(해제 결과가 비어 있음), C1의 "처리중" 주문이 있다
> - **When** M1이 해제 결과 "해제 실패"로 관리자 조회를 한다
> - **Then** C1의 해제 실패 주문만 나온다
> - **When** M1이 상태 "실패"로 관리자 조회를 한다
> - **Then** "실패" 주문 세 개가 나오고, 해제 결과가 비어 있는 주문은 비어 있는 채로 나온다
> - **When** M1이 상태 "처리중"으로 관리자 조회를 한다
> - **Then** C1의 "처리중" 주문만 나온다

> **TC-033** · `docs/test-cases/order-placement.md` 절 "TC-033 고객은 처리중인 주문과 실패한 주문도 조회한다"
> - **Given** C1에게 "처리중" 주문과 "실패" 주문이 있다
> - **When** C1이 "처리중" 주문을 조회한다
> - **Then** 상태 "처리중"인 그 주문을 받는다
> - **When** C1이 "실패" 주문을 조회한다
> - **Then** 상태 "실패", 사유 "잠시 후 다시 시도"인 그 주문을 받는다
> - **When** C1이 자기 주문 목록을 조회한다
> - **Then** "처리중" 주문과 "실패" 주문이 모두 나온다

> **TC-034** · `docs/test-cases/order-placement.md` 절 "TC-034 고객이 확정 주문을 조회하면 주문 번호, 항목, 상태, 주문 시각이 나온다"
> - **Given** 상품 A의 재고가 10개다
> - **And** C1이 상품 A 3개를 주문해 "확정"으로 기록됐다
> - **When** C1이 그 주문을 조회한다
> - **Then** 주문 번호, 주문 항목(상품 A, 수량 3), 상태 "확정", 주문 시각이 나온다

> **TC-035** · `docs/test-cases/order-placement.md` 절 "TC-035 주문 목록은 최근 주문이 먼저 나오고 쪽으로 나뉜다"
> - **Given** C1에게 주문 25개가 있다
> - **When** C1이 쪽 지정 없이 자기 주문 목록을 조회한다
> - **Then** 가장 최근 주문 20개가 최근 순서로 나오고 `hasNext`는 true다
> - **When** C1이 `page=1`로 조회한다
> - **Then** 나머지 5개가 나오고 `hasNext`는 false다
> - **When** C1이 `size=101`로 조회한다
> - **Then** 400을 받는다

- [x] T003 [P] 단계 순서 목록에 `T002-P1`, `T002-P2`를 더하고, 이 목록을 설명하는 클래스 주석을 맞춘다. 사용자 확인 필요 (build-logic/src/main/groovy/msa/quality/TestCaseIdCheck.groovy)
  - `STAGE_ORDER`를 `['P1', 'P2', 'P3', 'T002-P1', 'T002-P2'].asImmutable()`로 바꾼다.
  - 클래스 주석에서 단계가 `T002`인 줄을 "단계 순서에 들지 않아 건너뛴다"고 설명한 문장만 새 단계 이름에 맞게 고친다. 검사 코드(`check()`, `testCaseStageRows()`)는 한 줄도 바꾸지 않는다.
  - 이 변경만으로는 지금 단계(`P3`)의 검사 결과가 바뀌지 않는다. 지금 단계보다 순서가 뒤인 줄은 검사하지 않기 때문이다.
- [x] T004 [P] 단계 표시를 `msa.stage=T002-P1`로 바꾼다. 사용자 확인 필요 (gradle.properties)
  - 근거: 001 tasks.md의 T082도 단계 표시를 해당 단계의 준비 작업에서 올렸다.
- [x] T005 세 설정이 승인받은 내용과 같은지 확인한다 (config/quality/test-case-stages.csv, build-logic/src/main/groovy/msa/quality/TestCaseIdCheck.groovy, gradle.properties)
  - 명령: `Select-String -Path config/quality/test-case-stages.csv -Pattern ',T002$'` → 0줄
  - 명령: `Select-String -Path config/quality/test-case-stages.csv -Pattern ',T002-P1$'` → 11줄, `-Pattern ',T002-P2$'` → 3줄
  - 명령: `git diff --stat` → 바뀐 파일이 위 세 파일뿐이다.
  - 명령: `.\gradlew.bat :services:order-service:checkTestCaseIds` → **실패해야 한다**(종료 코드가 0이 아니다). 실패 메시지에 나오는 번호는 시나리오 3.1, 1.3, 2.3, 2.2, 2.5, 1.4, 2.4, 1.5, 2.6의 테스트 케이스 번호 아홉 개이고, 이 티켓의 테스트가 아직 없다는 뜻이다. 시나리오 1.1, 1.2, 2.1의 번호는 001의 테스트가 이미 있어서 나오지 않는다(위 "검사의 한계"). 이 아홉 개가 T044에서 통과로 바뀐다. 통과하거나 다른 번호가 나오면 멈추고 사용자에게 묻는다.
  - 명령: `.\gradlew.bat :services:inventory-service:checkTestCaseIds` → 종료 코드 0. 재고 서비스는 이 티켓과 관계없이 계속 통과해야 한다.

### 기반 (테스트 도우미)

**⚠️ 이 절이 끝나기 전에는 User Story 1을 시작하지 않는다.**

- [x] T006 서비스 통합 테스트의 도우미 `ViewTestSupport`를 만든다. `IntegrationTestSupport`를 상속한다 (services/order-service/src/test/java/com/example/msa/order/integration/ViewTestSupport.java)
  - 근거: plan.md Source Code 트리, research.md 항목 12(주문을 `OrderMapper`로 직접 저장한다)와 항목 13(WireMock 없이 `IntegrationTestSupport`를 상속한다). `IntegrationTestSupport`는 고치지 않는다.
  - 고객 C1, C2의 `sub`는 `IntegrationTestSupport`에 이미 있는 `C1`, `C2`를 쓴다. 관리자 M1의 `sub` 값 `admin-m1`과 상수 `M1`은 이 클래스에 둔다 `[제안]`.
  - **관리자 흉내**: `admin(String sub)`가 `jwt()`에 `sub`를 주고 권한 `ROLE_ADMIN`을 준다 `[제안]`. Spring Security Test의 `jwt()`는 이 티켓이 만드는 역할 변환기를 거치지 않으므로 권한을 직접 준다(research.md 항목 9의 "테스트 때 알아 둘 것"). 고객은 `customer(sub)`를 그대로 쓴다.
  - **주문 준비**: `@Autowired OrderMapper`로 주문을 저장한다. 메서드 `seed(고객 ID, 상태, 사유, 해제 결과, 항목...)`가 `insertOrder`로 주문을 넣고(요청 키는 `newKey()`), 항목마다 `insertOrderItem`을 부르고, 항목에 거절 사유가 있으면 `updateItemRejectReason`을, 상태가 처리중이 아니면 `updateOrderResult`를 부르고, 주문 번호를 돌려준다 `[제안]`. 항목은 기존 `OrderWithItems.StoredItem`을 쓴다. 테스트가 끝나면 `IntegrationTestSupport`가 주문 테이블을 비운다.
  - **조회 요청**: `get(RequestPostProcessor auth, String path)`가 GET 요청을 보내고 기존 `Reply`로 돌려준다. `auth`가 null이면 토큰 없이 보낸다. 처리되지 않은 예외는 `IntegrationTestSupport.postOrder`와 같은 방식으로 500으로 본다. 쿼리 문자열은 `path`에 붙여서 넘긴다.
  - **응답 읽기**: 목록 응답에서 주문 번호를 순서대로 읽는 `orderNos(Reply)` 같은 짧은 도우미를 둔다 `[제안]`. 아직 없는 구현 클래스를 참조하지 않는다. 응답은 JSON 글자로 읽는다. 그래야 구현 전의 테스트가 컴파일 오류가 아니라 단언 실패로 끝난다.
  - 이 클래스는 기대값을 정하지 않는다. 기대값은 각 테스트가 `docs/test-cases/order-placement.md`에서 가져온다.

이 목록에서는 항목 ID를 쓰지 않았다.

### User Story 1 — 고객이 자기 주문 하나를 조회한다 (Priority: P1)

**목표**: 로그인한 고객이 주문 번호 하나로 자기 주문을 조회한다. 주문에는 주문 번호, 항목, 상태, 거절·실패 사유, 주문 시각이 있다. 다른 고객의 주문, 없는 주문, 숫자가 아닌 주문 번호는 모두 같은 "없음"(404)이다.

**독립 테스트** (spec User Story 1): 고객 C1의 주문을 준비한 뒤 C1이 그 번호로 조회하면 주문을 받고, C2가 같은 번호로 조회하면 "없음"(404)을 받는지 확인한다.

#### 인수 시나리오 테스트 (구현보다 먼저)

테스트 다섯 개는 한 클래스 `ViewOrderIntegrationTest`에 모은다(plan.md 표). 같은 파일에 메서드를 더하므로 `[P]`를 붙이지 않았다. 아래 응답 필드 이름(`orderNo`, `status`, `reason`, `shortageProductIds`, `missingProductIds`, `items`, `createdAt`)과 값(`REJECTED`, `OUT_OF_STOCK` 같은 영어 이름)은 계약 파일 `specs/002-view-orders/contracts/order-query-api.yaml`이 정한 것이다. 테스트 케이스 문서는 한국어 개념만 적었다(맨 끝 "사용자 확인 필요" 5번).

- [x] T007 [US1] 시나리오 1.1의 서비스 통합 테스트를 쓴다. 이 시나리오는 "그 주문이 거절 상태로 기록되어 있고 C1이 조회할 수 있다" 줄만 검증한다. 상품 A 3개, 상품 B 2개(B는 재고 부족)로 거절된 C1의 주문을 `seed`로 준비하고, C1이 `GET /orders/{주문 번호}`로 조회한다. 상태 200, `status`가 `REJECTED`, `reason`이 `OUT_OF_STOCK`, `shortageProductIds`가 `["B"]`, `items`가 A 3개와 B 2개인 것을 단언한다. `@DisplayName`은 시나리오 1.1의 테스트 케이스 ID로 시작한다. 기대값은 `docs/test-cases/order-placement.md`에서 그대로 가져온다 (services/order-service/src/test/java/com/example/msa/order/integration/ViewOrderIntegrationTest.java)
  - 재고 수량(A는 10개, B는 1개 그대로)은 주문을 만드는 단계의 기대값이고 001이 검증했다. 이 테스트는 확인하지 않는다(spec 시나리오 1.1).
- [x] T008 [US1] 시나리오 1.2의 서비스 통합 테스트를 같은 파일에 더한다. C1의 주문을 준비하고 C2가 그 번호로 조회하면 404를 받는지 본다. 응답 본문이 Problem Details 형식(`application/problem+json`)이고 `code`가 `ORDER_NOT_FOUND`인지도 단언한다(공통 전제의 조회 API 오류 응답 줄). `@DisplayName`은 시나리오 1.2의 테스트 케이스 ID로 시작한다 (services/order-service/src/test/java/com/example/msa/order/integration/ViewOrderIntegrationTest.java)
- [x] T009 [US1] 시나리오 1.3의 서비스 통합 테스트를 같은 파일에 더한다. 주문 9999는 없다. C1이 `GET /orders/9999`로 조회하면 404를 받고, 이 응답이 "C2의 주문을 C1이 조회했을 때"의 응답과 같은지(`status`, `type`, `title`, `detail`, `code`가 모두 같다) 본다. C1이 `GET /orders/abc`로 조회해도 404이고 `code`가 `ORDER_NOT_FOUND`인지 본다. `@DisplayName`은 시나리오 1.3의 테스트 케이스 ID로 시작한다 (services/order-service/src/test/java/com/example/msa/order/integration/ViewOrderIntegrationTest.java)
  - 주문 번호 9999는 쓰지 않는 번호다. 시퀀스는 1부터 올라가고 테스트가 끝나면 주문 테이블을 비우므로 겹치지 않는다.
- [x] T010 [US1] 시나리오 1.4의 서비스 통합 테스트를 같은 파일에 더한다. 메서드 둘로 나눈다. 첫째는 A 3개와 B 2개(B 부족)로 거절된 주문을 조회해 상태 거절(`REJECTED`), 사유 재고 부족(`OUT_OF_STOCK`), 부족한 상품 B(`shortageProductIds`가 `["B"]`)를 단언한다. 둘째는 A 3개, Z 1개, B 2개(Z는 없는 상품, B 부족)로 거절된 주문을 조회해 상태 거절, 사유 상품 없음(`PRODUCT_NOT_FOUND`), `missingProductIds`가 `["Z"]`, `shortageProductIds`가 `["B"]`를 단언한다. `@DisplayName`은 시나리오 1.4의 테스트 케이스 ID로 시작한다 (services/order-service/src/test/java/com/example/msa/order/integration/ViewOrderIntegrationTest.java)
- [x] T011 [US1] 시나리오 1.5의 서비스 통합 테스트를 같은 파일에 더한다. 상품 A 3개로 확정된 C1의 주문을 조회해 `orderNo`, `items`(상품 A, 수량 3), `status`가 `CONFIRMED`, `createdAt`이 있고 비어 있지 않은 것을 단언한다. `@DisplayName`은 시나리오 1.5의 테스트 케이스 ID로 시작한다 (services/order-service/src/test/java/com/example/msa/order/integration/ViewOrderIntegrationTest.java)
  - 주문 시각의 값은 단언하지 않는다. 문서가 "주문 시각이 나온다"까지만 정했고, `CREATED_AT`은 DB가 `SYSTIMESTAMP`로 넣어서 테스트가 값을 정할 수 없다(research.md 항목 12).
  - `createdAt`의 글자 모양을 보고에 적을 수 있도록 테스트 로그에 한 줄 남긴다(`log.info`) `[제안]`. 단언하지 않는다. T047이 이 값을 읽는다.
- [x] T012 [US1] 위 테스트가 실패하는 것을 확인한다 (services/order-service/src/test/java/com/example/msa/order/integration/ViewOrderIntegrationTest.java)
  - 명령: `.\gradlew.bat :services:order-service:test --tests "com.example.msa.order.integration.ViewOrderIntegrationTest"`
  - 기대 결과: 종료 코드가 0이 아니고, 테스트 메서드 여섯 개(T007, T008, T009, T010 둘, T011)가 모두 실패한다. 실패 이유는 조회 API가 없어서 생긴 것이다(예: 주문 하나 조회가 404인데 응답 본문에 `code`가 없다). 하나라도 통과하면 멈추고 사용자에게 묻는다.
  - 컴파일 오류가 나면 도우미나 테스트를 고친다. **앱이 뜨지 못하면 멈추고 보고한다.** `IntegrationTestSupport`를 `InventoryWireMock` 없이 직접 상속한 테스트는 지금 없어서, 컨텍스트가 뜨는지 이 작업이 처음 확인한다(research.md 4절). 원인이 WireMock 없음이면 research.md 항목 13의 결정을 바꾸는 일이라 사용자에게 묻는다.

**이 절의 시나리오 원문**

> **TC-002** (spec 시나리오 1.1) · `docs/test-cases/order-placement.md` 절 "TC-002 항목 하나라도 재고가 부족하면 주문 전체가 거절되고 기록된다"
> - **Given** 상품 A의 재고가 10개, 상품 B의 재고가 1개다
> - **When** 고객이 상품 A 3개와 상품 B 2개를 한 주문으로 요청한다
> - **Then** 고객은 주문 번호와 "거절", 부족한 상품 B를 받는다
> - **And** 그 주문이 "거절" 상태로 기록되어 있고 C1이 조회할 수 있다
> - **And** 상품 A의 재고는 10개, 상품 B의 재고는 1개 그대로다 (A도 줄지 않는다)

> **TC-010** (spec 시나리오 1.2) · `docs/test-cases/order-placement.md` 절 "TC-010 로그인한 사람만 주문하고, 자기 주문만 본다"
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

시나리오 1.2가 검증하는 줄은 위 인용의 셋째 줄부터 다섯째 줄이다. 첫째, 둘째 줄(토큰 없이 주문)은 001이 검증했다. 여섯째 줄부터는 시나리오 2.1, 4.1, 4.2가 검증한다.

> **TC-026** (spec 시나리오 1.3) · `docs/test-cases/order-placement.md` 절 "TC-026 없는 주문 번호와 숫자가 아닌 주문 번호는 "없음"이다"
> - **Given** 주문 9999는 없다
> - **When** C1이 주문 9999를 조회한다
> - **Then** "없음"(404)을 받는다. 다른 고객의 주문을 조회했을 때와 같은 응답이다
> - **When** C1이 주문 번호 `abc`로 조회한다
> - **Then** "없음"(404)을 받는다

> **TC-031** (spec 시나리오 1.4) · `docs/test-cases/order-placement.md` 절 "TC-031 거절된 주문을 조회하면 사유와 상품 목록이 함께 나온다"
> - **Given** 상품 A의 재고가 10개, 상품 B의 재고가 1개다. 상품 Z는 재고 데이터에 없다
> - **And** C1이 상품 A 3개와 상품 B 2개를 주문해 "거절"로 기록됐다
> - **When** C1이 그 주문을 조회한다
> - **Then** 상태 "거절", 사유 "재고 부족", 부족한 상품 B가 나온다
> - **Given** C1이 상품 A 3개, 상품 Z 1개, 상품 B 2개를 주문해 "거절"로 기록됐다
> - **When** C1이 그 주문을 조회한다
> - **Then** 상태 "거절", 사유 "상품 없음", 없는 상품 Z와 부족한 상품 B가 모두 나온다

> **TC-034** (spec 시나리오 1.5) · `docs/test-cases/order-placement.md` 절 "TC-034 고객이 확정 주문을 조회하면 주문 번호, 항목, 상태, 주문 시각이 나온다"
> - **Given** 상품 A의 재고가 10개다
> - **And** C1이 상품 A 3개를 주문해 "확정"으로 기록됐다
> - **When** C1이 그 주문을 조회한다
> - **Then** 주문 번호, 주문 항목(상품 A, 수량 3), 상태 "확정", 주문 시각이 나온다

#### 구현

- [x] T013 [P] [US1] 주문 번호 변환의 단위 테스트를 쓴다. 이 테스트는 아직 없는 `OrderNumbers`를 참조하므로 컴파일 오류로 실패한다. 그 실패를 본 뒤 T015를 한다 (services/order-service/src/test/java/com/example/msa/order/domain/OrderNumbersTest.java)
  - 검사할 것: 글자 `abc`는 숫자가 아니라서 변환 결과가 비어 있다(시나리오 1.3의 "숫자가 아닌 주문 번호"). 글자 `9999`는 숫자 9999로 바뀐다.
  - `long`에 담기지 않는 아주 큰 숫자와 앞뒤 공백, 음수는 단언하지 않는다. 문서에 기대값이 없다(맨 끝 "사용자 확인 필요" 4번).
- [x] T014 [US1] 조회 Mapper 테스트를 쓴다. plan.md 표의 시나리오 1.1, 1.2, 1.4, 1.5 "Mapper" 칸이다. 이 테스트는 아직 없는 Mapper 메서드를 참조하므로 컴파일 오류로 실패한다. 그 실패를 본 뒤 T016과 T017을 한다 (services/order-service/src/test/java/com/example/msa/order/mapper/OrderQueryMapperTest.java)
  - `@MybatisTest`, `@AutoConfigureTestDatabase(replace = NONE)`, `@ImportTestcontainers(OracleTestContainer.class)`, `@ActiveProfiles("test")`로 기존 `OrderMapperTest`와 같은 방식으로 쓴다. `OrderMapperTest`는 고치지 않는다.
  - 거절 주문의 상태, 사유, 항목별 거절 사유(`OUT_OF_STOCK`, `PRODUCT_NOT_FOUND`)를 읽는다(시나리오 1.1, 1.4의 값: A 3개, Z 1개, B 2개). **항목의 순서는 단언하지 않는다.** 순서 없는 비교(`containsExactlyInAnyOrder`)를 쓴다 `[제안]`. 조회 결과의 항목 순서를 정한 기준 문서가 없다(맨 끝 "사용자 확인 필요" 4번).
  - 주문 번호와 고객이 모두 맞아야 읽힌다. 고객이 다르면 결과가 없다(시나리오 1.2).
  - `CREATED_AT`을 읽으면 `createdAt`이 비어 있지 않다(시나리오 1.5).
  - 새 Mapper 메서드 이름은 `selectCustomerOrder(orderNo, customerId)`로 한다 `[제안]`.
- [x] T015 [US1] 주문 번호 변환을 만든다. 순수 Java 클래스 `OrderNumbers`의 `parse(글자)`가 숫자면 주문 번호를, 숫자가 아니거나 `long`에 담기지 않으면 빈 값(`OptionalLong`)을 돌려준다 `[제안]`. 예외를 던지지 않는다 (services/order-service/src/main/java/com/example/msa/order/domain/OrderNumbers.java)
  - 근거: research.md 항목 11. 숫자 타입으로 받으면 `abc`에 Spring이 400을 낸다. 시나리오 1.3은 404를 요구한다.
  - `domain` 패키지는 Spring과 MyBatis를 모른다. ArchUnit 규칙이 검사한다.
- [x] T016 [P] [US1] 조회 응답 DTO를 만들고 `OrderWithItems`에 주문 시각 필드를 더한다 (services/order-service/src/main/java/com/example/msa/order/dto/OrderDetailResponse.java, services/order-service/src/main/java/com/example/msa/order/dto/OrderWithItems.java)
  - `OrderDetailResponse`는 record다. 필드는 `orderNo`, `status`, `reason`, `shortageProductIds`, `missingProductIds`, `items`, `createdAt`이고, 저장된 주문에서 만드는 `from(OrderWithItems)`가 있다. 부족한 상품과 없는 상품 목록은 기존 `OrderResponse.from`과 같은 방식으로 항목별 거절 사유에서 만든다. `items`의 각 줄은 `productId`, `quantity`다.
  - 필드 규칙은 data-model.md 2절의 원문을 따른다.
    - `reason`: 「글자 또는 `null`」. 「거절이면 `OUT_OF_STOCK`(재고 부족) 또는 `PRODUCT_NOT_FOUND`(상품 없음). 실패이면 `RETRY_LATER`(잠시 후 다시 시도). 그 밖에는 `null`」
    - `shortageProductIds`, `missingProductIds`: 「`OUT_OF_STOCK`인 항목의 상품 ID는 `shortageProductIds`, `PRODUCT_NOT_FOUND`인 항목은 `missingProductIds`. 없으면 빈 목록. 상품 ID 순」
    - `items`: 「주문 항목. 상품 ID 순」
    - `createdAt`: 「시각대 없는 ISO 8601(예: `2026-10-11T13:45:11.123456`)」. plan 그대로다 `[제안]`. 사용자는 2026-10-13에 이 모양을 문서에 더하지 않고 테스트도 단언하지 않기로만 정했다(맨 끝 "사용자 확인 필요" 4번).
    - 「필드는 빠지지 않는다. 값이 없는 필드(`reason`, `releaseStatus`)는 `null`로 낸다.」
    - 「`ORDERS.CUSTOMER_ID`와 `ORDERS.REQUEST_KEY`는 응답에 내지 않는다.」
  - `OrderWithItems`에 `LocalDateTime createdAt`과 getter, setter를 더한다. 기존 필드와 기존 쓰기 흐름은 이 필드를 쓰지 않으므로 바꾸지 않는다(data-model.md 6절).
- [x] T017 [US1] 주문 하나 조회 SQL을 만든다. 새 `resultMap` `orderViewMap`과 `select` 하나, Mapper 메서드 하나를 더한다 (services/order-service/src/main/resources/mapper/OrderMapper.xml, services/order-service/src/main/java/com/example/msa/order/mapper/OrderMapper.java)
  - `orderViewMap`은 기존 `orderWithItemsMap`을 `extends`로 이어받고 `CREATED_AT`을 `createdAt`에 짝짓는 `result` 하나만 더한다. 기존 `resultMap`과 기존 `select`는 바꾸지 않는다.
  - `selectCustomerOrder`: `WHERE o.ORDER_NO = #{orderNo} AND o.CUSTOMER_ID = #{customerId}`, 항목은 `ORDER BY i.PRODUCT_ID`. 주문과 항목은 `LEFT JOIN`으로 `<collection>` 한 번에 읽는다. 고객 조건을 SQL에 넣어서 다른 고객의 주문이 없는 주문과 같은 빈 결과가 된다(research.md 항목 11).
  - 값은 `#{}`로만 넣고 `SELECT *`를 쓰지 않는다. SQL은 Oracle 19c에서 도는 것만 쓴다(`docs/standards/coding-conventions.md` 3-2절, 3-3절). 기계 검사가 `${`와 `SELECT *`, 19c 이후 키워드를 찾는다.
  - 이 작업이 끝나면 T014의 Mapper 테스트가 통과한다.
- [x] T018 [P] [US1] "없음" 응답 본문을 만든다. `orderNotFound()`(이름 `[제안]`)가 404 Problem Details를 돌려준다 (services/order-service/src/main/java/com/example/msa/order/exception/ProblemDetailsHandler.java)
  - `code`는 `ORDER_NOT_FOUND`, `type`은 `urn:msa-example:problem:ORDER_NOT_FOUND`다. 기존 `problem(...)` 메서드를 쓴다.
  - 한 가지 `title`과 한 가지 `detail` 글만 쓴다. `detail`에 요청한 주문 번호를 담지 않는다. 담으면 경우마다 글이 달라져서 그 주문이 있는지 알 수 있게 된다. 기준은 `docs/requirements/uc-002-view-orders.md` 3절 "업무 규칙"의 규칙 칸이다: 「고객은 다른 고객의 주문을 볼 수 없다. 다른 고객의 주문 번호로 조회하면 "없음"으로 응답해, 그 주문이 있는지도 알 수 없게 한다」(research.md 항목 11).
- [x] T019 [US1] 조회 서비스를 만든다. `findMine(고객 ID, 주문 번호 글자)`(이름 `[제안]`)가 `OrderNumbers.parse`로 글자를 숫자로 바꾸고, 숫자가 아니면 DB를 부르지 않고 빈 값을 돌려준다. 숫자면 `selectCustomerOrder`를 부르고 `OrderDetailResponse`로 바꿔 돌려준다 (services/order-service/src/main/java/com/example/msa/order/service/OrderQueryService.java)
  - 읽기 전용 트랜잭션을 메서드마다 붙인다(`@Transactional(readOnly = true)`) `[제안]`. 클래스에는 붙이지 않는다. `ArchitectureTest`의 주석이 "`@Transactional`을 클래스가 아니라 메서드에 붙인다"고 설명하고 있어서, 그 설명이 계속 맞게 하려는 것이다.
  - 이 서비스는 `client` 패키지를 부르지 않는다. 재고 서비스를 부르지 않는다.
  - 반환 형식은 `Optional<OrderDetailResponse>`로 한다 `[제안]`. 빈 값이면 컨트롤러가 404로 바꾼다. 서비스가 웹 예외를 던지지 않는다.
- [x] T020 [US1] 조회 컨트롤러를 만든다. `GET /orders/{orderNo}`가 `@AuthenticationPrincipal Jwt`의 `getSubject()`와 경로의 글자를 서비스에 넘기고, 결과가 있으면 200과 본문을, 없으면 `ErrorResponseException`으로 404(`orderNotFound()`)를 낸다 (services/order-service/src/main/java/com/example/msa/order/controller/OrderQueryController.java)
  - **클래스에 `@RequestMapping`을 두지 않고 메서드마다 경로를 쓴다** `[제안]`. plan.md가 tasks.md에 넘긴 결정이다. 2단계에서 `/admin/orders`가 더해져도 클래스 하나가 두 경로 접두사를 가지지 않는다. 001의 `OrderController`는 고치지 않는다.
  - `{orderNo}`는 `String`으로 받는다. 업무 판단(숫자 변환)은 서비스가 한다.
  - 고객 ID는 토큰의 `sub`에서만 얻는다. 쿼리 매개변수나 헤더로 받은 고객 ID는 쓰지 않는다. `SecurityConfig`는 이 단계에서 바꾸지 않는다. `anyRequest().authenticated()`가 이 경로를 이미 막는다.
- [x] T021 [US1] 이 절의 테스트가 모두 통과하는지 확인한다 (services/order-service/src/test/java/com/example/msa/order/integration/ViewOrderIntegrationTest.java)
  - 명령: `.\gradlew.bat :services:order-service:test --tests "com.example.msa.order.integration.ViewOrderIntegrationTest" --tests "com.example.msa.order.domain.OrderNumbersTest" --tests "com.example.msa.order.mapper.OrderQueryMapperTest"`
  - 기대 결과: 종료 코드 0. 통과·실패 수를 보고에 적는다.
  - Mapper 테스트가 통과하면 인라인 뷰 없는 단일 조회 SQL이 실제 Oracle에서 도는 것이 확인된다. 쪽 지정 SQL은 T031, T034에서 확인한다.

이 목록에서는 항목 ID를 쓰지 않았다.

**체크포인트**: 고객이 자기 주문 하나를 조회하고, 다른 고객의 주문과 없는 주문과 숫자가 아닌 주문 번호는 같은 404다.

### User Story 2 — 고객이 자기 주문 목록을 조회한다 (Priority: P1)

**목표**: 로그인한 고객의 주문만 주문 번호가 큰 순서로 나오고, `page`와 `size`로 쪽이 나뉜다. 다른 고객의 주문은 섞이지 않는다. 처리중·실패·거절·확정 주문이 모두 나온다. 주문이 없으면 오류 없이 빈 목록이다. 관리자 역할만 있는 사용자도 이 조회로는 자기 주문만 본다.

**독립 테스트** (spec User Story 2): 고객 C1, C2의 주문을 준비한다. C1이 목록을 조회해 C1의 주문만 나오고, C2가 조회해 C2의 주문만 나오는지 확인한다.

#### 인수 시나리오 테스트 (구현보다 먼저)

테스트 여섯 개는 한 클래스 `ListOrdersIntegrationTest`에 모은다(plan.md 표). 같은 파일에 메서드를 더하므로 `[P]`를 붙이지 않았다. 목록 응답의 모양(`orders`, `page`, `size`, `hasNext`)은 계약 파일이 정했다.

- [x] T022 [US2] 시나리오 2.1의 서비스 통합 테스트를 쓴다. C1의 주문을 준비하고 C1이 `GET /orders`로 조회하면 상태 200이고 `orders`에 그 주문만 있는지 단언한다. `@DisplayName`은 시나리오 2.1의 테스트 케이스 ID로 시작한다. 기대값은 `docs/test-cases/order-placement.md`에서 그대로 가져온다 (services/order-service/src/test/java/com/example/msa/order/integration/ListOrdersIntegrationTest.java)
- [x] T023 [US2] 시나리오 2.2의 서비스 통합 테스트를 같은 파일에 더한다. C1의 주문과 C2의 주문을 준비한다. C1이 조회하면 C1의 주문만 나오고 C2의 주문은 나오지 않는다. C2가 조회하면 C2의 주문만 나오고 C1의 주문은 나오지 않는다. `@DisplayName`은 시나리오 2.2의 테스트 케이스 ID로 시작한다 (services/order-service/src/test/java/com/example/msa/order/integration/ListOrdersIntegrationTest.java)
- [x] T024 [US2] 시나리오 2.3의 서비스 통합 테스트를 같은 파일에 더한다. C2의 주문이 하나도 없는 상태에서 C2가 조회하면 상태 200이고 `orders`가 빈 배열인지 단언한다(공통 전제의 "주문이 하나도 없는 목록도 200이다"). `@DisplayName`은 시나리오 2.3의 테스트 케이스 ID로 시작한다 (services/order-service/src/test/java/com/example/msa/order/integration/ListOrdersIntegrationTest.java)
- [x] T025 [US2] 시나리오 2.4의 서비스 통합 테스트를 같은 파일에 더한다. C1의 처리중 주문과 실패 주문(사유 `RETRY_LATER`)을 준비한다. 처리중 주문을 조회하면 `status`가 `PENDING`이다. 실패 주문을 조회하면 `status`가 `FAILED`, `reason`이 `RETRY_LATER`다. C1이 목록을 조회하면 두 주문이 모두 나온다. `@DisplayName`은 시나리오 2.4의 테스트 케이스 ID로 시작한다 (services/order-service/src/test/java/com/example/msa/order/integration/ListOrdersIntegrationTest.java)
- [x] T026 [US2] 시나리오 2.5의 서비스 통합 테스트를 같은 파일에 더한다. 관리자 M1(`admin(M1)`)의 주문과 C1의 주문을 준비한다. M1이 `GET /orders`로 조회하면 M1의 주문만 나온다. M1이 자기 주문을 조회하면 그 주문을 받는다. M1이 C1의 주문을 조회하면 404를 받는다. `@DisplayName`은 시나리오 2.5의 테스트 케이스 ID로 시작한다 (services/order-service/src/test/java/com/example/msa/order/integration/ListOrdersIntegrationTest.java)
- [x] T027 [US2] 시나리오 2.6의 서비스 통합 테스트를 같은 파일에 더한다. C1에게 주문 25개를 준비하고 저장 결과의 주문 번호를 순서대로 기억한다. 쪽 지정 없이 조회하면 상태 200, `orders`가 20개, 주문 번호가 큰 순서로 가장 최근 20개, `hasNext`가 true다. `page=1`로 조회하면 나머지 5개(가장 오래된 5개를 큰 순서로)와 `hasNext`가 false다. `size=101`로 조회하면 상태 400이고 `code`가 `INVALID_QUERY_PARAMETER`다. `@DisplayName`은 시나리오 2.6의 테스트 케이스 ID로 시작한다 (services/order-service/src/test/java/com/example/msa/order/integration/ListOrdersIntegrationTest.java)
- [x] T028 [P] [US2] 쿼리의 고객 ID를 무시하는지 보는 서비스 통합 테스트를 쓴다. C1의 주문과 C2의 주문을 준비하고, C1의 토큰으로 `GET /orders?customerId=` 뒤에 C2의 `sub`를 붙여 부른다. 결과에 C1의 주문만 있는지 단언한다. 인수 시나리오 테스트가 아니므로 테스트 케이스 ID를 붙이지 않는다 (services/order-service/src/test/java/com/example/msa/order/integration/OrderOwnerQueryIntegrationTest.java)
  - 기대값은 `docs/requirements/uc-002-view-orders.md` 3절 "업무 규칙"의 첫 문장이다: 「고객은 다른 고객의 주문을 볼 수 없다.」
- [x] T029 [US2] 위 테스트가 실패하는 것을 확인한다 (services/order-service/src/test/java/com/example/msa/order/integration/ListOrdersIntegrationTest.java)
  - 명령: `.\gradlew.bat :services:order-service:test --tests "com.example.msa.order.integration.ListOrdersIntegrationTest" --tests "com.example.msa.order.integration.OrderOwnerQueryIntegrationTest"`
  - 기대 결과: 종료 코드가 0이 아니고, T022~T028의 테스트가 모두 실패한다. 실패 이유는 `GET /orders`가 없어서 생긴 것이다(지금 `/orders`에는 POST만 있어서 405가 나온다). T025는 처리중·실패 주문 하나씩을 조회하는 부분이 T020의 주문 하나 조회로 이미 통과하더라도 목록 부분에서 실패해야 한다. 하나라도 통과하면 멈추고 사용자에게 묻는다.

**이 절의 시나리오 원문**

> **TC-010** (spec 시나리오 2.1) · `docs/test-cases/order-placement.md` 절 "TC-010 로그인한 사람만 주문하고, 자기 주문만 본다"
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

시나리오 2.1이 검증하는 줄은 위 인용의 여섯째 줄과 일곱째 줄이다. 이 두 줄 앞에는 C1의 주문 1004가 있다는 Given만 있다. 다른 고객의 주문이 있는 상태는 시나리오 2.2가 확인한다.

> **TC-029** (spec 시나리오 2.2) · `docs/test-cases/order-placement.md` 절 "TC-029 고객의 목록에는 다른 고객의 주문이 섞이지 않는다"
> - **Given** C1에게 주문 1004가 있고, C2에게 주문 1005가 있다
> - **When** C1이 자기 주문 목록을 조회한다
> - **Then** 주문 1004만 나오고 주문 1005는 나오지 않는다
> - **When** C2가 자기 주문 목록을 조회한다
> - **Then** 주문 1005만 나오고 주문 1004는 나오지 않는다

> **TC-028** (spec 시나리오 2.3) · `docs/test-cases/order-placement.md` 절 "TC-028 주문이 하나도 없는 고객의 목록은 비어 있다"
> - **Given** C2의 주문이 하나도 없다
> - **When** C2가 자기 주문 목록을 조회한다
> - **Then** 오류 없이 빈 목록을 받는다

> **TC-033** (spec 시나리오 2.4) · `docs/test-cases/order-placement.md` 절 "TC-033 고객은 처리중인 주문과 실패한 주문도 조회한다"
> - **Given** C1에게 "처리중" 주문과 "실패" 주문이 있다
> - **When** C1이 "처리중" 주문을 조회한다
> - **Then** 상태 "처리중"인 그 주문을 받는다
> - **When** C1이 "실패" 주문을 조회한다
> - **Then** 상태 "실패", 사유 "잠시 후 다시 시도"인 그 주문을 받는다
> - **When** C1이 자기 주문 목록을 조회한다
> - **Then** "처리중" 주문과 "실패" 주문이 모두 나온다

> **TC-030** (spec 시나리오 2.5) · `docs/test-cases/order-placement.md` 절 "TC-030 관리자 역할만 있는 사용자는 고객용 조회로 자기 주문만 본다"
> - **Given** M1의 주문 1006과 C1의 주문 1004가 있다
> - **When** M1이 자기 주문 목록을 조회한다
> - **Then** 주문 1006만 나온다
> - **When** M1이 주문 1006을 조회한다
> - **Then** 주문 1006을 받는다
> - **When** M1이 C1의 주문 1004를 조회한다
> - **Then** "없음"(404)을 받는다

> **TC-035** (spec 시나리오 2.6) · `docs/test-cases/order-placement.md` 절 "TC-035 주문 목록은 최근 주문이 먼저 나오고 쪽으로 나뉜다"
> - **Given** C1에게 주문 25개가 있다
> - **When** C1이 쪽 지정 없이 자기 주문 목록을 조회한다
> - **Then** 가장 최근 주문 20개가 최근 순서로 나오고 `hasNext`는 true다
> - **When** C1이 `page=1`로 조회한다
> - **Then** 나머지 5개가 나오고 `hasNext`는 false다
> - **When** C1이 `size=101`로 조회한다
> - **Then** 400을 받는다

#### 구현

- [x] T030 [P] [US2] 쪽 지정의 단위 테스트를 쓴다. 이 테스트는 아직 없는 `PageRequest`를 참조하므로 컴파일 오류로 실패한다. 그 실패를 본 뒤 T032를 한다 (services/order-service/src/test/java/com/example/msa/order/domain/PageRequestTest.java)
  - 검사할 것: `page`와 `size`를 주지 않으면 `page`는 0, `size`는 20이다(공통 전제: `page`는 0부터, `size`는 기본 20). `size=100`은 맞고 `size=101`은 틀리다(공통 전제: 최대 100, 범위 밖은 400). `page=-1`은 틀리다(`page`는 0부터). `page=1`, `size=20`이면 건너뛸 줄 수(offset)는 20이다(시나리오 2.6: 둘째 쪽은 앞의 20개 다음부터).
  - `size=0`, 음수 `size`, 정수가 아닌 `page`·`size`는 단언하지 않는다. 문서에 기대값이 없다(맨 끝 "사용자 확인 필요" 4번).
- [x] T031 [US2] 조회 Mapper 테스트에 목록 SQL을 검사하는 메서드를 더한다. plan.md 표의 시나리오 2.1~2.4, 2.6 "Mapper" 칸이다. 아직 없는 Mapper 메서드를 참조하므로 컴파일 오류로 실패한다. 그 실패를 본 뒤 T034를 한다 (services/order-service/src/test/java/com/example/msa/order/mapper/OrderQueryMapperTest.java)
  - 고객 조건으로 목록을 읽는다. 두 고객의 주문이 있을 때 자기 주문만 읽는다. 주문이 없는 고객은 빈 결과다. 처리중 주문과 실패 주문을 읽는다.
  - 정렬은 주문 번호가 큰 순서다(공통 전제). 항목이 여러 개인 주문이 쪽 경계에서 잘리지 않고 주문 단위로 묶여 나온다(예: 항목 2개짜리 주문 3개를 `limit` 2로 읽으면 주문 2개와 각 항목이 온다). `size + 1`개를 읽으면 다음 쪽이 있는지 알 수 있다(시나리오 2.6). 쪽 경계에서 `offset`만큼 건너뛴다.
  - 새 Mapper 메서드 이름은 `selectCustomerOrders(customerId, offset, limit)`로 한다 `[제안]`.
- [x] T032 [US2] 쪽 지정과 조회 조건 예외를 만든다. 순수 Java다 (services/order-service/src/main/java/com/example/msa/order/domain/PageRequest.java, services/order-service/src/main/java/com/example/msa/order/domain/InvalidQueryException.java)
  - `PageRequest`는 record `(int page, int size)`다 `[제안]`. `of(글자 page, 글자 size)`가 값을 검사해서 만든다. 상수 `DEFAULT_SIZE = 20`, `MAX_SIZE = 100`은 설정값이 아니라 `domain`의 상수다(research.md 항목 3). `offset()`(page × size)과 `fetchSize()`(size + 1)를 둔다.
  - `InvalidQueryException`은 틀린 매개변수 이름과 이유의 목록(`field`, `message`)을 담는 업무 예외다. 기존 `OrderItems.Violation`과 같은 방식으로 쓴다 `[제안]`.
  - 규칙은 data-model.md 4절의 원문을 따른다. `page`의 규칙은 「0 이상의 정수. 없으면 0」이고 `size`의 규칙은 「1 이상 100 이하의 정수. 없으면 20」이다. 틀리면 400이다.
- [x] T033 [US2] 목록 응답 봉투를 만든다. record `OrderPage<T>`가 `orders`, `page`, `size`, `hasNext`를 담는다 `[제안]`. 관리자 목록도 같은 봉투를 쓰도록 형식 매개변수 `T`로 만든다. 정적 메서드 `of(읽은 줄 목록, PageRequest)`가 `size + 1`개를 읽은 목록에서 `hasNext`를 정하고 `size`개로 자른다 (services/order-service/src/main/java/com/example/msa/order/dto/OrderPage.java)
  - plan.md 트리의 `OrderPage` 이름 그대로다. 형식 매개변수는 이 tasks.md가 더한 것이다. 계약 파일은 `OrderPage`와 `AdminOrderPage`를 따로 적었지만 JSON 모양이 같다.
  - 「주문이 하나도 없으면 `orders`는 빈 배열이다. `hasNext`는 `false`다.」
- [x] T034 [US2] 고객 목록 조회 SQL을 만든다 (services/order-service/src/main/resources/mapper/OrderMapper.xml, services/order-service/src/main/java/com/example/msa/order/mapper/OrderMapper.java)
  - `selectCustomerOrders`: `ORDERS`에서 `CUSTOMER_ID = #{customerId}`인 주문을 `ORDER BY ORDER_NO DESC OFFSET #{offset} ROWS FETCH NEXT #{limit} ROWS ONLY`로 자른 안쪽 SELECT(인라인 뷰)를 만들고, 그 결과에 `ORDER_ITEMS`를 `LEFT JOIN`한다. 바깥에서 `ORDER BY o.ORDER_NO DESC, i.PRODUCT_ID`로 정렬한다. `orderViewMap`을 쓴다.
  - 근거: data-model.md 3절. 주문과 항목을 합친 뒤에 자르면 주문 하나가 쪽 사이에서 잘린다.
  - 이 작업이 끝나면 T031의 테스트가 통과한다. 인라인 뷰로 자른 뒤 항목을 합치는 SQL이 실제 Oracle에서 도는지는 research.md 4절의 확인하지 못한 것이다. 이 테스트가 처음 확인한다. 돌지 않으면 멈추고 사용자에게 묻는다.
- [x] T035 [US2] 조회 조건이 틀렸을 때의 400 응답을 만든다. `invalidQueryParameter(오류 목록)`(이름 `[제안]`)이 400 Problem Details를 돌려주고, `InvalidQueryException`을 받는 `@ExceptionHandler`가 이를 응답으로 바꾼다 (services/order-service/src/main/java/com/example/msa/order/exception/ProblemDetailsHandler.java)
  - `code`는 `INVALID_QUERY_PARAMETER`, `type`은 `urn:msa-example:problem:INVALID_QUERY_PARAMETER`다. 틀린 매개변수 이름과 이유를 기존 `errors`(`field`, `message`)에 담는다.
  - 기존 `handleMethodArgumentNotValid`, `handleHttpMessageNotReadable`은 고치지 않는다.
- [x] T036 [US2] 조회 서비스에 목록 메서드를 더한다. `listMine(고객 ID, PageRequest)`(이름 `[제안]`)가 `selectCustomerOrders(고객 ID, offset, fetchSize)`를 부르고 `OrderPage.of`로 봉투를 만든다. 메서드에 `@Transactional(readOnly = true)`를 붙인다 (services/order-service/src/main/java/com/example/msa/order/service/OrderQueryService.java)
- [x] T037 [US2] 조회 컨트롤러에 `GET /orders`를 더한다. `page`와 `size`를 선택 쿼리 매개변수 글자로 받아 `[제안]` `PageRequest.of`에 넘기고, 서비스를 불러 200과 `OrderPage`를 돌려준다 (services/order-service/src/main/java/com/example/msa/order/controller/OrderQueryController.java)
  - 고객 ID는 토큰의 `sub`에서만 얻는다. 알 수 없는 쿼리 매개변수(`customerId` 등)는 받지 않으므로 무시된다.
  - 틀린 값은 `InvalidQueryException`이 되고, T035의 처리기가 400으로 바꾼다.
- [x] T038 [US2] 이 절의 테스트와 앞 절의 테스트가 모두 통과하는지 확인한다 (services/order-service/src/test/java/com/example/msa/order/integration/ListOrdersIntegrationTest.java)
  - 명령: `.\gradlew.bat :services:order-service:test --tests "com.example.msa.order.integration.ViewOrderIntegrationTest" --tests "com.example.msa.order.integration.ListOrdersIntegrationTest" --tests "com.example.msa.order.integration.OrderOwnerQueryIntegrationTest" --tests "com.example.msa.order.domain.*" --tests "com.example.msa.order.mapper.OrderQueryMapperTest"`
  - 기대 결과: 종료 코드 0. 통과·실패 수를 보고에 적는다.

이 목록에서는 항목 ID를 쓰지 않았다.

**체크포인트**: 고객이 자기 주문 목록을 최근 순서로 쪽을 나눠 조회한다.

### User Story 3 — 로그인하지 않은 사람은 주문을 조회할 수 없다 (Priority: P1)

**목표**: 토큰이 없거나 서명이 틀리거나 만료된 토큰으로 하는 목록 조회, 주문 하나 조회, 관리자 조회는 401로 거부한다.

**독립 테스트** (spec User Story 3): 로그인 정보 없이, 서명이 틀린 토큰으로, 만료된 토큰으로 조회를 보내 401을 받는지 확인한다.

**이 스토리에는 구현 작업이 없다.** 기존 `SecurityConfig`의 `anyRequest().authenticated()`와 `ProblemAuthenticationEntryPoint`가 조회 세 경로를 이미 401로 막는다. 이 절은 그 동작을 시나리오로 고정하는 테스트와, 테스트가 결함을 잡는지 보는 확인으로 이루어진다.

#### 인수 시나리오 테스트

- [x] T039 [US3] 시나리오 3.1의 서비스 통합 테스트를 쓴다. 토큰 없이 `GET /orders`, `GET /orders/{임의의 번호}`, `GET /admin/orders`를 부르면 각각 401이다. 서명이 틀린 토큰과 만료된 토큰으로 `GET /orders`를 부르면 각각 401이다. `@DisplayName`은 시나리오 3.1의 테스트 케이스 ID로 시작한다. 기대값은 `docs/test-cases/order-placement.md`에서 그대로 가져온다 (services/order-service/src/test/java/com/example/msa/order/integration/UnauthenticatedViewIntegrationTest.java)
  - 틀린 토큰과 만료된 토큰은 001의 `InvalidTokenIntegrationTest`와 같은 방법으로 만든다. 이 클래스 안에 테스트용 RSA 키 쌍, 토큰을 만드는 메서드, 테스트용 `JwtDecoder`를 세우는 `@TestConfiguration`을 둔다 `[제안]`. `InvalidTokenIntegrationTest`는 고치지 않는다. 그 클래스의 도우미가 `private`이라서 가져다 쓸 수 없다. Spring Security Test의 `jwt()`는 토큰 검증을 건너뛰므로 이 테스트에는 쓰지 않는다.
  - 401 응답 본문이 Problem Details(`application/problem+json`)이고 토큰이 없으면 `code`가 `UNAUTHORIZED`인지 단언한다(공통 전제의 주문 API 오류 응답 본문 줄, 001이 이미 만든 동작이다).
- [x] T040 [US3] 위 테스트가 **통과하는 것**을 확인한다. 이것이 미리 정한 예외다 (services/order-service/src/test/java/com/example/msa/order/integration/UnauthenticatedViewIntegrationTest.java)
  - 명령: `.\gradlew.bat :services:order-service:test --tests "com.example.msa.order.integration.UnauthenticatedViewIntegrationTest"`
  - 기대 결과: 종료 코드 0. 구현 없이 통과하는 이유는 위 "이 스토리에는 구현 작업이 없다"에 적었다. 실패하면 멈추고 보고한다. 001의 인증 동작이나 이 테스트에 문제가 있다는 뜻이다.
- [x] T041 [US3] 이 테스트가 결함을 잡는지 되돌려 확인한다 (services/order-service/src/main/java/com/example/msa/order/config/SecurityConfig.java)
  - 1단계: `SecurityConfig`에서 `anyRequest().authenticated()`를 `anyRequest().permitAll()`로 잠시 바꾼다.
  - 2단계: T040의 명령을 다시 실행한다. 토큰 없는 요청이 401이 아니게 되어 테스트가 실패해야 한다. 실패를 보지 못하면 멈추고 사용자에게 묻는다.
  - 3단계: 바꾼 줄을 되돌린다. `git diff -- services/order-service/src/main/java/com/example/msa/order/config/SecurityConfig.java`의 출력이 없어야 한다.
  - 4단계: T040의 명령을 다시 실행해 종료 코드 0을 본다. 실행한 명령과 종료 코드를 모두 보고에 적는다.

**이 절의 시나리오 원문**

> **TC-025** (spec 시나리오 3.1) · `docs/test-cases/order-placement.md` 절 "TC-025 토큰이 없거나 서명이 틀리거나 만료된 토큰으로는 주문을 조회할 수 없다"
> - **When** 토큰 없이 주문 목록을 조회한다
> - **Then** 401을 받는다
> - **When** 토큰 없이 주문 하나를 조회한다
> - **Then** 401을 받는다
> - **When** 토큰 없이 관리자 조회를 한다
> - **Then** 401을 받는다
> - **When** 서명이 틀린 토큰으로 주문 목록을 조회한다
> - **Then** 401을 받는다
> - **When** 만료된 토큰으로 주문 목록을 조회한다
> - **Then** 401을 받는다

**체크포인트**: 토큰이 없거나 틀리거나 만료된 조회가 401이고, 그 테스트가 결함을 잡는 것을 확인했다.

### 마무리와 확인

- [x] T042 주문 서비스의 아키텍처 테스트가 새 클래스에도 통과하는지 확인한다 (services/order-service/src/test/java/com/example/msa/order/architecture/ArchitectureTest.java)
  - 명령: `.\gradlew.bat :services:order-service:test --tests "com.example.msa.order.architecture.ArchitectureTest"`
  - 기대 결과: 종료 코드 0. `ArchitectureTest`는 고치지 않는다. 규칙 다섯 개(controller는 mapper·client를 직접 부르지 않는다, mapper·client·domain은 service·controller를 모른다, domain은 Spring·MyBatis를 모른다, 애너테이션 SQL 금지, 트랜잭션 안 `client` 호출 금지)가 새 `OrderQueryController`, `OrderQueryService`, `domain`의 새 클래스, `dto`의 새 클래스에도 적용되어 통과해야 한다.
  - 실패하면 규칙을 고치지 않고 새 코드를 고친다. 규칙 예외나 검사를 끄는 표시를 사용자 승인 없이 넣지 않는다(저장소 루트 `CLAUDE.md` 7절).
- [x] T043 주문 서비스 규칙 문서의 "하는 일"과 "패키지 구조"에 고객 조회를 적는다. 1단계와 2단계로 나눠 고친다 `[제안]` (services/order-service/CLAUDE.md)
  - "하는 일" 목록에 `GET /orders`와 `GET /orders/{orderNo}`를 더한다. 로그인한 고객 본인의 주문만 돌려주고, 다른 고객의 주문과 없는 주문과 숫자가 아닌 주문 번호는 같은 404라는 것을 적는다. API 기준 문서로 `specs/002-view-orders/contracts/order-query-api.yaml`을 더한다.
  - "패키지 구조"의 `controller/`, `service/`, `mapper/`, `domain/`, `dto/`, `exception/` 줄에 이 단계의 새 클래스를 더한다. 관리자 조회에 해당하는 클래스(`ProblemAccessDeniedHandler`, `RealmRoleJwtConverter`, `AdminOrderResponse`)는 2단계 PR(T070)에서 더한다.
  - 그 밖의 부분(지킬 규칙, 테스트, DB, 용어)은 바꾸지 않는다. plan.md가 고치라고 한 곳은 "하는 일"과 패키지 구조뿐이다.
- [x] T044 이 단계의 기준을 확인한다. 이 단계 머리의 "끝났다고 보는 기준" 표에서 GitHub Actions를 뺀 네 줄을 차례로 실행한다 (gradle.properties)
  - 먼저 `docker ps`로 로컬 인프라가 켜져 있는지 보고, 켜져 있으면 내릴지 사용자에게 묻는다 `[제안]`.
  - 기대 결과: `msa.stage=T002-P1` 한 줄, `.\gradlew.bat build` 종료 코드 0, 이 단계 인수 시나리오 테스트 네 클래스의 종료 코드 0, 재고 서비스 파일이 바뀌지 않음. 실행한 명령과 종료 코드, 테스트 통과·실패 수를 보고에 적는다.
  - `build`에서 `checkTestCaseIds`가 통과하는 것을 본다. T005에서 실패했던 아홉 개 번호가 통과로 바뀐 것이 이 단계의 기계 검사 결과다.
  - Spotless가 서식 위반으로 실패하면 `.\gradlew.bat spotlessApply`로 서식만 맞추고 다시 실행한다 `[제안]`. `git diff --stat`으로 이 단계가 만든 파일 밖이 바뀌지 않았는지 본다.
- [x] T045 사용자가 push한 뒤 GitHub Actions 결과를 확인한다. push 전이면 "실행하지 않았다"로 보고한다 (.github/workflows/build.yml)
  - 명령: `gh run list --branch feat/002-view-orders-p1 --limit 1` → 결론이 `success`.
- [x] T046 시나리오 1.1~1.5, 2.1~2.6, 3.1을 하나씩 대조한다. quickstart.md 5절의 방법이다 (파일 없음, 대화창 보고)
  - 시나리오마다: (1) `docs/test-cases/order-placement.md`의 해당 절을 읽는다. (2) 테스트의 `@DisplayName`이 그 절의 번호로 시작하는지 본다. 명령: `Get-ChildItem services/order-service/src/test/java -Recurse -Filter *.java | Select-String -Pattern '@DisplayName\("TC-'`. 나온 줄의 번호를 이 단계의 "이 절의 시나리오 원문" 블록 첫 줄의 번호와 하나씩 맞춰 본다. (3) 테스트가 단언하는 값이 그 절의 Then과 같은지 본다. (4) 문서에 없는 값을 단언한 테스트가 없는지 본다.
  - **001의 테스트와 번호가 같은 시나리오**: 시나리오 1.1, 1.2, 2.1이 쓰는 번호는 001의 테스트도 같은 번호로 시작한다. 그래서 위 `Select-String` 결과만으로는 이 티켓의 테스트가 있는지 알 수 없고, 기계 검사도 이를 잡지 못한다("검사의 한계"). 시나리오 1.1, 1.2는 `ViewOrderIntegrationTest` 안에서, 시나리오 2.1은 `ListOrdersIntegrationTest` 안에서 "이 절의 시나리오 원문" 블록 첫 줄의 번호로 시작하는 `@DisplayName`이 있는지 따로 본다.
  - **계약 대조**: 테스트가 단언하는 응답 필드 이름과 값이 `specs/002-view-orders/contracts/order-query-api.yaml`과 같은지 눈으로 대조한다. 계약을 검증하는 도구는 만들지 않는다. 외부 API의 계약 테스트 층이 없다(맨 끝 "사용자 확인 필요" 6번).
  - 결과를 시나리오 12개 × (테스트 클래스와 메서드, `@DisplayName` 시작, 단언한 값, 문서와 같은가) 표로 보고에 적는다.
- [x] T047 research.md 4절의 "확인하지 못한 것" 가운데 이 단계에서 확인할 수 있는 것의 결과를 적는다 (파일 없음, 대화창 보고)
  - 인라인 뷰로 자른 뒤 항목을 합치는 SQL이 Oracle 21c XE에서 도는지: T031, T034의 결과.
  - `IntegrationTestSupport`를 직접 상속한 테스트의 컨텍스트가 뜨는지: T012의 결과.
  - `LocalDateTime`으로 읽은 주문 시각과 JSON 글자 모양: T011이 남긴 로그의 `createdAt` 값을 적는다. 시각대(Oracle 컨테이너의 시각대, `SYSTIMESTAMP`와의 일치)는 비교하지 않았다. 단언할 기대값이 문서에 없다. 비교하지 못했으면 "확인하지 못했다"로 적는다.
  - 쪽 지정 매개변수가 정수가 아닐 때 400이 나오는 구현: 구현했지만 문서에 기대값이 없어서 단언하지 않았다. "테스트하지 않았다"로 적는다.
- [x] T048 사용자에게 보고하고 멈춘다. 만들거나 고친 파일, 각 확인 작업의 명령과 결과, 시나리오 1.1~1.5, 2.1~2.6, 3.1을 하나씩 대조한 결과, T041의 되돌려 확인한 결과, T047의 결과를 적는다. 커밋·push·PR은 사용자가 요청할 때 한다 (파일 없음, 대화창 보고)

이 목록에서는 항목 ID를 쓰지 않았다.

---

## Phase 2: T002-P2 단계 PR — 관리자 조회 (브랜치 `feat/002-view-orders-p2`)

**목적**: 관리자가 모든 고객의 주문을 상태와 재고 해제 결과로 조회한다. 관리자가 아닌 사람의 관리자 조회는 403이다(spec User Story 4). 보안 설정을 바꾸는 부분(`/admin/**` 규칙, 역할 변환기, 403 처리기)이 이 단계에만 있다.

**담는 것** (plan.md "PR 단계" 표의 2단계 줄): `/admin/**` 규칙, `RealmRoleJwtConverter`, `ProblemAccessDeniedHandler`, `GET /admin/orders`와 `releaseStatus`, 상태·해제 결과 조건, 시나리오 4.1~4.4의 테스트, 주문 서비스 `CLAUDE.md`의 관리자 조회 부분. `msa.stage=T002-P2`.

**끝났다고 보는 기준** (quickstart.md 3절의 2단계 줄): `.\gradlew.bat build` 종료 코드 0, `AdminOrderSearchIntegrationTest`와 `RealmRoleJwtConverterTest`와 1단계 테스트가 통과, GitHub Actions 성공

| 확인할 것 | 명령 | 기대 결과 |
|---|---|---|
| 단계 표시 | `Select-String -Path gradle.properties -Pattern '^msa.stage=T002-P2$'` | 한 줄 |
| 빌드와 모든 검사 | `.\gradlew.bat build` | 종료 코드 0. 1단계 테스트와 001의 테스트가 그대로 통과한다 |
| 이 단계의 인수 시나리오 테스트 | `.\gradlew.bat :services:order-service:test --tests "com.example.msa.order.integration.AdminOrderSearchIntegrationTest" --tests "com.example.msa.order.config.RealmRoleJwtConverterTest"` | 종료 코드 0. 시나리오 4.1~4.4의 테스트가 통과로 나온다 |
| 재고 서비스 변경 없음 | `git fetch origin` 뒤 `git diff origin/main --stat -- services/inventory-service` | 출력 없음 |
| GitHub Actions | 사용자가 push한 뒤 `gh run list --branch feat/002-view-orders-p2 --limit 1` | 결론이 `success` |

**시작 조건**: Phase 1의 PR이 main에 병합되어 있고, 이 단계의 브랜치를 최신 main에서 만들었다. 브랜치를 만드는 일은 사용자가 한다.

- 확인 명령: `git branch --show-current` → `feat/002-view-orders-p2`
- 확인 명령: `Select-String -Path gradle.properties -Pattern '^msa.stage=T002-P1$'` → 한 줄 (이 단계가 시작할 때 값이다)
- 확인 명령: `Select-String -Path build-logic/src/main/groovy/msa/quality/TestCaseIdCheck.groovy -Pattern "T002-P2"` → 한 줄 이상 (단계 이름이 1단계 PR에서 이미 들어갔다)

### 준비

- [x] T049 단계 표시를 올리는 변경을 사용자에게 보여 주고 승인을 받는다. 사용자 확인 필요 (파일 없음, 대화창 보고)
  - 보여 줄 것: `gradle.properties`의 `msa.stage`를 `T002-P1`에서 `T002-P2`로 올린다. 이유는 2단계 PR에서 단계가 `T002-P2`인 줄(시나리오 4.1, 4.2, 4.3, 4.4의 테스트 케이스)이 검사 대상이 되어, 이 PR의 테스트가 있어야 빌드가 통과하게 하려는 것이다.
  - 단계 표(`test-case-stages.csv`)와 단계 순서 목록(`STAGE_ORDER`)은 1단계 PR에서 이미 고쳤으므로 이 단계에서는 바꾸지 않는다. 1단계 PR에서 바뀐 내용이 승인받은 대로 main에 있는지도 함께 보인다.
- [x] T050 단계 표시를 `msa.stage=T002-P2`로 바꾼다. 사용자 확인 필요 (gradle.properties)
- [x] T051 단계 표시가 승인받은 내용과 같고, 2단계 테스트가 아직 없음을 기계 검사가 잡는지 확인한다 (gradle.properties)
  - 명령: `git diff --stat` → 바뀐 파일이 `gradle.properties` 하나다.
  - 명령: `.\gradlew.bat :services:order-service:checkTestCaseIds` → **실패해야 한다**. 실패 메시지에 나오는 번호는 시나리오 4.3, 4.4의 테스트 케이스 번호 둘이다. 시나리오 4.1, 4.2의 번호는 001의 테스트가 이미 있어서 나오지 않는다(위 "검사의 한계"). 이 둘이 T071에서 통과로 바뀐다. 통과하거나 다른 번호가 나오면 멈추고 사용자에게 묻는다.
  - 명령: `.\gradlew.bat :services:inventory-service:checkTestCaseIds` → 종료 코드 0.

### User Story 4 — 관리자가 상태와 해제 결과로 주문을 조회한다 (Priority: P2)

**목표**: 관리자(`ADMIN` 역할)가 모든 고객의 주문을 `status`, `releaseStatus` 조건으로 조회한다. 조건은 생략할 수 있고 둘을 함께 주면 모두 맞는 주문만 나온다. 없는 이름이나 값은 400이다. 응답에는 재고 해제 결과(`releaseStatus`)가 함께 나온다. 관리자가 아닌 사람은 403이다.

**독립 테스트** (spec User Story 4): 고객 C1, C2와 관리자 M1을 준비하고 상태와 해제 결과가 다른 주문을 준비한다. C1이 관리자 조회를 요청하면 403을 받고, M1이 조건을 바꿔 조회하면 조건에 맞는 주문이 재고 해제 결과와 함께 나오는지 확인한다.

#### 인수 시나리오 테스트 (구현보다 먼저)

테스트 네 개는 한 클래스 `AdminOrderSearchIntegrationTest`에 모은다(plan.md 표). 같은 파일에 메서드를 더하므로 `[P]`를 붙이지 않았다. 관리자 흉내는 `ViewTestSupport`의 `admin(M1)`이다. 이 흉내는 역할 변환기를 거치지 않는다. 변환기가 `realm_access.roles`를 읽는지는 T057이 확인한다.

- [x] T052 [US4] 시나리오 4.1의 서비스 통합 테스트를 쓴다. 이 시나리오는 "C1이 관리자 조회를 요청한다 → 403을 받는다" 줄만 검증한다. 일반 고객 C1(`customer(C1)`, 관리자 권한 없음)이 `GET /admin/orders`를 부르면 상태 403이고, 응답 본문이 Problem Details(`application/problem+json`)이며 `code`가 `FORBIDDEN`인지 단언한다(공통 전제의 조회 API 오류 응답 줄). `@DisplayName`은 시나리오 4.1의 테스트 케이스 ID로 시작한다. 기대값은 `docs/test-cases/order-placement.md`에서 그대로 가져온다 (services/order-service/src/test/java/com/example/msa/order/integration/AdminOrderSearchIntegrationTest.java)
- [x] T053 [US4] 시나리오 4.2의 서비스 통합 테스트를 같은 파일에 더한다. C1과 C2에게 각각 실패 주문(해제 결과는 서로 다른 값으로 준다)을 준비한다. M1(`admin(M1)`)이 `GET /admin/orders?status=FAILED`로 조회하면 상태 200이고 두 주문이 모두 나오며, 각 주문에 준비한 `releaseStatus` 값이 함께 있는지 단언한다. `@DisplayName`은 시나리오 4.2의 테스트 케이스 ID로 시작한다 (services/order-service/src/test/java/com/example/msa/order/integration/AdminOrderSearchIntegrationTest.java)
  - 응답에 주문한 고객의 ID가 있는지는 단언하지 않는다(문서에 없다. 맨 끝 "사용자 확인 필요" 4번).
- [x] T054 [US4] 시나리오 4.3의 서비스 통합 테스트를 같은 파일에 더한다. C1의 확정 주문, C1의 실패 주문(해제 결과 해제 완료 `RELEASED`), C2의 실패 주문(해제 결과 해제 실패 `RELEASE_FAILED`)을 준비한다. M1이 조건 없이 조회하면 세 주문이 모두 나온다. `status=FAILED&releaseStatus=RELEASE_FAILED`로 조회하면 C2의 실패 주문만 나온다. `releaseStatus=RELEASE_FAILED`만 주면 C2의 실패 주문만 나온다. 없는 상태 이름 `status=UNKNOWN`은 400이고 없는 해제 결과 값 `releaseStatus=UNKNOWN`도 400이며 `code`가 `INVALID_QUERY_PARAMETER`다. `@DisplayName`은 시나리오 4.3의 테스트 케이스 ID로 시작한다 (services/order-service/src/test/java/com/example/msa/order/integration/AdminOrderSearchIntegrationTest.java)
  - 값 `UNKNOWN`은 테스트가 고른 "없는 이름"이다 `[제안]`. 대소문자 구분과 빈 값(`status=`)은 단언하지 않는다(문서에 없다. 맨 끝 "사용자 확인 필요" 4번).
- [x] T055 [US4] 시나리오 4.4의 서비스 통합 테스트를 같은 파일에 더한다. C1의 실패 주문(해제 실패), C2의 실패 주문(해제 완료), C2의 실패 주문(해제 결과 비어 있음), C1의 처리중 주문을 준비한다. `releaseStatus=RELEASE_FAILED`로 조회하면 C1의 해제 실패 주문만 나온다. `status=FAILED`로 조회하면 실패 주문 세 개가 나오고 해제 결과가 비어 있는 주문의 `releaseStatus`는 필드가 있고 값이 `null`이다. `status=PENDING`으로 조회하면 C1의 처리중 주문만 나온다. `@DisplayName`은 시나리오 4.4의 테스트 케이스 ID로 시작한다 (services/order-service/src/test/java/com/example/msa/order/integration/AdminOrderSearchIntegrationTest.java)
- [x] T056 [US4] 위 테스트가 실패하는 것을 확인한다 (services/order-service/src/test/java/com/example/msa/order/integration/AdminOrderSearchIntegrationTest.java)
  - 명령: `.\gradlew.bat :services:order-service:test --tests "com.example.msa.order.integration.AdminOrderSearchIntegrationTest"`
  - 기대 결과: 종료 코드가 0이 아니고, T052~T055의 테스트가 모두 실패한다. 실패 이유는 관리자 조회 API가 없어서 생긴 것이다(`/admin/orders`가 404). 하나라도 통과하면 멈추고 사용자에게 묻는다.

**이 절의 시나리오 원문**

> **TC-010** (spec 시나리오 4.1, 4.2) · `docs/test-cases/order-placement.md` 절 "TC-010 로그인한 사람만 주문하고, 자기 주문만 본다"
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

시나리오 4.1이 검증하는 줄은 위 인용의 여덟째 줄과 아홉째 줄(C1이 관리자 조회를 요청한다, 403을 받는다)이다. 시나리오 4.2가 검증하는 줄은 열째 줄부터 열두째 줄까지(C1과 C2에게 각각 "실패" 주문이 있다, M1이 상태 "실패"로 관리자 조회를 한다, C1과 C2의 실패 주문이 모두 재고 해제 결과와 함께 나온다)이다.

> **TC-027** (spec 시나리오 4.3) · `docs/test-cases/order-placement.md` 절 "TC-027 관리자 조회의 조건은 생략할 수 있고, 둘을 함께 주면 모두 맞아야 하며, 잘못된 값은 거부한다"
> - **Given** C1의 "확정" 주문, C1의 "실패" 주문(해제 결과 "해제 완료"), C2의 "실패" 주문(해제 결과 "해제 실패")이 있다
> - **When** M1이 조건 없이 관리자 조회를 한다
> - **Then** 세 주문이 모두 나온다
> - **When** M1이 상태 "실패"와 해제 결과 "해제 실패"를 함께 주고 관리자 조회를 한다
> - **Then** C2의 "실패" 주문만 나온다
> - **When** M1이 해제 결과 "해제 실패"만 주고 관리자 조회를 한다
> - **Then** C2의 "실패" 주문만 나온다
> - **When** M1이 없는 상태 이름으로 관리자 조회를 한다
> - **Then** 400을 받는다
> - **When** M1이 없는 해제 결과 값으로 관리자 조회를 한다
> - **Then** 400을 받는다

> **TC-032** (spec 시나리오 4.4) · `docs/test-cases/order-placement.md` 절 "TC-032 관리자는 해제 실패 주문, 해제 결과가 비어 있는 주문, 처리중 주문을 찾는다"
> - **Given** C1의 "실패" 주문(해제 결과 "해제 실패"), C2의 "실패" 주문(해제 결과 "해제 완료"), C2의 "실패" 주문(해제 결과가 비어 있음), C1의 "처리중" 주문이 있다
> - **When** M1이 해제 결과 "해제 실패"로 관리자 조회를 한다
> - **Then** C1의 해제 실패 주문만 나온다
> - **When** M1이 상태 "실패"로 관리자 조회를 한다
> - **Then** "실패" 주문 세 개가 나오고, 해제 결과가 비어 있는 주문은 비어 있는 채로 나온다
> - **When** M1이 상태 "처리중"으로 관리자 조회를 한다
> - **Then** C1의 "처리중" 주문만 나온다

#### 구현

- [x] T057 [P] [US4] 역할 변환기의 단위 테스트를 쓴다. 이 테스트는 아직 없는 `RealmRoleJwtConverter`를 참조하므로 컴파일 오류로 실패한다. 그 실패를 본 뒤 T060을 한다 (services/order-service/src/test/java/com/example/msa/order/config/RealmRoleJwtConverterTest.java)
  - 검사할 것: 변환한 인증 정보가 Spring Security의 `hasRole("ADMIN")` 판정을 통과하는지 본다 `[제안]`. 판정은 `AuthorityAuthorizationManager.hasRole("ADMIN")`으로 한다 `[제안]`. 이 API가 Spring Security 7.1.1에 맞는지는 컴파일하지 않아서 확인하지 못했다. 토큰의 `realm_access.roles`에 `ADMIN`이 있으면 통과한다. `CUSTOMER`만 있으면 통과하지 못한다. `realm_access` 클레임이 없는 토큰도 통과하지 못한다.
  - **권한 이름 글자(`ROLE_` 접두어)는 단언하지 않는다.** 사용자가 2026-10-13에 정했다(맨 끝 "사용자 확인 필요" 4번). `AuthorityAuthorizationManager.hasRole`이 접두어를 붙여 찾는 것은 Spring Security 쪽의 일이다.
  - 기대값 출처: `docs/design/architecture.md` 7절의 「역할은 Keycloak realm 역할(`CUSTOMER`, `ADMIN`)을 Spring Security 권한으로 바꿔 쓴다.」. `ADMIN`만 관리자 판정을 통과한다는 기대값은 시나리오 4.1의 "관리자가 아닌 사람은 403"에서 나온다. 토큰의 `realm_access.roles` 클레임 모양은 어떤 기준 문서에도 없다. 이 테스트는 그 모양을 입력으로만 쓴다. 실제 Keycloak 토큰이 이 모양인지는 티켓 003의 E2E에서 확인한다.
  - 실제 Keycloak 토큰의 `realm_access.roles` 구조는 이 환경에서 확인하지 못했다. 티켓 003의 E2E에서 확인한다(research.md 4절).
- [x] T058 [P] [US4] 관리자 조회 조건의 단위 테스트를 쓴다. 이 테스트는 아직 없는 `OrderSearch`를 참조하므로 컴파일 오류로 실패한다. 그 실패를 본 뒤 T061을 한다 (services/order-service/src/test/java/com/example/msa/order/domain/OrderSearchTest.java)
  - 검사할 것: 조건이 둘 다 없으면 조건 없음이다. 상태 `FAILED`와 해제 결과 `RELEASE_FAILED`는 각각 맞는 값이다. 상태 `UNKNOWN`은 틀리고 해제 결과 `UNKNOWN`도 틀리다(시나리오 4.3). 상태 네 개(`PENDING`, `CONFIRMED`, `REJECTED`, `FAILED`)와 해제 결과 세 개(`NOT_REQUIRED`, `RELEASED`, `RELEASE_FAILED`)가 모두 맞는 값이다. 이 이름들은 001이 만든 `OrderStatus`, `ReleaseStatus`의 값이다.
  - 소문자 이름과 빈 글자는 단언하지 않는다(문서에 없다).
- [x] T059 [US4] 조회 Mapper 테스트에 관리자 조회 SQL을 검사하는 메서드를 더한다. plan.md 표의 시나리오 4.3, 4.4 "Mapper" 칸이다. 아직 없는 Mapper 메서드를 참조하므로 컴파일 오류로 실패한다. 그 실패를 본 뒤 T063을 한다 (services/order-service/src/test/java/com/example/msa/order/mapper/OrderQueryMapperTest.java)
  - 상태 조건, 해제 결과 조건, 둘 다 준 조건, 조건 없음이 각각 맞는 주문만 읽는다(시나리오 4.3의 값). 모든 고객의 주문이 나온다.
  - 해제 결과가 비어 있는 주문의 `releaseStatus`를 `null`로 읽는다(시나리오 4.4).
  - 새 Mapper 메서드 이름은 `selectAdminOrders(status, releaseStatus, offset, limit)`로 한다 `[제안]`.
- [x] T060 [US4] 역할 변환기를 만든다. 검증한 JWT를 인증 정보로 바꾸는 `RealmRoleJwtConverter`다. 토큰의 `realm_access.roles` 목록의 이름마다 `ROLE_` 접두어를 붙여 권한으로 만든다(`ADMIN` → `ROLE_ADMIN`). `realm_access`나 `roles`가 없으면 권한이 없다 (services/order-service/src/main/java/com/example/msa/order/config/RealmRoleJwtConverter.java)
  - 근거: research.md 항목 9. Spring Security의 기본 변환기는 `scope` 값만 읽어서 Keycloak의 realm 역할을 권한으로 바꾸지 못한다.
  - Spring Security 7.1.1의 변환기 API가 맞는지는 컴파일하지 않아서 확인하지 못했다(research.md 4절). T057이 통과하면 확인된다. 맞지 않으면 멈추고 사용자에게 묻는다.
- [x] T061 [US4] 관리자 조회 조건을 만든다. 순수 Java 값 객체 `OrderSearch`가 글자 `status`, `releaseStatus`를 검사해서 `[제안]` `OrderStatus`, `ReleaseStatus` 또는 없음으로 바꾼다. 틀린 값은 `InvalidQueryException`을 던진다 (services/order-service/src/main/java/com/example/msa/order/domain/OrderSearch.java)
  - 규칙은 data-model.md 4절의 원문을 따른다. `status`의 규칙은 「`PENDING`, `CONFIRMED`, `REJECTED`, `FAILED` 가운데 하나. 없으면 조건 없음. 대소문자를 구분한다. 빈 값은 틀린 값이다」이고 `releaseStatus`의 규칙은 「`NOT_REQUIRED`, `RELEASED`, `RELEASE_FAILED` 가운데 하나. 없으면 조건 없음. 대소문자를 구분한다. 빈 값은 틀린 값이다」이다. 대소문자와 빈 값의 처리는 문서에 기대값이 없어서 단언하지 않는다. 구현은 data-model.md를 따른다.
  - 틀린 매개변수 이름(`status`, `releaseStatus`)을 `InvalidQueryException`의 `field`에 담는다.
- [x] T062 [P] [US4] 관리자용 응답 DTO를 만든다. record `AdminOrderResponse`가 `OrderDetailResponse`의 모든 필드에 `releaseStatus`를 더한 모양이다. `OrderWithItems`에서 만드는 `from(OrderWithItems)`가 있다 (services/order-service/src/main/java/com/example/msa/order/dto/AdminOrderResponse.java)
  - 필드 규칙은 data-model.md 2절의 원문을 따른다: `releaseStatus`는 「`NOT_REQUIRED`(해제 불필요), `RELEASED`(해제 완료), `RELEASE_FAILED`(해제 실패). 해제 요청 전이거나 "실패"가 아니면 `null`. 필드는 항상 있다」. 「고객용 응답(`OrderDetailResponse`)에는 `releaseStatus`가 없다.」
  - 주문한 고객의 ID(`customerId`)는 넣지 않는다. plan 그대로다 `[제안]`. 사용자는 2026-10-13에 이를 문서에 더하지 않고 테스트도 단언하지 않기로만 정했다(맨 끝 "사용자 확인 필요" 4번).
- [x] T063 [US4] 관리자 조회 SQL을 만든다 (services/order-service/src/main/resources/mapper/OrderMapper.xml, services/order-service/src/main/java/com/example/msa/order/mapper/OrderMapper.java)
  - `selectAdminOrders`: T034와 같은 인라인 뷰 모양이다. 안쪽 SELECT에 고객 조건은 없고, MyBatis 동적 SQL(`<where>`와 `<if>`)로 `status`가 있을 때만 `STATUS = #{status}`, `releaseStatus`가 있을 때만 `RELEASE_STATUS = #{releaseStatus}`를 붙인다. 둘 다 있으면 둘 다 맞는 주문만 나온다. `ORDER BY ORDER_NO DESC`, `OFFSET`, `FETCH NEXT`, 바깥 `LEFT JOIN`, `orderViewMap`은 T034와 같다.
  - 이 작업이 끝나면 T059의 테스트가 통과한다.
- [x] T064 [P] [US4] 403 응답을 만든다. `ProblemDetailsHandler`에 `forbidden()`(이름 `[제안]`)을 더하고, `ProblemAccessDeniedHandler`를 새로 만든다 (services/order-service/src/main/java/com/example/msa/order/exception/ProblemDetailsHandler.java, services/order-service/src/main/java/com/example/msa/order/exception/ProblemAccessDeniedHandler.java)
  - `code`는 `FORBIDDEN`, `type`은 `urn:msa-example:problem:FORBIDDEN`이다.
  - `ProblemAccessDeniedHandler`는 Spring Security의 `AccessDeniedHandler`를 구현한 `@Component`다. 기존 `ProblemAuthenticationEntryPoint`와 같은 방식으로 `ProblemDetailsHandler.problem(...)`으로 본문을 만들어 `application/problem+json`으로 쓴다. 보안 필터에서 나는 오류라 컨트롤러 예외 처리기가 닿지 못하기 때문이다. 토큰 내용은 응답과 로그에 담지 않는다.
- [x] T065 [US4] 보안 설정을 바꾼다. 세 가지다 (services/order-service/src/main/java/com/example/msa/order/config/SecurityConfig.java)
  - `authorizeHttpRequests`에서 `anyRequest()` 앞에 `requestMatchers("/admin/**").hasRole("ADMIN")`을 더한다. `GET /admin/orders` 하나가 아니라 `/admin/` 아래 전체를 막는다. 토큰이 없으면 이 규칙보다 인증 실패가 먼저라서 403이 아니라 401이다(research.md 항목 9).
  - `oauth2ResourceServer`의 `jwt(...)` 설정에 `RealmRoleJwtConverter`를 연결한다. 지금은 `Customizer.withDefaults()`다.
  - `exceptionHandling`에 `accessDeniedHandler(ProblemAccessDeniedHandler)`를 연결한다. `securityFilterChain` 메서드의 매개변수에 `ProblemAccessDeniedHandler`가 더해진다.
  - 클래스 설명 주석에 `/admin/**` 규칙을 한 줄 더한다. 다른 줄과 `POST /orders`·health 규칙은 바꾸지 않는다.
  - `oauth2ResourceServer`가 자기 403 처리기를 따로 둘 수 있는지는 확인하지 못했다(research.md 4절). T052가 403 응답 본문이 `application/problem+json`이고 `code`가 `FORBIDDEN`인지 단언하므로 거기서 확인된다.
- [x] T066 [US4] 조회 서비스에 관리자 조회 메서드를 더한다. `searchAll(OrderSearch, PageRequest)`(이름 `[제안]`)가 `selectAdminOrders`를 부르고 `AdminOrderResponse`로 바꿔 `OrderPage.of`로 봉투를 만든다. 메서드에 `@Transactional(readOnly = true)`를 붙인다 (services/order-service/src/main/java/com/example/msa/order/service/OrderQueryService.java)
- [x] T067 [US4] 조회 컨트롤러에 `GET /admin/orders`를 더한다. `status`, `releaseStatus`, `page`, `size`를 선택 쿼리 매개변수 글자로 받아 `[제안]` `OrderSearch`와 `PageRequest`로 만들어 서비스에 넘기고, 200과 `OrderPage<AdminOrderResponse>`를 돌려준다 (services/order-service/src/main/java/com/example/msa/order/controller/OrderQueryController.java)
  - 컨트롤러는 권한 판단을 하지 않는다. `/admin/**` 규칙(T065)이 403을 낸다. 코딩 규약 3-1절이 컨트롤러의 업무 판단을 금지한다.
- [x] T068 [US4] 이 절의 테스트와 앞 단계의 테스트가 모두 통과하는지 확인한다 (services/order-service/src/test/java/com/example/msa/order/integration/AdminOrderSearchIntegrationTest.java)
  - 명령: `.\gradlew.bat :services:order-service:test`
  - 기대 결과: 종료 코드 0. 통과·실패 수를 보고에 적는다. 이 명령은 001의 테스트와 1단계 테스트도 함께 돈다.

이 목록에서는 항목 ID를 쓰지 않았다.

**체크포인트**: 관리자가 상태와 해제 결과로 모든 고객의 주문을 조회하고, 관리자가 아닌 사람은 403이다.

### 마무리와 확인

- [x] T069 주문 서비스의 아키텍처 테스트가 새 클래스에도 통과하는지 확인한다 (services/order-service/src/test/java/com/example/msa/order/architecture/ArchitectureTest.java)
  - 명령: `.\gradlew.bat :services:order-service:test --tests "com.example.msa.order.architecture.ArchitectureTest"`
  - 기대 결과: 종료 코드 0. T042와 같은 다섯 규칙이 새 `RealmRoleJwtConverter`(`config`), `ProblemAccessDeniedHandler`(`exception`), `OrderSearch`(`domain`), `AdminOrderResponse`(`dto`)에도 적용되어 통과해야 한다. 규칙을 고치지 않는다.
- [x] T070 주문 서비스 규칙 문서에 관리자 조회를 더한다. 1단계와 2단계로 나눠 고친다 `[제안]` (services/order-service/CLAUDE.md)
  - "하는 일" 목록에 `GET /admin/orders`를 더한다. `ADMIN` 역할만 부를 수 있고, `/admin/` 아래 전체가 `ADMIN` 규칙에 걸린다는 것과 응답에 재고 해제 결과가 함께 나온다는 것을 적는다.
  - "패키지 구조"의 `exception/`, `config/`, `dto/` 줄에 `ProblemAccessDeniedHandler`, `RealmRoleJwtConverter`, `AdminOrderResponse`를 더한다.
  - 그 밖의 부분은 바꾸지 않는다.
- [x] T071 이 단계의 기준을 확인한다. 이 단계 머리의 "끝났다고 보는 기준" 표에서 GitHub Actions를 뺀 네 줄을 차례로 실행한다 (gradle.properties)
  - 먼저 `docker ps`로 로컬 인프라가 켜져 있는지 보고, 켜져 있으면 내릴지 사용자에게 묻는다 `[제안]`.
  - 기대 결과: `msa.stage=T002-P2` 한 줄, `.\gradlew.bat build` 종료 코드 0, 이 단계 인수 시나리오 테스트의 종료 코드 0, 재고 서비스 파일이 바뀌지 않음. 실행한 명령과 종료 코드, 테스트 통과·실패 수를 보고에 적는다.
  - `build`에서 `checkTestCaseIds`가 통과하는 것을 본다. T051에서 실패했던 두 개 번호가 통과로 바뀐 것이 이 단계의 기계 검사 결과다.
  - Spotless가 서식 위반으로 실패하면 `.\gradlew.bat spotlessApply`로 서식만 맞추고 다시 실행한다 `[제안]`.
- [ ] T072 사용자가 push한 뒤 GitHub Actions 결과를 확인한다. push 전이면 "실행하지 않았다"로 보고한다 (.github/workflows/build.yml)
  - 명령: `gh run list --branch feat/002-view-orders-p2 --limit 1` → 결론이 `success`.
- [x] T073 시나리오 4.1~4.4를 하나씩 대조하고, 시나리오 16개를 모두 합쳐 확인한다. quickstart.md 5절의 방법이다 (파일 없음, 대화창 보고)
  - 시나리오마다: (1) 해당 절을 읽는다. (2) `@DisplayName`이 그 절의 번호로 시작하는지 본다. 명령: `Get-ChildItem services/order-service/src/test/java -Recurse -Filter *.java | Select-String -Pattern '@DisplayName\("TC-'`. 나온 줄의 번호를 이 단계의 "이 절의 시나리오 원문" 블록 첫 줄의 번호와 하나씩 맞춰 본다. (3) 단언하는 값이 Then과 같은지 본다. (4) 문서에 없는 값을 단언한 테스트가 없는지 본다.
  - 시나리오 4.1, 4.2가 쓰는 번호는 001의 테스트도 같은 번호로 시작하므로 `Select-String` 결과만으로 이 티켓의 테스트가 있는지 알 수 없다. `AdminOrderSearchIntegrationTest` 안에서 그 번호로 시작하는 `@DisplayName`이 둘 있는지 따로 본다.
  - **계약 대조**: 관리자 응답의 필드(`releaseStatus` 포함)와 오류 응답(`FORBIDDEN`, `INVALID_QUERY_PARAMETER`)이 `specs/002-view-orders/contracts/order-query-api.yaml`과 같은지 눈으로 대조한다. 새 도구를 더하지 않는다.
  - 시나리오 16개(1.1~1.5, 2.1~2.6, 3.1, 4.1~4.4)가 모두 통과하는 테스트에 짝지어졌는지 하나씩 적는다. 이 표가 spec의 첫째 성공 기준의 증거다.
- [x] T074 research.md 4절의 "확인하지 못한 것" 가운데 이 단계에서 확인할 수 있는 것의 결과를 적는다 (파일 없음, 대화창 보고)
  - `JwtAuthenticationConverter` 설정 방법이 Spring Security 7.1.1의 API와 맞는지: T057, T060의 결과.
  - Spring Security가 403을 `application/problem+json`으로 내는지: T052의 결과.
  - 상태·해제 결과 조건이 있는 관리자 조회가 Oracle에서 도는지: T059, T063의 결과.
  - Keycloak 26.8.0이 발급한 토큰에 `realm_access.roles`가 들어 있는지: 이 환경에서 확인하지 못했다. 티켓 003의 E2E에서 확인한다.
  - 관리자 조회(`STATUS`, `RELEASE_STATUS` 조건)의 실행 계획: 확인하지 않았다. 인덱스를 더하지 않았다.
- [x] T075 사용자에게 보고하고 멈춘다. 만들거나 고친 파일, 각 확인 작업의 명령과 결과, 시나리오 4.1~4.4와 16개 전체를 하나씩 대조한 결과, T074의 결과를 적는다. 커밋·push·PR은 사용자가 요청할 때 한다 (파일 없음, 대화창 보고)

이 목록에서는 항목 ID를 쓰지 않았다.

---

## plan 표와 작업 대응

plan.md "인수 시나리오와 테스트 층" 표의 줄마다, 그 줄의 단계·테스트 층에 놓인 작업 번호다. 표의 "—" 칸은 그 층에 테스트가 없다는 뜻이라 작업도 없다. 줄은 spec 시나리오 번호로 가리킨다. 모든 단계에서 함께 도는 아키텍처 테스트는 T042(1단계)와 T069(2단계)에서 확인한다.

| spec 시나리오 | 단계 | 단위 | Mapper | 서비스 통합 (인수) | 서비스 통합 테스트 클래스 |
|---|---|---|---|---|---|
| 1.1 | Phase 1 | — | T014 | T007 | `ViewOrderIntegrationTest` |
| 1.2 | Phase 1 | — | T014 | T008 | `ViewOrderIntegrationTest` |
| 1.3 | Phase 1 | T013 | — | T009 | `ViewOrderIntegrationTest` |
| 1.4 | Phase 1 | — | T014 | T010 | `ViewOrderIntegrationTest` |
| 1.5 | Phase 1 | — | T014 | T011 | `ViewOrderIntegrationTest` |
| 2.1 | Phase 1 | — | T031 | T022 | `ListOrdersIntegrationTest` |
| 2.2 | Phase 1 | — | T031 | T023 | `ListOrdersIntegrationTest` |
| 2.3 | Phase 1 | — | T031 | T024 | `ListOrdersIntegrationTest` |
| 2.4 | Phase 1 | — | T031 | T025 | `ListOrdersIntegrationTest` |
| 2.5 | Phase 1 | — | — | T026 | `ListOrdersIntegrationTest` |
| 2.6 | Phase 1 | T030 | T031 | T027 | `ListOrdersIntegrationTest` |
| 3.1 | Phase 1 | — | — | T039 | `UnauthenticatedViewIntegrationTest` |
| 4.1 | Phase 2 | T057 | — | T052 | `AdminOrderSearchIntegrationTest` |
| 4.2 | Phase 2 | — | — | T053 | `AdminOrderSearchIntegrationTest` |
| 4.3 | Phase 2 | T058 | T059 | T054 | `AdminOrderSearchIntegrationTest` |
| 4.4 | Phase 2 | — | T059 | T055 | `AdminOrderSearchIntegrationTest` |

받치는 테스트 가운데 시나리오 표에 줄이 없는 것:

| 테스트 | 층 | 단계 | 기대값 출처 | 작업 |
|---|---|---|---|---|
| `OrderOwnerQueryIntegrationTest` | 서비스 통합 | Phase 1 | `docs/requirements/uc-002-view-orders.md` 3절 "업무 규칙"의 첫 문장 | T028 |
| `OrderNumbersTest` | 단위 | Phase 1 | 시나리오 1.3의 기대값 | T013 |
| `PageRequestTest` | 단위 | Phase 1 | 공통 전제의 마지막 줄과 시나리오 2.6 | T030 |
| `OrderSearchTest` | 단위 | Phase 2 | 시나리오 4.3의 기대값 | T058 |
| `RealmRoleJwtConverterTest` | 단위 | Phase 2 | `docs/design/architecture.md` 7절, 시나리오 4.1 | T057 |
| `OrderQueryMapperTest` | Mapper | Phase 1, 2 | 위 표의 Mapper 칸 | T014, T031, T059 |

research.md 4절 "확인하지 못한 것"과 확인 작업:

| 확인하지 못한 것 (research.md 4절) | 단계 | 작업 |
|---|---|---|
| 인라인 뷰로 자른 뒤 `ORDER_ITEMS`를 합치는 SQL이 Oracle 21c XE에서 도는지 | Phase 1 | T031, T034, T047 |
| `JwtAuthenticationConverter` 설정 방법이 Spring Security 7.1.1의 API와 맞는지 | Phase 2 | T057, T060, T074 |
| Keycloak 26.8.0 토큰에 `realm_access.roles`가 들어 있는지 | 티켓 003 | 확인할 수 없다. T074가 "확인하지 못했다"로 적는다 |
| `IntegrationTestSupport`를 직접 상속한 테스트의 컨텍스트가 뜨는지 | Phase 1 | T012, T047 |
| 주문 시각의 값과 시각대, JSON 모양 | Phase 1 | T011, T047 (모양만. 시각대는 확인하지 않는다) |
| 관리자 조회의 실행 계획 | — | 확인하지 않는다. T074가 적는다 |
| 쪽 지정 매개변수가 정수가 아닐 때 400이 나오는 구현 방법 | Phase 1 | 구현하지만 문서에 기대값이 없어 단언하지 않는다. T047이 "테스트하지 않았다"로 적는다 |
| Spring Security 기본 403 처리기의 응답 본문 모양 | Phase 2 | T052, T074 |

이 절에서는 항목 ID를 쓰지 않았다.

---

## 의존 관계와 실행 순서

### 단계 사이

- **Phase 1 → Phase 2**: Phase 1의 PR이 main에 병합된 뒤, main에서 Phase 2 브랜치를 만든다. Phase 2는 Phase 1의 `ViewTestSupport`, `OrderQueryController`, `OrderQueryService`, `PageRequest`, `OrderPage`, `InvalidQueryException`, `orderViewMap`을 쓴다.
- 이 tasks.md를 담은 PR(`docs/002-view-orders-tasks`)은 Phase 1보다 먼저 병합한다.

### 단계 안

- 준비 → 기반 → 스토리 절 → 마무리 순서다. 기반이 끝나기 전에는 스토리 절을 시작하지 않는다.
- 스토리 절 안에서는 인수 시나리오 테스트 → 실패 확인 → 받치는 테스트와 구현 → 통과 확인 순서다.
- Phase 1: User Story 1 → 2 → 3. 스토리 1이 컨트롤러·서비스·`resultMap`의 뼈대를 만들고, 스토리 2가 같은 클래스에 목록 메서드를 더한다. 스토리 3은 구현이 없어서 앞 스토리와 독립이다.
- 같은 파일을 여러 작업이 넓히는 곳에는 `[P]`를 붙이지 않았다: `ViewOrderIntegrationTest`(T007~T011), `ListOrdersIntegrationTest`(T022~T027), `AdminOrderSearchIntegrationTest`(T052~T055), `OrderQueryMapperTest`(T014, T031, T059), `OrderMapper.xml`과 `OrderMapper.java`(T017, T034, T063), `ProblemDetailsHandler`(T018, T035, T064), `OrderQueryService`(T019, T036, T066), `OrderQueryController`(T020, T037, T067).

### 동시에 할 수 있는 작업의 예

```text
# Phase 1 준비: 서로 다른 파일 (T001 승인 뒤)
T002 config/quality/test-case-stages.csv
T003 build-logic/src/main/groovy/msa/quality/TestCaseIdCheck.groovy
T004 gradle.properties

# Phase 1 User Story 1 구현: 서로 다른 파일
T013 domain/OrderNumbersTest.java
T016 dto/OrderDetailResponse.java, dto/OrderWithItems.java
T018 exception/ProblemDetailsHandler.java

# Phase 1 User Story 2 구현: [P]는 T030 하나뿐이라 동시에 할 짝이 없다

# Phase 2 User Story 4 구현
T057 config/RealmRoleJwtConverterTest.java
T058 domain/OrderSearchTest.java
T062 dto/AdminOrderResponse.java
T064 exception/ProblemDetailsHandler.java, exception/ProblemAccessDeniedHandler.java
```

테스트 작업과 그 테스트가 참조하는 구현 작업은 동시에 하지 않는다. 구현은 테스트의 실패를 본 뒤에 한다. T015, T032, T033, T060, T061에 `[P]`가 없는 이유다. T033은 T032가 만드는 `PageRequest`를 인자로 받는다.

## 구현 전략

- **1단계가 MVP다.** 고객이 자기 주문의 결과를 볼 수 있어야 거절·실패 결과가 쓸모가 있다(spec User Story 1의 "Why this priority"). 관리자 조회는 고객 조회와 따로 동작하는 P2다.
- **단계마다 PR 하나를 병합한다.** 각 PR은 헌법을 지킨 채로 병합된다. 이 티켓은 다른 서비스를 부르지 않아서 제한 시간, 재시도, 서킷 브레이커가 해당 없다(plan.md Constitution Check).
- **한 단계 안에서는 스토리 순서대로 한다.** 스토리마다 체크포인트에서 그때까지의 테스트가 모두 통과하는지 본다.
- **001의 코드는 필요한 만큼만 고친다.** 고치는 곳은 `OrderMapper`·`OrderMapper.xml`(메서드와 SQL 추가), `OrderWithItems`(필드 추가), `ProblemDetailsHandler`(메서드 추가), `SecurityConfig`(2단계)뿐이다. `OrderController`, `OrderService`는 고치지 않는다.
- **새 라이브러리, 새 Flyway 파일, 새 인덱스, 루트 `contracts/` 변경이 없다.**

---

## 자동으로 고른 것

이번 실행에서 묻지 않고 고른 것이다. 본문에서 `[제안]`을 붙인 곳이다. 사용자가 받아들이면 본문의 태그를 지운다. plan.md, research.md, data-model.md의 원문을 옮기면서 따라온 `[제안]`은 이 표에 넣지 않았다. 그 태그는 원래 문서의 것이다.

| # | 고른 것 | 다른 선택지 | 고른 이유 | 작업 |
|---|---|---|---|---|
| 1 | 단계 이름 `T002-P1`, `T002-P2`와 브랜치 이름 `feat/002-view-orders-p1`, `feat/002-view-orders-p2`를 plan.md 제안 그대로 쓴다 | 다른 이름 | 사용자가 정하지 않았다. 브랜치 이름은 `docs/standards/git-workflow.md` 2절의 규칙(`feat/<폴더 이름>-p<단계 번호>`, 40자 이하)에 맞는다 | 머리말, T004, T050 |
| 2 | 설정 세 곳을 모두 1단계 PR에서 고치고, 2단계 PR에서는 `msa.stage`만 올린다 | 단계 순서 목록의 `T002-P2`를 2단계에서 더한다 | 001이 단계 표의 모든 줄을 첫 단계에서 넣은 방식과 같다. 단계 표 검사의 첫째 항목이 표 전체를 첫 단계부터 요구한다. plan.md도 1단계를 제안했다 | T001~T004, T049, T050 |
| 3 | 시나리오 1.2·2.1용 줄과 시나리오 4.1·4.2용 줄로, 같은 테스트 케이스 번호를 두 줄로 나눈다 | 한 줄(`T002-P1`)만 둔다 | 표에 같은 번호가 같은 서비스에 단계만 달리 두 줄 있는 전례가 있다. 검사 결과는 어느 쪽이든 같다(001의 테스트가 이미 그 번호로 시작한다). 줄이 둘이어야 4.1·4.2가 2단계에 속한다는 사실이 표에 남는다 | T002 |
| 4 | 단계 순서를 `P1, P2, P3, T002-P1, T002-P2`로 둔다 | `T002-P1, T002-P2`를 앞에 둔다 | 이 티켓의 단계에서도 001의 테스트가 계속 있어야 한다 | T003 |
| 5 | `msa.stage`를 각 단계의 준비 작업에서 올린다 | 단계 끝에서 올린다 | 001의 T082와 같다. 올린 직후 기계 검사가 이 단계의 테스트가 아직 없다고 실패하는 것을 볼 수 있다(T005, T051) | T004, T050 |
| 6 | 인수 시나리오마다 작업 하나를 두고, 같은 클래스 안에서 메서드를 더한다 | 클래스마다 작업 하나 | 시나리오 16개가 작업에 하나씩 짝지어져서 빠짐을 찾기 쉽다 | T007~T011, T022~T027, T052~T055 |
| 7 | 시나리오 3.1은 구현 작업 없이 "통과 확인 → 되돌려 확인하기"로 한다 | 실패 확인을 건너뛴다 | 구현 전에도 통과한다. 저장소 루트 `CLAUDE.md` 6절의 되돌려 확인하는 방식으로 테스트가 결함을 잡는 것을 보인다 | T040, T041 |
| 8 | `@Transactional(readOnly = true)`를 서비스 메서드마다 붙인다 | 클래스에 붙인다 | `ArchitectureTest` 주석이 "클래스가 아니라 메서드에 붙인다"고 설명한다. 그 설명이 계속 맞게 한다 | T019, T036, T066 |
| 9 | `OrderQueryController`에 클래스 `@RequestMapping`을 두지 않고 메서드마다 경로를 쓴다 | 클래스에 `/` 접두사를 둔다 | plan.md가 tasks.md에 넘겼다. 클래스 하나가 두 경로 접두사(`/orders`, `/admin/orders`)를 가지지 않는다 | T020 |
| 10 | 목록 봉투는 형식 매개변수가 있는 `OrderPage<T>` 하나다 | `OrderPage`와 `AdminOrderPage` 둘 | JSON 모양이 같다. 클래스 하나가 중복을 없앤다 | T033 |
| 11 | 서비스가 주문 번호 글자를 받아 숫자로 바꾸고, 결과가 없으면 `Optional`로 돌려준다. 컨트롤러가 404로 바꾼다 | 컨트롤러가 변환한다 | 코딩 규약 3-1절이 컨트롤러의 업무 판단을 금지한다. 서비스가 웹 예외를 던지지 않는다 | T019, T020 |
| 12 | 컨트롤러가 `page`, `size`, `status`, `releaseStatus`를 글자로 받아 `domain`의 값 객체에 넘긴다 | `int`와 enum으로 받는다 | 숫자 타입이나 enum으로 받으면 틀린 값에 Spring이 400을 낸다. 오류 본문의 `code`를 이 티켓이 정한다(`INVALID_QUERY_PARAMETER`) | T032, T037, T061, T067 |
| 13 | Mapper 메서드 이름 `selectCustomerOrder`, `selectCustomerOrders`, `selectAdminOrders`와 `resultMap` 이름 `orderViewMap` | 다른 이름 | plan.md는 개수(3개)만 정했다 | T014, T017, T031, T034, T059, T063 |
| 14 | 테스트 도우미의 상수와 메서드: `M1 = "admin-m1"`, `admin(sub)`(권한 `ROLE_ADMIN`), `seed(...)`, `get(...)`, `orderNos(...)` | 다른 이름과 값 | 문서에 값이 없다. 테스트가 정한 입력일 뿐 기대값이 아니다 | T006 |
| 15 | 시나리오 3.1 테스트 클래스 안에 RSA 키 쌍과 `JwtDecoder` 설정을 둔다 | `InvalidTokenIntegrationTest`의 도우미를 `ViewTestSupport`로 옮긴다 | 001의 테스트를 고치지 않는다. 도우미가 `private`이다 | T039 |
| 16 | 시나리오 4.3의 "없는 이름"으로 `UNKNOWN`을 쓴다 | 다른 글자 | 문서가 값을 정하지 않았다. research.md 3절이 같은 예를 들었다 | T054 |
| 17 | 시나리오 1.5는 `createdAt`이 비어 있지 않은 것만 단언하고, 값은 로그에 한 줄 남긴다 | 형식을 단언한다 | 문서가 "주문 시각이 나온다"까지만 정했다. 로그는 research.md 4절의 미확인 항목을 보고하려는 것이다 | T011, T047 |
| 18 | 주문 서비스 `CLAUDE.md`를 1단계에 고객 조회, 2단계에 관리자 조회로 나눠 고친다 | 2단계에서 한꺼번에 고친다 | 코드가 main에 들어가는 PR에서 규칙 문서가 그 코드를 설명한다 | T043, T070 |
| 19 | 응답 필드 이름과 값을 눈으로 계약 파일과 대조한다 | 계약 검증 도구를 더한다 | 이 저장소에 외부 API 계약을 검증하는 층이 없고, 새 도구는 사용자 확인 없이 더하지 않는다. research.md 5절이 이 선택을 사용자에게 넘겼고, 사용자가 2026-10-13에 눈으로 대조하기로 정했다(맨 끝 "사용자 확인 필요" 6번) | T046, T073 |
| 20 | Phase 2에 "기반" 절을 두지 않는다 | 빈 절을 둔다 | 2단계에 모든 스토리가 기대는 새 코드가 없다. 테스트 도우미는 1단계에서 만든다 | Phase 2 |
| 21 | 로컬 인프라가 켜져 있으면 내릴지 먼저 묻고, `build`를 돌린다 | 묻지 않고 내린다, 일부 작업만 돌린다 | 이 PC의 메모리가 부족해 백그라운드 작업이 멈춘 전례가 있다(2026-10-07, 2026-10-08) | T044, T071 |
| 22 | Spotless 위반이면 `spotlessApply`로 서식만 맞춘다 | 손으로 고친다 | 이 저장소가 쓰는 포매터를 그대로 쓴다. 이 단계가 만든 파일 밖이 바뀌지 않았는지 `git diff --stat`으로 본다 | T044, T071 |

---

## 사용자 확인 필요

아래는 이번 실행에서 하지 않았다. 기준 문서나 설정을 고치거나, 판정 기준 문서에 없는 기대값을 정하는 일이라서다. 헌법의 "반드시" 규칙을 어기게 되는 선택은 없었다.

1. **(2026-10-13 정함) 설정 세 곳의 변경** (T001, T049). 사용자가 추천대로 정했다: 1단계 PR에서 세 곳을 고치고, TC-010은 두 줄로 나누고, 2단계 PR에서는 `msa.stage`만 올린다. T001, T049는 구현할 때 고칠 내용을 한 번 더 보여 주는 작업으로 남긴다. 아래는 정하기 전의 설명이다. 테스트 케이스 단계 표(`config/quality/test-case-stages.csv`), 단계 순서 목록(`build-logic`의 `TestCaseIdCheck.groovy`의 `STAGE_ORDER`), 단계 표시(`gradle.properties`의 `msa.stage`)다. 곳마다 무엇을 어떻게 고치는지는 T001에 있다. 1단계 PR에서 세 곳을 고치고 2단계 PR에서 `msa.stage`만 올린다는 순서도 사용자가 정한다.
2. **(2026-10-13 정함: 별도 docs PR로 고친다. 시점은 사용자가 정한다) 코딩 규약 문서의 한 문장.** `docs/standards/coding-conventions.md` 3-8절 표의 마지막에서 둘째 행(테스트 케이스 ID 검사)은 "티켓 002로 넘긴 부분은 단계를 `T002`로 적어 검사하지 않는다"고 적었다. 단계 이름을 `T002-P1`, `T002-P2`로 바꾸고 `STAGE_ORDER`에 넣으면 이 문장이 맞지 않게 된다. 이 문서를 고치는 것은 기준 문서를 고치는 일이고, 이 PR과 다른 일이라서 별도 docs PR로 해야 한다. 고칠 문장 `[제안]`: "티켓 002의 부분은 단계를 `T002-P1`, `T002-P2`로 적고, 이 단계 이름은 단계 순서 목록에 들어 있고, 지금 단계에 따라 검사한다." 고칠지, 어느 시점에 할지 사용자가 정한다.
3. **(2026-10-13 정함) 시나리오 3.1 실패 확인의 예외** (T040, T041). 시나리오 3.1의 테스트는 구현 전에도 통과해서 "실패를 보지 못하면 멈춰 묻는다"를 그대로 지킬 수 없다. 사용자가 추천대로, 구현을 잠시 망가뜨려 실패를 보고 되돌리는 방식으로 정했다.
4. **(2026-10-13 정함: 문서에 더하지 않는다) 문서에 아직 없어서 단언하지 않은 동작.** 테스트 케이스 문서에 기대값이 없는 동작이다. 구현은 data-model.md를 따르지만 테스트는 단언하지 않는다. 문서에 더할지는 사용자가 정한다. 더하려면 별도 docs PR이 필요하다.
   - 관리자 응답에 주문한 고객의 ID를 넣지 않는 것 (research.md 3절의 후보 5)
   - 관리자 조회의 쿼리 값이 대소문자를 구분하고 빈 값이 400인 것 (후보 6)
   - `size=0`, 음수 `size`, 정수가 아닌 `page`·`size`가 400인 것 (이 tasks.md를 쓰며 찾았다. 공통 전제는 "범위 밖의 값은 400"까지만 정했고, `size`의 아래 한계와 정수가 아닌 값은 정하지 않았다)
   - `long`에 담기지 않는 주문 번호, 음수 주문 번호, 공백이 낀 주문 번호의 처리
   - 역할 변환기가 만드는 권한 이름의 `ROLE_` 접두어 (`docs/design/architecture.md` 7절은 "Spring Security 권한으로 바꿔 쓴다"까지만 적었다). T057은 글자 대신 `hasRole("ADMIN")` 판정으로 확인한다
   - 조회 결과 항목의 순서(상품 ID 순). 기준 문서에 없어서 T014는 순서를 단언하지 않는다
   - 주문 시각 `createdAt`의 글자 모양과 시각대
5. **(2026-10-13 정함: 계속 이 방식) 응답 필드 이름과 값의 출처.** 테스트가 단언하는 영어 필드 이름과 값(`orderNo`, `status`의 `REJECTED` 등)은 테스트 케이스 문서가 아니라 계약 파일 `specs/002-view-orders/contracts/order-query-api.yaml`이 정했다. 001도 같은 방식이었다. research.md 3절이 이 방식이 괜찮은지 물었고, 이 tasks.md의 입력에는 답이 없었다. 계속 이 방식으로 가도 되는지 사용자가 정한다.
6. **(2026-10-13 정함: 눈으로 대조) 외부 API 계약을 검증하는 방법.** 지시는 "계약을 어긴 응답이 없는지 확인하는 방법이 plan에 있으면 따르고, 없으면 새 도구를 더하지 말고 여기에 적는다"였다. plan(research.md 항목 8, 5절)에는 계약 테스트 층이 없다. 대신 인수 시나리오 테스트가 필드 이름을 직접 단언하고, T046·T073이 사람의 눈으로 대조한다. 한계: 테스트가 단언하지 않는 필드가 계약과 어긋나도 기계는 잡지 못한다. 외부 API 계약 테스트를 더할지 사용자가 정한다.
7. **구현 중에 생길 수 있는 결정.** 아래 경우가 생기면 작업이 멈추고 사용자에게 묻도록 적었다.
   - 실패 확인에서 앱이 뜨지 못할 때, 특히 WireMock 없이 `IntegrationTestSupport`를 상속한 테스트의 컨텍스트 (T012)
   - 인라인 뷰로 자른 뒤 항목을 합치는 SQL이 Oracle에서 돌지 않을 때 (T034)
   - Spring Security 7.1.1의 변환기 API가 plan의 설계와 맞지 않을 때 (T060)
   - 로컬 인프라를 내려야 할 때 (T044, T071)

---

## 용어

- **단계 (phase)**: 이 문서에서 PR 하나에 대응하는 작업 묶음이다. Phase 1은 `T002-P1`, Phase 2는 `T002-P2`다.
- **단계 표시 (`msa.stage`)**: 지금 PR이 어느 단계인지 적는 `gradle.properties`의 속성이다. 테스트 케이스 ID 검사가 이 값보다 순서가 뒤인 단계의 줄은 검사하지 않는다. 이 티켓은 1단계 PR에서 `T002-P1`, 2단계 PR에서 `T002-P2`로 올린다.
- **단계 순서 목록 (`STAGE_ORDER`)**: 단계 이름의 순서를 적은 `TestCaseIdCheck.groovy`의 목록이다. 이 티켓은 뒤에 `T002-P1`, `T002-P2`를 더한다.
- **테스트 케이스 단계 표 (test-case stage table)**: 테스트 케이스마다 어느 서비스가 어느 단계에서 테스트를 갖춰야 하는지 적은 `config/quality/test-case-stages.csv`다. 테스트 케이스 ID 검사가 읽는다.
- **받치는 테스트 (supporting test)**: 인수 시나리오를 직접 검증하지 않고 단위, Mapper, 변환기를 확인하는 테스트다. 테스트 케이스 번호를 붙이지 않는다.
- **체크포인트 (checkpoint)**: 스토리 절 끝에서 그때까지 만든 것이 모두 도는지 멈춰 보는 지점이다.
- **되돌려 확인하기 (revert-to-fail check)**: 이미 통과하는 테스트가 결함을 잡는지 보려고 구현 한 줄을 잠시 망가뜨려 실패를 보고 되돌리는 확인이다. 이 티켓은 시나리오 3.1에 쓴다(T041).
- **도우미 클래스 (test support class)**: 여러 테스트가 함께 쓰는 준비 코드를 모아 둔 테스트용 클래스다. 이 티켓은 주문 준비 코드를 `ViewTestSupport`에 둔다.
- **읽기 전용 트랜잭션 (read-only transaction)**: 데이터를 바꾸지 않는다고 알리고 여는 트랜잭션이다. 조회 서비스가 쓴다.
- **쪽 지정 (paging)**: 많은 결과를 정해진 개수씩 나눠 읽는 것이다. 이 티켓은 `page`(0부터)와 `size`(한 쪽의 개수)로 지정한다.
- **`hasNext`**: 다음 쪽이 있는지 알리는 응답 필드다. 한 쪽보다 한 줄 더 읽어서 정한다.
- **인라인 뷰 (inline view)**: SQL의 `FROM` 자리에 쓰는 안쪽 SELECT다. 이 티켓은 주문을 먼저 정렬하고 자른 결과에 주문 항목을 합치는 데 쓴다.
- **값 객체 (value object)**: 값을 검사해서 담아 두는, 바뀌지 않는 작은 객체다. 이 티켓은 `PageRequest`와 `OrderSearch`가 쿼리 매개변수를 이렇게 담는다.
- **realm 역할 (realm role)**: Keycloak 안에서 사용자에게 붙이는 역할이다. 토큰의 `realm_access.roles`에 들어 있고, 이 프로젝트의 `CUSTOMER`와 `ADMIN`이다.
- **Problem Details**: HTTP API의 오류 응답을 JSON으로 적는 표준 형식(RFC 9457)이다. 이 프로젝트의 모든 오류 응답이 이 형식이다.
- **Testcontainers**: 테스트를 실행할 때 Docker 컨테이너를 띄우고 끝나면 지우는 라이브러리다. 이 티켓의 Mapper 테스트와 서비스 통합 테스트가 실제 Oracle을 띄우는 데 쓴다.
- **Spotless**: 소스 서식을 검사하고 맞추는 Gradle 플러그인이다. `spotlessApply`가 서식을 맞춘다.
- **MVP (minimum viable product)**: 사용자가 쓸 수 있는 가장 작은 완성품이다. 이 티켓에서는 1단계가 MVP다.
- **형식 매개변수 (type parameter)**: 클래스가 다룰 자료형을 나중에 정하도록 `<T>`로 비워 둔 자리다. 이 티켓의 `OrderPage<T>`가 고객용 주문과 관리자용 주문을 같은 봉투로 담는 데 쓴다.
- **동적 SQL (dynamic SQL)**: 조건에 따라 SQL 조각을 붙이거나 빼는 MyBatis 기능이다. 이 티켓의 관리자 조회가 `status`, `releaseStatus` 조건에 쓴다.
- **`resultMap`**: SQL 결과의 열을 자바 객체의 필드에 짝짓는 MyBatis 설정이다. 이 티켓은 기존 `orderWithItemsMap`을 이어받는 `orderViewMap`을 더한다.
- **인수 시나리오 (acceptance scenario)**: 기능이 완성됐다고 인정하려면 통과해야 하는 시험 상황이다. 이 티켓에서는 `docs/test-cases/`의 테스트 케이스가 인수 시나리오이고, spec이 16개로 나눠 번호를 붙였다.
- **서비스 통합 테스트 (service integration test)**: 서비스 하나를 실제 DB와 함께 띄우고 API부터 DB까지 확인하는 테스트다. 이 티켓에서는 인수 시나리오를 이 테스트로 검증한다.
- **Mapper 테스트 (mapper test)**: MyBatis SQL이 실제 Oracle에서 맞게 도는지 확인하는 테스트다. 이 티켓에서는 `OrderQueryMapperTest`가 조회 SQL을 확인한다.
- **역할 변환기 (role converter)**: 검증한 JWT의 역할 정보를 Spring Security의 권한으로 바꾸는 클래스다(`JwtAuthenticationConverter`의 한 종류). 이 티켓에서는 `RealmRoleJwtConverter`가 토큰의 `realm_access.roles`를 권한으로 바꾼다.
- **`AccessDeniedHandler`**: 로그인은 했지만 권한이 없는 요청에 어떤 응답을 낼지 정하는 Spring Security의 처리기다. 이 티켓에서는 `ProblemAccessDeniedHandler`가 403을 Problem Details로 낸다.
- **`ROLE_` 접두어 (ROLE_ prefix)**: Spring Security의 `hasRole("ADMIN")`이 권한 이름 `ROLE_ADMIN`을 찾는 규칙이다. 이 티켓의 테스트는 이 글자를 단언하지 않는다.
- **WireMock**: 다른 서비스인 척 정해진 응답이나 지연, 오류를 돌려주는 테스트용 가짜 서버다. 이 티켓은 재고 서비스를 부르지 않아서 쓰지 않는다.
- **ArchUnit**: 패키지와 클래스 사이의 의존 규칙을 테스트 코드로 검사하는 라이브러리다. 이 티켓에서는 `ArchitectureTest`가 새 클래스에도 계층 규칙을 적용한다.
- **Flyway**: DB 스키마를 버전 번호가 붙은 SQL 파일로 관리하고 차례로 적용하는 도구다. 이 티켓은 새 Flyway 파일을 만들지 않는다.
- **Keycloak, JWT**: Keycloak은 로그인과 토큰 발급을 맡는 인증 서버이고, JWT는 그 서버가 서명해 발급하는 사용자 정보 토큰이다. 이 티켓의 조회 API는 이 토큰을 서비스가 직접 검증하고, 고객은 토큰의 `sub`로만 구별한다.
