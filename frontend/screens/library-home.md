# 도서관 홈 화면 설계

**상태**: 설계 문서 (목업 데이터 포함)  
**근거**: 기획서 16.2절, API 명세 4.1절, 요구사항 정의서 5.15절 (FR-UIX-02)  
**UD 의존**: UD-28 (확정, 2026-07-29)

---

## 1. 화면 개요

사용자가 도서관에 진입했을 때 보는 대시보드 화면. AI가 자동으로 추출한 5가지 통찰을 한눈에 확인.

### 구성 요소

1. **주요 주제** (관계도 기준 상위 6개)
2. **최근 발견 연결** (AI가 제안한 新 관계, 최근 5개)
3. **열린 질문과 지식 공백** (Lint 결과, 최근 5개)
4. **모순** (발견된 모순, 최근 5개)
5. **검토 대기 변경** (미승인 변경 세트, 최근 5개)
6. **최근 활동** (모든 활동 유형 통합, 최근 10개)

---

## 2. 주요 주제 섹션

### 2.1 설계 결정: 관계 수 노출 여부

**결정**: **노출함** (숫자 + 단위 표시)

**근거**:
- UD-28 확정 기준이 "관계 수 양방향 합산"이므로, 사용자에게 "왜 이것이 주요 주제인지" 근거를 투명하게 전달
- 관계 수 = 지식 구조의 중심성 지표 → 사용자가 직관적으로 이해 가능
- "관계 24개" 같은 수치는 지식 축적 정도를 시각적으로 체감하게 함
- Phase 2에서 필터링(예: "관계 10개 이상만")이 필요할 때 이미 숫자를 노출했으므로 자연스러움

### 2.2 빈 상태 처리

| 상태 | 조건 | 메시지 | 액션 |
|---|---|---|---|
| **신규 도서관** | 주제 0개 | "아직 지식이 없습니다. 자료를 업로드하면 AI가 자동으로 주제를 생성합니다." | [자료 업로드하기] |
| **처리 중** | CONCEPT 있음 + PUBLISHED 없음 | "처리 중인 주제들이 검토 대기 중입니다. [검토하러 가기]" | [검토 화면] |
| **공표 예정** | 처리 중 + 변경 세트 진행 중 | "AI가 주제를 분석하는 중입니다. 곧 완료됩니다." | (없음) |

**근거**: UD-28 `PUBLISHED` 필터에서 파생. Ingest 직후에는 CONCEPT 페이지가 생성되지만 변경 세트 승인 전까지는 도서관 홈에 표시되지 않음 → 사용자에게 진행 상태를 명확히 전달.

### 2.3 와이어프레임

```
┌─────────────────────────────────────────────────────────┐
│ 주요 주제 (관계도 기준)          [더 보기] →             │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  1️⃣ 신경망 구조                                    관계 24개 │
│      결합구조와 활성화 함수를 핵심으로...               │
│                                                         │
│  2️⃣ 주의 메커니즘                                  관계 18개 │
│      Transformer의 핵심 구성 요소...                 │
│                                                         │
│  ... (4개 더)                                          │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

### 2.4 API 호출

**도서관 홈은 단일 API 호출로 모든 데이터를 가져온다:**

```typescript
GET /api/v1/libraries/{libraryId}
```

**응답** (4.1절 394~405행):
```json
{
  "data": {
    "id": "...",
    "name": "...",
    "home": {
      "topics": [
        {
          "name": "신경망 구조",
          "pageCount": 12
        },
        ...
      ],
      "recentRelations": [...],
      "openQuestions": [...],
      "knowledgeGaps": [...],
      "openContradictionCount": 3,
      "pendingChangeSetCount": 2,
      "recentActivities": [...]
    }
  }
}
```

**검증**: API 명세 4.1절 387~417행에 도서관 홈 요약 스키마 확정 ✅

---

## 3. 최근 발견 연결

### 3.1 설계

- 제목: "최근 발견 연결"
- 데이터 출처: `home.recentRelations[]` (위의 도서관 홈 응답에 포함)
- 상한: 5개 (API 명세에서 제한)
- 각 항목: 출처 페이지 → 대상 페이지, 관계 유형, AI 확신도
- 액션: [승인하기] / [거절하기]

### 3.2 상세 조회 (필요 시)

전체 관계 목록이 필요하면:
```typescript
GET /api/v1/libraries/{libraryId}/relations
  ?status=PROPOSED&sort=createdAt,desc&limit=5
