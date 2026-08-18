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

## 2026-08-12 — Phase 1 QA gate 계획·정본 교차 대조

**한 일**

- `.claude/CONSTITUTION.md` → `.claude/team/qa/CLAUDE.md` → `.claude/team/qa/STATE.md` 순서로 읽고, 루트 `CLAUDE.md`·QA `WORKLOG.md`와 저장소 실제 파일 목록을 교차 확인했다.
- 기획서 3·18·23절, 요구사항 정의서 1.4·5.1~5.9·6.1·9·11~12절, API 명세 2~9·15~17절, QA 체크리스트 3·5~6절, `qa/test_scenarios.md` A-1~A-5·S-1~S-7을 대조해 Phase 1 gate를 AC-1·2·4·5 자동 검증으로 고정했다. AC-3·6·7은 Phase 4 사용자 평가로 유지했다.
- AC-1은 `/uploads` → `/uploads/{uploadId}/complete` → `/libraries/{libraryId}/ingest-jobs`, AC-2는 API 4.1의 `home.topics`(PUBLISHED CONCEPT·ACCEPTED 관계 상위 6개), AC-4는 API 9.2 citation과 API 7.2 evidence 재조회, AC-5는 생성 claim↔case mapping manifest와 dense holdout hard gate로 정의했다.
- `NFR-SEC-01/02/03/04/05/06/08/09`와 API 15절 1·6·8·9·10·11·12·14번을 Phase 1 보안 자동/통합 검증 대상으로 매핑했다. `qa/test_scenarios.md`의 S-1~S-7은 checklist의 위협명과는 맞지만 요구사항 6.1의 S 매핑과 충돌한다. 현재 S-3(SSRF)·S-6(공개 저작권)은 각각 Phase 2·3 범위여서 리더 결정 전 Phase 1 PASS 근거로 사용하지 않는다.
- 현재 `backend/`에는 `phase0_benchmark.py`와 constitution schema만 있고 Phase 1 API·Worker·fixture/test harness가 없음을 확인했다.

**검증 명령과 실제 결과**

- `python qa/verify_phase0_evaluation.py` → `RESULT PASS failures=0`, exit 0.
- `python evaluation/validate_evaluation.py` → `검증 완료: 오류 0건`, exit 0.
- `python qa/verify_phase0_benchmark.py --self-check` → `SELF-CHECK PASS ...`, exit 0.
- `python -m py_compile qa/verify_phase0_benchmark.py` → exit 0.
- `rg --files backend frontend qa evaluation | Sort-Object` → Phase 1 API/Worker/test harness 파일 없음; Phase 0 harness·frontend mock·QA validator만 존재.
- `rg -n "POST /|GET /|PATCH /|PUT /|DELETE /" qa/test_scenarios.md` → `/ingest-plans`, `/search`, `/libraries/{id}/topics`, `PATCH /ingest-jobs` 등 API 정본에 없는 경로가 남아 있음.

**산출물 경로**

- `.claude/team/qa/STATE.md` — Phase 1 계획·gate·차단 조건·재현 명령 갱신.
- `.claude/team/qa/WORKLOG.md` — 본 항목 append.

**미해결**

- 리더가 S 번호 충돌과 Phase 2·3 범위 혼입을 정본 기준으로 결정해야 한다.
- backend가 Phase 1 API/Worker, fixture, claim mapping manifest를 제공하기 전 AC-1·2·4·보안 통합 테스트는 미실행이다.
- holdout API·validator가 실행되기 전 AC-5 자동 모순 판정·적용과 production 승격은 BLOCKED다.

## 2026-08-12 — M0/M1 QA API 정합화·인수 준비

**무엇을 했는가**

- `qa/test_scenarios.md`의 활성 경로·메서드를 API 명세 2~9절 기준으로 교정했다. `/ingest-plans`는 `/uploads` → `/uploads/{uploadId}/complete` → `/libraries/{libraryId}/ingest-jobs` 흐름으로, `POST /search`는 대화·메시지 API로, `/libraries/{id}/topics`는 `home.topics`와 `/pages?topic=`로, `PATCH /ingest-jobs`는 `/ingest-jobs/{jobId}/cancel` 등 실제 경로로 대체했다. 버전·publication·export·claims·references 경로도 실제 `libraryId` 계층으로 보정했다.
- A-17의 UD-12 기대 모델을 `claims.page_version_id` 제거·`claim_statuses` 버전별 상태·`evidences.claim_id` 유지로 교정하고, `GET /libraries/{libraryId}/pages/{pageId}/claims?include=evidences`를 명시했다.
- 기존 S-1~S-16 ID와 Phase 2·3 시나리오는 삭제·renumber하지 않았다. `docs/검증-QA-계약-확정-체크리스트.md`에 `NFR-SEC-* + API 15절 + Phase 1 포함 여부` 매핑표를 추가하고, 보안 감사 일정은 해당 매핑의 S-1~S-16을 가리키도록 명확화했다. checklist의 과거 `PATCH /ingest-jobs/{id}` 예시도 실제 plan-approval 경로로 대체했다.
- backend M1 API·Worker·fixture가 없으므로 test code는 만들지 않고, `qa/test_scenarios.md`에 `M1-AUTH-01`, `M1-ISO-01`, `M1-PRIVATE-01`, `M1-CONST-01`, `M1-REF-01`의 auth rotation·owner isolation·private default·constitution immutability·explicit reference direction 인수 케이스와 실행 명령을 준비했다. API 6.1의 구매·재개 endpoint 미정과 refresh replay 오류 enum 미정을 차단 조건으로 남겼다.

**검증 명령과 실제 결과**

- `python qa/verify_phase0_benchmark.py --self-check` → `SELF-CHECK PASS ...`, exit 0.
- `rg -n '/ingest-plans|POST /search|GET /libraries/\\{id\\}/topics|PATCH /ingest-jobs|POST /publications|POST /exports|GET /public/pages/\\{id\\}' qa/test_scenarios.md docs/검증-QA-계약-확정-체크리스트.md` → `NO_MATCH: stale active API paths`, exit 0.
- `git -c safe.directory=C:/Users/PC/Desktop/ZeroWiki-SaaS diff --check -- qa/test_scenarios.md docs/검증-QA-계약-확정-체크리스트.md .claude/team/qa/STATE.md .claude/team/qa/WORKLOG.md` → whitespace 오류 없음(라인 종료 변환 경고만), exit 0.

**미해결·삭제/대체 기록**

- 삭제한 QA 시나리오·S 식별자는 없다. 오래된 endpoint 표기는 위의 실제 API 경로로 대체했고, Phase 2 SSRF·Notion·export·알림과 Phase 3 공개 저작권·공개 응답 사례는 범위에서 제외하되 문서에서 보존했다.
- API 명세 6.1의 `PURCHASE_QUOTA` 후 구매·재개 호출은 endpoint가 명시되지 않아 A-10을 상태 확인·Phase 2 계약 대기로 제한했다. API 명세 3.1의 refresh replay 구체 오류 enum도 임의 확정하지 않았다.
- backend M1 산출물 제공 전 `python -m unittest discover -s qa -p "test_m1_*.py" -v`는 실행하지 않는다. AC-1·AC-2·AC-4·보안 통합 실행과 AC-5 holdout gate는 계속 BLOCKED다.

