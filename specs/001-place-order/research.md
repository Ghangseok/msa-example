# Research: 주문 생성 (Phase 0)

**기능 폴더**: `specs/001-place-order` | **작성일**: 2026-10-05 | **입력**: [spec.md](spec.md), [plan.md](plan.md)

이 문서는 plan을 쓰기 전에 정한 것과 조사한 것을 적는다. 항목마다 **결정**, **이유**, **고려한 대안**을 적는다.

- "사용자 결정 (2026-10-05)"이라고 적은 것은 사용자가 대화에서 고른 것이다.
- `[제안]`은 사용자가 정하지 않았는데 Claude가 채운 것이다. 사용자가 검토해서 받아들이면 태그를 지운다.
- 이 문서는 항목 ID를 되도록 쓰지 않고 이름으로 부른다. ID를 쓴 곳에는 바로 아래에 출처와 원문을 펼쳤다.

## 0. 버전과 확인한 방법

조회일은 **2026-10-05**(UTC 12시 무렵)이다. Maven Central은 `https://repo1.maven.org/maven2/<그룹 경로>/<아티팩트>/maven-metadata.xml`을 `curl`로 받아 `<release>`와 버전 목록을 읽었다. 아래 "확인 방법" 칸에 그 밖의 주소와 명령을 적었다.

| 구성 요소 | 정한 버전 | 확인 방법 | 비고 |
|---|---|---|---|
| JDK | 17.0.2 | `java -version` | tech-stack.md 그대로 |
| Spring Boot | 4.1.1 | Maven Central `org/springframework/boot/spring-boot` 메타데이터. 4.1.1 pom의 Last-Modified는 2026-08-20 | 4.2.0-M2는 마일스톤이라 쓰지 않는다 |
| Spring Framework, Spring Security | 7.0.9, 7.1.1 | `spring-boot-dependencies-4.1.1.pom`의 `spring-framework.version`, `spring-security.version` | Spring Boot가 관리 |
| Gradle wrapper | 9.8.0 | `https://services.gradle.org/versions/current` | Spring Boot 4.1.1 문서(`https://docs.spring.io/spring-boot/4.1/system-requirements.html`)가 "Gradle 8.x (8.14 이상)와 9.x"를 지원한다고 적는다 |
| JUnit Jupiter | 6.0.3 | `spring-boot-dependencies-4.1.1.pom`의 `junit-jupiter.version` | **tech-stack.md에는 "JUnit 5"로 적혀 있다.** 5절 참고 |
| AssertJ | 3.27.7 | 같은 pom | Spring Boot가 관리 |
| Testcontainers | 2.0.5 (`testcontainers-oracle-xe` 모듈) | 같은 pom의 `testcontainers.version`과 Maven Central 메타데이터 | 2.x에서 모듈 이름이 `testcontainers-oracle-xe`로 바뀌었다 |
| Flyway | 12.4.0 (+ `flyway-database-oracle` 12.4.0) | 같은 pom, Maven Central에서 `flyway-database-oracle` 12.4.0 pom이 HTTP 200 | Spring Boot가 관리 |
| Oracle JDBC | 23.26.3.0.0 | 같은 pom의 `oracle-database.version` | Spring Boot가 관리. 21c XE에 접속할 수 있다 |
| OpenTelemetry SDK | 1.62.0 | 같은 pom의 `opentelemetry.version` | Spring Boot가 관리 |
| Spring Boot 스타터 | 4.1.1 | Maven Central에서 `spring-boot-starter-webmvc`, `-validation`, `-actuator`, `-restclient`, `-flyway`, `-oauth2-resource-server`, `-opentelemetry`, `-security-test`, `spring-boot-testcontainers`의 4.1.1 pom이 모두 HTTP 200 | |
| mybatis-spring-boot-starter | **4.1.0** (MyBatis 3.5.19, MyBatis-Spring 4.1.0) | Maven Central 메타데이터. GitHub 릴리스 노트(`gh api repos/mybatis/spring-boot-starter/releases`)와 README의 Requirements 절 | 4.1.0은 2026-07-16에 Spring Boot 4.1을 대상으로 나왔다. 4.0.x는 Spring Boot 4.0 대상이다 |
| mybatis-spring-boot-starter-test | 4.1.0 | Maven Central 메타데이터 | |
| Resilience4j | 2.4.0 (`resilience4j-circuitbreaker`, `resilience4j-retry`) | Maven Central 메타데이터. jar의 클래스 파일 버전 61(Java 17) | 2026-03-14 릴리스 |
| OpenTelemetry Logback appender | **2.28.1-alpha** | 버전별 pom의 `opentelemetry-api` 의존성 버전 비교 | 2.28.1-alpha가 Spring Boot가 관리하는 SDK 1.62.0과 맞는다. 최신 2.32.0-alpha는 1.66.0을 요구한다. 이름에 alpha가 붙은 것은 이 모듈의 정식 배포 형태다 |
| WireMock | 3.13.2 (`wiremock-standalone`) | Maven Central 메타데이터 | 4.0.0은 아직 beta다 |
| swagger-request-validator-core | 2.46.1 | pom의 `maven.compiler.release` 값 8 | 3.0.0은 이름을 `openapi-request-validator`로 바꾸며 `maven.compiler.release` 21이 됐다. JDK 17에서 쓸 수 없다 |
| ArchUnit | 1.5.1 (`archunit-junit5`) | Maven Central 메타데이터 | |
| Spotless Gradle 플러그인 | 8.10.3 | Maven Central 메타데이터. jar 클래스 파일 버전 61 | |
| palantir-java-format | 2.101.0 | Maven Central 메타데이터. jar 클래스 파일 버전 55(Java 11) | |
| SpotBugs Gradle 플러그인 | 6.5.12 | `https://plugins.gradle.org/m2/com/github/spotbugs/snom/spotbugs-gradle-plugin/maven-metadata.xml`. jar 클래스 파일 버전 55. README에 "Gradle 8.2+" | |
| SpotBugs | 4.10.4 | Maven Central 메타데이터와 GitHub 릴리스(`gh api repos/spotbugs/spotbugs/releases`) | 4.10.2 릴리스 노트에 "Java 11 호환을 유지한다"는 변경이 있다 |
| (쓰지 않음) Spring Cloud | 2025.1.3 | `https://start.spring.io/actuator/info`의 `bom-ranges` | "Spring Boot 4.0.0 이상 4.2.0-M1 미만"을 지원한다. 결정 8에서 쓰지 않기로 했다 |

## 1. spec에서 넘어온 결정

### 결정 1. spec의 `[제안]` 11개를 모두 받아들인다

- **결정**: 사용자 결정 (2026-10-05). spec.md의 Assumptions → "`[제안]` 모음" 11개를 모두 받아들인다. plan은 이 내용을 정해진 것으로 보고 쓴다.
- **남은 일**: spec.md에서 11개의 `[제안]` 태그를 지우는 일은 spec 수정이다. 무엇을 지울지 보여 주고 사용자 승인을 받은 뒤 고친다. plan을 마친 보고에 목록을 적는다.

