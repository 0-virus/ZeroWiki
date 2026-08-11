# ZeroWiki 팀 종합 현황

작성일: 2026-07-29 23:50 UTC  
**최종 갱신: 2026-08-11 23:02 +09:00 (Phase 1 즉시 착수 가능성·계약 차단 감사)**
대상: 리더 + 팀 전체  
기준 문서: `docs/ZeroWiki-요구사항-정의서.md` 12절(정본), 각 역할의 `STATE.md`, `.claude/team/JOURNAL.md`, `docs/PM-미확정결정-우선순위.md`

---

## 1. 전체 현황 (4개 역할)

| 역할 | 단계 | 산출물 | 상태 | 주요 차단 |
| --- | --- | --- | --- | --- |
| **PM** | Phase 1 착수 감사 | 종합 현황, 결정 요청서 | ✅ core 착수 경계·최소 vertical slice 종합 | Spring Boot scaffold·holdout gate 추적 |
| **Backend** | Phase 1 계약 감사 | ERD, benchmark harness·결과, constitution schema | ✅ v3 dev gate PASS, feature 구현 미착수 | Spring Boot/Ingest scaffold 부재·holdout API 미실행 |
| **Frontend** | Phase 1 준비 | 도서관 홈 + 변경 검토 화면 | ✅ build/typecheck PASS, 목업만 연동 | backend 실 API·첫 slice 계약 연결 부재 |
| **QA** | Phase 1 품질 대기 | 독립 verifier·holdout review/gold | ✅ v3 dev PASS·70-case gold 동결 | holdout API/validator·production gate 미실행 |

**코드 진행도** — Frontend는 목업·실 API 미연동이지만 `node_modules`가 있고 production build/typecheck가 통과했다. Backend는 Phase 0 stdlib harness와 schema만 있고 Spring Boot API·Ingest Worker는 아직 없다. QA는 v3 dev verifier와 70-case 사용자 review/machine gold를 보유한다.
**UD 정본:** Phase 0에서 UD-11·17·25·29를 확정했다. 사용자 결정 5건(UD-03·10·19·20·22)과 Phase 4 사용자 테스트 UD-26은 계속 추적한다.

**사용자 지시와 리더 판정 (2026-08-11):** 사용자는 Phase 0 완료까지 지속 작업과 필요한 의사결정을 리더에게 위임했다. 기술 검증 실행과 `rubric-context-v3` 26-case dev gate는 통과했지만 holdout API·production 승격은 미실행·미확정이다. Phase 1 core는 계약·scaffold 준비를 진행할 수 있고, 모순 자동 판정·적용은 holdout 및 독립 production gate 전까지 차단한다.

---

## 2. 역할별 상세

### 2.1 Backend — Phase 0 실행 완료, Phase 1 feature 구현은 scaffold 대기

**단계:** Phase 0 기술 검증 실행 완료. 100문서 2단계 benchmark는 100/100, Markdown 왕복은 100/100, replay는 1,000 change set p95 0.0000697초다.

- mapping manifest와 generated claim join을 생성했고 QA 구조 검증을 통과했다.
- 관계 후보는 MiniLM 384차원으로 확정했다(ROC-AUC 0.9632, warm 100문서 3.418초).
- constitution 12필드 JSON Schema v1을 고정했다.
- 기존 baseline 모순 결과는 accuracy 19.2%·sampled FPR 100%로 실패했지만, 최신 `rubric-context-v3` dev 결과는 26/26 완료·recall 0.957·accuracy 0.923·sampled FPR 0으로 dev gate를 통과했다. 두 결과는 서로 다른 실행으로 보존한다.
- 현재 `backend/` 실측에는 `contracts/`·`scripts/`만 있고 `pom.xml`·Gradle 파일·`src/main/java`가 없어 Spring Boot API·Ingest Worker feature 구현과 빌드 검증을 시작할 실행 scaffold가 없다. 지금 가능한 일은 계약·설계·scaffold 준비다.

**차단:** (1) feature 구현은 Spring Boot/Ingest scaffold 부재, (2) 모순 자동 판정·적용은 holdout API/validator와 독립 production gate 미실행. Phase 1에서는 모순을 사용자 검토 제안으로만 다룬다.

---

### 2.2 Frontend — 목업 화면 구현·정적 검증 완료, 실 API 연결 대기

**단계:** Phase 1 첫 slice 연결 준비(마지막 갱신: 2026-08-11).

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

