# 프론트엔드 화면·API 갭 검증

작성일: 2026-07-27  
상태: 프론트엔드 관점 교차 검증  
기준 문서:
- `ZeroWiki-MVP-서비스-기획서.md`
- `ZeroWiki-API-명세-초안.md`
- `ZeroWiki-ERD-초안.md`

## 1. 화면별 필요 API 매핑

이 절에서는 기획서 16절의 핵심 화면과 8절 온보딩, 11절 변경 검토 화면을 그리는 데 필요한 API 호출을 매핑한다.

### 1.1 글로벌 홈 (기획서 16.1)

**화면 구성**
- 중앙: AI 사서 입력창
- 왼쪽: 도서관과 주제 탐색기
- 상단: 전체 검색
- 보조 영역: 최근 Ingest, 위험 변경, 정기 Lint, 포크 업데이트 알림

| 화면 요소 | 필요 API | 사용 필드 | 호출 수 | 비고 |
| --- | --- | --- | --- | --- |
| 사용자 정보 | `GET /me` | `displayName`, `plan` | 1 | |
| 도서관 목록 | `GET /libraries` | `id`, `name`, `counts.pages`, `counts.pendingChanges` | 1 | cursor pagination |
| 주제 탐색기 | `GET /libraries/{libraryId}/pages?type=CONCEPT,FLOW` | `id`, `slug`, `title`, `relatedPages` | N | 도서관 수만큼 호출 |
| AI 사서 입력 | `POST /libraries/{libraryId}/conversations` | (입력만) | - | 대화 생성 |
| 전체 검색 | `GET /libraries/{libraryId}/search?q=...` | `resultType`, `title`, `snippet`, `pageId`, `sourceId` | 1 | 검색 결과 (페이지, 원본, 주장) |
| 최근 Ingest 알림 | `GET /libraries/{libraryId}/activities?type=INGEST` | `id`, `type`, `title`, `metadata.documentCount` | 1 | |
| 위험 변경 | `GET /libraries/{libraryId}/change-sets?status=READY_FOR_REVIEW&riskLevel=HIGH` | `id`, `title`, `riskLevel`, `counts.pendingItems` | 1 | |
| 정기 Lint 알림 | `GET /notifications?type=LINT_COMPLETED` | `id`, `title`, `resourceId`, `createdAt` | 1 | |
| 포크 업데이트 알림 | `GET /libraries/{libraryId}/forks?hasUpdates=true` | `id`, `title`, `sourcePageTitle` | 1 | |

