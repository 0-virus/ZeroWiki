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

---

## 2026-07-30 — 사용자 5건 확정 반영: UD-01·02·06·07·14 (세션 abackend-propagate-c040db3579fed93e)

**한 일**

1. **요구사항 정의서 12절 정본 확인** — 리더 확정 5건 직접 검증
   - ✓ UD-01 확정(2026-07-30): 파일당 200MB, ZIP당 1GB, Import당 1000개 문서 (초과 시 413)
   - ✓ UD-02 확정(2026-07-30): Access Token 15분, Refresh Token 30일(회전)
   - ✓ UD-06 확정(2026-07-30): 자동 적용하지 않음, **항상 변경 세트 생성**
   - ✓ UD-07 확정(2026-07-30): 400자
   - ✓ UD-14 확정(2026-07-30): 400자
   - ✓ UD-28 확정(2026-07-29): 주제는 page_type='CONCEPT', name은 page_versions.title, pageCount는 page_relations status='ACCEPTED' incoming·outgoing 합산, pages.status='PUBLISHED'만 대상, 상위 6개, 비동기 배치

2. **ERD 수정 — 확정 항목 반영**
   - ✓ 8절 810행: UD-14를 "**확정 (2026-07-30, 사용자): 400자**"로 표기
   - ✓ 4.5절 402행 evidences.excerpt: "**최대 400자** (UD-07·UD-14 확정). 저장 범위와 노출 범위를 일치시켜 조회 시 재절단 불필요" 명시

3. **UD-28 확정 구현 검증**
   - ✓ page_relations 인덱스: 5.1절 763~764행 `(library_id, source_page_id, status, created_at DESC)` + `(library_id, target_page_id, status, created_at DESC)` — status='ACCEPTED' 필터와 양방향 합산 쿼리 지탱. 추가 인덱스 불필요.

4. **UD-06 확정 상태 머신 검증**
   - ✓ change_sets.status: `DRAFT`, `READY_FOR_REVIEW`, `PARTIALLY_APPROVED`, `APPROVED`, `REJECTED`, `APPLIED`, `SUPERSEDED`. 자동 적용이 없으므로 이미 설계 반영됨. APPLIED는 승인(APPROVED) 후에만 진행되므로 "항상 변경 세트 생성" 원칙 준수.

5. **UD-02 토큰 수명 검증**
   - ✓ 토큰 수명은 인증 서버 설정 사항이며 스키마 레벨에 반영할 사항 없음. `auth_sessions` 테이블의 `expires_at` 필드로 기간 관리 가능.

6. **STATE.md 갱신** — 차단 요인 목록 재정리
   - 사용자 결정 대기: 10건 → **5건** (UD-03, UD-10, UD-19, UD-20, UD-22)
   - 기술 결정 대기: 10건 → **9건** (UD-08, UD-09, UD-12, UD-13, UD-15, UD-16, UD-18, UD-21, UD-23)
   - 벤치마크 대기: 4건 (UD-11, UD-17, UD-25, UD-26)
   - **잔여 미확정: 18건** (정본 요구사항 정의서 12절 직접 확인)

**산출물**

- `docs/ZeroWiki-ERD-초안.md` (v1.2) — 8절·4.5절 수정, 9절 개정 이력 추가
- `.claude/team/backend/STATE.md` — 전체 덮어쓰기 갱신
- `.claude/team/backend/WORKLOG.md` — 이번 세션 기록 append

**검증 결과**

| 항목 | 확인 내용 | 결과 |
| --- | --- | --- |
| UD-28 인덱스 | page_relations 763~764행 `(status, created_at)` 포함 | ✓ 충분 |
| UD-06 상태머신 | change_sets.status APPLIED는 APPROVED 후만 진행 | ✓ 설계 반영 |
| UD-02 스키마 | auth_sessions.expires_at으로 토큰 수명 관리 | ✓ 스키마 필요 없음 |
| 미확정 건수 | 요구사항 정의서 12절 직접 세기 | ✓ 18건 = 사용자 5 + 기술 9 + 벤치마크 4 |

