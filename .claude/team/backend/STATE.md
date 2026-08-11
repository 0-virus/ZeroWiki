# backend 현재 상태

> **덮어쓰기 스냅샷.** 매 턴 종료 전에 이 파일 전체를 최신 상태로 갱신한다(헌법 제4조 1항).
> 시간순 기록은 `WORKLOG.md`에 append 한다.

마지막 갱신: 2026-08-11 22:34 KST (Phase 1 착수 가능성·계약 차단 감사)

## 현재 단계

**Phase 0 기술 실행 완료 · rubric-context-v3 dev gate PASS · Phase 1 계약 감사 완료 · 실행 가능한 backend 구현은 Spring Boot scaffold 부재로 미착수 · holdout 전체 사용자 승인/QA machine gold freeze 진행 중 · production 승격 미확정.**

100문서 2단계 로컬 benchmark와 26건 GPT-5.6 Sol contradiction-only 비교, 결과 계약 검증은 완료했다. claim-specific `rubric-context-v3`는 accuracy `0.9230769`, conflict recall `0.9565217`, sampled FPR `0`으로 dev gate를 통과했다. holdout 입력 70행은 source-grounded 재구성을 마쳤고 전체 사용자 승인과 QA machine gold freeze를 진행 중이지만, 승인·freeze·원본 Claim mapping·production 결정 전에는 holdout API를 호출하지 않으며 모순 자동 판정·자동 적용 승격을 차단한다.

## 진행 중 작업

OpenAI prompt variant 실행 4건과 QA 독립 검증은 완료했다. `rubric-context-v3-r1`은
26/26 completed, 26 API calls/no retries, accuracy `24/26=0.9230769`, recall
`22/23=0.9565217`, sampled FPR `0/3=0`, macro-F1 `0.902`, 비용 `$0.14906`, latency
total/p95 `64.443268s/5.344194s`로 dev gate PASS이며 QA validator도 exit 0이다. 신규
반복 누적 비용은 `$0.55644`, `$0.80` cap 잔여는 `$0.24356`이다. holdout API 호출은 0회이고
holdout gold 파일은 만들지 않았으며, 최종 holdout review 분포는 5개 conflict label 각 8건과 `NO_CONFLICT` 30건을 목표로 전체 사용자 승인 및 QA machine gold freeze 진행 중이다. production 승격은 미확정이다. Phase 1 실행 가능성을 별도 감사한 결과 `backend/`에는 `contracts/`와 `scripts/`만 있고 Spring Boot build scaffold·Java 소스·Maven/Gradle 파일이 없어 기능 구현은 아직 시작할 수 없다.

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
7. GPT-5.6 Sol 비교 실행
   - `backend/scripts/phase0_benchmark.py --execute-openai-contradictions`에 Responses API Structured Outputs 경로 추가
   - `evaluation/results/phase0-gpt-5_6-sol-r1/`: 26/26 호출 완료, input/output 6,016/2,553 token, actual cost $0.10667, latency mean/p95 3.214/6.531초
   - contradiction accuracy 0.615, conflict recall 0.783, sampled FPR 0.000 → 품질 게이트 FAIL
   - 유료 API 호출은 26건으로 제한했고 응답 원문·API key는 저장하지 않음
8. OpenAI 후속 평가 입력·harness 계약
   - `phase0_benchmark.py`에 `current`·`rubric-v2`·`rubric-context-v2`, 중립 context 결합, dense `openai-predictions.csv`, `FAILED`/`NOT_ATTEMPTED`와 `NO_CONFLICT` 구분, `--max-cost-usd` cap을 추가했다.
   - `evaluation/contradiction-contexts.csv`는 기존 CT 입력의 `time_or_version`·`environment_or_condition`·`subject`와 원문 참조를 보유하며 source ref는 prompt에서 제외한다.
   - `evaluation/contradiction-holdout-input.csv`는 gold·`판단_이유` 없이 stable HT 입력과 양쪽 source ref를 보유한다. loader/self-check는 exact `HT-001`~`HT-070` 집합과 holdout 내부 NFKC·연속 공백 축약·case-fold·A/B 순서 무시 중복 및 기존 CT pair 중복을 거부한다.