## 2026-08-12 — 보안 매핑표 개념 S 명시 및 최종 self-check

- checklist 매핑표에 `요구사항 6.1 개념 S 표기` 열을 추가해, 예를 들어 정본 S-1(격리)↔기존 QA S-5, 정본 S-2(참조)↔기존 QA S-12, 정본 S-6(프롬프트)↔기존 QA S-1처럼 번호 차이를 드러냈다. `NFR-SEC-08` 감사 로그는 기존 QA S-7의 멱등성 케이스에 함께 추적하고 `NFR-SEC-10/UD-20`은 미확정으로 Phase 1에 임의 편입하지 않았다.
- 기존 QA S-7 예상 결과에 API 15-10 감사 로그(actor·timestamp·target·result)를 추가했다. ID·사례는 삭제·renumber하지 않았다.
- `python qa/verify_phase0_benchmark.py --self-check` → `SELF-CHECK PASS ...`, exit 0. 금지된 stale 활성 경로 rg 검색(`/ingest-plans`, `POST /search`, `/libraries/{id}/topics`, `PATCH /ingest-jobs`, 기타 구경로) → `NO_MATCH: stale active API paths`, exit 0. QA 변경 파일 `git diff --check` → whitespace 오류 없음(라인 종료 변환 경고만), exit 0.

## 2026-08-12 — M1/M2 stdlib black-box·계약 self-check 준비

**무엇을 했는가**

- `qa/test_m1_m2_blackbox.py`를 새 QA 소유 black-box 실행기로 추가했다. 외부 프레임워크·의존성 없이 `unittest`, `urllib`, `zipfile`만 사용하며 `ZEROWIKI_BASE_URL`, `ZEROWIKI_QA_TIMEOUT`로 재사용한다.
- M1 live 케이스는 회원가입·로그인·`/me`, Access 900초 계약·Refresh cookie 속성·rotation/replay 거부, PRIVATE 기본·owner isolation, 헌법 버전 불변·`VERSION_CONFLICT`, 동일 사용자 단방향 reference·cross-owner 거부를 검증한다.
- M2 live 케이스는 `/sources`, presigned `/uploads`, Obsidian ZIP PUT·complete, sourceVersion 연결·`EXISTING_SOURCES` ingest job·계획 승인 전 `PROCESSING` 금지를 검증한다. 파일/ZIP/Import 제한·MIME/압축/path traversal/secret/signed URL 조건은 정적 계약 self-check로 함께 확인한다.
- `qa/test_scenarios.md`의 M1 실행 대기 문구를 실행 준비·M2 계약 acceptance로 갱신했다. M2 controller 부재, API·PostgreSQL 미실행은 PASS로 승격하지 않고 `BLOCKED_BACKEND_M2`/`BLOCKED_API_UNREACHABLE`로 드러낸다.

**검증 명령과 실제 결과**

- `python -m py_compile qa/test_m1_m2_blackbox.py` → exit 0.
- `python qa/test_m1_m2_blackbox.py --self-check` → static 6개 PASS, `BLOCKED_BACKEND_M2` 1개 skip, exit 0.
- `python -m unittest discover -s qa -p "test_m1_m2_blackbox.py" -v` → static 6개 PASS, backend M2 surface 1개 skip, live M1/M2 2개 class `BLOCKED_API_UNREACHABLE`, exit 0.
- API reachability probe `localhost:8080` → `API_UNREACHABLE`; PostgreSQL probe `localhost:5432` → `POSTGRES_UNREACHABLE`.
- `.\mvnw.cmd -q -DskipTests compile` (workdir: `backend`) → exit 0. backend M1 컴파일은 PASS지만 live API 인수 PASS로 해석하지 않는다.

**미해결·차단**

- backend 소스에 M1 controller만 있고 M2 source/upload/ingest controller가 없다. `qa/test_m1_m2_blackbox.py`의 static contract는 정본 계약을 확인했지만 M2 구현·실행은 BLOCKED다.
- API와 PostgreSQL을 실행 가능한 격리 fixture로 제공한 뒤 동일 unittest 명령을 다시 실행해야 M1 live 및 M2 acceptance를 판정할 수 있다. 현재 test는 계정·원본·토큰을 생성하지 않았다.

## 2026-08-12 — M2 backend surface 재확인 및 live gate 유지

- backend 산출물을 재조회한 결과 `SourceController`, `UploadController`, `IngestController`와 `/sources`, `/uploads`, `/libraries/{libraryId}/ingest-jobs` route 표면이 확인됐다. QA 실행기는 M2 핵심 route inventory를 정적으로 확인하며 구현 표면을 런타임 PASS로 오인하지 않는다.
- Refresh cookie acceptance에 정본 UD-02의 30일(2,592,000초) `Max-Age`를 명시적으로 검증하도록 보강했다. 실행기 설정은 `ZEROWIKI_BASE_URL`·`ZEROWIKI_QA_TIMEOUT`만 사용한다.

**검증 명령과 실제 결과**

- `python -m py_compile qa/test_m1_m2_blackbox.py` → exit 0.
- `python qa/test_m1_m2_blackbox.py --self-check` → static 6개 PASS(backend M1/M2 route inventory 포함), skip 0, exit 0.
- `python -m unittest discover -s qa -p "test_m1_m2_blackbox.py" -v` → static 6개 PASS, live M1/M2 2개 class `BLOCKED_API_UNREACHABLE`(WinError 10061), exit 0.
- `http://localhost:8080` 및 `localhost:5432` probe → 각각 `API_UNREACHABLE`, `POSTGRES_UNREACHABLE`. 계정·원본·토큰은 생성하지 않았다.

**판정·잔여 위험**

- M1/M2 static contract 및 backend route inventory는 PASS다. M1 인증·도서관·헌법과 M2 Source/Upload/Obsidian/Ingest의 실제 acceptance는 API·PostgreSQL 실행 전까지 BLOCKED다.
- 기존 WORKLOG의 과거 M2 부재 관찰은 당시 실행 시점 기록으로 보존하며, 이번 항목이 최신 backend surface 관찰과 현재 gate를 덮어쓴다.

## 2026-08-12 — M2 presigned URL 계약 차단 확인

- backend M2 route inventory는 존재하지만 `UploadService.plan()`이 `"/uploads/" + id + "/content"` 상대 경로를 `uploadUrl`로 반환한다. API 5.2의 presigned URL 계약 및 QA acceptance의 절대 signed URL 조건과 불일치하므로 구현 PASS로 처리하지 않는다. backend 파일은 수정하지 않고 QA static/live check에 blocker를 고정했다.
- `qa/test_m1_m2_blackbox.py`는 complete 응답에서 enclosing source `id`를 source version으로 잘못 선택하지 않도록 `latestVersion.id` 우선 추출을 추가했다. 실제 API가 반환하는 `sources[].latestVersion.id`를 `EXISTING_SOURCES` 요청에 연결한다.

**검증 명령과 실제 결과**

- `python -m py_compile qa/test_m1_m2_blackbox.py` → exit 0.
- `python qa/test_m1_m2_blackbox.py --self-check` → 7개 중 6개 PASS, `test_backend_m2_upload_plan_does_not_expose_local_path` FAIL, exit 1. 실패 근거는 local authenticated path가 presigned URL 대신 노출되는 정적 계약 위반이다.
- live 재실행 전 API·PostgreSQL probe는 각각 `API_UNREACHABLE`, `POSTGRES_UNREACHABLE`로 확인됐다. 따라서 M1/M2 runtime acceptance는 여전히 BLOCKED다.

