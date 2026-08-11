# qa 작업 저널

> **append-only.** 새 항목을 파일 맨 아래에 추가한다. 기존 항목은 수정·삭제하지 않는다(헌법 제4조 2항).
> 형식: `날짜 / 무엇을 했는가 / 산출물 경로 / 미해결 사항`
> 현재 상태 스냅샷은 `STATE.md`를 본다.

---

## 2026-07-27 — 검증·계약 확정 체크리스트 초안 (세션 a8461ee3)

**한 일**

- 미확정 계약 20건 권고안 초안 — API 명세 18절 10건, ERD 8절 10건. `사용자 결정 ✅` / `기술적 권고 ⚠️` 구분
- MVP 검증 기준(기획서 3절)을 측정 가능한 테스트로 변환 — A-1~A-5 자동화, U-1~U-4 사용자 평가
- 보안 요구사항·제품 리스크 매트릭스 — S-1~S-10 공격 시나리오와 기획서상 방어 위치
- 에러 계약과 엣지케이스 커버리지 검증
- 검증 일정·책임 배분 정의

**산출물**

- `docs/검증-QA-계약-확정-체크리스트.md`

**미해결**

- frontend·backend 의견 미수집 → 권고안 통합 불가
- 통합 섹션(9절) 미작성
- 검증 기준의 테스트 코드 전환 미착수
- 평가 세트(정답 연결·모순 후보 사람 표시) 미작성 — 기획서 25절 2번

---

## 2026-07-29 — 계약 변경 전파 및 검증 체크리스트 갱신 (세션 aqa-7713a3a78852c69e)

**한 일**

1. **헌법·지침·상태 읽기**
   - `.claude/CONSTITUTION.md` v1.1 (pm 역할 신설, 제1조 6·7항 추가)
   - `.claude/team/qa/CLAUDE.md`, `STATE.md` 기존 상태 확인

2. **변경된 기준 문서 검토**
   - `docs/ZeroWiki-요구사항-정의서.md` 완독 (신규, v0.5) — FR·NFR 14군·6군, UD-01~27 정의
   - `docs/ZeroWiki-API-명세-초안.md` (v0.2 2026-07-29 조정) — 15절 보안 신규 13·14·15번, 18절·19절 추적
   - `docs/ZeroWiki-ERD-초안.md` 부분 읽기 — 4절 idempotency_records 캐시 규칙 확인
   - `docs/ZeroWiki-MVP-서비스-기획서.md` 부분 확인 — 17절·20절 알림·상태 정의

3. **체크리스트 갱신**
   - **2절 미확정 계약:** UD-01~27 번호 체계로 정렬. 사용자 결정 9건 + 기술 권고 13건 + 신규 5건 + 확정 2건으로 분류. 정본을 요구사항 정의서 12절로 명시
   - **3절 검증 기준:** 신규 항목 8개 추가
     - A-6~A-8: 언어·발췌 검증 (FR-UIX-12·13·14)
     - A-9~A-10: 운영 상태 2종 검증 (FR-ING-26)
     - U-5~U-6: 상태 구별·변경 승인 4종 검증
     - S-14: 응답 캐시 단기 수명 값 제외 (NFR-SEC-09)
   - **3절 보안:** S-1~S-10에 S-11~S-13 추가 (API 15절 13·14·15번 신규)

4. **교차 검증 수행**
   - **4절 신규 추가:** 세 문서 간 일관성 검증
   - **발견 1:** API 19절 추적 매트릭스에서 FR-ING-26, NFR-SEC-13·14·15 미기재
   - **발견 2:** ERD enum 확인 미완료 — ingest_jobs.status가 CANCELLED, PAUSED_QUOTA 수용하는지 미확인
   - **발견 3:** UD 번호는 요구사항·API·ERD 간 일관적이나, API 18절이 UD-21~27 신규 항목 모두 기재했는지 재검토 필요

5. **STATE 갱신**
   - 현재 단계: 컨트랙트 검증 단계 → 체크리스트 갱신 완료
   - 다음 작업: backend 확인, 리더 갱신 요청, 테스트 시나리오 상세화

**산출물**

- `docs/검증-QA-계약-확정-체크리스트.md` (2절 UD 27건, 3절 신규 항목 8개, 4절 교차 검증)
- `.claude/team/qa/STATE.md` (현재 단계 갱신)

**미해결**

- backend 확인 필요: ERD enum (CANCELLED, PAUSED_QUOTA) 구현 여부
- 리더 갱신 필요: API 19절에 신규 항목(FR-ING-26, NFR-SEC-13·14·15) 추가
- 테스트 시나리오 상세화 미착수 (3절 A/U/S를 실행 가능한 테스트로 변환)
- 평가 세트 미작성 (기획서 25절 2번)

---

## 2026-07-29 — STATE 최신화 및 UD 분류 재계산 (세션 aqa-state-49dac4e90d49aec6)

**한 일**

1. **지침 파일 재읽기**
   - `.claude/CONSTITUTION.md` 1.1 확인 (헌법 > 모든 하위 지침)
   - `.claude/team/qa/CLAUDE.md` 확인 (검증 태도: 무비판적 수용 금지, 근거 문서 직접 확인)

2. **정본 문서 직접 확인**
   - `docs/ZeroWiki-요구사항-정의서.md` 12절 완전 읽기
     - UD-01~UD-20 기존 20건 (12.1절)
     - UD-21~UD-28 신규 8건 (12.2절) — **UD-28 신규 발견(도서관 홈 topics 산출 기준)**
     - 확정: UD-04, UD-05(리더 기술, 2026-07-29), UD-24, UD-27(사용자, 2026-07-29)
   - `docs/ZeroWiki-ERD-초안.md` 300~304행
     - PAUSED_QUOTA, CANCELLED 정의 확인 ✓
   - `docs/ZeroWiki-API-명세-초안.md` 19절(요구사항 추적)
     - API 6.1절: FR-ING-01~09, FR-ING-19~26 기재 (FR-ING-26 포함 ✓)
     - API 15절: NFR-SEC-01~10 (13·14·15번은 ... 각각 대응)

3. **State의 오류 재확인**
   - **STATE 15행:** "미확정 27건" → 실제 28건 (UD-28 추가)
   - **STATE 26~28행(backend 요청):** 이미 처리됨 확인 (ERD 303~304행에 상태 정의)
   - **STATE 30~31행(리더 요청):** 이미 반영 확인 (API 6.1절에 FR-ING-26, API 15절에 NFR-SEC 참조)
   - **STATE 39~40행(분류):** "사용자 결정 9건 / 기술 권고 13건 / 확정 2건" → 재계산 필요

4. **UD 재계산 (정본 기준)**
   - 전체: 28건 (UD-01 ~ UD-28)
   - 확정: 4건 (UD-04, 05, 24, 27)
   - 미확정: 24건
     - 사용자 결정: 10건 (UD-01, 02, 03, 06, 07, 10, 14, 19, 20, 22)
     - 기술 권고: 10건 (UD-08, 09, 12, 13, 15, 16, 18, 21, 23, 28)
     - 벤치마크: 4건 (UD-11, 17, 25, 26)

5. **STATE.md 완전 갱신**
   - 현재 단계, 진행 중, 다음 작업, 차단 요인, 산출물 갱신
   - 부록에 28건 UD 분류표 추가 (정본 기준, 계산 검증 완료)

**산출물**

- `.claude/team/qa/STATE.md` (완전 갱신, UD 28건 분류표 포함)

**미해결**

- 이전 "즉시 요청" 2건은 이미 처리됨
- 테스트 시나리오 상세화 미착수 (3절 A/U/S를 실행 가능한 테스트로 변환)
- 평가 세트 미작성 (기획서 25절 2번)

---

## 2026-07-29 17:00 — API 19절 오독 정정 (세션 aqa-correction-70c6a2b9)

**한 일**

