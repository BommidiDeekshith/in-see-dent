package com.inseedent.repository;

import com.inseedent.domain.Embedding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmbeddingRepository extends JpaRepository<Embedding, Long> {
    List<Embedding> findByEntityType(String entityType);
    
    List<Embedding> findByEntityTypeAndEntityId(String entityType, Long entityId);
}
