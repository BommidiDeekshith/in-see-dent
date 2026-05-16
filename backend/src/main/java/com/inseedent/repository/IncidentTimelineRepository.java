package com.inseedent.repository;

import com.inseedent.domain.IncidentTimeline;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface IncidentTimelineRepository extends JpaRepository<IncidentTimeline, Long> {
    List<IncidentTimeline> findByIncident_IdOrderByEventTimeAsc(Long incidentId);
    
    List<IncidentTimeline> findByIncident_IdAndEventTimeBetween(Long incidentId, LocalDateTime start, LocalDateTime end);
}
