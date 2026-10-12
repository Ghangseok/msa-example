# Data Model: 주문 조회

**기능 폴더**: `specs/002-view-orders` | **작성일**: 2026-10-11 | **입력**: [spec.md](spec.md)의 Key Entities, [research.md](research.md)

이 문서는 조회가 읽는 데이터, 조회 규칙, 응답 모양, 쿼리 매개변수 검사를 적는다.

- 이 티켓은 새 테이블, 새 열, 새 시퀀스를 만들지 않는다. 티켓 001이 만든 `ORDERS`와 `ORDER_ITEMS`를 읽기만 한다. 테이블 정의는 `specs/001-place-order/data-model.md` 2절이 기준이다.
- `[제안]`은 사용자가 정하지 않았는데 Claude가 채운 것이다. 이 문서의 값은 모두 `[제안]`이다. 이유는 [research.md](research.md)의 같은 번호 항목에 있다.
- SQL은 19c에서 도는 것만 쓴다(코딩 규약 3-3절). 값은 `#{}`로만 넣고 `SELECT *`를 쓰지 않는다(코딩 규약 3-2절).

## 1. DB와 계정

| 서비스 | DB 컨테이너 | 계정 (PDB `XEPDB1` 안) | 읽는 테이블 |
|---|---|---|---|
| 주문 서비스 | `order-db` | `ORDER_SVC` | `ORDERS`, `ORDER_ITEMS` |

- 재고 DB(`inventory-db`)와 재고 서비스는 쓰지 않는다. 조회는 다른 서비스를 부르지 않는다.
- Flyway 파일을 더하지 않는다(research.md 항목 10).

## 2. 읽는 열과 응답 필드

| DB 열 | 응답 필드 | 타입 | 뜻 |
|---|---|---|---|
| `ORDERS.ORDER_NO` | `orderNo` | 정수(int64) | 주문 번호 |
| `ORDERS.STATUS` | `status` | 글자 | `PENDING`(처리중), `CONFIRMED`(확정), `REJECTED`(거절), `FAILED`(실패) |
| `ORDERS.REASON` | `reason` | 글자 또는 `null` | 거절이면 `OUT_OF_STOCK`(재고 부족) 또는 `PRODUCT_NOT_FOUND`(상품 없음). 실패이면 `RETRY_LATER`(잠시 후 다시 시도). 그 밖에는 `null` |
| `ORDER_ITEMS.PRODUCT_ID`, `QUANTITY` | `items[].productId`, `items[].quantity` | 글자, 정수 | 주문 항목. 상품 ID 순 |
| `ORDER_ITEMS.REJECT_REASON` | `shortageProductIds`, `missingProductIds` | 글자 목록 | `OUT_OF_STOCK`인 항목의 상품 ID는 `shortageProductIds`, `PRODUCT_NOT_FOUND`인 항목은 `missingProductIds`. 없으면 빈 목록. 상품 ID 순 |
| `ORDERS.CREATED_AT` | `createdAt` | 글자 | 주문 시각. 시각대 없는 ISO 8601(예: `2026-10-11T13:45:11.123456`). research.md 항목 15 |
| `ORDERS.RELEASE_STATUS` | `releaseStatus` (관리자 응답만) | 글자 또는 `null` | `NOT_REQUIRED`(해제 불필요), `RELEASED`(해제 완료), `RELEASE_FAILED`(해제 실패). 해제 요청 전이거나 "실패"가 아니면 `null`. 필드는 항상 있다 |

- `ORDERS.CUSTOMER_ID`와 `ORDERS.REQUEST_KEY`는 응답에 내지 않는다. `CUSTOMER_ID`는 조회 조건으로만 쓴다. 관리자 응답에도 넣지 않는다(research.md 항목 6의 "사용자가 확인할 것").
- 상태와 사유와 해제 결과의 값 목록은 001이 정했다(`specs/001-place-order/data-model.md` 4-1절, 4-2절). 이 티켓은 값을 더하지 않는다. 그래서 상태 값을 더하는 깨는 변경이 없다.
- 고객용 응답(`OrderDetailResponse`)에는 `releaseStatus`가 없다.

## 3. 조회 규칙

