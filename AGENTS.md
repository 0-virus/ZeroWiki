# ZeroWiki Codex 운영 지침

이 저장소에서 Codex는 기존 Claude 팀의 문서·상태를 다시 만들지 않고 그대로 공유한다. 작업을 시작하기 전에 반드시 `.claude/CONSTITUTION.md`와 루트 `CLAUDE.md`를 순서대로 읽는다. 헌법은 편집하지 않는다. 이 파일과 하위 지침이 충돌하면 헌법이 우선한다.

## 역할과 정본

| 역할 | Codex 에이전트 | 필독 순서 | 소유 영역 |
| --- | --- | --- | --- |
| backend | `zerowiki-backend` | `.claude/team/backend/CLAUDE.md` → `STATE.md` | `backend/**`, backend 소유 문서·평가 자료 |
| frontend | `zerowiki-frontend` | `.claude/team/frontend/CLAUDE.md` → `STATE.md` | `frontend/**`, frontend 소유 검증 문서 |
| qa | `zerowiki-qa` | `.claude/team/qa/CLAUDE.md` → `STATE.md` | `qa/**`, QA 소유 체크리스트 |
| pm | `zerowiki-pm` | `.claude/team/pm/CLAUDE.md` → `STATE.md` | PM 기획·종합 문서 |

역할의 현재 상태와 이력은 `.claude/team/{역할}/STATE.md`, `WORKLOG.md`가 공통 정본이다. `.codex` 아래에 복제본을 만들지 않는다. 전체 판단 이력은 `.claude/team/JOURNAL.md`, 제품·계약의 진실 공급원은 루트 `CLAUDE.md`의 기준 문서 표를 따른다.

## 팀 실행 규칙

- 사용자가 팀·병렬 작업을 명시하거나 작업이 서로 다른 역할 소유 영역 둘 이상에 걸치면 해당 custom agent를 위임한다. 단일 영역의 작은 작업은 주 에이전트가 직접 처리한다.
- 주 에이전트가 작업 분해, 충돌 없는 파일 배정, 의존성 관리, 결과 검증과 최종 종합을 소유한다.
- 서로 독립적인 읽기·검증은 병렬화한다. 같은 파일을 둘 이상의 에이전트에게 동시에 맡기지 않는다.
- backend·frontend·qa를 기본 역할로 사용하고, pm은 여러 역할 산출물을 종합하거나 사용자 결정 요청을 만들 때만 사용한다.
- 에이전트에게 헌법 → 역할 `CLAUDE.md` → 역할 `STATE.md` 순서로 읽도록 하고, 과거 근거가 필요할 때만 `WORKLOG.md`를 추가로 읽힌다.
- 타 역할 파일 변경이 필요하면 직접 편집하지 말고 주 에이전트에게 보고한다. 주 에이전트가 해당 소유 역할에 후속 작업을 보낸다.
- 모든 하위 에이전트 결과를 기다린 뒤 실제 파일을 다시 읽고 검증한다. 근거 없는 완료 보고는 접수하지 않는다.

Codex 역할 정의는 `.codex/agents/*.toml`, 저장소 스킬은 `.agents/skills/*/SKILL.md`, 상세 실행법은 `docs/Codex-에이전트-팀-운영-가이드.md`에 있다.

## 공통 작업 규약

- 응답은 한국어로 작성하고 기술 용어·코드 식별자는 원문을 유지한다.
- 사용자 지시가 기존 문서와 충돌하면 사용자 지시가 우선한다. 미확정 항목은 임의로 확정하지 않는다.
- 파일 소유권, 편집 금지 목록, Git 권한은 `.claude/CONSTITUTION.md`와 `.claude/team/README.md`를 따른다.
- 수치나 완료 상태는 정본과 실제 산출물을 직접 확인한다. 행 번호에 의존하지 말고 내용 검색으로 잔존 참조를 검증한다.
- 편집한 파일을 다시 읽고, 문서·설정은 검색 결과, 코드는 타입체크·빌드·동작 확인 결과를 완료 보고에 포함한다.
- 의미 있는 역할 작업은 종료 전에 해당 `STATE.md`를 최신 스냅샷으로 갱신하고 `WORKLOG.md`에 append-only로 남긴다. 구조 변경은 같은 턴에 루트 지침과 해당 역할 지침에도 반영한다.
- 사용자가 명시하지 않은 커밋·브랜치 변경·push를 하지 않는다.

## 브리핑

세션 시작, 장기 중단 후 복귀, 다음 작업 선정에는 `$brief`를 사용한다. 이 스킬은 주 에이전트가 직접 수행하는 읽기 전용 절차이며 하위 에이전트를 생성하거나 파일을 수정하지 않는다.
