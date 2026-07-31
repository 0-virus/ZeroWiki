import React from 'react';

interface Contradiction {
  id: string;
  description: string;
  confidence: number;
  pages: string[];
}

interface ContradictionsSectionProps {
  count: number;
  contradictions?: Contradiction[];
  isLoading?: boolean;
}

export const ContradictionsSection: React.FC<ContradictionsSectionProps> = ({
  count,
  contradictions = [],
  isLoading = false
}) => {
  if (isLoading) {
    return (
      <section className="contradictions-section-skeleton">
        <h2 className="section-title">모순</h2>
        <div className="contradictions-list">
          {[1, 2, 3].map((i) => (
            <div key={i} className="contradiction-item-skeleton">
              <div className="skeleton-icon" />
              <div className="skeleton-text skeleton-main" />
              <div className="skeleton-button" />
            </div>
          ))}
        </div>
      </section>
    );
  }

  if (count === 0) {
    return (
      <section className="contradictions-section-empty">
        <h2 className="section-title">모순</h2>
        <div className="empty-state">
          <p className="empty-text">
            발견된 모순이 없습니다. 지식이 일관성 있게 정리되어 있습니다. ✨
          </p>
        </div>
      </section>
    );
  }

  // 🚀 목업 데이터: 실제 API 연동은 갭 #21 API 완성 후
  // ASSUMPTION(갭 #21): 모순 조회 API는 아직 구현 대기 중
  const mockContradictions: Contradiction[] = [
    {
      id: 'con-1',
      description: '신경망 구조 페이지: "활성화 함수는 비선형이어야 한다" vs 활성화 함수 페이지: "선형 활성화도 가능하다"',
      confidence: 0.87,
      pages: ['신경망 구조', '활성화 함수'],
    },
    {
      id: 'con-2',
      description: 'Transformer 아키텍처 페이지: "self-attention은 O(n²) 복잡도" vs 주의 메커니즘 페이지: "linear attention은 O(n)이다"',
      confidence: 0.79,
      pages: ['Transformer 아키텍처', '주의 메커니즘'],
    },
    {
      id: 'con-3',
      description: '배치 정규화 페이지에서 학습/추론 시 통계 계산 방식의 모순',
      confidence: 0.92,
      pages: ['배치 정규화'],
    },
  ];

  const displayContradictions = contradictions.length > 0 ? contradictions : mockContradictions;

  return (
    <section className="contradictions-section">
      <div className="section-header">
        <h2 className="section-title">모순</h2>
        <a href="#" className="more-link">
          더 보기 ({count}개) →
        </a>
      </div>
      <div className="contradictions-list">
        {displayContradictions.slice(0, 5).map((contradiction, index) => (
          <div key={contradiction.id || index} className="contradiction-item">
            <div className="contradiction-icon">⚠️</div>
            <div className="contradiction-content">
              <p className="contradiction-text">
                {contradiction.description}
              </p>
              <div className="contradiction-pages">
                {contradiction.pages.map((page, i) => (
                  <span key={i} className="page-badge">
                    📄 {page}
                  </span>
                ))}
              </div>
              <p className="confidence-text">
                신뢰도: {(contradiction.confidence * 100).toFixed(0)}%
              </p>
            </div>
            <button className="action-button small">검토하기</button>
          </div>
        ))}
      </div>
    </section>
  );
};
