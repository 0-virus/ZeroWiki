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

---

## 7. 기술 결정 9건 구현 관점 권고안 (2026-07-30)

기준: `docs/검증-QA-계약-확정-체크리스트.md` 2.2절 qa 권고, `docs/ZeroWiki-요구사항-정의서.md` 12절 UD 정본. 각 항목은 ERD 설계에 대고 **실제 구현 가능 여부와 되돌리기 비용**을 검증한다. 모든 권고는 **권고(미확정)** 표기이며, 리더 확정 전 최종이 아니다.

### 7.1 UD-12: 페이지 버전별 Claim 복제 vs 재사용 ⚠️ 권고(미확정) — 보류

**qa 권고:** 변경되지 않은 Claim은 상태만 참조, 새로운 주장만 생성 (**재사용**)

**현재 상태 분석:**
- qa 권고는 **재사용** (claim 행은 하나, 버전마다 상태만 참조)
- 현재 ERD: **복제** 방식 (claims.page_version_id FK가 버전 종속 의미)

**ERD 설계 문제:**
- `claims.page_version_id` → `page_versions.id` (행 383)
- 이 구조는 **Claim이 특정 버전에 종속**됨을 의미 = 복제 방식
- 재사용하려면 이 FK가 없어야 하고, 대신 **연결 테이블** 필요

**재사용 구현을 위한 ERD 수정안:**

| 테이블 | 변경 사항 |
| --- | --- |
| `claims` | `page_version_id` FK **제거**. `claim_key`, `statement`, `confidence`만 저장 (버전 무관) |
| `claim_statuses` (신규) | Claim의 버전별 상태 저장: `claim_id` FK, `page_version_id` FK, `knowledge_status`, `valid_from`, `valid_to`, `conditions`, `created_at` |
| `evidences` | `claim_id` FK 유지 (Claim 기준으로 근거 저장, 버전 무관) |

**claim_key 생성 규칙 (명시 필수):**

**리더 지적:** "텍스트 해시면 표현 수정도 새 주장이 된다"

→ **규칙 수정:** claim_key는 **의미 정규화** 기반
- `claim_key = blake3(intent_hash(statement))`
- intent_hash: 문법적 동의어·표현 다양성은 무시, 의미 변화만 감지
- 예: "신경망은 생물학적 모델이다" ≈ "생물학 모델: 신경망" → **같은 claim_key**
- 예: "신경망은 생물학적 모델이다" vs "신경망은 생물학적 모델이 아니다" → **다른 claim_key**

**되돌리기 비용:** 🔴 **매우 높음**
- Phase 1에 재사용 구조로 구현하면 행 구조 완성
- Phase 1에 복제로 구현하면 (현재 ERD) 버전 이력 재구성 불가능 → Phase 2 마이그레이션 시 모든 claim_status 역산 필요

**backend 권고 (qa 권고 수용):** ⚠️ **재사용 방식 선택 권고**
- 저장 공간, 버전 간 이력 추적, Phase 2 확장성에서 우수
- **但:** ERD 4.5절 `claims` 테이블 구조 재설계 필수
  - `page_version_id` FK 제거
  - `claim_statuses` 연결 테이블 신규 추가
  - `evidences.claim_id`는 유지 (버전 무관)
- claim_key 생성 규칙: **의미 정규화 기반** 명시 필수

**근거:**
- ERD 4.5절 378~392행 (claims, claim_statuses 구조)
- qa 권고 `docs/검증-QA-계약-확정-체크리스트.md` 2.2절 46행

---

### 7.2 UD-13: 도서관 버전 과거 조회 (이벤트 재생 vs 스냅샷) ✅ 권고(미확정)

**qa 권고:** 초기는 이벤트 재생(change_set 순차 적용), 데이터 커질 시 스냅샷 추가

**ERD 설계 확인:**
- `library_versions` 테이블 — 4.6절 495~506행
  - `change_set_id` (UNIQUE) — 이 버전을 만든 변경 세트 지정 — 502행
  - 설명: "해당 버전에서 생성된 불변 page_versions와 change_set을 통해 상태를 재구성한다" — 507행
- `change_sets` 테이블 — 4.6절 445~462행
  - `status`: APPLIED 상태 포함 — 456행
- **설계 평가:** ✅ 이벤트 재생 기반 가능

**구현 메커니즘:**
1. 특정 library_version 조회 요청 → base_library_version_id로부터 시작
2. 해당 버전 이후의 모든 change_sets (status = APPLIED) 역순 탐색하지 않고 **해당 버전 생성 시점의 change_set까지만 앞으로 재생**
3. 각 change_item 적용하며 최종 상태 구성
4. **스냅샷 추가 시:** library_versions에 `snapshot_data` (jsonb) 추가, 초기화만 함

