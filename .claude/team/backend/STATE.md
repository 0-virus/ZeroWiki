# backend 현재 상태

> **덮어쓰기 스냅샷.** 매 턴 종료 전에 이 파일 전체를 최신 상태로 갱신한다(헌법 제4조 1항).
> 시간순 기록은 `WORKLOG.md`에 append 한다.

마지막 갱신: 2026-07-31 (Phase 0 벤치마크 최종 계획 완성, 사용자 라벨링 대기)

## 현재 단계

**Phase 0 벤치마크 준비 단계** — 36개 원본 기반 예비 벤치마크. UD-11·17·25·29 확정 목표. 최종 계획 확정: 1단계 데이터셋·평가 세트 준비(2~4일) → 2단계 Ingest 프로토타입(3주) → 3단계 UD-29·갭 #23 측정(병행) → 결과 문서화. **주의: AC-1("문서 100개 완료율") 정식 검증은 Phase 1로 이관.** **차단:** 사용자 라벨링 5건 파일럿 완료 대기.

## 진행 중 작업

없음. Phase 0 사용자 라벨링 준비 단계.

## 완료 항목

**Phase 0 벤치마크 계획 최종화** (2026-07-31 완료)

1. **리더 2가지 결정 반영**
   - ✅ 데이터셋 규모: "문서 100개" → "36개 원본 예비 벤치마크"로 명시
   - ✅ AC-1 유예: "정식 검증은 Phase 1로 이관" 계획서에 명시
   - ✅ 평가 세트: 공개 벤치마크 폐기, 사용자 직접 라벨링 확정
   - ✅ 규모 축소: 연결 20~30건, 모순 10~15건 (기존 50~100/30~50건)

2. **라벨링 가이드 작성**
   - ✅ 연결 라벨 가이드: 형식·필드·예시·파일럿·소요 시간
   - ✅ 모순 라벨 가이드: 5가지 분류·형식·부정 예시
   - ✅ 파일럿 프로세스: 5건 → 형식 검증 1일 → 본작업 병렬

3. **CSV 템플릿 작성**
   - ✅ `evaluation/relation-labels-template.csv` (연결, 5건 파일럿 + 샘플)
   - ✅ `evaluation/contradiction-labels-template.csv` (모순, 5건 파일럿 + 샘플)
   - ✅ 인코딩: UTF-8 + LF, 부정 예시 포함

**기술 결정 9건 최종 처리 (2026-07-30):**

**✅ 확정 6건 (리더 2026-07-30):**
- UD-08 (공개 URL slug) — UNIQUE 제약 지탱
- UD-09 (관리자 API) — 스키마 영향 없음
- UD-13 (이벤트 재생) — Phase 0 성능 측정: 도서관당 500+ change_sets, 1초 초과 시 Phase 2 스냅샷 필수
- UD-15 (JSON Schema) — jsonb 필드 준비 완료
- UD-16 (관계 유형) — 7종 CHECK 제약 확정
- UD-18 (Notion 중복) — 인덱스 (owner_id, content_hash, notion_id), 사용자 내부만

**✅ 확정 3건 (리더 2026-07-30):**
- UD-12 (Claim 복제) — **재사용 구조 확정**: claims에서 page_version_id FK 제거, claim_statuses 신설, page_id FK 추가(v1.5). UD-29 정해질 때까지 자동 매칭 미구현
- UD-21 (Lint 스케줄) — 가입일 기준 분산, 29·30·31일은 말일로 당김
- UD-23 (알림 보존) — 차등 보존(위험 변경·삭제 유예 30일 / 포크·Lint·편집 14일 / 진행 7일), 삭제 유예는 유예 기간까지, archived_at 필드 추가

**🆕 신설 1건:**
- UD-29 (claim_key 생성 규칙) — **벤치마크 대기**: 의미 정규화는 불가능(해시의 한계). AI 제안 + 사용자 승인 모델 권고

**frontend 갭 검토 (2026-07-30):**

✅ **갭 #23** (REQUEST_CHANGES 재생성 완료 감지) — **옵션 A 확정**
- polling 필드: change_items[] (review_status, after_snapshot)
- changeSetId: 같은 ID 내에서만 재생성 (불변)
- 완료 감지: polling 기반 (202 Accepted 후 별도 응답 없음)
- polling 간격: UD-04 기본값 2초
- **확정:** 옵션 A(기존 항목 갱신) — change_items.id 불변, review_status REVISION_REQUESTED→PENDING 전이
- 상태머신 정의: 완료 (검증 9.6절)
- 벤치마크: Phase 0에 "변경 항목 1개 재생성 평균 시간" 추가
- 근거: `docs/검증-백엔드-ERD-API-정합성.md` 9.1~9.6절, API 명세 8.1절(리더 신설 문단)

**ERD 조정 (2026-07-29~30):**

**v1.3→v1.4 (구조 변경, 2026-07-30):**
- ✓ claims 재설계 (UD-12): page_version_id 제거, 버전 비종속 구조
- ✓ claim_statuses 신규: 버전별 Claim 상태 (UNIQUE claim_id·page_version_id)
- ✓ notifications archived_at 추가 (UD-23)
- ✓ page_relations.relation_type 7종 CHECK 제약 (UD-16)
- ✓ source_versions 복합 인덱스 (UD-18)
- ✓ 정합성 규칙 갱신: contradictions·evidences 버전 비종속 의미 재정의

