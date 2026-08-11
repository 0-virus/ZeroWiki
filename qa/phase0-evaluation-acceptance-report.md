# Phase 0 평가 세트 독립 QA 재인수 보고서

검증일: 2026-08-11 (초기 재인수: 2026-08-10)
대상: `evaluation/pages-inventory.csv`, `evaluation/relation-labels.csv`,
`evaluation/contradiction-labels.csv`, `evaluation/README.md`
판정: **CSV 구조·mapping manifest 조인 PASS / rubric-context-v3 dev 품질 gate PASS / holdout·production BLOCKED**

## 1. 기준과 범위

- 제품 기준: `docs/ZeroWiki-MVP-서비스-기획서.md` 3절
- 측정 기준: `docs/검증-QA-계약-확정-체크리스트.md` 3절
- 인수 기준: `docs/ZeroWiki-요구사항-정의서.md` 1.4절·11절의 AC-3·AC-5
- 실행 시나리오: `qa/test_scenarios.md` A-4·U-1
- enum 정본: `docs/ZeroWiki-요구사항-정의서.md` 12.1절 UD-16

`qa/verify_phase0_evaluation.py`는 backend validator와 별도로 CSV 스키마, 필수값,
참조, 유일성, enum, negative 규칙, 중복, stable `case_id`와 분모를 검증한다.

## 2. Findings

### HIGH-1 (이전) — Ingest-level AC-5 mapping manifest 부재

`contradiction-labels.csv`의 `case_id`와 README의 NFKC·공백 축소·case-fold·순서
무시 exact-pair 계약으로 **offline 후보 채점은 재현 가능**해졌다. 그러나 원본
page/claim 또는 seed 입력이 Ingest 결과의 `claim_id`로 어떻게 이어지는지 나타내는
manifest는 당시 없었다. 실제 `GET /libraries/{id}/contradictions` 응답을 gold
`case_id`에 결정론적으로 조인할 수 없으므로 당시 Ingest-level AC-5 최종 통과는
선언할 수 없었다.

영향: AC-5, A-4, FR-KNW-05, FR-ING-11.

당시 필요 변경(backend 소유): seed source/page/claim 식별자, gold `case_id`, Ingest
후 `claim_id` 쌍을 연결하는 versioned mapping manifest와 생성 절차를 추가하는 것.

**현재 상태: 해결 완료(PASS, 2026-08-11 후속 검증).** 실제
`evaluation/results/phase0-manifest.json`(schema v1, 26 cases),
`phase0-generated-claims.csv`, `phase0-contradictions.csv`가 도착했고,
`qa/verify_phase0_benchmark.py`가 manifest hash·gold label·generated claim pair를
전체 조인했다. 독립 실행에서 mapping 구조는 PASS이며, 남은 product gate는 mapping
부재가 아니라 contradiction 분류 품질이다(`recall=1.000`, `sampled_fpr=1.000`,
`classification_accuracy=0.192`, `RESULT FAIL`).

## 3. 이전 findings 재판정

| 이전 finding | 재판정 | 근거 |
| --- | --- | --- |
| AC-3을 gold “정확도”로 대체 | 해소 | README가 pair/strict recall과 bounded precision을 offline 지표로 명명하고 AC-3=U-1 사용자 평가를 별도 유지 |
| 모순 FPR/FDR 분모 혼동 | 해소 | sampled FPR=`NO_CONFLICT` 오탐/3, FDR은 사람이 판정한 positive 탐지 분모로 분리하고 0분모는 N/A |
| contradiction stable join 규칙 부재 | offline 범위 해소 | `CT-001`~`CT-026`과 `case_id` 우선, NFKC exact-pair 보조 규칙 추가 |
| `*_page_id` 헤더에 slug 저장 | 해소 | `source_page_slug`, `target_page_slug`로 변경하고 inventory `page_slug` exact join 명시 |
| AC-5 요구사항의 A-5 참조 | 해소 | `docs/ZeroWiki-요구사항-정의서.md` 11절이 A-4를 참조 |
| QA A-4의 32건/25건·구 enum | 해소 | 실제 conflict 23건, 최소 19건, 확정 5종 및 manifest precondition으로 갱신 |

AC-3은 해소된 것이 아니라 **offline 평가 세트의 인수 범위가 아님이 명확해진 것**이다.
최종 AC-3 판정은 계속 U-1에서 검증 사용자 5~10명의
`유용 판정 / 평가한 제안 >= 70%`로 수행한다.

