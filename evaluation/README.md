# Phase 0 평가 세트 (NoteWiki 기준 케이스 데이터)

`.claude/team/backend/PHASE-0-벤치마크-계획.md` 1-2절(평가 세트 구성)에 대응하는 실제 케이스 데이터.
원본은 **NoteWiki 저장소**(`C:/Users/PC/Desktop/ZeroWiki/NoteWiki`)의 `wiki/`에서 추출했다.

## 파일

| 파일 | 내용 | 행 수 |
| --- | --- | --- |
| `pages-inventory.csv` | 페이지 인벤토리 (page_id ↔ slug ↔ 제목 ↔ 타입 ↔ 경로) | 180 |
| `relation-labels.csv` | 정답 연결 세트 | 84 (positive 76 / negative 8) |
| `contradiction-labels.csv` | 정답 모순 세트 | 26 (실제 모순 23 / NO_CONFLICT 3) |
| `contradiction-contexts.csv` | 정답 케이스에 결합하는 중립 context와 원문 참조 | `case_id`별 1행 |
| `contradiction-contexts-v3.csv` | claim A/B별 time·condition 매핑 context와 원문 참조 | `case_id`별 1행 |
| `contradiction-holdout-input.csv` | gold·판단 이유 없이 별도 라벨링하는 holdout 입력 | `HT-NNN` stable ID |
| `relation-labels-template.csv` | 원본 템플릿 (형식 참조용, 수정 X) | 5 |
| `contradiction-labels-template.csv` | 원본 템플릿 (형식 참조용, 수정 X) | 5 |

## 인수 실측 및 계획서 반영

1. **데이터셋 규모**
   - 계획서의 역사적 가정: 원본 36개 · 페이지 152개
   - CSV 실측: `source` **37** + `concept` **127** + `flow` **16** = 총 **180**
   - 지식 페이지(`concept` + `flow`)는 **143**개이고, 소스 요약 페이지 37개까지 포함한 인벤토리 총계가 180개다.
   - `.claude/team/backend/PHASE-0-벤치마크-계획.md`는 이 인수 실측값으로 갱신했다.

2. **페이지 ID 표기**
   - 계획서 예시는 `page-001` 형식이나, 라벨 CSV는 **slug**(`n-plus-1-problem` 등)를 그대로 쓴다.
     파일 경로와 1:1이라 검증이 쉽고, 위키가 커져도 ID가 밀리지 않기 때문.
   - `page-NNN` 형식이 필요하면 `pages-inventory.csv`로 조인한다.

3. **분량**
   - 계획서 목표는 연결 20~30건 / 모순 10~15건이지만, 7종 관계와 5종 모순 분류가 각각
     통계적으로 의미 있는 표본을 갖도록 상향했다(관계 타입당 8~15건, 모순 분류당 3~5건).

## 반복 검증

Python 표준 라이브러리만 사용하는 검증 스크립트가 다음을 검사한다.

- CSV 파싱과 정확한 헤더, 필수 필드
- 인벤토리 `page_id`·`page_slug` 유일성과 `page_type` 집계
- relation slug 참조 무결성, UD-16 7종, direction·confidence enum, positive/negative 규칙
- relation 및 contradiction의 방향 무시 중복 쌍
- contradiction 5종과 `NO_CONFLICT`

```powershell
python evaluation/validate_evaluation.py
```

## relation-labels.csv

`source_page_slug, target_page_slug, relation_type, direction, confidence, label, 근거_한줄`

- 양 끝점은 `pages-inventory.csv`의 `page_slug`와 exact join한다.

- `relation_type`: UD-16 확정 7종. **negative 행은 비워둔다**(계획서 규칙 준수).
- 관계 타입 분포: `DEPENDS_ON` 15, `PART_OF` 12, `RELATED_TO` 12, `APPLIES_TO` 12,
  `EXPLAINS` 9, `CAUSES` 8, `CONTRASTS_WITH` 8
- **페이지 쌍은 전체에서 유일하다**(방향 무시 기준). 한 쌍에 두 개의 라벨이 붙지 않으므로
  채점 시 "정답 관계 타입 1개"로 단순 비교할 수 있다.
- negative 8건 중 **4건은 어려운 오탐 케이스**다 — 표현이 겹치거나 한쪽 페이지가 다의어를
  다뤄 모델이 잘못 연결하기 쉬운 쌍(`java-buffer`/`http-cache`, `domain`/`forward-proxy`,
  `serialization`/`css-modules`, `static-keyword`/`graph-view`). 나머지 4건은 명백한 무관 쌍.

## contradiction-labels.csv

`case_id, claim_A_statement, claim_B_statement, classification, 판단_이유`

