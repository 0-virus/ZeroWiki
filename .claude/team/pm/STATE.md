# pm 현재 상태

> 덮어쓰기 스냅샷. 시간순 판단과 변경 사유는 `WORKLOG.md`에 append-only로 남긴다.

마지막 갱신: 2026-08-18 20:38 +09:00 (Gradle 전환·정본 v1.5·Phase 1 최종 감사 반영)

## 현재 단계

**Phase 1: M0~M7 및 AC-1·2·4·5 최종 `PASS/완료`.**

- M1/M2 live suite는 exit 0이며 필수 assertion 14/14가 PASS했다. 선택 expired-upload fixture 1건은 skip이고 V1~V5 Flyway history는 success=true다.
- M3 Ingest/100-doc live는 `done=100`, post-apply job `COMPLETED`, `published=100`, `topics=6`, `citations=5`로 PASS했다.
- M4 knowledge static 8/8 및 통합 trace/owner evidence, M5 change-set static 3/3 및 apply/version evidence, M6 search/AI static 6/6 및 3 search mode/citation evidence를 최종 bundle에 묶어 PASS했다.
- M7 security는 8/8 PASS다. backend Gradle 9.7.0 wrapper `clean test`는 15 suites/39 tests, failures/errors/skips 0이고 `build bootJar`도 PASS했으며 frontend build 17 routes/typecheck/lint도 PASS다.
- backend의 “M3 knowledge”와 PM/API 17절의 M3 Ingest·M4 knowledge 명칭은 모순으로 남아 리더 정합화가 필요하지만 Phase 1 완료를 막지 않는다.

## AC·보안 최종 상태

- AC-1: **PASS** — 100-doc `done=100/100`.
- AC-2: **PASS** — `published=100`, `topics=6`.
- AC-4: **PASS** — `citations=5`와 page/claim/source version/locator trace.
- AC-5: **PASS** — Batch 70/70, recall 0.800, accuracy 0.843, sampled FPR 0, Wilson 95% upper 0.114, macro-F1 0.801.
- 보안: M7 8/8 PASS 및 NFR/API evidence bundle 완료.
- Phase 1 최종 판정: **PASS/완료**.

## AC-5 비용·승인 상태

- 실행 전 정본 수치: v3 26건 actual `$0.149060`, 누적 `$0.55644`, cap `$0.80`, 잔여 `$0.24356`.
- Batch 실제 70건 비용: **`$0.2108`**. 실행 전 잔여 cap 안이다.
- 실행 후 누적: `$0.55644 + $0.2108 = $0.76724`; cap 잔여: `$0.80 - $0.76724 = $0.03276`.
- 기존 provider/model/cap을 변경하지 않고 `gpt-5.6-sol` + `/v1/responses` Batch를 사용했다. AC-5 결과는 holdout gate PASS이나 production provider/model 승격을 의미하지 않는다.
- production provider/model, 자동 모순 판정·apply, production release는 별도 사용자/리더 운영 승인이다.

## Milestone 최종 snapshot

| Milestone | 최종 상태 | 근거 |
| --- | --- | --- |
| M0 | 완료 | Gradle wrapper 단일 build·계약·`clean test`·`build bootJar` 증거 |
| M1 | 완료·PASS | auth/library/owner isolation live 및 M7 보안 재확인 |
| M2 | 완료·PASS | 필수 live 14/14, optional expiry 1 skip |
| M3 Ingest | 완료·PASS | 100-doc `done=100`, `published=100`, post-apply `COMPLETED` |
| M4 knowledge | 완료·PASS | static 8/8, owner/trace 통합 evidence |
| M5 change-set | 완료·PASS | static 3/3, apply/version/audit evidence |
| M6 search/AI | 완료·PASS | static 6/6, search 3종, citations 5 |
| M7 final | 완료·PASS | security 8/8, AC-1·2·4·5, build/typecheck/lint |

의존 사슬: **M0 → M1 → M2 → M3 100-doc → M4 → M5 → M6 → M7 security/AC → AC-5 Batch/validator PASS → Phase 1 PASS/완료 → production 별도 운영 승인**.

## 다음 작업·남은 위험

Phase 1의 필수 다음 작업은 없다. 다음 운영 단계에서만 다음을 별도 승인·기록한다.

1. production provider/model 승격 및 production release.
2. 자동 모순 판정·apply 여부.
3. backend “M3 knowledge”와 PM/API milestone 명칭 정합화.
4. 현재 API/PostgreSQL listener가 없을 때 동일 live run 재현이 어려운 환경 risk.
5. legacy `/`·`/review-changes`는 mock showcase다. 실제 Phase 1 API 흐름은 `/libraries/**`이며 완료 blocker는 아니지만 제품 진입점으로 사용할 때 정리가 필요하다.

실제 Phase 1 blocker: **없음**. expired-upload fixture skip은 선택 subgate이며 필수 14/14 PASS를 뒤집지 않는다.

## 검증 명령과 결과

- `backend\gradlew.bat -g %LOCALAPPDATA%\ZeroWiki-Gradle-9.7 clean test --no-daemon --non-interactive --console=plain` → exit 0; 15 suites/39 tests, failures/errors/skips 0.
- 같은 Gradle cache로 `build bootJar` → exit 0; executable `bootJar` 생성.
- `frontend npm.cmd run build` → exit 0; Next 14.2.35, 17 routes.
- `frontend npx.cmd tsc --noEmit` → exit 0.
- `frontend npm.cmd run lint` → exit 0.
- M1/M2 live → 14/14 PASS + optional expiry fixture 1 skip.
- Phase 1 100-doc live → PASS (`done=100`, `published=100`, `topics=6`, `citations=5`).
- M7 security → 8/8 PASS.
- AC-5 Batch → 70/70 PASS; recall 0.800, accuracy 0.843, FPR 0, Wilson upper 0.114, macro-F1 0.801, cost `$0.2108`.
- 비용 → 실행 전 잔여 `$0.24356`, 실행 후 누적 `$0.76724`, 잔여 `$0.03276`.

## 소유·변경 경계

PM 소유는 `docs/PM-*.md`와 `.claude/team/pm/**`다. backend/frontend/qa 파일과 정본 계약 문서는 읽고 대조만 했다. 이번 갱신에서 타 역할 변경 되돌리기·커밋은 없다.
