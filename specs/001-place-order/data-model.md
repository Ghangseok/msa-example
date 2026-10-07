# Data Model: 주문 생성

**기능 폴더**: `specs/001-place-order` | **작성일**: 2026-10-05 | **입력**: [spec.md](spec.md)의 Key Entities, [research.md](research.md)

이 문서는 두 서비스의 DB 테이블, 상태 전이, 검사 규칙, 처리 순서를 적는다.

- 출발점은 설계 문서(`docs/design/architecture.md`) 6절 "데이터"의 제안이다. plan에서 정한 것은 이 문서가 기준이다.
- 설계 6절과 달라진 곳은 두 가지다. `REQUEST_HASH`를 뺐고(research.md 결정 14), 항목 줄에 결과 열을 더했다(research.md 4절). 설계 문서는 2026-10-07에 이 문서에 맞게 고쳤다(PR #18).
- SQL은 19c에서 도는 것만 쓴다(코딩 규약 3-3절). 이름은 대문자 스네이크 케이스다.

## 1. DB와 계정

| 서비스 | DB 컨테이너 | 계정 (PDB `XEPDB1` 안) | 아는 테이블 |
|---|---|---|---|
| 주문 서비스 | `order-db` | `ORDER_SVC` | `ORDERS`, `ORDER_ITEMS`, 시퀀스 `ORDERS_SEQ` |
| 재고 서비스 | `inventory-db` | `INVENTORY_SVC` | `STOCK`, `RESERVATIONS`, `RESERVATION_ITEMS` |

두 서비스는 서로의 DB에 접속하지 않는다. 주문 서비스는 재고 수량을 재고 API로만 다룬다. 테스트에서는 서비스마다 Testcontainers로 띄운 `gvenzl/oracle-xe:21.3.0-slim-faststart` 컨테이너 하나를 쓴다.

## 2. 주문 DB (`ORDER_SVC`)

Flyway 파일: `services/order-service/src/main/resources/db/migration/V1__orders.sql` (P2 단계 PR)

### 2-1. `ORDERS_SEQ`

주문 번호를 만드는 시퀀스다. `START WITH 1 INCREMENT BY 1 NOCACHE`로 만든다 `[제안]`. 주문 서비스 인스턴스가 2개일 때 번호에 빈칸이 생겨도 문제는 없다. 그래도 테스트에서 번호를 읽기 쉽게 캐시를 쓰지 않는다.

### 2-2. `ORDERS`

| 열 | 타입 | NULL | 뜻 |
|---|---|---|---|
| `ORDER_NO` | `NUMBER(19)` | 아니오 | 주문 번호. `ORDERS_SEQ`에서 받는다. 재고 예약의 멱등 키다 |
| `CUSTOMER_ID` | `VARCHAR2(255)` | 아니오 | 고객 ID. 검증한 JWT의 `sub` 값이다 |
| `REQUEST_KEY` | `VARCHAR2(36)` | 아니오 | 주문 요청 키(UUID 문자열) |
| `STATUS` | `VARCHAR2(20)` | 아니오 | `PENDING`, `CONFIRMED`, `REJECTED`, `FAILED` |
| `REASON` | `VARCHAR2(30)` | 예 | `OUT_OF_STOCK`, `PRODUCT_NOT_FOUND`, `RETRY_LATER`. 처리중과 확정이면 비어 있다 |
| `RELEASE_STATUS` | `VARCHAR2(20)` | 예 | `NOT_REQUIRED`, `RELEASED`, `RELEASE_FAILED`. "실패"가 아니거나 해제 요청 전이면 비어 있다 |
| `CREATED_AT` | `TIMESTAMP` | 아니오 | 처음 기록한 시각 |
| `UPDATED_AT` | `TIMESTAMP` | 아니오 | 마지막으로 바꾼 시각 |

제약:

- `PK_ORDERS` 기본 키 (`ORDER_NO`)
- `UK_ORDERS_CUSTOMER_KEY` 고유 제약 (`CUSTOMER_ID`, `REQUEST_KEY`). 같은 고객의 같은 키는 주문 하나다. 다른 고객의 같은 키는 별개다.
- `CK_ORDERS_STATUS`, `CK_ORDERS_REASON`, `CK_ORDERS_RELEASE_STATUS` CHECK 제약으로 값 목록을 막는다 `[제안]`.

### 2-3. `ORDER_ITEMS`

| 열 | 타입 | NULL | 뜻 |
|---|---|---|---|
| `ORDER_NO` | `NUMBER(19)` | 아니오 | 주문 번호 |
| `PRODUCT_ID` | `VARCHAR2(20)` | 아니오 | 상품 ID(`^[A-Z0-9-]{1,20}$`) |
| `QUANTITY` | `NUMBER(2)` | 아니오 | 수량 1~99 |
| `REJECT_REASON` | `VARCHAR2(20)` | 예 | 거절된 주문에서 이 상품이 부족했으면 `OUT_OF_STOCK`, 없었으면 `PRODUCT_NOT_FOUND`. 그 밖에는 비어 있다 `[제안]` |

제약:

- `PK_ORDER_ITEMS` 기본 키 (`ORDER_NO`, `PRODUCT_ID`). 같은 상품은 한 주문에 한 줄만 들어간다.
- `FK_ORDER_ITEMS_ORDERS` 외래 키 (`ORDER_NO`) → `ORDERS`
- `CK_ORDER_ITEMS_QTY` CHECK (`QUANTITY BETWEEN 1 AND 99`)

주문 응답의 `shortageProductIds`와 `missingProductIds`는 `REJECT_REASON`에서 만든다. 그래서 같은 키로 다시 보내도 처음과 같은 목록이 나온다.

## 3. 재고 DB (`INVENTORY_SVC`)

Flyway 파일: `services/inventory-service/src/main/resources/db/migration/V1__inventory.sql`, `V2__seed_stock.sql` (P1 단계 PR)

### 3-1. `STOCK`

| 열 | 타입 | NULL | 뜻 |
|---|---|---|---|
| `PRODUCT_ID` | `VARCHAR2(20)` | 아니오 | 상품 ID |
| `QUANTITY` | `NUMBER(10)` | 아니오 | 남은 수량 |

제약:

- `PK_STOCK` 기본 키 (`PRODUCT_ID`)
- `CK_STOCK_QTY` CHECK (`QUANTITY >= 0`). 수량을 줄이는 코드에 실수가 있어도 음수가 저장되지 않게 하는 마지막 방어선이다. 아래 업무 규칙을 지킨다.

> **BR-003** · `docs/requirements/uc-001-place-order.md` 6절 "업무 규칙"
> 재고 수량은 0보다 작아지지 않는다. 동시에 들어온 주문이 같은 재고를 두고 다퉈도 마찬가지다

초기 데이터 `V2__seed_stock.sql`에는 `P-001`부터 `P-005`까지 각 100개를 넣는다 `[제안]`. 테스트는 이 행을 쓰지 않고, 테스트 케이스의 상품 ID(`A`, `B`, `Z`)를 직접 넣고 지운다.

### 3-2. `RESERVATIONS`

| 열 | 타입 | NULL | 뜻 |
|---|---|---|---|
| `ORDER_NO` | `NUMBER(19)` | 아니오 | 주문 번호(멱등 키) |
| `STATUS` | `VARCHAR2(20)` | 아니오 | `RESERVED`, `REJECTED`, `RELEASED` |
| `REASON` | `VARCHAR2(30)` | 예 | 거절이면 `OUT_OF_STOCK` 또는 `PRODUCT_NOT_FOUND` |
| `CREATED_AT` | `TIMESTAMP` | 아니오 | 처음 기록한 시각 |
| `UPDATED_AT` | `TIMESTAMP` | 아니오 | 마지막으로 바꾼 시각 |

제약:

- `PK_RESERVATIONS` 기본 키 (`ORDER_NO`). 같은 주문 번호의 예약은 한 줄뿐이다. 같은 번호가 동시에 들어와도 한쪽만 들어간다.
- `CK_RESERVATIONS_STATUS`, `CK_RESERVATIONS_REASON` CHECK 제약 `[제안]`.

해제 표식은 `STATUS = 'RELEASED'`이고 `RESERVATION_ITEMS`에 줄이 없는 예약 기록이다. 이번 단계에서는 예약 기록을 지우지 않는다.

### 3-3. `RESERVATION_ITEMS`

| 열 | 타입 | NULL | 뜻 |
|---|---|---|---|
| `ORDER_NO` | `NUMBER(19)` | 아니오 | 주문 번호 |
| `PRODUCT_ID` | `VARCHAR2(20)` | 아니오 | 상품 ID. `STOCK`에 없는 상품도 들어간다(없는 상품 목록을 남기려고) |
| `QUANTITY` | `NUMBER(2)` | 아니오 | 요청 수량 |
| `RESULT` | `VARCHAR2(20)` | 예 | 거절된 예약에서 부족했으면 `OUT_OF_STOCK`, 없었으면 `PRODUCT_NOT_FOUND` `[제안]` |

제약:

- `PK_RESERVATION_ITEMS` 기본 키 (`ORDER_NO`, `PRODUCT_ID`)
- `FK_RESERVATION_ITEMS_RES` 외래 키 (`ORDER_NO`) → `RESERVATIONS`

`STOCK`으로 가는 외래 키는 두지 않는다. 없는 상품의 줄도 남겨야 하기 때문이다.

## 4. 상태 전이

### 4-1. 주문 상태 (`ORDERS.STATUS`)

| 지금 상태 | 다음 상태 | 조건 |
|---|---|---|
| (없음) | `PENDING` | 고객이 확인되고 항목과 키가 올바른 새 요청 |
| `PENDING` | `CONFIRMED` | 예약 결과 `RESERVED` |
| `PENDING` | `REJECTED` | 예약 결과 `REJECTED` (사유 `OUT_OF_STOCK` 또는 `PRODUCT_NOT_FOUND`) |
| `PENDING` | `FAILED` | 서킷이 열려 있음, 재고 서비스가 409·500을 돌려줌, 일시 오류(P2에서는 한 번, P3에서는 재시도 뒤 30초 한도), 예약 결과 `RELEASED`(일어나지 않아야 하지만 받으면 실패로 둔다 `[제안]`) |

`CONFIRMED`, `REJECTED`, `FAILED`에서 다른 상태로 가지 않는다. 이 규칙은 `domain` 패키지의 `OrderStatus.canMoveTo(...)`에 둔다(코딩 규약 3-1절).

### 4-2. 재고 해제 결과 (`ORDERS.RELEASE_STATUS`)

"실패" 주문에만 값이 들어간다.

| 상황 | 값 |
|---|---|
| 서킷이 처음부터 열려 있어 예약 요청을 한 번도 보내지 않음 | `NOT_REQUIRED` (실패로 바꾸는 저장에서 함께 넣는다) |
| 예약 요청을 보낸 뒤 실패. 해제 요청 전 | 비어 있음 |
| 해제 요청의 결과가 `RELEASED` 또는 `REJECTED` | `RELEASED` (재고가 줄어든 채 남지 않았다) |
| 해제 요청이 끝내 실패(서킷, 일시 오류, 409·500) | `RELEASE_FAILED` (오류 로그도 남긴다) |

### 4-3. 예약 상태 (`RESERVATIONS.STATUS`)

| 지금 상태 | 요청 | 다음 상태와 수량 변화 |
|---|---|---|
| (없음) | 예약, 모든 항목 충분 | `RESERVED`, 항목마다 수량 감소 |
| (없음) | 예약, 부족하거나 없는 상품 있음 | `REJECTED`, 수량 그대로 |
| (없음) | 해제 | `RELEASED` (해제 표식), 수량 그대로 |
| `RESERVED` | 해제 | `RELEASED`, 항목마다 수량 복구 |
| `RESERVED`, `REJECTED`, `RELEASED` | 같은 내용의 예약 재요청 | 그대로, 저장된 결과를 돌려준다 |
| `RESERVED`, `REJECTED` | 다른 내용의 예약 요청 | 그대로, 409 |
| `RELEASED` (해제 표식, 항목 없음) | 예약 요청 | 그대로, `RELEASED`를 돌려준다(내용 비교 없음) |
| `RELEASED` (해제된 예약, 항목 있음) | 예약 요청 | 같은 내용이면 `RELEASED`, 다른 내용이면 409 |
| `REJECTED`, `RELEASED` | 해제 | 그대로, 지금 상태를 돌려준다 |

## 5. 검사 규칙

### 5-1. 주문 요청 (주문 서비스 `controller`와 `domain`)

| 검사 | 실패하면 | 어디서 |
|---|---|---|
| 토큰이 있고 서명·만료·발급자(`http://localhost:8180/realms/msa`)가 맞다 | 401 | Spring Security 리소스 서버 |
| `Idempotency-Key` 헤더가 있고 UUID 문자열(36자)이다 | 400 | controller |
| 주문 항목이 1개 이상 20개 이하, 수량 1~99, 같은 상품은 한 줄 | 400 | `domain`의 `OrderItems.validate(...)` |
| 상품 ID가 `^[A-Z0-9-]{1,20}$`이고 수량이 정수다 | 400 | controller의 Bean Validation과 `domain` |

주문 항목 규칙의 출처는 아래 업무 규칙이다.

> **BR-009** · `docs/requirements/uc-001-place-order.md` 6절 "업무 규칙"
> 주문 항목은 1개 이상 20개 이하이고, 각 수량은 1 이상 99 이하이며, 같은 상품은 한 줄에만 나온다

검사에 실패한 요청은 주문을 기록하지 않고 재고 서비스에도 보내지 않는다. 키도 저장하지 않으므로, 같은 키로 고쳐 보낸 요청은 새 주문이 된다(spec의 받아들인 제안 7번).

### 5-2. 예약 요청 (재고 서비스 `controller`)

| 검사 | 실패하면 |
|---|---|
| 경로의 주문 번호가 양의 정수다 | 400 |
| 항목이 1개 이상 20개 이하, 수량 1~99, 같은 상품은 한 줄, 상품 ID 형식 | 400 |

재고 서비스도 같은 규칙으로 검사한다. 주문 서비스만 부르는 내부 API이지만, 계약에 적은 형식을 서비스가 직접 지키게 하려는 것이다 `[제안]`.

## 6. 처리 순서

### 6-1. 주문 생성 (주문 서비스)

트랜잭션과 원격 호출을 나눈다(코딩 규약 3-1절). `OrderService.place()`에는 `@Transactional`이 없다.

1. controller가 토큰의 `sub`, `Idempotency-Key`, 항목을 받아 검사한다(5-1절).
2. `OrderTxService.savePending()`(트랜잭션 1)이 `ORDERS`에 `PENDING`을, `ORDER_ITEMS`에 항목을 넣는다.
   - 고유 제약 `UK_ORDERS_CUSTOMER_KEY`에 걸리면 이미 있는 주문을 항목과 함께 읽는다. 다른 요청이 같은 키로 아직 커밋하지 않았으면 Oracle이 그 커밋까지 기다린 뒤 제약 위반을 낸다.
   - 읽은 주문과 항목이 같으면(상품 ID 순으로 정렬해 상품과 수량을 비교, 순서는 보지 않는다) 그 주문의 지금 상태로 200을 돌려준다.
   - 다르면 422를 돌려준다.
3. `InventoryClient.reserve(orderNo, items)`를 트랜잭션 밖에서 부른다. P2는 시도 한 번이고, P3는 재시도와 전체 30초 한도가 붙는다. 서킷이 열려 있으면 그 시도는 요청을 보내지 않고 바로 실패한다.
4. 결과에 따라 `OrderTxService.applyResult()`(트랜잭션 2)로 저장한다.
   - `RESERVED`이면 `CONFIRMED`로 바꾼다.
   - `REJECTED`이면 `REJECTED`와 사유, 항목별 `REJECT_REASON`을 저장한다.
   - 그 밖이면 `FAILED`와 `RETRY_LATER`를 저장한다. 예약 요청을 한 번도 보내지 않았으면 `RELEASE_STATUS`에 `NOT_REQUIRED`도 함께 저장한다.
5. `FAILED`이고 예약 요청을 보낸 적이 있으면, 해제를 `releaseExecutor`에 맡긴다. 그리고 바로 201로 응답한다.
6. 해제 작업이 `InventoryClient.release(orderNo)`를 부른다. P2는 시도 한 번이고, P3는 예약과 같은 재시도 정책이다. 끝나면 `RELEASE_STATUS`를 새 트랜잭션으로 저장한다.

"예약 요청을 보낸 적이 있는가"는 호출마다 만드는 문맥 객체가 센다 `[제안]`. 서킷 브레이커가 호출을 허가해 실제로 요청을 보낸 시도만 센다. 서킷 상태를 미리 따로 물어보지 않는다. 미리 물어보면 반쯤 열린 상태에서 시험 호출 허가를 하나 더 써 버리기 때문이다.

### 6-2. 예약 (재고 서비스, 한 트랜잭션)

설계 6절 "예약 처리"를 따르되, 1번의 비교를 해시 대신 항목 직접 비교로 바꾼다(research.md 결정 14).

1. `RESERVATIONS`에서 주문 번호를 찾는다. 있으면 아래처럼 한다.
   - 해제 표식(항목 없음)이면 `RELEASED`를 돌려준다.
   - 항목이 있으면 `RESERVATION_ITEMS`를 상품 ID 순으로 읽어 들어온 항목과 비교한다. 다르면 409, 같으면 저장된 결과(상태, 사유, `RESULT`로 만든 상품 목록)를 돌려준다.
2. 없으면 항목의 재고 행을 상품 ID 순서로 하나씩 `SELECT PRODUCT_ID, QUANTITY FROM STOCK WHERE PRODUCT_ID = #{productId} FOR UPDATE`로 잠그고 읽는다. 행이 없는 상품은 없는 상품 목록에, 수량이 모자란 상품은 부족한 상품 목록에 모은다.
3. 없는 상품이나 부족한 상품이 하나라도 있으면 아무 수량도 바꾸지 않고 `REJECTED`로 정한다. 사유는 없는 상품이 하나라도 있으면 `PRODUCT_NOT_FOUND`, 아니면 `OUT_OF_STOCK`이다. 그렇지 않으면 항목마다 `UPDATE STOCK SET QUANTITY = QUANTITY - #{quantity} WHERE PRODUCT_ID = #{productId}`로 줄이고 `RESERVED`로 정한다.
4. `RESERVATIONS`와 `RESERVATION_ITEMS`(항목별 `RESULT` 포함)를 넣고 커밋한다. 같은 주문 번호가 동시에 들어와 `PK_RESERVATIONS`에 걸리면 전체를 롤백하고 1번부터 다시 한다. 다시 하는 횟수는 한 번으로 둔다 `[제안]`.

Oracle이 교착을 발견하면(`ORA-00060`) 트랜잭션을 롤백하고 500을 돌려준다. 상품 ID 순서로 잠그므로 교착은 일어나지 않아야 한다.

### 6-3. 해제 (재고 서비스, 한 트랜잭션)

1. `RESERVATIONS`에서 주문 번호를 `SELECT ... FOR UPDATE`로 잠그고 읽는다.
2. 상태에 따라 처리한다.
   - `RESERVED`이면 `RESERVATION_ITEMS`를 상품 ID 순서로 읽어 항목마다 수량을 되돌리고 `RELEASED`로 바꾼다.
   - `REJECTED`나 `RELEASED`이면 그대로 두고 지금 상태를 돌려준다.
   - 없으면 `RELEASED` 표식을 넣는다. `PK_RESERVATIONS`에 걸리면 1번부터 한 번 다시 한다 `[제안]`.

## 7. Java 쪽 모양

| 서비스 | 패키지 | 이름 | 종류 |
|---|---|---|---|
| 주문 | `domain` | `OrderStatus`, `OrderReason`, `ReleaseStatus` | enum (이름 문자열로 저장) |
| 주문 | `domain` | `OrderItems` | 항목 검사와 정렬 비교를 담은 값 객체 |
| 주문 | `dto` | `PlaceOrderRequest`, `OrderItemRequest`, `OrderResponse` | record |
| 주문 | `dto` | `OrderWithItems` | 일반 클래스 (`<collection>`으로 읽는 1:N 부모) |
| 주문 | `client` | `InventoryClient`, `ReservationResult`, `InventoryCallFailure` | 클래스, record, 예외 |
| 주문 | `config` | `InventoryClientProperties`, `ReleaseExecutorProperties` | `@ConfigurationProperties` record |
| 재고 | `domain` | `ReservationStatus`, `ReservationReason`, `ReservationDecision` | enum, 판단 로직 |
| 재고 | `dto` | `ReserveRequest`, `ReservationResponse`, `StockRow` | record |
| 재고 | `dto` | `ReservationWithItems` | 일반 클래스 (`<collection>`으로 읽는 1:N 부모) |

record와 일반 클래스를 나눈 이유는 research.md 결정 9에 있다.

## 용어

- **PDB (Pluggable Database)**: Oracle 컨테이너 DB 안에 들어 있는 독립된 DB 하나다. XE의 기본 PDB 이름은 `XEPDB1`이고, 두 서비스 계정이 각자의 컨테이너에서 이 PDB를 쓴다.
- **기본 키 (primary key)**: 테이블에서 한 줄을 구별하는 열의 조합이다. 같은 값이 두 번 들어갈 수 없다.
- **외래 키 (foreign key)**: 다른 테이블의 기본 키를 가리키는 열이다. 가리키는 줄이 없으면 넣을 수 없다.
- **CHECK 제약 (check constraint)**: 열 값이 정한 조건을 만족해야만 저장되게 하는 DB 규칙이다. 수량 범위와 상태 값 목록을 이것으로 막는다.
- **해제 표식 (release marker)**: 예약 기록이 없는데 해제 요청이 먼저 왔을 때 남기는 "해제됨" 기록이다. 늦게 도착한 예약 요청이 재고를 줄이지 못하게 막는다.
- **`SELECT ... FOR UPDATE`**: 읽은 행을 트랜잭션이 끝날 때까지 잠가 다른 트랜잭션이 바꾸지 못하게 하는 SQL이다. 재고 행과 예약 기록을 이것으로 잠근다.
- **값 객체 (value object)**: 값 자체로 같고 다름을 판단하는 작은 객체다. 주문 항목 목록을 검사하고 비교하는 `OrderItems`가 값 객체다.
- **호출 허가 (permission)**: Resilience4j 서킷 브레이커가 호출을 보내도 되는지 알려 주는 판단이다. 열려 있으면 허가하지 않는다.
- **NOCACHE 시퀀스 (NOCACHE sequence)**: 번호를 미리 메모리에 받아 두지 않고 요청할 때마다 하나씩 만드는 Oracle 시퀀스다. 번호가 조금 느리게 나오지만 건너뛰는 번호가 적다.
