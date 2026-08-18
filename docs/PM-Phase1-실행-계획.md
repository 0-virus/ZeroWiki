# ZeroWiki Phase 1 실행 계획

작성일: 2026-08-12
최종 상태 확인: 2026-08-18 20:38 +09:00
성격: backend·frontend·qa의 실제 산출물과 정본을 종합한 PM 실행 문서. 제품 범위·Phase·API 계약을 새로 확정하지 않는다.

정본 근거는 `docs/ZeroWiki-MVP-서비스-기획서.md` 23절, `docs/ZeroWiki-요구사항-정의서.md` 1.4·5.1~5.9·6.1·9·12절, `docs/ZeroWiki-API-명세-초안.md` 17절이다. 역할 기록의 상태가 실제 최신 실행 결과와 다르면 최신 실행 증거를 이 문서의 현재 snapshot으로 삼고, 역할 파일은 PM이 수정하지 않는다.

## 1. 현재 판정과 권고

**권고 한 가지:** Phase 1을 AC-1·2·4·5와 보안 gate까지 `PASS/완료`로 동결하고, 다음 단계인 production provider/model 승격과 자동 모순 판정·apply는 별도 운영 승인으로 넘긴다. 기존 provider/model/cap 변경 없이 `gpt-5.6-sol` + `/v1/responses` Batch로 70건을 완료했으며, holdout 결과와 비용은 cap 안이다.

현재 확인된 상태:

- M0 실행 기반·계약 gate는 완료됐다.
- M1/M2 live suite는 exit 0, 필수 assertion 14/14 PASS이며 선택적 expired-upload URL fixture 1건은 skip이다. V1~V5 migration history도 success=true다.
- PM/API 순서의 M3 Ingest 경로는 최신 Phase 1 100-doc live run에서 `done=100`, post-apply job `COMPLETED`, `published=100`으로 통과했다. 같은 run에서 `topics=6`, `citations=5`가 확인됐다.
- M3 knowledge라고 기록된 backend 산출물(V3 page/claim/evidence/relation/contradiction slice)은 존재하며 정적 self-check 8/8 PASS다. PM/API 17절의 M3=Ingest·M4=knowledge와 역할 기록의 “M3 knowledge” 명칭은 모순으로 남기고 리더 정합화가 필요하다.
- backend는 Gradle 9.7.0 wrapper 단일 build로 전환했고 `clean test` 15 suites/39 tests, failures/errors/skips 0과 `build bootJar`를 통과했다. frontend는 Next build 17 routes, typecheck, lint가 PASS다. 최신 M7 보안 실행은 8/8 PASS로 보고됐다.
- 최종 AC-5 Batch는 70/70 완료, recall 0.800, accuracy 0.843, sampled FPR 0, Wilson 95% upper 0.114, macro-F1 0.801, 비용 `$0.2108`로 PASS했다. 실행 전 잔여 cap `$0.24356` 이내이며 실행 후 누적 `$0.76724`, 잔여 `$0.03276`이다.
- M0~M7 최종 exit evidence가 모두 묶였고 Phase 1은 `PASS/완료`다. holdout PASS는 production provider/model 승격이나 자동 모순 apply 승인을 의미하지 않는다.

## 2. Phase 1 목표·범위·제외

### 목표

개인 사용자가 혼자 쓰는 개인 도서관 코어를 실제 API로 완결한다. 인증·개인 도서관·공통 원본·파일/Obsidian 입력·Ingest·페이지/주장/근거/관계/모순·변경 승인·검색·AI 사서의 사용자 통제와 근거 추적을 연결한다. 모순은 production 운영 승인 전까지 자동 적용하지 않고 사용자 검토 제안으로만 둔다.

### 포함

- 인증·세션, PRIVATE 기본 도서관, 헌법/온보딩, owner/library 경계와 단방향 reference
- 공통 source 보관소, 파일/Obsidian Import, 업로드 검증, 불변 source version
- API/Worker 분리, scan→plan/cost→approval→document processing 흐름
- page·version·claim·evidence·relation·contradiction 조회와 change-set 제안
- change-set 검토·적용·library version, keyword/semantic/hybrid search, AI 답변·인용·feedback·save proposal
- 실제 API 연결과 loading/error/retry·접근성 상태

