'use client';

import { useEffect, useState } from 'react';
import Link from 'next/link';
import { useParams } from 'next/navigation';
import { HeaderBar } from '@/components/HeaderBar';
import { api, ChangeSetSummary, getApiErrorMessage } from '@/lib/api';
import styles from '../../library.module.css';

function riskLabel(riskLevel: string) {
  return riskLevel === 'MAJOR' ? '주요 변경' : riskLevel;
}

function countLabel(changeSet: ChangeSetSummary) {
  const { total, minor, major } = changeSet.counts;
  return `총 ${total}건 · Minor ${minor}건 · Major ${major}건`;
}

export default function ChangeSetsPage() {
  const { libraryId } = useParams<{ libraryId: string }>();
  const [changeSets, setChangeSets] = useState<ChangeSetSummary[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');
  const [reloadKey, setReloadKey] = useState(0);

  useEffect(() => {
    if (!libraryId) return;

    let active = true;
    setIsLoading(true);
    setError('');

    api
      .listChangeSets(libraryId)
      .then((response) => {
        if (active) setChangeSets(response.data ?? []);
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
            href={`/libraries/${encodeURIComponent(libraryId)}/pages`}
          >
            페이지 목록
          </Link>
        </div>

        <header className={styles.header}>
          <div>
            <h1 className={styles.title}>변경 검토</h1>
            <p className={styles.muted}>
              검토 대기 중인 변경 세트를 확인하고 항목별 결정을 제출합니다.
            </p>
          </div>
        </header>

        <section className={styles.section} aria-labelledby="change-set-list-title">
          <h2 id="change-set-list-title">검토 대기 목록</h2>
          {isLoading && (
            <p className={styles.status} role="status" aria-live="polite">
              변경 세트를 불러오는 중입니다.
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
          {!isLoading && !error && changeSets.length === 0 && (
            <p className={styles.empty} role="status">
              검토할 변경 세트가 없습니다.
            </p>
          )}
          {!isLoading && !error && changeSets.length > 0 && (
            <ul className={styles.list}>
              {changeSets.map((changeSet) => (
                <li key={changeSet.id}>
                  <Link
                    className={styles.item}
                    href={`/libraries/${encodeURIComponent(libraryId)}/change-sets/${encodeURIComponent(changeSet.id)}`}
                  >
                    <strong className={styles.itemTitle}>{changeSet.id}</strong>
                    {changeSet.summary && (
                      <span className={styles.itemMeta}>{changeSet.summary}</span>
                    )}
                    <span className={styles.itemMeta}>
                      <span className={styles.statusBadge}>{changeSet.status}</span>{' '}
                      {riskLabel(changeSet.riskLevel)} · {countLabel(changeSet)} · revision{' '}
                      {changeSet.revision}
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
