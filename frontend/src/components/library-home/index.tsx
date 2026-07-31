'use client';

import React, { useState, useEffect } from 'react';
import { LibraryHomeResponse } from '@/types/library';
import { mockLibraryHomeResponse } from '@/data/mockLibraryHome';
import { TopicsSection } from './TopicsSection';
import { RecentRelationsSection } from './RecentRelationsSection';
import { OpenQuestionsSection } from './OpenQuestionsSection';
import { ContradictionsSection } from './ContradictionsSection';
import { PendingChangesSection } from './PendingChangesSection';
import { RecentActivitiesSection } from './RecentActivitiesSection';
import styles from './LibraryHome.module.css';

interface LibraryHomeProps {
  libraryId: string;
}

export const LibraryHome: React.FC<LibraryHomeProps> = ({ libraryId }) => {
  const [data, setData] = useState<LibraryHomeResponse | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchLibraryHome = async () => {
      try {
        setIsLoading(true);
        setError(null);

        // 🚀 목업 데이터로 개발 중
        // 실제 API 연동: GET /api/v1/libraries/{libraryId}
        // ASSUMPTION: 아직 실 API 엔드포인트 미사용, 목업 데이터만 사용

        // 시뮬레이션 지연 (로딩 상태 테스트)
        await new Promise(resolve => setTimeout(resolve, 500));

        // TODO: 실제 API 호출로 교체
        // const response = await fetch(`/api/v1/libraries/${libraryId}`);
        // if (!response.ok) throw new Error('Failed to fetch library home');
        // const data = await response.json();

        setData(mockLibraryHomeResponse);
      } catch (err) {
        setError(err instanceof Error ? err.message : '오류가 발생했습니다.');
      } finally {
        setIsLoading(false);
      }
    };

    fetchLibraryHome();
  }, [libraryId]);

  if (error) {
    return (
      <div className={styles.errorContainer}>
        <div className={styles.errorBox}>
          <h2>오류 발생</h2>
          <p>{error}</p>
          <button
            className={styles.retryButton}
            onClick={() => window.location.reload()}
          >
            다시 시도
          </button>
        </div>
      </div>
    );
  }

  const home = data?.data.home;

  return (
    <div className={styles.libraryHome}>
      <header className={styles.header}>
        <h1>{data?.data.name || '도서관'}</h1>
        <p className={styles.subtitle}>
          지식을 정리하고, AI와 함께 성장하세요
        </p>
      </header>

      <main className={styles.mainContent}>
        {/* 섹션 1: 주요 주제 */}
        <TopicsSection
          topics={home?.topics || []}
          isLoading={isLoading}
        />

        {/* 섹션 2: 최근 발견 연결 */}
        <RecentRelationsSection
          relations={home?.recentRelations || []}
          isLoading={isLoading}
        />

        {/* 섹션 3: 열린 질문과 지식 공백 */}
        <OpenQuestionsSection
          questions={home?.openQuestions || []}
          gaps={home?.knowledgeGaps || []}
          isLoading={isLoading}
        />

        {/* 섹션 4: 모순 */}
        <ContradictionsSection
          count={home?.openContradictionCount || 0}
          isLoading={isLoading}
        />

        {/* 섹션 5: 검토 대기 변경 */}
        <PendingChangesSection
          count={home?.pendingChangeSetCount || 0}
          isLoading={isLoading}
        />

        {/* 섹션 6: 최근 활동 */}
        <RecentActivitiesSection
          activities={home?.recentActivities || []}
          isLoading={isLoading}
        />
      </main>
    </div>
  );
};

export default LibraryHome;
