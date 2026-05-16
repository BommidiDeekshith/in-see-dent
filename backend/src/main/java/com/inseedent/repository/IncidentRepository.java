package com.inseedent.repository;

import com.inseedent.domain.Incident;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface IncidentRepository extends JpaRepository<Incident, Long> {
    Page<Incident> findByStatus(String status, Pageable pageable);
    
    Page<Incident> findBySeverity(String severity, Pageable pageable);
    
    List<Incident> findByStartTimeBetween(LocalDateTime start, LocalDateTime end);
    
    @Query("SELECT i FROM Incident i WHERE i.status = :status ORDER BY i.startTime DESC")
    List<Incident> findActiveIncidents(@Param("status") String status);
    
    Page<Incident> findAll(Pageable pageable);
}
