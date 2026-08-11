# qa 현재 상태

> **덮어쓰기 스냅샷.** 매 턴 종료 전에 이 파일 전체를 최신 상태로 갱신한다.
> 시간순 기록은 `WORKLOG.md`에 append 한다.

마지막 갱신: 2026-08-11 +09:00 (사용자 전체 승인·machine gold freeze)

## 현재 단계

**70-case 한국어 사용자용 review 승인·machine gold freeze 완료 — holdout API 미실행 / production BLOCKED**

- `qa/contradiction-holdout-review.csv`의 exact 헤더는 `사례_ID,주장_A,주장_B,시간_또는_버전,환경_또는_조건,주제,분류,비모순_사유,판단_이유,원문_확인,사용자_결정`이다.
- `NOT_PROVIDED`는 `정보 없음`, 분류 enum은 `직접 모순/시간 변화/조건 차이/관점 차이/판단 보류/모순 없음`, `source_verified=TRUE`는 `원문_확인=확인`으로 표시한다. case ID와 기술 고유명사는 유지하고 `사용자_결정`은 70행 모두 `승인`이다.
- 이 CSV는 사용자용 review이며 backend harness 입력이 아니다. 이번 사용자 메시지의 `전체 승인`을 근거로 70행 `사용자_결정`을 모두 `승인`으로 기록하고, 표시 분류를 enum으로 역매핑한 별도 machine gold `qa/contradiction-holdout-gold.csv`를 생성·동결했다.
- 독립 검증 결과 70/70 unique, 목표 분포 `TRUE_CONFLICT=8`, `TIME_CHANGE=8`, `CONDITION_DIFF=8`, `VIEWPOINT_DIFF=8`, `POSSIBLE_CONFLICT=8`, `NO_CONFLICT=30`, sentinel 역매핑 후 review/input parity 0, review/gold label·reason join 0, source_ref 140개(고유 60, missing 0), NFKC 내부·기존 CT pair 중복 0, 사용자 결정 승인 70건이다. raw UTF-8 header와 `load_gold_labels()`도 exact 3열·70행으로 통과했다.

## 현재 게이트

| 게이트 | 판정 | 근거 |
| --- | --- | --- |
| rubric-context-v3 26-case dev 품질 | **PASS** | completion 26/26, recall 0.957, accuracy 0.923, sampled FPR 0, validator exit 0 |
| dev Wilson 95% FPR upper | 보고 전용 | 0.561; NO_CONFLICT 3건 표본이라 holdout hard gate로 해석하지 않음 |
| 한국어 review 헤더·표시값·구조 | **PASS** | exact 11열, 70/70 unique, sentinel 역매핑 parity 0, source 140/60/missing 0 |
| 한국어 review 목표 분포 | **PASS** | conflict 5종 각 8건, `NO_CONFLICT=30` |
| 사용자 승인·machine gold freeze | **PASS** | 사용자 `전체 승인` 기록, raw UTF-8 `case_id,classification,판단_이유`, `load_gold_labels()` 70행 및 join 검증 exit 0 |
| holdout API·validator | **BLOCKED** | frozen gold는 준비됐으나 이번 작업에서 API·validator를 호출하지 않음 |
| production 승격 | **BLOCKED** | holdout gate 및 Wilson <= 0.20 미실행 |

## 이전 결과 보존

| 실행 | recall | sampled FPR | 6-class accuracy | 판정 |
| --- | ---: | ---: | ---: | --- |
| qwen3:1.7b baseline | 1.000 | 1.000 | 0.192 | 이전 FAIL |
| GPT sparse | 0.783 | 0.000 | 0.615 | 이전 FAIL |
| rubric-v2 r2 dense | 0.739 | 0.000 | 0.615 | 이전 FAIL |
| rubric-context-v2 r1 dense | 0.783 | 0.000 | 0.692 | 이전 FAIL |
| rubric-context-v3 r1 dense | 0.957 | 0.000 | 0.923 | 현재 dev PASS |

## 진행 중 작업

- 승인된 한국어 review와 frozen machine gold를 보존한다. 이번 작업에서는 holdout API를 호출하지 않았다.
- 기존 qwen/sparse/v2 FAIL은 과거 결과로 보존하며 현재 v3 dev PASS와 혼동하지 않는다.

## 다음 작업

1. frozen gold를 사용해 holdout API와 `--gold-labels` validator를 실행한다(별도 실행 승인 필요).
2. holdout completion/recall/accuracy/FPR/Wilson hard gate 결과를 독립 검증한다.
3. holdout gate 통과 뒤에만 production 승격을 검토한다.

