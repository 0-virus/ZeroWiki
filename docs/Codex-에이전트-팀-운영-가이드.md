# Codex 에이전트 팀 운영 가이드

이 문서는 기존 ZeroWiki Claude 팀을 Codex에서 같은 역할·소유권·상태 기록으로 운영하는 방법이다. Claude 전용 명령을 흉내 내는 별도 런타임이 아니라 Codex의 project instructions, custom agents, subagent threads, repository skills를 사용한다.

## 1. 구성

| 목적 | Claude | Codex | 공동 정본 |
| --- | --- | --- | --- |
| 저장소 지침 | `CLAUDE.md` | `AGENTS.md` | `.claude/CONSTITUTION.md`와 기준 문서 |
| 역할 정의 | `.claude/agents/*.md` | `.codex/agents/*.toml` | 역할별 `.claude/team/{역할}/CLAUDE.md` |
| 현재 상태 | 역할별 `STATE.md` | 같은 파일을 직접 사용 | `.claude/team/{역할}/STATE.md` |
| 작업 이력 | 역할별 `WORKLOG.md` | 같은 파일을 직접 사용 | `.claude/team/{역할}/WORKLOG.md` |
| 팀 종합 이력 | `.claude/team/JOURNAL.md` | 같은 파일을 직접 사용 | `.claude/team/JOURNAL.md` |
| 브리핑 스킬 | `.claude/skills/brief` | `.agents/skills/brief` | 동일한 읽기·판정 절차 |

상태 파일을 `.codex` 아래에 복사하지 않는다. 두 실행기가 같은 STATE·WORKLOG를 읽고 쓰므로 재개 지점과 판단 이력이 갈라지지 않는다.

## 2. 바로 시작하기

저장소 루트에서 Codex를 시작한다. Codex는 루트 `AGENTS.md`, `.codex/config.toml`, `.codex/agents/*.toml`, `.agents/skills/**`를 프로젝트 설정으로 발견한다. 기존 세션에서 파일이 바로 보이지 않으면 Codex를 재시작한다.

현황만 확인할 때:

```text
$brief를 사용해 backend, frontend, qa, pm의 현재 상태와 지금 해야 할 한 가지를 보고해.
```

팀 작업을 시작할 때:

```text
ZeroWiki 에이전트 팀으로 이 작업을 수행해. backend, frontend, qa 역할에
소유 영역이 겹치지 않게 분배하고 독립 작업은 병렬 실행해. 모든 결과를 기다린 뒤
주 에이전트가 실제 파일과 검증 출력을 다시 확인하고 하나의 결과로 종합해: <작업>
```

특정 역할만 사용할 때:

```text
zerowiki-backend custom agent에게 <작업>을 맡겨. 헌법과 backend 역할 상태를
먼저 읽게 하고, 결과와 검증 근거를 받은 뒤 네가 직접 확인해.
```

PM까지 필요한 예:

```text
backend, frontend, qa가 각각 계약 영향을 검토하게 하고, 결과가 모인 뒤
zerowiki-pm이 사용자 결정 요청서와 우선순위를 종합하게 해.
```

Codex CLI에서는 `/agent`로 실행 중인 하위 에이전트 스레드를 열어 상태와 결과를 확인할 수 있다. 실행 중인 에이전트에는 주 에이전트를 통해 후속 지시·방향 수정·중단 요청을 보낸다.

## 3. 역할별 동작

각 custom agent는 시작 시 다음 순서를 지킨다.

1. `.claude/CONSTITUTION.md`
2. 루트 `CLAUDE.md`
3. `.claude/team/{역할}/CLAUDE.md`
4. `.claude/team/{역할}/STATE.md`
5. 작업에 필요한 기준 문서의 실제 절
6. 역사적 판단이 필요할 때만 `WORKLOG.md`와 통합 `JOURNAL.md`

작업이 끝나면 자기 소유 파일만 수정하고, 편집 결과를 다시 읽고, 검증 명령과 출력을 보고한다. 의미 있는 작업이면 `STATE.md`를 갱신하고 `WORKLOG.md`에 append-only 기록을 남긴다. 주 에이전트는 보고를 그대로 믿지 않고 중요한 산출물을 직접 확인한다.

