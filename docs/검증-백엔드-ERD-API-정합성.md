# 백엔드 ERD·API 정합성 검증

작성일: 2026-07-27  
상태: 백엔드 관점 교차 검증  
기준 문서:
- `ZeroWiki-ERD-초안.md`
- `ZeroWiki-API-명세-초안.md`
- `ZeroWiki-MVP-서비스-기획서.md`

---

## 1. 작업 #5: ERD 엔터티와 API 리소스 양방향 매핑

### 1.1 검증 목표

ERD에 정의된 모든 데이터 엔터티가 API 계약에 충분하게 표현되고, 역으로 API에 노출된 모든 리소스가 명확한 데이터 기저를 가지는지 확인한다.

### 1.2 ERD 엔터티 목록 (4절)

Phase 1 코어 범위 내 엔터티 36개:

| 엔터티 | 용도 | API 노출 여부 |
| --- | --- | --- |
| `users` | 사용자 계정 | ✓ |
| `auth_sessions` | 인증 세션 | 내부만 |
| `libraries` | 도서관 | ✓ |
| `library_constitution_versions` | 운영 헌법 버전 | ✓ |
| `library_references` | 도서관 참조 | ✓ |
| `sources` | 공통 원본 | ✓ |
| `source_versions` | 원본 버전 | ✓ |
| `library_sources` | 도서관-원본 연결 | 포함 (importance, trust) |
| `ingest_jobs` | Ingest 작업 | ✓ |
| `ingest_items` | Ingest 항목 | ✓ |
| `job_questions` | Ingest 질문 | ✓ |
| `pages` | 페이지 | ✓ |
| `page_versions` | 페이지 버전 | ✓ |
| `claims` | 주장 | ✓ |
| `evidences` | 근거 | ✓ |
| `page_relations` | 페이지 관계 | ✓ |
| `contradictions` | 모순 | ✓ |
| `change_sets` | 변경 세트 | ✓ |
| `change_items` | 변경 항목 | ✓ |
| `change_reviews` | 변경 검토 | 포함 (decisions) |
| `library_versions` | 도서관 버전 | ✓ |
| `conversations` | 대화 | ✓ |
| `messages` | 메시지 | ✓ |
| `message_citations` | 메시지 인용 | ✓ |
| `lint_runs` | Lint 실행 | ✓ |
| `lint_findings` | Lint 발견 | ✓ |
| `feedback_events` | 피드백 | 내부만 |
| `notifications` | 알림 | ✓ |
| `usage_ledger` | 사용량 원장 | ✓ (집계) |
| `subscriptions` | 구독 | ✓ (plan) |
| `audit_logs` | 감사 로그 | 제한 (관리자만) |
| `page_publications` | 공개 페이지 | ✓ (Phase 3) |
| `page_forks` | 포크 | ✓ (Phase 3) |
| `edit_proposals` | 편집 제안 | ✓ (Phase 3) |
| `content_reports` | 콘텐츠 신고 | ✓ (Phase 3) |
| `content_embeddings` | 임베딩 | 내부만 |

**소계:** 36개 엔터티 중 32개 Phase 1~3에서 API 노출

### 1.3 API 리소스와 ERD 매핑

#### 인증 & 계정 (섹션 3)

| API 경로 | 메서드 | ERD 대상 | 정합성 |
| --- | --- | --- | --- |
| `/auth/sign-up` | POST | users, auth_sessions | ✓ |
| `/auth/login` | POST | auth_sessions | ✓ |
| `/auth/refresh` | POST | auth_sessions | ✓ |
| `/auth/logout` | POST | auth_sessions (revoked_at) | ✓ |
| `/auth/logout-all` | POST | auth_sessions | ✓ |
| `/me` | GET | users | ✓ |
| `/me` | PATCH | users | ✓ |
| `/me/deletion` | POST | users (status, deletion_requested_at) | ✓ |
| `/me/deletion` | DELETE | users (status) | ✓ |

#### 도서관 (섹션 4)

