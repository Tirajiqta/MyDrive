package org.example.mydrive.repositories;

import org.example.mydrive.entities.DocumentContentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentContentRepository extends JpaRepository<DocumentContentEntity, Long> {
}
