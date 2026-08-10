# Phase 0 평가 세트 독립 QA 재인수 보고서

검증일: 2026-08-11 (초기 재인수: 2026-08-10)
대상: `evaluation/pages-inventory.csv`, `evaluation/relation-labels.csv`,
`evaluation/contradiction-labels.csv`, `evaluation/README.md`
판정: **CSV 구조·mapping manifest 조인 PASS / contradiction 품질 gate FAIL**

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
