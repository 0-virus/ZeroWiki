'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import styles from './HeaderBar.module.css';

const navItems = [
  { href: '/', label: '홈' },
  { href: '/', label: '도서관' },
  { href: '/', label: '문서' },
  { href: '/review-changes', label: '변경 검토' },
  { href: '/', label: 'Ingest' },
  { href: '/', label: '그래프' },
  { href: '/', label: '온보딩' },
];

export function HeaderBar() {
  const pathname = usePathname();

  return (
    <header className={styles.header}>
      <Link href="/" className={styles.logo}>
        ZERO_WIKI
      </Link>

      <nav className={styles.nav}>
        {navItems.map((item) => {
          const isActive = pathname === item.href;
          return (
            <Link
              key={item.href}
              href={item.href}
              className={`${styles.navLink} ${isActive ? styles.active : ''}`}
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
