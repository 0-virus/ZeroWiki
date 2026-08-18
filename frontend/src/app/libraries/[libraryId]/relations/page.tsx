'use client';

import { FormEvent, useEffect, useState } from 'react';
import Link from 'next/link';
import { useParams } from 'next/navigation';
import { HeaderBar } from '@/components/HeaderBar';
import { api, getApiErrorMessage, RelationSummary } from '@/lib/api';
import styles from '../../library.module.css';

const FEEDBACK_TYPES = [
  ['DIFFERENT_TOPIC', '주제가 다름'],
  ['NOT_CAUSAL', '인과관계가 아님'],
  ['WEAK_SOURCE', '출처가 약함'],
  ['IRRELEVANT', '도서관 목적과 무관함'],
  ['DUPLICATE_CONCEPT', '중복 개념'],
  ['OTHER', '기타'],
] as const;

export default function RelationsPage() {
  const { libraryId } = useParams<{ libraryId: string }>();
  const [relations, setRelations] = useState<RelationSummary[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');
  const [reloadKey, setReloadKey] = useState(0);
  const [activeRelationId, setActiveRelationId] = useState('');
  const [feedbackType, setFeedbackType] = useState<string>(FEEDBACK_TYPES[0][0]);
  const [comment, setComment] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [feedbackError, setFeedbackError] = useState('');
  const [notice, setNotice] = useState('');

  useEffect(() => {
    if (!libraryId) return;

    let active = true;
    setIsLoading(true);
    setError('');

    api
      .listRelations(libraryId)
      .then((response) => {
        if (active) setRelations(response.data ?? []);
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

  function openFeedback(relationId: string) {
    setActiveRelationId(relationId);
    setFeedbackType(FEEDBACK_TYPES[0][0]);
    setComment('');
    setFeedbackError('');
    setNotice('');
  }

  async function submitFeedback(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!libraryId || !activeRelationId) return;

    setIsSubmitting(true);
    setFeedbackError('');
    setNotice('');
    try {
      await api.submitRelationFeedback(libraryId, activeRelationId, {
        feedbackType,
        comment: comment.trim(),
        scope: 'LIBRARY',
      });
      setActiveRelationId('');
      setNotice('관계 피드백이 현재 도서관의 검토 제안으로 제출되었습니다. 자동 적용하지 않습니다.');
      setReloadKey((key) => key + 1);
    } catch (requestError) {
      setFeedbackError(getApiErrorMessage(requestError));
    } finally {
      setIsSubmitting(false);
    }
  }

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
            페이지 보기
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
            <h1 className={styles.title}>관계 검토</h1>
            <p className={styles.muted}>
              AI가 제안한 관계를 확인하고 잘못된 연결의 이유를 제출합니다.
            </p>
          </div>
        </header>

        {notice && (
          <p className={styles.notice} role="status" aria-live="polite">
            {notice}
          </p>
        )}
        {feedbackError && (
          <p className={styles.error} role="alert">
            {feedbackError}
          </p>
        )}

        <section className={styles.section} aria-labelledby="relation-list-title">
          <h2 id="relation-list-title">관계 목록</h2>
          {isLoading && (
            <p className={styles.status} role="status" aria-live="polite">
              관계를 불러오는 중입니다…
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
          {!isLoading && !error && relations.length === 0 && (
            <p className={styles.empty} role="status">
              검토할 관계가 없습니다.
            </p>
          )}
          {!isLoading && !error && relations.length > 0 && (
            <div className={styles.reviewList}>
              {relations.map((relation) => (
                <article className={styles.reviewCard} key={relation.id}>
                  <div className={styles.relationEndpoints}>
                    <Link
                      href={`/libraries/${encodeURIComponent(libraryId)}/pages/${encodeURIComponent(relation.sourcePageId)}`}
                    >
                      {relation.sourcePageId}
                    </Link>
                    <span aria-hidden="true">↔</span>
                    <Link
                      href={`/libraries/${encodeURIComponent(libraryId)}/pages/${encodeURIComponent(relation.targetPageId)}`}
                    >
                      {relation.targetPageId}
                    </Link>
                  </div>
                  <div className={styles.itemMeta}>
                    {[relation.relationType, relation.status]
                      .filter(Boolean)
                      .join(' · ')}
                    {typeof relation.confidence === 'number' &&
                      ` · 신뢰도 ${Math.round(Math.min(1, Math.max(0, relation.confidence)) * 100)}%`}
                  </div>
                  {relation.rationale && <p className={styles.muted}>{relation.rationale}</p>}
                  <button
                    className={styles.back}
                    type="button"
                    onClick={() => openFeedback(relation.id)}
                    aria-expanded={activeRelationId === relation.id}
                    aria-controls={`relation-feedback-${relation.id}`}
                  >
                    잘못된 연결 피드백
                  </button>

                  {activeRelationId === relation.id && (
                    <form
                      className={styles.reviewForm}
                      id={`relation-feedback-${relation.id}`}
                      onSubmit={submitFeedback}
                    >
                      <div className={styles.field}>
                        <label htmlFor={`feedback-type-${relation.id}`}>피드백 이유</label>
                        <select
                          id={`feedback-type-${relation.id}`}
                          value={feedbackType}
                          onChange={(event) => setFeedbackType(event.target.value)}
                          disabled={isSubmitting}
                        >
                          {FEEDBACK_TYPES.map(([value, label]) => (
                            <option key={value} value={value}>
                              {label}
                            </option>
                          ))}
                        </select>
                      </div>
                      <div className={styles.field}>
                        <label htmlFor={`feedback-comment-${relation.id}`}>설명</label>
                        <textarea
                          id={`feedback-comment-${relation.id}`}
                          value={comment}
                          onChange={(event) => setComment(event.target.value)}
                          required
                          disabled={isSubmitting}
                          placeholder="이 연결을 어떻게 판단했는지 적어 주세요."
                        />
                      </div>
                      <div className={styles.inlineActions}>
                        <button className={styles.submit} type="submit" disabled={isSubmitting}>
                          {isSubmitting ? '제출 중…' : '검토 제안 제출'}
                        </button>
                        <button
                          className={styles.back}
                          type="button"
                          onClick={() => setActiveRelationId('')}
                          disabled={isSubmitting}
                        >
                          닫기
                        </button>
                      </div>
                    </form>
                  )}
                </article>
              ))}
            </div>
          )}
        </section>
      </main>
    </>
  );
}
