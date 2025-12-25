package org.example.mydrive.repositories;

import org.example.mydrive.entities.FolderTranslationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FolderTranslationRepository extends JpaRepository<FolderTranslationEntity, Long> {
}
