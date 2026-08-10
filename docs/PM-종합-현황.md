# ZeroWiki 팀 종합 현황

작성일: 2026-07-29 23:50 UTC  
**최종 갱신: 2026-08-10 (Phase 0 평가 데이터 인수 결과 및 실행 차단 조건 반영)**
대상: 리더 + 팀 전체  
기준 문서: `docs/ZeroWiki-요구사항-정의서.md` 12절(정본), 각 역할의 `STATE.md`, `.claude/team/JOURNAL.md`, `docs/PM-미확정결정-우선순위.md`

---

## 1. 전체 현황 (4개 역할)

| 역할 | 단계 | 산출물 | 상태 | 주요 차단 |
| --- | --- | --- | --- | --- |
| **PM** | Phase 0 | 종합 현황, 결정 요청서 | 🔄 평가 인수 결과 반영 완료 | mapping manifest 계약·Ingest harness 착수 순서 정렬 |
| **Backend** | Phase 0 | ERD v1.5, 평가 CSV·검증기·벤치마크 계획 | ✅ CSV 구조·offline 채점 계약 인수 완료 | Ingest-level AC-5 mapping manifest, 실행 전 LLM·유료 API 승인 |
| **Frontend** | Phase 0 | 도서관 홈 + 변경 검토 화면 | ✅ 하이파이 시안 기반 전면 재구현 완료 | — |
| **QA** | Phase 0 | 테스트 시나리오·독립 검증기·평가 인수 보고서 | ✅ CSV 구조·offline 채점 계약 재인수 완료 | Ingest-level AC-5 mapping manifest, AC-3 U-1 미실행 |

**코드 진행도** — Frontend: Next.js 프로젝트 + 도서관 홈·변경 검토 화면 2종 구현(목록 데이터 기반, 실 API 미연동). Backend/QA: 실행 코드 없음(문서 단계).  
**UD 정본상 미확정:** 사용자 결정 5건(UD-03·10·19·20·22), Phase 0 직접 벤치마크 4건(UD-11·17·25·29), Phase 4 별도 사용자 테스트 1건(UD-26). 실행에 필요한 **LLM provider/model 선택·유료 API 사용 범위·토큰/비용 예산은 별도 사용자 결정·승인 사항**이다.

---

## 2. 역할별 상세

### 2.1 Backend — 평가 데이터 인수 완료, Ingest 실행 준비

**단계:** Phase 0 평가 데이터 구조·참조 무결성 인수 완료, Ingest 프로토타입 실행 준비(마지막 갱신: 2026-08-10 22:30 KST).

**인수된 사실** (`.claude/team/backend/STATE.md`, `evaluation/README.md`):

- 페이지 인벤토리 180행: source 37 / concept 127 / flow 16. 지식 페이지(concept + flow)는 143개다.
- 연결 라벨 84행: positive 76 / negative 8. endpoint slug 참조, UD-16 7종, direction·confidence·중복 규칙을 통과했다.
- 모순 라벨 26행: conflict 23 / `NO_CONFLICT` 3. stable `case_id` 26개와 5종 분류 계약을 통과했다.
- `evaluation/validate_evaluation.py` 반복 검증 결과 오류 0건이다.
- 기존 계획의 원본·페이지 수와 라벨링 대기는 **역사적 가정**이며, 현재 실행 입력은 위 실측 CSV다.

**인수 범위와 차단 경계:**

- ✅ CSV 구조·참조 무결성과 offline 채점 계약은 인수됐다.
- ⛔ Ingest-level AC-5는 원본 page/claim·gold `case_id`·생성 `claim_id`를 잇는 versioned mapping manifest가 없어 차단됐다.
- ⏳ AC-3은 CSV 지표가 아니라 U-1 사용자 5~10명의 실제 제안 관계 유용성 평가이며 미실행이다.
- ⏳ 라벨 의미 정확성의 독립 이중 검토와 외부 NoteWiki 원본 대조는 별도 위험으로 남아 있다.

**미확정 계약 분리** (`docs/ZeroWiki-요구사항-정의서.md` 12절):