| API 경로 | 메서드 | ERD 대상 | 정합성 |
| --- | --- | --- | --- |
| `/libraries` | POST | libraries, library_constitution_versions | ✓ |
| `/libraries` | GET | libraries | ✓ |
| `/libraries/{libraryId}` | GET | libraries | ✓ |
| `/libraries/{libraryId}` | PATCH | libraries | ✓ |
| `/libraries/{libraryId}/archive` | POST | libraries (status) | ✓ |
| `/libraries/{libraryId}/restore` | POST | libraries (status) | ✓ |
| `/libraries/{libraryId}/delete` | DELETE | libraries (status) | ✓ |
| `/libraries/{libraryId}/constitution` | GET | library_constitution_versions | ✓ |
| `/libraries/{libraryId}/constitution` | PUT | library_constitution_versions | ✓ |
| `/libraries/{libraryId}/constitution/versions` | GET | library_constitution_versions | ✓ |
| `/libraries/{libraryId}/references` | GET | library_references | ✓ |
| `/libraries/{libraryId}/references` | POST | library_references | ✓ |
| `/libraries/{libraryId}/references/{targetLibraryId}` | DELETE | library_references | ✓ |

#### 원본 (섹션 5)

| API 경로 | 메서드 | ERD 대상 | 정합성 |
| --- | --- | --- | --- |
| `/sources` | GET | sources, source_versions | ✓ |
| `/sources/{sourceId}` | GET | sources, source_versions | ✓ |
| `/sources/{sourceId}/versions` | GET | source_versions | ✓ |
| `/sources/{sourceId}/versions/{versionNo}/content` | GET | source_versions | ✓ |
| `/sources/{sourceId}` | PATCH | sources (title) | ✓ |
| `/sources/{sourceId}` | DELETE | sources (status) | ✓ |
| `/uploads` | POST | source_versions (storage_key 준비) | ✓ |
| `/uploads/{uploadId}/complete` | POST | sources, source_versions | ✓ |
| `/web-clips` | POST | sources, source_versions | ✓ |

#### Ingest (섹션 6)

| API 경로 | 메서드 | ERD 대상 | 정합성 |
| --- | --- | --- | --- |
| `/libraries/{libraryId}/ingest-jobs` | POST | ingest_jobs | ✓ |
| `/libraries/{libraryId}/ingest-jobs` | GET | ingest_jobs | ✓ |
| `/libraries/{libraryId}/ingest-jobs/{jobId}` | GET | ingest_jobs, ingest_items (plan) | ✓ |
| `/libraries/{libraryId}/ingest-jobs/{jobId}/items` | GET | ingest_items | ✓ |
| `/libraries/{libraryId}/ingest-jobs/{jobId}/plan-approval` | POST | ingest_jobs (status) | ✓ |
| `/libraries/{libraryId}/ingest-jobs/{jobId}/questions` | GET | job_questions | ✓ |
| `/libraries/{libraryId}/ingest-jobs/{jobId}/answers` | POST | job_questions (answer, status) | ✓ |
| `/libraries/{libraryId}/ingest-jobs/{jobId}/cancel` | POST | ingest_jobs (status) | ✓ |
| `/libraries/{libraryId}/ingest-jobs/{jobId}/retry` | POST | ingest_items (retry_count) | ✓ |

#### 페이지와 지식 객체 (섹션 7)

| API 경로 | 메서드 | ERD 대상 | 정합성 |
| --- | --- | --- | --- |
| `/libraries/{libraryId}/pages` | GET | pages, page_versions | ✓ |
| `/libraries/{libraryId}/pages/{pageId}` | GET | pages, page_versions, claims | ✓ |
| `/libraries/{libraryId}/pages/by-slug/{slug}` | GET | pages | ✓ |
| `/libraries/{libraryId}/pages/{pageId}/versions` | GET | page_versions | ✓ |
| `/libraries/{libraryId}/pages/{pageId}/versions/{versionNo}` | GET | page_versions | ✓ |
| `/libraries/{libraryId}/pages/{pageId}/diff` | GET | page_versions | ✓ |
| `/libraries/{libraryId}/pages/{pageId}/edit-proposals` | POST | change_sets, change_items | ✓ |
| `/libraries/{libraryId}/pages/{pageId}/archive` | POST | change_sets, change_items | ✓ |
| `/libraries/{libraryId}/claims/{claimId}` | GET | claims, evidences | ✓ |
| `/libraries/{libraryId}/claims/{claimId}/evidences` | GET | evidences, sources | ✓ |
| `/libraries/{libraryId}/claims/{claimId}/status-proposals` | POST | change_sets, change_items | ✓ |
| `/libraries/{libraryId}/relations` | GET | page_relations | ✓ |
| `/libraries/{libraryId}/contradictions` | GET | contradictions | ✓ |
| `/libraries/{libraryId}/contradictions/{contradictionId}` | GET | contradictions, claims | ✓ |
| `/libraries/{libraryId}/contradictions/{contradictionId}/resolution` | POST | change_sets, change_items | ✓ |
| `/libraries/{libraryId}/relations/{relationId}/feedback` | POST | feedback_events | ✓ |