9. Holdout 실행 경로·rubric 보완
   - `rubric-v2`와 `rubric-context-v2`에 기존 평가 문장과 겹치지 않는 six-label synthetic examples를 공통으로 추가했다.
   - 선택적 `--benchmark-input`·`--gold-labels`가 stable HT 입력/gold를 join해 기존 prediction·metrics 경로를 재사용하며 기본 `--labels` 26-case 모드는 유지한다.
   - gold 파일은 아직 생성하지 않았고 holdout API·production 실행은 미실행이다. 사용자 review 승인 후에만 machine gold enum으로 역매핑한다.
10. Prompt variant 실행·stop condition
   - `evaluation/results/phase0-gpt-5_6-sol-rubric-v2-r1/`: 24/26 완료, 2 `FAILED`, `$0.117765` 진단 run(품질 판정 제외).
   - `evaluation/results/phase0-gpt-5_6-sol-rubric-v2-r2/`: 26/26, `$0.13556`, accuracy `0.615`, recall `0.739`, sampled FPR `0`, macro-F1 `0.553` → FAIL.
   - `evaluation/results/phase0-gpt-5_6-sol-rubric-context-v2-r1/`: 26/26, 27 API attempts(재시도 1회 성공), `$0.154055`, accuracy `0.692`, recall `0.783`, sampled FPR `0`, macro-F1 `0.683` → FAIL.
   - 세 실행 합계 `$0.40738`, `$0.80` 기준 잔여 `$0.39262`; TIME 5/5는 개선됐으나 CONDITION 0/5(모두 `NO_CONFLICT`)이고 accuracy gate 21/26·recall gate 19/23을 충족하지 못해 holdout을 중단했다.
   - retry 계약은 case당 최대 2 attempts, 429/5xx·네트워크·incomplete·출력 없음·JSON parse만 1회 재시도, 일반 4xx no-retry, `max_output_tokens` incomplete retry는 512, unknown usage 실패는 metrics에 보존한다.
11. Claim-specific context v3 구현
   - `evaluation/contradiction-contexts-v3.csv`는 claim A/B별 time·condition과 shared subject를 보유하며 CT-011을 A=`로컬 개발 환경`, B=`운영 데이터베이스`로 분리했다.
   - `phase0_benchmark.py`에 `rubric-context-v3`와 variant별 `load_contexts_v3`를 추가했고, 명시 context 차이를 `NO_CONFLICT`보다 우선하는 축 분류 prompt와 CT-011형 JSON/prompt 누출 self-check를 유지했다.
   - 구현 단계 API 호출은 0회였고 기존 v2 결과·dense/sparse schema를 보존했다. 실제 v3 실행·QA 결과는 다음 12번에 기록한다.
12. Claim-specific context v3 실행·QA
   - `evaluation/results/phase0-gpt-5_6-sol-rubric-context-v3-r1/`: 26/26 completed, 26 calls/no retries, 비용 `$0.14906`, latency total/p95 `64.443268s/5.344194s`.
   - accuracy `24/26=0.9230769`, conflict recall `22/23=0.9565217`, sampled FPR `0/3=0`, macro-F1 `0.902`; 오분류는 `CT-017`(gold `VIEWPOINT_DIFF` → prediction `POSSIBLE_CONFLICT`)과 `CT-021`(gold `POSSIBLE_CONFLICT` → prediction `NO_CONFLICT`)이다.
   - QA validator exit 0·`RESULT PASS failures=0`; 기존 누적 `$0.40738`과 합산한 신규 반복 비용은 `$0.55644`, cap 잔여는 `$0.24356`이다. dev gate는 PASS지만 holdout 미실행·production 승격 미확정이다.
13. Holdout 최종 입력 재구성
   - `evaluation/contradiction-holdout-input.csv` 70행을 실제 NoteWiki 원문 근거로 재구성했다. 최종 review 목표는 `TRUE_CONFLICT`, `TIME_CHANGE`, `CONDITION_DIFF`, `VIEWPOINT_DIFF`, `POSSIBLE_CONFLICT` 각 8건과 `NO_CONFLICT` 30건이다.
   - 초기 초안 분포 `34/4/2/0/1/29`와 HT-065 blocker는 교체 pair로 해소했다. 140개 source ref가 모두 실재하고 NFKC 정규화 내부 중복은 0건이다.
   - claim·context·subject와 사용자용 review 헤더·값은 기술 고유명사를 제외하고 한국어이며, machine input의 exact header/stable ID 계약과 분리한다. holdout input에는 gold/classification/`판단_이유`를 넣지 않았고, `user_decision`은 blank다. 별도 승인 전 machine gold enum 역매핑·gold 파일 생성·API/production 실행은 하지 않는다.
   - QA review 파일은 수정하지 않았다. harness에는 exact HT set·NFKC 내부 중복 검증이 추가되어 self-check PASS를 유지한다.