**근거**

- 요구사항 정의서 12절 684~703행 (UD-01~14 정본)
- 요구사항 정의서 12절 719행 (UD-28 정본)
- ERD 8절 805~816행 (미확정 표)
- ERD 4.5절 402행, 5.1절 763~764행 (스키마·인덱스)
- ERD 4.6절 445~461행 (change_sets 상태 머신)

**미해결**

- **UD-03, UD-10, UD-19, UD-20, UD-22**: 사용자 결정 대기
- **UD-08, UD-09, UD-12, UD-13, UD-15, UD-16, UD-18, UD-21, UD-23**: 기술 결정 대기
- **UD-11, UD-17, UD-25, UD-26**: 벤치마크 대기

---

## 2026-07-30 — STATE 정정 + 기술 결정 9건 권고안 작성 (세션 abackend-propagate-corrections)

**한 일**

1. **STATE.md 완전 덮어쓰기** — 리더 지적 사항 모두 정정
   - ✓ UD-28 "미정" 표기 제거, "다음 작업: 없음"으로 정정
   - ✓ 현재 단계 업데이트 (UD-28 확정 구현 사양 반영)
   - ✓ 차단 요인 재정리: 5/9/4 (사용자/기술/벤치마크)

2. **ERD 9절 개정 이력 추가** (v1.0 ~ v1.2)
   - ✓ 버전 번호를 문서에 명시 (없는 v1.3 표기 제거)
   - ✓ 각 버전의 변경 내용 기록

3. **기술 결정 9건 구현 관점 권고안** — qa 권고 vs ERD 설계 검증
   - **UD-08** (공개 URL slug): ✅ qa 권고 동의, UNIQUE 제약 충분
   - **UD-09** (관리자 API): ✅ qa 권고 동의, 스키마 영향 없음 (policy 결정)
   - **UD-12** (Claim 복제): ✅ qa 권고 동의, claim_key 설계 기반 구현 가능. **되돌리기 비용 높음** — Phase 1부터 도입 필수. claim_key 규칙 명시 필요
   - **UD-13** (이벤트 재생): ✅ qa 권고 동의, change_sets 영구 보존과 삭제 정책 충돌 없음 (계정 보존 기간 일치). **되돌리기 비용 높음** — Phase 0 벤치마크에서 성능 측정 필수
   - **UD-15** (JSON Schema): ✅ qa 권고 동의, library_constitution_versions jsonb 필드 준비 완료
   - **UD-16** (관계 유형): ✅ qa 권고 동의, varchar enum으로 초기 7개 고정, Phase 2 확장 설계 가능
   - **UD-18** (Notion 중복): ✅ qa 권고 동의, content_hash + source_metadata 결합 가능. **복합 인덱스 추가 권고** — `(content_hash, source_metadata->>'notion_id')`
   - **UD-21** (Lint 스케줄): ✅ qa 권고 동의, trigger_type 및 created_at 필드 충분. 구현 시 확정
   - **UD-23** (알림 보존): ⚠️ **ERD 스키마 수정 필요** — `archived_at` 컬럼 추가 권고. 현재 설계는 알림 영구 누적 구조

**산출물**

- `docs/검증-백엔드-ERD-API-정합성.md` — 7절 기술 결정 9건 권고안 추가 (총 8개 항목 + 총괄 요약)
- `.claude/team/backend/STATE.md` — 전체 덮어쓰기 + 차단 요인 상태 업데이트
- `.claude/team/backend/WORKLOG.md` — 이번 세션 기록 append

**검증 상세 근거**

