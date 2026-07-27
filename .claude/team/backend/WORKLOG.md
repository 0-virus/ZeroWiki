# backend 작업 저널

> **append-only.** 새 항목을 파일 맨 아래에 추가한다. 기존 항목은 수정·삭제하지 않는다(헌법 제4조 2항).
> 형식: `날짜 / 무엇을 했는가 / 산출물 경로 / 미해결 사항`
> 현재 상태 스냅샷은 `STATE.md`를 본다.

---

## 2026-07-27 — ERD·API 정합성 교차 검증 (세션 a8461ee3)

**한 일**

- ERD 엔터티와 API 리소스 양방향 매핑 (작업 #5) — 양호, API 구현 가능
- Ingest·ChangeSet 상태 머신 정합성 검증 (작업 #6) — MINOR, `risk_level` 판정 기준 미확정
- 동시성 제어 저장 근거 확인 (작업 #7) — MAJOR, 낙관적 락 `revision` 필드 부재
- 인덱스와 쿼리 패턴이 API 필터를 지탱하는지 검증 (작업 #8) — MAJOR, 복합 인덱스 15개 이상 미정의

**산출물**

- `docs/검증-백엔드-ERD-API-정합성.md`

**결론**

BLOCKER 없음. 모든 불일치는 ERD 업데이트로 해결 가능.

**미해결**

- MAJOR 3건 ERD 미반영: `revision` 필드(`libraries`, `change_sets`, `library_constitution_versions`), `idempotency_records` 엔터티, 복합 인덱스 15개 이상
- MINOR 1건: ChangeSet `risk_level` 판정 기준
- 미확정 계약 의견 미제출: API 18절 1~4번, ERD 8절 1, 2, 3, 5, 6, 7, 8번

---

## 2026-07-27 — 팀 스캐폴딩 수립 (리더)

**한 일**

- 헌법·역할 지침·상태·저널 파일 체계 생성 (리더 작업)

**산출물**

- `.claude/CONSTITUTION.md`, `.claude/team/backend/{CLAUDE,STATE,WORKLOG}.md`

**미해결**

- 없음