### 결정 2. 판정 기준 문서에 없는 기대값은 테스트 케이스 문서에 먼저 더한다

- **결정**: 사용자 결정 (2026-10-05).
  - spec 체크리스트 Notes의 "인수 시나리오가 없는 FR" 가운데 일부는 판정 기준 문서에 기대값이 있다. 그 기대값은 거기서 가져온다.
  - 판정 기준 문서에 없는 아홉 가지(아래 ㉠, ㉡, ㉢, ㉣, ㉥, ㉦, ㉧, ㉨, ㉩. ㉥부터 ㉨까지는 2026-10-06, ㉩는 2026-10-07 사용자 결정으로 더했다)는 `docs/test-cases/order-placement.md`에 새 테스트 케이스로 더한다. 초안은 이 절 끝에 있고, 사용자 승인을 받은 뒤 main에서 만든 별도 docs 브랜치와 PR로 넣는다.
  - ㉤(P2 단계의 임시 동작)에는 전용 테스트를 두지 않는다.
- **이유**: 저장소 루트 `CLAUDE.md` 7절은 문서에 없는 기대값을 지어내지 말고 "문서에 먼저 추가하자고 묻는다"고 정한다. ㉤은 P3에서 재시도가 들어오면 바뀌는 동작이다. P2에서 전용 테스트를 만들면 P3에서 그 테스트를 고치거나 지워야 하는데, 같은 절이 이것을 막는다. ㉤의 경로는 "서킷이 열리는 시나리오"(spec 시나리오 7.1)가 P2와 P3 모두에서 지나간다.
- **고려한 대안**: spec.md의 FR 문장을 기대값으로 쓰는 안. spec.md가 판정 기준 목록에 없어서 고르지 않았다. 설계·분석 문서를 판정 기준에 더하는 안. CLAUDE.md를 고쳐야 하고 다른 기능에도 번져서 고르지 않았다.

판정 기준 문서에서 기대값을 가져오는 FR은 아래와 같다. FR 번호는 spec-kit 번호라서 펼치지 않는다.

| FR | 기대값을 가져오는 곳 |
|---|---|
| FR-014 (요청에 적힌 사용자 정보를 믿지 않는다) | 유스케이스 6절 업무 규칙 "주문은 로그인한 고객 본인의 이름으로만 만든다" |
| FR-020 (거절·실패 사유와 상품 목록을 기록한다) | 주문 조회 유스케이스 2-1절 "각 주문에는 … 거절·실패 사유(재고 부족이면 부족한 상품) … 이 있다" |
| FR-027의 500 쪽 (500이면 재시도하지 않는다) | 테스트 케이스 문서 공통 전제 "재고 서비스가 409나 500을 돌려주면 주문 서비스는 재시도하지 않는다" |
| FR-037 (값을 설정으로 둔다) | 비기능 요구사항 문서 "장애 전파 차단" 항목의 목표값(30초, 2.5초, 즉시·1·2·4·8초) |
| FR-012 (재고 등록·조회 API를 만들지 않는다) | 유스케이스 8절 "범위 밖". "재고 서비스를 외부에 열지 않는다"는 배포(Gateway 경로)의 일이라 티켓 003에서 확인한다 |

#### 테스트 케이스 문서에 더할 케이스 초안 `[제안]`

번호는 문서에 넣을 때 다음 빈 번호부터 붙인다. 아래 기대값은 괄호 안의 설계·분석 문서 문장에서 가져왔다. 사용자가 승인하면 이 기대값이 판정 기준이 된다.

**㉠ 거절된 예약에 해제 요청이 와도 재고는 그대로다** (재고 서비스, P1. 근거: 설계 6절 "해제 처리" 2번 "REJECTED나 RELEASED면 그대로 둔다")

- **Given** 상품 A의 재고가 2개다
- **And** 주문 번호 2001로 상품 A 3개 예약을 요청해 REJECTED를 받았다
- **When** 주문 번호 2001의 해제 요청이 온다
- **Then** 해제 요청은 REJECTED를 돌려준다
- **And** 상품 A의 재고는 2개다

**㉡ 해제한 예약의 기록은 남아 있어서, 같은 예약 요청이 다시 와도 재고가 줄지 않는다** (재고 서비스, P1. 근거: 도메인 분석 5절 "예약 기록은 이번 단계에서 지우지 않는다", 6절 표 1번 "저장된 첫 결과를 그대로 돌려준다")

- **Given** 상품 A의 재고가 10개다
- **And** 주문 번호 2002로 상품 A 3개 예약이 처리됐고, 그 뒤 해제됐다
- **When** 주문 번호 2002, 상품 A, 3개로 예약을 다시 요청한다
- **Then** 재고 서비스는 RELEASED를 돌려준다
- **And** 상품 A의 재고는 10개다

**㉢ 재시도하던 중에 서킷이 열리면 남은 재시도를 하지 않는다** (주문 서비스, P3. 근거: 설계 4절 서킷 브레이커 표 "열려 있을 때: 재고를 부르지 않고 바로 실패한다. 재시도도 하지 않는다", "기록 단위: 시도 하나하나", "열리는 조건: 최근 10번의 시도 중 50% 이상 실패 (최소 10번 기록된 뒤)")

- **Given** 재고 서비스가 모든 예약 요청에 503을 돌려준다
- **And** 고객 C1의 첫 주문이 예약 요청 6번을 모두 실패하고 "실패"로 끝났다
- **When** 고객 C1이 둘째 주문을 요청한다
- **Then** 둘째 주문의 예약 요청은 정확히 4번 간다(열 번째 실패한 시도에서 서킷이 열린다)
- **And** 둘째 주문은 "실패"로 기록되고 고객은 "잠시 후 다시 시도"를 받는다

**㉣ 해제 요청도 일시 오류면 같은 정책으로 재시도한다** (주문 서비스, P3. 근거: 설계 3절 그림 "DELETE /reservations/1001 (비동기, 같은 재시도 정책)")

- **Given** 재고 서비스가 예약 요청에 409를 돌려준다
- **And** 재고 서비스가 해제 요청의 처음 두 번에 503을 돌려주고, 세 번째부터 정상으로 처리한다
- **When** 고객이 상품 A를 3개 주문한다
- **Then** 고객은 주문 번호와 "잠시 후 다시 시도"를 받는다
- **And** 해제 요청은 정확히 3번 갔다. 두 번째는 첫 번째 응답을 받은 뒤 0.5초 안에, 세 번째는 두 번째 응답을 받은 뒤 0.8초 이상 1.5초 이하에 갔다
- **And** 주문에 "해제 완료"가 기록된다

**㉥ 예약된 재고에 해제가 두 번 와도 수량은 한 번만 돌아온다** (재고 서비스, P1. 근거: 도메인 분석 6절 표 5번 "해제 요청, 예약됨 상태: 수량을 복구하고 해제됨으로 바꾼다", 8번 "해제 요청을 여러 번: 수량은 한 번만 복구된다". 2026-10-06 사용자 결정으로 더했다)

