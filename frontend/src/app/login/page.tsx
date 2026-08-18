import { Suspense } from 'react';
import LoginForm from './LoginForm';
import styles from '../account.module.css';

function LoginFallback() {
  return (
    <main className={styles.page}>
      <p className={styles.status} role="status" aria-live="polite">
        로그인 화면을 준비 중입니다…
      </p>
    </main>
  );
}

export default function LoginPage() {
  return (
    <Suspense fallback={<LoginFallback />}>
      <LoginForm />
    </Suspense>
  );
}