- `case_id`: `CT-NNN` 형식의 stable 평가 케이스 식별자. 문장 수정이나 정렬 순서와 무관하게 결과를 조인한다.
- 분류 분포: `TRUE_CONFLICT` 5, `TIME_CHANGE` 5, `CONDITION_DIFF` 5, `VIEWPOINT_DIFF` 5,
  `POSSIBLE_CONFLICT` 3, `NO_CONFLICT` 3
- 주장은 실제 위키 서술에 근거한다. 예:
  - `TIME_CHANGE` — 서블릿 스펙의 Oracle → Jakarta EE 이관(`wiki/concepts/servlet-spec.md`),
    Spring Boot 2.x 기본 풀 HikariCP(`wiki/concepts/hikaricp.md`)
  - `CONDITION_DIFF` — `ddl-auto=update`의 로컬 편의 vs 운영 금기
    (`wiki/concepts/hibernate-ddl-auto.md`)
  - `TRUE_CONFLICT` — 커버리지 100%의 보증 범위(`wiki/concepts/test-coverage.md`)
- `NO_CONFLICT` 3건은 오탐 측정용이다. 특히 3번째(`Filter`/`Interceptor`)는 두 서술이 서로
  다른 대상을 말하는데 문장 구조가 대비형이라 모순으로 오판하기 쉬운 케이스다.

## OpenAI prompt 입력·holdout

`contradiction-contexts.csv`의 context 열은 `time_or_version`,
`environment_or_condition`, `subject`로 제한하며, 입력에 근거가 없으면
`NOT_PROVIDED`를 사용한다. `source_ref`는 원문 추적용 메타데이터이고 모델 prompt에는
주입하지 않는다. `phase0_benchmark.py --prompt-variant rubric-context-v2`가 이 파일을
읽어 claim A/B와 중립 JSON으로 결합한다.

`contradiction-contexts-v3.csv`는 같은 subject에 대해 claim A/B의
`time_or_version`와 `environment_or_condition`을 별도로 매핑한다. `rubric-context-v3`는
명시적으로 매핑된 TIME·CONDITION 차이를 `NO_CONFLICT`보다 먼저 적용해 차이를 가장 잘
설명하는 축을 고르고, source ref와 case/gold/reason은 prompt에서 제외한다.

`contradiction-holdout-input.csv`는 `case_id`, 두 claim, 같은 세 context 열, 양쪽
`source_ref`만 가진다. 정답 classification이나 `판단_이유`를 저장하지 않아 QA가 별도로
라벨링할 수 있다. 기존 `CT-NNN` claim pair와 exact normalized pair가 겹치지 않는지
harness `--self-check`가 확인하며, holdout loader는 exact `HT-001`~`HT-070` 집합과
holdout 내부의 NFKC·연속 공백 축약·case-fold·A/B 순서 무시 중복도 거부한다.

### Holdout 실행 입력 계약

holdout을 실행할 때만 `--benchmark-input evaluation/contradiction-holdout-input.csv`
와 `--gold-labels <QA가 별도로 만든 CSV>`를 함께 지정한다. gold 파일의 헤더는
`case_id,classification,판단_이유`이며 두 파일의 stable `HT-NNN` 집합이 일치해야 한다.
join한 gold와 `case_id`는 manifest·평가에만 사용하고 모델 prompt에는 넣지 않는다.
기존 `--labels evaluation/contradiction-labels.csv` 경로와 six-label 26-case 기본 모드는
그대로 유지된다. `rubric-v2`, `rubric-context-v2`, `rubric-context-v3`에는 평가 문장과
겹치지 않는 synthetic label example이 공통으로 포함되며 context variant는 중립 context
JSON만 추가한다. v3 실행은 `--openai-contexts evaluation/contradiction-contexts-v3.csv`를
지정한다.

### Holdout 최종 데이터 스냅샷 (2026-08-11)

`contradiction-holdout-input.csv`는 실제 NoteWiki 원문을 근거로 70행을 재구성했다.
최종 review 목표는 `TRUE_CONFLICT`, `TIME_CHANGE`, `CONDITION_DIFF`,
`VIEWPOINT_DIFF`, `POSSIBLE_CONFLICT` 각 8건과 `NO_CONFLICT` 30건이다. 초기 초안의
`34/4/2/0/1/29` 분포와 HT-065 경계 blocker는 교체 pair로 해소했으며, 140개 source
reference가 모두 존재하고 NFKC 정규화 중복은 0건이다.

claim·context·subject는 기술 고유명사를 제외하고 한국어로 작성했다. 사용자용 review의
헤더·값도 한국어로 유지하며, machine input의 exact header/stable ID 계약과 분리한다.
review 판정은 별도 승인 전까지 machine gold enum으로 역매핑하지 않으며, `user_decision`은
비어 있다. gold 파일 생성, holdout API 실행, production 적용은 모두 아직 실행하지
않았다. QA review 파일은 이 작업에서 수정하지 않는다.

