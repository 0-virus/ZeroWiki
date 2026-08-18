'use client';

import { useEffect, useState } from 'react';
import Link from 'next/link';
import { useParams } from 'next/navigation';
import { HeaderBar } from '@/components/HeaderBar';
import { api, getApiErrorMessage, IngestJob } from '@/lib/api';
import {
  INGEST_NEXT_ACTION_LABELS,
  INGEST_STATUS_LABELS,
  isPollingIngestStatus,
  progressPercent,
} from '@/lib/ingest';
import styles from '../../../library.module.css';

function retryAfterSeconds(response: Response) {
  const value = Number(response.headers.get('Retry-After'));
  return Number.isFinite(value) && value >= 0 ? Math.max(1, Math.ceil(value)) : 2;
}

export default function IngestJobDetailPage() {
  const { libraryId, jobId } = useParams<{ libraryId: string; jobId: string }>();
  const [job, setJob] = useState<IngestJob | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isRetrying, setIsRetrying] = useState(false);
  const [error, setError] = useState('');
  const [reloadKey, setReloadKey] = useState(0);

  useEffect(() => {
    if (!libraryId || !jobId) return;

    let active = true;
    let timer: number | undefined;
    setIsLoading(true);
    setError('');

    async function loadJob() {
      let nextDelay = 2;
      try {
        const response = await api.getIngestJob(libraryId, jobId, (apiResponse) => {
          nextDelay = retryAfterSeconds(apiResponse);
        });
        if (!active) return;
        setJob(response.data);
        setIsLoading(false);

        if (isPollingIngestStatus(response.data.status)) {
          timer = window.setTimeout(loadJob, nextDelay * 1000);
        }
      } catch (requestError) {
        if (!active) return;
        setError(getApiErrorMessage(requestError));
        setIsLoading(false);
      }
    }

    loadJob();

    return () => {
      active = false;
      if (timer !== undefined) window.clearTimeout(timer);
    };
  }, [libraryId, jobId, reloadKey]);

  async function handleRetry() {
    if (!libraryId || !jobId) return;
    setIsRetrying(true);
    setError('');
    try {
      await api.retryIngestJob(libraryId, jobId);
      setReloadKey((key) => key + 1);
    } catch (requestError) {
      setError(getApiErrorMessage(requestError));
    } finally {
      setIsRetrying(false);
    }
  }

  const percent = job ? progressPercent(job) : null;
  const statusLabel = job
    ? INGEST_STATUS_LABELS[job.status] ?? job.status
    : '확인 중';

  return (
    <>
      <HeaderBar />
      <main className={styles.page}>
        <div className={styles.actions}>
          <Link
            className={styles.back}
            href={`/libraries/${encodeURIComponent(libraryId)}/ingest`}
          >
            작업 목록으로
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
            <h1 className={styles.title}>Ingest 진행</h1>
            <p className={styles.muted}>서버가 안내하는 간격으로 상태를 갱신합니다.</p>
          </div>
          {job && <span className={styles.statusBadge}>{statusLabel}</span>}
        </header>

        {isLoading && !job && (
          <p className={styles.status} role="status" aria-live="polite">
            Ingest 상태를 불러오는 중입니다…
          </p>
        )}
        {error && (
          <div className={styles.error} role="alert">
            <p>{error}</p>
            <button className={styles.back} type="button" onClick={() => setReloadKey((key) => key + 1)}>
              다시 시도
            </button>
          </div>
        )}

        {job && (
          <>
            <section className={styles.section} aria-labelledby="ingest-status-title">
              <h2 id="ingest-status-title">현재 상태</h2>
              <p className={styles.notice} role="status" aria-live="polite">
                {statusLabel}
                {job.nextAction && (
                  <> · 다음 단계: {INGEST_NEXT_ACTION_LABELS[job.nextAction] ?? job.nextAction}</>
                )}
              </p>
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
                    <div className={styles.progressBar} style={{ width: `${percent}%` }} />
                  </div>
                  <p className={styles.muted}>{percent}% 완료</p>
                </>
              )}
              {job.progress &&
                typeof job.progress.completed === 'number' &&
                typeof job.progress.total === 'number' && (
                  <p className={styles.muted}>
                    문서 {job.progress.completed} / {job.progress.total}
                  </p>
                )}
              {job.status === 'PAUSED_QUOTA' && (
                <p className={styles.warning} role="status">
                  처리량이 부족해 작업이 보류되었습니다. 구매 후 작업을 재개할 수 있습니다.
                </p>
              )}
              {job.status === 'FAILED' && (
                <div className={styles.error} role="alert">
                  <p>작업이 실패했습니다. 실패 항목을 다시 시도할 수 있습니다.</p>
                  <button
                    className={styles.submit}
                    type="button"
                    onClick={handleRetry}
                    disabled={isRetrying}
                  >
                    {isRetrying ? '재시도 요청 중…' : '실패 항목 재시도'}
                  </button>
                </div>
              )}
            </section>

            {job.plan && (
              <section className={styles.section} aria-labelledby="ingest-plan-title">
                <h2 id="ingest-plan-title">처리 계획</h2>
                <dl className={styles.planGrid}>
                  {typeof job.plan.documents === 'number' && (
                    <div className={styles.detailCell}>
                      <dt>문서</dt>
                      <dd>{job.plan.documents}</dd>
                    </div>
                  )}
                  {typeof job.plan.processable === 'number' && (
                    <div className={styles.detailCell}>
                      <dt>처리 가능</dt>
                      <dd>{job.plan.processable}</dd>
                    </div>
                  )}
                  {typeof job.plan.duplicates === 'number' && (
                    <div className={styles.detailCell}>
                      <dt>중복 후보</dt>
                      <dd>{job.plan.duplicates}</dd>
                    </div>
                  )}
                  {typeof job.plan.heldForReview === 'number' && (
                    <div className={styles.detailCell}>
                      <dt>검토 보류</dt>
                      <dd>{job.plan.heldForReview}</dd>
                    </div>
                  )}
                  {typeof job.plan.estimatedTokens === 'number' && (
                    <div className={styles.detailCell}>
                      <dt>예상 토큰</dt>
                      <dd>{job.plan.estimatedTokens}</dd>
                    </div>
                  )}
                  {typeof job.plan.estimatedBillableUnits === 'number' && (
                    <div className={styles.detailCell}>
                      <dt>예상 처리량</dt>
                      <dd>{job.plan.estimatedBillableUnits}</dd>
                    </div>
                  )}
                </dl>
                {job.plan.topicGroups && job.plan.topicGroups.length > 0 && (
                  <>
                    <h3>주제 묶음</h3>
                    <ul className={styles.planList}>
                      {job.plan.topicGroups.map((group) => (
                        <li key={`${group.name}-${group.count}`}>
                          {group.name}: {group.count}개
                        </li>
                      ))}
                    </ul>
                  </>
                )}
              </section>
            )}
          </>
        )}
      </main>
    </>
  );
}
