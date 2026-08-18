'use client';

import { FormEvent, useEffect, useState } from 'react';
import Link from 'next/link';
import { useParams } from 'next/navigation';
import { HeaderBar } from '@/components/HeaderBar';
import {
  api,
  ContradictionClassification,
  ContradictionSummary,
  getApiErrorMessage,
} from '@/lib/api';
import styles from '../../library.module.css';

const CLASSIFICATIONS: Array<[ContradictionClassification, string]> = [
  ['TRUE_CONFLICT', '실제 모순'],
  ['TIME_CHANGE', '시간 차이'],
  ['CONDITION_DIFF', '조건 차이'],
  ['VIEWPOINT_DIFF', '관점 차이'],
  ['POSSIBLE_CONFLICT', '모순 가능성'],
];

function classificationLabel(value: string) {
  return CLASSIFICATIONS.find(([classification]) => classification === value)?.[1] ?? value;
}

export default function ContradictionsPage() {
  const { libraryId } = useParams<{ libraryId: string }>();
  const [contradictions, setContradictions] = useState<ContradictionSummary[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');
  const [reloadKey, setReloadKey] = useState(0);
  const [activeId, setActiveId] = useState('');
  const [classification, setClassification] =
    useState<ContradictionClassification>('TRUE_CONFLICT');
  const [resolutionNote, setResolutionNote] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [resolutionError, setResolutionError] = useState('');
  const [notice, setNotice] = useState('');

  useEffect(() => {
    if (!libraryId) return;

    let active = true;
    setIsLoading(true);
    setError('');

    api
      .listContradictions(libraryId)
      .then((response) => {
        if (active) setContradictions(response.data ?? []);
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

  function openResolution(contradiction: ContradictionSummary) {
    setActiveId(contradiction.id);
    setClassification(contradiction.classification);
    setResolutionNote('');
    setResolutionError('');
    setNotice('');
  }

  async function submitResolution(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!libraryId || !activeId) return;

    setIsSubmitting(true);
    setResolutionError('');
    setNotice('');
    try {
      await api.resolveContradiction(libraryId, activeId, {
        decision: 'CLASSIFY',
        classification,
        resolutionNote: resolutionNote.trim(),
      });
      setActiveId('');
      setNotice('모순 판단 제안이 변경 세트로 제출되었습니다. 자동 판정하거나 적용하지 않습니다.');
      setReloadKey((key) => key + 1);
    } catch (requestError) {
      setResolutionError(getApiErrorMessage(requestError));
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
            href={`/libraries/${encodeURIComponent(libraryId)}/relations`}
          >
            관계 검토
          </Link>
        </div>

        <header className={styles.header}>
          <div>
            <h1 className={styles.title}>모순 검토</h1>
            <p className={styles.muted}>
              AI가 찾은 모순 후보를 확인하고 최종 판단을 변경 세트로 제안합니다.
            </p>
          </div>
        </header>

        {notice && (
          <p className={styles.notice} role="status" aria-live="polite">
            {notice}
          </p>
        )}
        {resolutionError && (
          <p className={styles.error} role="alert">
            {resolutionError}
          </p>
        )}

        <section className={styles.section} aria-labelledby="contradiction-list-title">
          <h2 id="contradiction-list-title">모순 후보 목록</h2>
          {isLoading && (
            <p className={styles.status} role="status" aria-live="polite">
              모순 후보를 불러오는 중입니다…
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
          {!isLoading && !error && contradictions.length === 0 && (
            <p className={styles.empty} role="status">
              검토할 모순 후보가 없습니다.
            </p>
          )}
          {!isLoading && !error && contradictions.length > 0 && (
            <div className={styles.reviewList}>
              {contradictions.map((contradiction) => (
                <article className={styles.reviewCard} key={contradiction.id}>
                  <div className={styles.claimComparison}>
                    <p>
                      <strong>주장 A</strong>
                      <br />
                      {contradiction.leftClaimStatement}
                    </p>
                    <p>
                      <strong>주장 B</strong>
                      <br />
                      {contradiction.rightClaimStatement}
                    </p>
                  </div>
                  <div className={styles.itemMeta}>
                    {classificationLabel(contradiction.classification)} · {contradiction.status}
                    {typeof contradiction.confidence === 'number' &&
                      ` · 신뢰도 ${Math.round(Math.min(1, Math.max(0, contradiction.confidence)) * 100)}%`}
                  </div>
                  {contradiction.explanation && <p>{contradiction.explanation}</p>}
                  {contradiction.resolutionNote && (
                    <p className={styles.notice}>기존 판단: {contradiction.resolutionNote}</p>
                  )}
                  {contradiction.status === 'OPEN' ? (
                    <>
                      <button
                        className={styles.back}
                        type="button"
                        onClick={() => openResolution(contradiction)}
                        aria-expanded={activeId === contradiction.id}
                        aria-controls={`contradiction-resolution-${contradiction.id}`}
                      >
                        사용자 판단 제안
                      </button>
                      {activeId === contradiction.id && (
                        <form
                          className={styles.reviewForm}
                          id={`contradiction-resolution-${contradiction.id}`}
                          onSubmit={submitResolution}
                        >
                          <div className={styles.field}>
                            <label htmlFor={`classification-${contradiction.id}`}>판단</label>
                            <select
                              id={`classification-${contradiction.id}`}
                              value={classification}
                              onChange={(event) =>
                                setClassification(event.target.value as ContradictionClassification)
                              }
                              disabled={isSubmitting}
                            >
                              {CLASSIFICATIONS.map(([value, label]) => (
                                <option key={value} value={value}>
                                  {label}
                                </option>
                              ))}
                            </select>
                          </div>
                          <div className={styles.field}>
                            <label htmlFor={`resolution-note-${contradiction.id}`}>판단 근거</label>
                            <textarea
                              id={`resolution-note-${contradiction.id}`}
                              value={resolutionNote}
                              onChange={(event) => setResolutionNote(event.target.value)}
                              required
                              disabled={isSubmitting}
                              placeholder="두 주장을 어떻게 구분했는지 적어 주세요."
                            />
                          </div>
                          <div className={styles.inlineActions}>
                            <button className={styles.submit} type="submit" disabled={isSubmitting}>
                              {isSubmitting ? '제출 중…' : '판단 제안 제출'}
                            </button>
                            <button
                              className={styles.back}
                              type="button"
                              onClick={() => setActiveId('')}
                              disabled={isSubmitting}
                            >
                              닫기
                            </button>
                          </div>
                        </form>
                      )}
                    </>
                  ) : (
                    <p className={styles.muted}>이미 검토된 항목입니다.</p>
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
