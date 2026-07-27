# ZeroWiki

개인 지식 도서관 SaaS. 스택: Spring Boot 모듈러 모놀리스 + 별도 Worker / Next.js + TypeScript / PostgreSQL(+pgvector) / S3 호환 스토리지.

## 최우선: 헌법을 먼저 읽어라

이 프로젝트의 모든 에이전트는 작업을 시작하기 전에 **반드시** 다음 파일을 읽는다. 예외 없다.

> **`.claude/CONSTITUTION.md`** — 절대 불변 헌법

헌법은 파일 소유권 경계, 문서 진실 공급원 서열, 사용자 승인이 필요한 미확정 계약, 기록·검증·보안 의무를 정한다. 어떤 에이전트도 헌법을 편집할 수 없다. 헌법과 하위 지침이 충돌하면 헌법이 우선한다.

## 역할별 지침

팀원으로 스폰되었다면 헌법을 읽은 직후 자기 역할 파일 3개를 읽는다.

| 역할 | 지침 | 상태 | 저널 |
| --- | --- | --- | --- |
| backend | `.claude/team/backend/CLAUDE.md` | `.claude/team/backend/STATE.md` | `.claude/team/backend/WORKLOG.md` |
| frontend | `.claude/team/frontend/CLAUDE.md` | `.claude/team/frontend/STATE.md` | `.claude/team/frontend/WORKLOG.md` |
| qa | `.claude/team/qa/CLAUDE.md` | `.claude/team/qa/STATE.md` | `.claude/team/qa/WORKLOG.md` |

팀 운영 방법(스폰 프롬프트 템플릿, 작업 분할 원칙)은 `.claude/team/README.md`, 에이전트 팀 기능 자체의 사용법은 `docs/에이전트-팀-운영-가이드.md`에 있다.

## 기준 문서

| 문서 | 내용 |
| --- | --- |
| `docs/ZeroWiki-MVP-서비스-기획서.md` | 제품 정의·원칙·MVP 범위·Phase (진실 1순위) |
| `docs/ZeroWiki-ERD-초안.md` | 데이터 모델 (8절 = 미확정 10건) |
| `docs/ZeroWiki-API-명세-초안.md` | REST API 계약 (18절 = 미확정 10건, 리더 전용 편집) |
| `docs/검증-백엔드-ERD-API-정합성.md` | 백엔드 교차 검증 결과 |
| `docs/검증-프론트엔드-화면-API-갭.md` | 화면·API 갭 12건 |
| `docs/검증-QA-계약-확정-체크리스트.md` | 계약 권고안·테스트 변환·보안 매트릭스 |

## 공통 규칙

- 응답 언어는 한국어. 기술 용어와 코드 식별자는 원문 유지.
- 문서 인용은 `파일 경로 + 절 번호`로 한다.
- 커밋은 리더만 한다.
- 표시 모드는 `in-process` 고정 (Windows Terminal은 분할 창 미지원).