#### 변경 검토 & 버전 (섹션 8)

| API 경로 | 메서드 | ERD 대상 | 정합성 |
| --- | --- | --- | --- |
| `/libraries/{libraryId}/change-sets` | GET | change_sets | ✓ |
| `/libraries/{libraryId}/change-sets/{changeSetId}` | GET | change_sets, change_items | ✓ |
| `/libraries/{libraryId}/change-sets/{changeSetId}/items` | GET | change_items | ✓ |
| `/libraries/{libraryId}/change-sets/{changeSetId}/reviews` | POST | change_reviews | ✓ |
| `/libraries/{libraryId}/change-sets/{changeSetId}/apply` | POST | change_sets (status), library_versions | ✓ |
| `/libraries/{libraryId}/change-sets/{changeSetId}/recalculate` | POST | change_sets (base_library_version_id) | ✓ |
| `/libraries/{libraryId}/versions` | GET | library_versions | ✓ |
| `/libraries/{libraryId}/versions/{versionNo}` | GET | library_versions | ✓ |
| `/libraries/{libraryId}/versions/diff` | GET | library_versions (버전 비교) | ✓ |

#### 검색 & AI 사서 (섹션 9)

| API 경로 | 메서드 | ERD 대상 | 정합성 |
| --- | --- | --- | --- |
| `/libraries/{libraryId}/search` | GET | pages, claims, sources, content_embeddings | ✓ |
| `/libraries/{libraryId}/search-expansions` | POST | library_references (확장 검색) | ✓ |
| `/libraries/{libraryId}/conversations` | POST | conversations | ✓ |
| `/libraries/{libraryId}/conversations` | GET | conversations | ✓ |
| `/libraries/{libraryId}/conversations/{conversationId}` | GET | messages | ✓ |
| `/libraries/{libraryId}/conversations/{conversationId}/messages` | POST | messages | ✓ |
| `/libraries/{libraryId}/conversations/{conversationId}/messages/{messageId}` | GET | messages, message_citations | ✓ |
| `/libraries/{libraryId}/messages/{messageId}/save-proposal` | POST | change_sets, change_items | ✓ |
| `/libraries/{libraryId}/messages/{messageId}/feedback` | POST | feedback_events | ✓ |

#### Lint (섹션 10)

| API 경로 | 메서드 | ERD 대상 | 정합성 |
| --- | --- | --- | --- |
| `/libraries/{libraryId}/lint-runs` | POST | lint_runs | ✓ |
| `/libraries/{libraryId}/lint-runs` | GET | lint_runs | ✓ |
| `/libraries/{libraryId}/lint-runs/{lintRunId}` | GET | lint_runs | ✓ |
| `/libraries/{libraryId}/lint-runs/{lintRunId}/findings` | GET | lint_findings | ✓ |
| `/libraries/{libraryId}/lint-findings/{findingId}/decision` | POST | lint_findings (status) | ✓ |
| `/libraries/{libraryId}/lint-findings/{findingId}/change-proposal` | POST | change_sets, change_items | ✓ |

#### 알림과 활동 (섹션 11)

| API 경로 | 메서드 | ERD 대상 | 정합성 |
| --- | --- | --- | --- |
| `/notifications` | GET | notifications | ✓ |
| `/notifications/unread-count` | GET | notifications (read_at) | ✓ |
| `/notifications/{notificationId}/read` | POST | notifications (read_at) | ✓ |
| `/notifications/read-all` | POST | notifications (read_at) | ✓ |
| `/libraries/{libraryId}/activities` | GET | audit_logs | ✓ |

