# frontend 현재 상태

> **덮어쓰기 스냅샷.** 매 턴 종료 전에 이 파일 전체를 최신 상태로 갱신한다(헌법 제4조 1항).
> 시간순 기록은 `WORKLOG.md`에 append 한다.

마지막 갱신: 2026-08-11 (Phase 0 벤치마크 결과·Phase 1 착수 범위 반영)

## 현재 단계

**Phase 0 기술 검증 실행 완료·제품 게이트 조건부 FAIL; Phase 1 core 착수 허용**. 도서관 홈·변경 검토의 시각 토큰과 목업 컴포넌트는 존재하지만 실 API 연동은 없다. 현재 `frontend/src/app`에는 `/`와 `/review-changes` 라우트만 있고 `frontend/node_modules`가 없어 frontend 개발 서버·빌드·타입체크를 실행할 수 없다. 인증·원본 보관·도서관·Import·변경 승인·검색 골격은 착수할 수 있지만, 모순 자동 판정·자동 적용 UI는 품질 게이트 통과 전까지 차단하고 사용자 검토 제안으로만 취급한다. Phase 0 결정과 근거는 `docs/Phase-0-벤치마크-결과.md` 및 `docs/검증-프론트엔드-화면-API-갭.md` 2-C에 기록했다.

## Phase 0 결정 반영

- `UD-11`: 중요한 주장만 생성. frontend는 서버 Claim 배열·페이지네이션을 렌더링하며 생성량을 하드코딩하지 않는다.
- `UD-17`: `sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2`, 384차원. 모델명·차원·관측 threshold는 UI에 노출하지 않는다.
- `UD-25`: reference local budget은 scan p95 15초, synthesis/answer-generation p95 20초. 검색·citation을 포함한 answer endpoint는 Phase 1 integration에서 재검증하며 production SLA·고정 ETA로 일반화하지 않는다.
- `UD-29`: 자동 identity key를 만들지 않는다. AI mapping 제안은 기존 변경 검토에서 사용자가 확정한다.
- 모순 품질: 6-class accuracy 19.2%, sampled FPR 100%이므로 자동 판정·자동 적용 UI를 제공하지 않는다. 독립 평가 accuracy 80% 이상 통과 전까지 사용자 검토 제안만 허용한다.

## 진행 중 작업

1. ✅ **변경 검토 화면 컴포넌트 구현 완료** (2026-07-30)
   - 산출물: `src/components/review-changes/` 6개 파일
   - 2단계 위험도 UI (SAFE/DANGER 표시, wire `MINOR | MAJOR`와 API 8.1절 정본 일치)
   - 아코디언 목록 + 필터 탭 + 일괄 승인 + 진행 카운터

2. ✅ **도서관 홈 화면 컴포넌트 구현 완료** (2026-07-30)
   - 산출물: `src/components/library-home/` 7개 파일
   - 6개 섹션 (주제·연결·질문·모순·변경·활동)

