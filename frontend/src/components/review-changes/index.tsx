'use client';

import { useState, useMemo } from 'react';
import {
  ChangeSet,
  ChangeItem,
  DecisionType,
  FilterTab,
  LocalDecisionState,
} from '../../types/changes';
import { mockChangeSet } from '../../data/mockReviewChanges';
import styles from './ReviewChanges.module.css';
import { FilterTabs } from './FilterTabs';
import { AccordionList } from './AccordionList';
import { ProgressBar } from './ProgressBar';

export function ReviewChangesScreen() {
  const [changeSet] = useState<ChangeSet>(mockChangeSet);
  const [filterTab, setFilterTab] = useState<FilterTab>('ALL');
  const [expandedItemId, setExpandedItemId] = useState<string | null>(
    changeSet.items?.[0]?.changeItemId || null
  );
  const [decisions, setDecisions] = useState<LocalDecisionState>({});
  const [isSubmitting, setIsSubmitting] = useState(false);

  // 필터링된 항목 목록 (API 계약: MINOR/MAJOR)
  const filteredItems = useMemo(() => {
    if (!changeSet.items) return [];
    if (filterTab === 'ALL') return changeSet.items;

    return changeSet.items.filter((item) => {
      if (filterTab === 'SAFE') return item.riskLevel === 'MINOR';
      if (filterTab === 'DANGER') return item.riskLevel === 'MAJOR';
      return true;
    });
  }, [changeSet.items, filterTab]);

  // 판단 완료 카운트
  const decisionCount = Object.keys(decisions).length;
  const totalCount = changeSet.items?.length || 0;

  // 위험도별 카운트 (2단계: 안전/위험)
  const safeCount = changeSet.counts?.safe || 0;
  const dangerCount = changeSet.counts?.danger || 0;

  // 판단 처리
  const handleDecision = (changeItemId: string, decision: DecisionType, comment?: string) => {
    setDecisions((prev) => ({
      ...prev,
      [changeItemId]: { decision, comment },
    }));
  };

  // 일괄 승인 (SAFE/MINOR 항목만)
  const handleBatchApproveSafe = () => {
    const batchDecisions: LocalDecisionState = {};
    changeSet.items?.forEach((item) => {
      if (item.riskLevel === 'MINOR') {
        batchDecisions[item.changeItemId] = { decision: 'APPROVE' };
      }
    });
    setDecisions((prev) => ({ ...prev, ...batchDecisions }));
  };

  // 적용 버튼 (모든 판단을 API로 제출)
  const handleApply = async () => {
    setIsSubmitting(true);
    try {
      // 목업: 각 결정을 순차 호출
      for (const [itemId, decision] of Object.entries(decisions)) {
        await new Promise((resolve) => setTimeout(resolve, 200));
        console.log(`POST /reviews: ${itemId}`, decision);
      }
      alert(`${decisionCount}개 항목 판단이 제출되었습니다`);
      setDecisions({});
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className={styles.reviewScreenContainer}>
      {/* 헤더 */}
      <div className={styles.reviewHeader}>
        <div className={styles.headerBreadcrumb}>
          <span>도서관</span> <span>/</span> <span>변경 세트 #{changeSet.id.split('-')[1]}</span>
        </div>
        <h1 className={styles.headerTitle}>{changeSet.title}</h1>
        <div className={styles.headerMeta}>
          <span className={styles.statusBadge}>준비 완료</span>
          <span className={styles.versionInfo}>
            v{changeSet.baseLibraryVersionNo} → v{changeSet.baseLibraryVersionNo + 1} (총{' '}
            {totalCount}건)
          </span>
        </div>
      </div>

      {/* 변경 이유 */}
      <section className={styles.section}>
        <h2 className={styles.sectionTitle}>📝 변경 이유</h2>
        <p className={styles.summaryBox}>{changeSet.summary}</p>
      </section>

      {/* 필터 탭 + 일괄 승인 */}
      <div className={styles.filterBar}>
        <FilterTabs
          filterTab={filterTab}
          onFilterChange={setFilterTab}
          counts={{ safe: safeCount, danger: dangerCount }}
        />
        {safeCount > 0 && (
          <button className={styles.batchApproveBtn} onClick={handleBatchApproveSafe}>
            안전 {safeCount}건 일괄 승인
          </button>
        )}
      </div>

      {/* 아코디언 목록 */}
      {filteredItems.length > 0 ? (
        <AccordionList
          items={filteredItems}
          expandedItemId={expandedItemId}
          onExpandChange={setExpandedItemId}
          decisions={decisions}
          onDecision={handleDecision}
        />
      ) : (
        <div className={styles.emptyState}>
          <p>항목이 없습니다.</p>
        </div>
      )}

      {/* 진행 바 */}
      <ProgressBar completed={decisionCount} total={totalCount} />

      {/* 하단 액션 */}
      <div className={styles.bottomActions}>
        <button className={styles.deferBtn} disabled={isSubmitting}>
          나중에
        </button>
        <button
          className={styles.applyBtn}
          onClick={handleApply}
          disabled={decisionCount === 0 || isSubmitting}
        >
          {isSubmitting ? '제출 중...' : '승인 항목 적용'}
        </button>
      </div>
    </div>
  );
}
