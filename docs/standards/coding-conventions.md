# 코딩 규약

- 상태: **확정 (2026-10-04)** — A안(전통 계층형 + MyBatis)
- 쓰는 곳: `/speckit-constitution`, 각 서비스의 `services/<svc>/CLAUDE.md`

## 1. 고른 안과 비교한 안

| | **A안: 전통 계층형 + MyBatis (선택)** | B안: 정석 헥사고날 + JPA | C안: 기능 단위 패키지 + 실용 계층 + JPA |
|---|---|---|---|
| 구조 | `controller/`, `service/`, `mapper/`, `client/`, `dto/`를 서비스 최상위에 둔다 | `domain/`, `application/port`, `adapter/` | 기능마다 `api/`, `application/`, `domain/`, `infra/` |
| 영속성 | MyBatis XML에 SQL을 직접 쓴다 | 도메인 모델과 JPA 엔티티를 따로 둔다 | 도메인 객체가 JPA 엔티티를 겸한다 |
| 장점 | 익숙하다. Oracle SQL, 잠금, 실행 계획을 직접 다룬다 | 도메인이 기술에서 완전히 분리된다 | 기능 코드가 한곳에 모인다 |
| 약점 | 업무 규칙이 service와 SQL에 흩어지기 쉽다 | 지금 규모에 비해 코드가 많다 | 도메인이 JPA에 조금 묶인다 |

**A안의 약점을 줄이는 규칙** (3절에 자세히)

- 상태 전이, 항목 검사 같은 업무 규칙은 `domain` 패키지에 모은다. service는 흐름만 조립한다.
- SQL에는 업무 판단을 넣지 않는다. 예외는 동시성을 지키기 위한 잠금과 조건부 갱신이다.
- 다른 서비스 호출은 `client` 패키지에만 둔다. 테스트에서 WireMock으로 바꿔 끼울 수 있다.

## 2. 패키지 구조

```text
services/order-service/src/main/java/com/example/msa/order/
├── OrderServiceApplication.java
├── controller/   # REST 컨트롤러. 요청 검증, 응답 변환만
├── service/      # 업무 흐름 조립, 트랜잭션 경계
├── mapper/       # MyBatis Mapper 인터페이스
├── client/       # 다른 서비스 호출 (InventoryClient). 재시도·서킷 브레이커 적용 지점
├── domain/       # 상태 enum, 업무 규칙 (OrderStatus의 전이 규칙, 주문 항목 검사)
├── dto/          # API 요청·응답 record, 조회 결과 객체
├── exception/    # 업무 예외, ProblemDetail 변환
└── config/       # 보안, MyBatis, 재시도·서킷 브레이커, 추적 설정

services/order-service/src/main/resources/
├── mapper/OrderMapper.xml        # SQL
├── db/migration/V1__orders.sql   # Flyway
└── application.yml
```

**의존 방향**

```text
controller → service → mapper
                     → client
                     → domain
dto는 어디서나 쓸 수 있다
```

- controller는 mapper와 client를 직접 부르지 않는다.
- mapper, client, domain은 service와 controller를 모른다.
- domain은 Spring과 MyBatis를 모른다(순수 Java). 그래서 단위 테스트가 쉽다.
- 이 규칙은 ArchUnit 테스트로 강제한다. ([testing.md](testing.md))

## 3. 규칙

### 3-1. 계층

- **controller**: 요청 형식 검사(Bean Validation), service 호출, 응답 변환만 한다. 업무 판단을 하지 않는다.
- **service**: 흐름을 조립하고 트랜잭션 경계를 정한다. 판단은 domain에 맡긴다.
- **domain**: "처리중에서만 확정으로 갈 수 있다", "같은 상품은 한 줄" 같은 규칙을 메서드로 둔다. 예: `OrderStatus.canMoveTo(...)`, `OrderItems.validate(...)`.
- **원격 호출과 트랜잭션을 섞지 않는다** (STD-018). 주문 생성은 아래처럼 나눈다.

  ```text
  OrderService.place()            # @Transactional 없음. 흐름만 조립
    ├─ OrderTxService.savePending()   # @Transactional — 처리중 저장
    ├─ InventoryClient.reserve()      # 트랜잭션 밖. 재시도·서킷
    └─ OrderTxService.applyResult()   # @Transactional — 결과 저장
  ```

  같은 클래스 안에서 `@Transactional` 메서드를 부르면 Spring 프록시를 거치지 않아 트랜잭션이 걸리지 않는다. 그래서 트랜잭션 메서드는 별도 빈(`OrderTxService`)에 둔다. `[문헌]`

