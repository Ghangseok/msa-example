# 브랜치와 PR 규칙

- 상태: **확정 (2026-10-04, 2026-10-05 브랜치 이름 규칙 개정)** — GitHub Flow + spec 단위 브랜치
- 쓰는 곳: `/speckit-constitution`(개발 절차), 모든 커밋과 PR

## 1. 고른 안과 비교한 안

| | **A안: GitHub Flow + spec 단위 브랜치 (선택)** | B안: 트렁크 기반 | C안: Git Flow |
|---|---|---|---|
| 방식 | main 하나 + spec마다 짧은 기능 브랜치, PR로 병합 | main에 직접 올리거나 하루 안에 끝나는 브랜치 | develop, release, hotfix 브랜치를 따로 둠 |
| 장점 | spec, 브랜치, PR이 하나씩 짝지어져 추적하기 쉽다. AI가 만든 코드를 검토하는 관문이 생긴다 | 가장 빠르다 | 릴리스 관리가 엄격하다 |
| 약점 | PR을 만드는 수고가 든다 | spec과 구현을 검토할 지점이 없다 | 실습 규모에는 과하다 |

## 2. 브랜치

`main`은 언제나 빌드와 테스트가 통과한다. 직접 push하지 않고 PR로만 바꾼다.

그 밖의 브랜치 이름은 `<종류>/<설명>` 한 가지 형식만 쓴다.

| 항목 | 규칙 |
|---|---|
| 종류 | PR 제목에 쓸 커밋 종류(3절)와 같은 값을 쓴다: `feat`, `fix`, `docs`, `refactor`, `test`, `chore`, `build`, `ci`, `perf` |
| 설명 | 영어 소문자, 숫자, 하이픈(`-`)만 쓴다. 2~4단어로 쓴다. 브랜치 이름 전체는 40자 이하로 한다 |
| spec 작업 | 설명 자리에 `specs/` 폴더 이름을 그대로 쓴다. 예: `feat/001-place-order` |
| spec이 커서 단계별로 PR을 나눌 때 | 폴더 이름 뒤에 `-p`와 tasks.md의 단계(Phase) 번호를 붙인다. 예: `feat/001-place-order-p2` |
| 번호 | spec 번호(`001`) 말고는 일련번호를 붙이지 않는다 |

| 작업 | 브랜치 | PR 제목 |
|---|---|---|
| 주문 생성 기능 | `feat/001-place-order` | `feat(order): 주문 생성 API 추가` |
| 주문 생성 기능의 2단계 | `feat/001-place-order-p2` | `feat(order): 주문 생성 재시도와 서킷 브레이커 추가` |
| kind 클러스터 설정 | `chore/kind-cluster` | `chore(infra): kind 클러스터 설정 추가` |
| 이 문서 고치기 | `docs/branch-naming` | `docs(docs): 브랜치 이름 규칙을 커밋 종류와 맞춤` |

이렇게 정한 이유:

- 브랜치 앞부분과 PR 제목 앞부분이 같다. 외울 목록이 하나뿐이다.
- spec 번호는 spec-kit이 `specs/` 폴더에 붙이는 번호다. 따로 관리하지 않아도 된다.
- 일련번호를 붙이지 않는 이유는 세 가지다. GitHub가 PR마다 번호(`#1`, `#2` …)를 이미 붙인다. 여러 창에서 동시에 작업하면 같은 번호를 쓸 수 있다. 브랜치는 병합하면 지워지므로 번호가 남지 않는다.
- 설명을 영어로 쓰는 이유: 한글 브랜치 이름은 URL에서 `%ED%95%9C`처럼 바뀌어 읽기 어렵고, 터미널에서 입력하기도 불편하다.

운영 규칙:

- 브랜치 하나에 PR 하나를 만든다. PR 하나에는 한 가지 일만 담는다. 다른 일이 생기면 main에서 새 브랜치를 만든다.
- 브랜치는 짧게 산다. 병합하면 GitHub가 자동으로 지운다(6-1절 "Automatically delete head branches").
- 병합된 PR의 브랜치에는 다시 push하지 않는다. 병합 뒤에 더할 것이 생기면 최신 main에서 새 브랜치를 만든다. 지워진 브랜치에 push하면 같은 이름의 브랜치가 원격에 다시 생기고, 그 커밋은 main에 들어가지 않는다. push하기 전에는 그 브랜치의 PR이 병합되지 않았는지 `gh pr list --state all`로 확인한다.
- `/speckit-specify`를 돌리기 전에 브랜치를 만든다. spec-kit의 git 확장을 켜면 `/speckit-specify`가 브랜치를 대신 만든다. 지금은 꺼져 있다(`.specify/extensions.yml` 없음). 켤 때는 확장이 만드는 브랜치 이름이 이 규칙과 맞는지 먼저 확인한다.
- spec-kit은 브랜치 이름으로 기능 폴더를 찾지 않는다. 환경변수 `SPECIFY_FEATURE_DIRECTORY`나 `.specify/feature.json` 파일로 찾는다(`.specify/scripts/python/common.py`의 `get_feature_paths` 함수). `.specify/feature.json`은 저장소에 올라가지 않는 파일이라(`.specify/.gitignore`) 브랜치를 바꿔도 따라 바뀌지 않는다. 다른 spec 브랜치로 옮기면 `SPECIFY_FEATURE_DIRECTORY`로 기능 폴더를 다시 지정한다.

## 3. 커밋 메시지