### 최근 claim-specific v3 실행

`evaluation/results/phase0-gpt-5_6-sol-rubric-context-v3-r1/`에서 26/26건을 26회 호출·retry
없이 완료했다. accuracy `24/26=0.9230769`, conflict recall `22/23=0.9565217`, sampled
FPR `0/3=0`, macro-F1 `0.902`, 비용 `$0.14906`, latency total/p95
`64.443268s/5.344194s`이며 QA validator는 exit 0이다. 오분류는 `CT-017`
(`VIEWPOINT_DIFF`를 `POSSIBLE_CONFLICT`로 예측)과 `CT-021` (`POSSIBLE_CONFLICT`를
`NO_CONFLICT`로 예측)이다. 이는 dev gate PASS 결과이며 holdout 미실행·production 승격 미확정을
변경하지 않는다.

## 채점 방법

### 연결 오프라인 지표

현재 CSV가 제공하는 evaluated candidate universe `R`은 라벨된 84개 페이지 쌍
(positive 76 + sampled negative 8)으로 한정된다.

- **pair recall** = `관계를 출력한 positive 쌍 / 76`. 끝점 순서를 무시하고 쌍만 일치시키며,
  relation type은 보지 않는다.
- **strict recall** = `UD-16 relation_type과 direction까지 일치한 positive / 76`.
  `forward`는 source/target 순서까지 같아야 하고 `bidirectional`은 끝점 순서를 무시한다.
- **sampled negative FPR** = `negative 8쌍 중 어떤 관계라도 출력한 쌍 / 8`.
- **bounded pair precision** = `R 안에서 관계를 출력한 positive 쌍 / R 안에서 관계를 출력한 전체 쌍`.
  분모가 0이면 `0%`나 `100%`가 아니라 **N/A**로 기록한다.

`R` 밖에서 출력된 관계는 gold label이 없으므로 위 precision의 분자·분모에서 제외하며,
사람이 유용성 또는 오탐 여부를 판정한 뒤에만 확장할 수 있다. 따라서 이 값은 전체 페이지
공간의 precision이 아니라 **라벨된 84쌍에 한정된 표본 지표**다.

이 오프라인 recall·precision은 **AC-3가 아니다.** `docs/ZeroWiki-요구사항-정의서.md`
11절의 AC-3은 U-1에서 검증 사용자 5~10명이 실제 제안 관계를 평가해
`유용 판정 / 평가한 제안 >= 70%`인지 확인하는 사용자 인수 기준이다. 이 CSV로 AC-3을
대체하거나 통과로 선언할 수 없다.

### 모순 오프라인 지표와 매칭 계약

- scorer의 1순위 조인 키는 stable `case_id`다. 문장 쌍을 직접 주입하는 오프라인 harness는
  입력부터 출력까지 `case_id`를 보존해야 한다.
- `case_id`를 보존할 수 없는 문장 단위 보조 비교는 양쪽 문장을 Unicode NFKC 정규화하고,
  앞뒤 공백 제거·연속 공백 축소·case-fold한 뒤 **claim A/B 순서를 무시한 exact pair**로만
  일치시킨다. 의미 유사도나 임의 임계값으로 자동 일치시키지 않으며, exact 불일치는 사람이 판정한다.
- **모순 recall(AC-5 오프라인 분모)** = `탐지된 conflict case_id / 23`.
  80% 기준의 최소 정수는 19건이다.
- **sampled FPR** = `NO_CONFLICT 오탐 case_id 수 / 3`.
- **FDR** = `오탐으로 판정된 탐지 / 사람이 정답 여부를 판정한 전체 positive 탐지`.
  분모가 0이면 **N/A**다. 평가 세트 밖 탐지는 사람이 판정하기 전까지 FDR에 포함하지 않는다.
- 분류 정확도는 탐지에 성공한 실제 conflict에 한해 5종 confusion matrix로 별도 집계한다.

현재 CSV에는 원본 `page_id`·`claim_id` 또는 seed 입력에서 Ingest가 생성한 Claim으로 이어지는
매핑이 없다. 따라서 stable `case_id`는 오프라인 케이스 조인을 안정화하지만, **Ingest-level
AC-5 재현을 완성하지는 않는다.** 원본 page/claim mapping manifest와 Ingest 결과 `claim_id`
대응이 마련되기 전에는 AC-5 최종 통과를 선언할 수 없다.

## 인코딩

UTF-8(BOM 없음), LF 줄바꿈, 쉼표 포함 필드는 큰따옴표로 감쌈.
