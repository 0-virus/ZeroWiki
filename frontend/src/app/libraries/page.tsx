'use client';

import { FormEvent, useEffect, useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { HeaderBar } from '@/components/HeaderBar';
import {
  ApiError,
  api,
  getApiErrorMessage,
  LibraryCreateRequest,
  LibrarySummary,
} from '@/lib/api';
import styles from './library.module.css';

const initialForm = {
  name: '',
  description: '',
  templateType: 'LEARNING_RESEARCH' as LibraryCreateRequest['templateType'],
  goal: '',
  sourceTypes: '',
  knowledgeLevel: '',
  trustedSources: '',
  cautionAreas: '',
};

export default function LibrariesPage() {
  const router = useRouter();
  const [libraries, setLibraries] = useState<LibrarySummary[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');
  const [form, setForm] = useState(initialForm);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [reloadKey, setReloadKey] = useState(0);

  useEffect(() => {
    let active = true;
    setIsLoading(true);
    setError('');

    api
      .listLibraries()
      .then((response) => {
        if (active) setLibraries(response.data ?? []);
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
  }, [reloadKey]);

  function updateField(
    field: Exclude<keyof typeof initialForm, 'templateType'>,
    value: string,
  ) {
    setForm((current) => ({ ...current, [field]: value }));
  }

  async function handleCreate(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError('');
    setIsSubmitting(true);

    const body: LibraryCreateRequest = {
      name: form.name,
      description: form.description,
      templateType: form.templateType,
      onboarding: {
        goal: form.goal,
        sourceTypes: form.sourceTypes
          .split(',')
          .map((sourceType) => sourceType.trim())
          .filter(Boolean),
        knowledgeLevel: form.knowledgeLevel,
        trustedSources: form.trustedSources,
        cautionAreas: form.cautionAreas,
      },
    };

    try {
      const response = await api.createLibrary(body);
      if (!response.data?.id) {
        throw new ApiError(502, '도서관 생성 응답에 id가 없습니다.', {
          code: 'LIBRARY_ID_MISSING',
        });
      }
      router.push(`/libraries/${encodeURIComponent(response.data.id)}`);
    } catch (requestError) {
      setError(getApiErrorMessage(requestError));
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <>
      <HeaderBar />
      <main className={styles.page}>
        <header className={styles.header}>
          <div>
            <h1 className={styles.title}>내 도서관</h1>
            <p className={styles.muted}>개인 자료와 운영 헌법을 관리합니다.</p>
          </div>
        </header>

        <div className={styles.layout}>
          <section className={styles.section} aria-labelledby="library-list-title">
            <h2 id="library-list-title">도서관 목록</h2>
            {isLoading && (
              <p className={styles.status} role="status" aria-live="polite">
                도서관을 불러오는 중입니다…
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
            {!isLoading && !error && libraries.length === 0 && (
              <p className={styles.empty} role="status">
                아직 만든 도서관이 없습니다. 오른쪽에서 첫 도서관을 만들어 보세요.
              </p>
            )}
            {!isLoading && !error && libraries.length > 0 && (
              <ul className={styles.list}>
                {libraries.map((library) => (
                  <li key={library.id}>
                    <Link
                      className={styles.item}
                      href={`/libraries/${encodeURIComponent(library.id)}`}
                    >
                      <span className={styles.itemTitle}>{library.name}</span>
                      {library.description && (
                        <span className={styles.itemMeta}>{library.description}</span>
                      )}
                      {typeof library.counts?.pages === 'number' && (
                        <span className={styles.itemMeta}>
                          페이지 {library.counts.pages}개
                        </span>
                      )}
                    </Link>
                  </li>
                ))}
              </ul>
            )}
          </section>

          <section className={styles.section} aria-labelledby="create-library-title">
            <h2 id="create-library-title">도서관 만들기</h2>
            <form className={styles.form} onSubmit={handleCreate} aria-busy={isSubmitting}>
              <div className={styles.field}>
                <label htmlFor="library-name">이름</label>
                <input
                  id="library-name"
                  value={form.name}
                  onChange={(event) => updateField('name', event.target.value)}
                  required
                />
              </div>
              <div className={styles.field}>
                <label htmlFor="library-description">설명</label>
                <textarea
                  id="library-description"
                  value={form.description}
                  onChange={(event) => updateField('description', event.target.value)}
                  required
                />
              </div>
              <div className={styles.field}>
                <label htmlFor="library-template">템플릿</label>
                <select
                  id="library-template"
                  value={form.templateType}
                  onChange={(event) =>
                    setForm((current) => ({
                      ...current,
                      templateType: event.target.value as LibraryCreateRequest['templateType'],
                    }))
                  }
                >
                  <option value="LEARNING_RESEARCH">학습·연구</option>
                  <option value="PERSONAL_DECISION">개인 의사결정</option>
                </select>
              </div>

              <fieldset className={styles.fieldset}>
                <legend className={styles.legend}>초기 운영 질문</legend>
                <div className={styles.field}>
                  <label htmlFor="onboarding-goal">무엇을 알고 결정하거나 만들고 싶은가</label>
                  <textarea
                    id="onboarding-goal"
                    value={form.goal}
                    onChange={(event) => updateField('goal', event.target.value)}
                    required
                  />
                </div>
                <div className={styles.field}>
                  <label htmlFor="onboarding-sources">넣을 자료 종류 (쉼표로 구분)</label>
                  <input
                    id="onboarding-sources"
                    value={form.sourceTypes}
                    onChange={(event) => updateField('sourceTypes', event.target.value)}
                    required
                  />
                </div>
                <div className={styles.field}>
                  <label htmlFor="onboarding-level">현재 지식 수준</label>
                  <input
                    id="onboarding-level"
                    value={form.knowledgeLevel}
                    onChange={(event) => updateField('knowledgeLevel', event.target.value)}
                    required
                  />
                </div>
                <div className={styles.field}>
                  <label htmlFor="onboarding-trusted">신뢰하는 출처와 관점</label>
                  <textarea
                    id="onboarding-trusted"
                    value={form.trustedSources}
                    onChange={(event) => updateField('trustedSources', event.target.value)}
                    required
                  />
                </div>
                <div className={styles.field}>
                  <label htmlFor="onboarding-caution">AI가 조심해야 할 영역</label>
                  <textarea
                    id="onboarding-caution"
                    value={form.cautionAreas}
                    onChange={(event) => updateField('cautionAreas', event.target.value)}
                    required
                  />
                </div>
              </fieldset>

              {error && (
                <p className={styles.error} role="alert">
                  {error}
                </p>
              )}
              <button className={styles.submit} type="submit" disabled={isSubmitting}>
                {isSubmitting ? '생성 중…' : '도서관 생성'}
              </button>
            </form>
          </section>
        </div>
      </main>
    </>
  );
}
