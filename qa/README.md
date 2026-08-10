# ZeroWiki QA

**기준:** `docs/검증-QA-계약-확정-체크리스트.md` 3절과
`docs/ZeroWiki-요구사항-정의서.md` 11절
**상태:** 제품 테스트 시나리오 40개 정의, 제품 구현 대상 테스트는 전부 미실행

## 구조

```text
qa/
├── README.md
├── test_scenarios.md
├── verify_phase0_evaluation.py
├── verify_phase0_benchmark.py
└── phase0-evaluation-acceptance-report.md
```

- `test_scenarios.md`: A-1~A-18, U-1~U-6, S-1~S-16의 Precondition·단계·예상 결과.
- `verify_phase0_evaluation.py`: `evaluation/*.csv`의 스키마·참조·유일성·enum·negative
  규칙·중복·분모를 검사하는 표준 라이브러리 전용 검증기. LF와 CRLF checkout을
  모두 허용하지만 mixed/bare CR과 BOM은 거부한다.
- `verify_phase0_benchmark.py`: backend의 manifest schema v1 JSON과
  `phase0-generated-claims.csv`를 먼저 조인한 뒤, 문서별 benchmark CSV와
  contradiction output을 독립 검증한다. manifest의 case/gold/generated ID 해시와
  라벨 대응, 100문서 완료율, AC-5 recall·분류 정확도를 검사하며 산출물 경로는
  인자로 받는다.
- `phase0-evaluation-acceptance-report.md`: 평가 세트와 AC-3·AC-5 채점 계약의 독립
  QA 인수 결과.

평가 CSV는 `evaluation/**`에 있으며 backend 소유다. QA는 해당 파일을 읽고 검증하며
직접 수정하지 않는다.

## 현재 검증 상태

| 범위 | 상태 |
| --- | --- |
| 제품 자동화 시나리오 A-1~A-18 | 미실행 |
| 사용자 평가 시나리오 U-1~U-6 | 미실행 |
| 보안 시나리오 S-1~S-16 | 미실행 |
| Phase 0 CSV 구조 인수 검사 | 실행 완료 — 구조 PASS |
| Phase 0 offline 채점 계약 | 재인수 완료 |
| Phase 0 benchmark 산출물 구조 | `100/100` 완료·mapping join PASS |
| Phase 0 contradiction 품질 | **FAIL** — recall 1.000이지만 accuracy 0.192, sampled FPR 1.000 |
| Ingest-level AC-5 product gate | **FAIL** — 정확도 gate; 개선된 모델 재실행 필요 |

CSV 구조와 offline 채점 계약 PASS는 제품 기능이나 AC-3·Ingest-level AC-5의 통과를
뜻하지 않는다. AC-3은 검증 사용자 5~10명의 평가가 필요한 사용자 인수 기준이고,
Ingest-level AC-5는 gold `case_id`와 생성 `claim_id`를 잇는 mapping manifest가
필요하다.

## 재현

PowerShell에서 저장소 루트를 현재 디렉터리로 두고 실행한다.

```powershell
$env:PYTHONIOENCODING='utf-8'
python qa/verify_phase0_evaluation.py
```

현재 구조가 유효하면 종료 코드 0과 다음 요약을 출력한다.

```text
COUNTS pages=180 page_types={'concept': 127, 'flow': 16, 'source': 37} relations=84 positive=76 negative=8 contradictions=26 conflict=23 no_conflict=3
RESULT PASS failures=0
```

backend 산출물이 준비되면 다음 명령으로 동일 manifest/output 묶음을 재검증한다.

```powershell
$env:PYTHONIOENCODING='utf-8'
python qa/verify_phase0_benchmark.py `
  --manifest evaluation/results/phase0-manifest.json `
  --generated-claims evaluation/results/phase0-generated-claims.csv `
  --benchmark-output evaluation/results/phase0-benchmark.csv `
  --expected-documents 100 `
  --contradiction-output evaluation/results/phase0-contradictions.csv
```

구조·매핑이 PASS여도 이 명령의 `RESULT FAIL`은 품질 gate 실패다. 현재 실제 실행은
`BENCHMARK_METRICS expected=100 completed=100 completion_rate=1.000`,
`AC5_METRICS ... recall=1.000 sampled_fpr=1.0 classification_accuracy=0.192`,
`RESULT FAIL failures=1`을 출력한다. `--self-check`는 LF/CRLF 회귀, manifest hash,
gold/generated join과 recall-only false PASS 방지를 점검한다.

## 참고

- 제품·계약 정본: `docs/ZeroWiki-MVP-서비스-기획서.md` 3절,
  `docs/ZeroWiki-요구사항-정의서.md` 11절·12절
- 측정 기준: `docs/검증-QA-계약-확정-체크리스트.md` 3절
- 상세 시나리오: `qa/test_scenarios.md`
- Phase 0 인수 결과: `qa/phase0-evaluation-acceptance-report.md`
