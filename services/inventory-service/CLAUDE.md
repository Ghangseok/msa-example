# 재고 서비스 (inventory-service) 규칙

이 문서는 재고 서비스에만 해당하는 규칙을 적는다. 모든 서비스에 공통인 규칙은 저장소 루트의 `CLAUDE.md`, `.specify/memory/constitution.md`, `docs/standards/`에 있다.

## 하는 일

재고 서비스는 주문 서비스만 부르는 내부 API 두 개를 제공한다. 클러스터 밖에는 열지 않는다.

- `PUT /reservations/{orderNo}`: 주문 항목 전체의 재고를 한꺼번에 예약한다.
- `DELETE /reservations/{orderNo}`: 예약을 해제하고, 줄였던 재고를 되돌린다.

주문 번호(`orderNo`)가 멱등 키다. 같은 주문 번호로 여러 번 불러도 재고는 한 번만 바뀐다. API의 기준은 저장소 루트 `contracts/inventory-api.yaml`이다. 이 서비스는 다른 서비스를 부르지 않는다.

## 패키지 구조

```text
services/inventory-service/src/main/java/com/example/msa/inventory/
├── InventoryServiceApplication.java
├── controller/   # ReservationController. 요청 형식 검사와 응답 변환만 한다
├── service/      # ReservationService(흐름, 고유 제약 충돌 시 한 번 다시), ReservationTxService(트랜잭션)
├── mapper/       # StockMapper, ReservationMapper
├── domain/       # ReservationStatus, ReservationReason, ReservationDecision. Spring과 MyBatis를 모른다
├── dto/          # ReserveRequest, ReservationResponse, ReleaseResponse, StockRow, ReservationWithItems
├── exception/    # Problem Details 변환
└── config/       # 관측 설정(ObservabilityConfig)

services/inventory-service/src/main/resources/
├── application.yml
├── logback-spring.xml
├── mapper/StockMapper.xml, mapper/ReservationMapper.xml   # SQL은 여기에만 쓴다
└── db/migration/V1__inventory.sql, V2__seed_stock.sql     # Flyway
```

`client` 패키지는 없다. 다른 서비스를 부르지 않기 때문이다. 계층 사이의 의존 방향은 `docs/standards/coding-conventions.md` 2절 "패키지 구조"를 따르고, ArchUnit 테스트가 검사한다.

## DB

- 이 서비스는 `inventory-db` 컨테이너의 `INVENTORY_SVC` 계정만 쓴다. 다른 서비스의 DB에는 접속하지 않는다.
- 테이블은 세 개다.
  - `STOCK`: 상품별 남은 수량
  - `RESERVATIONS`: 주문 번호 하나에 예약 하나
  - `RESERVATION_ITEMS`: 예약에 들어 있는 상품과 수량
- 스키마는 Flyway 마이그레이션으로만 바꾼다. 이미 적용된 파일은 고치지 않고 새 파일을 더한다.
- SQL은 Oracle 19c에서 도는 것만 쓴다. Gradle 빌드의 기계 검사가 금지 키워드를 찾는다.

## 지킬 규칙

1. **재고 행은 언제나 상품 ID 순서로 잠근다.** 두 주문이 같은 두 상품을 서로 다른 순서로 잠그면, 각자 상대가 잡은 행을 기다리며 둘 다 멈춘다(교착). 모든 주문이 같은 순서로 잠그면 교착이 생기지 않는다.
2. **`RESERVATION_ITEMS`는 `STOCK`으로 외래 키를 두지 않는다.** 재고 데이터에 없는 상품도 항목으로 남겨야 한다. 그래야 같은 주문 번호로 다시 요청이 왔을 때, 처음과 같은 "없는 상품 목록"을 돌려줄 수 있다.
3. **예약과 해제는 각각 한 트랜잭션이다.** 같은 주문 번호가 동시에 들어와 기본 키에 걸리면, 전체를 되돌리고 처음부터 한 번 다시 한다.
4. **SQL은 Mapper XML에만 쓴다.** 애너테이션 SQL을 쓰지 않는다. 값은 `#{}`로만 넣고, `SELECT *`를 쓰지 않는다.
5. **응답은 계약을 따른다.** 업무 결과(예약됨, 거절됨, 해제됨)는 처음 요청이든 재요청이든 언제나 200이다. 오류는 Problem Details(`application/problem+json`)로 낸다.

## 테스트

- Mapper 테스트와 서비스 통합 테스트는 Testcontainers로 띄운 실제 Oracle(`OracleTestContainer`) 하나를 함께 쓴다. H2 같은 대체 DB를 쓰지 않는다.
- 테스트는 자기가 쓸 데이터를 직접 넣고 끝나면 지운다. 상품 ID는 테스트 케이스 문서의 `A`, `B`, `Z`를 쓴다. Flyway 초기 데이터(`P-001`~`P-005`)는 테스트에서 쓰지 않는다.
- 인수 시나리오를 검증하는 테스트의 `@DisplayName`은 테스트 케이스 번호로 시작한다. 기대값은 `docs/test-cases/order-placement.md`에서 그대로 가져온다.
- 테스트 설정은 `application-test.yml`이다. 테스트에서는 OTLP 내보내기를 끈다.

## 용어

- **멱등 키 (idempotency key)**: 받는 쪽이 같은 요청인지 알아보는 값이다. 이 서비스에서는 주문 번호다.
- **교착 (deadlock)**: 두 작업이 서로 상대가 잡은 잠금을 기다리며 둘 다 멈추는 상태다. 재고 행을 상품 ID 순서로 잠가서 막는다.
- **해제 표식 (release marker)**: 예약 기록이 없는데 해제 요청이 먼저 왔을 때 남기는 "해제됨" 기록이다. 늦게 온 예약 요청이 재고를 줄이지 못하게 막는다.
- **Flyway**: DB 스키마와 초기 데이터를 버전 번호가 붙은 SQL 파일로 관리하고 차례로 적용하는 도구다.