## 2026-08-12 — M2 ingest answer route 및 bounded plan approval 보강

- API 6.1의 `POST /libraries/{libraryId}/ingest-jobs/{jobId}/answers`를 M2 정적 endpoint·backend route inventory에 추가했다. backend는 현재 `questions/{questionId}/answer`를 제공하므로 API 계약 불일치가 static FAIL로 드러난다.
- M2 live 케이스는 `SCANNING`을 bounded poll(`ZEROWIKI_QA_POLL_SECONDS`, `ZEROWIKI_QA_POLL_LIMIT`)하고 `PLAN_REVIEW`에서 `expectedEstimatedTokens`·`expectedPlanRevision`을 포함한 plan approval을 호출해 승인 전 `PROCESSING` 금지와 승인 후 전이를 함께 확인한다.
- complete 응답의 `sources[].latestVersion.id`를 우선 추출하도록 해 source ID를 source-version ID로 잘못 연결하지 않게 했다.

**검증 명령과 실제 결과**

- `python -m py_compile qa/test_m1_m2_blackbox.py` → exit 0.
- `python qa/test_m1_m2_blackbox.py --self-check` → 7개 중 5개 PASS·2개 FAIL(local upload path, `/answers` route), exit 1. 두 실패는 API 정본과 backend 구현의 구체적 차이다.
- live acceptance는 API·PostgreSQL 미실행으로 `BLOCKED_API_UNREACHABLE`이며, 계정·원본·토큰은 만들지 않았다.

## 2026-08-12 — 최종 self-check·환경 차단 기록

- 최신 `python qa/test_m1_m2_blackbox.py --self-check`는 7개 중 5개 PASS·2개 FAIL(local upload path·`/answers` route), exit 1이다. `python -m unittest discover -s qa -p "test_m1_m2_blackbox.py" -v`도 동일 2개 FAIL과 M1/M2 live 2개 `BLOCKED_API_UNREACHABLE` skip으로 exit 1이다.
- `python -m py_compile qa/test_m1_m2_blackbox.py`는 exit 0, `python qa/verify_phase0_benchmark.py --self-check`는 `SELF-CHECK PASS`·exit 0이다.
- backend compile 재실행은 `C:\.m2\repository` 생성 권한 부족 `LocalRepositoryNotAccessibleException`으로 exit 1이었다. 이전 compile PASS 기록과 달리 최신 실행은 환경 차단으로 기록하며 backend 파일은 수정하지 않았다.
- 금지 stale 활성 경로 rg는 `NO_MATCH: stale active API paths`·exit 0, QA 변경 파일 `git diff --check`는 whitespace 오류 없이 exit 0(기존 LF→CRLF 경고만), 신규 실행기 trailing-whitespace self-check도 PASS했다.

## 2026-08-12 — M3 page/trace/review acceptance 및 upload 계약 확장

**무엇을 했는가**

- `qa/test_m3_knowledge_blackbox.py`를 추가했다. 표준 라이브러리만 사용하며 API 7.1~7.3·9.2 endpoint/shape, 페이지 목록·상세, Claim-Evidence-sourceVersion 재조회와 Evidence 400자/원문 언어, `ACCEPTED` 관계·`OPEN` 모순 review 차단, AC-1 100문서/95%, AC-2 최소 5 topics/95% classification, AC-4 citation triple·`insufficientEvidence` 입력 계약을 static/live로 검증한다.
- `qa/test_m1_m2_blackbox.py`의 upload acceptance를 API 5.2 확정 계약(`uploadId`, absolute `uploadUrl`, `method`, `requiredHeaders`, `expiresAt`)으로 갱신했다. required headers 전달, 만료 시각, 다른 사용자 session mismatch PUT/complete 거부, 만료 URL fixture(`ZEROWIKI_QA_EXPIRED_UPLOAD_URL`), signed URL 오류·로그 redaction 조건을 추가했다.
- `qa/test_scenarios.md`, `qa/README.md`, `docs/검증-QA-계약-확정-체크리스트.md`에 M3 실행 명령·fixture 차단(`BLOCKED_M3_*`)·AC-1/2/4 traceability와 upload local/test 계약을 기록했다. M3 route/fixture 부재를 PASS로 낮추지 않는다.

**검증 명령과 실제 결과**

- `python -m py_compile qa/test_m1_m2_blackbox.py qa/test_m3_knowledge_blackbox.py` → exit 0.
- `python qa/test_m3_knowledge_blackbox.py --self-check` → API/requirements/acceptance calculator 7개 PASS, backend M3 route inventory 6건 미구현 FAIL, exit 1. 실패는 `@RequestMapping("/libraries/{libraryId}/pages")`, relations, contradictions, claims/evidences, contradiction resolution route 부재다.
- `python qa/test_m1_m2_blackbox.py --self-check` → 9개 중 6개 PASS·3개 FAIL(local upload path, DTO `requiredHeaders`, explicit expiry/session comparison). API/endpoint static은 PASS이며 backend 계약 위반은 FAIL로 유지한다.
- `python -m unittest discover -s qa -p "test_m3_knowledge_blackbox.py" -v` → static M3 route inventory FAIL, live class는 `BLOCKED_API_UNREACHABLE` (localhost:8080 WinError 10061); exit 1.
- `python qa/verify_phase0_benchmark.py --self-check` → `SELF-CHECK PASS`, exit 0. stale API 경로 검색 및 QA diff whitespace 검사는 후속 최종 검증에서 재실행한다.

**미해결·차단**

- backend M3 controller/DTO/서비스가 없어 실제 page·claim/evidence·relation/contradiction acceptance를 실행하지 않았다. 100문서 ingest, generated topics, source version, answer citation fixture도 필요하다.
- backend `UploadPlanResponse`는 아직 `requiredHeaders`가 없고 upload session expiry를 저장·검사하지 않으며, `UploadService.plan()`은 상대 local path를 반환한다. backend 소유이므로 QA는 FAIL 근거만 유지하고 수정하지 않는다.

- 후속 정합화: local upload path 정적 정규식을 `session.getId()` 등 실제 변수형에도 매칭하도록 보강했다. 재실행 결과 M1/M2 self-check는 9개 중 6개 PASS·3개 FAIL(local path, `requiredHeaders`, explicit expiry/session comparison)으로 실제 backend 결함을 놓치지 않는다.

## 2026-08-12 — M2 presigned bearer/session 검증 정정

- exact presigned `uploadUrl`은 bearer capability이므로 다른 앱 Authorization token을 붙인 PUT을 ownership 실패로 판정하지 않도록 `mismatch_put`을 서명 query 변조 또는 다른 `uploadId` path로 변경했다. 다른 사용자 token의 authenticated `complete`와 만료 URL PUT은 계속 실패 gate다.
- Refresh cookie의 `Secure` 요구는 `ZEROWIKI_QA_EXPECT_SECURE_COOKIE` 환경변수로 분리했다. 기본값 `true`는 production gate를 유지하고, local HTTP functional 실행은 `false`로 명시할 수 있다.

## 2026-08-12 — M1/M2 worker 분리 진단·M3 fixture bootstrap·M4 §8 black-box

