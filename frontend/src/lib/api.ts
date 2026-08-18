const API_BASE_URL = (process.env.NEXT_PUBLIC_API_BASE_URL ?? '').replace(/\/$/, '');

export interface ApiEnvelope<T> {
  data: T;
}

interface ApiErrorPayload {
  error?: {
    code?: string;
    message?: string;
    requestId?: string;
    details?: Array<{ field?: string; reason?: string }>;
  };
}

export class ApiError extends Error {
  readonly status: number;
  readonly code?: string;
  readonly requestId?: string;
  readonly details: Array<{ field?: string; reason?: string }>;

  constructor(
    status: number,
    message: string,
    options: {
      code?: string;
      requestId?: string;
      details?: Array<{ field?: string; reason?: string }>;
    } = {},
  ) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.code = options.code;
    this.requestId = options.requestId;
    this.details = options.details ?? [];
  }
}

export interface User {
  id: string;
  email: string;
  displayName: string;
  locale?: string;
  status?: string;
  plan?: string;
  createdAt?: string;
}

export interface AuthResponse {
  data: {
    accessToken?: string;
    user?: User;
  };
}

export interface LibraryCounts {
  sources?: number;
  pages?: number;
  openContradictions?: number;
  pendingChanges?: number;
}

export interface LibrarySummary {
  id: string;
  name: string;
  description?: string;
  templateType?: 'LEARNING_RESEARCH' | 'PERSONAL_DECISION' | string;
  visibility?: string;
  status?: string;
  constitutionVersion?: number;
  currentVersion?: { versionNo?: number } | null;
  counts?: LibraryCounts;
  revision?: number;
}

export interface LibraryCreateRequest {
  name: string;
  description: string;
  templateType: 'LEARNING_RESEARCH' | 'PERSONAL_DECISION';
  onboarding: {
    goal: string;
    sourceTypes: string[];
    knowledgeLevel: string;
    trustedSources: string;
    cautionAreas: string;
  };
}

export interface SourceSummary {
  id: string;
  title?: string;
  detectedLanguage?: string | null;
  sourceType?: string;
}

export interface UploadSession {
  uploadId: string;
  uploadUrl: string;
  method: string;
  requiredHeaders: Record<string, string>;
  expiresAt: string;
}

export interface PageSummary {
  id: string;
  libraryId?: string;
  slug?: string;
  pageType?: string;
  status?: string;
  topics?: unknown;
  version?: PageVersion | null;
  claimSummary?: {
    total?: number;
    withoutEvidence?: number;
    openContradictions?: number;
  };
  relatedPages?: RelatedPage[];
}

export interface PageVersion {
  id?: string;
  versionNo: number;
  title: string;
  markdownBody: string;
  summary?: string;
  createdAt?: string;
}

export interface RelatedPage {
  id: string;
  slug?: string | null;
  title?: string | null;
  relationType?: string | null;
}

export interface PageDetail extends PageSummary {
  version: PageVersion;
  relatedPages?: RelatedPage[];
}

export interface Evidence {
  id: string;
  evidenceType?: string;
  sourceVersionId: string;
  sourceTitle: string;
  locator?: unknown;
  excerpt: string;
  excerptLanguage: string;
}

export type KnowledgeStatus =
  | 'VERIFIED_FACT'
  | 'SOURCE_CLAIM'
  | 'AI_SYNTHESIS'
  | 'USER_JUDGMENT'
  | 'UNVERIFIED_HYPOTHESIS'
  | string;

export interface Claim {
  id: string;
  statement: string;
  knowledgeStatus: KnowledgeStatus;
  confidence?: number | null;
  conditions?: unknown;
  evidenceCount?: number;
  evidenceTruncated?: boolean;
  evidences?: Evidence[];
}

export interface RelationSummary {
  id: string;
  libraryId: string;
  sourcePageId: string;
  targetPageId: string;
  relationType: string;
  rationale: string | null;
  confidence: number | null;
  status: string;
  createdByType: string;
  createdAt: string;
}