| 조회 | 조건 | 정렬 | 쪽 지정 |
|---|---|---|---|
| 고객의 주문 하나 (`GET /orders/{orderNo}`) | `ORDER_NO = 주문 번호` **그리고** `CUSTOMER_ID = 토큰의 sub` | 항목은 `PRODUCT_ID` 순 | 없음 |
| 고객의 주문 목록 (`GET /orders`) | `CUSTOMER_ID = 토큰의 sub` | `ORDER_NO` 큰 순, 항목은 `PRODUCT_ID` 순 | `page`, `size` |
| 관리자의 주문 목록 (`GET /admin/orders`) | 모든 고객. `status`를 주면 `STATUS = 값`, `releaseStatus`를 주면 `RELEASE_STATUS = 값`. 둘 다 주면 둘 다 맞는 주문 | `ORDER_NO` 큰 순, 항목은 `PRODUCT_ID` 순 | `page`, `size` |

- **다른 고객의 주문**: 주문 하나 조회가 `CUSTOMER_ID` 조건을 SQL에 넣기 때문에, 다른 고객의 주문은 없는 주문과 같은 빈 결과가 된다. 서비스가 빈 결과를 `ORDER_NOT_FOUND`(404)로 바꾼다(research.md 항목 11).
- **쪽 지정 SQL의 모양**: 목록은 `ORDERS`에서 조건에 맞는 주문을 `ORDER_NO` 큰 순으로 정렬하고 `OFFSET (page × size) ROWS FETCH NEXT (size + 1) ROWS ONLY`로 자른 안쪽 SELECT(인라인 뷰)를 만든다. 그 결과에 `ORDER_ITEMS`를 `LEFT JOIN`한다. 주문과 항목을 합친 뒤에 자르면 주문 하나가 쪽 사이에서 잘리기 때문이다. `size + 1`개를 읽는 이유는 `hasNext`를 정하려는 것이다.
- **항목이 없는 주문**: `LEFT JOIN`이므로 항목이 없는 주문도 나온다. 001은 항목 없는 주문을 만들지 않는다. 이 처리는 방어가 아니라 001의 `selectOrderWithItems`와 같은 모양을 쓰는 것이다.
- **관리자 조건 생략**: 조건이 없으면 모든 주문이다. SQL의 `WHERE`는 조건이 있을 때만 붙는다(MyBatis 동적 SQL의 `<where>`와 `<if>`).
- **읽기 전용**: 조회는 주문을 바꾸지 않는다. 서비스 메서드는 읽기 전용 트랜잭션이다.

## 4. 쿼리 매개변수 검사

검사는 `domain` 패키지가 한다(Spring을 모르는 순수 Java). 컨트롤러는 글자로 받은 값을 넘긴다. 틀리면 400(`INVALID_QUERY_PARAMETER`)이고 `errors`에 매개변수 이름과 이유를 담는다.

| 매개변수 | 어디서 | 규칙 | 틀리면 |
|---|---|---|---|
| `orderNo` | `GET /orders/{orderNo}` 경로 | 숫자만 있고 `long`에 담긴다 | 400이 아니라 404(`ORDER_NOT_FOUND`)다. 시나리오 1.3 |
| `page` | 목록 두 개 | 0 이상의 정수. 없으면 0 | 400 |
| `size` | 목록 두 개 | 1 이상 100 이하의 정수. 없으면 20 | 400 |
| `status` | `GET /admin/orders` | `PENDING`, `CONFIRMED`, `REJECTED`, `FAILED` 가운데 하나. 없으면 조건 없음. 대소문자를 구분한다. 빈 값은 틀린 값이다 | 400. 시나리오 4.3 |
| `releaseStatus` | `GET /admin/orders` | `NOT_REQUIRED`, `RELEASED`, `RELEASE_FAILED` 가운데 하나. 없으면 조건 없음. 대소문자를 구분한다. 빈 값은 틀린 값이다 | 400. 시나리오 4.3 |

- 알 수 없는 쿼리 매개변수(예: `customerId`)는 무시한다. 고객 ID는 토큰의 `sub`에서만 얻는다.
- 쪽 크기 기본값 20과 최대값 100은 `domain`의 상수다(research.md 항목 3). 설정값이 아니다.

## 5. 응답 모양

