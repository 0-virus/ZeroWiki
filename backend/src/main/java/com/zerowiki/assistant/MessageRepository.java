package com.zerowiki.assistant;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<MessageEntity, UUID> {
    List<MessageEntity> findAllByConversationIdOrderByCreatedAtAsc(UUID conversationId);

    Optional<MessageEntity> findByIdAndConversationId(UUID id, UUID conversationId);
}
