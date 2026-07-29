# ZeroWiki REST API 명세 초안

작성일: 2026-07-27  
최종 수정: 2026-07-29 (요구사항 정의서 0.2 기준 조정)  
상태: MVP API 계약 초안  
기준 문서: `ZeroWiki-MVP-서비스-기획서.md`(1순위), `ZeroWiki-요구사항-정의서.md`(요구사항 근거), `ZeroWiki-ERD-초안.md`

이 문서의 각 계약은 `docs/ZeroWiki-요구사항-정의서.md`의 FR·NFR ID를 근거로 갖는다. 요구사항 정의서와 충돌하면 요구사항 정의서가 우선하며, 충돌은 이 문서의 결함으로 간주한다. 미확정 값은 `UD-NN`으로 참조하며 같은 문서 12절에 정의되어 있다.

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
9. 클라이언트는 LLM 공급자명이나 실제 모델명을 지정하지 않고 제품 처리 모드만 전달한다. (FR-ING-13, FR-UIX-11)
10. **위키 콘텐츠는 한국어로 생성한다.** 원본 언어와 무관하게 페이지 본문·제목·요약·주장 문장은 한국어다. (FR-UIX-12, 2026-07-29 사용자 확정)
11. **원본에서 인용한 발췌문은 번역하지 않고 원문 그대로 반환한다.** `excerpt`, `locator`가 가리키는 텍스트는 원본 언어를 유지한다. 번역된 발췌는 근거로서 효력이 없다. (FR-UIX-13)

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

서버 동작 (ERD 4.1절 `idempotency_records` 대응):

1. 키를 `(사용자, 키)` 단위로 저장하고 요청 본문 해시를 함께 기록한다.
2. 같은 키·같은 본문 해시의 재요청은 **저장된 응답을 그대로 반환**한다. 처리를 다시 실행하지 않으며 사용량을 차감하지 않는다(FR-ING-24).
3. 같은 키·다른 본문 해시는 `409 IDEMPOTENCY_KEY_REUSED`.
4. 첫 요청이 아직 처리 중이면 `409 IDEMPOTENCY_KEY_IN_PROGRESS`를 반환한다.
5. 레코드는 24시간 후 만료된다.

**저장 응답에서 제외할 값:** 서명 URL, 다운로드 URL, 토큰 등 **수명이 짧거나 재사용되면 안 되는 값은 캐시하지 않는다**(NFR-SEC-09). 해당 필드를 포함하는 응답은 캐시 시 필드를 제거하고, 재요청 시 서버가 새로 발급해 채운다. 만료된 서명 URL을 그대로 돌려주면 클라이언트가 실패하고, 유효한 URL을 장기 보관하면 접근 통제가 무력화된다.

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

MVP 웹 클라이언트는 polling을 기본으로 한다. 추후 SSE를 추가한다.

**polling 간격 (UD-04 확정, 2026-07-29)**

서버가 작업 조회 응답에 `Retry-After`(초)를 반환하고 **클라이언트는 이를 우선한다.** 헤더가 없을 때만 아래 기본값을 쓴다.

| 작업 | 기본 간격 | 근거 |
| --- | --- | --- |
| Ingest | 2초 | 단계 전이가 잦고 사용자 입력 대기 상태(`PLAN_REVIEW`·`QUESTION_WAITING`)를 빨리 감지해야 한다 |
| AI 답변 | 1초 | 사용자가 화면 앞에서 기다린다 |
| Lint·Export | 5초 | 중간 입력이 없고 진행도 표시로 충분하다 |

서버가 간격을 지시하는 방식을 택한 이유는 부하에 따라 조절할 수 있기 때문이다. 클라이언트 고정 간격만 쓰면 사용자가 늘었을 때 polling이 부하를 키운다.

**최대 생성 시간은 `UD-25`(성능 목표)에 종속되며 Phase 0 벤치마크 후 정한다.** 그전까지 서버는 초과 시 작업을 `FAILED`로 종결하고 재시도 가능하게 둔다.

