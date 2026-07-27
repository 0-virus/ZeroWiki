# ZeroWiki REST API 명세 초안

작성일: 2026-07-27  
상태: MVP API 계약 초안  
기준 문서: `ZeroWiki-MVP-서비스-기획서.md`, `ZeroWiki-ERD-초안.md`

## 1. 목적과 범위

이 문서는 Next.js 클라이언트와 Spring Boot API 서버 사이의 MVP 계약을 정의한다. Phase 1의 개인 도서관 코어를 상세 범위로 삼고 Phase 2·3 API는 경로와 책임을 우선 정의한다.

기본 URL:

```text
/api/v1
```

API 원칙:

1. JSON 필드는 `camelCase`, URL 리소스 이름은 복수형 `kebab-case`를 사용한다.
2. 리소스 ID는 UUID 문자열을 사용한다.
3. 시간은 UTC ISO 8601 형식으로 반환한다.
4. 목록은 cursor pagination을 사용한다.
5. 오래 걸리는 Ingest, Lint, AI 답변, Export는 `202 Accepted`로 작업을 반환한다.
6. 원본과 발행된 버전은 수정 API를 제공하지 않는다.
7. 의미 있는 본문 수정은 즉시 반영하지 않고 변경 세트를 만든다.
8. 모든 도서관 API에서 소유권 또는 공개 권한을 서버가 검증한다.
9. 클라이언트는 LLM 공급자명이나 실제 모델명을 지정하지 않고 제품 처리 모드만 전달한다.

## 2. 공통 규약

### 2.1 인증

웹 MVP는 짧은 수명의 Access Token과 회전 가능한 Refresh Token을 사용한다.

- Access Token: `Authorization: Bearer <token>`
- Refresh Token: `HttpOnly`, `Secure`, `SameSite=Lax` 쿠키 권장
- 공개 페이지와 회원가입·로그인을 제외한 API는 인증 필수
- Access Token에는 최소 `sub`, `sessionId`, `exp`만 포함하고 도서관 권한은 요청 시 확인

### 2.2 성공 응답

단일 리소스:

```json
{
  "data": {
    "id": "4bc2b73d-c207-4b0e-9db9-44251cf79754"
  }
}
```

목록:

```json
{
  "data": [
    {
      "id": "4bc2b73d-c207-4b0e-9db9-44251cf79754"
    }
  ],
  "page": {
    "nextCursor": "eyJ1cGRhdGVkQXQiOiIyMDI2...",
    "hasNext": true
  }
}
```

생성:

- 동기 생성: `201 Created`
- 비동기 작업 생성: `202 Accepted`
- 응답에 생성된 리소스 URL을 `Location` 헤더로 제공

삭제:

- 본문 없는 성공은 `204 No Content`
- 복구 가능한 삭제 예약은 `202 Accepted`와 삭제 예정 정보를 반환

### 2.3 오류 응답

```json
{
  "error": {
    "code": "INGEST_PLAN_NOT_APPROVED",
    "message": "처리 계획 승인이 필요합니다.",
    "requestId": "req_01K1...",
    "details": [
      {
        "field": "processingMode",
        "reason": "지원하지 않는 처리 모드입니다."
      }
    ]
  }
}
```

주요 HTTP 상태:

| 상태 | 사용 |
| --- | --- |
| `400` | 형식 오류, 잘못된 상태 전이 |
| `401` | 인증 없음 또는 만료 |
| `403` | 리소스 접근 권한 없음 |
| `404` | 리소스 없음. 다른 사용자 리소스도 정보 노출 방지를 위해 404 가능 |
| `409` | 중복, 버전 충돌, 이미 처리된 검토 |
| `413` | 파일 또는 Import 크기 제한 초과 |
| `422` | 문법은 맞지만 도메인 검증 실패 |
| `429` | 요청 빈도 또는 AI 처리량 초과 |
| `500` | 내부 오류 |
| `503` | 일시적인 LLM·스토리지 공급자 장애 |

공통 오류 코드:

| 코드 | 의미 |
| --- | --- |
| `VALIDATION_FAILED` | 요청 검증 실패 |
| `RESOURCE_NOT_FOUND` | 리소스 없음 |
| `FORBIDDEN` | 권한 부족 |
| `VERSION_CONFLICT` | 기준 버전과 현재 버전 충돌 |
| `INVALID_STATE_TRANSITION` | 허용되지 않는 작업 상태 변경 |
| `DUPLICATE_SOURCE` | 동일 원본 후보 존재 |
| `SECRET_DETECTED` | 비밀번호·API 키 가능성으로 저장 차단 |
| `QUOTA_EXCEEDED` | AI 처리량 부족 |
| `DEPENDENCY_UNAVAILABLE` | 외부 공급자 장애 |

### 2.4 페이지네이션과 정렬

```text
?limit=20&cursor=<opaque-cursor>&sort=updatedAt,desc
```

- `limit` 기본 20, 최대 100
- cursor는 서버가 생성한 불투명 문자열
- 클라이언트는 cursor 내부 구조에 의존하지 않는다.

### 2.5 멱등성

다음 생성·명령 API는 `Idempotency-Key` 헤더를 지원해야 한다.

- 원본 업로드 완료
- URL 클리핑
- Import/Ingest 시작
- 처리 계획 승인
- 변경 세트 검토·적용
- AI 질문 전송
- Lint와 Export 시작
- 공개·포크