## 4. Claude 팀 기능과의 운용 차이

| Claude 팀 개념 | Codex 운용 |
| --- | --- |
| 팀원 spawn | 역할 이름의 custom agent를 subagent thread로 생성 |
| `SendMessage` | 주 에이전트가 실행 중 스레드에 후속 지시나 방향 수정을 전달 |
| 공유 task list | 주 에이전트의 계획과 각 thread 상태로 추적 |
| 팀원 간 직접 조율 | 주 에이전트를 허브로 전달해 소유권 충돌을 예방 |
| 세션 종료 후 재개 | 새 역할 thread가 공동 `STATE.md`와 `WORKLOG.md`에서 재개 |
| `/brief` | Codex의 `$brief` 저장소 스킬 |

두 시스템의 API와 UI는 같지 않지만 역할 경계, 작업 분할, 상태 지속, 완료 검증은 같은 규약으로 유지된다.

## 5. 분할 기준

- 한 파일 또는 한 역할의 국소 작업: 주 에이전트가 직접 수행한다.
- 둘 이상의 역할 소유 영역에 걸친 작업: 역할별 custom agent에 분할한다.
- 읽기·조사·검증처럼 독립적인 작업: 병렬 실행한다.
- 같은 파일을 만지는 작업 또는 선행 산출물 의존 작업: 순차 실행한다.
- pm: 구현 역할의 산출물이 모였거나 사용자 결정 문서를 만들 때만 실행한다.

`.codex/config.toml`은 최대 4개의 하위 스레드를 허용한다. 실제 동시 실행 수는 사용 중인 Codex 환경의 상위 제한을 넘을 수 없다. 상위 제한이 더 작으면 역할 하나가 끝난 뒤 다음 역할을 실행한다.

## 6. 안전과 검증

- 헌법의 편집 금지 파일을 수정하지 않는다.
- 다른 역할 소유 파일은 주 에이전트가 해당 역할에 재배정한다.
- 사용자 승인 대상 계약은 임의로 확정하지 않는다.
- 에이전트들은 부모 세션의 sandbox와 approval 정책을 기본 상속한다. 작업 시작 전에 부모 세션 권한을 확인한다.
- 문서·설정 변경은 내용 검색, 코드 변경은 타입체크·빌드와 실제 동작 확인, 이름 변경은 옛 참조의 전역 검색으로 검증한다.
- Git 작업은 헌법과 사용자 지시에 따른다. 단순 팀 실행 요청은 commit, branch 변경, push 권한을 포함하지 않는다.

## 7. 파일별 책임

| 파일 | 책임 |
| --- | --- |
| `AGENTS.md` | Codex가 자동 로드하는 저장소 전체 운영 규칙 |
| `.codex/config.toml` | subagent 활성화와 동시 thread 상한 |
| `.codex/agents/zerowiki-*.toml` | 네 역할의 발견 가능한 custom agent 정의 |
| `.agents/skills/brief/SKILL.md` | 현재 상태 종합 절차 |
| `.agents/skills/brief/agents/openai.yaml` | `$brief`의 UI 표시 정보 |

역할 책임을 바꾸면 Claude와 Codex 정의를 함께 점검한다. 단, 현재 상태나 긴 이력은 공통 정본에만 기록한다.

## 8. 빠른 자체 점검

```powershell
Get-Content -Raw -Encoding UTF8 AGENTS.md
Get-Content -Raw -Encoding UTF8 .codex/config.toml
Get-ChildItem .codex/agents/*.toml | Select-Object Name
Get-Content -Raw -Encoding UTF8 .agents/skills/brief/SKILL.md
```

Codex를 재시작한 뒤 `$brief`가 제안되고, 팀 요청에서 `zerowiki-backend`, `zerowiki-frontend`, `zerowiki-qa`, `zerowiki-pm`을 선택할 수 있으면 발견 구성이 완료된 것이다.
