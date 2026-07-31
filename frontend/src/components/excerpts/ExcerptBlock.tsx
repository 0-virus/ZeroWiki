'use client';

import styles from './ExcerptBlock.module.css';

interface ExcerptBlockProps {
  excerpt: string;
  language: string; // BCP 47: "en", "ko", etc.
  sourceTitle: string;
  confidence: number; // 0~1.0
  sourceUrl?: string;
}

const languageLabels: Record<string, string> = {
  en: 'English',
  ko: '한국어',
  fr: 'Français',
  ja: '日本語',
  zh: '中文',
  de: 'Deutsch',
  es: 'Español',
};

function getLanguageLabel(lang: string): string {
  return languageLabels[lang] || lang.toUpperCase();
}

function getConfidenceColor(confidence: number): string {
  if (confidence >= 0.9) return '#4CAF50'; // 초록색
  if (confidence >= 0.7) return '#FFC107'; // 노란색
  return '#FF9800'; // 주황색
}

export function ExcerptBlock({
  excerpt,
  language,
  sourceTitle,
  confidence,
  sourceUrl,
}: ExcerptBlockProps) {
  const languageLabel = getLanguageLabel(language);
  const confusedPercent = Math.round(confidence * 100);

  return (
    <div className={styles.excerptContainer}>
      <div className={styles.excerptMeta}>
        <div className={styles.sourceInfo}>
          <span className={styles.sourceTitle}>{sourceTitle}</span>
          {sourceUrl && (
            <a href={sourceUrl} target="_blank" rel="noopener noreferrer" className={styles.sourceLink}>
              🔗
            </a>
          )}
        </div>
      </div>

      <div className={styles.excerptContent}>
        <div className={styles.languageBadge}>[{languageLabel}]</div>
        <p className={styles.excerptText}>{excerpt}</p>
      </div>

      <div className={styles.excerptFooter}>
        <div className={styles.confidenceBar}>
          <div
            className={styles.confidenceLevel}
            style={{ width: `${confidence * 100}%`, backgroundColor: getConfidenceColor(confidence) }}
          />
        </div>
        <span className={styles.confidenceValue}>신뢰도: {confusedPercent}%</span>
      </div>
    </div>
  );
}