같은 사용자, 같은 경로, 같은 키의 요청은 24시간 동안 같은 결과를 반환한다. 동일 키로 다른 본문을 보내면 `409 IDEMPOTENCY_KEY_REUSED`를 반환한다.

### 2.6 낙관적 잠금

변경 가능한 주요 리소스는 응답에 `revision`을 포함한다. 수정 요청은 다음 중 하나를 사용한다.

```http
If-Match: "7"
```

또는 명령 본문의 `expectedRevision`. 일치하지 않으면 `409 VERSION_CONFLICT`를 반환한다.

### 2.7 비동기 작업 표현

```json
{
  "data": {
    "id": "bd6df40a-acde-4c64-9722-eed57fb2f91e",
    "type": "INGEST",
    "status": "SCANNING",
    "progress": {
      "completed": 12,
      "total": 100,
      "percent": 12
    },
    "nextAction": null,
    "createdAt": "2026-07-27T03:10:00Z"
  }
}
```

`nextAction` 예:

- `APPROVE_PLAN`
- `ANSWER_QUESTIONS`
- `REVIEW_CHANGES`
- `PURCHASE_QUOTA`
- `RETRY_FAILED_ITEMS`

MVP 웹 클라이언트는 polling을 기본으로 한다. 작업 조회 시 `Retry-After`를 반환할 수 있다. 추후 SSE를 추가한다.

## 3. 인증과 계정

### 3.1 엔드포인트

| Method | Path | 설명 | 응답 |
| --- | --- | --- | --- |
| `POST` | `/auth/sign-up` | 회원가입 | `201` |
| `POST` | `/auth/login` | 로그인 | `200` |
| `POST` | `/auth/refresh` | 토큰 갱신 | `200` |
| `POST` | `/auth/logout` | 현재 세션 폐기 | `204` |
| `POST` | `/auth/logout-all` | 전체 세션 폐기 | `204` |
| `GET` | `/me` | 내 계정 조회 | `200` |
| `PATCH` | `/me` | 표시 이름·언어 수정 | `200` |
| `POST` | `/me/deletion` | 계정 삭제 예약 | `202` |
| `DELETE` | `/me/deletion` | 30일 내 삭제 취소 | `204` |

회원가입 요청:

```json
{
  "email": "user@example.com",
  "password": "a-strong-password",
  "displayName": "제로"
}
```

계정 응답:

```json
{
  "data": {
    "id": "5ab02562-efcc-492e-b483-9e6680550146",
    "email": "user@example.com",
    "displayName": "제로",
    "locale": "ko-KR",
    "status": "ACTIVE",
    "plan": "FREE",
    "createdAt": "2026-07-27T03:00:00Z"
  }
}
```

삭제 예약 응답에는 `purgeScheduledAt`을 포함한다.

## 4. 도서관

### 4.1 도서관 CRUD

| Method | Path | 설명 |
| --- | --- | --- |
| `POST` | `/libraries` | 도서관과 초기 운영 헌법 생성 |
| `GET` | `/libraries` | 내 도서관 목록 |
| `GET` | `/libraries/{libraryId}` | 도서관 홈 요약 조회 |
| `PATCH` | `/libraries/{libraryId}` | 이름·설명 수정 |
| `POST` | `/libraries/{libraryId}/archive` | 보관 |
| `POST` | `/libraries/{libraryId}/restore` | 보관 복구 |
| `DELETE` | `/libraries/{libraryId}` | 삭제 예약 |

생성 요청:

```json
{
  "name": "분산 시스템 학습",
  "description": "논문과 기술 문서를 연결해 학습한다.",
  "templateType": "LEARNING_RESEARCH",
  "onboarding": {
    "goal": "분산 시스템의 설계 원리를 이해하고 기술 선택에 활용한다.",
    "sourceTypes": ["PAPER", "TECHNICAL_ARTICLE", "BOOK_NOTE"],
    "knowledgeLevel": "INTERMEDIATE",
    "trustedSources": "원 논문과 공식 문서를 우선한다.",
    "cautionAreas": "벤더 마케팅 자료는 사실과 주장을 구분한다."
  }
}
```

생성 응답의 주요 필드:

```json
{
  "data": {
    "id": "cc9be2b2-acde-4554-b3fd-2599d3f2ad18",
    "name": "분산 시스템 학습",
    "templateType": "LEARNING_RESEARCH",
    "visibility": "PRIVATE",
    "status": "ACTIVE",
    "constitutionVersion": 1,
    "currentVersion": null,
    "counts": {
      "sources": 0,
      "pages": 0,
      "openContradictions": 0,
      "pendingChanges": 0
    },
    "createdAt": "2026-07-27T03:20:00Z",
    "revision": 1
  }
}
```

`GET /libraries/{libraryId}`는 홈 화면을 위해 다음 요약을 포함할 수 있다.

- 주요 주제
- 최근 발견한 연결
- 열린 질문과 지식 공백
- 모순과 검토 대기 변경 수
- 최근 활동

상세 목록은 각각의 전용 API로 조회한다.

### 4.2 운영 헌법

| Method | Path | 설명 |
| --- | --- | --- |
| `GET` | `/libraries/{libraryId}/constitution` | 현재 운영 헌법 |
| `PUT` | `/libraries/{libraryId}/constitution` | 새 운영 헌법 버전 생성 |
| `GET` | `/libraries/{libraryId}/constitution/versions` | 변경 이력 |
| `GET` | `/libraries/{libraryId}/constitution/versions/{versionNo}` | 특정 버전 |

