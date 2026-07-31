import { FilterTab } from '../../types/changes';
import styles from './ReviewChanges.module.css';

interface FilterTabsProps {
  filterTab: FilterTab;
  onFilterChange: (tab: FilterTab) => void;
  counts: {
    safe: number;
    danger: number;
  };
}

export function FilterTabs({ filterTab, onFilterChange, counts }: FilterTabsProps) {
  const tabs: Array<{ id: FilterTab; label: string; count?: number; color?: string }> = [
    { id: 'ALL', label: '전체', count: counts.safe + counts.danger },
    { id: 'SAFE', label: '안전', count: counts.safe, color: 'green' },
    { id: 'DANGER', label: '위험', count: counts.danger, color: 'red' },
  ];

  return (
    <div className={styles.filterTabs}>
      {tabs.map((tab) => (
        <button
          key={tab.id}
          className={`${styles.filterTab} ${filterTab === tab.id ? styles.active : ''}`}
          onClick={() => onFilterChange(tab.id)}
          data-color={tab.color}
        >
          <span className={styles.tabLabel}>{tab.label}</span>
          <span className={styles.tabCount}>{tab.count || 0}</span>
        </button>
      ))}
    </div>
  );
}