1. **오독 확인 (리더 지적)**
   - 원래 발견: "API 19절의 13·14·15번 참조가 `NFR-SEC-13/14/15`로 요구사항 정의서에 없음"
   - 리더 지적: 19절의 "13·14·15번"은 **API 15절 내 항목 번호**
   - 직접 확인: `docs/ZeroWiki-API-명세-초안.md` 1395~1414행 15절
     - 15절 항목 13: "사용자 간 원본 dedup 금지" → FR-SRC-05
     - 15절 항목 14: "검색 범위 제한" → FR-LIB-07, FR-ASK-07, NFR-SEC-03
     - 15절 항목 15: "공개 페이지 ID 미노출" → NFR-SEC-01
   - 결론: API 19절의 참조가 **정확하고 일치함** ✓

2. **STATE·CLAUDE·체크리스트 정정**
   - STATE: 오독 부분을 "오독 정정(2026-07-29 리더 확인)"으로 표기
   - STATE: 다음 작업 1순위를 "테스트 시나리오 상세화"로 변경 (차단 요인 제거)
   - 체크리스트 4절: 근거 표기가 이미 올바름 ("API 명세 15절 13·14·15번") — 결론만 정정

3. **교차 검증 재판정**
   - API 19절 ↔ API 15절: 완벽히 대응 ✓
   - UD-28 결정 주체 불일치: API 명세 18절(기획pm) vs 요구사항 정의서 12.2절(기술) — **리더가 정정 필요**

**산출물**

- `.claude/team/qa/STATE.md` (오독 정정 표기 + 우선순위 갱신)
- `.claude/team/qa/WORKLOG.md` (본 항목 추가)
- `.claude/team/qa/CLAUDE.md` 8절 (학습 노트 추가 예정)
- `docs/검증-QA-계약-확정-체크리스트.md` 4절 (결론 정정 예정)

**미해결**

- UD-28 결정 주체 충돌 → 리더가 API 명세 18절 정정할 때 함께 처리
- 테스트 시나리오 상세화 (이제 차단 없음, 우선 1순위)

---

## 2026-07-27 — 팀 스캐폴딩 수립 (리더)

**한 일**

- 헌법·역할 지침·상태·저널 파일 체계 생성 (리더 작업)

**산출물**

- `.claude/CONSTITUTION.md`, `.claude/team/qa/{CLAUDE,STATE,WORKLOG}.md`

**미해결**

- 없음

---

## 2026-07-29 17:30 — 오독 정정 및 미이행 3건 완료 (세션 aqa-correction-final-d7e31ca4)

**한 일**

1. **체크리스트 284행 정정**
   - 변경 전: `"리더에게: API 19절 갱신 필요 항목 (FR-ING-26, NFR-SEC-13·14·15)"`
   - 변경 후: `"backend에게: FR-ING-26 enum 구현 확인 (CANCELLED, PAUSED_QUOTA) (2026-07-29 확정: API 19절 6.1행에 이미 포함)"`
   - 근거: FR-ING-26은 API 19절 6.1행에 이미 포함, NFR-SEC-13·14·15는 존재하지 않는 ID

2. **CLAUDE.md 8절 학습 노트 명확화**
   - 변경 전: 기술적 설명 (접두사 없는 번호는...)
   - 변경 후: 리더 지적 내용 그대로 반영 ("인용된 절을 직접 열어 대응을 확인하기 전에는 요구사항 ID로 단정하지 않는다")

3. **WORKLOG 2026-07-29 15:30 항목의 "미해결" 부분 철회**
   - 원래 기록: "API 15절의 NFR-SEC-13·14·15 참조가 실질적 근거 부족 → 리더에게 보고 필요"
   - **철회 사유:** 2026-07-29 17:00 리더 대조로 이 항목이 오독임을 확인. API 19절의 "13·14·15번"은 요구사항 ID가 아니라 API 15절 내 항목 번호이며, 정확하게 대응함.
   - **근거:** `docs/ZeroWiki-API-명세-초안.md` 19절 15행 = API 15절 1395~1414행 대응 관계 확인 완료

**산출물**

- `docs/검증-QA-계약-확정-체크리스트.md` 280~284행 (문항 2 정정)
- `.claude/team/qa/CLAUDE.md` 8절 (학습 노트 명확화)
- `.claude/team/qa/WORKLOG.md` (본 항목 추가 — append-only 규칙 준수)

**미해결**

- UD-28 결정 주체 충돌 (API 명세 18절 vs 요구사항 정의서 12.2절) → 리더 정정 대기

---

## 2026-07-30 — 권고안·근거 열 복원 및 학습 노트 추가 (세션 bg-qa-restore-recovery-20d7a5c3)

**한 일**

1. **권고안·근거 열 소실 발견 및 원인 규명**
   - 커밋 6966f08에서 UD 번호 체계로 표를 재구성하면서 권고안·근거 열이 완전히 삭제됨
   - 원본 커밋 008c713 대조: API 18절 10건 + ERD 8절 10건의 권고안 데이터 영구 손실 상태 확인

2. **권고안·근거 열 복원 (git show 008c713으로 확인)**
   - **사용자 결정 (2.1절):** 10건 중 9건 원본 권고안 + 근거 복원
     - UD-01: 파일당 200MB, ZIP당 1GB, Import당 1000개 문서
     - UD-02: Access 15분, Refresh 30일
     - UD-03: **가격 모델 26절 → 21절로 정정** (리더 지시)
     - UD-06, UD-07, UD-10, UD-14, UD-19, UD-20: 각 권고안 + 근거 복구
     - **UD-07·UD-14 연동:** 400자 제약 공통 표기 (저작권 정책 일관성)
   - **기술 권고 (2.2절):** 13건 권고안 원본 대조 완료
     - UD-04~UD-05, UD-08~UD-09: API 원본 권고
     - UD-11~UD-18: ERD 원본 권고
     - UD-21, UD-23, UD-25, UD-26: 신규 항목 권고
   - **UD-22 처리:** PM 권고로 명시 (운영 정책, 요구사항 정의서 12.2절 반영)
   - **확정 항목 (2.3절):** UD-28 추가 (기술 결정, 리더 2026-07-29)

3. **CLAUDE.md 8절 학습 노트 추가 예정**
   - 표 재구성 시 원본 열 손실 리스크: "표를 다시 만들 때 원본의 모든 열을 먼저 센다. 구조 변경은 조회 → 검증 → 테스트 → 커밋 순서로 진행한다."
   - 헌법 제7조 1항(구조 변경) 적용 범위 확대

4. **STATE.md 갱신 예정**
   - 현재 단계: 체크리스트 권고안 복원 완료 → 차단 요인 해제
   - 다음 작업: 테스트 시나리오 상세화 (리더 배분 대기)

**산출물**

- `docs/검증-QA-계약-확정-체크리스트.md` 2절 전체 복원 (권고안·근거 열 22개 항목)

**미해결**

- 테스트 시나리오 상세화 (리더 배분 대기)

---

## 2026-07-30 — 사용자 확정 5건 이동 및 UD-28 모순 해소 (세션 qa-propagate-4b53a46f73c9f079)

**한 일**

1. **리더 지적 사항 정정**
   - STATE.md 부록: UD-28을 "기술 권고 필요 10건"에서 "확정됨 5건"으로 이동
   - 체크리스트 2.1절: 사용자 확정 5건(UD-01, 02, 06, 07, 14) 삭제 (2.1절은 "결정 필요" 항목만 유지)
   - 체크리스트 2.3절: 사용자 확정 5건을 확정 표에 추가 (2026-07-30 확정으로 표기)
   - 체크리스트 4절: 161행과 300행의 UD-28 결정 주체 미해결 표기 정정 (리더가 API 명세 18절을 `기술(리더)`로 이미 정정)

2. **STATE.md 최종 갱신**
   - 현재 단계: 계약 확정 및 체크리스트 정정 완료
   - 진행 중 작업: 1-4번 완료로 표기 (UD 분류, 권고안·근거 복원, 사용자 확정 이동, 모순 해소 모두 완료)
   - 다음 작업: 테스트 시나리오 상세화 (새 확정 4항목에 따른 추가 테스트 케이스 포함)
   - 부록 분류표: 확정 5건(UD-04, 05, 24, 27, 28), 미확정 24건으로 재계산 완료

