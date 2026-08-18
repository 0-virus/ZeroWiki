'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import styles from './HeaderBar.module.css';

const navItems = [
  { id: 'home', href: '/', label: '홈' },
  { id: 'libraries', href: '/libraries', label: '도서관' },
  { id: 'documents', href: '/', label: '문서' },
  { id: 'review-changes', href: '/review-changes', label: '변경 검토' },
  { id: 'ingest', href: '/', label: 'Ingest' },
  { id: 'graph', href: '/', label: '그래프' },
  { id: 'onboarding', href: '/libraries', label: '온보딩' },
];

export function HeaderBar() {
  const pathname = usePathname();

  return (
    <header className={styles.header}>
      <Link href="/" className={styles.logo}>
        ZERO_WIKI
      </Link>

      <nav className={styles.nav} aria-label="주요 메뉴">
        {navItems.map((item) => {
          const isActive =
            item.href === '/'
              ? item.id === 'home' && pathname === '/'
              : pathname === item.href || pathname.startsWith(`${item.href}/`);
          return (
            <Link
              key={item.id}
              href={item.href}
              className={`${styles.navLink} ${isActive ? styles.active : ''}`}
              aria-current={isActive ? 'page' : undefined}
            >
              {item.label}
            </Link>
          );
        })}
      </nav>

      <div className={styles.notificationBadge}>
        알림 <span className={styles.badge}>4</span>
      </div>
    </header>
  );
}
