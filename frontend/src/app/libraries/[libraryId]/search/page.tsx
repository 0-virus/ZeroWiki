'use client';

import { FormEvent, useEffect, useState } from 'react';
import Link from 'next/link';
import { useParams } from 'next/navigation';
import { HeaderBar } from '@/components/HeaderBar';
import { api, getApiErrorMessage, SearchMode, SearchResult } from '@/lib/api';
import styles from '../../library.module.css';

const SEARCH_MODES: Array<[SearchMode, string]> = [
  ['HYBRID', '하이브리드'],
  ['KEYWORD', '키워드'],
  ['SEMANTIC', '의미'],
];

function formatLocator(locator: unknown) {
  if (locator === null || locator === undefined) return '';

  try {
    return JSON.stringify(locator);
  } catch {
    return '';
  }
}

function resultPath(libraryId: string, result: SearchResult) {
  if (!result.pageId) return '';
  return `/libraries/${encodeURIComponent(libraryId)}/pages/${encodeURIComponent(result.pageId)}`;
}

export default function LibrarySearchPage() {
  const { libraryId } = useParams<{ libraryId: string }>();
  const [query, setQuery] = useState('');
  const [submittedQuery, setSubmittedQuery] = useState('');
  const [mode, setMode] = useState<SearchMode>('HYBRID');
  const [results, setResults] = useState<SearchResult[]>([]);
  const [targetLibraryIds, setTargetLibraryIds] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [isExpanding, setIsExpanding] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');

  useEffect(() => {
    if (!libraryId || !submittedQuery) {
      setResults([]);
      setIsLoading(false);
      return;
    }

    let active = true;
    setIsLoading(true);
    setError('');
    setNotice('');

    api
      .searchLibrary(libraryId, { query: submittedQuery, mode })
      .then((response) => {
        if (active) setResults(response.data ?? []);
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
  }, [libraryId, mode, submittedQuery]);

  function submitSearch(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const nextQuery = query.trim();
    setError('');
    setNotice('');

    if (!nextQuery) {
      setError('검색어를 입력해 주세요.');
      return;
    }

    setSubmittedQuery(nextQuery);
  }

  async function expandSearch(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!libraryId || !submittedQuery) return;

    const ids = targetLibraryIds
      .split(/[\s,]+/)
      .map((value) => value.trim())
      .filter(Boolean);

    if (ids.length === 0) {
      setError('확장할 참조 도서관 ID를 입력해 주세요.');
      return;
    }

    setIsExpanding(true);
    setError('');
    setNotice('');
    try {
      const response = await api.expandLibrarySearch(libraryId, {
        query: submittedQuery,
        targetLibraryIds: ids,
        mode,
      });
      setResults(response.data ?? []);
      setNotice('사용자가 선택한 참조 도서관까지 확장해 검색했습니다.');
    } catch (requestError) {
      setError(getApiErrorMessage(requestError));
    } finally {
      setIsExpanding(false);
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
            href={`/libraries/${encodeURIComponent(libraryId)}/assistant`}
          >
            AI 사서
          </Link>
        </div>

        <header className={styles.header}>
          <div>
            <h1 className={styles.title}>도서관 검색</h1>
            <p className={styles.muted}>
              현재 도서관을 먼저 검색합니다. 참조 도서관 확장은 사용자가 명시적으로 요청해야 합니다.
            </p>
          </div>
        </header>

        <section className={styles.section} aria-labelledby="search-form-title">
          <h2 id="search-form-title">검색어</h2>
          <form className={styles.searchForm} onSubmit={submitSearch}>
            <div className={styles.field}>
              <label htmlFor="library-search-query">검색어</label>
              <input
                id="library-search-query"
                value={query}
                onChange={(event) => setQuery(event.target.value)}
                placeholder="예: Raft 리더 선출"
                required
              />
            </div>
            <div className={styles.field}>
              <label htmlFor="library-search-mode">검색 방식</label>
              <select
                id="library-search-mode"
                value={mode}
                onChange={(event) => setMode(event.target.value as SearchMode)}
              >
                {SEARCH_MODES.map(([value, label]) => (
                  <option key={value} value={value}>
                    {label}
                  </option>
                ))}
              </select>
            </div>
            <button className={styles.submit} type="submit" disabled={isLoading || isExpanding}>
              {isLoading ? '검색 중…' : '검색'}
            </button>
          </form>
        </section>

        <section className={styles.section} aria-labelledby="search-expansion-title">
          <h2 id="search-expansion-title">참조 도서관 확장</h2>
          <p className={styles.muted}>
            연결된 도서관만 사용할 수 있습니다. ID를 직접 입력한 경우에만 확장 요청을 보냅니다.
          </p>
          <form className={styles.searchExpansion} onSubmit={expandSearch}>
            <div className={styles.field}>
              <label htmlFor="search-target-library-ids">참조 도서관 ID</label>
              <textarea
                id="search-target-library-ids"
                value={targetLibraryIds}
                onChange={(event) => setTargetLibraryIds(event.target.value)}
                placeholder="도서관 ID를 쉼표 또는 줄바꿈으로 구분"
                aria-describedby="search-target-library-hint"
              />
              <span className={styles.fileHint} id="search-target-library-hint">
                자동으로 참조 도서관을 추가하지 않습니다.
              </span>
            </div>
            <button
              className={styles.back}
              type="submit"
              disabled={!submittedQuery || isLoading || isExpanding}
            >
              {isExpanding ? '확장 검색 중…' : '선택한 도서관까지 검색'}
            </button>
          </form>
        </section>

        {notice && (
          <p className={styles.notice} role="status" aria-live="polite">
            {notice}
          </p>
        )}
        {error && (
          <p className={styles.error} role="alert">
            {error}
          </p>
        )}

        <section className={styles.section} aria-labelledby="search-result-title">
          <h2 id="search-result-title">검색 결과</h2>
          {isLoading && (
            <p className={styles.status} role="status" aria-live="polite">
              검색 결과를 불러오는 중입니다…
            </p>
          )}
          {!isLoading && !error && !submittedQuery && (
            <p className={styles.empty} role="status">
              검색어를 입력하면 결과가 표시됩니다.
            </p>
          )}
          {!isLoading && !error && submittedQuery && results.length === 0 && (
            <p className={styles.empty} role="status">
              검색 결과가 없습니다.
            </p>
          )}
          {!isLoading && results.length > 0 && (
            <div className={styles.searchResults} aria-live="polite">
              {results.map((result, index) => {
                const origin = result.origin;
                const isReferenced = origin.scope === 'REFERENCED';
                const href = resultPath(libraryId, result);
                const locator = formatLocator(result.locator);

                return (
                  <article
                    className={`${styles.searchResult} ${isReferenced ? styles.searchResultReferenced : ''}`}
                    key={`${result.resultType}-${result.pageId ?? result.sourceId ?? index}`}
                  >
                    <div className={styles.searchResultHeader}>
                      <span className={styles.statusBadge}>{result.resultType}</span>
                      <span className={styles.originBadge}>
                        {isReferenced ? '참조 도서관' : '현재 도서관'} · {origin.libraryName}
                      </span>
                    </div>
                    <h3 className={styles.searchResultTitle}>
                      {href ? <Link href={href}>{result.title}</Link> : result.title}
                    </h3>
                    <p className={styles.searchSnippet}>{result.snippet}</p>
                    <dl className={styles.searchMeta}>
                      <div>
                        <dt>도서관 ID</dt>
                        <dd>{origin.libraryId}</dd>
                      </div>
                      {typeof result.score === 'number' && (
                        <div>
                          <dt>점수</dt>
                          <dd>{result.score.toFixed(3)}</dd>
                        </div>
                      )}
                      {result.sourceId && (
                        <div>
                          <dt>원본 ID</dt>
                          <dd>{result.sourceId}</dd>
                        </div>
                      )}
                      {locator && (
                        <div>
                          <dt>위치</dt>
                          <dd>{locator}</dd>
                        </div>
                      )}
                    </dl>
                  </article>
                );
              })}
            </div>
          )}
        </section>
      </main>
    </>
  );
}