**v1.4→v1.5 (테넌시 앵커 추가, 2026-07-30):**
- ✓ claims.page_id FK 추가(NOT NULL) — 소유자 인가 검사 1홉 단축, 도서관 간 참조 구조 차단
- ✓ 정합성 규칙 보강: evidences·contradictions Claim이 현재 도서관에 속함 명시
- ✓ 정합성 규칙 신규: claim_statuses.page_version_id ∈ pages(claims.page_id)의 버전
- ✓ claim_statuses 인덱스 추가: `(claim_id, page_version_id)` — 버전 범위 검증 최적화

## 다음 작업

**1. 사용자 라벨링 5건 파일럿** (우선순위 1, 즉시)
- 템플릿 다운로드: `evaluation/relation-labels-template.csv`, `contradiction-labels-template.csv`
- 가이드 검토: `.claude/team/backend/PHASE-0-벤치마크-계획.md` 라벨링 가이드 섹션
- 5건 파일럿 작성 (30분)
- backend에 파일럿 5행 전달 (형식 검증 1일)

**2. 선행 조건 확보** (우선순위 2, 파일럿 검증 후)
- NoteWiki 원본 36개 + 페이지 152개 데이터셋 추출 및 정제
- LLM API 토큰 월별 예산 할당 (36문서 벤치마크 기준)
- 임베딩 모델 2~3개 후보 선정 (openai, anthropic, 오픈소스)

**3. 파이프라인 스크립트 개발** (우선순위 3, 선행조건 확보 후)
- `backend/scripts/phase0-benchmark.sh` 작성 (1단계 → 2단계 → 메트릭 수집)
- 메트릭 CSV 형식 정의
- Week 1 내 완료 목표

## 차단 요인

**사용자 결정 대기** (요구사항 정의서 12절 정본 — 5건):
- UD-03 (처리량 단위: Free·Basic·Advanced)
- UD-10 (Notion OAuth·결제 공급자)
- UD-19 (처리량 표시 단위·토큰 원장 환산 규칙)
- UD-20 (감사 로그 보존 기간·관리자 접근 정책)
- UD-22 (신고 처리 주체·검토 기한)

**벤치마크 대기** (9건):
- UD-11 (Claim 세밀도: 모든 문장 vs 주요 주장) — Phase 0 Week 2에서 2회 비교
- UD-17 (pgvector 임베딩 모델·차원) — Phase 0 Week 2에서 2~3개 모델 A/B 테스트
- UD-19 (처리량 표시 단위·토큰 환산 규칙) — UD-25 벤치마크 결과 기반
- UD-25 (성능 목표: 1단계·2단계 시간) — Phase 0 Week 2~3에서 측정
- UD-26 (개별 승인 건수 상한) — Phase 4 사용자 테스트로 이관됨
- UD-29 (claim_key 생성 규칙) — Phase 0 Week 3에서 의미 정규화 벤치마크
- **갭 #23 벤치마크:** 변경 항목 1개 재생성 평균 시간 (Phase 0 Week 3, 2초 polling 간격 검증)

**Phase 0 라벨링 준비:**
- 사용자 5건 파일럿 완료 대기

## 소유 산출물

| 경로 | 상태 |
| --- | --- |
| `docs/ZeroWiki-ERD-초안.md` | v1.5 완성. 기술 결정 9건 반영, ERD 구조 완성(claims 재설계 + 테넌시 앵커) |
| `docs/검증-백엔드-ERD-API-정합성.md` | 기술 결정 9건 권고안 + 갭 #23 검토(옵션 A 확정, 상태머신·벤치마크 정의) 완료. 총 9절 |
| `.claude/team/backend/PHASE-0-벤치마크-계획.md` | **최종본** (2026-07-31 리더 2가지 결정 + 부정 예시 수정 반영: label 컬럼 추가) |
| `evaluation/relation-labels-template.csv` | 신규. 사용자용 연결 라벨 템플릿 (label 컬럼 추가, negative 행은 relation_type·direction 비움). **backend 소유** |
| `evaluation/contradiction-labels-template.csv` | 신규. 사용자용 모순 라벨 템플릿 (5건 파일럿 + 샘플). **backend 소유** |
| `docs/Phase-0-벤치마크-결과.md` | 미생성 (Phase 0 완료 후) |
| `backend/` | 미생성 (Phase 0 선행 조건 확보 후) |

## 참고

- **요구사항 정의서:** v1.0(리더 확정). 확정 15건(사용자 5 + 기술 9 + UD-04·05·24·27), 미확정 12건(사용자 5 + 벤치마크 7, UD-29는 벤치마크로 이동)
- **API 명세:** 18절 확정 15건, Contradictions 엔드포인트 상세 스펙 대기
- **ERD 9절 개정 이력:** v1.4 = 기술 9건 처리 + 대규모 구조 변경 / v1.5 = 테넌시 앵커 추가
- **Phase 0 계획:** 36개 원본 기반 예비 벤치마크 (AC-1 정식 검증은 Phase 1로 이관)
