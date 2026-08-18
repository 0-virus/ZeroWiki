# ZeroWiki QA

**기준:** `docs/검증-QA-계약-확정-체크리스트.md` 3절과
`docs/ZeroWiki-요구사항-정의서.md` 11절
**상태:** Phase 1 자동 인수 및 보안 gate PASS. production provider/model 승격과
모순 자동 적용은 별도 운영 승인 대상이다.

## 구조

```text
qa/
├── README.md
├── test_scenarios.md
├── verify_phase0_evaluation.py
├── verify_phase0_benchmark.py
├── test_m1_m2_blackbox.py
├── test_m3_knowledge_blackbox.py
├── test_m4_change_review_blackbox.py
├── test_m6_search_ai_blackbox.py
├── test_m7_security_blackbox.py
├── test_phase1_acceptance_live.py
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
  인자로 받는다. HT dense 실행은 `--holdout-input` 8열과
  `--holdout-labels` machine gold를 `case_id`로 조인한다.
- `phase0-evaluation-acceptance-report.md`: 평가 세트와 AC-3·AC-5 채점 계약의 독립
  QA 인수 결과.
- `test_m1_m2_blackbox.py`: M1 인증·도서관·헌법과 M2 Source/Upload/Obsidian/Ingest
  계약을 stdlib `urllib`로 검증한다. upload 응답의 절대 URL·`requiredHeaders`·만료·
  세션 바인딩·signed URL redaction도 static/live gate에 포함한다.
- `test_m3_knowledge_blackbox.py`: API 7.1~7.3·9.2 페이지 목록/상세,
  Claim-Evidence-sourceVersion traceability, 관계·모순 review gate와 AC-1 100/95,
  AC-2 topic, AC-4 citation 입력 계약을 static/live로 검증한다.
- `test_m4_change_review_blackbox.py`: API 8절 change-set review/apply/recalculate와
  도서관 version history, revision conflict, owner boundary, apply 전 page version
  불변성을 static/live로 검증한다.
- `test_m6_search_ai_blackbox.py`: API 9.1~9.2의 `KEYWORD`·`SEMANTIC`·`HYBRID`,
  `CURRENT`·`REFERENCED` 명시적 확장, 400자 snippet, owner/reference boundary,
  conversation/message polling, citation triple, `insufficientEvidence`,
  save-proposal/feedback와 `UD-25` answer latency 기록을 static/live로 검증한다.
- `test_m7_security_blackbox.py`: 기존 S-1~S-16 ID를 유지한 NFR/API/Phase 매핑,
  현재 backend의 owner/library descendant·explicit reference·secret rejection·
  audit/change apply·error/log redaction route inventory와 frontend label/landmark/
  keyboard/focus/error static gate를 검증한다. M1/M2/M4/M6 live 흐름을 재사용하고
  owner descendant probe만 추가한다. API §2.5 Java idempotency가 request hash·replay·
  reused/in-progress 충돌 경로를 구현했는지 migration과 분리해 검증한다.
- `test_phase1_acceptance_live.py`: opt-in 시 단일 sequential 100문서 API+worker 흐름에서
  AC-1·AC-2·AC-4와 feedback/save-proposal 계약을 검증한다. raw token·signed URL은
  출력하거나 저장하지 않는다.

평가 CSV는 `evaluation/**`에 있으며 backend 소유다. QA는 해당 파일을 읽고 검증하며
직접 수정하지 않는다.

## 현재 검증 상태

| 범위 | 상태 |
| --- | --- |
| Phase 0 CSV 구조 인수 검사 | 실행 완료 — 구조 PASS |
| Phase 0 offline 채점 계약 | 재인수 완료 |
| Phase 1 HT holdout dense 품질 | **PASS** — 70/70, recall 0.800, 6-class accuracy 0.843, sampled FPR 0, Wilson upper 0.114 |
| Phase 1 Batch cost cap | **PASS (reconciliation note)** — aggregate `$0.2108` ≤ cap `$0.24356`; deduped CSV actual-cost 합계 `$0.205105` |
| M1/M2 static contract·worker profile | **PASS** — 10/10 |
| M3 knowledge static contract | **PASS** — 8/8 |
| M4 change-review static contract | **PASS** — 3/3 |
| M6 search/AI static contract | **PASS** — 6/6 |
| M7 security static·live | **PASS** — 8/8; idempotency replay와 foreign-owner 403/404 boundary 포함 |
| Phase 1 AC-1·AC-2·AC-4 live | **PASS** — documents 100, done 100, published concepts 100, topics 6, citations 5 |
| Phase 4 사용자 평가 | **후속 범위** — Phase 1 자동 인수 gate와 분리 |

Phase 1 HT holdout의 최종 dense validator 재검증은 다음과 같다.

```powershell
$env:PYTHONIOENCODING='utf-8'
python -B qa/verify_phase0_benchmark.py `
  --manifest evaluation/results/phase1-holdout-gpt-5_6-sol-batch-r1/phase0-manifest.json `
  --generated-claims evaluation/results/phase1-holdout-gpt-5_6-sol-batch-r1/phase0-generated-claims.csv `
  --benchmark-output evaluation/results/phase1-holdout-gpt-5_6-sol-batch-r1/phase0-benchmark.csv `
  --dense-predictions evaluation/results/phase1-holdout-gpt-5_6-sol-batch-r1/openai-predictions.csv `
  --holdout-input evaluation/contradiction-holdout-input.csv `
  --holdout-labels qa/contradiction-holdout-gold.csv `
  --expected-documents 70
```

실제 출력은 `BENCHMARK_METRICS expected=70 completed=70 completion_rate=1.000`,
`DENSE_METRICS ... recall=0.800 sampled_fpr=0.000 wilson95_fpr_upper=0.114 accuracy=0.843`,
`RESULT PASS failures=0`, exit 0이다. manifest/generated와 HT input/gold join도 PASS다.

비용 대조 결과 `phase0-batch-metrics.json`은 70 completed/0 failed,
input `53504`, output `5136`, actual cost `$0.2108`, cap `$0.24356`이다. 최종
deduped CSV 70행의 합계는 input `52762`, output `4880`, actual cost `$0.205105`로
metrics보다 `$0.005695`(742 input, 256 output tokens) 낮다. 차이는 초기
`batch-state-000-020.json`과 겹치는 retry tranche `batch-state-006-007.json`이
provider attempt 비용 ledger에는 포함되고 최종 case별 CSV에는 한 번만 남기기 때문이다.
state ledger 합계는 metrics JSON과 일치하며 cap gate는 PASS지만, 두 합계의 의미가
다르므로 exact CSV=metrics equality는 별도 reconciliation note로 보존한다.

CSV 구조와 이전 Phase 0 offline 결과는 Phase 1 HT holdout dense 결과와
분리한다. holdout은 gold `case_id`와 생성 `claim_id`를 잇는 mapping
manifest까지 검증한다. 보안 gate는 holdout으로 대체하지 않고 M7에서 별도로
8/8 PASS했으며, AC-3 사용자 평가는 Phase 4 후속 범위다.

## 재현

PowerShell에서 저장소 루트를 현재 디렉터리로 두고 실행한다.

```powershell
$env:PYTHONIOENCODING='utf-8'
python qa/verify_phase0_evaluation.py
```

M1/M2/M3/M4/M6 제품 계약 검증은 다음 명령으로 재현한다.

```powershell
$env:ZEROWIKI_BASE_URL = "http://localhost:8080"
$env:ZEROWIKI_QA_EXPECT_SECURE_COOKIE = "false"  # local HTTP only; production is true/default
python qa/test_m1_m2_blackbox.py --self-check
python qa/test_m3_knowledge_blackbox.py --self-check
python qa/test_m4_change_review_blackbox.py --self-check
python qa/test_m6_search_ai_blackbox.py --self-check
python qa/test_m7_security_blackbox.py --self-check
python -m unittest discover -s qa -p "test_m1_m2_blackbox.py" -v
python -m unittest discover -s qa -p "test_m3_knowledge_blackbox.py" -v
python -m unittest discover -s qa -p "test_m4_change_review_blackbox.py" -v
python -m unittest discover -s qa -p "test_m6_search_ai_blackbox.py" -v
python -m unittest discover -s qa -p "test_m7_security_blackbox.py" -v
```

서버가 없거나 M3/M4 fixture가 없으면 live 검사는 `BLOCKED_*` skip로 남는다. skip은
제품 기능 PASS가 아니며, M3 AC-1·2·4 완료 판정에는 실제 100문서·page/claim/evidence·
관계/모순·answer fixture가 필요하다.

M6 live 실행은 `ZEROWIKI_QA_REFERENCED_LIBRARY_ID`로 참조 fixture를 재사용할 수 있고,
없으면 격리 QA 계정이 현재·참조·미등록·타 사용자 도서관을 만든다. 검색은 세 mode를
각각 호출하고, 기본 검색은 `CURRENT`, `search-expansions`만 등록된 target의
`REFERENCED` 결과를 허용한다. 답변은 `QUEUED`에서 message detail을 bounded polling해
`COMPLETED`를 확인하고 `M6_LATENCY_SECONDS=...`를 출력한다. `UD-25` p95는 단일 sample로
승격하지 않으며, 준비된 성능 실행에서만 `ZEROWIKI_QA_ENFORCE_LATENCY=true`를 쓴다.
API §9.2가 feedback request body를 정의하지 않으므로 기본 probe는 body 없이 실행하고,
승인된 body가 있으면 `ZEROWIKI_QA_FEEDBACK_BODY`에 JSON으로 주입한다.

### M1/M2 API·Worker 분리 재검증

`SCANNING`은 API가 ingest job을 저장한 뒤 별도 worker가 같은 PostgreSQL에서
`IngestWorker.runOnce()`를 실행해야 `PLAN_REVIEW`로 전이된다. 두 프로세스의 profile과
DB/JWT 설정을 각각 같은 값으로 로드한다(비밀값은 secret store에서 주입하고 문서·로그에
기록하지 않는다).

```powershell
# 터미널 1 — API profile
$env:ZEROWIKI_DB_URL = "jdbc:postgresql://localhost:5432/zerowiki"
$env:ZEROWIKI_DB_USERNAME = "zerowiki"
$env:ZEROWIKI_DB_PASSWORD = "<load-from-secret-store>"
$env:ZEROWIKI_JWT_SECRET = "<load-from-secret-store>"
$env:ZEROWIKI_COOKIE_SECURE = "false"  # local HTTP functional only
Set-Location backend
.\gradlew.bat -g "$env:LOCALAPPDATA\ZeroWiki-Gradle-9.7" bootRun --args="--spring.profiles.active=api"

# 터미널 2 — 같은 DB 설정의 worker profile
$env:ZEROWIKI_DB_URL = "jdbc:postgresql://localhost:5432/zerowiki"
$env:ZEROWIKI_DB_USERNAME = "zerowiki"
$env:ZEROWIKI_DB_PASSWORD = "<load-from-secret-store>"
$env:ZEROWIKI_JWT_SECRET = "<load-from-secret-store>"
Set-Location backend
.\gradlew.bat -g "$env:LOCALAPPDATA\ZeroWiki-Gradle-9.7" bootRun --args="--spring.profiles.active=worker"

# 터미널 3 — QA
$env:ZEROWIKI_BASE_URL = "http://localhost:8080"
$env:ZEROWIKI_QA_EXPECT_SECURE_COOKIE = "false"
$env:ZEROWIKI_QA_POLL_LIMIT = "30"
Set-Location C:\Users\PC\Desktop\ZeroWiki-SaaS
python -m unittest discover -s qa -p "test_m1_m2_blackbox.py" -v
```

worker 로그에 scan 실행이 보이고 QA ingest 상태가 `SCANNING`에서 `PLAN_REVIEW`로
전이되어야 한다. API가 reachable인데 `BLOCKED_WORKER_NOT_RUNNING`이 나오면 worker가
미기동이거나 API와 다른 DB를 보고 있는 것이므로 Phase 1 gate를 차단한다. `Secure`는
production 실행에서 `ZEROWIKI_QA_EXPECT_SECURE_COOKIE`를 생략(기본 true)하고, local
HTTP에서만 false로 내려 기능 실패와 production 보안 실패를 분리한다.

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
