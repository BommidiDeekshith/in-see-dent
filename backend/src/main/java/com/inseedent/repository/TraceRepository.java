package com.inseedent.repository;

import com.inseedent.domain.Trace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TraceRepository extends JpaRepository<Trace, Long> {
    List<Trace> findByIncident_Id(Long incidentId);
    
    Optional<Trace> findByTraceId(String traceId);
    
    List<Trace> findByServiceNameAndTimestampBetween(String serviceName, LocalDateTime start, LocalDateTime end);
    
    List<Trace> findByStatus(String status);
}
