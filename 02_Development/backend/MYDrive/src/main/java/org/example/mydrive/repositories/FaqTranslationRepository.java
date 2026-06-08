package org.example.mydrive.repositories;

import org.example.mydrive.entities.FaqTranslationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FaqTranslationRepository extends JpaRepository<FaqTranslationEntity, Long> {
    Optional<FaqTranslationEntity> findByFaq_IdAndLanguageEntity_Code(Long faqId, String code);
}
