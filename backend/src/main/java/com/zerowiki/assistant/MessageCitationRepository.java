package com.zerowiki.assistant;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MessageCitationRepository extends JpaRepository<MessageCitationEntity, UUID> {
    List<MessageCitationEntity> findAllByMessageIdOrderByCitationOrderAsc(UUID messageId);
}