**산출물**

- `docs/검증-QA-계약-확정-체크리스트.md` (2.1절 사용자 확정 5건 삭제, 2.3절 확정 표에 추가, 4절 UD-28 모순 해소)
- `.claude/team/qa/STATE.md` (부록 전환, 숫자 오류 정정)

**부록 관리 방식 변경 (2026-07-30 16:15)**

- 이유: 부록 분류표가 두 번 연속 정본(요구사항 정의서 12절)과 어긋남(UD-28 누락, 사용자 확정 5건 미반영)
- 근거: CLAUDE.md 8절 2번("옮긴 순간부터 정본과 어긋나기 시작한다"), 헌법 제7조 8항("숫자는 지침에 적지 않는다")
- 변경: 부록에서 숫자를 옮겨 적는 방식 → 정본을 가리키고 "세는 방법"만 기록
- 확정 이력: 정본 요구사항 정의서 12절과 체크리스트 2.3절 참조
- STATE.md 21~22행 숫자 오류 정정: "확정 5건, 미확정 24건"은 부록에만 의존하지 않고 매번 정본 확인
- 46행 체크리스트 설명 정정: "기술 권고 12건" → "기술 권고 9건" (미확정)

**미해결**

- 없음. 리더 지적 사항 모두 정정 완료. 부록 관리 방식 전환 완료.

---

## 2026-07-30 — 테스트 시나리오 상세화 (세션 qa-test-scenarios-4c7e9f2a5b1d3e6f)

**한 일**

1. **qa/ 디렉터리 구조 설계**
   - `qa/README.md`: 14개 자동화 테스트(A-1~A-14) + 6개 사용자 평가(U-1~U-6) + 14개 보안 테스트(S-1~S-14) 매트릭스
   - `qa/test_scenarios.md`: 모든 항목의 상세 시나리오

2. **자동화 테스트 상세화 (A-1~A-14)**
   - A-1~A-10: 기존 검증 항목 (Import 안정성, 주제 생성, 근거 추적, 모순 탐지, 에러 처리, 언어 종합, 발췌문 언어, 용어 병기, 운영 상태)
   - **A-11~A-14: 새 확정 항목(UD-01, 02, 06, 07 관련) 테스트**
     - A-11: UD-01 파일당 200MB 한도 초과 → 413
     - A-12: UD-01 ZIP당 1GB 한도 초과 → 413
     - A-13: UD-01 Import당 1000개 한도 초과 → 413
     - A-14: UD-02 Access 15분/Refresh 30일 토큰 회전 (타임머신 필요)

3. **사용자 평가 테스트 상세화 (U-1~U-6)**
   - U-1: 제안 연결 70% 유용도 (100개 샘플, 검증 사용자 투표)
   - U-2: 2주 후 재사용 50% (5~10명 추적)
   - U-3: 기존 검색 대비 70% 유용 (Likert 척도)
   - U-4: 자동 분류 정확도 70% (Lint 검사)
   - U-5: CANCELLED·PAUSED_QUOTA 상태 구별 (UI 검증)
   - U-6: 변경 승인/거절/수정/보류 4개 액션 (기능 검증)

4. **보안 테스트 상세화 (S-1~S-14)**
   - S-1~S-13: 보안 위협 모델 (프롬프트 인젝션, ZIP 탐색, SSRF, 비밀 저장, 도서관 격리, 저작권, 비용 폭탄, 토큰, 캐싱, 오류 응답, dedup 금지, 참조 범위, ID 노출)
   - S-14: 멱등성 캐시에서 서명 URL 갱신 (UD-02 관련)

5. **테스트 데이터셋 정의**
   - contradiction_pairs.json: 의도적 모순 쌍 32개
   - quality_links.json: 제안 연결 100개
   - sample_documents/: 100개 테스트 문서

**산출물**

- `qa/README.md` (구조 + 매트릭스 정의)
- `qa/test_scenarios.md` (34개 항목 상세 시나리오)
- `.claude/team/qa/STATE.md` (산출물 업데이트)

**미해결**

- 평가 데이터셋 생성 (contradiction_pairs.json, quality_links.json) — Phase 1 시작 시 준비
- 테스트 코드 구현 (Python/Go/TypeScript) — 백엔드 구현 후 진행
- CI/CD 통합 — Phase 1 완료 후

**비고**

- 모든 34개 테스트 항목의 상태: "미실행" (코드 구현 대기)
- A-11~A-14는 새 확정 5건(UD-01, 02, 06, 07, 14) 중 4개 UD와 연계
- 각 테스트는 Precondition, 단계, 예상 결과 포함
- 보안 테스트는 기획서 24절 + API 명세 15절 기준

---

## 2026-07-30 17:00 — 기술 확정 6건 + 보류 3건 체크리스트 반영 (세션 qa-tech-confirm-8f2e3c5a6b1d4e7f)

**한 일**

1. **기술 팀 확정 6건 (리더 기술 판단) — 요구사항 정의서 12절 v1.0 기준**
   - 체크리스트 2.2절 → 2.3절로 이동 (확정 항목)
   - UD-08: slug 충돌 시 숫자 suffix, 변경 불가(내리고 새로 발행)
   - UD-09: Admin 태그로 구분, Phase 1에 관리자 API 없음
   - UD-13: 이벤트 재생, Phase 0에서 1초 기준 측정, 1초 초과 시 Phase 2 스냅샷
   - UD-15: jsonb 저장, Schema는 Phase 0 프로토타입에서 고정
   - UD-16: Phase 1 고정 7종 CHECK, Phase 2 테이블 분리
   - UD-18: content_hash + notion_id, **계정 내부 한정** (다른 사용자와 dedup 안 함)

2. **기술 팀 권고 3건 보류 표기 (backend 권고안 재작성 중)**
   - UD-12: "🔄 리더 보류" (Claim 복제 서술·재사용 동의 모순)
   - UD-21: "🔄 리더 보류" (부하 분산 누락)
   - UD-23: "🔄 리더 보류" (보존 기간 근거 부재)

3. **테스트 시나리오 신규 항목 정의 (기술 확정 6건 연계)**
   - **A-13: UD-13 재생 시간 측정** (Phase 0 벤치마크)
     - 도서관 버전 과거 조회 시 재생 시간 측정
     - 목표: 1초 이내 (1초 초과 시 Phase 2 스냅샷 전환 필요)
   - **A-14(수정): UD-08 동시 요청 경쟁 조건**
     - 두 개 동시 발행 요청에서 slug suffix 충돌 검증
     - 한쪽이 UNIQUE 제약 위반 에러 수신 후 재시도 성공
   - **S-15: UD-18 계정 경계 검증** (보안 강화)
     - 다른 사용자가 같은 Notion 문서를 가져왔을 때 중복으로 묶이지 않음
     - 사용자 간 데이터 노출 방지 (기획서 6.1, NFR-SEC-02)

**산출물**

- `docs/검증-QA-계약-확정-체크리스트.md` (2.2절 보류 3건 + 2.3절 확정 9건)
- `qa/test_scenarios.md` (신규 3개 항목: A-15, A-16, S-15)
- `.claude/team/qa/STATE.md` (갱신)

**미해결**

- 없음. 모든 항목 완료.

**구체 변경 내용:**

1. **2.2절 기술 권고 (7개 → 4개)**
   - 삭제된 확정 6개: UD-08, 09, 13, 15, 16, 18
   - 보류 3개 표기: UD-12, 21, 23 (🔄 리더 보류)
   - 유지 4개: UD-11, 17, 25, 26

2. **2.3절 기술 결정 (3개 → 9개)**
   - 기존: UD-04, 05, 28
   - 신규: UD-08, 09, 13, 15, 16, 18
   - 번호 순서: UD-04, 05, 08, 09, 13, 15, 16, 18, 28

