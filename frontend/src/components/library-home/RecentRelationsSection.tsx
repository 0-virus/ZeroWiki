import React from 'react';
import { RecentRelation } from '@/types/library';

interface RecentRelationsSectionProps {
  relations: RecentRelation[];
  isLoading?: boolean;
}

export const RecentRelationsSection: React.FC<RecentRelationsSectionProps> = ({
  relations,
  isLoading = false
}) => {
  const getRelationLabel = (type: string): string => {
    const labels: Record<string, string> = {
      RELATED: '관련됨',
      COMPONENT_OF: '구성 요소',
      ENHANCES: '개선함',
      TRAINS: '학습',
      USES: '사용함',
    };
    return labels[type] || '관계';
  };

  const getConfidenceBadge = (confidence: number): string => {
    if (confidence >= 0.9) return '높음';
    if (confidence >= 0.75) return '중간';
    return '낮음';
  };

  if (isLoading) {
    return (
      <section className="relations-section-skeleton">
        <h2 className="section-title">최근 발견 연결</h2>
        <div className="relations-list">
          {[1, 2, 3, 4, 5].map((i) => (
            <div key={i} className="relation-item-skeleton">
              <div className="skeleton-text skeleton-main" />
              <div className="skeleton-text skeleton-sub" />
              <div className="skeleton-button" />
            </div>
          ))}
        </div>
      </section>
    );
  }

  if (relations.length === 0) {
    return (
      <section className="relations-section-empty">
        <h2 className="section-title">최근 발견 연결</h2>
        <div className="empty-state">
          <p className="empty-text">
            아직 제안된 새로운 관계가 없습니다.
          </p>
        </div>
      </section>
    );
  }

  return (
    <section className="relations-section">
      <div className="section-header">
        <h2 className="section-title">최근 발견 연결</h2>
        <a href="#" className="more-link">
          더 보기 →
        </a>
      </div>
      <div className="relations-list">
        {relations.slice(0, 5).map((relation, index) => (
          <div key={index} className="relation-item">
            <div className="relation-arrow">
              {relation.sourceTitle} → {relation.targetTitle}
            </div>
            <div className="relation-meta">
              <span className="relation-type">
                {getRelationLabel(relation.relationType)}
              </span>
              <span className={`confidence-badge confidence-${getConfidenceBadge(relation.confidence).toLowerCase()}`}>
                확신도: {(relation.confidence * 100).toFixed(0)}%
              </span>
            </div>
            <div className="relation-actions">
              <button className="action-button small approve">승인하기</button>
              <button className="action-button small reject">거절하기</button>
            </div>
          </div>
        ))}
      </div>
    </section>
  );
};
