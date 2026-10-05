# msa-example 헌법 (Constitution)

이 문서는 msa-example 저장소의 모든 기능이 지켜야 하는 원칙이다. spec-kit의 모든 명령
(`/speckit-specify`, `/speckit-plan`, `/speckit-tasks`, `/speckit-implement`, `/speckit-analyze` 등)이
이 문서를 읽는다.

- 이 헌법에는 원칙만 짧게 적는다. 값, 문법, 도구 같은 세부 규칙은 `docs/standards/`에 있다.
  원칙마다 세부 규칙이 있는 파일과 절을 적는다.
- "반드시"는 MUST와 같은 뜻이고, "권장"은 SHOULD와 같은 뜻이다. 이 헌법의 원칙은 모두 "반드시" 규칙이다.
- 원칙을 가리킬 때는 이름으로 가리킨다. 예: "계약 우선 원칙".

## 핵심 원칙 (Core Principles)

### I. 서비스 자율성 (Service Autonomy)

**반드시**

- 서비스마다 자기 DB가 있다. 서비스는 다른 서비스의 DB에 접속하지 않는다.
  다른 서비스의 데이터가 필요하면 그 서비스의 API를 부른다.
- Pod 하나에는 서비스 하나만 넣는다. 다른 컨테이너는 로그 수집기 같은 사이드카일 때만 함께 넣는다.
- 여러 서비스가 함께 쓰는 `libs/`에는 추적 설정 같은 기술 코드만 둔다.
  업무 규칙, DTO, Mapper, SQL은 서비스끼리 공유하지 않는다.

**이유**: 서비스끼리 DB나 업무 코드를 공유하면, 한 서비스를 바꿀 때 다른 서비스도 함께 바꾸고
함께 배포해야 한다. 그러면 서비스를 나눈 의미가 없어진다.

**plan에서 확인할 질문**

- 이 기능이 다른 서비스의 DB 테이블을 읽거나 쓰는가?
- `libs/`에 업무 규칙, DTO, Mapper, SQL을 넣는가?

**세부 규칙**: `docs/standards/architecture-rules.md` 1절 "서비스 경계",
`docs/standards/coding-conventions.md` 3-3절 "DB"

### II. 계약 우선 (Contract First)

**반드시**

- 서비스 사이 API와 이벤트의 기준은 main에 병합된 `contracts/`의 명세(OpenAPI, AsyncAPI)다.
  API나 이벤트를 바꾸는 PR은 같은 PR에서 `contracts/`도 고친다.
- API와 이벤트는 추가하는 방식으로만 바꾼다. 필드를 지우거나, 필드의 이름·타입·뜻을 바꾸지 않는다.
- 받는 쪽은 모르는 필드를 무시한다. 상태 값(enum)을 추가하는 것은 깨는 변경으로 본다.
- 깨는 변경이 꼭 필요하면 ADR을 먼저 쓴다. 아직 병합하지 않은 PR 안에서 계약을 고치는 것은
  깨는 변경이 아니다.

**이유**: 서비스는 따로 배포된다. 배포하는 동안에는 옛 버전과 새 버전이 서로 호출한다.
추가만 하고 받는 쪽이 모르는 필드를 무시하면, 두 버전이 함께 돌아간다.

**plan에서 확인할 질문**

- 이 기능이 서비스 사이 API나 이벤트를 바꾸는가? 바꾼다면 `contracts/`도 함께 고치는가?
- 지우는 필드, 이름·타입·뜻이 바뀌는 필드, 새로 더하는 상태 값이 있는가? 있다면 ADR이 있는가?

**세부 규칙**: `docs/standards/architecture-rules.md` 2절 "서비스 사이 호출"

### III. 실패를 전제한 호출 (Design for Failure)

**반드시**

- 서비스 사이 호출은 쿠버네티스 Service 이름으로 한다.
- 모든 원격 호출에 연결 제한 시간과 응답 제한 시간을 둔다. 제한 시간은 코드에 적지 않고 설정값으로 둔다.
- 상태를 바꾸는 서비스 사이 API는 멱등하게 만든다.
- 재시도는 멱등한 호출에만 한다. 재시도할 오류의 종류와, 재시도를 포함한 전체 시간 한도를
  정해 두고 그 안에서만 한다.
- 다른 서비스를 동기로 부를 때는 서킷 브레이커를 둔다. 서킷이 열려 있으면 호출하지 않고 바로 실패한다.
- 원격 호출은 DB 트랜잭션 밖에서 한다.