3. **테스트 시나리오 신규 항목**
   - **A-15: UD-13 재생 시간 측정** (1초 기준, Phase 0 벤치마크)
   - **A-16: UD-08 동시 요청 경쟁 조건** (slug suffix 충돌, 재시도)
   - **S-15: UD-18 계정 경계 검증** (사용자 간 중복 판정 금지, 보안)

---

## 2026-07-30 17:30 — 최종 동기화: UD-12/21/23 확정 + UD-29 신설 (세션 qa-final-sync-2d4f8e3c6a1b5f9)

**한 일**

1. **추가 확정 3건 + 신규 1건 (개정 이력 v1.1)**
   - **UD-12 확정 (2026-07-30):** Claim 재사용 구조
     - `claims.page_version_id` 제거, `claim_statuses` 신설, `evidences.claim_id` 유지
     - 근거 추적이 버전을 초월하도록 설계
   - **UD-21 확정 (2026-07-30):** 월간 Lint 가입일 기반 실행
     - 가입일의 일자에 실행 (계정별 분산)
     - 가입일이 그 달에 없으면 말일로 당김 (1월 31일 가입자 → 2월 28/29일)
   - **UD-23 확정 (2026-07-30):** 알림 보존 기간 차별화
     - 위험 변경·삭제 유예 30일
     - 포크·Lint·편집 제안 14일
     - 진행 알림 7일
     - **삭제 유예 알림은 유예 기간 끝까지 보존** (사용자의 마지막 되살리기 기회)
   - **UD-29 신설 (벤치마크):** 버전 간 Claim 동일성 판정
     - UD-12 Claim 재사용에서 분리
     - blake3 해시 대신 의미 동일성 벤치마크 방식으로 확정 필요

2. **체크리스트 제목·항목 최종 구조**
   - 2.2절 제목 변경: "기술 팀 권고" → **"벤치마크로 결정"** (v1.1)
   - 2.2절 항목 정리:
     - 삭제 3건: UD-12, 21, 23 (→ 2.3절 기술 결정)
     - 유지 4건: UD-11, 17, 25, 26 (모두 벤치마크)
     - 추가 1건: **UD-29** (버전 간 Claim 동일성)
   - 2.3절 기술 결정: 9개 → **13개**
     - 기존: UD-04, 05, 28
     - 신규: UD-08, 09, 13, 15, 16, 18, 12, 21, 23
     - UD 번호 순서: 04, 05, 08, 09, 12, 13, 15, 16, 18, 21, 23, 28

3. **미확정 현황 최종 정리**
   - **사용자 결정 필요: 5건** (UD-03, 10, 19, 20, 22)
   - **벤치마크로 결정: 5건** (UD-11, 17, 25, 26, 29)
   - **기술 권고: 0건** (모두 확정 또는 벤치마크 재분류)
   - **합계 UD: 28건** = 사용자 5 + 확정 13 + 벤치마크 5 + 변경된 기술 5 (**검증 필요**)

**산출물**

- `docs/검증-QA-계약-확정-체크리스트.md` (2.2절 제목 + 구조, 2.3절 확정 3건 추가, UD-29 벤치마크 추가)
- `.claude/team/qa/STATE.md` (갱신)

**다음 단계 (테스트 작업)**

리더 지시에 따른 신규 테스트 시나리오 3개:
- **A-17: UD-12 Claim 재사용 정합성**
  - 페이지 버전 올라가도 `evidences.claim_id`가 끊기지 않는지
  - 근거 추적(기획서 6.4, PR-4) 버전 초월 유지 확인
  - **`claims`에서 FK 제거 구조라 이 검증이 제품 핵심 약속**
- **A-18: UD-21 월간 Lint 말일 보정**
  - 1월 31일 가입자의 2월 실행일이 28일(윤년 29일)인지
  - 경계 테스트로 적합
- **S-16: UD-23 알림 보존 만료 검증**
  - 특히 삭제 유예 알림이 유예 종료 전에 사라지지 않는지
  - **사용자 계정 되살리기 마지막 기회에 걸린 항목** (S 계열 보안 중요도)

**학습 노트**

리더 지적: backend 권고 `claim_key = blake3(intent_hash(statement))` 기각
- 해시는 근사 유사도를 표현할 수 없음 (완벽한 의미 정규화 전제 불가능)
- **권고안 통합 시 "이 방법이 실제로 계산 가능한가"를 한 번 더 물어야 함**
- QA의 역할: 근거 문서 검증 + 실현 가능성 검토

---

## 2026-08-10 22:04 — Phase 0 평가 세트 독립 QA 인수 검증

**한 일**

1. `evaluation/pages-inventory.csv`, `relation-labels.csv`,
   `contradiction-labels.csv`를 backend와 독립적으로 검사했다.
   - 정확한 헤더, UTF-8/LF, 필수값, ID/slug 참조, 유일성, self pair·중복 부재
   - UD-16 관계 7종, positive/negative 규칙, 모순 5종+`NO_CONFLICT`
   - 채점 분모 실측 및 타입별 분포
2. `evaluation/README.md`의 AC-3·AC-5 채점식을 기획서 3절, QA 체크리스트
   3절, 요구사항 정의서 AC-3·AC-5 및 QA 시나리오 A-4·U-1과 교차 대조했다.
3. CSV 구조는 PASS했으나 채점 계약은 인수 거절했다.
   - AC-3 사용자 평가를 positive gold recall로 대체하고 “정확도”로 오표기
   - 모순 오탐율의 FPR/FDR 분모 혼동
   - contradiction output↔gold stable 매칭 계약 부재
   - 실제 positive 23건·5종과 A-4의 32건/25건·`DIRECT_CONTRADICTION` 불일치
4. 실제 `qa/test_scenarios.md`를 검색해 A 18개, U 6개, S 16개, 총 40개임을
   재계산하고 STATE를 갱신했다.

**산출물**

- `qa/verify_phase0_evaluation.py`
- `qa/phase0-evaluation-acceptance-report.md`
- `qa/README.md`
- `.claude/team/qa/STATE.md`
- `.claude/team/qa/WORKLOG.md` (이 항목 append)

**검증**

- `$env:PYTHONIOENCODING='utf-8'; python qa/verify_phase0_evaluation.py`
  - 종료 코드 0
  - `COUNTS pages=180 ... relations=84 positive=76 negative=8 contradictions=26 conflict=23 no_conflict=3`
  - `RESULT PASS failures=0`
- `rg -c "^### A-[0-9]+:" qa/test_scenarios.md` → 18
- `rg -c "^### U-[0-9]+:" qa/test_scenarios.md` → 6
- `rg -c "^### S-[0-9]+:" qa/test_scenarios.md` → 16

**삭제·대체 기록**

- `qa/README.md`의 과거 34개 매트릭스(A-1~A-14, U-1~U-6, S-1~S-14)와
  존재하지 않는 `qa/evaluation_data/` 구조 설명을 제거했다.
- 이유: 실제 정본 산출물은 `qa/test_scenarios.md`의 40개 시나리오와
  `evaluation/**`의 CSV이며, 낡은 복제 목록이 현재 상태를 오도했다.
- 어디로: 시나리오 상세는 기존 `qa/test_scenarios.md`에 그대로 보존되어 있고,
  새 README는 그 파일과 이번 검증기·인수 보고서를 가리킨다. 판단·근거는
  `qa/phase0-evaluation-acceptance-report.md`에 보존했다.

**미해결**

- backend 소유 `evaluation/README.md`와 평가 manifest/CSV의 채점 계약 정정 필요.
- 리더 소유 `docs/ZeroWiki-요구사항-정의서.md` 11절 AC-5의 QA 항목 A-5 참조를
  실제 A-4와 동기화할 필요가 있다.
- 라벨 의미 정확성의 독립 이중 검토와 외부 NoteWiki 원본 대조는 미실행이다.

**동시 변경 재검증 정정 (2026-08-10 22:06)**

