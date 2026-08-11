# ZeroWiki Phase 0 벤치마크 결과

작성일: 2026-08-11
최종 run: `phase0-20260811-gpt-5_6-sol-rubric-context-v3-r1`

## 1. 판정

**Phase 0 기술 검증 실행은 완료했다. `rubric-context-v3` dev gate는 PASS지만 제품·production 게이트는 조건부다.** 100문서 2단계 처리, 관계 후보 비교, token·시간 측정, Markdown 왕복, 1,000 change set replay, constitution schema 고정과 GPT v3 contradiction-only 검증을 완료했다. 로컬 `qwen3:1.7b` 및 이전 GPT variant는 기준 품질에 미달했고, v3도 holdout 미실행·원본 Claim mapping·production 결정이 남아 자동 판정 승격을 확정하지 않는다.

리더 결정은 다음과 같다.

- Phase 1의 인증·원본 보관·도서관·Import·변경 승인·검색 골격은 착수할 수 있다.
- v3 dev gate 자체는 통과했지만 holdout·원본 Claim mapping·production 결정 전까지 모순 자동 판정·자동 적용은 비활성화하고 사용자 검토 제안으로만 취급한다.
- `qwen3:1.7b`는 Phase 0 기준선일 뿐 production provider 확정이 아니다.

## 2. 입력 현실

| 항목 | 결과 |
| --- | ---: |
| inventory | 180 |
| 실제 존재 | 163 |
| 누락 | 17 |
| 전체 relation label | 84 |
| 평가 가능 relation | 73 (positive 66 / negative 7) |
| 누락 endpoint로 제외 | 11 |
| contradiction | 26 (conflict 23 / NO_CONFLICT 3) |

100문서 Ingest는 실제 존재하는 inventory 행의 앞 100건을 사용했다. 누락 17건을 성공으로 간주하거나 대체하지 않았다.

## 3. 100문서 2단계 Ingest

| 지표 | 결과 |
| --- | ---: |
| 완료 | 100 / 100 (100%) |
| 평균 input token | 2,069.46 / 문서 |
| 평균 output token | 179.98 / 문서 |
| 평균 scan | 9.567초 / 문서 |
| scan p95 | 14.000초 / 문서 |
| 평균 synthesis | 12.989초 / 문서 |
| synthesis p95 | 19.319초 / 문서 |
| 로컬 유료 API 비용 | $0 |

초기 run은 output 128-token 제한 때문에 JSON이 잘린 36건이 실패해 완료율 64%였다. root cause에서 제한을 256으로 올리고 string 길이 schema를 추가한 뒤 실패 행만 resume해 100%를 달성했다. 전력 비용과 장비 감가, production provider 비용은 포함하지 않는다.

입력에서는 secret처럼 보이는 값의 우변과 알려진 access-key/token 형식을 local LLM 호출 전 `[REDACTED]`로 치환한다. 문서 원문이나 LLM 응답 본문은 benchmark 결과에 저장하지 않는다.

## 4. 관계와 embedding — UD-17

| 모델 | 차원 | ROC-AUC | 100문서 warm 시간 | 문서당 |
| --- | ---: | ---: | ---: | ---: |
| multilingual MiniLM | 384 | 0.9632 | 3.418초 | 0.0342초 |
| multilingual MPNet | 768 | 0.9848 | 32.289초 | 0.3229초 |

**결정: Phase 1 기본은 `sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2`, 384차원.** MPNet의 AUC 이득은 0.0216이지만 차원은 2배이고 warm 처리는 약 9.4배 느렸다. 실제 검색 실패가 측정될 때만 MPNet을 재검토한다. negative 7건뿐이므로 관측 threshold는 production threshold로 고정하지 않는다.

상세 분모와 관측값은 `evaluation/results/phase0-embedding-comparison.json`에 있다.

## 5. 모순 품질

| 지표 | 결과 |
| --- | ---: |
| conflict recall | 100% (23 / 23) |
| sampled FPR | 100% (3 / 3) |
| 6-class accuracy | 19.2% (5 / 26) |
| 예측 분포 | `CONDITION_DIFF` 26 / 26 |

