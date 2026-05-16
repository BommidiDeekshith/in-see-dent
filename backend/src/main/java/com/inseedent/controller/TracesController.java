package com.inseedent.controller;

import com.inseedent.domain.Trace;
import com.inseedent.exception.ResourceNotFoundException;
import com.inseedent.repository.IncidentRepository;
import com.inseedent.repository.TraceRepository;
import com.inseedent.service.incident.TimelineService;
import com.inseedent.util.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/incidents/{incidentId}/traces")
public class TracesController {

    @Autowired
    private TraceRepository traceRepository;

    @Autowired
    private IncidentRepository incidentRepository;

    @Autowired
    private TimelineService timelineService;

    /**
     * List all traces for an incident
     * GET /api/v1/incidents/{incidentId}/traces
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<Trace>>> listTraces(@PathVariable Long incidentId) {
        log.info("GET /api/v1/incidents/{}/traces - Listing traces", incidentId);

        try {
            if (!incidentRepository.existsById(incidentId)) {
                throw new ResourceNotFoundException("Incident not found with id: " + incidentId);
            }

            List<Trace> traces = traceRepository.findByIncident_Id(incidentId);
            return ResponseEntity.ok(ApiResponse.success(traces));
        } catch (ResourceNotFoundException e) {
            log.warn("Incident not found: {}", incidentId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error listing traces", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error listing traces: " + e.getMessage()));
        }
    }

    /**
     * Ingest a new trace for an incident
     * POST /api/v1/incidents/{incidentId}/traces
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Trace>> createTrace(
            @PathVariable Long incidentId,
            @Valid @RequestBody Trace trace) {
        
        log.info("POST /api/v1/incidents/{}/traces - Creating trace: {}", incidentId, trace.getTraceId());

        try {
            if (!incidentRepository.existsById(incidentId)) {
                throw new ResourceNotFoundException("Incident not found with id: " + incidentId);
            }

            // Validate trace
            if (trace.getTraceId() == null || trace.getTraceId().trim().isEmpty()) {
                throw new IllegalArgumentException("Trace ID cannot be empty");
            }

            // Set incident and timestamp if not provided
            trace.setIncident(incidentRepository.findById(incidentId).get());
            if (trace.getTimestamp() == null) {
                trace.setTimestamp(LocalDateTime.now());
            }

            Trace savedTrace = traceRepository.save(trace);

            // Auto-add timeline event for ERROR status traces
            if (savedTrace.getStatus() != null && savedTrace.getStatus().equalsIgnoreCase("ERROR")) {
                String description = String.format("Trace error in %s: %s (duration: %dms)", 
                        savedTrace.getServiceName(), 
                        savedTrace.getErrorMessage(), 
                        savedTrace.getDurationMs() != null ? savedTrace.getDurationMs() : 0);
                timelineService.addTimelineEvent(incidentId, "trace_error", description, 
                        "high", savedTrace.getServiceName(), savedTrace.getTraceData());
            }

            log.info("Trace created successfully for incident {}: id={}", incidentId, savedTrace.getId());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(savedTrace));
        } catch (ResourceNotFoundException e) {
            log.warn("Incident not found: {}", incidentId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (IllegalArgumentException e) {
            log.warn("Invalid trace input: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error creating trace", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error creating trace: " + e.getMessage()));
        }
    }
}
