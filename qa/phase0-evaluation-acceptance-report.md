# Phase 0 평가 세트 독립 QA 재인수 보고서

검증일: 2026-08-10
대상: `evaluation/pages-inventory.csv`, `evaluation/relation-labels.csv`,
`evaluation/contradiction-labels.csv`, `evaluation/README.md`
판정: **CSV 구조·offline 채점 계약 인수 / Ingest-level AC-5 차단 유지**

## 1. 기준과 범위

- 제품 기준: `docs/ZeroWiki-MVP-서비스-기획서.md` 3절
- 측정 기준: `docs/검증-QA-계약-확정-체크리스트.md` 3절
- 인수 기준: `docs/ZeroWiki-요구사항-정의서.md` 1.4절·11절의 AC-3·AC-5
- 실행 시나리오: `qa/test_scenarios.md` A-4·U-1
- enum 정본: `docs/ZeroWiki-요구사항-정의서.md` 12.1절 UD-16

`qa/verify_phase0_evaluation.py`는 backend validator와 별도로 CSV 스키마, 필수값,
참조, 유일성, enum, negative 규칙, 중복, stable `case_id`와 분모를 검증한다.

## 2. Findings

### HIGH-1 잔존 — Ingest-level AC-5 mapping manifest가 없다

`contradiction-labels.csv`의 `case_id`와 README의 NFKC·공백 축소·case-fold·순서
무시 exact-pair 계약으로 **offline 후보 채점은 재현 가능**해졌다. 그러나 원본
page/claim 또는 seed 입력이 Ingest 결과의 `claim_id`로 어떻게 이어지는지 나타내는
manifest는 아직 없다. 실제 `GET /libraries/{id}/contradictions` 응답을 gold
`case_id`에 결정론적으로 조인할 수 없으므로 Ingest-level AC-5 최종 통과는 선언할
수 없다.

영향: AC-5, A-4, FR-KNW-05, FR-ING-11.

필요 변경(backend 소유): seed source/page/claim 식별자, gold `case_id`, Ingest 후
`claim_id` 쌍을 연결하는 versioned mapping manifest와 생성 절차를 추가한다.

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
| Ingest-level AC-5 | conflict 23 | mapping manifest 전까지 차단 |

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

- mapping manifest가 없어 Ingest-level AC-5는 차단 상태다.
- relation negative 8건과 contradiction `NO_CONFLICT` 3건은 sampled FPR의
  분해능이 각각 12.5%p, 33.3%p로 낮다.
- 라벨의 의미 정확성에 대한 독립 이중 검토와 외부 NoteWiki 원본 대조는 미실행이다.
- AC-3은 평가 CSV가 아니라 실제 사용자 U-1을 실행해야 판정할 수 있다.