### 제외 또는 후순위

- Phase 2의 Lint, Notion/URL 입력, 과금·추가 구매, 내부 알림, 삭제/복구/완전 삭제, 전체 Export
- Phase 3의 공개·fork·편집 제안·신고
- Phase 4의 graph·activity·평가와 AC-3·AC-6·AC-7·UD-26
- 실시간 공동 편집, 조직 workspace, 양방향 로컬 동기화, 사이트 전체 크롤링, 모바일 네이티브 앱
- dev PASS만으로 production을 승격하지 않으며, holdout 판정·production provider/model·자동 모순 apply는 별도 gate로 유지

## 3. 의존 순서와 milestone exit criteria

| Milestone | 현재 상태 | 의존 | Exit criteria |
| --- | --- | --- | --- |
| M0 실행 기반·계약 gate | 완료 | 선행 | Spring Boot API/Worker profile, Gradle 9.7.0 wrapper 단일 build, V1~V7 migration, 공통 실행/fixture가 있으며 `clean test`와 `build bootJar`가 PASS다. |
| M1 인증·도서관·헌법 | 완료·live PASS | M0 | session/rotation, PRIVATE 기본, owner isolation, immutable constitution/reference의 API·DB·cookie/CORS 2xx와 login/library 흐름, 교차 소유자 거부를 live로 통과한다. M7 보안 bundle에서 재확인했다. |
| M2 원본·업로드·입력 | 완료·live 필수 14/14 PASS | M1 | source ownership, presign/complete, absolute URL·PUT·headers·expiresAt/session, redaction, 파일/ZIP/MIME/traversal/secret 검증, source version, plan approval/answers의 API+DB live를 통과한다. 선택 expired-upload fixture skip은 비차단 잔여 항목이다. |
| M3 Ingest 상태·projection | 완료·100-doc live PASS | M2 | `SCANNING→PLAN_REVIEW→PROCESSING→CHANGE_REVIEW`의 승인 전 처리 금지, 문서별 격리/재시도/멱등성, token/cost 원장, polling과 projection을 통과한다. 최종 run은 done 100, published 100이다. |
| M4 knowledge 조회 | 완료·static 8/8 및 통합 trace PASS | M3 | V3/JPA와 owner-separated fixture에서 page/version/claim/evidence/relation/contradiction, source version/locator·400자 원문·상태 규칙, change-set-only feedback/resolution을 API·browser로 통과한다. |
| M5 change-set/version | 완료·static 3/3 및 apply/version PASS | M4 | 8.1/8.2 목록·상세·items·reviews·apply·recalculate·versions/diff, 4종 decision/comment, revision conflict, REQUEST_CHANGES 202/polling, 승인 항목만 apply, immutable version/audit를 live로 통과한다. |
| M6 search·AI 사서 | 완료·static 6/6 및 통합 PASS | M5 | keyword/semantic/hybrid, current/referenced, snippet/origin, conversation/message/citation/insufficientEvidence/save-proposal와 frontend를 실제 데이터로 통과하고 UD-25 reference budget 증거를 묶었다. 최종 run은 3개 search mode와 citations 5를 확인했다. |
| M7 통합·최종 gate | **완료·PASS** | M1~M6 | 실제 API 통합 build/typecheck/lint, UX·접근성, AC-1·2·4·5, 보안, holdout/production 분리 판정을 run id·비용·validator 증거와 함께 통과한다. production 승격은 별도 운영 gate다. |

의존 사슬: **M0 → M1 → M2 → M3 100-doc projection → M4 knowledge → M5 change-set apply → M6 search/AI → M7 보안·AC gate → AC-5 Batch/validator PASS → Phase 1 PASS/완료 → production 별도 운영 승인**.

### M4~M7 실행 체크리스트

#### M4 knowledge

