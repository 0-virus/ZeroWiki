# ZeroWiki 팀 종합 현황

작성일: 2026-07-29 23:50 UTC  
**최종 갱신: 2026-08-11 (Phase 0 기술 검증 완료·product gate 조건부 FAIL)**
대상: 리더 + 팀 전체  
기준 문서: `docs/ZeroWiki-요구사항-정의서.md` 12절(정본), 각 역할의 `STATE.md`, `.claude/team/JOURNAL.md`, `docs/PM-미확정결정-우선순위.md`

---

## 1. 전체 현황 (4개 역할)

| 역할 | 단계 | 산출물 | 상태 | 주요 차단 |
| --- | --- | --- | --- | --- |
| **PM** | Phase 0 → 1 | 종합 현황, 결정 요청서 | ✅ Phase 0 결과·착수 범위 종합 | 모순 자동 판정 gate 추적 |
| **Backend** | Phase 0 → 1 | ERD, benchmark harness·결과, constitution schema | ✅ 기술 검증 실행 완료 | 모순 classification accuracy 19.2% |
| **Frontend** | Phase 1 준비 | 도서관 홈 + 변경 검토 화면 | ✅ 계약 정합화 완료 | `node_modules`·실 API 부재 |
| **QA** | Phase 0 → 1 | 테스트 시나리오·독립 검증기·인수 보고서 | ⚠️ 구조 통과·모순 품질 FAIL | 더 강한 모델의 독립 accuracy gate |

**코드 진행도** — Frontend는 목업·실 API 미연동이며 `node_modules`가 없다. Backend는 Phase 0 stdlib harness와 schema만 있고 Spring Boot API·Ingest Worker는 아직 없다. QA는 실제 benchmark 산출물 verifier를 보유한다.
**UD 정본:** Phase 0에서 UD-11·17·25·29를 확정했다. 사용자 결정 5건(UD-03·10·19·20·22)과 Phase 4 사용자 테스트 UD-26은 계속 추적한다.

**사용자 지시와 리더 판정 (2026-08-11):** 사용자는 Phase 0 완료까지 지속 작업과 의사결정을 리더에게 위임했다. 기술 검증 실행은 완료했다. 100문서·관계·시간/token·왕복·replay·schema는 통과했지만 모순 품질은 FAIL이다. Phase 1 core는 착수하고 모순 자동 판정·적용은 차단한다.

---

## 2. 역할별 상세

### 2.1 Backend — Phase 0 실행 완료, Phase 1 core 준비

**단계:** Phase 0 기술 검증 실행 완료. 100문서 2단계 benchmark는 100/100, Markdown 왕복은 100/100, replay는 1,000 change set p95 0.0000697초다.

- mapping manifest와 generated claim join을 생성했고 QA 구조 검증을 통과했다.
- 관계 후보는 MiniLM 384차원으로 확정했다(ROC-AUC 0.9632, warm 100문서 3.418초).
- constitution 12필드 JSON Schema v1을 고정했다.
- 모순은 recall 100%지만 accuracy 19.2%·sampled FPR 100%로 product gate 실패다.
- Spring Boot API·Ingest Worker는 아직 없으므로 Phase 1 core 구현은 이제 착수 대상이다.

**차단:** 모순 자동 판정·적용. 더 강한 모델이 독립 classification accuracy 80% gate를 통과할 때까지 사용자 검토 제안으로만 다룬다.

---

### 2.2 Frontend — 목업 화면 구현 완료, Phase 0 계약 정합화 진행

**단계:** Phase 0 완료 지원 — frontend 현실·계약 정합화 진행(마지막 갱신: 2026-08-11).

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

- `frontend/node_modules`와 backend API 구현이 없어 실 API 연동·build/typecheck/dev 검증을 완료로 보고할 수 없다.
- API 명세 8.1절 위험도 wire enum은 `MINOR|MAJOR`, UI 표시는 `SAFE|DANGER`, API 2.7·6.1절 질문 상태는 `QUESTION_REVIEW`로 현재 정합화됐다(`docs/검증-프론트엔드-화면-API-갭.md` 2-D). 남은 실제 blocker는 모순 classification accuracy `19.2%`·sampled FPR `100%`이며 자동 판정·적용은 차단한다.
- Phase 0에서 UD-11·17·25·29를 확정했고 화면 계약에 반영했다. 모순 자동 판정·적용 UI는 독립 accuracy gate 전까지 차단한다(`docs/검증-프론트엔드-화면-API-갭.md` 2-C).

