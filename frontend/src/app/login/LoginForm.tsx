'use client';

import { FormEvent, useState } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import { ApiError, api, getApiErrorMessage, setAccessToken } from '@/lib/api';
import styles from '../account.module.css';

type Mode = 'login' | 'signup';

function safeNext(value: string | null) {
  return value && value.startsWith('/') && !value.startsWith('//') && !/[\\\u0000-\u001f\u007f]/.test(value)
    ? value
    : '/libraries';
}

export default function LoginForm() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const [mode, setMode] = useState<Mode>('login');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [displayName, setDisplayName] = useState('');
  const [notice, setNotice] = useState('');
  const [error, setError] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError('');
    setNotice('');
    setIsSubmitting(true);

    try {
      if (mode === 'signup') {
        await api.signUp({ email, password, displayName });
        setMode('login');
        setNotice('가입이 완료되었습니다. 로그인해 주세요.');
        return;
      }

      const response = await api.login({ email, password });
      const token = response.data?.accessToken;
      if (!token) {
        setAccessToken(null);
        throw new ApiError(502, '로그인 응답에 세션 정보가 없습니다.', {
          code: 'AUTH_TOKEN_MISSING',
        });
      }

      router.replace(safeNext(searchParams.get('next')));
    } catch (submitError) {
      setError(getApiErrorMessage(submitError));
    } finally {
      setIsSubmitting(false);
    }
  }

  const isSignup = mode === 'signup';

  return (
    <main className={styles.page}>
      <section className={styles.card} aria-labelledby="account-title">
        <h1 id="account-title" className={styles.title}>
          {isSignup ? 'ZeroWiki 가입' : 'ZeroWiki 로그인'}
        </h1>
        <p className={styles.description}>
          개인 지식 도서관을 안전하게 관리하세요.
        </p>

        {notice && (
          <p className={styles.notice} role="status" aria-live="polite">
            {notice}
          </p>
        )}
        {error && (
          <p className={styles.error} role="alert" aria-live="assertive">
            {error}
          </p>
        )}

        <form className={styles.form} onSubmit={handleSubmit} aria-busy={isSubmitting}>
          {isSignup && (
            <div className={styles.field}>
              <label htmlFor="display-name">표시 이름</label>
              <input
                id="display-name"
                name="displayName"
                value={displayName}
                onChange={(event) => setDisplayName(event.target.value)}
                autoComplete="name"
                required
              />
            </div>
          )}

          <div className={styles.field}>
            <label htmlFor="email">이메일</label>
            <input
              id="email"
              name="email"
              type="email"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              autoComplete="email"
              required
            />
          </div>

          <div className={styles.field}>
            <label htmlFor="password">비밀번호</label>
            <input
              id="password"
              name="password"
              type="password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              autoComplete={isSignup ? 'new-password' : 'current-password'}
              required
            />
          </div>

          <button className={styles.submit} type="submit" disabled={isSubmitting}>
            {isSubmitting ? '처리 중…' : isSignup ? '가입하기' : '로그인'}
          </button>
        </form>

        <p className={styles.switchRow}>
          {isSignup ? '이미 계정이 있나요?' : '처음 방문하셨나요?'}{' '}
          <button
            className={styles.switch}
            type="button"
            onClick={() => {
              setMode(isSignup ? 'login' : 'signup');
              setError('');
              setNotice('');
            }}
          >
            {isSignup ? '로그인' : '가입하기'}
          </button>
        </p>
      </section>
    </main>
  );
}
