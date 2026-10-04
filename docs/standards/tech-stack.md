# 기술 스택

- 상태: **확정 (2026-10-04)**
- 쓰는 곳: `/speckit-constitution`, `/speckit-plan`의 Technical Context

| 항목 | 선택 | 근거 |
|---|---|---|
| 언어 | Java 17 (개발 PC의 JDK 17.0.2를 그대로 쓴다) | 사용자 결정 |
| 프레임워크 | Spring Boot **4.1.1** (Spring Framework 7.0, Spring Security 7.1) | 2026-10-04 기준 Java 17로 쓸 수 있는 최신 정식 버전. 4.x의 최소 Java 버전은 17이다 `[문헌]`. 4.2.0은 아직 마일스톤이라 쓰지 않는다 |
| 빌드 | Gradle wrapper, Groovy DSL, 루트 `settings.gradle`로 묶은 멀티 프로젝트 | ADR-0001 |
| DB | Oracle Database **21c XE**, 서비스마다 컨테이너 하나 | 사용자는 19 버전을 원했지만 19c에는 XE가 없다 `[문헌]`. 가장 가까운 XE인 21c를 쓰고, SQL은 19c에서도 돌아가는 것만 쓴다. 배치는 [ADR-0005](../adr/0005-oracle-xe-instance-per-service.md) |
| DB 형상 관리 | Flyway (Oracle용 모듈 포함) | 스키마와 초기 데이터를 코드로 관리한다 |
| 영속성 | **MyBatis** (mybatis-spring-boot-starter 4.0.x, Spring Boot 4용) | [coding-conventions.md](coding-conventions.md) A안. 4.0 계열이 Spring Boot 4와 Java 17 이상을 지원한다 `[문헌]`. 정확한 패치 버전은 plan에서 정한다 |
| 인증 | Keycloak(OIDC) + Spring Security OAuth2 Resource Server(JWT) | [ADR-0006](../adr/0006-keycloak-jwt-auth.md) |
| Gateway | Envoy Gateway (Gateway API 구현체) | [ADR-0007](../adr/0007-envoy-gateway.md) |
| 서비스 간 HTTP | Spring `RestClient` 또는 HTTP Interface(`@HttpExchange`) | plan에서 하나로 정한다 |
| 재시도 | Spring Framework 7 내장 재시도(`@Retryable` / `RetryTemplate`) | 별도 라이브러리 없이 지수 백오프를 쓸 수 있다 `[문헌]`. 서킷 브레이커와 순서를 맞추기 쉽도록 같은 라이브러리(Resilience4j)의 재시도를 쓰는 안과 plan에서 비교한다 |
| 서킷 브레이커 | Resilience4j (Spring Cloud Circuit Breaker 경유 또는 직접) | Spring Framework 7에는 서킷 브레이커가 없다. Spring Boot 4.1과 호환되는 버전은 plan에서 확인한다 (OQ-009, STD-019) |
| 추적·로그·메트릭 | OpenTelemetry (Spring Boot 4의 OpenTelemetry 지원 + OpenTelemetry Logback appender), 저장소와 화면은 `grafana/otel-lgtm` | STD-012, STD-013, design 10절 |
| 이미지 빌드 | Gradle bootJar + 공용 Dockerfile, 로컬 레지스트리(`localhost:5001`) | design 11절 |
| 로컬 클러스터 | kind, control-plane 1 + worker 2 | design 11절 |
| 테스트 | JUnit 5, AssertJ, Testcontainers(Oracle XE), WireMock, Spring Security Test, ArchUnit, mybatis-spring-boot-starter-test | [testing.md](testing.md) |
| 로컬 실행 | Docker Desktop, kind, kubectl, Helm | ADR-0003, [references/docker-desktop.md](../references/docker-desktop.md) |
| 형상 관리 | Git, GitHub Flow | [git-workflow.md](git-workflow.md) |

## Java 17이라서 생기는 제약

- **가상 스레드를 쓸 수 없다.** 가상 스레드는 Java 21부터다. 재고 호출을 기다리는 동안 요청 스레드가 묶이므로, 재고 서비스가 오래 느리면 주문 서비스의 스레드가 바닥날 수 있다. 서킷 브레이커로 막는다. → [OQ-009](../analysis/domain-analysis.md#9-미결-사항)
- JDK 17.0.2는 2022년 1월 패치다. 실습에는 문제가 없지만, 보안 패치가 필요한 환경으로 옮길 때는 17의 최신 패치로 올린다.

## Oracle 21c XE를 19c처럼 쓰기

- 21c 이후에 나온 SQL 기능(JSON 데이터 타입, SQL 매크로, 23ai의 `BOOLEAN` 타입과 `IF NOT EXISTS` 등)을 쓰지 않는다.
- MyBatis는 SQL을 그대로 보내므로, 19c 호환은 사람이 쓴 SQL에 달려 있다. PR 템플릿의 확인 항목으로 리뷰한다. ([git-workflow.md](git-workflow.md))
- 21c XE의 `COMPATIBLE` 파라미터를 19.0으로 낮춰 만드는 방법도 있지만 Oracle이 지원하는 방법이 아니다. 쓰지 않는다. `[현장]`
- XE에는 자원 제한이 있다(CPU 2개, 메모리 2GB, 사용자 데이터 12GB). 또 논리 환경(VM, 컨테이너, 물리 서버) 하나에 XE 하나만 뜬다. 그래서 서비스마다 컨테이너를 따로 띄운다. `[문헌]`