**필요 추가 사항:**
- **change_sets 영구 보존 정책 재확인** ← 리더 지적 항목

  **ERD 7절 권장 삭제 정책 확인:**
  - "변경·감사 기록: 계정 보존 기간 동안 유지" — 7절 796행
  - 기획서 18절(보안) 검증 필요
  
  **결론:** ✅ 충돌 없음
  - 이벤트 재생은 change_sets을 계속 참조해야 하므로, "계정 보존 기간"동안 보존은 설계와 일치
  - 계정 완전 삭제 시에만 change_sets도 함께 삭제되므로 정책 일관성 있음

**되돌리기 비용:** 🔴 **매우 높음**
- Phase 1에 이벤트 재생으로 설계하고 history 쿼리를 구현하면
- Phase 2에서 스냅샷으로 마이그레이션할 때:
  - 기존의 모든 library_version 데이터에 대해 스냅샷 백필 필요
  - 큰 도서관의 경우 재생 시간 초과 → 스냅샷 필수
  - 오프라인 배치 작업 필요 (다운타임 위험)

**backend 권고:** ✅ **qa 권고 동의 + 추가 권고**
- qa 권고 그대로 진행: 초기 이벤트 재생
- **추가:** Phase 0 벤치마크에서 "과거 버전 재생 시간" 측정 필수
  - 도서관당 500+ change_sets 상황에서 응답 시간 체크
  - 1초 초과 → Phase 2 스냅샷 추가 필수 명시

**근거:** ERD 4.6절 495~507행, 7절 796행, 기획서 18절 (별도 검증)

---

### 7.3 UD-08: 공개 URL slug 충돌·변경 정책 ✅ 권고(미확정)

**qa 권고:** 충돌 시 숫자 suffix 추가, 변경 불가(새로 발행)

**ERD 설계 확인 (4.9절):**
- `page_publications.public_slug` — UNIQUE 제약 — 677행
- **설계 평가:** ✅ 구현 가능

**구현 메커니즘:**
- 발행 요청 시 slug 중복 감지 → application layer에서 suffix(`-2`, `-3`, ...) 추가
- `page_publications.unpublished_at` null이면 활성 → 684행
- slug 변경 시 새 page_publications 행 생성 (기존은 unpublished_at 기록)

**필요 추가 사항:** 없음 (UNIQUE 제약이 충분)

**되돌리기 비용:** 🟢 **낮음**
- UNIQUE 제약은 database 레벨, slug suffix 규칙은 application 로직
- Phase 2에서 변경 가능

**backend 권고:** ✅ **qa 권고 동의**

---

### 7.4 UD-09: 관리자·운영 API 분리 여부 ✅ 권고(미확정)

**qa 권고:** 별도 tag `Admin` 사용, 공개 spec에 포함. Phase 1에는 관리자 API 없음

**ERD 설계 확인:**
- 스키마 레벨 영향 없음 (권한은 application layer)
- **설계 평가:** ✅ 정책 결정만 필요

**backend 권고:** ✅ **qa 권고 동의**
- Phase 1 MVP에는 관리자 기능 불필요
- Phase 2에서 audit_logs API 추가 시 함께 설계

---

### 7.5 UD-15: 구조화 헌법의 JSON Schema ✅ 권고(미확정)

**qa 권고:** Phase 0 프로토타입 이후 확정

**ERD 설계 확인 (4.2절):**
- `library_constitution_versions.constitution_document` (jsonb) — 저장 가능 — ERD 4.2절 참조
- **설계 평가:** ✅ 스키마 준비 완료

**backend 권고:** ✅ **qa 권고 동의**
- Phase 0에서 12개 필드(기획서 6.3절)에 대한 prototype JSON 작성
- 기획서 상 필드 목록과 실제 운영 헌법 인스턴스 샘플 필요

---

### 7.6 UD-16: 관계 유형 초기 목록·사용자 정의 허용 시점 ✅ 권고(미확정)

**qa 권고:** 초기 고정 7개, Phase 2에서 사용자 정의 추가

**ERD 설계 확인 (4.5절):**
- `page_relations.relation_type` (varchar(40)) — 예시: RELATED_TO, PART_OF, DEPENDS_ON, CAUSES, CONTRASTS_WITH, EXPLAINS, APPLIES_TO — 414행
- enum 예시 7개 제공
- **설계 평가:** ✅ 구현 가능 (고정 or 확장)