- Phase 0 직접 결정: UD-11(Claim 생성 범위), UD-17(임베딩 모델·차원), UD-25(성능 목표), UD-29(Claim 동일성 판정).
- Phase 4 별도 트랙: UD-26(개별 승인 상한). Phase 0 직접 결정 대상에 포함하지 않는다.
- 사용자 결정 5건: UD-03·10·19·20·22. 기존 결정 요청서를 유지하되, 현재 한 걸음과 혼합하지 않는다.
- 실행 전 별도 사용자 결정·승인: LLM provider/model 선택, 유료 API 사용 범위, 토큰·비용 예산.

**다음 작업:** mapping manifest 계약과 생성 절차를 먼저 고정하고, 그 계약을 소비하는 Ingest harness를 착수한다. provider/model·유료 API 예산은 사용자가 결정·승인한 뒤 실제 외부 호출에 적용한다.

---

### 2.2 Frontend — 변경 검토 화면 재구현 완료, 도서관 홈 실 API 연동 준비

**단계:** Phase 0 화면 설계·구현 단계 (마지막 갱신: 2026-07-31 11:20)

**완료 항목:**
- ✅ **Next.js 프로젝트 초기화** (2026-07-30)
  - TypeScript, React, 반응형 레이아웃, 다크 모드 지원
  - 의존성: react-markdown, remark-gfm, react-icons, **diff**
- ✅ **도서관 홈 컴포넌트 구현** (목록 데이터 기반, 실 API 미연동)
  - 6개 섹션 컴포넌트 + 메인 페이지
  - 목업 데이터: `mockLibraryHome.ts` (6개 주제, 5개 연결, 활동)
- ✅ **변경 검토 화면 2차 구현 & 사용자 결정 반영** (2026-07-31 완료)
  - 신규 컴포넌트: `FilterTabs`, `AccordionList`, `ChangeItemAccordion`, `ProgressBar` + 기존 `DiffViewer`
  - **2단계 UI 확정** — "검토(REVIEW)" 탭 제거, `ALL|SAFE|DANGER` 3개만 표시 (사용자 2026-07-31 결정)
  - 필터 로직 `SAFE→MINOR`, `DANGER→MAJOR` 정확 + 리더 검증 통과(잔존 코드 0건)
  - 항목별 `POST /reviews` 제출, 2초 polling 간격 (UD-04 확정)
  - **갭 #23 확정** (REQUEST_CHANGES 재생성 감지): polling 기반, change_items[] 필드 기반
- ✅ **필수 화면 설계 4종** (`frontend/screens/`)
  - `library-home.md` (9절)
  - `review-changes.md` (9절)
  - `excerpt-typography.md` (9절)
  - `ingest-progress.md` (9절, 상태 10종 정의)

**차단 요인 & 해소된 사항:**

**1. "검토(REVIEW)" 필터 탭 처리 방향 — ✅ 해소됨 (2026-07-31)**
- **사용자 결정**: 2단계 UI 채택, "검토(REVIEW)" 탭 제거
- **frontend 구현**: `FilterTab` → `ALL|SAFE|DANGER` 3개, `UIRiskLevel`·`mapRiskLevelToUI()` 완전 제거
- **리더 검증**: 잔존 코드 0건, 타입체크 통과, 필터 로직 정확(`SAFE→MINOR`, `DANGER→MAJOR`)
- 근거: `.claude/team/JOURNAL.md` 항목 11·12 (사용자 결정 2026-07-31, 리더 검증 완료)

**2. 미해소 API 갭 18건** (backend/리더 담당)

| 갭 # | 내용 | 소유자 | 상태 |
| --- | --- | --- | --- |
| #1 | 글로벌 홈 전체 도서관 횡단 조회 | 리더 | ⏳ |
| #2 | 도서관 홈 요약 응답 스키마 | backend | ⏳ |
| #3 | "주요 주제" 정의 (기획서 16절) | 리더 | ⏳ |
| #4 | 페이지 주제/태그 필드 | backend | ⏳ |
| #5 | 배치 근거 조회 API | backend | ⏳ |
| #6 | 활동 집계 API (활동 잔디) | backend | ⏳ |
| #7 | 그래프 시각화 페이로드 형태 | frontend 권고 → 리더 | ⏳ |
| #10 | 알림 실시간성 | 리더 | ⏳ |
| #11 | 도서관 참조 검색 응답 형태 | backend | ⏳ |
| #12 | Ingest 계획 승인 후 상태 전이 | backend | ⏳ |
| #13~20 | 변경 검토·알림·발음표기·업로드 검증·세션 관리 | 소유자별 | ⏳ |

