'use client';

import { useState } from 'react';
import { ChangeSet, DecisionType, ReviewDecision } from '../../types/changes';
import { DiffViewer } from './DiffViewer';
import { ActionButtons } from './ActionButtons';
import { RegenerationWaiting } from './RegenerationWaiting';
import { ExcerptBlock } from '../excerpts/ExcerptBlock';
import { mockChangeSet, mockChangeItemDiffData } from '../../data/mockReviewChanges';
import styles from './ReviewChanges.module.css';

export function ReviewChangesScreen() {
  const [changeSet] = useState<ChangeSet>(mockChangeSet);
  const [selectedItemIndex, setSelectedItemIndex] = useState(0);
  const [isLoading, setIsLoading] = useState(false);
  const [regenerationWaiting, setRegenerationWaiting] = useState(false);
  const [regenerationComment, setRegenerationComment] = useState('');

  const selectedItem = changeSet.items?.[selectedItemIndex];
  const diffData = mockChangeItemDiffData.ci1;

  const handleDecision = async (decision: DecisionType, comment?: string) => {
    setIsLoading(true);

    try {
      // 목업: API 호출 시뮬레이션
      await new Promise((resolve) => setTimeout(resolve, 1000));

      if (decision === 'REQUEST_CHANGES' && comment) {
        setRegenerationWaiting(true);
        setRegenerationComment(comment);
      } else {
        // 다른 결정: 성공 메시지 표시
        alert(`${decision} 처리되었습니다`);
      }
    } finally {
      setIsLoading(false);
    }
  };

  if (regenerationWaiting) {
    return (
      <RegenerationWaiting
        comment={regenerationComment}
        onNewChangeReady={() => {
          alert('새 변경이 로드되었습니다');
          setRegenerationWaiting(false);
        }}
        onCancel={() => setRegenerationWaiting(false)}
      />
    );
  }

  return (
    <div className={styles.reviewScreenContainer}>
      {/* 헤더 */}
      <div className={styles.reviewHeader}>
        <div className={styles.headerTitle}>
          <h1>변경 검토: {changeSet.title}</h1>
        </div>
        <div className={styles.headerMeta}>
          <span className={styles.riskBadge} data-level={changeSet.riskLevel}>
            위험도: {changeSet.riskLevel === 'MAJOR' ? '높음' : '낮음'}
          </span>
          <span className={styles.statusBadge}>상태: 검토 중</span>
        </div>
      </div>

      {/* 변경 이유 */}
      <section className={styles.section}>
        <h2>📝 변경 이유</h2>
        <p className={styles.summaryBox}>{changeSet.summary}</p>
      </section>

      {/* 변경 항목 선택 */}
      {changeSet.items && changeSet.items.length > 1 && (
        <section className={styles.section}>
          <h2>변경 항목 ({changeSet.items.length}개)</h2>
          <div className={styles.itemTabs}>
            {changeSet.items.map((item, idx) => (
              <button
                key={idx}
                className={`${styles.itemTab} ${selectedItemIndex === idx ? styles.active : ''}`}
                onClick={() => setSelectedItemIndex(idx)}
              >
                {item.targetType} - {item.targetName}
                <span className={styles.riskBadgeSmall} data-level={item.riskLevel}>
                  {item.riskLevel}
                </span>
              </button>
            ))}
          </div>
        </section>
      )}

      {/* Diff 뷰어 */}
      {selectedItem && (
        <>
          <section className={styles.section}>
            <DiffViewer
              title={selectedItem.targetName}
              beforeBody={
                typeof selectedItem.beforeSnapshot.markdownBody === 'string'
                  ? selectedItem.beforeSnapshot.markdownBody
                  : diffData.beforeBody
              }
              afterBody={
                typeof selectedItem.afterSnapshot.markdownBody === 'string'
                  ? selectedItem.afterSnapshot.markdownBody
                  : diffData.afterBody
              }
            />
          </section>

          {/* 근거 출처 */}
          {selectedItem.evidenceSummary && selectedItem.evidenceSummary.length > 0 && (
            <section className={styles.section}>
              <h2>🔗 근거 출처</h2>
              <div className={styles.evidenceList}>
                {selectedItem.evidenceSummary.map((evidence, idx) => (
                  <ExcerptBlock
                    key={idx}
                    excerpt={evidence.excerpt}
                    language={evidence.excerptLanguage}
                    sourceTitle={evidence.sourceTitle}
                    confidence={evidence.confidence}
                    sourceUrl={evidence.url}
                  />
                ))}
              </div>
            </section>
          )}

          {/* AI 신뢰도 */}
          <section className={styles.section}>
            <h2>🤖 AI 신뢰도</h2>
            <div className={styles.confidenceBox}>
              <div className={styles.confidenceBar}>
                <div
                  className={styles.confidenceProgress}
                  style={{ width: `${selectedItem.aiConfidence * 100}%` }}
                />
              </div>
              <p className={styles.confidenceText}>
                {Math.round(selectedItem.aiConfidence * 100)}%
              </p>
              <p className={styles.confidenceDesc}>
                이 변경은 제공된 근거에 의해 충분히 지지됩니다
              </p>
            </div>
          </section>
        </>
      )}

      {/* 판단 버튼 */}
      <ActionButtons onDecision={handleDecision} isLoading={isLoading} />
    </div>
  );
}