`status`는 요구사항 FR-ING-20의 작업 상태 8종과 다음과 같이 대응한다. 이 매핑 밖의 상태 값을 추가하지 않는다.

| 요구사항 상태 (FR-ING-20) | API `status` | `nextAction` |
| --- | --- | --- |
| 대기 | `QUEUED` | 없음 |
| 스캔 | `SCANNING` | 없음 |
| 계획 승인 대기 | `PLAN_REVIEW` | `APPROVE_PLAN` |
| 처리 | `PROCESSING` | 없음 |
| 질문 대기 | `QUESTION_REVIEW` | `ANSWER_QUESTIONS` |
| 변경 승인 대기 | `CHANGE_REVIEW` | `REVIEW_CHANGES` |
| 완료 | `COMPLETED` | 없음 |
| 실패 | `FAILED` | `RETRY_FAILED_ITEMS` |

정상 흐름 8종 외에 **운영 상태 2종**이 있다(FR-ING-26). 어느 단계에서든 진입할 수 있다.

| 요구사항 상태 (FR-ING-26) | API `status` | `nextAction` | 재개 |
| --- | --- | --- | --- |
| 취소됨 | `CANCELLED` | 없음 | 불가. 새 작업을 만든다 |
| 처리량 보류 | `PAUSED_QUOTA` | `PURCHASE_QUOTA` | 구매 후 중단 시점 단계로 복귀 |

`PAUSED_QUOTA`는 `FAILED`와 구별된다. **실패는 종결 상태이고 보류는 재개 가능한 상태다.** 처리량 부족을 `FAILED`로 기록하면 사용자가 구매 후에도 작업을 이어갈 수 없다.

`PAUSED_QUOTA`에서 복귀할 때 서버는 중단 시점의 단계(`SCANNING`·`PROCESSING` 등)로 돌아가며, 이미 처리한 항목을 다시 과금하지 않는다(FR-ING-24).

### 2.8 공통 enum과 언어 규약

#### 처리 모드 (FR-ING-13)

```text
processingMode: FAST | STANDARD | PRECISE
```

사용자 표시 문구는 `빠른 처리`·`표준 처리`·`정밀 처리`다. **응답 어디에도 LLM 공급자명·모델명·모델 버전을 포함하지 않는다.** 내부 모델 라우팅은 서버 책임이다.

#### 지식 상태 (FR-KNW-06)

```text
knowledgeStatus: VERIFIED_FACT | SOURCE_CLAIM | AI_SYNTHESIS | USER_JUDGMENT | HYPOTHESIS
```

5종 고정이다. 확장은 요구사항 변경을 거친다.

#### 언어 (FR-UIX-12, FR-UIX-13)

| 필드 | 언어 | 설명 |
| --- | --- | --- |
| 페이지 `title`, `markdownBody`, `summary` | 항상 한국어 | 원본 언어와 무관하게 한국어로 종합 |
| 주장 `statement` | 항상 한국어 | 동일 |
| Evidence `excerpt` | **원본 언어** | 번역하지 않는다 |
| 원본 `title` | 원본 언어 | 원문 제목을 보존한다 |

원본 응답에는 감지된 원본 언어를 `detectedLanguage`(BCP 47, 예: `en`, `ko`)로 포함한다. 감지에 실패하면 `null`을 반환하며, 이 경우에도 위키 생성 언어는 한국어다.

#### 전문 용어 병기 (FR-UIX-14, 2026-07-29 사용자 확정)

한국어로 종합할 때 전문 용어는 **처음 등장하는 자리에서 1회만** 원어를 병기한다.

```text
주의 기제(attention mechanism)는 신경망이 입력의 특정 부분에 집중하는 메커니즘이다.
주의 기제는 트랜스포머의 핵심이며, 자기 주의(self-attention)로 확장된다.
```