```

**검증**: API 명세 7.3절 868행 ✅

---

## 4. 열린 질문과 지식 공백

### 4.1 설계

- 제목: "열린 질문과 지식 공백"
- 데이터 출처: `home.openQuestions[]` + `home.knowledgeGaps[]` (도서관 홈에 포함)
- 상한: 각 5개
- 각 항목: 질문/공백 설명 + 대상 페이지
- 액션: [확인하기]

### 4.2 상세 조회 (필요 시)

Lint 검사 결과 상세 조회:
```typescript
GET /api/v1/libraries/{libraryId}/lint-runs/{lintRunId}/findings
```

**검증**: API 명세 10절 1208행 ✅
**주의**: Lint findings은 특정 lint-run의 결과이므로, 최신 run ID를 먼저 조회해야 함.

---

## 5. 모순

### 5.1 설계

- 제목: "모순"
- 데이터 출처: `home.openContradictionCount` (도서관 홈에 개수만 포함)
- 상한: 5개 표시
- 각 항목: 충돌 설명 + 신뢰도
- 액션: [검토하기]

### 5.2 상세 조회

모순 목록 조회:
```typescript
GET /api/v1/libraries/{libraryId}/contradictions
  ?status=OPEN&limit=5
```

**검증**: API 명세 7.3절 868행 ✅

---

## 6. 검토 대기 변경

### 6.1 설계

- 제목: "검토 대기 변경"
- 데이터 출처: `home.pendingChangeSetCount` (도서관 홈에 개수만 포함)
- 상한: 5개 표시
- 각 항목: 변경 제목 + 위험도 (MINOR/MAJOR)
- 액션: [검토하기]

### 6.2 상세 조회

검토 대기 변경 세트 조회:
```typescript
GET /api/v1/libraries/{libraryId}/change-sets
  ?status=READY_FOR_REVIEW&limit=5
