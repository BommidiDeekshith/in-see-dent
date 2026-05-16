package com.inseedent.repository;

import com.inseedent.domain.Log;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LogRepository extends JpaRepository<Log, Long> {
    List<Log> findByIncidentId(Long incidentId);
    
    List<Log> findByServiceNameAndTimestampBetween(String serviceName, LocalDateTime start, LocalDateTime end);
    
    List<Log> findByTraceId(String traceId);
}