**backend 권고:** ✅ **qa 권고 동의**
- Phase 1: enum 고정 (database enum type 또는 CHECK 제약)
- Phase 2: relation_type을 별도 테이블(relation_types)로 분리, FK 참조로 전환

---

### 7.7 UD-18: Notion Import 외부 ID·중복 판정 ✅ 권고(미확정)

**qa 권고:** 콘텐츠 해시 + 외부 ID 결합

**ERD 설계 확인 (4.3절):**
- `source_versions.content_hash` (char(64) SHA-256) — 247행
- `source_versions.source_metadata` (jsonb) — 외부 ID 저장 가능 — 239행
- `sources.owner_id` (FK) — 사용자 식별 — 213행
- 인덱스: `(content_hash)` 이미 정의 — 5.1절 755행
- **설계 평가:** ✅ 구현 가능

**구현 메커니즘:**
- Notion Import 시 source_metadata에 `{"notion_id": "...", ...}` 저장
- 중복 판정: **같은 사용자 내** `(content_hash, source_metadata.notion_id)` 복합 조건 검사
  - 사용자 A의 notion_id=123 ≠ 사용자 B의 notion_id=123 (다른 Notion 워크스페이스)

**리더 지적:** 인덱스가 전역이면 안 됨. 사용자 계정 내부만 중복 판정 (기획서 6.1, 설계 불변 조건 2번)

**필요 추가 사항 (인덱스 수정):** 
- ❌ **오류:** `(content_hash, source_metadata->>'notion_id')` — 전역 인덱스
- ✅ **수정:** `(owner_id, content_hash, source_metadata->>'notion_id')` — 사용자 내부 범위

**backend 권고:** ✅ **qa 권고 동의 + 인덱스 선두 수정**
- 인덱스를 `(owner_id, content_hash, source_metadata->>'notion_id')`로 정의
- 사용자 계정 내부에서만 중복 탐색 가능
- 근거: 기획서 6.1절 "공통 원본 보관소는 사용자당 하나", 설계 불변 2번

---

### 7.8 UD-21: 월간 Lint 실행 시각·타임존·대상 선정 ⚠️ 권고(미확정) — 보류

**qa 권고:** 기술 팀이 스케줄러 구현 시 확정

**ERD 설계 확인 (4.8절):**
- `lint_runs.trigger_type` (POST_INGEST, MONTHLY, ...) — 562행
- `lint_runs.created_at` — 시간 추적 — 566행
- **설계 평가:** ✅ 스케줄러 구현 후 확정 가능

**리더 지적:** "기본값을 쓰는 모든 사용자의 월간 Lint가 같은 순간에 몰린다" — **부하 분산 필요**

**현재 문제:**
- 모든 사용자가 기본값 "매월 1일 UTC 00:00"을 사용하면:
  - 동시 실행으로 LLM 비용·서버 부하 한꺼번에 급증
  - 1인 개발 조직에서는 비용 폭증 + 장애 위험

**backend 권고:** ⚠️ **부하 분산 포함**

다음 중 하나를 선택:

**옵션 A (권장):** 가입일 기준 분산
```
- 각 사용자의 월간 Lint 실행일 = (가입일의 일) → 월간 분산
- 예: 7월 15일 가입 → 매달 15일 실행
- 장점: 공평한 분산, 구현 단순
- 단점: 사용자가 변경할 수 없음
```

**옵션 B:** 계정 ID 해시 기반 분산
```
- 월간 실행일 = hash(user_id) % 28 (또는 7~31 범위)
- 예: hash(uuid) % 28 = 15 → 매달 15일 실행
- 장점: 균등 분산, 변경 불가능으로 안정적
- 단점: 사용자에게 설명하기 어려움
```

**옵션 C:** 사용자 설정 + 부하 분산
```
- 기본값: 가입일 기준 분산
- 선택: 사용자가 선호 요일/시각 선택 (월~금 선택, 시간대 선택)
- 장점: 유연성 + 분산 효과
- 단점: 구현 복잡, 모두 하루에 선택하면 분산 의미 없음
```

**backend 최종 권고:** **옵션 A 선택** (가입일 기준 분산)
- 단순하고 공평한 부하 분산
- Phase 2에서 옵션 C로 확장 가능

**근거:** 
- ERD 4.8절 556~566행 (lint_runs)
- 기획서 18절 성능·LLM 비용 (조직 역량 한계)

---

### 7.9 UD-23: 알림 보존 기간·읽음 처리 정책 ⚠️ 권고(미확정) — 보류

**qa 권고:** 내부 알림만 지원하므로 누적 규칙 필요