#### 과금 (섹션 12)

| API 경로 | 메서드 | ERD 대상 | 정합성 |
| --- | --- | --- | --- |
| `/billing/plan` | GET | subscriptions | ✓ |
| `/billing/usage` | GET | usage_ledger (집계) | ✓ |
| `/billing/usage/events` | GET | usage_ledger | ✓ |
| `/billing/checkout-sessions` | POST | subscriptions (외부 결제) | ✓ |
| `/billing/portal-sessions` | POST | subscriptions (외부 결제) | ✓ |

#### 내보내기 (섹션 13)

| API 경로 | 메서드 | ERD 대상 | 정합성 |
| --- | --- | --- | --- |
| `/libraries/{libraryId}/exports` | POST | 전체 (스냅샷) | ✓ |
| `/libraries/{libraryId}/exports/{exportId}` | GET | export_jobs (내부) | ✓ |
| `/libraries/{libraryId}/exports/{exportId}/download` | GET | export_jobs, S3 | ✓ |

#### 공개·포크·편집 제안 (섹션 14, Phase 3)

| API 경로 | 메서드 | ERD 대상 | 정합성 |
| --- | --- | --- | --- |
| `/libraries/{libraryId}/pages/{pageId}/publication-checks` | POST | page_publications (safety_check) | ✓ |
| `/libraries/{libraryId}/pages/{pageId}/publications` | POST | page_publications | ✓ |
| `/libraries/{libraryId}/pages/{pageId}/publication` | PATCH | page_publications | ✓ |
| `/libraries/{libraryId}/pages/{pageId}/publication` | DELETE | page_publications (unpublished_at) | ✓ |
| `/public/pages/{publicSlug}` | GET | page_publications (공개) | ✓ |
| `/public/pages/{publicSlug}/forks` | POST | page_forks | ✓ |
| `/libraries/{libraryId}/forks` | GET | page_forks | ✓ |
| `/libraries/{libraryId}/forks/{forkId}/diff` | GET | page_forks, page_versions | ✓ |
| `/libraries/{libraryId}/forks/{forkId}/update-proposals` | POST | change_sets, change_items | ✓ |
| `/public/pages/{publicSlug}/edit-proposals` | POST | edit_proposals | ✓ |
| `/libraries/{libraryId}/edit-proposals` | GET | edit_proposals | ✓ |
| `/libraries/{libraryId}/edit-proposals/{proposalId}/decision` | POST | edit_proposals (status) | ✓ |
| `/public/pages/{publicSlug}/reports` | POST | content_reports | ✓ |

### 1.4 매핑 정합성 결론

**정합성 평가:** ✓ 양호

- **전체 API 리소스:** 약 80개 엔드포인트
- **매핑 커버율:** 32/36 ERD 엔터티가 API에 노출되거나 내부 지원 (88%)
- **미노출 이유:** 4개는 설계상 내부 전용 (auth_sessions, feedback_events, content_embeddings) 또는 공개 버전 제외

**API에만 있는 리소스:**
- Exports (내부 job 엔터티는 ERD에서 아직 미정)
- Notion Import (Phase 2, ERD 미포함)

**ERD에만 있는 엔터티 (API 미노출):**
- `auth_sessions` - 보안상 내부만 (세션 조회 불필요)
- `feedback_events` - 기본적으로 도서관 학습용 내부 데이터
- `content_embeddings` - 검색 인프라용 내부 데이터

---

## 2. 작업 #6: Ingest·ChangeSet 상태 머신 정합성 검증

### 2.1 ERD 상태 정의 검증

#### Ingest 상태 (ERD 4.4절)

| 상태 | 정의 | 다음 상태 | API 표현 |
| --- | --- | --- | --- |
| `QUEUED` | 작업 생성 | SCANNING | 초기 상태 |
| `SCANNING` | 파일 스캔 중 | PLAN_REVIEW | 진행 중 |
| `PLAN_REVIEW` | 계획 승인 대기 | PROCESSING | nextAction: APPROVE_PLAN |
| `PROCESSING` | 문서 처리 중 | QUESTION_WAITING 또는 CHANGE_REVIEW | 진행 중 |
| `QUESTION_WAITING` | 질문 대기 | PROCESSING | nextAction: ANSWER_QUESTIONS |
| `CHANGE_REVIEW` | 변경 검토 대기 | COMPLETED | nextAction: REVIEW_CHANGES |
| `COMPLETED` | 작업 완료 | - | 종료 상태 |
| `PAUSED_QUOTA` | 처리량 부족 | PROCESSING | nextAction: PURCHASE_QUOTA |
| `CANCELLED` | 사용자 취소 | - | 종료 상태 |
| `FAILED` | 작업 실패 | - | 종료 상태 |

