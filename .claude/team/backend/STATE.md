# backend 현재 상태

> **덮어쓰기 스냅샷.** 매 턴 종료 전에 이 파일 전체를 최신 상태로 갱신한다(헌법 제4조 1항).
> 시간순 기록은 `WORKLOG.md`에 append 한다.

마지막 갱신: 2026-08-10 22:30 KST (QA 후속 평가 계약 정정 완료)

## 현재 단계

**Phase 0 평가 데이터 구조 인수 완료 · 채점 계약 QA 재인수 대기 · Ingest 프로토타입 실행 준비 단계.**

- 인벤토리: source 37 / concept 127 / flow 16 / total 180
- 지식 페이지: concept + flow = 143
- 연결 라벨: 84행 (positive 76 / negative 8)
- 모순 라벨: 26행 (실제 모순 23 / `NO_CONFLICT` 3)
- AC-1("문서 100개 Import 완료율 95%") 정식 검증은 기존 결정대로 Phase 1에서 수행한다.

## 진행 중 작업

없음. 평가 CSV의 구조·참조 무결성 검증과 계획 문서 정합화까지 완료했다.

## 완료 항목

### Phase 0 평가 데이터 인수 검증 (2026-08-10)

1. `evaluation/pages-inventory.csv`
   - 표준 CSV 파싱 성공, 정확한 헤더 확인
   - `page_id` 180개와 `page_slug` 180개 각각 유일
   - `page_type` 실측: source 37 / concept 127 / flow 16
2. `evaluation/relation-labels.csv`
   - 모든 endpoint slug가 인벤토리를 참조
   - UD-16 7종, direction, confidence, positive/negative 규칙 충족
   - 방향 무시 중복 페이지 쌍 없음
3. `evaluation/contradiction-labels.csv`
   - 5종 모순 분류와 `NO_CONFLICT`만 사용
   - 빈 필드와 방향 무시 중복 주장 쌍 없음
4. `evaluation/validate_evaluation.py`
   - 외부 의존성 없이 위 검사를 반복 실행
   - 최종 실행 결과: 오류 0건
5. 문서 정합화
   - `evaluation/README.md`의 concept 수를 실측 127로 정정하고 검증 명령 추가
   - Phase 0 계획의 역사적 36 source / 152 page 가정을 37 source / 143 knowledge page / 180 inventory 실측으로 대체
   - 사용자 라벨링 대기를 인수 완료로 전환
   - Phase 0 직접 결정 대상은 UD-11·17·25·29의 4건으로 정정하고 UD-26은 Phase 4 별도 트랙으로 유지

### QA 독립 인수 후속 정정 (2026-08-10)

1. relation endpoint 헤더를 실제 값에 맞춰 `source_page_slug`·`target_page_slug`로 변경하고 템플릿·계획·검증기를 동기화했다.
2. contradiction 26행에 stable `case_id`(`CT-001`~`CT-026`)를 추가하고 유일성·형식 검사를 추가했다.
3. 연결 지표를 pair recall·strict recall·bounded precision·sampled FPR로 분리했다. AC-3은 U-1 사용자 5~10명 유용성 평가이며 CSV로 대체할 수 없음을 명시했다.
4. 모순 지표를 recall, sampled FPR, FDR로 분리하고 FDR 분모가 0이면 N/A로 처리하도록 고정했다.
5. contradiction의 원본 page/claim 및 생성 `claim_id` 매핑이 없어 stable ID만으로는 Ingest-level AC-5를 재현할 수 없음을 blocker로 기록했다.

## 다음 작업

1. **실행 전 사용자 결정 확보**
   - LLM provider/model 후보와 Phase 0 토큰·비용 예산
   - 유료 API 사용 및 외부 호출 범위
2. **Ingest 입력 방식 확정**
   - 외부 NoteWiki 원본 본문을 저장소에 복제하지 않고 읽는 방식
   - 민감정보·비밀 차단과 사용자 계정 내부 격리 확인
3. **Phase 0 프로토타입 구현·실행**
   - 1단계 scan / 2단계 synthesis / 메트릭 수집
   - 평가 CSV로 연결 오프라인 지표와 모순 조건부 오프라인 지표 채점
   - AC-3은 U-1 사용자 평가로 별도 검증
   - AC-5는 원본 page/claim → 생성 `claim_id` mapping 확보 후 최종 검증
   - UD-11·17·25·29 결정 근거 작성
4. **후속 트랙**
   - UD-26은 Phase 4에서 5~10명 사용자 승인 피로 테스트로 측정
   - AC-1 정식 검증은 Phase 1의 문서 100개 실행으로 측정

## 차단 요인

### Phase 0 실행 차단

- **사용자 승인 필요:** LLM provider/model 선택, 유료 API 사용 범위, 토큰·비용 예산
- **입력 경로 확인 필요:** 외부 원본을 복제하지 않는 Ingest 방식과 민감정보 검사 절차
- **AC-5 재현 blocker:** 평가 case의 원본 page/claim 및 Ingest 생성 `claim_id` 대응 manifest 부재
- **AC-3 실행 blocker:** U-1 검증 사용자 5~10명과 실제 제안 관계 유용성 평가 미실행

### 미확정 계약 정본 (`docs/ZeroWiki-요구사항-정의서.md` 12절)

- 사용자 결정: UD-03, UD-10, UD-19, UD-20, UD-22
- Phase 0 벤치마크: UD-11, UD-17, UD-25, UD-29
- Phase 4 별도 트랙: UD-26

## 소유 산출물

| 경로 | 상태 |
| --- | --- |
| `.claude/team/backend/PHASE-0-벤치마크-계획.md` | 2026-08-10 인수 실측 반영. 평가 데이터 완료, 실행 전 blocker 명시 |
| `evaluation/pages-inventory.csv` | 사용자 인수 데이터. 180행 검증 완료, 라벨 판단·행 미변경 |
| `evaluation/relation-labels.csv` | 84행. endpoint slug 헤더 정정, 라벨 판단·행 내용 유지 |
| `evaluation/contradiction-labels.csv` | 26행. stable case_id 추가, 주장·분류·판단 내용 유지 |
| `evaluation/validate_evaluation.py` | 표준 라이브러리 기반 반복 검증 스크립트 |
| `evaluation/README.md` | 인수 실측·채점 방법·검증 명령 정합화 |
| `evaluation/relation-labels-template.csv` | `*_page_slug` 헤더와 slug 예시로 동기화 |
| `evaluation/contradiction-labels-template.csv` | stable `case_id` 열·예시로 동기화 |
| `docs/ZeroWiki-ERD-초안.md` | v1.5. 이번 작업 변경 없음 |
| `docs/검증-백엔드-ERD-API-정합성.md` | 이번 작업 변경 없음 |

## 참고

- CSV 라벨의 판단 내용과 행은 이번 인수 검증에서 수정하지 않았다.
- 외부 NoteWiki 원본 내용이나 비밀을 evaluation에 복제하지 않았다.
- 관계 CSV는 `source_page_slug`·`target_page_slug`로 `pages-inventory.csv.page_slug`를 참조한다.
