// 🚀 목업 데이터 (MOCK DATA)
// 실제 API 연동 전 화면 개발용. 완료 보고 시 이 파일을 제거하고 실 API 호출로 대체.
// 근거: 헌법 제4조 6항

import { LibraryHomeResponse } from '@/types/library';

export const mockLibraryHomeResponse: LibraryHomeResponse = {
  data: {
    id: 'lib-001',
    name: 'AI & 머신러닝 개인 도서관',
    home: {
      topics: [
        { name: '신경망 구조', pageCount: 12 },
        { name: '주의 메커니즘', pageCount: 8 },
        { name: '활성화 함수', pageCount: 6 },
        { name: '배치 정규화', pageCount: 5 },
        { name: 'Transformer 아키텍처', pageCount: 9 },
        { name: '손실 함수와 최적화', pageCount: 7 },
      ],
      recentRelations: [
        {
          sourceTitle: '신경망 구조',
          targetTitle: '활성화 함수',
          relationType: 'RELATED',
          confidence: 0.92,
        },
        {
          sourceTitle: '주의 메커니즘',
          targetTitle: 'Transformer 아키텍처',
          relationType: 'COMPONENT_OF',
          confidence: 0.88,
        },
        {
          sourceTitle: '배치 정규화',
          targetTitle: '신경망 구조',
          relationType: 'ENHANCES',
          confidence: 0.85,
        },
        {
          sourceTitle: '손실 함수와 최적화',
          targetTitle: '신경망 구조',
          relationType: 'TRAINS',
          confidence: 0.90,
        },
        {
          sourceTitle: 'Transformer 아키텍처',
          targetTitle: '주의 메커니즘',
          relationType: 'USES',
          confidence: 0.95,
        },
      ],
      openQuestions: [
        {
          description: '왜 ReLU가 자주 사용되는가?',
          targetTitle: '활성화 함수',
        },
        {
          description: 'Attention weight를 계산할 때 softmax를 사용하는 이유는?',
          targetTitle: '주의 메커니즘',
        },
        {
          description: '배치 정규화의 역전파 계산은 어떻게 되는가?',
          targetTitle: '배치 정규화',
        },
        {
          description: 'Cross-entropy loss와 다른 손실 함수의 선택 기준은?',
          targetTitle: '손실 함수와 최적화',
        },
        {
          description: 'Query, Key, Value의 차원을 동일하게 유지해야 하는가?',
          targetTitle: '주의 메커니즘',
        },
      ],
      knowledgeGaps: [
        {
          description: '역전파 알고리즘의 수학적 유도',
          targetTitle: '신경망 구조',
        },
        {
          description: '다양한 활성화 함수의 성능 비교',
          targetTitle: '활성화 함수',
        },
        {
          description: '배치 정규화의 통계적 성질',
          targetTitle: '배치 정규화',
        },
        {
          description: 'Self-attention의 계산 복잡도 분석',
          targetTitle: '주의 메커니즘',
        },
        {
          description: '경사 소실(Gradient Vanishing) 해결 방법',
          targetTitle: '신경망 구조',
        },
      ],
      openContradictionCount: 3,
      pendingChangeSetCount: 2,
      recentActivities: [
        {
          type: 'INGEST',
          title: '문서 10개 Import 완료',
          createdAt: '2026-07-30T14:30:00Z',
        },
        {
          type: 'RELATION',
          title: 'AI가 새로운 관계 5개 제안',
          createdAt: '2026-07-30T14:20:00Z',
        },
        {
          type: 'LINT',
          title: 'Lint 검사 완료 (7개 문제 발견)',
          createdAt: '2026-07-30T14:00:00Z',
        },
        {
          type: 'CHANGE_SET',
          title: '신경망 구조 페이지 수정 대기',
          createdAt: '2026-07-30T13:45:00Z',
        },
        {
          type: 'CONTRADICTION',
          title: '모순 3개 발견됨',
          createdAt: '2026-07-29T18:20:00Z',
        },
        {
          type: 'APPROVAL',
          title: '주의 메커니즘 변경사항 승인',
          createdAt: '2026-07-29T17:15:00Z',
        },
        {
          type: 'INGEST',
          title: '논문 5개 업로드',
          createdAt: '2026-07-29T16:30:00Z',
        },
        {
          type: 'RELATION',
          title: '관계 10개 자동 생성',
          createdAt: '2026-07-29T15:45:00Z',
        },
        {
          type: 'LINT',
          title: '정기 Lint 검사',
          createdAt: '2026-07-29T09:00:00Z',
        },
        {
          type: 'INGEST',
          title: '도서 1권 추가',
          createdAt: '2026-07-28T20:30:00Z',
        },
      ],
    },
  },
};