**이유**: 네트워크 호출은 실패하고, 느려지고, 처리는 됐는데 응답만 사라지기도 한다.
이것을 전제로 짜지 않으면 한 서비스의 장애가 다른 서비스로 번지고, 같은 요청이 두 번 처리된다.

**plan에서 확인할 질문**

- 새 원격 호출마다 제한 시간, 재시도 조건, 서킷 브레이커가 정해져 있는가?
- 상태를 바꾸는 API의 멱등 키는 무엇인가?
- 원격 호출이 DB 트랜잭션 안에서 일어나는 곳이 있는가?

**세부 규칙**: `docs/standards/architecture-rules.md` 2절 "서비스 사이 호출",
`docs/standards/coding-conventions.md` 3-1절 "계층"

### IV. 언제 사라져도 되는 Pod (Disposable Pods)

**반드시**

- 남아야 하는 데이터(로그인 세션 포함)는 Pod 메모리나 로컬 디스크에 두지 않는다.
  DB 같은 외부 저장소에 둔다.
- 서비스마다 readiness 프로브와 liveness 프로브를 둔다.
- 종료 신호를 받으면 처리 중인 요청을 끝낸 뒤 종료한다(그레이스풀 셧다운).
- Pod 하나가 사라져도 서비스가 계속 응답하도록 배포한다.
- 이미지는 서비스마다 하나다. 환경마다 다른 값은 이미지에 넣지 않고 ConfigMap과 Secret으로 넣는다.

**이유**: 쿠버네티스는 장애, 배포, 확장 때마다 Pod를 지우고 새로 만든다.
Pod 안에만 있는 것은 그때 사라진다.

**plan에서 확인할 질문**

- 이 기능이 Pod 메모리나 로컬 디스크에 남겨야 하는 상태를 만드는가?
- 새 설정값은 ConfigMap이나 Secret으로 들어가는가?

**세부 규칙**: `docs/standards/architecture-rules.md` 3절 "실행과 배포"
(프로브에 넣을 것과 뺄 것, 종료 대기 시간, replicas 수)

### V. 관측 가능성 (Observability)

**반드시**

- 서비스 사이 호출에 추적 정보를 넘긴다. 모든 로그 줄에 trace ID를 남긴다.
- 로그는 Pod 안 파일에 쓰지 않는다. 표준 출력으로 내보내고, 같은 로그를 Pod 밖 관측 저장소에도 보낸다.
- 비밀번호, 토큰 전체, 개인 정보는 로그에 남기지 않는다.

**이유**: 주문 한 건의 로그가 여러 서비스의 여러 Pod에 나뉘어 남는다. trace ID가 있어야 한곳에서
모아 볼 수 있다. 또 Pod가 바뀌면 Pod 안 로그는 사라진다.

**plan에서 확인할 질문**

- 새 원격 호출과 새 로그에 trace ID가 이어지는가?
- 로그에 비밀번호, 토큰, 개인 정보가 들어가는 곳이 있는가?

**세부 규칙**: `docs/standards/architecture-rules.md` 4절 "관측성",
`docs/standards/coding-conventions.md` 3-6절 "로그"

### VI. 서비스가 직접 확인하는 신원과 코드 밖의 비밀 (Verified Identity, External Secrets)

**반드시**

- 외부에 열린 API는 서비스가 JWT를 직접 검증한다. 사용자는 토큰의 `sub` 값으로만 식별한다.
  요청 본문이나 헤더로 받은 사용자 ID는 믿지 않는다.
- 비밀값(비밀번호, 키, 토큰)은 코드, 이미지, 저장소에 두지 않는다. 실행할 때 Secret이나 환경변수로 넣는다.
- 로컬 실습용 비밀번호도 같다. 실제 값은 저장소에 올리지 않는 `.env` 파일에만 두고,
  저장소에는 값을 비운 `.env.example`만 둔다.

**이유**: 서비스가 직접 검증해야 Gateway를 거치지 않은 요청과 사용자 위조를 막는다.
이 저장소는 공개 저장소라서, 저장소에 올린 비밀번호는 누구나 볼 수 있다.

**plan에서 확인할 질문**

- 새 외부 API에 인증이 걸리는가? 사용자 ID를 토큰에서만 얻는가?
- 새 비밀값은 어디에 두고, 실행할 때 어떻게 넣는가?

**세부 규칙**: `docs/standards/architecture-rules.md` 3절 "실행과 배포", 5절 "보안"

### VII. 실제 조건에서 테스트 먼저 (Test First, Real Conditions)

**반드시**