**무엇을 했는가**

- `qa/test_m1_m2_blackbox.py`에 API/Worker profile 분리 정적 검증을 유지하고, live bounded poll이 `SCANNING`에 고착될 때 `BLOCKED_WORKER_NOT_RUNNING`을 명시하도록 확정했다. API는 servlet profile, Worker는 `web-application-type: none`이며 동일 PostgreSQL을 사용해야 한다. local HTTP에서만 `ZEROWIKI_QA_EXPECT_SECURE_COOKIE=false`를 사용하고 production 기본값은 `true`로 유지한다.
- 실제 API가 실행됐던 이전 probe에서 job 수락 후 `SCANNING`이 bounded poll 내 `PLAN_REVIEW`로 전이되지 않은 것을 worker 미기동/DB 불일치 blocker로 기록했다. 이번 재프로브는 `localhost:8080`·`localhost:5432` listener 및 Java process가 없어 `BLOCKED_API_UNREACHABLE`로 종료됐다.
- `qa/test_m3_knowledge_blackbox.py`는 private library를 class 단위로 재사용하고, page가 없으면 정본에 존재하는 M2 upload/complete/ingest API로 실제 source fixture를 생성·승인 시도한다. page 생성 endpoint를 추측하지 않으며 page projection이 없으면 `BLOCKED_M3_PAGE_FIXTURE`로 남긴다. 기존 owner boundary, apply 전 page version immutability, Claim-Evidence-sourceVersion 재조회 검증을 유지했다.
- `qa/test_m4_change_review_blackbox.py`를 추가했다. API 8.1~8.2의 change-set list/detail/items/reviews/apply/recalculate와 library versions 계약을 static/live로 검증하고, foreign owner boundary, stale revision `409 VERSION_CONFLICT`, apply 전 불변성, `APPLIED` 후 버전 증가를 검사한다. 현재 backend M4 controller 부재는 static FAIL blocker로 보존한다. API 정본에 없는 `PARTIALLY_APPROVED` 요구는 제거했다.
- `qa/README.md`와 `qa/test_scenarios.md`에 API/Worker 두 터미널 재검증 절차, Secure cookie env 분리, M3 실제 fixture 한계, M4 실행 명령·차단 기준을 추가했다. stale 경로는 보존된 과거 설명 외 활성 테스트 경로에 두지 않았다.

**검증 명령과 실제 결과**

- `python -m py_compile qa/test_m1_m2_blackbox.py qa/test_m3_knowledge_blackbox.py qa/test_m4_change_review_blackbox.py` → exit 0.
- `python qa/test_m1_m2_blackbox.py --self-check` → 10/10 PASS, exit 0.
- `python qa/test_m3_knowledge_blackbox.py --self-check` → 8/8 PASS, exit 0.
- `python qa/test_m4_change_review_blackbox.py --self-check` → API endpoint/terms PASS, backend M4 route inventory 5개 FAIL, exit 1. 이는 backend 미구현을 숨기지 않는 의도적 blocker다.
- `$env:ZEROWIKI_QA_EXPECT_SECURE_COOKIE='false'; $env:ZEROWIKI_QA_POLL_LIMIT='10'; python -m unittest discover -s qa -p 'test_m1_m2_blackbox.py' -v` → static 10개 PASS, live M1/M2 2개 class `BLOCKED_API_UNREACHABLE` skip, exit 0.
- `python -m unittest discover -s qa -p 'test_m3_knowledge_blackbox.py' -v` → static 8개 PASS, live `BLOCKED_API_UNREACHABLE` skip, exit 0.
- `python -m unittest discover -s qa -p 'test_m4_change_review_blackbox.py' -v` → API unreachable skip + backend M4 route inventory FAIL, exit 1.
- `python qa/verify_phase0_benchmark.py --self-check` → `SELF-CHECK PASS`, exit 0.
- `rg -n --glob 'qa/**' --glob 'docs/검증-QA-계약-확정-체크리스트.md' '/ingest-plans|POST /search|/libraries/\\{id\\}/topics|PATCH /ingest-jobs' .` → `NO_MATCH: stale active API paths`, exit 0.
- `git -c safe.directory='C:/Users/PC/Desktop/ZeroWiki-SaaS' diff --check -- qa .claude/team/qa` → whitespace 오류 없음, exit 0(기존 LF→CRLF 경고만).

**미해결·차단**

- API·PostgreSQL·Worker를 실행 가능한 격리 환경에서 같은 DB로 기동한 뒤 M1/M2 live를 재실행해야 `SCANNING` 진단을 해소하고 Phase 1 ingest gate를 판정할 수 있다.
- backend 정본 API에 page 생성/seed endpoint가 없고 현재 ingest 구현은 page projection을 만들지 않으므로, M3 page/claim/evidence/relation/topic/answer live fixture는 backend seed/projection 제공 전 차단된다.
- backend API §8 change-set/version controller route 5개가 없어 M4 review/apply/version gate는 static FAIL·live BLOCKED다. backend 소유 파일은 수정하지 않았다.

## 2026-08-12 — M4 controller annotation 조합 정합화

**무엇을 했는가**

- backend가 `ChangeSetController`를 추가한 뒤 기존 QA exact-string 검사가 class-level `@RequestMapping("/libraries/{libraryId}")`와 method-level `@GetMapping`/`@PostMapping` 조합을 인식하지 못해 false FAIL을 냈다.
- `qa/test_m4_change_review_blackbox.py`가 해당 controller의 공통 prefix와 9개 method suffix를 조합해 검사하도록 수정했다. API §8 정본에 없는 `PARTIALLY_APPROVED` 요구는 이미 제거한 상태를 유지했다.
- M4 static route inventory는 backend 구현을 읽은 결과 PASS로 갱신했고, live gate는 API·page/version fixture가 없으므로 계속 BLOCKED로 유지했다.

**검증 명령과 실제 결과**

- `python qa/test_m4_change_review_blackbox.py --self-check` → 3/3 PASS, exit 0.
- `python -m py_compile qa/test_m1_m2_blackbox.py qa/test_m3_knowledge_blackbox.py qa/test_m4_change_review_blackbox.py` → exit 0.
- `git -c safe.directory='C:/Users/PC/Desktop/ZeroWiki-SaaS' diff --check -- qa .claude/team/qa` → whitespace 오류 없음, exit 0(기존 LF→CRLF 경고만).

**미해결·차단**

- M4 live review/apply/version은 API와 page/version fixture가 실행된 뒤 재검증한다. backend controller 파일은 소유권상 수정하지 않았다.

## 2026-08-12 — M4 live 입력 정합화 보강

- M4 page fixture 조회를 API 계약과 동일한 `type=CONCEPT&status=PUBLISHED` 필터로 제한했다.
- 새 library의 `currentVersion`이 `null`일 수 있는 실제 backend 응답을 반영해 `expectedLibraryVersion=0`을 초기값으로 사용하고, 환경 주입 버전은 선택적으로 덮어쓰도록 했다. 버전 목록·apply 이후 증가 검증은 유지한다.
- `python -m py_compile qa/test_m4_change_review_blackbox.py` → exit 0, `python qa/test_m4_change_review_blackbox.py --self-check` → 3/3 PASS, `git diff --check -- qa .claude/team/qa` → exit 0.

## 2026-08-12 — M6 검색·AI 사서 계약 및 실행 준비