export type ContradictionClassification =
  | 'TRUE_CONFLICT'
  | 'TIME_CHANGE'
  | 'CONDITION_DIFF'
  | 'VIEWPOINT_DIFF'
  | 'POSSIBLE_CONFLICT';

export type ContradictionStatus = 'OPEN' | 'ACCEPTED' | 'DISMISSED' | 'RESOLVED' | string;

export interface ContradictionSummary {
  id: string;
  leftClaimId: string;
  rightClaimId: string;
  leftClaimStatement: string;
  rightClaimStatement: string;
  classification: ContradictionClassification;
  explanation?: string;
  confidence?: number | null;
  status: ContradictionStatus;
  resolutionNote?: string | null;
  createdAt?: string;
  resolvedAt?: string | null;
}

export interface ContradictionResolution {
  changeSetId?: string;
}

export interface ChangeSetCounts {
  total: number;
  minor: number;
  major: number;
}

export interface ChangeSetSummary {
  id: string;
  libraryId: string;
  status: string;
  riskLevel: string;
  summary: string | null;
  baseLibraryVersionNo: number | null;
  counts: ChangeSetCounts;
  revision: number;
}

export type ChangeSetDecision = 'APPROVE' | 'REJECT' | 'REQUEST_CHANGES' | 'DEFER';

export interface ChangeItemSummary {
  changeItemId: string;
  targetType: string;
  targetId: string;
  operation: string;
  riskLevel: string;
  reason: string | null;
  reviewStatus: string;
  beforeSnapshot: unknown;
  afterSnapshot: unknown;
  evidenceSummary: unknown;
  aiConfidence: number | null;
}

export interface ChangeSetReviewDecision {
  changeItemId: string;
  decision: ChangeSetDecision;
  comment?: string;
}

export type SearchMode = 'KEYWORD' | 'SEMANTIC' | 'HYBRID';

export type SearchResultScope = 'CURRENT' | 'REFERENCED';

export interface SearchOrigin {
  scope: SearchResultScope;
  libraryId: string;
  libraryName: string;
}

export interface SearchResult {
  resultType: string;
  title: string;
  snippet: string;
  score?: number | null;
  pageId?: string | null;
  sourceId?: string | null;
  locator?: unknown;
  origin: SearchOrigin;
}

export interface SearchExpansionRequest {
  query: string;
  targetLibraryIds: string[];
  mode: SearchMode;
}

export type AssistantProcessingMode = 'FAST' | 'STANDARD' | 'PRECISE';

export interface ConversationMessageRequest {
  content: string;
  answerMode: string;
  processingMode: AssistantProcessingMode;
  searchScope: {
    currentLibrary: boolean;
    referencedLibraryIds: string[];
  };
}

export interface ConversationMessageStart {
  userMessageId: string;
  assistantMessageId: string;
  status: string;
}

export interface AssistantCitation {
  id?: string;
  order: number;
  pageId?: string | null;
  pageTitle?: string | null;
  claimId?: string | null;
  sourceVersionId?: string | null;
  locator?: unknown;
}

export interface AssistantSuggestions {
  missingEvidence?: unknown;
  expandToReferencedLibraries?: boolean;
  webSearchAvailable?: boolean;
}

export interface AssistantMessage {
  id: string;
  role: string;
  status: string;
  answerMode?: string;
  content?: string;
  insufficientEvidence?: boolean;
  citations?: AssistantCitation[];
  suggestions?: AssistantSuggestions;
  completedAt?: string | null;
}

export interface SaveProposalRequest {
  target: {
    type: string;
    pageType: string;
    title: string;
  };
  includeCitationIds: string[];
}

export interface SaveProposalResponse {
  changeSetId: string;
  affectedPages?: unknown;
}

