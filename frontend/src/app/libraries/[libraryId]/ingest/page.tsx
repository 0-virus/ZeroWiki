'use client';

import { useEffect, useState } from 'react';
import Link from 'next/link';
import { useParams } from 'next/navigation';
import { HeaderBar } from '@/components/HeaderBar';
import { api, getApiErrorMessage, IngestJob } from '@/lib/api';
import { INGEST_STATUS_LABELS, progressPercent } from '@/lib/ingest';
import styles from '../../library.module.css';

export default function IngestJobsPage() {
  const { libraryId } = useParams<{ libraryId: string }>();
  const [jobs, setJobs] = useState<IngestJob[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');
  const [reloadKey, setReloadKey] = useState(0);

  useEffect(() => {
    if (!libraryId) return;

    let active = true;
    setIsLoading(true);
    setError('');

    api
      .listIngestJobs(libraryId)
      .then((response) => {
        if (active) setJobs(response.data ?? []);
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
            href={`/libraries/${encodeURIComponent(libraryId)}/sources`}
          >
            원본·Obsidian Import
          </Link>
        </div>

        <header className={styles.header}>
          <div>
            <h1 className={styles.title}>Ingest 작업</h1>
            <p className={styles.muted}>진행 중인 Import와 처리 결과를 확인합니다.</p>
          </div>
        </header>

        <section className={styles.section} aria-labelledby="ingest-list-title">
          <h2 id="ingest-list-title">작업 목록</h2>
          {isLoading && (
            <p className={styles.status} role="status" aria-live="polite">
              Ingest 작업을 불러오는 중입니다…
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
          {!isLoading && !error && jobs.length === 0 && (
            <p className={styles.empty} role="status">
              아직 Ingest 작업이 없습니다.
            </p>
          )}
          {!isLoading && !error && jobs.length > 0 && (
            <ul className={styles.list}>
              {jobs.map((job) => {
                const percent = progressPercent(job);
                return (
                  <li key={job.id}>
                    <Link
                      className={styles.item}
                      href={`/libraries/${encodeURIComponent(libraryId)}/ingest/${encodeURIComponent(job.id)}`}
                    >
                      <span className={styles.itemTitle}>
                        {job.importType ?? 'Import'}
                      </span>
                      <span className={styles.statusBadge}>
                        {INGEST_STATUS_LABELS[job.status] ?? job.status}
                      </span>
                      {percent !== null && (
                        <>
                          <div
                            className={styles.progressTrack}
                            role="progressbar"
                            aria-label="Ingest 진행률"
                            aria-valuemin={0}
                            aria-valuemax={100}
                            aria-valuenow={percent}
                          >
                            <div
                              className={styles.progressBar}
                              style={{ width: `${percent}%` }}
                            />
                          </div>
                          <span className={styles.itemMeta}>{percent}%</span>
                        </>
                      )}
                    </Link>
                  </li>
                );
              })}
            </ul>
          )}
        </section>
      </main>
    </>
  );
}
