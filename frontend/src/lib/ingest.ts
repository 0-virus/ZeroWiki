import { IngestJob, IngestStatus } from './api';

export const INGEST_STATUS_LABELS: Record<IngestStatus, string> = {
  QUEUED: '대기',
  SCANNING: '스캔 중',
  PLAN_REVIEW: '계획 승인 대기',
  PROCESSING: '처리 중',
  QUESTION_REVIEW: '질문 대기',
  CHANGE_REVIEW: '변경 승인 대기',
  COMPLETED: '완료',
  FAILED: '실패',
  CANCELLED: '취소됨',
  PAUSED_QUOTA: '처리량 보류',
};

export const INGEST_NEXT_ACTION_LABELS: Record<string, string> = {
  APPROVE_PLAN: '처리 계획 승인',
  ANSWER_QUESTIONS: '질문에 답변',
  REVIEW_CHANGES: '변경 검토',
  PURCHASE_QUOTA: '처리량 구매',
  RETRY_FAILED_ITEMS: '실패 항목 재시도',
};

export function isPollingIngestStatus(status: IngestStatus) {
  return !['COMPLETED', 'FAILED', 'CANCELLED', 'PAUSED_QUOTA'].includes(status);
}

export function progressPercent(job: IngestJob) {
  const percent = job.progress?.percent;
  if (typeof percent === 'number') return Math.min(100, Math.max(0, percent));

  const completed = job.progress?.completed;
  const total = job.progress?.total;
  if (typeof completed === 'number' && typeof total === 'number' && total > 0) {
    return Math.min(100, Math.max(0, Math.round((completed / total) * 100)));
  }

  return null;
}
