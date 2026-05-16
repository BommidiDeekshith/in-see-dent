package com.inseedent.controller;

import com.inseedent.domain.Log;
import com.inseedent.exception.ResourceNotFoundException;
import com.inseedent.repository.IncidentRepository;
import com.inseedent.repository.LogRepository;
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
@RequestMapping("/api/v1/incidents/{incidentId}/logs")
public class LogsController {

    @Autowired
    private LogRepository logRepository;

    @Autowired
    private IncidentRepository incidentRepository;

    @Autowired
    private TimelineService timelineService;

    /**
     * List all logs for an incident
     * GET /api/v1/incidents/{incidentId}/logs
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<Log>>> listLogs(@PathVariable Long incidentId) {
        log.info("GET /api/v1/incidents/{}/logs - Listing logs", incidentId);

        try {
            if (!incidentRepository.existsById(incidentId)) {
                throw new ResourceNotFoundException("Incident not found with id: " + incidentId);
            }

            List<Log> logs = logRepository.findByIncident_Id(incidentId);
            return ResponseEntity.ok(ApiResponse.success(logs));
        } catch (ResourceNotFoundException e) {
            log.warn("Incident not found: {}", incidentId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error listing logs", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error listing logs: " + e.getMessage()));
        }
    }

    /**
     * Ingest a new log for an incident
     * POST /api/v1/incidents/{incidentId}/logs
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Log>> createLog(
            @PathVariable Long incidentId,
            @Valid @RequestBody Log logEntry) {
        
        log.info("POST /api/v1/incidents/{}/logs - Creating log entry", incidentId);

        try {
            if (!incidentRepository.existsById(incidentId)) {
                throw new ResourceNotFoundException("Incident not found with id: " + incidentId);
            }

            // Validate log
            if (logEntry.getMessage() == null || logEntry.getMessage().trim().isEmpty()) {
                throw new IllegalArgumentException("Log message cannot be empty");
            }

            // Set incident and timestamp if not provided
            logEntry.setIncident(incidentRepository.findById(incidentId).get());
            if (logEntry.getTimestamp() == null) {
                logEntry.setTimestamp(LocalDateTime.now());
            }

            Log savedLog = logRepository.save(logEntry);

            // Auto-add timeline event for ERROR level logs
            if (logEntry.getLogLevel() != null && logEntry.getLogLevel().equalsIgnoreCase("ERROR")) {
                String description = String.format("Error log: %s from %s", 
                        logEntry.getMessage(), logEntry.getServiceName());
                timelineService.addTimelineEvent(incidentId, "log_error", description, 
                        "high", logEntry.getServiceName(), logEntry.getLogData());
            }

            log.info("Log created successfully for incident {}: id={}", incidentId, savedLog.getId());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(savedLog));
        } catch (ResourceNotFoundException e) {
            log.warn("Incident not found: {}", incidentId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (IllegalArgumentException e) {
            log.warn("Invalid log input: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error creating log", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error creating log: " + e.getMessage()));
        }
    }
}