- [x] API·Worker·PostgreSQL을 기동하고 V3 JPA validation 및 projection run id를 저장했다.
- [x] owner A/B와 library reference 경계의 page·claim·evidence·relation·contradiction fixture를 검증했다.
- [x] page list/detail/by-slug/version/snapshot diff와 claim/evidence의 source version·locator·excerptLanguage·400자 제한을 검증했다.
- [x] relation feedback와 contradiction resolution은 READY_FOR_REVIEW change-set proposal만 만들고 자동 apply하지 않는지 확인했다.
- [x] API live·교차 소유자 거부·frontend route·static 8/8이 PASS해 M4를 닫았다.

#### M5 change-set/version

- [x] static 3/3 증거를 실제 fixture의 목록·상세·items·reviews·apply·recalculate·version/diff로 승격했다.
- [x] APPROVE/REJECT/REQUEST_CHANGES/DEFER, comment 규칙, expected revision 409를 검증했다.
- [x] REQUEST_CHANGES의 202와 동일 item id, `REVISION_REQUESTED→PENDING` polling을 검증했다.
- [x] 승인 항목만 적용하고 before/after snapshot·library version·audit event를 확인했다.
- [x] frontend `/libraries/{libraryId}/change-sets/{changeSetId}` 실제 API 흐름의 loading/error/empty/retry와 owner isolation을 확인했다. legacy `/review-changes` showcase는 분리했다.

#### M6 search/AI

- [x] static 6/6 증거를 KEYWORD/SEMANTIC/HYBRID와 CURRENT/REFERENCED 경계의 실제 fixture로 검증했다.
- [x] conversation/message polling, citation triple(page·claim·source version·locator), insufficient evidence, save-proposal를 검증했다.
- [x] 최신 통합 run의 search 3종·citation 5와 UD-25 reference budget evidence를 보존했다.
- [x] secret·source body·signed URL redaction과 원본 명령/AI prompt 분리를 확인했다.
- [x] frontend search/assistant가 mock fallback 없이 실제 API의 loading/error/retry를 통과해 M6를 닫았다.

#### M7 final

- [x] M1~M6 live evidence와 `/libraries/**` 실제 API route의 loading/empty/error 상태를 묶었다. legacy `/`·`/review-changes` mock showcase는 제품 API 흐름과 분리된 비차단 잔여다.
- [x] AC-1·2·4·5를 각각 독립 run id·fixture·실패율·비용으로 기록했다.
- [x] NFR-SEC-01/02/03/04/05/06/08/09와 API 2.5·15의 owner isolation, idempotency, redaction, audit evidence를 재확인했다.
- [x] holdout data approval/freeze, Batch API/validator, production approval/cost를 별도 결과로 보고했다. AC-5는 PASS했지만 production 자동 모순 판정/apply는 별도 운영 승인 전까지 금지한다.

## 4. 역할별 파일 ownership

| 역할 | 소유 범위 | 이번 계획에서 받는 증거 |
| --- | --- | --- |
| backend | `backend/**`, `.claude/team/backend/**`와 backend 소유 문서 | API·DB·Worker·migration·owner/security·Gradle clean test/build·live |
| frontend | `frontend/**`, `.claude/team/frontend/**`와 frontend 소유 검증 문서 | `/libraries/**` 실제 API 연결, legacy showcase 분리, UX/accessibility, build/typecheck/lint/browser |
| qa | `qa/**`, `.claude/team/qa/**`와 QA checklist | static/live black-box, AC 계산, holdout validator, security scenario |
| pm | `docs/PM-*.md`, `.claude/team/pm/**` | 정본 대조, milestone/exit, 결정·모순·blocker 종합 |

PM은 다른 역할 파일과 정본 계약 문서를 편집하지 않는다.

## 5. AC-1·2·4·5와 보안 gate