**N+1 호출 문제**
- 도서관 수가 증가하면 주제 탐색기 `GET /libraries/{libraryId}/pages` 호출이 기하급수적으로 증가한다.
- 다중 도서관 정보를 한 번에 조회하는 통합 API 부재(갭 #1 참조).

---

### 1.2 도서관 홈 (기획서 16.2)

**화면 구성**
1. 주요 주제
2. AI가 최근 발견한 연결
3. 열린 질문과 지식 공백
4. 모순과 검토 대기 변경
5. 최근 활동

| 화면 요소 | 필요 API | 사용 필드 | 호출 수 | 비고 |
| --- | --- | --- | --- | --- |
| 도서관 홈 요약 | `GET /libraries/{libraryId}` | `name`, `counts.*, currentVersion` | 1 | API 명세 4.1절: "요약을 포함할 수 있다" → 명확한 스키마 필요(갭 #2) |
| 주요 주제 | 위 응답에 포함 | `topicSummary` | - | |
| 최근 발견 연결 | `GET /libraries/{libraryId}/relations?status=PROPOSED&sort=createdAt,desc&limit=5` | `id`, `sourcePageId`, `targetPageId`, `relationType`, `confidence`, `createdAt` | 1 | 제한된 결과(5개) |
| 열린 질문 | `GET /libraries/{libraryId}/lint-findings?type=KNOWLEDGE_GAP&status=OPEN&limit=5` | `id`, `description`, `targetId` | 1 | Lint 결과 활용 |
| 모순 | `GET /libraries/{libraryId}/contradictions?status=OPEN&limit=5` | `id`, `leftClaimId`, `rightClaimId`, `confidence` | 1 | |
| 검토 대기 변경 | `GET /libraries/{libraryId}/change-sets?status=READY_FOR_REVIEW&limit=5` | `id`, `title`, `riskLevel`, `counts.items` | 1 | |
| 최근 활동 | `GET /libraries/{libraryId}/activities?limit=10` | `id`, `type`, `title`, `metadata`, `createdAt` | 1 | 모든 활동 유형 통합 |

**지원되지 않는 조회**
- "주요 주제"의 명확한 정의 부재 → AI가 생성한 개념 페이지? 최상위 분류? (갭 #2)
- 도서관 홈 요약 응답 스키마가 API 명세에서 "다음을 포함할 수 있다"로만 표현되어 선택사항 불명확 (갭 #3)

---

### 1.3 문서 페이지 (기획서 16.3)

**화면 구성**
- 문서 본문을 바로 표시
- 관련 페이지와 주제
- 주장 상태
- 변경 이력
- 근거 보기
- AI에게 이 페이지 질문

| 화면 요소 | 필요 API | 사용 필드 | 호출 수 | 비고 |
| --- | --- | --- | --- | --- |
| 페이지 본문 | `GET /libraries/{libraryId}/pages/{pageId}` | `title`, `markdown_body`, `summary`, `version` | 1 | 현재 버전 포함 |
| 관련 페이지 | 위 응답에 포함 | `relatedPages[].{pageId, title, relationType, rationale}` | - | API 명세 7.1절에 포함 |
| 주제/태그 | 위 응답에 포함 | `topics[]` | - | 명세에 없음 (갭 #4) |
| 주장 상태 | `GET /libraries/{libraryId}/pages/{pageId}/claims` | `id`, `statement`, `knowledgeStatus`, `confidence` | 1 | |
| 근거 상세 | `GET /libraries/{libraryId}/claims/{claimId}/evidences` | `sourceVersionId`, `sourceTitle`, `locator`, `excerpt` | N | 주장별 호출(과도할 수 있음) |
| 변경 이력 | `GET /libraries/{libraryId}/pages/{pageId}/versions` | `versionNo`, `title`, `createdAt`, `createdByType` | 1 | |
| 페이지 질문 | `POST /libraries/{libraryId}/conversations` | (질문 내용만 입력) | - | 새 대화 또는 기존 대화에 메시지 추가 |

**이력 내용 조회**
- 변경 이력의 "diff 보기"를 클릭할 때 추가 호출 필요: `GET /libraries/{libraryId}/pages/{pageId}/diff?from={versionNo1}&to={versionNo2}` | 1 | (지연 로딩) |

**N+1 호출 문제**
- 여러 주장의 근거를 모두 표시하려면 주장별로 `GET /claims/{claimId}/evidences` 호출 필요 → **배치 근거 조회 API 필요 (갭 #5)**

---

### 1.4 보조 그래프 (기획서 16.4)

**화면 구성**
- 지식 구조 시각화
- 최근 Ingest로 새로 생긴 노드와 연결 강조
- 기간별 활동 잔디

| 화면 요소 | 필요 API | 사용 필드 | 호출 수 | 비고 |
| --- | --- | --- | --- | --- |
| 관계 그래프 | `GET /libraries/{libraryId}/relations?status=ACCEPTED` | `sourcePageId`, `targetPageId`, `relationType`, `confidence` | 1 | 그래프 시각화용 노드·엣지 |
| 페이지 목록 (노드) | `GET /libraries/{libraryId}/pages` | `id`, `slug`, `title`, `pageType`, `status` | 1 | 모든 페이지를 노드로 변환 |
| 최근 Ingest 강조 | `GET /libraries/{libraryId}/activities?type=INGEST&since={7daysAgo}` | `metadata.documentCount`, `metadata.newPageIds` | 1 | 최근 추가된 노드 강조 |
| 활동 잔디 | **API 없음** | 날짜별 활동 수 | - | **갭 #6: 활동 집계 API 필요** |

**그래프 페이로드 형태 문제**
- 관계 응답이 `{sourcePageId, targetPageId, relationType}`만 포함하는데, 프론트에서는 페이지 제목도 필요할 수 있음.
- **N+1 호출 회피: 관계 응답에 페이지 제목을 포함하거나, 페이지 목록과 관계를 분리한 후 클라이언트에서 조인할지 미정 (갭 #7)**

---

### 1.5 온보딩 (기획서 8절)

**화면 구성**
필수 질문 5개:
1. 이 도서관으로 무엇을 알고 결정하거나 만들어내고 싶은가?
2. 어떤 종류의 자료를 넣을 예정인가?
3. 현재 해당 분야에 대해 어느 정도 알고 있는가?
4. 어떤 출처와 관점을 더 신뢰하는가?
5. AI가 반드시 물어봐야 하거나 조심해야 할 영역은 무엇인가?

| 화면 요소 | 필요 API | 사용 필드 | 호출 수 | 비고 |
| --- | --- | --- | --- | --- |
| 도서관 생성 + 초기 헌법 | `POST /libraries` | `name`, `description`, `templateType`, `onboarding` | 1 | 요청에 5개 질문 답변 포함 |
| 초기 헌법 반영 | 위 응답에 포함 | `constitutionVersion: 1` | - | |

**성공 기준**
- 온보딩 완료 후 빈 도서관으로 진입 가능해야 한다.

---

### 1.6 변경 검토 화면 (기획서 11절, API 명세 8.1절)

**화면 구성** (GitHub PR 유사)
- 변경 이유
- 영향을 받는 페이지
- 변경 전후 diff
- 근거 출처
- AI 신뢰도
- 승인 / 거절 / 수정 요청 / 판단 보류

| 화면 요소 | 필요 API | 사용 필드 | 호출 수 | 비고 |
| --- | --- | --- | --- | --- |
| 변경 세트 상세 | `GET /libraries/{libraryId}/change-sets/{changeSetId}` | `id`, `title`, `summary`, `riskLevel`, `status`, `baseLibraryVersionId` | 1 | |
| 변경 항목 목록 | `GET /libraries/{libraryId}/change-sets/{changeSetId}/items` | `changeItemId`, `targetType`, `targetId`, `operation`, `riskLevel`, `reason`, `reviewStatus` | 1 | |
| 변경 전후 데이터 | 위 응답에 포함 | `beforeSnapshot`, `afterSnapshot` | - | jsonb로 표현 → diff 생성은 클라이언트? 서버? (갭 #8) |
| 근거 출처 | 위 응답에 포함 | `evidenceSummary` | - | |
| AI 신뢰도 | 위 응답에 포함 | `aiConfidence` | - | |
| 변경 검토 제출 | `POST /libraries/{libraryId}/change-sets/{changeSetId}/reviews` | `decisions[]{changeItemId, decision, comment}` | 1 | |

**갭 #8: diff 표현 미확정**
- API 명세 18절 5번: "unified diff vs 구조화 블록 diff" 미확정
- 현재 명세에서 `beforeSnapshot`, `afterSnapshot`은 `jsonb`로 전체 객체를 반환함
- 클라이언트가 직접 diff를 생성할 때 형식 불명확 (작업 #4에서 권고안 제시)

---

### 1.7 모바일 반응형 웹 (기획서 16.5)

**지원 범위**
- AI 질문과 답변
- 페이지 탐색과 읽기
- 자료 업로드
- 변경 승인과 알림 확인

| 화면 요소 | 필요 API | 호출 수 | 비고 |
| --- | --- | --- | --- |
| AI 질문 | `POST /libraries/{libraryId}/conversations/{conversationId}/messages` | 1 | |
| 답변 조회 (polling) | `GET /libraries/{libraryId}/conversations/{conversationId}/messages/{messageId}` | N | polling 간격 미확정(갭 #9) |
| 페이지 목록 | `GET /libraries/{libraryId}/pages` | 1 | |
| 페이지 읽기 | `GET /libraries/{libraryId}/pages/{pageId}` | 1 | |
| 자료 업로드 | `POST /uploads` → `POST /uploads/{uploadId}/complete` | 2 | 3단계 업로드 |
| 변경 승인 | `GET /libraries/{libraryId}/change-sets?status=READY_FOR_REVIEW` | 1 | |
| 변경 검토 제출 | `POST /libraries/{libraryId}/change-sets/{changeSetId}/reviews` | 1 | |
| 알림 | `GET /notifications` | 1 | |

---

### 1.8 종합 호출 분석

| 화면 | 동기 호출 | 비동기(polling) | 지연 로딩 | 총 호출 | N+1 위험 |
| --- | --- | --- | --- | --- | --- |
| 글로벌 홈 | 8 | 0 | 0 | 8 | **높음** (도서관 수×1) |
| 도서관 홈 | 6 | 0 | 0 | 6 | 낮음 |
| 문서 페이지 | 5 | 0 | 2 | 7+ | **높음** (주장 수×1) |
| 보조 그래프 | 3 | 0 | 0 | 3 | 중간 |
| 온보딩 | 1 | 0 | 0 | 1 | 없음 |
| 변경 검토 | 3 | 0 | 0 | 3 | 낮음 |

---

## 2. 프론트엔드 관점 API 갭 목록

### 갭 #1: 글로벌 홈의 전체 도서관 횡단 조회

**영향 화면**: 글로벌 홈  
**현재 상황**: 도서관 목록 `GET /libraries`는 기본 정보만 반환. 각 도서관의 주제, 활동을 보이려면 도서관별로 `GET /libraries/{libraryId}/pages?type=CONCEPT,FLOW` 호출 필요.  
**문제**: N+1 호출 발생.  
**심각도**: **MAJOR**

**제안**:
- `GET /libraries` 응답에 `topicSummary: [{topicName, pageCount}]` 포함
- 또는 별도 `GET /dashboard/libraries-overview` 엔드포인트로 모든 도서관의 최상위 통계 반환

---

### 갭 #2: 도서관 홈 요약 응답 스키마 명확화

**영향 화면**: 도서관 홈  
**현재 상황**: API 명세 4.1절에서 `GET /libraries/{libraryId}`가 "다음을 포함할 수 있다" 방식으로 선택사항 명시. 클라이언트 입장에서 응답에 `topicSummary`, `recentRelations` 등이 있는지 없는지 불명확.  
**문제**: 선택 필드의 존재 여부를 가정하고 코드를 작성하면 런타임 에러 발생 가능.  
**심각도**: **MAJOR**

**제안**:
- 도서관 홈 요약 응답에 포함되는 필드를 명시적으로 정의
- 필드 생략은 하지 말고 `null` 또는 빈 배열로 반환하거나, 별도 엔드포인트 분리 권고

```json
{
  "data": {
    "id": "...",
    "name": "...",
    "topicSummary": [],
    "recentRelations": [],
    "openQuestions": [],
    "contradictions": [],
    "pendingChanges": 0,
    "activities": []
  }
}
```

---

### 갭 #3: "주요 주제" 정의 부재

**영향 화면**: 도서관 홈 (요소 1)  
**현재 상황**: 기획서에서 "주요 주제"라고 표현하지만, 구체적으로 무엇을 보여줄지 불명확. AI가 자동 생성한 개념? 상위 분류? 사용자가 정의한 카테고리?  
**문제**: UI 설계와 API 설계가 엇갈릴 수 있음.  
**심각도**: **MAJOR**

**제안**:
- 기획서에서 "주요 주제는 Ingest 후 AI가 자동 분류한 CONCEPT 페이지 중 관계 수 상위 N개"처럼 구체화
- API: `GET /libraries/{libraryId}/pages?type=CONCEPT&sort=relationCount,desc&limit=10`

---

### 갭 #4: 페이지의 주제/태그 필드 부재

**영향 화면**: 문서 페이지  
**현재 상황**: API 명세 7.1절 페이지 응답에 `pageType`, `status` 등은 있지만, 사용자가 쉽게 식별할 `topic`, `tags`, `categories` 필드 없음.  
**문제**: 기획서에서 "관련 페이지와 주제"를 보여주려면 추가 필드 필요.  
**심각도**: **MINOR**

**제안**:
- 페이지 응답에 `topics: string[]` 필드 추가
- 또는 페이지 검색 시 `topic` 필터 활용: `GET /libraries/{libraryId}/pages?topic=consensus`

---

### 갭 #5: 배치 근거 조회 API 부재

**영향 화면**: 문서 페이지 (근거 보기)  
**현재 상황**: 한 페이지의 여러 주장을 보여줄 때, 각 주장의 근거를 조회하려면 `GET /claims/{claimId}/evidences` 호출을 주장 수만큼 반복.  
**예**: 주장 12개 → 12번의 호출  
**심각도**: **MAJOR**

**제안**:
- `GET /libraries/{libraryId}/pages/{pageId}/evidences?claimIds={id1},{id2},...` 배치 조회 엔드포인트
- 또는 페이지 응답에 `claimsWithEvidences` 중첩 구조로 포함

```json
{
  "data": {
    "id": "...",
    "claims": [
      {
        "id": "...",
        "statement": "...",
        "evidences": [...]
      }
    ]
  }
}
```

---

### 갭 #6: 활동 집계 API (활동 잔디)

**영향 화면**: 보조 그래프 (활동 잔디)  
**현재 상황**: API 명세에 `GET /libraries/{libraryId}/activities`는 있으나, 날짜별 활동 수 집계는 없음. 클라이언트가 모든 활동을 받아 메모리에서 집계해야 함.  
**문제**: 대규모 활동이 많으면 pagination으로 여러 페이지 조회 필요 → 초기 로딩 느림.  
**심각도**: **MINOR**

**제안**:
- `GET /libraries/{libraryId}/activities/summary?granularity=DAY&since={date}` 추가
- 응답: `{date: "2026-07-27", count: 5, types: {INGEST: 2, CHANGE: 3}}`

---

### 갭 #7: 그래프 시각화 페이로드 형태 미정

**영향 화면**: 보조 그래프  
**현재 상황**: 관계 조회 `GET /libraries/{libraryId}/relations`이 `sourcePageId`, `targetPageId` ID만 반환. 그래프를 그리려면 페이지 제목도 필요한데, 분리된 `GET /pages` 호출로 조회해야 함.  
**문제**: 프론트에서 두 응답을 조인해야 하며, 페이지 수가 많으면 ID 맵 생성 오버헤드.  
**심각도**: **MINOR**

**제안**:
1. 관계 응답에 페이지 제목 포함 (응답 크기 증가)
   ```json
   {
     "sourcePageId": "...",
     "sourcePageTitle": "...",
     "targetPageId": "...",
     "targetPageTitle": "..."
   }
   ```
2. 또는 클라이언트가 페이지 목록을 먼저 받은 후 ID 맵으로 제목 채우기 (권장: 캐싱 용이)

---

### 갭 #8: diff 표현 형식 미확정

**영향 화면**: 변경 검토 화면 (diff 보기)  
**현재 상황**: API 명세 18절 5번에서 "unified diff vs 구조화 블록 diff" 미결정. 현재 스키마는 `beforeSnapshot`, `afterSnapshot` 전체 `jsonb` 반환.  
**문제**: 페이지 본문(markdown)의 diff를 어떻게 표현할지 불명확.  
**심각도**: **BLOCKER** (작업 #4에서 근거와 권고안 제시 필요)

**임시 상황**:
- 현재 명세: `beforeSnapshot`, `afterSnapshot` 전체 객체
- 클라이언트는 이를 받아 diff 라이브러리(e.g., diff-match-patch)로 계산해야 함
- 서버가 사전에 diff를 계산하면 프론트 부담 감소

---

### 갭 #9: 비동기 작업 polling 간격 미확정

**영향 화면**: 모든 비동기 작업 (Ingest, AI 답변, Lint, Export)  
**현재 상황**: API 명세 2.7절에서 "MVP는 polling 기본"이라고만 하고, 구체적 간격(1초? 5초? 10초?)은 18절 4번 미확정 계약 목록에 포함.  
**문제**: 간격이 너무 짧으면 서버 부하, 너무 길면 UX 지연.  
**심각도**: **MAJOR** (작업 #3에서 권고안 제시 필요)

**현재 제약**:
- API: `Retry-After` 헤더 반환 가능 (선택사항)
- 클라이언트는 이를 무시할 수 있음 → 서버 부하 조절 불가

---

### 갭 #10: 알림 실시간성 부재

**영향 화면**: 글로벌 홈 (알림 영역)  
**현재 상황**: API 명세 17절에서 "서비스 내부 알림만 지원, 추후 SSE 추가"라고 명시. MVP는 polling만 지원.  
**문제**: 사용자가 화면을 보고 있어도 위험 변경, Lint 완료 같은 중요 알림이 지연될 수 있음.  
**심각도**: **MINOR** (MVP 범위 명시)

**권고**:
- MVP에서는 사용자가 새로고침했을 때 `GET /notifications`로 최신 정보 조회
- Phase 2에서 SSE 또는 WebSocket 추가 계획

---

### 갭 #11: 도서관 참조 검색 응답 형태

**영향 화면**: AI 사서 (연결 도서관 확장 검색)  
**현재 상황**: API 명세 9.1절에서 `POST /libraries/{libraryId}/search-expansions`로 연결 도서관 검색을 별도 호출. 하지만 응답 형태가 명시되지 않음.  
**문제**: 클라이언트가 "현재 도서관 결과"와 "참조 도서관 결과"를 분리해서 보여야 하는데, 응답에서 출처(source) 정보가 명확하지 않음.  
**심각도**: **MINOR**

**제안**:
- 응답에 `source: "CURRENT" | "REFERENCED"` 필드 추가
- 또는 결과를 `currentLibrary`, `referencedLibraries` 그룹으로 분리

---

### 갭 #12: Ingest 계획 승인 후 상태 전이 명확화

**영향 화면**: Ingest 화면 (계획 검토)  
**현재 상황**: API 명세 6.1절에서 `PLAN_REVIEW → PROCESSING` 전이가 자명하지 않음. 계획 승인 직후 상태가 뭔지(아직 PLAN_REVIEW? PROCESSING? PROCESSING 중이지만 계산 중?), nextAction이 뭔지 불명확.  
**문제**: UI에서 "계획 승인됨" 표시와 프로그레스 진행도를 어떻게 조합할지 불명확.  
**심각도**: **MAJOR** (작업 #3에서 상태 머신 검증)

---

## 3. 비동기 작업 UX 흐름 검증

이 절에서는 API 명세 2.7절의 202 Accepted + polling 모델이 실제 사용자 흐름을 지탱하는지 검증한다.

### 3.1 대량 Ingest 흐름 (기획서 10절, API 명세 6절)

**예상 상태 전이**

```
QUEUED → SCANNING 
  → PLAN_REVIEW (nextAction: APPROVE_PLAN) 
    [사용자 승인]
  → PROCESSING 
    → QUESTION_WAITING (nextAction: ANSWER_QUESTIONS) 
      [사용자 답변]
    → PROCESSING 
  → CHANGE_REVIEW (nextAction: REVIEW_CHANGES) 
    [사용자 검토 및 승인]
  → COMPLETED
```

**상태별 UI 시나리오**

| 상태 | nextAction | 사용자 화면 | 문제점 |
| --- | --- | --- | --- |
| QUEUED | null | "대기 중..." (로딩) | |
| SCANNING | null | 진행도 표시 (X / 100) | |
| PLAN_REVIEW | APPROVE_PLAN | 계획 검토 화면으로 이동 필요 | Ingest 상세(`GET .../ingest-jobs/{jobId}`)에서 `plan` 필드 포함되는가? → YES (API 명세 6.1절) |
| PLAN_REVIEW + [승인] | - | 승인 후 PROCESSING으로 상태 변이 기다리는 동안 UI 피드백 | `202 Accepted` 응답만 주고 비동기 상태 변이 → polling 필요 |
| PROCESSING | null | 진행도 표시, "문서별 처리 중..." | 진행도 `progress.completed / progress.total` 포함? → YES |
| QUESTION_WAITING | ANSWER_QUESTIONS | 질문 목록 화면으로 이동 필요 | `GET .../ingest-jobs/{jobId}/questions` → YES |
| [답변 제출] | - | 답변 후 PROCESSING 재개 대기 | polling으로 상태 감시 |
| CHANGE_REVIEW | REVIEW_CHANGES | 변경 검토 화면으로 이동 필요 | `changeSetId` 포함? API 명세 6.1절: "처리가 끝나면... changeSetId를 반환" → 추론: `change_set_id` 응답 필드 필수 확인 (갭 #13) |
| COMPLETED | - | "완료" 표시, 도서관 버전 발행됨 | 최종 상태에서 `libraryVersion` 정보 필요? (갭 #14) |

**사용자가 화면을 떠났다 돌아온 경우**

1. 사용자가 Ingest 진행 중 다른 페이지로 이동
2. 나중에 도서관 홈으로 복귀
3. **예상 행동**: 현재 상태를 알아야 함 → `GET /libraries/{libraryId}/ingest-jobs`로 최신 작업 조회 필요
4. **확인 사항**: 
   - 진행 중인 Ingest 작업이 명확하게 식별되는가? (여러 작업이 동시 진행 중이면?)
   - **갭 #15**: 진행 중인 작업 표시 API 명확화 필요

**polling 간격의 UX 영향**

- 간격 1초: SCANNING → PLAN_REVIEW 전이 즉시 감지 가능 (좋음), 서버 부하 증가
- 간격 5초: 최악의 경우 5초 지연 후 화면 전환 (사용자 답답함)
- 간격 10초: PROCESSING 단계에서 진행도 표시가 둔함 → UX 저하
- **권고**: Ingest의 단계별로 다른 간격 사용
  - SCANNING, PROCESSING (진행도 있음): 2~3초
  - PLAN_REVIEW, QUESTION_WAITING (사용자 입력 대기): 1초 (또는 즉시 재조회)

---

### 3.2 AI 사서 답변 생성 흐름 (API 명세 9.2절)

**상태 전이**

```
POST /conversations/{conversationId}/messages (질문 전송)
  → 202 Accepted, {assistantMessageId, status: QUEUED}
  
polling GET /conversations/{conversationId}/messages/{messageId}
  → status: QUEUED → GENERATING → COMPLETED
```

**상태별 UI**

| 상태 | 사용자 화면 | polling 간격 |
| --- | --- | --- |
| QUEUED | "답변 준비 중..." | 1초 |
| GENERATING | "생각 중..." + 진행도? | 1초 (진행도 불가능) |
| COMPLETED | 답변 텍스트 표시 | - |
| FAILED | "오류 발생" + 재시도 버튼 | - |

**문제점**

- API 명세 2.7절에서 답변 생성 상태는 `progress` 필드가 없음 → 진행도 표시 불가능
- 답변이 "생각 중"인지 "거의 다 되었는지" 알 수 없음 → **갭 #16: 답변 생성 진행도 필드 추가**

**사용자 화면 이탈**

- 사용자가 답변 생성 중 다른 페이지로 이동
- 복귀 후 `GET /conversations/{conversationId}` (메시지 목록)으로 답변 상태 확인 가능
- **API 명세에서 명확한지 확인**: 메시지 목록 조회 시 진행 중인 메시지의 `status`가 유지되는가? → 명세 9.2절에서 "초기 응답"은 `status: QUEUED`이지만, 목록 조회 시 상태도 추적되는지 명시 필요 (갭 #17)

---

### 3.3 Lint 실행 흐름 (API 명세 10절)

**상태 전이**

```
POST /libraries/{libraryId}/lint-runs
  → 202 Accepted, {lintRunId, status: QUEUED}

polling GET /lint-runs/{lintRunId}
  → status: QUEUED → RUNNING → COMPLETED
```

**특이점**

- 대량 Ingest와 달리 중간 단계 사용자 입력 없음
- 완료 후 자동 알림 발송 (API 명세 17절)

**UI**

- QUEUED / RUNNING: 진행도 표시
- COMPLETED: Lint 결과 화면으로 이동 또는 알림

---

### 3.4 Export 흐름 (API 명세 13절)

**상태 전이**

```
POST /libraries/{libraryId}/exports
  → 202 Accepted, {exportId, status: QUEUED}

polling GET /exports/{exportId}
  → status: QUEUED → GENERATING → COMPLETED
  → 다운로드 URL 생성
```

**문제점**

- 상태는 3가지(QUEUED, GENERATING, COMPLETED) → 진행도 필드 없음
- ZIP 크기가 클 때 사용자에게 "어느 정도 진행되었는지" 알려줄 수 없음
- **갭 #18: Export 진행도 필드 추가**

---

### 3.5 종합 검증

| 흐름 | 상태 단계 | 중간 사용자 입력 | 진행도 표시 | 화면 이탈 복구 | polling 간격 권고 |
| --- | --- | --- | --- | --- | --- |
| Ingest | 7+ | YES (2회) | YES | ✓ (`/ingest-jobs`) | 2~3초 (단계별) |
| AI 답변 | 3 | NO | NO | ✓ (`/conversations`) | 1초 (또는 즉시) |
| Lint | 3 | NO | YES | ✓ (`/lint-runs`) | 2초 |
| Export | 3 | NO | NO | ✓ (`/exports`) | 5초 |

**nextAction 필드만으로 UI 분기 충분한가?**

- Ingest: APPROVE_PLAN, ANSWER_QUESTIONS, REVIEW_CHANGES, PURCHASE_QUOTA, RETRY_FAILED_ITEMS → **충분**
- AI 답변: nextAction 필드 없음 (status로만 판단) → **불충분** (갭 #19)
- Lint: nextAction 필드 없음 → **불충분** (갭 #20)
- Export: nextAction 필드 없음 → **불충분** (갭 #21)

**권고**: 모든 비동기 작업에 `nextAction` 필드 추가

---

## 4. 변경 검토 화면 데이터 요구와 diff 표현 결론

기획서 11절의 GitHub PR 유사 변경 검토 화면을 분석한다.

### 4.1 화면 요소와 데이터 요구

**기획서 11절 변경 검토 화면 구성**

1. 변경 이유
2. 영향을 받는 페이지
3. 변경 전후 diff
4. 근거 출처
5. AI 신뢰도
6. 승인 / 거절 / 수정 요청 / 판단 보류 4분기

**API 명세 8.1절 변경 세트 응답 대조**

| 화면 요소 | API 필드 | 응답 포함 | 상태 |
| --- | --- | --- | --- |
| 변경 이유 | `change_sets.summary` | YES | ✓ |
| 영향 페이지 | `change_items[].before_snapshot.target_id`? | 불명확 | **갭 #22** |
| 전후 diff | `change_items[].before_snapshot`, `.after_snapshot` | YES (jsonb) | ✓ (형식 미정) |
| 근거 출처 | `change_items[].evidence_summary` | YES | ✓ |
| AI 신뢰도 | `change_items[].ai_confidence` | YES | ✓ |
| 승인/거절/수정 | `POST .../reviews` (요청 필드) | YES | ✓ |

### 4.2 영향을 받는 페이지 목록 명확화

**문제점**

- API 명세 8.1절에서 `GET /change-sets/{changeSetId}` 응답에 "영향을 받는 페이지"가 명시되지 않음
- `change_items[].before_snapshot`에는 변경 대상이 있지만, "이 변경으로 인해 영향을 받을 다른 페이지"는 표현되지 않음
- 예: 페이지 A를 수정하면 → 페이지 B, C와의 관계가 깨질 수 있음 (사용자에게 미리 알려야 함)

**갭 #22: 변경의 영향 범위 API**

**제안**:
- `GET /change-sets/{changeSetId}/impact` → `affectedPages[]` 응답
- 또는 `change_set` 응답에 `impactedPages` 필드 추가

```json
{
  "data": {
    "id": "...",
    "title": "...",
    "affectedPages": [
      {
        "pageId": "...",
        "title": "...",
        "reason": "관계 깨짐",
        "severity": "MAJOR"
      }
    ]
  }
}
```

### 4.3 diff 표현 형식: unified vs 구조화 블록

**API 명세 18절 5번 미확정 계약**

**현재 스키마**

API 명세 8.1절: `change_items`의 응답

```json
{
  "before_snapshot": {
    "title": "Old Title",
    "markdown_body": "# Old\n...",
    "summary": "..."
  },
  "after_snapshot": {
    "title": "New Title", 
    "markdown_body": "# New\n...",
    "summary": "..."
  }
}
```

**선택지 1: Unified Diff (문자열)**

서버가 diff를 사전 계산해서 반환

```json
{
  "diff": "--- a/pageTitle\n+++ b/pageTitle\n@@ -1,3 +1,3 @@\n-Old Title\n+New Title\n..."
}
```

**장점**
- 표준 형식 (git, GitHub 등에서 사용)
- 클라이언트가 diff 라이브러리 필요 없음 (또는 가벼운 렌더링만)

**단점**
- markdown에서 문법 강조 표현 어려움
- 블록 단위 변경 표현 불편 (예: 문단 전체 교체)

---

**선택지 2: 구조화 블록 Diff**

markdown을 블록으로 파싱한 후 변경 표현

```json
{
  "blocks": [
    {
      "type": "HEADING",
      "level": 1,
      "before": "Old Title",
      "after": "New Title",
      "status": "CHANGED"
    },
    {
      "type": "PARAGRAPH",
      "before": "...",
      "after": "...",
      "status": "UNCHANGED"
    }
  ]
}
```

**장점**
- 사용자가 "어느 문단이 바뀌었는지" 명확히 볼 수 있음
- 문법 강조 용이
- UI에서 블록별 승인/거절 가능 (향후)

**단점**
- 서버에서 markdown 파싱 필요 (복잡도 증가)
- 클라이언트도 구조를 해석해야 함
- 표준이 아님

---

**선택지 3: 클라이언트 diff 계산 (현재 명세)**

서버는 before/after 스냅샷만 반환, 클라이언트가 diff 라이브러리로 계산

```javascript
// 클라이언트 (예: diff-match-patch 라이브러리)
const diffs = computeDiff(before.markdownBody, after.markdownBody);
```

**장점**
- 서버 부담 최소화
- 클라이언트 유연성 (diff 알고리즘 선택 가능)

**단점**
- 모든 클라이언트가 구현해야 함
- 대용량 문서에서 계산 느릴 수 있음 (UX 저하)
- 통일된 표현 보장 안 됨

---

### 4.4 프론트엔드 권고안

**권고: 선택지 2 (구조화 블록 Diff) + 선택지 3 (클라이언트 fallback)**

**근거**

1. **사용자 경험**: 문단 단위 변경이 눈에 띄고, 마크다운 문법을 무시한 순수 콘텐츠 변경 강조
2. **향후 확장성**: 블록별 선택적 승인 (예: "문단 1은 승인, 문단 2는 거절")이 가능
3. **점진적 도입**: 
   - Phase 1: 선택지 3 (클라이언트 계산) → MVP 릴리스
   - Phase 2: 서버가 구조화 diff 제공 → UI 개선

**Phase 1 구현**

```typescript
// 클라이언트: markdown diff 계산
import { diffLines } from 'diff';

const diffs = diffLines(before.markdownBody, after.markdownBody);
// 결과: [{value: "...", added: true|false|undefined}, ...]

// markdown을 블록으로 해석해서 시각화
```

**API 변경 사항**

- 현재 API 명세 유지 (before_snapshot, after_snapshot 반환)
- Phase 2에서 추가 필드 고려: `structuredDiff?: Block[]`

**클라이언트 라이브러리 권고**

- `diff` npm 패키지 (문자 / 라인 단위)
- `markdown-to-blocks` 또는 유사 파서로 블록 추출

---

### 4.5 변경 검토 화면 구현 체크리스트

| 요소 | API 지원 | 구현 난도 | 비고 |
| --- | --- | --- | --- |
| 변경 이유 | ✓ | 낮음 | `summary` 필드 |
| 영향 페이지 | ✗ | 높음 | **갭 #22** → 별도 조회 필요 |
| 전후 diff | ✓ (형식 미정) | 중간 | 클라이언트 diff 계산 |
| 근거 출처 | ✓ | 중간 | `evidence_summary`는 jsonb → 해석 필요 |
| AI 신뢰도 | ✓ | 낮음 | `ai_confidence` 필드 |
| 승인/거절/수정 | ✓ | 낮음 | `POST .../reviews` |

---



