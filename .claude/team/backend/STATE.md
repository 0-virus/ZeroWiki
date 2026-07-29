# backend 현재 상태

> **덮어쓰기 스냅샷.** 매 턴 종료 전에 이 파일 전체를 최신 상태로 갱신한다(헌법 제4조 1항).
> 시간순 기록은 `WORKLOG.md`에 append 한다.

마지막 갱신: 2026-07-29 15:30 UTC (QA 검증 + frontend 갭 #2·#3 통합 완료)

## 현재 단계

Phase 0 이전 — **문서 검증 및 스키마 조정 완료.** 요구사항 정의서(v0.2) 기반 ERD·API 명세 정합성 확인 완료. 리더 피드백 모두 반영. **코드 없음.**

## 진행 중 작업

없음. ERD 조정 완료.

## 완료 항목

**ERD 조정 (2026-07-29 완료):**
- ✓ revision 필드 (libraries, change_sets, library_constitution_versions)
- ✓ idempotency_records 엔터티 신규 추가 (멱등성 키 24시간 캐시)
- ✓ 복합 인덱스 16개 (API 필터·정렬·JOIN·검색)
- ✓ source_versions.detected_language 추가 (원본 언어 감지, API 2.8절·5.1절 계약 반영)
- ✓ idempotency_records.response_body 설명 수정 (서명 URL 제외, NFR-SEC-09 반영)
- ✓ ingest_jobs 상태 명확화 (8가지 + 부수 상태)
- ✓ ERD 8절을 UD-11~20과 상호 참조
- ✓ 헤딩 레벨 수정 (### 5.1)

## 다음 작업

1. **리더의 API 명세 수정 대기** (frontend 갭 + pm 기획서 반영):
   - **MAJOR 4건**: 도서관 홈 응답(4.1절 + topicSummary), 배치 근거 조회(7.2절), 상태 전이 명확화(6.1절), ingest_jobs 상태 enum(2.7절 ✓완료)
   - **MINOR 3건**: 페이지 topics(7.1절), 활동 집계(11절), 검색 확장(9.1절)
   - 확정 후 ERD·DB 스키마 재검토 필요 (특히 MAJOR 항목)
   - **참고**: topicSummary는 content_embeddings 기반 주제 추출 (Phase 2~3)

2. **ChangeSet risk_level 판정 기준 권고안** — 검증 문서 2절 미확정. MINOR 심각도

3. **미확정 계약 의견 제출** — pm/qa와 협의:
   - ERD 8절 1, 2, 3, 5, 6, 7, 8번 (UD-11, 12, 13, 15, 16, 17, 18)
   - API 명세 18절 1, 2, 3, 4번 (UD-01~04)
   - 새 미확정 3건: UD-21(Lint 스케줄), UD-22(신고 처리), UD-23(알림 보존)

## 차단 요인

- **사용자 결정 대기**: UD-01, 02, 03, 04, 14, 19, 20, 21, 22, 23 (토큰 수명·처리량·원본 발췌·감사 로그·Lint·신고·알림)
- **벤치마크 대기**: UD-11, 17 (Claim 세밀도, 임베딩 모델·차원) — Phase 0 완료 후 확정
- **기술 결정 대기**: UD-12, 13, 15, 16, 18 (Claim 복제·버전 과거 조회·헌법 스키마·관계 유형·Notion ID)

## 소유 산출물

| 경로 | 상태 |
| --- | --- |
| `docs/ZeroWiki-ERD-초안.md` | v1.2 완성. 모든 검토 항목 반영 완료 |
| `docs/검증-백엔드-ERD-API-정합성.md` | 참조용 (수정 불필요) |
| `backend/` | 미생성 |
