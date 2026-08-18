# ZeroWiki 팀 종합 현황

최종 갱신: 2026-08-18 20:38 +09:00
대상: 리더 + 팀 전체
상세 실행 근거: `docs/PM-Phase1-실행-계획.md`

> 이 문서는 최신 snapshot만 유지한다. 2026-07-29~08-11의 scaffold 준비·Phase 1 착수 전 판단은 `.claude/team/pm/WORKLOG.md`에 보존한다.

## 1. 최종 판정

**Phase 1은 M0~M7과 AC-1·2·4·5 및 보안 gate를 모두 통과해 `PASS/완료`다. 실제 Phase 1 blocker는 없다.**

기획서 23절의 Phase 1 개인 도서관 코어와 API 명세 17절 1~7번 순서 안에서 인증·도서관·원본·Ingest·지식 객체·변경 검토·검색·AI 사서를 완결했다. Lint·Notion·URL·과금·알림·삭제/복구·공개·fork·그래프·사용자 평가는 Phase 2~4로 유지해 범위를 앞당기지 않았다.

## 2. 역할별 최신 근거

| 역할 | 최종 상태 | 실제 근거 | Phase 1 차단 |
| --- | --- | --- | --- |
| backend | **PASS/완료** | Gradle 9.7.0 wrapper 단일 build, `clean test` 15 suites/39 tests failures/errors/skips 0, `build bootJar` exit 0 | 없음 |
| frontend | **PASS/완료** | Next.js 14.2.35 build 17 routes, typecheck/lint exit 0, `/libraries/**` 실제 API 흐름 | 없음 |
| qa | **PASS/완료** | 100문서 live, M7 security 8/8, holdout validator 70/70 PASS | 없음 |
| pm | **PASS/완료** | 기획서 23절·요구사항 9/12절·API 17절과 M0~M7/AC gate 교차 감사 | 없음 |

frontend의 legacy `/`·`/review-changes`는 mock showcase다. 실제 Phase 1 제품 흐름은 `/libraries/**`이며 완료 판정을 막지 않지만, 두 legacy route를 제품 진입점으로 승격할 때는 별도 정리가 필요하다.

## 3. 인수 gate

| Gate | 판정 | 근거 |
| --- | --- | --- |
| AC-1 | **PASS** | 100문서 `done=100/100` |
| AC-2 | **PASS** | `published=100`, `topics=6` |
| AC-4 | **PASS** | `citations=5`와 page/claim/source version/locator trace |
| AC-5 | **PASS** | Batch 70/70, recall 0.800, accuracy 0.843, sampled FPR 0, Wilson upper 0.114, macro-F1 0.801 |
| 비용 | **PASS** | Batch actual `$0.2108`; 실행 후 누적 `$0.76724`, cap `$0.80`, 잔여 `$0.03276` |
| 보안 | **PASS** | M7 security 8/8 |

의존 사슬은 **M0 Gradle build gate → M1 → M2 → M3 100-doc Ingest → M4 knowledge → M5 change-set/version → M6 search/AI → M7 보안·AC → AC-5 holdout → Phase 1 완료**로 닫혔다.

## 4. 완료와 분리된 결정·운영 위험

다음은 Phase 1 blocker가 아니다.

1. production provider/model 승격과 production release.
2. 모순 자동 판정·자동 apply 허용 여부.
3. backend 과거 “M3 knowledge” 명칭과 기획서/API 순서의 M3 Ingest·M4 knowledge 명칭 정합화.
4. listener가 내려간 환경에서 동일 live run을 즉시 재현할 수 없는 운영 위험.
5. 사용자 결정 대기 UD-03·10·19·20·22와 Phase 4 사용자 검증 UD-26.

holdout PASS는 production provider/model 승인이나 자동 모순 apply 승인을 뜻하지 않는다. PM 종합은 사용자 결정을 대체하지 않는다.

## 5. 정본 경계

- 제품 범위·Phase: `docs/ZeroWiki-MVP-서비스-기획서.md` 22~23절
- 완료 판정: `docs/ZeroWiki-요구사항-정의서.md` 9절
- 미확정 사항: `docs/ZeroWiki-요구사항-정의서.md` 12절
- 구현 순서: `docs/ZeroWiki-API-명세-초안.md` 17절
- 역할 최신 상태: `.claude/team/{backend,frontend,qa,pm}/STATE.md`
- 시간순 판단: 각 역할 `WORKLOG.md`와 `.claude/team/JOURNAL.md`

PM은 정본 계약과 타 역할 파일을 편집하지 않는다.