## 4. 구조·분모 검증 결과

QA 독립 검증 결과:

```text
COUNTS pages=180 page_types={'concept': 127, 'flow': 16, 'source': 37} relations=84 positive=76 negative=8 contradictions=26 conflict=23 no_conflict=3
RESULT PASS failures=0
```

backend validator도 동일 데이터에 대해 오류 0건을 반환해야 인수한다.

| 지표 | 분모 | 현재 판정 |
| --- | ---: | --- |
| relation pair recall | positive 76 | offline 채점 가능 |
| relation strict recall | positive 76 | type·direction 계약에 따라 채점 가능 |
| relation sampled FPR | negative 8 | 채점 가능, 1건당 12.5%p |
| bounded pair precision | 라벨된 84쌍 중 출력 쌍 | 분모 0은 N/A |
| contradiction recall | conflict 23 | offline 채점 가능, 통과 최소 19 |
| contradiction sampled FPR | `NO_CONFLICT` 3 | 채점 가능, 1건당 33.3%p |
| contradiction FDR | 사람이 판정한 positive 탐지 | 분모 0은 N/A |
| AC-3 사용자 유용성 | 사용자 평가 제안 | U-1 전까지 미실행 |
| Ingest-level AC-5 mapping | conflict 23 | manifest/gold/generated join PASS |
| Ingest-level AC-5 품질 gate | conflict 23 | contradiction accuracy/FPR FAIL |

## 5. A-4 동기화

`qa/test_scenarios.md` A-4와 QA 체크리스트 3.1절을 다음 계약으로 맞췄다.

- 입력 후보 26건: conflict 23 + `NO_CONFLICT` 3
- AC-5 recall 통과: conflict 23건 중 최소 19건 탐지
- 분류 enum: `TRUE_CONFLICT`, `TIME_CHANGE`, `CONDITION_DIFF`,
  `VIEWPOINT_DIFF`, `POSSIBLE_CONFLICT`
- `NO_CONFLICT`는 recall에서 제외하고 sampled FPR 분모로 사용
- Ingest-level 실행 전 mapping manifest 필수

## 6. 재현 명령

```powershell
$env:PYTHONIOENCODING='utf-8'
python qa/verify_phase0_evaluation.py
python evaluation/validate_evaluation.py
```

잔존 참조 검사는 활성 schema·시나리오·계약 파일을 대상으로 수행한다. 재판정 표와
append-only WORKLOG의 과거 판단은 보존 기록이므로 대상에서 제외한다.

```powershell
rg -n "source_page_id,target_page_id|32개 의도적 모순|25개 이상 자동 탐지|DIRECT_CONTRADICTION|AC-5[^\r\n]*\| A-5 \|" evaluation qa/verify_phase0_evaluation.py qa/test_scenarios.md qa/README.md docs/검증-QA-계약-확정-체크리스트.md docs/ZeroWiki-요구사항-정의서.md
```

정상 상태에서는 과거 판단을 보존한 WORKLOG 외의 활성 계약·시나리오에서 결과가
없어야 한다.

## 7. 남은 위험

- mapping manifest precondition은 해소됐다. contradiction 품질 gate가 남아 있다.
- relation negative 8건과 contradiction `NO_CONFLICT` 3건은 sampled FPR의
  분해능이 각각 12.5%p, 33.3%p로 낮다.
- 라벨의 의미 정확성에 대한 독립 이중 검토와 외부 NoteWiki 원본 대조는 미실행이다.
- AC-3은 평가 CSV가 아니라 실제 사용자 U-1을 실행해야 판정할 수 있다.

## 8. 2026-08-11 Phase 0 실제 산출물 독립 인수

> 이전 실행 스냅샷이다. 이 절의 FAIL/BLOCKED 판정은 당시 qwen 결과를 보존한
> 기록이며, 최신 현재 판정은 11절의 rubric-context-v3 dev PASS 및
> holdout·production BLOCKED이다.

### 8.1 범위와 계약

backend 최종 산출물 묶음은 다음 하나의 실행 ID와 연결된다.

- `evaluation/results/phase0-manifest.json`: schema v1, `run_id`, 26개 `cases`,
  각 case의 `gold.claim_a/b` page·claim 식별자와 `generated_claim_ids` 2개
- `evaluation/results/phase0-generated-claims.csv`: 위 manifest의 `case_id`별 생성
  claim 쌍