- 형식: `한국어(원어)`. 원어는 원본 표기를 그대로 쓴다
- 같은 페이지 안에서 이미 병기한 용어는 이후 한국어로만 쓴다
- 새 개념이 등장하면 그 용어에 대해 다시 1회 병기한다
- **정의 섹션과 핵심 주장에는 병기를 유지한다** — 사용자가 해당 부분만 읽어도 원어를 알 수 있어야 한다

병기는 생성 시점의 콘텐츠 규칙이므로 API 응답에 별도 필드를 두지 않는다. 클라이언트는 `markdownBody`에 포함된 병기를 그대로 렌더링한다.

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

`GET /libraries/{libraryId}`는 도서관 홈(FR-UIX-02)을 위해 다음 요약을 **항상 포함한다.** 값이 없으면 필드를 생략하지 않고 빈 배열 또는 `null`을 반환한다. 선택적 필드로 두면 클라이언트가 매번 존재 여부를 확인해야 하고 누락과 빈 상태를 구별할 수 없다.

```json
{
  "data": {
    "id": "cc9be2b2-acde-4554-b3fd-2599d3f2ad18",
    "name": "분산 시스템 학습",
    "home": {
      "topics": [],
      "recentRelations": [],
      "openQuestions": [],
      "knowledgeGaps": [],
      "openContradictionCount": 0,
      "pendingChangeSetCount": 0,
      "recentActivities": []
    }
  }
}
```

| 필드 | 빈 값 | 대응 요구사항 |
| --- | --- | --- |
| `topics` | `[]` | FR-UIX-02 주요 주제. 항목은 `{ name, pageCount }` |
| `recentRelations` | `[]` | FR-UIX-02 최근 발견 연결 |
| `openQuestions` | `[]` | FR-UIX-02 열린 질문 |
| `knowledgeGaps` | `[]` | FR-UIX-02 지식 공백 |
| `openContradictionCount` | `0` | FR-UIX-02 모순 수 |
| `pendingChangeSetCount` | `0` | FR-UIX-02 검토 대기 변경 수 |
| `recentActivities` | `[]` | FR-UIX-02 최근 활동 |

각 배열의 기본 길이는 홈 화면 표시에 필요한 만큼으로 제한하며, 상세 목록은 전용 API로 조회한다.

주제 항목은 이름과 페이지 수만 담는다.

```json
{ "name": "합의 알고리즘", "pageCount": 12 }
```

주제에 속한 페이지 목록은 홈 응답에 넣지 않는다. 사용자가 주제를 선택하면 `GET /libraries/{libraryId}/pages?topic=합의%20알고리즘`으로 조회한다. 홈 응답에 주제별 페이지를 미리 담으면 응답이 커지는데, 사용자는 대개 한두 주제만 열어본다.

**`topics`의 산출 기준은 아직 확정되지 않았다(`UD-28`).** 기획서 16절은 "AI가 도서관의 콘텐츠를 분석해 관계도가 높은 주제를 자동 선정한다"고 방향을 정했으나, 관계도를 무엇으로 측정하는지(관계 수·중심성·임베딩 클러스터 크기), 몇 개를 표시하는지, 언제 재계산하는지가 미정이다. 확정 전에는 빈 배열을 반환해도 계약 위반이 아니다.

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

원본 내용 수정 API는 제공하지 않는다(FR-SRC-06). 같은 URL 재클리핑이나 새 파일 업로드는 `source_versions`를 추가한다.

원본 응답에는 `detectedLanguage`(BCP 47)를 포함한다. 이 값은 발췌문 표기와 Ingest 프롬프트 구성에 쓰이며, **위키 생성 언어를 바꾸지 않는다**(FR-UIX-12). 원본 `title`은 원문 제목을 보존한다.

`DELETE /sources/{sourceId}`는 참조 중인 원본에 대해 `409`와 참조하는 도서관 목록을 반환한다(FR-SRC-08).

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

#### 상태 전이표

