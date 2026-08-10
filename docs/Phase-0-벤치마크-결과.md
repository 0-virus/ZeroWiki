# ZeroWiki Phase 0 벤치마크 결과

작성일: 2026-08-11
최종 run: `phase0-20260811-qwen3-1_7b-r2`

## 1. 판정

**Phase 0 기술 검증 실행은 완료했다. 제품 게이트는 조건부 FAIL이다.** 100문서 2단계 처리, 관계 후보 비교, token·시간 측정, Markdown 왕복, 1,000 change set replay, constitution schema 고정은 완료했다. 다만 로컬 `qwen3:1.7b`의 모순 6-class 정확도가 19.2%이고 sampled FPR이 100%여서 모순 자동 탐지 품질은 Phase 1 허용 수준이 아니다.

리더 결정은 다음과 같다.

- Phase 1의 인증·원본 보관·도서관·Import·변경 승인·검색 골격은 착수할 수 있다.
- 모순 자동 판정·자동 적용은 비활성화한다. 더 강한 모델이 독립 평가에서 classification accuracy 80% 이상을 통과할 때까지 사용자 검토 제안으로만 취급한다.
- `qwen3:1.7b`는 Phase 0 기준선일 뿐 production provider 확정이 아니다.

## 2. 입력 현실

| 항목 | 결과 |
| --- | ---: |
| inventory | 180 |
| 실제 존재 | 163 |
| 누락 | 17 |
| 전체 relation label | 84 |
| 평가 가능 relation | 73 (positive 66 / negative 7) |
| 누락 endpoint로 제외 | 11 |
| contradiction | 26 (conflict 23 / NO_CONFLICT 3) |

100문서 Ingest는 실제 존재하는 inventory 행의 앞 100건을 사용했다. 누락 17건을 성공으로 간주하거나 대체하지 않았다.

## 3. 100문서 2단계 Ingest

| 지표 | 결과 |
| --- | ---: |
| 완료 | 100 / 100 (100%) |
| 평균 input token | 2,069.46 / 문서 |
| 평균 output token | 179.98 / 문서 |
| 평균 scan | 9.567초 / 문서 |
| scan p95 | 14.000초 / 문서 |
| 평균 synthesis | 12.989초 / 문서 |
| synthesis p95 | 19.319초 / 문서 |
| 로컬 유료 API 비용 | $0 |

초기 run은 output 128-token 제한 때문에 JSON이 잘린 36건이 실패해 완료율 64%였다. root cause에서 제한을 256으로 올리고 string 길이 schema를 추가한 뒤 실패 행만 resume해 100%를 달성했다. 전력 비용과 장비 감가, production provider 비용은 포함하지 않는다.

입력에서는 secret처럼 보이는 값의 우변과 알려진 access-key/token 형식을 local LLM 호출 전 `[REDACTED]`로 치환한다. 문서 원문이나 LLM 응답 본문은 benchmark 결과에 저장하지 않는다.

## 4. 관계와 embedding — UD-17

| 모델 | 차원 | ROC-AUC | 100문서 warm 시간 | 문서당 |
| --- | ---: | ---: | ---: | ---: |
| multilingual MiniLM | 384 | 0.9632 | 3.418초 | 0.0342초 |
| multilingual MPNet | 768 | 0.9848 | 32.289초 | 0.3229초 |

**결정: Phase 1 기본은 `sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2`, 384차원.** MPNet의 AUC 이득은 0.0216이지만 차원은 2배이고 warm 처리는 약 9.4배 느렸다. 실제 검색 실패가 측정될 때만 MPNet을 재검토한다. negative 7건뿐이므로 관측 threshold는 production threshold로 고정하지 않는다.

상세 분모와 관측값은 `evaluation/results/phase0-embedding-comparison.json`에 있다.

## 5. 모순 품질

| 지표 | 결과 |
| --- | ---: |
| conflict recall | 100% (23 / 23) |
| sampled FPR | 100% (3 / 3) |
| 6-class accuracy | 19.2% (5 / 26) |
| 예측 분포 | `CONDITION_DIFF` 26 / 26 |

recall만 보면 통과처럼 보이지만 모든 쌍을 conflict로 출력한 결과다. QA verifier가 이 경우를 `PASS`하던 결함을 발견해 26건 전체 classification accuracy 80% gate를 추가했다. 수정된 verifier의 최종 결과는 `FAIL failures=1`이다.

`qwen3:4b`는 두 번 모두 2.5GB blob 다운로드 후 Windows/D-drive SHA 검증 단계가 장시간 정지해 비교 실행에 진입하지 못했다. 유료 외부 모델 호출은 하지 않았다.

## 6. Markdown 왕복, replay, constitution

| 항목 | 결과 |
| --- | --- |
| Markdown export → import | 100 / 100 byte hash 일치 |
| event replay | 1,000 change set × 25회 |
| replay median | 0.0000619초 |
| replay p95 | 0.0000697초 |
| Phase 2 snapshot trigger | 1초 |
| snapshot 필요 | 아니오 |
| constitution | `backend/contracts/library-constitution.schema.json`, top-level 12필드 |

왕복은 현재 exporter/parser가 없으므로 ZIP byte-preserving 기준이다. 실제 Markdown AST 변환기가 생기면 구조 동등성 검사를 추가한다. replay는 in-memory prototype이므로 DB·network 비용이 포함된 Phase 1 integration benchmark로 다시 확인한다.

## 7. Phase 0 의사결정

| UD | 결정 |
| --- | --- |
| UD-11 | Phase 1은 중요한 주장만 생성한다. 100문서에서 관측 문장 7,501개 대비 중요 주장 100개로 제한했다. 모순 자동 판정은 비활성화한다. |
| UD-17 | multilingual MiniLM, 384차원 |
| UD-25 | reference local 환경의 per-document budget을 scan p95 15초, synthesis/answer-generation p95 20초로 둔다. 실제 검색·citation을 포함한 answer endpoint는 Phase 1 integration에서 재검증한다. |
| UD-29 | 자동 identity key를 만들지 않는다. AI가 동일성 mapping을 제안하고 기존 변경 승인 흐름에서 사용자가 확정한다. |
| UD-13 | Phase 1 이벤트 재생 유지. Phase 2 snapshot 불필요. |
| UD-15 | API/ERD와 맞춘 12필드 JSON Schema v1 고정. |

## 8. 재현 명령

```powershell
python backend/scripts/phase0_benchmark.py --execute-phase0 --resume `
  --source-root C:\Users\halma\Desktop\NoteWiki `
  --model-root D:\ZeroWikiPhase0\models `
  --scan-model qwen3:1.7b --synthesis-model qwen3:1.7b `
  --document-count 100 --run-id phase0-20260811-qwen3-1_7b-r2 `
  --output-dir evaluation/results

python qa/verify_phase0_benchmark.py `
  --manifest evaluation/results/phase0-manifest.json `
  --generated-claims evaluation/results/phase0-generated-claims.csv `
  --benchmark-output evaluation/results/phase0-benchmark.csv `
  --contradiction-output evaluation/results/phase0-contradictions.csv `
  --expected-documents 100
```

구조·mapping·100문서 완료율은 통과하지만, 최종 명령은 모순 accuracy gate에서 의도대로 exit 1이다.