14. Phase 1 착수 가능성·계약 차단 감사
   - 저장소 실측: `backend/`에는 `backend/contracts/library-constitution.schema.json`과 `backend/scripts/phase0_benchmark.py`만 존재한다. `pom.xml`, `build.gradle*`, `settings.gradle*`, `gradlew*`, `src/main/java`가 없어 Spring Boot API·Ingest Worker의 실행 scaffold가 없다. 기능 구현·의존성 설치는 이 감사에서 하지 않았다.
   - 현재 정본의 구현 순서(`docs/ZeroWiki-API-명세-초안.md` 17절)는 인증·사용자·도서관·운영 헌법 → 업로드·원본·도서관 연결 → Ingest 상태 머신 → 페이지·Claim·Evidence·관계·모순 조회 → ChangeSet 검토·적용·버전 → 검색이다. 인증은 Bearer Access Token과 회전 Refresh Token, 요청 시 도서관 권한 확인(`docs/ZeroWiki-API-명세-초안.md` 2.1절), Access 15분/Refresh 30일(`docs/ZeroWiki-요구사항-정의서.md` 12.1절 UD-02)으로 고정되어 있다.
   - Phase 1에 필요한 확정 계약은 사용자별 소유권·도서관 경계, PRIVATE 기본·명시적 단방향 참조, 원본 공통 보관소와 불변 `source_versions`, 사용자 계정 내부 content hash 중복 후보, 파일 200MB/ZIP 1GB/Import 1000문서 및 MIME·압축·secret 검사, 10종 Ingest 상태와 계획 승인 선행, 항상 ChangeSet을 생성하는 변경 승인, `MINOR|MAJOR` wire risk enum, revision 기반 낙관적 잠금, 이벤트 재생 버전, 현재 도서관 우선 검색과 명시적 확장 검색이다(`docs/ZeroWiki-요구사항-정의서.md` 5.1~5.7절·6.1절, `docs/ZeroWiki-API-명세-초안.md` 2~9절, `docs/ZeroWiki-ERD-초안.md` 4~6절).
   - 미확정 UD의 실제 범위: `UD-03`은 요금제별 처리량 단위와 도서관 개수 한도를 정하지 않아 Phase 1 기본 도서관 CRUD·소유권은 가능하지만 요금제별 library limit·quota enforcement는 차단한다(`docs/ZeroWiki-요구사항-정의서.md` 5.2·5.14절, 12.1절). `UD-10`은 Notion OAuth·결제 공급자이며 둘 다 Phase 2 경로이므로 Phase 1 파일/Obsidian Import를 차단하지 않는다(`docs/ZeroWiki-API-명세-초안.md` 6.2·12절, `docs/ZeroWiki-요구사항-정의서.md` 5.4·5.14절). `UD-19`는 처리량 표시·토큰 원장 환산 규칙이므로 raw token 계획·상태 골격은 가능하나 billable unit 표시·quota 차감·`PAUSED_QUOTA`의 과금 동작은 차단한다(`docs/ZeroWiki-요구사항-정의서.md` 5.5·5.14절, 12.1절; `docs/ZeroWiki-ERD-초안.md` 8절). `UD-20`은 감사 로그 보존 기간·관리자 접근 정책이므로 위험 변경 감사 이벤트 기록은 필요하지만 retention cleanup·관리자 조회/접근 정책은 차단한다(`docs/ZeroWiki-요구사항-정의서.md` 6.1절 NFR-SEC-10, 12.1절; `docs/ZeroWiki-ERD-초안.md` 4.8·7~8절). `UD-22`는 신고 처리 주체·기한이며 Phase 3 `FR-PUB-17`만 차단하므로 Phase 1 core에는 영향이 없다(`docs/ZeroWiki-요구사항-정의서.md` 5.11절·12.2절, `docs/ZeroWiki-MVP-서비스-기획서.md` 23절).
   - 착수 판정: 계약·ERD 검토와 scaffold 준비는 가능하지만 실행 가능한 Phase 1 feature implementation은 **불가**(Spring Boot scaffold 부재). scaffold와 사용자 계약 승인이 준비되면 첫 최소 단위는 `인증 + 도서관 소유권 경계 + 공통 원본 메타데이터/불변 버전 조회`로 고정 권고한다. 이 단위는 UD-03/10/19/20/22의 미확정 실행 경로를 포함하지 않으며, 이후 Import·ChangeSet·검색을 이어 붙인다.

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