수정 요청:

```json
{
  "expectedVersion": 2,
  "purpose": "분산 시스템 설계와 운영 장애를 함께 학습한다.",
  "audience": "중급 백엔드 개발자",
  "knowledgeLevel": "INTERMEDIATE",
  "sourcePolicy": {
    "priority": ["PRIMARY_RESEARCH", "OFFICIAL_DOCS", "BOOK", "BLOG"],
    "minimumEvidenceForVerifiedFact": 1
  },
  "taxonomyPolicy": {
    "preferredPageTypes": ["CONCEPT", "FLOW", "SYNTHESIS", "QUESTION"]
  },
  "linkPolicy": {
    "minimumConfidence": 0.75,
    "avoidWeakSimilarity": true
  },
  "stalenessPolicy": {
    "defaultReviewDays": 365
  },
  "riskPolicy": {
    "factUpdate": "HIGH",
    "deleteContent": "HIGH"
  },
  "privacyPolicy": {
    "blockSecrets": true
  },
  "naturalLanguageRules": "원 논문과 공식 문서를 우선하고 관점 차이를 모순으로 단정하지 않는다.",
  "changeReason": "운영 장애 사례를 포함하도록 목적을 확장"
}
```

### 4.3 도서관 참조

| Method | Path | 설명 |
| --- | --- | --- |
| `GET` | `/libraries/{libraryId}/references` | 단방향 참조 목록 |
| `POST` | `/libraries/{libraryId}/references` | 참조 추가 |
| `DELETE` | `/libraries/{libraryId}/references/{targetLibraryId}` | 참조 제거 |

요청:

```json
{
  "targetLibraryId": "ae7a4b55-d42c-4ccb-9d0e-f23acfc156f9"
}
```

MVP에서는 동일 사용자 소유 도서관만 연결할 수 있다.

## 5. 공통 원본 보관소

### 5.1 원본 조회

| Method | Path | 설명 |
| --- | --- | --- |
| `GET` | `/sources` | 내 공통 원본 목록 |
| `GET` | `/sources/{sourceId}` | 원본과 최신 버전 메타데이터 |
| `GET` | `/sources/{sourceId}/versions` | 원본 버전 목록 |
| `GET` | `/sources/{sourceId}/versions/{versionNo}` | 특정 버전 메타데이터 |
| `GET` | `/sources/{sourceId}/versions/{versionNo}/content` | 권한 검사 후 원본 다운로드 |
| `PATCH` | `/sources/{sourceId}` | 제목 등 논리 메타데이터 수정 |
| `DELETE` | `/sources/{sourceId}` | 삭제 또는 참조 충돌 반환 |

필터 예:

```text
GET /sources?type=WEB&libraryId=<uuid>&q=consensus&limit=20
```

원본 내용 수정 API는 제공하지 않는다. 같은 URL 재클리핑이나 새 파일 업로드는 `source_versions`를 추가한다.

### 5.2 파일 업로드

큰 파일과 ZIP Import를 고려해 3단계 직접 업로드를 권장한다.

| Method | Path | 설명 |
| --- | --- | --- |
| `POST` | `/uploads` | 업로드 세션과 presigned URL 생성 |
| `POST` | `/uploads/{uploadId}/complete` | 해시·크기 검증 후 원본 버전 확정 |
| `DELETE` | `/uploads/{uploadId}` | 미완료 업로드 취소 |

업로드 세션 요청:

```json
{
  "fileName": "distributed-systems.zip",
  "mediaType": "application/zip",
  "byteSize": 18420831,
  "sha256": "1c8a..."
}
```

완료 요청:

```json
{
  "libraryId": "cc9be2b2-acde-4554-b3fd-2599d3f2ad18",
  "sourceType": "OBSIDIAN",
  "title": "분산 시스템 Obsidian Vault",
  "startIngest": true,
  "processingMode": "STANDARD"
}
```

비밀번호·API 키 가능성이 확인되면 원본 확정을 중단하고 `422 SECRET_DETECTED`를 반환한다. 개인정보·기밀 가능성은 차단 대신 경고 플래그를 반환할 수 있다.

### 5.3 URL 클리핑

| Method | Path | 설명 |
| --- | --- | --- |
| `POST` | `/web-clips` | 공개 URL 한 건 수집 |
| `GET` | `/web-clips/{clipJobId}` | 수집 상태 조회 |

요청:

```json
{
  "libraryId": "cc9be2b2-acde-4554-b3fd-2599d3f2ad18",
  "url": "https://example.com/article",
  "processingMode": "STANDARD",
  "startIngest": true
}
```

서버 정책:

- `http`와 `https`만 허용
- 사설 IP, loopback, link-local 주소 차단으로 SSRF 방어
- 로그인·유료 콘텐츠 우회 금지
- 리디렉션 횟수와 응답 크기 제한
- 같은 URL을 다시 수집하면 기존 행을 덮어쓰지 않고 새 `source_version` 생성
- YouTube 자막을 얻지 못하면 `UNSUPPORTED_NO_TRANSCRIPT`

## 6. Import와 Ingest

### 6.1 작업 생성