| UD | ERD 절 | 설계 확인 | 권고 결과 |
| --- | --- | --- | --- |
| UD-12 | 4.5절 378~392행 | claim_key 필드 존재, 버전 간 대응 설계됨 | ✅ qa 동의 + claim_key 규칙 명시 필요 |
| UD-13 | 4.6절 495~507행 / 7절 796행 | change_set_id + 영구 보존 정책 일치 | ✅ qa 동의 + Phase 0 성능 측정 필수 |
| UD-23 | 4.8절 597~610행 | archived_at 필드 없음, read_at만 있음 | ⚠️ ERD 수정 권고 |

**미해결**

- **UD-23**: notifications 테이블에 `archived_at` 컬럼 추가 필요 (권고안 반영 대기)
- **UD-12**: claim_key 생성 규칙 문서화 필요 (권고안 반영 대기)
- **UD-13**: Phase 0 벤치마크 성능 측정 계획 필요 (권고안 반영 대기)

---

## 2026-07-30 — frontend 갭 #21: Contradictions API 추가 요청 (세션 abackend-propagate-gap21)

**한 일**

1. **frontend 갭 #21 검토** — 도서관 홈 화면의 모순 조회 API 정의 부재
   - frontend-propagate 요청 확인
   - 기획서 16.2절·요구사항 정의서 5.3절(FR-KNW-08) 대조
   
2. **현재 상태 검증**
   - ✓ ERD: contradictions 엔터티 설계 완료 (4.5절 426~441행)
   - ✓ API 명세: 경로만 등재, 상세 정의 없음 (7.3절 868행)
   - ⚠️ **필터·응답·페이지네이션 미정** — 리더에게 추가 요청

3. **API 스펙 제안** — 리더에게 요청한 내용
   - GET /libraries/{libraryId}/contradictions
   - 필터: status(OPEN|RESOLVED|ALL), classification(TRUE_CONFLICT|TIME_CHANGE|...), since, limit, after
   - 응답: data[], leftClaimStatement, rightClaimStatement, explanation, resolutionNote 포함
   - 심각도: MAJOR (도서관 홈 구현 차단)

**산출물**

- 리더에게 API 정의 추가 요청 메시지 전송 (msg_id: 5c5437b3-...)
- STATE.md 업데이트: "다음 작업" 1번 추가

**근거**

- 기획서 16.2절 (도서관 홈 필수 섹션)
- 요구사항 정의서 5.3절 FR-KNW-08 (모순 조회)
- ERD 4.5절 426~441행 (contradictions 엔터티)
- API 명세 7.3절 863~914행 (경로 목록)

**다음 단계**

1. 리더가 API 명세에 Contradictions 조회 섹션 추가
2. backend가 응답 필드와 ERD 정합성 검증
3. frontend가 화면 구현

---

## 2026-07-30 — 기술 결정 9건 재검토 및 정정 (세션 abackend-revise-ud12-21-23)

**한 일**

**리더 지적 사항 반영 (UD-12·21·23·18 재작성):**

1. **UD-12 (Claim 복제 vs 재사용) — 모순 정정**
   - ❌ 문제: qa 권고 "재사용" 동의하면서 구현은 "복제"
   - ✅ 정정: 재사용 방식 선택, ERD 수정안 제시
     - `claims` 테이블에서 `page_version_id` FK 제거
     - `claim_statuses` 테이블 신규 추가 (버전별 상태)
     - claim_key: 의미 정규화 기반 (표현 다양성 무시, 의미 변화만 감지)
   - ⏸️ 상태: 보류 (ERD 구조 재검토 필요)

2. **UD-21 (Lint 스케줄) — 부하 분산 추가**
   - ❌ 문제: "매월 1일 UTC" 기본값은 동시 실행 문제
   - ✅ 정정: 부하 분산 방식 추가 (가입일 기준, 매달 같은 일자)
     - 예: 7월 15일 가입 → 매달 15일 Lint 실행
     - 장점: 공평한 분산, 구현 단순
   - ⏸️ 상태: 보류 (분산 방식 리더 재검토)