export type ConversationFeedbackRating = 'HELPFUL' | 'NOT_HELPFUL';

export interface ConversationFeedbackRequest {
  rating: ConversationFeedbackRating;
  comment?: string;
}

export interface ConversationFeedbackResponse {
  accepted: boolean;
}

export interface UploadCompleteRequest {
  libraryId: string;
  sourceType: 'OBSIDIAN';
  title: string;
  startIngest: boolean;
  processingMode: 'STANDARD';
}

export type IngestStatus =
  | 'QUEUED'
  | 'SCANNING'
  | 'PLAN_REVIEW'
  | 'PROCESSING'
  | 'QUESTION_REVIEW'
  | 'CHANGE_REVIEW'
  | 'COMPLETED'
  | 'FAILED'
  | 'CANCELLED'
  | 'PAUSED_QUOTA';

export interface IngestProgress {
  completed?: number;
  total?: number;
  percent?: number;
}

export interface IngestPlan {
  documents?: number;
  processable?: number;
  duplicates?: number;
  heldForReview?: number;
  topicGroups?: Array<{ name: string; count: number }>;
  estimatedTokens?: number;
  estimatedBillableUnits?: number;
}

export interface IngestJob {
  id: string;
  libraryId?: string;
  importType?: string;
  processingMode?: string;
  status: IngestStatus;
  progress?: IngestProgress | null;
  plan?: IngestPlan | null;
  nextAction?: string | null;
  changeSetId?: string | null;
  createdAt?: string;
}

type ApiResponseObserver = (response: Response) => void;

let accessToken: string | null = null;
let refreshPromise: Promise<string | null> | null = null;

function buildUrl(path: string) {
  return `${API_BASE_URL}${path.startsWith('/') ? path : `/${path}`}`;
}

async function readJson<T>(response: Response): Promise<T | undefined> {
  if (response.status === 204) return undefined;

  const text = await response.text();
  if (!text) return undefined;

  try {
    return JSON.parse(text) as T;
  } catch {
    return undefined;
  }
}

async function readApiError(response: Response) {
  const payload = await readJson<ApiErrorPayload>(response);
  const error = payload?.error;

  return new ApiError(response.status, error?.message ?? '요청을 처리하지 못했습니다.', {
    code: error?.code,
    requestId: error?.requestId,
    details: error?.details,
  });
}

function tokenFromResponse(payload: AuthResponse | undefined) {
  const token = payload?.data?.accessToken;
  return typeof token === 'string' && token.length > 0 ? token : null;
}

export function setAccessToken(token: string | null) {
  accessToken = token;
}

export function clearAccessToken() {
  accessToken = null;
}

export function getApiErrorMessage(error: unknown) {
  if (!(error instanceof ApiError)) return '예상하지 못한 오류가 발생했습니다.';

  switch (error.status) {
    case 401:
      return '로그인이 필요합니다. 다시 로그인해 주세요.';
    case 403:
      return '이 요청을 수행할 권한이 없습니다.';
    case 404:
      return '요청한 자료를 찾을 수 없습니다.';
    case 409:
      return '다른 변경과 충돌했습니다. 최신 상태를 확인해 주세요.';
    case 413:
      return '업로드 파일 크기가 제한을 초과했습니다.';
    case 422:
      return error.details[0]?.reason ?? error.message ?? '입력값을 확인해 주세요.';
    case 429:
      return '요청이 잠시 제한되었습니다. 잠시 후 다시 시도해 주세요.';
    case 503:
      return '일시적인 서버 장애입니다. 잠시 후 다시 시도해 주세요.';
    default:
      return error.message || '요청을 처리하지 못했습니다.';
  }
}