| Method | Path | 설명 |
| --- | --- | --- |
| `POST` | `/libraries/{libraryId}/ingest-jobs` | 기존 원본 또는 Import 아카이브 처리 시작 |
| `GET` | `/libraries/{libraryId}/ingest-jobs` | 작업 목록 |
| `GET` | `/libraries/{libraryId}/ingest-jobs/{jobId}` | 작업 상태와 계획 |
| `GET` | `/libraries/{libraryId}/ingest-jobs/{jobId}/items` | 문서별 상태 |
| `POST` | `/libraries/{libraryId}/ingest-jobs/{jobId}/plan-approval` | 처리 계획 승인·수정 |
| `GET` | `/libraries/{libraryId}/ingest-jobs/{jobId}/questions` | 열린 질문 |
| `POST` | `/libraries/{libraryId}/ingest-jobs/{jobId}/answers` | 질문 답변 |
| `POST` | `/libraries/{libraryId}/ingest-jobs/{jobId}/cancel` | 작업 취소 |
| `POST` | `/libraries/{libraryId}/ingest-jobs/{jobId}/retry` | 실패 항목 재시도 |

기존 원본 처리 요청:

```json
{
  "importType": "EXISTING_SOURCES",
  "sourceVersionIds": [
    "b98fe600-a833-4780-8500-a694c17fb878"
  ],
  "processingMode": "STANDARD"
}
```

생성 직후 `202 SCANNING`을 반환한다.

작업 상세 예:

```json
{
  "data": {
    "id": "bd6df40a-acde-4c64-9722-eed57fb2f91e",
    "libraryId": "cc9be2b2-acde-4554-b3fd-2599d3f2ad18",
    "importType": "OBSIDIAN_ZIP",
    "processingMode": "STANDARD",
    "status": "PLAN_REVIEW",
    "progress": {
      "completed": 100,
      "total": 100,
      "percent": 100
    },
    "plan": {
      "documents": 100,
      "processable": 94,
      "duplicates": 3,
      "heldForReview": 3,
      "topicGroups": [
        {
          "name": "합의 알고리즘",
          "count": 28
        }
      ],
      "estimatedTokens": 920000,
      "estimatedBillableUnits": 100
    },
    "nextAction": "APPROVE_PLAN",
    "createdAt": "2026-07-27T04:00:00Z"
  }
}
```

계획 승인:

```json
{
  "decision": "APPROVE",
  "includedItemIds": [
    "c3900cf2-c293-49b0-a8c4-154e0b7596b0"
  ],
  "processingMode": "STANDARD",
  "expectedEstimatedTokens": 920000
}
```

`expectedEstimatedTokens` 또는 계획 revision이 현재 계획과 다르면 `409 PLAN_CHANGED`를 반환해 비용이 바뀐 작업의 묵시적 승인을 방지한다.

질문 답변:

```json
{
  "answers": [
    {
      "questionId": "03f2eb34-1654-4f49-bd1b-4800af40cc78",
      "answer": "이 문서는 Raft와 Paxos의 운영 복잡도를 비교하기 위해 저장했습니다."
    }
  ]
}
```

처리가 끝나면 작업은 `CHANGE_REVIEW`가 되고 `changeSetId`를 반환한다. 승인된 변경 적용과 후속 전체 Lint가 완료되면 최종 `COMPLETED`가 된다.

### 6.2 Notion Import

Phase 2에서 다음 책임으로 추가한다.

| Method | Path | 설명 |
| --- | --- | --- |
| `POST` | `/integrations/notion/connections` | OAuth 연결 시작 |
| `GET` | `/integrations/notion/callback` | OAuth callback |
| `GET` | `/integrations/notion/resources` | 가져올 페이지 선택 |
| `POST` | `/libraries/{libraryId}/notion-imports` | 일회성 Import 시작 |
| `DELETE` | `/integrations/notion/connections/{connectionId}` | 연결 해제 |

지속 동기화 API는 MVP에서 제공하지 않는다.

## 7. 페이지와 지식 객체

### 7.1 페이지

| Method | Path | 설명 |
| --- | --- | --- |
| `GET` | `/libraries/{libraryId}/pages` | 페이지 목록 |
| `GET` | `/libraries/{libraryId}/pages/{pageId}` | 현재 페이지 상세 |
| `GET` | `/libraries/{libraryId}/pages/by-slug/{slug}` | slug로 조회 |
| `GET` | `/libraries/{libraryId}/pages/{pageId}/versions` | 페이지 이력 |
| `GET` | `/libraries/{libraryId}/pages/{pageId}/versions/{versionNo}` | 특정 버전 |
| `GET` | `/libraries/{libraryId}/pages/{pageId}/diff` | 두 버전 비교 |
| `POST` | `/libraries/{libraryId}/pages/{pageId}/edit-proposals` | 사용자 본문 수정 변경 세트 생성 |
| `POST` | `/libraries/{libraryId}/pages/{pageId}/archive` | 보관 변경 세트 생성 |

목록 필터:

```text
GET /libraries/{libraryId}/pages?type=CONCEPT&status=PUBLISHED&topic=consensus&q=raft
```

페이지 상세:

