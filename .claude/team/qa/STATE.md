# qa 현재 상태

> **덮어쓰기 스냅샷.** 매 턴 종료 전에 이 파일 전체를 최신 상태로 갱신한다.
> 시간순 기록은 `WORKLOG.md`에 append 한다.

마지막 갱신: 2026-08-10 22:21 +09:00

## 현재 단계

**Phase 0 평가 세트 재인수 완료 — offline 인수, Ingest-level AC-5 차단**

- 제품 테스트 시나리오는 `qa/test_scenarios.md`에 A 18개, U 6개, S 16개로 총
  40개가 정의돼 있다. 제품 구현 대상 테스트는 모두 미실행이다.
- backend가 relation slug 헤더, contradiction stable `case_id`, AC-3 분리,
  relation/contradiction 채점식과 0분모 처리를 반영했다.
- QA 독립 검증기와 backend validator가 동일 데이터에서 모두 종료 코드 0을
  반환했다.
- CSV 구조와 offline 채점 계약은 인수했다. Ingest-level AC-5는 mapping manifest
  부재로 최종 통과 판정을 보류한다.

## 이번 재인수 결과

- CSV 실측: pages 180, relations 84(positive 76 / negative 8), contradictions
  26(conflict 23 / `NO_CONFLICT` 3), stable case ID 26개.
- 이전 AC-3 대체, FPR/FDR 혼동, slug 헤더, offline contradiction join, 요구사항
  A-5 참조 finding은 해소됐다.
- A-4는 conflict 23건 중 최소 19건, 확정 5종 enum, `NO_CONFLICT` sampled FPR,
  mapping manifest precondition으로 동기화했다.
- AC-3은 계속 별도 U-1 사용자 평가다.

## 진행 중 작업

- 없음. 요청된 재인수 검증과 QA 소유 문서 동기화를 완료했다.

## 다음 작업

1. backend가 seed source/page/claim→Ingest `claim_id` mapping manifest와 생성 절차를
   제공하면 A-4 Ingest-level AC-5를 실행한다.
2. 검증 사용자 5~10명이 준비되면 U-1로 AC-3 유용성 70% 기준을 평가한다.
3. 라벨 의미 정확성의 독립 이중 검토와 외부 NoteWiki 원본 대조를 별도 수행한다.

## 차단 요인

- Ingest-level AC-5: gold `case_id`와 생성 `claim_id` 쌍을 잇는 mapping manifest
  미작성.
- AC-3: 검증 사용자 5~10명의 실제 유용성 평가 미실행.

## 소유 산출물

| 경로 | 상태 |
| --- | --- |
| `qa/test_scenarios.md` | 40개 시나리오 정의, A-4 최신 평가 세트 계약 반영 |
| `qa/verify_phase0_evaluation.py` | 새 slug/case_id 스키마 독립 검증, PASS |
| `qa/phase0-evaluation-acceptance-report.md` | offline 인수 / Ingest-level AC-5 차단 |
| `qa/README.md` | 재인수 상태와 차단 조건 반영 |
| `.claude/team/qa/STATE.md` | 현재 스냅샷 |
| `.claude/team/qa/WORKLOG.md` | 시간순 작업 기록 |

## 부록: UD 분류 판정 방법

UD 정본은 `docs/ZeroWiki-요구사항-정의서.md` 12절이다. 목록·분류·건수를 이
파일에 복제하지 않는다. 필요할 때 정본의 12.1절과 12.2절 표를 직접 읽고 행 단위로
판정한다.