**3. 해소된 갭 4건 ✅**
- ✅ 갭 #8: diff 표현 (2026-07-29, UD-05 확정: Phase 1 스냅샷 + 클라이언트 diff)
- ✅ 갭 #9: polling 간격 (2026-07-29, UD-04 확정: 기본값 2초)
- ✅ 갭 #23: REQUEST_CHANGES 재생성 감지 (2026-07-30 backend 확정, 2026-07-31 frontend 구현·리더 검증)
- ✅ 갭 #24: 모순 조회 API (2026-07-30 리더 확인)

**다음 작업:**
1. 미해소 갭 18건을 소유자별 분류해 전달 (backend/리더에게)
2. Ingest 진행 화면 컴포넌트 구현 (2순위)
3. 도서관 홈 실 API 연동 (3순위)

---

### 2.3 QA — offline 재인수 완료, Ingest-level 판정 보류

**단계:** Phase 0 평가 세트 재인수 완료(마지막 갱신: 2026-08-10 22:21 KST).

**완료 항목** (`.claude/team/qa/STATE.md`, `qa/phase0-evaluation-acceptance-report.md`):

- `qa/verify_phase0_evaluation.py`와 backend validator가 동일 실측에서 모두 종료 코드 0을 반환했다.
- relation slug 헤더, contradiction stable `case_id`, pair/strict recall·bounded precision·sampled FPR, contradiction recall·FPR·FDR와 0분모 N/A 계약을 재인수했다.
- A-4는 conflict 23건 중 최소 19건 탐지, 확정 5종 enum, `NO_CONFLICT` sampled FPR, mapping manifest 선행 조건으로 동기화됐다.
- 제품 테스트 시나리오는 A 18개·U 6개·S 16개로 총 40개가 정의돼 있으나, 제품 구현 대상 테스트는 모두 미실행이다.

**남은 차단과 위험:**

1. Ingest-level AC-5: gold `case_id`와 생성 `claim_id` 쌍을 잇는 mapping manifest 미작성.
2. AC-3: U-1 검증 사용자 5~10명의 실제 유용성 평가 미실행.
3. 라벨 의미 정확성 독립 이중 검토와 외부 NoteWiki 원본 대조 미실행.
4. sampled FPR 분해능: relation negative 8건은 1건당 12.5%p, contradiction `NO_CONFLICT` 3건은 1건당 33.3%p다.

**다음 작업:** backend가 mapping manifest와 생성 절차를 제공한 뒤 A-4 Ingest-level AC-5를 실행한다. AC-3은 사용자 모집·U-1 실행이 가능한 시점에 별도로 판정한다.

---

### 2.4 PM — 평가 인수 경계와 실행 우선순위 종합

**단계:** backend·QA의 Phase 0 평가 데이터 인수 결과를 제품 범위·결정 주체·의존 사슬 기준으로 종합(마지막 갱신: 2026-08-10).

**확정 사실:**

- 평가 CSV는 실측 180/84/26행이며 구조·offline 채점 계약이 독립 검증을 통과했다.
- 이 인수는 Ingest-level AC-5 또는 AC-3의 통과를 뜻하지 않는다.
- 요구사항 정의서 12절의 미확정 분류는 사용자 결정 5건, Phase 0 직접 벤치마크 4건, Phase 4 별도 UD-26으로 유지한다.

**권고(미확정) — 다음 한 걸음:** **mapping manifest 계약 및 이를 소비하는 Ingest harness 착수**.