- `evaluation/results/phase0-benchmark.csv`: 문서별 실행 상태·시간·토큰
- `evaluation/results/phase0-contradictions.csv`: 생성 claim 쌍별 분류·confidence
- `evaluation/results/phase0-metrics.json`: 100문서·모순·왕복·이벤트 재생 요약

`qa/verify_phase0_benchmark.py`는 이 경로들을 인자로 받고, manifest hash와 gold 라벨
대응까지 확인한 뒤 benchmark/AC-5 품질을 판정한다. 산출물이 없을 때는 `BLOCKED`를
반환하고, 현재처럼 구조는 맞지만 품질 기준이 깨지면 `FAIL`을 반환한다.

### 8.2 심각도 순 findings

#### HIGH-1 — 모순 품질 gate 실패

독립 실행 결과는 conflict recall `1.000`이지만 sampled FPR `1.000`, 전체 분류
정확도 `0.192`였다. 26건을 모두 `CONDITION_DIFF`로 출력해 `NO_CONFLICT` 3건도
전부 오탐했고, recall만 검사하던 이전 verifier라면 잘못 PASS할 수 있었다. 정확도
최소 `0.80` gate를 추가한 verifier가 이를 차단했다.

영향: 요구사항 AC-5/A-4, `FR-KNW-05`, `FR-ING-11`, 기획서 3·23절의 모순 품질
검증. `100/100` 처리 완료만으로 Phase 0 완료나 Phase 1 품질 통과를 선언할 수 없다.

#### HIGH-2 (발견 당시) — Windows root-relative path traversal 방어 결함

backend `relative_path()`와 `safe_zip_member()`는 `../` 및 backslash traversal은
거부하고 secret redaction도 동작하지만, Windows `Path('/absolute.md').is_absolute()`가
거짓이 되는 환경에서 `/absolute.md`를 허용했다.

재현:

```text
PASS relative_path rejects '../escape.md'
PASS relative_path rejects 'wiki/../escape.md'
FAIL relative_path rejects root-relative '/absolute.md'
PASS safe_zip_member rejects 'wiki\\..\\escape.md'
PASS redact_secrets removes values/tokens
```

영향: 기획서 18절·요구사항 15절 S-2/NFR-SEC 입력 경로 방어. 당시 backend 소유
파일은 QA가 수정하지 않았고, 수정 요청으로 이 finding을 전달했다.

**현재 상태: 해결 완료(PASS, 2026-08-11 후속 재검증).** backend가
`relative_path()` 조건에 `candidate.drive`와 `candidate.root`를 추가하고,
self-check에 `../escape.md`, `/absolute.md`, `C:\\absolute.md` 회귀 케이스를
추가했다. 후속 독립 실행 결과는 세 경로와 ZIP member traversal, `redact_secrets`
모두 PASS였다.

```text
PASS relative_path rejects '/absolute.md'
PASS relative_path rejects 'C:\\absolute.md'
PASS safe_zip_member rejects '/absolute.md'
PASS redact_secrets
self-check: PASS
```

#### MEDIUM-1 — 원본 inventory 17건 누락

NoteWiki root 기준 inventory 180행 중 163개가 존재하고 17개가 누락됐다. 존재하는
163개에 대한 ZIP Markdown byte roundtrip은 PASS지만, `--allow-missing` 없이는
harness가 종료 코드 1로 완료 판정을 거부한다. 현재 별도 100문서 실행은 100/100으로
완료됐으나 전체 inventory 입력 완결성은 확보되지 않았다.

#### MEDIUM-2 — 비용·제품 통합 검증은 미완료

metrics의 `monetary_api_cost_usd=0`은 local Ollama 실행 비용만 뜻한다. 실제 공급자
비용, API 응답·citation, 사용자 AC-3, 보안 시나리오 S-1~S-16은 이 산출물만으로
통과시킬 수 없다.

### 8.3 재현 명령과 실제 출력

```powershell
$env:PYTHONIOENCODING='utf-8'
python qa/verify_phase0_evaluation.py
python evaluation/validate_evaluation.py
python qa/verify_phase0_benchmark.py `
  --manifest evaluation/results/phase0-manifest.json `
  --generated-claims evaluation/results/phase0-generated-claims.csv `
  --benchmark-output evaluation/results/phase0-benchmark.csv `
  --expected-documents 100 `
  --contradiction-output evaluation/results/phase0-contradictions.csv