**ERD 정의:** ✓ 상태 머신 명확  
**API 구현:** ✓ API 명세 6.1절과 일치

#### ChangeSet 상태 (ERD 4.6절)

| 상태 | 정의 | 다음 상태 | 역할 |
| --- | --- | --- | --- |
| `DRAFT` | 초안 | READY_FOR_REVIEW | AI/사용자 생성 |
| `READY_FOR_REVIEW` | 검토 준비 | PARTIALLY_APPROVED, APPROVED, REJECTED | 자동/수동 전환 |
| `PARTIALLY_APPROVED` | 부분 승인 | APPROVED, REJECTED | 항목별 승인 진행 |
| `APPROVED` | 전체 승인 | APPLIED | 최종 승인 |
| `REJECTED` | 거절됨 | - | 종료 상태 |
| `APPLIED` | 반영됨 | - | 발행 완료 |
| `SUPERSEDED` | 대체됨 | - | 충돌로 인한 자동 전환 |

**ERD 정의:** ✓ 상태 머신 명확  
**API 구현:** ✓ API 명세 8.1절과 일치

### 2.2 상태 전이 경로 검증

#### Ingest 정상 흐름 (기획서 10절)

1. 파일 제공 → QUEUED
2. 스캔 완료 → SCANNING → PLAN_REVIEW
3. 계획 승인 → PROCESSING
4. 문서 처리 → QUESTION_WAITING (선택) → PROCESSING (계속)
5. 처리 완료 → CHANGE_REVIEW
6. 변경 적용 → COMPLETED

**API 매핑:**
```
POST /ingest-jobs (201 SCANNING)
  ↓
GET /ingest-jobs/{jobId} (status: PLAN_REVIEW)
  ↓
POST /ingest-jobs/{jobId}/plan-approval (202 accepted)
  ↓
GET /ingest-jobs/{jobId} (status: PROCESSING)
  ↓
GET /ingest-jobs/{jobId}/questions (nextAction: ANSWER_QUESTIONS)
  ↓
POST /ingest-jobs/{jobId}/answers (202 accepted)
  ↓
GET /ingest-jobs/{jobId} (status: CHANGE_REVIEW)
  ↓
POST /change-sets/{changeSetId}/apply (202 accepted, libraryVersion 생성)
```

**정합성:** ✓ 양호

#### ChangeSet 정상 흐름 (기획서 11절)

1. AI/사용자 생성 → DRAFT
2. 검토 준비 → READY_FOR_REVIEW
3. 항목별 승인 → PARTIALLY_APPROVED (또는 직접 APPROVED)
4. 최종 승인 → APPROVED
5. 반영 → APPLIED (library_versions 생성)

**API 매핑:**
```
변경 원본 (Ingest, 직접 편집 등)
  ↓
POST /change-sets/{changeSetId}/reviews (approval decisions)
  ↓
GET /change-sets/{changeSetId} (status: APPROVED)
  ↓
POST /change-sets/{changeSetId}/apply (libraryVersion 생성)
```

**정합성:** ✓ 양호

### 2.3 상태 정의 미확정 사항

**미확정:** 기획서 11절 "일괄 승인"과 "개별 승인" 분류의 자동화 기준

- ERD에는 `risk_level` (SAFE, REVIEW, HIGH)이 있으나
- API 명세 8.1절 "안전한 항목 일괄 승인" 요청에서 자동 판정 로직 미정

**영향:**
- Ingest 완료 후 자동 일괄 승인 범위 결정 필요
- risk_level 판정 기준 확정 필요

**권고:** 기획서와 API 명세를 함께 업데이트해 risk_level 판정 기준을 명시할 것

---

## 3. 작업 #7: 동시성 제어 저장 근거 확인

