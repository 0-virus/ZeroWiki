# qa 현재 상태

> **덮어쓰기 스냅샷.** 매 턴 종료 전에 이 파일 전체를 최신 상태로 갱신한다.
> 시간순 기록은 `WORKLOG.md`에 append 한다.

마지막 갱신: 2026-08-11 04:32 +09:00

## 현재 단계

**Phase 0 기술 검증 실행 완료 — 구조·실행 지표 PASS, product 품질 gate FAIL / Phase 1 품질 진입 BLOCKED**

- `qa/verify_phase0_evaluation.py`와 `evaluation/validate_evaluation.py`는 평가 CSV
  구조·계약에서 종료 코드 0이다. Windows checkout의 CRLF는 허용하고 mixed/bare CR,
  BOM, CSV 무결성은 계속 거부한다.
- backend 실제 산출물(`evaluation/results`)은 schema v1 manifest 26건, generated
  claim mapping, benchmark 100행, contradiction output 26행, metrics JSON이다.
- 독립 validator는 manifest hash/gold 대응/generated pair join, 100문서 완료율,
  AC-5 recall과 분류 정확도를 검사한다. 실제 구조·mapping·100/100 완료는 PASS이나
  contradiction `recall=1.000`, `sampled_fpr=1.000`, `classification_accuracy=0.192`
  로 `RESULT FAIL`, exit 1이다. recall-only false PASS를 차단했다.
- metrics의 Markdown roundtrip 100개 무손실과 event replay 1000 change sets/25회
  p95 약 0.000070초는 PASS했다.
- `backend/contracts/library-constitution.schema.json` 구조는 top-level 12 property,
  required 12개, `additionalProperties=false`로 독립 확인했다.

## 이번 검증 결과

| 게이트 | 판정 | 근거 |
| --- | --- | --- |
| CSV 구조·LF/CRLF·BOM·열 무결성 | PASS | `verify_phase0_evaluation.py` exit 0; LF/CRLF 회귀 self-check PASS |
| manifest schema/hash·gold/generated mapping | PASS | schema v1, 26 case 전체 대응, pair join PASS |
| AC-1 실행 범위 | PASS | benchmark 100/100 `COMPLETED`, completion 1.000 |
| AC-5/A-4 품질 | **FAIL** | conflict 23/23 recall, 그러나 accuracy 0.192/FPR 1.000 |
| Markdown roundtrip | PASS | 100 files, mismatches 0 |
| event replay | PASS | 1000 change sets, 25 iterations, p95 < 1s |
| 경로 보안 | PASS | 리더가 `drive`·`root` 거부와 `/absolute.md` 회귀 self-check를 추가해 PASS |
| Phase 0 전체/Phase 1 품질 진입 | **BLOCKED** | 모순 품질 HIGH 해소와 재실행 필요 |

## 진행 중 작업

- 없음. 독립 validator·실제 산출물 검증 및 QA 문서 기록 완료.

## 다음 작업

1. contradiction 분류 모델/프롬프트를 개선해 `NO_CONFLICT` 오탐과 5종 분류 붕괴를
   해소하고 동일한 validator로 새 output을 재검증한다.
2. 원본 inventory 누락 17건을 확인하고, 전체 dataset roundtrip 및 benchmark 필요 시
   재실행한다. 별도 100문서 실행은 이미 완료했으므로 결과를 혼용하지 않는다.
3. API citation/constitution 저장·조회, S-1~S-16, U-1은 제품 구현 후 별도 실행한다.

## 차단 요인

- **HIGH:** contradiction accuracy 0.192, sampled FPR 1.000. recall 1.000만으로
  AC-5 품질 통과를 선언할 수 없다.
- **MEDIUM:** NoteWiki inventory 180행 중 17개 원본이 없어 전체 inventory 완료 판정은
  불가하다. 존재하는 163개 roundtrip은 PASS다.
- **MEDIUM:** local Ollama metrics의 monetary cost는 0이며 실제 provider 비용은
  측정되지 않았다.
- AC-3 U-1 사용자 유용성, API 통합 citation, 보안 시나리오 전체는 미실행이다.

## 소유 산출물

| 경로 | 상태 |
| --- | --- |
| `qa/verify_phase0_evaluation.py` | LF/CRLF checkout 허용, mixed/bare CR·BOM·CSV 구조 검사, PASS |
| `qa/verify_phase0_benchmark.py` | backend manifest v1/output 독립 validator, accuracy gate, self-check |
| `qa/phase0-evaluation-acceptance-report.md` | 실제 산출물 심각도 findings·명령·판정·잔여 위험 기록 |
| `qa/README.md` | validator 사용법과 현재 FAIL/BLOCKED 상태 기록 |
| `qa/test_scenarios.md` | A-1~A-18, U-1~U-6, S-1~S-16 정의 |
| `docs/검증-QA-계약-확정-체크리스트.md` | A-4 독립 인수 명령·recall/accuracy/FPR gate 주의 기록 |
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
```

마지막 명령은 현재 의도된 product gate 실패로 종료 코드 1을 반환한다. `RESULT FAIL`
없는 종료 코드 0을 품질 통과로 해석하지 않는다.

## 부록: UD 분류 판정 방법

UD 정본은 `docs/ZeroWiki-요구사항-정의서.md` 12절이다. 목록·분류·건수를 이
파일에 복제하지 않는다. 필요할 때 정본의 12.1절과 12.2절 표를 직접 읽고 행 단위로
판정한다.