3. **UD-23 (알림 보존) — 기간 근거 및 종류별 차등**
   - ❌ 문제: 7일 단일 기간 근거 부족, 위험 변경 알림 손실 위험
   - ✅ 정정: 알림 종류별 차등 보존
     - 위험 변경·삭제 유예: 30일 (승인 필요, 재실행 불가)
     - 포크·Lint·편집 제안: 14일 (재실행 가능)
     - 진행 알림: 7일 (실시간, 재실행 가능)
   - ERD: notifications에 `archived_at` 필드 추가
   - ⏸️ 상태: 보류 (알림 종류별 보존 리더 재검토)

4. **UD-18 (Notion 중복 판정) — 인덱스 선두 수정**
   - ❌ 문제: `(content_hash, notion_id)` 전역 인덱스는 사용자 간 중복 추적
   - ✅ 정정: `(owner_id, content_hash, notion_id)` — 사용자 내부만
   - ✅ 상태: 확정 (인덱스 수정 완료)

**산출물**

- `docs/검증-백엔드-ERD-API-정합성.md` — 7.1·7.8·7.9절 재작성, 8절 총괄 테이블 업데이트
- `.claude/team/backend/STATE.md` — 기술 결정 6건 확정, 3건 보류 표기
- `.claude/team/backend/WORKLOG.md` — 이번 세션 기록

**확정·보류 상태 (리더 판정)**

| 상태 | UD | 항목 | 이유 |
| --- | --- | --- | --- |
| ✅ 확정 | 08 | 공개 URL slug | UNIQUE 제약 충분 |
| ✅ 확정 | 09 | 관리자 API | 스키마 영향 없음 |
| ✅ 확정 | 13 | 이벤트 재생 | change_sets 삭제 정책 일치 |
| ✅ 확정 | 15 | JSON Schema | jsonb 필드 준비 완료 |
| ✅ 확정 | 16 | 관계 유형 | enum 고정 가능 |
| ✅ 확정 | 18 | Notion 중복 | 인덱스 수정 완료 |
| ⏸️ 보류 | 12 | Claim 복제 | 재사용/복제 명확화, ERD 수정안 |
| ⏸️ 보류 | 21 | Lint 스케줄 | 부하 분산 방식 재검토 |
| ⏸️ 보류 | 23 | 알림 보존 | 종류별 차등 보존 재검토 |

**근거**

- `docs/검증-백엔드-ERD-API-정합성.md` 7절 (UD-12·21·23·18)
- 기획서 6.1절 (공통 원본 사용자 내부 원칙)
- ERD 4.3·4.5·4.8절 (스키마 설계)

**미해결**

- **UD-12**: 재사용 구조 ERD 재설계, claim_key 의미 정규화 규칙 작성
- **UD-21**: 부하 분산 방식 (가입일 vs 계정 ID 해시) 최종 판단
- **UD-23**: 알림 종류별 보존 기간 확정 및 실제 기획서 17절 알림 종류 대조

---

## 2026-07-30 — 기술 결정 6건 확정 반영 및 ERD 갱신 (세션 abackend-finalize-6confirmed)

**한 일**

1. **리더 기술 결정 6건 확정** (요구사항 정의서 v1.0, API 명세 18절 반영 완료)
   - ✅ UD-08 (공개 URL slug) — 확정
   - ✅ UD-09 (관리자 API) — 확정
   - ✅ UD-13 (이벤트 재생) — 확정, Phase 0 성능 측정 조건: 도서관당 500+ change_sets 기준 1초 초과 시 Phase 2 스냅샷 필수
   - ✅ UD-15 (JSON Schema) — 확정
   - ✅ UD-16 (관계 유형) — 확정
   - ✅ UD-18 (Notion 중복) — 확정, 판정은 사용자 계정 내부로 한정