## 차단 요인

- **HIGH:** holdout API/validator가 아직 실행되지 않아 recall/FPR/Wilson hard gate가 측정되지 않았다.
- **MEDIUM:** dev Wilson upper 0.561은 3개 NO_CONFLICT 표본의 관측치일 뿐이며 holdout 전에는 report-only다.

## 소유 산출물

| 경로 | 상태 |
| --- | --- |
| `qa/verify_phase0_evaluation.py` | LF/CRLF checkout 허용, mixed/bare CR·BOM·CSV 구조 검사, PASS |
| `qa/verify_phase0_benchmark.py` | sparse 호환 + dense predictions validator, accuracy/FPR/Wilson gate, self-check |
| `qa/contradiction-holdout-gold.csv` | 사용자 승인 후 생성·동결한 machine gold, exact `case_id,classification,판단_이유` 70행 |
| `qa/phase0-evaluation-acceptance-report.md` | 실제 산출물 심각도 findings·명령·판정·잔여 위험 기록 |
| `qa/README.md` | validator 사용법과 현재 FAIL/BLOCKED 상태 기록 |
| `qa/test_scenarios.md` | A-1~A-18, U-1~U-6, S-1~S-16 정의 |
| `docs/검증-QA-계약-확정-체크리스트.md` | A-4 sparse/dense 인수 명령·recall/accuracy/FPR/Wilson gate 계약 |
| `.claude/team/qa/STATE.md` | 현재 스냅샷 |
| `.claude/team/qa/WORKLOG.md` | append-only 작업 이력 |

## 재현 명령

```powershell
$env:PYTHONIOENCODING='utf-8'
python qa/verify_phase0_evaluation.py
python evaluation/validate_evaluation.py
python qa/verify_phase0_benchmark.py --self-check
python qa/verify_phase0_benchmark.py `
  --manifest evaluation/results/phase0-manifest.json `
  --generated-claims evaluation/results/phase0-generated-claims.csv `
  --benchmark-output evaluation/results/phase0-benchmark.csv `
  --expected-documents 100 `
  --contradiction-output evaluation/results/phase0-contradictions.csv

python qa/verify_phase0_benchmark.py `
  --manifest evaluation/results/phase0-gpt-5_6-sol-r1/phase0-manifest.json `
  --generated-claims evaluation/results/phase0-gpt-5_6-sol-r1/phase0-generated-claims.csv `
  --benchmark-output evaluation/results/phase0-gpt-5_6-sol-r1/phase0-benchmark.csv `
  --expected-documents 26 `
  --contradiction-output evaluation/results/phase0-gpt-5_6-sol-r1/phase0-contradictions.csv
```

위 두 benchmark 명령은 **이전 결과 재현용**이다. qwen/GPT sparse 이전 실행은 각각
종료 코드 1이며, GPT의 이전 지표는 `recall=0.783 sampled_fpr=0.0
classification_accuracy=0.615`이다. 이 과거 `RESULT FAIL`을 최신 v3 dev 판정으로
해석하지 않는다.

최신 v3 dev 재현 명령은 다음과 같고 종료 코드 0이다.

```powershell
python qa/verify_phase0_benchmark.py `
  --manifest evaluation/results/phase0-gpt-5_6-sol-rubric-context-v3-r1/phase0-manifest.json `
  --generated-claims evaluation/results/phase0-gpt-5_6-sol-rubric-context-v3-r1/phase0-generated-claims.csv `
  --benchmark-output evaluation/results/phase0-gpt-5_6-sol-rubric-context-v3-r1/phase0-benchmark.csv `
  --dense-predictions evaluation/results/phase0-gpt-5_6-sol-rubric-context-v3-r1/openai-predictions.csv `
  --labels evaluation/contradiction-labels.csv `
  --expected-documents 26
```

`completion=1.000 recall=0.957 sampled_fpr=0.000 accuracy=0.923
macro_f1=0.902`, `RESULT PASS`, exit 0이다. Dense 경로에는 sparse
`--contradiction-output`을 함께 주지 않는다.

Dense 계약 self-check는 exit 0이며 synthetic 25-case 데이터에서
`completion=1.000 recall=1.000 sampled_fpr=0.000 wilson95_fpr_upper=0.161`
및 `accuracy=1.000 macro_f1=1.000`을 확인한다. 실제 holdout labels/API 호출은
아직 수행하지 않았다.

## 부록: UD 분류 판정 방법