python -B qa/verify_phase0_benchmark.py .../phase0-gpt-5_6-sol-r1 --expected-documents 26
→ BENCHMARK_METRICS expected=26 completed=26 completion_rate=1.000
→ AC5_METRICS conflict=23 detected=18 recall=0.783 sampled_fpr=0.0 classification_accuracy=0.615
→ RESULT FAIL failures=2 (구조·manifest·mapping·완료율 PASS, 품질 gate FAIL)

python -B backend/scripts/phase0_benchmark.py --self-check
→ self-check: PASS (prompt gold 누출·context/holdout schema·CT pair 중복 검사 포함)

python -B evaluation/validate_evaluation.py
→ 검증 완료: 오류 0건

holdout loader/schema audit (Python stdlib, UTF-8 read)
→ loader_rows=70, header_exact=True, ids_exact=True
→ source_refs=140 missing=0, holdout_internal_duplicate_pairs=0, ct_overlap_pairs=0
→ forbidden_gold_fields=0

python -B backend/scripts/phase0_benchmark.py --self-check
→ self-check: PASS (retry fixture·4xx no-retry·순차 budget reservation 포함)

python -B evaluation/validate_evaluation.py
→ 검증 완료: 오류 0건

python -B backend/scripts/phase0_benchmark.py --self-check
→ self-check: PASS (v2 회귀·v3 context schema·CT-011 A/B mapping·prompt 금지 필드 포함)

python -B evaluation/validate_evaluation.py
→ 검증 완료: 오류 0건

python -B qa/verify_phase0_benchmark.py --manifest evaluation/results/phase0-gpt-5_6-sol-rubric-context-v3-r1/phase0-manifest.json --generated-claims evaluation/results/phase0-gpt-5_6-sol-rubric-context-v3-r1/phase0-generated-claims.csv --benchmark-output evaluation/results/phase0-gpt-5_6-sol-rubric-context-v3-r1/phase0-benchmark.csv --dense-predictions evaluation/results/phase0-gpt-5_6-sol-rubric-context-v3-r1/openai-predictions.csv --labels evaluation/contradiction-labels.csv --expected-documents 26
→ DENSE_METRICS completed=26/26 recall=0.957 accuracy=0.923 sampled_fpr=0.000 macro_f1=0.902
→ RESULT PASS failures=0