- **Given** 상품 A의 재고가 10개다
- **And** 주문 번호 2003으로 상품 A 3개 예약이 처리됐다
- **When** 주문 번호 2003의 해제 요청이 온다
- **Then** 해제 요청은 RELEASED를 돌려준다
- **And** 상품 A의 재고는 10개다
- **When** 같은 해제 요청이 한 번 더 온다
- **Then** 해제 요청은 RELEASED를 돌려준다
- **And** 상품 A의 재고는 여전히 10개다 (13개가 아니다)

아래 세 케이스는 spec Edge Cases에서 사용자가 받아들인 제안(결정 1)을 테스트 케이스로 옮긴 것이다. 어느 제안을 옮길지는 2026-10-06 사용자 결정이고, 문장은 `[제안]`이다.

**㉦ 서명이 틀리거나 만료된 토큰으로는 주문할 수 없다** (주문 서비스, P2. 근거: spec Edge Cases의 받아들인 제안 9번 "토큰이 없을 때와 같이 401로 거부하고 주문을 기록하지 않는다")

- **When** 서명이 틀린 토큰으로 주문한다
- **Then** 401을 받고 주문은 기록되지 않는다
- **When** 만료된 토큰으로 주문한다
- **Then** 401을 받고 주문은 기록되지 않는다

**㉧ 형식이 틀린 주문 항목은 기록되지 않는다** (주문 서비스, P2. 근거: 받아들인 제안 11번 "상품이 비었거나 수량이 정수가 아니면 400을 돌려준다. 주문을 기록하지 않고 재고 서비스에도 요청하지 않는다")

- **When** 상품이 빈 항목이 있는 주문, 수량이 정수가 아닌(1.5) 항목이 있는 주문을 각각 요청한다
- **Then** 두 경우 모두 400을 받는다
- **And** 주문은 기록되지 않고 재고 서비스에 요청이 가지 않는다

**㉨ 거부된 요청의 키는 남지 않고, 같은 키의 내용 비교는 항목 순서를 보지 않는다** (주문 서비스, P2. 근거: 받아들인 제안 7번 "거부된 요청은 주문을 기록하지 않으므로 키도 남지 않는다. 같은 키로 고쳐 보낸 올바른 요청은 새 주문이 된다", 8번 "항목 순서는 내용 비교에서 보지 않는다")

- **When** 고객이 요청 키 K4로 수량이 0인 항목이 있는 주문을 보내 400을 받고, 같은 키 K4로 상품 A 1개를 주문한다
- **Then** 주문 번호를 받고 그 주문이 기록된다
- **When** 고객이 요청 키 K5로 상품 A 1개와 상품 B 1개를 "A, B" 순서로 주문하고, 같은 키 K5로 "B, A" 순서로 같은 요청을 한 번 더 보낸다
- **Then** 두 응답의 주문 번호가 같다 (422가 아니다)
- **And** 주문은 하나만 기록된다

**㉩ 주문 서비스는 재고 서비스 호출에 추적 정보를 넘긴다** (주문 서비스, P2. 근거: 헌법 "관측 가능성" 원칙의 "서비스 사이 호출에 추적 정보를 넘긴다". 2026-10-07 사용자 결정으로 더했고, 문장은 `[제안]`이다)

- **Given** 재고 서비스가 예약 요청에 RESERVED를 돌려준다
- **When** 고객이 상품 A 1개를 주문한다
- **Then** 재고 서비스가 받은 예약 요청에 W3C `traceparent` 헤더가 있다

### 결정 3. "예약된 재고를 해제하면 수량이 돌아온다"는 P1에서 재고 쪽 테스트로 확인한다

- **결정**: 사용자 결정 (2026-10-05). "30초 한도" 테스트 케이스(spec 시나리오 9.1)의 재고 쪽은 재고 서비스 테스트다. 그래서 P1 단계 PR에서 먼저 만든다. 내용은 예약, 해제, 수량 복구이고 기대값은 "상품 A의 재고는 10개다"다. 주문 쪽(시도 6번, 실패 기록, 해제 요청)은 P3에서 만든다.
- **이유**: 해제 기능은 P1에서 만든다. 그 검증을 P3까지 미루면 검증하지 않은 코드가 P1에 병합된다. 이 케이스의 비고가 이미 "재고 쪽은 재고 서비스 테스트로 검증한다"고 나눠 두었으므로 문서를 고칠 필요가 없다.
- **고려한 대안**: 유스케이스 7절 표를 고쳐 이 케이스를 재고 쪽과 주문 쪽으로 나누는 안, 새 테스트 케이스를 더하는 안. 둘 다 같은 내용을 문서에 한 번 더 적게 된다.

## 2. 범위

### 결정 4. 헌법이 요구하는 앱 설정은 서비스를 만드는 단계에 넣는다

- **결정**: 사용자 결정 (2026-10-05). 아래 설정은 서비스를 만드는 PR에 함께 넣는다.
  - 대상: 모든 로그 줄의 trace ID, OTLP 전송, readiness·liveness, 그레이스풀 셧다운, 서비스별 CLAUDE.md
  - 재고 서비스는 P1 단계 PR, 주문 서비스는 P2 단계 PR이다.
  - 서비스 사이 호출의 추적 정보 전달은 재고 호출이 생기는 P2 단계 PR에 넣는다.
- **이유**: PR마다 헌법을 지킨 채로 병합된다. 기능이 없는 빈 모듈을 미리 만들지 않는다.
- **티켓 003으로 넘기는 것**: Deployment의 프로브 설정, `terminationGracePeriodSeconds`, replicas, ConfigMap·Secret 매니페스트, 이미지 빌드.

### 결정 5. CI는 P1 단계 PR에 넣는다

- **결정**: 사용자 결정 (2026-10-05). `.github/workflows/`의 CI를 P1 단계 PR에 빌드 골격의 일부로 넣는다.
  - 검사 범위는 `docs/standards/git-workflow.md` 4절의 필수 검사다. 빌드와 단위·Mapper·서비스 통합·계약·ArchUnit 테스트이고, 기계 검사도 빌드 안에서 함께 돈다.
  - 병합한 뒤 main 규칙에 필수 검사를 켜는 일은 사용자가 한다.
- **[제안] 세부**: GitHub Actions의 `ubuntu-latest`에서 JDK 17(Temurin)을 설치하고 `./gradlew build`를 돌린다 `[제안]`. Testcontainers는 러너의 Docker를 쓴다 `[제안]`. Gradle 캐시는 `gradle/actions/setup-gradle`로 둔다 `[제안]`. Actions 버전은 P1 구현 때 조회해서 고정한다 `[제안]`.

## 3. 기술 선택

### 결정 6. 서비스 간 HTTP는 RestClient

