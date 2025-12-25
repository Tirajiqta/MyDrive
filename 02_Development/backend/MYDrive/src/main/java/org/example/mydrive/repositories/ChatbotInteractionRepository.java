package org.example.mydrive.repositories;

import org.example.mydrive.entities.ChatbotInteractionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatbotInteractionRepository extends JpaRepository<ChatbotInteractionEntity, Long> {
}
