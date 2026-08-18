# frontend 현재 상태

> **덮어쓰기 스냅샷.** 매 턴 종료 전에 이 파일 전체를 최신 상태로 갱신한다(헌법 제4조 1항).
> 시간순 기록은 `WORKLOG.md`에 append 한다.

마지막 갱신: 2026-08-18 20:19:51 +09:00

## 현재 단계

**Phase 1 frontend 완료·PASS.** 정본 범위는 `docs/ZeroWiki-MVP-서비스-기획서.md` 23절과 `docs/ZeroWiki-API-명세-초안.md` 17절의 인증 → 원본·업로드 → Ingest → 지식 객체 → 변경 검토 → 검색 → AI 사서 순서다. 실제 API 연결 화면은 `frontend/src/app/libraries/**`와 `frontend/src/app/login/**`에 있으며 새 기능·의존성 추가 없이 최종 정적 감사를 마쳤다.

- Phase 1 최종 gate: AC-1·2·4·5 및 M7 보안 PASS. AC-5 Batch는 70/70 완료, recall 0.800, accuracy 0.843, sampled FPR 0, Wilson 95% upper 0.114다.
- frontend build는 `/_not-found` 포함 17개 route를 출력했고 build/typecheck/lint가 모두 exit 0이다.
- AI 사서 feedback는 API 명세 9.2절과 동일하게 `rating: HELPFUL | NOT_HELPFUL` 필수, `comment` 선택으로 전송하며 `data.accepted === true`만 성공 처리한다.
- production provider/model 승격과 모순 자동 판정·자동 apply는 Phase 1 완료와 별도 운영 승인이다. frontend는 자동 적용하지 않는다.

## 진행 중 작업

- 없음.

## 검증 결과

- `npm.cmd run build` (`frontend/`) → exit 0, Next.js 14.2.35, 17개 route. webpack `PackFileCacheStrategy` snapshot warning은 있었지만 compile·type validation·static generation과 최종 exit는 성공했다.
- `npx.cmd tsc --noEmit` (`frontend/`) → exit 0.
- `npm.cmd run lint` (`frontend/`) → exit 0, `No ESLint warnings or errors`.
- 정적 접근성 기본 확인:
  - 루트 layout의 `<html lang="ko">`
  - 실제 API 화면의 `main`/heading, form `label`·`htmlFor`, loading/error의 `role="status|alert"`·`aria-live`
  - Ingest `role="progressbar"`와 `aria-valuemin/max/now`
  - Header navigation `aria-label`·`aria-current`
  - `prefers-reduced-motion` 지원 및 focus outline 제거 규칙 없음
- `rg -n "mvn|Maven|mvnw|Gradle|gradlew" .claude/team/frontend/STATE.md .claude/team/frontend/CLAUDE.md docs/검증-프론트엔드-화면-API-갭.md` 감사 시 frontend 소유 활성 문서에 현재형 Maven 실행 의존은 없었다. backend Gradle 전환에 따른 frontend 문서 수정은 필요 없다.

## 다음 작업

1. Phase 1 범위의 추가 구현은 없다.
2. production provider/model 승격 또는 모순 자동 판정·apply 승인이 내려오면 별도 운영 gate로 다룬다.
3. legacy showcase route를 제품 진입점으로 사용할 결정이 생기면 `/`와 `/review-changes`의 mock 소비를 실제 library 선택 흐름으로 정리한다.

## 차단 요인

- **Phase 1 완료 blocker 없음.**
- `/`와 `/review-changes`는 여전히 `frontend/src/data/mockLibraryHome.ts`·`mockReviewChanges.ts`를 소비하는 legacy showcase다. 실제 Phase 1 API 흐름은 `/libraries/**`에 있고 최종 gate를 막지는 않지만, PM 계획의 “mock 제거” 현재형 문구와 소스 현실은 완전히 같지 않다.
- 이번 턴은 정적 감사와 build/typecheck/lint를 재실행했으며 브라우저 keyboard/visual 수동 인수는 재실행하지 않았다. 기존 Phase 1 M7 통합 PASS를 대체하거나 무효화하지 않는다.

## 주요 산출물

| 경로 | 현재 상태 |
| --- | --- |
| `frontend/src/lib/api.ts` | 인증 refresh, 원본·업로드·Ingest, 지식 객체, 변경 검토, 검색·AI 사서·feedback API client |
| `frontend/src/app/login/**` | 가입·로그인·세션 진입 |
| `frontend/src/app/libraries/**` | Phase 1 실제 API 화면 13개 |
| `frontend/src/components/HeaderBar.tsx` | 고유 nav key, navigation landmark, `aria-current` |
| `frontend/src/app/globals.css` | 반응형 token과 `prefers-reduced-motion` |
| `frontend/src/data/mock*.ts` | legacy `/`·`/review-changes` showcase에서만 사용 |
| `.claude/team/frontend/STATE.md` | 이 최신 스냅샷 |
| `.claude/team/frontend/WORKLOG.md` | 시간순 append-only 기록 |