- backend가 검증 중 `evaluation/README.md`의 데이터셋 실측을 source 37 + concept
  127 + flow 16 = 180으로 갱신했다. 최초 발견한 concept 124 표기 불일치는 해소됐다.
- QA 인수 보고서와 STATE에서 해소된 finding을 제거했다. AC-3 식, 모순 오탐율 식,
  contradiction 매칭 계약, `*_page_id`/slug 문제는 최종 재검증 시 그대로 남아 있다.

---

## 2026-08-10 22:21 — backend 반영 후 Phase 0 평가 세트 재인수

**한 일**

1. backend 변경을 독립 재검증했다.
   - relation 헤더 `source_page_slug,target_page_slug`와 inventory exact join
   - contradiction `case_id` 26개, `CT-001`~`CT-026` 형식·유일성·연속성
   - UD-16 7종, negative 공란 규칙, 모순 5종+`NO_CONFLICT`, 중복·분모
2. README의 채점 계약을 정본 AC-3·AC-5와 교차 대조했다.
   - relation pair/strict recall, sampled FPR, bounded precision과 0분모 N/A 확인
   - AC-3이 U-1 사용자 평가로 별도 유지됨을 확인
   - contradiction recall, sampled FPR, FDR, case_id/exact-pair 규칙 확인
3. QA A-4를 실제 평가 세트에 맞췄다.
   - conflict 23건 중 최소 19건
   - 확정 5종 enum
   - `NO_CONFLICT` 3건은 sampled FPR 분모
   - mapping manifest를 Ingest-level 실행 precondition으로 명시
4. QA 체크리스트 3.1절 A-4도 같은 23/19 및 manifest 조건으로 동기화했다.
5. 이전 findings를 재판정했다. offline 채점 계약 문제는 해소됐고, Ingest-level
   page/claim→생성 `claim_id` mapping manifest 부재만 HIGH blocker로 남겼다.

**산출물**

- `qa/verify_phase0_evaluation.py`
- `qa/phase0-evaluation-acceptance-report.md`
- `qa/test_scenarios.md`
- `qa/README.md`
- `docs/검증-QA-계약-확정-체크리스트.md`
- `.claude/team/qa/STATE.md`
- `.claude/team/qa/WORKLOG.md` (이 항목 append)

**검증**

- `$env:PYTHONIOENCODING='utf-8'; python qa/verify_phase0_evaluation.py`
  - 종료 코드 0, `RESULT PASS failures=0`
  - pages 180, relations 84(76/8), contradictions 26(23/3)
- `python evaluation/validate_evaluation.py`
  - 종료 코드 0, `검증 완료: 오류 0건`

**삭제·대체 기록**

- `qa/test_scenarios.md` A-4의 가상 `evaluation_data/contradiction_pairs.json`,
  32건/25건, `DIRECT_CONTRADICTION` 판단을 실제 `evaluation/**`의 23건/19건,
  확정 5종, mapping manifest precondition으로 대체했다.
- 같은 문서 하단의 존재하지 않는 `evaluation_data/` JSON 구조 설명을 실제
  `evaluation/*.csv` 포인터와 미작성 manifest 설명으로 대체했다.
- `docs/검증-QA-계약-확정-체크리스트.md` 3.1절 A-4의 32건/25건 기준을 실제
  23건/19건과 manifest 차단 조건으로 대체했다.
- `qa/phase0-evaluation-acceptance-report.md`의 이전 인수 거절 findings 본문을
  재인수 보고서로 대체했다. 판단은 삭제하지 않고 새 보고서 3절의 재판정 표와
  2026-08-10 22:04 WORKLOG 항목에 보존했다.

**미해결**

- Ingest-level AC-5용 mapping manifest 미작성.
- AC-3 U-1 사용자 평가 미실행.
- 라벨 의미 정확성 독립 이중 검토와 외부 NoteWiki 원본 대조 미실행.

**최종 잔존 검색**

- 활성 schema·시나리오·계약 파일에서 옛 CSV 헤더, `32개 의도적 모순`,
  `25개 이상 자동 탐지`, `DIRECT_CONTRADICTION`, AC-5→A-5 표 참조를 검색했다.
- 결과: `ACTIVE_STALE_REFERENCES=0` (종료 코드 0으로 명시 처리).
- append-only WORKLOG와 재인수 보고서의 재판정 표에 남은 과거 문구는 판단 이력으로
  의도적으로 보존했다.

---

## 2026-08-11 04:00 — Phase 0 실제 산출물 독립 인수 및 품질 gate

**한 일**

1. Windows checkout에서 기존 `b"\r" not in raw` 검사가 CRLF를 거짓 실패시키던
   원인을 최소 수정했다. `valid_newlines()`는 LF/CRLF를 모두 허용하고 mixed/bare CR은
   거부한다. BOM·UTF-8·헤더·열 수·CSV enum/참조/유일성 검사는 유지했다.
2. `qa/verify_phase0_benchmark.py`를 backend 실제 contract에 맞췄다.
   - schema v1 manifest의 `run_id`/cases/gold/generated IDs와 manifest hash 검증
   - `phase0-generated-claims.csv` 전체 26 case join 및 contradiction output join
   - per-document benchmark 100행·완료율·유한 metric 검증
   - recall만으로 통과하지 않도록 분류 정확도 최소 0.80 gate 추가
   - 산출물 경로는 CLI 인자로 받아 도착 후 재실행 가능
   - self-check에 LF/CRLF 회귀와 recall-only false PASS 방지 추가
3. 실제 결과를 독립 실행했다. 구조·mapping·100/100 완료는 통과했으나
   `recall=1.000`, `sampled_fpr=1.000`, `classification_accuracy=0.192`여서
   `RESULT FAIL`, exit 1을 확인했다.
4. metrics JSON을 교차 확인했다. Markdown roundtrip 100개/불일치 0, event replay
   1000 change sets·25회·p95 약 0.000070초·snapshot 미필요는 PASS했다.
5. constitution schema 구조(12 properties/12 required), backend path traversal·ZIP
   member·secret redaction을 독립 확인했다. `../`와 backslash traversal/redaction은
   PASS지만 Windows root-relative `/absolute.md`는 backend validator가 허용해 FAIL이다.
6. QA report, README, QA checklist, STATE를 실제 판정으로 갱신했다. `evaluation/**`,
   backend 소유 파일은 수정하지 않았다.

**검증 명령과 실제 결과**

- `python qa/verify_phase0_evaluation.py` → `RESULT PASS failures=0`
- `python evaluation/validate_evaluation.py` → 기존 구조 validator 오류 0건
- `python qa/verify_phase0_benchmark.py --self-check` → `SELF-CHECK PASS ...`
- `python qa/verify_phase0_benchmark.py --manifest evaluation/results/phase0-manifest.json --generated-claims evaluation/results/phase0-generated-claims.csv --benchmark-output evaluation/results/phase0-benchmark.csv --expected-documents 100 --contradiction-output evaluation/results/phase0-contradictions.csv` → `BENCHMARK_METRICS ... completion_rate=1.000`, `AC5_METRICS ... recall=1.000 sampled_fpr=1.0 classification_accuracy=0.192`, `RESULT FAIL failures=1`, exit 1
- `python backend/scripts/phase0_benchmark.py --source-root C:\Users\halma\Desktop\NoteWiki --roundtrip --allow-missing` → 163 files roundtrip PASS, inventory 180 중 17 누락

**미해결·보고**

- HIGH: contradiction 분류 품질 FAIL. 모델/분류 전략 개선 후 동일 validator 재실행 필요.
- HIGH: backend `relative_path('/absolute.md')` root-relative 허용. backend에 정확한 재현과
  path anchor 거부 수정 필요. QA는 소유권 밖 파일을 편집하지 않았다.
- MEDIUM: 전체 NoteWiki inventory 17개 원본 누락. 100문서 실행 범위와 혼용 금지.
- local Ollama 비용 0은 실제 provider 비용이 아니며, API citation·U-1·S-1~S-16은
  미실행이다.
