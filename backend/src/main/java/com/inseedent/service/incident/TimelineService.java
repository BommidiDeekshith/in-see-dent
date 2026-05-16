package com.inseedent.service.incident;

import com.inseedent.domain.Incident;
import com.inseedent.domain.IncidentTimeline;
import com.inseedent.exception.ResourceNotFoundException;
import com.inseedent.repository.IncidentRepository;
import com.inseedent.repository.IncidentTimelineRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class TimelineService {

    @Autowired
    private IncidentTimelineRepository timelineRepository;

    @Autowired
    private IncidentRepository incidentRepository;

    /**
     * Add a timeline event to an incident
     */
    @Transactional
    public IncidentTimeline addTimelineEvent(Long incidentId, String eventType, String description, String severity) {
        log.debug("Adding timeline event for incident {}: type={}, severity={}", incidentId, eventType, severity);

        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new ResourceNotFoundException("Incident not found with id: " + incidentId));

        IncidentTimeline event = IncidentTimeline.builder()
                .incident(incident)
                .eventType(eventType)
                .description(description)
                .severity(severity)
                .eventTime(LocalDateTime.now())
                .build();

        IncidentTimeline savedEvent = timelineRepository.save(event);
        log.info("Timeline event added for incident {}: id={}", incidentId, savedEvent.getId());

        return savedEvent;
    }

    /**
     * Add a timeline event with additional event data
     */
    @Transactional
    public IncidentTimeline addTimelineEvent(Long incidentId, String eventType, String description, 
                                            String severity, String eventSource, String eventData) {
        log.debug("Adding timeline event for incident {}: type={}, source={}", incidentId, eventType, eventSource);

        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new ResourceNotFoundException("Incident not found with id: " + incidentId));

        IncidentTimeline event = IncidentTimeline.builder()
                .incident(incident)
                .eventType(eventType)
                .description(description)
                .severity(severity)
                .eventSource(eventSource)
                .eventData(eventData)
                .eventTime(LocalDateTime.now())
                .build();

        IncidentTimeline savedEvent = timelineRepository.save(event);
        log.info("Timeline event added for incident {}: id={}, source={}", incidentId, savedEvent.getId(), eventSource);

        return savedEvent;
    }

    /**
     * Get timeline events for an incident, sorted by event time (ascending)
     */
    public List<IncidentTimeline> getTimelineForIncident(Long incidentId) {
        log.debug("Fetching timeline for incident {}", incidentId);

        if (!incidentRepository.existsById(incidentId)) {
            throw new ResourceNotFoundException("Incident not found with id: " + incidentId);
        }

        return timelineRepository.findByIncidentIdOrderByEventTimeAsc(incidentId);
    }

    /**
     * Get timeline events within a time range for an incident
     */
    public List<IncidentTimeline> getTimelineForIncidentBetween(Long incidentId, LocalDateTime start, LocalDateTime end) {
        log.debug("Fetching timeline for incident {} between {} and {}", incidentId, start, end);

        if (!incidentRepository.existsById(incidentId)) {
            throw new ResourceNotFoundException("Incident not found with id: " + incidentId);
        }

        return timelineRepository.findByIncidentIdAndEventTimeBetween(incidentId, start, end);
    }
}