- 근거: offline 입력은 준비됐지만 실제 Ingest 출력의 `claim_id`와 gold `case_id`를 잇지 못해 AC-5가 막혀 있다.
- 트레이드오프: harness부터 임의 조인을 넣으면 빠르게 보이지만, 매칭 계약 변경 시 scorer·결과를 다시 만들어야 한다.
- 대안: 먼저 offline 모델 비교만 수행할 수 있으나 Ingest-level AC-5 차단을 해소하지 못하므로 우선순위가 낮다.

**의존 사슬:** mapping manifest 계약·생성 절차 → Ingest harness → Phase 0 실행·offline/ingest 지표 수집 → UD-11·17·25·29 근거 → Phase 1 설계. AC-3은 사용자 5~10명 U-1, UD-26은 Phase 4 사용자 테스트로 각각 별도 분기한다.

**사용자 결정 묶음:** 기존 UD-03·10·19·20·22는 결정 요청서대로 유지한다. 실제 외부 호출 전에는 LLM provider/model, 유료 API 사용 범위, 토큰·비용 예산에 대한 사용자 결정·승인이 추가로 필요하다.

---

## 3. 미확정 계약 현황 (결정 주체·Phase 분리)

### 3.1 사용자 결정 필요 (5건) — Phase별 구현 시 결정

| UD | 내용 | Phase | 차단 기능 | 문서 |
| --- | --- | --- | --- | --- |
| **UD-03** | Free·Basic·Advanced 처리량 단위 | 1 | API 18절 3 / 가격 모델 | PM-미확정결정-우선순위.md 2.2절 |
| **UD-10** | Notion OAuth·결제 공급자 선정 | 1 | API 18절 10 / 외부 연동 | PM-미확정결정-우선순위.md 2.3절 |
| **UD-19** | 처리량 표시 단위·토큰 환산 규칙 | 2 | ERD 8절 9 / 과금 UI | PM-미확정결정-우선순위.md 2.4절 |
| **UD-20** | 감사 로그 보존 기간·접근 정책 | 2 | ERD 8절 10 / 감사 시스템 | PM-미확정결정-우선순위.md 2.5절 |
| **UD-22** | 신고 처리 주체·검토 기한 | 3 | 기획서 15절 / 공개·포크 | 미확정 |

**상태:** 모두 사용자 결정 대기 중이다. 평가 데이터 인수로 자동 해소되지 않았으며, 제품·Phase 필요 시점에 맞춰 결정한다.

### 3.2 기술 결정 — 0건 (모두 확정 ✅)

**확정 12건** (2026-07-30, 요구사항 정의서 v1.1 기준): 
- API 정책: UD-04(polling), UD-05(diff), UD-08(slug), UD-09(관리자 API)
- 데이터 모델: UD-12(Claim 재사용), UD-13(이벤트 재생), UD-15(JSON Schema), UD-16(관계 유형), UD-18(Notion 중복)
- 스케줄/기능: UD-21(Lint 스케줄), UD-23(알림 보존), UD-28(주요 주제)

### 3.3 벤치마크·사용자 테스트 필요 — Phase 0 직접 4건 + Phase 4 별도 1건

| UD | 내용 | 대상 | 방법 | 우선순위 | 비고 |
| --- | --- | --- | --- | --- | --- |
| **UD-11** | Claim 세밀도 (중요 vs 모든 문장) | API·구조 설계 | Phase 0 Ingest 실행·품질/비용 비교 | 🔴 필수 | Phase 1 구현 조건 |
| **UD-17** | pgvector 임베딩 모델·차원 | 검색 설계 | Phase 0 후보 비교 | 🟡 중요 | provider/model·비용 승인 필요 |
| **UD-25** | 성능 목표 수치 (스캔·응답 시간) | 인프라 설계 | Phase 0 실행 시간·비용 측정 | 🟡 중요 | NFR 정량화 |
| **UD-29** | Claim 동일성 판정 메커니즘 | Claim 버전 연속성 | Phase 0 Ingest 결과와 mapping 검증 | 🔴 필수 | mapping manifest와 직접 연결 |
| **UD-26** | 개별 승인 건수 상한 (승인 피로) | UX 설계 | **Phase 4 사용자 테스트** | 🟢 별도 | Phase 0 직접 결정 대상 아님 |