```json
{
  "data": {
    "id": "88e59d1a-7fb2-4f63-b522-bd59d75f8214",
    "libraryId": "cc9be2b2-acde-4554-b3fd-2599d3f2ad18",
    "slug": "raft-consensus",
    "pageType": "CONCEPT",
    "status": "PUBLISHED",
    "version": {
      "id": "b3524f0d-884b-44eb-844b-a9344c68bc57",
      "versionNo": 3,
      "title": "Raft 합의 알고리즘",
      "markdownBody": "# Raft 합의 알고리즘\n...",
      "summary": "Raft의 역할 분리와 로그 복제 과정을 설명한다.",
      "createdAt": "2026-07-27T05:00:00Z"
    },
    "claimSummary": {
      "total": 12,
      "withoutEvidence": 1,
      "openContradictions": 2
    },
    "relatedPages": [
      {
        "pageId": "593fd0a6-dd39-4f21-9898-7877a158dc51",
        "title": "Paxos",
        "relationType": "CONTRASTS_WITH",
        "rationale": "두 알고리즘의 합의 절차와 이해 가능성을 비교한다."
      }
    ]
  }
}
```

본문 수정 요청:

```json
{
  "baseVersionNo": 3,
  "proposedMarkdown": "# Raft 합의 알고리즘\n...",
  "reason": "오탈자를 수정하고 운영상 주의점을 추가"
}
```

응답은 즉시 새 페이지 버전이 아니라 `201`과 `changeSetId`를 반환한다.

### 7.2 주장과 근거

| Method | Path | 설명 |
| --- | --- | --- |
| `GET` | `/libraries/{libraryId}/pages/{pageId}/claims` | 현재 페이지 주장 목록 |
| `GET` | `/libraries/{libraryId}/claims/{claimId}` | 주장과 상태 |
| `GET` | `/libraries/{libraryId}/claims/{claimId}/evidences` | 근거 목록 |
| `POST` | `/libraries/{libraryId}/claims/{claimId}/status-proposals` | 주장 상태 수정 제안 |
| `POST` | `/libraries/{libraryId}/sources/{sourceId}/trust-proposals` | 출처 신뢰도 수정 제안 |

주장 응답:

```json
{
  "data": {
    "id": "0f72a0e5-e370-445f-97f9-e4a92f16e525",
    "statement": "Raft는 리더 선출과 로그 복제를 분리해 설명한다.",
    "knowledgeStatus": "SOURCE_CLAIM",
    "confidence": 0.94,
    "conditions": null,
    "evidences": [
      {
        "id": "e76db59a-94e7-4614-9f1c-594a57b1f296",
        "evidenceType": "SUPPORTS",
        "sourceVersionId": "b98fe600-a833-4780-8500-a694c17fb878",
        "sourceTitle": "In Search of an Understandable Consensus Algorithm",
        "locator": {
          "page": 2,
          "section": "2"
        },
        "excerpt": "..."
      }
    ]
  }
}
```

### 7.3 관계와 모순

| Method | Path | 설명 |
| --- | --- | --- |
| `GET` | `/libraries/{libraryId}/relations` | 관계 목록·그래프 데이터 |
| `GET` | `/libraries/{libraryId}/contradictions` | 모순 목록 |
| `GET` | `/libraries/{libraryId}/contradictions/{contradictionId}` | 양쪽 주장과 근거 |
| `POST` | `/libraries/{libraryId}/contradictions/{contradictionId}/resolution` | 모순 판단 변경 세트 생성 |
| `POST` | `/libraries/{libraryId}/relations/{relationId}/feedback` | 연결 피드백 |

관계 조회 필터:

```text
GET /libraries/{libraryId}/relations?pageId=<uuid>&status=ACCEPTED&since=2026-07-01T00:00:00Z
```

모순 해소 요청:

```json
{
  "decision": "CLASSIFY",
  "classification": "TIME_CHANGE",
  "resolutionNote": "두 주장은 서로 다른 소프트웨어 버전을 대상으로 한다."
}
```

위험 변경이므로 응답은 `changeSetId`를 반환하며 검토·적용 절차를 거친다.

관계 피드백:

```json
{
  "feedbackType": "IRRELEVANT",
  "comment": "주제는 비슷하지만 현재 도서관의 운영 목적과 무관합니다.",
  "scope": "LIBRARY"
}
```

## 8. 변경 검토와 버전

### 8.1 변경 세트

| Method | Path | 설명 |
| --- | --- | --- |
| `GET` | `/libraries/{libraryId}/change-sets` | 검토 대기·이력 목록 |
| `GET` | `/libraries/{libraryId}/change-sets/{changeSetId}` | 변경 이유, 영향, diff, 근거 |
| `GET` | `/libraries/{libraryId}/change-sets/{changeSetId}/items` | 변경 항목 목록 |
| `POST` | `/libraries/{libraryId}/change-sets/{changeSetId}/reviews` | 전체 또는 항목별 판단 |
| `POST` | `/libraries/{libraryId}/change-sets/{changeSetId}/apply` | 승인 항목 발행 |
| `POST` | `/libraries/{libraryId}/change-sets/{changeSetId}/recalculate` | 기준 버전 충돌 시 재계산 |

목록 필터:

```text
GET /libraries/{libraryId}/change-sets?status=READY_FOR_REVIEW&riskLevel=HIGH
```

검토 요청:

```json
{
  "expectedRevision": 4,
  "decisions": [
    {
      "changeItemId": "8f9b2959-f582-49a5-b1f8-143700de1719",
      "decision": "APPROVE"
    },
    {
      "changeItemId": "eddfcc0c-d56b-49f7-91d4-7f1c6c53869d",
      "decision": "REJECT",
      "comment": "관점 차이를 사실 모순으로 판단했습니다."
    },
    {
      "changeItemId": "d626b320-c8b2-4690-bd98-97ef364df65a",
      "decision": "DEFER",
      "comment": "추가 원본을 확인한 뒤 결정합니다."
    }
  ]
}
```

