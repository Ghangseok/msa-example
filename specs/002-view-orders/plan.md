# Implementation Plan: 주문 조회

**Branch**: `feat/002-view-orders` (spec, PR #29로 병합), `docs/002-view-orders-plan` (이 plan) `[제안]`. spec의 시나리오 2.6 추가는 별도 PR `docs/002-spec-list-paging` `[제안]`. 구현 브랜치는 `feat/002-view-orders-p1`, `-p2`다 `[제안]`. | **Date**: 2026-10-11 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/002-view-orders/spec.md`

**함께 만든 문서**: [research.md](research.md) (선택지와 근거), [data-model.md](data-model.md) (읽는 열, 조회 규칙, 응답 모양), [contracts/order-query-api.yaml](contracts/order-query-api.yaml), [quickstart.md](quickstart.md) (검증 방법). tasks.md는 `/speckit-tasks`가 만든다.

이 문서에서 "사용자 결정"은 사용자가 정한 것이다. `[제안]`은 Claude가 채운 것이고, 자세한 이유는 research.md에 있다. 사용자가 정하지 않은 내용을 "결정"이라고 쓰지 않는다.

## Summary

주문 서비스에 주문을 읽는 API 세 개를 더한다. 고객이 자기 주문 목록과 주문 하나를 조회하고, 관리자가 모든 고객의 주문을 상태와 재고 해제 결과로 조회한다. 조회는 읽기만 한다.

- **고객 조회**: `GET /orders`와 `GET /orders/{orderNo}`다. 조회 SQL에 고객 조건(토큰의 `sub`)을 넣는다. 그래서 다른 고객의 주문은 없는 주문과 같은 "없음"(404)이 된다. 숫자가 아닌 주문 번호도 같은 404다.
- **관리자 조회**: `GET /admin/orders?status=&releaseStatus=`다. `/admin/` 아래를 `ADMIN` 역할만 부를 수 있게 막는다. 토큰의 `realm_access.roles`를 권한으로 바꾸는 변환기와 403을 Problem Details로 내는 처리기를 새로 만든다. 응답에는 재고 해제 결과(`releaseStatus`)가 함께 나온다.
- **목록**: 주문 번호가 큰 순서로, `page`와 `size`로 쪽을 나눠 읽는다. 응답은 `orders`, `page`, `size`, `hasNext`를 담은 객체다.
- **DB와 의존성**: 새 Flyway 파일, 새 인덱스, 새 라이브러리가 없다. 재고 서비스를 부르지 않는다.
- **정한 것과 제안**: 오류 종류(`code`) 값 3개, 성공 응답 코드 200, 정렬, 쪽 크기, 응답 필드 이름은 Claude가 정한 `[제안]`이다. 앞의 네 가지는 2026-10-12에 사용자가 승인해 테스트 케이스 문서에 반영됐다(PR #30). 문서에 아직 없는 동작은 research.md 3절에 후보로 남겼다.
- **PR 개수**: 사용자 결정 (2026-10-11): Y안, PR 2개다. 1단계는 스토리 1, 2, 3이고 2단계는 스토리 4다. "PR 단계" 절에 있다.

## Technical Context

**Language/Version**: Java 17 (JDK 17.0.2)

**Primary Dependencies**: 새로 더하는 의존성이 없다. `services/order-service/build.gradle`에 이미 있는 것만 쓴다.

- Spring Boot 4.1.1 (Spring Framework 7.0.9, Spring Security 7.1.1)의 스타터 webmvc, validation, oauth2-resource-server
- mybatis-spring-boot-starter 4.1.0 (MyBatis 3.5.19, MyBatis-Spring 4.1.0)
- 이 기능이 쓰지 않는 것: Resilience4j, `RestClient`, WireMock. 다른 서비스를 부르지 않기 때문이다.
- 버전을 고른 이유와 확인한 방법: `specs/001-place-order/research.md` 0절, research.md 0절

**Storage**: Oracle Database 21c XE, `order-db`의 `ORDER_SVC` 계정. 기존 `ORDERS`, `ORDER_ITEMS`를 읽기만 한다. 새 테이블, 새 열, 새 Flyway 파일이 없다(research.md 항목 10).

**Testing**:

- JUnit Jupiter 6.0.3(Spring Boot 관리), AssertJ, Testcontainers 2.0.5(`testcontainers-oracle-xe`, 이미지 `gvenzl/oracle-xe:21.3.0-slim-faststart`)
- Spring Security Test(고객과 관리자를 `jwt()`로 흉내 낸다), ArchUnit, mybatis-spring-boot-starter-test
- WireMock과 계약 검증기(swagger-request-validator)는 쓰지 않는다. 이 기능은 상대 서비스를 부르지 않고, 서비스 사이 API를 바꾸지 않기 때문이다(research.md 항목 8, 13).

**Build**: 001과 같다. Gradle wrapper 9.8.0, `msa.java-service` convention plugin(Spotless, SpotBugs, 기계 검사). 이 기능은 기계 검사를 위한 설정 세 곳을 고쳐야 한다. `config/quality/test-case-stages.csv`, `build-logic`의 `TestCaseIdCheck.groovy`, `gradle.properties`다. tasks 단계의 일이고 사용자에게 확인을 받는다(research.md 항목 13).

**Target Platform**: 이 PC(Windows 11, Docker Desktop)에서 테스트한다. 배포는 티켓 003이다.

**Project Type**: 기존 주문 서비스(Spring Boot 마이크로서비스)에 API를 더한다. 새 서비스는 없다.

**Performance Goals**: 목표값 없음. `docs/requirements/non-functional.md`의 비기능 요구사항 표에 주문 조회에 해당하는 행이 없다. 응답 시간, 처리량 같은 수치 목표를 만들지 않았다. 쪽 크기 20과 100은 응답 한 번의 크기이지 성능 목표가 아니다(research.md 항목 3).

**Constraints**:

- Java 17이라 가상 스레드를 쓸 수 없다. 조회는 DB만 읽고 원격 호출을 기다리지 않아서 스레드를 오래 잡지 않는다.
- SQL은 19c에서 도는 것만 쓴다. 쪽 지정은 `OFFSET ... ROWS FETCH NEXT ... ROWS ONLY`다.
- 다른 고객의 주문 번호, 없는 주문 번호, 숫자가 아닌 주문 번호의 응답이 서로 구별되면 안 된다.
- Oracle XE의 자원 제한은 CPU 2개, 메모리 2GB, 사용자 데이터 12GB다.

**Scale/Scope**:

- API 3개: 외부 `GET /orders`, `GET /orders/{orderNo}`, `GET /admin/orders`. 새 테이블 0개.
- 인수 시나리오 16개, 테스트 케이스 13개. 아래 "인수 시나리오와 테스트 층" 표가 짝지었다.

## Constitution Check

*GATE: Phase 0 연구를 시작하기 전에 통과해야 한다. Phase 1 설계가 끝난 뒤 다시 확인한다.*

**먼저 읽은 문서**: `docs/standards/`의 다섯 문서를 모두 읽었다(`architecture-rules.md`, `tech-stack.md`, `testing.md`, `coding-conventions.md`, `git-workflow.md`). 함께 읽은 문서는 다음과 같다.

- `docs/design/architecture.md` 전체
- `docs/adr/0006-keycloak-jwt-auth.md`
- `docs/requirements/uc-002-view-orders.md`, `docs/requirements/non-functional.md`
- `docs/test-cases/order-placement.md`의 공통 전제와 조회 케이스
- `.specify/memory/constitution.md`, 저장소 루트 `CLAUDE.md`
- `specs/001-place-order/`의 plan.md, research.md, data-model.md, quickstart.md, contracts/order-api.yaml
- 주문 서비스 코드(읽기만 했다): `OrderController`, `SecurityConfig`, `OrderService`, `OrderTxService`, `OrderMapper`와 `OrderMapper.xml`, `dto`, `domain`, `ProblemDetailsHandler`, `ProblemAuthenticationEntryPoint`, `V1__orders.sql`, 테스트의 `IntegrationTestSupport`, `CustomerIdentityIntegrationTest`, `InvalidTokenIntegrationTest`, `OrderMapperTest`, `ArchitectureTest`
- `config/quality/test-case-stages.csv`, `build-logic`의 `TestCaseIdCheck.groovy`

### 1차 확인 (Phase 0 전)

#### 원칙 확인

| 원칙 | 확인할 질문에 대한 답 | 결과 |
|------|----------------------|------|
| 서비스 자율성 | 다른 서비스의 DB 테이블을 읽거나 쓰지 않는다. 주문 서비스의 `order-db`에서 `ORDERS`, `ORDER_ITEMS`만 읽는다. `libs/`에는 아무것도 넣지 않는다. DTO, Mapper, SQL 모두 `services/order-service` 안에 둔다 | 통과 |
| 계약 우선 | 서비스 사이 API와 이벤트를 바꾸지 않는다. 더하는 API 세 개는 고객과 관리자가 부르는 외부 API(`docs/design/architecture.md` 5절 표의 "외부" 구분)라서 저장소 루트 `contracts/`의 현재본을 고치지 않는다. 조회 API의 OpenAPI는 `specs/002-view-orders/contracts/`에 쓴다. 지우는 필드, 이름·타입·뜻이 바뀌는 필드, 새로 더하는 상태 값이 없다. 오류 `code` 값 3개를 더하지만 `code`는 값 목록(`enum`)이 아니다 | 통과 |
| 실패를 전제한 호출 | 새 원격 호출이 없다. 재고 서비스를 부르지 않는다. 그래서 제한 시간, 재시도, 서킷 브레이커가 해당 없다. 상태를 바꾸는 API가 없어서 멱등 키도 해당 없다. 원격 호출이 트랜잭션 안에 있는 곳이 없다 | 통과 |
| 언제 사라져도 되는 Pod | Pod 메모리나 로컬 디스크에 남겨야 하는 상태가 없다. 세션이 없고 쪽 정보는 요청마다 받는다. 새 설정값이 없다. 쪽 크기 상수는 환경마다 다르지 않아 ConfigMap으로 받지 않는다 | 통과 |
| 관측 가능성 | 서비스 사이 호출이 없다. 요청 로그의 trace ID는 001의 설정(OpenTelemetry, ECS JSON 로그)을 그대로 쓴다. 새 로그를 더하더라도 토큰, 고객 ID, 요청한 주문 번호를 담지 않는다 `[제안]` | 통과 |
| 서비스가 직접 확인하는 신원과 코드 밖의 비밀 | 세 API 모두 `anyRequest().authenticated()`에 걸려 서비스가 JWT를 직접 검증한다. 고객 ID는 `@AuthenticationPrincipal Jwt`의 `jwt.getSubject()`로만 얻는다. 쿼리 매개변수와 헤더의 고객 ID는 쓰지 않는다. 관리자 여부는 토큰의 역할로 정한다. 새 비밀값이 없다 | 통과 |
| 실제 조건에서 테스트 먼저 | 인수 시나리오 16개마다 검증 테스트가 있다(아래 "인수 시나리오와 테스트 층" 표). 영속성은 Testcontainers의 실제 Oracle이다. WireMock을 쓰지 않는 것은 대체가 아니라 상대 서비스 호출이 없기 때문이다. 기대값은 확정된 `docs/test-cases/`에서 그대로 가져온다. 오류 `code`, 성공 응답 코드, 정렬, 쪽 지정은 PR #30으로 문서에 반영됐다. 문서에 아직 없는 동작(research.md 3절의 후보 5, 6)은 테스트의 기대값으로 쓰지 않는다. 초안 상태의 문서에서 가져온 기대값이 없다. tasks.md에서 테스트 작업을 구현보다 앞에 둔다 | 통과 |
| 검수할 수 있는 산출물 | plan, research, data-model, quickstart에 쓴 항목 ID는 바로 아래에 출처와 원문을 펼쳤다. Claude가 채운 것에는 `[제안]`을 붙였다. 각 문서 맨 아래에 용어 절이 있다 | 통과 |

#### 반드시 규칙 확인 (docs/standards/architecture-rules.md)

| # | 규칙 | 이 plan에서 지키는 방법, 또는 해당 없음과 그 이유 | 결과 |
|---|------|-----------------------------------------------|------|
| 1 | STD-001 | 주문 서비스가 자기 DB(`order-db`)만 읽는다. 재고 DB와 재고 API를 쓰지 않는다 | 통과 |
| 2 | STD-002 | 해당 없음. Pod 구성은 배포 매니페스트의 일이고 티켓 003이다 | 해당 없음 |
| 3 | STD-015 | `libs/`를 바꾸지 않는다. 조회 코드는 모두 주문 서비스 안에 둔다 | 통과 |
| 4 | STD-003 | 해당 없음. 이 기능은 다른 서비스를 부르지 않는다 | 해당 없음 |
| 5 | STD-004 | 해당 없음. 원격 호출이 없다 | 해당 없음 |
| 6 | STD-005 | 해당 없음. 상태를 바꾸는 서비스 사이 API가 없다. 조회 API 세 개는 읽기 전용이다 | 해당 없음 |
| 7 | STD-010 | 새 API만 더한다. 이미 있는 `POST /orders`의 응답을 바꾸지 않는다. 새 응답 스키마에서 필드를 지우거나 이름·타입·뜻을 바꾸는 일이 없다. 오류 `code`는 값 목록이 아니라서 값 3개를 더해도 규칙을 어기지 않는다(research.md 항목 1) | 통과 |
| 8 | STD-014 | 해당 없음. 이 기능은 서비스 사이 API를 바꾸지 않는다. 조회 API는 외부 API다(research.md 항목 8) | 해당 없음 |
| 9 | STD-016 | 해당 없음. 재시도할 원격 호출이 없다 | 해당 없음 |
| 10 | STD-018 | 해당 없음. 원격 호출이 없다. 조회 서비스(`OrderQueryService`)의 읽기 전용 트랜잭션은 `client` 패키지를 부르지 않아서 `@Transactional`이 `client`를 직접 부르지 않는 ArchUnit 규칙을 그대로 통과한다 | 해당 없음 |
| 11 | STD-019 | 해당 없음. 다른 서비스를 동기로 부르지 않는다 | 해당 없음 |
| 12 | STD-006 | 남아야 하는 데이터가 모두 DB에 있다. 세션을 만들지 않는다 | 통과 |
| 13 | STD-007 | 앱 쪽 health 설정은 바꾸지 않는다. 조회 API는 health 경로를 건드리지 않는다. Deployment의 프로브는 티켓 003이다 | 통과 (앱 쪽), 해당 없음 (Deployment) |
| 14 | STD-008 | 그레이스풀 셧다운 설정은 바꾸지 않는다. `terminationGracePeriodSeconds`는 티켓 003이다 | 통과 (앱 쪽), 해당 없음 (Pod) |
| 15 | STD-009 | 환경마다 다른 새 값이 없다. 비밀값을 더하지 않는다 | 통과 |
| 16 | STD-011 | 해당 없음. replicas는 배포 매니페스트의 일이고 티켓 003이다 | 해당 없음 |
| 17 | STD-012 | 서비스 사이 호출이 없어서 추적 정보를 넘길 곳이 없다. 로그 줄의 trace ID는 001의 설정이 그대로 남긴다 | 통과 |
| 18 | STD-013 | 로그 출력 방식을 바꾸지 않는다. 파일 appender를 두지 않는다 | 통과 |
| 19 | STD-017 | 세 API 모두 서비스가 JWT를 직접 검증한다. 고객 ID는 토큰의 `sub`에서만 얻는다 | 통과 |

**이 표에 나온 항목**

| ID | 출처 | 원문 |
|---|---|---|
| STD-001 | `docs/standards/architecture-rules.md` 1절 "서비스 경계" | 규칙: 서비스마다 자기 DB를 가진다. 다른 서비스의 DB에 접속하지 않고, 필요한 데이터는 그 서비스의 API로 얻는다<br>이유: DB를 나누지 않으면 따로 배포하고 따로 바꿀 수 없다 |
| STD-002 | `docs/standards/architecture-rules.md` 1절 "서비스 경계" | 규칙: Pod 하나에는 마이크로서비스 하나만 넣는다. 여러 컨테이너는 로그 수집기 같은 사이드카일 때만 허용한다<br>이유: 한 Pod에 넣으면 따로 배포·확장할 수 없다 |
| STD-015 | `docs/standards/architecture-rules.md` 1절 "서비스 경계" | 규칙: `libs/`에는 여러 서비스가 꼭 함께 써야 하는 기술 코드(추적 설정 등)만 둔다. 도메인 규칙, DTO, Mapper와 SQL은 공유하지 않는다<br>이유: 공유가 늘면 서비스끼리 엮여 따로 배포할 수 없게 된다 |
| STD-003 | `docs/standards/architecture-rules.md` 2절 "서비스 사이 호출" | 규칙: 서비스 사이 호출은 쿠버네티스 Service 이름으로 한다(예: `http://inventory:8080`). Pod IP를 쓰지 않고, 별도 디스커버리 서버(Eureka 등)를 두지 않는다<br>이유: Pod는 바뀔 때마다 IP가 바뀐다 |
| STD-004 | `docs/standards/architecture-rules.md` 2절 "서비스 사이 호출" | 규칙: 모든 원격 호출에 연결 제한 시간과 응답 제한 시간을 둔다. 시간이 지나면 호출한 쪽은 기다리지 않고 실패 응답을 돌려준다. 값은 하드코딩하지 않고 설정으로 둔다<br>이유: 제한 시간이 없으면 한 서비스의 장애가 호출한 서비스로 번진다 |
| STD-005 | `docs/standards/architecture-rules.md` 2절 "서비스 사이 호출" | 규칙: 상태를 바꾸는 서비스 사이 API는 멱등이어야 한다. 호출한 쪽은 재시도할 때 같은 멱등 키(예: 주문 번호)를 다시 보낸다<br>이유: 응답만 사라진 경우 재시도가 중복 처리된다 |
| STD-010 | `docs/standards/architecture-rules.md` 2절 "서비스 사이 호출" | 규칙: API와 이벤트는 추가하는 방식으로만 바꾼다. 필드를 지우거나 이름·타입·뜻을 바꾸지 않는다. 깨는 변경이 꼭 필요하면 ADR을 먼저 쓴다. 받는 쪽은 모르는 필드를 무시한다. 상태 값(enum)을 추가하는 것은 깨는 변경으로 본다. 이 규칙은 main에 병합된 `contracts/` 현재본을 기준으로 한다. 병합하기 전의 PR 안에서 계약을 고치는 것은 깨는 변경이 아니다<br>이유: 배포 중에는 옛 버전과 새 버전이 서로 호출한다 |
| STD-014 | `docs/standards/architecture-rules.md` 2절 "서비스 사이 호출" | 규칙: 서비스 사이 API와 이벤트를 바꾸면 같은 PR에서 `contracts/`의 현재본(OpenAPI, AsyncAPI)도 고친다<br>이유: `specs/NNN/contracts/`에는 그 기능에서 바뀐 부분만 있어 전체 계약을 알 수 없다 |
| STD-016 | `docs/standards/architecture-rules.md` 2절 "서비스 사이 호출" | 규칙: 재시도는 멱등한 호출에만 한다. 연결 실패, 시도 제한 시간 초과, 502·503·504에만 재시도하고, 업무 결과(재고 부족)와 4xx에는 재시도하지 않는다. 재시도를 포함한 전체 시간은 정해진 한도를 넘지 않는다<br>이유: 멱등하지 않은 호출을 재시도하면 중복 처리된다. 업무 결과는 다시 물어도 같다 |
| STD-018 | `docs/standards/architecture-rules.md` 2절 "서비스 사이 호출" | 규칙: 원격 호출을 DB 트랜잭션 안에서 하지 않는다. 호출 전후의 저장은 각각 별도 트랜잭션으로 끝낸다<br>이유: 호출을 기다리는 동안 DB 연결과 잠금을 잡고 있게 된다. 최대 30초 동안 잡힐 수 있다 |
| STD-019 | `docs/standards/architecture-rules.md` 2절 "서비스 사이 호출" | 규칙: 다른 서비스를 동기로 호출할 때는 서킷 브레이커를 둔다. 서킷이 열려 있으면 호출하지 않고 바로 실패하며, 재시도도 하지 않는다<br>이유: 상대 서비스가 오래 멈추면 호출하는 쪽의 스레드가 묶여 함께 멈춘다. Java 17이라 가상 스레드로 피할 수 없다 |
| STD-006 | `docs/standards/architecture-rules.md` 3절 "실행과 배포" | 규칙: 남아야 하는 데이터(세션 포함)를 Pod 메모리나 로컬 디스크에 두지 않는다. DB 같은 외부 저장소에 둔다<br>이유: Pod는 언제든 사라지고, Pod끼리 메모리를 공유하지 않는다 |
| STD-007 | `docs/standards/architecture-rules.md` 3절 "실행과 배포" | 규칙: Actuator health로 readiness와 liveness를 노출하고, Deployment에 두 프로브를 모두 설정한다. liveness에는 외부 시스템(DB, 다른 서비스) 상태를 넣지 않는다<br>이유: 준비 전 요청과 교착을 막는다. liveness가 외부 장애에 반응하면 모든 Pod가 함께 재시작된다 |
| STD-008 | `docs/standards/architecture-rules.md` 3절 "실행과 배포" | 규칙: 그레이스풀 셧다운을 켠다. 앱의 종료 대기 시간(`spring.lifecycle.timeout-per-shutdown-phase`)은 Pod의 `terminationGracePeriodSeconds`보다 짧게 둔다. Spring Boot 버전마다 기본값이 다를 수 있으므로 설정에 명시한다<br>이유: 처리 중인 요청이 끊기지 않게 한다. 대기 시간이 더 길면 쿠버네티스가 먼저 강제 종료한다 |
| STD-009 | `docs/standards/architecture-rules.md` 3절 "실행과 배포" | 규칙: 이미지는 서비스마다 하나다. 환경마다 다른 값은 ConfigMap, 비밀값은 Secret에 두고 환경변수로 받는다. 이미지와 저장소에 비밀번호를 넣지 않는다. 로컬 실습용 비밀번호도 같다. 실제 값은 저장소에 올리지 않는 `.env` 파일에만 두고, 저장소에는 값을 비운 `.env.example`만 둔다<br>이유: 환경마다 이미지를 따로 만들지 않기 위해서다. 이 저장소는 공개 저장소라서, 저장소에 올린 비밀번호는 누구나 볼 수 있다 |
| STD-011 | `docs/standards/architecture-rules.md` 3절 "실행과 배포" | 규칙: 서비스마다 replicas를 2 이상으로 둔다<br>이유: 하나면 새 Pod가 뜰 때까지 서비스가 멈춘다 |
| STD-012 | `docs/standards/architecture-rules.md` 4절 "관측성" | 규칙: 서비스 사이 호출에 추적 정보를 넘기고, 로그 한 줄마다 trace ID를 남긴다. OpenTelemetry를 쓴다<br>이유: 주문 한 건의 로그가 여러 Pod에 나뉘어 남는다 |
| STD-013 | `docs/standards/architecture-rules.md` 4절 "관측성" | 규칙: 로그를 Pod 안 파일에 쓰지 않는다. 표준 출력으로 내보내고, 같은 로그를 OTLP로 관측 저장소(`grafana/otel-lgtm`)에도 보낸다<br>이유: Pod가 교체되면 Pod 안 로그는 사라진다. 저장소에 모아야 trace ID로 여러 서비스의 로그를 함께 찾는다 |
| STD-017 | `docs/standards/architecture-rules.md` 5절 "보안" | 규칙: 외부에 열린 API는 Keycloak이 발급한 JWT를 서비스가 직접 검증한다(OAuth2 Resource Server). 사용자 식별은 토큰의 `sub`로 하고, 요청 본문이나 별도 헤더로 받은 사용자 ID를 믿지 않는다<br>이유: Gateway를 거치지 않은 요청도 막고, 사용자 위조를 막는다 |

**1차 확인 결과**: 통과. 원칙 8개 모두 통과했다. 반드시 규칙 19개 가운데 실패는 0개다. 해당 없음은 #2, #4, #5, #6, #8, #9, #10, #11, #16 아홉 줄이고, #13, #14 두 줄은 일부가 해당 없음이다. 해당 없는 부분은 원격 호출이 없어서이거나(#4, #5, #6, #9, #10, #11), 서비스 사이 API가 아니어서이거나(#8), 배포 매니페스트라서 티켓 003에서 확인한다(#2, #13, #14, #16).

### 2차 확인 (Phase 1 뒤)

Phase 1 산출물(data-model.md, contracts/order-query-api.yaml, quickstart.md)과 research.md의 선택지를 기준으로 다시 확인했다. 아키텍처 규칙은 `docs/standards/architecture-rules.md`의 절과 규칙 이름으로 가리킨다.

| 원칙 | Phase 1 설계로 다시 확인한 내용 | 결과 |
|------|------------------------------|------|
| 서비스 자율성 | data-model.md 1절에서 읽는 테이블이 `ORDERS`, `ORDER_ITEMS`뿐이다. 새 클래스 목록(data-model.md 6절)이 모두 주문 서비스 패키지 안에 있다(1절 "서비스 경계"의 "서비스마다 자기 DB" 규칙과 "`libs/`에는 기술 코드만" 규칙) | 통과 |
| 계약 우선 | 계약 파일이 새 스키마 5개(`OrderItemView`, `OrderDetail`, `AdminOrder`, `OrderPage`, `AdminOrderPage`)와 `Problem`, `FieldError` 복사본만 담는다. 상태·사유·해제 결과의 값 목록이 `specs/001-place-order/data-model.md`와 같다. `Problem.code`는 `enum`이 아니다. 요청의 `status`, `releaseStatus`는 값 목록이지만 새 요청 매개변수이고 기존 호출을 바꾸지 않는다(2절 "서비스 사이 호출"의 "추가하는 방식으로만 바꾼다" 규칙과 "같은 PR에서 `contracts/` 현재본도 고친다" 규칙) | 통과 |
| 실패를 전제한 호출 | data-model.md 3절의 모든 조회가 DB 하나만 읽는다. 원격 호출이 없다(2절 "서비스 사이 호출"의 제한 시간, 재시도, 서킷 브레이커, 트랜잭션 밖 원격 호출 규칙은 해당 없음) | 통과 |
| 언제 사라져도 되는 Pod | `page`, `size`, `hasNext`는 요청과 응답에만 있고 서버에 남지 않는다. 쪽 크기 상수는 코드에 있고 환경마다 같다(3절 "실행과 배포"의 "남아야 하는 데이터는 외부 저장소에" 규칙) | 통과 |
| 관측 가능성 | 로그 설정에 변동이 없다. data-model.md 2절이 응답에서 `CUSTOMER_ID`를 뺐다(4절 "관측성"의 로그 규칙) | 통과 |
| 서비스가 직접 확인하는 신원과 코드 밖의 비밀 | 계약 파일이 세 경로에 `bearerAuth`를 요구하고 401과 403을 Problem Details로 정의한다. 요청 매개변수에 고객 ID 필드가 없다. 알 수 없는 쿼리 매개변수(`customerId` 등)는 무시한다(5절 "보안"의 JWT 직접 검증 규칙) | 통과 |
| 실제 조건에서 테스트 먼저 | 아래 "인수 시나리오와 테스트 층" 표에서 16개가 모두 테스트 층과 서비스 통합 테스트 클래스에 배정됐다. 기대값을 바꾼 곳이 없다. 기대값이 문서에 없는 동작은 검사하지 않기로 한 목록이 research.md 3절에 있다 | 통과 |
| 검수할 수 있는 산출물 | 문서마다 인용 블록의 원문을 기준 문서와 글자 단위로 대조했다. 보고서에 방법과 결과를 적었다. 범위 표기와 직접 만든 줄임 표기를 쓰지 않았다 | 통과 |

**2차 확인 결과**: 통과. 1차와 결과가 같다. 설계 때문에 새로 생긴 위반은 없다.

## Project Structure

### Documentation (this feature)

```text
specs/002-view-orders/
├── spec.md              # /speckit-specify 산출물 (PR #29로 병합)
├── plan.md              # 이 파일
├── research.md          # Phase 0: 선택지, 근거, 확인하지 못한 것
├── data-model.md        # Phase 1: 읽는 열, 조회 규칙, 응답 모양
├── quickstart.md        # Phase 1: 검증 방법
├── contracts/
│   └── order-query-api.yaml   # 주문 조회 외부 API (이 기능이 더한 부분)
├── checklists/
│   └── requirements.md  # spec 품질 체크리스트
└── tasks.md             # /speckit-tasks가 만든다 (이 명령은 만들지 않는다)
```

### Source Code (repository root)

새 파일은 `새`, 기존 파일을 고치는 곳은 `고침`으로 표시했다. 표시가 없는 파일은 바꾸지 않는다. 파일 이름은 tasks.md가 고르는 `[제안]`이다.

```text
msa-example/
├── config/quality/test-case-stages.csv        # 고침: 조회 케이스의 단계 값 (tasks 단계, 사용자 확인)
├── gradle.properties                          # 고침: msa.stage (tasks 단계, 사용자 확인)
├── build-logic/src/main/groovy/msa/quality/
│   └── TestCaseIdCheck.groovy                 # 고침: 단계 순서 목록 STAGE_ORDER (tasks 단계, 사용자 확인)
├── contracts/                                 # 바꾸지 않는다 (서비스 사이 API만 있다)
└── services/order-service/
    ├── CLAUDE.md                              # 고침: "하는 일"과 패키지 구조에 조회를 적는다
    └── src/
        ├── main/java/com/example/msa/order/
        │   ├── controller/OrderQueryController.java     # 새: GET 3개. 요청 받기, 서비스 호출, 응답 변환만
        │   ├── service/OrderQueryService.java           # 새: 읽기 전용 트랜잭션. 흐름만 조립
        │   ├── mapper/OrderMapper.java                  # 고침: 조회 메서드 3개 추가
        │   ├── domain/                                  # 새: OrderNumbers, PageRequest, OrderSearch, InvalidQueryException
        │   ├── dto/                                     # 새: OrderDetailResponse, AdminOrderResponse, OrderPage
        │   │   └── OrderWithItems.java                  # 고침: createdAt 필드
        │   ├── exception/
        │   │   ├── ProblemAccessDeniedHandler.java      # 새: 403 Problem Details
        │   │   └── ProblemDetailsHandler.java           # 고침: 404, 400(조회 조건), 403 본문 만드는 메서드와 InvalidQueryException 처리
        │   └── config/
        │       ├── SecurityConfig.java                  # 고침: /admin/** 규칙, 역할 변환기, 403 처리기 연결
        │       └── RealmRoleJwtConverter.java           # 새: realm_access.roles → ROLE_*
        ├── main/resources/
        │   └── mapper/OrderMapper.xml                   # 고침: orderViewMap(resultMap), select 3개
        │                                                # db/migration은 바꾸지 않는다 (새 파일 없음)
        └── test/java/com/example/msa/order/
            ├── domain/                                  # 새: OrderNumbersTest, PageRequestTest, OrderSearchTest
            ├── config/RealmRoleJwtConverterTest.java    # 새: 역할 변환기 단위 테스트
            ├── mapper/OrderQueryMapperTest.java         # 새: 조회 SQL의 Mapper 테스트 (OrderMapperTest는 고치지 않는다)
            └── integration/
                ├── ViewTestSupport.java                 # 새: 주문 준비·조회 도우미. IntegrationTestSupport를 상속
                ├── ViewOrderIntegrationTest.java        # 새: 시나리오 1.1~1.5
                ├── ListOrdersIntegrationTest.java       # 새: 시나리오 2.1~2.6
                ├── UnauthenticatedViewIntegrationTest.java  # 새: 시나리오 3.1
                ├── AdminOrderSearchIntegrationTest.java # 새: 시나리오 4.1~4.4
                └── OrderOwnerQueryIntegrationTest.java  # 새: 쿼리의 고객 ID를 무시하는지 (받치는 테스트)
```

**Structure Decision**: 패키지 구조는 코딩 규약 2절을 따르고, 001의 `OrderController`와 `OrderService`는 고치지 않는다. 조회는 새 컨트롤러와 새 서비스로 나눈다. 이유는 세 가지다.

- 주문 생성은 `OrderService`가 트랜잭션 없이 원격 호출을 포함한 흐름을 조립한다. 조회는 읽기 전용 트랜잭션 하나로 끝난다. 한 클래스에 섞으면 클래스의 규칙(트랜잭션 없음)과 조회의 규칙(읽기 전용 트랜잭션)이 부딪힌다.
- 컨트롤러의 `@RequestMapping("/orders")`가 `POST /orders`에 묶여 있다. 관리자 경로(`/admin/orders`)를 같은 클래스에 두면 클래스 하나가 두 경로 접두사를 가진다.
- 001 코드를 고치는 줄이 줄어든다. 고치는 곳은 `OrderMapper`(메서드 추가), `OrderMapper.xml`(SQL 추가), `OrderWithItems`(필드 추가), `ProblemDetailsHandler`(메서드 추가), `SecurityConfig`뿐이다.

`OrderQueryController`가 `@RequestMapping`을 클래스에 두지 않고 메서드마다 경로를 쓰는 것은 tasks.md가 정한다.

테스트 클래스를 시나리오 묶음(스토리)별로 나눈 것은 `[제안]`이다. 001은 테스트 케이스마다 클래스를 나눴다. 이 plan은 같은 도우미와 같은 준비 코드를 쓰는 시나리오를 한 클래스에 모았다.

## PR 단계

**사용자 결정 (2026-10-11): Y안, PR 2개.** 아래 표는 고르기 전의 후보와 근거를 남긴 것이다. 단계 이름(`T002-P1`, `T002-P2`)과 브랜치 이름(`feat/002-view-orders-p1`, `feat/002-view-orders-p2`)은 사용자가 정하지 않았다 `[제안]`. 단계마다 브랜치 하나, PR 하나다. 브랜치 이름은 `docs/standards/git-workflow.md` 2절의 규칙(`feat/002-view-orders-p<번호>`)을 따른다.

| 후보 | 나누는 방식 | 장점 | 단점 |
|---|---|---|---|
| X. PR 1개 | 스토리 4개를 모두 한 PR | 단계 표 변경이 한 번이다 | 보안 설정 변경과 목록 조회가 한 PR에 섞여 검토할 양이 늘어난다 |
| **Y. PR 2개 (추천)** | 1단계: 스토리 1, 2, 3(시나리오 1.1~1.5, 2.1~2.6, 3.1). 2단계: 스토리 4(시나리오 4.1~4.4) | spec의 우선순위(P1, P2)가 그대로 PR 경계다. 관리자 규칙, 역할 변환기, 403 처리기가 2단계에만 있다 | 1단계가 쪽 지정과 DTO까지 포함해 크다 |
| Z. PR 3개 | 1단계: 스토리 1, 3. 2단계: 스토리 2. 3단계: 스토리 4 | PR이 가장 작다 | 단계 표를 세 번 바꾼다. 스토리 1과 2가 같은 DTO와 `resultMap`을 쓰는데 PR이 나뉜다 |

추천 근거와 자세한 비교는 research.md 항목 14에 있다.

PR과 별개로 먼저 처리할 일이 둘 있다. 둘 다 사용자 결정이 필요하다.

- **docs PR(완료)**: 기대값 후보 1~4와 새 테스트 케이스를 테스트 케이스 문서에 더한 PR #30이 2026-10-12에 병합됐다. 이제 `code` 값, 정렬, 쪽 크기를 인수 시나리오 테스트가 검사할 수 있다.
- **spec PR**: spec에 시나리오 2.6과 요구사항 한 줄을 더하는 별도 PR이다. plan이 spec의 시나리오 번호를 가리키므로 구현 PR보다 먼저 병합한다.
- **설정 변경**: 위 Source Code 트리에서 "tasks 단계, 사용자 확인"이라고 적은 세 파일이다. 어느 PR 단계에서 고칠지는 tasks.md가 정한다. 단계 이름 목록(`STAGE_ORDER`)은 1단계 PR에서 더하는 것이 자연스럽다 `[제안]`.

## 인수 시나리오와 테스트 층

각 시나리오를 어느 테스트 층에서 검증하는지 적는다. "단계" 칸은 사용자가 정한 Y안(PR 2개)을 기준으로 한다. 단계 이름 `T002-P1`, `T002-P2`도 `[제안]`이다(001의 `P1`, `P2`, `P3`와 겹치지 않게 했다). 사용자가 PR 개수를 정하면 tasks.md가 고쳐 쓴다. 이 표의 "서비스" 칸이 `test-case-stages.csv`의 내용이 된다(서비스 값은 `order`). 인수 시나리오 테스트의 `@DisplayName`은 테스트 케이스 번호로 시작한다. 단위·Mapper 테스트는 인수 시나리오를 받치는 테스트라서 번호를 붙이지 않는다 `[제안]`.

| 테스트 케이스 | spec 시나리오 | 서비스 | 단계 (Y안) | 단위 | Mapper | 서비스 통합 (인수) |
|---|---|---|---|---|---|---|
| TC-002 | 1.1 | 주문 | T002-P1 | — | 거절 주문의 상태·사유·항목별 거절 사유 읽기 | `ViewOrderIntegrationTest`: 거절 주문을 C1이 조회하면 받는다 |
| TC-010 | 1.2 | 주문 | T002-P1 | — | 주문 번호와 고객이 모두 맞아야 읽힌다 | `ViewOrderIntegrationTest`: C2가 C1의 주문을 조회하면 404 |
| TC-026 | 1.3 | 주문 | T002-P1 | `OrderNumbersTest`: `abc`는 숫자가 아님 | — | `ViewOrderIntegrationTest`: 주문 9999, 주문 번호 `abc`가 404이고 다른 고객의 주문과 같은 응답 |
| TC-031 | 1.4 | 주문 | T002-P1 | — | 항목별 거절 사유(`OUT_OF_STOCK`, `PRODUCT_NOT_FOUND`)를 읽기 | `ViewOrderIntegrationTest`: 사유 "재고 부족"과 B, 사유 "상품 없음"과 Z·B |
| TC-034 | 1.5 | 주문 | T002-P1 | — | `CREATED_AT`을 읽기 | `ViewOrderIntegrationTest`: 주문 번호, 항목 A 3개, 상태 "확정", 주문 시각 |
| TC-010 | 2.1 | 주문 | T002-P1 | — | 고객 조건으로 목록을 읽기 | `ListOrdersIntegrationTest`: C1의 목록에 C1의 주문만 |
| TC-029 | 2.2 | 주문 | T002-P1 | — | 두 고객의 주문이 있을 때 고객 조건으로 거르기 | `ListOrdersIntegrationTest`: C1에게 1004만, C2에게 1005만 |
| TC-028 | 2.3 | 주문 | T002-P1 | — | 주문이 없는 고객은 빈 결과 | `ListOrdersIntegrationTest`: C2의 빈 목록, 200 |
| TC-033 | 2.4 | 주문 | T002-P1 | — | 처리중·실패 주문을 읽기 | `ListOrdersIntegrationTest`: 처리중, 실패(사유 "잠시 후 다시 시도"), 목록에 둘 다 |
| TC-030 | 2.5 | 주문 | T002-P1 | — | — | `ListOrdersIntegrationTest`: M1(관리자 역할)이 자기 주문 1006만, 1006은 받고 C1의 1004는 404 |
| TC-035 | 2.6 | 주문 | T002-P1 | `PageRequestTest`: `size=101`, 음수 `page`는 틀림 | 정렬(주문 번호가 큰 순서), `size + 1`개 읽기, 쪽 경계 | `ListOrdersIntegrationTest`: 주문 25개, 쪽 지정 없이 20개와 `hasNext` true, `page=1`은 5개와 false, `size=101`은 400 |
| TC-025 | 3.1 | 주문 | T002-P1 | — | — | `UnauthenticatedViewIntegrationTest`: 토큰 없음 3경로, 서명이 틀린 토큰, 만료된 토큰이 401 |
| TC-010 | 4.1 | 주문 | T002-P2 | `RealmRoleJwtConverterTest`: `realm_access.roles`가 권한이 됨 | — | `AdminOrderSearchIntegrationTest`: C1의 관리자 조회가 403 |
| TC-010 | 4.2 | 주문 | T002-P2 | — | — | `AdminOrderSearchIntegrationTest`: M1이 상태 "실패"로 조회하면 C1·C2의 실패 주문이 해제 결과와 함께 |
| TC-027 | 4.3 | 주문 | T002-P2 | `OrderSearchTest`: 없는 이름·값은 틀림 | 상태·해제 결과 조건 조합 | `AdminOrderSearchIntegrationTest`: 조건 없음, 둘 다, 해제 결과만, 없는 상태, 없는 해제 결과 |
| TC-032 | 4.4 | 주문 | T002-P2 | — | 해제 결과가 비어 있는 주문을 `null`로 읽기 | `AdminOrderSearchIntegrationTest`: 해제 실패, 상태 "실패" 셋, 처리중 |

**이 표에 나온 항목**

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

> **TC-026** · `docs/test-cases/order-placement.md` 절 "TC-026 없는 주문 번호와 숫자가 아닌 주문 번호는 "없음"이다"
> - **Given** 주문 9999는 없다
> - **When** C1이 주문 9999를 조회한다
> - **Then** "없음"(404)을 받는다. 다른 고객의 주문을 조회했을 때와 같은 응답이다
> - **When** C1이 주문 번호 `abc`로 조회한다
> - **Then** "없음"(404)을 받는다

> **TC-031** · `docs/test-cases/order-placement.md` 절 "TC-031 거절된 주문을 조회하면 사유와 상품 목록이 함께 나온다"
> - **Given** 상품 A의 재고가 10개, 상품 B의 재고가 1개다. 상품 Z는 재고 데이터에 없다
> - **And** C1이 상품 A 3개와 상품 B 2개를 주문해 "거절"로 기록됐다
> - **When** C1이 그 주문을 조회한다
> - **Then** 상태 "거절", 사유 "재고 부족", 부족한 상품 B가 나온다
> - **Given** C1이 상품 A 3개, 상품 Z 1개, 상품 B 2개를 주문해 "거절"로 기록됐다
> - **When** C1이 그 주문을 조회한다
> - **Then** 상태 "거절", 사유 "상품 없음", 없는 상품 Z와 부족한 상품 B가 모두 나온다

> **TC-034** · `docs/test-cases/order-placement.md` 절 "TC-034 고객이 확정 주문을 조회하면 주문 번호, 항목, 상태, 주문 시각이 나온다"
> - **Given** 상품 A의 재고가 10개다
> - **And** C1이 상품 A 3개를 주문해 "확정"으로 기록됐다
> - **When** C1이 그 주문을 조회한다
> - **Then** 주문 번호, 주문 항목(상품 A, 수량 3), 상태 "확정", 주문 시각이 나온다

> **TC-029** · `docs/test-cases/order-placement.md` 절 "TC-029 고객의 목록에는 다른 고객의 주문이 섞이지 않는다"
> - **Given** C1에게 주문 1004가 있고, C2에게 주문 1005가 있다
> - **When** C1이 자기 주문 목록을 조회한다
> - **Then** 주문 1004만 나오고 주문 1005는 나오지 않는다
> - **When** C2가 자기 주문 목록을 조회한다
> - **Then** 주문 1005만 나오고 주문 1004는 나오지 않는다

> **TC-028** · `docs/test-cases/order-placement.md` 절 "TC-028 주문이 하나도 없는 고객의 목록은 비어 있다"
> - **Given** C2의 주문이 하나도 없다
> - **When** C2가 자기 주문 목록을 조회한다
> - **Then** 오류 없이 빈 목록을 받는다

> **TC-033** · `docs/test-cases/order-placement.md` 절 "TC-033 고객은 처리중인 주문과 실패한 주문도 조회한다"
> - **Given** C1에게 "처리중" 주문과 "실패" 주문이 있다
> - **When** C1이 "처리중" 주문을 조회한다
> - **Then** 상태 "처리중"인 그 주문을 받는다
> - **When** C1이 "실패" 주문을 조회한다
> - **Then** 상태 "실패", 사유 "잠시 후 다시 시도"인 그 주문을 받는다
> - **When** C1이 자기 주문 목록을 조회한다
> - **Then** "처리중" 주문과 "실패" 주문이 모두 나온다

> **TC-030** · `docs/test-cases/order-placement.md` 절 "TC-030 관리자 역할만 있는 사용자는 고객용 조회로 자기 주문만 본다"
> - **Given** M1의 주문 1006과 C1의 주문 1004가 있다
> - **When** M1이 자기 주문 목록을 조회한다
> - **Then** 주문 1006만 나온다
> - **When** M1이 주문 1006을 조회한다
> - **Then** 주문 1006을 받는다
> - **When** M1이 C1의 주문 1004를 조회한다
> - **Then** "없음"(404)을 받는다

> **TC-035** · `docs/test-cases/order-placement.md` 절 "TC-035 주문 목록은 최근 주문이 먼저 나오고 쪽으로 나뉜다"
> - **Given** C1에게 주문 25개가 있다
> - **When** C1이 쪽 지정 없이 자기 주문 목록을 조회한다
> - **Then** 가장 최근 주문 20개가 최근 순서로 나오고 `hasNext`는 true다
> - **When** C1이 `page=1`로 조회한다
> - **Then** 나머지 5개가 나오고 `hasNext`는 false다
> - **When** C1이 `size=101`로 조회한다
> - **Then** 400을 받는다

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

> **TC-032** · `docs/test-cases/order-placement.md` 절 "TC-032 관리자는 해제 실패 주문, 해제 결과가 비어 있는 주문, 처리중 주문을 찾는다"
> - **Given** C1의 "실패" 주문(해제 결과 "해제 실패"), C2의 "실패" 주문(해제 결과 "해제 완료"), C2의 "실패" 주문(해제 결과가 비어 있음), C1의 "처리중" 주문이 있다
> - **When** M1이 해제 결과 "해제 실패"로 관리자 조회를 한다
> - **Then** C1의 해제 실패 주문만 나온다
> - **When** M1이 상태 "실패"로 관리자 조회를 한다
> - **Then** "실패" 주문 세 개가 나오고, 해제 결과가 비어 있는 주문은 비어 있는 채로 나온다
> - **When** M1이 상태 "처리중"으로 관리자 조회를 한다
> - **Then** C1의 "처리중" 주문만 나온다

받치는 테스트로, 테스트 케이스에 매이지 않는 아래 테스트를 둔다. 기대값 출처는 이 표에 쓴 것을 따른다.

| 테스트 | 층 | 확인하는 것 | 기대값 출처 |
|---|---|---|---|
| `PageRequestTest` | 단위 | 쪽 크기와 쪽 번호의 검사 | 테스트 케이스 문서의 공통 전제 마지막 줄(`size` 기본 20·최대 100, 범위 밖 400)과 시나리오 2.6 |
| `OrderQueryMapperTest` | Mapper | 인라인 뷰로 자른 뒤 항목을 합치는 SQL이 주문 단위로 묶이는지 | SQL이 맞게 도는지는 기대값이 아니라 동작 확인이다. 정렬 순서(주문 번호가 큰 순서)는 테스트 케이스 문서의 공통 전제 마지막 줄이 기대값이다 |
| `OrderOwnerQueryIntegrationTest` | 서비스 통합 | `GET /orders?customerId=C2`를 C1이 부르면 C1의 주문만 나온다 | BR-101의 첫째 문장(다른 고객의 주문을 볼 수 없다) |

**이 표에 나온 항목**

| ID | 출처 | 원문 |
|---|---|---|
| BR-101 | `docs/requirements/uc-002-view-orders.md` 3절 "업무 규칙" | 고객은 다른 고객의 주문을 볼 수 없다. 다른 고객의 주문 번호로 조회하면 "없음"으로 응답해, 그 주문이 있는지도 알 수 없게 한다 |

모든 단계에서 함께 도는 아키텍처 테스트(ArchUnit)와 Gradle 기계 검사 작업은 그대로 돈다. 새 클래스는 이 검사를 모두 통과해야 한다. 운영 테스트와 E2E는 티켓 003이다.

**알아 둘 것**:

- 조회는 재고 서비스를 부르지 않아서 WireMock이 필요 없다. 서비스 통합 테스트는 `InventoryWireMock`이 아니라 `IntegrationTestSupport`를 상속한 `ViewTestSupport`를 쓴다(research.md 항목 13).
- "주문이 있다"는 상태는 `OrderMapper`로 직접 저장해서 준비한다(research.md 항목 12). 주문 번호는 시퀀스가 정하므로, 테스트 케이스 문서의 "주문 1004"는 테스트에서 저장 결과의 번호로 대신한다.
- 인수 시나리오 테스트는 `@DisplayName`을 테스트 케이스 번호로 시작하므로, 같은 번호를 쓰는 001의 테스트와 이름이 겹칠 수 있다. 번호를 같이 쓰는 두 케이스는 plan의 표로 구분한다(research.md 항목 13의 "검사의 한계").

## Complexity Tracking

> **권장 규칙을 어길 때만 채운다.** "반드시" 규칙 위반은 여기에 적어 넘길 수 없다.
> 바꾸려면 ADR을 쓰고 헌법을 개정한다 (.specify/memory/constitution.md의 "거버넌스" 절).

없음. 아키텍처 규칙 문서의 규칙은 모두 "반드시" 수준이고, 이 plan은 그 규칙을 어기지 않는다.

## 용어

- **외부 API (external API)**: 고객이나 관리자가 Gateway를 거쳐 부르는 API다. 서비스끼리 부르는 서비스 사이(내부) API와 구별한다. 조회 API 세 개가 외부 API다.
- **읽기 전용 트랜잭션 (read-only transaction)**: 데이터를 바꾸지 않는다고 알리고 여는 트랜잭션이다. 조회 서비스가 쓴다.
- **쪽 지정 (paging)**: 많은 결과를 정해진 개수씩 나눠 읽는 것이다. 이 기능은 `page`와 `size`로 지정하고, 응답의 `hasNext`가 다음 쪽이 있는지 알린다.
- **realm 역할 (realm role)**: Keycloak 안에서 사용자에게 붙이는 역할이다. 토큰의 `realm_access.roles`에 들어 있고, 이 프로젝트의 `CUSTOMER`와 `ADMIN`이다.
- **`JwtAuthenticationConverter`**: 검증한 JWT를 Spring Security의 인증 정보(권한 포함)로 바꾸는 변환기다. 이 기능은 토큰의 `realm_access.roles`를 권한으로 바꾸는 데 쓴다.
- **`AccessDeniedHandler`**: 로그인은 했지만 권한이 없는 요청을 어떤 응답으로 거부할지 정하는 Spring Security의 처리기다. 이 기능은 403을 Problem Details로 낸다.
- **Problem Details**: HTTP API의 오류 응답을 JSON으로 적는 표준 형식(RFC 9457)이다. 모든 오류 응답이 이 형식이다.
- **도우미 클래스 (test support class)**: 여러 테스트가 함께 쓰는 준비 코드를 모아 둔 테스트용 클래스다. 이 기능은 `ViewTestSupport`다.
- **받치는 테스트 (supporting test)**: 인수 시나리오를 직접 검증하지 않고 단위, Mapper, 변환기를 확인하는 테스트다. 테스트 케이스 번호를 붙이지 않는다.
- **테스트 케이스 단계 표 (test-case stage table)**: 테스트 케이스마다 어느 서비스가 어느 단계에서 테스트를 갖춰야 하는지 적은 표(`config/quality/test-case-stages.csv`)다. 테스트 케이스 ID 검사가 이 표와 `msa.stage`로 검사 범위를 정한다.
- **단계 (stage)**: PR 경계에 붙인 이름이다. 001은 `P1`, `P2`, `P3`이고, 이 기능은 후보로 `T002-P1`, `T002-P2`를 제안한다.
- **Constitution Check**: plan이 헌법을 지키는지 확인하는 절이다. Phase 0 전에 한 번, Phase 1 뒤에 다시 확인한다.
- **Phase 0, Phase 1**: `/speckit-plan`의 단계다. Phase 0은 정하지 않은 기술 문제를 조사해 research.md에 적고, Phase 1은 data-model, contracts, quickstart를 만든다.
