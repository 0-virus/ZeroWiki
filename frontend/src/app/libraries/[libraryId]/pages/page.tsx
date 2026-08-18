'use client';

import { useEffect, useState } from 'react';
import Link from 'next/link';
import { useParams } from 'next/navigation';
import { HeaderBar } from '@/components/HeaderBar';
import { api, getApiErrorMessage, PageSummary } from '@/lib/api';
import styles from '../../library.module.css';

function pageTitle(page: PageSummary) {
  return page.version?.title ?? page.slug ?? page.id;
}

function topicLabels(topics: unknown) {
  if (!Array.isArray(topics)) return [];
  return topics.filter((topic): topic is string => typeof topic === 'string');
}

export default function PagesPage() {
  const { libraryId } = useParams<{ libraryId: string }>();
  const [pages, setPages] = useState<PageSummary[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');
  const [reloadKey, setReloadKey] = useState(0);

  useEffect(() => {
    if (!libraryId) return;

    let active = true;
    setIsLoading(true);
    setError('');

    api
      .listPages(libraryId)
      .then((response) => {
        if (active) setPages(response.data ?? []);
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
  }, [libraryId, reloadKey]);

  return (
    <>
      <HeaderBar />
      <main className={styles.page}>
        <div className={styles.actions}>
          <Link className={styles.back} href={`/libraries/${encodeURIComponent(libraryId)}`}>
            도서관으로
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

        <header className={styles.header}>
          <div>
            <h1 className={styles.title}>페이지</h1>
            <p className={styles.muted}>도서관에 발행된 지식 페이지를 탐색합니다.</p>
          </div>
        </header>

        <section className={styles.section} aria-labelledby="page-list-title">
          <h2 id="page-list-title">페이지 목록</h2>
          {isLoading && (
            <p className={styles.status} role="status" aria-live="polite">
              페이지 목록을 불러오는 중입니다…
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
          {!isLoading && !error && pages.length === 0 && (
            <p className={styles.empty} role="status">
              아직 발행된 페이지가 없습니다.
            </p>
          )}
          {!isLoading && !error && pages.length > 0 && (
            <ul className={styles.list}>
              {pages.map((page) => (
                <li key={page.id}>
                  <Link
                    className={styles.item}
                    href={`/libraries/${encodeURIComponent(libraryId)}/pages/${encodeURIComponent(page.id)}`}
                  >
                    <strong className={styles.itemTitle}>{pageTitle(page)}</strong>
                    {page.version?.summary && (
                      <span className={styles.itemMeta}>{page.version.summary}</span>
                    )}
                    <span className={styles.itemMeta}>
                      {[page.pageType, page.status, ...topicLabels(page.topics)]
                        .filter(Boolean)
                        .join(' · ')}
                    </span>
                  </Link>
                </li>
              ))}
            </ul>
          )}
        </section>
      </main>
    </>
  );
}
