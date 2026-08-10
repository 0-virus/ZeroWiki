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
└── phase0-evaluation-acceptance-report.md
```

- `test_scenarios.md`: A-1~A-18, U-1~U-6, S-1~S-16의 Precondition·단계·예상 결과.
- `verify_phase0_evaluation.py`: `evaluation/*.csv`의 스키마·참조·유일성·enum·negative
  규칙·중복·분모를 검사하는 표준 라이브러리 전용 검증기.
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
| Ingest-level AC-5 | mapping manifest 부재로 차단 |

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

## 참고

- 제품·계약 정본: `docs/ZeroWiki-MVP-서비스-기획서.md` 3절,
  `docs/ZeroWiki-요구사항-정의서.md` 11절·12절
- 측정 기준: `docs/검증-QA-계약-확정-체크리스트.md` 3절
- 상세 시나리오: `qa/test_scenarios.md`
- Phase 0 인수 결과: `qa/phase0-evaluation-acceptance-report.md`
