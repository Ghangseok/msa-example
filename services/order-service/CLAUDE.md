# 주문 서비스 (order-service) 규칙

이 문서는 주문 서비스에만 해당하는 규칙을 적는다. 모든 서비스에 공통인 규칙은 저장소 루트의 `CLAUDE.md`, `.specify/memory/constitution.md`, `docs/standards/`에 있다.

## 하는 일

주문 서비스는 고객이 부르는 외부 API를 제공하고, 재고는 재고 서비스의 내부 API로 예약한다.

- `POST /orders`: 로그인한 고객의 주문을 "처리중"으로 기록하고, 재고를 예약해서 결과(확정, 거절, 실패)를 돌려준다.
- `GET /orders`, `GET /orders/{orderNo}`: 로그인한 고객 본인의 주문 목록과 주문 하나를 돌려준다. 목록은 주문 번호가 큰 순서이고 `page`, `size`로 쪽을 나눈다. 다른 고객의 주문, 없는 주문, 숫자가 아닌 주문 번호는 모두 같은 404(`ORDER_NOT_FOUND`)다.

API의 기준은 `specs/001-place-order/contracts/order-api.yaml`(주문 생성)과 `specs/002-view-orders/contracts/order-query-api.yaml`(주문 조회)이다. 재고 서비스 API의 기준은 저장소 루트 `contracts/inventory-api.yaml`이다. 이 서비스는 재고 서비스를 `InventoryClient`로만 부른다.

## 패키지 구조

```text
services/order-service/src/main/java/com/example/msa/order/
├── OrderServiceApplication.java
├── controller/   # OrderController(주문 생성), OrderQueryController(주문 조회). 요청 형식 검사와 응답 변환만 한다
├── service/      # OrderService(트랜잭션 없음, 흐름), OrderTxService(트랜잭션), ReservationReleaser(해제를 따로 돌림), OrderQueryService(읽기 전용 조회)
├── mapper/       # OrderMapper
├── client/       # InventoryClient. 재고 서비스를 부르는 유일한 곳이다
├── domain/       # OrderStatus, OrderReason, ReleaseStatus, OrderItems, OrderNumbers, PageRequest, InvalidQueryException. Spring과 MyBatis를 모른다
├── dto/          # PlaceOrderRequest, OrderItemRequest, OrderResponse, OrderWithItems, OrderDetailResponse, OrderPage
├── exception/    # Problem Details 변환(조회의 404·400 포함), 401 처리
└── config/       # SecurityConfig, InventoryClientConfig, ReleaseExecutorConfig, ObservabilityConfig, 설정 클래스

services/order-service/src/main/resources/
├── application.yml
├── logback-spring.xml
├── mapper/OrderMapper.xml        # SQL은 여기에만 쓴다
└── db/migration/V1__orders.sql   # Flyway
```

계층 사이의 의존 방향은 `docs/standards/coding-conventions.md` 2절 "패키지 구조"를 따르고, ArchUnit 테스트가 검사한다.

## DB

- 이 서비스는 `order-db` 컨테이너의 `ORDER_SVC` 계정만 쓴다. 다른 서비스의 DB에는 접속하지 않는다.
- 테이블은 두 개이고, 주문 번호는 시퀀스 `ORDERS_SEQ`가 만든다.
  - `ORDERS`: 주문 하나에 한 줄. 같은 고객의 같은 요청 키는 한 줄뿐이다(`UK_ORDERS_CUSTOMER_KEY`).
  - `ORDER_ITEMS`: 주문에 들어 있는 상품과 수량. 거절된 주문에서는 항목마다 거절 사유가 들어간다.
- 스키마는 Flyway 마이그레이션으로만 바꾼다. 이미 적용된 파일은 고치지 않고 새 파일을 더한다.
- SQL은 Oracle 19c에서 도는 것만 쓴다. Gradle 빌드의 기계 검사가 금지 키워드를 찾는다.