3. ✅ **시안 시각 디자인 토큰 전면 적용 완료** (2026-07-31)
   - **globals.css 재작성**:
     * CSS 변수: --bg (#fdfcfc), --ink (#201d1d), --muted (#646262), --faint (#9a9898), --panel (#f8f7f7), --danger (#ff3b30), --warn (#ff9f0a), --ok (#30d158), --body-alt (#424245), --hair (rgba(15,0,0,0.12)), --del-bg, --add-bg
     * 폰트: JetBrains Mono (Google Fonts) + D2Coding (fallback)
     * 제거됨: -apple-system, BlinkMacSystemFont, Segoe UI, sans-serif
     * 제거됨: box-shadow, 8px/6px radius
     * 타이포그래피 애니메이션: @keyframes zw-type (logo typing), zw-caret
     * 버튼 3종 (primary/secondary/subtle) + 의미색 버튼 3종 (ok/danger/warn)
   
   - **HeaderBar 컴포넌트 신규 작성** (`src/components/HeaderBar.tsx`):
     * 56px sticky 헤더, z-index 10
     * ZERO_WIKI 로고 (width 9ch, animation: zw-type 1.2s)
     * 네비게이션 7개 링크 (홈·도서관·문서·변경검토·Ingest·그래프·온보딩)
     * 알림 배지 (4건)
     * usePathname으로 active 링크 표시
   
   - **HeaderBar.module.css** 신규 작성:
     * 헤더 배치: flex, justify-content space-between
     * 로고 애니메이션: steps(9,end) 1.2s, border-right 2px solid
     * 네비 링크: --muted (기본) → --ink (active, weight-700, bottom border)
     * 반응형: 768px/480px breakpoint
   
   - **ReviewChanges.module.css 재작성**:
     * Material Design 색상 완전 제거 (#2196f3, #4caf50, #f44336, #ff9800, #e0e0e0 → var(...))
     * 위험도 배지: [data-level='MAJOR'] → background: var(--danger), [data-level='MINOR'] → background: var(--ok)
     * 필터 탭: active → background: var(--ink), border-color: var(--ink)
     * 버튼 (승인/거절/수정요청/보류): ok/danger/warn/muted 색상
     * Diff 뷰어: deleted/added 배경 → --del-bg/--add-bg
     * 모달·진행바·신뢰도: 모두 변수 기반
   
   - **LibraryHome.module.css 재작성**:
     * Material Design 색상 제거 (#2563eb, #10b981, #ef4444 등 → var(...))
     * 섹션 배경: white → var(--bg), 호버 → var(--panel)
     * "더보기" 링크: color --ink, border-bottom (언더라인 효과)
     * 버튼: primary/approve/reject → ink/ok/danger 색상
     * 에러 박스·스켈레톤: 변수 기반

4. ✅ **두 화면에 HeaderBar 통합** (2026-07-31):
   - `src/components/review-changes/index.tsx`: import HeaderBar, return <><HeaderBar /><div>...</div></>
   - `src/components/library-home/index.tsx`: import HeaderBar, return <><HeaderBar /><div>...</div></>

5. ✅ **시각 토큰 정적 검증** (2026-07-31):
   - grep #2196f3|#4caf50|#f44336|#ff9800|#e0e0e0 등: **0건** ✅
   - grep -apple-system|Segoe UI 등: **0건** ✅
   - grep box-shadow: **0건** ✅
   - grep border-radius [0-9]: **0건** (모두 4px) ✅
   - `npm.cmd run build`: **실행 시 실패** — `frontend/node_modules` 부재로 `next`를 찾지 못함
   - `npm.cmd exec -- tsc --noEmit`: **실행 시 실패** — `next`, `react`, `diff` 모듈 부재
   - 개발 서버: **실행 확인 불가** — frontend 의존성 및 backend 실 API 서버 부재 (`backend/`에는 Phase 0 benchmark script만 존재)

6. ✅ **Ingest 진행 문서 상태명 정합화** (2026-08-11)
   - `frontend/screens/ingest-progress.md`의 active `QUESTION_WAITING` 표기를 모두 확정 계약 `QUESTION_REVIEW`로 변경

## 다음 작업

**1순위: Phase 1 core vertical slice 착수**
- 인증 → 도서관/헌법 → 업로드·Import → Ingest 계획 승인·polling → 변경 검토·적용 → 검색·홈 갱신
- `UD-11·17·25·29` 결정은 서버 결과를 소비하는 형태로 연결하고 client matching·고정 ETA·모델 노출을 금지
- 모순 섹션은 사용자 검토 제안만 제공하며 자동 판정·자동 적용 action은 차단

**2순위: API·frontend 계약 잔존 참조 확인**
- API 8.1절 `MINOR | MAJOR`·API 6.1절 `QUESTION_REVIEW`와 frontend 타입·polling 흐름의 실제 연동 시 일치 여부 검증

**3순위: 의존성 설치 후 재검증**
- `npm.cmd ci`, `npm.cmd run build`, `npm.cmd exec -- tsc --noEmit`, 라우트·렌더링 확인

## 차단 요인

- `frontend/node_modules` 부재로 build/typecheck/dev 실행 불가
- backend 실 API 구현 부재로 현재 화면은 목업만 사용 (`backend/`에는 Phase 0 benchmark script만 존재)
- 모순 자동 판정·자동 적용은 accuracy 80% 이상 독립 평가 전까지 차단됨(현재 6-class accuracy 19.2%, sampled FPR 100%). 이는 사용자 검토 제안 UI와 Phase 1 core 착수를 막지 않음

## 소유 산출물

| 경로 | 상태 | 갱신 |
| --- | --- | --- |
| `frontend/src/app/globals.css` | **재작성 완료** (CSS 변수 + JetBrains Mono + 제거됨: box-shadow, system UI 폰트) | 2026-07-31 |
| `frontend/src/components/HeaderBar.tsx` | **신규 작성** (56px sticky, logo typing, nav links) | 2026-07-31 |
| `frontend/src/components/HeaderBar.module.css` | **신규 작성** (시안 토큰 기반) | 2026-07-31 |
| `frontend/src/components/review-changes/ReviewChanges.module.css` | **재작성 완료** (Material Design → 변수) | 2026-07-31 |
| `frontend/src/components/library-home/LibraryHome.module.css` | **재작성 완료** (Material Design → 변수) | 2026-07-31 |
| `frontend/src/components/review-changes/index.tsx` | **갱신** (HeaderBar import + 통합) | 2026-07-31 |
| `frontend/src/components/library-home/index.tsx` | **갱신** (HeaderBar import + 통합) | 2026-07-31 |
| `frontend/screens/ingest-progress.md` | **갱신** (active `QUESTION_REVIEW` 상태명 정합화) | 2026-08-11 |
| `docs/검증-프론트엔드-화면-API-갭.md` | **갱신** (Phase 0 결정·착수 범위·모순 UI 차단·API 계약 반영 기록) | 2026-08-11 |
| `.claude/team/frontend/STATE.md` | **현재 스냅샷** (Phase 0 benchmark 결과·Phase 1 착수 범위 반영) | 2026-08-11 |
| `.claude/team/frontend/WORKLOG.md` | **2026-08-11 항목 append** (Phase 0 결정·모순 UI 차단·Phase 1 범위) | 2026-08-11 |

## 루트 CLAUDE.md 업데이트 필요

(2026-07-30 요청사항 유지)
- `frontend/` 상태: "Next.js 프로젝트 + 도서관 홈 컴포넌트 + 화면 설계 3개 + 시각 디자인 토큰 전면 적용 완료"

## 기술 메모

- **CSS 변수 공급망**: globals.css `:root` 정의 → 각 module.css에서 `var(--ink)` 등으로 소비
- **폰트 로드**: Google Fonts (JetBrains Mono) + 로컬 D2Coding @font-face
- **헤더 sticky**: position sticky, top 0, z-index 10 (콘텐츠 위에 떠 있음)
- **로고 애니메이션**: width 애니메이션 (0ch → 9ch), steps(9,end) (글자별 나타남), 1.2s 지속
- **색상 의미**:
  * --ink (#201d1d): 주 텍스트·주 버튼 배경
  * --muted (#646262): 보조 텍스트·필터 탭 기본
  * --panel (#f8f7f7): 호버·배경 변화
  * --danger (#ff3b30): 거절·위험 표시
  * --ok (#30d158): 승인·안전 표시
  * --warn (#ff9f0a): 수정요청·경고
  * --hair (rgba 12%): 테두리·구분선
- **제거된 Material Design**: 파란색 (#2196f3), 주황색 (#ff9800), 빨강색 (#f44336) 등 완전 제거. 시안의 6색 체계로 통일.

## 현재 확인된 계약

- 변경 판단은 API 명세 8.1절의 4종(`APPROVE`, `REJECT`, `REQUEST_CHANGES`, `DEFER`)을 따른다.
- diff는 Phase 1 클라이언트 계산, polling은 `Retry-After` 우선·Ingest 2초 기본값(UD-05·UD-04)이다.
- 위험도 wire enum은 `MINOR | MAJOR`, UI label은 `SAFE | DANGER`이며 API 8.1절의 필터·응답·일괄 승인 예시와 일치한다.
- Ingest 질문 상태는 `QUESTION_REVIEW`이며 API 2.7·6.1절과 frontend 흐름이 일치한다.
- 헤더 높이: 56px, sticky positioning
- 폰트: JetBrains Mono 우선, D2Coding 폴백
- 배경: #fdfcfc, 텍스트: #201d1d
- 반응형 breakpoint: 768px, 480px
