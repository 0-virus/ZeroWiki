# frontend 현재 상태

> **덮어쓰기 스냅샷.** 매 턴 종료 전에 이 파일 전체를 최신 상태로 갱신한다(헌법 제4조 1항).
> 시간순 기록은 `WORKLOG.md`에 append 한다.

마지막 갱신: 2026-07-31 (변경 검토 화면 하이파이 참고 자료 검토 완료 + 구조/흐름 확정)

## 현재 단계

**변경 검토 화면 컴포넌트 1차 구현 완료 + 하이파이 참고 자료 검토 완료** — Diff 뷰어(`diff` npm 패키지), 4개 판단 버튼 모달 (APPROVE/REJECT/REQUEST_CHANGES/DEFER), REQUEST_CHANGES 재생성 대기 화면(2초 polling) 구현. 목록 데이터로 기본 화면 구현. 원문 발췌 블록 컴포넌트 (ExcerptBlock) 추가. `diff` 패키지 설치 완료. 갭 #23 확정 반영 완료. 개발 서버에서 시각 검증 가능 상태. **구조/흐름은 API 명세 8.1절 계약 준수 확정** (항목별 즉시 반영, 배치 발행 없음). 시각적 개선(색상 체계, 타이포그래피)은 향후 UI polish 시 참고.

## 진행 중 작업

1. 변경 검토 화면 컴포넌트 구현 (1차) — **완료** (2026-07-30)
   - 산출물: `src/components/review-changes/` (6개 파일)
   - **구현 내용**:
     * Diff 뷰어 (DiffViewer.tsx): `diff` 패키지 사용, unified diff 형식
     * 판단 버튼 (ActionButtons.tsx): 4개 버튼 + 2개 모달 (REJECT/REQUEST_CHANGES)
     * 재생성 대기 화면 (RegenerationWaiting.tsx): 2초 polling, 3초 후 완료 시뮬레이션
     * 메인 컴포넌트 (index.tsx): 변경 세트 표시, 항목 선택, 근거·신뢰도
     * CSS 모듈 (ReviewChanges.module.css): 반응형, 다크 모드
   - **원문 발췌 컴포넌트** (ExcerptBlock.tsx): 언어 배지, 신뢰도 바
   - **목록 데이터**: mockReviewChanges.ts (3개 변경 항목 목록)
   - **타입 정의**: changes.ts (DecisionType, ChangeSet, ChangeItem 등)
   - **라이브러리**: `diff` npm 패키지 설치 완료 ✅
   - **갭 #23 확정** (2026-07-30): REQUEST_CHANGES 재생성 완료 감지 메커니즘 확정
     * polling: `GET /change-sets/{changeSetId}/items` (2초)
     * 완료 감지: `change_items[].review_status` (REVISION_REQUESTED → PENDING)
     * RegenerationWaiting.tsx 주석 갱신 ✅
   - **근거**: 설계 문서 `frontend/screens/review-changes.md`, API 명세 8.1절, 검증 문서 9절
   - 산출물: `frontend/screens/ingest-progress.md` (9개 절)
   - **설계 결정**:
     * 작업 상태 10종 (정상 흐름 8개 + 운영 상태 2개)
     * PAUSED_QUOTA (주황색, ⏸️) vs FAILED (빨강색, ✗) 명확한 구분
     * 진행도 표시: 단계별 통계 (완료/처리/보류/실패/대기)
     * 다음 액션: 계획 승인·질문 답변·변경 검토
   - **API 필드 검증**: status, progress, nextAction 필드 ✅
   - **Polling 간격**: 2초 (UD-04 확정, 단계별 동일)
   - **토큰 관리**: 401 응답 시 자동 재발급 (Refresh Token), 다중 탭 동기화
   - **업로드 한도**: 클라이언트 사전 검증 (200MB/파일, 1GB/ZIP, 1000개 문서) + 413 에러 처리
   - **근거**: 기획서 10절, UD-01·02, FR-ING-20·26, FR-NTF-05, 갭 #15·19·20

## 다음 작업

**1순위: 변경 검토 화면 UI 검증 및 polish** ✅ 라우트 준비 완료
- 라우트: `http://localhost:3001/review-changes` (2026-07-30)
- 개발 서버에서 시각 검증 (npm run dev, 포트 3001, 자동 감지)
- 반응형 레이아웃 확인 (모바일/태블릿)
- 다크 모드 동작 확인
- 인터랙션 (버튼·모달·폴링) 테스트
- 필요시 CSS 미세 조정