- 테스트는 기능 명세에서 요청하지 않아도 만든다. spec-kit의 기본 안내에 "테스트는 선택"이라는
  문구가 있어도 이 원칙이 우선한다.
- tasks.md에서 인수 시나리오(`docs/test-cases/`의 테스트 케이스)를 검증하는 테스트 작업을
  구현 작업보다 앞에 둔다. 테스트가 실패하는 것을 확인한 뒤 구현한다.
- 영속성 테스트는 Testcontainers로 띄운 실제 Oracle에서 한다. H2 같은 대체 DB를 쓰지 않는다.
- 서비스 통합 테스트에서는 상대 서비스를 실제로 띄우지 않고 WireMock으로 대신한다.
- 매 빌드에서 단위, Mapper, 서비스 통합, 계약, 아키텍처 테스트가 모두 통과해야 한다.

**이유**: 이 프로젝트가 확인하려는 멱등성, 제한 시간, 재시도, 동시 예약은 실제 DB와 실제 HTTP에서만
제대로 확인된다. 테스트를 먼저 쓰면 구현이 인수 시나리오를 빠뜨렸는지 바로 드러난다.

**plan에서 확인할 질문**

- 기능의 인수 시나리오마다 그것을 검증하는 테스트가 계획되어 있는가?
- 실제 Oracle이나 WireMock 대신 다른 것으로 바꾼 곳이 있는가?

**세부 규칙**: `docs/standards/testing.md` 2절 "B안의 테스트 층", 3절 "규칙"

### VIII. 검수할 수 있는 산출물 (Reviewable Artifacts)

**반드시**

- 사용자는 Claude가 만든 모든 산출물(spec.md, plan.md, tasks.md, 코드, 커밋 메시지, PR 본문)을 검수한다.
  산출물은 사용자가 다른 파일을 열지 않고도 확인할 수 있게 쓴다.
- 산출물에서 항목 ID를 쓰면 바로 아래에 출처 파일, 그 파일 안의 위치, 항목의 원문을 적는다.
- 사용자가 정하지 않았는데 Claude가 채운 내용에는 `[제안]` 태그를 붙이고, 결정된 내용과 나눠 쓴다.
- 글은 이 프로젝트를 처음 보는 주니어 개발자가 읽는다고 보고 쓴다. 새 용어는 문서 맨 아래 용어 절에 정의한다.

**이유**: 출처를 따라가지 못하는 산출물은 검수할 수 없다. 결정과 제안이 섞이면,
사용자가 정하지 않은 내용이 요구사항이 된다.

**plan에서 확인할 질문**

- spec.md와 plan.md에 출처와 원문을 함께 적지 않은 항목 ID가 있는가?
- 사용자가 정하지 않은 내용이 `[제안]` 없이 결정처럼 적혀 있는가?

**세부 규칙**: 저장소 루트 `CLAUDE.md` 1절 "사용자가 모든 결과를 검수한다",
2절 "항목 ID를 쓰면 바로 아래에 출처와 원문을 적는다", 3절 "글쓰기", 4절 "새 용어는 맨 아래에 정의한다"

## 기술 제약 (Technical Constraints)

- 기술 스택과 버전은 `docs/standards/tech-stack.md`를 따른다. plan의 Technical Context에는 이 문서의 값을 쓴다.
  이 문서에 "plan에서 정한다"고 적힌 항목만 plan에서 고른다.
- 코드 구조와 작성 규칙(계층 구조, MyBatis와 SQL, DB, 이름, 로그)은 `docs/standards/coding-conventions.md`를 따른다.
  계층 사이 의존 규칙은 ArchUnit 테스트로 검사한다.
- 한 서비스에만 해당하는 규칙(패키지 구조, DB 스키마 규칙)은 `services/<서비스>/CLAUDE.md`에 적는다.

## 개발 절차와 품질 관문 (Development Workflow and Quality Gates)

**plan의 Constitution Check**

1. 이 절을 채우기 전에 `docs/standards/`의 다섯 문서를 먼저 모두 읽는다:
   `architecture-rules.md`, `tech-stack.md`, `testing.md`, `coding-conventions.md`, `git-workflow.md`
2. 이 헌법의 원칙마다 "plan에서 확인할 질문"에 답한다.
3. `docs/standards/architecture-rules.md`의 "반드시" 규칙을 하나씩 확인한다.
   이 plan과 관계없는 규칙은 "해당 없음"과 그 이유를 적는다.
4. 하나라도 어기면 plan은 통과하지 못한다. Phase 0 연구를 시작하기 전에 확인하고,
   Phase 1 설계가 끝난 뒤 다시 확인한다.

