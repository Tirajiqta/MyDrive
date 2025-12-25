package org.example.mydrive.repositories;

import org.example.mydrive.entities.FaqTranslationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FaqTranslationRepository extends JpaRepository<FaqTranslationEntity, Long> {
}
