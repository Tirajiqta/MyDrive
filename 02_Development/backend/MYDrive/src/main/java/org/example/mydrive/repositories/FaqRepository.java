package org.example.mydrive.repositories;

import org.example.mydrive.entities.FaqEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FaqRepository extends JpaRepository<FaqEntity, Long> {
    Optional<FaqEntity> findByInternalQuestionKey(String internalQuestionKey);
}