```

실제 요약:

```text
qa/verify_phase0_evaluation.py: RESULT PASS failures=0
benchmark: expected=100 completed=100 completion_rate=1.000
AC5: conflict=23 detected=23 recall=1.000 sampled_fpr=1.0 classification_accuracy=0.192
qa/verify_phase0_benchmark.py: RESULT FAIL failures=1 (exit 1)
```

metrics 교차 검증은 `roundtrip files_checked=100, mismatches=[], passed=true`,
event replay `change_sets=1000, iterations=25, p95≈0.000070s,
snapshot_required=false`로 PASS했다. LF/CRLF 회귀 self-check는 LF·CRLF PASS,
mixed·bare CR 거부 PASS였고 실제 results 파일도 모두 줄바꿈 검사를 통과했다.

`backend/contracts/library-constitution.schema.json`은 표준 JSON 검사로 top-level
12 properties, required 12개, `additionalProperties=false` 및 6개 nested object의
required/additionalProperties 규칙을 PASS했다. 이는 schema 구조 인수이며 실제 API
저장·조회 통합 검증을 대체하지 않는다.

### 8.4 최종 판정과 잔여 위험

| 게이트 | 판정 | 근거 |
| --- | --- | --- |
| CSV 구조·인코딩·LF/CRLF | PASS | 독립 verifier exit 0 |
| manifest schema/hash·26 case mapping | PASS | schema v1, gold/generated 전체 join |
| 100문서 AC-1 실행 | PASS(실행 범위) | 100/100 COMPLETED, 1.000 |
| AC-5/A-4 품질 | **FAIL** | accuracy 0.192, sampled FPR 1.000 |
| Markdown roundtrip/replay | PASS | 100 파일 무손실, replay p95 < 1s |
| 경로 보안 | PASS | backend root/drive guard 및 self-check 후속 재검증 |
| Phase 0 전체 완료/Phase 1 품질 진입 | **BLOCKED** | HIGH-1 모순 품질 gate FAIL |

따라서 현재는 **기술 검증 실행은 완료됐지만 product gate는 FAIL이며 Phase 1 품질
진입은 BLOCKED**다. 경로 보안 finding은 해결됐으며, 모델/분류 전략을 개선한 새
산출물을 같은 명령으로 재실행해야 한다.

## 9. 2026-08-11 GPT model-specific run 독립 재검증

### 9.1 대상과 범위

backend가 생성한 model-specific 결과를 기존 validator 변경 없이 재검증했다.

- 대상 디렉터리: `evaluation/results/phase0-gpt-5_6-sol-r1/`
- 실행 ID: `phase0-20260811-gpt-5_6-sol-r1`
- manifest schema v1, gold 26 cases, generated claim mapping 26행
- benchmark CSV 26행, 모두 `COMPLETED`
- contradiction output 18행
- `openai-calls.csv`의 API key·응답 본문은 QA 검증 범위에서 제외하고 읽지 않았다.

GPT run은 contradiction 26-case 실행이므로 `--expected-documents 26`으로
검증했다. qwen baseline의 100문서 benchmark 완료율과 직접 합산하거나 동일
실행 범위로 간주하지 않는다.

### 9.2 심각도 순 findings

#### HIGH-3 — GPT contradiction 품질 gate 실패

독립 validator 결과:

```text
BENCHMARK_METRICS expected=26 completed=26 completion_rate=1.000
AC5_METRICS conflict=23 detected=18 recall=0.783 sampled_fpr=0.0 classification_accuracy=0.615
FAIL AC-5 recall 0.783 >= 0.800
FAIL Phase 0 contradiction classification accuracy 0.615 >= 0.800
RESULT FAIL failures=2
EXIT_CODE=1
```

따라서 GPT 결과도 AC-5/A-4 최소 recall 0.80 및 분류 정확도 0.80을 충족하지
못한다. Phase 1 품질 진입과 모순 자동 적용은 계속 BLOCKED다.

#### MEDIUM-3 — contradiction output 8개 case 누락

18개 output 행은 모두 generated claim pair에 조인되고 5종 enum을 통과했다.
그러나 다음 8개 case는 output에 없었다:

```text
CT-011, CT-012, CT-014, CT-015, CT-021, CT-024, CT-025, CT-026
```

validator는 누락 prediction을 `NO_CONFLICT`로 간주해 accuracy에 반영한다. 실제
계산은 conflict 18/23 탐지, `NO_CONFLICT` 0/3 탐지, 정답 16/26이다. output
행 수를 26으로 강제하는 계약은 현재 validator에 없지만, 이번에는 누락된
positive case가 recall·accuracy gate를 낮추므로 품질 통과를 우회하지 못했다.

### 9.3 baseline 비교

| 지표 | qwen3:1.7b baseline | GPT model-specific | 변화 | 판정 |
| --- | ---: | ---: | ---: | --- |
| contradiction recall | 1.000 (23/23) | 0.783 (18/23) | -0.217 | GPT FAIL |
| sampled FPR | 1.000 (3/3) | 0.000 (0/3) | -1.000 | GPT 개선 |
| 6-class accuracy | 0.192 (5/26) | 0.615 (16/26) | +0.423 | GPT 개선이나 gate 미달 |
| validator exit code | 1 | 1 | 동일 | 둘 다 FAIL |

qwen은 26개 전부를 `CONDITION_DIFF`로 출력했고, GPT는
`TRUE_CONFLICT=6`, `TIME_CHANGE=3`, `CONDITION_DIFF=2`, `VIEWPOINT_DIFF=6`,
`POSSIBLE_CONFLICT=1`을 출력했다. GPT의 분류 다양성과 sampled FPR은 개선됐지만,
positive case 누락으로 recall이 기준 아래에 남았다.

### 9.4 구조 검증과 재현 명령

```powershell
$env:PYTHONIOENCODING='utf-8'
python qa/verify_phase0_evaluation.py
# COUNTS pages=180 ... contradictions=26 conflict=23 no_conflict=3
# RESULT PASS failures=0; exit 0

