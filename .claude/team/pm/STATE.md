# pm 현재 상태

> **덮어쓰기 스냅샷.** 매 턴 종료 전에 이 파일 전체를 최신 상태로 갱신한다(헌법 제4조 1항).
> 시간순 기록은 `WORKLOG.md`에 append 한다.

마지막 갱신: 2026-08-11 23:02 +09:00 (Phase 1 즉시 착수 가능성·계약 차단 감사)

## 현재 단계

**Phase 0 기술 검증·`rubric-context-v3` dev gate 완료 — Phase 1 core는 계약·scaffold 준비 가능, feature 구현은 Spring Boot/Ingest scaffold 부재로 미착수 — holdout API·production 승격 차단**

`rubric-context-v3`는 26/26 완료, conflict recall `0.9565217`, accuracy `0.9230769`, sampled FPR `0`, macro-F1 `0.902`로 dev gate를 통과했다. QA의 70-case 한국어 review와 machine gold는 실제 파일상 사용자 승인 70건·정확한 UTF-8 헤더로 동결되어 있으나, holdout API/validator는 실행하지 않았다. production LLM provider/model·승격은 확정하지 않는다.

## 진행 중 작업

Phase 1 구현 순서를 기획서 23절·API 명세 17절·요구사항 정의서 12절과 대조해, 현재 즉시 가능한 범위를 계약·설계·scaffold 준비로 한정했다. `backend/`에는 `contracts/`·`scripts/`만 있고 Spring Boot build 파일·Java source·Ingest Worker가 없어 실제 feature 구현은 시작할 수 없다.

## 완료 항목

1. Phase 0 결과와 역할 STATE/실제 산출물을 대조했다.
2. `rubric-context-v3` dev 결과를 QA validator로 재실행해 `RESULT PASS failures=0`을 확인했다.
3. Backend self-check와 evaluation validator를 각각 exit 0으로 재실행했다.
4. Frontend `npm.cmd run build`와 `npx.cmd tsc --noEmit`를 각각 exit 0으로 확인했다. 화면은 목업 데이터 기반이며 실 API는 없다.
5. QA holdout review/gold를 직접 읽어 70행·사용자 결정 `승인` 70건·목표 분포·BOM 없는 UTF-8 헤더를 확인했다. QA STATE의 과거 “진행 중” 기록은 현재 상단 스냅샷 및 실제 파일의 완료 상태와 분리해 해석한다.
6. 첫 최소 vertical slice를 `인증 + 도서관 소유권 경계 + 공통 원본 메타데이터/불변 버전 조회`로 권고했다. 이는 미확정 UD-03·10·19·20·22의 Phase 1 실행 경로를 포함하지 않는다.

## 다음 작업

1. Spring Boot API·Ingest Worker 실행 scaffold가 마련되고 구현 단계 전환이 승인되면 첫 vertical slice를 구현한다.
2. API 명세 17절 순서(`인증·사용자·도서관·운영 헌법 → 업로드·원본 연결 → Ingest 승인 상태 머신 → 지식 조회 → 변경 세트 → 검색`)에 따라 의존 기능을 확장한다.
3. 실제 API integration에서 citation/search 포함 p95를 재검증한다.
4. 별도 실행 승인을 받은 뒤 frozen gold로 holdout API/validator를 실행하고, 결과 전에는 모순 자동 판정·자동 적용·production 승격을 하지 않는다.
5. 사용자 결정 UD-03·10·19·20·22와 Phase 4 AC-3·UD-26을 정본 기준으로 계속 추적한다.

## 차단 요인

- **실행 scaffold:** `backend/`에 Spring Boot build 파일·Java source·Ingest Worker가 없어 Phase 1 feature 구현·빌드 검증을 시작할 수 없다.
- **제품 승격:** v3 dev gate PASS만으로 production을 확정할 수 없다. holdout API/validator, 원본 Claim mapping, production provider/model·비용 승인과 독립 gate가 남아 있다.
- **미확정 사용자 결정:** UD-03은 Phase 1 요금제별 도서관 한도(구현 시)와 Phase 2 처리량 과금, UD-10·UD-19는 Phase 2, UD-20은 Phase 2 감사 정책, UD-22는 Phase 3 신고 절차를 막는다.
- **Phase 4:** AC-3 U-1 사용자 평가와 UD-26 승인 상한 테스트는 기획서 23절상 Phase 4다.
- **입력·통합 위험:** NoteWiki inventory 누락 17건·평가 불가 relation 11행, API 실연동·citation/search integration 미검증, Frontend HeaderBar duplicate-key 경고가 남아 있다.

## 핵심 권고·의존 사슬

**권고:** scaffold를 먼저 마련하고 첫 slice를 작게 구현한다. 모순 자동 판정·적용은 사용자 검토 제안으로만 둔다.

**의존 사슬:** `scaffold → 인증/도서관/헌법 → 원본/불변 버전 → Import/Ingest 승인 → 페이지·Claim·Evidence·관계·모순 조회 → ChangeSet 검토·적용·버전 → 검색 → integration p95/citation → holdout gate → production 검토`.

## 소유 산출물

| 경로 | 상태 | 산출물 유형 |
| --- | --- | --- |
| `docs/ZeroWiki-MVP-서비스-기획서.md` | 변경 없음 | 기획서 (진실 1순위) |
| `docs/PM-미확정결정-우선순위.md` | 2026-08-11 갱신 | 사용자 결정 요청·Phase 의존성 |
| `docs/PM-종합-현황.md` | 2026-08-11 23:02 갱신 | 종합 현황·첫 slice·의존 사슬·위험 |
| `.claude/team/pm/CLAUDE.md` | 변경 없음 | 역할 지침 |
| `.claude/team/pm/WORKLOG.md` | 2026-08-11 23:02 append | 시간순 기록 |

## 검증 근거

```text
python qa/verify_phase0_benchmark.py ...rubric-context-v3... --expected-documents 26
→ DENSE_METRICS completed=26/26 recall=0.957 sampled_fpr=0.000 accuracy=0.923 macro_f1=0.902
→ RESULT PASS failures=0 (exit 0)

python -B backend/scripts/phase0_benchmark.py --self-check
→ self-check: PASS (exit 0)

python -B evaluation/validate_evaluation.py
→ 검증 완료: 오류 0건 (exit 0)

frontend: npm.cmd run build; npx.cmd tsc --noEmit
→ Next 14 build 성공·static pages 5/5·두 명령 모두 exit 0

holdout artifact audit (Python stdlib, read-only)
→ review/gold rows=70/70, 사용자 승인=70, 목표 분포 일치, exact UTF-8 headers, BOM 없음

backend scaffold inventory
→ pom.xml/build.gradle/settings.gradle/gradlew/src/main/java 모두 없음
```

홀드아웃 API·validator는 위 검증에 포함되지 않았으며 실행하지 않았다.
