## 무엇을

<!-- 이 PR이 하는 일을 한두 줄로 -->

## 관련

<!-- 이 줄은 squash 커밋 메시지에 그대로 남는다. 예: Refs: specs/001-place-order, UC-001, TC-003, STD-005 -->
Refs: specs/NNN-..., 

## 확인

- [ ] spec.md, plan.md, tasks.md와 코드가 서로 맞는다
- [ ] 관련 TC를 검증하는 테스트가 있고 통과한다 (테스트 이름에 TC ID)
- [ ] 서비스 사이 API나 이벤트를 바꿨다면 `contracts/` 현재본도 고쳤다 (STD-014)
- [ ] API는 추가하는 방식으로만 바꿨다. 깨는 변경이면 ADR이 있다 (STD-010)
- [ ] 원격 호출이 DB 트랜잭션 안에 있지 않다 (STD-018)
- [ ] 제한 시간, 주소, 비밀번호를 코드에 적지 않았다 (STD-004, STD-009)
- [ ] 기계 검사가 잡지 못하는 19c 이후 SQL 문법(FROM 없는 SELECT, GROUP BY의 별칭, VALUES로 여러 행 넣기, UPDATE의 조인)을 쓰지 않았다 (`docs/standards/coding-conventions.md` 3-8절 "기계로 검사하는 규칙")
- [ ] 검사를 끄는 표시의 허용 목록을 바꿨다면 PR 본문에 이유를 적었다
- [ ] `docs/`의 기준 문서가 바뀌어야 한다면 함께 고쳤다
