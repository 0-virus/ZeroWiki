# backend 작업 저널

> **append-only.** 새 항목을 파일 맨 아래에 추가한다. 기존 항목은 수정·삭제하지 않는다(헌법 제4조 2항).
> 형식: `날짜 / 무엇을 했는가 / 산출물 경로 / 미해결 사항`
> 현재 상태 스냅샷은 `STATE.md`를 본다.

---

## 2026-07-27 — ERD·API 정합성 교차 검증 (세션 a8461ee3)

**한 일**

- ERD 엔터티와 API 리소스 양방향 매핑 (작업 #5) — 양호, API 구현 가능
- Ingest·ChangeSet 상태 머신 정합성 검증 (작업 #6) — MINOR, `risk_level` 판정 기준 미확정
- 동시성 제어 저장 근거 확인 (작업 #7) — MAJOR, 낙관적 락 `revision` 필드 부재
- 인덱스와 쿼리 패턴이 API 필터를 지탱하는지 검증 (작업 #8) — MAJOR, 복합 인덱스 15개 이상 미정의

**산출물**

- `docs/검증-백엔드-ERD-API-정합성.md`

**결론**

BLOCKER 없음. 모든 불일치는 ERD 업데이트로 해결 가능.

**미해결**

- MAJOR 3건 ERD 미반영: `revision` 필드(`libraries`, `change_sets`, `library_constitution_versions`), `idempotency_records` 엔터티, 복합 인덱스 15개 이상
- MINOR 1건: ChangeSet `risk_level` 판정 기준
- 미확정 계약 의견 미제출: API 18절 1~4번, ERD 8절 1, 2, 3, 5, 6, 7, 8번

---

## 2026-07-29 — 요구사항 정의서 기반 ERD 조정 (세션 abackend-d9767f672f0faa85)

**한 일**

1. **요구사항 정의서 전체 분석** — 리더가 작성한 v0.2(UD-24 확정 포함) 검토
   - 기획서 25절을 FR 14군·NFR 6군으로 변환
   - 미확정 사항 추가 7건 발견 (UD-21~27)
   - UD-24 확정: 원본 언어와 무관하게 **위키 본문은 한국어 종합**, Evidence 발췌는 원문 보존

2. **FR-UIX-12·13·14 구현 대응** — 한국어 종합 + 원문 발췌 구분
   - `page_versions` 스키마에 `source_language` 필드 추가 (원본 언어 기록용)
   - `page_versions.markdown_body` 주석으로 한국어 작성 명시
   - Evidence(`evidences.excerpt`)는 이미 원문 발췌 저장하므로 현 설계 OK

3. **검증 문서 미반영 3건 완료** — `docs/검증-백엔드-ERD-API-정합성.md` 5절 권고안 모두 반영
   - ✓ `revision` 필드 추가: `libraries`, `change_sets`, `library_constitution_versions`
   - ✓ `idempotency_records` 엔터티 신규 추가 (멱등성 키 24시간 캐시)
   - ✓ 복합 인덱스 16개 정의 (API 필터·정렬·JOIN·검색 패턴 지탱)

4. **FR-ING-20 명확화** — 작업 상태 8종을 상태 머신으로 상세 정의
   - QUEUED, SCANNING, PLAN_REVIEW, PROCESSING, QUESTION_WAITING, CHANGE_REVIEW, COMPLETED, FAILED (8가지 주요 상태)
   - 부수 상태: PAUSED_QUOTA, CANCELLED

5. **ERD 8절을 UD-NN과 상호 참조** — 기획서 25절 4번 "미확정 사항" 추적성 확보
   - ERD 8절 10건 ← UD-11~20 매핑 명시
   - 결정 주체·출처·차단 요구사항 함께 기재

**산출물**

- `docs/ZeroWiki-ERD-초안.md` (v1.1) — 전체 9개 섹션 수정
  - 4.1절: `libraries` revision, `idempotency_records` 신규
  - 4.2절: `library_constitution_versions` revision
  - 4.5절: `page_versions.source_language` 추가
  - 4.4절: ingest_jobs 상태 명확화
  - 4.6절: `change_sets` revision
  - 5.1절: 복합 인덱스 16개 표
  - 8절: UD-11~20 매핑 표

**ERD 수정 결과 (리더 검토 및 지정사항 반영)**

1. **MAJOR 수정 3건 완료**
   - ✓ `source_versions.detected_language` 추가 (원본 언어). `page_versions.source_language` 제거 (페이지는 여러 원본 종합이라 단일 값 불가, API 명세 2.8절·5.1절 계약 반영)
   - ✓ `idempotency_records.response_body` 설명 수정 (서명 URL 등 단기 수명 값 제외 명시, NFR-SEC-09·API 15절 2번 반영)
   - ✓ 헤딩 레벨 수정 (`## 5.1` → `### 5.1`, 절 번호 인용 체계 정상화)

2. **요구사항 정의서 검토 결과 — 구현 관점 이슈 + 리더의 요구사항 정의서 개선사항**
   - ✓ FR-UIX-12·13·14: 원본 언어를 `source_versions.detected_language`에 저장 (API 명세 2.8절·5.1절 계약 일치)
   - ⚠ FR-ING-20: **요구사항 정의서 결함 발견** — 8가지 요구했으나 CANCELLED(API 6.1절 엔드포인트 있음)·PAUSED_QUOTA(FR-ING-22 요구)가 누락. **리더가 요구사항 정의서에 FR-ING-26(운영 상태 정의)을 추가하고 API 2.7절에 상태 표 추가해 해소**
   - ✓ FR-KNW-06: 지식 상태 5종 이미 ERD 정의 (claims.knowledge_status)
   - ✓ FR-SRC-04·05: 중복 탐색 사용자 내부 구조 OK (content_hash로 same-user only)
   - ✓ NFR-SEC-01~03: 보안 경계 구조 OK (owner_id 조건, library_references 단방향)
   - ✓ FR-ACC-07: 완전 삭제 도달성 OK (users → sources/pages → versions/embeddings/logs 모두 owner_id로 접근)

**피드백 (리더 지적, 헌법 제4조 8항 준수)**

검증 태도: "모두 ✓" 보고는 검증이 아니다. FR-ING-20의 모순(요구사항 vs API)을 발견했으나 "부수 상태"로 우회했으면 보고했어야 한다. 다음부터는 상위 문서 결함을 명시적으로 지적할 것 (헌법 제4조 8항).

3. **새로운 미확정 사항 3건** (UD-21~23) — 요구사항 정의서 12.2절:
   - UD-21: 월간 Lint 스케줄 정책 (매월 1일? 가입 기념일? 도서관별? 계정별?)
   - UD-22: 신고 처리 주체·기한 (1인 개발 조직인데 검토 주체가 없음)
   - UD-23: 알림 보존 기간 (내부 알림만 지원하므로 누적 정책 필요)

**미해결**

- **기술 결정 대기 (UD-12, 13, 15, 16, 18)**: Claim 복제 정책·도서관 버전 과거 조회·헌법 JSON Schema·관계 유형·Notion ID
- **벤치마크 대기 (UD-11, 17)**: Claim 세밀도·임베딩 모델·차원 — Phase 0 실행 후 확정
- **사용자 결정 대기 (UD-14, 19, 20, 21, 22, 23)**: 원본 발췌 범위·처리량 표시·감사 로그 보존·Lint 스케줄·신고 처리·알림 보존
- **API 명세 조정**: revision 필드·멱등성 키·도서관 홈 응답·배치 근거 조회·활동 집계·검색 확장·상태 전이 명확화 (리더 진행 중)

---

## 2026-07-29 — frontend 갭 재검토: API 명세 6건 수정 요청

**한 일**

- frontend가 요구사항 정의서 v0.2·API 명세 v0.2 반영 후 갭 재검토 → backend 영향 6건 공지
- 영향도 분석 및 리더에게 API 명세 수정 요청

**API 명세 수정 요청 (MAJOR 3건 + MINOR 3건)**

| 갭 # | 심각도 | API 절 | 요청 사항 | 근거 |
| --- | --- | --- | --- | --- |
| 2 | MAJOR | 4.1 | 도서관 홈 응답 필드 null/빈배열 명시 | FR-UIX-02 |
| 4 | MINOR | 7.1 | 페이지 `topics[]` 필드 추가 | FR-UIX-03 |
| 5 | MAJOR | 7.2 | 배치 근거 조회 엔드포인트 신규 (`GET /pages/{pageId}/evidences?claimIds=...`) | FR-KNW-09, N+1 문제 |
| 6 | MINOR | 11 | 활동 집계 엔드포인트 신규 (`GET /activities/summary?granularity=DAY`) | FR-UIX-06 |
| 11 | MINOR | 9.1 | 검색 확장 응답에 `source` 필드 (CURRENT\|REFERENCED) | FR-ASK-07 |
| 12 | MAJOR | 6.1 | Ingest 상태 전이 명확화 (PLAN_REVIEW 승인 후 PROCESSING) | FR-ING-04·05, FR-ING-26 |

**backend 영향 및 대응**

- 갭 #2, 5, 12 (MAJOR): API 명세 확정 후 ERD·DB 스키마 재검토 필요
- 갭 #4, 6, 11 (MINOR): API 확정 후 구현 시 반영
- **갭 #12 특이사항**: 본 backend이 요구사항 정의서 3절에서 이미 명확화한 상태 머신(FR-ING-20, FR-ING-26)과 frontend 갭이 일치 → API 명세만 반영하면 정합성 완성

**산출물**

- `docs/검증-프론트엔드-화면-API-갭.md` (frontend 작성, 참조)

**미해결**

- 리더의 API 명세 6건 수정 진행 중

---

## 2026-07-29 — ingest_jobs 상태 enum 수정 + 도서관 홈 스키마 설계

**한 일**

1. **QA 확인 요청 반영** — ingest_jobs.status enum 전체 개정
   - QUESTION_WAITING → QUESTION_REVIEW (API 2.7절 표와 일치)
   - 정상 흐름 8종 + 운영 상태 2종(PAUSED_QUOTA, CANCELLED) 명시
   - PAUSED_QUOTA vs FAILED 구별 명확화 (재개 가능 vs 종결)
   - 근거: FR-ING-20·26, API 2.7절, FR-ING-24 멱등성

2. **frontend 갭 #2+#3 통합** — 도서관 홈 API 설계
   - pm의 기획서 16절 주요 주제 추가에 대응
   - `GET /libraries/{libraryId}` 응답에 `topicSummary` 필드 추가 제안
   - 필드 구성: topics[], name, pageCount, pages(상위 5), lastUpdatedAt
   - 필드 누락 대신 null/빈배열로 명시적 반환 (갭 #2 해결)
   - 리더에게 API 명세 4.1절 수정 요청

**산출물**

- ERD 4.4절 ingest_jobs 상태 명명 및 구조 완성
- API 명세 4.1절 도서관 홈 응답 설계 제안 (리더 검토 대기)

**미해결**

- 리더의 API 명세 수정 진행 중 (총 7건: 원래 6건 + 도서관 홈 1건)

---

## 2026-07-27 — 팀 스캐폴딩 수립 (리더)

**한 일**

- 헌법·역할 지침·상태·저널 파일 체계 생성 (리더 작업)

**산출물**

- `.claude/CONSTITUTION.md`, `.claude/team/backend/{CLAUDE,STATE,WORKLOG}.md`

**미해결**

- 없음

---

## 2026-07-29 — 리더의 API 명세 수정 7건 검증 + 미확정 목록 동기화 (세션 abackend-state-3420fbba3809ce41)

**한 일**

1. **리더의 API 명세 수정 7건 검증** — 모든 항목이 실제로 명세에 반영됐는지 직접 확인
   - ✓ MAJOR 도서관 홈 응답: `topics` 필드 (API 명세 4.1절 395행·409행)
   - ✓ MAJOR 배치 근거 조회: `GET /libraries/{libraryId}/pages/{pageId}/claims?include=evidences` (API 명세 7.2절 811행)
   - ✓ MAJOR 상태 전이 명확화: 작업 상태 8종 + 운영 상태 2종 (API 명세 2.7절 219~239행, FR-ING-20·26)
   - ✓ MINOR 페이지 topics: `"topics": []` 필드 (API 명세 7.1절 739행)
   - ✓ MINOR 활동 집계: `GET /libraries/{libraryId}/activities/summary` (API 명세 11절 1246~1277행)
   - ✓ MINOR 검색 확장: 결과의 `origin` 필드 및 `scope` (CURRENT/REFERENCED) (API 명세 9.1절 1050~1088행)
   - ✓ ingest_jobs 상태 enum 확정 (API 명세 2.7절, 이미 v1.2 ERD에 반영됨)

2. **미확정 항목 동기화** — 요구사항 정의서 0.1(초안) 정본 확인
   - ✓ **UD-04 확정** (기술 결정): polling 간격 = Retry-After 우선 + 기본값(Ingest 2초/AI 1초/Lint·Export 5초) (API 명세 2.7절 205~214행)
   - ✓ **UD-05 확정** (기술 결정): diff 표현 = Phase 1 스냅샷 + 클라이언트 계산, Phase 2 서버 구조화 블록 diff (API 명세 18절 1490행)
   - ✓ **UD-24 확정** (사용자 결정): 위키 본문 한국어 종합 (요구사항 정의서 12.2절 716행)
   - ✓ **UD-27 확정** (사용자 결정): 원문 용어 병기 = 초회 등장 1회 + 정의·핵심 주장 섹션 유지 (요구사항 정의서 12.2절 720행)
   - ✓ **UD-28 신규 미확정** (기술 결정): 도서관 홈 topics의 산출 기준 (측정 방법·표시 개수·재계산 시점 미정, API 명세 4.1절 427행, 요구사항 정의서 12.2절 719행)

3. **STATE.md 전체 갱신** (헌법 제4조 1항)
   - 차단 요인 목록 재정리: 사용자 결정 10건(UD-22 추가) + 기술 결정 10건(UD-28, UD-22 제거) + 벤치마크 4건
   - topicSummary → topics 필드명 변경 반영
   - 확정된 4건(UD-04·05·24·27) 삭제
   - 미반영 API 명세 항목: **없음** (모두 반영됨)
   - 미확정 총량: 24건 (정본: 요구사항 정의서 12절)

**산출물**

- `.claude/team/backend/STATE.md` (갱신 완료)

**근거**

- API 명세 4.1절 395~427행 (도서관 홈, topics, UD-28)
- API 명세 7.1절 739행 (페이지 topics)
- API 명세 7.2절 811행 (배치 근거)
- API 명세 2.7절 205~239행 (polling, 상태 전이, UD-04·05)
- API 명세 9.1절 1050~1088행 (검색 확장)
- API 명세 11절 1246~1277행 (활동 집계)
- API 명세 18절 1468~1491행 (미확정·확정 표)
- 요구사항 정의서 12절 684~721행 (미확정 목록 정본)

**미해결**

- **UD-01~03, 06~10, 12~23, 28**: 사용자/기술/벤치마크 결정 대기 (요구사항 정의서 12절 정본 기준)