- 커밋·git 조작 없음.

---

## 2026-08-11 — HIGH path finding 리더 시정 재검증

- 리더가 backend `relative_path()`에서 Windows `drive`·`root`를 즉시 거부하도록 수정했다.
- self-check에 `../escape.md`, `/absolute.md`, `C:\absolute.md` 회귀를 추가했다.
- `python backend/scripts/phase0_benchmark.py --self-check`를 재실행해 PASS를 확인했다.
- 경로 보안 HIGH는 해소됐다. 남은 product gate blocker는 모순 classification accuracy 0.192다.

---

## 2026-08-11 04:23 — path guard 후속 QA 독립 재검증

**한 일**

- 헌법 → `.claude/team/qa/CLAUDE.md` → `STATE.md` 순으로 재독했다.
- backend가 추가한 `candidate.drive`·`candidate.root` guard와 path self-check를 읽고,
  QA report의 과거 HIGH-2는 발견 당시 사실로 보존하면서 현재 판정을 해결 완료(PASS)로
  갱신했다.
- `STATE.md` timestamp를 갱신했다. product blocker는 contradiction accuracy 하나로
  남겼다.

**검증 명령과 실제 결과**

- `python backend/scripts/phase0_benchmark.py --self-check` → `self-check: PASS`
- 독립 path/redaction probe → `relative_path`가 `../escape.md`,
  `wiki/../escape.md`, `/absolute.md`, `C:\\absolute.md`, 빈 경로를 모두 거부;
  `safe_zip_member` traversal과 `redact_secrets`도 PASS; exit 0
- `python qa/verify_phase0_evaluation.py` → `RESULT PASS failures=0`
- `python evaluation/validate_evaluation.py` → `검증 완료: 오류 0건`

**현재 판정**

- path security: **PASS / finding resolved**
- contradiction product gate: **FAIL** (`accuracy=0.192`, `sampled_fpr=1.000`)
- Phase 1 quality entry: **BLOCKED** until contradiction output is improved and rerun.

---

## 2026-08-11 04:32 — report 상단 mapping manifest 상태 정정

**한 일**

- 실제 schema v1 manifest·generated claims 도착 사실을 반영해
  `qa/phase0-evaluation-acceptance-report.md` 상단 판정을 갱신했다.
- 과거 HIGH-1 mapping manifest 부재 finding과 당시 재현·필요 변경은 보존하고,
  현재는 manifest hash/gold/generated 전체 join PASS, contradiction 품질 gate FAIL로
  분리했다.

**검증**

- `python qa/verify_phase0_benchmark.py --self-check` → PASS
- `python qa/verify_phase0_evaluation.py` → `RESULT PASS failures=0`
- 실제 benchmark validator → mapping PASS, `accuracy=0.192`, `sampled_fpr=1.000`,
  `RESULT FAIL`, exit 1
- `git diff --check`는 최종 종료 전에 실행한다.

**미해결**

- contradiction 분류 품질 개선 및 동일 산출물 재실행 필요.

---

## 2026-08-11 09:12 — GPT model-specific benchmark 독립 QA 재검증

**한 일**

1. backend 산출물 `evaluation/results/phase0-gpt-5_6-sol-r1/`를 읽기 전용으로
   확인하고 기존 `qa/verify_phase0_benchmark.py`를 변경 없이 실행했다.
2. manifest schema/hash·gold 26 case·generated claim mapping 26행,
   benchmark 26행 `COMPLETED`를 확인했다.
3. contradiction output 18행의 generated pair join과 5종 label enum은 모두
   유효했지만 `CT-011, CT-012, CT-014, CT-015, CT-021, CT-024, CT-025, CT-026`
   8개 case가 누락된 사실을 별도 계산으로 확인했다.
4. qwen baseline과 비교한 결과 GPT는 sampled FPR과 분류 정확도는 개선했지만
   recall·accuracy gate를 통과하지 못했다. `openai-calls.csv`의 key·응답 본문은
   읽거나 기록하지 않았다.

**산출물**

- `qa/phase0-evaluation-acceptance-report.md` (GPT run 독립 인수 섹션·findings·비교표)
- `.claude/team/qa/STATE.md` (GPT 최신 판정·차단 요인·재현 명령)
- `.claude/team/qa/WORKLOG.md` (본 항목 append-only)

**검증 명령과 실제 결과**

- `python qa/verify_phase0_evaluation.py` → `RESULT PASS failures=0`, exit 0
- `python evaluation/validate_evaluation.py` → `검증 완료: 오류 0건`, exit 0
- `python qa/verify_phase0_benchmark.py --self-check` → `SELF-CHECK PASS ...`, exit 0
- `python qa/verify_phase0_benchmark.py --manifest evaluation/results/phase0-gpt-5_6-sol-r1/phase0-manifest.json --generated-claims evaluation/results/phase0-gpt-5_6-sol-r1/phase0-generated-claims.csv --benchmark-output evaluation/results/phase0-gpt-5_6-sol-r1/phase0-benchmark.csv --expected-documents 26 --contradiction-output evaluation/results/phase0-gpt-5_6-sol-r1/phase0-contradictions.csv` → `BENCHMARK_METRICS expected=26 completed=26 completion_rate=1.000`, `AC5_METRICS conflict=23 detected=18 recall=0.783 sampled_fpr=0.0 classification_accuracy=0.615`, `RESULT FAIL failures=2`, exit 1
- qwen baseline 동일 validator → `recall=1.000 sampled_fpr=1.0 classification_accuracy=0.192`, `RESULT FAIL failures=1`, exit 1

**미해결**

- GPT output 8개 누락 case를 해소하고 동일 26-case validator에서 recall·accuracy
  0.80 이상을 재검증해야 한다. Phase 1 품질 진입은 계속 BLOCKED다.

---

## 2026-08-11 13:08 — dense predictions validator 계약 확장 및 회귀 검증

**한 일**

1. `qa/verify_phase0_benchmark.py`에 기존 sparse 경로를 유지하는 opt-in
   `--dense-predictions` CSV 검증을 추가했다. 모든 gold case의 정확히 한 행,
   단일 `run_id`, `COMPLETED`/`FAILED` status, 6-class enum, confidence/token/
   cost/latency 범위와 실패 행의 blank 필드를 검사한다. FAILED와 누락은
   `NO_CONFLICT`로 대체하지 않는다.
2. dense metrics에 completion, conflict recall, sampled FPR, 6-class accuracy,
   detected-conflict subtype accuracy, joint subtype accuracy, 6x6 confusion,
   macro-F1, Wilson 95% FPR upper를 추가하고 completion 100%/recall·accuracy
   >= 0.80/FPR <= 0.10/Wilson upper <= 0.20 gate를 적용했다.
3. `--self-check`에 누락·FAILED 오인 방지, FPR hard gate, confusion/macro-F1/
   Wilson 검사를 추가했다. 체크리스트와 STATE에 dense 계약과 후속 holdout 작업을
   기록했다. 실제 API 호출·holdout label 생성은 하지 않았다.

**검증 명령과 실제 결과**

- `python -m py_compile qa/verify_phase0_benchmark.py` → exit 0
- `python qa/verify_phase0_benchmark.py --self-check` → `SELF-CHECK PASS ...`,
  synthetic dense `expected=25 completed=25 failed=0 completion_rate=1.000
  recall=1.000 sampled_fpr=0.000 wilson95_fpr_upper=0.161 accuracy=1.000
  macro_f1=1.000`, exit 0
- qwen sparse baseline (`--expected-documents 100`) →
  `BENCHMARK_METRICS expected=100 completed=100 completion_rate=1.000`,
  `AC5_METRICS conflict=23 detected=23 recall=1.000 sampled_fpr=1.0
  classification_accuracy=0.192`, `RESULT FAIL failures=1`, exit 1
