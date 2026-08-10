# Phase 0 벤치마크 실행 계획

**작성일:** 2026-07-31  
**최종 수정:** 2026-08-10 (평가 데이터 인수 실측 반영)
**상태:** 평가 데이터 인수 완료 · 파이프라인 실행 대기
**근거:** 기획서 23절, 25절; 요구사항 정의서 1.3~1.4절, 11절, 12절

---

## 개요

Phase 0의 목표는 **인수한 NoteWiki 평가 데이터(source 37개, 지식 페이지 143개)를 기반으로 Ingest 파이프라인·연결·모순 탐지의 기술적 타당성을 검증**하고, 미확정 계약 4건(UD-11·17·25·29)의 결정 근거를 만드는 것이다. 요구사항 정의서 12절의 벤치마크 결정 항목은 이 4건과 UD-26까지 5건이지만, **UD-26은 Phase 4 사용자 테스트 별도 트랙**이므로 Phase 0 확정 목표에 포함하지 않는다.

### 데이터셋 규모 주의

기획서 25절을 근거로 처음에는 "원본 36개·위키 페이지 152개"를 계획 가정으로 사용했다. 2026-08-10 인수 CSV 실측은 `source` 37개, `concept` 127개, `flow` 16개이며, 지식 페이지(`concept`+`flow`) 143개와 소스 요약 페이지를 합친 인벤토리 총계는 180개다. **역사적 36/152 가정은 이 실측으로 대체한다.** 다만 기획서 3절 MVP 가설과 AC-1·NFR-PRF-01·NFR-COST-03은 "문서 100개" 기준이므로, 37개 원본 실행은 예비 수치이며 AC-1("문서 100개 완료율 95%")의 정식 검증은 Phase 1로 이관한다.

**일정 추정:** 3~4주 (1인 개발 + 반복 측정 포함)  
**결과물:** `docs/Phase-0-벤치마크-결과.md` (NEW) + ERD·API 명세 업데이트

---

## 1단계: 데이터셋·평가 세트 준비 (선행 필수)

### 1-1. 기준 데이터셋 구성

**무엇:** 기획서 25절 1번의 계획 가정을 실제 인수한 `evaluation/pages-inventory.csv`로 재계측한 기준 데이터셋

**어떻게:**
- 인수 데이터 실측:
  - `page_type=source`: 37개
  - `page_type=concept`: 127개
  - `page_type=flow`: 16개
  - 지식 페이지: 143개, 소스 요약을 포함한 인벤토리 총계: 180개
- 메타데이터는 `page_id`, `page_slug`, 제목, 페이지 타입, 원본 저장소 상대 경로를 포함한다.
- 모든 데이터는 **사용자 1인 계정 내부로 격리** (FR-SRC-04 준수)
- 인수 파일: `evaluation/pages-inventory.csv`. 원본 외부 저장소의 본문은 이 저장소에 복제하지 않는다.

**차단 요인 검토:**
- ✓ 기존 데이터가 있는가? → 인벤토리와 정답 라벨 인수 완료. 실제 Ingest 입력 본문은 외부 원본을 안전하게 읽는 방식 확인 필요
- ✓ 데이터 정제가 필요한가? → 민감정보·비밀 검사(기획서 18절)

**소요 기간:** 2일 (데이터 정제 포함)

### 1-2. 평가 세트 구성 (기획서 25절 2번)

**무엇:** 기준 데이터셋에 대한 정답 레이블:
- **정답 연결:** 의미 있는 관계 (예: "개념 A가 개념 B를 설명한다")
- **모순 후보:** 의도적으로 삽입한 모순 (예: "주장 P와 주장 Q는 서로 다른 조건 하에서 나온 것인가?")

**어떻게** (확정):

**사용자가 직접 라벨링한 평가 세트를 인수했다.** 공개 벤치마크 데이터셋(SQuAD, FEVER)은 NoteWiki의 도메인·언어와 일치하지 않아 AC-3·AC-5 실측을 대체할 수 없다는 기존 판단은 유지한다.