**1. "검토(REVIEW)" 필터 탭 처리 방향 — ✅ 해소됨 (2026-07-31)**
- **사용자 결정**: 2단계 UI 채택, "검토(REVIEW)" 탭 제거
- **frontend 구현**: `FilterTab` → `ALL|SAFE|DANGER` 3개, `UIRiskLevel`·`mapRiskLevelToUI()` 완전 제거
- **리더 검증**: 2026-07-31 당시 잔존 코드 0건·타입체크 통과·필터 로직 정확(`SAFE→MINOR`, `DANGER→MAJOR`)을 기록했다. 2026-08-11 현재 frontend 의존성 부재로 재실행은 미검증이다.
- 근거: `.claude/team/JOURNAL.md` 항목 11·12 (사용자 결정 2026-07-31, 리더 검증 완료)

**2. API 갭 목록 — 정본 2절 활성 16건** (backend/리더 담당)

| 갭 # | 내용 | 소유자 | 상태 |
| --- | --- | --- | --- |
| #1 | 글로벌 홈 전체 도서관 횡단 조회 | 리더 | ⏳ |
| #2 | 도서관 홈 요약 응답 스키마 | backend | ✅ 해소 (UD-28 반영) |
| #3 | "주요 주제" 정의 (기획서 16절) | 리더 | ✅ 해소 (UD-28 반영) |
| #4 | 페이지 주제/태그 필드 | backend | ⏳ |
| #5 | 배치 근거 조회 API | backend | ⏳ |
| #6 | 활동 집계 API (활동 잔디) | backend | ⏳ |
| #7 | 그래프 시각화 페이로드 형태 | frontend 권고 → 리더 | ⏳ |
| #10 | 알림 실시간성 | 리더 | ⏳ |
| #11 | 도서관 참조 검색 응답 형태 | backend | ⏳ |
| #12 | Ingest 계획 승인 후 상태 전이 | backend | ⏳ |
| #13~20 | 변경 검토·알림·발음표기·업로드 검증·세션 관리 | 소유자별 | ⏳ |

정본 2절의 활성 항목은 #1·#4·#5·#6·#7·#10·#11·#12·#13~#20이다. 후속 4절의 **#22(변경 영향 범위 API)** 는 별도 잔존 갭이며, #23·#24는 해소됐다. #21은 본문에서만 참조되고 별도 갭 항목이 없어 수치에 포함하지 않는다.

**3. 해소된 갭 6건 ✅**
- ✅ 갭 #2: 도서관 홈 요약의 `topics` 산출 기준 (2026-07-29, UD-28 확정)
- ✅ 갭 #3: 주요 주제 산출 기준 (2026-07-29, UD-28 확정)
- ✅ 갭 #8: diff 표현 (2026-07-29, UD-05 확정: Phase 1 스냅샷 + 클라이언트 diff)
- ✅ 갭 #9: polling 간격 (2026-07-29, UD-04 확정: 기본값 2초)
- ✅ 갭 #23: REQUEST_CHANGES 재생성 감지 (2026-07-30 backend 확정, 2026-07-31 frontend 구현·리더 검증)
- ✅ 갭 #24: 모순 조회 API (2026-07-30 리더 확인)

**다음 작업:**
1. 리더가 위험도 enum·질문 상태명을 확정하면 frontend 계약을 정합화한다.
2. 인증→도서관/헌법→업로드→Ingest→변경 검토의 첫 vertical slice를 설계한다.
3. 의존성 설치 후 build/typecheck와 라우트·렌더링을 재검증한다.

---

### 2.3 QA — benchmark 독립 검증 완료, 모순 품질 FAIL

**단계:** Phase 0 구조·실행 산출물 독립 검증 완료(마지막 갱신: 2026-08-11).

**완료 항목** (`.claude/team/qa/STATE.md`, `qa/phase0-evaluation-acceptance-report.md`):

- `qa/verify_phase0_evaluation.py`와 backend validator가 동일 실측에서 모두 종료 코드 0을 반환했다.
- relation slug 헤더, contradiction stable `case_id`, pair/strict recall·bounded precision·sampled FPR, contradiction recall·FPR·FDR와 0분모 N/A 계약을 재인수했다.
- mapping manifest·100문서 결과·contradiction output의 run ID와 join을 검증했다.
- recall-only verifier가 전부 conflict로 예측한 결과를 false PASS하던 결함을 발견해 전체 classification accuracy 80% gate를 추가했다.
- 제품 테스트 시나리오는 A 18개·U 6개·S 16개로 총 40개가 정의돼 있으나, 제품 구현 대상 테스트는 모두 미실행이다.

