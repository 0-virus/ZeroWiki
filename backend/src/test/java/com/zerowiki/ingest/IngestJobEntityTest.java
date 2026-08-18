package com.zerowiki.ingest;

import com.zerowiki.source.SourceVersionEntity;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class IngestJobEntityTest {
    @Test
    void follows_scan_approval_processing_and_review_states() {
        ObjectMapper mapper = new ObjectMapper();
        var plan = mapper.createObjectNode().put("documents", 1).put("processable", 1)
                .put("estimatedTokens", 3);
        IngestJobEntity job = IngestJobEntity.create(UUID.randomUUID(), UUID.randomUUID(), ImportType.FILES,
                ProcessingMode.STANDARD, plan, 3, 1);

        assertThat(job.getStatus()).isEqualTo(IngestStatus.SCANNING);
        job.moveToPlanReview(plan, 3);
        job.approvePlan();
        job.markProgress(1);
        job.moveToChangeReview();

        assertThat(job.getStatus()).isEqualTo(IngestStatus.CHANGE_REVIEW);
        job.complete();
        assertThat(job.getStatus()).isEqualTo(IngestStatus.COMPLETED);
    }
}
