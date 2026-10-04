# ADR-0002 외부 문서는 docs/ 아래에 성격별로 나눠 둔다

- 상태: 채택
- 날짜: 2026-10-04
- 관련: [ADR-0001](0001-single-speckit-at-root.md), [docs/README.md](../README.md)

## 배경

요구사항, 분석서, 설계서, 테스트 케이스 같은 외부 문서를 어디에 둘지 정해야 한다. 처음에는 `references/` 하나에 모으는 안이 있었다.

그런데 `references`는 "참고만 하는 자료"로 읽힌다. 요구사항, 분석서, 설계서, 테스트 케이스는 spec이 따라야 하는 **기준**이고, spec이 어느 문서에서 나왔는지 거슬러 찾을 수 있어야 한다. 이 넷을 참고 자료와 한 폴더에 섞으면 무엇이 기준이고 무엇이 참고인지 흐려진다.

## 결정

```text
docs/
├── requirements/   # 요구사항 정의서, 유스케이스
├── analysis/       # 분석서, AS-IS 분석
├── design/         # 아키텍처·인터페이스·DB·화면 설계서
├── test-cases/     # QA나 고객이 만든 테스트 케이스와 시나리오
├── standards/      # 코딩 규약, 아키텍처 규칙 → /speckit-constitution의 입력
├── adr/            # 여러 서비스에 걸친 아키텍처 결정 기록
├── references/     # 진짜 참고 자료: 규정, 벤더 API 문서, 레거시 매뉴얼
└── _originals/     # 받은 원본(xlsx, pptx, hwp)
```

- 외부 문서를 `specs/` 안에 넣지 않는다. `specs/`는 spec-kit이 관리하는 기능별 산출물 폴더다.
- `_originals/`의 원본은 AI에게 직접 주지 않는다. 위 폴더에 둔 md 변환본을 `@`로 가리킨다.
- 기준 문서의 항목에는 ID를 붙이고, spec과 plan은 그 ID를 출처로 남긴다. ID 규칙은 [docs/README.md](../README.md)에 있다.

## 고려한 대안

| 대안 | 고르지 않은 이유 |
|---|---|
| `references/` 하나에 모두 | 기준과 참고가 섞여 spec의 출처를 따지기 어렵다 |
| `specs/` 안에 외부 문서 | spec-kit 산출물과 섞인다. 기능 하나에 묶이지 않는 문서를 둘 곳이 없다 |

## 결과

- 좋아지는 점: 기존 가이드(spec-kit-claude-code-guide)가 쓰는 `docs/analysis`, `docs/design`, `docs/standards`와 이름이 맞는다. spec-kit 명령마다 넘길 폴더가 분명하다.
- 감수할 점: 원본을 받으면 md로 바꿔 알맞은 폴더에 옮기는 일이 생긴다.

## 바꿀 때

문서 종류가 늘어 이 분류에 맞지 않는 문서가 생기면 폴더를 더하고 이 ADR을 갱신한다.