2. **ERD 갱신** (v1.3으로 버전 업데이트)
   - ✅ UD-16: page_relations.relation_type 7종 CHECK 제약으로 확정
     - `RELATED_TO`, `PART_OF`, `DEPENDS_ON`, `CAUSES`, `CONTRASTS_WITH`, `EXPLAINS`, `APPLIES_TO`
     - 설명: "확정 7종 (UD-16, 2026-07-30). Phase 2에서 사용자 정의 유형 추가"
   - ✅ UD-18: source_versions 복합 인덱스 추가
     - 인덱스: `(owner_id, content_hash, source_metadata->>'notion_id')`
     - 용도: "Notion Import 중복 판정 (UD-18 확정): 사용자 내 notion_id 기준 탐색"

3. **STATE.md 갱신**
   - 기술 결정 9건 → 3건으로 축약 (UD-12·21·23만 보류)
   - 미확정 총량: 18건 → **12건** (사용자 5 + 기술 3 + 벤치마크 4)
   - 확정 총량: 9건 → **15건** (사용자 5 + 기술 6 + UD-04·05·24·27)

4. **ERD 9절 개정 이력 추가**
   - v1.3 (2026-07-30): 기술 결정 6건 확정 반영, UD-16 CHECK 제약, UD-18 인덱스

**산출물**

- `docs/ZeroWiki-ERD-초안.md` (v1.3) — UD-16 CHECK 제약, UD-18 인덱스, 개정 이력 추가
- `.claude/team/backend/STATE.md` — 차단 요인 3건으로 축약, 미확정 12건
- `.claude/team/backend/WORKLOG.md` — 확정 반영 세션 기록

**근거**

- 요구사항 정의서 v1.0 (리더 확정, 개정 이력)
- API 명세 18절 (리더 반영)
- ERD 4.5절 414행 (relation_type)
- ERD 5.1절 754행 (인덱스 설계)

**다음 단계**

- UD-12·21·23 보류 사항 재작성 (리더 재검토 대기)
- Contradictions API 상세 스펙 (리더가 API 명세에 추가)

---

## 2026-07-30 — 기술 결정 최종 확정 및 ERD v1.4 대규모 구조 변경 (세션 abackend-finalize-all-decisions)

**한 일**

1. **기술 결정 9건 최종 처리** (리더 2026-07-30)
   - **6건 확정됨(이전):** UD-08·09·13·15·16·18
   - **3건 새로 확정:**
     - UD-12: 재사용 구조, claims.page_version_id 제거, claim_statuses 신설
     - UD-21: 가입일 기준 분산 + 월말 처리(29·30·31일은 말일로)
     - UD-23: 차등 보존(30/14/7일), 삭제 유예는 유예 기간까지, archived_at 추가

2. **UD-29 신설** (벤치마크 대기)
   - 주제: Claim 버전별 대응 규칙(claim_key 생성)
   - 핵심: 의미 정규화는 불가능(해시의 한계)
   - 권고: AI 제안 + 사용자 승인 모델(기획서 11·12절 패턴 일치)
   - 조건: UD-29 정해질 때까지 UD-12 자동 매칭 미구현

