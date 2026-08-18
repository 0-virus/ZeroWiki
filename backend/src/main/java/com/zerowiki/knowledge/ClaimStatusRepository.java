package com.zerowiki.knowledge;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClaimStatusRepository extends JpaRepository<ClaimStatusEntity, UUID> {
    List<ClaimStatusEntity> findAllByClaimIdOrderByCreatedAtDesc(UUID claimId);

    Optional<ClaimStatusEntity> findByClaimIdAndPageVersionId(UUID claimId, UUID pageVersionId);
}
