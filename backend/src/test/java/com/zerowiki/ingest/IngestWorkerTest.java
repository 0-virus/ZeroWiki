package com.zerowiki.ingest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IngestWorkerTest {
    @Mock IngestJobRepository jobRepository;
    @Mock IngestService ingestService;

    @Test
    void worker_scans_oldest_scanning_job_before_processing() {
        UUID owner = UUID.randomUUID();
        IngestJobEntity job = IngestJobEntity.create(UUID.randomUUID(), owner, ImportType.OBSIDIAN_ZIP,
                ProcessingMode.STANDARD, new ObjectMapper().createObjectNode(), 0, 0);
        when(jobRepository.findFirstByStatusOrderByCreatedAtAsc(IngestStatus.SCANNING))
                .thenReturn(Optional.of(job));
        IngestWorker worker = new IngestWorker(jobRepository, ingestService);

        assertThat(worker.runOnce()).isTrue();

        verify(ingestService).scan(job.getId());
        verify(jobRepository, never()).findFirstByStatusOrderByCreatedAtAsc(IngestStatus.PROCESSING);
    }

    @Test
    void worker_processes_when_no_scan_is_waiting() {
        UUID owner = UUID.randomUUID();
        IngestJobEntity job = IngestJobEntity.create(UUID.randomUUID(), owner, ImportType.OBSIDIAN_ZIP,
                ProcessingMode.STANDARD, new ObjectMapper().createObjectNode(), 0, 0);
        job.moveToPlanReview(job.getPlan(), job.getEstimatedTokens());
        job.approvePlan();
        when(jobRepository.findFirstByStatusOrderByCreatedAtAsc(IngestStatus.SCANNING))
                .thenReturn(Optional.empty());
        when(jobRepository.findFirstByStatusOrderByCreatedAtAsc(IngestStatus.PROCESSING))
                .thenReturn(Optional.of(job));
        when(ingestService.processOne(job.getId())).thenReturn(true);
        IngestWorker worker = new IngestWorker(jobRepository, ingestService);

        assertThat(worker.runOnce()).isTrue();

        verify(ingestService).processOne(job.getId());
    }
}