UD 정본은 `docs/ZeroWiki-요구사항-정의서.md` 12절이다. 목록·분류·건수를 이
파일에 복제하지 않는다. 필요할 때 정본의 12.1절과 12.2절 표를 직접 읽고 행 단위로
판정한다.

## 2026-08-11 후속 holdout QA 최신 스냅샷

- QA artifact `qa/contradiction-holdout-review.csv`를 생성했다. 입력 70건과 case_id 70개가 1:1로 일치하고, `user_decision`은 전부 빈칸이다.
- NoteWiki 원문 root `C:/Users/PC/Desktop/ZeroWiki/NoteWiki`의 source reference 140개(고유 파일 57개)를 실제로 읽었다. 누락 파일은 없다. HT-065는 eager-loading 근거가 원문에서 충분하지 않아 `판단_이유`에 `REVIEW_REQUIRED`를 기록했다.
- 독립 라벨 분포: `TRUE_CONFLICT=34`, `TIME_CHANGE=4`, `CONDITION_DIFF=2`, `VIEWPOINT_DIFF=0`, `POSSIBLE_CONFLICT=1`, `NO_CONFLICT=29`. 목표 분포를 맞추기 위해 애매한 라벨을 강제하지 않았다.
- `qa/verify_phase0_benchmark.py` dense profile은 dev(`--labels`)에서 Wilson upper를 출력만 하고, holdout(`--holdout-labels/--gold-labels`)에서만 Wilson <= 0.20을 hard gate로 적용한다. self-check는 두 profile을 모두 검증하며 exit 0이다.
- 이 후속 작업에서는 API 호출과 gold 파일 생성을 하지 않았다. HT-065 adjudication 및 holdout gold 확정은 후속 결정 사항이다.

## 2026-08-11 rubric dense 최종 독립 QA 스냅샷

- 기존 validator로 두 dev dense variant를 재검증했다. `rubric-v2 r2`: 26/26, recall 0.739, sampled FPR 0, accuracy 0.615, detected subtype 0.765, joint subtype 0.565, macro-F1 0.553, exit 1. `rubric-context-v2 r1`: 26/26, recall 0.783, sampled FPR 0, accuracy 0.692, detected subtype 0.833, joint subtype 0.652, macro-F1 0.683, exit 1.
- 두 variant 모두 recall/accuracy dev gate FAIL이다. Wilson upper 0.561은 3개 NO_CONFLICT 표본의 관측치로만 기록했다. 계획대로 holdout API 실행과 gold freeze를 진행하지 않았다.
- holdout review 분포는 `TRUE_CONFLICT=34`, `TIME_CHANGE=4`, `CONDITION_DIFF=2`, `VIEWPOINT_DIFF=0`, `POSSIBLE_CONFLICT=1`, `NO_CONFLICT=29`이며 목표 분포와 다르다. HT-065 `REVIEW_REQUIRED` 때문에 사용자 승인 전 초안은 승인/gold freeze 불가다.
- 보고서와 QA 체크리스트에 variant별 confusion matrix, 실제 명령/exit, metric 및 holdout 차단 판정을 append했다. 기존 qwen/GPT 및 model-specific 결과 기록은 보존했다.

## 2026-08-11 rubric-context-v3 최종 QA 스냅샷

- API 재호출 없이 `evaluation/results/phase0-gpt-5_6-sol-rubric-context-v3-r1/`의
  dense prediction을 기존 `--labels` dev 계약으로 독립 검증했다. 26/26
  `COMPLETED`, conflict recall `22/23=0.957`, sampled FPR `0/3=0.000`,
  6-class accuracy `24/26=0.923`, detected-conflict subtype accuracy `21/22=0.955`,
  joint subtype accuracy `21/23=0.913`, macro-F1 `0.902`, Wilson upper `0.561`이다.
- 오분류는 `CT-017` (`VIEWPOINT_DIFF` → `POSSIBLE_CONFLICT`)와 `CT-021`
  (`POSSIBLE_CONFLICT` → `NO_CONFLICT`)이다. completion/recall/accuracy/FPR
  dev gate는 PASS이며, validator exit는 0이다.
- dev Wilson upper는 NO_CONFLICT 3건의 작은 표본 때문에 report-only로 유지한다.
  `qa/verify_phase0_benchmark.py --self-check`, backend self-check,
  `evaluation/validate_evaluation.py`도 각각 exit 0이다.
- holdout API 실행·gold freeze는 아직 하지 않았다. holdout review 목표 분포가
  실제 분포와 다르고 HT-065 `REVIEW_REQUIRED`가 남아 있어 사용자 승인 전
  holdout 실행과 production 승격은 BLOCKED다.