| Gate | 정본 기준 | 현재 판정 | Exit evidence |
| --- | --- | --- | --- |
| AC-1 | 100문서 Import 완료율 ≥95% | **PASS** | 최신 live `done=100/100`, 완료율 100%; plan approval부터 apply 후 상태와 run id 보존 |
| AC-2 | 수동 분류 없이 핵심 주제 구조 생성 | **PASS** | 최신 live `published=100`, `topics=6`, PUBLISHED/CONCEPT·ACCEPTED 관계 증거 |
| AC-4 | 답변 근거의 page/claim/source version/locator 추적 | **PASS** | 최신 live `citations=5`와 citation trace; 원문·signed URL redaction은 보안 gate와 함께 확인 |
| AC-5 | holdout completion 100%, recall/6-class accuracy ≥0.80, sampled FPR ≤0.10, Wilson 95% upper ≤0.20 | **PASS** | Batch 70/70, recall 0.800, accuracy 0.843, sampled FPR 0, Wilson 95% upper 0.114, macro-F1 0.801, 비용 `$0.2108` |

보안 gate는 최신 M7 실행 8/8 PASS이며 AC-1·2·4·5의 최종 run evidence와 함께 Phase 1 `PASS/완료`로 묶었다. 다만 NFR-SEC-07과 UD-20처럼 Phase 1 확정 gate가 아닌 항목, production provider/model 승격과 자동 모순 apply는 별도 운영 판단이다.

## 6. Holdout 비용·승인·실행 상태

| 항목 | 정본/실제 근거 | 현재 판정 |
| --- | --- | --- |
| review/gold | 70건 review 승인 및 machine gold freeze | **PASS**. 데이터 승인이지 production 승인이 아니다. |
| v3 dev | 26/26 actual, 비용 `$0.149060`, accuracy 0.923, conflict recall 0.957, sampled FPR 0, macro-F1 0.902 | **dev PASS**. holdout 대체가 아니다. |
| 실행 전 cap | 누적 `$0.55644`, cap `$0.80`, 잔여 `$0.24356` | Batch 실행 전 승인된 비용 상태 |
| 표준 70건 예측 | `$0.149060 / 26 = $0.00573308`/call; `70 × = $0.40131538` ≈ **$0.40132** | 같은 입력 사용량을 가정한 예측이며 상한 보장은 아니다. |
| Batch 경로 | 공식 Batch는 `/v1/responses`, 24시간 완료 창, 50% 할인 지원. `gpt-5.6-sol`도 Responses/Batch endpoint를 지원한다. | 기존 provider/model/cap 변경 없이 **기존 승인 범위 안에서 실행 가능**하다는 PM 판정 |
| Batch 70건 사전 예측 | `$0.40131538 × 50% = $0.20065769` ≈ **$0.20066** | 실제 비용과 다른 예측치이며 상한 보장이 아니다. |
| Batch 70건 실제 결과 | **70/70**, recall 0.800, accuracy 0.843, sampled FPR 0, Wilson 95% upper 0.114, macro-F1 0.801, 비용 **`$0.2108`** | **PASS**. 실행 전 잔여 cap 안이다. |
| 실행 후 cap | `$0.55644 + $0.2108 = $0.76724`; `$0.80 - $0.76724 = $0.03276` | 누적 cap 안, 잔여 `$0.03276` |
| 제출 상태 | Batch request·validator 완료 | **PASS**. output/error·usage·retry와 validator 결과를 최종 evidence로 보존한다. |

