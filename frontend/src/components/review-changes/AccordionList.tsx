import { ChangeItem, DecisionType, LocalDecisionState } from '../../types/changes';
import { ChangeItemAccordion } from './ChangeItemAccordion';
import styles from './ReviewChanges.module.css';

interface AccordionListProps {
  items: ChangeItem[];
  expandedItemId: string | null;
  onExpandChange: (itemId: string | null) => void;
  decisions: LocalDecisionState;
  onDecision: (itemId: string, decision: DecisionType, comment?: string) => void;
}

export function AccordionList({
  items,
  expandedItemId,
  onExpandChange,
  decisions,
  onDecision,
}: AccordionListProps) {
  return (
    <div className={styles.accordionList}>
      {items.map((item) => {
        const isExpanded = expandedItemId === item.changeItemId;
        const decision = decisions[item.changeItemId];

        return (
          <ChangeItemAccordion
            key={item.changeItemId}
            item={item}
            isExpanded={isExpanded}
            onToggle={() => onExpandChange(isExpanded ? null : item.changeItemId)}
            decision={decision}
            onDecision={(dec, comment) => onDecision(item.changeItemId, dec, comment)}
          />
        );
      })}
    </div>
  );
}