- GPT sparse run (`--expected-documents 26`) →
  `BENCHMARK_METRICS expected=26 completed=26 completion_rate=1.000`,
  `AC5_METRICS conflict=23 detected=18 recall=0.783 sampled_fpr=0.0
  classification_accuracy=0.615`, `RESULT FAIL failures=2`, exit 1

**미해결/인계**

- backend가 label-free 26-case holdout dense CSV를 생성하면 새 CLI로 실제
  gate를 검증해야 한다. `evaluation/README.md`에는 dense header/metric/gate
  설명이 아직 없으므로 backend/리더가 해당 문서를 계약 정본에 맞게 보강해야 한다.

---

## 2026-08-11 13:25 — backend dense header 및 holdout gold 호환 보정

**한 일**

- backend `openai-predictions.csv` 정본 header에 맞춰 QA dense validator와
  self-check synthetic rows를 일치시켰다: `prompt_variant`, `prediction`,
  expected/actual token·cost·latency 필드를 검증한다.
- FAILED self-check는 `prediction`과 actual accounting 필드를 공란으로 두고,
  confusion에 `NO_CONFLICT`를 암묵 삽입하지 않는지 확인한다. FPR test는
  `prediction`을 변조한다.
- 기존 `--labels` full gold header를 유지하면서 `--gold-labels`/`--holdout-labels`
  선택 입력으로 `case_id,classification,판단_이유` 3열 holdout gold를 dense
  검증에 사용할 수 있게 했다. backend/evaluation 파일은 편집하지 않았다.

**검증 명령과 실제 결과**

- `python qa/verify_phase0_benchmark.py --self-check` → `SELF-CHECK PASS ...`,
  dense metrics `completion_rate=1.000 recall=1.000 sampled_fpr=0.000
  wilson95_fpr_upper=0.161 accuracy=1.000 macro_f1=1.000`, exit 0
- qwen sparse validator → `recall=1.000 sampled_fpr=1.0
  classification_accuracy=0.192`, `RESULT FAIL failures=1`, exit 1
- GPT sparse validator → `recall=0.783 sampled_fpr=0.0
  classification_accuracy=0.615`, `RESULT FAIL failures=2`, exit 1
- `python -m py_compile qa/verify_phase0_benchmark.py` → exit 0
- `git -c safe.directory=C:/Users/PC/Desktop/ZeroWiki-SaaS diff --check` → exit 0

**미해결/인계**

- 실제 dense predictions와 holdout gold가 도착하면 `--dense-predictions`와
  `--gold-labels`로 gate를 실행한다. 26-case의 negative 3건 때문에 Wilson
  upper gate는 표본 확대 또는 명시적 계약 결정 없이는 통과할 수 없다.

---

## 2026-08-11 13:25 — 최종 회귀 확인

- `python qa/verify_phase0_benchmark.py --self-check` → dense metrics 출력 및
  `SELF-CHECK PASS`, exit 0
- qwen sparse → `recall=1.000 sampled_fpr=1.0 classification_accuracy=0.192`,
  `RESULT FAIL failures=1`, exit 1
- GPT sparse → `recall=0.783 sampled_fpr=0.0 classification_accuracy=0.615`,
  `RESULT FAIL failures=2`, exit 1
- `python -m py_compile qa/verify_phase0_benchmark.py` 및
  `git -c safe.directory=C:/Users/PC/Desktop/ZeroWiki-SaaS diff --check` → exit 0
- `--gold-labels`/`--holdout-labels` 3열 gold 선택 입력과 backend dense header를
  반영했으며, 실제 holdout/API 호출은 하지 않았다.

## 2026-08-11 후속 holdout 독립 라벨링 및 dense gate profile

- 작업: `evaluation/contradiction-holdout-input.csv` 70건의 claim/context를 QA가 독립 판정하고 `qa/contradiction-holdout-review.csv`를 생성했다. backend/evaluation 파일은 편집하지 않았다.
- 근거: `C:/Users/PC/Desktop/ZeroWiki/NoteWiki`의 source reference 140개(고유 57개)를 실제로 열어 읽었고, 존재·비어 있지 않음을 확인했다. artifact의 copied fields는 입력과 70/70 일치한다.
- 라벨 분포: `TRUE_CONFLICT=34`, `TIME_CHANGE=4`, `CONDITION_DIFF=2`, `VIEWPOINT_DIFF=0`, `POSSIBLE_CONFLICT=1`, `NO_CONFLICT=29`; NO_CONFLICT negative_reason은 `DIFFERENT_SUBJECT=4`, `COMPLEMENTARY=22`, `SAME_PROPOSITION=3`이다. 목표 분포는 맞추지 않았고 HT-065 판단 이유에 `REVIEW_REQUIRED`를 기록했다.
- 계약: `user_decision` 70건 공란, 6종 enum 유효, NO_CONFLICT 외 negative_reason 공란, source 누락 없음. gold 파일 생성/API 호출은 하지 않았다.
- `qa/verify_phase0_benchmark.py`는 dev `--labels`에서 Wilson upper를 보고만 하고 holdout `--holdout-labels/--gold-labels`에서만 Wilson <= 0.20을 hard gate로 적용하도록 최소 수정했다. `python qa/verify_phase0_benchmark.py --self-check` exit 0; dev profile 3 negative의 Wilson upper 0.561은 출력만, holdout profile 20 negative의 0 false positive upper 0.161은 gate 통과, 1 false positive upper 0.236은 gate 실패를 확인했다.

## 2026-08-11 rubric dense 최종 재검증

- API 재호출 없이 다음 두 prediction CSV를 기존 validator로 실행했다: `evaluation/results/phase0-gpt-5_6-sol-rubric-v2-r2/openai-predictions.csv`, `evaluation/results/phase0-gpt-5_6-sol-rubric-context-v2-r1/openai-predictions.csv`.
- rubric-v2 r2 실제 출력: `completion=26/26`, `recall=0.739`, `sampled_fpr=0.000`, `accuracy=0.615`, `detected_conflict_subtype_accuracy=0.765`, `joint_subtype_accuracy=0.565`, `macro_f1=0.553`, `wilson95_fpr_upper=0.561`, `RESULT FAIL failures=2`, exit 1.
- rubric-context-v2 r1 실제 출력: `completion=26/26`, `recall=0.783`, `sampled_fpr=0.000`, `accuracy=0.692`, `detected_conflict_subtype_accuracy=0.833`, `joint_subtype_accuracy=0.652`, `macro_f1=0.683`, `wilson95_fpr_upper=0.561`, `RESULT FAIL failures=2`, exit 1.
- self-check exit 0, 두 dense confusion matrix 출력 확인, `python -m py_compile qa/verify_phase0_benchmark.py` exit 0, `git diff --check` exit 0. 두 dev gate가 모두 FAIL이므로 holdout API 실행과 gold freeze는 차단 상태로 기록했다.

---

## 2026-08-11 rubric-context-v3 최종 독립 QA

**범위**

- API 재호출 없이 `evaluation/results/phase0-gpt-5_6-sol-rubric-context-v3-r1/`의
  manifest, generated claims, benchmark CSV, metrics, dense prediction을 직접 읽었다.
- 오분류를 gold `evaluation/contradiction-labels.csv`와 case_id로 join해 확인했다.
  `CT-017`: `VIEWPOINT_DIFF` → `POSSIBLE_CONFLICT`, `CT-021`:
  `POSSIBLE_CONFLICT` → `NO_CONFLICT`.

**검증 결과**

- v3 dense: 26/26 completed, recall `0.957` (22/23), sampled FPR `0.000` (0/3),
  6-class accuracy `0.923` (24/26), detected subtype `0.955` (21/22),
  joint subtype `0.913` (21/23), macro-F1 `0.902`, Wilson upper `0.561`.
- confusion matrix: TRUE `5,0,0,0,0,0`; TIME `0,5,0,0,0,0`;
  CONDITION `0,0,5,0,0,0`; VIEW `0,0,0,4,1,0`;
  POSSIBLE `0,0,0,0,2,1`; NO `0,0,0,0,0,3`.