python evaluation/validate_evaluation.py
# 검증 완료: 오류 0건; exit 0

python qa/verify_phase0_benchmark.py --self-check
# SELF-CHECK PASS manifest, mapping, newline, recall-only false PASS rejection; exit 0

python qa/verify_phase0_benchmark.py `
  --manifest evaluation/results/phase0-gpt-5_6-sol-r1/phase0-manifest.json `
  --generated-claims evaluation/results/phase0-gpt-5_6-sol-r1/phase0-generated-claims.csv `
  --benchmark-output evaluation/results/phase0-gpt-5_6-sol-r1/phase0-benchmark.csv `
  --expected-documents 26 `
  --contradiction-output evaluation/results/phase0-gpt-5_6-sol-r1/phase0-contradictions.csv
# 위 9.2절 출력; exit 1
```

구조·mapping·label enum은 PASS이지만 GPT product gate는 FAIL이다. 새 모델의
재실행에서는 26개 case 전체 output 또는 누락 사유를 결정론적으로 기록하고,
동일 validator에서 recall·accuracy를 다시 통과해야 한다.

## 10. 2026-08-11 rubric dense variant 독립 재검증

### 10.1 범위와 판정 프로파일

API 재호출 없이 backend가 생성한 두 dense prediction CSV를 기존
`qa/verify_phase0_benchmark.py`로 재실행했다. 두 실행 모두 기존 26-case
`evaluation/contradiction-labels.csv`를 `--labels`로 사용한 dev profile이다.
따라서 Wilson upper는 관측치로만 출력하고 hard gate로 적용하지 않았다.

holdout review artifact `qa/contradiction-holdout-review.csv`는 70건이며,
분포는 `TRUE_CONFLICT=34`, `TIME_CHANGE=4`, `CONDITION_DIFF=2`,
`VIEWPOINT_DIFF=0`, `POSSIBLE_CONFLICT=1`, `NO_CONFLICT=29`이다. 목표
`5종 x 8건 + NO_CONFLICT 30건`과 불일치하고 HT-065에
`REVIEW_REQUIRED`가 남아 있어 사용자 승인 및 gold freeze가 불가능하다.
그러므로 holdout label 확정이나 holdout API 실행은 진행하지 않았다.

### 10.2 재현 명령과 실제 결과

