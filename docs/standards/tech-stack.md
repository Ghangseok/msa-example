# 기술 스택

- 상태: **확정 (2026-10-04, 2026-10-05 정적 분석 행 추가, 2026-10-05 로컬 실행 인프라 버전 절 추가, 2026-10-07 001 plan의 결정 반영)**
- 쓰는 곳: `/speckit-constitution`, `/speckit-plan`의 Technical Context

| 항목 | 선택 | 근거 |
|---|---|---|
| 언어 | Java 17 (개발 PC의 JDK 17.0.2를 그대로 쓴다) | 사용자 결정 |
| 프레임워크 | Spring Boot **4.1.1** (Spring Framework 7.0, Spring Security 7.1) | 2026-10-04 기준 Java 17로 쓸 수 있는 최신 정식 버전. 4.x의 최소 Java 버전은 17이다 `[문헌]`. 4.2.0은 아직 마일스톤이라 쓰지 않는다 |
| 빌드 | Gradle wrapper, Groovy DSL, 루트 `settings.gradle`로 묶은 멀티 프로젝트 | ADR-0001 |
| DB | Oracle Database **21c XE**, 서비스마다 컨테이너 하나 | 사용자는 19 버전을 원했지만 19c에는 XE가 없다 `[문헌]`. 가장 가까운 XE인 21c를 쓰고, SQL은 19c에서도 돌아가는 것만 쓴다. 배치는 [ADR-0005](../adr/0005-oracle-xe-instance-per-service.md) |
| DB 형상 관리 | Flyway (Oracle용 모듈 포함) | 스키마와 초기 데이터를 코드로 관리한다 |
| 영속성 | **MyBatis** (mybatis-spring-boot-starter **4.1.0**, Spring Boot 4.1용. MyBatis 3.5.19, MyBatis-Spring 4.1.0) | [coding-conventions.md](coding-conventions.md) A안. 4.1.0은 Spring Boot 4.1을 대상으로 나왔고, 4.0.x는 Spring Boot 4.0 대상이다. 2026-10-05 001 plan에서 정했다(`specs/001-place-order/research.md` 결정 9) |
| 인증 | Keycloak(OIDC) + Spring Security OAuth2 Resource Server(JWT) | [ADR-0006](../adr/0006-keycloak-jwt-auth.md) |
| Gateway | Envoy Gateway (Gateway API 구현체) | [ADR-0007](../adr/0007-envoy-gateway.md) |
| 서비스 간 HTTP | Spring `RestClient` | 2026-10-05 001 plan에서 정했다(`specs/001-place-order/research.md` 결정 6). 호출이 예약과 해제 두 개뿐이라 HTTP Interface의 인터페이스와 등록 설정을 더 두지 않는다 |
| 재시도 | Resilience4j Retry **2.4.0** (`resilience4j-retry`) | 서킷 브레이커와 같은 라이브러리라 "재시도(서킷 브레이커(재고 호출))" 순서를 한곳에 적을 수 있다. 재시도를 포함한 전체 30초 한도는 Resilience4j에 없어서 직접 만든다. 2026-10-05 001 plan에서 Spring Framework 7 내장 재시도와 비교해 정했다(`specs/001-place-order/research.md` 결정 7) |
| 서킷 브레이커 | Resilience4j **2.4.0** 핵심 모듈을 직접 쓴다(`resilience4j-circuitbreaker`). Spring Boot 자동 설정 모듈과 Spring Cloud Circuit Breaker는 쓰지 않는다 | Spring Framework 7에는 서킷 브레이커가 없다. 핵심 모듈은 Spring에 의존하지 않아 Spring Boot 버전과 묶이지 않는다. 2026-10-05 001 plan에서 정했다(`specs/001-place-order/research.md` 결정 8) (OQ-009, STD-019) |
| 추적·로그·메트릭 | OpenTelemetry (Spring Boot 4의 OpenTelemetry 지원 + OpenTelemetry Logback appender), 저장소와 화면은 `grafana/otel-lgtm` | STD-012, STD-013, design 10절 |
| 이미지 빌드 | Gradle bootJar + 공용 Dockerfile, 로컬 레지스트리(`localhost:5001`) | design 11절 |
| 로컬 클러스터 | kind, control-plane 1 + worker 2 | design 11절 |
| 테스트 | JUnit Jupiter 6 (Spring Boot 4.1.1이 관리하는 6.0.3), AssertJ, Testcontainers(Oracle XE), WireMock, Spring Security Test, ArchUnit, mybatis-spring-boot-starter-test | [testing.md](testing.md). JUnit 버전은 Spring Boot가 관리하는 값을 쓴다(`specs/001-place-order/research.md` 0절) |
| 정적 분석 | SpotBugs (Gradle 플러그인 **6.5.12**, SpotBugs **4.10.4**) | 2026-10-05 사용자 결정. Error Prone은 2.43.0부터 실행에 JDK 21이 필요해서 이 저장소의 JDK 17에 맞지 않는다 `[문헌]`. 버전과 끌 규칙은 2026-10-05 001 plan에서 정했다(`specs/001-place-order/research.md` 결정 10). 끄는 규칙은 `EI_EXPOSE_REP`, `EI_EXPOSE_REP2` 두 개이고, 이 목록을 바꾸려면 PR에서 사용자가 승인한다. 검사 목록은 [coding-conventions.md](coding-conventions.md) 3-8절 |
| 로컬 실행 | Docker Desktop, kind, kubectl, Helm | ADR-0003, [references/docker-desktop.md](../references/docker-desktop.md) |
| 형상 관리 | Git, GitHub Flow | [git-workflow.md](git-workflow.md) |

