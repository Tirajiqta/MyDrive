package org.example.mydrive.repositories;

import org.example.mydrive.entities.SupportTicketEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SupportTicketRepository extends JpaRepository<SupportTicketEntity, Long> {
    List<SupportTicketEntity> findByUser_IdOrderByCreatedAtDesc(Long userId);
}