**무엇을 했는가**

- `qa/test_m6_search_ai_blackbox.py`를 표준 라이브러리만으로 추가했다. API §9.1~9.2의 9개 route, `KEYWORD`·`SEMANTIC`·`HYBRID`, `CURRENT`·`REFERENCED` explicit expansion, 400자 snippet, owner/reference boundary, conversation/message `QUEUED`→`COMPLETED` polling, citation triple/source-version 재조회, `insufficientEvidence`, save-proposal/feedback route와 `UD-25` latency 기록을 static/live로 준비했다.
- backend controller가 class-level prefix와 method-level annotation을 조합할 수 있는 현실을 반영해 M6 route matcher를 full path 또는 controller prefix+suffix로 구현했다. 현재 backend에는 해당 9개 Search/Conversation route가 없어 static inventory FAIL을 정확히 유지한다. backend 파일은 수정하지 않았다.
- API §9.2에는 feedback request body가 없으므로 임의 enum을 계약으로 만들지 않았다. live는 기본 body 없는 endpoint probe를 하고 승인된 body가 있을 때만 `ZEROWIKI_QA_FEEDBACK_BODY` JSON을 주입한다. `UD-25` answer p95 20초는 `M6_LATENCY_SECONDS`로 sample을 기록하고, 단일 sample은 p95 PASS로 승격하지 않는다.
- `qa/README.md`와 `qa/test_scenarios.md`에 M6 추적성, 실행 명령, 9개 기대 route, fixture/provider/API 차단 조건, feedback body 미정 사항을 추가했다.

**검증 명령과 실제 결과**

- `python -m py_compile qa/test_m1_m2_blackbox.py qa/test_m3_knowledge_blackbox.py qa/test_m4_change_review_blackbox.py qa/test_m6_search_ai_blackbox.py` → exit 0.
- `python qa/test_m6_search_ai_blackbox.py --self-check` → API route/terms/helper 5개 PASS, backend M6 route inventory 9개 FAIL, exit 1. 실패 목록은 `GET /libraries/{libraryId}/search`, `POST /libraries/{libraryId}/search-expansions`, conversation 3개, message 2개, save-proposal, feedback이다.
- `python -m unittest discover -s qa -p "test_m6_search_ai_blackbox.py" -v` → static 5개 PASS + backend inventory 1개 FAIL, live class `BLOCKED_API_UNREACHABLE` skip(`localhost:8080`, WinError 10061), 종료 코드 1.
- `rg -n --glob 'qa/**' '/ingest-plans|POST /search|/libraries/\{id\}/topics|PATCH /ingest-jobs' .` → M6 변경 범위에서 stale 활성 경로 없음(`NO_MATCH`); 의도적 과거 설명은 보존했다.
- `git -c safe.directory='C:/Users/PC/Desktop/ZeroWiki-SaaS' diff --check -- qa .claude/team/qa` → whitespace 오류 없음, exit 0(기존 LF→CRLF 경고만).

**미해결·차단**

- backend M6 route 9개와 검색 index·AI provider가 없어 static FAIL/live 실행 차단이다. 구현 후 동일 self-check와 live 명령을 재실행해야 한다.
- API/PostgreSQL가 현재 실행되지 않아 current/reference 검색 fixture, page·claim·sourceVersion citation, save/feedback 실제 응답과 latency를 판정하지 못했다.
- API §9.2 feedback body가 미정이다. 승인된 계약이 정본에 추가되기 전에는 QA가 임의 요청 schema나 enum을 PASS로 고정하지 않는다.

## 2026-08-12 — M6 검색 결과 envelope self-check 보강

- `_validate_search_results()`가 API §9.1 응답의 `score`, `pageId` 또는 `sourceId`, `origin.libraryName`까지 live에서 요구하도록 보강했다. 400자·scope·library boundary와 citation shape synthetic self-check는 계속 PASS한다.
- `python -m py_compile qa/test_m6_search_ai_blackbox.py` → exit 0, `python qa/test_m6_search_ai_blackbox.py --self-check` → 계약/helper 5개 PASS·backend route 9개 FAIL, exit 1. 실패 사유와 기대 route는 변하지 않았다.

## 2026-08-12 — M7 Phase 1 보안·a11y gate 정합화

**무엇을 했는가**

- `qa/test_m7_security_blackbox.py`를 추가해 기존 `S-1~S-16`을 renumber하지 않고
  `NFR-SEC-01/02/03/04/05/06/08/09`, API 2.5·15절, Phase 범위를 교차 검사한다.
  현재 backend route를 owner/library descendant, explicit `library_references`,
  source secret rejection, prompt command non-execution surface, change apply/audit,
  error/log redaction으로 정적 inventory하고, frontend representative page의
  `main`/`header`/`nav`, label↔control, native keyboard interaction,
  `:focus-visible`, alert/status error state를 stdlib 정적으로 검사한다.
- 기존 suite 중복을 만들지 않기 위해 M2 `test_source_secret_rejection_and_prompt_text_is_data`
  에 synthetic `api_key` fixture의 저장 전 `422 SECRET_DETECTED`·redaction과
  prompt-shaped data acceptance를 추가하고, M6
  `test_ai_question_idempotency_key_reuses_the_same_result`에 same-key duplicate
  AI question probe를 추가했다. M7 live는 owner descendant route probe만 추가했다.
- `qa/test_scenarios.md`에 S-1~S-16 식별자·요구사항/NFR·API 근거·Phase 1 포함/제외
  표를 append했다. SSRF·공개 저작권·Notion·알림·기존 Export 시나리오는 보존하되
  Phase 1 PASS로 해석하지 않는다.

**검증 명령과 실제 결과**

- `python -m py_compile qa/test_m1_m2_blackbox.py qa/test_m6_search_ai_blackbox.py qa/test_m7_security_blackbox.py` → exit 0.
- `python qa/test_m1_m2_blackbox.py --self-check` → 10/10 PASS, exit 0.
- `python qa/test_m6_search_ai_blackbox.py --self-check` → 6/6 PASS(현재 backend Search/Conversation route inventory 포함), exit 0.
- `python qa/test_m7_security_blackbox.py --self-check` → 7개 중 6 PASS, 1 FAIL, exit 1. 유일한 FAIL은 `idempotency_records` migration은 있으나 backend Java에 `Idempotency-Key` 처리·same-result/replay·`IDEMPOTENCY_KEY_REUSED`·`IDEMPOTENCY_KEY_IN_PROGRESS` 경로가 없는 의도적 blocker다.
- `$env:ZEROWIKI_BASE_URL='http://localhost:8080'; $env:ZEROWIKI_QA_EXPECT_SECURE_COOKIE='false'; python -m unittest test_m7_security_blackbox.M7LiveBlackBoxTest.test_owner_boundary_covers_current_library_descendant_routes -v` → API reachable, live FAIL. foreign token의 `GET /libraries/{libraryId}/search`와 `/conversations`가 기대 403/404 대신 500 `INTERNAL_ERROR`를 반환했다(requestId 예: `req_f1b4a741-c99f-43bb-ba02-71e3dff09739`, `req_a7f91146-535e-4587-bf41-c7ed568239f5`). response body에는 library ID/owner title이 없었으나 500 자체로 NFR-SEC-01/02 gate FAIL이다.
- `qa/README.md`, `.claude/team/qa/STATE.md`를 M7 현재 판정·명령·blocker로 갱신했다. backend/frontend 소유 파일은 편집하지 않았다.

