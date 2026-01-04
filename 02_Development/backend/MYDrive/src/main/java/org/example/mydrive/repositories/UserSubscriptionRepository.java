package org.example.mydrive.repositories;

import org.example.mydrive.entities.UserSubscriptionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserSubscriptionRepository extends JpaRepository<UserSubscriptionEntity, Long> {
    Optional<UserSubscriptionEntity> findByPlanEntity_Id(Long planId);
    boolean existsByUserId(Long userId);

}
