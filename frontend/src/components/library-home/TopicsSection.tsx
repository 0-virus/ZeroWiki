import React from 'react';
import { Topic } from '@/types/library';

interface TopicsSectionProps {
  topics: Topic[];
  isLoading?: boolean;
}

export const TopicsSection: React.FC<TopicsSectionProps> = ({
  topics,
  isLoading = false
}) => {
  if (isLoading) {
    return (
      <section className="topic-section-skeleton">
        <h2 className="section-title">주요 주제 (관계도 기준)</h2>
        <div className="topics-list">
          {[1, 2, 3, 4, 5, 6].map((i) => (
            <div key={i} className="topic-item-skeleton">
              <div className="skeleton-text skeleton-title" />
              <div className="skeleton-text skeleton-subtitle" />
              <div className="skeleton-text skeleton-badge" />
            </div>
          ))}
        </div>
      </section>
    );
  }

  if (topics.length === 0) {
    return (
      <section className="topic-section-empty">
        <h2 className="section-title">주요 주제 (관계도 기준)</h2>
        <div className="empty-state">
          <p className="empty-text">
            아직 지식이 없습니다. 자료를 업로드하면 AI가 자동으로 주제를 생성합니다.
          </p>
          <button className="action-button primary">자료 업로드하기</button>
        </div>
      </section>
    );
  }

  // 처리 중인 주제만 있는 경우 (실제로는 CONCEPT 페이지만 있고 PUBLISHED 없는 경우)
  // 현재 목업에서는 이 상황을 시뮬레이션하지 않음

  return (
    <section className="topic-section">
      <div className="section-header">
        <h2 className="section-title">주요 주제 (관계도 기준)</h2>
        <a href="#" className="more-link">
          더 보기 →
        </a>
      </div>
      <div className="topics-list">
        {topics.slice(0, 6).map((topic, index) => (
          <div key={index} className="topic-item">
            <div className="topic-number">{index + 1}️⃣</div>
            <div className="topic-content">
              <h3 className="topic-name">{topic.name}</h3>
              <p className="topic-excerpt">
                이 주제의 핵심 개념들을 탐색하세요.
              </p>
            </div>
            <div className="topic-relations">관계 {topic.pageCount * 2}개</div>
          </div>
        ))}
      </div>
    </section>
  );
};
