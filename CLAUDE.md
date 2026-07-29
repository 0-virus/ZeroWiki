# ZeroWiki

개인 지식 도서관 SaaS. 스택: Spring Boot 모듈러 모놀리스 + 별도 Worker / Next.js + TypeScript / PostgreSQL(+pgvector) / S3 호환 스토리지.

## 최우선: 헌법을 먼저 읽어라

이 프로젝트의 모든 에이전트는 작업을 시작하기 전에 **반드시** 다음 파일을 읽는다. 예외 없다.

> **`.claude/CONSTITUTION.md`** — 절대 불변 헌법

헌법은 파일 소유권 경계, 문서 진실 공급원 서열, 사용자 승인이 필요한 미확정 계약, 기록·검증·보안 의무, **구조 변경 시 지침 동기화 의무(제7조)** 를 정한다. 어떤 에이전트도 헌법을 편집할 수 없다. 헌법과 하위 지침이 충돌하면 헌법이 우선한다.

## 역할별 지침

팀원으로 스폰되었다면 헌법을 읽은 직후 자기 역할 파일 3개를 읽는다.

| 역할 | 지침 | 상태 | 저널 |
| --- | --- | --- | --- |
| backend | `.claude/team/backend/CLAUDE.md` | `.claude/team/backend/STATE.md` | `.claude/team/backend/WORKLOG.md` |
| frontend | `.claude/team/frontend/CLAUDE.md` | `.claude/team/frontend/STATE.md` | `.claude/team/frontend/WORKLOG.md` |
| qa | `.claude/team/qa/CLAUDE.md` | `.claude/team/qa/STATE.md` | `.claude/team/qa/WORKLOG.md` |
| pm | `.claude/team/pm/CLAUDE.md` | `.claude/team/pm/STATE.md` | `.claude/team/pm/WORKLOG.md` |

팀 운영 방법(스폰 프롬프트 템플릿, 작업 분할 원칙)은 `.claude/team/README.md`, 에이전트 팀 기능 자체의 사용법은 `docs/에이전트-팀-운영-가이드.md`에 있다.

## 팀 스킬

| 스킬 | 용도 |
| --- | --- |
| `/brief` | 리더가 네 역할의 `STATE.md`·저널을 직접 읽고 종합해 지금 시작할 일을 보고한다. 세션 시작 시나 다음 작업을 정할 때 쓴다. 읽기 전용, 스폰 없음 |

정의는 `.claude/skills/{이름}/SKILL.md`에 있고 리더가 소유한다.

## 프로젝트 구조

```
ZeroWiki-SaaS/
├── CLAUDE.md                     이 파일. 프로젝트 전체 지침 (리더 소유)
├── .claude/
│   ├── CONSTITUTION.md           절대 불변 헌법 (전원 편집 금지)
│   ├── agents/                   역할 에이전트 정의 zerowiki-{역할}.md (리더 소유)
│   ├── skills/{이름}/SKILL.md    팀 스킬 정의 (리더 소유)
│   └── team/
│       ├── README.md             팀 운영 방법 (리더 소유)
│       └── {역할}/               CLAUDE.md · STATE.md · WORKLOG.md (각 역할 소유)
├── docs/                         기준 문서·검증 문서 (아래 표 참조)
├── backend/                      미생성. Spring Boot 앱 + Ingest Worker
├── frontend/                     미생성. Next.js + TypeScript
└── qa/                           미생성. 테스트 코드·평가 세트
```

`backend/`·`frontend/`·`qa/`는 아직 없다. **코드는 한 줄도 없으며 현재는 문서 단계다.** 없는 디렉터리를 있다고 가정하지 않는다.

## 기준 문서

숫자(미확정 건수, 갭 건수 등)는 이 표에 적지 않는다. 곧 낡고, 낡은 숫자는 근거로 인용되어 잘못된 판단을 낳는다. **건수가 필요하면 해당 문서의 표를 직접 세라.**