1. **연결 평가 세트: 84건 인수 완료** (positive 76 / negative 8)
   - 초기 목표 20~30건은 UD-16 7종의 유형별 표본 확보를 위해 상향됐다.
   - 후보 선정: 143개 지식 페이지의 무작위 표본이 아니라 이미 관련 있다고 알려진 주제 클러스터에서 시작
   - 태깅: UD-16 확정 7종 (`RELATED_TO`, `PART_OF`, `DEPENDS_ON`, `CAUSES`, `CONTRASTS_WITH`, `EXPLAINS`, `APPLIES_TO`) + 방향 + 확신도(high/medium/low)
   - **부정 예시 8건 포함** (관련 없는 페이지 쌍 — 오탐 측정용)
   - 형식: `source_page_slug, target_page_slug, relation_type, direction, confidence, label, 근거_한줄`
   - 파일: `evaluation/relation-labels.csv`

2. **모순 평가 세트: 26건 인수 완료** (실제 모순 23 / `NO_CONFLICT` 3)
   - 초기 목표 10~15건은 5종 분류별 표본 확보를 위해 상향됐다.
   - 후보: 시간 경과로 업데이트된 내용, 출처별로 다르게 서술된 내용 우선
   - 분류 5종 (API 명세 7.3절):
     - `TRUE_CONFLICT`: 명백한 사실 충돌 (예: "출판년도 불일치")
     - `TIME_CHANGE`: 시간에 따른 변화 (예: "기술 진화")
     - `CONDITION_DIFF`: 조건 차이 (예: "특정 환경에서만 성립")
     - `VIEWPOINT_DIFF`: 관점 차이 (예: "학파·전문가별 입장")
     - `POSSIBLE_CONFLICT`: 의심스러운 충돌 (명확하지 않음)
   - **부정 예시 3건 포함** (실제 모순 아님)
   - 형식: `case_id, claim_A_statement, claim_B_statement, classification, 판단_이유`
   - 파일: `evaluation/contradiction-labels.csv`

**선행 조건:**
- ✓ 사용자 직접 라벨링 완료
- ✓ CSV 인수 검증 완료 (`evaluation/validate_evaluation.py`, 오류 0건)
- ✓ 템플릿과 라벨링 규칙 보존 (추가 라벨 작성 시 재사용)

**현재 상태:** 평가 세트 준비 완료. 과거 예상(5건 파일럿 후 총 35~45건, 2~4시간)은 실제 110개 라벨 행 인수로 대체됐다.

---

## 2단계: 2단계 Ingest 프로토타입 실행 (핵심)

### 2-1. 프로토타입 구성

**무엇:** 2단계 AI 처리 파이프라인(Ingest 9단계 5~8 구현)의 프로토타입 실행

**대상 문서:** 1단계 데이터셋 37개 원본 → 약 100문단 또는 "의미 있는 절 단위" 분할 (UD-11에 따라 달라짐)

**처리 흐름:**
```
1단계 스캔(저비용)
  ├─ 파일 파싱 / 중복 탐색 / 민감정보 탐지 / 주제 분류
  └─ 기존 지식 페이지(143개: concept 127 + flow 16) 후보 자동 매칭 → 처리 계획 제시

처리 계획 승인 (사용자)

2단계 처리(고품질) — 핵심 측정 지점
  ├─ 핵심 주장·근거 추출 (Claim·Evidence)
  ├─ 기존 지식과 의미 연결 (Relation)
  ├─ 종합 페이지 작성 또는 기존 페이지 수정
  ├─ 모순 후보 판정 (Contradiction)
  ├─ 위험 변경 분류
  └─ 토큰 사용량·소요 시간 기록

초안 도서관에 변경 세트 적용 (검토 용이)
승인(일괄·개별) → 새 버전 발행
```

**실행 방식:**

