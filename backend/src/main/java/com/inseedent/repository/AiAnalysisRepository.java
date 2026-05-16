package com.inseedent.repository;

import com.inseedent.domain.AiAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AiAnalysisRepository extends JpaRepository<AiAnalysis, Long> {
    Optional<AiAnalysis> findByIncidentIdOrderByCreatedAtDesc(Long incidentId);
    
    List<AiAnalysis> findByIncidentId(Long incidentId);
    
    List<AiAnalysis> findByAnalysisStatus(String status);
}