안전한 항목 일괄 승인:

```json
{
  "expectedRevision": 4,
  "selection": {
    "riskLevels": ["SAFE"]
  },
  "decision": "APPROVE"
}
```

적용 요청:

```json
{
  "expectedChangeSetRevision": 5,
  "expectedLibraryVersion": 12,
  "summary": "Raft 자료 20건 Ingest 결과 반영"
}
```

적용 성공:

```json
{
  "data": {
    "changeSetId": "b63466af-62f3-477a-8258-e2aa23044c9c",
    "status": "APPLIED",
    "libraryVersion": {
      "id": "29c7d74d-b4a7-46fa-b5d0-5fb25182ed47",
      "versionNo": 13,
      "createdAt": "2026-07-27T06:00:00Z"
    },
    "postIngestLintRunId": "a17c767b-4d15-4761-b45e-7a571b12eec7"
  }
}
```

### 8.2 도서관 버전

| Method | Path | 설명 |
| --- | --- | --- |
| `GET` | `/libraries/{libraryId}/versions` | 발행 이력 |
| `GET` | `/libraries/{libraryId}/versions/{versionNo}` | 버전 요약 |
| `GET` | `/libraries/{libraryId}/versions/diff` | 두 도서관 버전 간 변경 |

```text
GET /libraries/{libraryId}/versions/diff?from=12&to=13
```

## 9. 검색과 AI 사서

### 9.1 전통·하이브리드 검색

| Method | Path | 설명 |
| --- | --- | --- |
| `GET` | `/libraries/{libraryId}/search` | 페이지·주장·원본 검색 |
| `POST` | `/libraries/{libraryId}/search-expansions` | 연결 도서관 확장 검색 |

검색:

```text
GET /libraries/{libraryId}/search?q=raft+leader+election&types=PAGE,CLAIM,SOURCE&mode=HYBRID&limit=20
```

`mode`:

- `KEYWORD`: PostgreSQL 전문 검색
- `SEMANTIC`: pgvector
- `HYBRID`: 두 결과 결합, 기본값

응답 결과에는 `resultType`, `title`, `snippet`, `score`, `pageId` 또는 `sourceId`, 근거 위치를 포함한다.

연결 도서관 확장은 반드시 별도 사용자 명령으로 수행한다.

```json
{
  "query": "Raft 장애 복구",
  "targetLibraryIds": [
    "ae7a4b55-d42c-4ccb-9d0e-f23acfc156f9"
  ],
  "mode": "HYBRID"
}
```

서버는 `library_references`에 등록된 대상만 허용한다.

### 9.2 대화와 답변

| Method | Path | 설명 |
| --- | --- | --- |
| `POST` | `/libraries/{libraryId}/conversations` | 대화 생성 |
| `GET` | `/libraries/{libraryId}/conversations` | 대화 목록 |
| `GET` | `/libraries/{libraryId}/conversations/{conversationId}` | 메시지 목록 |
| `POST` | `/libraries/{libraryId}/conversations/{conversationId}/messages` | 질문 전송 |
| `GET` | `/libraries/{libraryId}/conversations/{conversationId}/messages/{messageId}` | 생성 상태·답변 |
| `POST` | `/libraries/{libraryId}/messages/{messageId}/save-proposal` | 답변을 위키에 반영할 변경 세트 생성 |
| `POST` | `/libraries/{libraryId}/messages/{messageId}/feedback` | 답변 평가 |

질문:

```json
{
  "content": "Raft와 Paxos를 이해하기 쉬움과 운영 안정성 관점에서 비교해줘.",
  "answerMode": "COMPARE",
  "processingMode": "STANDARD",
  "searchScope": {
    "currentLibrary": true,
    "referencedLibraryIds": []
  }
}
```

초기 응답:

```json
{
  "data": {
    "userMessageId": "6e457d12-82b3-4bd3-b2e0-19989ec321fd",
    "assistantMessageId": "944e36e1-21fb-4705-8948-2f5b51c9e89d",
    "status": "QUEUED"
  }
}
```

완료된 답변:

```json
{
  "data": {
    "id": "944e36e1-21fb-4705-8948-2f5b51c9e89d",
    "role": "ASSISTANT",
    "status": "COMPLETED",
    "answerMode": "COMPARE",
    "content": "Raft는 역할과 단계가 명시적이어서...",
    "insufficientEvidence": false,
    "citations": [
      {
        "order": 1,
        "pageId": "88e59d1a-7fb2-4f63-b522-bd59d75f8214",
        "pageTitle": "Raft 합의 알고리즘",
        "claimId": "0f72a0e5-e370-445f-97f9-e4a92f16e525",
        "sourceVersionId": "b98fe600-a833-4780-8500-a694c17fb878",
        "locator": {
          "page": 2
        }
      }
    ],
    "suggestions": {
      "missingEvidence": [],
      "expandToReferencedLibraries": false,
      "webSearchAvailable": false
    },
    "completedAt": "2026-07-27T06:30:00Z"
  }
}
```

근거가 부족하면:

