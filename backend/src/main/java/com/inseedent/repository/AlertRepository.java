package com.inseedent.repository;

import com.inseedent.domain.Alert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AlertRepository extends JpaRepository<Alert, Long> {
    List<Alert> findByIncident_Id(Long incidentId);
    
    List<Alert> findByTriggeredAtBetween(LocalDateTime start, LocalDateTime end);
}