```yaml
스크립트: backend/scripts/phase0-benchmark.sh (신규 작성)
  - Spring Boot CLI 또는 독립형 Jar로 Ingest 파이프라인 호출
  - 매개변수: --dataset="기준_데이터셋.csv" --benchmark-mode=true
  - 결과: 각 문서별 메트릭 CSV + 파이프라인 로그
```

**측정 항목 (NFR-PRF-05 기준):**

| 항목 | 단위 | 기록 방법 | 판정 기준 |
|------|------|---------|---------|
| **처리 성공률** | % | 성공 문서 / 전체 | **AC-1 예비:** 95% 이상? (정식 검증은 Phase 1로 이관) |
| **문서당 토큰 비용** | token/doc | 1단계 + 2단계 입출력 합계 (LLM API 로그) | NFR-COST-03: 평균값 기록, 분포 분석 |
| **1단계 스캔 시간** | sec/doc | Worker 로그 타임스탬프 | NFR-PRF-02: 처리 계획이 대기 가능한 시간? (기준: 2분 이내 권고) |
| **2단계 처리 시간** | sec/doc | Worker 로그 | NFR-PRF-05: 단계별·모델별 분포 측정 |
| **연결 pair/strict recall** | % | positive 76쌍 중 쌍 탐지 / UD-16 type·direction까지 일치 | 오프라인 보조 지표. AC-3 대체 불가 |
| **연결 bounded precision** | % | 라벨된 84쌍 안에서 탐지한 positive / 탐지한 전체 | 분모 0은 N/A, 84쌍 밖은 사람 판정 전 제외 |
| **연결 sampled FPR** | % | negative에서 관계를 출력한 쌍 / 8 | 표본 오탐 지표 |
| **AC-3 사용자 유용성** | % | U-1의 사용자 유용 판정 / 평가한 제안 | 5~10명 평가, 70% 이상 |
| **모순 recall** | % | stable case_id로 탐지한 conflict / 23 | AC-5 80%는 최소 19건. Ingest mapping 전 조건부 |
| **모순 sampled FPR** | % | `NO_CONFLICT` 오탐 / 3 | 표본 오탐 지표 |
| **모순 FDR** | % | 사람이 오탐으로 판정한 탐지 / 사람이 판정한 전체 positive 탐지 | 분모 0은 N/A |

### 2-2. 실행 프로토콜

**단계별 수행:**

```
Week 1 (시작 D+1~D+7):
  Day 1-2: 스크립트 작성 (파이프라인 호출, 메트릭 수집)
  Day 3-4: 1단계 스캔 단독 실행 → 메트릭 수집
  Day 5-7: 2단계 처리 단독 실행 (작은 배치부터)

Week 2 (D+8~D+14):
  Day 8-10: 전체 파이프라인 E2E 실행 (37개 원본)
  Day 11-12: 정합성 검사 + 정답 세트 비교 (연결·모순 정확도)
  Day 13-14: 재실행(반복성 검증) + 이상치 분석

Week 3 (D+15~D+21):
  Day 15-16: UD-11(Claim 세밀도) 비교 실행 (모든 문장 vs 주요 주장 2회)
  Day 17-18: UD-17(임베딩 모델) A/B 테스트 (2~3개 모델 비교)
  Day 19-21: 결과 문서화 + 차단 사항 정리
```

**소요 기간:** 3주

### 2-3. 측정 결과 기록

**결과 형식:** `docs/Phase-0-벤치마크-결과.md` (신규, backend 작성)