## 지킬 규칙

1. **`OrderService.place()`에는 트랜잭션이 없다.** 저장은 `OrderTxService`의 메서드마다 따로 커밋한다. 처리중으로 저장(트랜잭션 1) → 재고 예약 호출(트랜잭션 밖) → 결과 저장(트랜잭션 2) 순서다. 원격 호출을 기다리는 동안 DB 연결과 잠금을 잡고 있지 않게 하려는 것이다. `@Transactional`이 붙은 코드는 `client` 패키지를 직접 부르지 않는다. ArchUnit 테스트가 검사한다.
2. **고객 ID는 JWT의 `sub`에서만 얻는다.** 요청 본문이나 헤더로 받은 사용자 정보는 믿지 않는다. 토큰 전체는 로그에 남기지 않는다.
3. **재고는 `InventoryClient`로만 부른다.** `RestClient`는 Spring Boot가 주는 `RestClient.Builder`로 만든다. `RestClient.builder()`를 직접 부르면 추적 정보(`traceparent`)가 붙지 않는다.
4. **재고 호출의 제한 시간, 서킷 브레이커 값, 주소는 코드에 적지 않는다.** `application.yml`에 두고 `@ConfigurationProperties`로 받는다.
5. **실패한 주문의 해제는 고객 응답 뒤에 따로 돌린다.** `ReservationReleaser`가 `releaseExecutor`에서 `InventoryClient.release()`를 부르고, 결과를 새 트랜잭션으로 저장한다. 해제도 예약과 같은 재시도 정책(일시 오류에 즉시, 1, 2, 4, 8초 뒤 최대 5번, 재시도 포함 전체 30초)을 따른다.
6. **SQL은 Mapper XML에만 쓴다.** 애너테이션 SQL을 쓰지 않는다. 값은 `#{}`로만 넣고, `SELECT *`를 쓰지 않는다.
7. **오류 응답은 Problem Details(`application/problem+json`)로 낸다.** `type`은 `urn:msa-example:problem:<코드>`이고, 확장 필드는 `code`와 `errors`다. 401도 같다.

## 테스트

- Mapper 테스트와 서비스 통합 테스트는 Testcontainers로 띄운 실제 Oracle(`OracleTestContainer`) 하나를 함께 쓴다. H2 같은 대체 DB를 쓰지 않는다.
- 재고 서비스 자리에는 WireMock(`InventoryWireMock`)을 둔다. 고객은 Spring Security Test의 `jwt()`로 흉내 낸다.
- 테스트 설정은 `application-test.yml`이다. 시간 값을 1/5로 줄이고, OTLP 내보내기를 끈다.
- 인수 시나리오를 검증하는 테스트의 `@DisplayName`은 테스트 케이스 번호로 시작한다. 기대값은 `docs/test-cases/order-placement.md`에서 그대로 가져온다.
- 테스트는 자기가 쓸 데이터를 직접 넣고 끝나면 지운다.

## 용어

- **멱등 키 (idempotency key)**: 받는 쪽이 같은 요청인지 알아보는 값이다. 이 서비스에서 주문 생성의 멱등 키는 고객 ID와 주문 요청 키의 조합이고, 재고 예약의 멱등 키는 주문 번호다.
- **서킷 브레이커 (circuit breaker)**: 상대 서비스 호출이 계속 실패하면 한동안 호출을 멈추고 바로 실패를 돌려주는 장치다. 재고 호출에 하나 둔다.
- **해제 실행기 (release executor)**: 실패한 주문의 예약 해제를 고객 응답과 따로 돌리는 스레드 풀이다.
- **Problem Details**: HTTP API의 오류 응답을 JSON으로 적는 표준 형식(RFC 9457)이다.
- **Flyway**: DB 스키마를 버전 번호가 붙은 SQL 파일로 관리하고 차례로 적용하는 도구다.