클라이언트가 다음 화면을 결정할 수 있도록 전이를 명시한다. 표에 없는 전이는 `400 INVALID_STATE_TRANSITION`이다.

| 현재 | 트리거 | 다음 | 클라이언트 동작 |
| --- | --- | --- | --- |
| — | `POST /ingest-jobs` | `SCANNING` | `202`. polling 시작 |
| `SCANNING` | 스캔 완료 | `PLAN_REVIEW` | 계획·비용 표시, 승인 요청 |
| `PLAN_REVIEW` | `POST /plan-approval` (`APPROVE`) | `PROCESSING` | `202`. polling 유지 |
| `PLAN_REVIEW` | `POST /plan-approval` (`REJECT`) | `CANCELLED` | 종료 |
| `PROCESSING` | 맥락 불분명 항목 발생 | `QUESTION_WAITING` | 질문 표시 (FR-ING-05) |
| `QUESTION_WAITING` | `POST /answers` | `PROCESSING` | polling 유지 |
| `PROCESSING` | 처리 완료 | `CHANGE_REVIEW` | `changeSetId`로 검토 화면 이동 |
| `CHANGE_REVIEW` | 변경 세트 `apply` 및 후속 Lint 완료 | `COMPLETED` | 종료 |
| 임의 진행 상태 | 처리량 소진 | `PAUSED_QUOTA` | 구매 안내. **`FAILED` 아님** |
| `PAUSED_QUOTA` | 처리량 구매 | 중단 시점 상태 | polling 재개 |
| 임의 진행 상태 | `POST /cancel` | `CANCELLED` | 종료 |
| 임의 진행 상태 | 복구 불가 오류 | `FAILED` | `POST /retry`로 실패 항목만 재시도 |

`QUESTION_WAITING`은 **작업 전체를 멈추지 않는다.** 맥락이 불분명한 항목만 보류하고 나머지는 계속 처리한다(FR-ING-05). 따라서 `QUESTION_WAITING` 상태에서도 `progress.completed`가 증가할 수 있다.

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
| `GET` | `/libraries/{libraryId}/pages/{pageId}/diff` | 두 버전 비교 (스냅샷 반환) |
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
    "topics": ["합의 알고리즘", "분산 시스템"],
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

#### diff 표현 (UD-05 확정, 2026-07-29)

**Phase 1은 서버가 스냅샷을 반환하고 클라이언트가 diff를 계산한다.**

```text
GET /libraries/{libraryId}/pages/{pageId}/diff?from=2&to=3
```

```json
{
  "data": {
    "from": { "versionNo": 2, "title": "...", "markdownBody": "..." },
    "to":   { "versionNo": 3, "title": "...", "markdownBody": "..." }
  }
}
```

**Phase 2에서 서버가 구조화 블록 diff를 추가한다.** 응답에 `structuredDiff` 필드를 더하는 하위 호환 변경이며, 블록 단위(`HEADING`·`PARAGRAPH`·`LIST` 등)로 `UNCHANGED`·`CHANGED`·`ADDED`·`REMOVED` 상태를 표시한다.

Phase 1에서 unified diff를 서버가 생성하지 않는 이유는, 라인 단위 문자열 diff가 Markdown 구조를 감춰 **작은 변경을 문단 전체 교체처럼 보이게 하기 때문**이다. 전문 용어 병기 하나가 바뀌어도 문단 전체가 변경으로 표시되면(FR-UIX-14) 사용자가 변경의 중요도를 오판한다. 그렇다고 Phase 1부터 서버 구조화 diff를 만들면 Markdown 파싱 계층이 먼저 필요해 개인 도서관 코어 검증이 늦어진다. 따라서 Phase 1은 클라이언트 계산으로 두고, 구조화 diff는 Phase 2에서 서버가 제공한다.

블록별 선택 승인은 MVP 범위가 아니다. 승인 단위는 변경 항목(`changeItem`)이다.

### 7.2 주장과 근거

