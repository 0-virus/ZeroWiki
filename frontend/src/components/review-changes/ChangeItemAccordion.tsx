'use client';

import { useState, useMemo } from 'react';
import { ChangeItem, DecisionType } from '../../types/changes';
import { DiffViewer } from './DiffViewer';
import { ActionButtons } from './ActionButtons';
import { ExcerptBlock } from '../excerpts/ExcerptBlock';
import { mockChangeItemDiffData } from '../../data/mockReviewChanges';
import styles from './ReviewChanges.module.css';

interface ChangeItemAccordionProps {
  item: ChangeItem;
  isExpanded: boolean;
  onToggle: () => void;
  decision?: { decision: DecisionType; comment?: string };
  onDecision: (decision: DecisionType, comment?: string) => void;
}

const DECISION_LABELS: Record<DecisionType, string> = {
  APPROVE: '✓ 승인',
  REJECT: '✗ 거절',
  REQUEST_CHANGES: '± 수정 요청',
  DEFER: '… 보류',
};

export function ChangeItemAccordion({
  item,
  isExpanded,
  onToggle,
  decision,
  onDecision,
}: ChangeItemAccordionProps) {
  const [isLoading, setIsLoading] = useState(false);
  const diffData = useMemo(
    () => mockChangeItemDiffData.ci1,
    []
  );

  const handleDecision = async (dec: DecisionType, comment?: string) => {
    setIsLoading(true);
    try {
      await new Promise((resolve) => setTimeout(resolve, 500));
      onDecision(dec, comment);
    } finally {
      setIsLoading(false);
    }
  };

  // API 계약: MINOR/MAJOR 직접 매핑
  const getRiskColor = (risk: 'MINOR' | 'MAJOR') => {
    switch (risk) {
      case 'MINOR':
        return '#22c55e'; // 녹색 (안전)
      case 'MAJOR':
        return '#ef4444'; // 빨강 (위험)
    }
  };

  const getRiskLabel = (risk: 'MINOR' | 'MAJOR') => {
    switch (risk) {
      case 'MINOR':
        return '안전';
      case 'MAJOR':
        return '위험';
    }
  };

  return (
    <div className={styles.accordionItem}>
      {/* 아코디언 헤더 */}
      <button className={styles.accordionHeader} onClick={onToggle}>
        <span className={styles.accordionCaret}>{isExpanded ? '▼' : '▶'}</span>

        <div className={styles.accordionContent}>
          <span className={styles.accordionTitle}>{item.targetName}</span>

          <span className={styles.accordionMeta}>
            <span
              className={styles.riskBadge}
              style={{ backgroundColor: getRiskColor(item.riskLevel), color: 'white' }}
            >
              {getRiskLabel(item.riskLevel)}
            </span>

            <span className={styles.operationType}>
              {item.operation === 'CREATE' && '새 항목'}
              {item.operation === 'UPDATE' && '수정'}
              {item.operation === 'DELETE' && '삭제'}
            </span>

            {decision && (
              <span className={styles.decisionBadge} data-decision={decision.decision}>
                {DECISION_LABELS[decision.decision]}
              </span>
            )}
          </span>
        </div>
      </button>

      {/* 아코디언 본문 */}
      {isExpanded && (
        <div className={styles.accordionBody}>
          {/* 변경 이유 */}
          <section className={styles.accordionSection}>
            <h3 className={styles.accordionSectionTitle}>변경 이유</h3>
            <p className={styles.reasonText}>{item.reason}</p>
          </section>

          {/* Diff */}
          <section className={styles.accordionSection}>
            <DiffViewer
              title={item.targetName}
              beforeBody={
                typeof item.beforeSnapshot.markdownBody === 'string'
                  ? item.beforeSnapshot.markdownBody
                  : diffData.beforeBody
              }
              afterBody={
                typeof item.afterSnapshot.markdownBody === 'string'
                  ? item.afterSnapshot.markdownBody
                  : diffData.afterBody
              }
            />
          </section>

          {/* 근거 */}
          {item.evidenceSummary && item.evidenceSummary.length > 0 && (
            <section className={styles.accordionSection}>
              <h3 className={styles.accordionSectionTitle}>🔗 근거 ({item.evidenceSummary.length}개)</h3>
              <div className={styles.evidenceList}>
                {item.evidenceSummary.map((evidence, idx) => (
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
          <section className={styles.accordionSection}>
            <h3 className={styles.accordionSectionTitle}>🤖 AI 신뢰도</h3>
            <div className={styles.confidenceBox}>
              <div className={styles.confidenceBar}>
                <div
                  className={styles.confidenceProgress}
                  style={{ width: `${item.aiConfidence * 100}%` }}
                />
              </div>
              <p className={styles.confidenceText}>{Math.round(item.aiConfidence * 100)}%</p>
            </div>
          </section>

          {/* 판단 버튼 */}
          <ActionButtons onDecision={handleDecision} isLoading={isLoading} />
        </div>
      )}
    </div>
  );
}
