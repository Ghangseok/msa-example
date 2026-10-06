# Quickstart: 주문 생성 검증

**기능 폴더**: `specs/001-place-order` | **작성일**: 2026-10-05

이 문서는 단계별 PR이 끝났을 때 기능이 맞게 도는지 확인하는 방법을 적는다. 구현 코드는 담지 않는다. 테이블은 [data-model.md](data-model.md), API는 [contracts/](contracts/), 어느 인수 시나리오를 어느 테스트가 확인하는지는 [plan.md](plan.md)의 "인수 시나리오와 테스트 층" 표에 있다.

## 1. 준비

| 준비할 것 | 확인 명령 | 기대 결과 |
|---|---|---|
| JDK 17 | `java -version` | `17.0.2` |
| Docker Desktop이 떠 있다 | `docker info` | 종료 코드 0 |
| Oracle 이미지 (처음 한 번 받는다) | `docker pull gvenzl/oracle-xe:21.3.0-slim-faststart` | 종료 코드 0 |

- 001의 테스트에는 로컬 실행 인프라(`infra/`, `tools/`)가 필요 없다. Testcontainers가 테스트마다 Oracle 컨테이너를 직접 띄운다.
- 로컬 인프라가 떠 있어서 메모리가 모자라면, `.\tools\infra-down.ps1`로 내릴지 사용자에게 먼저 묻는다. 내려도 Oracle 데이터는 볼륨에 남는다.
- Docker나 Testcontainers에 문제가 생기면 `docs/references/docker-desktop.md`부터 본다.

## 2. 빌드와 모든 검사

PowerShell에서는 `.\gradlew.bat`, Git Bash와 CI에서는 `./gradlew`를 쓴다.

```powershell
.\gradlew.bat build
```

`build`는 아래를 모두 돌린다.

- 컴파일과 Spotless 포맷 검사
- SpotBugs
- 기계 검사 작업: Lombok 의존성, 검사를 끄는 표시, Mapper XML, 19c 이후 키워드, 테스트 케이스 ID
- 단위, Mapper, 서비스 통합, 계약, ArchUnit 테스트

기대 결과는 종료 코드 0이다. 실패한 검사의 보고서는 각 서비스의 `build/reports/` 아래에 있다.

보고에는 실행한 명령과 결과를 함께 적는다(저장소 루트 `CLAUDE.md` 6절). 예:

```text
확인: .\gradlew.bat build → 종료 코드 0, 테스트 N개 통과, 실패 0
```

## 3. 단계별로 확인할 것

지금 단계는 `gradle.properties`의 `msa.stage`에 적혀 있다. 테스트 케이스 ID 검사는 `config/quality/test-case-stages.csv`에서 단계가 지금 단계 이하인 줄만 본다.

| 단계 | `msa.stage` | 확인할 것 | 기대 결과 |
|---|---|---|---|
| P1 | `P1` | `.\gradlew.bat build`, `.\gradlew.bat :services:inventory-service:test` | 둘 다 종료 코드 0. 재고 서비스의 인수 시나리오 테스트(spec 시나리오 1.1, 1.2, 1.3, 1.4, 1.5, 시나리오 2.1·3.1·3.3·6.1·8.1·9.1의 재고 쪽, 테스트 케이스 추가 PR의 재고 케이스 세 개)가 모두 통과한다 |
| P1 | `P1` | 루트 `contracts/inventory-api.yaml`과 `specs/001-place-order/contracts/inventory-api.yaml`의 내용 비교(`git diff --no-index`) | 차이 없음 |
| P1 | `P1` | PR의 GitHub Actions | 성공 |
| P2 | `P2` | `.\gradlew.bat build` | 종료 코드 0. 주문 서비스의 인수 시나리오 테스트(spec 시나리오 2.1, 3.1, 3.2, 3.3, 4.1, 5.1, 6.1, 7.1, 7.2, 테스트 케이스 추가 PR의 P2 주문 케이스 네 개)가 통과한다. 재고 쪽 테스트는 P1에서 이미 통과했고 이 단계에서 바뀌지 않는다 |
| P3 | `P3` | `.\gradlew.bat build` | 종료 코드 0. spec 시나리오 8.1, 9.1의 주문 쪽, 테스트 케이스 추가 PR의 주문 케이스 두 개가 통과하고, P1·P2의 테스트도 그대로 통과한다 |

## 4. 테스트 하나만 돌리기

```powershell
.\gradlew.bat :services:inventory-service:test --tests "com.example.msa.inventory.integration.*"
```

Gradle의 `--tests`는 클래스와 메서드 이름으로 고른다. `@DisplayName`으로는 고르지 못한다. 테스트 클래스 이름은 tasks.md에서 정한다.

## 5. (선택) 로컬에서 앱을 직접 띄워 보기

001의 인수 기준은 위 테스트다. 앱을 직접 띄워 보는 것은 확인을 돕는 선택 사항이다 `[제안]`.

1. `.\tools\infra-up.ps1`로 로컬 인프라를 띄운다(`docs/references/docker-desktop.md` 3-4절).
2. `.env`의 DB 접속 값을 환경변수로 넣고 `.\gradlew.bat :services:inventory-service:bootRun`을 실행한다.
3. `curl.exe -X PUT http://localhost:8080/reservations/1 -H "Content-Type: application/json" -d "{\"items\":[{\"productId\":\"P-001\",\"quantity\":1}]}"`를 보낸다. 기대 결과는 200과 `"status":"RESERVED"`다.

주문 서비스를 띄우려면 Keycloak 토큰과 재고 서비스 주소가 필요하다. 이 확인은 티켓 003의 E2E에서 한다.

## 용어

- **Gradle wrapper (`gradlew`, `gradlew.bat`)**: 저장소에 정해 둔 Gradle 버전을 내려받아 실행하는 스크립트다. PC에 Gradle을 설치하지 않아도 같은 버전으로 빌드한다.
- **`build` 작업**: Gradle에서 컴파일, 검사, 테스트, 패키징을 모두 돌리는 작업이다. 이 저장소에서는 기계 검사도 여기에 묶는다.
- **`bootRun`**: Spring Boot 앱을 Gradle에서 바로 띄우는 작업이다. jar를 만들지 않고 실행한다.
- **`msa.stage`**: 지금 PR이 어느 단계(P1, P2, P3)인지 적어 두는 Gradle 속성이다. 테스트 케이스 ID 검사가 이 값으로 검사 범위를 정한다.
- **GitHub Actions**: GitHub가 제공하는 CI 실행 환경이다. PR마다 빌드와 테스트를 돌린다.
