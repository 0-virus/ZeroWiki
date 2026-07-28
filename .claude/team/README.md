# ZeroWiki 에이전트 팀 운영

이 디렉터리는 팀 에이전트의 지침·상태·저널을 담는다. 에이전트 팀 기능 자체의 사용법은 `docs/에이전트-팀-운영-가이드.md`를 본다.

## 계층 구조

```
.claude/CONSTITUTION.md              절대 불변 헌법 — 편집 불가, 사용자만 변경
  └─ .claude/team/{역할}/CLAUDE.md   역할 지침 — 해당 에이전트가 수정 가능
       ├─ STATE.md                   현재 상태 스냅샷 (덮어쓰기)
       └─ WORKLOG.md                 작업 저널 (append-only)
```

충돌 시 항상 헌법이 우선한다. 하위 지침에 헌법과 어긋나는 조항이 있으면 그 조항은 무효다.

루트 `CLAUDE.md`는 모든 세션에 자동 로드되며 헌법을 가리키는 포인터 역할을 한다.

## 파일 소유권 요약 (헌법 제1조)

| 소유자 | 편집 가능 |
| --- | --- |
| backend | `backend/**`, `.claude/team/backend/**`, `docs/ZeroWiki-ERD-초안.md`, `docs/검증-백엔드-ERD-API-정합성.md` |
| frontend | `frontend/**`, `.claude/team/frontend/**`, `docs/검증-프론트엔드-화면-API-갭.md` |
| qa | `qa/**`, `.claude/team/qa/**`, `docs/검증-QA-계약-확정-체크리스트.md` |
| pm | `.claude/team/pm/**`, `docs/ZeroWiki-MVP-서비스-기획서.md`, `docs/PM-*.md` |
| 리더 | `CLAUDE.md`, `docs/ZeroWiki-API-명세-초안.md`, `docs/에이전트-팀-운영-가이드.md`, `.claude/agents/**`, 모든 git 조작 |
| 편집 금지 | `.claude/CONSTITUTION.md`, `docs/ZeroWiki-기획-인터뷰-전문.txt`, `docs/WikiHub-경쟁서비스-조사.md`, `.omc/**` |

## 팀원 타입

`.claude/agents/`에 네 정의가 있다. 팀원과 서브에이전트 양쪽에 재사용한다.

| 타입 | 역할 |
| --- | --- |
| `zerowiki-backend` | 데이터 모델·서버·Worker |
| `zerowiki-frontend` | 화면·클라이언트·UX |
| `zerowiki-qa` | 검증·테스트·보안·계약 통합 |
| `zerowiki-pm` | 종합·기획 피드백·결정 추적·우선순위 권고 (헌법 v1.1 신설) |

정의 본문은 팀원 시스템 프롬프트에 **추가**되며(대체 아님) 필독 파일 경로를 포함한다. `.claude/team/` 아래 파일은 계층 자동 로드 대상이 아니므로 이 경로 명시가 로드를 보장한다.

주의: 서브에이전트 정의의 `skills`·`mcpServers` frontmatter는 팀원으로 실행될 때 적용되지 않는다(가이드 4.3).

## 스폰 프롬프트 템플릿

팀원은 리더의 대화 기록을 상속하지 않는다. 읽어야 할 문서 경로를 항상 명시한다(가이드 10.1).

```text
zerowiki-backend 타입으로 backend 라는 이름의 팀원을 스폰해라. 프롬프트:
"너는 ZeroWiki의 backend다. 먼저 .claude/CONSTITUTION.md,
.claude/team/backend/CLAUDE.md, .claude/team/backend/STATE.md를 순서대로 읽어라.
그다음 <작업 내용>. 참조 문서: <경로 + 절 번호>.
결과는 <산출물 경로>에 쓰고, 턴 종료 전 STATE.md와 WORKLOG.md를 갱신해라."
```

frontend·qa도 동일한 형태로 경로만 바꾼다. 팀원 이름은 `backend`·`frontend`·`qa`로 고정해 두면 이후 지목이 쉽다.

## 운영 원칙

- 표시 모드는 `in-process` 고정 (Windows Terminal은 분할 창 미지원, 가이드 5절).
- 팀 크기 3~4명(backend·frontend·qa + 필요 시 pm). 팀원당 5~6개 작업이 적정(가이드 10.2, 10.3).
- **pm은 상시 스폰 대상이 아니다.** 세 역할의 산출물이 쌓여 종합할 것이 생겼을 때, 또는 사용자 결정 요청서가 필요할 때 스폰한다. 종합할 대상이 없는 상태에서 pm을 띄우면 빈 보고서만 나온다.
- 같은 파일을 두 팀원이 편집하지 않도록 소유권 표를 지킨다(가이드 10.5).
- 리더가 위임 대신 직접 구현하기 시작하면 "팀원들이 작업을 마칠 때까지 기다려라"로 교정한다(가이드 10.6).
- in-process 팀원은 `/resume`으로 복원되지 않는다. 세션이 끊기면 팀원을 새로 스폰하고, 각 역할의 `STATE.md`가 재개 지점을 제공한다(가이드 13절).
- 위험한 작업에는 계획 승인을 요구한다: "변경 전에 계획 승인을 요구해라"(가이드 4.4).

## 현재 상태 (2026-07-27)

Phase 0 이전. 코드 없음. 문서 검증 3종 완료.

**최우선 차단 요인:** 미확정 계약 20건 (API 명세 18절 10건 + ERD 8절 10건). 이 중 8건은 사용자 결정 필요.

| 역할 | 다음 작업 |
| --- | --- |
| backend | ERD 수정 3건 반영(revision 필드 / idempotency_records / 복합 인덱스), 계약 의견 제출 |
| frontend | 갭 12건 소유자별 전달, 계약 의견 제출, 와이어프레임 |
| qa | 양쪽 의견 통합, 검증 기준 테스트 시나리오화, 리더 보고 |
| pm | 최초 종합(`docs/PM-종합-현황.md`), 사용자 결정 8건 결정 요청서화, 기획서 정합성 점검 |

각 역할의 `STATE.md`가 정본이다.

pm 신설은 2026-07-28, 헌법 v1.1. 기획서 소유권이 리더 → pm으로 이관되었다.