recall만 보면 통과처럼 보이지만 모든 쌍을 conflict로 출력한 결과다. QA verifier가 이 경우를 `PASS`하던 결함을 발견해 26건 전체 classification accuracy 80% gate를 추가했다. 수정된 verifier의 최종 결과는 `FAIL failures=1`이다.

`qwen3:4b`는 두 번 모두 2.5GB blob 다운로드 후 Windows/D-drive SHA 검증 단계가 장시간 정지해 비교 실행에 진입하지 못했다. 유료 외부 모델 호출은 하지 않았다.

## 6. Markdown 왕복, replay, constitution

| 항목 | 결과 |
| --- | --- |
| Markdown export → import | 100 / 100 byte hash 일치 |
| event replay | 1,000 change set × 25회 |
| replay median | 0.0000619초 |
| replay p95 | 0.0000697초 |
| Phase 2 snapshot trigger | 1초 |
| snapshot 필요 | 아니오 |
| constitution | `backend/contracts/library-constitution.schema.json`, top-level 12필드 |

왕복은 현재 exporter/parser가 없으므로 ZIP byte-preserving 기준이다. 실제 Markdown AST 변환기가 생기면 구조 동등성 검사를 추가한다. replay는 in-memory prototype이므로 DB·network 비용이 포함된 Phase 1 integration benchmark로 다시 확인한다.

## 7. Phase 0 의사결정

| UD | 결정 |
| --- | --- |
| UD-11 | Phase 1은 중요한 주장만 생성한다. 100문서에서 관측 문장 7,501개 대비 중요 주장 100개로 제한했다. 모순 자동 판정은 비활성화한다. |
| UD-17 | multilingual MiniLM, 384차원 |
| UD-25 | reference local 환경의 per-document budget을 scan p95 15초, synthesis/answer-generation p95 20초로 둔다. 실제 검색·citation을 포함한 answer endpoint는 Phase 1 integration에서 재검증한다. |
| UD-29 | 자동 identity key를 만들지 않는다. AI가 동일성 mapping을 제안하고 기존 변경 승인 흐름에서 사용자가 확정한다. |
| UD-13 | Phase 1 이벤트 재생 유지. Phase 2 snapshot 불필요. |
| UD-15 | API/ERD와 맞춘 12필드 JSON Schema v1 고정. |

## 8. 재현 명령

```powershell
python backend/scripts/phase0_benchmark.py --execute-phase0 --resume `
  --source-root C:\Users\halma\Desktop\NoteWiki `
  --model-root D:\ZeroWikiPhase0\models `
  --scan-model qwen3:1.7b --synthesis-model qwen3:1.7b `
  --document-count 100 --run-id phase0-20260811-qwen3-1_7b-r2 `
  --output-dir evaluation/results

python qa/verify_phase0_benchmark.py `
  --manifest evaluation/results/phase0-manifest.json `
  --generated-claims evaluation/results/phase0-generated-claims.csv `
  --benchmark-output evaluation/results/phase0-benchmark.csv `
  --contradiction-output evaluation/results/phase0-contradictions.csv `
  --expected-documents 100
```

구조·mapping·100문서 완료율은 통과하지만, 최종 명령은 모순 accuracy gate에서 의도대로 exit 1이다.

## 9. GPT-5.6 Sol 비교 실행 (model-specific)

실행일: 2026-08-11
run: `phase0-20260811-gpt-5_6-sol-r1`
산출물: `evaluation/results/phase0-gpt-5_6-sol-r1/`

