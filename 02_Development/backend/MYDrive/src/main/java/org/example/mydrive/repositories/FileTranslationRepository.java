package org.example.mydrive.repositories;

import org.example.mydrive.entities.FileTranslationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FileTranslationRepository extends JpaRepository<FileTranslationEntity, Long> {
}
