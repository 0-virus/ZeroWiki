import React from 'react';

interface ChangeSet {
  id: string;
  title: string;
  risk: 'MINOR' | 'MAJOR';
  affectedPages: number;
  createdAt: string;
}

interface PendingChangesSectionProps {
  count: number;
  changesets?: ChangeSet[];
  isLoading?: boolean;
}

export const PendingChangesSection: React.FC<PendingChangesSectionProps> = ({
  count,
  changesets = [],
  isLoading = false
}) => {
  if (isLoading) {
    return (
      <section className="changes-section-skeleton">
        <h2 className="section-title">검토 대기 변경</h2>
        <div className="changes-list">
          {[1, 2].map((i) => (
            <div key={i} className="change-item-skeleton">
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
      <section className="changes-section-empty">
        <h2 className="section-title">검토 대기 변경</h2>
        <div className="empty-state">
          <p className="empty-text">
            검토 대기 중인 변경이 없습니다. 모든 변경이 완료되었습니다. ✅
          </p>
        </div>
      </section>
    );
  }

  // 🚀 목업 데이터: 실제 API 연동은 별도 작업
  const mockChangesets: ChangeSet[] = [
    {
      id: 'cs-1',
      title: '신경망 구조 페이지 전반 수정',
      risk: 'MAJOR',
      affectedPages: 3,
      createdAt: '2026-07-30T13:45:00Z',
    },
    {
      id: 'cs-2',
      title: '활성화 함수 설명 추가',
      risk: 'MINOR',
      affectedPages: 1,
      createdAt: '2026-07-30T10:20:00Z',
    },
  ];

  const displayChangesets = changesets.length > 0 ? changesets : mockChangesets;

  const getRiskBadge = (risk: string): React.ReactNode => {
    return risk === 'MAJOR' ? '🔴 위험' : '🟡 주의';
  };

  return (
    <section className="changes-section">
      <div className="section-header">
        <h2 className="section-title">검토 대기 변경</h2>
        <a href="#" className="more-link">
          더 보기 ({count}개) →
        </a>
      </div>
      <div className="changes-list">
        {displayChangesets.slice(0, 5).map((changeset, index) => (
          <div key={changeset.id || index} className="change-item">
            <div className={`change-risk risk-${changeset.risk.toLowerCase()}`}>
              {getRiskBadge(changeset.risk)}
            </div>
            <div className="change-content">
              <h4 className="change-title">{changeset.title}</h4>
              <p className="change-meta">
                영향받는 페이지: {changeset.affectedPages}개
              </p>
            </div>
            <button className="action-button small">검토하기</button>
          </div>
        ))}
      </div>
    </section>
  );
};