```markdown
# Phase 0 벤치마크 결과

## 1. 데이터셋 요약
- 원본 37개, 지식 페이지 143개(concept 127 + flow 16), 인벤토리 총 180개, 총 문단 수 ~X

## 2. 처리 성공률 (AC-1 예비 검증)
- 성공: Y개 / 전체 37개 (Z%)
- 실패 원인: [목록]
- **결론: AC-1 예비 수치만 확보. 정식 검증은 Phase 1(사용자 실제 100+ 문서 Ingest)로 이관**

## 3. 토큰 비용 분석 (NFR-COST-03)
- 평균 문서당: AAAA token
- 분포: [박스플롯 데이터]
- 예상 월 비용: 문서 100개 기준 $X (처리 계획 단계별)
- 권고: 무료/기본/고급 요금제 단위 설계

## 4. 처리 시간 분석 (NFR-PRF-02, 05)
- 1단계 스캔: 평균 N초/문서
  - 처리 계획 대기 시간 기준(2분)에 적합? [YES/NO]
- 2단계 처리: 평균 M초/문서
  - 모델별 분포: [빠른·표준·정밀 처리 모드]

## 5. 연결·모순 품질 (오프라인 지표 + AC-3·AC-5)
- 연결 pair recall / strict recall / bounded precision / sampled FPR
- AC-3: U-1 사용자 5~10명 유용성 평가 결과 (CSV 지표로 대체하지 않음)
- 모순 recall / sampled FPR / FDR (분모 0은 N/A)
- AC-5: 원본 page/claim → 생성 claim_id mapping 확보 후 최종 판정
- 결론: 리스크 1, 2번 평가 [충족/미충족/조건부]

## 6. UD 확정 권고

### UD-11 (Claim 세밀도)
- 측정: 모든 문장 vs 주요 주장 2회 비교
- 결과: [행 수·처리 시간·정확도]
- 권고: [선택지 A vs B]

### UD-17 (임베딩 모델)
- 테스트 모델: [모델1, 모델2, 모델3]
- 결과: 의미 검색 정확도 [%, %, %]
- 권고: [모델명·차원]

### UD-25 (성능 목표)
- 1단계 목표: ~2분 (처리 계획 대기)
- 2단계 목표: [모델별 시간 범위]
- 전체 Ingest(100문서): 예상 ~H시간

### UD-29 (claim_key 생성)
- 측정: 버전 간 Claim 수동 매칭 시나리오 (의미 정규화 벤치마크)
- 결과: [오탐 사례, 오율]
- 권고: AI 제안 + 사용자 승인 모델 (기획서 11절 패턴 일치) [확인됨]

## 7. Markdown 왕복 검증 (FR-EXP-05)
- 내보내기 → 재수입 → 데이터 일치도: X%
- 손실 항목: [없음 / 목록]

## 8. 해결되지 않은 문제
- [UD-26은 5~10명 사용자 테스트 필요 — Phase 4로 미룸]
```

**차단 요인:**
- 미확정: LLM provider/model, 유료 API 액세스 범위, 토큰·비용 예산
- 미실행: 임베딩 모델 2~3개 후보 조사와 비교 기준 수립

---

## 3단계: UD-29 + 갭 #23 (2단계와 병행)

### 3-1. UD-29: claim_key 생성 규칙 (의미 정규화 벤치마크)

**무엇:** 버전 간 Claim 재사용 구조에서 "같은 주장"을 판정하는 규칙 평가

**현재 상태:**
- ERD v1.5: claims.page_id FK 추가, claim_statuses 신설 (버전별 상태)
- 미확정: claim_key 동일성 판정 메커니즘 (UD-29)
- 권고: "의미 정규화는 불가능(해시의 한계) → AI 제안 + 사용자 승인 모델"

**벤치마크 계획:**
1. **수동 시나리오 (10~15개):** 같은 주장이 다양한 표현으로 나타나는 경우
   - 예: "Python은 해석형 언어다" vs "Python은 동적 타입의 스크립트 언어다"
   - 사람이 "같은가/다른가" 판정

2. **자동 제안 실행:** 2단계에서 생성된 Claims 쌍에 대해
   - Embedding 기반 유사도 측정
   - LLM 제안: "동일 Claim으로 취급? 이유?"
   - 정확도 측정 (recall·precision)

3. **결론:** 의미 정규화 불가능 → AI 제안 후 사용자 승인 필요 [재확인]

**소요 기간:** 2~3일 (2단계 결과 사용)

### 3-2. 갭 #23: REQUEST_CHANGES 재생성 평균 시간

