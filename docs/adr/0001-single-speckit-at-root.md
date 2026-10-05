# ADR-0001 저장소 루트에 spec-kit 하나를 둔다

- 상태: 채택
- 날짜: 2026-10-04
- 관련: [ADR-0002](0002-docs-folder-structure.md), STD-014, STD-015

## 배경

여러 서비스를 한 저장소에 두고 spec-kit으로 개발한다. spec-kit을 어디에 둘지 정해야 한다.

- A안: 저장소 루트에 `.specify/` 하나를 두고, `specs/`를 기능 단위로 관리한다.
- B안: 서비스마다 `.specify/`를 두고, 서비스별로 spec을 관리한다.

spec은 업무 기능 단위이고, MSA에서 업무 기능은 보통 여러 서비스에 걸친다. 예를 들어 주문 생성은 주문 서비스와 재고 서비스를 함께 건드린다.

## 결정

A안을 택한다.

```text
msa-example/
├── .claude/  .specify/  CLAUDE.md
├── specs/                 # spec-kit 산출물 (기능 단위, 서비스 횡단)
├── docs/                  # 기준 문서와 참고 자료 (ADR-0002)
├── contracts/             # 서비스 사이 API·이벤트 계약의 현재본 (OpenAPI, AsyncAPI)
├── services/
│   ├── order-service/     # CLAUDE.md, build.gradle, src/
│   ├── payment-service/
│   └── inventory-service/
├── libs/                  # 공통 라이브러리. 최소한으로만 둔다
├── infra/                 # docker-compose, k8s, helm
├── tests/e2e/             # 여러 서비스를 거치는 테스트
├── tools/                 # 빌드 보조 스크립트, AS-IS 기록기
└── settings.gradle
```

- **spec.md**는 기술에 중립인 사용자 스토리로 쓴다. 서비스별로 나누지 않는다.
- **서비스별 구현 위치**는 plan.md의 Source Code 절에 `services/order-service/...`, `services/inventory-service/...`처럼 적는다.
- **서비스별 규칙**(패키지 구조, DB)은 `services/<svc>/CLAUDE.md`에 적는다. Claude Code는 하위 폴더의 파일을 읽을 때 그 폴더의 CLAUDE.md도 읽는다.
- **어떤 기능이 어느 서비스를 건드렸는지**는 plan.md의 경로를 grep으로 찾는다. 예: `grep -rln "services/inventory-service" specs/*/plan.md`
- **소유권**은 `services/`에 CODEOWNERS를 걸고, spec은 PR 리뷰로 관리한다.
- **계약 현재본**은 `contracts/`에 둔다. `specs/NNN/contracts/`는 그 기능에서 바뀐 부분만 담기 때문에, 지금 전체 계약이 어떤지는 따로 한곳에 있어야 한다.

## 고려한 대안

| 대안 | 고르지 않은 이유 |
|---|---|
| B안: 서비스마다 `.specify/` | 하나의 업무 기능이 서비스 수만큼 쪼개지고, 서비스마다 헌법이 달라진다. 둘 다 나중에 고치기 어렵다. B안에서 A안으로 가려면 서비스마다 따로 매긴 spec 번호를 합쳐야 한다 |

## 결과

- 좋아지는 점: 기능 하나를 spec 하나로 본다. 헌법이 하나라서 서비스끼리 규칙이 어긋나지 않는다.
- 감수할 점: 서비스별 독립성이 spec 수준에서는 드러나지 않는다. CODEOWNERS와 plan.md 경로로 보완한다.

## 바꿀 때

특정 서비스가 커져 독립이 필요해지면 그 서비스에만 `.specify/`를 추가한다. 다른 곳은 고칠 필요가 없다.

spec-kit은 명령을 실행하는 **현재 폴더**에서 위로 올라가며 가장 가까운 `.specify/`를 찾는다(`.specify/scripts/python/common.py`의 `find_specify_root`). 루트에서 Claude Code를 띄우면 루트의 것이 쓰이므로, 서비스 전용 spec-kit을 쓰려면 그 서비스 폴더에서 세션을 열거나 환경변수 `SPECIFY_INIT_DIR`로 지정한다.

## 용어

- **spec-kit**: 기능을 spec, plan, tasks, 구현 순서로 만들게 돕는 도구다. 이 저장소에서는 `/speckit-specify` 같은 명령으로 쓴다.
- **헌법 (constitution)**: spec-kit의 모든 명령이 읽는 프로젝트 원칙 문서다. `.specify/memory/constitution.md` 파일이 헌법이다.
- **서비스 횡단 (cross-service)**: 기능 하나가 여러 서비스에 걸쳐 있는 것이다. 주문 생성은 주문 서비스와 재고 서비스를 함께 건드린다.
- **계약 (contract)**: 서비스 사이 API와 이벤트의 형식을 적은 명세다. 이 저장소에서는 `contracts/`의 OpenAPI, AsyncAPI 파일이 계약이다.
- **계약 현재본 (current contracts)**: 지금 main에 있는 서비스 사이 계약 전체다. `contracts/`에 두고, `specs/NNN/contracts/`에는 그 기능에서 바뀐 부분만 둔다.
- **공통 라이브러리 (`libs/`)**: 여러 서비스가 함께 쓰는 코드를 두는 폴더다. 추적 설정 같은 기술 코드만 두고 업무 코드는 두지 않는다.
- **CODEOWNERS**: 폴더마다 검토 책임자를 정하는 GitHub 파일이다. 협업자가 생기면 `services/`에 둔다.
