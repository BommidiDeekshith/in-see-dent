package com.inseedent.repository;

import com.inseedent.domain.AiAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AiAnalysisRepository extends JpaRepository<AiAnalysis, Long> {
    Optional<AiAnalysis> findByIncident_IdOrderByCreatedAtDesc(Long incidentId);
    
    List<AiAnalysis> findByIncident_Id(Long incidentId);
    
    List<AiAnalysis> findByAnalysisStatus(String status);
}