async function refreshAccessToken() {
  if (refreshPromise) return refreshPromise;

  refreshPromise = (async () => {
    try {
      const response = await fetch(buildUrl('/auth/refresh'), {
        method: 'POST',
        credentials: 'include',
        headers: { Accept: 'application/json' },
      });

      if (!response.ok) {
        clearAccessToken();
        return null;
      }

      const payload = await readJson<AuthResponse>(response);
      const token = tokenFromResponse(payload);
      setAccessToken(token);
      return token;
    } catch {
      clearAccessToken();
      return null;
    } finally {
      refreshPromise = null;
    }
  })();

  return refreshPromise;
}

function redirectToLogin() {
  if (typeof window === 'undefined' || window.location.pathname === '/login') return;

  const next = `${window.location.pathname}${window.location.search}`;
  window.location.assign(`/login?next=${encodeURIComponent(next)}`);
}

export async function apiRequest<T>(
  path: string,
  init: RequestInit = {},
  options: {
    retryOnUnauthorized?: boolean;
    onResponse?: ApiResponseObserver;
    allowEmptyResponse?: boolean;
  } = {},
): Promise<T> {
  const headers = new Headers(init.headers);
  headers.set('Accept', 'application/json');

  if (init.body && !(init.body instanceof FormData) && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json');
  }

  if (accessToken) headers.set('Authorization', `Bearer ${accessToken}`);

  const response = await fetch(buildUrl(path), {
    ...init,
    credentials: 'include',
    headers,
  });

  if (response.status === 401 && options.retryOnUnauthorized !== false) {
    const token = await refreshAccessToken();
    if (token) {
      return apiRequest<T>(path, init, {
        retryOnUnauthorized: false,
        onResponse: options.onResponse,
        allowEmptyResponse: options.allowEmptyResponse,
      });
    }

    redirectToLogin();
  }

  options.onResponse?.(response);

  if (!response.ok) throw await readApiError(response);

  const payload = await readJson<T>(response);
  if (response.status !== 204 && payload === undefined && !options.allowEmptyResponse) {
    throw new ApiError(502, '서버 응답을 읽을 수 없습니다.', { code: 'INVALID_RESPONSE' });
  }

  return payload as T;
}

