package com.inseedent.repository;

import com.inseedent.domain.AnalysisArtifact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnalysisArtifactRepository extends JpaRepository<AnalysisArtifact, Long> {
    List<AnalysisArtifact> findByAnalysisId(Long analysisId);
    
    List<AnalysisArtifact> findByAnalysisIdAndArtifactType(Long analysisId, String artifactType);
}