기존 Phase 0 결과를 덮어쓰지 않고, 유료 호출을 모순 정답 26건에 한정해 각 케이스를 한 번씩 평가했다. 문서 100건의 2단계 ingest는 재실행하지 않았으므로 문서 처리 token/latency와 모순 분류 API의 token/latency는 동일 분모의 직접 비교가 아니다. `.env`의 `OPENAI_API_KEY`는 값 자체를 출력·저장하지 않고 메모리에서만 사용했으며, Responses API 요청은 `store=false`와 `text.format.type=json_schema`, `strict=true`를 사용했다. 모델·가격 기준은 [GPT-5.6 Sol 공식 모델 문서](https://developers.openai.com/api/docs/models/gpt-5.6-sol), 구조화 출력 형식은 [Structured model outputs 공식 문서](https://developers.openai.com/api/docs/guides/structured-outputs)를 따른다.

### 9-1. 결과 비교

| 지표 | `qwen3:1.7b` 기준선 | `gpt-5.6-sol` 새 실행 | 변화 |
| --- | ---: | ---: | ---: |
| 분류 정확도 (26) | 19.2% (5/26) | 61.5% (16/26) | +42.3%p |
| conflict recall (23) | 100.0% (23/23) | 78.3% (18/23) | -21.7%p |
| sampled FPR (3) | 100.0% (3/3) | 0.0% (0/3) | -100.0%p |
| 실제 API 호출 | 로컬 Ollama | 26회 | — |
| 실제 token | contradiction별 별도 기록 없음 | input 6,016 / output 2,553 (합계 8,569) | 신규 기록 |
| 실제 비용 | $0 (로컬) | $0.10667 | +$0.10667 |
| latency | synthesis p95 19.319초 (100문서 기준) | 평균 3.214초 / p95 6.531초 (26건) | 분모 상이 |

GPT-5.6 Sol은 기준선보다 정확도와 sampled FPR을 개선했지만, `CONDITION_DIFF`·`POSSIBLE_CONFLICT`를 보수적으로 `NO_CONFLICT`로 둔 5건 때문에 recall이 80% 아래로 내려갔다. 오분류 케이스는 `CT-009`, `CT-010`, `CT-011`, `CT-012`, `CT-013`, `CT-014`, `CT-015`, `CT-017`, `CT-021`, `CT-022`이다.

### 9-2. 예산과 검증

| 항목 | 사전 예상 | 실제 |
| --- | ---: | ---: |
| input token | 4,928 (stdlib 근사) | 6,016 |
| output token | 6,656 (256 × 26 budget) | 2,553 |
| 비용 | $0.22432 (input $5/M, output $30/M ceiling) | $0.10667 |
| latency | 20초/호출, 총 520초 (기준선 synthesis p95 budget) | 총 83.573초 |

기존 QA validator 실행 결과는 `exit 1`이며 구조·manifest·mapping·완료율은 통과했지만 품질 게이트가 실패했다.

```text
BENCHMARK_METRICS expected=26 completed=26 completion_rate=1.000
AC5_METRICS conflict=23 detected=18 recall=0.783 sampled_fpr=0.0 classification_accuracy=0.615
RESULT FAIL failures=2
```

따라서 이 실행도 모순 자동 판정·자동 적용을 production으로 승격시키지 않는다. `gpt-5.6-sol`은 이번 비교 실험의 후보 결과이며, Phase 1에서는 기존 결정대로 사용자 검토 제안으로만 취급한다.

### 9-3. GPT 재현 명령

```powershell
python -B backend/scripts/phase0_benchmark.py `
  --execute-openai-contradictions `
  --labels evaluation/contradiction-labels.csv `
  --openai-run-id phase0-20260811-gpt-5_6-sol-r1 `
  --openai-output-dir evaluation/results/phase0-gpt-5_6-sol-r1

python -B qa/verify_phase0_benchmark.py `
  --manifest evaluation/results/phase0-gpt-5_6-sol-r1/phase0-manifest.json `
  --generated-claims evaluation/results/phase0-gpt-5_6-sol-r1/phase0-generated-claims.csv `
  --benchmark-output evaluation/results/phase0-gpt-5_6-sol-r1/phase0-benchmark.csv `
  --contradiction-output evaluation/results/phase0-gpt-5_6-sol-r1/phase0-contradictions.csv `
  --expected-documents 26
```

### 9-4. 후속 prompt variant·holdout 재현 계약

기존 결과 디렉터리는 덮어쓰지 않는다. `rubric-v2`는 claim A/B에 label priority rubric을
추가하고 `rubric-context-v2`는 `evaluation/contradiction-contexts.csv`의
`time_or_version`, `environment_or_condition`, `subject`만 중립 JSON context로 함께
입력한다. `source_ref`는 추적용 메타데이터라 prompt에 넣지 않으며, 두 variant 모두
gold `classification`·`판단_이유`·`case_id`를 모델 입력에 포함하지 않는다.

```powershell
python -B backend/scripts/phase0_benchmark.py `
  --execute-openai-contradictions `
  --prompt-variant rubric-context-v2 `
  --labels evaluation/contradiction-labels.csv `
  --openai-contexts evaluation/contradiction-contexts.csv `
  --max-cost-usd 0.80 `
  --openai-run-id phase0-20260811-gpt-5_6-sol-rubric-context-v2 `
  --openai-output-dir evaluation/results/phase0-gpt-5_6-sol-rubric-context-v2
```

모든 케이스의 prediction·confidence·status·token·cost·latency는
`openai-predictions.csv`에 dense하게 기록한다. 완료된 `NO_CONFLICT`와 API `FAILED` 또는
`NOT_ATTEMPTED`는 각각 prediction 값과 status로 구분하고, 기존 QA 계약인
`phase0-contradictions.csv`에는 완료된 비`NO_CONFLICT`만 sparse하게 기록한다. 비용 cap은
모든 케이스의 잠재 retry ceiling을 upfront 합산하지 않고, 각 first attempt 직전 누적
known actual/unknown reservation과 해당 호출 예약을 비교한다. first attempt 실패 뒤에는
그 attempt의 known actual 또는 unknown reservation을 차감한 남은 cap에 retry reservation이
들어갈 때만 재시도하며, 그렇지 않으면 `retry_budget_reservation`으로 해당 케이스를
`FAILED`로 남긴다. `rubric-v2`/`rubric-context-v2`는 최대 2 attempts, 일반 transient retry는
256 output tokens, `incomplete_details.reason=max_output_tokens` retry는 512 output tokens로
고정한다. HTTP 인증·일반 요청 4xx는 재시도하지 않으며, incomplete reason은
`incomplete_<safe_code>`로만 보존한다. usage가 없는 실패는 비용 0으로 완료 처리하지 않고
`unknown_failed_attempts`와 unknown reservation을 metrics에 기록한다.

holdout 입력은 `evaluation/contradiction-holdout-input.csv`이며 stable `HT-NNN`, 두 claim,
세 context 열, 양쪽 `source_ref`만 포함한다. gold·`판단_이유`는 저장하지 않고 기존 `CT-NNN`
쌍과 중복하지 않는다. API 실행 전 `python -B backend/scripts/phase0_benchmark.py
--self-check`가 context/holdout CSV schema와 prompt gold 누출을 검사한다.

`rubric-context-v3`는 기존 v2 결과를 보존한 채 `evaluation/contradiction-contexts-v3.csv`를
사용한다. 이 CSV는 claim A/B 각각의 `time_or_version`·`environment_or_condition`과
shared `subject`를 제공하며, prompt JSON에도 이 구조를 그대로 유지한다. 명시·매핑된
TIME/CONDITION 차이는 `NO_CONFLICT`보다 먼저 적용하고, source ref·gold·reason·case ID는
prompt에서 제외한다. `CT-011`은 A=`로컬 개발 환경`, B=`운영 데이터베이스`로 분리해
조건 축이 shared context에 묻히지 않도록 했다.

### 9-5. prompt variant 실행 결과와 stop condition

네 후속 model-specific 디렉터리는 기존 `phase0-gpt-5_6-sol-r1` 결과를 덮어쓰지 않고 각각 보존했다.
`rubric-v2-r1`은 CT-008·CT-022의 `FAILED`를 확인하는 진단 실행이므로 품질 판정 분모에서
제외한다. retry 계약을 반영한 `rubric-v2-r2`와 context 입력 실행은 모두 dense 26행을
생성했다.

| 실행 | 산출물 디렉터리 | 완료/호출 attempts | 비용 | accuracy | conflict recall | sampled FPR | macro-F1 | 판정 |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | --- |
| `rubric-v2-r1` (진단) | `evaluation/results/phase0-gpt-5_6-sol-rubric-v2-r1/` | 24/26 (2 `FAILED`) | $0.117765 | — | — | — | — | 품질 판정 제외 |
| `rubric-v2-r2` | `evaluation/results/phase0-gpt-5_6-sol-rubric-v2-r2/` | 26/26 (26 attempts) | $0.135560 | 0.615 (16/26) | 0.739 (17/23) | 0.000 (0/3) | 0.553 | FAIL |
| `rubric-context-v2-r1` | `evaluation/results/phase0-gpt-5_6-sol-rubric-context-v2-r1/` | 26/26 (27 attempts, retry 1 성공) | $0.154055 | 0.692 (18/26) | 0.783 (18/23) | 0.000 (0/3) | 0.683 | FAIL |
| `rubric-context-v3-r1` | `evaluation/results/phase0-gpt-5_6-sol-rubric-context-v3-r1/` | 26/26 (26 attempts, retry 없음) | $0.149060 | 0.923 (24/26) | 0.957 (22/23) | 0.000 (0/3) | 0.902 | PASS (dev) |

새 네 실행의 합계 비용은 `$0.55644`이며 `$0.80` 전체 cap 기준 잔여 예산은 `$0.24356`다.
`rubric-context-v2-r1`에서 TIME 계열은 5/5로 개선됐지만 CONDITION 계열은 0/5(모두
`NO_CONFLICT`)였고, 전체 정확도 21/26 및 conflict recall 19/23 stop condition을 충족하지
못했다(accuracy gate 기준 21/26, recall gate 기준 19/23). 후속 claim-specific
`rubric-context-v3-r1`은 26/26 완료, API 26회·retry 0회, latency total `64.443268s`/p95
`5.344194s`로 accuracy `24/26=0.9230769`, recall `22/23=0.9565217`, sampled FPR
`0/3=0`, macro-F1 `0.902`를 기록해 dev gate를 통과했다. 오분류는 `CT-017`
(`VIEWPOINT_DIFF` → `POSSIBLE_CONFLICT`)과 `CT-021` (`POSSIBLE_CONFLICT` →
`NO_CONFLICT`)이다. 누적 결과는 비교 실험으로만 보존하며, 70건 holdout API 호출은 0회이고
production 자동 판정 승격은 확정하지 않는다.

재현·QA 명령은 다음과 같다(유료 실행 명령은 이미 생성된 디렉터리를 재현할 때만 사용한다).

```powershell
python -B backend/scripts/phase0_benchmark.py `
  --execute-openai-contradictions --prompt-variant rubric-v2 `
  --labels evaluation/contradiction-labels.csv --max-cost-usd 0.40 `
  --openai-run-id phase0-20260811-gpt-5_6-sol-rubric-v2-r1 `
  --openai-output-dir evaluation/results/phase0-gpt-5_6-sol-rubric-v2-r1

python -B backend/scripts/phase0_benchmark.py `
  --execute-openai-contradictions --prompt-variant rubric-v2 `
  --labels evaluation/contradiction-labels.csv --max-cost-usd 0.682235 `
  --openai-run-id phase0-20260811-gpt-5_6-sol-rubric-v2-r2 `
  --openai-output-dir evaluation/results/phase0-gpt-5_6-sol-rubric-v2-r2

python -B backend/scripts/phase0_benchmark.py `
  --execute-openai-contradictions --prompt-variant rubric-context-v2 `
  --labels evaluation/contradiction-labels.csv `
  --openai-contexts evaluation/contradiction-contexts.csv --max-cost-usd 0.546675 `
  --openai-run-id phase0-20260811-gpt-5_6-sol-rubric-context-v2-r1 `
  --openai-output-dir evaluation/results/phase0-gpt-5_6-sol-rubric-context-v2-r1

python -B backend/scripts/phase0_benchmark.py --self-check
python -B evaluation/validate_evaluation.py

python -B qa/verify_phase0_benchmark.py `
  --manifest evaluation/results/phase0-gpt-5_6-sol-rubric-v2-r2/phase0-manifest.json `
  --generated-claims evaluation/results/phase0-gpt-5_6-sol-rubric-v2-r2/phase0-generated-claims.csv `
  --benchmark-output evaluation/results/phase0-gpt-5_6-sol-rubric-v2-r2/phase0-benchmark.csv `
  --dense-predictions evaluation/results/phase0-gpt-5_6-sol-rubric-v2-r2/openai-predictions.csv `
  --labels evaluation/contradiction-labels.csv `
  --expected-documents 26

python -B qa/verify_phase0_benchmark.py `
  --manifest evaluation/results/phase0-gpt-5_6-sol-rubric-context-v2-r1/phase0-manifest.json `
  --generated-claims evaluation/results/phase0-gpt-5_6-sol-rubric-context-v2-r1/phase0-generated-claims.csv `
  --benchmark-output evaluation/results/phase0-gpt-5_6-sol-rubric-context-v2-r1/phase0-benchmark.csv `
  --dense-predictions evaluation/results/phase0-gpt-5_6-sol-rubric-context-v2-r1/openai-predictions.csv `
  --labels evaluation/contradiction-labels.csv `
  --expected-documents 26
```

### 9-6. claim-specific context v3 실행 결과

`backend/scripts/phase0_benchmark.py --prompt-variant rubric-context-v3`와
`load_contexts_v3`를 추가했으며, v2 variant·기존 세 결과 디렉터리·dense/sparse schema는
변경하지 않았다. v3의 분류 목표는 양립 여부만이 아니라 차이를 가장 잘 설명하는 축이며,
명시된 claim-specific time/condition 차이, viewpoint, 근거 부족, 같은 context의 직접
불일치 순서로 판단하도록 prompt에 기록했다. self-check는 v2 회귀와 CT-011형 A/B JSON
분리 및 prompt 금지 필드(`source_ref`, gold/reason/case_id)를 검사한다.

```powershell
python -B backend/scripts/phase0_benchmark.py --self-check
python -B backend/scripts/phase0_benchmark.py `
  --execute-openai-contradictions --prompt-variant rubric-context-v3 `
  --labels evaluation/contradiction-labels.csv `
  --openai-contexts evaluation/contradiction-contexts-v3.csv --max-cost-usd 0.39262 `
  --openai-run-id phase0-20260811-gpt-5_6-sol-rubric-context-v3-r1 `
  --openai-output-dir evaluation/results/phase0-gpt-5_6-sol-rubric-context-v3-r1

python -B qa/verify_phase0_benchmark.py `
  --manifest evaluation/results/phase0-gpt-5_6-sol-rubric-context-v3-r1/phase0-manifest.json `
  --generated-claims evaluation/results/phase0-gpt-5_6-sol-rubric-context-v3-r1/phase0-generated-claims.csv `
  --benchmark-output evaluation/results/phase0-gpt-5_6-sol-rubric-context-v3-r1/phase0-benchmark.csv `
  --dense-predictions evaluation/results/phase0-gpt-5_6-sol-rubric-context-v3-r1/openai-predictions.csv `
  --labels evaluation/contradiction-labels.csv `
  --expected-documents 26
```

실제 v3 실행은 `26/26 completed`, `26 calls/no retries`, 비용 `$0.14906`이었다.
`phase0-metrics.json`의 actual token은 input/output `19,204/1,768`, latency total/p95는
`64.443268s/5.344194s`이며, QA validator는 exit 0·`RESULT PASS failures=0`을 반환했다.
오분류는 `CT-017`(gold `VIEWPOINT_DIFF`, prediction `POSSIBLE_CONFLICT`)과
`CT-021`(gold `POSSIBLE_CONFLICT`, prediction `NO_CONFLICT`) 두 건뿐이다. 이 실행은
개발 비교 gate PASS를 의미하지만, holdout은 실행하지 않았고 production 승격을 확정하지 않는다.