- `insufficientEvidence: true`
- 모른다는 내용을 답변에 명시
- 필요한 자료 제안
- 연결된 도서관 확장 여부 제안
- 웹 검색은 자동 실행하지 않고 별도 승인 가능한 제안만 반환

답변 저장 제안:

```json
{
  "target": {
    "type": "NEW_PAGE",
    "pageType": "SYNTHESIS",
    "title": "Raft와 Paxos 비교"
  },
  "includeCitationIds": [
    "b2552e88-6ac1-4ea3-bbd3-e2b7e4427fc2"
  ]
}
```

응답은 `changeSetId`와 영향을 받는 기존 페이지를 반환한다.

## 10. Lint

| Method | Path | 설명 |
| --- | --- | --- |
| `POST` | `/libraries/{libraryId}/lint-runs` | 수동 Lint 시작 |
| `GET` | `/libraries/{libraryId}/lint-runs` | 실행 목록 |
| `GET` | `/libraries/{libraryId}/lint-runs/{lintRunId}` | 상태·요약 |
| `GET` | `/libraries/{libraryId}/lint-runs/{lintRunId}/findings` | 검사 결과 |
| `POST` | `/libraries/{libraryId}/lint-findings/{findingId}/decision` | 수락·무시·해결 |
| `POST` | `/libraries/{libraryId}/lint-findings/{findingId}/change-proposal` | 수정 변경 세트 생성 |

시작 요청:

```json
{
  "scope": "FULL",
  "checks": [
    "BROKEN_LINK",
    "ORPHAN_PAGE",
    "DUPLICATE",
    "MISSING_EVIDENCE",
    "CONTRADICTION",
    "STALE_CLAIM",
    "KNOWLEDGE_GAP"
  ],
  "processingMode": "STANDARD"
}
```

대량 Ingest 후 전체 Lint는 서버가 자동 생성한다. 월간 Lint는 스케줄러가 생성하며 중복 실행을 막는다.

## 11. 알림과 활동

| Method | Path | 설명 |
| --- | --- | --- |
| `GET` | `/notifications` | 내 알림 목록 |
| `GET` | `/notifications/unread-count` | 읽지 않은 알림 수 |
| `POST` | `/notifications/{notificationId}/read` | 읽음 |
| `POST` | `/notifications/read-all` | 모두 읽음 |
| `GET` | `/libraries/{libraryId}/activities` | Import·변경·발행 등 최근 활동 |

알림 유형:

- `RISKY_CHANGE_REVIEW_REQUIRED`
- `LINT_COMPLETED`
- `FORK_SOURCE_UPDATED`
- `EDIT_PROPOSAL_RECEIVED`
- `ACCOUNT_PURGE_REMINDER`
- `QUOTA_REQUIRED`

단순 Import 완료는 기본적으로 활동에만 기록한다.

## 12. 사용량과 과금

| Method | Path | 설명 |
| --- | --- | --- |
| `GET` | `/billing/plan` | 현재 요금제 |
| `GET` | `/billing/usage` | 기간별 처리량 |
| `GET` | `/billing/usage/events` | 사용량 상세 |
| `POST` | `/billing/checkout-sessions` | 구독·추가 처리량 결제 시작 |
| `POST` | `/billing/portal-sessions` | 결제 관리 화면 |

```text
GET /billing/usage?period=2026-07
```

응답:

```json
{
  "data": {
    "period": "2026-07",
    "plan": "PERSONAL_BASIC",
    "includedUnits": 1000,
    "usedUnits": 620,
    "remainingUnits": 380,
    "breakdown": {
      "scan": 40,
      "ingest": 430,
      "assistant": 100,
      "lint": 50
    }
  }
}
```

사용자에게는 문서 수와 처리량으로 설명하되 내부 원장은 토큰과 공급자 비용을 기록한다.

## 13. 내보내기

| Method | Path | 설명 |
| --- | --- | --- |
| `POST` | `/libraries/{libraryId}/exports` | ZIP 생성 시작 |
| `GET` | `/libraries/{libraryId}/exports/{exportId}` | 생성 상태 |
| `GET` | `/libraries/{libraryId}/exports/{exportId}/download` | 만료되는 다운로드 URL |

요청:

```json
{
  "format": "ZEROWIKI_ZIP",
  "include": [
    "RAW_SOURCES",
    "MARKDOWN_PAGES",
    "ATTACHMENTS",
    "RELATIONS",
    "CLAIMS_AND_EVIDENCE",
    "SOURCE_TRUST",
    "VERSIONS",
    "CONSTITUTION"
  ]
}
```

내보내기 ZIP에는 기계 판독 가능한 `manifest.json`과 안정적인 Markdown 경로·frontmatter를 포함한다. 내보낸 자료의 재Import 왕복 검증을 Phase 0에서 수행한다.

## 14. 공개·포크·편집 제안

Phase 3 API다.

### 14.1 공개 페이지

| Method | Path | 설명 |
| --- | --- | --- |
| `POST` | `/libraries/{libraryId}/pages/{pageId}/publication-checks` | 민감정보·저작권 검사 |
| `POST` | `/libraries/{libraryId}/pages/{pageId}/publications` | 특정 버전 공개 |
| `PATCH` | `/libraries/{libraryId}/pages/{pageId}/publication` | 포크·제안 정책 변경 |
| `DELETE` | `/libraries/{libraryId}/pages/{pageId}/publication` | 비공개 전환 |
| `GET` | `/public/pages/{publicSlug}` | 공개 페이지 조회 |

