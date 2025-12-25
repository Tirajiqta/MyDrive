package org.example.mydrive.repositories;

import org.example.mydrive.entities.PlanTranslationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlanTranslationRepository extends JpaRepository<PlanTranslationEntity, Long> {
}