## 로컬 실행 인프라 버전

2026-10-05 사용자 결정이다. 값은 "정의한 곳" 칸의 파일에 고정되어 있고, 이 표는 그 파일과 같아야 한다. 띄우고 내리는 방법은 [references/docker-desktop.md](../references/docker-desktop.md) 3-4절과 4-1절에 있다.

| 구성 요소 | 버전 | 정의한 곳 |
|---|---|---|
| Oracle XE 이미지 (로컬 실행과 Testcontainers에서 같은 이미지) | `gvenzl/oracle-xe:21.3.0-slim-faststart` | `infra/compose/compose.yaml` |
| Keycloak | `quay.io/keycloak/keycloak:26.8.0` | `infra/compose/compose.yaml` |
| 관측 도구 | `grafana/otel-lgtm:0.35.0` | `infra/compose/compose.yaml` |
| kind 노드 | `kindest/node:v1.36.4` (digest까지 고정) | `infra/kind/cluster.yaml` |
| Envoy Gateway | Helm 차트 `gateway-helm` v1.9.2 | `tools/infra-up.ps1` |
| 로컬 이미지 레지스트리 | `registry:3.1.2` | `tools/infra-up.ps1` |

## Java 17이라서 생기는 제약

- **가상 스레드를 쓸 수 없다.** 가상 스레드는 Java 21부터다. 재고 호출을 기다리는 동안 요청 스레드가 묶이므로, 재고 서비스가 오래 느리면 주문 서비스의 스레드가 바닥날 수 있다. 서킷 브레이커로 막는다. → [OQ-009](../analysis/domain-analysis.md#9-미결-사항)
- JDK 17.0.2는 2022년 1월 패치다. 실습에는 문제가 없지만, 보안 패치가 필요한 환경으로 옮길 때는 17의 최신 패치로 올린다.

## Oracle 21c XE를 19c처럼 쓰기

- 21c 이후에 나온 SQL 기능(JSON 데이터 타입, SQL 매크로, 23ai의 `BOOLEAN` 타입과 `IF NOT EXISTS` 등)을 쓰지 않는다.
- MyBatis는 SQL을 그대로 보내므로, 19c 호환은 사람이 쓴 SQL에 달려 있다. PR 템플릿의 확인 항목으로 리뷰한다. ([git-workflow.md](git-workflow.md))
- 21c XE의 `COMPATIBLE` 파라미터를 19.0으로 낮춰 만드는 방법도 있지만 Oracle이 지원하는 방법이 아니다. 쓰지 않는다. `[현장]`
- XE에는 자원 제한이 있다(CPU 2개, 메모리 2GB, 사용자 데이터 12GB). 또 논리 환경(VM, 컨테이너, 물리 서버) 하나에 XE 하나만 뜬다. 그래서 서비스마다 컨테이너를 따로 띄운다. `[문헌]`

## 용어

- **Gradle wrapper**: 프로젝트에 정해 둔 Gradle 버전을 내려받아 실행하는 스크립트다. PC에 Gradle을 따로 설치하지 않아도 모두 같은 버전으로 빌드한다.
- **멀티 프로젝트 빌드 (multi-project build)**: 루트 `settings.gradle` 하나로 여러 하위 프로젝트를 묶어 빌드하는 Gradle 구성이다. 서비스마다 하위 프로젝트 하나를 둔다.
- **Groovy DSL**: Gradle 빌드 파일을 Groovy 문법으로 쓰는 방식이다. `build.gradle` 파일이 이 형식이다.
- **마일스톤 (milestone)**: 정식 버전 전에 기능을 미리 써 보라고 내는 시험판이다. 이 프로젝트는 마일스톤 버전을 쓰지 않는다.
- **패치 버전 (patch version)**: 버전 번호의 셋째 자리로, 버그와 보안 문제만 고친 판을 뜻한다. JDK 17.0.2에서는 마지막 2가 패치 버전이다.
- **XE (Express Edition)**: Oracle이 무료로 배포하는 작은 DB 판이다. CPU 2개, 메모리 2GB, 사용자 데이터 12GB까지 쓸 수 있다.
- **Flyway**: DB 스키마와 초기 데이터를 버전 번호가 붙은 SQL 파일로 관리하고 차례로 적용하는 도구다. 테이블과 재고 초기 데이터를 Flyway로 만든다.
- **MyBatis**: SQL을 XML 파일에 직접 쓰고, 그 결과를 Java 객체에 담아 주는 영속성 프레임워크다. 이 프로젝트는 JPA 대신 MyBatis를 쓴다.
- **OIDC (OpenID Connect)**: OAuth 2.0 위에 로그인과 사용자 정보를 더한 표준 인증 규격이다. Keycloak이 이 규격으로 토큰을 발급한다.
- **Keycloak**: 사용자 계정, 로그인, 토큰 발급을 맡는 오픈소스 인증 서버다. 이 프로젝트는 Keycloak을 직접 만들지 않고 가져다 쓰며, 계정은 초기 데이터로 넣는다.
- **OAuth2 리소스 서버 (OAuth2 Resource Server)**: 요청에 담긴 토큰을 검증하고 보호된 API를 내주는 서버의 역할이다. 주문 서비스가 Spring Security로 이 역할을 한다.
- **Gateway API**: 쿠버네티스 클러스터 입구의 라우팅을 정하는 표준 규격이다. Ingress를 잇는 규격이고, Envoy Gateway가 이 규격을 구현한다.
- **Envoy Gateway**: Envoy 프록시를 써서 Gateway API를 구현한 오픈소스 프로젝트다. 이 프로젝트의 클러스터 입구다.
- **RestClient**: Spring Framework 6.1부터 들어 있는 동기 HTTP 클라이언트로, 메서드를 이어 불러 요청을 만든다. 서비스 간 HTTP 호출 후보 가운데 하나이고, plan에서 HTTP Interface와 비교해 고른다.
- **HTTP Interface (`@HttpExchange`)**: Java 인터페이스의 메서드에 애너테이션을 붙이면 Spring이 HTTP 호출 코드를 대신 만들어 주는 방식이다. 서비스 간 HTTP 호출 후보 가운데 하나이고, plan에서 RestClient와 비교해 고른다.
- **`@Retryable` / `RetryTemplate`**: Spring Framework 7에 들어 있는 재시도 기능이다. 애너테이션이나 템플릿 객체로 재시도 횟수와 대기 시간을 정한다.
- **지수 백오프 (exponential backoff)**: 재시도할 때마다 대기 시간을 두 배씩 늘리는 방식이다. 재고 호출은 즉시, 1초, 2초, 4초, 8초 뒤에 다시 시도한다.
- **Resilience4j**: 서킷 브레이커, 재시도, 속도 제한 같은 장애 대응 기능을 모은 Java 라이브러리다. 재고 호출의 서킷 브레이커를 이것으로 만든다.
- **Spring Cloud Circuit Breaker**: 여러 서킷 브레이커 라이브러리를 같은 방식으로 쓰게 감싸 주는 Spring Cloud 모듈이다. Resilience4j를 이 모듈로 감싸 쓸지 직접 쓸지는 plan에서 정한다.
- **서킷 브레이커 (circuit breaker)**: 상대 서비스 호출이 계속 실패하면 한동안 호출을 멈추고 바로 실패를 돌려주는 장치다. 재고 서비스가 멈췄을 때 주문 서비스까지 멈추지 않게 막는다.
- **OpenTelemetry**: 추적, 로그, 메트릭을 모으고 보내는 방법을 정한 오픈소스 표준과 도구 모음이다. 두 서비스가 이것으로 관측 데이터를 보낸다.
- **Logback appender**: 로그를 어디로 내보낼지 정하는 Logback의 출력 장치다. 로그를 OTLP로도 보내려고 OpenTelemetry appender를 `logback-spring.xml`에 따로 설정한다.
- **`grafana/otel-lgtm`**: 로그(Loki), 추적(Tempo), 메트릭(Prometheus), 화면(Grafana)을 컨테이너 하나에 담은 개발용 관측 도구다. compose로 클러스터 밖에 띄워, Pod가 사라져도 기록이 남게 한다.
- **bootJar**: Spring Boot 앱과 필요한 라이브러리를 실행할 수 있는 jar 파일 하나로 묶는 Gradle 작업이다. 이 jar로 서비스 이미지를 만든다.
- **로컬 레지스트리 (local registry)**: 이 PC 안에서 컨테이너 이미지를 올리고 내려받는 저장소다. `localhost:5001`에 두고, kind 노드가 여기서 이미지를 받는다.
- **kind (Kubernetes IN Docker)**: Docker 컨테이너를 노드로 써서 쿠버네티스 클러스터를 만드는 도구다. 이 프로젝트의 로컬 클러스터는 kind로 만든다.
- **Helm**: 쿠버네티스 설정 파일 여러 개를 묶어 설치하고 버전을 관리하는 패키지 도구다. 이 프로젝트에서는 Envoy Gateway를 Helm으로 설치한다.
- **JUnit 5**: Java의 표준 테스트 프레임워크다. 모든 테스트를 이것으로 쓴다.
- **AssertJ**: 테스트에서 `assertThat(값).isEqualTo(기대값)`처럼 읽기 쉽게 값을 비교하게 해 주는 라이브러리다. 모든 테스트의 검증 문에 쓴다.
- **Testcontainers**: 테스트를 실행할 때 Docker 컨테이너를 띄우고 끝나면 지우는 라이브러리다. 실제 Oracle에서 테스트하려고 쓴다.
- **WireMock**: 다른 서비스인 척 정해진 응답이나 지연, 오류를 돌려주는 테스트용 가짜 서버다. 서비스 통합 테스트에서 재고 서비스 대신 쓴다.
- **Spring Security Test**: 테스트에서 로그인한 사용자나 JWT를 흉내 내게 해 주는 Spring 모듈이다. 서비스 통합 테스트에서 Keycloak 없이 고객과 관리자를 흉내 낸다.
- **ArchUnit**: 패키지와 클래스 사이의 의존 규칙을 테스트 코드로 검사하는 라이브러리다. 계층 규칙과 일부 기계 검사를 이것으로 한다.
- **`@MybatisTest`**: mybatis-spring-boot-starter-test가 주는 애너테이션으로, MyBatis Mapper만 띄워 SQL을 시험한다. Mapper 테스트에서 실제 Oracle과 함께 쓴다.
- **SpotBugs**: 컴파일된 클래스 파일을 읽어 흔한 버그 패턴을 찾는 정적 분석 도구다. 매 빌드에서 Gradle 플러그인으로 돌린다.
- **Error Prone**: 컴파일할 때 버그 패턴을 찾는 Google의 정적 분석 도구다. 2.43.0부터 실행에 JDK 21이 필요해서 이 프로젝트에서는 쓰지 않는다.
- **정적 분석 (static analysis)**: 프로그램을 실행하지 않고 소스나 클래스 파일만 읽어 문제를 찾는 검사다. 이 저장소에서는 SpotBugs를 쓴다.
- **가상 스레드 (virtual thread)**: Java 21부터 쓸 수 있는 가벼운 스레드로, 응답을 기다리는 동안 운영체제 스레드를 잡고 있지 않는다. 이 프로젝트는 Java 17이라 쓸 수 없어서, 서킷 브레이커로 스레드가 바닥나는 것을 막는다.
- **`COMPATIBLE` 파라미터 (COMPATIBLE parameter)**: Oracle DB가 어느 버전의 기능까지 쓸지 정하는 설정이다. Oracle이 지원하지 않는 방법이라 19.0으로 낮추지 않는다.
- **SQL 매크로 (SQL macro)**: SQL 조각을 함수처럼 만들어 다른 SQL에 끼워 넣는 Oracle 21c 기능이다. 19c에 없어서 쓰지 않는다.
- **논리 환경 (logical environment)**: 운영체제 하나가 도는 실행 단위로, VM, 컨테이너, 물리 서버가 각각 하나다. Oracle XE는 논리 환경 하나에 하나만 뜨므로 서비스마다 컨테이너를 따로 띄운다.
- **faststart 이미지 (faststart image)**: DB를 미리 만들어 넣어 둬서 처음 뜨는 시간을 줄인 Oracle XE 이미지다. 로컬 실행과 Testcontainers 모두 이 이미지를 쓴다.
- **digest**: 이미지 내용으로 계산한 고유 해시다. 태그와 달리 바뀌지 않아서, kind 노드 이미지를 digest까지 적어 고정한다.
