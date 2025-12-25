package org.example.mydrive.repositories;

import org.example.mydrive.entities.SharedItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SharedItemRepository extends JpaRepository<SharedItemEntity, Long> {
}
