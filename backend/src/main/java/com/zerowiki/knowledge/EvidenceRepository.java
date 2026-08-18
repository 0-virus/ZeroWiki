package com.zerowiki.knowledge;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;
import java.util.Optional;

public interface EvidenceRepository extends JpaRepository<EvidenceEntity, UUID> {
    List<EvidenceEntity> findAllByClaimIdOrderByCreatedAtAsc(UUID claimId);

    Optional<EvidenceEntity> findByClaimIdAndSourceVersionId(UUID claimId, UUID sourceVersionId);
}
