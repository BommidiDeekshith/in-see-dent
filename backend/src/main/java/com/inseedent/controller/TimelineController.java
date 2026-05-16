package com.inseedent.controller;

import com.inseedent.domain.IncidentTimeline;
import com.inseedent.dto.TimelineEventDTO;
import com.inseedent.service.incident.TimelineService;
import com.inseedent.util.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/incidents/{incidentId}/timeline")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Tag(name = "Timeline", description = "Incident chronological timeline")
public class TimelineController {

    private final TimelineService timelineService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<TimelineEventDTO>>> getTimeline(@PathVariable Long incidentId) {
        List<TimelineEventDTO> events = timelineService.getTimelineForIncident(incidentId).stream()
                .map(this::toDto)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(events));
    }

    private TimelineEventDTO toDto(IncidentTimeline e) {
        return TimelineEventDTO.builder()
                .id(e.getId())
                .eventType(e.getEventType())
                .eventSource(e.getEventSource())
                .description(e.getDescription())
                .severity(e.getSeverity())
                .eventTime(e.getEventTime())
                .eventData(e.getEventData())
                .build();
    }
}
