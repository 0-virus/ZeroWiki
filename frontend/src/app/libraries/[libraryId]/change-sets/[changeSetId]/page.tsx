'use client';

import { FormEvent, useEffect, useState } from 'react';
import Link from 'next/link';
import { useParams } from 'next/navigation';
import { DiffViewer } from '@/components/review-changes/DiffViewer';
import { HeaderBar } from '@/components/HeaderBar';
import {
  api,
  ChangeItemSummary,
  ChangeSetDecision,
  ChangeSetReviewDecision,
  ChangeSetSummary,
  getApiErrorMessage,
} from '@/lib/api';
import styles from '../../../library.module.css';

const DECISIONS: Array<{ value: ChangeSetDecision; label: string }> = [
  { value: 'APPROVE', label: '승인' },
  { value: 'REJECT', label: '거절' },
  { value: 'REQUEST_CHANGES', label: '수정 요청' },
  { value: 'DEFER', label: '보류' },
];

interface LocalDecision {
  decision: ChangeSetDecision;
  comment: string;
}

function jsonText(value: unknown) {
  if (value == null) return '';
  if (typeof value === 'string') return value;

  try {
    return JSON.stringify(value, null, 2);
  } catch {
    return String(value);
  }
}

function requiresComment(decision: ChangeSetDecision | undefined) {
  return decision === 'REJECT' || decision === 'REQUEST_CHANGES';
}

function itemLabel(item: ChangeItemSummary) {
  return `${item.targetType} · ${item.targetId}`;
}

function evidenceText(value: unknown) {
  const text = jsonText(value);
  return text.length > 0 ? text : '제공된 근거가 없습니다.';
}

