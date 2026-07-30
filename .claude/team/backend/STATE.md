# backend 현재 상태

> **덮어쓰기 스냅샷.** 매 턴 종료 전에 이 파일 전체를 최신 상태로 갱신한다(헌법 제4조 1항).
> 시간순 기록은 `WORKLOG.md`에 append 한다.

마지막 갱신: 2026-07-30 (ERD v1.5: claims 테넌시 앵커 추가)

## 현재 단계

Phase 0 이전 — **기술 결정 완료, ERD 구조 검증 완료.** 요구사항 정의서 v1.0, API 명세 18절 확정 반영 완료. 기술 결정 9건 모두 처리(6건 리더 확정, 3건 확정 포함). **UD-29 신설:** claim_key 의미 정규화 불가(벤치마크 대기). **ERD v1.5 완성:** 대규모 구조 변경(UD-12 재사용) + 테넌시 앵커(UD-12 후속). **코드 없음.**

## 진행 중 작업

없음. ERD 구조 변경 완료.

## 완료 항목

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

1. **Contradictions 엔드포인트 상세 정의** (갭 #21, frontend 제출)
   - 리더가 API 명세에 추가할 때까지 대기
   - 필터(status, classification, since), 페이지네이션, 응답 필드 정의 필요
   - ERD contradictions 엔터티 정합성 검증 준비

## 차단 요인

**사용자 결정 대기** (요구사항 정의서 12절 정본 — 5건):
- UD-03 (처리량 단위: Free·Basic·Advanced)
- UD-10 (Notion OAuth·결제 공급자)
- UD-19 (처리량 표시 단위·토큰 원장 환산 규칙)
- UD-20 (감사 로그 보존 기간·관리자 접근 정책)
- UD-22 (신고 처리 주체·검토 기한)

**벤치마크 대기** (7건):
- UD-11 (Claim 세밀도: 모든 문장 vs 중요 주장) — UD-12 재설계와 함께 평가
- UD-17 (pgvector 임베딩 모델·차원)
- UD-25 (성능 목표: 스캔 완료·답변 응답 시간)
- UD-26 (개별 승인 건수 상한)
- UD-29 (claim_key 생성 규칙) — 의미 정규화 대안 평가 필요

## 소유 산출물

| 경로 | 상태 |
| --- | --- |
| `docs/ZeroWiki-ERD-초안.md` | v1.5 완성. 기술 결정 9건 반영, ERD 구조 완성(claims 재설계 + 테넌시 앵커) |
| `docs/검증-백엔드-ERD-API-정합성.md` | 기술 결정 6건 확정 권고안 완료 |
| `backend/` | 미생성 |

## 참고

- **요구사항 정의서:** v1.0(리더 확정). 확정 15건(사용자 5 + 기술 9 + UD-04·05·24·27), 미확정 12건(사용자 5 + 벤치마크 7, UD-29는 벤치마크로 이동)
- **API 명세:** 18절 확정 15건, Contradictions 엔드포인트 상세 스펙 대기
- **ERD 9절 개정 이력:** v1.4 = 기술 9건 처리 + 대규모 구조 변경 / v1.5 = 테넌시 앵커 추가
- **구조 변경 완료:** claims 재설계(UD-12) + 테넌시 앵커(UD-12 후속) + 정합성 규칙 완성. UD-11(Claim 세밀도) 결과에 따라 인덱스 설계 조정 필요(행 수 불확실성).