**미해결·차단**

- backend가 API 2.5 idempotency middleware/service를 supported route에 연결하기 전에는 M7 static gate와 M6 live duplicate probe를 PASS로 낮출 수 없다.
- backend는 foreign owner의 M6 search/conversation 500 원인을 requestId 로그와 대조해 403/404 boundary로 고쳐야 한다. 원인 확인·수정은 backend 소유이므로 QA는 route와 재현만 보고한다.
- M2 secret/prompt live와 M6 idempotency live 전체는 API·worker·fixture 상태를 확인한 후 재실행한다. Phase 1 보안 gate는 idempotency와 owner boundary FAIL이 해소되기 전 BLOCKED다.

## 2026-08-12 — M7 targeted live 재검증

- `$env:ZEROWIKI_BASE_URL='http://localhost:8080'; $env:ZEROWIKI_QA_EXPECT_SECURE_COOKIE='false'; python -m unittest test_m1_m2_blackbox.M2LiveBlackBoxTest.test_source_secret_rejection_and_prompt_text_is_data -v` → **PASS**, exit 0. synthetic `api_key` content는 `422 SECRET_DETECTED`로 차단되고 complete error/source query에 fixture·upload URL이 없었으며, prompt-shaped markdown은 `FILE` source data로 완료되었다.
- 동일 환경에서 `python -m unittest test_m6_search_ai_blackbox.M6LiveBlackBoxTest.test_ai_question_idempotency_key_reuses_the_same_result -v` → **FAIL**, exit 1. 동일 user/path/body와 `Idempotency-Key`를 붙인 두 AI 질문이 서로 다른 assistant message ID(`c0333af0-adf4-4d8f-83ef-5a95f5d4dfda` vs `fc1135b0-cb3a-4740-b105-95769ea9e9a2`)를 생성했다. 첫 probe의 unsupported `EXPLAIN`은 API 정본/실제 backend enum에 맞춰 `COMPARE`로 수정한 뒤 재현한 결과다.
- `python -m py_compile qa/test_m1_m2_blackbox.py qa/test_m6_search_ai_blackbox.py qa/test_m7_security_blackbox.py`, M1/M2·M3·M4·M6 self-check 재실행은 각각 py_compile exit 0, M1/M2 10/10, M3 8/8, M4 3/3, M6 6/6 PASS를 확인했다. M7 self-check는 6/7 PASS + API §2.5 idempotency 1 FAIL(exit 1)로 의도된 차단을 유지했다.
- M7 owner descendant live는 별도 targeted 실행에서 API reachable, foreign search/conversations 500 `INTERNAL_ERROR` FAIL을 재현했다. backend 소유 원인 확인 전 QA는 403/404 expected gate를 완화하지 않는다.

## 2026-08-12 — Phase 1 100-document live acceptance runner

**What changed**

- Added only `qa/test_phase1_acceptance_live.py`, a single opt-in stdlib black-box test. It builds 100 unique Markdown entries in memory, uploads and starts ingest, approves `PLAN_REVIEW`, checks `DONE` items >=95/100, approves/applies every ingest change item, requires job `COMPLETED`, checks published CONCEPT/topic classification, claim/evidence/sourceVersion traceability, all three search modes, a cited assistant answer, the exact feedback body, and save-proposal.
- Reused the existing `LiveBlackBoxBase`/`ApiClient` and M3/M6 helpers. No new dependency, framework, fixture file, token output, or signed URL persistence was added.

**Verification**

- `python -m py_compile qa/test_phase1_acceptance_live.py` — PASS.
- In-memory archive self-check — PASS (`ARCHIVE_BYTES=25284`, `MARKDOWN=100`, `UNIQUE_MARKDOWN=100`).
- `python qa/test_phase1_acceptance_live.py` and `python -m unittest discover -s qa -p "test_phase1_acceptance_live.py" -v` — both exited 0 with explicit `BLOCKED_API_UNREACHABLE` skip; `localhost:8080` had no listener at execution time.
- `git diff --check -- qa/test_phase1_acceptance_live.py` — PASS.

**Next gate / blocker**

- Run with `ZEROWIKI_QA_RUN_PHASE1_ACCEPTANCE=true`, `ZEROWIKI_QA_POLL_SECONDS=0.02`, and `ZEROWIKI_QA_PHASE1_TIMEOUT=240` once the API and worker share a live database. The post-apply `job.status=COMPLETED` assertion is a hard product gate; a stale jar leaving `CHANGE_REVIEW` must be reported as FAIL.

## 2026-08-12 — Batch holdout QA 준비 및 Phase 1 feedback 계약 보강

**무엇을 했는가**

- `qa/verify_phase0_benchmark.py`의 dense validator를 holdout 실제 식별자 `HT-NNN`까지 허용하도록 보강했다. Wilson hard-gate profile은 정확히 70개 gold case와 70개 prediction row를 요구한다. dev `CT-NNN` 경로와 기존 sparse manifest 조인은 유지했다.
- self-check에 Batch output 순서 비결정성을 반영해 reversed 70행을 `case_id`로 조인하는 경로를 추가했다. 69행 누락, 70행 중 `FAILED` 1행, sampled FPR 3/30(0.10) 및 Wilson upper 0.256 초과를 각각 gate 실패로 확인한다. 성공 fixture는 70/70, recall 1.000, accuracy 1.000, sampled FPR 0.000, Wilson upper 0.114다.
- `qa/test_phase1_acceptance_live.py`의 feedback assertion을 API 확정 계약대로 HTTP 200 및 `data.accepted is True`로 좁혔다. 기존 201/202/204 허용은 제거했다.
- backend 소스는 정적 inventory로 재확인했다. `OPENAI_BATCHES_URL`·가격/크기 상수와 sync용 request body·prompt 누출 self-check는 있으나, Batch `/v1/files` upload·`/v1/batches` create/poll·output file download/JSONL 결과 조인 구현은 아직 없다. 따라서 실제 Batch gate는 실행하지 않았고 backend 파일은 편집하지 않았다.

**검증 명령과 실제 결과**

- `python -B qa/verify_phase0_benchmark.py --self-check` → exit 0. 70-case HT fixture의 reversed join, 69-row omission, FAILED completion block, FPR/Wilson block 포함.
- `python -B backend/scripts/phase0_benchmark.py --self-check` → exit 0. gold/case/reason/source_ref prompt 누출 self-check PASS.
- `python -m py_compile qa/verify_phase0_benchmark.py qa/test_phase1_acceptance_live.py` → exit 0.
- `ZEROWIKI_QA_RUN_PHASE1_ACCEPTANCE=true` 환경에서 `python -m unittest discover -s qa -p "test_phase1_acceptance_live.py" -v` → exit 0, `PHASE1_ACCEPTANCE_PASS documents=100 done=100 publishedConcepts=100 topics=6 citations=5`.
- opt-in 없는 `python qa/test_phase1_acceptance_live.py` → exit 0, 의도적 skip(`OPT_IN_PHASE1_100_DOCS`).
- `git -c safe.directory=C:/Users/PC/Desktop/ZeroWiki-SaaS diff --check` → exit 0(기존 LF→CRLF 경고만).