| Method | Path | 설명 |
| --- | --- | --- |
| `GET` | `/libraries/{libraryId}/pages/{pageId}/claims` | 현재 페이지 주장 목록. `?include=evidences`로 근거 동봉 |
| `GET` | `/libraries/{libraryId}/claims/{claimId}` | 주장과 상태 |
| `GET` | `/libraries/{libraryId}/claims/{claimId}/evidences` | 근거 목록 |

**근거 보기(FR-ASK-04)는 페이지의 여러 주장에 대한 근거를 한 번에 필요로 한다.** 주장마다 `/claims/{claimId}/evidences`를 호출하면 N+1이 발생하므로, 목록 엔드포인트에 `include`를 지원한다.

```text
GET /libraries/{libraryId}/pages/{pageId}/claims?include=evidences
```

응답의 각 주장에 `evidences` 배열이 동봉된다. `include`를 생략하면 주장만 반환하고 `evidenceCount`만 포함한다. 동봉 시 주장당 근거 수가 많으면 서버가 상한을 적용하고 `evidenceTruncated: true`를 표시한다. 전체가 필요하면 개별 엔드포인트를 쓴다.
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
        "excerpt": "Raft separates leader election from log replication.",
        "excerptLanguage": "en"
      }
    ]
  }
}
```

`statement`는 한국어로 생성된 주장이고 `excerpt`는 **원본 언어 그대로**인 발췌다(FR-UIX-12·13). 두 값의 언어가 다른 것은 정상이며, 클라이언트는 `excerptLanguage`로 원문 표기를 구분한다. `excerpt` 최대 길이는 `UD-07 미확정`, 저작권상 허용 범위는 `UD-14 미확정`이다.

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

`feedbackType` enum은 요구사항 FR-FBK-01의 6종과 대응한다.

```text
feedbackType: DIFFERENT_TOPIC | NOT_CAUSAL | WEAK_SOURCE | IRRELEVANT | DUPLICATE_CONCEPT | OTHER
```

`scope`는 FR-FBK-02·03을 표현한다.

| `scope` | 적용 범위 | 기본값 |
| --- | --- | --- |
| `LIBRARY` | 현재 도서관의 운영 헌법과 이후 AI 판단에만 반영 | **기본값** |
| `ALL_LIBRARIES` | 사용자의 모든 도서관에 공통 선호로 적용 | 사용자가 명시적으로 선택할 때만 |

`scope`를 생략하면 `LIBRARY`다. 서버가 사용자 동의 없이 `ALL_LIBRARIES`로 확대하지 않는다(FR-FBK-02).

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

변경 세트 응답은 낙관적 잠금을 위해 `revision`을 포함한다(2.6절, ERD 4.6절 `change_sets.revision`). 클라이언트는 이 값을 검토·적용 요청의 `expectedRevision`·`expectedChangeSetRevision`에 그대로 넣는다.

```json
{
  "data": {
    "id": "b63466af-62f3-477a-8258-e2aa23044c9c",
    "libraryId": "cc9be2b2-acde-4554-b3fd-2599d3f2ad18",
    "status": "READY_FOR_REVIEW",
    "riskLevel": "HIGH",
    "summary": "Raft 자료 20건 Ingest 결과",
    "baseLibraryVersionNo": 12,
    "counts": {
      "total": 34,
      "safe": 28,
      "review": 4,
      "high": 2
    },
    "revision": 4
  }
}
```

`revision`을 노출하는 리소스는 `libraries`, `change_sets`, 그리고 현재 버전을 직접 수정하는 운영 헌법이다. 조회 응답에서 받은 값을 그대로 되돌려 보내지 않으면 `409 VERSION_CONFLICT`가 발생한다.

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
    },
    {
      "changeItemId": "5a1c8e77-3b90-4a2e-9f11-0c6d84b2f5aa",
      "decision": "REQUEST_CHANGES",
      "comment": "결론은 맞지만 근거를 원 논문으로 교체해 다시 제안해주세요."
    }
  ]
}
```

