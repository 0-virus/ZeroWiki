// 변경 검토 화면 타입 정의

export type DecisionType = 'APPROVE' | 'REJECT' | 'REQUEST_CHANGES' | 'DEFER';

export interface Evidence {
  sourceId: string;
  sourceTitle: string;
  excerpt: string;
  excerptLanguage: string; // BCP 47
  confidence: number; // 0~1.0
  url?: string;
}

export interface ChangeItem {
  changeItemId: string;
  targetType: 'PAGE' | 'RELATION' | 'CLAIM';
  targetId: string;
  targetName: string;
  operation: 'CREATE' | 'UPDATE' | 'DELETE';
  riskLevel: 'MINOR' | 'MAJOR';
  reason: string;
  beforeSnapshot: Record<string, any>;
  afterSnapshot: Record<string, any>;
  evidenceSummary: Evidence[];
  aiConfidence: number; // 0~1.0
  reviewStatus?: string;
}

export interface ChangeSet {
  id: string;
  title: string;
  summary: string;
  riskLevel: 'MINOR' | 'MAJOR';
  status: string;
  baseLibraryVersionId: string;
  items?: ChangeItem[];
}

export interface ReviewDecision {
  changeItemId: string;
  decision: DecisionType;
  comment?: string; // 필수: REJECT, REQUEST_CHANGES
}

export interface RegenerationState {
  isWaiting: boolean;
  userComment: string;
  startTime?: number;
}