### 3.1 낙관적 잠금 설계 (API 명세 2.6절)

**API 계약:**
```
If-Match: "7"  (또는)
expectedRevision: 4
```

**ERD 설계:** 
- 엔터티별 revision 필드 있는가? → **미정의**

**문제:** MAJOR

ERD 초안에서 revision 필드를 명시한 엔터티:
- `libraries` - 응답에 `revision: 1` 포함 (API 명세 4.1절)
- `change_sets` - 응답에 `expectedChangeSetRevision` 사용 (API 명세 8.1절)

하지만 ERD 4.2절과 4.6절에서 **revision 또는 version 컬럼이 정의되지 않음**

**필요한 스키마 수정:**

```markdown
#### `libraries` 추가 컬럼

| 컬럼 | 타입 | 제약/설명 |
| --- | --- | --- |
| `revision` | bigint | 낙관적 잠금용 버전 (PATCH 등 수정 시 증가) |
| `updated_at` | timestamptz | 수정 시각 |
```

```markdown
#### `change_sets` 추가 컬럼

| 컬럼 | 타입 | 제약/설명 |
| --- | --- | --- |
| `revision` | bigint | 검토·적용 시 증가 |
```

```markdown
#### `library_constitution_versions` 추가 컬럼

| 컬럼 | 타입 | 제약/설명 |
| --- | --- | --- |
| `revision` | bigint | 버전 선택 시 증가 |
```

### 3.2 멱등성 키 저장 (API 명세 2.5절)

**API 계약:**
- 생성·명령 작업에 `Idempotency-Key` 헤더 지원
- 24시간 동안 같은 결과 반환
- 다른 본문으로 다시 보내면 `409 IDEMPOTENCY_KEY_REUSED`

**ERD 설계:**
- 멱등성 키 저장 테이블 **미정의**

**필요한 스키마 (신규 엔터티):**

```markdown
#### `idempotency_records`

| 컬럼 | 타입 | 제약/설명 |
| --- | --- | --- |
| `id` | uuid | PK |
| `user_id` | uuid | FK → `users.id` |
| `idempotency_key` | varchar(255) | UNIQUE(`user_id`, `idempotency_key`) |
| `request_path` | text | 엔드포인트 경로 |
| `request_body_hash` | char(64) | SHA-256 (요청 본문) |
| `response_status` | integer | 응답 상태 코드 |
| `response_body` | jsonb | 응답 본문 저장 |
| `created_at` | timestamptz | 생성 시각 |
| `expires_at` | timestamptz | 24시간 후 자동 삭제 대상 |

인덱스: (`user_id`, `idempotency_key`), (`expires_at`)
```

**심각도:** MAJOR  
**근거:** API 명세 2.5절에 명시되어 있으나 ERD에 저장 구조 없음

### 3.3 버전 충돌 감지 (API 명세 8.1절)

**요청 예:**
```json
{
  "expectedChangeSetRevision": 5,
  "expectedLibraryVersion": 12,
  "summary": "..."
}
```

**필요한 검증:**
1. Change Set revision이 `expectedChangeSetRevision`과 일치
2. 현재 Library Version이 `expectedLibraryVersion`과 일치
3. 불일치 시 → `409 PLAN_CHANGED` 또는 `VERSION_CONFLICT`

**ERD 근거:** ✓ `change_sets.base_library_version_id`와 `library_versions` 존재

**정합성:** ✓ 양호

---

## 4. 작업 #8: 인덱스와 쿼리 패턴이 API 필터를 지탱하는지 검증

### 4.1 API 주요 필터 목록

#### 도서관 목록 (API 4.1절)

```
GET /libraries?owner_id=...&status=...&updated_at DESC
```

**ERD 인덱스 (4.2절):**
- `(owner_id, status, updated_at DESC)` ✓

**정합성:** ✓ 양호

#### 원본 조회 (API 5.1절)

```
GET /sources?type=WEB&libraryId=<uuid>&q=consensus&limit=20
```

**필요한 쿼리:**
```sql
SELECT * FROM sources 
WHERE owner_id = :user_id 
  AND source_type = :type
ORDER BY updated_at DESC
LIMIT 20;
```