**게이트 판정·잔여 위험**

- AC-5 holdout 필수 기준은 모두 유지한다: 70/70 completion, recall ≥0.80, six-class accuracy ≥0.80, sampled FPR ≤0.10, Wilson 95% upper ≤0.20. dev Wilson은 3개 negative 표본 한계로 report-only다.
- **HIGH/BLOCKED:** Batch 실행 경로와 실제 70행 output이 없어 holdout API/validator quality gate는 미실행이다. backend output이 준비되면 `case_id` join으로 순서를 무시하고, 누락/FAILED는 100% completion에서 차단해야 한다.
- **MEDIUM:** 현재 QA validator는 dense CSV 행별 token/cost 및 quality gate를 검증하지만, Batch metrics JSON의 aggregate usage/cost가 CSV 합계와 일치하는지와 provider cap(`OPENAI_BATCH_REMAINING_CAP_USD`)은 Batch 산출물 schema가 생긴 뒤 별도 대조가 필요하다. 비용·토큰 합계가 없거나 unknown usage인 결과는 PASS로 취급하지 않는다.

## 2026-08-12 — HT/70 validator 변경 후 dev 회귀 확인

- `python -B qa/verify_phase0_benchmark.py --manifest evaluation/results/phase0-gpt-5_6-sol-rubric-context-v3-r1/phase0-manifest.json --generated-claims evaluation/results/phase0-gpt-5_6-sol-rubric-context-v3-r1/phase0-generated-claims.csv --benchmark-output evaluation/results/phase0-gpt-5_6-sol-rubric-context-v3-r1/phase0-benchmark.csv --dense-predictions evaluation/results/phase0-gpt-5_6-sol-rubric-context-v3-r1/openai-predictions.csv --labels evaluation/contradiction-labels.csv --expected-documents 26` → exit 0, `completion=1.000 recall=0.957 sampled_fpr=0.000 accuracy=0.923`, Wilson `0.561` report-only, `RESULT PASS failures=0`.

## 2026-08-12 — HT holdout validator 실제 경로 수정 및 partial Batch 재검증

**무엇을 했는가**

- `qa/verify_phase0_benchmark.py`에 `--holdout-input`을 추가했다. dense+`--holdout-labels` 실행은 8열 `evaluation/contradiction-holdout-input.csv`와 3열 machine gold를 `case_id`로 조인해 `GOLD_HEADERS` 형태의 HT manifest gold를 구성한다. dev 실행은 기존 `--labels` full CSV 경로를 그대로 사용한다.
- `validate_manifest()`와 `validate_generated_claims()`의 case ID 검사를 CT 전용 `CASE_ID`에서 CT/HT 공용 `DENSE_CASE_ID`로 바꿨다. HT-001..HT-070의 유일성·입력 필수값·enum·reason·manifest/generated 대응을 실제 main 경로에서 검증한다.
- self-check에 synthetic 70행 HT input+machine gold join → HT manifest → generated claims 검증을 추가했다. 기존 dev self-check와 reversed 70행, 누락 1행, FAILED 1행, 3/30 false-positive Wilson 실패 gate는 보존했다.
- backend 파일은 편집하지 않았다. `evaluation/results/phase1-holdout-gpt-5_6-sol-batch-r1/`의 실제 runner 산출물은 20행(19 completed·1 failed)뿐이므로 validator가 manifest/generated는 통과시키고 70행·completion·recall·accuracy gate를 정직하게 FAIL한다.

**검증 명령과 실제 결과**

- `python -m py_compile qa/verify_phase0_benchmark.py qa/test_phase1_acceptance_live.py` → exit 0.
- `python -B qa/verify_phase0_benchmark.py --self-check` → exit 0, `SELF-CHECK PASS`; HT input/gold join, 70행 reversed join, omission/FAILED/FPR/Wilson 차단 포함.
- `python -B backend/scripts/phase0_benchmark.py --self-check` → `self-check: PASS`, exit 0.
- dev 회귀 명령(`phase0-gpt-5_6-sol-rubric-context-v3-r1`, `--labels`, expected 26) → `completion=1.000 recall=0.957 sampled_fpr=0.000 accuracy=0.923`, `RESULT PASS failures=0`, exit 0.
- 실제 HT 명령(`--holdout-input evaluation/contradiction-holdout-input.csv`, `--holdout-labels qa/contradiction-holdout-gold.csv`, expected 70) → `PASS holdout input/gold`, `PASS manifest: gold case_id 전체 대응`, `PASS generated claims: manifest case_id 전체 대응`; 이어 `completed=19/70, failed=1, completion_rate=0.271, recall=0.250, accuracy=0.243`, `RESULT FAIL failures=7`, exit 1.
- `git -c safe.directory=C:/Users/PC/Desktop/ZeroWiki-SaaS diff --check` → exit 0(기존 LF→CRLF 경고만).

**미해결·차단**

- **HIGH:** full 70행 Batch output이 없어 AC-5 holdout hard gate(70/70 completion, recall·accuracy ≥0.80, sampled FPR ≤0.10, Wilson upper ≤0.20)는 BLOCKED다. 20행 partial 결과를 PASS로 완화하지 않았다.
- **MEDIUM:** `phase0-batch-metrics.json`의 aggregate usage/cost(`completed=19`, `failed=1`, `actual_cost_usd=0.0632025`, `max_cost_usd=0.24356`)와 dense CSV 합계·provider cap의 대조는 full Batch 산출물 계약 후 수행한다.

## 2026-08-12 — Phase 1 HT holdout 70행 최종 QA PASS

**무엇을 했는가**

- 새 runner 산출물 `evaluation/results/phase1-holdout-gpt-5_6-sol-batch-r1/`를 기존 `qa/verify_phase0_benchmark.py` 명령으로 독립 재검증했다. `--holdout-input` 8열 + `--holdout-labels` machine gold 조인, manifest/generated mapping, dense prediction을 실제 main 경로로 실행했다.
- 결과는 `70/70`, `failed=0`, recall `0.800`, six-class accuracy `0.843`, sampled FPR `0.000`, Wilson 95% upper `0.114`, `RESULT PASS failures=0`, validator exit 0이다. AC-5 holdout quality gate를 PASS로 갱신했고, M7 보안 FAIL 때문에 전체 Phase 1/production gate는 계속 BLOCKED로 유지한다.
- `phase0-batch-metrics.json`의 aggregate는 input `53504`, output `5136`, actual cost `$0.2108`, cap `$0.24356`(cap 이내)이다. `openai-predictions.csv`와 `phase0-benchmark.csv` 70개 COMPLETED 행의 deduped 합계는 input `52762`, output `4880`, actual cost `$0.205105`이다.
- aggregate와 CSV 차이 `$0.005695`(742 input, 256 output tokens)는 초기 `batch-state-000-020.json`과 겹치는 retry `batch-state-006-007.json`의 provider attempt가 aggregate state ledger에는 포함되고 최종 case별 CSV에는 한 번만 남기기 때문이다. state ledger 합계는 metrics JSON과 일치한다. cap은 PASS하되 exact CSV=metrics equality는 reconciliation note로 기록했다. backend 파일은 편집하지 않았다.

**검증 명령과 실제 결과**

