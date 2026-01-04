package org.example.mydrive.repositories;

import org.example.mydrive.entities.FolderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FolderRepository extends JpaRepository<FolderEntity, Long> {
    Optional<FolderEntity> findByIdAndOwnerIdAndIsDeletedFalse(Long id, Long ownerId);

    List<FolderEntity> findAllByOwnerIdAndParentIsNullAndIsDeletedFalse(Long ownerId);

    List<FolderEntity> findAllByOwnerIdAndParentIdAndIsDeletedFalse(Long ownerId, Long parentId);

    boolean existsByOwnerIdAndParentIsNullAndNameIgnoreCaseAndIsDeletedFalse(Long ownerId, String name);

    boolean existsByOwnerIdAndParentIdAndNameIgnoreCaseAndIsDeletedFalse(Long ownerId, Long parentId, String name);

}
