# Implementation Plan: [FEATURE]

**Branch**: `[###-feature-name]` | **Date**: [DATE] | **Spec**: [link]

**Input**: Feature specification from `/specs/[###-feature-name]/spec.md`

**Note**: This template is filled in by the `/speckit-plan` command; its definition describes the execution workflow.

## Summary

[Extract from feature spec: primary requirement + technical approach from research]

## Technical Context

<!--
  ACTION REQUIRED: Replace the content in this section with the technical details
  for the project. The structure here is presented in advisory capacity to guide
  the iteration process.
-->

**Language/Version**: [e.g., Python 3.11, Swift 5.9, Rust 1.75 or NEEDS CLARIFICATION]

**Primary Dependencies**: [e.g., FastAPI, UIKit, LLVM or NEEDS CLARIFICATION]

**Storage**: [if applicable, e.g., PostgreSQL, CoreData, files or N/A]

**Testing**: [e.g., pytest, XCTest, cargo test or NEEDS CLARIFICATION]

**Target Platform**: [e.g., Linux server, iOS 15+, WASM or NEEDS CLARIFICATION]

**Project Type**: [e.g., library/cli/web-service/mobile-app/compiler/desktop-app or NEEDS CLARIFICATION]

**Performance Goals**: [domain-specific, e.g., 1000 req/s, 10k lines/sec, 60 fps or NEEDS CLARIFICATION]

**Constraints**: [domain-specific, e.g., <200ms p95, <100MB memory, offline-capable or NEEDS CLARIFICATION]

**Scale/Scope**: [domain-specific, e.g., 10k users, 1M LOC, 50 screens or NEEDS CLARIFICATION]

## Constitution Check

*GATE: Phase 0 연구를 시작하기 전에 통과해야 한다. Phase 1 설계가 끝난 뒤 다시 확인한다.*

<!--
  이 절을 채우는 순서 (.specify/memory/constitution.md의 "개발 절차와 품질 관문" 절)
  1. 아래 다섯 문서를 먼저 모두 읽는다.
     - docs/standards/architecture-rules.md
     - docs/standards/tech-stack.md
     - docs/standards/testing.md
     - docs/standards/coding-conventions.md
     - docs/standards/git-workflow.md
  2. 헌법의 원칙마다 "plan에서 확인할 질문"에 답한다. 원칙은 이름으로 적는다.
  3. docs/standards/architecture-rules.md의 "반드시" 규칙을 하나씩 확인한다.
     이 plan과 관계없는 규칙은 "해당 없음"과 그 이유를 적는다.
  4. 표에 항목 ID를 쓰면 표 바로 아래에 출처와 원문을 펼친다 (CLAUDE.md 2절).
  5. "반드시" 규칙을 하나라도 어기면 통과하지 못한다. Complexity Tracking으로 넘길 수 없다.
-->

**먼저 읽은 문서**: [docs/standards/의 다섯 문서를 모두 읽었는지 적는다]

### 원칙 확인

| 원칙 | 확인할 질문에 대한 답 | 결과 |
|------|----------------------|------|
| [헌법의 원칙 이름. 원칙마다 한 행] | [답] | [통과 / 실패] |

### 반드시 규칙 확인 (docs/standards/architecture-rules.md)

| 규칙 | 이 plan에서 지키는 방법, 또는 해당 없음과 그 이유 | 결과 |
|------|-----------------------------------------------|------|
| [규칙 ID. 반드시 규칙마다 한 행] | [방법 또는 해당 없음과 이유] | [통과 / 실패 / 해당 없음] |

**이 표에 나온 항목**: [표에 나온 규칙 ID마다 출처 파일, 절, 원문을 펼친다]

## Project Structure

### Documentation (this feature)

```text
specs/[###-feature]/
├── plan.md              # This file (/speckit-plan command output)
├── research.md          # Phase 0 output (/speckit-plan command)
├── data-model.md        # Phase 1 output (/speckit-plan command)
├── quickstart.md        # Phase 1 output (/speckit-plan command)
├── contracts/           # Phase 1 output (/speckit-plan command)
└── tasks.md             # Phase 2 output (/speckit-tasks command - NOT created by /speckit-plan)
```

### Source Code (repository root)
<!--
  ACTION REQUIRED: Replace the placeholder tree below with the concrete layout
  for this feature. Delete unused options and expand the chosen structure with
  real paths (e.g., apps/admin, packages/something). The delivered plan must
  not include Option labels.
-->

```text
# [REMOVE IF UNUSED] Option 1: Single project (DEFAULT)
src/
├── models/
├── services/
├── cli/
└── lib/

tests/
├── contract/
├── integration/
└── unit/

# [REMOVE IF UNUSED] Option 2: Web application (when "frontend" + "backend" detected)
backend/
├── src/
│   ├── models/
│   ├── services/
│   └── api/
└── tests/

frontend/
├── src/
│   ├── components/
│   ├── pages/
│   └── services/
└── tests/

# [REMOVE IF UNUSED] Option 3: Mobile + API (when "iOS/Android" detected)
api/
└── [same as backend above]

ios/ or android/
└── [platform-specific structure: feature modules, UI flows, platform tests]
```

**Structure Decision**: [Document the selected structure and reference the real
directories captured above]

## Complexity Tracking

> **권장 규칙을 어길 때만 채운다.** "반드시" 규칙 위반은 여기에 적어 넘길 수 없다.
> 바꾸려면 ADR을 쓰고 헌법을 개정한다 (.specify/memory/constitution.md의 "거버넌스" 절).

| 어긴 권장 규칙 | 필요한 이유 | 더 단순한 방법을 고르지 않은 이유 |
|---------------|------------|--------------------------------|
| [예: 권장 규칙의 출처와 원문] | [지금 필요한 이유] | [단순한 방법으로는 안 되는 이유] |