**무엇:** 변경 검토 화면에서 "수정 요청 후 재생성" 시 항목 갱신 시간 측정

**메커니즘 (API 명세 8.1절 옵션 A 확정):**
```
POST /change-sets/{id}/items/{itemId}/request-changes
  → 202 Accepted

클라이언트 polling (2초 간격, UD-04 기본값)
GET /change-sets/{id}/items
  → change_items[].review_status: REVISION_REQUESTED → PENDING
  → change_items[].after_snapshot: 갱신된 결과

완료 감지: review_status 변화 감지
```

**측정 항목:**
- 재생성 요청 → 완료까지 소요 시간 (초)
- 규모별: 소/중/대 변경 항목 3개 측정
- polling 간격 2초가 적절한가? (99% 감지율 기준)

**실행:** 2단계 "위험 변경" 샘플에서 REQUEST_CHANGES 시뮬레이션

**소요 기간:** 1~2일 (2단계 완료 후)

---

## 4단계: UD-26 (별도 트랙 — 본계획 범위 밖)

**상태:** Phase 4 사용자 테스트로 이관 (개별 승인 건수 상한)

**이유:** 기술 벤치마크가 아니라 5~10명 사용자 UX 테스트 필요  
**대상 기준:** AC-4 "승인 피로" 리스크, NFR-USE-02

---

## 결과물 및 문서화 위치

### 결과 문서
- **신규:** `docs/Phase-0-벤치마크-결과.md` (backend 작성, 1차)
- **기타 업데이트 (확정 후):**
  - 요구사항 정의서 12절 정본: UD-11·17·25·29 결정 상태 반영을 리더에게 요청
  - ERD 8절 부분집합: UD-11·17 결과 반영
  - API 명세 18절: UD-25 수치 반영
  - 기획서 24절(리스크): 연결·모순 품질 기반 재평가

### 타 역할 영향
- **frontend:** API 명세 업데이트 후 화면 연결 재확인
- **qa:** CSV 구조·오프라인 지표 자동 검증, AC-3 U-1 사용자 시나리오와 AC-5 mapping 조건 별도 인수

### 기록 의무
- STATE.md: Phase 0 진행 상황 업데이트 (주간)
- WORKLOG.md: 각 주간 완료 항목 + 발견사항 append

---

## 선행 조건 체크리스트

| 항목 | 상태 | 담당 | 확인 필요 |
|------|------|------|---------|
| NoteWiki 평가 인벤토리 확보 | 완료 | backend | source 37 / concept 127 / flow 16 / total 180 검증 |
| 평가 세트 작성·인수 | 완료 | 사용자/backend | relation 84행, contradiction 26행; 자동 검증 오류 0건 |
| 실제 Ingest 입력 본문 접근 | 확인 필요 | backend | 외부 원본 내용을 저장소에 복제하지 않는 안전한 읽기 방식 필요 |
| LLM API 토큰 예산 | ? | backend | 월 예산 내 100문서 처리 가능한가? |
| 임베딩 모델 후보 조사 | 미실행 | backend | 2~3개 모델 선정 필요 |
| Markdown 왕복 테스트 환경 | 미실행 | backend | Phase 0 전에 스크립트 준비 |

---

## 위험 및 완화 전략

| 위험 | 영향 | 완화 |
|------|------|------|
| LLM 비용 폭증 | Phase 0 예산 초과 | Week 1에 작은 배치로 단가 측정, 조기 예산 조정 |
| 라벨링 작업 지연 | 해소 | 110개 라벨 행 인수 및 자동 검증 완료 |
| 임베딩 모델 성능 편차 | UD-17 결정 지연 | 사전에 3개 모델 선정 + Week 2에 A/B 테스트 |
| Markdown 변환 손실 | FR-EXP-05 미충족 | Week 1 프로토타입에서 테스트 후 변환기 개선 |
| UD-29 의미 정규화 불가능 | Claim 자동 매칭 보류 | 이미 권고 인정 — Phase 1에서 수동 승인으로 대체 |

