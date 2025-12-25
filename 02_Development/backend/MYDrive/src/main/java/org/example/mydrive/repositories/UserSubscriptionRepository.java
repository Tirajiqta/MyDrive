package org.example.mydrive.repositories;

import org.example.mydrive.entities.UserSubscriptionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserSubscriptionRepository extends JpaRepository<UserSubscriptionEntity, Long> {
}