- backend API가 없어 실 API 연동은 미실행이지만 `npm.cmd run build`와 `npx.cmd tsc --noEmit`는 2026-08-11 모두 종료 코드 0이다. 현재 화면은 목업 데이터를 직접 소비한다.
- API 명세 8.1절 위험도 wire enum은 `MINOR|MAJOR`, UI 표시는 `SAFE|DANGER`, API 2.7·6.1절 질문 상태는 `QUESTION_REVIEW`로 정합화됐다(`docs/검증-프론트엔드-화면-API-갭.md` 2-D). 최신 v3 dev gate는 통과했지만 holdout 전 모순 자동 판정·적용은 차단한다.
- `HeaderBar`의 반복 `href: '/'`에 동일 React key가 있어 dev console duplicate-key 경고가 남아 있다. 렌더링·첫 slice 착수는 막지 않지만 API 연결 전에 수정해야 한다.
- Phase 0에서 UD-11·17·25·29를 확정했고 화면 계약에 반영했다. 모순 자동 판정·적용 UI는 독립 accuracy gate 전까지 차단한다(`docs/검증-프론트엔드-화면-API-갭.md` 2-C).

**1. "검토(REVIEW)" 필터 탭 처리 방향 — ✅ 해소됨 (2026-07-31)**
- **사용자 결정**: 2단계 UI 채택, "검토(REVIEW)" 탭 제거
- **frontend 구현**: `FilterTab` → `ALL|SAFE|DANGER` 3개, `UIRiskLevel`·`mapRiskLevelToUI()` 완전 제거
- **리더 검증**: 2026-07-31 당시 잔존 코드 0건·타입체크 통과·필터 로직 정확(`SAFE→MINOR`, `DANGER→MAJOR`)을 기록했고, 2026-08-11 build/typecheck도 종료 코드 0으로 재검증했다.
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
1. backend scaffold가 마련되면 인증→도서관/헌법→원본 메타데이터·불변 버전 조회의 첫 vertical slice를 API와 연결한다.
2. 업로드→Ingest 계획 승인→변경 검토·적용→검색 순서로 확장한다.
3. API 연결 후 build/typecheck와 두 라우트·렌더링을 재검증한다.

---

### 2.3 QA — dev 품질 PASS, holdout 실행 대기

**단계:** `rubric-context-v3` dev 독립 검증 및 70-case review/gold freeze 완료(마지막 갱신: 2026-08-11).

**완료 항목** (`.claude/team/qa/STATE.md`, `qa/phase0-evaluation-acceptance-report.md`):

- `qa/verify_phase0_evaluation.py`와 backend validator가 동일 실측에서 모두 종료 코드 0을 반환했다.
- relation slug 헤더, contradiction stable `case_id`, pair/strict recall·bounded precision·sampled FPR, contradiction recall·FPR·FDR와 0분모 N/A 계약을 재인수했다.
- mapping manifest·100문서 결과·contradiction output의 run ID와 join을 검증했다.
- recall-only verifier가 전부 conflict로 예측한 결과를 false PASS하던 결함을 발견해 전체 classification accuracy 80% gate를 추가했다.
- 제품 테스트 시나리오는 A 18개·U 6개·S 16개로 총 40개가 정의돼 있으나, 제품 구현 대상 테스트는 모두 미실행이다.
- `rubric-context-v3`는 26/26 완료, recall 0.957, accuracy 0.923, sampled FPR 0, validator exit 0이다.
- 사용자용 `qa/contradiction-holdout-review.csv`와 machine `qa/contradiction-holdout-gold.csv`는 각각 70행이며 사용자 결정 70건이 모두 `승인`, exact UTF-8 헤더·목표 분포가 확인됐다.

**남은 차단과 위험:**

1. holdout API와 `--gold-labels` validator가 아직 실행되지 않아 holdout recall/accuracy/FPR/Wilson gate를 모른다.
2. production LLM provider/model·유료 API 범위·승격은 사용자 승인 전 확정하지 않는다.
3. AC-3: U-1 검증 사용자 5~10명의 실제 유용성 평가는 Phase 4에서 실행한다.

**다음 작업:** 별도 실행 승인을 받은 뒤 frozen gold로 holdout API·validator를 실행하고, 그 결과가 나오기 전에는 모순 자동 판정·적용과 production 승격을 허용하지 않는다.

---

### 2.4 PM — Phase 0 결과와 Phase 1 허용 범위 종합

**단계:** Phase 0 결과와 Phase 1 즉시 착수 경계를 종합(마지막 갱신: 2026-08-11 23:02 +09:00).

**확정 사실:**

- 평가 CSV는 실측 pages 180(source 37/concept 127/flow 16), relations 84, contradictions 26행이며 구조·offline 채점 계약이 독립 검증을 통과했다.
- UD-11·17·25·29는 Phase 0 결과로 확정했다.
- AC-3·UD-26은 Phase 4 사용자 테스트로 유지한다.

