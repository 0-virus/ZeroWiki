# 변경 검토 화면 설계

**최종 갱신**: 2026-07-30  
**상태**: 설계 완료 (라이브러리 선정 대기)  
**기준 문서**:
- `ZeroWiki-MVP-서비스-기획서.md` 11절
- `ZeroWiki-API-명세-초안.md` 8.1절
- `ZeroWiki-요구사항-정의서.md` FR-CHG-05
- `검증-프론트엔드-화면-API-갭.md` 갭 #13

## 1. 화면 개요

GitHub Pull Request 유사 형태의 변경 검토 화면. 사용자가 AI가 생성한 변경을 승인·거절·수정 요청·보류할 수 있다.

| 요소 | 설명 |
| --- | --- |
| **페이지 상단** | 변경 제목 + 위험도(MINOR/MAJOR) + 상태 |
| **변경 이유** | AI의 변경 요약 (markdown) |
| **영향 페이지** | 이 변경으로 영향을 받을 페이지 목록 + 영향 사유 |
| **변경 항목 목록** | 개별 변경 항목 (페이지/관계/주장 등) + 위험도 |
| **전후 Diff** | markdown 형식 줄 단위 비교 (클라이언트 계산) |
| **근거 출처** | evidenceSummary 렌더링 + 원문 링크 |
| **AI 신뢰도** | confidence 백분율 표시 |
| **판단 영역** | 4개 버튼: APPROVE / REJECT / REQUEST_CHANGES / DEFER |

## 2. UI 구조

### 2.1 상단 영역 (메타데이터)

변경 제목, 위험도 배지, 상태 배지를 표시한다.

- **변경 제목**: `change_set.title`
- **위험도 배지**: MINOR (회색) / MAJOR (주황색)
- **상태 배지**: READY_FOR_REVIEW (파란색)

### 2.2 변경 이유 섹션

`change_set.summary` 필드를 markdown으로 렌더링한다. 최대 높이 300px, 스크롤 가능.

### 2.3 영향 페이지 섹션

API 명세 8.1절에 "영향 페이지" 상세가 명시되지 않음 (갭 #22). 현재는 변경 항목 목록에서 변경 대상만 표시하고, Phase 2에서 상세 영향 분석 추가 예정.

### 2.4 변경 항목 선택

다중 변경이 있을 경우, 각 항목을 선택할 수 있는 UI 제공.

### 2.5 Diff 뷰어 섹션

**UD-05 확정 (Phase 1)**: 클라이언트에서 `beforeSnapshot.markdownBody`와 `afterSnapshot.markdownBody`를 비교하여 unified diff 형식으로 표시한다.

- **제거 라인**: 빨강 배경 + `-` 접두사
- **추가 라인**: 초록 배경 + `+` 접두사
- **변경 없는 라인**: 회색 텍스트 (context, 3~5줄)
- **라이브러리**: `diff` npm 패키지 (라인 단위 diff 계산)

### 2.6 근거 출처 섹션

`change_items[].evidence_summary` 배열의 각 항목을 표시한다.

- 원본 제목 + 링크
- 발췌문 (원본 언어, 최대 400자 - UD-07 확정)
- 언어 배지 (갭 #14: `[English]`, `[한국어]` 등)
- 신뢰도 스타 (5단계)

### 2.7 AI 신뢰도 섹션

`change_items[].ai_confidence` (0~1.0)를 퍼센트로 표시한다.

- 90% 이상: 초록색 (신뢰도 높음)
- 70~89%: 노란색 (신뢰도 중간)
- 70% 미만: 주황색 (신뢰도 낮음)

## 3. 판단 영역 (4개 버튼)

### 3.1 APPROVE (승인)

- **스타일**: 초록색 배경
- **동작**: 확인 모달 → `POST /reviews` with `decision: APPROVE`
- **응답**: 성공 시 도서관 홈으로 이동

### 3.2 REJECT (거절)

- **스타일**: 회색 배경
- **동작**: 모달 열기 → comment 입력 필수 → `POST /reviews` with `decision: REJECT`
- **comment 필수**: 최소 20자 권장
- **응답**: 성공 시 "변경이 거절되었습니다" 메시지

### 3.3 REQUEST_CHANGES (수정 요청)

- **스타일**: 주황색 배경
- **동작**: 모달 열기 → comment 입력 필수 → `POST /reviews` with `decision: REQUEST_CHANGES`
- **comment 필수**: 최소 20자 권장
- **재생성 대기**: 성공 후 polling 시작 (2초 간격)
  - `GET /change-sets/{changeSetId}` 상태 감시
  - 새 변경 준비되면 "새 변경 준비됨" 배지 표시
  - 사용자가 "[새 변경 확인]" 클릭 → 새 변경 항목 로드

### 3.4 DEFER (보류)

- **스타일**: 파란색 배경
- **동작**: 확인 모달 → `POST /reviews` with `decision: DEFER`
- **응답**: 성공 시 화면 유지 또는 도서관 홈으로 이동

## 4. 필요한 API 호출

| API | 용도 |
| --- | --- |
| `GET /libraries/{libraryId}/change-sets/{changeSetId}` | 변경 세트 메타데이터 |
| `GET /libraries/{libraryId}/change-sets/{changeSetId}/items` | 변경 항목 목록 (diff, 근거, 신뢰도) |
| `POST /libraries/{libraryId}/change-sets/{changeSetId}/reviews` | 판단 제출 |

## 5. 갭 및 미확정 계약

### 갭 #22: 영향 페이지 상세 API 부재

**현황**: API 명세 8.1절에서 변경 세트 응답이 "영향을 받는 페이지" 정보를 명시하지 않음.

**권고(미확정)**: backend에 `GET /change-sets/{changeSetId}/impact` 또는 `affectedPages` 필드 요청.

**현재 구현**: Fallback으로 변경 항목 목록에서 변경 대상만 표시.

### 갭 #23: REQUEST_CHANGES 재생성 완료 감지

**현황**: `REQUEST_CHANGES` 후 AI 재생성 완료를 감지하는 메커니즘 불명확.

**권고(미확정)**: backend에 명확히 문의 필요. 현재 가정: 상태 필드 변화로 감지, 2초 간격 polling.

## 6. 라이브러리 후보 (선정 대기)

| 라이브러리 | 용도 | 상태 |
| --- | --- | --- |
| **`diff` (npm)** | 라인 단위 diff 계산 | 권고 (UI 검토 필요) |
| **`react-diff-viewer-continued`** | diff 시각화 | 대안 |

## 7. 마크다운 병기 처리 (UD-27 확정, 갭 #14)

변경 검토 화면에서:
- **본문**: 한국어만 (병기 없음)
- **발췌**: 원본 언어 그대로 → 언어 배지 표시 필수

## 기준 문서 확인 완료

- API 명세 8.1절
- 기획서 11절  
- FR-CHG-05
- 검증-프론트엔드-화면-API-갭.md 갭 #13