**남은 차단과 위험:**

1. 모순 classification accuracy 19.2%·sampled FPR 100%로 품질 gate 실패.
2. AC-3: U-1 검증 사용자 5~10명의 실제 유용성 평가는 Phase 4에서 실행한다.
3. sampled FPR 분해능: contradiction `NO_CONFLICT` 3건은 1건당 33.3%p다.

**다음 작업:** 더 강한 contradiction 모델 후보의 동일 26건 독립 재평가. 그전에는 모순 자동 판정·적용을 허용하지 않는다.

---

### 2.4 PM — Phase 0 결과와 Phase 1 허용 범위 종합

**단계:** Phase 0 기술 검증 완료·product gate 조건부 FAIL을 종합(마지막 갱신: 2026-08-11).

**확정 사실:**

- 평가 CSV는 실측 180/84/26행이며 구조·offline 채점 계약이 독립 검증을 통과했다.
- UD-11·17·25·29는 Phase 0 결과로 확정했다.
- AC-3·UD-26은 Phase 4 사용자 테스트로 유지한다.

**다음 한 걸음:** **Phase 1 core scaffolding 착수.**

- 허용: 인증·원본 보관·도서관·Import·변경 승인·검색 골격.
- 차단: 모순 자동 판정·자동 적용.
- 계약: wire `MINOR|MAJOR`, UI `SAFE|DANGER`, 질문 상태 `QUESTION_REVIEW`.

**의존 사슬:** Phase 1 core → 실제 API integration benchmark → 더 강한 contradiction 모델 독립 gate. AC-3·UD-26은 Phase 4 사용자 테스트로 분기한다.

**사용자 결정 묶음:** 기존 UD-03·10·19·20·22는 결정 요청서대로 유지한다. 실제 외부 호출 전에는 LLM provider/model, 유료 API 사용 범위, 토큰·비용 예산에 대한 사용자 결정·승인이 추가로 필요하다.

**Phase 0 상태:** 기술 검증 실행 완료. 상세 결과 문서는 `docs/Phase-0-벤치마크-결과.md`다. 모순 품질 실패는 숨기지 않고 Phase 1 기능 gate로 이관한다.

**남은 검증:** 실제 Spring Boot/Ingest Worker가 생긴 뒤 reference budget과 citation을 integration으로 재검증한다.

---

## 3. 미확정 계약 현황 (결정 주체·Phase 분리)

### 3.1 사용자 결정 필요 (5건) — Phase별 구현 시 결정

| UD | 내용 | Phase | 차단 기능 | 문서 |
| --- | --- | --- | --- | --- |
| **UD-03** | Free·Basic·Advanced 처리량 단위 | 1(도서관 한도) / 2(처리량 과금) | API 18절 3 / 가격 모델 | PM-미확정결정-우선순위.md 2.2절 |
| **UD-10** | Notion OAuth·결제 공급자 선정 | 2 | API 18절 10 / 외부 연동 | PM-미확정결정-우선순위.md 2.3절 |
| **UD-19** | 처리량 표시 단위·토큰 환산 규칙 | 2 | ERD 8절 9 / 과금 UI | PM-미확정결정-우선순위.md 2.4절 |
| **UD-20** | 감사 로그 보존 기간·접근 정책 | 2 | ERD 8절 10 / 감사 시스템 | PM-미확정결정-우선순위.md 2.5절 |
| **UD-22** | 신고 처리 주체·검토 기한 | 3 | 기획서 15절 / 공개·포크 | 미확정 |

**상태:** 모두 사용자 결정 대기 중이다. 평가 데이터 인수로 자동 해소되지 않았으며, 제품·Phase 필요 시점에 맞춰 결정한다.

### 3.2 기술 결정 — 0건 (모두 확정 ✅)

**확정 12건** (2026-07-30, 요구사항 정의서 v1.1 기준): 
- API 정책: UD-04(polling), UD-05(diff), UD-08(slug), UD-09(관리자 API)
- 데이터 모델: UD-12(Claim 재사용), UD-13(이벤트 재생), UD-15(JSON Schema), UD-16(관계 유형), UD-18(Notion 중복)
- 스케줄/기능: UD-21(Lint 스케줄), UD-23(알림 보존), UD-28(주요 주제)

