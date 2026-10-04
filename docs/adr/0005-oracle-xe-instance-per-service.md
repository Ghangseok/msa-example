# ADR-0005 서비스마다 Oracle 21c XE 컨테이너를 따로 두고, 클러스터 밖에서 띄운다

- 상태: 채택
- 날짜: 2026-10-04
- 관련: [tech-stack.md](../standards/tech-stack.md), [design/architecture.md](../design/architecture.md), [references/docker-desktop.md](../references/docker-desktop.md), STD-001, ADR-0003

## 배경

- 사용자는 DB로 Oracle XE 19 버전을 원했다. 그런데 19c에는 XE가 없다. 나온 XE는 11g, 18c, 21c이고, 그 뒤로는 Oracle AI Database 26ai Free(옛 23ai Free)가 XE 자리를 이었다. `[문헌]`
- 원문은 서비스마다 자기 DB를 가진다고 한다(P§0, P§5).
- XE는 논리 환경(VM, 컨테이너, 물리 서버) 하나에 하나만 뜬다. 같은 환경에서 둘째를 띄우면 `ORA-00442`로 시작하지 않는다. 메모리는 2GB, CPU는 2개, 사용자 데이터는 12GB까지 쓴다. `[문헌]`
- 개발 PC의 메모리는 24GB이고, kind 노드, Spring Boot Pod 4개, Keycloak이 함께 뜬다.

## 결정

1. **버전**: Oracle Database **21c XE**를 쓴다. SQL은 19c에서도 돌아가는 것만 쓴다. (tech-stack.md)
2. **서비스마다 XE 컨테이너 하나**: 컨테이너는 그 자체로 논리 환경 하나이므로 라이선스 제한에 걸리지 않는다.

   | 서비스 | 컨테이너 | 호스트 포트 | 계정 (기본 PDB `XEPDB1` 안) |
   |---|---|---|---|
   | 주문 | `order-db` | 1521 | `ORDER_SVC` |
   | 재고 | `inventory-db` | 1522 | `INVENTORY_SVC` |
   | 결제 (후속) | `payment-db` | 1523 | `PAYMENT_SVC` |

3. **위치**: 클러스터 밖에서 docker compose로 띄운다(`infra/compose/`). Keycloak도 같은 compose에 둔다. 컨테이너는 `kind` Docker 네트워크에 붙여, Pod가 `order-db:1521`처럼 이름으로 찾게 한다.
4. **데이터**: 컨테이너마다 이름 붙은 볼륨(`order-db-data`, `inventory-db-data`)에 둔다. kind 클러스터를 다시 만들어도 남는다.
5. **이미지**: 로컬 실행과 Testcontainers에서 같은 이미지(gvenzl/oracle-xe 21 계열, faststart)를 쓴다. `[제안]`
6. **메모리**: WSL 메모리를 14GB로 둔다. (references/docker-desktop.md 3-3절)

## 고려한 대안

| 대안 | 고르지 않은 이유 |
|---|---|
| 인스턴스 하나 + 서비스별 스키마 | 메모리가 가장 적게 든다(약 2GB). 하지만 DB가 멈추면 모든 서비스가 함께 멈추고, 원문의 "자기 DB"와 다르다 |
| 인스턴스 하나 + 서비스별 PDB | 접속 주소까지 나뉜다. 하지만 21c XE는 PDB가 3개까지라 기본 PDB를 빼면 둘밖에 남지 않아, 결제 서비스를 넣을 자리가 없다. 장애 격리도 없다 |
| Oracle AI Database 26ai Free | 지금 Oracle이 무료로 내놓는 최신판이다. 하지만 19c와 차이가 커서 23ai부터 생긴 기능을 모르고 쓰기 쉽다 |
| Oracle 19c Enterprise Edition 컨테이너 | 19c와 똑같다. 하지만 이미지가 크고 라이선스 동의가 필요하며, 실습용으로는 무겁다 |
| 클러스터 안 StatefulSet | 클러스터를 다시 만들 때마다 데이터와 큰 이미지를 다시 올려야 한다 |

## 결과

- 좋아지는 점:
  - 원문의 "서비스마다 자기 DB"를 그대로 구현한다. 접속 정보도, 재시작도, 버전 올리기도 서비스마다 따로 한다.
  - **DB 장애 격리를 실험할 수 있다.** 재고 DB를 멈춰도 주문 조회는 계속된다. (TC-109)
  - 한 서비스의 무거운 쿼리가 다른 서비스를 느리게 하지 않는다.
- 감수할 점:
  - 메모리를 DB마다 약 2GB 쓴다. 결제 서비스가 들어오면 2GB가 더 든다. 그때 WSL 메모리를 16GB로 늘리거나, 통합 테스트를 돌릴 때 클러스터의 일부를 멈춘다.
  - 처음 띄울 때 DB 생성 시간이 DB 수만큼 걸린다.
  - 관리할 컨테이너와 볼륨이 늘어난다.

## 바꿀 때

- 메모리가 부족하면 "인스턴스 하나 + 서비스별 스키마"로 내릴 수 있다. 서비스 코드는 그대로이고 접속 정보(ConfigMap, Secret)만 바뀐다.
- 운영 대상이 19c로 정해지면 19c 환경에서 Flyway 마이그레이션을 한 번 돌려 호환성을 확인한다.