- `python -B qa/verify_phase0_benchmark.py --manifest evaluation/results/phase1-holdout-gpt-5_6-sol-batch-r1/phase0-manifest.json --generated-claims evaluation/results/phase1-holdout-gpt-5_6-sol-batch-r1/phase0-generated-claims.csv --benchmark-output evaluation/results/phase1-holdout-gpt-5_6-sol-batch-r1/phase0-benchmark.csv --dense-predictions evaluation/results/phase1-holdout-gpt-5_6-sol-batch-r1/openai-predictions.csv --holdout-input evaluation/contradiction-holdout-input.csv --holdout-labels qa/contradiction-holdout-gold.csv --expected-documents 70` → `DENSE_METRICS expected=70 completed=70 failed=0 completion_rate=1.000 conflict=40 detected=32 recall=0.800 sampled_fpr=0.000 wilson95_fpr_upper=0.114 accuracy=0.843`, `RESULT PASS failures=0`, exit 0.
- PowerShell `Import-Csv` 직접 합계 대조 → predictions/benchmark rows 각각 70, `COMPLETED=70`; predictions input/output/cost `52762/4880/$0.205105`; metrics aggregate `53504/5136/$0.2108`; delta `$0.005695`; `UnderCap=True`.
- `python -B qa/verify_phase0_benchmark.py --self-check` → `SELF-CHECK PASS`, exit 0.
- `python -B backend/scripts/phase0_benchmark.py --self-check` → `self-check: PASS`, exit 0.
- `python -m py_compile qa/verify_phase0_benchmark.py qa/test_phase1_acceptance_live.py` → exit 0.
- `git -c safe.directory=C:/Users/PC/Desktop/ZeroWiki-SaaS diff --check -- qa .claude/team/qa` → exit 0(기존 LF→CRLF 경고만).

**게이트 판정·잔여 위험**

- **PASS:** Phase 1 AC-5 dense holdout quality 및 provider cost cap.
- **MEDIUM reconciliation:** aggregate provider-attempt 비용과 deduped final CSV 비용은 retry overlap 때문에 동일하지 않다. 비용 회계 계약이 logical case cost만 요구하면 CSV 합계, provider 실제 청구액을 요구하면 aggregate를 기준으로 삼아야 하며, 정확한 선택은 backend/리더 계약 확인이 필요하다.
- **BLOCKED:** 전체 Phase 1 보안 gate와 production 승격은 M7 API §2.5 idempotency FAIL, foreign-owner search/conversations 500 boundary FAIL 등 기존 finding이 해소될 때까지 닫지 않는다.

## 2026-08-18 — Phase 1 최종 독립 QA 및 Gradle 전환 동기화

**무엇을 했는가**

- 최신 backend/frontend 코드와 evaluation 산출물을 기존 QA validator와
  M1/M2·M3·M4·M6·M7 self-check로 독립 재검증했다.
- API는 현재 off이므로 M7 live를 재실행하지 않았다. 이전 최종 live
  8/8 PASS 근거와 이번 M7 static 7/7 PASS를 구분해 기록했다.
- `qa/README.md`, `qa/test_scenarios.md`, `.claude/team/qa/STATE.md`의
  idempotency FAIL, foreign-owner 500, API unreachable, holdout BLOCKED 현재형 문구를
  최신 PASS 스냅샷으로 교체했다. 이전 실패 근거는 이 WORKLOG의
  기존 항목을 수정·삭제하지 않고 보존했다.
- QA 활성 문서의 Maven 실행 명령을 Gradle wrapper
  `.\gradlew.bat bootRun --args="--spring.profiles.active=<api|worker>"`로 동기화했다.
  평가 CSV의 Maven 개념 문구와 WORKLOG 과거 명령은 역사/데이터이므로
  변경하지 않았다.

**산출물 경로**

- `qa/README.md`
- `qa/test_scenarios.md`
- `.claude/team/qa/STATE.md`
- `.claude/team/qa/WORKLOG.md`

**검증 명령과 실제 결과**

- `python -B qa/verify_phase0_evaluation.py` → `RESULT PASS failures=0`, exit 0.
- `python -B evaluation/validate_evaluation.py` → `검증 완료: 오류 0건`, exit 0.
- `python -B qa/verify_phase0_benchmark.py --self-check` → `SELF-CHECK PASS`, exit 0.
- HT 70행 실제 validator(`phase1-holdout-gpt-5_6-sol-batch-r1`, holdout input+gold,
  expected 70) → 70/70, failed 0, recall 0.800, accuracy 0.843, sampled FPR 0,
  Wilson upper 0.114, macro-F1 0.801, `RESULT PASS failures=0`, exit 0.
- `python -B qa/test_m1_m2_blackbox.py --self-check` → 10/10 PASS, exit 0.
- `python -B qa/test_m3_knowledge_blackbox.py --self-check` → 8/8 PASS, exit 0.
- `python -B qa/test_m4_change_review_blackbox.py --self-check` → 3/3 PASS, exit 0.
- `python -B qa/test_m6_search_ai_blackbox.py --self-check` → 6/6 PASS, exit 0.
- `python -B qa/test_m7_security_blackbox.py --self-check` → static 7/7 PASS, exit 0.
- Batch state JSON 7개 직접 합산 → input 53504, output 5136, cost `$0.2108`;
  metrics JSON과 일치. final prediction CSV 70행 → 52762/4880/`$0.205105`.
- backend 소유 실행 `backend\gradlew.bat -g .gradle-user-home clean test
  --no-daemon --non-interactive --console=plain` → exit 0. QA가
  `backend/build/test-results/test/TEST-*.xml` 15개를 직접 합산해 39 tests,
  failures 0, errors 0, skipped 0을 확인했다.
- backend 소유 실행 `backend\gradlew.bat -g .gradle-user-home bootJar
  --no-daemon --non-interactive --console=plain` → exit 0. QA가
  `backend/build/libs/zerowiki-backend-0.1.0-SNAPSHOT.jar` 생성을 확인했다.
- QA의 wrapper 직접 재실행 2회는 소스 컴파일 후 Gradle cache jar
  `AccessDeniedException`으로 각각 exit 1. 코드/test 실패가 아닌 Windows cache
  access lock으로 분리했다.

**미해결 사항**

- Phase 1 제품 blocker는 없다.
- production provider/model 승격과 모순 자동 적용은 별도 운영 승인
  대상이며, AC-3·6·7은 Phase 4 후속 범위다.
- Windows Gradle cache `AccessDeniedException`은 독립 wrapper 재실행성의
  환경 주의사항으로 남긴다. 성공한 clean test XML과 bootJar 산출물은
  별도로 확인했다.

**Gradle 성공 경로 보정**

- 위에 `.gradle-user-home`으로 기록한 성공 명령의 cache 경로는 잘못된
  전사다. 실제 성공 명령은 `-g "$env:LOCALAPPDATA\ZeroWiki-Gradle-9.7"`를
  사용한 `clean test` 및 `bootJar`이며 둘 다 exit 0이다. append-only 규칙에
  따라 기존 줄을 수정하지 않고 이 보정을 추가한다.
- 동일 cache로 QA가 `.\gradlew.bat -g "$env:LOCALAPPDATA\ZeroWiki-Gradle-9.7"
  test --no-daemon --non-interactive --console=plain`을 재실행해
  `BUILD SUCCESSFUL in 1m 26s`, 4 actionable tasks executed, exit 0을 독립 확인했다.