**인수와 결정의 경계:** CSV 구조·offline 채점 계약 인수는 위 UD를 확정하지 않는다. UD-11·17·25·29는 실제 Ingest 실행 결과로, UD-26은 Phase 4 사용자 피로 테스트로 확정 근거를 만든다.

---

## 4. 우선순위 & 리더의 즉시 행동

### 권고(미확정) — 다음 한 걸음 하나

**mapping manifest 계약 및 Ingest harness를 착수한다.** manifest에는 최소한 seed source/page/claim 식별자, gold `case_id`, Ingest 생성 `claim_id` 쌍, 버전과 생성 절차가 포함돼야 한다. 이 계약을 먼저 고정해야 A-4 Ingest-level AC-5가 재현 가능하고 UD-29 근거도 같은 경로에서 수집할 수 있다.

**의존 사슬:** mapping manifest → Ingest harness → Phase 0 실행·지표 수집 → UD-11·17·25·29 근거 → Phase 1 설계. AC-3 U-1과 Phase 4 UD-26은 이 사슬의 선행 조건이 아니라 별도 검증 분기다.

**사용자 결정·승인 분리:** harness의 로컬 계약·골격 작업과 별개로, 실제 외부 호출 전에 LLM provider/model, 유료 API 사용 범위, 토큰·비용 예산을 사용자에게 결정·승인받는다. 기존 사용자 UD-03·10·19·20·22도 정본 12절과 결정 요청서에서 계속 추적한다.

---

## 5. 주요 발견 & 기획 정합성 검토

### 5.1 문서 정합성 ✅ 확인 (3개 문서쌍)

