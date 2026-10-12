# Quickstart: 주문 조회 검증

**기능 폴더**: `specs/002-view-orders` | **작성일**: 2026-10-11

이 문서는 단계별 PR이 끝났을 때 주문 조회가 맞게 도는지 확인하는 방법을 적는다. 구현 코드는 담지 않는다. 읽는 열과 조회 규칙은 [data-model.md](data-model.md), API는 [contracts/order-query-api.yaml](contracts/order-query-api.yaml), 어느 인수 시나리오를 어느 테스트가 확인하는지는 [plan.md](plan.md)의 "인수 시나리오와 테스트 층" 표에 있다.

- `[제안]`은 사용자가 정하지 않았는데 Claude가 채운 것이다. PR은 사용자가 Y안(2개)으로 정했다. 단계 이름은 `[제안]`이다.
- 이 문서의 테스트 클래스 이름은 plan.md의 Source Code 트리를 따른 `[제안]`이다. 구현 전이라서 아직 존재하지 않는다. tasks.md가 이름을 정한다.

## 1. 준비

| 준비할 것 | 확인 명령 | 기대 결과 |
|---|---|---|
| JDK 17 | `java -version` | `17.0.2` |
| Docker Desktop이 떠 있다 | `docker info` | 종료 코드 0 |
| Oracle 이미지 (처음 한 번 받는다) | `docker pull gvenzl/oracle-xe:21.3.0-slim-faststart` | 종료 코드 0 |

- 이 티켓의 테스트에는 로컬 실행 인프라(`infra/`, `tools/`)가 필요 없다. Testcontainers가 Oracle 컨테이너를 직접 띄운다. Keycloak도 필요 없다. 고객과 관리자는 Spring Security Test의 `jwt()`로 흉내 낸다.
- 로컬 인프라가 떠 있어서 메모리가 모자라면, `.\tools\infra-down.ps1`로 내릴지 사용자에게 먼저 묻는다.
- Docker나 Testcontainers에 문제가 생기면 `docs/references/docker-desktop.md`부터 본다.

## 2. 빌드와 모든 검사

PowerShell에서는 `.\gradlew.bat`, Git Bash와 CI에서는 `./gradlew`를 쓴다.

```powershell
.\gradlew.bat build
```

`build`는 컴파일, Spotless, SpotBugs, 기계 검사 작업(Lombok, 검사를 끄는 표시, Mapper XML, 19c 이후 키워드, 테스트 케이스 ID), 단위·Mapper·서비스 통합·ArchUnit 테스트를 모두 돌린다. 기대 결과는 종료 코드 0이다.

보고에는 실행한 명령과 결과를 함께 적는다(저장소 루트 `CLAUDE.md` 6절). 예:

```text
확인: .\gradlew.bat build → 종료 코드 0, 테스트 N개 통과, 실패 0
```

## 3. 단계별로 확인할 것 (사용자 결정: Y안, PR 2개)

`gradle.properties`의 `msa.stage`가 지금 단계다. 단계 이름 `T002-P1`, `T002-P2`는 `[제안]`이다. 이 값을 쓰려면 `TestCaseIdCheck.groovy`의 단계 순서 목록에 더해야 한다. PR 개수가 정해지기 전에는 쓰지 않는다(research.md 항목 13).

| 단계 | `msa.stage` | 확인할 것 | 기대 결과 |
|---|---|---|---|
| 1단계 | `T002-P1` | `.\gradlew.bat build` | 종료 코드 0. 서비스 통합 테스트 `ViewOrderIntegrationTest`, `ListOrdersIntegrationTest`, `UnauthenticatedViewIntegrationTest`가 통과한다(시나리오 1.1~1.5, 2.1~2.6, 3.1). 001의 테스트도 그대로 통과한다 |
| 1단계 | `T002-P1` | 단계 표 검사 | 단계가 `T002-P1`인 줄마다 `@DisplayName`이 그 번호로 시작하는 테스트가 있다 |
| 1단계 | `T002-P1` | PR의 GitHub Actions | 성공 |
| 2단계 | `T002-P2` | `.\gradlew.bat build` | 종료 코드 0. `AdminOrderSearchIntegrationTest`가 통과한다(시나리오 4.1~4.4). `RealmRoleJwtConverterTest`와 1단계 테스트도 통과한다 |
| 2단계 | `T002-P2` | PR의 GitHub Actions | 성공 |