공식 근거: [OpenAI Batch API Reference](https://developers.openai.com/api/reference/resources/batches), [GPT-5.6 Sol model page](https://developers.openai.com/api/docs/models/gpt-5.6-sol). 실제 비용 `$0.2108`은 사전 예측 `$0.20066`보다 높지만 실행 전 잔여 cap `$0.24356` 안이며, 누적 `$0.76724`로 전체 cap `$0.80` 안이다. 70 custom id, claim↔case mapping, gold, output/error file, usage/retry와 validator 결과를 보존했으며, holdout PASS만으로 auto-promote/auto-apply하지 않는다.

### 사용자·리더 결정 묶음

- **이번 AC-5 실행:** 추가 model/provider/cap 변경 없이 기존 승인 범위의 Batch 실행을 완료했다. 추가 예산을 새로 확정하거나 cap을 늘리지 않았고, AC-5는 PASS다.
- **운영 실행:** Batch JSONL·manifest·validator 결과와 비용 원장을 보존한다. 재실행이 필요하면 실제 잔여 cap `$0.03276`과 재시도 비용을 먼저 확인한다.
- **production:** holdout PASS 뒤 provider/model의 production 사용, 자동 모순 판정·apply, production 승격은 별도 승인이다. 이번 계획은 이를 확정하지 않는다.
- **정합화:** backend의 “M3 knowledge”와 PM/API 17절의 M3 Ingest·M4 knowledge 번호는 리더 정합화가 필요하다. 새 milestone이나 계약을 만들지 않는다.

## 7. 미확정 항목·모순·실제 blocker

### 미확정/정합화 필요

- milestone 명칭(M3 knowledge vs PM/API M3 Ingest·M4 knowledge)
- holdout PASS 이후 production provider/model 및 자동 적용 여부
- 정본의 기술 결정(UD-04·05·17·25·28·29 등)은 PM이 임의 확정하지 않는다.

### 실제 blocker

Phase 1 완료를 막는 실제 blocker는 **없다**. AC-1·2·4·5와 M0~M7 exit evidence가 모두 PASS다. 다음 항목은 완료 후 운영 risk/별도 승인이지 Phase 1 blocker가 아니다.

1. **Production 운영 승인:** provider/model 승격, 자동 모순 판정·apply, production release는 사용자/리더의 별도 운영 결정이 필요하다.
2. **정합화:** backend “M3 knowledge”와 PM/API 17절의 M3 Ingest·M4 knowledge 명칭 모순은 리더가 정합화해야 한다.
3. **재현 환경:** 현재 API/PostgreSQL listener가 없으면 동일 live run 재실행은 어렵다. 최신 PASS를 무효화하지 않지만 후속 운영 재현 risk가 남는다.
4. legacy `/`·`/review-changes`는 mock showcase다. 실제 Phase 1 API 흐름은 `/libraries/**`이며 완료 gate를 막지 않지만, 제품 진입점으로 승격하려면 별도 정리가 필요하다.

### 모순이 아닌 항목

- expired-upload URL fixture 1건 skip은 필수 14/14 PASS를 뒤집지 않는 선택적 subgate다.
- dev 26/26, review/gold freeze, holdout 70/70은 서로 다른 evidence다. holdout PASS도 production 승인으로 해석하지 않는다.

## 8. 검증 근거와 추적

최신 직접/보고 결과:

- `backend\gradlew.bat -g %LOCALAPPDATA%\ZeroWiki-Gradle-9.7 clean test --no-daemon --non-interactive --console=plain` → exit 0; 15 suites/39 tests, failures/errors/skips 0.
- 같은 Gradle cache로 `build bootJar` → exit 0; executable `bootJar` 생성.
- `frontend npm.cmd run build` → exit 0; Next 14.2.35, 17 routes.
- `frontend npx.cmd tsc --noEmit` → exit 0.
- `frontend npm.cmd run lint` → exit 0, warnings/errors 없음.
- M1/M2 live → 필수 14/14 PASS, optional expired-upload fixture 1 skip.
- Phase 1 100-doc live → PASS; `done=100`, `published=100`, `topics=6`, `citations=5`.
- M7 security latest run → 8/8 PASS.
- AC-5 Batch → PASS; 70/70, recall 0.800, accuracy 0.843, sampled FPR 0, Wilson 95% upper 0.114, macro-F1 0.801, cost `$0.2108`.
- 비용 gate → 실행 전 잔여 `$0.24356`, 실행 후 누적 `$0.76724`, cap `$0.80`, 잔여 `$0.03276`.
- Phase 1 final → **PASS/완료**. production provider/model 승격·자동 모순 apply는 별도 운영 승인.
- 정적 증거 → M2 10/10, knowledge 8/8, change-review 3/3, search/AI 6/6 PASS.

갱신 범위는 PM 소유 `docs/PM-*.md`와 STATE/WORKLOG다. 이전의 “cap 초과로 holdout 불가”, “100-doc/AC-1·2·4 미실행”, “M7 미착수”, “AC-5 미제출”이라는 현재형 서술은 최신 evidence에 맞춰 대체했고, 과거 판단은 WORKLOG에 보존한다. 타 역할 변경 되돌리기·커밋은 없다.