```

**검증**: API 명세 8절 922행 ✅

---

## 7. 최근 활동

### 7.1 설계

- 제목: "최근 활동"
- 데이터 출처: `home.recentActivities[]` (도서관 홈에 포함)
- 상한: 10개
- 각 항목: 타입 아이콘 + 활동 설명 + 시각
- 정렬: 최신순

### 7.2 상세 조회

모든 활동 목록:
```typescript
GET /api/v1/libraries/{libraryId}/activities?limit=10
```

**검증**: API 명세 11절 1261행 ✅

---

## 8. API 호출 종합 및 최적화

### 8.1 설계 결정: 단일 호출 vs 분산 호출

**결정**: **기본은 단일 호출, 상세는 분산 호출**

**근거**:
- 초기 로딩: `GET /libraries/{libraryId}` 1개 호출로 모든 섹션 데이터 획득
- 사용자가 [더 보기] 클릭 시 상세 API 호출 (lazy loading)
- 도서관 홈 응답에 데이터 길이 제한이 있으므로, 상세는 전용 엔드포인트로 조회

### 8.2 최적화 체크리스트

| # | 호출 | 시점 | 필요 여부 |
|---|---|---|---|
| 1 | `GET /libraries/{libraryId}` | 페이지 진입 | ✅ 필수 |
| 2 | `GET /libraries/{libraryId}/relations?status=PROPOSED...` | [최근 발견 연결 더 보기] | 선택 |
| 3 | `GET /libraries/{libraryId}/lint-runs/{lintRunId}/findings` | [열린 질문 더 보기] | 선택 |
| 4 | `GET /libraries/{libraryId}/contradictions?status=OPEN...` | [모순 더 보기] | 선택 |
| 5 | `GET /libraries/{libraryId}/change-sets?status=READY...` | [검토 대기 더 보기] | 선택 |
| 6 | `GET /libraries/{libraryId}/activities?limit=...` | [최근 활동 더 보기] | 선택 |

---

## 9. 라이브러리 후보 및 트레이드오프

**필요 라이브러리**:
1. **Markdown 렌더러** — 주요 주제 요약, 활동 설명 등
2. **아이콘 라이브러리** — 활동 타입 시각화 (📥 ✅ 🔍 ➕ 등)
3. (선택사항) **그래프 라이브러리** — Phase 2에서 사용

### 9.1 Markdown 렌더러

**후보 1: `react-markdown` + `remark-gfm`**
- 장점: 가볍고, GitHub Flavored Markdown 지원
- 단점: 커스텀 컴포넌트 매핑이 필요함
- 권고: ✅ 사용

**후보 2: `markdown-it`**
- 장점: 매우 빠름, 플러그인 풍부
- 단점: React 컴포넌트 직접 매핑 복잡
- 권고: 호스팅 블로그용, 본 프로젝트에는 과함

### 9.2 아이콘 라이브러리

**후보 1: `react-icons`**
- 장점: 수십 개 아이콘 라이브러리 통합 (Feather, Font Awesome, Material 등)
- 단점: 번들 크기 증가 가능
- 권고: ✅ 사용 (트리 셰이킹으로 최소화)

### 9.3 Diff 뷰어 (Phase 1 필요, 아직 미정)

변경 검토 화면(갭 #13)에서 필요. UD-05 확정(클라이언트 계산)에 맞는 라이브러리 선정 필요.
- 후보: `diff-match-patch`, `react-diff-viewer-continued` 등
- **별도 제안 문서 필요** (리더 승인 대상)

### 9.4 그래프 시각화 (Phase 4 필요, 아직 미정)

보조 그래프 화면에서 필요 (갭 #7).
- 후보: `react-flow-renderer`, `cytoscape.js` 등
- **별도 제안 문서 필요** (리더 승인 대상)

---

## 10. 구현 체크리스트

- [ ] 컴포넌트 구조 설계 (각 섹션 = 재사용 가능 컴포넌트)
- [ ] Skeleton loader 설계 (로딩 중 상태)
- [ ] 에러 상태 처리 (API 호출 실패)
- [ ] 빈 상태 UX 정의 (신규 도서관 / 처리 중 / 공표 예정)
- [ ] 반응형 모바일 레이아웃 (기획서 16.5절)
- [ ] 접근성 (alt 텍스트, 키보드 네비게이션)
- [ ] 목업 데이터 → 실제 API 연동 시 "완료"로 보고

---

## 11. 다음 단계

1. **디자인 시스템 확정** — 컬러, 타이포그래피, 간격 (리더 또는 디자이너)
2. **라이브러리 승인** — `react-markdown`, `react-icons` (리더)
3. **컴포넌트 구현** — 각 섹션을 React 컴포넌트로 (목업 데이터 먼저)
4. **API 연동** — `Retry-After` 기반 polling 추가 (UD-04 확정)
5. **라이브러리 2차 제안** — diff 뷰어, 그래프 시각화 (별도 문서)

---

## 12. 오류 정정 기록

**2026-07-30 리더 지적:**
1. 모순 조회 API 부재 → 실제로는 API 7.3절 868행에 존재
2. Lint findings 경로 오류 → `GET .../lint-runs/{id}/findings` (내 설계는 잘못된 경로)
3. 도서관 홈 응답 구조 간과 → 모든 필요 데이터가 `home` 필드에 통합 제공
4. API 호출 최적화 미반영 → 초기 로딩은 단일 호출, 상세는 분산 호출로 수정

**정정 완료:**
- 7.1 표 갱신: 모순 ✅ (API 7.3절 확정)
- API 호출 구조 재설계: 도서관 홈 단일 응답 활용
- Lint findings 경로 정정

---

## 첨부: 목업 데이터 구조

```typescript
// 예시: 도서관 홈 응답 (실제 API 데이터로 교체 전)
export const mockLibraryHome = {
  id: "lib-001",
  name: "AI & 머신러닝 개인 도서관",
  home: {
    topics: [
      { name: "신경망 구조", pageCount: 12 },
      { name: "주의 메커니즘", pageCount: 8 },
      // ... 6개까지
    ],
    recentRelations: [
      { sourceTitle: "신경망 구조", targetTitle: "활성화 함수", relationType: "RELATED", confidence: 0.92 },
      // ... 5개까지
    ],
    openQuestions: [
      { description: "왜 ReLU가 자주 사용되는가?", targetTitle: "활성화 함수" },
      // ... 5개까지
    ],
    knowledgeGaps: [
      { description: "배치 정규화의 역전파", targetTitle: "배치 정규화" },
      // ... 5개까지
    ],
    openContradictionCount: 3,
    pendingChangeSetCount: 2,
    recentActivities: [
      { type: "INGEST", title: "문서 10개 Import 완료", createdAt: "2026-07-30T..." },
      // ... 10개까지
    ]
  }
};
```

**중요**: 이 파일에 목업이 포함되면 구현 완료 시 "목업 데이터 제거 및 API 연동"을 별도 작업으로 표기.