- **결정**: 사용자 결정 (2026-10-05). `InventoryClient` 클래스 하나에서 `RestClient`로 예약(`PUT`)과 해제(`DELETE`)를 부른다. 상태 코드별 처리도 같은 클래스에 둔다.
- **이유**: 호출이 두 개뿐이라 인터페이스와 등록 설정을 더 두지 않는다. 상태 코드별 처리(200 결과, 409·500은 재시도 없음, 502·503·504는 재시도)가 한곳에 보인다.
- **[제안] 세부**:
  - `RestClient`는 Spring Boot가 주는 `RestClient.Builder`로 만든다 `[제안]`. 그래야 추적 정보(`traceparent`)가 자동으로 붙는다.
  - 요청 팩토리는 별도 의존성이 없는 `JdkClientHttpRequestFactory`를 쓴다 `[제안]`. 연결 제한 시간과 요청 제한 시간을 모두 2.5초로 설정한다 `[제안]`.
  - JDK HttpClient의 요청 제한 시간이 연결 시간까지 포함하는지는 확인하지 못했다. 6절 참고.
- **고려한 대안**: HTTP Interface(`@HttpExchange`). 경로가 많을 때 낫지만, 상태 코드별 처리를 결국 감싸는 클래스에서 다시 해야 한다.

### 결정 7. 재시도는 Resilience4j Retry

- **결정**: 사용자 결정 (2026-10-05). P3 단계에서 `resilience4j-retry` 2.4.0으로 재시도한다. 순서는 "재시도(서킷 브레이커(재고 호출))"이고, 서킷 브레이커가 시도 하나하나를 센다.
- **이유**: 서킷 브레이커와 같은 라이브러리라서 `Decorators`로 순서를 한 줄에 적을 수 있다. 시도마다 대기 간격을 함수로 자유롭게 줄 수 있다.
- **이 선택 때문에 따로 만드는 것** `[제안]`:
  - **재시도를 포함한 전체 30초 한도**: Resilience4j Retry에는 이 기능이 없다. 요청마다 시작 시각과 시도 횟수를 담은 작은 문맥 객체를 만든다. 그리고 요청마다 `Retry`를 새로 만든다(설정 객체는 공유한다) `[제안]`. 재시도 여부 판단 함수(`retryOnException`)가 "지금 시각 + 다음 대기 < 시작 시각 + 30초"일 때만 재시도를 허락한다 `[제안]`.
    - 이 판단은 공통 전제 "30초가 되기 전이면 시도를 시작한다"와 맞는다.
    - 시도당 2.5초와 대기 즉시·1·2·4·8초를 모두 더해도 30초를 넘지 않는다(설계 4절 최악 시간표). 그래도 진행 중인 시도는 30초에 끊는다. 공통 전제가 "시도는 30초에 끊는다"고 정하고, 위 계산은 한 시도가 연결 시간을 포함해 2.5초를 넘지 않는다는 확인하지 못한 전제(6절)에 기대기 때문이다. 시도마다 제한 시간을 "시도당 제한 시간"과 "30초까지 남은 시간" 가운데 짧은 쪽으로 준다(2026-10-06 사용자 승인, tasks.md의 User Story 9 "전체 한도 판단을 만든다" 작업).
  - **대기 즉시·1·2·4·8초**: `IntervalBiFunction`으로 시도 번호에 따라 0, 1000, 2000, 4000, 8000밀리초를 돌려준다 `[제안]`. Resilience4j 2.4.0 소스(`RetryImpl`)를 확인했다. 직접 넘긴 간격 함수에는 하한 검사가 없고 `Thread.sleep(간격)`을 부르므로 0을 쓸 수 있다.
  - **재시도할 예외**: 연결 실패와 시간 초과(`ResourceAccessException`), 502·503·504(`HttpServerErrorException`의 상태 코드로 판단)만 재시도한다. 서킷이 열려 생긴 `CallNotPermittedException`은 재시도하지 않는다.
- **고려한 대안**: Spring Framework 7의 `RetryTemplate`. 7.0.2부터 대기를 포함한 전체 제한 시간(`timeout`)이 들어 있다(Spring Framework v7.0.9 소스의 `RetryPolicy`, `RetryTemplate`에서 확인). 사용자가 Resilience4j를 골랐다.
- **문서와 어긋남**: tech-stack.md의 재시도 행은 "Spring Framework 7 내장 재시도"를 선택으로 적는다. 5절 참고.

### 결정 8. 서킷 브레이커는 Resilience4j 2.4.0 핵심 모듈을 직접 쓴다

- **결정**: 사용자 결정 (2026-10-05). `resilience4j-circuitbreaker`와 `resilience4j-retry`만 쓴다. Spring Boot 자동 설정 모듈(`resilience4j-spring-boot4`)과 Spring Cloud Circuit Breaker는 쓰지 않는다.
- **이유**: 두 핵심 모듈은 Spring에 의존하지 않아 Spring Boot 버전과 묶이지 않는다. `resilience4j-spring-boot4` 2.4.0은 Spring Boot 4.0.0과 Spring 7.0.2를 대상으로 빌드됐다(pom에서 확인). 설정값은 우리 `@ConfigurationProperties`로 받는다(코딩 규약 3-5절).
- **설계 4절 값과 Resilience4j 설정의 대응** `[제안]`:

  | 설계 4절 | Resilience4j 설정 |
  |---|---|
  | 대상: 재고 서비스 호출(예약, 해제)에 서킷 하나 | `CircuitBreaker` 인스턴스 하나(`inventory`)를 예약과 해제가 함께 쓴다 |
  | 기록 단위: 시도 하나하나 | 서킷 브레이커를 재시도 안쪽에 둔다 |
  | 실패로 세는 것: 연결 실패, 시도 제한 시간 초과, 5xx. 업무 결과와 4xx는 성공 | `recordException`에 연결 실패·시간 초과·5xx 판단 함수를 준다. 이 판단에 걸리지 않는 예외(4xx)와 정상 응답은 성공으로 센다 |
  | 열리는 조건: 최근 10번 중 50% 이상 실패 (최소 10번 기록된 뒤) | `slidingWindowType=COUNT_BASED`, `slidingWindowSize=10`, `minimumNumberOfCalls=10`, `failureRateThreshold=50` |
  | 열려 있는 시간 10초, 그 뒤 시험 호출 3번 | `waitDurationInOpenState=10s`, `permittedNumberOfCallsInHalfOpenState=3` |
  | 반쯤 열린 상태: 시험 호출이 기준 아래로 실패하면 닫고, 아니면 다시 연다 | 반쯤 열린 상태에서도 `failureRateThreshold=50`을 쓴다(3번 중 1번 이하 실패면 닫힘) |
  | 열려 있을 때: 바로 실패, 재시도도 하지 않는다 | `CallNotPermittedException`을 재시도 대상에서 뺀다 |

- **고려한 대안**: `resilience4j-spring-boot4` 2.4.0(yml 설정과 애너테이션, Spring Boot 4.1.1 호환을 확인해야 함), Spring Cloud Circuit Breaker 5.0.3(Spring Cloud 2025.1.3, Spring Boot 4.1.1 지원. 감싼 API가 "재시도 바깥, 서킷 안쪽" 조합을 직접 지원하지 않음).

### 결정 9. MyBatis 스타터 4.1.0, 단순 조회는 record, 1:N 부모는 일반 클래스