---

## 다음 단계

1. **backend 선행 작업 (리더 go-ahead 후)**
   - 외부 NoteWiki 원본 본문을 복제하지 않고 읽을 안전한 입력 방식 확인
   - LLM API 토큰 예산 확보
   - 임베딩 모델 2~3개 후보 선정
   - Week 1 스크립트 작성 (1단계·2단계 파이프라인)

2. **평가 세트 유지 관리**
   - `python evaluation/validate_evaluation.py`를 변경 때마다 실행
   - 추가 라벨이 필요할 때만 아래 라벨링 규칙과 템플릿을 재사용

---

## 라벨링 규칙 (인수 완료 데이터 검증·추가 라벨용)

Phase 0 정답 세트는 인수 완료됐다. 아래는 현재 CSV의 검증 기준이며, 향후 라벨을 추가할 때 같은 규칙과 템플릿을 사용한다. **현재 사용자 라벨링 대기 작업은 없다.**

### 준비 사항

- CSV 템플릿: `evaluation/relation-labels-template.csv`, `evaluation/contradiction-labels-template.csv`
- 완료 데이터: `evaluation/relation-labels.csv` 84행, `evaluation/contradiction-labels.csv` 26행
- 편집기: Excel, Google Sheets, 또는 텍스트 에디터

### 1. 연결(Relation) 라벨링

**현재 규모:** 의미 있는 관계 76건 + negative 8건

**단계:**

1. **추가 라벨이 필요하면 파일럿 5건으로 형식 검증**
   - 템플릿 첫 5행 작성
   - CSV 형식 확인 (쉼표, 따옴표 인식)
   - backend에 피드백 (문제 있으면 즉시 수정)

2. **검증 후 필요한 표본만 추가**
   - 143개 지식 페이지 중 관계가 있다고 알려진 클러스터부터 시작
   - 각 페이지마다 2~3개 정도의 관계 선택

**형식:**

```csv
source_page_slug,target_page_slug,relation_type,direction,confidence,label,근거_한줄
javascript-async,async-await,EXPLAINS,forward,high,positive,동기/비동기 개념이 async-await 문법의 전제를 설명한다
n-plus-1-problem,orm,RELATED_TO,bidirectional,medium,positive,N+1은 ORM 사용 시 흔한 성능 문제다
cache-control,http-cache,PART_OF,forward,low,positive,Cache-Control은 HTTP 캐시를 조종하는 핵심 헤더다
java-buffer,http-cache,,,low,negative,표현은 겹치지만 실제 관계가 없는 오탐 측정용 쌍
```

**필드 설명:**

- `source_page_slug`: 출발 페이지의 `page_slug` 값
- `target_page_slug`: 도착 페이지의 `page_slug` 값
- `relation_type`: UD-16 확정 7종 중 하나 (**negative 행은 비워둔다**)
  - `RELATED_TO`: 주제가 연관됨
  - `PART_OF`: 목표가 더 큰 개념의 부분
  - `DEPENDS_ON`: 선행 학습 필요
  - `CAUSES`: 인과관계
  - `CONTRASTS_WITH`: 대비/반대 관점
  - `EXPLAINS`: 설명/근거 제공
  - `APPLIES_TO`: 실제 적용 예시
- `direction`: `forward`(일방향) / `bidirectional`(양방향) (**negative 행은 비워둔다**)
- `confidence`: `high` / `medium` / `low` (확신도)
- `label`: `positive`(의미 있는 관계) / `negative`(오탐 측정용, 관련 없음) — **positive와 negative 모두 포함 필수**
- `근거_한줄`: 왜 이 관계가 의미 있는가? (negative 행: 오탐 이유)

**부정 예시 2~3건 포함 (중요)**

negative 행은 `relation_type`과 `direction`을 **비워두고** `label`에 `negative`를 명시해야 한다. 이렇게 하면 파이프라인이 출력한 관계와 정답을 대조할 때, negative 행은 "모델이 어떤 관계로 잘못 예측해도 오탐으로 카운트할 수 있는" 기준이 된다. 즉, 모델이 `java-buffer`와 `http-cache` 사이에 어떤 7종 관계로든 제안하면 오탐이다.