`decision` enum은 요구사항 FR-CHG-05의 4종과 일대일 대응한다.

| 요구사항 (FR-CHG-05) | `decision` | 서버 동작 |
| --- | --- | --- |
| 승인 | `APPROVE` | 적용 대상에 포함 |
| 거절 | `REJECT` | 적용하지 않고 종결. 거절 사유는 운영 헌법 학습에 반영 (FR-FBK-04) |
| 수정 요청 | `REQUEST_CHANGES` | 적용하지 않고 항목을 재생성 대상으로 표시. `comment`가 재생성 입력이 된다 |
| 판단 보류 | `DEFER` | 적용하지 않고 검토 대기로 유지. 변경 세트는 미완결 상태로 남는다 |

`REQUEST_CHANGES`가 하나라도 있으면 변경 세트는 `apply` 후에도 완결되지 않고 재생성 결과를 기다린다. `comment`는 `REQUEST_CHANGES`와 `REJECT`에서 필수다.

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

각 결과에는 **출처 도서관을 나타내는 `origin`을 포함한다.**

```json
{
  "resultType": "PAGE",
  "title": "Raft 합의 알고리즘",
  "origin": {
    "scope": "CURRENT",
    "libraryId": "cc9be2b2-acde-4554-b3fd-2599d3f2ad18",
    "libraryName": "분산 시스템 학습"
  }
}
```

`scope`는 `CURRENT` 또는 `REFERENCED`다. 확장 검색 결과에서 현재 도서관과 참조 도서관의 결과가 섞이면 사용자가 지식의 출처를 오인한다. 이는 단순한 UX 문제가 아니라 **도서관 경계 인식의 문제**이며(FR-LIB-04·05, NFR-SEC-03), 클라이언트는 `REFERENCED` 결과를 시각적으로 구분해 표시해야 한다.

`snippet`의 최대 길이는 `UD-07 미확정`이다.

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

`checks`를 생략하면 전체 검사를 실행한다. enum은 요구사항 FR-LNT-05의 12종과 일대일 대응한다.

| 요구사항 검사 항목 (기획서 13절) | `checks` 값 |
| --- | --- |
| 깨진 연결 | `BROKEN_LINK` |
| 고아 페이지 | `ORPHAN_PAGE` |
| 중복 개념 | `DUPLICATE_CONCEPT` |
| 상호 참조 누락 | `MISSING_BACKLINK` |
| 출처 없는 주요 주장 | `MISSING_EVIDENCE` |
| 출처 간 모순 | `CONTRADICTION` |
| 최신 자료에 의해 낡은 주장 | `STALE_CLAIM` |
| 낮은 신뢰도의 핵심 주장 | `LOW_CONFIDENCE_CORE_CLAIM` |
| 도서관 목적과 무관한 연결 | `OFF_PURPOSE_RELATION` |
| 운영 헌법과 실제 구조의 불일치 | `CONSTITUTION_DRIFT` |
| 포크 원본의 새 버전과 반영 필요성 | `FORK_SOURCE_UPDATED` |
| 지식 흐름상 중요한 공백 | `KNOWLEDGE_GAP` |

`FORK_SOURCE_UPDATED`는 Phase 3 이후에만 결과를 생성한다.

대량 Ingest 후 전체 Lint는 서버가 자동 생성한다(FR-LNT-01). 월간 Lint는 스케줄러가 생성하며 중복 실행을 막는다(FR-LNT-02). **월간 Lint의 실행 시각·타임존 기준과 대상 선정 방식은 `UD-21 미확정`이다.** 확정 전에는 스케줄 조회·변경 API를 계약으로 고정하지 않는다.

Lint가 제안하는 웹 검색과 보완 Ingest는 **사용자 승인 후에만** 실행한다(FR-LNT-07). `lint-findings/{findingId}/change-proposal`은 변경 세트만 만들고 외부 호출을 하지 않는다.