- **결정**: 사용자 결정 (2026-10-05).
  - `mybatis-spring-boot-starter` 4.1.0을 쓴다.
  - 설정은 `mybatis.configuration.map-underscore-to-camel-case=true`와 `mybatis.configuration.arg-name-based-constructor-auto-mapping=true`다.
  - 단순 조회 결과는 record로 만든다. 1:N을 `<collection>`으로 읽는 부모 객체만 일반 클래스로 둔다(Lombok 없이).
- **이유**:
  - 4.1.0은 Spring Boot 4.1을 대상으로 나왔다(GitHub README의 Requirements: "master : MyBatis 3.5, MyBatis-Spring 4.1, Java 17+ and Spring Boot 4.1").
  - `argNameBasedConstructorAutoMapping`(MyBatis 3.5.10부터)은 열 이름을 생성자 인자 이름으로 찾는다. 이 설정이 있어야 record의 생성자에 값을 넣을 수 있다. Spring Boot Gradle 플러그인이 컴파일 옵션 `-parameters`를 켜므로 인자 이름이 클래스 파일에 남는다.
  - MyBatis 3.5.19 문서에는 생성자 안에서 `<collection>`을 채우는 설명이 없다. 그래서 1:N 부모는 record로 만들 수 없다고 판단했다. 이 판단은 확인하지 못했다. P1의 Mapper 테스트에서 확인한다(6절).
- **고려한 대안**: 4.0.1(Spring Boot 4.0 대상), 1:N도 평면 행 record로 읽어 Java에서 묶는 안(코딩 규약 3-2절 "`<collection>`으로 한 번에 읽는다"와 어긋남).
- **문서와 어긋남**: tech-stack.md는 "mybatis-spring-boot-starter 4.0.x"라고 적는다. 5절 참고.

### 결정 10. SpotBugs 6.5.12 플러그인 + 4.10.4, 처음부터 끄는 규칙은 두 개

- **결정**: 사용자 결정 (2026-10-05).
  - SpotBugs Gradle 플러그인 6.5.12와 SpotBugs 4.10.4를 쓴다.
  - `EI_EXPOSE_REP`와 `EI_EXPOSE_REP2` 두 규칙만 `config/spotbugs/exclude.xml`에서 끈다. 나머지는 모두 켜고, 하나라도 찾으면 빌드를 실패시킨다.
  - 이 파일을 바꾸려면 PR에서 사용자가 승인한다.
- **이유**: 두 규칙은 List를 담은 record와 생성자로 빈을 받는 Spring 클래스에서 거의 언제나 오탐을 낸다.
- **[제안] 세부**: `effort=max`, `reportLevel=low`로 둔다 `[제안]`. 보고서는 HTML로 낸다 `[제안]`.

### 결정 11. 기계 검사의 위치와 형식

- **결정**: 사용자 결정 (2026-10-05).
  - **Gradle 작업으로 하는 검사**: `build-logic/` 포함 빌드의 convention plugin `msa.java-service`에 둔다. 대상은 Lombok 의존성, 검사를 끄는 표시, Mapper XML의 `${}`와 `SELECT *`, 19c 이후 키워드, 테스트 케이스 ID다. Spotless와 SpotBugs 적용도 같은 플러그인에서 한다. 두 서비스는 `plugins { id 'msa.java-service' }`로 적용한다.
  - **ArchUnit 규칙**: `libs/archunit-rules`(테스트용 java-library)에 두고, 각 서비스 테스트가 불러 쓴다. 대상은 계층 의존, 애너테이션 SQL 금지, `@Transactional`에서 `client` 직접 호출 금지다.
  - **허용 목록**: `config/quality/suppression-allowlist.csv`. 열은 `path,marker,reason,approved_pr`이고, `${}` 예외도 이 파일에 적는다.
  - **테스트 케이스 단계 표**: `config/quality/test-case-stages.csv`. 열은 `tc_id,service,stage`다. 지금 단계는 `gradle.properties`의 `msa.stage`(P1, P2, P3)에 적는다.
    - 검사는 두 가지를 본다. 첫째, `docs/test-cases/order-placement.md`의 모든 테스트 케이스 ID가 이 표에 있는가. 둘째, 단계가 지금 단계 이하인 줄마다 그 서비스의 테스트 소스에 `@DisplayName("<ID>`로 시작하는 테스트가 하나 이상 있는가.
    - 티켓 002로 넘긴 부분은 단계를 `T002`로 적어 검사하지 않는다. 운영 테스트(`docs/test-cases/operations.md`)는 대상에서 뺀다.
- **이유**: 검사 코드가 한곳에 있다. 단계가 오를 때만 검사 범위가 넓어지므로, P1에서 아직 없는 P2·P3 케이스 때문에 빌드가 실패하지 않는다. 그렇다고 검사를 끄지도 않는다.
- **[제안] 세부**: Groovy DSL로 쓴 precompiled script plugin(`build-logic/src/main/groovy/msa.java-service.gradle`)으로 만든다 `[제안]`. 버전은 `gradle/libs.versions.toml`(버전 카탈로그) 한곳에 모은다 `[제안]`. Spring Boot BOM은 Gradle의 `platform(...)`으로 가져온다 `[제안]`. 그러면 `io.spring.dependency-management` 플러그인이 필요 없다.

### 결정 12. 계약 테스트는 swagger-request-validator-core 2.46.1

- **결정**: 사용자 결정 (2026-10-05). `com.atlassian.oai:swagger-request-validator-core` 2.46.1만 쓴다. 요청과 응답을 검증기의 `Request`·`Response` 객체로 옮기는 작은 테스트 도우미를 서비스마다 만든다.
  - **재고 서비스(제공자)**: 서비스 통합 테스트가 보낸 실제 요청과 받은 응답을 `contracts/inventory-api.yaml`로 검증한다.
  - **주문 서비스(소비자)**: 두 가지를 검증한다. WireMock 스텁이 돌려주는 응답과, WireMock이 받은 요청(`getAllServeEvents`)이다.
  - 계약은 OpenAPI 3.0.3으로 쓴다.
- **이유**: JDK 17에서 돌고(바이트코드 Java 8), Spring과 WireMock 버전에 묶이지 않는다. 2.46.1의 WireMock 모듈은 옛 WireMock 2(`wiremock-jre8`)용이고 MockMvc 모듈은 Spring 6.2 대상이라 쓰지 않는다. 3.0.0(이름 바꾼 `openapi-request-validator`)은 Java 21로 컴파일돼서 쓸 수 없다.
- **[제안] 세부**: 테스트 도우미는 공유하지 않고 서비스마다 둔다 `[제안]`. 계약을 읽는 쪽과 내주는 쪽이 각자 자기 테스트로 확인하는 구조다. 공유 모듈을 만들면 서비스 사이에 테스트 코드가 엮인다.

### 결정 13. 상태 코드와 오류 형식