### 3-2. MyBatis와 SQL

- Mapper 인터페이스와 XML(`resources/mapper/*.xml`)을 짝지어 쓴다. 애너테이션 SQL은 쓰지 않는다. SQL을 한곳에서 찾기 위해서다.
- 파라미터는 `#{}`만 쓴다. `${}`는 SQL 주입 위험 때문에 쓰지 않는다. 정렬 열처럼 꼭 필요하면 허용 목록으로 검사한 값만 넣는다.
- `map-underscore-to-camel-case=true`로 열 이름을 자동으로 매핑한다.
- `SELECT *`를 쓰지 않는다. 필요한 열을 적는다.
- 주문과 주문 항목 같은 1:N 조회는 `resultMap`의 `<collection>`으로 한 번에 읽는다. 주문마다 항목을 따로 조회(N+1)하지 않는다.
- 주문 번호는 Oracle 시퀀스로 만든다(`<selectKey order="BEFORE">`).
- 페이징은 `OFFSET ... ROWS FETCH NEXT ... ROWS ONLY`로 쓴다(12c부터 지원, 19c 호환).
- 재고처럼 동시성이 중요한 갱신은 잠금 없이 "읽고 나서 쓰기"를 하지 않는다. 조건부 UPDATE를 쓰거나, `SELECT ... FOR UPDATE`로 잠근 뒤 쓴다. (BR-003)
- 여러 행을 잠글 때는 언제나 같은 순서(예: 상품 ID 순)로 잠근다. 순서가 다르면 교착이 생긴다.
- enum은 이름 문자열(VARCHAR2)로 저장한다. 날짜는 `TIMESTAMP` ↔ `java.time` 타입으로 매핑한다.

### 3-3. DB

- 19c에서 돌아가는 SQL만 쓴다. 21c 이후 기능(JSON 데이터 타입, SQL 매크로, 23ai의 `BOOLEAN`, `IF NOT EXISTS` 등)을 쓰지 않는다. (tech-stack.md)
- 스키마는 Flyway 마이그레이션으로만 바꾼다. 이미 적용된 마이그레이션 파일은 고치지 않고 새 파일을 더한다.
- 서비스는 자기 DB의 자기 계정만 쓴다. (STD-001, ADR-0005)
- 이름은 대문자 스네이크 케이스다. `ORDER`는 예약어이므로 테이블은 `ORDERS`로 쓴다.

### 3-4. 이름

- 기본 패키지: `com.example.msa.<서비스>` (예: `com.example.msa.inventory`)
- 클래스 접미사: `Controller`, `Service`, `TxService`(트랜잭션 전용), `Mapper`, `Client`, `Request`/`Response`(API DTO)
- Mapper 메서드: `select…`, `insert…`, `update…`, `delete…`로 시작한다. SQL id와 같게 둔다.

### 3-5. 코드

- API DTO는 `record`로 쓴다. 조회 결과 객체도 가능하면 record로 쓴다. MyBatis에서 record를 쓰는 설정(생성자 매핑)은 plan에서 확인한다.
- Lombok을 쓰지 않는다. record와 IDE 생성 코드로 충분하다.
- 설정값은 `@ConfigurationProperties`로 받는다. 제한 시간, 재시도, 다른 서비스 주소, 비밀번호를 코드에 적지 않는다. (STD-004, STD-009)
- 오류 응답은 RFC 9457 Problem Details 형식(Spring `ProblemDetail`)으로 통일한다.
- 포매터는 빌드에 묶는다(Spotless + palantir-java-format, 4칸 들여쓰기).

### 3-6. 로그

- JSON 구조화 로그를 표준 출력으로 낸다. 모든 줄에 traceId가 들어가게 한다. (STD-012, STD-013)
- 재고 호출은 몇 번째 시도인지, 서킷 상태가 무엇인지 남긴다.
- 비밀번호, 토큰 전체, 개인 정보를 로그에 남기지 않는다. MyBatis SQL 로그는 로컬에서만 켠다.

### 3-7. 테스트 코드

- 테스트 이름은 `@DisplayName`에 한국어로 쓰고, 인수 시나리오면 TC ID를 앞에 붙인다. ([testing.md](testing.md))
