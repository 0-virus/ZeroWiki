// 🚀 목업 데이터: 변경 검토 화면
// 실제 API 연동 전 개발용
// 헌법 제4조 6항: 실 API 연동 완료 전에 "완료"로 보고하지 않음

import { ChangeSet, ChangeItem } from '../types/changes';

export const mockChangeSet: ChangeSet = {
  id: 'cs-raft-001',
  title: 'Raft 알고리즘 페이지 재정렬 및 근거 추가',
  summary:
    '논문 "In Search of an Understandable Consensus Algorithm (Ongaro & Ousterhout, 2014)"의 내용을 바탕으로 Raft 알고리즘의 구조를 더 명확하게 정렬하고, 각 섹션에 원본 인용을 추가했습니다. 새로운 연결과 모순 판정도 포함됩니다.',
  riskLevel: 'MAJOR',
  status: 'READY_FOR_REVIEW',
  baseLibraryVersionId: 'v-10',
  baseLibraryVersionNo: 12,
  counts: {
    total: 3,
    safe: 2,
    danger: 1,
  },
  items: [
    {
      changeItemId: 'ci-1',
      targetType: 'PAGE',
      targetId: 'page-raft',
      targetName: 'Raft 알고리즘',
      operation: 'UPDATE',
      riskLevel: 'MAJOR',
      reason: '논문을 더 충실히 반영하도록 본문 구조 개선',
      beforeSnapshot: {
        title: 'Raft 알고리즘',
        markdownBody:
          '# Raft 알고리즘\n\nRaft는 분산 시스템에서 상태 머신 복제를 구현하기 위한 합의 알고리즘이다.\n\n이 알고리즘은 낡았다.',
        summary: 'Raft 합의 알고리즘 개요',
      },
      afterSnapshot: {
        title: 'Raft 합의 알고리즘',
        markdownBody:
          '# Raft 합의 알고리즘\n\n## 소개\n\nRaft는 분산 시스템에서 상태 머신 복제(state machine replication)를 구현하기 위한 합의 알고리즘이다.\n\n## 핵심 특징\n\nRaft는 다음 특징을 제공한다:\n- Byzantine Fault Tolerance를 제공한다\n- Leader-based 구조로 단순하다\n- 실제 시스템에 구현 가능하다',
        summary: 'Raft 합의 알고리즘의 개요와 핵심 특징',
      },
      evidenceSummary: [
        {
          sourceId: 'src-raft-paper',
          sourceTitle: 'In Search of an Understandable Consensus Algorithm (Ongaro & Ousterhout, 2014)',
          excerpt:
            'Raft is a consensus algorithm for managing a replicated log. It produces a result equivalent to (multi-)Paxos, and it is as efficient as Paxos in the general case, but its structure is quite different from Paxos.',
          excerptLanguage: 'en',
          confidence: 0.95,
          url: 'https://raft.github.io/raft.pdf',
        },
      ],
      aiConfidence: 0.87,
    },
    {
      changeItemId: 'ci-2',
      targetType: 'RELATION',
      targetId: 'rel-raft-paxos',
      targetName: 'Raft ↔ Paxos 비교',
      operation: 'CREATE',
      riskLevel: 'MINOR',
      reason: 'Raft와 Paxos의 동등성을 명확히 하기 위해 연결 추가',
      beforeSnapshot: {
        type: 'RELATION',
        status: null,
      },
      afterSnapshot: {
        sourcePageId: 'page-raft',
        targetPageId: 'page-paxos',
        relationType: 'SIMILAR_TO',
        rationale: 'Raft는 Paxos와 동등한 결과를 제공하지만 더 직관적인 구조를 가진다',
        confidence: 0.92,
      },
      evidenceSummary: [
        {
          sourceId: 'src-raft-paper',
          sourceTitle: 'In Search of an Understandable Consensus Algorithm',
          excerpt: 'Raft produces a result equivalent to (multi-)Paxos',
          excerptLanguage: 'en',
          confidence: 0.95,
          url: 'https://raft.github.io/raft.pdf',
        },
      ],
      aiConfidence: 0.92,
    },
    {
      changeItemId: 'ci-3',
      targetType: 'CLAIM',
      targetId: 'claim-raft-understandable',
      targetName: '"Raft는 이해하기 쉽다"',
      operation: 'CREATE',
      riskLevel: 'MINOR',
      reason: '논문의 핵심 주장 추출',
      beforeSnapshot: {
        type: 'CLAIM',
        status: null,
      },
      afterSnapshot: {
        statement: 'Raft 알고리즘은 Paxos에 비해 이해하고 구현하기 더 쉽다.',
        knowledgeStatus: 'CLAIM_FROM_SOURCE',
        confidence: 0.88,
      },
      evidenceSummary: [
        {
          sourceId: 'src-raft-paper',
          sourceTitle: 'In Search of an Understandable Consensus Algorithm',
          excerpt: 'Our goal is to provide a consensus algorithm for practical systems',
          excerptLanguage: 'en',
          confidence: 0.90,
          url: 'https://raft.github.io/raft.pdf',
        },
      ],
      aiConfidence: 0.88,
    },
  ],
};

export const mockChangeItemDiffData = {
  ci1: {
    title: 'Raft 알고리즘',
    beforeBody:
      'Raft는 분산 시스템에서 상태 머신 복제를 구현하기 위한 합의 알고리즘이다.\n\n이 알고리즘은 낡았다.',
    afterBody:
      'Raft는 분산 시스템에서 상태 머신 복제(state machine replication)를 구현하기 위한 합의 알고리즘이다.\n\n Raft는 다음 특징을 제공한다:\n- Byzantine Fault Tolerance를 제공한다\n- Leader-based 구조로 단순하다\n- 실제 시스템에 구현 가능하다',
  },
};
