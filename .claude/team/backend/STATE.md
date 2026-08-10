# backend 현재 상태

> **덮어쓰기 스냅샷.** 매 턴 종료 전에 이 파일 전체를 최신 상태로 갱신한다(헌법 제4조 1항).
> 시간순 기록은 `WORKLOG.md`에 append 한다.

마지막 갱신: 2026-08-11 03:57 KST (Phase 0 실행 검증 및 조건부 제품 게이트 기록)

## 현재 단계

**Phase 0 기술 실행 완료 · 제품 게이트 조건부 FAIL · Phase 1 코어 착수 허용.**

100문서 2단계 로컬 benchmark와 결과 계약 검증은 완료했다. 모순 자동 판정 품질은 게이트를 통과하지 못했으므로 Phase 1에서는 사용자 검토 제안으로만 취급하고 자동 판정·자동 적용을 차단한다.

## 진행 중 작업

없음. Phase 1 backend 코어 구현은 리더의 작업 배분에 따라 착수할 수 있다.

## 완료 항목

### Phase 0 실행·산출물

1. `backend/scripts/phase0_benchmark.py`
   - 표준 라이브러리 우선 CSV/path safety/Markdown byte roundtrip/manifest/로컬 Ollama HTTP harness
   - CSV parser 들여쓰기 오류 수정
   - scan·synthesis 기본 모델을 모두 `qwen3:1.7b`로 고정(Phase 0 실행 기준선)
   - 문서별 실패 격리·resume·token/time 집계
2. 실제 결과 검증
   - `evaluation/results/phase0-benchmark.csv`: 100행, `COMPLETED` 100
   - 평균 input/output token: 2,069.46 / 179.98
   - 평균 scan/process: 9.567초 / 12.989초; p95: 14.000초 / 19.319초
   - Markdown 왕복: 100/100 hash 일치
   - `phase0-manifest.json`·generated claims·contradictions: 26 case 조인과 QA CSV schema 통과
3. 입력 분모 보존
   - inventory 180, 존재 163, 누락 17
   - relation 84 중 평가 가능 73 (positive 66 / negative 7), 누락 endpoint 영향 11행
   - 누락 원본을 성공으로 세지 않고 별도 분모로 기록
4. `backend/contracts/library-constitution.schema.json`
   - JSON Schema v1, `required`/`properties` 12필드, `additionalProperties=false`
5. UD 실행 판단
   - UD-11: 중요한 주장만 생성하는 기준선(관측 문장 7,501 대비 100 important claims)
   - UD-13: 1,000 change set × 25 replay, p95 0.0000697초 < 1초 → Phase 1 event replay, snapshot 보류
   - UD-15: 12필드 JSON Schema v1 기록
   - UD-17: 평가 가능 relation 73건 비교 결과 MiniLM 384d를 Phase 1 기본 후보로 선택
   - UD-25: scan p95 15초·synthesis p95 20초 reference budget; citation/search 포함 integration 재검증 필요
   - UD-29: 자동 Claim 동일성·자동 `claim_key` 매칭 금지; AI 제안 후 사용자 승인만 허용
6. 계약 정합화
   - ERD `risk_level` wire enum: `MINOR | MAJOR`; UI 표시: `SAFE | DANGER`
   - 질문 대기 canonical 상태: `QUESTION_REVIEW`, `nextAction=ANSWER_QUESTIONS`
   - ERD 및 backend 검증 문서에 Phase 0 측정·게이트 기록

## 검증 결과

```text
python -B backend/scripts/phase0_benchmark.py --self-check
→ self-check: PASS

python -B evaluation/validate_evaluation.py
→ 검증 완료: 오류 0건

python -B qa/verify_phase0_benchmark.py ... --expected-documents 100
→ 구조·완료율·AC-5 PASS
→ Phase 0 contradiction classification accuracy 0.192 < 0.800
→ RESULT FAIL failures=1
```

제품 게이트는 **조건부 FAIL**이다. conflict recall 1.000만으로 통과시키지 않으며 sampled FPR 1.000 및 6-class accuracy 0.192를 근거로 모순 자동 판정을 차단한다.

## 다음 작업

1. Phase 1 core: 인증·원본·도서관·Import·변경 승인·검색 골격을 구현하되 모순 결과는 사용자 검토 제안으로만 저장한다.
2. 원본 17건을 별도 보충하거나 제외 사유를 유지하고, relation/모순 평가 분모를 다시 계산한다.
3. 원본 Claim과 생성 Claim의 실제 대응 manifest가 마련되기 전까지 UD-29 자동 동일성 판정과 AC-5 production mapping을 선언하지 않는다.
4. UD-25 citation·검색을 포함한 integration p95를 재측정한다.
5. production provider/model·유료 호출 범위는 사용자 결정 전 확정하지 않는다. `qwen3:4b`는 검증 지연으로 후속 최적화 트랙이다.

## 차단 요인

- 제품 게이트: 모순 6-class accuracy 0.192로 0.800 기준 미달, 자동 판정·자동 적용 차단.
- 입력: NoteWiki inventory 파일 17건 부재, 관계 라벨 11행은 평가 불가.
- 매핑: 현재 generated claim ID는 평가 조인용이며 실제 원본 page/claim 대응을 증명하지 않는다.
- AC-3 사용자 유용성(U-1 5~10명)과 production 검색/citation integration은 미실행.
- production LLM provider/model 및 비용 예산은 미확정. Phase 0은 로컬 Ollama, 유료 비용 `$0`만 사용했다.

## 소유 산출물

| 경로 | 상태 |
| --- | --- |
| `backend/scripts/phase0_benchmark.py` | Phase 0 실행 harness 및 self-check PASS |
| `backend/contracts/library-constitution.schema.json` | 12필드 JSON Schema v1 검증 PASS |
| `evaluation/results/phase0-benchmark.csv` | 100/100 완료, QA 구조 PASS |
| `evaluation/results/phase0-manifest.json` | 26 case, schema·generated join PASS |
| `evaluation/results/phase0-generated-claims.csv` | 26 case generated pair |
| `evaluation/results/phase0-contradictions.csv` | 26행 출력, QA join PASS; 품질 게이트 FAIL |
| `evaluation/results/phase0-metrics.json` | token/time, relation 제한, roundtrip, replay, 모순 지표 |
| `evaluation/results/phase0-embedding-comparison.json` | UD-17 MiniLM/MPNet 비교 및 73 relation 분모 |
| `docs/Phase-0-벤치마크-결과.md` | 기술 실행 완료·제품 게이트 조건부 FAIL |
| `docs/ZeroWiki-ERD-초안.md` | UD-13/15/17/29 및 enum/canonical status 반영 |
| `docs/검증-백엔드-ERD-API-정합성.md` | Phase 0 검증 결과·게이트·계약 제한 반영 |

## 참고

- `WORKLOG.md`는 시간순 append-only 기록이며 과거 판단을 삭제하지 않는다.
- `__pycache__`/`.pyc`는 실행 후 backend 범위에서 정리한다.
- 외부 NoteWiki 원문과 비밀·LLM 응답 본문은 저장소에 복제하지 않는다.
