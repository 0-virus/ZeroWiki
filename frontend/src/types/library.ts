// 도서관 홈 화면의 타입 정의
// 실제 API 명세는 docs/ZeroWiki-API-명세-초안.md 4.1절 참조

export interface Topic {
  name: string;
  pageCount: number;
}

export interface RecentRelation {
  sourceTitle: string;
  targetTitle: string;
  relationType: string;
  confidence: number;
}

export interface OpenQuestion {
  description: string;
  targetTitle: string;
}

export interface KnowledgeGap {
  description: string;
  targetTitle: string;
}

export interface RecentActivity {
  type: 'INGEST' | 'RELATION' | 'LINT' | 'CONTRADICTION' | 'CHANGE_SET' | 'APPROVAL';
  title: string;
  createdAt: string;
}

export interface LibraryHome {
  topics: Topic[];
  recentRelations: RecentRelation[];
  openQuestions: OpenQuestion[];
  knowledgeGaps: KnowledgeGap[];
  openContradictionCount: number;
  pendingChangeSetCount: number;
  recentActivities: RecentActivity[];
}

export interface LibraryHomeResponse {
  data: {
    id: string;
    name: string;
    home: LibraryHome;
  };
}
