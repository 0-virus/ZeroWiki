# backend 현재 상태

> **덮어쓰기 스냅샷.** 시간순 기록은 `WORKLOG.md`에 append 한다.

마지막 갱신: 2026-08-18 KST (Phase 1 최종 backend 감사·Gradle 전환)

## 현재 단계

**Phase 1 backend PASS/완료.** Spring Boot 4.1.0/Java 21 backend는 Gradle 9.7.0 단일 build로 전환했고, clean test·build·bootJar와 Phase 0/holdout harness를 재검증했다. production provider/model 승격과 자동 모순 apply는 별도 운영 승인 전까지 수행하지 않는다.

## 진행 중 작업

- 없음. 이번 감사에서 legacy build 파일·wrapper·생성 cache를 제거하고 `build.gradle`, `settings.gradle`, Gradle wrapper로 dependency, Java toolchain, test, executable jar 동작을 보존했다.
- 로컬 object store 기본 경로는 Maven `target/` 제거에 맞춰 ignored Gradle output인 `build/object-store`로 동기화했다.

## 완료·검증 근거

- Gradle: `gradlew.bat -g %LOCALAPPDATA%\ZeroWiki-Gradle-9.7 clean test --no-daemon --non-interactive --console=plain` → exit 0, 15 suites/39 tests, failures 0, errors 0, skipped 0.
- Package: 같은 cache의 `gradlew.bat ... build bootJar` → exit 0, `build`와 executable `bootJar` 생성.
- Phase 1 live 정본 대조: 100문서 `done=100`, `published=100`, `topics=6`, `citations=5`; M7 security 8/8 PASS.
- AC-5: Batch 70/70, recall 0.800, accuracy 0.843, sampled FPR 0, Wilson 95% upper 0.114, macro-F1 0.801, actual cost `$0.2108` PASS.
- Python harness: `phase0_benchmark.py --self-check`와 `openai_batch_holdout.py --self-check`는 각각 exit 0/PASS, 두 파일 `py_compile`은 exit 0, holdout QA validator는 `expected=70`, `completed=70`, `failed=0`, `RESULT PASS failures=0`이다. 유료 Batch 재제출과 raw output 저장·출력은 하지 않았다.

## 다음 작업

- production provider/model 사용과 자동 모순 판정·apply는 사용자 운영 승인 후 별도 진행한다.
- 운영 재현이 필요할 때 PostgreSQL/API/worker를 다시 기동하고 M7 foreign-owner live probe를 재실행한다.
- Windows IDE가 dependency JAR를 인덱싱하는 환경에서는 workspace 밖 Gradle user home(`-g %LOCALAPPDATA%\ZeroWiki-Gradle-9.7`)을 사용한다.

## 차단 요인·남은 위험

- Phase 1 완료 blocker는 없다.
- 현재 API/PostgreSQL listener가 없으면 live suite 즉시 재현은 불가하지만, 보존된 최신 PASS를 무효화하지 않는 운영 재현 risk다.
- `UD-03`, `UD-19`, `UD-20`, production provider/model과 자동 apply는 미확정이며 임의로 확정하지 않았다.
- M6 SEMANTIC/HYBRID는 provider 미확정으로 owner-scoped keyword fallback이고 후보 전체 O(N) scan이다. 규모 증가 후 측정값이 생기면 인덱스/pgvector 경로를 별도 검토한다.

## 소유 산출물

- `backend/build.gradle`, `backend/settings.gradle`, `backend/gradlew`, `backend/gradlew.bat`, `backend/gradle/wrapper/**`
- `backend/src/main/**`, `backend/src/test/**`, `backend/contracts/**`, `backend/scripts/**`
- `docs/ZeroWiki-ERD-초안.md`, `docs/검증-백엔드-ERD-API-정합성.md`
- 본 파일과 `.claude/team/backend/WORKLOG.md`
