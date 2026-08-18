'use client';

import { useEffect, useState } from 'react';
import Link from 'next/link';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import { useParams } from 'next/navigation';
import { HeaderBar } from '@/components/HeaderBar';
import {
  api,
  Claim,
  Evidence,
  getApiErrorMessage,
  PageDetail,
} from '@/lib/api';
import styles from '../../../library.module.css';

const KNOWLEDGE_STATUS_LABELS: Record<string, string> = {
  VERIFIED_FACT: '검증된 사실',
  SOURCE_CLAIM: '출처의 주장',
  AI_SYNTHESIS: 'AI 종합·추론',
  USER_JUDGMENT: '사용자 판단',
  UNVERIFIED_HYPOTHESIS: '미검증 가설',
};

function locatorText(locator: Evidence['locator']) {
  if (!locator || typeof locator !== 'object' || Array.isArray(locator)) {
    return locator == null ? '' : String(locator);
  }

  return Object.entries(locator)
    .filter(([, value]) => value !== null && value !== undefined && value !== '')
    .map(([key, value]) => `${key}: ${String(value)}`)
    .join(' · ');
}

function jsonText(value: unknown) {
  if (value == null) return '';
  if (typeof value === 'string') return value;

  try {
    return JSON.stringify(value);
  } catch {
    return String(value);
  }
}

function confidenceText(confidence: Claim['confidence']) {
  if (typeof confidence !== 'number') return null;
  return `${Math.round(Math.min(1, Math.max(0, confidence)) * 100)}%`;
}

function EvidenceBlock({ evidence }: { evidence: Evidence }) {
  const locator = locatorText(evidence.locator);

  return (
    <article className={styles.evidence}>
      <div className={styles.evidenceMeta}>
        <strong>{evidence.sourceTitle}</strong>
        {evidence.evidenceType && <span>{evidence.evidenceType}</span>}
        <span>원본 버전: {evidence.sourceVersionId}</span>
        {locator && <span>{locator}</span>}
      </div>
      <p className={styles.evidenceLanguage}>
        원문 언어: <code>{evidence.excerptLanguage}</code>
      </p>
      <blockquote className={styles.excerpt}>{evidence.excerpt}</blockquote>
    </article>
  );
}

function ClaimBlock({ claim }: { claim: Claim }) {
  const confidence = confidenceText(claim.confidence);
  const evidences = claim.evidences ?? [];

  return (
    <article className={styles.claim}>
      <div className={styles.claimHeader}>
        <span className={styles.statusBadge}>
          {KNOWLEDGE_STATUS_LABELS[claim.knowledgeStatus] ?? claim.knowledgeStatus}
        </span>
        {confidence && <span className={styles.itemMeta}>신뢰도 {confidence}</span>}
      </div>
      <p className={styles.claimStatement}>{claim.statement}</p>
      {claim.conditions != null && (
        <p className={styles.itemMeta}>조건: {jsonText(claim.conditions)}</p>
      )}
      <details className={styles.evidenceDisclosure}>
        <summary>
          근거 보기 ({evidences.length}
          {typeof claim.evidenceCount === 'number' && claim.evidenceCount !== evidences.length
            ? ` / ${claim.evidenceCount}`
            : ''}
          )
        </summary>
        {evidences.length > 0 ? (
          <div className={styles.evidenceList}>
            {evidences.map((evidence) => (
              <EvidenceBlock key={evidence.id} evidence={evidence} />
            ))}
          </div>
        ) : (
          <p className={styles.empty}>연결된 근거가 없습니다.</p>
        )}
        {claim.evidenceTruncated && (
          <p className={styles.warning} role="status">
            일부 근거만 표시되었습니다. 원본 버전과 위치를 확인해 주세요.
          </p>
        )}
      </details>
    </article>
  );
}