```powershell
python qa/verify_phase0_benchmark.py `
  --manifest evaluation/results/phase0-gpt-5_6-sol-rubric-v2-r2/phase0-manifest.json `
  --generated-claims evaluation/results/phase0-gpt-5_6-sol-rubric-v2-r2/phase0-generated-claims.csv `
  --benchmark-output evaluation/results/phase0-gpt-5_6-sol-rubric-v2-r2/phase0-benchmark.csv `
  --expected-documents 26 `
  --dense-predictions evaluation/results/phase0-gpt-5_6-sol-rubric-v2-r2/openai-predictions.csv `
  --labels evaluation/contradiction-labels.csv
# DENSE_METRICS expected=26 completed=26 failed=0 completion_rate=1.000
# recall=0.739 sampled_fpr=0.000 wilson95_fpr_upper=0.561
# accuracy=0.615 detected_conflict_subtype_accuracy=0.765
# joint_subtype_accuracy=0.565 macro_f1=0.553
# RESULT FAIL failures=2; exit 1

python qa/verify_phase0_benchmark.py `
  --manifest evaluation/results/phase0-gpt-5_6-sol-rubric-context-v2-r1/phase0-manifest.json `
  --generated-claims evaluation/results/phase0-gpt-5_6-sol-rubric-context-v2-r1/phase0-generated-claims.csv `
  --benchmark-output evaluation/results/phase0-gpt-5_6-sol-rubric-context-v2-r1/phase0-benchmark.csv `
  --expected-documents 26 `
  --dense-predictions evaluation/results/phase0-gpt-5_6-sol-rubric-context-v2-r1/openai-predictions.csv `
  --labels evaluation/contradiction-labels.csv
# DENSE_METRICS expected=26 completed=26 failed=0 completion_rate=1.000
# recall=0.783 sampled_fpr=0.000 wilson95_fpr_upper=0.561
# accuracy=0.692 detected_conflict_subtype_accuracy=0.833
# joint_subtype_accuracy=0.652 macro_f1=0.683
# RESULT FAIL failures=2; exit 1
```

| variant | completion | recall | sampled FPR | accuracy | detected subtype | joint subtype | macro-F1 | Wilson upper (관측) | exit |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| rubric-v2 r2 | 26/26 | 0.739 | 0.000 | 0.615 | 0.765 | 0.565 | 0.553 | 0.561 | 1 |
| rubric-context-v2 r1 | 26/26 | 0.783 | 0.000 | 0.692 | 0.833 | 0.652 | 0.683 | 0.561 | 1 |

두 variant 모두 completion/FPR은 통과했지만 recall와 6-class accuracy가
각각 0.80 gate에 미달해 dev gate를 통과하지 못했다. rubric-context-v2 r1이
기존 rubric-v2 r2보다 recall `+0.044`, accuracy `+0.077`, macro-F1 `+0.130`
개선됐지만 product gate 판정은 여전히 FAIL이다. 26-case의 NO_CONFLICT가
3건뿐이므로 Wilson upper `0.561`은 관측치로만 기록한다.

### 10.3 confusion matrix (gold 행 → prediction 열)

열 순서는 `TRUE_CONFLICT,TIME_CHANGE,CONDITION_DIFF,VIEWPOINT_DIFF,
POSSIBLE_CONFLICT,NO_CONFLICT`이다.

```text
rubric-v2 r2
TRUE_CONFLICT       5,0,0,0,0,0
TIME_CHANGE         1,3,0,1,0,0
CONDITION_DIFF      0,0,0,0,0,5
VIEWPOINT_DIFF      1,0,0,4,0,0
POSSIBLE_CONFLICT   1,0,0,0,1,1
NO_CONFLICT         0,0,0,0,0,3

rubric-context-v2 r1
TRUE_CONFLICT       5,0,0,0,0,0
TIME_CHANGE         0,5,0,0,0,0
CONDITION_DIFF      0,0,0,0,0,5
VIEWPOINT_DIFF      0,0,2,3,0,0
POSSIBLE_CONFLICT   0,0,1,0,2,0
NO_CONFLICT         0,0,0,0,0,3
```

### 10.4 최종 QA 판정

- **HIGH:** 두 rubric dense variant 모두 recall/accuracy gate FAIL(exit 1). 따라서 holdout API 실행을 금지하고, 사용자 승인 전 gold freeze도 금지한다.
- **MEDIUM:** holdout review 분포가 목표와 다르고 HT-065가 `REVIEW_REQUIRED`이다. 관점 라벨 0건을 포함해 분포를 맞추기 위한 강제 재라벨링은 하지 않았다.
- 구조 검증과 dense self-check는 별도로 PASS이지만, 이는 모델 품질 gate 통과를 의미하지 않는다. 기존 qwen/GPT sparse 및 이전 model-specific 결과 기록은 보존한다.

## 11. 2026-08-11 rubric-context-v3 최종 독립 QA

