import React from 'react';
import { OpenQuestion, KnowledgeGap } from '@/types/library';

interface OpenQuestionsSectionProps {
  questions: OpenQuestion[];
  gaps: KnowledgeGap[];
  isLoading?: boolean;
}

export const OpenQuestionsSection: React.FC<OpenQuestionsSectionProps> = ({
  questions,
  gaps,
  isLoading = false
}) => {
  if (isLoading) {
    return (
      <section className="questions-section-skeleton">
        <h2 className="section-title">열린 질문과 지식 공백</h2>
        <div className="questions-list">
          {[1, 2, 3, 4, 5].map((i) => (
            <div key={i} className="question-item-skeleton">
              <div className="skeleton-icon" />
              <div className="skeleton-text skeleton-main" />
              <div className="skeleton-text skeleton-sub" />
            </div>
          ))}
        </div>
      </section>
    );
  }

  if (questions.length === 0 && gaps.length === 0) {
    return (
      <section className="questions-section-empty">
        <h2 className="section-title">열린 질문과 지식 공백</h2>
        <div className="empty-state">
          <p className="empty-text">
            모든 내용이 검증되었습니다! 🎉
          </p>
        </div>
      </section>
    );
  }

  const allItems = [
    ...questions.map(q => ({ type: 'question' as const, ...q })),
    ...gaps.map(g => ({ type: 'gap' as const, ...g })),
  ].slice(0, 5);

  return (
    <section className="questions-section">
      <div className="section-header">
        <h2 className="section-title">열린 질문과 지식 공백</h2>
        <a href="#" className="more-link">
          더 보기 →
        </a>
      </div>
      <div className="questions-list">
        {allItems.map((item, index) => (
          <div key={index} className="question-item">
            <div className="question-icon">
              {item.type === 'question' ? '❓' : '📭'}
            </div>
            <div className="question-content">
              <p className="question-text">
                {item.description}
              </p>
              <p className="question-target">
                📄 {item.targetTitle}
              </p>
            </div>
            <button className="action-button small">확인하기</button>
          </div>
        ))}
      </div>
    </section>
  );
};