### 3.3 벤치마크 결정 완료 4건 + Phase 4 별도 1건

| UD | 내용 | 대상 | 방법 | 우선순위 | 비고 |
| --- | --- | --- | --- | --- | --- |
| **UD-11** | 중요한 주장만 생성 | API·구조 설계 | 100문서 7,501문장 대비 100 Claim | ✅ 확정 | 모순 자동 판정 차단 |
| **UD-17** | MiniLM 384차원 | 검색 설계 | embedding 후보 2종 비교 | ✅ 확정 | MPNet은 품질 필요 시 재검토 |
| **UD-25** | reference p95 scan 15초·generation 20초 | 인프라 설계 | 100문서 실측 | ✅ 확정 | Phase 1 integration 재검증 |
| **UD-29** | AI 제안 + 사용자 승인 | Claim 버전 연속성 | mapping manifest·품질 결과 | ✅ 확정 | 자동 identity key 금지 |
| **UD-26** | 개별 승인 건수 상한 (승인 피로) | UX 설계 | **Phase 4 사용자 테스트** | 🟢 별도 | Phase 0 직접 결정 대상 아님 |

**경계:** UD-11·17·25·29는 확정했다. UD-26은 Phase 4 사용자 피로 테스트로 결정한다.

---

## 4. 우선순위 & 리더의 즉시 행동

### 다음 한 걸음 하나

**Phase 1 core scaffolding을 착수한다.** 인증·원본 보관·도서관·Import·변경 승인·검색 골격을 우선하며 모순 자동 판정·적용은 feature gate로 막는다.

**의존 사슬:** Phase 1 core → 실제 API integration benchmark → 더 강한 contradiction 모델 독립 gate. AC-3 U-1과 UD-26은 Phase 4 분기다.

**사용자 결정·승인 분리:** harness의 로컬 계약·골격 작업과 별개로, 실제 외부 호출 전에 LLM provider/model, 유료 API 사용 범위, 토큰·비용 예산을 사용자에게 결정·승인받는다. 기존 사용자 UD-03·10·19·20·22도 정본 12절과 결정 요청서에서 계속 추적한다.

---

## 5. 주요 발견 & 기획 정합성 검토

### 5.1 문서 정합성 ✅ 확인 (3개 문서쌍)