3. **ERD v1.4 대규모 구조 변경** (UD-12·21·23 확정)

   **A. claims 테이블 재설계:**
   - ✅ `page_version_id` FK 제거 (버전 비종속)
   - ✅ `knowledge_status`, `valid_from`, `valid_to`, `conditions` 제거 → claim_statuses로 이동
   - ✅ 설명: "버전 간 대응 안정 키. 생성 규칙은 UD-29에 위임"

   **B. claim_statuses 테이블 신규:**
   - ✅ PK: id
   - ✅ FK: claim_id, page_version_id
   - ✅ 컬럼: knowledge_status, confidence, valid_from, valid_to, conditions, created_at
   - ✅ UNIQUE(claim_id, page_version_id)
   - ✅ 인덱스: (page_version_id), (claim_id, created_at DESC)

   **C. notifications 테이블 (UD-23):**
   - ✅ archived_at 추가 (아카이브·삭제 시각, NULL = 활성)
   - ✅ 인덱스: (user_id, archived_at DESC)
   - ✅ 보존 정책: 차등 30/14/7일, 삭제 유예는 유예 기간까지

   **D. 정합성 규칙 갱신:**
   - ✅ 규칙 8: "Evidence는 반드시 사용자가 접근 가능한 source_version을 참조해야 한다. **Claim은 버전 비종속이므로, Claim을 통한 버전 추적은 claim_statuses를 거친다.**"
   - ✅ 규칙 9: "모순의 두 Claim은 ... **Claim이 버전 비종속이므로, 특정 버전에서의 모순 판정은 해당 버전의 claim_statuses에서 시작한다.**"

   **E. ERD 8절 미확정 표 갱신:**
   - ✅ 9개 항목(UD-11~20) 정리
   - ✅ UD-12·13·21·23 확정 표기
   - ✅ UD-29(신규) 추가: 벤치마크, "의미 정규화 불가(해시의 한계). AI 제안 + 사용자 승인으로 처리 추천"

   **F. ERD 9절 개정 이력:**
   - ✅ v1.4 추가: "대규모 구조 변경, UD-12·21·23 확정, UD-29 신설"

4. **영향 분석** (리더 지적)
   - ✅ UD-11(Claim 세밀도)과 맞물림: 행 수 불확실성 → 인덱스 설계 조정 필요
   - ✅ 구조 변경으로 contradictions·evidences 쿼리 패턴 변경
   - ✅ 버전 추적·정합성 규칙·인덱스 설계 전체 재검토 필요

**산출물**

- `docs/ZeroWiki-ERD-초안.md` (v1.4) — 대규모 구조 변경, 3개 테이블 수정/신설, 정합성 규칙 갱신
- `.claude/team/backend/STATE.md` — 완전 덮어쓰기, 기술 결정 9건 모두 처리, 미확정 11건(UD-29 추가)
- `.claude/team/backend/WORKLOG.md` — 최종 세션 기록

**근거**

- 요구사항 정의서 v1.0 (리더 확정)
- 리더의 UD-12 지적: "해시는 근사 유사도를 표현할 수 없다"
- 리더의 UD-29 신설: AI 제안+사용자 승인이 기획서 패턴과 일치
- ERD 설계 변경 시 지침: 옮기기 전 열·제약·인덱스 세기, 후 누락 확인(CLAUDE.md)

**주의 사항**

1. **구조 변경의 파급:**
   - claims 재설계로 claim_key 생성 규칙은 표류 상태(UD-29 벤치마크 대기)
   - Phase 1에서는 자동 매칭 미구현, 수동 승인 또는 보류
   - UD-11 결과에 따라 claim_statuses 행 수 급증 가능 → 인덱스 설계 동적 조정

2. **미확정 상태:**
   - UD-29는 Phase 0 벤치마크에서 의미 정규화 대안 평가 필요
   - 그 전까지 UD-12 재사용 구조는 설계만 하고 구현하지 않음

**다음 단계**