- 시나리오 16개의 합계는 1단계 12개, 2단계 4개다. 둘이 합쳐 16개가 모두 통과해야 spec의 첫째 성공 기준을 채운다.
- PR 개수가 다르게 정해지면 이 표의 줄을 새 경계에 맞춰 고친다.

## 4. 테스트 하나만 돌리기

```powershell
.\gradlew.bat :services:order-service:test --tests "com.example.msa.order.integration.ViewOrderIntegrationTest"
```

Gradle의 `--tests`는 클래스와 메서드 이름으로 고른다. `@DisplayName`으로는 고르지 못한다.

## 5. 인수 시나리오 16개를 하나씩 대조하는 방법

완료를 말하기 전에 spec의 시나리오와 테스트를 하나씩 대조한다(저장소 루트 `CLAUDE.md` 6절).

1. `docs/test-cases/order-placement.md`에서 해당 절의 Given/When/Then을 읽는다.
2. 테스트의 `@DisplayName`이 그 절의 번호와 제목으로 시작하는지 본다.
3. 테스트가 확인하는 값(상태 코드, 필드 값, 개수)이 그 절의 Then과 같은지 본다. 문서에 없는 값을 기대값으로 쓴 테스트가 있으면 research.md 3절의 후보 5, 6(아직 문서에 없다)과 대조한다.
4. 테스트가 실패하는 것을 먼저 본다. 구현 전에 쓴 테스트는 단언 실패로 끝나야 한다. 컴파일 오류로 끝나면 안 된다(001의 `IntegrationTestSupport`가 응답을 글자(JSON)로 읽는 이유다).

## 6. (선택) 로컬에서 앱을 직접 띄워 보기 `[제안]`

이 티켓의 인수 기준은 위 테스트다. 앱을 직접 띄워 보는 것은 선택 사항이다.

1. `.\tools\infra-up.ps1`로 로컬 인프라(Oracle, Keycloak)를 띄운다(`docs/references/docker-desktop.md` 3-4절).
2. `.env`의 접속 값을 환경변수로 넣고 `.\gradlew.bat :services:order-service:bootRun`을 실행한다.
3. Keycloak에서 받은 토큰으로 조회를 보낸다. 토큰을 받는 방법과 `ADMIN` 계정(`m1`)으로 관리자 조회를 확인하는 일은 티켓 003의 E2E에서 한다. 이 plan 단계에서는 실행해 보지 않았다.

```text
curl.exe -H "Authorization: Bearer <토큰>" "http://localhost:8080/orders?page=0&size=20"
curl.exe -H "Authorization: Bearer <토큰>" "http://localhost:8080/admin/orders?status=FAILED"
```

기대 결과는 첫째가 200과 `orders` 배열, 둘째가 `ADMIN` 토큰이면 200과 각 주문의 `releaseStatus`, 아니면 403이다.

## 용어

- **Gradle wrapper (`gradlew`, `gradlew.bat`)**: 저장소에 정해 둔 Gradle 버전을 내려받아 실행하는 스크립트다. PC에 Gradle을 설치하지 않아도 같은 버전으로 빌드한다.
- **`build` 작업**: Gradle에서 컴파일, 검사, 테스트, 패키징을 모두 돌리는 작업이다. 이 저장소에서는 기계 검사도 여기에 묶는다.
- **`msa.stage`**: 지금 PR이 어느 단계인지 적어 두는 Gradle 속성(`gradle.properties`)이다. 테스트 케이스 ID 검사가 이 값으로 검사 범위를 정한다.
- **`bootRun`**: Spring Boot 앱을 Gradle에서 바로 띄우는 작업이다. jar를 만들지 않고 실행한다.
- **GitHub Actions**: GitHub가 제공하는 CI 실행 환경이다. PR마다 빌드와 테스트를 돌린다.
- **인수 시나리오 (acceptance scenario)**: 기능이 완성됐다고 인정하려면 통과해야 하는 시험 상황이다. `docs/test-cases/`의 테스트 케이스가 인수 시나리오다.
