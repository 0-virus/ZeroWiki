# frontend 현재 상태

> **덮어쓰기 스냅샷.** 매 턴 종료 전에 이 파일 전체를 최신 상태로 갱신한다(헌법 제4조 1항).
> 시간순 기록은 `WORKLOG.md`에 append 한다.

마지막 갱신: 2026-07-31 (시안 시각 디자인 토큰 전면 적용 완료)

## 현재 단계

**시안 시각 디자인 토큰 전면 적용 완료** — 도서관 홈 + 변경 검토 양 화면에 시안의 완전한 시각 언어 적용 완료. globals.css에 CSS 변수 정의 (#fdfcfc bg, #201d1d ink, #646262 muted, JetBrains Mono 폰트). 공용 헤더 컴포넌트(HeaderBar.tsx) 추가 (56px sticky, 로고 typing 애니메이션, 네비게이션 링크 7개). 양 화면 CSS 모듈 재작성 (Material Design #2196f3·#4caf50·#ff9800 등 → var(--ink)·var(--ok)·var(--warn) 등). 모든 box-shadow 제거, border-radius 4px 통일. 폰트 스택 JetBrains Mono + D2Coding. **검증**: grep으로 Material Design 색상/System UI 폰트/box-shadow 0건 확인 ✅. 빌드 성공 ✅. 개발 서버 실행 중 (localhost:3000).

## 진행 중 작업

1. ✅ **변경 검토 화면 컴포넌트 구현 완료** (2026-07-30)
   - 산출물: `src/components/review-changes/` 6개 파일
   - 2단계 위험도 UI (SAFE/DANGER, MINOR/MAJOR 계약 기반)
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

5. ✅ **품질 검증** (2026-07-31):
   - grep #2196f3|#4caf50|#f44336|#ff9800|#e0e0e0 등: **0건** ✅
   - grep -apple-system|Segoe UI 등: **0건** ✅
   - grep box-shadow: **0건** ✅
   - grep border-radius [0-9]: **0건** (모두 4px) ✅
   - npm run build: **성공** ✅
   - npm run dev: **실행 중, localhost:3000** ✅

## 다음 작업

**1순위: 시각 검증 (브라우저)**
- localhost:3000 도서관 홈 방문
- localhost:3000/review-changes 변경 검토 화면 방문
- 확인 항목:
  * 헤더 배치 (56px, sticky)
  * 로고 애니메이션 (typing effect 9ch)
  * 색상 적용 (배경 #fdfcfc, 텍스트 #201d1d, 버튼 색상)
  * 폰트 (JetBrains Mono)
  * 위험도 배지 (DANGER 빨강, SAFE 녹색)
  * 필터 탭 스타일 (active 검정 배경, 흰 글자)
  * 반응형 (모바일 320px)
  * 다크 모드 (있으면 테스트, 없으면 생략)

**2순위: 일부 미완료 항목 정리** (선택사항, 기획서 및 pm 승인 후)
- 위험도 텍스트 (MAJOR/MINOR → DANGER/SAFE 표시는 UI만, API 계약은 유지)
- 헤더의 네비게이션 라우트 확정 (현재 임시: '/' 대부분)

**3순위: Ingest 진행 화면 설계 및 구현**

## 차단 요인

- 없음 (개발 서버 실행 중, 즉시 시각 검증 가능)

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
| `docs/검증-프론트엔드-화면-API-갭.md` | **유지** (갭 12건 변함 없음) | 2026-07-30 |
| `.claude/team/frontend/STATE.md` | **현재 스냅샷** (시각 디자인 토큰 전면 적용) | 2026-07-31 |
| `.claude/team/frontend/WORKLOG.md` | **2026-07-31 항목 append** (토큰 적용, HeaderBar, CSS 재작성) | 2026-07-31 |

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
  * --danger (#ff3b30): 거절·MAJOR·위험
  * --ok (#30d158): 승인·MINOR·안전
  * --warn (#ff9f0a): 수정요청·경고
  * --hair (rgba 12%): 테두리·구분선
- **제거된 Material Design**: 파란색 (#2196f3), 주황색 (#ff9800), 빨강색 (#f44336) 등 완전 제거. 시안의 6색 체계로 통일.

## 확정 계약 (변경 불가)

- API 계약 준수: 항목별 `POST /reviews`, MINOR/MAJOR 필드 유지
- 2단계 위험도 UI: SAFE (MINOR) / DANGER (MAJOR)
- 헤더 높이: 56px, sticky positioning
- 폰트: JetBrains Mono 우선, D2Coding 폴백
- 배경: #fdfcfc, 텍스트: #201d1d
- 반응형 breakpoint: 768px, 480px