**브랜치, 커밋, PR**

- `docs/standards/git-workflow.md`를 따른다.
- PR 하나에는 한 가지 일만 담는다.
- PR 본문은 `.github/pull_request_template.md`를 채운다. 필수 검사(CI)가 생기면 통과해야 병합한다.
- 병합은 사용자가 PR을 검토한 뒤에 한다.

**검사**

- `/speckit-analyze`는 이 헌법의 "반드시" 규칙을 어긴 것을 가장 심각한 문제(CRITICAL)로 분류한다.
  그런 문제가 나오면 spec, plan, tasks를 고친다. 헌법을 고쳐서 넘기지 않는다.

**spec-kit 템플릿을 바꾸는 방법**

- spec-kit이 설치한 원본 템플릿(`.specify/templates/*.md`)은 고치지 않는다.
  spec-kit을 새 버전으로 올릴 때 덮어써지기 때문이다.
- 바꿀 템플릿은 `.specify/templates/overrides/`에 같은 이름으로 둔다. spec-kit은 원본보다 이 파일을 먼저 쓴다.
- 지금 덮어쓴 템플릿은 두 개다.
  - `plan-template.md`: Constitution Check 절과 Complexity Tracking 절
  - `tasks-template.md`: 테스트를 필수로 바꾼 문구

## 거버넌스 (Governance)

**우선순위**

- 이 헌법은 spec-kit의 템플릿과 명령 안내보다 우선한다. 다른 문서가 이 헌법과 어긋나면 헌법을 따르고,
  어긋난 문서를 고친다.
- Claude가 일하는 방식은 저장소 루트의 `CLAUDE.md`를 따른다.

**규칙의 수준**

- 반드시(MUST): 어기면 리뷰에서 막는다. plan.md의 Complexity Tracking에 이유를 적어 넘길 수 없다.
  바꾸려면 ADR을 쓰고 이 헌법을 개정한다.
- 권장(SHOULD): 어길 때 plan.md의 Complexity Tracking에 이유를 적는다.
- Complexity Tracking에는 권장 규칙을 어긴 것만 적는다.

**개정 절차**

- 헌법은 PR로만 개정한다. 사용자가 PR을 검토하고 병합한다.
- 원칙을 바꾸면 그 원칙이 가리키는 `docs/standards/` 문서와 관련 ADR도 같은 PR에서 고친다.
- 개정할 때마다 아래 버전 규칙에 따라 버전을 올리고, 최종 개정일을 바꾼다.

**버전 규칙 (SemVer)**

- MAJOR(첫째 자리): 원칙을 지우거나 원칙의 뜻을 바꿀 때 올린다.
- MINOR(둘째 자리): 원칙이나 절을 더하거나, 원칙의 내용을 크게 넓힐 때 올린다.
- PATCH(셋째 자리): 뜻이 바뀌지 않는 문장 다듬기와 오타 수정에 올린다.

**준수 확인**

- 모든 plan은 Constitution Check를 통과해야 한다.
- 모든 PR은 검토할 때 이 헌법을 지켰는지 확인한다.

**Version (버전)**: 1.0.0 | **Ratified (비준일)**: 2026-10-05 | **Last Amended (최종 개정일)**: 2026-10-05

## 용어

- **헌법 (constitution)**: spec-kit의 모든 명령이 읽는 프로젝트 원칙 문서다. 이 파일
  `.specify/memory/constitution.md`가 헌법이다.
- **반드시 / 권장 (MUST / SHOULD)**: 규칙의 강도다. "반드시"는 예외 없이 지키고, "권장"은 이유를 적으면 어길 수 있다.
- **사이드카 (sidecar)**: 주 컨테이너와 같은 Pod에서 로그 수집 같은 보조 일만 하는 컨테이너다.
  이 저장소에서는 사이드카일 때만 Pod에 컨테이너를 더 넣는다.
- **계약 (contract)**: 서비스 사이 API와 이벤트의 형식을 적은 명세다. 이 저장소에서는 `contracts/`의
  OpenAPI, AsyncAPI 파일이 계약이다.
- **이벤트 (event)**: 한 서비스가 일어난 일을 알리려고 메시지 브로커에 올리는 메시지다.
  받는 서비스가 나중에 꺼내 읽는다. 지금 단계에서는 쓰지 않는다.
- **깨는 변경 (breaking change)**: 옛 버전의 호출하는 쪽이나 받는 쪽이 더는 동작하지 않게 만드는 변경이다.
  예: 필드 삭제, 필드 이름 변경, 새 상태 값 추가.