- **결정**: 사용자 결정 (2026-10-05). 아래 표와 같다. 자세한 형식은 [contracts/order-api.yaml](contracts/order-api.yaml), [contracts/inventory-api.yaml](contracts/inventory-api.yaml)에 있다.

  | API | 상황 | 상태 코드 |
  |---|---|---|
  | `POST /orders` | 새 주문을 기록함(확정·거절·실패) | 201 |
  | `POST /orders` | 같은 고객이 같은 키로 다시 보냄(처리중 포함) | 200 |
  | `POST /orders` | 키가 없거나 UUID가 아님, 항목이 잘못됨 | 400 |
  | `POST /orders` | 토큰이 없거나 검증 실패 | 401 |
  | `POST /orders` | 같은 키에 다른 내용 | 422 |
  | `PUT /reservations/{orderNo}` | 업무 결과(RESERVED, REJECTED, RELEASED). 처음과 재요청 모두 | 200 |
  | `DELETE /reservations/{orderNo}` | 해제 결과(RELEASED, 거절된 예약이면 REJECTED 그대로) | 200 |
  | 재고 API | 같은 주문 번호에 다른 내용 | 409 |
  | 재고 API | 형식 오류 | 400 |
  | 재고 API | 교착 등 예상하지 못한 오류 | 500 |

  - 오류 응답은 Problem Details(`application/problem+json`)다. `type`은 `urn:msa-example:problem:<코드>`이고, 확장 필드로 `code`와 `errors`를 둔다.
  - 상태 값 목록(enum)은 지금 전부 정한다. 나중에 값을 늘리면 계약을 깨는 변경이기 때문이다.
  - 주문 `status`: PENDING, CONFIRMED, REJECTED, FAILED
  - 주문 `reason`: OUT_OF_STOCK, PRODUCT_NOT_FOUND, RETRY_LATER
  - 예약 `status`: RESERVED, REJECTED, RELEASED
  - 예약 `reason`: OUT_OF_STOCK, PRODUCT_NOT_FOUND
- **이유**: 실패도 주문이 기록된 결과다. 같은 키로 다시 보내도 결과가 바뀌지 않으므로 503을 쓰지 않는다. 재고 API는 처음과 재요청 모두 200을 돌려줘서, "처음과 같은 결과를 돌려준다"를 상태 코드까지 지킨다.
- **[제안] 세부**:
  - `Location` 헤더는 조회 API가 생기는 티켓 002에서 더한다 `[제안]`.
  - 401 응답도 Problem Details로 내도록 인증 실패 처리기(`AuthenticationEntryPoint`)를 둔다 `[제안]`. 코딩 규약 3-5절의 "오류 응답은 Problem Details로 통일"을 따른다.
  - 주문 응답에는 `releaseStatus`를 넣지 않는다 `[제안]`. 해제 결과는 관리자 조회(티켓 002)에서 보여 준다.

### 결정 14. 예약 내용 비교는 예약 항목을 직접 비교한다

- **결정**: 사용자 결정 (2026-10-05). 같은 주문 번호의 재요청이 오면 저장된 예약 항목을 상품 ID 순으로 읽는다. 그리고 들어온 항목을 같은 순서로 정렬한 것과 비교한다. `REQUEST_HASH` 열은 만들지 않는다. 해제 표식(항목 없음)이 있으면 비교하지 않고 RELEASED를 돌려준다.
- **이유**: 해시 알고리즘과 직렬화 규칙을 정하고 고정할 필요가 없다. 비교 코드가 읽기 쉽다. 항목은 최대 20개라 한 번 더 읽어도 부담이 작다.
- **고려한 대안**: SHA-256 `REQUEST_HASH`. 열 하나로 비교가 끝나지만, 직렬화 규칙을 바꾸면 옛 기록과의 비교가 깨진다.

## 4. 묻지 않고 `[제안]`으로 채운 것

