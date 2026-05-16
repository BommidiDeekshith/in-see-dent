package com.inseedent.repository;

import com.inseedent.domain.Deployment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DeploymentRepository extends JpaRepository<Deployment, Long> {
    List<Deployment> findByIncidentId(Long incidentId);
    
    List<Deployment> findByServiceNameAndDeploymentTimeBetween(String serviceName, LocalDateTime start, LocalDateTime end);
}
