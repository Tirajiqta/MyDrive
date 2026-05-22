package org.example.mydrive.repositories;

import org.example.mydrive.entities.DocumentSignatureEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentSignatureRepository extends JpaRepository<DocumentSignatureEntity, Long> {
    List<DocumentSignatureEntity> findAllByFileId(Long fileId);
}
