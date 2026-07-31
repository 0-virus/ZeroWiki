'use client';

import { useEffect, useState } from 'react';
import styles from './ReviewChanges.module.css';

interface RegenerationWaitingProps {
  comment: string;
  onNewChangeReady: () => void;
  onCancel: () => void;
}

export function RegenerationWaiting({
  comment,
  onNewChangeReady,
  onCancel,
}: RegenerationWaitingProps) {
  const [elapsed, setElapsed] = useState(0);
  const [isNewChangeReady, setIsNewChangeReady] = useState(false);

  useEffect(() => {
    // Polling 시뮬레이션: 3초 후 새 변경 준비됨
    const timer = setTimeout(() => {
      setIsNewChangeReady(true);
    }, 3000);

    return () => clearTimeout(timer);
  }, []);

  useEffect(() => {
    const interval = setInterval(() => {
      setElapsed((prev) => prev + 1);
    }, 1000);

    return () => clearInterval(interval);
  }, []);

  // 갭 #23 확정 (2026-07-30, 리더 기술 확정):
  // - polling: GET /change-sets/{changeSetId}/items (2초 간격)
  // - 완료 감지: change_items[].review_status 필드 전이 (REVISION_REQUESTED → PENDING)
  // - changeSetId: 불변 (새 항목 생성 안 함, after_snapshot만 갱신)
  // 근거: docs/ZeroWiki-API-명세-초안.md 8.1절, docs/검증-백엔드-ERD-API-정합성.md 9절

  return (
    <div className={styles.regenerationContainer}>
      <div className={styles.regenerationContent}>
        <h2>⏳ 재생성 대기 중</h2>

        <div className={styles.regenerationInfo}>
          <div className={styles.infoSection}>
            <h3>당신의 요청:</h3>
            <p className={styles.commentBox}>{comment}</p>
          </div>

          <div className={styles.infoSection}>
            <h3>AI가 변경을 다시 생성하고 있습니다</h3>
            <div className={styles.spinner}>⟳</div>
            <p>경과 시간: {elapsed}초</p>
            <p className={styles.estimatedTime}>예상 시간: 1~3분</p>
          </div>

          {isNewChangeReady && (
            <div className={styles.readyNotification}>
              <span className={styles.readyBadge}>새 변경 준비됨</span>
              <p>새로운 변경이 준비되었습니다. 확인하려면 버튼을 클릭하세요.</p>
            </div>
          )}
        </div>

        <div className={styles.regenerationActions}>
          <button
            className={styles.checkButton}
            onClick={onNewChangeReady}
            disabled={!isNewChangeReady}
          >
            {isNewChangeReady ? '[새 변경 확인]' : '대기 중...'}
          </button>
          <button className={styles.cancelButton} onClick={onCancel}>
            [취소]
          </button>
        </div>
      </div>
    </div>
  );
}