- **기획서 ↔ 요구사항**: 2건 불일치 해소 (17절 알림·20절 작업 상태)
- **요구사항 ↔ ERD·API**: 기술 결정 12건 반영으로 정합성 확보
- **frontend 갭**: 정본 2절 활성 16건(#1·#4·#5·#6·#7·#10·#11·#12·#13~#20) + 후속 4절 #22 잔존, #2·#3·#8·#9·#23·#24 해소. #21은 별도 항목 부재로 미집계.
- **frontend 계약**: wire `MINOR|MAJOR`, UI `SAFE|DANGER`, 질문 상태 `QUESTION_REVIEW`로 정합화 완료(`docs/검증-프론트엔드-화면-API-갭.md` 2-D).

### 5.2 범위 & 우선순위 관리 ✅ 정책 준수

- **MVP 범위**: 기획서 22절 제외 목록 엄격히 준수
- **Phase 순서**: Phase 0 기술 검증 실행 완료. Phase 1 core 착수 허용, 모순 자동 판정·적용 차단. **UD-26은 Phase 4** 사용자 테스트로 유지한다.
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
| **AC-5 품질 경계** | 실행 계약 | mapping join은 통과했으나 accuracy 19.2%·FPR 100% | ⛔ 자동 판정 차단 |
| **UD-29 UX 영향** | Phase 0 확정 | AI mapping 제안은 기존 변경 승인 흐름에서 사용자가 확정 | ✅ 확정 |
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

## 6. 현황 수치 (2026-08-11 기준)

### 6.1 코드 진행도

| 항목 | 산출물 | 상태 |
| --- | --- | --- |
| **Frontend** | Next.js 프로젝트 초기화, 도서관 홈·변경 검토 화면 2종 | 목업·실 API 미연동. `node_modules` 부재; Phase 0 계약 반영 완료 |
| **Backend** | ERD, Phase 0 harness·결과, constitution schema | 기술 검증 실행 완료. Spring Boot API·Worker는 미구현 |
| **QA** | 테스트 시나리오, 평가·benchmark 독립 검증기 | 구조 통과, 모순 품질 gate FAIL, AC-3은 Phase 4 |

### 6.2 기획서 수정·유보 상태

| 항목 | 수정 내용 | 절 번호 | 근거 | 상태 |
| --- | --- | --- | --- | --- |
| 기획서 17절 | 알림 목록 → 명시적 타입 재정의 | 17절 | 요구사항 정의서 불일치 해소 | ✅ 2026-07-29 |
| 기획서 20절 | 작업 상태 8종 명확화 | 20절 | 기획서 내부 일관성 | ✅ 2026-07-29 |
| 기획서 16절 | 주요 주제 정의 추가 (AI 자동 선정) | 16절·UD-28 | frontend 갭 해소 | ✅ 2026-07-30 |
| 기획서 22절 | 변경 없음 | 22절 | 사용자 승인 없는 MVP 제외 목록 변경 금지 | ✅ 유지 |
| 기획서 23절 | 변경 없음 | 23절 | 사용자 승인 없는 Phase 정의 변경 금지 | ✅ 유지 |

### 6.3 정합성 상태 (2026-08-11)

| 대상 | 상태 | 기준 | 갱신일 |
| --- | --- | --- | --- |
| **기획서 ↔ 요구사항** | 2건 불일치 해소, 정합성 확보 | `docs/ZeroWiki-요구사항-정의서.md` 12절 v1.1 | 2026-07-31 |
| **요구사항 ↔ ERD** | 기술 9건 확정, v1.5 정합성 확인 | ERD 8절 + UD 정본 | 2026-07-31 |
| **요구사항 ↔ API 명세** | 기술 4건 확정, 18절 미확정 정의 | API 명세 18절 + UD 정본 | 2026-07-31 |
| **Frontend 갭** | 정본 2절 활성 16건 + 후속 #22 잔존; #2·#3·#8·#9·#23·#24 해소; #21 미집계 | `docs/검증-프론트엔드-화면-API-갭.md` 2절·4절 | 2026-08-11 |
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
| **PM** | `.claude/team/pm/STATE.md` | `.claude/team/pm/WORKLOG.md` | 2026-08-11 | ✅ 최신 |
| **Backend** | `.claude/team/backend/STATE.md` | `.claude/team/backend/WORKLOG.md` | 2026-08-10 | ✅ 최신 |
| **Frontend** | `.claude/team/frontend/STATE.md` | `.claude/team/frontend/WORKLOG.md` | 2026-08-11 | ✅ 최신 |
| **QA** | `.claude/team/qa/STATE.md` | `.claude/team/qa/WORKLOG.md` | 2026-08-10 | ✅ 최신 |

### 7.3 검증 & 결정 문서

| 문서 | 소유 | 내용 | 갱신일 |
| --- | --- | --- | --- |
| `docs/검증-백엔드-ERD-API-정합성.md` | backend | ERD·API 교차 검증, 기술 9건 권고안 | 2026-07-30 ✅ |
| `docs/검증-프론트엔드-화면-API-갭.md` | frontend | 정본 2절 활성 16건 + 후속 #22 잔존; #2·#3·#8·#9·#23·#24 해소; 2-C·2-D 추가 대조 | 2026-08-11 ✅ |
| `qa/phase0-evaluation-acceptance-report.md` | qa | CSV·benchmark 계약 인수, 모순 품질 gate FAIL | 2026-08-11 ⚠️ |
| `docs/PM-미확정결정-우선순위.md` | pm | 사용자 결정 5건 요청서 | 2026-08-11 ✅ |
| `docs/PM-종합-현황.md` | pm | 종합 스냅샷 (본 문서) | 2026-08-11 ✅ |

### 7.4 벤치마크 & 계획

| 문서 | 소유 | 내용 | 갱신일 |
| --- | --- | --- | --- |
| `.claude/team/backend/PHASE-0-벤치마크-계획.md` | backend | 실측 source 37·지식 페이지 143·총 인벤토리 180, 실행 차단 조건 | 2026-08-10 ✅ 갱신 |
| `evaluation/relation-labels-template.csv` | backend | 연결 라벨 템플릿 (20~30건, 부정 예시 포함) | 2026-07-31 ✅ |
| `evaluation/contradiction-labels-template.csv` | backend | 모순 라벨 템플릿 (10~15건, 분류 5종) | 2026-07-31 ✅ |
| `docs/Phase-0-벤치마크-결과.md` | leader | 100문서·관계·모순·token/시간·왕복·replay·UD 결정 | 2026-08-11 ✅ |
