package org.example.mydrive.repositories;

import org.example.mydrive.entities.PlanEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PlanRepository extends JpaRepository<PlanEntity, Long> {
    Optional<PlanEntity> findByType(PlanEntity.PlanType type);
    Optional<PlanEntity> findByInternalNameAndIsActiveTrue(String internalName);
    Optional<PlanEntity> findFirstByTypeAndIsActiveTrue(PlanEntity.PlanType type);
}