### 2. 모순(Contradiction) 라벨링

**현재 규모:** 실제 모순 23건 + `NO_CONFLICT` 3건

**단계:**

1. **추가 라벨이 필요하면 파일럿 5건으로 형식 검증**
   - 템플릿 첫 5행 작성
   - 분류 5종 확인 (각 유형 최소 1건씩)

2. **검증 후 필요한 표본만 추가**
   - 시간 경과로 업데이트된 내용 우선
   - 출처별로 다르게 서술된 내용 우선

**형식:**

```csv
case_id,claim_A_statement,claim_B_statement,classification,판단_이유
CT-001,Python은 정적 타입 언어다,Python은 동적 타입 언어다,TRUE_CONFLICT,기본 특성에서 직접 충돌
CT-002,메모리 구조 X는 Y바이트다,메모리 구조 X는 Z바이트다 (최신 표준),TIME_CHANGE,기술 버전 업데이트
CT-003,이 알고리즘은 모든 환경에서 최적이다,이 알고리즘은 메모리 부족 환경에서는 비효율적이다,CONDITION_DIFF,조건에 따라 성능 달라짐
CT-004,관점A: 이 정책은 효율적이다,관점B: 이 정책은 비효율적이다,VIEWPOINT_DIFF,전문가 입장 차이
CT-005,이 현상은 원인 A 때문일 수 있다,이 현상은 원인 B 때문일 수 있다,POSSIBLE_CONFLICT,두 원인 모두 가능성 있음 (미확실)
```

**필드 설명:**

- `case_id`: `CT-NNN` 형식의 stable 케이스 식별자
- `claim_A_statement`: 첫 번째 주장 (원문에서 인용, 100자 이내)
- `claim_B_statement`: 두 번째 주장
- `classification`: 5가지 중 하나
  - `TRUE_CONFLICT`: 명백한 사실 충돌 (예: 날짜, 통계, 기본 특성)
  - `TIME_CHANGE`: 시간 경과로 변화 (예: 기술 진화, 정책 변경)
  - `CONDITION_DIFF`: 조건에 따른 차이 (예: 환경, 상황, 대상)
  - `VIEWPOINT_DIFF`: 관점/입장 차이 (예: 학파, 전문가, 이념)
  - `POSSIBLE_CONFLICT`: 의심스러운 충돌 (명확하지 않음)
- `판단_이유`: 왜 이것이 모순인가? (한 줄)

**부정 예시 2~3건 포함 (중요)**

```csv
CT-006,이것은 실제 모순이 아닙니다. 같은 주장을 다르게 표현했습니다.,이것도 같은 내용입니다.,NO_CONFLICT,같은 주장의 다른 표현 (오탐 측정용)
```

### 3. 템플릿 다운로드 및 제출

**위치:**
- `evaluation/relation-labels-template.csv` (연결)
- `evaluation/contradiction-labels-template.csv` (모순)

**형식:**
- 인코딩: UTF-8 (한글 포함)
- 줄바꿈: LF (Unix 스타일)
- 따옴표: 필드에 쉼표나 줄바꿈 있으면 큰따옴표로 감싸기

**검증:**
- 저장 후 `python evaluation/validate_evaluation.py` 실행
- 오류 0건과 분포 출력 확인 후 평가 파이프라인 입력으로 사용

---

## 참고 자료

- 기획서 23절 (Phase 정의) → Phase 0 목표
- 기획서 25절 (벤치마크 상세) → 1단계·2단계 구성
- 요구사항 정의서 11절 (인수 기준) → AC-1~AC-5 측정 방법
- 요구사항 정의서 12절 → UD-11·17·25·29는 Phase 0, UD-26은 Phase 4 별도 트랙
- API 명세 18절 → UD-25 수치 반영 대상
