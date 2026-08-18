# qa 현재 상태

> **덮어쓰기 스냅샷.** 시간순 결과와 이전 FAIL/BLOCKED는 `WORKLOG.md`에 보존한다.

마지막 갱신: 2026-08-18 +09:00 (Phase 1 최종 독립 QA 및 Gradle 전환 정합화)

## 현재 단계

**Phase 1 자동 인수 및 보안 gate PASS**

- 자동 인수 범위는 `docs/ZeroWiki-요구사항-정의서.md` 9절의
  `AC-1·AC-2·AC-4·AC-5`와 Phase 1 치명 보안 리스크 0이다.
- AC-1·2·4 live 최종 run은 `documents=100`, `done=100`,
  `publishedConcepts=100`, `topics=6`, `citations=5`로 PASS했다.
- AC-5 holdout은 70/70 `COMPLETED`, recall `0.800`, 6-class accuracy `0.843`,
  sampled FPR `0.000`, Wilson 95% upper `0.114`, macro-F1 `0.801`로 PASS했다.
- Batch provider-attempt aggregate cost는 `$0.2108` / cap `$0.24356`다. deduped final
  CSV cost `$0.205105`와의 `$0.005695` 차이는 retry overlap을 포함한 ledger
  차이며, state ledger 합계는 metrics JSON과 일치한다.
- M7 최종 보안 실행은 8/8 PASS다. API §2.5 idempotency request
  hash·same-result replay·reused/in-progress 충돌 경로와 foreign-owner
  search/conversation 403/404 boundary를 포함한다.
- production provider/model 승격과 모순 자동 적용은 Phase 1 완료와
  별개의 운영 승인 대상이다. AC-3·6·7 사용자 평가는 Phase 4
  후속 범위다.

## 최신 독립 검증

| 검증 | 결과 |
| --- | --- |
| `qa/verify_phase0_evaluation.py` | PASS, exit 0 |
| `evaluation/validate_evaluation.py` | PASS, exit 0 |
| `qa/verify_phase0_benchmark.py --self-check` | PASS, exit 0 |
| 실제 HT 70행 validator | PASS, exit 0; recall 0.800 / accuracy 0.843 / FPR 0 / Wilson 0.114 / macro-F1 0.801 |
| M1/M2 self-check | 10/10 PASS, exit 0 |
| M3 self-check | 8/8 PASS, exit 0 |
| M4 self-check | 3/3 PASS, exit 0 |
| M6 self-check | 6/6 PASS, exit 0 |
| M7 static self-check | 7/7 PASS, exit 0; 최종 live 항목 포함 8/8 PASS |
| backend Gradle test | QA 독립 실행 `BUILD SUCCESSFUL`, exit 0; XML 15 suites / 39 tests / failures 0 / errors 0 / skipped 0 |
| backend Gradle bootJar | backend 실행 exit 0; QA가 `build/libs/zerowiki-backend-0.1.0-SNAPSHOT.jar` 생성 확인 |

Gradle wrapper는 `backend\gradlew.bat`이다. `%LOCALAPPDATA%\ZeroWiki-Gradle-9.7`
cache로 backend `clean test`·`bootJar` 및 QA 독립 `test`가 모두 exit 0이다.
QA는 `backend/build/test-results/test/TEST-*.xml` 15개도 직접 합산했다.

## 진행 중 작업

- QA 활성 문서의 backend 실행 명령을 Gradle wrapper
  `bootRun --args="--spring.profiles.active=<api|worker>"`로 동기화했다.
- `qa/README.md`, `qa/test_scenarios.md`, 이 STATE의 2026-08-12 장애 판정을
  최신 PASS 스냅샷으로 교체했다.

## 다음 작업

1. production provider/model 승격 승인 후 운영 설정과 실측 모니터링을
   별도 gate로 검증한다.
2. Phase 4에서 AC-3·6·7 사용자 평가를 진행한다.

## 차단 요인

- **Phase 1 제품 blocker 없음.**

## 소유 산출물

| 경로 | 상태 |
| --- | --- |
| `qa/verify_phase0_evaluation.py` | 평가 CSV 구조 validator PASS |
| `qa/verify_phase0_benchmark.py` | CT/HT dense·sparse validator 및 self-check PASS |
| `qa/contradiction-holdout-gold.csv` | 승인·동결된 70행 machine gold |
| `qa/test_m1_m2_blackbox.py` | 10/10 PASS |
| `qa/test_m3_knowledge_blackbox.py` | 8/8 PASS |
| `qa/test_m4_change_review_blackbox.py` | 3/3 PASS |
| `qa/test_m6_search_ai_blackbox.py` | 6/6 PASS |
| `qa/test_m7_security_blackbox.py` | 최종 static/live 8/8 PASS |
| `qa/test_phase1_acceptance_live.py` | 100-document AC-1·2·4 PASS |
| `qa/README.md` | 최신 gate·Gradle wrapper 재현 명령 |
| `qa/test_scenarios.md` | S ID/NFR/API/Phase 추적 및 최신 M7 PASS |

## 재현 명령

```powershell
$env:PYTHONIOENCODING='utf-8'
python -B qa/verify_phase0_evaluation.py
python -B evaluation/validate_evaluation.py
python -B qa/verify_phase0_benchmark.py --self-check
python -B qa/test_m1_m2_blackbox.py --self-check
python -B qa/test_m3_knowledge_blackbox.py --self-check
python -B qa/test_m4_change_review_blackbox.py --self-check
python -B qa/test_m6_search_ai_blackbox.py --self-check
python -B qa/test_m7_security_blackbox.py --self-check

Set-Location backend
.\gradlew.bat -g "$env:LOCALAPPDATA\ZeroWiki-Gradle-9.7" test --no-daemon --non-interactive --console=plain
```

HT 70행 재현 인자는 `qa/README.md`의 Phase 1 holdout 명령을 따른다.