export default function PageDetailPage() {
  const { libraryId, pageId } = useParams<{ libraryId: string; pageId: string }>();
  const [page, setPage] = useState<PageDetail | null>(null);
  const [claims, setClaims] = useState<Claim[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');
  const [reloadKey, setReloadKey] = useState(0);

  useEffect(() => {
    if (!libraryId || !pageId) return;

    let active = true;
    setIsLoading(true);
    setError('');

    Promise.all([api.getPage(libraryId, pageId), api.listClaims(libraryId, pageId)])
      .then(([pageResponse, claimsResponse]) => {
        if (!active) return;
        setPage(pageResponse.data);
        setClaims(claimsResponse.data ?? []);
      })
      .catch((requestError) => {
        if (active) setError(getApiErrorMessage(requestError));
      })
      .finally(() => {
        if (active) setIsLoading(false);
      });

    return () => {
      active = false;
    };
  }, [libraryId, pageId, reloadKey]);

  return (
    <>
      <HeaderBar />
      <main className={styles.page}>
        <div className={styles.actions}>
          <Link
            className={styles.back}
            href={`/libraries/${encodeURIComponent(libraryId)}/pages`}
          >
            페이지 목록으로
          </Link>
          <Link
            className={styles.back}
            href={`/libraries/${encodeURIComponent(libraryId)}/relations`}
          >
            관계 검토
          </Link>
          <Link
            className={styles.back}
            href={`/libraries/${encodeURIComponent(libraryId)}/contradictions`}
          >
            모순 검토
          </Link>
        </div>

        {isLoading && (
          <p className={styles.status} role="status" aria-live="polite">
            페이지와 근거를 불러오는 중입니다…
          </p>
        )}
        {!isLoading && error && (
          <div className={styles.error} role="alert">
            <p>{error}</p>
            <button
              className={styles.back}
              type="button"
              onClick={() => setReloadKey((key) => key + 1)}
            >
              다시 시도
            </button>
          </div>
        )}
        {!isLoading && !error && page && (
          <>
            <header className={styles.header}>
              <div>
                <p className={styles.breadcrumb}>{page.pageType ?? '페이지'}</p>
                <h1 className={styles.title}>{page.version.title}</h1>
                {page.version.summary && <p className={styles.muted}>{page.version.summary}</p>}
              </div>
              <span className={styles.statusBadge}>{page.status ?? '상태 미정'}</span>
            </header>

            <section className={styles.section} aria-labelledby="page-body-title">
              <h2 id="page-body-title">본문</h2>
              <div className={styles.markdown}>
                <ReactMarkdown remarkPlugins={[remarkGfm]} skipHtml>
                  {page.version.markdownBody}
                </ReactMarkdown>
              </div>
            </section>

            <section className={styles.section} aria-labelledby="claim-title">
              <h2 id="claim-title">주장과 원문 근거</h2>
              {claims.length > 0 ? (
                <div className={styles.claimList}>
                  {claims.map((claim) => (
                    <ClaimBlock key={claim.id} claim={claim} />
                  ))}
                </div>
              ) : (
                <p className={styles.empty} role="status">
                  이 페이지에 등록된 주요 주장이 없습니다.
                </p>
              )}
            </section>

            {page.relatedPages && page.relatedPages.length > 0 && (
              <section className={styles.section} aria-labelledby="related-pages-title">
                <h2 id="related-pages-title">관련 페이지와 주제</h2>
                <ul className={styles.list}>
                  {page.relatedPages.map((relatedPage) => (
                    <li key={relatedPage.id}>
                      <Link
                        className={styles.item}
                        href={`/libraries/${encodeURIComponent(libraryId)}/pages/${encodeURIComponent(relatedPage.id)}`}
                      >
                        <strong className={styles.itemTitle}>
                          {relatedPage.title ?? relatedPage.slug ?? relatedPage.id}
                        </strong>
                        {relatedPage.relationType && (
                          <span className={styles.itemMeta}>{relatedPage.relationType}</span>
                        )}
                      </Link>
                    </li>
                  ))}
                </ul>
              </section>
            )}
          </>
        )}
      </main>
    </>
  );
}
