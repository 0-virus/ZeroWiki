# 변경 검토 화면 — Hi-fi 와이어프레임 참고 자료 (외부 디자인 가져오기)

**갱신**: 2026-07-31
**출처**: claude.ai/design 프로젝트 "Hi-fi 와이어프레임 제작" (`https://claude.ai/design/p/909dd058-1ca0-4b24-8c60-d2e73c48e1b0?file=change-review.dc.html`), `change-review.dc.html` + `support.js`
**원본 저장**: `frontend/screens/design-source/change-review.dc.html` (dc-runtime 없이는 그대로 실행되지 않는 원본 그대로)
**정적 재현(인터랙션 확인용)**: Artifact — https://claude.ai/code/artifact/648804bd-dbeb-4ab3-bfa1-8b58946aa249

이 문서는 기존 구현(`frontend/src/components/review-changes/`)과 화면 설계(`review-changes.md`)를 대체하지 않는다. **하이파이 시안 하나를 정리해 놓은 참고 자료**이며, 반영 여부는 frontend 역할이 판단한다.

## 1. 시안이 보여주는 구조

- 상단 breadcrumb(도서관 > 변경 세트 #b634) + 제목 + 상태 배지(READY_FOR_REVIEW) + 기준 버전 라인(v12 → v13, 총 34건 = 안전 28·검토 4·위험 2, revision 4).
- **위험도 필터 탭**: 전체/안전/검토/위험 4개 탭, 각 탭에 개수 표시. 선택된 탭만 굵게 + 밑줄.
- 탭 바 우측에 **"안전 28건 일괄 승인"** 버튼 — 위험도 SAFE인 항목을 한 번에 전부 승인 상태로 바꾼다.
- **항목 목록**: 아코디언(한 번에 하나만 펼침). 접힌 줄에 캐럿·제목·위험도 배지·종류·현재 판단 상태(미판단/✓ 승인됨/× 거절됨/± 수정 요청/… 보류)를 한 줄로 보여준다.
- 펼친 항목 안에 변경 이유, **전/후 split diff**(변경 전 v{N} / 변경 후 제안, 각각 배경색으로 삭제·추가 강조), 영향 페이지·근거 건수·AI 신뢰도 한 줄, 근거 원문 보기 링크, 판단 버튼 4개(승인/거절/수정 요청/판단 보류) + "거절·수정 요청에는 사유가 필요하며 운영 헌법 학습에 반영됩니다" 안내문.
- **하단 바**: "판단 완료 N / 34" 진행 카운터 + "나중에" / "승인 항목 적용 — v13 발행" 두 버튼.
- 시각 스타일: JetBrains Mono/D2Coding 모노스페이스, `#fdfcfc`/`#201d1d` 명암 대비, 헤더 로고에 타자기 타이핑 애니메이션. 다른 화면(`global-home`, `library-home`, `graph`, `ingest-plan`, `mobile`, `onboarding`, `page-attention`)과 공유하는 시각 언어로 보인다(해당 파일은 가져오지 않음).

## 2. 노출된 커스터마이즈 축 (원본 `data-props`)

원본은 프리뷰 도구에서 조절 가능한 속성을 노출한다. 실제 구현 시 참고할 만한 "디자인 결정 지점" 목록으로 읽을 수 있다.

| 속성 | 옵션 | 기본값 | 의미 |
| --- | --- | --- | --- |
| `diffLayout` | SPLIT / UNIFIED | SPLIT | diff를 좌우 분할로 볼지, 한 컬럼(unified)으로 볼지 |
| `riskColorMode` | SEMANTIC / MONO | SEMANTIC | 위험도 배지에 색을 쓸지(빨강/주황/회색), 무채색으로만 표시할지 |
| `badgeStyle` | OUTLINE / FILLED | FILLED | 배지 스타일(시안 실제 마크업은 OUTLINE으로 보임 — 미완전 반영 가능성) |
| `evidenceOpenByDefault` | boolean | false | 근거 섹션을 기본으로 펼쳐둘지 |
| `bodyFontSize` | 14~18px | 14 | 본문 폰트 크기 |
| `showRail` | boolean | true | (이 화면에는 rail 마크업이 없음 — 공유 레이아웃 프레임의 잔여 속성으로 추정) |
| `askTheme` | DARK / CREAM | DARK | (이 화면에는 AI 질문 패널 마크업이 없음 — 다른 화면과 공유하는 props 스키마의 잔여 속성으로 추정) |

## 3. 기존 구현·설계 문서와의 차이 (frontend 역할이 검토할 지점)

| 항목 | 하이파이 시안 | 현재 `review-changes.md` / 구현 |
| --- | --- | --- |
| 위험도 체계 | SAFE / REVIEW / HIGH (3단계, 필터 탭까지 있음) | MINOR / MAJOR (2단계, API 명세 8.1절 계약) — **명칭·단계 수가 다르다.** API 계약을 SAFE/REVIEW/HIGH로 바꾸자는 뜻이 아니라, UI 라벨을 이 3단계 감각으로 매핑할지 검토 지점 |
| 항목 탐색 방식 | 아코디언 목록 전체를 위험도로 필터링, 한 번에 여러 항목을 오가며 판단 | 항목 탭으로 하나씩 선택해 보는 방식 (`selectedItemIndex`) |
| 일괄 처리 | "안전 N건 일괄 승인" 버튼 존재 | 없음 (항목별 개별 승인만) |
| 판단 완료 후 흐름 | 항목별 결정은 로컬 상태로만 쌓이고, **별도의 "v13 발행" 버튼**으로 한 번에 확정 | 항목 결정 시 즉시 API 호출(`POST /reviews`)로 확정 — 배치 발행 개념 없음 |
| Diff 레이아웃 | split을 기본으로 하되 unified 전환 옵션 존재 | split 고정 (라이브러리 미선정 상태, `review-changes.md` 6절) |
| 사유 안내 문구 | "거절·수정 요청에는 사유가 필요하며 **운영 헌법 학습에 반영됩니다**" | 사유 필수(최소 20자 권장)만 명시, "운영 헌법 학습" 언급 없음 — 이 문구가 가리키는 기능(거절 사유를 AI 재학습에 쓰는 것)은 현재 어떤 기준 문서에도 없어 보임 |

이 표는 결론이 아니라 **frontend가 반영 여부를 판단할 재료**다. 특히 "일괄 승인"과 "판단 완료 후 별도 발행" 두 흐름은 API 명세 8.1절(`POST /reviews`)의 계약과 맞을지 확인이 필요하고, "운영 헌법 학습" 문구는 근거 문서가 없으므로 그대로 채용하지 말 것.

## 4. 재현 아티팩트에 대해

`frontend/screens/design-source/change-review.dc.html`은 dc-runtime(`support.js`, claude.ai/design 프로젝트 전용, 70KB 생성 코드)이 있어야 렌더링되므로 이 저장소만으로는 열리지 않는다. 위 Artifact 링크는 같은 마크업·상태 로직(필터 탭, 아코디언 펼침, 일괄 승인, 개별 판단 버튼, 진행 카운터)을 순수 HTML/CSS/JS로 손으로 재현한 것으로, 실제 동작을 눈으로 확인하는 용도다. 다크·라이트 테마를 모두 지원한다. React/Next.js 컴포넌트가 아니므로 그대로 복사해 쓸 수 없다 — 참고용 프로토타입이다.