- dev gate는 completion/recall/accuracy/FPR 모두 PASS이고 dense validator exit 0이다.
  Wilson은 3개 negative 표본에서 report-only다.

**재현 명령과 실제 종료 코드**

- `python qa/verify_phase0_benchmark.py --manifest evaluation/results/phase0-gpt-5_6-sol-rubric-context-v3-r1/phase0-manifest.json --generated-claims evaluation/results/phase0-gpt-5_6-sol-rubric-context-v3-r1/phase0-generated-claims.csv --benchmark-output evaluation/results/phase0-gpt-5_6-sol-rubric-context-v3-r1/phase0-benchmark.csv --dense-predictions evaluation/results/phase0-gpt-5_6-sol-rubric-context-v3-r1/openai-predictions.csv --labels evaluation/contradiction-labels.csv --expected-documents 26` → `RESULT PASS failures=0`, exit 0.
- `python qa/verify_phase0_benchmark.py --self-check` → `SELF-CHECK PASS`, exit 0.
- `python backend/scripts/phase0_benchmark.py --self-check` → `self-check: PASS`, exit 0.
- `python evaluation/validate_evaluation.py` → `검증 완료: 오류 0건`, exit 0.

**차단/인계**

- `qa/contradiction-holdout-review.csv`의 목표 분포 불일치와 HT-065
  `REVIEW_REQUIRED` 때문에 사용자 승인·gold freeze 전 holdout API 실행은 하지 않았다.
- holdout 검증 전 production 승격은 하지 않는다. 기존 qwen/GPT 및 rubric-v2/
  rubric-context-v2 결과 기록은 보존했다.

---

## 2026-08-11 STATE/보고서 현재 스냅샷 정합성 수정

- `.claude/team/qa/STATE.md` 상단의 현재 단계·게이트 표·진행 작업·다음 작업·차단
  요인을 최신 v3 결과에 맞췄다. rubric-context-v3 dev는 PASS이며, holdout review,
  HT-065 adjudication, frozen gold와 holdout validator를 거쳐야 production 승격할
  수 있으므로 holdout/production은 BLOCKED로 유지했다.
- qwen baseline, GPT sparse, rubric-v2/context-v2 dense의 FAIL은 `이전 결과`로
  명시해 보존하고 현재 v3 판정과 구분했다.
- `qa/phase0-evaluation-acceptance-report.md` 첫 판정을 v3 dev 품질 PASS /
  holdout·production BLOCKED로 갱신했다. 과거 실행 섹션의 FAIL 기록은 삭제하지 않았다.
- stale current 문구를 `rg`로 점검하고, QA 소유 파일만 수정했다.

## 2026-08-11 70-case 한국어 review 초안 후속

- backend 2차 `evaluation/contradiction-holdout-input.csv`를 정본으로 읽고 HT-021, HT-023, HT-025, HT-027, HT-037, HT-038, HT-043, HT-049, HT-067, HT-069의 claim/context/subject/time을 review에 exact 동기화했다.
- 독립 원문 판정은 TIME 4건, CONDITION 3건(HT-037/038/049), VIEWPOINT 1건(HT-043), POSSIBLE 1건(HT-067), TRUE 1건(HT-069)이다. HT-049는 명시 업무 조건 차이로 CONDITION_DIFF, HT-069는 Reverse Proxy 직접 노출과 뒤쪽 전달의 직접 모순으로 TRUE_CONFLICT를 적용했다.
- review 구조 검증 결과 70/70 unique, parity 0, source_ref 140개·고유 60개·missing 0, NFKC 내부/CT pair 중복 0, 한국어 판단 이유 70건, 검토 플래그 0, user_decision 비공란 0으로 exit 0이다.
- 실제 분포는 TRUE 9 / TIME 8 / CONDITION 9 / VIEWPOINT 7 / POSSIBLE 8 / NO 29로 목표 8 / 8 / 8 / 8 / 8 / 30과 불일치한다. 목표에 맞춘 강제 재분류는 하지 않았고 사용자 승인·adjudication 대기 상태로 기록했다.
- target-only check는 exit 1(`target_match=False`)이며, gold freeze·holdout API·production 승격은 계속 BLOCKED다.

## 2026-08-11 사용자용 한국어 review 계약 전환

- `qa/contradiction-holdout-review.csv`를 사용자용 exact 한국어 헤더
  `사례_ID,주장_A,주장_B,시간_또는_버전,환경_또는_조건,주제,분류,비모순_사유,판단_이유,원문_확인,사용자_결정`로 전환했다. `NOT_PROVIDED`는 `정보 없음`, 여섯 분류 enum은 `직접 모순/시간 변화/조건 차이/관점 차이/판단 보류/모순 없음`, `source_verified=TRUE`는 `원문_확인=확인`으로 표시하고 case ID·기술 고유명사·사용자 결정 공란을 유지했다.
- 이 파일은 사용자용 review이며 backend harness 입력이나 machine gold가 아니다. 사용자 승인 후 표시 분류를 역매핑해 별도 machine gold `case_id,classification,판단_이유`를 생성·검증·동결한다. 승인 전 gold freeze·holdout API·production 승격은 하지 않는다.
- 한국어 header/sentinel 역매핑 검증 exit 0: 70/70 unique, 목표 분포 TRUE/TIME/CONDITION/VIEWPOINT/POSSIBLE 각 8 및 NO 30, parity 0, source_ref 140개·고유 60개·missing 0, NFKC 내부/CT pair 중복 0, 한국어 판단 이유 70건, 검토 플래그 0, 사용자 결정 비공란 0.
- `.claude/team/qa/STATE.md` 현재 스냅샷과 인수 보고서·체크리스트의 현재형 차단 문구를 사용자 승인 대기 및 machine gold 분리 계약에 맞춰 갱신했다. 과거 v2/이전 review FAIL 기록은 삭제하지 않았다.

## 2026-08-11 사용자 전체 승인·machine gold freeze

- 사용자 메시지의 명시적 `전체 승인`을 승인 출처로 기록하고, `qa/contradiction-holdout-review.csv`의 `사용자_결정` 70행을 모두 `승인`으로 채웠다.
- 사용자용 review 표시 분류를 역매핑해 QA 소유 `qa/contradiction-holdout-gold.csv`를 생성·동결했다. exact header는 `case_id,classification,판단_이유`이며 user-facing review와 backend harness/gold 계약을 분리했다.
- 독립 UTF-8 Python 표준 라이브러리 검증 exit 0: review/gold/input 각 70행, IDs ordered join, 승인 70/blank 0, 목표 분포 8/8/8/8/8/30, review/input parity 0, gold label·reason join 0, source 140/60/missing 0, NFKC 내부·CT overlap 0, errors 0.
- API 호출·응답 처리·holdout validator 실행은 하지 않았고 production 승격도 하지 않았다. frozen gold 이후 holdout gate 검증이 남아 있다.

## 2026-08-11 gold freeze 후 validator self-check

- `python qa/verify_phase0_benchmark.py --self-check` 실행 결과 `SELF-CHECK PASS`, exit 0. 이는 validator 계약 self-check이며 holdout API 호출이나 gold 변경을 수행하지 않았다.

## 2026-08-11 machine gold raw UTF-8 header 교정

- 리더 재검증에서 PowerShell 표시가 아닌 raw bytes/UTF-8 `csv.DictReader`로 `qa/contradiction-holdout-gold.csv`를 확인한 결과 첫 header가 실제 `case_id,classification,??_??`로 손상된 것을 발견했다.
- `apply_patch`로 첫 줄만 exact `case_id,classification,판단_이유`로 교정했다. 이후 Python fieldnames exact 확인 및 `backend.scripts.phase0_benchmark.load_gold_labels(Path('qa/contradiction-holdout-gold.csv'))` 70행 반환이 exit 0으로 통과했다.
- API 호출·holdout 실행·gold 데이터 행 변경은 없었고, backend/evaluation 파일도 수정하지 않았다.
