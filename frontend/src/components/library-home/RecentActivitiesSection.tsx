import React from 'react';
import { RecentActivity } from '@/types/library';
import {
  MdDownload,
  MdCheckCircle,
  MdFeedback,
  MdAddCircle,
  MdWarning,
  MdApproval,
} from 'react-icons/md';

interface RecentActivitiesSectionProps {
  activities: RecentActivity[];
  isLoading?: boolean;
}

export const RecentActivitiesSection: React.FC<RecentActivitiesSectionProps> = ({
  activities,
  isLoading = false
}) => {
  const getActivityIcon = (type: string): React.ReactNode => {
    switch (type) {
      case 'INGEST':
        return <MdDownload className="activity-icon" title="파일 업로드" />;
      case 'RELATION':
        return <MdAddCircle className="activity-icon" title="관계 생성" />;
      case 'LINT':
        return <MdFeedback className="activity-icon" title="Lint 검사" />;
      case 'CONTRADICTION':
        return <MdWarning className="activity-icon" title="모순 발견" />;
      case 'CHANGE_SET':
        return <MdCheckCircle className="activity-icon" title="변경 세트" />;
      case 'APPROVAL':
        return <MdApproval className="activity-icon" title="승인" />;
      default:
        return <MdCheckCircle className="activity-icon" />;
    }
  };

  const formatTime = (dateStr: string): string => {
    try {
      const date = new Date(dateStr);
      const now = new Date();
      const diffMs = now.getTime() - date.getTime();
      const diffMins = Math.floor(diffMs / 60000);
      const diffHours = Math.floor(diffMs / 3600000);
      const diffDays = Math.floor(diffMs / 86400000);

      if (diffMins < 1) return '방금 전';
      if (diffMins < 60) return `${diffMins}분 전`;
      if (diffHours < 24) return `${diffHours}시간 전`;
      if (diffDays < 7) return `${diffDays}일 전`;

      return date.toLocaleDateString('ko-KR');
    } catch {
      return dateStr;
    }
  };

  if (isLoading) {
    return (
      <section className="activities-section-skeleton">
        <h2 className="section-title">최근 활동</h2>
        <div className="activities-list">
          {[1, 2, 3, 4, 5].map((i) => (
            <div key={i} className="activity-item-skeleton">
              <div className="skeleton-icon" />
              <div className="skeleton-text skeleton-main" />
              <div className="skeleton-text skeleton-sub" />
            </div>
          ))}
        </div>
      </section>
    );
  }

  if (activities.length === 0) {
    return (
      <section className="activities-section-empty">
        <h2 className="section-title">최근 활동</h2>
        <div className="empty-state">
          <p className="empty-text">
            아직 활동이 없습니다.
          </p>
        </div>
      </section>
    );
  }

  return (
    <section className="activities-section">
      <div className="section-header">
        <h2 className="section-title">최근 활동</h2>
        <a href="#" className="more-link">
          더 보기 →
        </a>
      </div>
      <div className="activities-list">
        {activities.slice(0, 10).map((activity, index) => (
          <div key={index} className="activity-item">
            <div className="activity-icon-wrapper">
              {getActivityIcon(activity.type)}
            </div>
            <div className="activity-content">
              <p className="activity-title">{activity.title}</p>
              <p className="activity-time">{formatTime(activity.createdAt)}</p>
            </div>
          </div>
        ))}
      </div>
    </section>
  );
};