| 응답 | 담는 것 |
|---|---|
| 주문 하나 (`OrderDetailResponse`) | `orderNo`, `status`, `reason`, `shortageProductIds`, `missingProductIds`, `items`, `createdAt` |
| 관리자용 주문 (`AdminOrderResponse`) | 위와 같은 필드 + `releaseStatus` |
| 목록 (`OrderPage`) | `orders`(주문 목록), `page`, `size`, `hasNext` |

- 주문이 하나도 없으면 `orders`는 빈 배열이다. `hasNext`는 `false`다.
- 필드는 빠지지 않는다. 값이 없는 필드(`reason`, `releaseStatus`)는 `null`로 낸다. 001의 `OrderResponse`가 `reason`을 `null`로 내는 방식과 같다.
- 자세한 형식은 [contracts/order-query-api.yaml](contracts/order-query-api.yaml)에 있다.

## 6. Java 쪽 모양

| 서비스 | 패키지 | 이름 | 종류 |
|---|---|---|---|
| 주문 | `domain` | `OrderNumbers` | 주문 번호 글자를 숫자로 바꾸는 순수 Java 도구 |
| 주문 | `domain` | `PageRequest`, `OrderSearch` | 쿼리 매개변수를 검사한 값 객체 |
| 주문 | `domain` | `InvalidQueryException` | 매개변수가 틀렸을 때 던지는 업무 예외 |
| 주문 | `dto` | `OrderDetailResponse`, `AdminOrderResponse`, `OrderPage` | record |
| 주문 | `dto` | `OrderWithItems` | 일반 클래스. `createdAt` 필드를 더한다 |
| 주문 | `service` | `OrderQueryService` | 읽기 전용 트랜잭션 서비스 |
| 주문 | `controller` | `OrderQueryController` | 조회 API 세 개 |
| 주문 | `exception` | `ProblemAccessDeniedHandler` | 403 Problem Details |
| 주문 | `config` | `RealmRoleJwtConverter` | 토큰의 `realm_access.roles`를 권한으로 바꿈 |

- 이름은 `[제안]`이다. tasks.md가 고른다. 이 표는 코딩 규약 3-4절의 접미사(`Controller`, `Service`, `Response`)를 따랐다.
- 조회 결과를 담는 `OrderWithItems`는 001의 1:N 부모 클래스다. `createdAt`을 더해도 기존 쓰기 흐름은 이 필드를 쓰지 않는다. 읽는 SQL은 기존 `orderWithItemsMap`을 이어받는 새 `resultMap`에서만 `CREATED_AT`을 채운다.

## 용어

- **조회 조건 (search condition)**: 목록에서 어떤 주문을 읽을지 정하는 값이다. 관리자 조회의 `status`, `releaseStatus`다.
- **쿼리 매개변수 (query parameter)**: 주소 끝의 `?` 뒤에 붙여 조건을 넘기는 값이다. 예: `/admin/orders?status=FAILED`.
- **인라인 뷰 (inline view)**: SQL의 `FROM` 자리에 쓰는 안쪽 SELECT다. 주문을 먼저 정렬하고 자른 결과에 항목을 합치는 데 쓴다.
- **`LEFT JOIN`**: 왼쪽 표의 줄을 모두 남기고 오른쪽 표를 붙이는 SQL 합치기다. 항목이 없는 주문도 결과에 남는다.
- **동적 SQL (dynamic SQL)**: 조건에 따라 SQL 조각을 붙이거나 빼는 MyBatis 기능이다. 관리자 조회의 `status`, `releaseStatus` 조건에 쓴다.
- **읽기 전용 트랜잭션 (read-only transaction)**: 데이터를 바꾸지 않는다고 알리고 여는 트랜잭션이다. 조회 서비스가 쓴다.
- **값 객체 (value object)**: 값을 검사해서 담아 두는, 바뀌지 않는 작은 객체다. 쪽 지정과 조회 조건을 이렇게 담는다.
- **응답 봉투 (response envelope)**: 목록과 쪽 정보를 함께 담은 응답 객체다. 이 기능에서는 `OrderPage`다.
- **`hasNext`**: 다음 쪽이 있는지 알리는 응답 필드다. 한 쪽보다 한 줄 더 읽어서 정한다.
- **시각대 (time zone)**: 시각이 어느 지역의 시계 기준인지 나타내는 정보다. 주문 시각은 시각대 없이 DB가 저장한 값을 그대로 낸다.
