# pm 현재 상태

> **덮어쓰기 스냅샷.** 매 턴 종료 전에 이 파일 전체를 최신 상태로 갱신한다(헌법 제4조 1항).
> 시간순 기록은 `WORKLOG.md`에 append 한다.

마지막 갱신: 2026-08-10 22:38 +09:00

## 현재 단계

**Phase 0 평가 데이터 offline 인수 완료 — mapping manifest 계약·Ingest harness 착수 우선순위 정렬**

## 진행 중 작업

없음. backend·QA 인수 결과를 PM 종합·결정 요청 문서에 반영했다.

## 완료 항목

1. 평가 CSV 실측을 반영했다: pages 180(source 37 / concept 127 / flow 16), relations 84(positive 76 / negative 8), contradictions 26(conflict 23 / `NO_CONFLICT` 3).
2. CSV 구조·offline 채점 계약 인수와 Ingest-level AC-5 차단을 구분했다.
3. AC-3은 U-1 사용자 5~10명 유용성 평가이며 미실행임을 분리했다.
4. 요구사항 정의서 12절 기준으로 사용자 결정 5건(UD-03·10·19·20·22), Phase 0 직접 벤치마크 4건(UD-11·17·25·29), Phase 4 별도 UD-26을 구분했다.
5. 다음 한 걸음을 mapping manifest 계약 및 이를 소비하는 Ingest harness 착수로 권고했다.

## 다음 작업

1. backend가 versioned mapping manifest 계약·생성 절차와 Ingest harness를 마련하면 QA의 A-4 Ingest-level AC-5 재검증 결과를 종합한다.
2. 실제 외부 호출 전 LLM provider/model, 유료 API 사용 범위, 토큰·비용 예산에 대한 사용자 결정·승인을 추적한다.
3. U-1 사용자 5~10명이 준비되면 AC-3 결과를, Phase 4에서는 UD-26 승인 피로 테스트 결과를 별도 반영한다.

## 차단 요인

- Ingest-level AC-5: gold `case_id`와 생성 `claim_id`를 잇는 mapping manifest 부재.
- AC-3: U-1 사용자 유용성 평가 미실행.
- 실제 외부 호출: LLM provider/model·유료 API 범위·토큰/비용 예산 사용자 결정·승인 필요.

## 소유 산출물

| 경로 | 상태 | 산출물 유형 |
| --- | --- | --- |
| `docs/ZeroWiki-MVP-서비스-기획서.md` | 변경 없음 | 기획서 (진실 1순위) |
| `docs/PM-미확정결정-우선순위.md` | 2026-08-10 갱신 | 결정 요청서·실행 승인 경계 |
| `docs/PM-종합-현황.md` | 2026-08-10 갱신 | 종합 현황·의존 사슬·권고 |
| `.claude/team/pm/CLAUDE.md` | 변경 없음 | 역할 지침 |
| `.claude/team/pm/WORKLOG.md` | 2026-08-10 append | 시간순 기록 |