| 항목 | 고른 것 | 이유 |
|---|---|---|
| 해제를 고객 응답 뒤에 비동기로 돌리는 방식 | 주문 서비스에 해제 전용 `ThreadPoolTaskExecutor` 빈(`releaseExecutor`, 스레드 2~4개, 대기열 100) `[제안]`. 흐름은 이렇다. "실패" 결과를 저장하는 트랜잭션이 커밋된다 → `OrderService`가 별도 빈(`ReservationReleaser`)에 해제를 맡긴다 → 바로 응답한다. 해제를 마치면 해제 결과를 새 트랜잭션으로 저장한다 `[제안]` | 고객 응답이 해제를 기다리지 않는다. 같은 클래스 안에서 비동기 메서드를 부르면 프록시를 거치지 않으므로 별도 빈에 둔다. 종료할 때는 실행 중인 해제를 기다린다(`setWaitForTasksToCompleteOnShutdown(true)`, 최대 30초) `[제안]`. 그래도 인스턴스가 죽으면 해제 결과가 빈 "실패" 주문이 남는다. 이것은 설계 3절이 적어 둔 알려진 한계다 |
| 상품 ID 형식 | 영어 대문자·숫자·하이픈 1~20자(`^[A-Z0-9-]{1,20}$`), DB는 `VARCHAR2(20)` `[제안]` | 테스트 케이스의 "상품 A", "상품 Z"를 그대로 `A`, `Z`로 쓸 수 있다. 형식이 틀리면 spec의 받아들인 제안 11번대로 400이다 |
| 초기 재고 데이터 | Flyway `V2__seed_stock.sql`에 `P-001`부터 `P-005`까지, 각 100개 `[제안]` | 로컬 실행과 티켓 003의 E2E가 쓸 상품이다. 테스트는 테스트 케이스의 상품 ID(`A`, `B`, `Z`)를 직접 넣고 지운다(testing.md 4절). 그래서 초기 데이터와 겹치지 않는다 |
| 주문 번호 | `ORDERS_SEQ` 시퀀스, `NUMBER(19)`, JSON에서는 정수(int64) `[제안]` | 코딩 규약 3-2절 "주문 번호는 Oracle 시퀀스로 만든다" |
| 고객 ID 저장 | JWT `sub`를 `VARCHAR2(255)`로 저장 `[제안]` | Keycloak의 `sub`는 UUID 문자열이다. 다른 발급자로 바꿔도 담을 수 있게 넉넉히 둔다 |
| 거절 상품 목록 저장 | 주문 항목 줄과 예약 항목 줄에 결과 열을 하나씩 둔다(`ORDER_ITEMS.REJECT_REASON`, `RESERVATION_ITEMS.RESULT`, 값은 OUT_OF_STOCK·PRODUCT_NOT_FOUND·비어 있음) `[제안]` | 부족한 상품과 없는 상품은 주문 항목의 일부다. 표를 더 만들지 않는다. 재고 서비스는 거절된 주문 번호의 재요청에 "처음과 같은" 목록을 돌려줘야 하므로 이 열에서 다시 만든다 |
| 1:N 조회 결과 클래스 | `OrderWithItems`(주문 서비스), `ReservationWithItems`(재고 서비스)는 일반 클래스. 그 밖의 조회 결과는 record `[제안]` | 결정 9 |
| 설정 클래스 | `InventoryClientProperties`(주소, 연결·요청 제한 시간, 재시도, 전체 한도, 서킷 값), `ReleaseExecutorProperties` `[제안]` | 코딩 규약 3-5절 "설정값은 `@ConfigurationProperties`로 받는다" |
| 재고 서비스 주소 기본값 | `http://inventory:8080`, 환경변수로 덮어쓴다 `[제안]` | 아키텍처 규칙 2절의 Service 이름 호출 |
| 로그 형식 | Spring Boot 구조화 로그 `logging.structured.format.console=ecs` `[제안]`. 추적이 켜져 있으면 trace ID와 span ID가 MDC에 들어가 줄마다 찍힌다 | 코딩 규약 3-6절 "JSON 구조화 로그, 모든 줄에 traceId". 요청 밖에서 찍히는 시작 로그에는 trace ID가 없다. 이것은 추적이 없는 줄이라 생기는 일이다 |
| OTLP 전송 | `spring-boot-starter-opentelemetry` + `opentelemetry-logback-appender-1.0` 2.28.1-alpha를 `logback-spring.xml`에 설정한다. 주소는 환경변수(기본 `http://lgtm:4318`)로 받는다. 테스트에서는 내보내기를 끈다 `[제안]` | 설계 10절. 테스트에는 관측 저장소가 없다 |
| 프로브 | `management.endpoint.health.probes.enabled=true`. readiness 그룹에 DB 연결 확인(`db`), liveness 그룹에는 외부 상태를 넣지 않는다. 노출 엔드포인트는 `health`만 둔다 `[제안]` | 설계 8절과 아키텍처 규칙 3절 |
| 그레이스풀 셧다운 | `server.shutdown=graceful`, `spring.lifecycle.timeout-per-shutdown-phase=35s`를 설정에 적는다 | 설계 8절의 값(35초)이다. 기본값과 같아도 설정에 명시하라는 규칙(아키텍처 규칙 3절)을 따른다 |
| WireMock | `org.wiremock:wiremock-standalone` 3.13.2와 JUnit 확장 `WireMockExtension` `[제안]` | Jetty를 안에 숨긴(shaded) 판이라 Spring Boot가 관리하는 Jetty 버전과 부딪히지 않는다. JUnit 6에서 이 확장이 도는지는 확인하지 못했다(6절) |
| Testcontainers Oracle 재사용 | 서비스마다 `@ServiceConnection`이 붙은 Oracle 컨테이너 하나를 정적 필드로 두고, 그 서비스의 테스트 전체에서 같이 쓴다 `[제안]` | testing.md 3절 "Oracle 컨테이너는 서비스의 테스트 전체에서 하나를 재사용" |
| 테스트 시간 줄이기 | 서비스 통합 테스트는 시간 값을 모두 1/5로 줄인 설정으로 돌린다 `[제안]`. 시도당 500ms, 대기 0·200·400·800·1600ms, 전체 6s, 서킷 열림 2s이고, 지연과 기대값의 허용 오차도 같은 비율로 줄인다. 예를 들어 "3초 뒤 응답"은 600ms, "31초 안"은 6.2초 안, "즉시"는 앞 응답 뒤 0.1초 안이다. 아래 세 값은 설정한 시간과 관계없는 응답 시간이라 줄이지 않는다 `[제안]`. health 확인의 "1초 안에 200", 서킷이 열린 뒤의 "1초 안에 실패", "고객 응답 뒤 5초 안에 해제 요청"이다 | 공통 전제 "테스트에서는 같은 비율로 줄여 돌릴 수 있다"와 testing.md 3절 "시간"을 따른다. 1/10로 줄이면 "즉시"의 허용 오차가 0.05초가 되어 CI에서 흔들리기 쉬워서 1/5로 둔다. 기본값(줄이지 않은 값)이 설정에 맞게 들어갔는지는 단위 테스트로 따로 확인한다 |
| 테스트 케이스 추가 docs PR의 순서 | 결정 2의 docs PR을 P1 단계 PR보다 먼저 병합한다 `[제안]` | 테스트 케이스 ID 검사가 문서의 모든 케이스를 단계 표에서 찾으므로, 새 케이스가 먼저 문서에 있어야 P1이 그 케이스를 단계 표에 넣을 수 있다 |
| 주문 API 계약 위치 | `specs/001-place-order/contracts/order-api.yaml`에만 둔다. 저장소 루트 `contracts/`에는 서비스 사이 API인 재고 API만 둔다 `[제안]` | 아키텍처 규칙 2절이 루트 `contracts/`에 두라고 한 것은 서비스 사이 API다. 주문 API는 고객이 부르는 외부 API다 |

## 5. 기준 문서와 어긋난 것 (고칠지 따로 묻는다)

아래는 plan의 결정이 기준 문서의 글과 다른 곳이다. 고치는 일은 docs 수정이라서, 무엇을 왜 고칠지 보여 주고 사용자 승인을 받은 뒤 한다. plan은 고치기 전에도 아래 결정대로 쓴다.

| 문서 | 지금 글 | 고칠 내용 | 이유 |
|---|---|---|---|
| `docs/standards/tech-stack.md` 표 "재시도" | Spring Framework 7 내장 재시도(`@Retryable` / `RetryTemplate`) | Resilience4j Retry 2.4.0(서킷 브레이커와 같은 라이브러리) | 결정 7 |
| 같은 표 "서킷 브레이커" | Resilience4j (Spring Cloud Circuit Breaker 경유 또는 직접) | Resilience4j 2.4.0 핵심 모듈 직접 | 결정 8 |
| 같은 표 "서비스 간 HTTP" | `RestClient` 또는 HTTP Interface, plan에서 하나로 정한다 | `RestClient` | 결정 6 |
| 같은 표 "영속성" | mybatis-spring-boot-starter 4.0.x | 4.1.0(Spring Boot 4.1 대상) | 결정 9 |
| 같은 표 "테스트" | JUnit 5 | JUnit Jupiter 6(Spring Boot 4.1.1이 관리하는 6.0.3) | 0절 버전 조회 |
| 같은 표 "정적 분석" | 정확한 버전과 끌 규칙은 plan에서 정한다 | 플러그인 6.5.12, SpotBugs 4.10.4, `EI_EXPOSE_REP`·`EI_EXPOSE_REP2` 끔 | 결정 10 |
| `docs/standards/testing.md` 2절 "계약" 행 | OpenAPI 검증 라이브러리 (plan에서 확정) | swagger-request-validator-core 2.46.1 | 결정 12 |
| `docs/design/architecture.md` 머리말과 5·6절 | 5절과 6절의 경로, 필드, 열 이름은 plan까지 제안 | plan에서 정한 이름으로 바꾸고 `[제안]` 표시를 지운다. 6절의 `REQUEST_HASH`를 빼고 항목 직접 비교로 바꾼다. 주문·예약 항목의 결과 열을 더한다 | 결정 13, 14, 4절의 결과 열 |
| `docs/test-cases/order-placement.md` | — | 1절 결정 2의 케이스 아홉 개를 더한다 | 결정 2 |
| `specs/001-place-order/spec.md` | `[제안]` 태그 11개(Assumptions의 "`[제안]` 모음"과 본문) | 태그를 지운다 | 결정 1 |

## 6. 확인하지 못한 것

아래는 버전 메타데이터와 소스, 문서로만 판단했다. 아직 코드가 없어 빌드하거나 실행해 보지 못했다. 확인할 단계와 방법을 함께 적는다.