공개 요청:

```json
{
  "pageVersionNo": 4,
  "publicationCheckId": "87246e2d-0306-4f21-88e0-59529290a94a",
  "confirmedWarnings": [
    "SOURCE_QUOTE_REVIEWED"
  ],
  "forkAllowed": true,
  "editProposalPolicy": "INVITED"
}
```

검사 결과가 만료됐거나 페이지 버전이 달라지면 `409 PUBLICATION_CHECK_STALE`을 반환한다.

### 14.2 포크

| Method | Path | 설명 |
| --- | --- | --- |
| `POST` | `/public/pages/{publicSlug}/forks` | 내 도서관으로 포크 |
| `GET` | `/libraries/{libraryId}/forks` | 포크 목록과 업데이트 여부 |
| `GET` | `/libraries/{libraryId}/forks/{forkId}/diff` | 원본 최신 버전 비교 |
| `POST` | `/libraries/{libraryId}/forks/{forkId}/update-proposals` | 선택적 반영 변경 세트 |

포크는 원본 publication, 원본 page version, 생성된 내 page의 계보를 영구 기록한다.

### 14.3 편집 제안과 신고

| Method | Path | 설명 |
| --- | --- | --- |
| `POST` | `/public/pages/{publicSlug}/edit-proposals` | 초대 사용자 편집 제안 |
| `GET` | `/libraries/{libraryId}/edit-proposals` | 받은 제안 |
| `POST` | `/libraries/{libraryId}/edit-proposals/{proposalId}/decision` | 수락·거절 |
| `POST` | `/public/pages/{publicSlug}/reports` | 공개 페이지 신고 |

편집 제안을 수락하면 내부 페이지를 즉시 덮어쓰지 않고 `originType=EDIT_PROPOSAL`인 변경 세트를 만든다.

## 15. 보안 요구사항

1. 모든 쿼리는 인증 사용자와 소유자 조건을 함께 적용한다.
2. 객체 저장소 키를 API 응답에 직접 노출하지 않고 짧은 수명의 서명 URL을 사용한다.
3. 업로드는 확장자가 아니라 MIME sniffing, 크기, 압축 해제 크기와 파일 수를 검사한다.
4. ZIP path traversal과 압축 폭탄을 차단한다.
5. URL 클리핑은 SSRF, DNS rebinding, 과도한 redirect를 방어한다.
6. 원본 문서 본문은 LLM 시스템·개발자 명령과 분리된 데이터 영역으로 전달한다.
7. 구조화된 LLM 출력은 버전이 지정된 JSON Schema로 검증한 뒤 저장한다.
8. 원본에서 발견한 명령문을 시스템 명령으로 실행하지 않는다.
9. 비밀번호·API 키·토큰 탐지는 객체 영구 저장 전에 실행한다.
10. 공개와 위험 변경에는 행위자, 시각, 대상, 결과를 감사 로그로 남긴다.
11. 오류 응답과 로그에 원본 본문, 토큰, 서명 URL을 포함하지 않는다.
12. 사용량 차감과 변경 적용은 DB 트랜잭션 및 멱등성 키로 중복 실행을 방지한다.

## 16. OpenAPI 구현 기준

Spring Boot 구현 시 다음 단위로 OpenAPI tag를 나눈다.

- `Auth`
- `Account`
- `Libraries`
- `Constitutions`
- `Sources`
- `Uploads`
- `Ingest`
- `Pages`
- `Claims`
- `Relations`
- `Contradictions`
- `Change Sets`
- `Search`
- `Assistant`
- `Lint`
- `Notifications`
- `Billing`
- `Exports`
- `Publications`

공통 Schema:

- `ApiError`
- `FieldError`
- `CursorPage`
- `AsyncJob`
- `ResourceReference`
- `Citation`
- `UsageSummary`

enum은 문자열로 노출하며 서버 내부 Java enum 이름과 API 값을 명시적으로 매핑한다. enum 추가는 하위 호환 변경으로 취급하므로 클라이언트는 알 수 없는 값을 안전하게 처리해야 한다.

## 17. 구현 순서

1. 인증·사용자·도서관·운영 헌법
2. 업로드·원본·도서관 원본 연결
3. Ingest 작업·항목·계획 승인 상태 머신
4. 페이지·버전·주장·근거·관계·모순 조회
5. 변경 세트 검토·적용·도서관 버전
6. 키워드·의미 검색
7. AI 사서 대화·인용·답변 저장 제안
8. Lint·알림·사용량
9. Notion·URL 클리핑·내보내기
10. 공개·포크·편집 제안·신고

## 18. 구현 전에 확정할 계약

1. 파일당·ZIP당·Import당 최대 크기와 문서 수
2. Access Token과 Refresh Token의 수명
3. Free·Basic·Advanced 처리량 단위
4. AI 메시지 polling 간격과 최대 생성 시간
5. 페이지 diff 표현을 unified diff로 할지 구조화 블록 diff로 할지
6. 사용자 직접 편집 중 오탈자 같은 안전 변경을 자동 적용할지 항상 변경 세트를 만들지
7. 검색 결과에서 원본 발췌문을 노출하는 최대 길이
8. 공개 URL slug 충돌과 변경 정책
9. OpenAPI에 포함할 관리자·운영 API의 별도 분리 여부
10. Notion OAuth와 결제 공급자 선정
