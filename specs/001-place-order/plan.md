# Implementation Plan: 주문 생성

**Branch**: `feat/001-place-order` (spec·plan·tasks, PR #16으로 병합), `feat/001-place-order-p1` (P1), `feat/001-place-order-p2` (P2), `feat/001-place-order-p3` (P3) | **Date**: 2026-10-05 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/001-place-order/spec.md`

**함께 만든 문서**: [research.md](research.md) (결정과 버전), [data-model.md](data-model.md) (테이블과 처리 순서), [contracts/inventory-api.yaml](contracts/inventory-api.yaml), [contracts/order-api.yaml](contracts/order-api.yaml), [quickstart.md](quickstart.md) (검증 방법). tasks.md는 `/speckit-tasks`가 만든다.

이 문서에서 "사용자 결정"은 2026-10-05 대화에서 사용자가 고른 것이다. `[제안]`은 Claude가 채운 것이고, 자세한 이유는 research.md에 있다.

## Summary

주문 생성 기능을 두 서비스로 만든다.

- **재고 서비스**는 주문 번호를 멱등 키로 하는 예약·해제 API를 만든다. 재고 행은 상품 ID 순서로 잠근다. 그래서 동시 주문에도 초과 판매와 교착이 없다.
- **주문 서비스**는 JWT로 고객을 확인하고, 주문 요청 키로 중복 주문을 막는다. 주문을 "처리중"으로 먼저 기록한 뒤 `RestClient`로 재고를 예약한다.
- 재고 호출에는 Resilience4j 서킷 브레이커와 시도당 2.5초 제한 시간을 둔다(P2). 재시도와 재시도 포함 전체 30초 한도는 P3에서 더한다.
- 실패한 주문의 예약 해제는 고객 응답 뒤 별도 실행기에서 한다.

PR은 우선순위대로 세 개로 나눈다. P1(빌드 골격 + 기계 검사 + CI + 재고 서비스 + 재고 쪽 인수 시나리오 테스트 전부), P2(주문 서비스 + 서킷 브레이커), P3(재시도 + 30초 한도)다. 그 앞에 docs PR 두 개(0-A 테스트 케이스 추가, 0-B 기준 문서 맞추기)를 병합한다.

## Technical Context

**Language/Version**: Java 17 (JDK 17.0.2)

**Primary Dependencies**:

- Spring Boot 4.1.1 (Spring Framework 7.0.9, Spring Security 7.1.1). 스타터는 webmvc, validation, actuator, flyway, opentelemetry이고, 주문 서비스에는 oauth2-resource-server와 restclient가 더 붙는다.
- mybatis-spring-boot-starter 4.1.0 (MyBatis 3.5.19, MyBatis-Spring 4.1.0)
- Flyway 12.4.0 + flyway-database-oracle, Oracle JDBC 23.26.3.0.0 (Spring Boot 관리)
- Resilience4j 2.4.0 `resilience4j-circuitbreaker`, `resilience4j-retry` (주문 서비스만)
- OpenTelemetry Logback appender 2.28.1-alpha
- 버전을 고른 이유와 확인한 방법: research.md 0절

**Storage**: Oracle Database 21c XE, 서비스마다 컨테이너 하나(`order-db`의 `ORDER_SVC`, `inventory-db`의 `INVENTORY_SVC`). 스키마와 초기 데이터는 Flyway로 관리한다. 테이블은 data-model.md에 있다.

**Testing**:

- JUnit Jupiter 6.0.3(Spring Boot 관리), AssertJ 3.27.7
- Testcontainers 2.0.5(`testcontainers-oracle-xe`, 이미지 `gvenzl/oracle-xe:21.3.0-slim-faststart`)
- WireMock 3.13.2(`wiremock-standalone`), Spring Security Test
- ArchUnit 1.5.1, mybatis-spring-boot-starter-test 4.1.0
- swagger-request-validator-core 2.46.1(계약 테스트)

**Build**:

- Gradle wrapper 9.8.0(Groovy DSL)
- 루트 `settings.gradle`에 포함 빌드 `build-logic/`과 버전 카탈로그 `gradle/libs.versions.toml`을 둔다.
- Spotless 8.10.3 + palantir-java-format 2.101.0
- SpotBugs Gradle 플러그인 6.5.12 + SpotBugs 4.10.4
- CI는 GitHub Actions다.

**Target Platform**: 이 PC(Windows 11, Docker Desktop)에서 테스트한다. 배포 대상은 kind 위의 Linux 컨테이너이고, 배포는 티켓 003에서 한다.

**Project Type**: 웹 서비스 두 개(Spring Boot 마이크로서비스)를 묶은 Gradle 멀티 프로젝트

**Performance Goals**: 이 기능의 성능 목표는 아래 비기능 요구사항의 목표값뿐이다. 처리량 목표는 없다.

> **NFR-002** · `docs/requirements/non-functional.md` 비기능 요구사항 표
> 요구사항: **장애 전파 차단.** 재고 서비스가 느리거나 멈춰도 주문 서비스는 멈추지 않고, 정해진 제한 시간 안에 실패를 응답한다
> 목표값: 재시도 포함 전체 30초, 시도당 2.5초, 재시도 최대 5회(즉시, 1, 2, 4, 8초 뒤). 서킷이 열린 뒤에는 1초 안에 실패 응답

**Constraints**:

- Java 17이라 가상 스레드를 쓸 수 없다. 재고 호출이 길어지면 요청 스레드가 묶이므로 서킷 브레이커로 막는다.
- SQL은 19c에서 도는 것만 쓴다.
- Oracle XE의 자원 제한은 CPU 2개, 메모리 2GB, 사용자 데이터 12GB다.

**Scale/Scope**:

- 서비스 2개, 테이블 5개, 시퀀스 1개
- API 3개: 외부 `POST /orders`, 내부 `PUT /reservations/{orderNo}`, 내부 `DELETE /reservations/{orderNo}`
- 인수 시나리오: 테스트 케이스 15개, 그리고 추가할 케이스 4개(research.md 결정 2)

## Constitution Check

*GATE: Phase 0 연구를 시작하기 전에 통과해야 한다. Phase 1 설계가 끝난 뒤 다시 확인한다.*

**먼저 읽은 문서**: `docs/standards/`의 다섯 문서를 모두 읽었다(`architecture-rules.md`, `tech-stack.md`, `testing.md`, `coding-conventions.md`, `git-workflow.md`). 함께 읽은 문서는 다음과 같다.

- `docs/design/architecture.md`
- `docs/adr/`의 ADR 7개
- `docs/test-cases/order-placement.md`, `docs/test-cases/operations.md`
- `docs/requirements/`의 세 문서, `docs/analysis/domain-analysis.md`
- `.specify/memory/constitution.md`, 저장소 루트 `CLAUDE.md`

### 1차 확인 (Phase 0 전)

#### 원칙 확인

| 원칙 | 확인할 질문에 대한 답 | 결과 |
|------|----------------------|------|
| 서비스 자율성 | 주문 서비스는 `order-db`, 재고 서비스는 `inventory-db`만 쓴다. 주문 서비스는 재고 수량을 재고 API로만 다룬다. `libs/`에는 ArchUnit 규칙(`libs/archunit-rules`)만 두고 업무 규칙, DTO, Mapper, SQL은 두지 않는다. 계약 테스트 도우미도 공유하지 않고 서비스마다 둔다 | 통과 |
| 계약 우선 | 재고 API를 새로 만든다. P1 단계 PR에서 저장소 루트 `contracts/inventory-api.yaml`을 함께 만든다. 새 API라서 지우거나 바뀌는 필드가 없다. 상태 값 목록은 처음부터 모두 정한다(research.md 결정 13). 오류 `code`는 enum으로 묶지 않아 나중에 값을 더해도 깨지지 않는다 | 통과 |
| 실패를 전제한 호출 | 재고 호출 주소는 Service 이름이 기본값이고 설정으로 바꾼다. 연결·요청 제한 시간은 2.5초이고 설정값이다(P2). 예약은 주문 번호를 멱등 키로 한 `PUT`, 해제는 멱등한 `DELETE`다. 재시도는 연결 실패·시간 초과·502·503·504에만 하고, 전체 30초 안에서만 한다(P3). 서킷 브레이커는 재고 호출을 처음 만드는 P2부터 둔다. 원격 호출은 트랜잭션 밖(`OrderService`)에서 하고, 앞뒤 저장은 `OrderTxService`의 별도 트랜잭션이다 | 통과 |
| 언제 사라져도 되는 Pod | 남아야 하는 데이터(주문, 요청 키, 예약 기록)는 모두 DB에 있다. 서킷 상태는 Pod 메모리에 있지만 남아야 하는 데이터가 아니다. 해제 대기열도 메모리에 있다. 인스턴스가 죽으면 해제 결과가 빈 "실패" 주문이 남는데, 이것은 설계 3절이 적어 둔 알려진 한계다. 새 설정값은 환경변수로 받는다. ConfigMap·Secret과 Deployment 프로브는 티켓 003이다 | 통과 |
| 관측 가능성 | `RestClient`를 Spring Boot가 주는 `RestClient.Builder`로 만들어 `traceparent`를 넘긴다(P2) `[제안]`. 로그는 ECS JSON으로 표준 출력에 쓰고 `[제안]` trace ID를 MDC로 남긴다. 같은 로그를 OpenTelemetry Logback appender로 OTLP에 보낸다. 재고 호출 로그에는 시도 번호와 서킷 상태를 남긴다. 토큰과 비밀번호는 로그에 남기지 않는다 | 통과 |
| 서비스가 직접 확인하는 신원과 코드 밖의 비밀 | `POST /orders`는 주문 서비스가 JWT를 직접 검증하고, 고객 ID는 토큰의 `sub`에서만 얻는다. 재고 API는 외부에 열지 않으며 이번 단계에서 서비스 간 인증을 하지 않는다(인증 결정 ADR의 감수할 점). DB 비밀번호는 환경변수와 `.env`로 넣는다. 테스트는 Testcontainers가 만든 임시 계정을 쓴다 | 통과 |
| 실제 조건에서 테스트 먼저 | 인수 시나리오마다 검증 테스트가 있다(아래 "인수 시나리오와 테스트 층" 표). 영속성은 Testcontainers의 실제 Oracle, 상대 서비스는 WireMock이다. 기대값은 확정된 `docs/test-cases/`에서 그대로 가져온다. 테스트 케이스 문서에 없는 기대값은 문서에 먼저 더한다(research.md 결정 2). tasks.md에서 테스트 작업을 구현보다 앞에 둔다. 초안 상태의 문서에서 가져온 기대값은 없다 | 통과 |
| 검수할 수 있는 산출물 | plan, research, data-model, contracts, quickstart에 쓴 항목 ID는 바로 아래에 출처와 원문을 펼쳤다. Claude가 채운 것에는 `[제안]`을 붙였다. 각 파일 맨 아래에 용어 절이 있다 | 통과 |

#### 반드시 규칙 확인 (docs/standards/architecture-rules.md)

| # | 규칙 | 이 plan에서 지키는 방법, 또는 해당 없음과 그 이유 | 결과 |
|---|------|-----------------------------------------------|------|
| 1 | STD-001 | 서비스마다 자기 Oracle 계정과 컨테이너를 쓴다. 주문 서비스는 재고 API만 부른다. 테스트도 서비스마다 자기 컨테이너를 쓴다 | 통과 |
| 2 | STD-002 | 해당 없음. Pod 구성은 배포 매니페스트의 일이고 티켓 003이다. 이 기능은 서비스마다 앱 하나만 만든다 | 해당 없음 |
| 3 | STD-015 | `libs/archunit-rules`에 ArchUnit 규칙(기술 코드)만 둔다 | 통과 |
| 4 | STD-003 | 재고 서비스 주소 기본값은 `http://inventory:8080`이고 환경변수로 덮어쓴다. 디스커버리 서버를 두지 않는다 | 통과 |
| 5 | STD-004 | `InventoryClientProperties`의 연결·요청 제한 시간 2.5초. 시간이 지나면 시도는 실패로 끝난다. 값은 설정에 있다 | 통과 |
| 6 | STD-005 | 예약은 `PUT /reservations/{orderNo}`, 해제는 `DELETE /reservations/{orderNo}`이고 주문 번호가 멱등 키다. 주문 서비스는 재시도할 때 같은 주문 번호를 보낸다 | 통과 |
| 7 | STD-010 | 새 API다. 응답 상태 값 목록을 처음부터 모두 정했다. 받는 쪽은 모르는 필드를 무시하도록 역직렬화 설정을 둔다 `[제안]` | 통과 |
| 8 | STD-014 | P1 단계 PR에서 루트 `contracts/inventory-api.yaml`을 만든다. P2·P3에서 재고 API를 바꾸지 않는다 | 통과 |
| 9 | STD-016 | P3의 Resilience4j Retry가 연결 실패·시도 제한 시간 초과·502·503·504에만 재시도한다. 업무 결과와 409·500·4xx에는 재시도하지 않는다. 요청마다 30초 한도를 둔다. P2에는 재시도가 없다 | 통과 |
| 10 | STD-018 | `OrderService.place()`에는 트랜잭션이 없고, 저장은 `OrderTxService`의 메서드마다 따로 커밋한다. ArchUnit이 `@Transactional`에서 `client` 직접 호출을 막는다 | 통과 |
| 11 | STD-019 | 재고 호출(예약, 해제)에 서킷 브레이커 하나를 둔다(P2). 열려 있으면 호출하지 않고 바로 실패하며, `CallNotPermittedException`은 재시도하지 않는다(P3) | 통과 |
| 12 | STD-006 | 남아야 하는 데이터는 DB에 있다. 세션을 쓰지 않는다(JWT 무상태) | 통과 |
| 13 | STD-007 | 앱 쪽: Actuator health의 readiness·liveness 그룹을 켜고, liveness에는 외부 상태를 넣지 않는다. Deployment에 프로브를 설정하는 일은 티켓 003이다 | 통과 (앱 쪽), 해당 없음 (Deployment) |
| 14 | STD-008 | 앱 쪽: `server.shutdown=graceful`, `spring.lifecycle.timeout-per-shutdown-phase=35s`를 설정에 적는다. `terminationGracePeriodSeconds`(45초)는 티켓 003이다 | 통과 (앱 쪽), 해당 없음 (Pod) |
| 15 | STD-009 | 환경마다 다른 값(DB 주소·계정·비밀번호, 재고 주소, OTLP 주소)은 환경변수로 받는다. 저장소에 비밀번호를 넣지 않는다. 이미지와 ConfigMap·Secret은 티켓 003이다 | 통과 (앱 쪽), 해당 없음 (이미지) |
| 16 | STD-011 | 해당 없음. replicas는 배포 매니페스트의 일이고 티켓 003이다 | 해당 없음 |
| 17 | STD-012 | `spring-boot-starter-opentelemetry`로 추적을 켜고, `RestClient.Builder`로 추적 정보를 넘긴다. 로그 줄마다 trace ID가 남는다 | 통과 |
| 18 | STD-013 | 로그는 표준 출력(ECS JSON)과 OTLP(appender)로만 낸다. 파일 appender를 두지 않는다 | 통과 |
| 19 | STD-017 | 주문 서비스는 OAuth2 리소스 서버로 JWT의 서명·만료·발급자를 검증한다. 고객 ID는 `sub`에서만 얻는다 | 통과 |

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

**1차 확인 결과**: 통과. 원칙 8개 모두 통과했다. 반드시 규칙 19개 가운데 실패는 0개다. 해당 없음은 #2와 #16 두 줄이고, #13, #14, #15 세 줄은 일부가 해당 없음이다. 해당 없는 부분은 모두 배포 매니페스트라서 티켓 003에서 확인한다.

### 2차 확인 (Phase 1 뒤)

Phase 1 산출물(data-model.md, contracts/, quickstart.md)과 research.md의 결정을 기준으로 다시 확인했다. 아키텍처 규칙은 `docs/standards/architecture-rules.md`의 절과 규칙 이름으로 가리킨다.

| 원칙 | Phase 1 설계로 다시 확인한 내용 | 결과 |
|------|------------------------------|------|
| 서비스 자율성 | data-model.md 1절에서 두 서비스의 테이블이 겹치지 않는다. `RESERVATION_ITEMS`는 `STOCK`으로 외래 키를 두지 않고, `ORDER_ITEMS`도 재고 테이블을 가리키지 않는다(1절 "서비스 경계"의 "서비스마다 자기 DB" 규칙과 "`libs/`에는 기술 코드만" 규칙) | 통과 |
| 계약 우선 | `contracts/inventory-api.yaml`의 상태 값(RESERVED, REJECTED, RELEASED)과 사유 값이 data-model.md의 CHECK 제약과 같다. 오류 `code`는 문자열이라 값을 더해도 깨지지 않는다. 응답 스키마에 `additionalProperties: false`를 두지 않아 필드를 더할 수 있다(2절 "서비스 사이 호출"의 "추가하는 방식으로만 바꾼다" 규칙과 "같은 PR에서 `contracts/` 현재본도 고친다" 규칙) | 통과 |
| 실패를 전제한 호출 | data-model.md 6-1절의 처리 순서에서 원격 호출(3번, 6번)이 트랜잭션 1·2 밖에 있다. 예약 요청을 보낸 적이 있는지는 호출 문맥이 세고, 서킷 상태를 미리 묻지 않는다. 6-2절의 재고 행 잠금 순서는 상품 ID 순이다(2절 "서비스 사이 호출"의 Service 이름 호출, 제한 시간, 멱등, 재시도, 트랜잭션 밖 원격 호출, 서킷 브레이커 규칙) | 통과 |
| 언제 사라져도 되는 Pod | 요청 키와 예약 기록이 DB 고유 제약으로 지켜진다. 그래서 Pod가 바뀌어도 중복 주문이나 중복 예약이 생기지 않는다(3절 "실행과 배포"의 "남아야 하는 데이터는 외부 저장소에" 규칙) | 통과 |
| 관측 가능성 | quickstart.md와 research.md 4절의 로그·OTLP 설정에 변동이 없다(4절 "관측성"의 추적 전달 규칙과 로그 규칙) | 통과 |
| 서비스가 직접 확인하는 신원과 코드 밖의 비밀 | `contracts/order-api.yaml`이 `bearerAuth`를 요구하고 401을 Problem Details로 정의한다. 요청 본문에 고객 ID 필드가 없다(5절 "보안"의 JWT 직접 검증 규칙) | 통과 |
| 실제 조건에서 테스트 먼저 | 아래 "인수 시나리오와 테스트 층" 표에서 모든 테스트 케이스가 테스트 층과 단계에 배정됐다. 재고 쪽 인수 시나리오 테스트는 모두 P1에 두어 재고 구현보다 먼저 쓴다(2026-10-06 사용자 승인). 기대값을 바꾼 곳이 없다. 공통 전제는 시간을 같은 비율로 줄여 돌리는 것을 허용한다. 1/5라는 비율은 research.md 4절의 `[제안]`이다 | 통과 |
| 검수할 수 있는 산출물 | plan을 만들 때 쓴 검사 스크립트와 그 결과는 이 문서에 남아 있지 않다. 2026-10-06에 다시 검사했다. 명령은 파일마다 `grep -nE '(STD\|OQ\|BR\|NFR\|TC\|UC)-[0-9]{3}\|ADR-[0-9]{4}\|P§' <파일> \| grep -vE '^[0-9]+:\s*>'`(인용 블록 밖에서 항목 ID를 쓴 줄 찾기)이다. research.md, data-model.md, quickstart.md, `contracts/`의 두 파일은 0줄이다. plan.md는 Constitution Check의 아키텍처 규칙 표와 "인수 시나리오와 테스트 층" 표의 줄만 나오고, 두 표 모두 바로 아래 "이 표에 나온 항목"에서 펼쳤다 | 통과 |

**2차 확인 결과**: 통과. 1차와 결과가 같다. 설계 때문에 새로 생긴 위반은 없다.

## Project Structure

### Documentation (this feature)

```text
specs/001-place-order/
├── spec.md              # /speckit-specify 산출물
├── plan.md              # 이 파일
├── research.md          # Phase 0: 결정, 버전, 확인하지 못한 것
├── data-model.md        # Phase 1: 테이블, 상태 전이, 처리 순서
├── quickstart.md        # Phase 1: 검증 방법
├── contracts/
│   ├── inventory-api.yaml   # 재고 내부 API (P1에서 루트 contracts/에도 같은 내용)
│   └── order-api.yaml       # 주문 외부 API (주문 생성 부분)
├── checklists/
│   └── requirements.md  # spec 품질 체크리스트
└── tasks.md             # /speckit-tasks가 만든다 (이 명령은 만들지 않는다)
```

### Source Code (repository root)

```text
msa-example/
├── settings.gradle                      # includeBuild('build-logic'), include 'services:inventory-service', 'services:order-service', 'libs:archunit-rules'
├── gradle.properties                    # msa.stage=P1 (P2·P3 PR에서 올린다)
├── gradlew, gradlew.bat, gradle/wrapper/ # Gradle 9.8.0
├── gradle/libs.versions.toml            # 버전 카탈로그
├── build-logic/                         # 포함 빌드 (P1)
│   ├── settings.gradle
│   ├── build.gradle                     # groovy-gradle-plugin
│   └── src/main/groovy/
│       ├── msa.java-service.gradle      # Java 17, Spring Boot BOM, Spotless, SpotBugs, 기계 검사 작업 연결
│       └── msa/quality/                 # 검사 작업: Lombok, 검사를 끄는 표시, Mapper XML, 19c 키워드, 테스트 케이스 ID
├── config/
│   ├── quality/
│   │   ├── suppression-allowlist.csv    # path,marker,reason,approved_pr
│   │   └── test-case-stages.csv         # tc_id,service,stage
│   └── spotbugs/exclude.xml             # EI_EXPOSE_REP, EI_EXPOSE_REP2
├── libs/
│   └── archunit-rules/                  # 계층·애너테이션 SQL·트랜잭션 안 원격 호출 규칙 (P1)
│       └── src/main/java/com/example/msa/archrules/
├── contracts/
│   └── inventory-api.yaml               # 재고 API 현재본 (P1)
├── .github/workflows/build.yml          # CI (P1)
└── services/
    ├── inventory-service/               # P1
    │   ├── CLAUDE.md                    # 이 서비스의 패키지·DB 규칙
    │   ├── build.gradle                 # plugins { id 'msa.java-service' }
    │   └── src/
    │       ├── main/java/com/example/msa/inventory/
    │       │   ├── InventoryServiceApplication.java
    │       │   ├── controller/          # ReservationController
    │       │   ├── service/             # ReservationService (흐름, 고유 제약 충돌 시 다시 시도), ReservationTxService (트랜잭션)
    │       │   ├── mapper/              # StockMapper, ReservationMapper
    │       │   ├── domain/              # ReservationStatus, ReservationReason, ReservationDecision
    │       │   ├── dto/                 # ReserveRequest, ReservationResponse, ReleaseResponse [제안], StockRow, ReservationWithItems
    │       │   ├── exception/           # Problem Details 변환
    │       │   └── config/              # MyBatis, 관측 설정(ObservabilityConfig [제안])
    │       ├── main/resources/
    │       │   ├── application.yml
    │       │   ├── logback-spring.xml
    │       │   ├── mapper/StockMapper.xml, mapper/ReservationMapper.xml
    │       │   └── db/migration/V1__inventory.sql, V2__seed_stock.sql
    │       └── test/java/com/example/msa/inventory/
    │           ├── OracleTestContainer.java  # 테스트용 Oracle 컨테이너 [제안]
    │           ├── domain/              # 단위 테스트
    │           ├── mapper/              # Mapper 테스트 (Testcontainers Oracle)
    │           ├── integration/         # 서비스 통합 테스트 (인수 시나리오)
    │           ├── contract/            # 제공자 계약 테스트, ProviderContractValidator [제안]
    │           └── architecture/        # ArchUnit
    └── order-service/                   # P2
        ├── CLAUDE.md
        ├── build.gradle
        └── src/
            ├── main/java/com/example/msa/order/
            │   ├── OrderServiceApplication.java
            │   ├── controller/          # OrderController
            │   ├── service/             # OrderService (트랜잭션 없음), OrderTxService, ReservationReleaser
            │   ├── mapper/              # OrderMapper
            │   ├── client/              # InventoryClient, InventoryCallContext (시도 수, 시작 시각), RetrySchedule [제안]
            │   ├── domain/              # OrderStatus, OrderReason, ReleaseStatus, OrderItems
            │   ├── dto/                 # PlaceOrderRequest, OrderResponse, OrderWithItems
            │   ├── exception/           # Problem Details 변환, 401 처리(ProblemAuthenticationEntryPoint [제안])
            │   └── config/              # SecurityConfig, InventoryClientConfig (RestClient, Resilience4j), ReleaseExecutorConfig, ObservabilityConfig [제안]
            ├── main/resources/
            │   ├── application.yml
            │   ├── logback-spring.xml
            │   ├── mapper/OrderMapper.xml
            │   └── db/migration/V1__orders.sql
            └── test/java/com/example/msa/order/
                ├── OracleTestContainer.java  # 테스트용 Oracle 컨테이너 [제안]
                ├── domain/              # 단위 테스트
                ├── mapper/              # Mapper 테스트
                ├── integration/         # 서비스 통합 테스트, InventoryWireMock [제안]
                ├── contract/            # 소비자 계약 테스트, ConsumerContractValidator [제안]
                ├── config/              # 설정 기본값 단위 테스트 [제안]
                ├── client/              # 대기 일정·30초 한도 판단 단위 테스트 [제안]
                └── architecture/        # ArchUnit
```

**Structure Decision**: 패키지 구조는 코딩 규약 2절을 따르고, 저장소 구조는 저장소 루트 spec-kit 결정 ADR의 트리를 따른다. 그 위에 더한 것은 세 가지다.

- `build-logic/`, `config/`, `libs/archunit-rules`: 기계 검사를 둘 곳이다(research.md 결정 11).
- `.github/workflows/`: CI다(research.md 결정 5).

트리의 `[제안]` 표시는 tasks.md가 고른 파일·패키지 이름이다. 이 경로를 트리에 더한 것은 2026-10-07 사용자 결정이다.

재고 서비스에는 `client/` 패키지가 없다. 다른 서비스를 부르지 않기 때문이다. 루트 `build.gradle`은 두지 않는다. 공통 설정은 convention plugin에 있다 `[제안]`.

## PR 단계

tasks.md의 단계(Phase)는 이 경계를 따른다. 단계마다 브랜치 하나, PR 하나다.

| 단계 | 브랜치 | 담는 것 | 끝났다고 보는 기준 |
|---|---|---|---|
| 0-A | main에서 만든 docs 브랜치 `docs/order-test-cases` `[제안]` | 테스트 케이스 문서에 케이스 아홉 개 추가(research.md 결정 2)와 공통 전제 두 줄 추가(성공 응답 코드, 401의 오류 형식). 사용자가 초안을 승인한 뒤 만든다 | 사용자가 PR을 병합 |
| 0-B | main에서 만든 docs 브랜치 `docs/align-001-decisions` `[제안]` | 기준 문서 네 개(`docs/standards/tech-stack.md`, `docs/standards/testing.md`, `docs/standards/coding-conventions.md`, `docs/design/architecture.md`)를 plan의 결정에 맞춘다(research.md 5절, 2026-10-06 사용자 결정). 저장소 루트 `CLAUDE.md` 7절에 받치는 테스트의 기대값 출처 예외를 더한다(2026-10-06 사용자 결정). 사용자가 고칠 글을 승인한 뒤 만든다 | 사용자가 PR을 병합 |
| P1 | `feat/001-place-order-p1` | 빌드 골격(Gradle wrapper, `settings.gradle`, `gradle.properties`, 버전 카탈로그, `build-logic`). 기계 검사와 `config/`, `libs/archunit-rules`. CI. 재고 서비스 전체(예약·해제 API, Flyway 스키마와 초기 데이터, 로그·추적·프로브·그레이스풀 셧다운 설정, `CLAUDE.md`). 재고 쪽 인수 시나리오 테스트 전부(P2·P3 스토리의 재고 쪽 포함). 루트 `contracts/inventory-api.yaml` | `.\gradlew.bat build` 종료 코드 0(`msa.stage=P1`), 재고 쪽 인수 시나리오 테스트 통과, GitHub Actions 성공 |
| P2 | `feat/001-place-order-p2` | 주문 서비스 전체(주문 API, JWT 검증, 항목·요청 키 검사, 주문 저장, `InventoryClient`의 시도당 제한 시간과 서킷 브레이커, 해제 실행기, 추적 전달, 로그·프로브·그레이스풀 셧다운 설정, `CLAUDE.md`). `msa.stage=P2` | `.\gradlew.bat build` 종료 코드 0, P1의 테스트도 그대로 통과, GitHub Actions 성공 |
| P3 | `feat/001-place-order-p3` | Resilience4j Retry(즉시·1·2·4·8초, 일시 오류만, 서킷 안쪽), 요청마다 30초 한도, 해제의 같은 재시도 정책. `msa.stage=P3` | `.\gradlew.bat build` 종료 코드 0, P1·P2의 테스트도 그대로 통과, GitHub Actions 성공 |

0-A는 PR #17, 0-B는 PR #18로 2026-10-07에 병합했다. spec·plan·tasks 문서는 그보다 먼저 PR #16(브랜치 `feat/001-place-order`)으로 병합했다. 그래서 P1 단계 PR은 새 브랜치 `feat/001-place-order-p1`에서 만들고, 문서 없이 구현만 담는다.

0-A와 0-B는 모두 P1 단계 PR보다 먼저 병합한다. 0-A를 먼저 병합하는 것은 research.md 4절 "테스트 케이스 추가 docs PR의 순서"의 `[제안]`이다. 0-B를 먼저 병합하는 것은 2026-10-06 사용자 승인이다. 헌법 "기술 제약" 절이 Technical Context에 기술 스택 문서의 값을 쓰라고 하는데, 이 plan의 MyBatis 스타터 4.1.0과 JUnit Jupiter 6은 0-B 전의 기술 스택 문서(4.0.x, JUnit 5)와 다르기 때문이다. Phase 0을 PR 두 개로 나누는 것은 tasks.md의 `[제안]`이다.

P2와 P3는 재고 서비스를 고치지 않는다. 재고 쪽 인수 시나리오 테스트는 재고 구현과 함께 모두 P1에 있다. 테스트 작업을 구현 작업보다 앞에 두려면 같은 PR에 있어야 하기 때문이다(2026-10-06 사용자 승인).

단계마다 쓰는 테스트 시간은 모두 1/5로 줄인 설정이다(research.md 4절) `[제안]`. 줄이지 않은 기본값이 설정에 맞게 들어갔는지는 단위 테스트로 확인한다.

## 인수 시나리오와 테스트 층

각 테스트 케이스를 어느 서비스의 어느 테스트 층에서, 어느 단계에 검증하는지 적는다. 이 표의 "서비스"와 "단계" 칸이 `config/quality/test-case-stages.csv`의 내용이 된다. 인수 시나리오 테스트의 `@DisplayName`은 테스트 케이스 ID로 시작한다. 단위·Mapper·계약 테스트는 인수 시나리오 테스트를 받치는 테스트라서 ID를 붙이지 않는다 `[제안]`.

| 테스트 케이스 | spec 시나리오 | 서비스 | 단계 | 단위 | Mapper | 서비스 통합 (인수) | 계약 |
|---|---|---|---|---|---|---|---|
| TC-003 | 1.1 | 재고 | P1 | 예약 판단(같은 내용, 다른 내용) | 예약 기록 삽입, 고유 제약 충돌 | 같은 요청 다시 보냄, 동시 두 건, 거절 뒤 재요청 | 제공자: 200 응답 |
| TC-005 | 1.2 | 재고 | P1 | — | 재고 행 잠금과 감소 | 서로 다른 주문 번호 20건 동시 | — |
| TC-007 | 1.3 | 재고 | P1 | — | 해제 표식 삽입 | 해제 먼저, 예약 나중, 해제 두 번 | 제공자: `DELETE` 200 |
| TC-008 | 1.4 | 재고 | P1 | — | — | 409, 재고 그대로 | 제공자: 409 Problem Details |
| TC-012 | 1.5 | 재고 | P1 | — | — | 두 순서 50건씩 동시, 교착 없음 | — |
| TC-004 | 9.1의 재고 쪽 | 재고 | P1 | — | 수량 복구 | 예약 → 해제 → 재고 10개 (사용자 결정, research.md 결정 3) | — |
| TC-001 | 2.1의 재고 쪽 | 재고 | P1 | — | — | 같은 예약 요청 → 7개, 3개 | — |
| TC-002 | 3.1의 재고 쪽 | 재고 | P1 | — | — | 같은 예약 요청 → 10개, 1개 그대로 | — |
| TC-015 | 3.3의 재고 쪽 | 재고 | P1 | 사유 우선순위(상품 없음이 먼저) | — | 같은 예약 요청 → A 10개, 변형의 A·B 그대로 | 제공자 |
| TC-011 | 6.1의 재고 쪽 | 재고 | P1 | — | — | 서로 다른 주문 번호의 예약 두 번 → 7개, 4개 | — |
| TC-006 | 8.1의 재고 쪽 | 재고 | P1 | — | — | 같은 예약 요청 → 7개 | — |
| TC-001 | 2.1 | 주문 | P2 | — | 주문·항목 저장, `<collection>` 조회 | RESERVED 스텁 → 확정, C1의 두 항목 주문 저장 | 소비자: 요청과 스텁 응답 |
| TC-002 | 3.1 | 주문 | P2 | — | 항목별 거절 사유 저장 | 거절, 부족한 B, "거절"로 저장. "C1이 조회할 수 있다"는 티켓 002 | 소비자 |
| TC-009 | 3.2 | 주문 | P2 | — | — | 예약 요청 1번 | — |
| TC-015 | 3.3 | 주문 | P2 | — | — | 상품 없음, Z, 요청 1번, 변형 | 소비자 |
| TC-010 | 4.1 | 주문 | P2 | — | — | 토큰 없음 → 401, 주문 기록 없음. 나머지 조회 부분은 티켓 002 | — |
| TC-014 | 5.1 | 주문 | P2 | 주문 항목 검사 다섯 경우 | — | 400 다섯 번, 기록 없음, 재고 요청 없음 | — |
| TC-011 | 6.1 | 주문 | P2 | — | 고객·키 고유 제약 | When 일곱 개 모두 | — |
| TC-013 | 7.1 | 주문 | P2 | — | — | 503 계속 → 서킷 열림, 1초 안 실패, 해제 불필요, 해제 실패, 회복 뒤 확정 | 소비자 |
| TC-008 | 7.2 | 주문 | P2 | — | — | 409 스텁 → 재시도 없음, 실패, 해제 요청 | 소비자: 409 스텁 |
| TC-006 | 8.1 | 주문 | P3 | 대기 일정(0·1·2·4·8초) | — | 503·503·200 → 확정, 요청 3번과 간격 | 소비자 |
| TC-004 | 9.1의 주문 쪽 | 주문 | P3 | 30초 한도 판단 | — | 3초 지연 → 요청 6번, 31초 안 응답, 실패, 해제 완료, health 1초 안 | 소비자 |
| TC-002 | 3.1의 조회 부분 | 주문 | T002 | — | — | 티켓 002에서 검증 ("C1이 조회할 수 있다") | — |
| TC-010 | 4.1의 조회 부분 | 주문 | T002 | — | — | 티켓 002에서 검증 (다른 고객의 주문, 자기 주문 목록, 관리자 조회) | — |

재고 쪽 줄의 단계를 모두 P1으로 둔 것은 2026-10-06 사용자 승인이다. 단계 `T002` 줄은 research.md 결정 11(사용자 결정)의 "티켓 002로 넘긴 부분은 단계를 `T002`로 적어 검사하지 않는다"를 따른다.

**이 표에 나온 항목**

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

테스트 케이스 추가 PR(research.md 결정 2)의 아홉 케이스는 아래처럼 배정한다. ㉠·㉡·㉢·㉣의 서비스와 단계는 research.md 결정 2의 `[제안]` 초안에 적힌 것이다 `[제안]`. ㉥, ㉦, ㉧, ㉨은 2026-10-06, ㉩는 2026-10-07 사용자 결정으로 더했다. 번호는 그 PR에서 정하므로 이 표에는 이름으로 적는다.

| 추가할 케이스 | 서비스 | 단계 | 테스트 층 |
|---|---|---|---|
| ㉠ 거절된 예약에 해제 요청이 와도 재고는 그대로다 | 재고 | P1 | 서비스 통합, 제공자 계약 |
| ㉡ 해제한 예약의 기록은 남아, 같은 예약 요청이 다시 와도 재고가 줄지 않는다 | 재고 | P1 | 서비스 통합 |
| ㉥ 예약된 재고에 해제가 두 번 와도 수량은 한 번만 돌아온다 | 재고 | P1 | 서비스 통합 |
| ㉦ 서명이 틀리거나 만료된 토큰으로는 주문할 수 없다 | 주문 | P2 | 서비스 통합 |
| ㉧ 형식이 틀린 주문 항목은 기록되지 않는다 | 주문 | P2 | 서비스 통합 |
| ㉨ 거부된 요청의 키는 남지 않고, 같은 키의 내용 비교는 항목 순서를 보지 않는다 | 주문 | P2 | 서비스 통합 |
| ㉩ 주문 서비스는 재고 서비스 호출에 추적 정보를 넘긴다 | 주문 | P2 | 서비스 통합 |
| ㉢ 재시도하던 중에 서킷이 열리면 남은 재시도를 하지 않는다 | 주문 | P3 | 서비스 통합 |
| ㉣ 해제 요청도 일시 오류면 같은 정책으로 재시도한다 | 주문 | P3 | 서비스 통합 |

테스트 케이스에 매이지 않는 받치는 테스트로, 주문 상태 전이의 단위 테스트를 P2에 둔다. 기대값은 도메인 분석 4절 "주문 상태"의 그림이다. 받치는 테스트는 판정 기준 문서에 더해 사용자가 확정한 `docs/design/`·`docs/analysis/` 문서에서도 기대값을 가져온다(2026-10-06 사용자 결정. 저장소 루트 `CLAUDE.md` 7절의 예외는 0-B에서 더한다).

모든 단계에서 함께 도는 아키텍처 테스트는 계층 의존, 애너테이션 SQL 금지, `@Transactional`에서 `client` 직접 호출 금지(ArchUnit)다. Gradle 기계 검사 작업도 모든 단계에서 함께 돈다. 운영 테스트(`docs/test-cases/operations.md`)와 E2E는 티켓 003이다.

## Complexity Tracking

> **권장 규칙을 어길 때만 채운다.** "반드시" 규칙 위반은 여기에 적어 넘길 수 없다.
> 바꾸려면 ADR을 쓰고 헌법을 개정한다 (.specify/memory/constitution.md의 "거버넌스" 절).

없음. 아키텍처 규칙 문서의 규칙은 모두 "반드시" 수준이고, 이 plan은 그 규칙을 어기지 않는다.

## 용어

- **멀티 프로젝트 빌드 (multi-project build)**: 루트 `settings.gradle` 하나로 여러 하위 프로젝트를 묶어 빌드하는 Gradle 구성이다. 서비스 두 개와 `libs/archunit-rules`가 하위 프로젝트다.
- **포함 빌드 (included build)**: `includeBuild`로 묶는, 따로 빌드되는 Gradle 프로젝트다. `build-logic/`이 포함 빌드이고, 공통 빌드 설정 플러그인을 담는다.
- **convention plugin**: 여러 하위 프로젝트에 같은 빌드 설정을 적용하려고 만든 Gradle 플러그인이다. `msa.java-service`가 두 서비스에 Java 버전, Spotless, SpotBugs, 기계 검사를 적용한다.
- **버전 카탈로그 (version catalog)**: 의존성 버전을 `gradle/libs.versions.toml` 한 파일에 모아 두는 Gradle 기능이다.
- **기계 검사 (automated rule check)**: 글로 적은 코딩 규칙을 빌드할 때 프로그램이 검사하게 만든 것이다. 규칙 목록은 코딩 규약 3-8절에 있다.
- **허용 목록 (allowlist)**: 규칙의 예외로 승인한 것만 적어 둔 파일이다. 검사를 끄는 표시와 `${}` 예외를 `config/quality/suppression-allowlist.csv`에 적고, 사용자가 PR에서 승인한다.
- **테스트 케이스 단계 표 (test-case stage table)**: 테스트 케이스마다 어느 서비스가 어느 단계에서 테스트를 갖춰야 하는지 적은 표다. 테스트 케이스 ID 검사가 이 표와 `msa.stage`로 검사 범위를 정한다.
- **Constitution Check**: plan이 헌법을 지키는지 확인하는 절이다. Phase 0 전에 한 번, Phase 1 뒤에 다시 확인한다.
- **Phase 0, Phase 1**: `/speckit-plan`의 단계다. Phase 0은 정하지 않은 기술 문제를 조사해 research.md에 적고, Phase 1은 data-model, contracts, quickstart를 만든다.
- **트랜잭션 전용 빈 (TxService)**: `@Transactional` 메서드만 모아 둔 별도 Spring 빈이다. 원격 호출과 저장을 나누고, 같은 클래스 안의 호출이 프록시를 거치지 않는 문제를 피한다.
- **서킷 브레이커 (circuit breaker)**: 상대 서비스 호출이 계속 실패하면 한동안 호출을 멈추고 바로 실패를 돌려주는 장치다. 재고 호출에 Resilience4j로 하나 둔다.
- **재시도 (retry)**: 실패한 호출을 잠시 기다렸다가 다시 보내는 것이다. P3에서 Resilience4j Retry로 일시 오류에만, 최대 5번, 전체 30초 안에서 한다.
- **호출 문맥 (call context)**: 재고 호출 한 번(재시도 포함)마다 만드는 작은 객체다. 시작 시각과 실제로 보낸 시도 수를 들고 다니며 30초 한도와 해제 필요 여부를 판단한다.
- **해제 실행기 (release executor)**: 실패한 주문의 예약 해제를 고객 응답과 따로 돌리는 스레드 풀이다. 종료할 때는 실행 중인 해제를 기다린다.
- **제공자 / 소비자 (provider / consumer)**: 계약 테스트에서 API를 내주는 쪽과 부르는 쪽이다. 재고 서비스가 제공자, 주문 서비스가 소비자다.
- **WireMock 스텁 (WireMock stub)**: WireMock에 "이 요청이 오면 이렇게 응답하라"고 미리 정해 둔 규칙이다. 주문 서비스 테스트에서 재고 서비스의 응답과 지연, 오류를 만든다.
- **ECS (Elastic Common Schema)**: 로그 JSON의 필드 이름을 정한 규칙이다. Spring Boot의 구조화 로그가 이 형식을 지원한다.
- **MDC (Mapped Diagnostic Context)**: 로그 라이브러리가 스레드마다 들고 다니는 키-값 저장소다. 추적 기능이 trace ID를 여기에 넣으면 로그 줄마다 찍힌다.
- **Problem Details**: HTTP API의 오류 응답을 JSON으로 적는 표준 형식(RFC 9457)이다. 모든 오류 응답이 이 형식이다.
- **멱등 키 (idempotency key)**: 받는 쪽이 같은 요청인지 알아보는 데 쓰는 값이다. 재고 예약은 주문 번호, 주문 생성은 고객과 주문 요청 키의 조합이다.
- **단계 (stage)**: 이 기능의 PR 경계인 P1, P2, P3이다. `gradle.properties`의 `msa.stage`에 지금 단계를 적는다.