| 확인하지 못한 것 | 이유 | 확인할 단계와 방법 |
|---|---|---|
| WireMock 3.13.2의 `WireMockExtension`이 JUnit Jupiter 6.0.3에서 도는지 | WireMock 3.13.2는 JUnit 5 API로 빌드됐다. 아직 코드가 없다 | P2 첫 서비스 통합 테스트 |
| JDK HttpClient의 요청 제한 시간이 연결 시간까지 포함해 시도당 2.5초를 지키는지 | 설계 4절은 "2.5초 (연결 포함)"인데, JDK의 연결 제한 시간과 요청 제한 시간은 따로 설정된다 | P2. WireMock으로 3초 지연을 넣은 테스트와, 응답하지 않는 주소로 연결하는 단위 테스트 |
| MyBatis 3.5.19에서 record 생성자 매핑이 되는지, 생성자 안 `<collection>`이 안 되는지 | 문서만 읽었다 | P1 첫 Mapper 테스트 |
| swagger-request-validator-core 2.46.1이 OpenAPI 3.0.3 계약과 JDK 17에서 도는지 | 실행해 보지 않았다 | P1 계약 테스트(제공자) |
| palantir-java-format이 JDK 17의 Gradle 9.8.0 안에서 Spotless로 도는지 | 실행해 보지 않았다 | P1 첫 `./gradlew build` |
| Testcontainers 2.0.5가 이 PC의 Docker Desktop에서 `gvenzl/oracle-xe:21.3.0-slim-faststart`를 띄우는지 | 실행해 보지 않았다. 문제가 생기면 `docs/references/docker-desktop.md`부터 본다 | P1 첫 Mapper 테스트 |
| Resilience4j 2.4.0 핵심 모듈이 Spring Boot 4.1.1 앱 안에서 문제없이 도는지 | 핵심 모듈은 Spring에 의존하지 않아 문제를 예상하지 않지만, 실행해 보지 않았다 | P2 서비스 통합 테스트 |
| GitHub Actions 러너에서 Oracle 컨테이너를 띄우는 시간과 메모리 | CI를 아직 만들지 않았다 | P1 PR의 첫 CI 실행 |

## 용어

- **convention plugin**: 여러 하위 프로젝트에 같은 빌드 설정을 적용하려고 만든 Gradle 플러그인이다. 이 저장소에서는 `build-logic/`의 `msa.java-service`가 두 서비스에 Spotless, SpotBugs, 기계 검사를 적용한다.
- **포함 빌드 (included build)**: 루트 `settings.gradle`의 `includeBuild`로 묶는, 따로 빌드되는 Gradle 프로젝트다. `build-logic/`이 포함 빌드이고, 그 안의 플러그인을 서비스가 가져다 쓴다.
- **버전 카탈로그 (version catalog)**: 의존성 버전을 `gradle/libs.versions.toml` 한 파일에 모아 두는 Gradle 기능이다. 버전을 바꿀 때 이 파일 하나만 고친다.
- **BOM (Bill of Materials)**: 서로 맞는 라이브러리 버전 묶음을 적은 파일이다. Spring Boot BOM을 가져오면 Spring Boot가 관리하는 라이브러리의 버전을 따로 적지 않아도 된다.
- **Maven Central**: Java 라이브러리를 올리고 내려받는 공개 저장소다. 버전을 조회할 때 이 저장소의 `maven-metadata.xml`을 읽었다.
- **클래스 파일 버전 (class file major version)**: 컴파일된 `.class` 파일에 적힌, 그 파일을 돌리는 데 필요한 최소 Java 버전 번호다. 55는 Java 11, 61은 Java 17, 65는 Java 21이다.
- **마일스톤 (milestone)**: 정식 버전 전에 기능을 미리 써 보라고 내는 시험판이다. 이 프로젝트는 마일스톤과 beta를 쓰지 않는다.
- **Decorators**: Resilience4j에서 서킷 브레이커와 재시도 같은 기능을 함수 하나에 차례로 감싸는 도구다. 감싼 순서가 실행 순서가 된다.
- **IntervalBiFunction**: Resilience4j Retry에서 몇 번째 시도인지 보고 다음 대기 시간을 돌려주는 함수다. 즉시·1·2·4·8초 대기를 이 함수로 준다.
- **CallNotPermittedException**: Resilience4j 서킷이 열려 있을 때 호출을 막으며 던지는 예외다. 이 예외는 재시도하지 않는다.
- **슬라이딩 윈도 (sliding window)**: 서킷 브레이커가 실패율을 계산하려고 최근 호출 결과를 모아 두는 범위다. 이 기능은 최근 10번의 시도를 본다.
- **요청 팩토리 (request factory)**: RestClient가 실제 HTTP 연결을 만들 때 쓰는 부품이다. 연결·요청 제한 시간을 여기에 설정한다.
- **생성자 자동 매핑 (constructor auto-mapping)**: MyBatis가 조회 결과의 열을 객체 생성자 인자에 넣는 기능이다. record는 setter가 없어서 이 기능으로 값을 넣는다.
- **`-parameters` 컴파일 옵션**: 메서드와 생성자 인자 이름을 클래스 파일에 남기는 Java 컴파일 옵션이다. MyBatis가 인자 이름으로 열을 찾을 때 필요하다.
- **오탐 (false positive)**: 문제가 없는데 검사 도구가 문제라고 알리는 것이다. SpotBugs에서 오탐이 잦은 두 규칙을 처음부터 끈다.
- **Problem Details**: HTTP API의 오류 응답을 JSON으로 적는 표준 형식(RFC 9457)이다. `type`, `title`, `status`, `detail`과 확장 필드를 담는다.
- **URN (Uniform Resource Name)**: 위치가 아니라 이름으로 대상을 가리키는 식별자 형식이다. 오류 종류를 `urn:msa-example:problem:<코드>`로 적는다.
- **구조화 로그 (structured log)**: 로그 한 줄을 정해진 필드가 있는 JSON으로 쓰는 방식이다. ECS는 Elastic이 정한 필드 이름 규칙이다.
- **MDC (Mapped Diagnostic Context)**: 로그 라이브러리가 스레드마다 들고 다니는 키-값 저장소다. 추적 기능이 trace ID를 여기에 넣으면 로그 줄마다 찍힌다.
- **shaded jar**: 라이브러리가 쓰는 다른 라이브러리를 이름을 바꿔 안에 넣은 jar다. 앱이 쓰는 같은 라이브러리와 버전이 부딪히지 않는다.
- **`@ServiceConnection`**: Spring Boot가 Testcontainers 컨테이너의 접속 정보를 앱 설정에 자동으로 넣게 하는 애너테이션이다. 테스트마다 접속 주소를 따로 적지 않는다.
- **GitHub Actions**: GitHub가 제공하는 CI 실행 환경이다. PR마다 빌드와 테스트를 돌린다.
- **ThreadPoolTaskExecutor**: Spring이 주는 스레드 풀 실행기다. 실패한 주문의 해제를 고객 응답과 따로 돌릴 때 쓴다.