[Conventional Commits](https://www.conventionalcommits.org/) 형식을 쓴다.

```text
<종류>(<범위>): <설명>

<본문 — 왜 바꿨는지. 선택>

Refs: 001-place-order, TC-003, STD-005
```

| 항목 | 값 |
|---|---|
| 종류 | `feat`, `fix`, `refactor`, `test`, `docs`, `chore`, `build`, `ci`, `perf` |
| 범위 | `order`, `inventory`, `contracts`, `infra`, `e2e`, `libs`, `docs`, `specs`, `repo`(저장소 루트의 설정 파일: `CLAUDE.md`, `.gitignore`, `.github/`), `speckit`(`.specify/` 아래의 헌법과 템플릿) |
| 설명 | 한국어로 쓴다. 한 줄로, 무엇을 했는지 쓴다. 예: `feat(order): 주문 생성 API 추가` |
| 깨는 변경 | 종류 뒤에 `!`를 붙이고 본문에 `BREAKING CHANGE:`를 쓴다. 서비스 사이 API를 깨는 변경은 ADR이 먼저 있어야 한다 (STD-010) |
| 출처 | 본문 끝 `Refs:`에 spec 폴더와 관련 ID(UC, BR, TC, STD, ADR)를 적는다 |

## 4. PR

- **하나의 PR에는 spec 하나**(또는 그 단계 하나)를 담는다. spec.md, plan.md, tasks.md와 코드가 같은 PR에 들어간다.
- spec, plan, tasks를 먼저 커밋해 **draft PR**로 열고, 구현을 같은 PR에 이어서 올린다. 그래야 구현 전에 spec을 검토할 수 있다.
- PR 제목은 커밋 메시지 형식을 따른다. squash 병합하면 제목이 main의 커밋 메시지가 된다.
- 본문은 [PR 템플릿](../../.github/pull_request_template.md)을 채운다.
- **병합은 squash merge만** 쓴다. 병합 후 브랜치를 지운다.
- **검토**: 지금은 혼자 개발하므로 PR을 열고 스스로 검토한다. Claude Code의 `/code-review`로 한 번 더 본다. 협업자가 생기면 `services/`에 CODEOWNERS를 둔다 (ADR-0001).
- **필수 검사(CI)**: 빌드, 단위·Mapper·서비스 통합·계약·ArchUnit 테스트([testing.md](testing.md)의 "매 빌드"). kind E2E는 필수가 아니다. CI 설정(`.github/workflows/`)은 첫 서비스 코드가 생길 때 만든다.

## 5. 처음 한 번 할 일

| 일 | 상태 |
|---|---|
| `git init` | 완료 (2026-10-04, 사용자) |
| 기본 브랜치를 `main`으로 | 완료 (2026-10-04). 커밋이 없던 `master`를 `main`으로 바꿨다 |
| 앞으로 만드는 저장소의 기본 브랜치도 `main`으로: `git config --global init.defaultBranch main` | 선택. 지금 전역 설정은 `master`다 |
| 루트 `.gitignore` | 완료 (2026-10-04) |
| PR 템플릿 `.github/pull_request_template.md` | 완료 (2026-10-04) |
| GitHub 원격 저장소 만들기 | 완료 (2026-10-04, 사용자). 공개 저장소 https://github.com/Ghangseok/msa-example |
| `gh auth login` | 완료 (2026-10-04, 사용자) |
| 병합 설정 (6-1절) | 완료 (2026-10-04, 사용자). squash 기본 메시지도 PR 제목과 본문으로 설정되어 있다 (2026-10-05 `gh api`로 확인) |
| 첫 커밋, `git remote add origin`, 첫 push | 완료 (2026-10-04). 커밋 `0b5b8c8`. 규칙을 켜기 전이라 main에 바로 올렸다 |
| main 규칙(ruleset) 켜기 (6-2절) | 완료 (2026-10-04, 사용자). 이제부터 main은 PR로만 바꾼다 |
| 필수 검사(CI)를 규칙에 추가 | CI를 만든 뒤에 |
| spec-kit git 확장 켜기 | 선택 |

## 6. GitHub 설정

공개 저장소라서 GitHub 무료 요금제에서도 규칙(ruleset)이 적용된다. 비공개 저장소였다면 적용되지 않는다. `[문헌]`

### 6-1. Settings > General > Pull Requests

| 항목 | 값 |
|---|---|
| Allow merge commits | 끔 |
| Allow squash merging | 켬. 기본 커밋 메시지는 **Pull request title and description** |
| Allow rebase merging | 끔 |
| Automatically delete head branches | 켬 |

squash 기본 메시지를 "제목과 본문"으로 두는 이유: PR 본문의 `Refs:` 줄(spec 폴더와 관련 ID)이 main의 커밋 메시지에 남아, `git log --grep "TC-003"`으로 어느 커밋이 그 기준을 다뤘는지 찾을 수 있다.

### 6-2. Settings > Rules > Rulesets — `main branch rule`

| 항목 | 값 |
|---|---|
| Enforcement status | **Active** (2026-10-04 켬, 2026-10-05 `gh api`로 확인) |
| Bypass list | 비움. 소유자도 main에 직접 push하지 못한다 |
| Target branches | Include default branch |
| Restrict deletions | 켬 |
| Require linear history | 켬 |
| Require a pull request before merging | 켬. Required approvals 0(혼자 개발, 자기 PR은 승인할 수 없다), Require conversation resolution 켬, Allowed merge methods는 Squash만 |
| Block force pushes | 켬 |
| Require status checks to pass | CI를 만든 뒤 켠다 |
| 나머지(Restrict creations/updates, signed commits, deployments, code scanning, code quality, coverage, Copilot review) | 끔 |

## 용어

이 절에는 2026-10-05 개정에서 처음 나온 용어만 적었다. 이 문서의 다른 용어는 나중에 채운다.

- **기능 폴더 (feature directory)**: spec-kit이 기능 하나의 산출물(spec.md, plan.md, tasks.md)을 두는 폴더. 예: `specs/001-place-order/`.
- **`SPECIFY_FEATURE_DIRECTORY`**: spec-kit 스크립트가 지금 작업할 기능 폴더를 알아내는 환경변수. 값을 주면 스크립트가 그 값을 `.specify/feature.json`에도 적어 둔다.
- **spec-kit git 확장 (git extension)**: `/speckit-specify`를 돌릴 때 git 브랜치를 자동으로 만들어 주는 spec-kit 추가 기능. 이 저장소에서는 꺼져 있다.