**권고 1건:** **Spring Boot/Ingest 실행 scaffold를 먼저 마련한 뒤, `인증 + 도서관 소유권 경계 + 공통 원본 메타데이터/불변 버전 조회`를 첫 최소 vertical slice로 구현한다.** 근거는 기획서 23절 Phase 1의 인증·원본 보관소·도서관·운영 헌법 및 API 명세 17절 1~2번 순서다. 현재는 계약·설계·scaffold 준비만 즉시 가능하고 feature 구현은 backend scaffold 부재로 막혀 있다.

- 허용: 인증·원본 보관·도서관·Import·변경 승인·검색 골격의 계약·scaffold 준비와 이후 구현.
- 차단: 모순 자동 판정·자동 적용.
- 계약: wire `MINOR|MAJOR`, UI `SAFE|DANGER`, 질문 상태 `QUESTION_REVIEW`.

**의존 사슬:** `Spring Boot/Ingest scaffold → 인증·사용자·도서관·운영 헌법 → 업로드·원본·도서관 연결 → Ingest 계획 승인 상태 머신 → 페이지·버전·주장·근거·관계·모순 조회 → 변경 세트 검토·적용·도서관 버전 → 검색`(API 명세 17절). 이후 `실 API integration p95/citation 검증 → holdout API/validator → 독립 production gate`를 거치며, AC-3·UD-26은 Phase 4로 분기한다.

**사용자 결정 묶음:** 기존 UD-03·10·19·20·22는 결정 요청서대로 유지한다. 실제 외부 호출 전에는 LLM provider/model, 유료 API 사용 범위, 토큰·비용 예산에 대한 사용자 결정·승인이 추가로 필요하다.

**Phase 0 상태:** 기술 검증 실행 및 v3 dev gate는 완료됐다. 상세 결과 문서는 `docs/Phase-0-벤치마크-결과.md`다. v3 dev PASS와 holdout 미실행을 혼동하지 않으며 모순 자동 판정·적용은 계속 기능 gate 뒤에 둔다.

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

**사용자 결정 묶음:** `UD-03`은 Phase 1에서 요금제별 도서관 한도를 구현할 때만 선행되고 처리량 과금은 Phase 2다. `UD-10`(Notion OAuth·결제 공급자)과 `UD-19`(처리량 표시·토큰 환산)은 Phase 2 외부 연동·과금 경로를 막지만 Phase 1 파일/Obsidian·기본 도서관 코어를 막지 않는다. `UD-20`은 Phase 2 감사 로그 보존·관리자 접근 정책을, `UD-22`는 Phase 3 신고 절차를 막는다. 이 분리는 `docs/ZeroWiki-요구사항-정의서.md` 12.1~12.2절과 `docs/ZeroWiki-MVP-서비스-기획서.md` 23절을 대조한 결과이며, pm은 값을 확정하지 않는다.

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

**Spring Boot/Ingest scaffold 준비를 먼저 완료한다.** 그 뒤 인증·도서관 소유권·원본 메타데이터/불변 버전 조회를 첫 최소 vertical slice로 구현하고, 모순 자동 판정·적용은 feature gate로 막는다.

**의존 사슬:** scaffold → API 명세 17절 순서의 core 기능 → 실 API integration p95/citation → holdout API/validator → production gate. AC-3 U-1과 UD-26은 Phase 4 분기다.

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
- **Phase 순서**: Phase 0 기술 검증 실행 완료. Phase 1 core의 계약·scaffold 준비는 허용하지만 feature 구현은 Spring Boot/Ingest scaffold 후에 시작한다. 모순 자동 판정·적용은 차단하며, **UD-26은 Phase 4** 사용자 테스트로 유지한다.
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
| **AC-5 품질 경계** | 실행 계약 | 과거 baseline은 accuracy 19.2%·FPR 100%; 최신 v3 dev는 accuracy 0.923·sampled FPR 0 | holdout 전 자동 판정 차단 |
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
| **Frontend** | Next.js 프로젝트 초기화, 도서관 홈·변경 검토 화면 2종 | 목업·실 API 미연동; build/typecheck 종료 코드 0, Phase 0 계약 반영 완료 |
| **Backend** | ERD, Phase 0 harness·결과, constitution schema | 기술 검증 실행 완료. Spring Boot API·Worker는 미구현 |
| **QA** | 테스트 시나리오, 평가·benchmark 독립 검증기, holdout review/gold | v3 dev PASS, holdout API/production gate 미실행, AC-3은 Phase 4 |

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
| **요구사항 ↔ API 명세** | Phase 1 순서·기술 계약 정합, 사용자 UD는 12절 대기 | API 명세 17~18절 + UD 정본 | 2026-08-11 |
| **Frontend 갭** | 정본 2절 활성 16건 + 후속 #22 잔존; #2·#3·#8·#9·#23·#24 해소; #21 미집계 | `docs/검증-프론트엔드-화면-API-갭.md` 2절·4절 | 2026-08-11 |
| **미확정 분류** | 사용자 UD-03·10·19·20·22 + Phase 4 UD-26; Phase 0 UD-11·17·25·29는 확정 | 요구사항 정의서 12절 | 2026-08-11 |