- Contradictions 엔드포인트 상세 스펙 정의 (갭 #21, 리더 API 명세 추가 대기)
- UD-29 벤치마크 계획: claim_key 자동 매칭 대안 평가(AI 제안+사용자 승인 vs 기타)

---

## 2026-07-30 — ERD v1.5: claims 테넌시 앵커 추가 (세션 abackend-tenancy-anchor)

**한 일**

1. **리더 지적 사항 검토 — ERD v1.4의 구조 결함**
   - ❌ 문제: `claims` 테이블에 `page_id` FK 없음
   - ❌ 파급: 소유자 인가 검사 4홉(claim_statuses→page_versions→pages→libraries), 도서관 간 재사용 구조 차단 불가
   - ✅ 근거: 기획서 6.1절(단방향 참조), 기획서 18절(보안), NFR-SEC-03(도서관 간 정보 노출 차단)

2. **ERD 수정 — `claims` 테이블에 `page_id` FK 추가**
   - ✅ `claims.page_id` (uuid, FK → `pages.id`, NOT NULL) 추가
   - ✅ 설명: "Claim이 속한 페이지(버전 비종속). `pages.library_id`로 한 홉에 도서관·소유자 접근"
   - ✅ 효과: 도서관 간 Claim 참조 구조로 차단, 인가 검사 성능 개선

3. **정합성 규칙 갱신**
   - ✅ 규칙 8 보강: "Evidence는 반드시... **`evidences.claim_id`는 현재 도서관에 속하는 Claim만 참조할 수 있다(소유자 인가 검사 단축).**"
   - ✅ 규칙 9 보강: "모순의 두 Claim은... **`contradictions.left_claim_id`·`right_claim_id`는 모두 현재 도서관에 속하는 Claim이어야 한다.**"
   - ✅ 규칙 10(신규): "**`claim_statuses.page_version_id`가 참조하는 버전은 `claims.page_id`와 동일 페이지에 속해야 한다.** 이를 통해 버전 간 Claim 재사용 범위를 같은 페이지 내로 제한한다."
   - ✅ 원래 규칙 10~12 → 11~13으로 번호 이동

4. **claim_statuses 인덱스 강화**
   - ✅ 기존 인덱스: `(page_version_id)`, `(claim_id, created_at DESC)`
   - ✅ 신규 인덱스: `(claim_id, page_version_id)` — Claim 검증 시 버전 범위 확인
   - 용도: 규칙 10 검증(같은 페이지 버전 확인)을 위한 조합 조건 최적화

5. **ERD 9절 개정 이력 v1.5 추가**
   - ✅ 테넌시 앵커 추가, 정합성 규칙 보강, claim_statuses 인덱스 추가
   - ✅ 설명: "1) Claim 소유자 접근을 4홉에서 1홉으로 단축. 2) 다른 도서관의 버전에서 Claim을 참조하는 것을 구조로 차단(정보 노출 방지). 3) 정합성 규칙 8·9 보강, 규칙 10 신규. 4) claim_statuses 인덱스 추가 — 버전 범위 검증 최적화"

**산출물**

- `docs/ZeroWiki-ERD-초안.md` (v1.5) — claims.page_id 추가, 정합성 규칙 갱신, 인덱스 보강, 개정 이력 추가
- `.claude/team/backend/STATE.md` — 완전 덮어쓰기, "다음 작업" 없음, "완료 항목" v1.5 반영
- `.claude/team/backend/WORKLOG.md` — 이번 세션 기록 append

**검증 체크리스트**

| 항목 | 확인 내용 | 결과 |
| --- | --- | --- |
| claims.page_id FK | NOT NULL, 기획서 6.1절·18절·NFR-SEC-03 준수 | ✅ |
| 인가 검사 단축 | pages.library_id 한 홉 접근 가능 | ✅ |
| 도서관 간 재사용 차단 | FK로 structural protection | ✅ |
| 정합성 규칙 | evidences·contradictions·claim_statuses 모두 보강 | ✅ |
| 인덱스 설계 | (claim_id, page_version_id) 추가로 규칙 10 검증 최적화 | ✅ |

**근거**

- 기획서 6.1절 (단방향 참조·단방향 사용자만 공유)
- 기획서 6.2절 (기본 비공개)
- 기획서 18절 (보안 설계)
- NFR-SEC-03 (도서관 간 정보 노출 차단)
- ERD 4.5절 378~389행 (claims 테이블)
- ERD 4.5절 390~410행 (claim_statuses 테이블)
- ERD 6절 780~813행 (정합성 규칙)

**다음 단계**

- Contradictions 엔드포인트 상세 스펙 정의 (갭 #21, 리더 API 명세 추가 대기)
- UD-29 벤치마크 계획: claim_key 자동 매칭 대안 평가(AI 제안+사용자 승인 vs 기타)
- 코드 구현: Spring Boot, Ingest Worker, API 개발 (Phase 0 후 시작)
