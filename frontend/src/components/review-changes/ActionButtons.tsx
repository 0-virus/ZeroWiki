'use client';

import { useState } from 'react';
import { DecisionType } from '../../types/changes';
import styles from './ReviewChanges.module.css';

interface ActionButtonsProps {
  onDecision: (decision: DecisionType, comment?: string) => Promise<void>;
  isLoading?: boolean;
}

export function ActionButtons({ onDecision, isLoading = false }: ActionButtonsProps) {
  const [activeModal, setActiveModal] = useState<DecisionType | null>(null);
  const [comment, setComment] = useState('');

  const handleApprove = async () => {
    if (confirm('이 변경을 승인하시겠습니까?')) {
      await onDecision('APPROVE');
    }
  };

  const handleReject = () => {
    setActiveModal('REJECT');
  };

  const handleRequestChanges = () => {
    setActiveModal('REQUEST_CHANGES');
  };

  const handleDefer = async () => {
    if (confirm('판단을 보류하시겠습니까?')) {
      await onDecision('DEFER');
    }
  };

  const submitDecision = async () => {
    if (!comment.trim()) {
      alert('사유를 입력하세요 (최소 20자)');
      return;
    }
    if (comment.trim().length < 20) {
      alert('최소 20자 이상 입력하세요');
      return;
    }

    await onDecision(activeModal!, comment);
    setActiveModal(null);
    setComment('');
  };

  return (
    <>
      <div className={styles.actionButtonsContainer}>
        <div className={styles.actionButtonsHeader}>판단 영역</div>
        <div className={styles.actionButtonsGroup}>
          <button
            className={`${styles.actionButton} ${styles.approveButton}`}
            onClick={handleApprove}
            disabled={isLoading}
          >
            ✓ 승인
          </button>
          <button
            className={`${styles.actionButton} ${styles.rejectButton}`}
            onClick={handleReject}
            disabled={isLoading}
          >
            ✗ 거절
          </button>
          <button
            className={`${styles.actionButton} ${styles.requestChangesButton}`}
            onClick={handleRequestChanges}
            disabled={isLoading}
          >
            ✏️ 수정 요청
          </button>
          <button
            className={`${styles.actionButton} ${styles.deferButton}`}
            onClick={handleDefer}
            disabled={isLoading}
          >
            ⏸️ 보류
          </button>
        </div>
      </div>

      {/* REJECT 모달 */}
      {activeModal === 'REJECT' && (
        <div className={styles.modalOverlay}>
          <div className={styles.modal}>
            <h3>거절 사유를 입력하세요 (필수)</h3>
            <textarea
              className={styles.modalTextarea}
              placeholder="이 변경을 거절하는 이유를 설명해주세요..."
              value={comment}
              onChange={(e) => setComment(e.target.value)}
            />
            <div className={styles.modalActions}>
              <button className={styles.submitButton} onClick={submitDecision} disabled={isLoading}>
                거절
              </button>
              <button className={styles.cancelButton} onClick={() => setActiveModal(null)}>
                취소
              </button>
            </div>
          </div>
        </div>
      )}

      {/* REQUEST_CHANGES 모달 */}
      {activeModal === 'REQUEST_CHANGES' && (
        <div className={styles.modalOverlay}>
          <div className={styles.modal}>
            <h3>수정을 요청할 내용을 입력하세요 (필수)</h3>
            <textarea
              className={styles.modalTextarea}
              placeholder="어떤 부분을 개선해달라고 요청하시겠습니까?&#10;예: 신뢰도 높은 추가 출처를 찾아주세요"
              value={comment}
              onChange={(e) => setComment(e.target.value)}
            />
            <div className={styles.modalActions}>
              <button className={styles.submitButton} onClick={submitDecision} disabled={isLoading}>
                수정 요청
              </button>
              <button className={styles.cancelButton} onClick={() => setActiveModal(null)}>
                취소
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  );
}