export const api = {
  signUp: (body: { email: string; password: string; displayName: string }) =>
    apiRequest<ApiEnvelope<User>>('/auth/sign-up', {
      method: 'POST',
      body: JSON.stringify(body),
    }),

  login: async (body: { email: string; password: string }) => {
    const response = await apiRequest<AuthResponse>('/auth/login', {
      method: 'POST',
      body: JSON.stringify(body),
    });
    setAccessToken(tokenFromResponse(response));
    return response;
  },

  logout: () =>
    apiRequest<void>('/auth/logout', { method: 'POST' }).finally(clearAccessToken),

  getMe: () => apiRequest<ApiEnvelope<User>>('/me'),

  listLibraries: () => apiRequest<ApiEnvelope<LibrarySummary[]>>('/libraries'),

  createLibrary: (body: LibraryCreateRequest) =>
    apiRequest<ApiEnvelope<LibrarySummary>>('/libraries', {
      method: 'POST',
      body: JSON.stringify(body),
    }),

  getLibrary: (libraryId: string) =>
    apiRequest<ApiEnvelope<LibrarySummary>>(`/libraries/${encodeURIComponent(libraryId)}`),

  getConstitution: (libraryId: string) =>
    apiRequest<ApiEnvelope<Record<string, unknown>>>(
      `/libraries/${encodeURIComponent(libraryId)}/constitution`,
    ),

  listSources: (libraryId: string) =>
    apiRequest<ApiEnvelope<SourceSummary[]>>(
      `/sources?${new URLSearchParams({ libraryId }).toString()}`,
    ),

  createUploadSession: (body: {
    fileName: string;
    mediaType: string;
    byteSize: number;
    sha256: string;
  }) =>
    apiRequest<ApiEnvelope<UploadSession>>('/uploads', {
      method: 'POST',
      body: JSON.stringify(body),
    }),

  uploadPresignedFile: async (session: UploadSession, file: File) => {
    const response = await fetch(session.uploadUrl, {
      method: session.method,
      body: file,
      headers: session.requiredHeaders,
    });
    if (!response.ok) {
      throw new ApiError(response.status, '파일 전송에 실패했습니다.', { code: 'UPLOAD_FAILED' });
    }
  },

  completeUpload: (uploadId: string, body: UploadCompleteRequest) =>
    apiRequest<ApiEnvelope<Record<string, unknown>>>(
      `/uploads/${encodeURIComponent(uploadId)}/complete`,
      { method: 'POST', body: JSON.stringify(body) },
      { allowEmptyResponse: true },
    ),

  deleteUpload: (uploadId: string) =>
    apiRequest<void>(`/uploads/${encodeURIComponent(uploadId)}`, { method: 'DELETE' }),

  listIngestJobs: (libraryId: string) =>
    apiRequest<ApiEnvelope<IngestJob[]>>(
      `/libraries/${encodeURIComponent(libraryId)}/ingest-jobs`,
    ),

  getIngestJob: (
    libraryId: string,
    jobId: string,
    onResponse?: ApiResponseObserver,
  ) =>
    apiRequest<ApiEnvelope<IngestJob>>(
      `/libraries/${encodeURIComponent(libraryId)}/ingest-jobs/${encodeURIComponent(jobId)}`,
      {},
      { onResponse },
    ),

  retryIngestJob: (libraryId: string, jobId: string) =>
    apiRequest<ApiEnvelope<IngestJob> | undefined>(
      `/libraries/${encodeURIComponent(libraryId)}/ingest-jobs/${encodeURIComponent(jobId)}/retry`,
      { method: 'POST' },
      { allowEmptyResponse: true },
    ),

  listPages: (libraryId: string) =>
    apiRequest<ApiEnvelope<PageSummary[]>>(
      `/libraries/${encodeURIComponent(libraryId)}/pages`,
    ),

  getPage: (libraryId: string, pageId: string) =>
    apiRequest<ApiEnvelope<PageDetail>>(
      `/libraries/${encodeURIComponent(libraryId)}/pages/${encodeURIComponent(pageId)}`,
    ),

  listClaims: (libraryId: string, pageId: string) =>
    apiRequest<ApiEnvelope<Claim[]>>(
      `/libraries/${encodeURIComponent(libraryId)}/pages/${encodeURIComponent(pageId)}/claims?include=evidences`,
    ),

  listChangeSets: (libraryId: string) =>
    apiRequest<ApiEnvelope<ChangeSetSummary[]>>(
      `/libraries/${encodeURIComponent(libraryId)}/change-sets?status=READY_FOR_REVIEW`,
    ),

  getChangeSet: (libraryId: string, changeSetId: string) =>
    apiRequest<ApiEnvelope<ChangeSetSummary>>(
      `/libraries/${encodeURIComponent(libraryId)}/change-sets/${encodeURIComponent(changeSetId)}`,
    ),

  listChangeSetItems: (libraryId: string, changeSetId: string) =>
    apiRequest<ApiEnvelope<ChangeItemSummary[]>>(
      `/libraries/${encodeURIComponent(libraryId)}/change-sets/${encodeURIComponent(changeSetId)}/items`,
    ),

  submitChangeSetReviews: (
    libraryId: string,
    changeSetId: string,
    body: { expectedRevision: number; decisions: ChangeSetReviewDecision[] },
  ) =>
    apiRequest<ApiEnvelope<unknown> | undefined>(
      `/libraries/${encodeURIComponent(libraryId)}/change-sets/${encodeURIComponent(changeSetId)}/reviews`,
      { method: 'POST', body: JSON.stringify(body) },
      { allowEmptyResponse: true },
    ),

  searchLibrary: (
    libraryId: string,
    params: { query: string; mode: SearchMode; types?: string[]; limit?: number },
  ) => {
    const query = new URLSearchParams({
      q: params.query,
      types: (params.types ?? ['PAGE', 'CLAIM', 'SOURCE']).join(','),
      mode: params.mode,
      limit: String(params.limit ?? 20),
    });

    return apiRequest<ApiEnvelope<SearchResult[]>>(
      `/libraries/${encodeURIComponent(libraryId)}/search?${query.toString()}`,
    );
  },

  expandLibrarySearch: (libraryId: string, body: SearchExpansionRequest) =>
    apiRequest<ApiEnvelope<SearchResult[]>>(
      `/libraries/${encodeURIComponent(libraryId)}/search-expansions`,
      { method: 'POST', body: JSON.stringify(body) },
    ),

  createConversation: (libraryId: string) =>
    apiRequest<ApiEnvelope<Record<string, unknown>>>(
      `/libraries/${encodeURIComponent(libraryId)}/conversations`,
      { method: 'POST' },
    ),

  sendConversationMessage: (
    libraryId: string,
    conversationId: string,
    body: ConversationMessageRequest,
  ) =>
    apiRequest<ApiEnvelope<ConversationMessageStart>>(
      `/libraries/${encodeURIComponent(libraryId)}/conversations/${encodeURIComponent(conversationId)}/messages`,
      { method: 'POST', body: JSON.stringify(body) },
    ),

  getConversationMessage: (libraryId: string, conversationId: string, messageId: string) =>
    apiRequest<ApiEnvelope<AssistantMessage>>(
      `/libraries/${encodeURIComponent(libraryId)}/conversations/${encodeURIComponent(conversationId)}/messages/${encodeURIComponent(messageId)}`,
    ),

  saveConversationMessageProposal: (
    libraryId: string,
    messageId: string,
    body: SaveProposalRequest,
  ) =>
    apiRequest<ApiEnvelope<SaveProposalResponse>>(
      `/libraries/${encodeURIComponent(libraryId)}/messages/${encodeURIComponent(messageId)}/save-proposal`,
      { method: 'POST', body: JSON.stringify(body) },
    ),

  submitConversationFeedback: (
    libraryId: string,
    messageId: string,
    body: ConversationFeedbackRequest,
  ) =>
    apiRequest<ApiEnvelope<ConversationFeedbackResponse>>(
      `/libraries/${encodeURIComponent(libraryId)}/messages/${encodeURIComponent(messageId)}/feedback`,
      { method: 'POST', body: JSON.stringify(body) },
    ),

  listRelations: (libraryId: string) =>
    apiRequest<ApiEnvelope<RelationSummary[]>>(
      `/libraries/${encodeURIComponent(libraryId)}/relations`,
    ),

  submitRelationFeedback: (
    libraryId: string,
    relationId: string,
    body: { feedbackType: string; comment: string; scope: 'LIBRARY' | 'ALL_LIBRARIES' },
  ) =>
    apiRequest<ApiEnvelope<Record<string, unknown>> | undefined>(
      `/libraries/${encodeURIComponent(libraryId)}/relations/${encodeURIComponent(relationId)}/feedback`,
      { method: 'POST', body: JSON.stringify(body) },
      { allowEmptyResponse: true },
    ),

  listContradictions: (libraryId: string) =>
    apiRequest<ApiEnvelope<ContradictionSummary[]>>(
      `/libraries/${encodeURIComponent(libraryId)}/contradictions`,
    ),

  resolveContradiction: (
    libraryId: string,
    contradictionId: string,
    body: {
      decision: 'CLASSIFY';
      classification: ContradictionClassification;
      resolutionNote: string;
    },
  ) =>
    apiRequest<ApiEnvelope<ContradictionResolution>>(
      `/libraries/${encodeURIComponent(libraryId)}/contradictions/${encodeURIComponent(contradictionId)}/resolution`,
      { method: 'POST', body: JSON.stringify(body) },
    ),
};