## 11. 알림과 활동

| Method | Path | 설명 |
| --- | --- | --- |
| `GET` | `/notifications` | 내 알림 목록 |
| `GET` | `/notifications/unread-count` | 읽지 않은 알림 수 |
| `POST` | `/notifications/{notificationId}/read` | 읽음 |
| `POST` | `/notifications/read-all` | 모두 읽음 |
| `GET` | `/libraries/{libraryId}/activities` | Import·변경·발행 등 최근 활동 |
| `GET` | `/libraries/{libraryId}/activities/summary` | 기간별 활동 집계 (활동 잔디용) |

알림 유형:

- `RISKY_CHANGE_REVIEW_REQUIRED`
- `LINT_COMPLETED`
- `FORK_SOURCE_UPDATED`
- `EDIT_PROPOSAL_RECEIVED`
- `ACCOUNT_PURGE_REMINDER`
- `QUOTA_REQUIRED`

단순 Import 완료는 기본적으로 활동에만 기록한다(FR-NTF-03). `QUOTA_REQUIRED`는 처리량 부족으로 작업이 보류됐을 때 발송한다(FR-NTF-05, 2026-07-29 사용자 승인으로 기획서 17절에 추가됨).

활동 집계는 활동 잔디(FR-UIX-06, Phase 4)를 위한 것이다.

```text
GET /libraries/{libraryId}/activities/summary?from=2026-01-01&to=2026-12-31&granularity=DAY
```

```json
{
  "data": {
    "granularity": "DAY",
    "buckets": [
      { "date": "2026-07-29", "count": 12 }
    ]
  }
}
```

`granularity`는 `DAY` 또는 `WEEK`다. 활동이 없는 구간은 배열에서 생략하며, 클라이언트가 빈 칸으로 렌더링한다. 알림 보존 기간과 읽음 처리 정책은 `UD-23 미확정`이다.

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
13. **원본 중복 후보 탐색은 사용자 계정 내부로 한정한다.** 사용자 간 콘텐츠 해시 비교나 중복 제거를 하지 않는다. 저장 비용 절감을 이유로도 허용하지 않는다. (FR-SRC-05, 기획서 6.1절)
14. **검색과 확장 검색은 `library_references`에 등록된 범위를 넘지 않는다.** 사용자 승인 없이 참조 도서관으로 자동 확장하지 않는다. (FR-LIB-07, FR-ASK-07, NFR-SEC-03)
15. 공개 페이지 응답에는 비공개 도서관의 페이지 ID·원본 ID·내부 버전 번호를 포함하지 않는다. (NFR-SEC-01) `[파생]`

이 절은 요구사항 정의서 6.1절(NFR-SEC-01~10)의 API 계층 구현 기준이다. 두 문서가 충돌하면 요구사항 정의서가 우선한다.

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

정본은 `docs/ZeroWiki-요구사항-정의서.md` 12절이다. 아래는 그중 API 계약에 직접 영향을 주는 항목이며, **UD 번호가 문서 간 공통 식별자**다. 어떤 에이전트도 단독 확정할 수 없다(헌법 제3조).

| UD | 확정할 계약 | 영향받는 절 | 결정 주체 |
| --- | --- | --- | --- |
| `UD-01` | 파일당·ZIP당·Import당 최대 크기와 문서 수 | 5.2, `413` 응답 | 사용자 |
| `UD-02` | Access Token과 Refresh Token의 수명 | 2.1, 3.1 | 사용자 |
| `UD-03` | Free·Basic·Advanced 처리량 단위 | 12 | 사용자 |
| `UD-28` | 도서관 홈 `topics`의 산출 기준 | 4.1 | 기획(pm) |
| `UD-06` | 사용자 직접 편집 중 안전 변경의 자동 적용 여부 | 7.1 | 사용자 |
| `UD-07` | 검색 결과 원본 발췌문 최대 길이 | 9.1, 7.2 | 사용자 |
| `UD-08` | 공개 URL slug 충돌과 변경 정책 | 14.1 | 기술 |
| `UD-09` | 관리자·운영 API의 OpenAPI 분리 여부 | 16 | 기술 |
| `UD-10` | Notion OAuth와 결제 공급자 선정 | 6.2, 12 | 사용자 |
| `UD-21` | 월간 Lint 실행 시각·타임존 기준과 대상 선정 | 10 | 기술 |
| `UD-23` | 알림 보존 기간과 읽음 처리 정책 | 11 | 기술 |

