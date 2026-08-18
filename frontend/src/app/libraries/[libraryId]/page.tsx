'use client';

import { useEffect, useState } from 'react';
import Link from 'next/link';
import { useParams } from 'next/navigation';
import { HeaderBar } from '@/components/HeaderBar';
import { api, getApiErrorMessage, LibrarySummary } from '@/lib/api';
import styles from '../library.module.css';

export default function LibraryDetailPage() {
  const { libraryId } = useParams<{ libraryId: string }>();
  const [library, setLibrary] = useState<LibrarySummary | null>(null);
  const [constitution, setConstitution] = useState<Record<string, unknown> | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    if (!libraryId) return;

    let active = true;
    setIsLoading(true);
    setError('');

    Promise.all([api.getLibrary(libraryId), api.getConstitution(libraryId)])
      .then(([libraryResponse, constitutionResponse]) => {
        if (!active) return;
        setLibrary(libraryResponse.data);
        setConstitution(constitutionResponse.data);
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
  }, [libraryId]);

  return (
    <>
      <HeaderBar />
      <main className={styles.page}>
        <div className={styles.actions}>
          <Link className={styles.back} href="/libraries">
            도서관 목록으로
          </Link>
          <Link
            className={styles.back}
            href={`/libraries/${encodeURIComponent(libraryId)}/sources`}
          >
            원본·Obsidian Import
          </Link>
          <Link
            className={styles.back}
            href={`/libraries/${encodeURIComponent(libraryId)}/ingest`}
          >
            Ingest 작업
          </Link>
          <Link
            className={styles.back}
            href={`/libraries/${encodeURIComponent(libraryId)}/pages`}
          >
            페이지
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
          <Link
            className={styles.back}
            href={`/libraries/${encodeURIComponent(libraryId)}/change-sets`}
          >
            변경 검토
          </Link>
          <Link
            className={styles.back}
            href={`/libraries/${encodeURIComponent(libraryId)}/search`}
          >
            검색
          </Link>
          <Link
            className={styles.back}
            href={`/libraries/${encodeURIComponent(libraryId)}/assistant`}
          >
            AI 사서
          </Link>
        </div>

        {isLoading && (
          <p className={styles.status} role="status" aria-live="polite">
            도서관과 운영 헌법을 불러오는 중입니다…
          </p>
        )}
        {!isLoading && error && (
          <p className={styles.error} role="alert">
            {error}
          </p>
        )}
        {!isLoading && !error && library && (
          <>
            <header className={styles.header}>
              <div>
                <h1 className={styles.title}>{library.name}</h1>
                {library.description && <p className={styles.muted}>{library.description}</p>}
              </div>
            </header>

            <section className={styles.section} aria-labelledby="library-summary-title">
              <h2 id="library-summary-title">도서관 정보</h2>
              <dl className={styles.detailGrid}>
                {library.visibility && (
                  <div className={styles.detailCell}>
                    <dt>공개 범위</dt>
                    <dd>{library.visibility}</dd>
                  </div>
                )}
                {library.status && (
                  <div className={styles.detailCell}>
                    <dt>상태</dt>
                    <dd>{library.status}</dd>
                  </div>
                )}
                {typeof library.constitutionVersion === 'number' && (
                  <div className={styles.detailCell}>
                    <dt>헌법 버전</dt>
                    <dd>{library.constitutionVersion}</dd>
                  </div>
                )}
                {typeof library.currentVersion?.versionNo === 'number' && (
                  <div className={styles.detailCell}>
                    <dt>현재 발행 버전</dt>
                    <dd>{library.currentVersion.versionNo}</dd>
                  </div>
                )}
              </dl>
            </section>

            <section className={styles.section} aria-labelledby="constitution-title">
              <h2 id="constitution-title">현재 운영 헌법</h2>
              {constitution && (
                <pre className={styles.constitution}>
                  {JSON.stringify(constitution, null, 2)}
                </pre>
              )}
              {!constitution && (
                <p className={styles.empty} role="status">
                  현재 운영 헌법이 없습니다.
                </p>
              )}
            </section>
          </>
        )}
      </main>
    </>
  );
}