**ERD 설계 확인 (4.8절):**
- `notifications` 테이블 — 4.8절 597~610행
- `read_at` (timestamptz) — 읽은 시각 — 608행
- `type` (varchar(40)) — 알림 종류 — 603행
- **스키마 문제:** ❌ 삭제/아카이브 시각 필드 없음

**리더 지적:** "7일이면 2주 자리한 사용자는 위험 변경 알림을 통째로 놓친다"

**기획서 17절 알림 종류 (기획서 확인 필요):**
- 위험 변경 (FR-CHG-06) — 승인 필요, 시간 결정 중요
- Lint 결과 (FR-LNT-03) — 참고용, 다시 실행 가능
- 포크 업데이트 (FR-PUB-13) — 선택적 추적
- 편집 제안 (FR-PUB-15) — 선택적 검토
- 진행 알림 (FR-ING-25, FR-ASK-05) — 실시간 알림, 재실행 가능
- 삭제 유예 (FR-ACC-07) — 시간 제한 있음, 필수

**알림 종류별 보존 기간 권고:**

| 알림 종류 | 중요도 | 권장 보존 | 근거 |
| --- | --- | --- | --- |
| 위험 변경 | 🔴 높음 | **30일** | 승인 필수, 재실행 불가, 주말 부재 대비 |
| 삭제 유예 | 🔴 높음 | **30일** | FR-ACC-07, 법적 보관 요구 가능 |
| 포크 업데이트 | 🟡 중간 | 14일 | 추적 가능, 재동기 가능 |
| Lint 결과 | 🟡 중간 | 14일 | 재실행 가능 |
| 편집 제안 | 🟡 중간 | 14일 | 제안 재요청 가능 |
| 진행 알림 | 🟢 낮음 | 7일 | 실시간, 재실행 가능 |

**backend 권고:** ⚠️ **ERD 수정 + 정책 명확화 필수**

1. **스키마 수정:**
   - `archived_at` 컬럼 추가
   - 인덱스: `(user_id, archived_at DESC)` (활성 알림 조회)

2. **보존 정책 (비즈니스 로직):**
   - 위험 변경·삭제 유예: 30일
   - 포크·Lint·편집 제안: 14일
   - 진행 알림: 7일
   - 자동 아카이빙: created_at 기준 배치 작업 (매일)

3. **구현 단계:**
   - Phase 1: `archived_at` 필드 추가, 수동 삭제만 지원
   - Phase 2: 자동 아카이빙 배치 추가, 사용자 보존 기간 설정 추가

**근거:**
- ERD 4.8절 597~610행 (notifications 테이블)
- 기획서 17절 (알림 6종)
- FR-CHG-06, FR-ACC-07 (시간 결정 필수)

---

## 8. 총괄 권고안 요약

**리더 확정 (2026-07-30):**
- ✅ 6건 확정: UD-08·09·13·15·16·18
- ⏸️ 3건 보류: UD-12·21·23 (재작성 필요)

| UD | 항목 | 권고 결과 | 상태 | 근거 |
| --- | --- | --- | --- | --- |
| UD-08 | 공개 URL slug 충돌·변경 | ✅ qa 동의 | **확정** | UNIQUE 제약 충분 |
| UD-09 | 관리자 API 분리 | ✅ qa 동의 | **확정** | 스키마 영향 없음 |
| **UD-12** | **Claim 복제 vs 재사용** | ⚠️ 모순 발견 | **보류** | 재사용 권고하면서 복제 설계. ERD 수정안 필요: `claims`에서 `page_version_id` 제거, `claim_statuses` 테이블 추가. claim_key는 의미 정규화 기반. |
| UD-13 | 이벤트 재생 vs 스냅샷 | ✅ qa 동의 | **확정** | change_sets 삭제 정책 일치, Phase 0 성능 측정 조건 추가 |
| UD-15 | JSON Schema | ✅ qa 동의 | **확정** | jsonb 필드 준비 완료 |
| UD-16 | 관계 유형 | ✅ qa 동의 | **확정** | enum 초기 고정, Phase 2 확장 |
| UD-18 | Notion 중복 판정 | ✅ qa 동의 + **인덱스 수정** | **확정** | 인덱스: `(owner_id, content_hash, source_metadata->>'notion_id')` — 사용자 내부만 |
| **UD-21** | **Lint 스케줄** | ⚠️ 부하 분산 누락 | **보류** | 동시 실행 문제. 가입일 기준 분산 추가 필요. |
| **UD-23** | **알림 보존** | ⚠️ 기간 근거 누락 | **보류** | 7일 단일 기간은 부족. 알림 종류별 차등 보존(위험 변경 30일, 진행 7일) 필요. ERD에 `archived_at` 필드 추가. |

---