ERD 측 미확정(`UD-11`~`UD-20`)과 벤치마크 의존 항목(`UD-25`, `UD-26`)은 요구사항 정의서 12절을 본다.

**확정된 항목 (2026-07-29 사용자 결정):**

| UD | 확정 내용 | 결정 주체 | 반영 위치 |
| --- | --- | --- | --- |
| `UD-24` | 위키 본문은 원본 언어와 무관하게 한국어로 종합 | 사용자 | 1절 원칙 10·11번, 2.8절 |
| `UD-27` | 전문 용어는 초회 등장 1회 병기, 정의·핵심 주장에는 유지 | 사용자 | 2.8절 |
| `UD-04` | `Retry-After` 우선 + 클라이언트 기본값(Ingest 2초 / AI 답변 1초 / Lint·Export 5초). 최대 생성 시간은 `UD-25`에 종속 | 기술(리더) | 2.7절 |
| `UD-05` | Phase 1은 스냅샷 반환 + 클라이언트 계산, Phase 2에 서버 구조화 블록 diff 추가 | 기술(리더) | 7.1절 |

`UD-04`·`UD-05`는 frontend 권고(2026-07-29)를 근거로 리더가 확정했다. 두 항목은 요구사항 정의서 12.1절에서 결정 주체가 **기술**이므로 사용자 결정을 기다리지 않는다.

## 19. 요구사항 추적

각 절이 근거로 삼는 요구사항이다. 요구사항 정의서 13.2절의 역방향 매핑에 해당한다.

| API 절 | 근거 요구사항 |
| --- | --- |
| 2.1, 3 | FR-ACC-01~04 |
| 2.5, 2.6 | FR-CHG-13, FR-ING-24, NFR-OPS-03 |
| 2.7 | FR-ING-20, FR-ING-25 |
| 2.8 | FR-ING-13, FR-KNW-06, FR-UIX-12·13 |
| 4.1 | FR-LIB-01~03, FR-LIB-08~10, FR-UIX-02 |
| 4.2 | FR-LIB-11~14 |
| 4.3 | FR-LIB-04·05, NFR-SEC-03 |
| 5.1 | FR-SRC-01~08 |
| 5.2 | FR-INP-02·10~12, NFR-SEC-04 |
| 5.3 | FR-INP-04~09·13 |
| 6.1 | FR-ING-01~09, FR-ING-19~26 |
| 6.2 | FR-INP-03 |
| 7.1 | FR-KNW-03, FR-CHG-06·09·11 |
| 7.2 | FR-KNW-05~07·09, FR-UIX-13 |
| 7.3 | FR-KNW-08·10·12, FR-FBK-01~04 |
| 8 | FR-CHG-01~05·08·10 |
| 9.1 | FR-SCH-01~05, FR-LIB-06·07, NFR-SEC-03 |
| 9.2 | FR-ASK-01~11 |
| 10 | FR-LNT-01~07 |
| 11 | FR-NTF-01~05, FR-ACC-08, FR-UIX-06 |
| 12 | FR-BIL-01~09 |
| 13 | FR-EXP-01·02·05 |
| 14 | FR-PUB-01~17 |
| 15 | NFR-SEC-01~10 (13·14·15번은 FR-SRC-05, FR-LIB-07·FR-ASK-07, NFR-SEC-01에 각각 대응) |
| 17 | 9절 Phase 정의 |