API 재호출 없이 backend가 생성한
`evaluation/results/phase0-gpt-5_6-sol-rubric-context-v3-r1/` 산출물을 직접 읽고
기존 `--labels` dev 계약으로 검증했다. 결과는 26/26 `COMPLETED`이며, 오분류는
`CT-017`(gold `VIEWPOINT_DIFF` → `POSSIBLE_CONFLICT`)과 `CT-021`(gold
`POSSIBLE_CONFLICT` → `NO_CONFLICT`) 두 건이다.

| 지표 | v3 결과 | dev gate/해석 |
| --- | ---: | --- |
| completion | 26/26 (1.000) | PASS |
| conflict recall | 22/23 (0.957) | PASS (>= 0.80) |
| sampled FPR | 0/3 (0.000) | PASS (<= 0.10) |
| 6-class accuracy | 24/26 (0.923) | PASS (>= 0.80) |
| detected-conflict subtype accuracy | 21/22 (0.955) | 보고 |
| joint subtype accuracy | 21/23 (0.913) | 보고 |
| macro-F1 | 0.902 | 보고 |
| Wilson 95% FPR upper | 0.561 | dev에서는 관측치만 보고 |

confusion matrix의 열 순서는
`TRUE_CONFLICT,TIME_CHANGE,CONDITION_DIFF,VIEWPOINT_DIFF,POSSIBLE_CONFLICT,NO_CONFLICT`이다.

```text
TRUE_CONFLICT       5,0,0,0,0,0
TIME_CHANGE         0,5,0,0,0,0
CONDITION_DIFF      0,0,5,0,0,0
VIEWPOINT_DIFF      0,0,0,4,1,0
POSSIBLE_CONFLICT   0,0,0,0,2,1
NO_CONFLICT         0,0,0,0,0,3
```

### 11.1 재현 명령과 실제 종료 코드

```powershell
python qa/verify_phase0_benchmark.py `
  --manifest evaluation/results/phase0-gpt-5_6-sol-rubric-context-v3-r1/phase0-manifest.json `
  --generated-claims evaluation/results/phase0-gpt-5_6-sol-rubric-context-v3-r1/phase0-generated-claims.csv `
  --benchmark-output evaluation/results/phase0-gpt-5_6-sol-rubric-context-v3-r1/phase0-benchmark.csv `
  --dense-predictions evaluation/results/phase0-gpt-5_6-sol-rubric-context-v3-r1/openai-predictions.csv `
  --labels evaluation/contradiction-labels.csv `
  --expected-documents 26
# DENSE_METRICS expected=26 completed=26 failed=0 completion_rate=1.000
# recall=0.957 sampled_fpr=0.000 wilson95_fpr_upper=0.561 accuracy=0.923
# detected_conflict_subtype_accuracy=0.955 joint_subtype_accuracy=0.913 macro_f1=0.902
# RESULT PASS failures=0; exit 0

python qa/verify_phase0_benchmark.py --self-check
# SELF-CHECK PASS ...; exit 0

python backend/scripts/phase0_benchmark.py --self-check
# self-check: PASS; exit 0

python evaluation/validate_evaluation.py
# 검증 완료: 오류 0건; exit 0
```

`--dense-predictions` 경로는 sparse `--contradiction-output`과 함께 주지 않는다.
둘을 함께 주면 validator가 의도적으로 exit 1로 거부하므로, 위 명령이 v3 dense
계약의 재현 명령이다.

### 11.2 최종 판정과 남은 blocker

- **PASS:** rubric-context-v3는 26-case dev completion/recall/accuracy/FPR gate를 모두 통과했다. completion 26/26, recall 0.957, accuracy 0.923, sampled FPR 0, validator exit 0이다.
- **보고 전용:** dev Wilson 95% FPR upper 0.561은 NO_CONFLICT 3건 표본의 관측치이며 holdout hard gate로 적용하지 않는다.
- **PASS:** 사용자용 70-case review CSV는 exact 한국어 11열 헤더와 표시값 계약을 충족한다. `NOT_PROVIDED`는 `정보 없음`, six enum은 `직접 모순/시간 변화/조건 차이/관점 차이/판단 보류/모순 없음`, `source_verified=TRUE`는 `원문_확인=확인`으로 표시한다.
- **PASS:** review는 70행 unique, 목표 분포 `8/8/8/8/8/30`, input과 sentinel 역매핑 후 parity 0, source_ref 140개(고유 60, missing 0), NFKC 내부·기존 CT pair 중복 0, 한국어 판단 이유 70건, 검토 플래그 0, 사용자 결정 `승인` 70건이다.
- **PASS:** 이번 사용자 메시지의 `전체 승인`을 승인 출처로 기록하고, 사용자용 review와 분리된 `qa/contradiction-holdout-gold.csv`를 exact `case_id,classification,판단_이유` 70행으로 생성·동결했다. review의 표시 분류·판단 이유와 gold join은 0건 불일치다.
- **BLOCKED:** frozen gold는 준비됐지만 이번 작업에서는 holdout API 또는 `--gold-labels` validator를 실행하지 않았다. holdout gate와 Wilson <= 0.20 검증 전에는 production 승격도 선언하지 않는다.