---

## 6.4 UD-29 현재 기획 경계

`UD-29`는 Phase 0 결과로 **확정**됐다. 자동 identity key는 만들지 않고, AI mapping을 변경 세트로 제안한 뒤 사용자가 승인한다. 평가 manifest의 generated ID는 평가 조인용일 뿐 자동 동일성 구현이 아니다(`docs/ZeroWiki-요구사항-정의서.md` 12.2절, `docs/ZeroWiki-ERD-초안.md` 8절).

따라서 Phase 1 변경 검토는 사용자 승인 흐름을 제공하되, 모순 자동 판정·자동 적용과 섞지 않는다. 승인 피로 임계값은 `UD-26`으로 남아 기획서 23절상 Phase 4 사용자 테스트에서 다룬다. 기획서 22절·23절의 범위·Phase는 사용자 승인 없이 변경하지 않는다.

---

## 7. 근거 문서 & 상호 참조

### 7.1 정본 (절대 참조)

| 문서 | 최종 갱신 | 상태 |
| --- | --- | --- |
| **`docs/ZeroWiki-요구사항-정의서.md` 12절** | 정본 직접 대조 | ✅ UD 결정 주체·미확정 값의 단일 원본 |
| **`docs/ZeroWiki-MVP-서비스-기획서.md`** | 2026-07-31 | ✅ 범위·Phase·원칙 정본 |
| **`.claude/team/JOURNAL.md`** | 2026-07-31 | ✅ 시간순 업무·판단·수정 기록 (리더 소유, 신설) |

### 7.2 역할별 상태 (시점 스냅샷)

| 역할 | STATE.md | WORKLOG.md | 갱신일 | 상태 |
| --- | --- | --- | --- | --- |
| **PM** | `.claude/team/pm/STATE.md` | `.claude/team/pm/WORKLOG.md` | 2026-08-11 | ✅ 최신 |
| **Backend** | `.claude/team/backend/STATE.md` | `.claude/team/backend/WORKLOG.md` | 2026-08-11 | ✅ 최신 |
| **Frontend** | `.claude/team/frontend/STATE.md` | `.claude/team/frontend/WORKLOG.md` | 2026-08-11 | ✅ 최신 |
| **QA** | `.claude/team/qa/STATE.md` | `.claude/team/qa/WORKLOG.md` | 2026-08-11 | ✅ 최신 |

### 7.3 검증 & 결정 문서

| 문서 | 소유 | 내용 | 갱신일 |
| --- | --- | --- | --- |
| `docs/검증-백엔드-ERD-API-정합성.md` | backend | ERD·API 교차 검증, 기술 9건 권고안 | 2026-07-30 ✅ |
| `docs/검증-프론트엔드-화면-API-갭.md` | frontend | 정본 2절 활성 16건 + 후속 #22 잔존; #2·#3·#8·#9·#23·#24 해소; 2-C·2-D 추가 대조 | 2026-08-11 ✅ |
| `qa/phase0-evaluation-acceptance-report.md` | qa | v3 dev PASS, holdout gold 동결, holdout·production BLOCKED | 2026-08-11 ⚠️ |
| `docs/PM-미확정결정-우선순위.md` | pm | 사용자 결정 5건 요청서 | 2026-08-11 ✅ |
| `docs/PM-종합-현황.md` | pm | 종합 스냅샷 (본 문서) | 2026-08-11 ✅ |

### 7.4 벤치마크 & 계획

| 문서 | 소유 | 내용 | 갱신일 |
| --- | --- | --- | --- |
| `.claude/team/backend/PHASE-0-벤치마크-계획.md` | backend | 실측 source 37·지식 페이지 143·총 인벤토리 180, 실행 차단 조건 | 2026-08-10 ✅ 갱신 |
| `evaluation/relation-labels-template.csv` | backend | 연결 라벨 템플릿 (20~30건, 부정 예시 포함) | 2026-07-31 ✅ |
| `evaluation/contradiction-labels-template.csv` | backend | 모순 라벨 템플릿 (10~15건, 분류 5종) | 2026-07-31 ✅ |
| `docs/Phase-0-벤치마크-결과.md` | leader | 100문서·관계·모순·token/시간·왕복·replay·UD 결정 | 2026-08-11 ✅ |
