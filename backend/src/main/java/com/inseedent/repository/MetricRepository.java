package com.inseedent.repository;

import com.inseedent.domain.Metric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MetricRepository extends JpaRepository<Metric, Long> {
    List<Metric> findByIncident_Id(Long incidentId);
    
    List<Metric> findByServiceNameAndTimestampBetween(String serviceName, LocalDateTime start, LocalDateTime end);
}