**라이브러리 필터링:**
```sql
SELECT DISTINCT s.* FROM sources s
JOIN library_sources ls ON s.id = ls.source_id
WHERE ls.library_id = :library_id
  AND s.source_type = :type
ORDER BY s.updated_at DESC;
```

**ERD 인덱스:**
- `sources`: `(owner_id)`, `(source_type)` → **미정의**
- `library_sources`: `(library_id, source_id)` → **미정의**

**필요한 인덱스:**

| 테이블 | 인덱스 | 용도 |
| --- | --- | --- |
| `sources` | `(owner_id, source_type, updated_at DESC)` | 사용자 내 필터·정렬 |
| `sources` | `(content_hash)` | ✓ 이미 정의 |
| `library_sources` | `(library_id, linked_at DESC)` | 도서관 내 원본 조회 |
| `library_sources` | `(source_id)` | 원본 삭제 시 참조 확인 |

**심각도:** MAJOR

#### 페이지 목록 (API 7.1절)

```
GET /libraries/{libraryId}/pages?type=CONCEPT&status=PUBLISHED&topic=consensus&q=raft
```

**필요한 쿼리:**
```sql
SELECT p.* FROM pages p
JOIN page_versions pv ON p.current_version_id = pv.id
WHERE p.library_id = :library_id
  AND p.page_type = :type
  AND p.status = :status
ORDER BY p.updated_at DESC;
```

**텍스트 검색:**
- `page_versions.markdown_body`에 전문 검색 → `tsvector` 생성 컬럼 필요

**ERD 인덱스:**
- `pages`: `(library_id, page_type, status)` → **미정의**
- `page_versions`: `tsvector` → **미정의 (검색·임베딩 5절 참조)**

**필요한 인덱스:**

| 테이블 | 인덱스 | 용도 |
| --- | --- | --- |
| `pages` | `(library_id, page_type, status, updated_at DESC)` | 목록 필터·정렬 |
| `page_versions` | `GIN(tsvector_column)` | 전문 검색 |

**심각도:** MAJOR

#### 변경 세트 목록 (API 8.1절)

```
GET /libraries/{libraryId}/change-sets?status=READY_FOR_REVIEW&riskLevel=HIGH
```

**ERD 인덱스:**
- `change_sets`: `(library_id, status, risk_level)` → **미정의**

**필요한 인덱스:**

| 테이블 | 인덱스 |
| --- | --- |
| `change_sets` | `(library_id, status, risk_level, created_at DESC)` |

**심각도:** MAJOR

#### Lint 실행 (API 10절)

```
GET /libraries/{libraryId}/lint-runs?trigger_type=POST_INGEST
```

**ERD 인덱스:**
- `lint_runs`: `(library_id, trigger_type)` → **미정의**

**필요한 인덱스:**

| 테이블 | 인덱스 |
| --- | --- |
| `lint_runs` | `(library_id, status, created_at DESC)` |

**심각도:** MAJOR

#### 대화 메시지 (API 9.2절)

```
GET /libraries/{libraryId}/conversations/{conversationId}/messages
```

**ERD 인덱스:**
- `messages`: `(conversation_id, created_at)` → **미정의**

**필요한 인덱스:**

| 테이블 | 인덱스 |
| --- | --- |
| `messages` | `(conversation_id, created_at)` |

**심각도:** MAJOR

### 4.2 관계 조회와 JOIN 성능 (API 7.3절)

```
GET /libraries/{libraryId}/relations?pageId=<uuid>&status=ACCEPTED&since=2026-07-01
```

**필요한 쿼리:**
```sql
SELECT * FROM page_relations
WHERE library_id = :library_id
  AND (source_page_id = :page_id OR target_page_id = :page_id)
  AND status = :status
  AND created_at >= :since
ORDER BY created_at DESC;
```

**ERD 인덱스:**
- `page_relations`: `(library_id, source_page_id, status)`, `(library_id, target_page_id, status)` → **미정의**

**필요한 인덱스:**

| 테이블 | 인덱스 |
| --- | --- |
| `page_relations` | `(library_id, source_page_id, status, created_at DESC)` |
| `page_relations` | `(library_id, target_page_id, status, created_at DESC)` |

**심각도:** MAJOR

### 4.3 의미 검색과 임베딩 (API 9.1절)