Phase 1 readiness inventory
→ `Get-ChildItem backend`/전체 build scaffold 탐색: `NO_SPRING_BUILD_OR_SRC_MAIN_JAVA_FILES`
→ `python -B backend/scripts/phase0_benchmark.py --self-check`: exit 0, `self-check: PASS`
→ `python -B evaluation/validate_evaluation.py`: exit 0, `검증 완료: 오류 0건`
→ `git -c safe.directory=C:/Users/PC/Desktop/ZeroWiki-SaaS diff --check -- .claude/team/backend/STATE.md .claude/team/backend/WORKLOG.md`: exit 0 (개행 변환 warning만)
→ STATE UTF-8/BOM/replacement/trailing: `utf8_bom=False replacement_chars=False trailing_lines=0`; 신규 WORKLOG 항목 trailing lines `0`
```

`rubric-context-v3` 개발 gate는 **PASS**다. 다만 holdout을 실행하지 않았고 원본 Claim mapping·production provider/model 결정이 남아 있으므로 production 자동 판정 승격은 확정하지 않는다.

## 다음 작업

1. Spring Boot API·Ingest Worker build scaffold가 마련되고 구현 단계 전환이 승인되면, 첫 최소 단위인 `인증 + 도서관 소유권 경계 + 공통 원본 메타데이터/불변 버전 조회`부터 구현한다. 현재는 설계·계약 준비만 가능하다.
2. `UD-03`·`UD-19` 결정 전에는 요금제별 도서관/처리량 한도, billable unit 표시·토큰 환산·quota 차감을 계약으로 고정하지 않는다. `UD-10` Notion/OAuth·결제와 `UD-22` 신고 처리는 각각 Phase 2/3 범위로 보류한다.
3. 원본 17건을 별도 보충하거나 제외 사유를 유지하고, relation/모순 평가 분모를 다시 계산한다.
4. 원본 Claim과 생성 Claim의 실제 대응 manifest가 마련되기 전까지 UD-29 자동 동일성 판정과 AC-5 production mapping을 선언하지 않는다.
5. UD-25 citation·검색을 포함한 integration p95를 재측정한다.
6. holdout 전체 사용자 승인과 QA machine gold freeze가 완료되기 전까지 gold 파일 생성·holdout API·production 모순 자동 판정을 보류하고, v3 결과를 승인 후 재검토한다.

## 차단 요인

- 실행 scaffold: `backend/`에 Spring Boot build 파일·Java 소스·Ingest Worker가 없어 Phase 1 feature implementation과 빌드 검증을 시작할 수 없다.
- 제품 승격: v3 dev gate는 accuracy 0.923·recall 0.957로 통과했지만 holdout 미실행·원본 Claim mapping·production 결정 미확정으로 자동 판정·자동 적용은 차단.
- 계약: `UD-03`은 Phase 1 도서관 개수/Phase 2 처리량 quota, `UD-19`는 billable unit·token ledger, `UD-20`은 audit retention/admin access를 차단한다. `UD-10`과 `UD-22`는 각각 Phase 2 Notion·결제와 Phase 3 신고 절차 범위만 차단한다.
- 승인·freeze: holdout 전체 사용자 승인과 QA machine gold freeze 진행 중이며, 완료 전 machine gold/API/production 판단을 만들지 않는다.
- 입력: NoteWiki inventory 파일 17건 부재, 관계 라벨 11행은 평가 불가.
- 매핑: 현재 generated claim ID는 평가 조인용이며 실제 원본 page/claim 대응을 증명하지 않는다.
- AC-3 사용자 유용성(U-1 5~10명)과 production 검색/citation integration은 미실행.
- production LLM provider/model 및 비용 예산은 미확정. GPT 비교 실험 신규 반복 누적 actual 비용은 `$0.55644`이며 production 확정이 아니다.

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
| `evaluation/results/phase0-gpt-5_6-sol-r1/` | GPT-5.6 Sol 26건 model-specific 결과·per-call token/cost/latency·QA 입력 |
| `evaluation/results/phase0-gpt-5_6-sol-rubric-v2-r1/` | rubric-v2 진단 26행 dense 결과, 2건 FAILED |
| `evaluation/results/phase0-gpt-5_6-sol-rubric-v2-r2/` | rubric-v2 26행 dense 결과·metrics |
| `evaluation/results/phase0-gpt-5_6-sol-rubric-context-v2-r1/` | rubric-context-v2 26행 dense 결과·retry metrics |
| `evaluation/results/phase0-gpt-5_6-sol-rubric-context-v3-r1/` | rubric-context-v3 26행 dense 결과·QA gate PASS |
| `evaluation/contradiction-contexts-v3.csv` | claim A/B별 context 매핑, v3 입력 schema |
| `evaluation/results/phase0-embedding-comparison.json` | UD-17 MiniLM/MPNet 비교 및 73 relation 분모 |
| `docs/Phase-0-벤치마크-결과.md` | 기술 실행·GPT 비교 완료·제품 게이트 조건부 FAIL |
| `docs/ZeroWiki-ERD-초안.md` | UD-13/15/17/29 및 enum/canonical status 반영 |
| `docs/검증-백엔드-ERD-API-정합성.md` | Phase 0 검증 결과·게이트·계약 제한 반영 |

## 참고

- `WORKLOG.md`는 시간순 append-only 기록이며 과거 판단을 삭제하지 않는다.
- `__pycache__`/`.pyc`는 실행 후 backend 범위에서 정리한다.
- 외부 NoteWiki 원문과 비밀·LLM 응답 본문은 저장소에 복제하지 않는다.
