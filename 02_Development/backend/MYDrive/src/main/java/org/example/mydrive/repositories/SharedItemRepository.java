package org.example.mydrive.repositories;

import org.example.mydrive.entities.SharedItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SharedItemRepository extends JpaRepository<SharedItemEntity, Long> {
    Optional<SharedItemEntity> findByTokenHash(String tokenHash);
    List<SharedItemEntity> findAllByOwnerId(Long ownerId);
}