export default function ChangeSetDetailPage() {
  const { libraryId, changeSetId } = useParams<{
    libraryId: string;
    changeSetId: string;
  }>();
  const [changeSet, setChangeSet] = useState<ChangeSetSummary | null>(null);
  const [items, setItems] = useState<ChangeItemSummary[]>([]);
  const [decisions, setDecisions] = useState<Record<string, LocalDecision>>({});
  const [isLoading, setIsLoading] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState('');
  const [submissionError, setSubmissionError] = useState('');
  const [notice, setNotice] = useState('');
  const [reloadKey, setReloadKey] = useState(0);

  useEffect(() => {
    if (!libraryId || !changeSetId) return;

    let active = true;
    setIsLoading(true);
    setError('');

    Promise.all([
      api.getChangeSet(libraryId, changeSetId),
      api.listChangeSetItems(libraryId, changeSetId),
    ])
      .then(([changeSetResponse, itemsResponse]) => {
        if (!active) return;
        setChangeSet(changeSetResponse.data);
        setItems(itemsResponse.data ?? []);
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
  }, [libraryId, changeSetId, reloadKey]);

  function selectDecision(itemId: string, decision: ChangeSetDecision) {
    setSubmissionError('');
    setNotice('');
    setDecisions((previous) => ({
      ...previous,
      [itemId]: {
        decision,
        comment: previous[itemId]?.comment ?? '',
      },
    }));
  }

  function updateComment(itemId: string, comment: string) {
    setDecisions((previous) => ({
      ...previous,
      [itemId]: {
        decision: previous[itemId]?.decision ?? 'REJECT',
        comment,
      },
    }));
  }

  function validateDecisions() {
    const selected = Object.entries(decisions);
    if (selected.length === 0) return '제출할 항목의 결정을 하나 이상 선택해 주세요.';

    for (const [itemId, value] of selected) {
      if (requiresComment(value.decision) && value.comment.trim().length === 0) {
        return `${itemId} 항목의 ${value.decision === 'REJECT' ? '거절' : '수정 요청'} 사유를 입력해 주세요.`;
      }
    }

    return '';
  }

  async function submitReviews(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!libraryId || !changeSetId || !changeSet) return;

    const validationError = validateDecisions();
    if (validationError) {
      setSubmissionError(validationError);
      return;
    }

    const reviewDecisions: ChangeSetReviewDecision[] = Object.entries(decisions).map(
      ([changeItemId, value]) => ({
        changeItemId,
        decision: value.decision,
        ...(requiresComment(value.decision) || value.comment.trim()
          ? { comment: value.comment.trim() }
          : {}),
      }),
    );

    setIsSubmitting(true);
    setSubmissionError('');
    setNotice('');
    try {
      await api.submitChangeSetReviews(libraryId, changeSetId, {
        expectedRevision: changeSet.revision,
        decisions: reviewDecisions,
      });
      setNotice('변경 검토 결정이 제출되었습니다. 자동 적용하지 않고 서버 상태를 따릅니다.');
      setDecisions({});
      setReloadKey((key) => key + 1);
    } catch (requestError) {
      setSubmissionError(getApiErrorMessage(requestError));
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <>
      <HeaderBar />
      <main className={styles.page}>
        <div className={styles.actions}>
          <Link
            className={styles.back}
            href={`/libraries/${encodeURIComponent(libraryId)}/change-sets`}
          >
            변경 검토 목록
          </Link>
          <Link className={styles.back} href={`/libraries/${encodeURIComponent(libraryId)}`}>
            도서관으로
          </Link>
        </div>

        {isLoading && (
          <p className={styles.status} role="status" aria-live="polite">
            변경 세트와 항목을 불러오는 중입니다.
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
        {!isLoading && !error && !changeSet && (
          <p className={styles.empty} role="status">
            변경 세트 정보가 없습니다.
          </p>
        )}
        {!isLoading && !error && changeSet && (
          <>
            <header className={styles.header}>
              <div>
                <p className={styles.breadcrumb}>변경 세트</p>
                <h1 className={styles.title}>{changeSet.id}</h1>
                {changeSet.summary && <p className={styles.muted}>{changeSet.summary}</p>}
              </div>
              <div className={styles.changeMeta}>
                <span className={styles.statusBadge}>{changeSet.status}</span>
                <span className={styles.statusBadge}>{changeSet.riskLevel}</span>
                <span>revision {changeSet.revision}</span>
              </div>
            </header>

            {notice && (
              <p className={styles.notice} role="status" aria-live="polite">
                {notice}
              </p>
            )}
            {submissionError && (
              <p className={styles.error} role="alert">
                {submissionError}
              </p>
            )}

            <form onSubmit={submitReviews}>
              <section className={styles.section} aria-labelledby="change-item-list-title">
                <h2 id="change-item-list-title">변경 항목</h2>
                {items.length === 0 && (
                  <p className={styles.empty} role="status">
                    변경 항목이 없습니다.
                  </p>
                )}
                {items.length > 0 && (
                  <div className={styles.reviewList}>
                    {items.map((item) => {
                      const selected = decisions[item.changeItemId];
                      const beforeBody = jsonText(item.beforeSnapshot);
                      const afterBody = jsonText(item.afterSnapshot);

                      return (
                        <article className={styles.changeItem} key={item.changeItemId}>
                          <div className={styles.changeItemHeader}>
                            <div>
                              <h3>{itemLabel(item)}</h3>
                              <p className={styles.changeMeta}>
                                {item.operation} · {item.riskLevel} · {item.reviewStatus}
                              </p>
                            </div>
                            <code>{item.changeItemId}</code>
                          </div>
                          {item.reason && <p className={styles.muted}>{item.reason}</p>}

                          {(beforeBody || afterBody) && (
                            <div className={styles.snapshotBlock}>
                              <DiffViewer
                                title="변경 전·후"
                                beforeBody={beforeBody}
                                afterBody={afterBody}
                              />
                            </div>
                          )}

                          <dl className={styles.changeMeta}>
                            {item.aiConfidence != null && (
                              <>
                                <dt>AI 신뢰도</dt>
                                <dd>{Math.round(Math.min(1, Math.max(0, item.aiConfidence)) * 100)}%</dd>
                              </>
                            )}
                            {item.evidenceSummary != null && (
                              <>
                                <dt>근거</dt>
                                <dd>
                                  <pre className={styles.snapshotBlock}>
                                    {evidenceText(item.evidenceSummary)}
                                  </pre>
                                </dd>
                              </>
                            )}
                          </dl>

                          <div className={styles.decisionGroup} role="group" aria-label={`${itemLabel(item)} 결정`}>
                            {DECISIONS.map(({ value, label }) => (
                              <button
                                className={`${styles.decisionButton} ${
                                  selected?.decision === value ? styles.decisionButtonActive : ''
                                }`}
                                type="button"
                                key={value}
                                aria-pressed={selected?.decision === value}
                                onClick={() => selectDecision(item.changeItemId, value)}
                                disabled={isSubmitting}
                              >
                                {label}
                              </button>
                            ))}
                          </div>

                          {selected && requiresComment(selected.decision) && (
                            <div className={styles.field}>
                              <label htmlFor={`comment-${item.changeItemId}`}>
                                {selected.decision === 'REJECT' ? '거절 사유' : '수정 요청 내용'}
                              </label>
                              <textarea
                                id={`comment-${item.changeItemId}`}
                                value={selected.comment}
                                onChange={(event) =>
                                  updateComment(item.changeItemId, event.target.value)
                                }
                                required
                                disabled={isSubmitting}
                                aria-describedby={`comment-hint-${item.changeItemId}`}
                              />
                              <span className={styles.reviewHint} id={`comment-hint-${item.changeItemId}`}>
                                거절과 수정 요청에는 사유가 필요합니다.
                              </span>
                            </div>
                          )}
                        </article>
                      );
                    })}
                  </div>
                )}
              </section>

              {items.length > 0 && (
                <div className={styles.inlineActions}>
                  <button className={styles.submit} type="submit" disabled={isSubmitting}>
                    {isSubmitting ? '제출 중…' : '선택한 결정 제출'}
                  </button>
                </div>
              )}
            </form>
          </>
        )}
      </main>
    </>
  );
}