- **ADR (Architecture Decision Record)**: 아키텍처 결정과 그 이유를 남기는 문서다. `docs/adr/`에 둔다.
- **멱등성 (idempotency)**: 같은 요청을 여러 번 받아도 결과가 한 번 받은 것과 같은 성질이다.
  재고 예약은 주문 번호로 같은 요청인지 알아본다.
- **서킷 브레이커 (circuit breaker)**: 상대 서비스 호출이 계속 실패하면 한동안 호출을 멈추고 바로 실패를 돌려주는 장치다.
  재고 서비스가 멈췄을 때 주문 서비스까지 멈추지 않게 막는다.
- **트랜잭션 (transaction)**: 여러 DB 변경을 한 묶음으로 처리해서, 모두 반영하거나 모두 되돌리는 단위다.
- **Pod**: 쿠버네티스가 컨테이너를 실행하는 가장 작은 단위다. 언제든 지워지고 새로 만들어진다.
- **프로브 (probe)**: 쿠버네티스가 Pod에 상태를 주기적으로 묻는 기능이다. readiness 프로브는 "요청을 받을 준비가
  됐나"를, liveness 프로브는 "살아 있나"를 묻는다.
- **그레이스풀 셧다운 (graceful shutdown)**: 종료 신호를 받으면 새 요청은 받지 않고, 처리 중인 요청을 끝낸 뒤 종료하는 방식이다.
- **ConfigMap, Secret**: 쿠버네티스가 환경마다 다른 값을 Pod에 넣어 주는 객체다. 일반 설정값은 ConfigMap에,
  비밀번호 같은 값은 Secret에 둔다.
- **trace ID (추적 ID)**: 요청 하나에 붙여 여러 서비스를 따라다니게 하는 번호다. 이 번호로 여러 서비스의 로그를 함께 찾는다.
- **관측 저장소 (observability backend)**: 로그, 추적, 메트릭을 모아 두고 보여 주는 곳이다.
  이 저장소에서는 `grafana/otel-lgtm` 컨테이너를 쓴다.
- **JWT (JSON Web Token)**: 로그인한 사용자 정보를 담고 서명한 토큰이다. 이 저장소에서는 Keycloak이 발급한다.
- **`sub`**: JWT 안에서 사용자를 구별하는 값이다. 이 저장소에서는 이 값을 고객 ID로 쓴다.
- **`.env` 파일 (environment file)**: 환경변수 값을 적어 두는 파일이다. `.gitignore`에 들어 있어 저장소에 올라가지 않는다.
- **인수 시나리오 (acceptance scenario)**: 기능이 완성됐다고 인정하려면 통과해야 하는 시험 상황이다.
  `docs/test-cases/`의 테스트 케이스가 인수 시나리오다.
- **Testcontainers**: 테스트를 실행할 때 Docker 컨테이너(예: Oracle)를 띄우고 끝나면 지우는 라이브러리다.
- **WireMock**: 다른 서비스인 척 정해진 응답이나 지연, 오류를 돌려주는 테스트용 가짜 서버다.
- **항목 ID (item ID)**: 기준 문서의 항목마다 붙인 번호다. 예: 아키텍처 규칙, 업무 규칙, 테스트 케이스의 번호.
  규칙은 `docs/README.md`의 "ID 규칙" 절에 있다.
- **`[제안]` 태그 (proposal tag)**: 원문이나 사용자 결정에 없고 Claude가 덧붙인 내용에 붙이는 표시다.
  사용자가 검토해서 받아들이면 지운다.
- **Constitution Check**: plan.md 안의 점검 절이다. plan이 이 헌법을 지키는지 확인한다.
- **Complexity Tracking**: plan.md 안의 절이다. 권장 규칙을 어길 수밖에 없을 때 그 이유를 적는다.
- **Phase 0 연구, Phase 1 설계**: `/speckit-plan`의 단계다. Phase 0은 정하지 않은 기술 문제를 조사해
  research.md에 적고, Phase 1은 데이터 모델, 계약, quickstart를 만든다.
- **덮어쓰기 템플릿 (template override)**: `.specify/templates/overrides/`에 둔 템플릿이다.
  spec-kit은 원본 템플릿 대신 이 파일을 쓴다.
- **SemVer (Semantic Versioning)**: 버전을 MAJOR.MINOR.PATCH 세 자리로 쓰고, 바뀐 정도에 따라 올릴 자리를 정하는 규칙이다.