```
GET /libraries/{libraryId}/search?mode=SEMANTIC
```

**필요한 쿼리:**
```sql
SELECT * FROM content_embeddings
WHERE owner_id = :user_id
  AND library_id = :library_id
  AND embedding <-> :query_vector < :distance_threshold
ORDER BY embedding <-> :query_vector
LIMIT 20;
```

**ERD 인덱스 (5절):**
- `content_embeddings`: `embedding` → `ivfflat` 또는 `hnsw` 벡터 인덱스 필요

**ERD 정의:** ✓ `embedding` 컬럼 있으나 인덱스 타입 미명시

**필요한 스키마:**
```markdown
#### `content_embeddings` 인덱스

| 인덱스 | 타입 | 용도 |
| --- | --- | --- |
| `(owner_id, library_id, content_type)` | B-tree | 필터 |
| `(embedding)` | ivfflat 또는 hnsw | 의미 검색 (pgvector) |
```

**권고:** pgvector 임베딩 모델 확정 후 벡터 인덱스 유형(ivfflat vs hnsw) 결정

**심각도:** MINOR (구현 단계에서 결정 가능)

### 4.4 감사 로그와 활동 (API 11절)

```
GET /libraries/{libraryId}/activities
```

**필요한 쿼리:**
```sql
SELECT * FROM audit_logs
WHERE library_id = :library_id
ORDER BY created_at DESC
LIMIT 20;
```

**ERD 인덱스:**
- `audit_logs`: `(library_id, created_at DESC)` → **미정의**

**필요한 인덱스:**

| 테이블 | 인덱스 |
| --- | --- |
| `audit_logs` | `(library_id, created_at DESC)` |
| `audit_logs` | `(actor_user_id, created_at DESC)` (선택) |

**심각도:** MAJOR

### 4.5 사용량 원장 집계 (API 12절)

```
GET /billing/usage?period=2026-07
```

**필요한 쿼리:**
```sql
SELECT operation_type, SUM(billable_units), SUM(provider_cost)
FROM usage_ledger
WHERE user_id = :user_id
  AND DATE_TRUNC('month', occurred_at) = :period
GROUP BY operation_type;
```

**ERD 인덱스:**
- `usage_ledger`: `(user_id, occurred_at)` → **미정의**

**필요한 인덱스:**

| 테이블 | 인덱스 |
| --- | --- |
| `usage_ledger` | `(user_id, occurred_at DESC)` |

**심각도:** MAJOR

### 4.6 인덱스 요약과 권고

**ERD 초안 인덱스 현황:**
- 정의됨: 3개 (libraries, sources content_hash, pages partial UNIQUE)
- 필요하나 미정의: 15개 이상

**권고:**
1. 모든 FK 외래키에는 기본적으로 인덱스 추가
2. 목록 조회 필터별로 복합 인덱스 정의
3. 정렬이 필요한 경우 created_at/updated_at를 인덱스 마지막에 포함
4. 벡터 검색은 pgvector 모델 확정 후 인덱스 타입 결정
5. 전문 검색은 `tsvector` 생성 컬럼 추가

---

## 5. 종합 정합성 결론

### 5.1 발견 사항 요약

| 작업 | 심각도 | 상태 | 영향 |
| --- | --- | --- | --- |
| #5 양방향 매핑 | - | ✓ 양호 | API 구현 가능 |
| #6 상태 머신 | MINOR | ⚠ 미확정 | risk_level 판정 기준 필요 |
| #7 동시성 제어 | MAJOR | ❌ 불일치 | revision 필드 추가 필요 |
| #8 인덱스·쿼리 | MAJOR | ❌ 불일치 | 15개 이상 인덱스 정의 필요 |

### 5.2 구현 차단 사항

**BLOCKER 없음** - 모든 불일치는 ERD 업데이트로 해결 가능

**MAJOR (즉시 수정 필요):**
1. revision 필드 추가 (libraries, change_sets, library_constitution_versions)
2. idempotency_records 엔터티 신규 추가
3. 복합 인덱스 15개 이상 정의

---

## 6. 다음 단계

1. ERD 초안 수정 제안 정리 (별도 섹션)
2. API 명세 미확정 계약 10건 권고안 (작업 #9)
3. 검증 문서 최종화 및 팀 공유

