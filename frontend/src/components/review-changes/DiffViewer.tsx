'use client';

import { useMemo } from 'react';
import { diffLines, Change } from 'diff';
import styles from './ReviewChanges.module.css';

interface DiffViewerProps {
  title: string;
  beforeBody: string;
  afterBody: string;
}

export function DiffViewer({ title, beforeBody, afterBody }: DiffViewerProps) {
  const diffResult = useMemo(() => {
    return diffLines(beforeBody, afterBody);
  }, [beforeBody, afterBody]);

  const addedCount = diffResult.filter((d) => d.added).length;
  const removedCount = diffResult.filter((d) => d.removed).length;
  const changedCount = Math.min(addedCount, removedCount);

  return (
    <div className={styles.diffViewerContainer}>
      <div className={styles.diffHeader}>
        <h3 className={styles.diffTitle}>{title}</h3>
        <div className={styles.diffStats}>
          라인 변경: {changedCount}개 (추가 {addedCount}개 / 삭제 {removedCount}개)
        </div>
      </div>

      <div className={styles.diffContent}>
        {diffResult.map((part, idx) => {
          let className = styles.diffUnchanged;
          let prefix = '  ';

          if (part.added) {
            className = styles.diffAdded;
            prefix = '+ ';
          } else if (part.removed) {
            className = styles.diffRemoved;
            prefix = '- ';
          }

          return (
            <div key={idx} className={`${styles.diffLine} ${className}`}>
              <span className={styles.diffPrefix}>{prefix}</span>
              <code className={styles.diffText}>{part.value}</code>
            </div>
          );
        })}
      </div>
    </div>
  );
}
