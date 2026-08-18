package com.zerowiki.ingest;

public enum IngestStatus {
    QUEUED,
    SCANNING,
    PLAN_REVIEW,
    PROCESSING,
    QUESTION_REVIEW,
    CHANGE_REVIEW,
    COMPLETED,
    FAILED,
    PAUSED_QUOTA,
    CANCELLED
}