| 문서 | 내용 | 편집 |
| --- | --- | --- |
| `docs/ZeroWiki-MVP-서비스-기획서.md` | 제품 정의·원칙·MVP 범위·Phase. **진실 1순위** | pm |
| `docs/ZeroWiki-요구사항-정의서.md` | FR/NFR 요구사항. **12절 = 미확정(UD) 정본**, 13절 = 추적성 | **리더 전용** |
| `docs/ZeroWiki-ERD-초안.md` | 데이터 모델. 8절 = 미확정 사항 | backend |
| `docs/ZeroWiki-API-명세-초안.md` | REST API 계약. 18절 = 확정할 계약, 19절 = 요구사항 추적 | **리더 전용** |
| `docs/검증-백엔드-ERD-API-정합성.md` | 백엔드 교차 검증 결과 | backend |
| `docs/검증-프론트엔드-화면-API-갭.md` | 화면·API 갭 목록 | frontend |
| `docs/검증-QA-계약-확정-체크리스트.md` | 계약 권고안·테스트 변환·보안 매트릭스 | qa |
| `docs/PM-*.md` | 종합 현황·결정 요청서 등 pm 산출물 | pm |
| `docs/에이전트-팀-운영-가이드.md` | 에이전트 팀 기능 사용법 | 리더 |
| `docs/ZeroWiki-기획-인터뷰-전문.txt` | 기획서의 근거 사료 | **전원 금지** |
| `docs/WikiHub-경쟁서비스-조사.md` | 경쟁 서비스 조사 | **전원 금지** |

**미확정 항목(UD)의 정본은 `docs/ZeroWiki-요구사항-정의서.md` 12절 하나뿐이다.** API 명세 18절과 ERD 8절은 그 부분집합이며, 충돌하면 12절이 이긴다. UD 번호는 문서 간 공통 식별자다.

## 작업별 참고 문서

무엇을 하려는지에 따라 먼저 열 문서가 다르다. 목록에 없는 문서를 근거로 삼지 말라는 뜻이 아니라, **이 문서를 안 보고 시작하면 반드시 헛돈다**는 뜻이다.

| 하려는 일 | 반드시 먼저 열 것 |
| --- | --- |
| 데이터 모델·스키마 설계 | ERD → 요구사항 정의서 5~6절 → API 명세(계약 정합) |
| API 엔드포인트 설계·변경 | API 명세 해당 절 → 요구사항 정의서 13절 추적성 → ERD |
| 화면 설계 | 기획서 16절(필수 화면) → API 명세(호출할 엔드포인트) → 검증-프론트엔드 갭 |
| 테스트·검증 기준 작성 | 기획서 3절(MVP 검증 기준) → 검증-QA 체크리스트 3절 → 요구사항 정의서 |
| 보안 설계·리뷰 | 기획서 18절 → API 명세 15절 → 요구사항 정의서 6.1절(NFR-SEC) |
| 미확정 항목 확인·권고 | **요구사항 정의서 12절(정본)** → 해당 문서의 미확정 절 |
| 범위·우선순위 판단 | 기획서 22절(MVP 제외)·23절(Phase) → API 명세 17절 |
| 팀 현황 파악 | 각 역할 `STATE.md` → `WORKLOG.md`(필요할 때만) |

**절 번호를 인용할 때는 그 절을 실제로 열어 확인한다.** 접두사 없는 번호(`13·14·15번` 등)는 요구사항 ID가 아니라 그 절 내부의 항목 번호일 수 있다. 확인 없이 단정한 인용은 헌법 제2조 5항 위반이다.

## 공통 규칙

- 응답 언어는 한국어. 기술 용어와 코드 식별자는 원문 유지.
- 문서 인용은 `파일 경로 + 절 번호`로 한다.
- **커밋·브랜치는 헌법 제8조를 따른다.** 스캐폴딩 단계(현재)는 `main`에 **리더만** 커밋한다. 사용자가 스캐폴딩 완료를 선언하면 리더가 `dev`를 분기하고, 그 뒤로는 각 역할이 **자기가 편집한 파일만** `dev`에 직접 커밋한다. 브랜치 조작과 `git push`는 단계와 무관하게 리더 전용이다.
- 커밋 메시지는 `<type>: <내용>` 형식이며 type은 `feat`(기능 구현) / `fix`(기능 수정) / `chore`(문서 작업 등) / `refactor`(리팩토링)다. 상세 내용은 빈 줄 뒤에 덧붙인다.
- 표시 모드는 `in-process` 고정 (Windows Terminal은 분할 창 미지원).
- **파일을 옮기거나 이름을 바꾸거나 문서 구조를 바꿨으면 같은 턴 안에 이 파일과 자기 역할 지침에 반영한다**(헌법 제7조). 미반영 구조 변경은 미완료 작업이다. 타 역할 소유 파일은 `SendMessage`로 요청하고 그 사실을 `WORKLOG.md`에 남긴다.
