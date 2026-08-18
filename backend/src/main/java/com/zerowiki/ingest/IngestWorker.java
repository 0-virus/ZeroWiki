package com.zerowiki.ingest;

import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * The worker profile owns state transitions that do not require user input.
 *
 * // ponytail: one DB-selected job per poll; add a queue/parallel claim only after throughput requires it.
 */
@Component
@Profile("worker")
public class IngestWorker {
    private final IngestJobRepository jobRepository;
    private final IngestService ingestService;

    public IngestWorker(IngestJobRepository jobRepository, IngestService ingestService) {
        this.jobRepository = jobRepository;
        this.ingestService = ingestService;
    }

    @Scheduled(fixedDelayString = "${zerowiki.worker.poll-ms:2000}")
    public void poll() {
        runOnce();
    }

    public boolean runOnce() {
        var scanning = jobRepository.findFirstByStatusOrderByCreatedAtAsc(IngestStatus.SCANNING);
        if (scanning.isPresent()) {
            ingestService.scan(scanning.get().getId());
            return true;
        }
        var processing = jobRepository.findFirstByStatusOrderByCreatedAtAsc(IngestStatus.PROCESSING);
        return processing.isPresent() && ingestService.processOne(processing.get().getId());
    }
}