**2순위: Ingest 진행 화면 컴포넌트 구현**
- 상태별 UI 렌더링 (10가지 상태)
- 다음 액션 모달 (계획 승인/질문 답변/변경 검토)
- Polling 로직 + 토큰 자동 재발급
- 업로드 한도 검증 컴포넌트

**3순위: 도서관 홈 실 API 연동**
- 목록 데이터 → 실제 API 호출로 교체
- 갭 #24(모순 조회 API, 이미 해소됨) 연동
- 에러 처리 및 재시도 로직

## 차단 요인

- 없음 (즉시 다음 화면 설계 착수 가능)
- ✅ 갭 #23·#24 모두 해소됨. 변경 검토·도서관 홈 실 API 연동 준비 완료

## 소유 산출물

| 경로 | 상태 | 갱신 |
| --- | --- | --- |
| `docs/검증-프론트엔드-화면-API-갭.md` | **최신화** (갭 #2·#3 해소, #8·#9 해소, #19~#20, #23·#24 해소) | 2026-07-30 |
| `.claude/team/frontend/STATE.md` | **현재 스냅샷** (변경 검토 화면 컴포넌트 구현) | 2026-07-30 |
| `.claude/team/frontend/WORKLOG.md` | **2026-07-30 항목 append** (변경 검토·발췌·Ingest·컴포넌트) | 2026-07-30 |
| `frontend/screens/library-home.md` | **설계 문서** (도서관 홈, 9절) | 2026-07-30 |
| `frontend/screens/review-changes.md` | **설계 문서** (변경 검토, 9절) | 2026-07-30 |
| `frontend/screens/excerpt-typography.md` | **설계 문서** (원문 발췌 표기, 9절) | 2026-07-30 |
| `frontend/screens/ingest-progress.md` | **설계 문서** (Ingest 진행, 9절) | 2026-07-30 |
| `frontend/src/types/changes.ts` | **타입 정의** (ChangeSet, ChangeItem, DecisionType 등) | 2026-07-30 |
| `frontend/src/types/library.ts` | **타입 정의** (LibraryHome 관련 인터페이스) | 2026-07-30 |
| `frontend/src/data/mockReviewChanges.ts` | **🚀 목록 데이터** (변경 세트, 3개 항목, 근거·신뢰도) | 2026-07-30 |
| `frontend/src/data/mockLibraryHome.ts` | **🚀 목록 데이터** (도서관 홈, 6개 주제·5개 연결·활동) | 2026-07-30 |
| `frontend/src/components/review-changes/` | **6개 컴포넌트 + CSS** (Diff·Button·Modal·Regeneration + 스타일) | 2026-07-30 |
| `frontend/src/components/excerpts/` | **ExcerptBlock 컴포넌트** (언어 배지, 신뢰도 바) | 2026-07-30 |
| `frontend/src/components/library-home/` | **6개 섹션 컴포넌트 + 메인** (7개 .tsx + CSS 모듈) | 2026-07-30 |
| `frontend/src/app/` | **Next.js 기본 구조** (layout.tsx, page.tsx, globals.css, review-changes/page.tsx) | 2026-07-30 |
| `frontend/src/app/review-changes/page.tsx` | **라우트** (ReviewChangesScreen 렌더링) | 2026-07-30 |
| `frontend/package.json` | **의존성** (react-markdown, remark-gfm, react-icons, **diff** 추가) | 2026-07-30 |

## 루트 CLAUDE.md 업데이트 필요

프로젝트 구조 섹션:
- `frontend/` 상태: "미생성" → "생성됨" 필요 (리더 소유, 변경 요청 완료)
- `frontend/` 현재 상태: "현재는 화면 설계 문서만" → "Next.js 프로젝트 + 도서관 홈 컴포넌트 (목록 데이터) + 화면 설계 3개(도서관 홈·변경 검토·발췌 표기)" 필요 (리더 소유)

## 다음 단계

**즉시 시작 가능**: Ingest 진행 화면 설계 (1순위)
1. 갭 #15·#19·#20 기준 설계 문서 작성
2. PAUSED_QUOTA vs FAILED 시각 구분
3. 토큰 만료 중 polling 처리