- **기획서 ↔ 요구사항**: 2건 불일치 해소 (17절 알림·20절 작업 상태)
- **요구사항 ↔ ERD·API**: 기술 결정 12건 반영으로 정합성 확보
- **frontend 갭**: 4건 해소(#8·#9·#23·#24 확정) + 18건 미해소 (backend/리더 담당)

### 5.2 범위 & 우선순위 관리 ✅ 정책 준수

- **MVP 범위**: 기획서 22절 제외 목록 엄격히 준수
- **Phase 순서**: 기획서 23절 준수. **UD-26만 Phase 4로 이관** (UX 테스트 필요)
- **제품 정합성**: 모든 역할이 기획서 기준으로 작업. 범위 이탈 없음

### 5.3 기술 결정 완료 ✅ 신속한 수렴

**기술 12건 단계적 확정 (2026-07-29~31)**
1. **API 정책 4건** (2026-07-29): UD-04·05·08·09 — polling·diff·slug·관리자 API
2. **데이터 모델 5건** (2026-07-30): UD-12·13·15·16·18 — Claim·이벤트·스키마·관계·Notion
3. **기능·스케줄 3건** (2026-07-30~31): UD-21·23·28 — Lint·알림·주요 주제

**결과:** 기술 미확정 **0건** ✅ (리더·backend·frontend 협업으로 신속 수렴)

### 5.4 새로운 발견 & 의사결정

| 발견 | 범주 | 처리 | 상태 |
| --- | --- | --- | --- |
| **UD-26 Phase 4 분리** | 벤치마크 트랙 | 기술 성능이 아닌 승인 피로 사용자 테스트로 분리 | ✅ Phase 배치만 확정, 값은 미확정 |
| **평가 데이터 실측** | 계획 가정 대체 | source 37·지식 페이지 143·총 180, 관계 84, 모순 26으로 인수 | ✅ offline 인수 |
| **AC-5 재현 경계** | 실행 계약 | stable `case_id`만으로 Ingest `claim_id` 조인 불가 | ⛔ manifest 대기 |
| **UD-29 UX 영향** | Phase 0 미확정 | Claim 동일성 판정으로 버전 변경 시 승인량 증가 가능성 → Ingest 실행 근거 필요 | 🟡 미확정 |
| **Frontend "검토" 탭** | 3단계 시안 vs 2단계 계약 | 사용자 2단계 UI 선택 → frontend 구현 완료 & 리더 검증 통과 | ✅ 해소됨 |

### 5.5 근거 문서 & 상호 참조

**기준 문서 체계 (2026-07-31 정상화)**
- 정본: `docs/ZeroWiki-요구사항-정의서.md` 12절 (UD 정본)
- 기획서: `docs/ZeroWiki-MVP-서비스-기획서.md` (22절 범위·23절 Phase)
- 검증: 각 역할 `STATE.md` + `docs/검증-*.md` 3종 + `.claude/team/JOURNAL.md`
- 결정 요청: `docs/PM-미확정결정-우선순위.md` (사용자 5건)

**통합 기록 신설 (2026-07-31)**
- `.claude/team/JOURNAL.md` — 리더 소유, 시간순 업무·판단·수정 이력
- PM-종합-현황.md와 분담: 시점 스냅샷 vs 시간 흐름

---

## 6. 현황 수치 (2026-08-10 기준)

### 6.1 코드 진행도

| 항목 | 산출물 | 상태 |
| --- | --- | --- |
| **Frontend** | Next.js 프로젝트 초기화, 도서관 홈·변경 검토 화면 2종 | 목록 데이터 기반, 실 API 미연동. 변경 검토는 2단계 UI 확정(사용자 결정) & 리더 검증 통과 |
| **Backend** | ERD v1.5, 평가 CSV·검증기·Phase 0 계획 | CSV 구조·offline 채점 계약 인수 완료, Ingest harness 미착수 |
| **QA** | 테스트 시나리오 40개, 독립 검증기, 인수 보고서 | offline 재인수 완료, 제품 구현 테스트·AC-3 미실행 |

### 6.2 기획서 수정·유보 상태

| 항목 | 수정 내용 | 절 번호 | 근거 | 상태 |
| --- | --- | --- | --- | --- |
| 기획서 17절 | 알림 목록 → 명시적 타입 재정의 | 17절 | 요구사항 정의서 불일치 해소 | ✅ 2026-07-29 |
| 기획서 20절 | 작업 상태 8종 명확화 | 20절 | 기획서 내부 일관성 | ✅ 2026-07-29 |
| 기획서 16절 | 주요 주제 정의 추가 (AI 자동 선정) | 16절·UD-28 | frontend 갭 해소 | ✅ 2026-07-30 |
| 기획서 22절 | 변경 없음 | 22절 | 사용자 승인 없는 MVP 제외 목록 변경 금지 | ✅ 유지 |
| 기획서 23절 | 변경 없음 | 23절 | 사용자 승인 없는 Phase 정의 변경 금지 | ✅ 유지 |

### 6.3 정합성 상태 (2026-08-10)

| 대상 | 상태 | 기준 | 갱신일 |
| --- | --- | --- | --- |
| **기획서 ↔ 요구사항** | 2건 불일치 해소, 정합성 확보 | `docs/ZeroWiki-요구사항-정의서.md` 12절 v1.1 | 2026-07-31 |
| **요구사항 ↔ ERD** | 기술 9건 확정, v1.5 정합성 확인 | ERD 8절 + UD 정본 | 2026-07-31 |
| **요구사항 ↔ API 명세** | 기술 4건 확정, 18절 미확정 정의 | API 명세 18절 + UD 정본 | 2026-07-31 |
| **Frontend 갭** | 4건 해소(#8·#9·#23·#24) / 18건 미해소 | `docs/검증-프론트엔드-화면-API-갭.md` 2절 | 2026-07-31 ✅ |
| **미확정 분류** | 사용자 5 + Phase 0 직접 4 + Phase 4 별도 1 | 요구사항 정의서 12절 | 2026-08-10 |

---

## 6.4 UD-29 기획 관점 평가 (권고, 미확정)

**상황:** UD-29(Claim 동일성 판정)는 요구사항 정의서 12.2절의 벤치마크 결정 항목이며 아직 미확정이다. UX와 승인 피로 영향도 실제 Ingest 결과 없이 판단할 수 없다.

**기획 관점 발견:**
- **기획서 24절 4번:** 승인 피로를 주요 리스크로 지목
- **우려 시나리오:** 페이지 버전 변경 시마다 주장 매칭을 사용자가 승인해야 함 → UD-26(개별 승인 상한) 영향
- **MVP 범위:** UD-29가 Phase 1에서 필수인지 Phase 2+ 최적화인지 재확인 필요

**권고(미확정):**
1. **Phase 0 벤치마크에 포함:** 실측 source 37개를 입력으로 버전 변경 시나리오를 구성하고 Claim 동일성 판정 성공·교정량을 측정
2. **UD-29 확정 근거:** mapping manifest를 통한 동일성 조인 성공률·사용자 교정량·실패 유형
3. **범위 변경 금지:** 기획서 22절·23절은 사용자 승인 없이 수정하지 않고, 결과가 나오면 권고(미확정)로 먼저 제시

---

## 7. 근거 문서 & 상호 참조

### 7.1 정본 (절대 참조)

| 문서 | 최종 갱신 | 상태 |
| --- | --- | --- |
| **`docs/ZeroWiki-요구사항-정의서.md` 12절** | 2026-07-31 | ✅ UD 정본 (사용자 5 + 벤치마크 5 = 10건 미확정) |
| **`docs/ZeroWiki-MVP-서비스-기획서.md`** | 2026-07-31 | ✅ 범위·Phase·원칙 정본 |
| **`.claude/team/JOURNAL.md`** | 2026-07-31 | ✅ 시간순 업무·판단·수정 기록 (리더 소유, 신설) |

### 7.2 역할별 상태 (시점 스냅샷)

| 역할 | STATE.md | WORKLOG.md | 갱신일 | 상태 |
| --- | --- | --- | --- | --- |
| **PM** | `.claude/team/pm/STATE.md` | `.claude/team/pm/WORKLOG.md` | 2026-07-31 | ✅ 최신 |
| **Backend** | `.claude/team/backend/STATE.md` | `.claude/team/backend/WORKLOG.md` | 2026-07-31 | ✅ 최신 |
| **Frontend** | `.claude/team/frontend/STATE.md` | `.claude/team/frontend/WORKLOG.md` | 2026-07-31 | ✅ 최신 |
| **QA** | `.claude/team/qa/STATE.md` | (미생성) | 2026-07-27 | ⏳ 미갱신 |

### 7.3 검증 & 결정 문서

| 문서 | 소유 | 내용 | 갱신일 |
| --- | --- | --- | --- |
| `docs/검증-백엔드-ERD-API-정합성.md` | backend | ERD·API 교차 검증, 기술 9건 권고안 | 2026-07-30 ✅ |
| `docs/검증-프론트엔드-화면-API-갭.md` | frontend | 갭 4건 해소(#8·#9·#23·#24) + 18건 미해소(backend/리더 담당) | 2026-07-31 ✅ |
| `qa/phase0-evaluation-acceptance-report.md` | qa | CSV 구조·offline 채점 계약 인수, Ingest-level AC-5 차단 | 2026-08-10 ✅ |
| `docs/PM-미확정결정-우선순위.md` | pm | 사용자 결정 5건 요청서 | 2026-07-30 ✅ |
| `docs/PM-종합-현황.md` | pm | 종합 스냅샷 (본 문서) | 2026-07-31 ✅ |

### 7.4 벤치마크 & 계획

| 문서 | 소유 | 내용 | 갱신일 |
| --- | --- | --- | --- |
| `.claude/team/backend/PHASE-0-벤치마크-계획.md` | backend | 실측 source 37·지식 페이지 143·총 인벤토리 180, 실행 차단 조건 | 2026-08-10 ✅ 갱신 |
| `evaluation/relation-labels-template.csv` | backend | 연결 라벨 템플릿 (20~30건, 부정 예시 포함) | 2026-07-31 ✅ |
| `evaluation/contradiction-labels-template.csv` | backend | 모순 라벨 템플릿 (10~15건, 분류 5종) | 2026-07-31 ✅ |
| `docs/Phase-0-벤치마크-결과.md` | (미생성) | Phase 0 완료 후 산출 예정 | — |