## 12. 2026-08-11 사용자용 한국어 review 계약

`qa/contradiction-holdout-review.csv`는 사람이 읽고 승인하는 전용 산출물이다. exact 헤더는 다음과 같다.

```text
사례_ID,주장_A,주장_B,시간_또는_버전,환경_또는_조건,주제,분류,비모순_사유,판단_이유,원문_확인,사용자_결정
```

case ID와 기술 고유명사는 원문을 유지한다. 이번 사용자 메시지의 `전체 승인`에 따라
`사용자_결정`은 모든 행에서 `승인`이다. 아래 역매핑으로 별도 machine gold를
생성·동결했으며, user-facing review 자체는 backend harness 입력으로 사용하지 않는다.

| 사용자 표시 | machine enum |
| --- | --- |
| 직접 모순 | TRUE_CONFLICT |
| 시간 변화 | TIME_CHANGE |
| 조건 차이 | CONDITION_DIFF |
| 관점 차이 | VIEWPOINT_DIFF |
| 판단 보류 | POSSIBLE_CONFLICT |
| 모순 없음 | NO_CONFLICT |

검증 명령과 실제 결과:

```powershell
@' ... Korean header/sentinel reverse-map/parity/source/NFKC checks ... '@ | python -
# headers_exact=True
# rows=70 unique=70
# distribution=TRUE8/TIME8/CONDITION8/VIEWPOINT8/POSSIBLE8/NO30
# target_match=True
# parity_mismatch=0
# source_refs/unique/missing=140 60 0
# NFKC_internal_ordered/unordered_dup=0 0
# NFKC_CT_overlap_ordered/unordered=0 0
# korean_reasons=70; flags=0; user_decisions_nonblank=0
# errors=0; exit 0
```

### 12.1 현재 인수 상태

위 검증은 UTF-8을 명시한 PowerShell Python 표준 라이브러리 검사로
`qa/contradiction-holdout-review.csv`, `evaluation/contradiction-holdout-input.csv`,
`evaluation/contradiction-labels.csv`, NoteWiki 원문을 읽기 전용으로 대조한 결과다.
이번 사용자 메시지의 `전체 승인`을 기록한 뒤 review의 70행을 `승인`으로 채우고,
표시값을 여섯 machine enum으로 역매핑한 별도
`qa/contradiction-holdout-gold.csv`를 생성·동결했다. review/input/gold join은
PASS이며, holdout API·validator는 아직 실행하지 않아 production은 BLOCKED다.

### 12.2 승인·gold freeze 검증

```text
review_headers_exact=True
gold_headers_exact=True
rows_review/input/gold=70 70 70
ids_unique_and_ordered_join=True
approval_approved/blank/nonapproved=70 0 0
distribution=TRUE_CONFLICT:8 TIME_CHANGE:8 CONDITION_DIFF:8 VIEWPOINT_DIFF:8 POSSIBLE_CONFLICT:8 NO_CONFLICT:30 target_match=True
review_input_parity_mismatch=0
gold_label_reason_join_mismatch=0
source_refs/unique/missing=140 60 0
NFKC_review_internal_ordered/unordered_dup=0 0
NFKC_CT_overlap_ordered/unordered=0 0
errors=0; exit 0
```

raw bytes/UTF-8 `csv.DictReader`의 fieldnames는
`['case_id', 'classification', '판단_이유']`였고,
`backend.scripts.phase0_benchmark.load_gold_labels(Path('qa/contradiction-holdout-gold.csv'))`
는 70행을 반환했다(재현 exit 0).

이 검증에서 API key, API 응답, holdout 호출은 다루지 않았다.

`python qa/verify_phase0_benchmark.py --self-check`도 `SELF-CHECK PASS`, exit 0으로
완료했다. self-check는 validator 계약만 검사하며 holdout API를 호출하지 않는다.
